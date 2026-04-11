package com.example.auth.domain;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.springframework.data.redis.core.index.Indexed;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;

import java.io.Serializable;
import java.security.Principal;
import java.time.Instant;
import java.util.Set;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
@JsonSubTypes({
        @JsonSubTypes.Type(value = OAuth2AuthorizationCodeGrantAuthorization.class, name = "AuthorizationCodeGrant"),
        @JsonSubTypes.Type(value = OAuth2Authorization.class, name = "OAuth2Authorization")
})
public class OAuth2AuthorizationCodeGrantAuthorization extends OAuth2AuthorizationGrantAuthorization implements Serializable {

    private Principal principal;

    private OAuth2AuthorizationRequest authorizationRequest;

    private AuthorizationCode authorizationCode;

    @Indexed
    private String state;

    public OAuth2AuthorizationCodeGrantAuthorization() {
        super();
    }

    public OAuth2AuthorizationCodeGrantAuthorization(
            String id, String registeredClientId, String principalName, Set<String> authorizedScopes
            , AccessToken accessToken, RefreshToken refreshToken, Principal principal
            , OAuth2AuthorizationRequest authorizationRequest, AuthorizationCode authorizationCode, String state
    ) {
        super(id, registeredClientId, principalName, authorizedScopes, accessToken, refreshToken);
        this.principal = principal;
        this.authorizationRequest = authorizationRequest;
        this.authorizationCode = authorizationCode;
        this.state = state;
    }

    public Principal getPrincipal() {
        return this.principal;
    }

    public OAuth2AuthorizationRequest getAuthorizationRequest() {
        return this.authorizationRequest;
    }

    public AuthorizationCode getAuthorizationCode() {
        return this.authorizationCode;
    }

    public String getState() {
        return this.state;
    }

    public static class AuthorizationCode extends AbstractToken {

        public AuthorizationCode() {
            super();
        }

        public AuthorizationCode(String tokenValue, Instant issuedAt, Instant expiresAt, boolean invalidated) {
            super(tokenValue, issuedAt, expiresAt, invalidated);
        }

    }

}
