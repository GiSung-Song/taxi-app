package com.taxi.userservice.oauth.service;

import com.taxi.userservice.enums.Provider;
import com.taxi.userservice.enums.Role;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import java.util.Collection;
import java.util.Map;

@Getter
public class CustomOAuth2User extends DefaultOAuth2User {

    private String email;
    private Role role;
    private Provider provider;
    private String providerId;
    private String name;

    public CustomOAuth2User(Collection<? extends GrantedAuthority> authorities, Map<String, Object> attributes,
                            String nameAttributeKey, String email, Role role, Provider provider, String providerId, String name) {
        super(authorities, attributes, nameAttributeKey);
        this.email = email;
        this.role = role;
        this.provider = provider;
        this.providerId = providerId;
        this.name = name;
    }
}
