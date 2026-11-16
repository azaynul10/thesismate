package com.thesismate.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.thesismate.dto.ReportView;
import com.thesismate.model.Phase;
import com.thesismate.repository.AppUserRepository;
import com.thesismate.service.ReportService;
import com.thesismate.service.ReportService.DownloadFile;
import com.thesismate.service.ReportService.FileKind;

/** F11: report upload, version history and file download. The rules live in ReportService. */
@RestController
@RequestMapping("/api")
public class ReportController {

    private final ReportService service;
    private final AppUserRepository users;

    public ReportController(ReportService service, AppUserRepository users) {
        this.service = service;
        this.users = users;
    }

    /** Multipart form: phase, similarityPercent and the three PDF files (report, similarityReport, aiReport). */
    @PostMapping(path = "/groups/{groupId}/reports", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ReportView submit(@PathVariable Long groupId,
                             @RequestParam(required = false) Phase phase,
                             @RequestParam(required = false) BigDecimal similarityPercent,
                             @RequestParam(name = "report", required = false) MultipartFile report,
                             @RequestParam(name = "similarityReport", required = false) MultipartFile similarityReport,
                             @RequestParam(name = "aiReport", required = false) MultipartFile aiReport,
                             Authentication auth) {
        return service.submit(currentUserId(auth), groupId, phase, similarityPercent,
                report, similarityReport, aiReport);
    }

    @GetMapping("/groups/{groupId}/reports")
    public List<ReportView> history(@PathVariable Long groupId, Authentication auth) {
        return service.history(currentUserId(auth), groupId);
    }

    /** kind = REPORT (default), SIMILARITY or AI. */
    @GetMapping("/report-versions/{versionId}/file")
    public ResponseEntity<Resource> download(@PathVariable Long versionId,
                                             @RequestParam(defaultValue = "REPORT") FileKind kind,
                                             Authentication auth) {
        DownloadFile file = service.download(currentUserId(auth), versionId, kind);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(file.fileName()).build().toString())
                .body(file.resource());
    }

    private Long currentUserId(Authentication auth) {
        return users.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not logged in"))
                .getId();
    }
}
