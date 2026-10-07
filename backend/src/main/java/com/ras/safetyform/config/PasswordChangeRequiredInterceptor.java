package com.ras.safetyform.config;

import com.ras.safetyform.service.AuthorizationException;
import com.ras.safetyform.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

        Integer userId = SessionUser.currentUserId(request);
        if (userId != null && userService.mustChangePassword(userId)) {
            throw new AuthorizationException("Password change required");
        }

        return true;
    }

    private boolean isPasswordChangeRoute(HttpServletRequest request) {
        String path = ApiPaths.relativePath(request);
        String method = request.getMethod();
        return (ApiPaths.LOGIN.equals(path) && "POST".equals(method))
                || (ApiPaths.LOGOUT.equals(path) && "POST".equals(method))
                || ((ApiPaths.PREFIX + "/auth/me").equals(path) && "GET".equals(method))
                || ((ApiPaths.PREFIX + "/auth/me/password").equals(path) && "POST".equals(method));
    }
}
