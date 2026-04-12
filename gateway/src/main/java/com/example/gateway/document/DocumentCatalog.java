package com.example.gateway.document;

import java.util.Map;

import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

@Component
public class DocumentCatalog {

    private final Map<String, DocumentRecord> documents = Map.of(
            "doc-user", new DocumentRecord("doc-user", "User Document", "user"),
            "doc-admin", new DocumentRecord("doc-admin", "Admin Document", "admin")
    );

    public Mono<DocumentRecord> findById(String id) {
        return Mono.justOrEmpty(documents.get(id));
    }
}
