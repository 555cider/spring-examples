package com.example.auth.domain.mixin;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class OAuth2AuthorizationResponseTypeMixin {

    @JsonCreator
    public OAuth2AuthorizationResponseTypeMixin(@JsonProperty("value") String value) {
    }

}
