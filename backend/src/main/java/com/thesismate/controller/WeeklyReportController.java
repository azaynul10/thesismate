package com.thesismate.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.thesismate.dto.GroupProgressView;
import com.thesismate.dto.WeeklyOverviewView;
import com.thesismate.dto.WeeklyReportRequest;
import com.thesismate.dto.WeeklyReportView;
import com.thesismate.dto.WeeklyReviewRequest;
import com.thesismate.repository.AppUserRepository;
import com.thesismate.service.SupervisorProgressService;
import com.thesismate.service.WeeklyReportService;

import jakarta.validation.Valid;

/**
 * F17: weekly progress reports and supervisor sign-off, plus the supervisor progress page (F16 + F17).
 * SecurityConfig limits /api/supervisor/** to supervisors and /api/coordinator/** to the coordinator.
 */
@RestController
@RequestMapping("/api")
public class WeeklyReportController {

    private final WeeklyReportService service;
    private final SupervisorProgressService progress;
    private final AppUserRepository users;

    public WeeklyReportController(WeeklyReportService service, SupervisorProgressService progress,
                                  AppUserRepository users) {
        this.service = service;
        this.progress = progress;
        this.users = users;
    }

    // ---- group side ----

    @GetMapping("/groups/{groupId}/weekly-reports")
    public WeeklyOverviewView overview(@PathVariable Long groupId, Authentication auth) {
        return service.overview(currentUserId(auth), groupId);
    }

    /** Hands in a new report for the week that starts on {@code weekStart} (a Monday, yyyy-MM-dd). */
    @PostMapping("/groups/{groupId}/weekly-reports/{weekStart}")
    @ResponseStatus(HttpStatus.CREATED)
    public WeeklyReportView submit(@PathVariable Long groupId,
                                   @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
                                   @Valid @RequestBody WeeklyReportRequest request, Authentication auth) {
        return service.submit(currentUserId(auth), groupId, weekStart, request);
    }

    // ---- supervisor side ----

    @GetMapping("/supervisor/weekly-reports")
    public List<WeeklyReportView> pending(Authentication auth) {
        return service.pendingForSupervisor(currentUserId(auth));
    }

    @PostMapping("/supervisor/weekly-reports/{reportId}/approve")
    public WeeklyReportView approve(@PathVariable Long reportId,
                                    @Valid @RequestBody(required = false) WeeklyReviewRequest request,
                                    Authentication auth) {
        return service.approve(currentUserId(auth), reportId, request == null ? null : request.comment());
    }

    @PostMapping("/supervisor/weekly-reports/{reportId}/return")
    public WeeklyReportView returnReport(@PathVariable Long reportId,
                                         @Valid @RequestBody WeeklyReviewRequest request, Authentication auth) {
        return service.returnReport(currentUserId(auth), reportId, request.comment());
    }

    @GetMapping("/supervisor/progress")
    public List<GroupProgressView> supervisorProgress(Authentication auth) {
        return progress.overview(currentUserId(auth));
    }

    // ---- coordinator ----

    /** Runs the missing-report reminders now (the same job that runs every morning). */
    @PostMapping("/coordinator/weekly-reports/reminders")
    public Map<String, Integer> runReminders() {
        return Map.of("remindersCreated", service.sendReminders());
    }

    private Long currentUserId(Authentication auth) {
        return users.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not logged in"))
                .getId();
    }
}
