package com.ogeedeveloper.backend.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Set;

@Entity
@Data

public class User {
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String First_name;

    private String Last_name;

    private String Email;

    private String Phone_number;

    private String Password;

    private String permanent_address;

    private String live_address;
    private String provider;

//    Role: One to many, one user can have multiple roles
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles;

}