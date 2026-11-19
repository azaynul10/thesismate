package com.thesismate.model;

/**
 * F16: where a task came from. MANUAL tasks are typed by students.
 * The other values are for tasks that later features create through MilestoneService.addGeneratedTask:
 * ACCEPTANCE_TEST (failed test cases at a project demo) and MEETING_ACTION (follow-up actions from meeting minutes).
 */
public enum TaskSource {
    MANUAL, ACCEPTANCE_TEST, MEETING_ACTION
}
