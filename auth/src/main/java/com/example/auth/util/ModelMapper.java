package com.example.auth.util;

import com.example.auth.domain.*;
import com.example.auth.domain.OAuth2AuthorizationCodeGrantAuthorization.AuthorizationCode;
import com.example.auth.domain.OAuth2AuthorizationGrantAuthorization.AccessToken;
import com.example.auth.domain.OAuth2AuthorizationGrantAuthorization.RefreshToken;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization.Token;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.util.StringUtils;

import java.security.Principal;

public class ModelMapper {

    public static OAuth2UserConsent convertOAuth2UserConsent(OAuth2AuthorizationConsent authorizationConsent) {
        String id = authorizationConsent.getRegisteredClientId().concat("-").concat(authorizationConsent.getPrincipalName());
        return new OAuth2UserConsent(id, authorizationConsent.getRegisteredClientId(), authorizationConsent.getPrincipalName(), authorizationConsent.getAuthorities());
    }

    public static OAuth2AuthorizationGrantAuthorization convertOAuth2AuthorizationGrantAuthorization(OAuth2Authorization authorization) {
        AuthorizationGrantType authorizationGrantType = authorization.getAuthorizationGrantType();
        if (AuthorizationGrantType.AUTHORIZATION_CODE.equals(authorizationGrantType)) {
            return convertOAuth2AuthorizationCodeGrantAuthorization(authorization);
        }
        if (AuthorizationGrantType.CLIENT_CREDENTIALS.equals(authorizationGrantType)) {
            return convertOAuth2ClientCredentialsGrantAuthorization(authorization);
        }
        return null;
    }

    private static OAuth2AuthorizationGrantAuthorization convertOAuth2AuthorizationCodeGrantAuthorization(OAuth2Authorization authorization) {
        OAuth2AuthorizationRequest authorizationRequest = authorization.getAttribute(OAuth2AuthorizationRequest.class.getName());

        AuthorizationCode authorizationCode = extractAuthorizationCode(authorization);
        AccessToken accessToken = extractAccessToken(authorization);
        RefreshToken refreshToken = extractRefreshToken(authorization);
        if (authorizationRequest == null || !authorizationRequest.getScopes().contains(OidcScopes.OPENID)) {
            return new OAuth2AuthorizationCodeGrantAuthorization(
                    authorization.getId(), authorization.getRegisteredClientId(), authorization.getPrincipalName()
                    , authorization.getAuthorizedScopes(), accessToken, refreshToken
                    , authorization.getAttribute(Principal.class.getName()), authorization.getAttribute(OAuth2AuthorizationRequest.class.getName())
                    , authorizationCode, authorization.getAttribute(OAuth2ParameterNames.STATE));
        }

        OidcAuthorizationCodeGrantAuthorization.IdToken idToken = extractIdToken(authorization);
        return new OidcAuthorizationCodeGrantAuthorization(
                authorization.getId(), authorization.getRegisteredClientId(), authorization.getPrincipalName()
                , authorization.getAuthorizedScopes(), accessToken, refreshToken
                , authorization.getAttribute(Principal.class.getName()), authorization.getAttribute(OAuth2AuthorizationRequest.class.getName())
                , authorizationCode, authorization.getAttribute(OAuth2ParameterNames.STATE), idToken);
    }

    private static OAuth2ClientCredentialsGrantAuthorization convertOAuth2ClientCredentialsGrantAuthorization(OAuth2Authorization authorization) {
        AccessToken accessToken = extractAccessToken(authorization);
        return new OAuth2ClientCredentialsGrantAuthorization(authorization.getId(), authorization.getRegisteredClientId(), authorization.getPrincipalName(), authorization.getAuthorizedScopes(), accessToken);
    }

    private static AuthorizationCode extractAuthorizationCode(OAuth2Authorization authorization) {
        if (authorization.getToken(OAuth2AuthorizationCode.class) == null) {
            return null;
        }

        Token<OAuth2AuthorizationCode> oauth2AuthorizationCode = authorization.getToken(OAuth2AuthorizationCode.class);
        return new AuthorizationCode(oauth2AuthorizationCode.getToken().getTokenValue(), oauth2AuthorizationCode.getToken().getIssuedAt(), oauth2AuthorizationCode.getToken().getExpiresAt(), oauth2AuthorizationCode.isInvalidated());
    }

