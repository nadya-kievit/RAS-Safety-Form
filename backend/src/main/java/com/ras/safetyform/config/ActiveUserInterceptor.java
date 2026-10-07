package com.ras.safetyform.config;

import com.ras.safetyform.dto.UserResponse;
import com.ras.safetyform.service.AuthenticationException;
import com.ras.safetyform.service.ResourceNotFoundException;
import com.ras.safetyform.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Ends sessions whose user was deactivated or deleted, so a deactivation takes
 * effect on the very next request even if the session itself was not revoked.
 */
@Component
public class ActiveUserInterceptor implements HandlerInterceptor {

    public static final String DEACTIVATED_MESSAGE = "Your account has been deactivated";

    private final UserService userService;

    public ActiveUserInterceptor(UserService userService) {
        this.userService = userService;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) {
        Integer userId = SessionUser.currentUserId(request);
        if (userId == null || isSessionEndingRoute(request)) {
            return true;
        }

        UserResponse user;
        try {
            user = userService.getUser(userId);
        } catch (ResourceNotFoundException exception) {
            invalidate(request);
            throw new AuthenticationException("Authentication required");
        }
        if (!user.active()) {
            invalidate(request);
            throw new AuthenticationException(DEACTIVATED_MESSAGE);
        }
        return true;
    }

    private void invalidate(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    private boolean isSessionEndingRoute(HttpServletRequest request) {
        String path = ApiPaths.relativePath(request);
        return "POST".equals(request.getMethod())
                && (ApiPaths.LOGIN.equals(path) || ApiPaths.LOGOUT.equals(path));
    }
}
