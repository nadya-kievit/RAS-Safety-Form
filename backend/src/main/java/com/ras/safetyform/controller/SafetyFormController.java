package com.ras.safetyform.controller;

import com.ras.safetyform.dto.PhotoResponse;
import com.ras.safetyform.dto.SafetyFormCreateRequest;
import com.ras.safetyform.dto.SafetyFormResponse;
import com.ras.safetyform.service.AuthenticationException;
import com.ras.safetyform.service.SafetyFormService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/safety-forms")
public class SafetyFormController {

    private final SafetyFormService safetyFormService;

    public SafetyFormController(SafetyFormService safetyFormService) {
        this.safetyFormService = safetyFormService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SafetyFormResponse createSafetyForm(
            @Valid @RequestBody SafetyFormCreateRequest request) {
        return safetyFormService.createForm(request);
    }

    @GetMapping("/{formId}")
    public SafetyFormResponse getSafetyForm(@PathVariable Integer formId) {
        return safetyFormService.getForm(formId);
    }

    @GetMapping
    public List<SafetyFormResponse> getSafetyForms(
            @RequestParam(name = "site_id", required = false) Integer siteId,
            @RequestParam(name = "user_id", required = false) Integer userId,
            @RequestParam(name = "start_date", required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate startDate,
            @RequestParam(name = "end_date", required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate endDate) {
        return safetyFormService.getForms(siteId, userId, startDate, endDate);
    }

    @PostMapping("/{formId}/photos/upload")
    @ResponseStatus(HttpStatus.CREATED)
    public List<PhotoResponse> uploadPhotos(
            @PathVariable Integer formId,
            @RequestParam("photos") List<MultipartFile> photos,
            HttpServletRequest request) {
        return safetyFormService.uploadPhotos(formId, requireUserId(request), photos);
    }

    @GetMapping("/{formId}/photos")
    public List<PhotoResponse> getPhotos(
            @PathVariable Integer formId,
            HttpServletRequest request) {
        return safetyFormService.getPhotos(formId, requireUserId(request));
    }

    private Integer requireUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object userId = session == null
                ? null
                : session.getAttribute(AuthController.USER_ID_SESSION_ATTRIBUTE);
        if (!(userId instanceof Integer authenticatedUserId)) {
            throw new AuthenticationException("Authentication required");
        }
        return authenticatedUserId;
    }
}
