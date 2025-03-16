package com.ogeedeveloper.backend.repository;

import com.ogeedeveloper.backend.model.Role;
import com.ogeedeveloper.backend.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(UserRole name);
}