package com.thesismate.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.thesismate.config.SecurityConfig;
import com.thesismate.dto.CapacityOverrideView;
import com.thesismate.dto.SupervisorCapacityView;
import com.thesismate.model.Track;
import com.thesismate.repository.AppUserRepository;
import com.thesismate.service.AppUserDetailsService;
import com.thesismate.service.AuthService;
import com.thesismate.service.SupervisorCapacityService;

@WebMvcTest(controllers = SupervisorCapacityController.class)
@Import({SecurityConfig.class, AuthService.class, AppUserDetailsService.class, GlobalExceptionHandler.class})
class SupervisorCapacityControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private SupervisorCapacityService service;

    @MockitoBean
    private AppUserRepository users;

    private static SupervisorCapacityView view() {
        return new SupervisorCapacityView(1L, "Sup One", "bio", "UB705",
                Set.of("ML"), Set.of(Track.THESIS), "Fall 2026", 5, 0, 5, true, 2L);
    }

    @Test
    void unauthenticatedGets401() throws Exception {
        mvc.perform(get("/api/supervisor/me/capacity").param("semester", "Fall 2026"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "student@t.test", roles = "STUDENT")
    void studentCannotReachSupervisorEndpoints() throws Exception {
        mvc.perform(get("/api/supervisor/me/capacity").param("semester", "Fall 2026"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "sup@t.test", roles = "SUPERVISOR")
    void supervisorReadsOwnCapacity() throws Exception {
        when(service.getMyCapacity("sup@t.test", "Fall 2026")).thenReturn(view());

        mvc.perform(get("/api/supervisor/me/capacity").param("semester", "Fall 2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capacity").value(5))
                .andExpect(jsonPath("$.remainingSlots").value(5))
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    @WithMockUser(username = "sup@t.test", roles = "SUPERVISOR")
    void supervisorUpdatesCapacity() throws Exception {
        when(service.updateCapacity(eq("sup@t.test"), any())).thenReturn(view());

        mvc.perform(put("/api/supervisor/me/capacity")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"semester\":\"Fall 2026\",\"capacity\":5,\"version\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(2));
    }

    @Test
    @WithMockUser(username = "sup@t.test", roles = "SUPERVISOR")
    void putWithoutCsrfIsForbidden() throws Exception {
        mvc.perform(put("/api/supervisor/me/capacity")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"semester\":\"Fall 2026\",\"capacity\":5,\"version\":2}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "coord@t.test", roles = "COORDINATOR")
    void coordinatorReadsOverrideHistory() throws Exception {
        when(service.getOverrides(1L)).thenReturn(List.of(
                new CapacityOverrideView(9L, "Fall 2026", 3, 6, "extra slot", "Coord", LocalDateTime.now())));

        mvc.perform(get("/api/coordinator/supervisors/1/overrides"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].newCapacity").value(6))
                .andExpect(jsonPath("$[0].reason").value("extra slot"));
    }

    @Test
    @WithMockUser(username = "sup@t.test", roles = "SUPERVISOR")
    void supervisorCannotUseCoordinatorOverride() throws Exception {
        mvc.perform(put("/api/coordinator/supervisors/1/capacity")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"semester\":\"Fall 2026\",\"newCapacity\":9,\"reason\":\"x\"}"))
                .andExpect(status().isForbidden());
    }
}
