package com.ogeedeveloper.backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SeverityType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String name;
    private String description;


    // Helper method to get common severity types
    public static SeverityType informational() {
        return new SeverityType(0, "Informational", "General notifications or updates; no immediate action required.");
    }

    public static SeverityType advisory() {
        return new SeverityType(1, "Advisory", "Indicates a potential risk or situation that requires caution and monitoring.");
    }

    public static SeverityType warning() {
        return new SeverityType(3, "Warning", "A clear threat is present; immediate attention and preparatory actions are recommended.");
    }

    public static SeverityType critical() {
        return new SeverityType(4, "Critical", "A severe emergency that poses an imminent risk to life or property; urgent response needed.");
    }

    public static SeverityType catastrophic() {
        return new SeverityType(5, "Catastrophic", "Extreme, life‐threatening conditions causing widespread impact; full-scale emergency response required.");
    }
}
