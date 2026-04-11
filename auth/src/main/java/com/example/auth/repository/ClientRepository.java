package com.example.auth.repository;

import com.example.auth.domain.Client;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

public interface ClientRepository extends R2dbcRepository<Client, String> {

    Mono<Client> findByClientId(String clientId);

}
