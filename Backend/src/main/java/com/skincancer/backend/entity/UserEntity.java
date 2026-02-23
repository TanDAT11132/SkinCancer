package com.skincancer.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "User", schema = "dbo")
public class UserEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "UserId", nullable = false)
    private UUID userId;

    @Column(name = "GoogleSub", nullable = false, length = 100, unique = true)
    private String googleSub;

    @Column(name = "Email", nullable = false, length = 256)
    private String email;

    @Column(name = "FullName", length = 150)
    private String fullName;

    @Column(name = "Gender", length = 10)
    private String gender;

    @Column(name = "Age")
    private Integer age;

    @Enumerated(EnumType.STRING)
    @Column(name = "Role", nullable = false, length = 10)
    private Role role = Role.USER;

    @Column(name = "CreatedAt", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "UpdatedAt")
    private LocalDateTime updatedAt;
}




