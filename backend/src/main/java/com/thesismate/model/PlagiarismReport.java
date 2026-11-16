package com.thesismate.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/** F11 gate data: the Turnitin similarity percentage plus the similarity and AI report files of one version. */
@Entity
@Table(name = "plagiarism_report")
public class PlagiarismReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "version_id", nullable = false, unique = true)
    private ReportVersion version;

    @Column(name = "similarity_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal similarityPercent;

    @Column(name = "similarity_original_name", nullable = false)
    private String similarityOriginalName;

    @Column(name = "similarity_storage_key", nullable = false)
    private String similarityStorageKey;

    @Column(name = "ai_original_name", nullable = false)
    private String aiOriginalName;

    @Column(name = "ai_storage_key", nullable = false)
    private String aiStorageKey;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected PlagiarismReport() {
    }

    public PlagiarismReport(ReportVersion version, BigDecimal similarityPercent,
                            String similarityOriginalName, String similarityStorageKey,
                            String aiOriginalName, String aiStorageKey) {
        this.version = version;
        this.similarityPercent = similarityPercent;
        this.similarityOriginalName = similarityOriginalName;
        this.similarityStorageKey = similarityStorageKey;
        this.aiOriginalName = aiOriginalName;
        this.aiStorageKey = aiStorageKey;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public ReportVersion getVersion() { return version; }
    public BigDecimal getSimilarityPercent() { return similarityPercent; }
    public String getSimilarityOriginalName() { return similarityOriginalName; }
    public String getSimilarityStorageKey() { return similarityStorageKey; }
    public String getAiOriginalName() { return aiOriginalName; }
    public String getAiStorageKey() { return aiStorageKey; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
