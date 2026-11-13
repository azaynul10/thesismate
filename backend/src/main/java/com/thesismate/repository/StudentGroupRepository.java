package com.thesismate.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.thesismate.model.StudentGroup;

public interface StudentGroupRepository extends JpaRepository<StudentGroup, Long> {
}