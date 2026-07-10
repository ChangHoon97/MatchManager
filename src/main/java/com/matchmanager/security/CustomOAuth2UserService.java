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
        final String familyName = oauth2User.getAttribute("family_name");
        final String givenName = oauth2User.getAttribute("given_name");
        if (rawEmail == null) {
            throw new OAuth2AuthenticationException("구글 계정에서 이메일 정보를 가져올 수 없습니다.");
        }
        final String email = rawEmail.trim().toLowerCase();
        final String displayName = resolveDisplayName(name, familyName, givenName, email);

        User user = userRepository.findByEmailAndDelYn(email, "N")
                .orElseGet(() -> {
                    User created = new User(email, null, displayName, displayName,
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

    private String resolveDisplayName(String name, String familyName, String givenName, String email) {
        String trimmedName = trimToNull(name);
        if (trimmedName != null) {
            return trimmedName;
        }

        String combinedName = joinNames(trimToNull(familyName), trimToNull(givenName));
        if (combinedName != null) {
            return combinedName;
        }

        String trimmedGivenName = trimToNull(givenName);
        if (trimmedGivenName != null) {
            return trimmedGivenName;
        }

        int atIndex = email.indexOf('@');
        if (atIndex > 0) {
            return email.substring(0, atIndex);
        }
        return "사용자";
    }

    private String joinNames(String familyName, String givenName) {
        if (familyName == null && givenName == null) {
            return null;
        }
        if (familyName == null) {
            return givenName;
        }
        if (givenName == null) {
            return familyName;
        }
        return familyName + givenName;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
