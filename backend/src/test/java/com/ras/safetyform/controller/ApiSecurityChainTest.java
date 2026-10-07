package com.ras.safetyform.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ras.safetyform.config.ActiveUserInterceptor;
import com.ras.safetyform.config.CsrfProtectionInterceptor;
import com.ras.safetyform.config.GlobalExceptionHandler;
import com.ras.safetyform.config.PasswordChangeRequiredInterceptor;
import com.ras.safetyform.config.SessionUser;
import com.ras.safetyform.dto.UserResponse;
import com.ras.safetyform.service.AuthService;
import com.ras.safetyform.service.AuthenticationException;
import com.ras.safetyform.service.AuthorizationException;
import com.ras.safetyform.service.LoginRateLimiter;
import com.ras.safetyform.service.SafetyFormService;
import com.ras.safetyform.service.SiteService;
import com.ras.safetyform.service.UserService;
import com.ras.safetyform.service.UserSessionService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/** Exercises routing, authentication, CSRF and error handling through the real MVC chain. */
class ApiSecurityChainTest {

    private final AuthService authService = mock(AuthService.class);
    private final UserService userService = mock(UserService.class);
    private final SafetyFormService safetyFormService = mock(SafetyFormService.class);
    private final SiteService siteService = mock(SiteService.class);
    private final UserSessionService userSessionService = mock(UserSessionService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        LoginRateLimiter limiter = new LoginRateLimiter(3, Duration.ofMinutes(15));
        String[] protectedPaths = {"/api/auth/**", "/api/users/**", "/api/sites/**", "/api/safety-forms/**"};
        mvc = MockMvcBuilders.standaloneSetup(
                        new AuthController(authService, userService, limiter),
                        new SafetyFormController(safetyFormService),
                        new UserController(userService, safetyFormService, userSessionService),
                        new SiteController(siteService, userService))
                .setCustomHandlerMapping(() -> {
                    RequestMappingHandlerMapping mapping = new RequestMappingHandlerMapping();
                    mapping.setPathPrefixes(Map.of(
                            "/api", HandlerTypePredicate.forAnnotation(RestController.class)));
                    return mapping;
                })
                .setControllerAdvice(new GlobalExceptionHandler())
                .addMappedInterceptors(protectedPaths, new ActiveUserInterceptor(userService))
                .addMappedInterceptors(protectedPaths, new CsrfProtectionInterceptor())
                .addMappedInterceptors(protectedPaths, new PasswordChangeRequiredInterceptor(userService))
                .build();
    }

    @Test
    void apiRoutesLiveUnderTheApiPrefixOnly() throws Exception {
        mvc.perform(get("/api/safety-forms")).andExpect(status().isUnauthorized());
        mvc.perform(get("/safety-forms")).andExpect(status().isNotFound());
    }

    @Test
    void requestsWithoutASessionAreRejected() throws Exception {
        mvc.perform(get("/api/safety-forms")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/safety-forms/5")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/safety-forms/5/photos")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/users")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/users/5/safety-forms")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/sites")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/sites/1/checklist")).andExpect(status().isUnauthorized());
    }

    @Test
    void theLegacyUnauthenticatedCreateEndpointIsGone() throws Exception {
        MockHttpSession session = signedInSession();

        mvc.perform(post("/api/safety-forms")
                        .session(session)
                        .header(SessionUser.CSRF_HEADER, "csrf-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"user_id\":7,\"site_id\":3,\"form_date\":\"2026-10-07T12:00:00Z\"}"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void loginIssuesACsrfTokenAndIndexesTheSessionByUser() throws Exception {
        when(authService.login(any())).thenReturn(user(true));

        MvcResult result = mvc.perform(login("alex", "password"))
                .andExpect(status().isOk())
                .andExpect(header().exists(SessionUser.CSRF_HEADER))
                .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertNotNull(session);
        assertEquals(7, session.getAttribute(SessionUser.USER_ID_ATTRIBUTE));
        assertEquals("7", session.getAttribute(
                org.springframework.session.FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME));
        assertEquals(
                session.getAttribute(SessionUser.CSRF_TOKEN_ATTRIBUTE),
                result.getResponse().getHeader(SessionUser.CSRF_HEADER));
    }

    @Test
    void deactivatedUsersAreToldSoWhenTheyLogIn() throws Exception {
        when(authService.login(any()))
                .thenThrow(new AuthorizationException("Your account has been deactivated"));

        mvc.perform(login("alex", "password"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Your account has been deactivated"));
    }

    @Test
    void repeatedFailedLoginsAreRateLimited() throws Exception {
        when(authService.login(any())).thenThrow(new AuthenticationException("Invalid username or password"));

        for (int attempt = 0; attempt < 3; attempt++) {
            mvc.perform(login("alex", "wrong")).andExpect(status().isUnauthorized());
        }
        mvc.perform(login("alex", "wrong"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    void stateChangingRequestsNeedTheCsrfToken() throws Exception {
        MockHttpSession session = signedInSession();
        when(userService.getUser(7)).thenReturn(user(true));

        mvc.perform(patch("/api/users/9/active")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Invalid or missing CSRF token"));
    }

    @Test
    void deactivatingAUserEndsTheirSessionsImmediately() throws Exception {
        MockHttpSession admin = signedInSession();
        when(userService.getUser(7)).thenReturn(user(true));
        when(userService.setUserActive(9, false, 7)).thenReturn(
                new UserResponse(9, "Sam", "Framer", "sam", "framer", false, false, Instant.now()));

        mvc.perform(patch("/api/users/9/active")
                        .session(admin)
                        .header(SessionUser.CSRF_HEADER, "csrf-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk());

        verify(userSessionService).invalidateAllSessions(9);
    }

    @Test
    void aSessionBelongingToADeactivatedUserIsRejectedOnTheNextRequest() throws Exception {
        MockHttpSession session = signedInSession();
        when(userService.getUser(7)).thenReturn(user(false));

        mvc.perform(get("/api/safety-forms").session(session))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Your account has been deactivated"));
        assertEquals(true, session.isInvalid());
    }

    @Test
    void framersCannotUseAdministratorSiteManagement() throws Exception {
        MockHttpSession session = signedInSession();
        when(userService.getUser(7)).thenReturn(user(true));
        doThrow(new AuthorizationException("Administrator access required"))
                .when(userService).requireAdmin(7);

        mvc.perform(post("/api/sites")
                        .session(session)
                        .header(SessionUser.CSRF_HEADER, "csrf-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Site\",\"checklistItems\":[\"Item\"]}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/sites?include_inactive=true").session(session))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/users").session(session)).andExpect(status().isForbidden());
    }

    @Test
    void usersCanListActiveSites() throws Exception {
        MockHttpSession session = signedInSession();
        when(userService.getUser(7)).thenReturn(user(true));
        when(siteService.getActiveSites()).thenReturn(List.of());

        mvc.perform(get("/api/sites").session(session)).andExpect(status().isOk());
    }

    private org.springframework.test.web.servlet.RequestBuilder login(String username, String password) {
        return post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}");
    }

    private MockHttpSession signedInSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionUser.USER_ID_ATTRIBUTE, 7);
        session.setAttribute(SessionUser.CSRF_TOKEN_ATTRIBUTE, "csrf-token");
        return session;
    }

    private UserResponse user(boolean active) {
        return new UserResponse(7, "Alex", "Admin", "alex", "admin", false, active, Instant.now());
    }
}
