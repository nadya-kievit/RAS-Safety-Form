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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "safety_form_responses",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_form_item_response",
                columnNames = {"safety_form_id", "checklist_item_id"}))
public class SafetyFormResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "safety_form_id", nullable = false)
    private SafetyForm safetyForm;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "checklist_item_id", nullable = false)
    private SafetyChecklistItem checklistItem;

    @Column(nullable = false)
    private boolean response;

    protected SafetyFormResponse() {
    }

    public SafetyFormResponse(
            SafetyForm safetyForm,
            SafetyChecklistItem checklistItem,
            boolean response) {
        this.safetyForm = safetyForm;
        this.checklistItem = checklistItem;
        this.response = response;
    }

    public Integer getId() {
        return id;
    }

    public SafetyForm getSafetyForm() {
        return safetyForm;
    }

    public SafetyChecklistItem getChecklistItem() {
        return checklistItem;
    }

    public boolean isResponse() {
        return response;
    }

    public void setResponse(boolean response) {
        this.response = response;
    }
}
