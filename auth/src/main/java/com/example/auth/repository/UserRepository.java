package com.example.auth.repository;

import java.util.Optional;

import com.example.auth.domain.User;
import org.springframework.data.repository.CrudRepository;

public interface UserRepository extends CrudRepository<User, Long> {

    Optional<User> findByUsername(String username);

}
