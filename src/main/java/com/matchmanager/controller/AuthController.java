package com.matchmanager.controller;

import com.matchmanager.dto.LoginRequestDto;
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
        if (userRepository.findByEmailAndDelYn(email, "N").isPresent()) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }

        User user = new User(
                email,
                passwordEncoder.encode(req.getPassword()),
                req.getNickname(),
                (req.getCelno() == null || req.getCelno().isBlank()) ? null : req.getCelno(),
                User.PROVIDER_LOCAL
        );
        userRepository.save(user);

        return ResponseEntity.ok(new UserInfoDto(user.getId(), user.getEmail(), user.getNickname()));
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
        return ResponseEntity.ok(new UserInfoDto(principal.getId(), principal.getEmail(), principal.getNickname()));
    }

    private java.util.Map<String, Object> errorBody(String message) {
        return java.util.Map.of("error", true, "message", message);
    }
}
