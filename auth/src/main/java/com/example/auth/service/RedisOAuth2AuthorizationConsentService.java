package com.example.auth.service;

import com.example.auth.domain.OAuth2UserConsent;
import com.example.auth.repository.redis.OAuth2UserConsentRepository;
import com.example.auth.util.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
@Profile("legacy-reactive-auth")
public class RedisOAuth2AuthorizationConsentService implements OAuth2AuthorizationConsentService {

    private static final Logger logger = LoggerFactory.getLogger(RedisOAuth2AuthorizationConsentService.class);
    private static final String CONSENT_PREFIX = "oauth2_consent:";

    private final OAuth2UserConsentRepository consentRepository;

    public RedisOAuth2AuthorizationConsentService(OAuth2UserConsentRepository consentRepository) {
        Assert.notNull(consentRepository, "OAuth2UserConsentRepository cannot be null");
        this.consentRepository = consentRepository;
    }

    @Override
    public void save(OAuth2AuthorizationConsent oAuth2AuthorizationConsent) {
        logger.info("〓〓〓 Saving consent for registered client ID: {}", oAuth2AuthorizationConsent.getRegisteredClientId());
        Assert.notNull(oAuth2AuthorizationConsent, "OAuth2AuthorizationConsent cannot be null");
        OAuth2UserConsent oAuth2UserConsent = ModelMapper.convertOAuth2UserConsent(oAuth2AuthorizationConsent);
        consentRepository
                .save(oAuth2UserConsent)
                .subscribe();
    }

    @Override
    public void remove(OAuth2AuthorizationConsent oAuth2AuthorizationConsent) {
        logger.info("〓〓〓 Removing consent for registered client ID: {}", oAuth2AuthorizationConsent.getRegisteredClientId());
        Assert.notNull(oAuth2AuthorizationConsent, "authorizationConsent cannot be null");

        consentRepository
                .deleteByRegisteredClientIdAndPrincipalName(oAuth2AuthorizationConsent.getRegisteredClientId(), oAuth2AuthorizationConsent.getPrincipalName())
                .subscribe();
    }

    @Override
    public OAuth2AuthorizationConsent findById(String registeredClientId, String principalName) {
        logger.info("〓〓〓 Finding consent for registered client ID: {}", registeredClientId);
        Assert.hasText(registeredClientId, "registeredClientId cannot be empty");
        Assert.hasText(principalName, "principalName cannot be empty");

        return consentRepository
                .findByRegisteredClientIdAndPrincipalName(registeredClientId, principalName)
                .mapNotNull(ModelMapper::convertOAuth2AuthorizationConsent)
                .block();
    }

}
