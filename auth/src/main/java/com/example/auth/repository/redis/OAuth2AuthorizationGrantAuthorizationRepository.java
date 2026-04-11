package com.example.auth.repository.redis;

import com.example.auth.domain.OAuth2AuthorizationGrantAuthorization;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveHashOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
@Profile("legacy-reactive-auth")
public class OAuth2AuthorizationGrantAuthorizationRepository {

    private final ReactiveHashOperations<String, String, OAuth2AuthorizationGrantAuthorization> hashOps;

    public OAuth2AuthorizationGrantAuthorizationRepository(ReactiveRedisConnectionFactory factory) {
        Jackson2JsonRedisSerializer<OAuth2AuthorizationGrantAuthorization> serializer = new Jackson2JsonRedisSerializer<>(OAuth2AuthorizationGrantAuthorization.class);
        RedisSerializationContext<String, OAuth2AuthorizationGrantAuthorization> context = RedisSerializationContext
                .<String, OAuth2AuthorizationGrantAuthorization>newSerializationContext(new StringRedisSerializer())
                .value(serializer)
                .build();
        this.hashOps = new ReactiveRedisTemplate<>(factory, context).opsForHash();
    }

    public Mono<OAuth2AuthorizationGrantAuthorization> findById(String id) {
        return hashOps.get("auth_grants", id);
    }

    public Mono<OAuth2AuthorizationGrantAuthorization> findByState(String state) {
        return hashOps.get("auth_grants", state);
    }

    public Mono<OAuth2AuthorizationGrantAuthorization> findByAuthorizationCode_TokenValue(String authorizationCode) {
        return hashOps.get("auth_codes", authorizationCode);
    }

    public Mono<OAuth2AuthorizationGrantAuthorization> findByStateOrAuthorizationCode_TokenValue(String state, String authorizationCode) {
        return findByState(state)
                .switchIfEmpty(findByAuthorizationCode_TokenValue(authorizationCode));
    }

    public Mono<OAuth2AuthorizationGrantAuthorization> findByAccessToken_TokenValue(String accessToken) {
        return hashOps.get("access_tokens", accessToken);
    }

    public Mono<OAuth2AuthorizationGrantAuthorization> findByRefreshToken_TokenValue(String refreshToken) {
        return hashOps.get("refresh_tokens", refreshToken);
    }

    public Mono<OAuth2AuthorizationGrantAuthorization> findByIdToken_TokenValue(String idToken) {
        return hashOps.get("id_tokens", idToken);
    }

    public Mono<Boolean> save(OAuth2AuthorizationGrantAuthorization authorization) {
        return hashOps.put("auth_grants", authorization.getId(), authorization);
    }

    public Mono<Long> deleteById(String id) {
        return hashOps.remove("auth_grants", id);
    }

}
