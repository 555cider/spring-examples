package com.example.auth.service;

import com.example.auth.repository.UserRepository;
import com.example.auth.repository.UserAuthorityRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public class JdbcUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserAuthorityRepository userAuthorityRepository;

    public JdbcUserDetailsService(UserRepository userRepository, UserAuthorityRepository userAuthorityRepository) {
        this.userRepository = userRepository;
        this.userAuthorityRepository = userAuthorityRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .map(user -> User.withUsername(user.getUsername())
                        .password(user.getPassword())
                        .authorities(userAuthorityRepository.findAuthoritiesByUsername(username).toArray(String[]::new))
                        .accountExpired(!user.isAccountNonExpired())
                        .accountLocked(!user.isAccountNonLocked())
                        .credentialsExpired(!user.isCredentialsNonExpired())
                        .disabled(!user.isEnabled())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
    }
}
