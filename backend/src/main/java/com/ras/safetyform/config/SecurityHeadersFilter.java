package com.ras.safetyform.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Adds browser security headers to every response and disables caching of API data. */
@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {

    private final boolean hstsEnabled;
    private final String contentSecurityPolicy;

    public SecurityHeadersFilter(
            @Value("${app.security.hsts-enabled:false}") boolean hstsEnabled,
            SupabaseStorageProperties storageProperties) {
        this.hstsEnabled = hstsEnabled;
        this.contentSecurityPolicy = buildContentSecurityPolicy(storageProperties.getUrl());
    }

    static String buildContentSecurityPolicy(String storageUrl) {
        // Photos are shown from short-lived signed URLs on the Supabase project host.
        String photoHost = "";
        try {
            String host = URI.create(storageUrl.strip()).getHost();
            if (host != null) {
                photoHost = " https://" + host;
            }
        } catch (IllegalArgumentException | NullPointerException ignored) {
            // Fall back to same-origin images only.
        }
        return "default-src 'self'; "
                + "script-src 'self'; "
                + "style-src 'self' 'unsafe-inline'; "
                + "img-src 'self' data: blob:" + photoHost + "; "
                + "connect-src 'self'; "
                + "object-src 'none'; "
                + "base-uri 'self'; "
                + "form-action 'self'; "
                + "frame-ancestors 'none'";
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        response.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
        response.setHeader("Content-Security-Policy", contentSecurityPolicy);
        if (hstsEnabled) {
            response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        }
        if (request.getRequestURI().startsWith(request.getContextPath() + ApiPaths.PREFIX + "/")) {
            response.setHeader("Cache-Control", "no-store");
        }
        chain.doFilter(request, response);
    }
}
