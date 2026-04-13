package com.example.client.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

@ConfigurationProperties(prefix = "app.client")
public class ClientApplicationProperties implements InitializingBean {

    private final Oidc oidc = new Oidc();

    public Oidc getOidc() {
        return oidc;
    }

    @Override
    public void afterPropertiesSet() {
        Assert.hasText(oidc.issuerUri, "app.client.oidc.issuer-uri must not be blank");
        Assert.hasText(oidc.clientId, "app.client.oidc.client-id must not be blank");
        Assert.hasText(oidc.clientSecret, "app.client.oidc.client-secret must not be blank");
    }

    public static class Oidc {
        private String issuerUri = "";

        private String clientId = "";

        private String clientSecret = "";

        public String getIssuerUri() {
            return issuerUri;
        }

        public void setIssuerUri(String issuerUri) {
            this.issuerUri = issuerUri;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }
    }
}
