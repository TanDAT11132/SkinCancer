package com.skincancer.backend.repository;

import com.skincancer.backend.entity.Prediction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PredictionRepository extends JpaRepository<Prediction, UUID> {
    Page<Prediction> findByImageUserUserIdOrderByRequestedAtDesc(UUID userId, Pageable pageable);
}
