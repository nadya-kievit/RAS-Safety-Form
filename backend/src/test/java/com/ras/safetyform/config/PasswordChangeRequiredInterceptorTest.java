package com.ras.safetyform.config;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ras.safetyform.service.AuthorizationException;
import com.ras.safetyform.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class PasswordChangeRequiredInterceptorTest {

    private final UserService userService = mock(UserService.class);
    private final PasswordChangeRequiredInterceptor interceptor =
            new PasswordChangeRequiredInterceptor(userService);

    @Test
    void blocksOtherRoutesUntilPasswordIsChanged() {
        MockHttpServletRequest request = authenticatedRequest("GET", "/api/users");
        when(userService.mustChangePassword(7)).thenReturn(true);

        assertThrows(
                AuthorizationException.class,
                () -> interceptor.preHandle(
                        request,
                        new MockHttpServletResponse(),
                        new Object()));
    }

    @Test
    void allowsRequiredPasswordChangeEndpoint() {
        MockHttpServletRequest request = authenticatedRequest(
                "POST",
                "/api/auth/me/password");

        assertTrue(interceptor.preHandle(
                request,
                new MockHttpServletResponse(),
                new Object()));
        verifyNoInteractions(userService);
    }

    @Test
    void allowsNormalRoutesAfterPasswordIsChanged() {
        MockHttpServletRequest request = authenticatedRequest("GET", "/api/users");
        when(userService.mustChangePassword(7)).thenReturn(false);

        assertTrue(interceptor.preHandle(
                request,
                new MockHttpServletResponse(),
                new Object()));
    }

    @Test
    void allowsLoginAndLogoutWithoutLookingUpTheUser() {
        for (String path : new String[] {"/api/auth/login", "/api/auth/logout"}) {
            assertTrue(interceptor.preHandle(
                    authenticatedRequest("POST", path),
                    new MockHttpServletResponse(),
                    new Object()));
        }
        verifyNoInteractions(userService);
    }

    private MockHttpServletRequest authenticatedRequest(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.getSession(true).setAttribute(
                SessionUser.USER_ID_ATTRIBUTE,
                7);
        return request;
    }
}
