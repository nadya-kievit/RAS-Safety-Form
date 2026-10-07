package com.ras.safetyform.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ras.safetyform.dto.UserResponse;
import com.ras.safetyform.service.AuthenticationException;
import com.ras.safetyform.service.ResourceNotFoundException;
import com.ras.safetyform.service.UserService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

class ActiveUserInterceptorTest {

    private final UserService userService = mock(UserService.class);
    private final ActiveUserInterceptor interceptor = new ActiveUserInterceptor(userService);

    @Test
    void endsTheSessionOfADeactivatedUserOnTheNextRequest() {
        when(userService.getUser(7)).thenReturn(user(false));
        MockHttpServletRequest request = authenticatedRequest("GET", "/api/safety-forms");
        MockHttpSession session = (MockHttpSession) request.getSession(false);

        AuthenticationException exception = assertThrows(
                AuthenticationException.class, () -> run(request));

        assertEquals("Your account has been deactivated", exception.getMessage());
        assertTrue(session.isInvalid());
    }

    @Test
    void endsTheSessionWhenTheUserNoLongerExists() {
        when(userService.getUser(7)).thenThrow(new ResourceNotFoundException("User not found"));

        assertThrows(
                AuthenticationException.class,
                () -> run(authenticatedRequest("GET", "/api/users")));
    }

    @Test
    void allowsActiveUsers() {
        when(userService.getUser(7)).thenReturn(user(true));

        assertTrue(run(authenticatedRequest("GET", "/api/users")));
    }

    @Test
    void ignoresAnonymousRequestsAndSessionEndingRoutes() {
        assertTrue(run(new MockHttpServletRequest("GET", "/api/users")));
        assertTrue(run(authenticatedRequest("POST", "/api/auth/login")));
        assertTrue(run(authenticatedRequest("POST", "/api/auth/logout")));
        verifyNoInteractions(userService);
    }

    private boolean run(MockHttpServletRequest request) {
        return interceptor.preHandle(request, new MockHttpServletResponse(), new Object());
    }

    private MockHttpServletRequest authenticatedRequest(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.getSession(true).setAttribute(SessionUser.USER_ID_ATTRIBUTE, 7);
        return request;
    }

    private UserResponse user(boolean active) {
        return new UserResponse(
                7, "Alex", "Framer", "alex", "framer", false, active, Instant.now());
    }
}
