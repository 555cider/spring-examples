package com.example.auth.util;

import com.example.auth.domain.OAuth2AuthorizationCodeGrantAuthorization;
import com.example.auth.domain.mixin.OAuth2AuthorizationCodeGrantAuthorizationMixin;
import com.example.auth.domain.mixin.OAuth2AuthorizationGrantAuthorizationMixin;
import com.example.auth.domain.mixin.OAuth2AuthorizationResponseTypeMixin;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponseType;

import static com.example.auth.domain.OAuth2AuthorizationGrantAuthorization.AccessToken;

public class Serializer {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.registerModules(SecurityJackson2Modules.getModules(Serializer.class.getClassLoader()));
        objectMapper.addMixIn(OAuth2AuthorizationResponseType.class, OAuth2AuthorizationResponseTypeMixin.class);
        objectMapper.addMixIn(OAuth2AuthorizationCodeGrantAuthorization.class, OAuth2AuthorizationCodeGrantAuthorizationMixin.class);
        objectMapper.addMixIn(AccessToken.class, OAuth2AuthorizationGrantAuthorizationMixin.class);

        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public static String serialize(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (
                JsonProcessingException e) {
            throw new RuntimeException("Serialization error: " + e.getMessage(), e);
        }
    }

    public static <T> T deserialize(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Deserialization error: " + e.getMessage(), e);
        }
    }

}
