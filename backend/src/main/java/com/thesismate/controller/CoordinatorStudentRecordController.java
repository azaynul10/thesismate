package com.thesismate.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.thesismate.dto.ImportResult;
import com.thesismate.dto.StudentRecordResponse;
import com.thesismate.repository.AppUserRepository;
import com.thesismate.service.StudentRecordImportService;

/** F6: coordinator-only. SecurityConfig already limits /api/coordinator/** to the COORDINATOR role. */
@RestController
@RequestMapping("/api/coordinator/student-records")
public class CoordinatorStudentRecordController {

    private final StudentRecordImportService importService;
    private final AppUserRepository users;

    public CoordinatorStudentRecordController(StudentRecordImportService importService, AppUserRepository users) {
        this.importService = importService;
        this.users = users;
    }

    @PostMapping(value = "/import", consumes = "text/csv")
    public ImportResult importCsv(@RequestBody String csv, Authentication auth) {
        Long coordinatorId = users.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not logged in"))
                .getId();
        return importService.importCsv(coordinatorId, csv);
    }

    @GetMapping
    public List<StudentRecordResponse> list() {
        return importService.list();
    }
}