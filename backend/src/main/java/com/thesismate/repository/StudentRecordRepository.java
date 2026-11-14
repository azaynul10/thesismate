package com.thesismate.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.thesismate.model.StudentRecord;

public interface StudentRecordRepository extends JpaRepository<StudentRecord, Long> {
    Optional<StudentRecord> findByStudentId(String studentId);
}