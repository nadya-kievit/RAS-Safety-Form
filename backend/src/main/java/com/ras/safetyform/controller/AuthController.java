package com.ras.safetyform.controller;

import com.ras.safetyform.config.SessionUser;
import com.ras.safetyform.dto.LoginRequest;
import com.ras.safetyform.dto.PasswordChangeRequest;
import com.ras.safetyform.dto.ProfileUpdateRequest;
import com.ras.safetyform.dto.UserResponse;
import com.ras.safetyform.service.AuthService;
import com.ras.safetyform.service.AuthenticationException;
import com.ras.safetyform.service.LoginRateLimiter;
import com.ras.safetyform.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final LoginRateLimiter loginRateLimiter;

    public AuthController(
            AuthService authService,
            UserService userService,
            LoginRateLimiter loginRateLimiter) {
        this.authService = authService;
        this.userService = userService;
        this.loginRateLimiter = loginRateLimiter;
    }

    @PostMapping("/login")
    public UserResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        String clientAddress = servletRequest.getRemoteAddr();
        loginRateLimiter.assertAllowed(clientAddress, request.username());

        UserResponse user;
        try {
            user = authService.login(request);
        } catch (AuthenticationException exception) {
            loginRateLimiter.recordFailure(clientAddress, request.username());
            throw exception;
        }
        loginRateLimiter.recordSuccess(clientAddress, request.username());

        HttpSession session = SessionUser.signIn(servletRequest, user.id());
        servletResponse.setHeader(SessionUser.CSRF_HEADER, SessionUser.csrfToken(session));
        return user;
    }

    @GetMapping("/me")
    public UserResponse getCurrentUser(
            HttpServletRequest request,
            HttpServletResponse response) {
        UserResponse user = userService.getUser(SessionUser.requireUserId(request));
        if (!user.active()) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            throw new AuthenticationException("Authentication required");
        }
        response.setHeader(
                SessionUser.CSRF_HEADER,
                SessionUser.csrfToken(request.getSession(false)));
        return user;
    }

    @PatchMapping("/me")
    public UserResponse updateCurrentUser(
            @Valid @RequestBody ProfileUpdateRequest profile,
            HttpServletRequest request) {
        return userService.updateProfile(SessionUser.requireUserId(request), profile);
    }

    @PostMapping("/me/password")
    public UserResponse changePassword(
            @Valid @RequestBody PasswordChangeRequest passwordChange,
            HttpServletRequest request) {
        return userService.changePassword(SessionUser.requireUserId(request), passwordChange);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
