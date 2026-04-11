package com.example.auth.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.index.Indexed;
import org.springframework.security.core.GrantedAuthority;

import java.util.Set;

public record OAuth2UserConsent(
        @Id String id
        , @Indexed String registeredClientId
        , @Indexed String principalName
        , Set<GrantedAuthority> authorities
) {
}
