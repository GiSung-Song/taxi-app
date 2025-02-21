package com.taxi.userservice.oauth.service;

import com.taxi.userservice.oauth.provider.GoogleUserInfo;
import com.taxi.userservice.oauth.provider.KakaoUserInfo;
import com.taxi.userservice.oauth.provider.NaverUserInfo;
import com.taxi.userservice.oauth.provider.OAuth2UserInfo;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
public class OAuthAttributes {

    private String nameAttributeKey;
    private OAuth2UserInfo oAuth2UserInfo;

    @Builder
    public OAuthAttributes(String nameAttributeKey, OAuth2UserInfo oAuth2UserInfo) {
        this.nameAttributeKey = nameAttributeKey;
        this.oAuth2UserInfo = oAuth2UserInfo;
    }

    public static OAuthAttributes of(String provider, String usernameAttributeName, Map<String, Object> attributes) {
        if ("kakao".equalsIgnoreCase(provider)) {
            return ofKakao(usernameAttributeName, attributes);
        } else if ("naver".equalsIgnoreCase(provider)) {
            return ofNaver(usernameAttributeName, attributes);
        } else {
            return ofGoogle(usernameAttributeName, attributes);
        }
    }

    public static OAuthAttributes ofKakao(String userNameAttributeName, Map<String, Object> attributes) {
        return OAuthAttributes.builder()
                .nameAttributeKey(userNameAttributeName)
                .oAuth2UserInfo(new KakaoUserInfo(attributes))
                .build();
    }

    public static OAuthAttributes ofNaver(String userNameAttributeName, Map<String, Object> attributes) {
        return OAuthAttributes.builder()
                .nameAttributeKey(userNameAttributeName)
                .oAuth2UserInfo(new NaverUserInfo(attributes))
                .build();
    }

    public static OAuthAttributes ofGoogle(String userNameAttributeName, Map<String, Object> attributes) {
        return OAuthAttributes.builder()
                .nameAttributeKey(userNameAttributeName)
                .oAuth2UserInfo(new GoogleUserInfo(attributes))
                .build();
    }
}
