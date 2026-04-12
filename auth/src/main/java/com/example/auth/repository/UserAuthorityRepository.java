package com.example.auth.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UserAuthorityRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserAuthorityRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<String> findAuthoritiesByUsername(String username) {
        return jdbcTemplate.queryForList("""
                select ua.authority
                from my_schema.user_authorities ua
                join my_schema.users u on u.id = ua.user_id
                where u.username = ?
                order by ua.authority
                """, String.class, username);
    }

    public List<String> findAuthoritiesByUserId(Long userId) {
        return jdbcTemplate.queryForList("""
                select authority
                from my_schema.user_authorities
                where user_id = ?
                order by authority
                """, String.class, userId);
    }

    public void replaceAuthorities(Long userId, Collection<String> authorities) {
        jdbcTemplate.update("delete from my_schema.user_authorities where user_id = ?", userId);

        Collection<String> sourceAuthorities = authorities == null ? List.of() : authorities;
        List<String> distinctAuthorities = new ArrayList<>(new LinkedHashSet<>(sourceAuthorities));
        if (distinctAuthorities.isEmpty()) {
            return;
        }

        jdbcTemplate.batchUpdate(
                "insert into my_schema.user_authorities (user_id, authority) values (?, ?)",
                distinctAuthorities,
                distinctAuthorities.size(),
                (ps, authority) -> {
                    ps.setLong(1, userId);
                    ps.setString(2, authority);
                }
        );
    }
}
