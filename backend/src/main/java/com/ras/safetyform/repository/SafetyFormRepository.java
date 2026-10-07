package com.ras.safetyform.repository;

import com.ras.safetyform.model.SafetyForm;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SafetyFormRepository extends JpaRepository<SafetyForm, Integer> {

    @Override
    @EntityGraph(attributePaths = {"user", "site.checklist"})
    Optional<SafetyForm> findById(Integer id);

    @EntityGraph(attributePaths = {"user", "site.checklist"})
    List<SafetyForm> findByUser_IdOrderByFormDateDescSubmittedAtDesc(Integer userId);

    @EntityGraph(attributePaths = {"user", "site.checklist"})
    @Query("""
            SELECT form
            FROM SafetyForm form
            WHERE (:siteId IS NULL OR form.site.id = :siteId)
              AND (:userId IS NULL OR form.user.id = :userId)
            ORDER BY form.formDate DESC, form.submittedAt DESC
            """)
    List<SafetyForm> findAllFiltered(
            @Param("siteId") Integer siteId,
            @Param("userId") Integer userId);

    @EntityGraph(attributePaths = {"user", "site.checklist"})
    @Query("""
            SELECT form
            FROM SafetyForm form
            WHERE (:siteId IS NULL OR form.site.id = :siteId)
              AND (:userId IS NULL OR form.user.id = :userId)
              AND form.formDate >= :startDate
            ORDER BY form.formDate DESC, form.submittedAt DESC
            """)
    List<SafetyForm> findAllFilteredFromDate(
            @Param("siteId") Integer siteId,
            @Param("userId") Integer userId,
            @Param("startDate") Instant startDate);

    @EntityGraph(attributePaths = {"user", "site.checklist"})
    @Query("""
            SELECT form
            FROM SafetyForm form
            WHERE (:siteId IS NULL OR form.site.id = :siteId)
              AND (:userId IS NULL OR form.user.id = :userId)
              AND form.formDate < :endDateExclusive
            ORDER BY form.formDate DESC, form.submittedAt DESC
            """)
    List<SafetyForm> findAllFilteredUntilDate(
            @Param("siteId") Integer siteId,
            @Param("userId") Integer userId,
            @Param("endDateExclusive") Instant endDateExclusive);

    @EntityGraph(attributePaths = {"user", "site.checklist"})
    @Query("""
            SELECT form
            FROM SafetyForm form
            WHERE (:siteId IS NULL OR form.site.id = :siteId)
              AND (:userId IS NULL OR form.user.id = :userId)
              AND form.formDate >= :startDate
              AND form.formDate < :endDateExclusive
            ORDER BY form.formDate DESC, form.submittedAt DESC
            """)
    List<SafetyForm> findAllFilteredBetweenDates(
            @Param("siteId") Integer siteId,
            @Param("userId") Integer userId,
            @Param("startDate") Instant startDate,
            @Param("endDateExclusive") Instant endDateExclusive);
}
