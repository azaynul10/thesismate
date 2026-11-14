package com.thesismate.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.thesismate.dto.ImportResult;
import com.thesismate.dto.StudentRecordResponse;
import com.thesismate.model.StudentRecord;
import com.thesismate.repository.StudentRecordRepository;

/** F6: coordinator imports credits and CGPA from a CSV. Valid rows are saved, bad rows are reported. */
@Service
public class StudentRecordImportService {

    static final String HEADER = "student_id,credits_completed,cgpa";
    private static final BigDecimal MAX_CGPA = new BigDecimal("4");

    private final StudentRecordRepository records;

    public StudentRecordImportService(StudentRecordRepository records) {
        this.records = records;
    }

    @Transactional
    public ImportResult importCsv(Long coordinatorId, String csv) {
        if (csv == null || csv.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The file is empty");
        }
        if (csv.startsWith("\uFEFF")) {
            csv = csv.substring(1);
        }
        String[] lines = csv.split("\\r?\\n");
        if (!lines[0].trim().toLowerCase().replace(" ", "").equals(HEADER)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The first line must be: " + HEADER);
        }

        int created = 0;
        int updated = 0;
        List<String> errors = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            if (line.isBlank()) {
                continue;
            }
            String where = "Line " + (i + 1) + ": ";
            String[] parts = line.split(",", -1);
            if (parts.length != 3) {
                errors.add(where + "expected 3 columns");
                continue;
            }
            String studentId = parts[0].trim();
            if (!studentId.matches("\\d{8}")) {
                errors.add(where + "student ID must be 8 digits");
                continue;
            }
            int credits;
            try {
                credits = Integer.parseInt(parts[1].trim());
            } catch (NumberFormatException e) {
                errors.add(where + "credits must be a whole number");
                continue;
            }
            if (credits < 0) {
                errors.add(where + "credits cannot be negative");
                continue;
            }
            BigDecimal cgpa;
            try {
                cgpa = new BigDecimal(parts[2].trim());
            } catch (NumberFormatException e) {
                errors.add(where + "CGPA must be a number");
                continue;
            }
            if (cgpa.scale() > 2) {
                errors.add(where + "CGPA can have at most 2 decimal places");
                continue;
            }
            if (cgpa.signum() < 0 || cgpa.compareTo(MAX_CGPA) > 0) {
                errors.add(where + "CGPA must be between 0 and 4");
                continue;
            }
            if (!seen.add(studentId)) {
                errors.add(where + "student ID " + studentId + " appears twice in this file");
                continue;
            }

            cgpa = cgpa.setScale(2);
            Optional<StudentRecord> existing = records.findByStudentId(studentId);
            if (existing.isPresent()) {
                existing.get().update(credits, cgpa, coordinatorId);
                records.save(existing.get());
                updated++;
            } else {
                records.save(new StudentRecord(studentId, credits, cgpa, coordinatorId));
                created++;
            }
        }
        return new ImportResult(created, updated, errors);
    }

    @Transactional(readOnly = true)
    public List<StudentRecordResponse> list() {
        return records.findAll(Sort.by("studentId")).stream()
                .map(r -> new StudentRecordResponse(
                        r.getStudentId(), r.getCreditsCompleted(), r.getCgpa(), r.getImportedAt()))
                .toList();
    }
}