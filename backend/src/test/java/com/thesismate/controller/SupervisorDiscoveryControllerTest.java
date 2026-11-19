package com.thesismate.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import com.thesismate.config.SecurityConfig;
import com.thesismate.dto.SupervisorDirectoryItem;
import com.thesismate.dto.SupervisorPageResponse;
import com.thesismate.model.Track;
import com.thesismate.repository.AppUserRepository;
import com.thesismate.service.AppUserDetailsService;
import com.thesismate.service.AuthService;
import com.thesismate.service.SupervisorDiscoveryService;

@WebMvcTest(controllers = SupervisorDiscoveryController.class)
@Import({SecurityConfig.class, AuthService.class, AppUserDetailsService.class, GlobalExceptionHandler.class})
class SupervisorDiscoveryControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private SupervisorDiscoveryService service;

    @MockitoBean
    private AppUserRepository users;

    private static SupervisorDirectoryItem sampleCard() {
        return new SupervisorDirectoryItem(
                1L, 10L, "Dr. Alice Rahman", "NLP and ML research", "07A-01C",
                List.of("Machine Learning", "NLP"), List.of(Track.THESIS, Track.PROJECT),
                "Spring 2027", 3, 1, 2, true);
    }

    @Test
    void unauthenticatedGets401() throws Exception {
        mvc.perform(get("/api/supervisors").param("semester", "Spring 2027"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "student1@t.test", roles = "STUDENT")
    void studentCanSearchSupervisorsAndResponseHasOnlyPublicFields() throws Exception {
        SupervisorPageResponse page = new SupervisorPageResponse(
                List.of(sampleCard()), 0, 10, 1, 1, false, false);
        when(service.search("Spring 2027", "NLP", "Alice", Track.THESIS, true, "remaining,desc", 0, 10))
                .thenReturn(page);

        mvc.perform(get("/api/supervisors")
                        .param("semester", "Spring 2027")
                        .param("area", "NLP")
                        .param("q", "Alice")
                        .param("track", "THESIS")
                        .param("hasSlots", "true")
                        .param("sort", "remaining,desc")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].profileId").value(1))
                .andExpect(jsonPath("$.items[0].supervisorId").value(10))
                .andExpect(jsonPath("$.items[0].fullName").value("Dr. Alice Rahman"))
                .andExpect(jsonPath("$.items[0].officeRoom").value("07A-01C"))
                .andExpect(jsonPath("$.items[0].remainingSlots").value(2))
                .andExpect(jsonPath("$.items[0].available").value(true))
                .andExpect(jsonPath("$.items[0].email").doesNotExist())
                .andExpect(jsonPath("$.items[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$.items[0].version").doesNotExist());
    }

    @Test
    @WithMockUser(username = "student1@t.test", roles = "STUDENT")
    void invalidSortOrPageReturns400ApiError() throws Exception {
        when(service.search("Spring 2027", null, null, null, null, "bad", -1, 10))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported sort parameter: bad"));

        mvc.perform(get("/api/supervisors")
                        .param("semester", "Spring 2027")
                        .param("sort", "bad")
                        .param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(username = "student1@t.test", roles = "STUDENT")
    void studentCanReadSingleSupervisorCard() throws Exception {
        when(service.getByProfileId(1L, "Spring 2027")).thenReturn(sampleCard());

        mvc.perform(get("/api/supervisors/1").param("semester", "Spring 2027"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Dr. Alice Rahman"))
                .andExpect(jsonPath("$.email").doesNotExist());
    }
}
