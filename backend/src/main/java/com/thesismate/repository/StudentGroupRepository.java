package com.thesismate.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.thesismate.model.StudentGroup;

import jakarta.persistence.LockModeType;

public interface StudentGroupRepository extends JpaRepository<StudentGroup, Long> {

    /** Locks the group row until the transaction ends, so two accepts cannot both take the last seat. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from StudentGroup g where g.id = :id")
    Optional<StudentGroup> findByIdForUpdate(@Param("id") Long id);
}