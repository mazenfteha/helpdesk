package com.mazen.helpdesk.repository;

import com.mazen.helpdesk.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    //Derived queries
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    
} 
    