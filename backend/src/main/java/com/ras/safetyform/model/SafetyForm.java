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
import java.time.Instant;

@Entity
@Table(name = "safety_forms")
public class SafetyForm {

    public static final String STATUS_SUBMITTED = "submitted";
    public static final String STATUS_REVIEWED = "reviewed";

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
    private Instant formDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    @Column(nullable = false, length = 20)
    private String status;

    protected SafetyForm() {
    }

    public SafetyForm(
            User user,
            Site site,
            Instant formDate,
            String notes,
            Instant submittedAt) {
        this.user = user;
        this.site = site;
        this.formDate = formDate;
        this.notes = notes;
        this.submittedAt = submittedAt;
        this.status = STATUS_SUBMITTED;
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

    public Instant getFormDate() {
        return formDate;
    }

    public String getNotes() {
        return notes;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
