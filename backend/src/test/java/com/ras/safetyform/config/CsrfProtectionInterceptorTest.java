package com.ras.safetyform.config;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ras.safetyform.service.AuthorizationException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CsrfProtectionInterceptorTest {

    private final CsrfProtectionInterceptor interceptor = new CsrfProtectionInterceptor();

    @Test
    void rejectsStateChangingRequestWithoutToken() {
        MockHttpServletRequest request = sessionRequest("POST", "/api/sites");

        assertThrows(AuthorizationException.class, () -> run(request));
    }

    @Test
    void rejectsStateChangingRequestWithWrongToken() {
        MockHttpServletRequest request = sessionRequest("PATCH", "/api/users/3/active");
        request.addHeader(SessionUser.CSRF_HEADER, "not-the-token");

        assertThrows(AuthorizationException.class, () -> run(request));
    }

    @Test
    void acceptsMatchingToken() {
        MockHttpServletRequest request = sessionRequest("DELETE", "/api/sites/1");
        request.addHeader(
                SessionUser.CSRF_HEADER,
                (String) request.getSession().getAttribute(SessionUser.CSRF_TOKEN_ATTRIBUTE));

        assertTrue(run(request));
    }

    @Test
    void doesNotRequireTokenForReadsLoginLogoutOrAnonymousRequests() {
        assertTrue(run(sessionRequest("GET", "/api/users")));
        assertTrue(run(sessionRequest("OPTIONS", "/api/users")));
        assertTrue(run(sessionRequest("POST", "/api/auth/login")));
        assertTrue(run(sessionRequest("POST", "/api/auth/logout")));
        assertTrue(run(new MockHttpServletRequest("POST", "/api/sites")));
    }

    private boolean run(MockHttpServletRequest request) {
        return interceptor.preHandle(request, new MockHttpServletResponse(), new Object());
    }

    private MockHttpServletRequest sessionRequest(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.getSession(true).setAttribute(SessionUser.CSRF_TOKEN_ATTRIBUTE, "expected-token");
        return request;
    }
}
