package com.example.auth.domain.mixin;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public abstract class OAuth2AuthorizationGrantAuthorizationMixin {
}
