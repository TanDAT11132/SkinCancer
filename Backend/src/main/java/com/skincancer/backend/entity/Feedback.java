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
@Table(name = "Feedback", schema = "dbo")
public class Feedback {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "FeedbackId", nullable = false)
    private UUID feedbackId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "PredictionId", nullable = false)
    private Prediction prediction;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "UserId", nullable = false)
    private UserEntity user;

    @Column(name = "IsCorrect")
    private Boolean isCorrect;

    @Column(name = "UserLabel", length = 100)
    private String userLabel;

    @Column(name = "Comment", length = 1000)
    private String comment;

    @Column(name = "AllowForRetrain", nullable = false)
    private boolean allowForRetrain;

    @Column(name = "CreatedAt", nullable = false)
    private LocalDateTime createdAt;
}
