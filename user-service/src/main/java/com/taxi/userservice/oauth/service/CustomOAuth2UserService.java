package com.taxi.userservice.oauth.service;

import com.taxi.userservice.entity.User;
import com.taxi.userservice.enums.Provider;
import com.taxi.userservice.enums.Role;
import com.taxi.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        DefaultOAuth2UserService oAuth2UserService = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = oAuth2UserService.loadUser(userRequest);

        String provider = userRequest.getClientRegistration().getRegistrationId();
        String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        OAuthAttributes attributes = OAuthAttributes.of(provider, userNameAttributeName, oAuth2User.getAttributes());

        String email = attributes.getOAuth2UserInfo().getEmail();

        User user = userRepository.findByEmail(email);

        Role role = user != null ? user.getRole() : Role.GUEST;
        String name = attributes.getOAuth2UserInfo().getName();
        Provider userProvider = attributes.getOAuth2UserInfo().getProvider();
        String providerId = attributes.getOAuth2UserInfo().getProviderId();

        return new CustomOAuth2User(
                oAuth2User.getAuthorities(),
                oAuth2User.getAttributes(),
                userNameAttributeName,
                email,
                role,
                userProvider,
                providerId,
                name
        );
    }
}
