package com.thesismate.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/** Credits and CGPA for one student, imported by the coordinator. */
@Entity
@Table(name = "student_record")
public class StudentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false, unique = true, length = 20)
    private String studentId;

    @Column(name = "credits_completed", nullable = false)
    private int creditsCompleted;

    @Column(nullable = false, precision = 3, scale = 2)
    private BigDecimal cgpa;

    @Column(name = "imported_by", nullable = false)
    private Long importedBy;

    @Column(name = "imported_at", nullable = false)
    private LocalDateTime importedAt;

    protected StudentRecord() {
    }

    public StudentRecord(String studentId, int creditsCompleted, BigDecimal cgpa, Long importedBy) {
        this.studentId = studentId;
        this.creditsCompleted = creditsCompleted;
        this.cgpa = cgpa;
        this.importedBy = importedBy;
    }

    @PrePersist
    void onCreate() {
        if (importedAt == null) {
            importedAt = LocalDateTime.now();
        }
    }

    public void update(int creditsCompleted, BigDecimal cgpa, Long importedBy) {
        this.creditsCompleted = creditsCompleted;
        this.cgpa = cgpa;
        this.importedBy = importedBy;
        this.importedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getStudentId() { return studentId; }
    public int getCreditsCompleted() { return creditsCompleted; }
    public BigDecimal getCgpa() { return cgpa; }
    public Long getImportedBy() { return importedBy; }
    public LocalDateTime getImportedAt() { return importedAt; }
}