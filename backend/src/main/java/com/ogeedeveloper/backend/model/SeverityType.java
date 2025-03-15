package com.ogeedeveloper.backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Data
@Entity
public class SeverityType {
    @Id
    private int id;
    private String name;
    private String description;
}
