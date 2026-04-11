package com.example.auth.domain;

import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import java.io.Serializable;
import java.security.Principal;
import java.time.Instant;
import java.util.Set;

public class OidcAuthorizationCodeGrantAuthorization extends OAuth2AuthorizationCodeGrantAuthorization implements Serializable {

    private IdToken idToken;

    public OidcAuthorizationCodeGrantAuthorization() {
    }

    public OidcAuthorizationCodeGrantAuthorization(
            String id, String registeredClientId, String principalName, Set<String> authorizedScopes
            , AccessToken accessToken, RefreshToken refreshToken, Principal principal
            , OAuth2AuthorizationRequest authorizationRequest, AuthorizationCode authorizationCode, String state
            , IdToken idToken
    ) {
        super(id, registeredClientId, principalName, authorizedScopes, accessToken, refreshToken, principal,
                authorizationRequest, authorizationCode, state);
        this.idToken = idToken;
    }

    public OidcAuthorizationCodeGrantAuthorization(OAuth2AuthorizationCodeGrantAuthorization authorization, IdToken idtoken) {
        super(authorization.getId(), authorization.getRegisteredClientId(), authorization.getPrincipalName()
                , authorization.getAuthorizedScopes(), authorization.getAccessToken(), authorization.getRefreshToken()
                , authorization.getPrincipal(), authorization.getAuthorizationRequest()
                , authorization.getAuthorizationCode(), authorization.getState());
        this.idToken = idtoken;
    }

    public IdToken getIdToken() {
        return this.idToken;
    }

    public static class IdToken extends AbstractToken {

        private ClaimsHolder claims;

        public IdToken() {
        }

        public IdToken(
                String tokenValue, Instant issuedAt, Instant expiresAt, boolean invalidated, ClaimsHolder claims
        ) {
            super(tokenValue, issuedAt, expiresAt, invalidated);
            this.claims = claims;
        }

        public ClaimsHolder getClaims() {
            return this.claims;
        }

    }

}
