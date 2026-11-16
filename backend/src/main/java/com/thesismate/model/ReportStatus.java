package com.thesismate.model;

/** Where a phase report stands. Only SUBMITTED is used in Sprint 1; approval and rejection come with F13. */
public enum ReportStatus {
    DRAFT, SUBMITTED, REVISION_REQUIRED, SUPERVISOR_APPROVED, COMMITTEE_APPROVED, REJECTED
}
