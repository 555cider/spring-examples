package com.example.gateway.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

@ConfigurationProperties(prefix = "app.gateway")
public class GatewayApplicationProperties implements InitializingBean {

    private final AuthServer authServer = new AuthServer();

    private final RateLimit rateLimit = new RateLimit();

    public AuthServer getAuthServer() {
        return authServer;
    }

    public RateLimit getRateLimit() {
        return rateLimit;
    }

    @Override
    public void afterPropertiesSet() {
        Assert.hasText(authServer.issuerUri, "app.gateway.auth-server.issuer-uri must not be blank");
        rateLimit.validate();
    }

    public static class AuthServer {
        private String issuerUri = "";

        public String getIssuerUri() {
            return issuerUri;
        }

        public void setIssuerUri(String issuerUri) {
            this.issuerUri = issuerUri;
        }
    }

    public static class RateLimit {
        private int replenishRate;

        private int burstCapacity;

        private int requestedTokens;

        public int getReplenishRate() {
            return replenishRate;
        }

        public void setReplenishRate(int replenishRate) {
            this.replenishRate = replenishRate;
        }

        public int getBurstCapacity() {
            return burstCapacity;
        }

        public void setBurstCapacity(int burstCapacity) {
            this.burstCapacity = burstCapacity;
        }

        public int getRequestedTokens() {
            return requestedTokens;
        }

        public void setRequestedTokens(int requestedTokens) {
            this.requestedTokens = requestedTokens;
        }

        private void validate() {
            Assert.isTrue(replenishRate > 0, "app.gateway.rate-limit.replenish-rate must be greater than 0");
            Assert.isTrue(burstCapacity > 0, "app.gateway.rate-limit.burst-capacity must be greater than 0");
            Assert.isTrue(requestedTokens > 0, "app.gateway.rate-limit.requested-tokens must be greater than 0");
        }
    }
}
