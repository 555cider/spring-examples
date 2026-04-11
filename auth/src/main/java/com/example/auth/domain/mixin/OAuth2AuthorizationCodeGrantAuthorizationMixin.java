package com.example.auth.domain.mixin;

import com.example.auth.domain.OAuth2AuthorizationCodeGrantAuthorization;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

public abstract class OAuth2AuthorizationCodeGrantAuthorizationMixin {

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = OAuth2AuthorizationCodeGrantAuthorization.AuthorizationCode.class, name = "AuthorizationCode")
    })
    public abstract OAuth2AuthorizationCodeGrantAuthorization.AuthorizationCode getAuthorizationCode();

}
