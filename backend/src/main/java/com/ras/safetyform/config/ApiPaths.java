package com.ras.safetyform.config;

import jakarta.servlet.http.HttpServletRequest;

public final class ApiPaths {

    public static final String PREFIX = "/api";
    public static final String LOGIN = PREFIX + "/auth/login";
    public static final String LOGOUT = PREFIX + "/auth/logout";

    private ApiPaths() {
    }

    /** The request path relative to the application context, e.g. {@code /api/auth/login}. */
    public static String relativePath(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }
}
