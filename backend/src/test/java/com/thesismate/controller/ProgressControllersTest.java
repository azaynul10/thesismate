package com.thesismate.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import com.thesismate.config.SecurityConfig;
import com.thesismate.dto.MilestonePlanView;
import com.thesismate.dto.ProgressSummary;
import com.thesismate.dto.WeeklyOverviewView;
import com.thesismate.model.AppUser;
import com.thesismate.model.Role;
import com.thesismate.model.Track;
import com.thesismate.repository.AppUserRepository;
import com.thesismate.service.AppUserDetailsService;
import com.thesismate.service.AuthService;
import com.thesismate.service.MilestoneService;
import com.thesismate.service.SupervisorProgressService;
import com.thesismate.service.WeeklyReportService;

@WebMvcTest(controllers = {MilestoneController.class, WeeklyReportController.class})
@Import({SecurityConfig.class, AuthService.class, AppUserDetailsService.class, GlobalExceptionHandler.class})
class ProgressControllersTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private MilestoneService milestones;

    @MockitoBean
    private WeeklyReportService weekly;

    @MockitoBean
    private SupervisorProgressService progress;

    @MockitoBean
    private AppUserRepository users;

    @BeforeEach
    void setUp() {
        when(users.findByEmail("student@t.test")).thenReturn(Optional.of(person(5L, Role.STUDENT)));
        when(users.findByEmail("sup@t.test")).thenReturn(Optional.of(person(20L, Role.SUPERVISOR)));
    }

    private AppUser person(long id, Role role) {
        AppUser u = new AppUser("u" + id + "@t.test", "User " + id, role, "pw", null);
        ReflectionTestUtils.setField(u, "id", id);
        return u;
    }

    private static MilestonePlanView emptyPlan() {
        return new MilestonePlanView(10L, Track.THESIS, "Spring 2027", true, false,
                new ProgressSummary(0, 0, 0, 0, 0, 0, 0), List.of());
    }

    // ---- login and roles ----

    @Test
    void anonymousUsersGet401() throws Exception {
        mvc.perform(get("/api/groups/10/milestones")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/groups/10/weekly-reports")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/supervisor/progress")).andExpect(status().isUnauthorized());
        verifyNoInteractions(milestones, weekly, progress);
    }

    @Test
    @WithMockUser(username = "student@t.test", roles = "STUDENT")
    void studentsCannotUseSupervisorOrCoordinatorRoutes() throws Exception {
        mvc.perform(get("/api/supervisor/weekly-reports")).andExpect(status().isForbidden());
        mvc.perform(get("/api/supervisor/progress")).andExpect(status().isForbidden());
        mvc.perform(post("/api/supervisor/weekly-reports/1/approve").with(csrf())).andExpect(status().isForbidden());
        mvc.perform(post("/api/coordinator/weekly-reports/reminders").with(csrf())).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "sup@t.test", roles = "SUPERVISOR")
    void supervisorsCannotRunTheCoordinatorReminderJob() throws Exception {
        mvc.perform(post("/api/coordinator/weekly-reports/reminders").with(csrf())).andExpect(status().isForbidden());
    }

    // ---- F16 ----

    @Test
    @WithMockUser(username = "student@t.test", roles = "STUDENT")
    void studentReadsThePlanOfTheirGroup() throws Exception {
        when(milestones.plan(5L, 10L)).thenReturn(emptyPlan());

        mvc.perform(get("/api/groups/10/milestones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupId").value(10))
                .andExpect(jsonPath("$.canEdit").value(true))
                .andExpect(jsonPath("$.summary.completionPercent").value(0));
    }

    @Test
    @WithMockUser(username = "student@t.test", roles = "STUDENT")
    void applyingTheTemplateReturns201() throws Exception {
        when(milestones.applyTemplate(5L, 10L)).thenReturn(emptyPlan());

        mvc.perform(post("/api/groups/10/milestones/template").with(csrf()))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "student@t.test", roles = "STUDENT")
    void postWithoutCsrfIsForbidden() throws Exception {
        mvc.perform(post("/api/groups/10/milestones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Survey\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "student@t.test", roles = "STUDENT")
    void blankMilestoneTitleReturns400WithFieldError() throws Exception {
        mvc.perform(post("/api/groups/10/milestones")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.title").value("Title is required"));
    }

    @Test
    @WithMockUser(username = "student@t.test", roles = "STUDENT")
    void taskStatusNeedsAValidValue() throws Exception {
        mvc.perform(patch("/api/tasks/7/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.status").value("Status is required"));
    }

    @Test
    @WithMockUser(username = "student@t.test", roles = "STUDENT")
    void deletingATaskReturns204() throws Exception {
        mvc.perform(delete("/api/tasks/7").with(csrf())).andExpect(status().isNoContent());
        verify(milestones).deleteTask(5L, 7L);
    }

    // ---- F17 ----

    @Test
    @WithMockUser(username = "student@t.test", roles = "STUDENT")
    void weeklyReportIsSubmittedForTheWeekInThePath() throws Exception {
        mvc.perform(post("/api/groups/10/weekly-reports/2026-11-16")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"done\":\"Read papers\",\"next\":\"Write survey\",\"blockers\":\"\"}"))
                .andExpect(status().isCreated());

        verify(weekly).submit(eq(5L), eq(10L), eq(LocalDate.of(2026, 11, 16)), any());
    }

    @Test
    @WithMockUser(username = "student@t.test", roles = "STUDENT")
    void weeklyReportNeedsDoneAndNext() throws Exception {
        mvc.perform(post("/api/groups/10/weekly-reports/2026-11-16")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"done\":\"\",\"next\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.done").value("Describe what was done this week"))
                .andExpect(jsonPath("$.fieldErrors.next").value("Describe what comes next"));
    }

    @Test
    @WithMockUser(username = "student@t.test", roles = "STUDENT")
    void studentReadsTheWeeklyOverview() throws Exception {
        when(weekly.overview(5L, 10L)).thenReturn(new WeeklyOverviewView(10L, Track.THESIS, true, true, null,
                LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 16), false,
                List.of(LocalDate.of(2026, 11, 9)), 0, List.of()));

        mvc.perform(get("/api/groups/10/weekly-reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canSubmit").value(true))
                .andExpect(jsonPath("$.missingWeeks[0]").value("2026-11-09"));
    }

    @Test
    @WithMockUser(username = "sup@t.test", roles = "SUPERVISOR")
    void supervisorApprovesWithoutABody() throws Exception {
        mvc.perform(post("/api/supervisor/weekly-reports/9/approve").with(csrf()))
                .andExpect(status().isOk());

        verify(weekly).approve(20L, 9L, null);
    }

    @Test
    @WithMockUser(username = "sup@t.test", roles = "SUPERVISOR")
    void supervisorReturnsAReportWithAComment() throws Exception {
        mvc.perform(post("/api/supervisor/weekly-reports/9/return")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"Add detail\"}"))
                .andExpect(status().isOk());

        verify(weekly).returnReport(20L, 9L, "Add detail");
    }

    @Test
    @WithMockUser(username = "sup@t.test", roles = "SUPERVISOR")
    void supervisorSeesTheirProgressOverview() throws Exception {
        when(progress.overview(20L)).thenReturn(List.of());

        mvc.perform(get("/api/supervisor/progress"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(username = "coord@t.test", roles = "COORDINATOR")
    void coordinatorRunsTheReminderJobByHand() throws Exception {
        when(weekly.sendReminders()).thenReturn(3);

        mvc.perform(post("/api/coordinator/weekly-reports/reminders").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remindersCreated").value(3));
    }
}
