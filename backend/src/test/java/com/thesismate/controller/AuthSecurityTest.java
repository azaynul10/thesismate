package com.thesismate.controller;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.thesismate.config.SecurityConfig;
import com.thesismate.model.AppUser;
import com.thesismate.model.Role;
import com.thesismate.repository.AppUserRepository;
import com.thesismate.service.AppUserDetailsService;
import com.thesismate.service.AuthService;

@WebMvcTest(controllers = {AuthController.class, HealthController.class})
@Import({SecurityConfig.class, AuthService.class, AppUserDetailsService.class, GlobalExceptionHandler.class})
class AuthSecurityTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PasswordEncoder encoder;

    @MockitoBean
    private AppUserRepository users;

    @Test
    void loginRotatesSessionId() throws Exception {
        AppUser student = new AppUser("student1@thesismate.test", "Student 1", Role.STUDENT,
                encoder.encode("password123"), "20000001");
        when(users.findByEmail("student1@thesismate.test")).thenReturn(Optional.of(student));

        MockHttpSession preLoginSession = new MockHttpSession();
        String preLoginId = preLoginSession.getId();

        var res = mvc.perform(post("/api/auth/login")
                        .session(preLoginSession)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"student1@thesismate.test\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andReturn();

        String postLoginId = res.getRequest().getSession(false).getId();
        assertNotEquals(preLoginId, postLoginId);
    }

    @Test
    void postWithoutCsrfTokenIsForbidden() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"student1@thesismate.test\",\"password\":\"password123\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void roleAccessAndUnauthenticatedChecks() throws Exception {
        mvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());

        AppUser student = new AppUser("student1@thesismate.test", "Student 1", Role.STUDENT,
                encoder.encode("password123"), "20000001");
        when(users.findByEmail("student1@thesismate.test")).thenReturn(Optional.of(student));

        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/api/auth/login")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"student1@thesismate.test\",\"password\":\"password123\"}"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("student1@thesismate.test"));

        mvc.perform(get("/api/coordinator/test").session(session))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/auth/logout").session(session).with(csrf()))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registrationRejectsOversizedFieldsAndMapsUniqueConstraintRaceTo409() throws Exception {
        String longPw = "a".repeat(73);
        mvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@example.test\",\"fullName\":\"A\",\"password\":\"" + longPw + "\",\"studentId\":\"22341034\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").exists());

        String longName = "N".repeat(256);
        mvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@example.test\",\"fullName\":\"" + longName + "\",\"password\":\"abcdefgh\",\"studentId\":\"22341034\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.fullName").exists());

        when(users.existsByEmail("race@example.test")).thenReturn(false);
        when(users.existsByStudentId("22341034")).thenReturn(false);
        when(users.saveAndFlush(any(AppUser.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

        mvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"race@example.test\",\"fullName\":\"Race User\",\"password\":\"abcdefgh\",\"studentId\":\"22341034\"}"))
                .andExpect(status().isConflict());
    }
}
