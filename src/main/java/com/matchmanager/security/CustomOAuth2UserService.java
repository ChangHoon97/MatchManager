package com.matchmanager.security;

import com.matchmanager.entity.User;
import com.matchmanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        String rawEmail = oauth2User.getAttribute("email");
        final String sub = oauth2User.getAttribute("sub");
        final String name = oauth2User.getAttribute("name");
        if (rawEmail == null) {
            throw new OAuth2AuthenticationException("구글 계정에서 이메일 정보를 가져올 수 없습니다.");
        }
        final String email = rawEmail.trim().toLowerCase();

        User user = userRepository.findByEmailAndDelYn(email, "N")
                .orElseGet(() -> {
                    User created = new User(email, null,
                            (name != null && !name.isBlank()) ? name : email,
                            null, User.PROVIDER_GOOGLE);
                    created.setProviderId(sub);
                    return userRepository.save(created);
                });

        if (user.getProviderId() == null) {
            user.setProviderId(sub);
            userRepository.save(user);
        }

        UserPrincipal principal = new UserPrincipal(user);
        principal.setAttributes(oauth2User.getAttributes());
        return principal;
    }
}
