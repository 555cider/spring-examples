package com.example.auth.service;

import com.example.auth.domain.OAuth2AuthorizationCodeGrantAuthorization;
import com.example.auth.domain.OAuth2AuthorizationGrantAuthorization;
import com.example.auth.domain.OAuth2ClientCredentialsGrantAuthorization;
import com.example.auth.util.ModelMapper;
import com.example.auth.util.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@Profile("legacy-reactive-auth")
public class RedisOAuth2AuthorizationService implements OAuth2AuthorizationService {

    private static final Logger logger = LoggerFactory.getLogger(RedisOAuth2AuthorizationService.class);
    private static final String OAUTH2_GRANT_PREFIX = "auth:oauth2:grant:";
    private static final Duration CODE_EXPIRATION = Duration.ofMinutes(10);
    private static final Duration ACCESS_TOKEN_EXPIRATION = Duration.ofHours(3);

    private final RegisteredClientRepository registeredClientRepository;
    private final ReactiveStringRedisTemplate template;

    public RedisOAuth2AuthorizationService(RegisteredClientRepository registeredClientRepository, ReactiveStringRedisTemplate template) {
        this.registeredClientRepository = registeredClientRepository;
        this.template = template;
    }

    @Override
    public void save(OAuth2Authorization authorization) {
        logger.info("〓〓〓 Saving authorization for registered client ID: {}", authorization.getRegisteredClientId());
        OAuth2AuthorizationGrantAuthorization oAuth2AuthorizationGrantAuthorization = ModelMapper.convertOAuth2AuthorizationGrantAuthorization(authorization);

        if (oAuth2AuthorizationGrantAuthorization instanceof OAuth2AuthorizationCodeGrantAuthorization codeAuthz) {
            template.opsForValue()
                    .set(OAUTH2_GRANT_PREFIX + codeAuthz.getAuthorizationCode().getTokenValue(), Serializer.serialize(codeAuthz), CODE_EXPIRATION)
                    .subscribe();
            return;
        }
//        } else if (AuthorizationGrantType.CLIENT_CREDENTIALS.equals(authorizationGrantType)) {
//            return convertOAuth2ClientCredentialsGrantAuthorization(authorization);
//        }
    }

    @Override
    public void remove(OAuth2Authorization authorization) {
        logger.info("〓〓〓 Removing authorization for registered client ID: {}", authorization.getRegisteredClientId());
        template.opsForValue()
                .delete(OAUTH2_GRANT_PREFIX + authorization.getId())
                .subscribe();
    }

    @Override
    public OAuth2Authorization findById(String id) {
        logger.info("〓〓〓 Finding authorization by ID: {}", id);
        return template
                .opsForValue()
                .get(OAUTH2_GRANT_PREFIX + id)
                .map(a -> Serializer.deserialize(a, OAuth2Authorization.class))
                .block();
    }

    @Override
    public OAuth2Authorization findByToken(String token, OAuth2TokenType tokenType) {
        logger.info("〓〓〓 Finding authorization by token: {} {}", tokenType != null ? tokenType.getValue() : null, token);
        if (new OAuth2TokenType(OAuth2ParameterNames.CODE).equals(tokenType)) {
            return template.opsForValue()
                    .get(OAUTH2_GRANT_PREFIX + token)
                    .map(str -> Serializer.deserialize(str, OAuth2AuthorizationCodeGrantAuthorization.class))
                    .mapNotNull(this::toOAuth2Authorization)
                    .block();
        }
        return template.opsForValue()
                .get(OAUTH2_GRANT_PREFIX + token)
                .map(str -> Serializer.deserialize(str, OAuth2ClientCredentialsGrantAuthorization.class))
                .mapNotNull(this::toOAuth2Authorization)
                .block();
    }

    private OAuth2Authorization toOAuth2Authorization(OAuth2AuthorizationGrantAuthorization authorizationGrantAuthorization) {
        String registeredClientId = authorizationGrantAuthorization.getRegisteredClientId();
        RegisteredClient registeredClient = registeredClientRepository.findById(registeredClientId);
        OAuth2Authorization.Builder builder = OAuth2Authorization.withRegisteredClient(registeredClient);
        return ModelMapper
                .mapOAuth2AuthorizationGrantAuthorization(builder, authorizationGrantAuthorization)
                .build();
    }

}
