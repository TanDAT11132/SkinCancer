package com.skincancer.backend.repository;

import com.skincancer.backend.entity.Feedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FeedbackRepository extends JpaRepository<Feedback, UUID> {
    Page<Feedback> findByAllowForRetrainTrueOrderByCreatedAtDesc(Pageable pageable);
}
