package com.example.gateway.document;

public record DocumentRecord(
        String id,
        String title,
        String ownerUsername
) {
}
