package com.example.auth.repository;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import com.example.auth.domain.User;
import org.springframework.data.relational.core.mapping.event.BeforeConvertCallback;
import org.springframework.stereotype.Component;

@Component
public class UserPersistenceCallbacks implements BeforeConvertCallback<User> {

    @Override
    public User onBeforeConvert(User user) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime createdAt = user.getCreatedAt() == null ? now : user.getCreatedAt();

        return new User(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                user.getEmail(),
                user.getTenantId(),
                createdAt,
                now
        );
    }
}
