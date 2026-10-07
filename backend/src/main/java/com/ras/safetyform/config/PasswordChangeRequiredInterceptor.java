package com.ras.safetyform.config;

import com.ras.safetyform.controller.AuthController;
import com.ras.safetyform.service.AuthorizationException;
import com.ras.safetyform.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class PasswordChangeRequiredInterceptor implements HandlerInterceptor {

    private final UserService userService;

    public PasswordChangeRequiredInterceptor(UserService userService) {
        this.userService = userService;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) {
        if (isPasswordChangeRoute(request)) {
            return true;
        }

        HttpSession session = request.getSession(false);
        Object userId = session == null
                ? null
                : session.getAttribute(AuthController.USER_ID_SESSION_ATTRIBUTE);
        if (userId instanceof Integer authenticatedUserId
                && userService.mustChangePassword(authenticatedUserId)) {
            throw new AuthorizationException("Password change required");
        }

        return true;
    }

    private boolean isPasswordChangeRoute(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        String method = request.getMethod();
        return ("/auth/login".equals(path) && "POST".equals(method))
                || ("/auth/logout".equals(path) && "POST".equals(method))
                || ("/auth/me".equals(path) && "GET".equals(method))
                || ("/auth/me/password".equals(path) && "POST".equals(method));
    }
}
