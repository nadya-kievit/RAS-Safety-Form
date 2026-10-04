package com.ras.safetyform.repository;

import com.ras.safetyform.model.Site;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SiteRepository extends JpaRepository<Site, Integer> {

    @EntityGraph(attributePaths = "checklist")
    List<Site> findByActiveTrueOrderByNameAsc();

    @EntityGraph(attributePaths = "checklist")
    Optional<Site> findOneById(Integer id);
}
