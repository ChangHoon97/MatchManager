package com.matchmanager.controller;

import com.matchmanager.dto.LoginRequestDto;
import com.matchmanager.dto.PasswordUpdateRequestDto;
import com.matchmanager.dto.ProfileUpdateRequestDto;
import com.matchmanager.dto.SignupRequestDto;
import com.matchmanager.dto.UserInfoDto;
import com.matchmanager.entity.User;
import com.matchmanager.repository.UserRepository;
import com.matchmanager.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final HttpSessionSecurityContextRepository securityContextRepository;

    @PostMapping("/signup")
    public ResponseEntity<UserInfoDto> signup(@Valid @RequestBody SignupRequestDto req) {
        String email = req.getEmail().trim().toLowerCase();
        String nickname = req.getNickname().trim();
        if (userRepository.findByEmailAndDelYn(email, "N").isPresent()) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }
        if (userRepository.findByNicknameAndDelYn(nickname, "N").isPresent()) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }

        User user = new User(
                email,
                passwordEncoder.encode(req.getPassword()),
                nickname,
                (req.getCelno() == null || req.getCelno().isBlank()) ? null : req.getCelno(),
                User.PROVIDER_LOCAL
        );
        userRepository.save(user);

        return ResponseEntity.ok(toUserInfo(user));
    }

    @PostMapping("/login")
    public ResponseEntity<UserInfoDto> login(@Valid @RequestBody LoginRequestDto req,
                                              HttpServletRequest request,
                                              HttpServletResponse response) {
        Authentication authRequest = new UsernamePasswordAuthenticationToken(
                req.getEmail().trim().toLowerCase(), req.getPassword());
        Authentication authentication = authenticationManager.authenticate(authRequest);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(new UserInfoDto(principal.getId(), principal.getEmail(), principal.getNickname()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        var session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(errorBody("로그인이 필요합니다."));
        }
        User user = userRepository.findByIdAndDelYn(principal.getId(), "N")
                .orElseThrow(() -> new IllegalArgumentException("사용자 정보를 찾을 수 없습니다."));
        return ResponseEntity.ok(toUserInfo(user));
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateMe(@AuthenticationPrincipal UserPrincipal principal,
                                      @Valid @RequestBody ProfileUpdateRequestDto req,
                                      HttpServletRequest request,
                                      HttpServletResponse response) {
        if (principal == null) {
            return ResponseEntity.status(401).body(errorBody("로그인이 필요합니다."));
        }

        User user = userRepository.findByIdAndDelYn(principal.getId(), "N")
                .orElseThrow(() -> new IllegalArgumentException("사용자 정보를 찾을 수 없습니다."));
        String nickname = req.getNickname().trim();
        userRepository.findByNicknameAndDelYn(nickname, "N")
                .filter(existing -> !existing.getId().equals(user.getId()))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
                });

        user.setNickname(nickname);
        user.setCelno((req.getCelno() == null || req.getCelno().isBlank()) ? null : req.getCelno().trim());
        userRepository.save(user);
        refreshAuthentication(user, request, response);

        return ResponseEntity.ok(toUserInfo(user));
    }

    @PutMapping("/me/password")
    public ResponseEntity<?> updatePassword(@AuthenticationPrincipal UserPrincipal principal,
                                            @Valid @RequestBody PasswordUpdateRequestDto req,
                                            HttpServletRequest request,
                                            HttpServletResponse response) {
        if (principal == null) {
            return ResponseEntity.status(401).body(errorBody("로그인이 필요합니다."));
        }

        User user = userRepository.findByIdAndDelYn(principal.getId(), "N")
                .orElseThrow(() -> new IllegalArgumentException("사용자 정보를 찾을 수 없습니다."));
        if (!User.PROVIDER_LOCAL.equals(user.getProvider())) {
            throw new IllegalArgumentException("Google 로그인 계정은 비밀번호를 변경할 수 없습니다.");
        }
        if (user.getPassword() == null || !passwordEncoder.matches(req.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 올바르지 않습니다.");
        }

        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);
        refreshAuthentication(user, request, response);

        return ResponseEntity.noContent().build();
    }

    private java.util.Map<String, Object> errorBody(String message) {
        return java.util.Map.of("error", true, "message", message);
    }

    private UserInfoDto toUserInfo(User user) {
        return new UserInfoDto(user.getId(), user.getEmail(), user.getNickname(), user.getCelno(), user.getProvider());
    }

    private void refreshAuthentication(User user, HttpServletRequest request, HttpServletResponse response) {
        Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal updatedPrincipal = new UserPrincipal(user);
        Authentication updatedAuth = new UsernamePasswordAuthenticationToken(
                updatedPrincipal,
                currentAuth == null ? null : currentAuth.getCredentials(),
                updatedPrincipal.getAuthorities()
        );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(updatedAuth);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }
}
