package com.example.auth.domain;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.index.Indexed;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = OAuth2AuthorizationGrantAuthorization.AccessToken.class, name = "ACCESS_TOKEN"),
        @JsonSubTypes.Type(value = OAuth2AuthorizationGrantAuthorization.RefreshToken.class, name = "REFRESH_TOKEN")
})
public class OAuth2AuthorizationGrantAuthorization {

    @Id
    private String id;

    private String registeredClientId;

    private String principalName;

    private Set<String> authorizedScopes;

    private AccessToken accessToken;

    private RefreshToken refreshToken;

    public OAuth2AuthorizationGrantAuthorization() {
    }

    protected OAuth2AuthorizationGrantAuthorization(
            String id, String registeredClientId, String principalName, Set<String> authorizedScopes
            , AccessToken accessToken, RefreshToken refreshToken
    ) {
        this.id = id;
        this.registeredClientId = registeredClientId;
        this.principalName = principalName;
        this.authorizedScopes = authorizedScopes;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    public String getId() {
        return this.id;
    }

    public String getRegisteredClientId() {
        return this.registeredClientId;
    }

    public String getPrincipalName() {
        return this.principalName;
    }

    public Set<String> getAuthorizedScopes() {
        return this.authorizedScopes;
    }

    public AccessToken getAccessToken() {
        return this.accessToken;
    }

    public RefreshToken getRefreshToken() {
        return this.refreshToken;
    }

    protected abstract static class AbstractToken implements Serializable {

        @Indexed
        private String tokenValue;

        private Instant issuedAt;

        private Instant expiresAt;

        private boolean invalidated;

        protected AbstractToken(String tokenValue, Instant issuedAt, Instant expiresAt, boolean invalidated) {
            this.tokenValue = tokenValue;
            this.issuedAt = issuedAt;
            this.expiresAt = expiresAt;
            this.invalidated = invalidated;
        }

        public AbstractToken() {

        }

        public String getTokenValue() {
            return this.tokenValue;
        }

        public Instant getIssuedAt() {
            return this.issuedAt;
        }

        public Instant getExpiresAt() {
            return this.expiresAt;
        }

        public boolean isInvalidated() {
            return this.invalidated;
        }

    }

    public static class ClaimsHolder {

        private final Map<String, Object> claims;

        public ClaimsHolder(Map<String, Object> claims) {
            this.claims = claims;
        }

        public Map<String, Object> getClaims() {
            return this.claims;
        }

    }

    public static class AccessToken extends AbstractToken {

        private OAuth2AccessToken.TokenType tokenType;

        private Set<String> scopes;

        private OAuth2TokenFormat tokenFormat;

        private ClaimsHolder claims;

        public AccessToken() {

        }

        public AccessToken(
                String tokenValue, Instant issuedAt, Instant expiresAt, boolean invalidated
                , OAuth2AccessToken.TokenType tokenType, Set<String> scopes, OAuth2TokenFormat tokenFormat
                , ClaimsHolder claims
        ) {
            super(tokenValue, issuedAt, expiresAt, invalidated);
            this.tokenType = tokenType;
            this.scopes = scopes;
            this.tokenFormat = tokenFormat;
            this.claims = claims;
        }

        public OAuth2AccessToken.TokenType getTokenType() {
            return this.tokenType;
        }

        public Set<String> getScopes() {
            return this.scopes;
        }

        public OAuth2TokenFormat getTokenFormat() {
            return this.tokenFormat;
        }

        public ClaimsHolder getClaims() {
            return this.claims;
        }

    }

    public static class RefreshToken extends AbstractToken {

        public RefreshToken(String tokenValue, Instant issuedAt, Instant expiresAt, boolean invalidated) {
            super(tokenValue, issuedAt, expiresAt, invalidated);
        }

    }

}
