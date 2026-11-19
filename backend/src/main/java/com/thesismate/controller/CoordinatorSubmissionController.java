package com.thesismate.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.thesismate.dto.DeliverableRequirementRequest;
import com.thesismate.dto.DeliverableRequirementView;
import com.thesismate.dto.ExtensionRequest;
import com.thesismate.dto.ExtensionView;
import com.thesismate.dto.SubmissionWindowRequest;
import com.thesismate.dto.SubmissionWindowView;
import com.thesismate.service.SubmissionWindowService;

import jakarta.validation.Valid;

/** F15: coordinator-only. SecurityConfig already limits /api/coordinator/** to the COORDINATOR role. */
@RestController
@RequestMapping("/api/coordinator")
public class CoordinatorSubmissionController {

    private final SubmissionWindowService service;

    public CoordinatorSubmissionController(SubmissionWindowService service) {
        this.service = service;
    }

    // ---------- submission windows ----------

    @PostMapping("/submission-windows")
    @ResponseStatus(HttpStatus.CREATED)
    public SubmissionWindowView createWindow(@Valid @RequestBody SubmissionWindowRequest req, Authentication auth) {
        return service.createWindow(auth.getName(), req);
    }

    @PutMapping("/submission-windows/{id}")
    public SubmissionWindowView updateWindow(@PathVariable Long id, @Valid @RequestBody SubmissionWindowRequest req) {
        return service.updateWindow(id, req);
    }

    @GetMapping("/submission-windows")
    public List<SubmissionWindowView> listWindows() {
        return service.listWindows();
    }

    // ---------- deliverable checklist ----------

    @PostMapping("/deliverable-requirements")
    @ResponseStatus(HttpStatus.CREATED)
    public DeliverableRequirementView addRequirement(@Valid @RequestBody DeliverableRequirementRequest req,
                                                     Authentication auth) {
        return service.addRequirement(auth.getName(), req);
    }

    @GetMapping("/deliverable-requirements")
    public List<DeliverableRequirementView> listRequirements() {
        return service.listRequirements();
    }

    @DeleteMapping("/deliverable-requirements/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRequirement(@PathVariable Long id) {
        service.deleteRequirement(id);
    }

    // ---------- extensions ----------

    @PostMapping("/extensions")
    public ExtensionView grantExtension(@Valid @RequestBody ExtensionRequest req, Authentication auth) {
        return service.grantExtension(auth.getName(), req);
    }

    @GetMapping("/extensions")
    public List<ExtensionView> listExtensions(@RequestParam Long windowId) {
        return service.listExtensions(windowId);
    }
}
