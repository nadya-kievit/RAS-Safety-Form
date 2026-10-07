package com.ras.safetyform.controller;

import com.ras.safetyform.config.SessionUser;
import com.ras.safetyform.dto.SafetyFormResponse;
import com.ras.safetyform.dto.UserActivationRequest;
import com.ras.safetyform.dto.UserCreateRequest;
import com.ras.safetyform.dto.UserResponse;
import com.ras.safetyform.service.SafetyFormService;
import com.ras.safetyform.service.UserService;
import com.ras.safetyform.service.UserSessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final SafetyFormService safetyFormService;
    private final UserSessionService userSessionService;

    public UserController(
            UserService userService,
            SafetyFormService safetyFormService,
            UserSessionService userSessionService) {
        this.userService = userService;
        this.safetyFormService = safetyFormService;
        this.userSessionService = userSessionService;
    }

    @GetMapping
    public List<UserResponse> getUsers(HttpServletRequest request) {
        Integer adminId = requireUserId(request);
        userService.requireAdmin(adminId);
        return userService.getAllUsers();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(
            @Valid @RequestBody UserCreateRequest user,
            HttpServletRequest request) {
        Integer adminId = requireUserId(request);
        userService.requireAdmin(adminId);
        return userService.createUser(user);
    }

    @PatchMapping("/{userId}/active")
    public UserResponse setUserActive(
            @PathVariable Integer userId,
            @Valid @RequestBody UserActivationRequest activation,
            HttpServletRequest request) {
        Integer adminId = requireUserId(request);
        userService.requireAdmin(adminId);
        UserResponse updated = userService.setUserActive(userId, activation.active(), adminId);
        if (!updated.active()) {
            userSessionService.invalidateAllSessions(userId);
        }
        return updated;
    }

    @GetMapping("/{userId}")
    public UserResponse getUser(@PathVariable Integer userId, HttpServletRequest request) {
        return userService.getUserForViewer(userId, requireUserId(request));
    }

    @GetMapping("/{userId}/safety-forms")
    public List<SafetyFormResponse> getSafetyForms(
            @PathVariable Integer userId,
            HttpServletRequest request) {
        return safetyFormService.getFormsForUser(userId, requireUserId(request));
    }

    private Integer requireUserId(HttpServletRequest request) {
        return SessionUser.requireUserId(request);
    }
}
