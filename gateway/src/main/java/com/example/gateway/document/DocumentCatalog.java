package com.example.gateway.document;

import java.util.Map;

import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

@Component
public class DocumentCatalog {

    private final Map<String, DocumentRecord> documents = Map.of(
            "doc-user-private", new DocumentRecord(
                    "doc-user-private",
                    "User Private Document",
                    "user",
                    "tenant-alpha",
                    DocumentSharingPolicy.OWNER_ONLY
            ),
            "doc-tenant-shared", new DocumentRecord(
                    "doc-tenant-shared",
                    "Tenant Shared Document",
                    "teammate",
                    "tenant-alpha",
                    DocumentSharingPolicy.TENANT
            ),
            "doc-outsider-private", new DocumentRecord(
                    "doc-outsider-private",
                    "Outsider Private Document",
                    "outsider",
                    "tenant-bravo",
                    DocumentSharingPolicy.OWNER_ONLY
            )
    );

    public Mono<DocumentRecord> findById(String id) {
        return Mono.justOrEmpty(documents.get(id));
    }
}
