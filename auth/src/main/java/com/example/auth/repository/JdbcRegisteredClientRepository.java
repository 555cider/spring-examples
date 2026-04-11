package com.example.auth.repository;

import com.example.auth.domain.Client;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Repository;
import org.springframework.util.Assert;

@Repository
@Profile("legacy-reactive-auth")
public class JdbcRegisteredClientRepository implements RegisteredClientRepository {

    private final ClientRepository clientRepository;

    public JdbcRegisteredClientRepository(ClientRepository clientRepository) {
        Assert.notNull(clientRepository, "clientRepository cannot be null");
        this.clientRepository = clientRepository;
    }

    @Override
    public void save(RegisteredClient registeredClient) {
        clientRepository.save(new Client(registeredClient));
    }

    @Override
    public RegisteredClient findById(String id) {
        return clientRepository.findById(id)
                .mapNotNull(Client::toRegisteredClient)
                .blockOptional()
                .orElse(null);
    }

    @Override
    public RegisteredClient findByClientId(String clientId) {
        return clientRepository.findByClientId(clientId)
                .mapNotNull(Client::toRegisteredClient)
                .blockOptional()
                .orElse(null);
    }

}
