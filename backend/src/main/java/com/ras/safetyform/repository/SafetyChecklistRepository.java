package com.ras.safetyform.repository;

import com.ras.safetyform.model.SafetyChecklist;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SafetyChecklistRepository extends JpaRepository<SafetyChecklist, Integer> {
}
