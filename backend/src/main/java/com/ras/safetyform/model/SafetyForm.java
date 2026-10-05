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
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "safety_forms")
public class SafetyForm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @Column(name = "form_date", nullable = false)
    private LocalDate formDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected SafetyForm() {
    }

    public SafetyForm(
            User user,
            Site site,
            LocalDate formDate,
            String notes,
            LocalDateTime submittedAt,
            LocalDateTime updatedAt) {
        this.user = user;
        this.site = site;
        this.formDate = formDate;
        this.notes = notes;
        this.submittedAt = submittedAt;
        this.updatedAt = updatedAt;
    }

    public Integer getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Site getSite() {
        return site;
    }

    public LocalDate getFormDate() {
        return formDate;
    }

    public String getNotes() {
        return notes;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
