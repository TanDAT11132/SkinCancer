package com.skincancer.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "ImageUpload", schema = "dbo")
public class ImageUpload {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "ImageId", nullable = false)
    private UUID imageId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "UserId", nullable = false)
    private UserEntity user;

    @Column(name = "FileUri", nullable = false, length = 500)
    private String fileUri;

    @Column(name = "FileHashSha256", columnDefinition = "CHAR(64)")
    private String fileHashSha256;

    @Column(name = "FileSizeBytes")
    private Long fileSizeBytes;

    @Column(name = "UploadedAt", nullable = false)
    private LocalDateTime uploadedAt;
}
