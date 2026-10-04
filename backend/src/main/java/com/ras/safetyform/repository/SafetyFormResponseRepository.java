package com.ras.safetyform.repository;

import com.ras.safetyform.model.SafetyFormResponse;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SafetyFormResponseRepository
        extends JpaRepository<SafetyFormResponse, Integer> {

    @EntityGraph(attributePaths = "checklistItem")
    List<SafetyFormResponse> findBySafetyForm_IdOrderByChecklistItem_IdAsc(Integer formId);
}
