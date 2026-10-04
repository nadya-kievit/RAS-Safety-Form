package com.ras.safetyform.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "safety_checklists")
public class SafetyChecklist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 150)
    private String name;

    protected SafetyChecklist() {
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
