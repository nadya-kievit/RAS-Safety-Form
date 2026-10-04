package com.ras.safetyform.repository;

import com.ras.safetyform.model.Photo;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PhotoRepository extends JpaRepository<Photo, Integer> {

    List<Photo> findBySafetyForm_IdOrderByCreatedAtAscIdAsc(Integer formId);
}
