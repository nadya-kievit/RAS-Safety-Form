package com.ras.safetyform.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "safety_checklist_items")
public class SafetyChecklistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "safety_checklist_id", nullable = false)
    private SafetyChecklist checklist;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String item;

    protected SafetyChecklistItem() {
    }

    public SafetyChecklistItem(SafetyChecklist checklist, String item) {
        this.checklist = checklist;
        this.item = item;
    }

    public Integer getId() {
        return id;
    }

    public SafetyChecklist getChecklist() {
        return checklist;
    }

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }
}
