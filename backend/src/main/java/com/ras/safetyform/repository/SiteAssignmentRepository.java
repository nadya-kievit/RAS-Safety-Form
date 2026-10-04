package com.ras.safetyform.repository;

import com.ras.safetyform.model.SiteAssignment;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SiteAssignmentRepository extends JpaRepository<SiteAssignment, Integer> {

    @EntityGraph(attributePaths = "site.checklist")
    List<SiteAssignment> findByUser_IdOrderByAssignmentDateDescIdAsc(Integer userId);

    @EntityGraph(attributePaths = "user")
    List<SiteAssignment>
            findBySite_IdAndAssignmentDateOrderByUser_LastNameAscUser_FirstNameAsc(
                    Integer siteId,
                    LocalDate assignmentDate);
}
