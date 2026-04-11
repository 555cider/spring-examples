package com.example.auth.repository.redis;

import com.example.auth.domain.OAuth2AuthorizationGrantAuthorization;
import com.example.auth.domain.OAuth2UserConsent;
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
public class OAuth2UserConsentRepository {

    private final ReactiveHashOperations<String, String, OAuth2UserConsent> hashOps;

    public OAuth2UserConsentRepository(ReactiveRedisConnectionFactory factory) {
        Jackson2JsonRedisSerializer<OAuth2AuthorizationGrantAuthorization> serializer = new Jackson2JsonRedisSerializer<>(OAuth2AuthorizationGrantAuthorization.class);
        RedisSerializationContext<String, OAuth2AuthorizationGrantAuthorization> context = RedisSerializationContext
                .<String, OAuth2AuthorizationGrantAuthorization>newSerializationContext(new StringRedisSerializer())
                .value(serializer)
                .build();
        this.hashOps = new ReactiveRedisTemplate<>(factory, context).opsForHash();
    }

    public Mono<OAuth2UserConsent> findByRegisteredClientIdAndPrincipalName(String registeredClientId, String principalName) {
        String key = buildKey(registeredClientId, principalName);
        return hashOps.get("user_consents", key);
    }

    public Mono<Void> save(OAuth2UserConsent userConsent) {
        String key = buildKey(userConsent.registeredClientId(), userConsent.principalName());
        return hashOps.put("user_consents", key, userConsent).then();
    }

    public Mono<Void> deleteByRegisteredClientIdAndPrincipalName(String registeredClientId, String principalName) {
        String key = buildKey(registeredClientId, principalName);
        return hashOps.remove("user_consents", key).then();
    }

    private String buildKey(String registeredClientId, String principalName) {
        return registeredClientId + ":" + principalName;
    }

}
