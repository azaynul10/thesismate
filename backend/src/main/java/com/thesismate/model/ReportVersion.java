package com.thesismate.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/** F11: one uploaded PDF of a report. Versions are never changed or deleted; a new upload is a new version. */
@Entity
@Table(name = "report_version")
public class ReportVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id", nullable = false)
    private Report report;

    @Column(name = "version_no", nullable = false)
    private int versionNo;

    @Column(name = "original_name", nullable = false)
    private String originalName;

    @Column(name = "storage_key", nullable = false)
    private String storageKey;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(nullable = false, length = 64)
    private String sha256;

    @Column(nullable = false)
    private boolean late;

    @Column(nullable = false)
    private boolean locked;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private AppUser uploadedBy;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    protected ReportVersion() {
    }

    public ReportVersion(Report report, int versionNo, String originalName, String storageKey,
                         long sizeBytes, String sha256, boolean late, AppUser uploadedBy) {
        this.report = report;
        this.versionNo = versionNo;
        this.originalName = originalName;
        this.storageKey = storageKey;
        this.sizeBytes = sizeBytes;
        this.sha256 = sha256;
        this.late = late;
        this.uploadedBy = uploadedBy;
    }

    @PrePersist
    void onCreate() {
        if (uploadedAt == null) {
            uploadedAt = LocalDateTime.now();
        }
    }

    /** Set when the version is approved (F13). A locked version is final. */
    public void lock() {
        this.locked = true;
    }

    public Long getId() { return id; }
    public Report getReport() { return report; }
    public int getVersionNo() { return versionNo; }
    public String getOriginalName() { return originalName; }
    public String getStorageKey() { return storageKey; }
    public long getSizeBytes() { return sizeBytes; }
    public String getSha256() { return sha256; }
    public boolean isLate() { return late; }
    public boolean isLocked() { return locked; }
    public AppUser getUploadedBy() { return uploadedBy; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
}
