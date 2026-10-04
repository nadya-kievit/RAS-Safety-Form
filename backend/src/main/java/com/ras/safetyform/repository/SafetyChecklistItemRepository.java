package com.ras.safetyform.repository;

import com.ras.safetyform.model.SafetyChecklistItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SafetyChecklistItemRepository
        extends JpaRepository<SafetyChecklistItem, Integer> {

    List<SafetyChecklistItem> findByChecklist_IdOrderByIdAsc(Integer checklistId);

    List<SafetyChecklistItem> findByIdIn(List<Integer> ids);
}
