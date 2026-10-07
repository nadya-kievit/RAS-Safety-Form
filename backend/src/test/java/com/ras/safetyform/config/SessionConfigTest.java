package com.ras.safetyform.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.session.web.http.CookieSerializer;

class SessionConfigTest {

    @Test
    void sessionCookiePersistsForThirtyDays() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        CookieSerializer serializer = new SessionConfig().cookieSerializer(false);

        serializer.writeCookieValue(new CookieSerializer.CookieValue(
                request,
                response,
                "session-id"));

        String cookie = response.getHeader("Set-Cookie");
        assertTrue(cookie.contains("RAS_SESSION="));
        assertTrue(cookie.contains("Max-Age=" + SessionConfig.COOKIE_MAX_AGE_SECONDS));
        assertTrue(cookie.contains("Path=/"));
        assertTrue(cookie.contains("HttpOnly"));
        assertTrue(cookie.contains("SameSite=Lax"));
        assertFalse(cookie.contains("Secure"));
    }

    @Test
    void productionSessionCookieCanBeSecure() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        CookieSerializer serializer = new SessionConfig().cookieSerializer(true);

        serializer.writeCookieValue(new CookieSerializer.CookieValue(
                request,
                response,
                "session-id"));

        assertTrue(response.getHeader("Set-Cookie").contains("Secure"));
    }
}