    private static AccessToken extractAccessToken(OAuth2Authorization authorization) {
        Token<OAuth2AccessToken> oauth2AccessToken = authorization.getAccessToken();
        if (oauth2AccessToken == null) {
            return null;
        }

        String tokenMetadata = oauth2AccessToken.getMetadata(OAuth2TokenFormat.class.getName());
        OAuth2TokenFormat tokenFormat = null;
        if (OAuth2TokenFormat.SELF_CONTAINED.getValue().equals(tokenMetadata)) {
            tokenFormat = OAuth2TokenFormat.SELF_CONTAINED;
        } else if (OAuth2TokenFormat.REFERENCE.getValue().equals(tokenMetadata)) {
            tokenFormat = OAuth2TokenFormat.REFERENCE;
        }
        return new AccessToken(oauth2AccessToken.getToken().getTokenValue(), oauth2AccessToken.getToken().getIssuedAt(), oauth2AccessToken.getToken().getExpiresAt(), oauth2AccessToken.isInvalidated(), oauth2AccessToken.getToken().getTokenType(), oauth2AccessToken.getToken().getScopes(), tokenFormat, new OAuth2AuthorizationGrantAuthorization.ClaimsHolder(oauth2AccessToken.getClaims()));
    }

    private static RefreshToken extractRefreshToken(OAuth2Authorization authorization) {
        Token<OAuth2RefreshToken> oauth2RefreshToken = authorization.getRefreshToken();
        if (oauth2RefreshToken == null) {
            return null;
        }

        return new RefreshToken(oauth2RefreshToken.getToken().getTokenValue(), oauth2RefreshToken.getToken().getIssuedAt(), oauth2RefreshToken.getToken().getExpiresAt(), oauth2RefreshToken.isInvalidated());
    }

    private static OidcAuthorizationCodeGrantAuthorization.IdToken extractIdToken(OAuth2Authorization authorization) {
        Token<OidcIdToken> oidcIdToken = authorization.getToken(OidcIdToken.class);
        if (oidcIdToken == null) {
            return null;
        }

        return new OidcAuthorizationCodeGrantAuthorization.IdToken(oidcIdToken.getToken().getTokenValue(), oidcIdToken.getToken().getIssuedAt(), oidcIdToken.getToken().getExpiresAt(), oidcIdToken.isInvalidated(), new OAuth2AuthorizationGrantAuthorization.ClaimsHolder(oidcIdToken.getClaims()));
    }

    public static OAuth2AuthorizationConsent convertOAuth2AuthorizationConsent(OAuth2UserConsent userConsent) {
        return OAuth2AuthorizationConsent.withId(userConsent.registeredClientId(), userConsent.principalName()).authorities((authorities) -> authorities.addAll(userConsent.authorities())).build();
    }

