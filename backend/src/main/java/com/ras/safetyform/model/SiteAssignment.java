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
import java.time.LocalDate;

@Entity
@Table(
        name = "site_assignments",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_site_assignment",
                columnNames = {"user_id", "site_id", "assignment_date"}))
public class SiteAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @Column(name = "assignment_date", nullable = false)
    private LocalDate assignmentDate;

    protected SiteAssignment() {
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

    public LocalDate getAssignmentDate() {
        return assignmentDate;
    }
}
