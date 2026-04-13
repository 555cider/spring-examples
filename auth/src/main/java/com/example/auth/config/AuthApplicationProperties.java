package com.example.auth.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

@ConfigurationProperties(prefix = "app")
public class AuthApplicationProperties implements InitializingBean {

    private final Auth auth = new Auth();

    private final Client client = new Client();

    public Auth getAuth() {
        return auth;
    }

    public Client getClient() {
        return client;
    }

    @Override
    public void afterPropertiesSet() {
        Assert.hasText(auth.issuer, "app.auth.issuer must not be blank");
        auth.demoClient.validate();

        if (auth.demoClient.enabled) {
            Assert.hasText(client.redirectUri, "app.client.redirect-uri must not be blank");
        }
    }

    public static class Auth {
        private String issuer;

        private final DemoUsers demoUsers = new DemoUsers();

        private final DemoClient demoClient = new DemoClient();

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }

        public DemoUsers getDemoUsers() {
            return demoUsers;
        }

        public DemoClient getDemoClient() {
            return demoClient;
        }
    }

    public static class DemoUsers {
        private boolean enabled;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class DemoClient {
        private boolean enabled;

        private String clientId = "";

        private String clientSecret = "";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
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

        private void validate() {
            if (!enabled) {
                return;
            }

            Assert.hasText(clientId, "app.auth.demo-client.client-id must not be blank");
            Assert.hasText(clientSecret, "app.auth.demo-client.client-secret must not be blank");
        }
    }

    public static class Client {
        private String redirectUri = "";

        public String getRedirectUri() {
            return redirectUri;
        }

        public void setRedirectUri(String redirectUri) {
            this.redirectUri = redirectUri;
        }
    }
}
