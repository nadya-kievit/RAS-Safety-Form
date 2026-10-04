package com.ras.safetyform.controller;

import com.ras.safetyform.dto.SafetyFormResponse;
import com.ras.safetyform.dto.SiteAssignmentResponse;
import com.ras.safetyform.dto.UserResponse;
import com.ras.safetyform.service.SafetyFormService;
import com.ras.safetyform.service.UserService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final SafetyFormService safetyFormService;

    public UserController(UserService userService, SafetyFormService safetyFormService) {
        this.userService = userService;
        this.safetyFormService = safetyFormService;
    }

    @GetMapping("/{userId}")
    public UserResponse getUser(@PathVariable Integer userId) {
        return userService.getUser(userId);
    }

    @GetMapping("/{userId}/assignments")
    public List<SiteAssignmentResponse> getAssignments(@PathVariable Integer userId) {
        return userService.getAssignments(userId);
    }

    @GetMapping("/{userId}/safety-forms")
    public List<SafetyFormResponse> getSafetyForms(@PathVariable Integer userId) {
        return safetyFormService.getFormsForUser(userId);
    }
}