    public static OAuth2Authorization.Builder mapOAuth2AuthorizationGrantAuthorization(OAuth2Authorization.Builder builder, OAuth2AuthorizationGrantAuthorization authorizationGrantAuthorization) {
        if (authorizationGrantAuthorization instanceof OAuth2AuthorizationCodeGrantAuthorization authorizationGrant) {
            builder = mapOAuth2AuthorizationCodeGrantAuthorization(builder, authorizationGrant);
        } else if (authorizationGrantAuthorization instanceof OAuth2ClientCredentialsGrantAuthorization authorizationGrant) {
            builder = mapOAuth2ClientCredentialsGrantAuthorization(builder, authorizationGrant);
        }
        builder.principalName(authorizationGrantAuthorization.getPrincipalName());
        builder.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE);
        return builder;
    }

    private static OAuth2Authorization.Builder mapOAuth2AuthorizationCodeGrantAuthorization(OAuth2Authorization.Builder builder, OAuth2AuthorizationCodeGrantAuthorization authorizationCodeGrantAuthorization) {
        builder.id(authorizationCodeGrantAuthorization.getId())
                .principalName(authorizationCodeGrantAuthorization.getPrincipalName())
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizedScopes(authorizationCodeGrantAuthorization.getAuthorizedScopes())
                .attribute(Principal.class.getName(), authorizationCodeGrantAuthorization.getPrincipal())
                .attribute(OAuth2AuthorizationRequest.class.getName(), authorizationCodeGrantAuthorization.getAuthorizationRequest());
        if (StringUtils.hasText(authorizationCodeGrantAuthorization.getState())) {
            builder.attribute(OAuth2ParameterNames.STATE, authorizationCodeGrantAuthorization.getState());
        }

        builder = mapAuthorizationCode(builder, authorizationCodeGrantAuthorization.getAuthorizationCode());
        builder = mapAccessToken(builder, authorizationCodeGrantAuthorization.getAccessToken());
        builder = mapRefreshToken(builder, authorizationCodeGrantAuthorization.getRefreshToken());
        if (authorizationCodeGrantAuthorization instanceof OidcAuthorizationCodeGrantAuthorization authorizationGrant) {
            builder = mapIdToken(builder, authorizationGrant.getIdToken());
        }

        return builder;
    }

    private static OAuth2Authorization.Builder mapOAuth2ClientCredentialsGrantAuthorization(OAuth2Authorization.Builder builder, OAuth2ClientCredentialsGrantAuthorization clientCredentialsGrantAuthorization) {
        builder.id(clientCredentialsGrantAuthorization.getId()).principalName(clientCredentialsGrantAuthorization.getPrincipalName()).authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS).authorizedScopes(clientCredentialsGrantAuthorization.getAuthorizedScopes());

        return mapAccessToken(builder, clientCredentialsGrantAuthorization.getAccessToken());
    }

    static OAuth2Authorization.Builder mapAuthorizationCode(OAuth2Authorization.Builder builder, AuthorizationCode authorizationCode) {
        if (authorizationCode == null) {
            return builder;
        }

        OAuth2AuthorizationCode oauth2AuthorizationCode = new OAuth2AuthorizationCode(authorizationCode.getTokenValue(), authorizationCode.getIssuedAt(), authorizationCode.getExpiresAt());
        return builder.token(oauth2AuthorizationCode, metadata -> metadata.put(Token.INVALIDATED_METADATA_NAME, authorizationCode.isInvalidated()));
    }

    static OAuth2Authorization.Builder mapAccessToken(OAuth2Authorization.Builder builder, AccessToken accessToken) {
        if (accessToken == null) {
            return builder;
        }

        OAuth2AccessToken oauth2AccessToken = new OAuth2AccessToken(accessToken.getTokenType(), accessToken.getTokenValue(), accessToken.getIssuedAt(), accessToken.getExpiresAt(), accessToken.getScopes());
        return builder.token(oauth2AccessToken, metadata -> {
            metadata.put(Token.INVALIDATED_METADATA_NAME, accessToken.isInvalidated());
            metadata.put(Token.CLAIMS_METADATA_NAME, accessToken.getClaims().getClaims());
            metadata.put(OAuth2TokenFormat.class.getName(), accessToken.getTokenFormat().getValue());
        });
    }

    static OAuth2Authorization.Builder mapRefreshToken(OAuth2Authorization.Builder builder, RefreshToken refreshToken) {
        if (refreshToken == null) {
            return builder;
        }

        OAuth2RefreshToken oauth2RefreshToken = new OAuth2RefreshToken(refreshToken.getTokenValue(), refreshToken.getIssuedAt(), refreshToken.getExpiresAt());
        return builder.token(oauth2RefreshToken, metadata -> metadata.put(Token.INVALIDATED_METADATA_NAME, refreshToken.isInvalidated()));
    }

    static OAuth2Authorization.Builder mapIdToken(OAuth2Authorization.Builder builder, OidcAuthorizationCodeGrantAuthorization.IdToken idToken) {
        if (idToken == null) {
            return builder;
        }

        OidcIdToken oidcIdToken = new OidcIdToken(idToken.getTokenValue(), idToken.getIssuedAt(), idToken.getExpiresAt(), idToken.getClaims().getClaims());
        return builder.token(oidcIdToken, metadata -> {
            metadata.put(Token.INVALIDATED_METADATA_NAME, idToken.isInvalidated());
            metadata.put(Token.CLAIMS_METADATA_NAME, idToken.getClaims().getClaims());
        });
    }

}
