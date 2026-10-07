package com.ras.safetyform.config;

import com.ras.safetyform.service.AuthorizationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Requires the session-bound token (issued by /auth/login and /auth/me) in the
 * X-CSRF-Token header on state-changing requests made with a session.
 */
@Component
public class CsrfProtectionInterceptor implements HandlerInterceptor {

    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) {
        if (SAFE_METHODS.contains(request.getMethod()) || isExemptRoute(request)) {
            return true;
        }

        HttpSession session = request.getSession(false);
        if (session == null) {
            // No ambient credentials to abuse; protected routes answer 401 on their own.
            return true;
        }

        String expected = (String) session.getAttribute(SessionUser.CSRF_TOKEN_ATTRIBUTE);
        String provided = request.getHeader(SessionUser.CSRF_HEADER);
        if (expected == null
                || provided == null
                || !MessageDigest.isEqual(
                        expected.getBytes(StandardCharsets.UTF_8),
                        provided.getBytes(StandardCharsets.UTF_8))) {
            throw new AuthorizationException("Invalid or missing CSRF token");
        }
        return true;
    }

    private boolean isExemptRoute(HttpServletRequest request) {
        String path = ApiPaths.relativePath(request);
        return ApiPaths.LOGIN.equals(path) || ApiPaths.LOGOUT.equals(path);
    }
}
