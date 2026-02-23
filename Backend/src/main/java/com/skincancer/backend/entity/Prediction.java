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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "Prediction", schema = "dbo")
public class Prediction {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "PredictionId", nullable = false)
    private UUID predictionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ImageId", nullable = false)
    private ImageUpload image;

    @Column(name = "RequestedAt", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "ClientApp", length = 50)
    private String clientApp;

    @Column(name = "ClientIp", length = 50)
    private String clientIp;

    @Column(name = "ModelName", length = 100)
    private String modelName;

    @Column(name = "ModelVersion", length = 50)
    private String modelVersion;

    @Column(name = "PredictedClass", nullable = false, length = 100)
    private String predictedClass;

    @Column(name = "Probability", nullable = false, precision = 6, scale = 5)
    private BigDecimal probability;

    @Column(name = "TopKJson", columnDefinition = "NVARCHAR(MAX)")
    private String topKJson;

    @Column(name = "RawResponseJson", columnDefinition = "NVARCHAR(MAX)")
    private String rawResponseJson;
}




