package com.ras.safetyform.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class SecurityHeadersFilterTest {

    private static SupabaseStorageProperties storage() {
        SupabaseStorageProperties properties = new SupabaseStorageProperties();
        properties.setUrl("https://project-ref.supabase.co/rest/v1/");
        properties.setSecretKey("secret");
        properties.setBucket("bucket");
        return properties;
    }

    private MockHttpServletResponse filter(boolean hsts, String path)
            throws ServletException, IOException {
        MockHttpServletResponse response = new MockHttpServletResponse();
        new SecurityHeadersFilter(hsts, storage()).doFilter(
                new MockHttpServletRequest("GET", path),
                response,
                new MockFilterChain());
        return response;
    }

    @Test
    void addsBrowserSecurityHeaders() throws Exception {
        MockHttpServletResponse response = filter(false, "/framer");

        assertEquals("nosniff", response.getHeader("X-Content-Type-Options"));
        assertEquals("DENY", response.getHeader("X-Frame-Options"));
        assertNotNull(response.getHeader("Referrer-Policy"));
        assertNotNull(response.getHeader("Permissions-Policy"));
        String csp = response.getHeader("Content-Security-Policy");
        assertTrue(csp.contains("frame-ancestors 'none'"));
        assertTrue(csp.contains("script-src 'self';"));
        assertTrue(csp.contains("img-src 'self' data: blob: https://project-ref.supabase.co;"));
        assertNull(response.getHeader("Strict-Transport-Security"));
    }

    @Test
    void addsHstsOnlyWhenEnabled() throws Exception {
        assertTrue(filter(true, "/framer")
                .getHeader("Strict-Transport-Security")
                .startsWith("max-age="));
    }

    @Test
    void neverCachesApiResponses() throws Exception {
        assertEquals("no-store", filter(false, "/api/users").getHeader("Cache-Control"));
        assertFalse("no-store".equals(filter(false, "/assets/app.js").getHeader("Cache-Control")));
    }
}
