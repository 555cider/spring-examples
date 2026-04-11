package com.example.auth.service;

import com.example.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@Profile("legacy-reactive-auth")
public class JdbcUserDetailsService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(JdbcUserDetailsService.class);

    private final UserRepository userDetailsRepository;

    public JdbcUserDetailsService(UserRepository userDetailsRepository) {
        this.userDetailsRepository = userDetailsRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.info("〓〓〓 Retrieving user: {}", username);
        return userDetailsRepository.findOneByUsername(username)
                .blockOptional()
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
    }

}
