package com.ogeedeveloper.backend.repository;

import com.ogeedeveloper.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
//    Find user by username
    Optional<User> findUserByUsername(String username);

//    Find user by email
    Optional<User> findUserByEmail(String email);

//    Find user based on their username and password
    Optional<User> findByUsernameOrEmail(String username, String email);

//    Boolean check to see if user exists by username
    Boolean existsByEmail(String email);

    //    Boolean check to see if user exists by email
    Boolean existsByUsername(String username);

//    Lookup Provider
}
