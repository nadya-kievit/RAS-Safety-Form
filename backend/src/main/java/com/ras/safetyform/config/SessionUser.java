package com.ras.safetyform.config;

import com.ras.safetyform.service.AuthenticationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.session.FindByIndexNameSessionRepository;

/** Reads and writes the authenticated user held in the HTTP session. */
public final class SessionUser {

    public static final String USER_ID_ATTRIBUTE = "authenticatedUserId";
    public static final String CSRF_TOKEN_ATTRIBUTE = "csrfToken";
    public static final String CSRF_HEADER = "X-CSRF-Token";

    private static final SecureRandom RANDOM = new SecureRandom();

    private SessionUser() {
    }

    /** The signed-in user id, or null when the request has no authenticated session. */
    public static Integer currentUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object userId = session == null ? null : session.getAttribute(USER_ID_ATTRIBUTE);
        return userId instanceof Integer authenticatedUserId ? authenticatedUserId : null;
    }

    public static Integer requireUserId(HttpServletRequest request) {
        Integer userId = currentUserId(request);
        if (userId == null) {
            throw new AuthenticationException("Authentication required");
        }
        return userId;
    }

    /** Starts a fresh session (preventing fixation) indexed by user id for later revocation. */
    public static HttpSession signIn(HttpServletRequest request, Integer userId) {
        HttpSession existing = request.getSession(false);
        if (existing != null) {
            existing.invalidate();
        }
        HttpSession session = request.getSession(true);
        session.setAttribute(USER_ID_ATTRIBUTE, userId);
        session.setAttribute(
                FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME,
                principalName(userId));
        session.setAttribute(CSRF_TOKEN_ATTRIBUTE, newToken());
        return session;
    }

    public static String principalName(Integer userId) {
        return String.valueOf(userId);
    }

    /** The CSRF token for the session, created on demand for sessions that predate it. */
    public static String csrfToken(HttpSession session) {
        Object token = session.getAttribute(CSRF_TOKEN_ATTRIBUTE);
        if (token instanceof String existing) {
            return existing;
        }
        String created = newToken();
        session.setAttribute(CSRF_TOKEN_ATTRIBUTE, created);
        return created;
    }

    private static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
