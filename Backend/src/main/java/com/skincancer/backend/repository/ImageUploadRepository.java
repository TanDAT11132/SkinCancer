package com.skincancer.backend.repository;

import com.skincancer.backend.entity.ImageUpload;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ImageUploadRepository extends JpaRepository<ImageUpload, UUID> {
    Page<ImageUpload> findByUserUserIdOrderByUploadedAtDesc(UUID userId, Pageable pageable);
}
