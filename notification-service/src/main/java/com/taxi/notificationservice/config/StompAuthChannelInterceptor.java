package com.taxi.notificationservice.config;

import com.taxi.common.core.exception.CustomAuthException;
import com.taxi.common.core.exception.CustomInternalException;
import com.taxi.common.security.JwtTokenUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenUtil jwtTokenUtil;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authorization = accessor.getFirstNativeHeader("Authorization");

            if (authorization != null && authorization.startsWith("Bearer ")) {
                try {
                    String token = authorization.substring(7);

                    String email = jwtTokenUtil.extractEmail(token);
                    String role = jwtTokenUtil.extractRole(token);

                    log.info("email : {}", email);
                    log.info("role : {}", role);

                    UsernamePasswordAuthenticationToken authenticationToken
                            = new UsernamePasswordAuthenticationToken(email, null, List.of(new SimpleGrantedAuthority(role)));

                    accessor.setUser(authenticationToken);
                } catch (Exception e) {
                    throw new CustomAuthException("토큰이 올바르지 않습니다.");
                }
            } else {
                throw new CustomInternalException("토큰이 없습니다.");
            }
        }

        return message;
    }
}
