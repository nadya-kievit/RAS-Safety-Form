package com.ras.safetyform.repository;

import com.ras.safetyform.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Integer> {
}
