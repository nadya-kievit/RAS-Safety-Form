package com.ras.safetyform.controller;

import com.ras.safetyform.config.SessionUser;
import com.ras.safetyform.dto.PhotoResponse;
import com.ras.safetyform.dto.SafetyFormCreateRequest;
import com.ras.safetyform.dto.SafetyFormResponse;
import com.ras.safetyform.dto.SubmissionStatusRequest;
import com.ras.safetyform.service.InvalidRequestException;
import com.ras.safetyform.service.SafetyFormService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
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

    @PostMapping(path = "/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public SafetyFormResponse submitSafetyForm(
            @Valid @RequestPart("submission") SafetyFormCreateRequest submission,
            @RequestPart(name = "photos", required = false) List<MultipartFile> photos,
            HttpServletRequest request) {
        return safetyFormService.submitForm(
                submission,
                SessionUser.requireUserId(request),
                photos == null ? List.of() : photos);
    }

    @GetMapping("/{formId}")
    public SafetyFormResponse getSafetyForm(
            @PathVariable Integer formId,
            HttpServletRequest request) {
        return safetyFormService.getForm(formId, SessionUser.requireUserId(request));
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
                    LocalDate endDate,
            @RequestParam(name = "time_zone", required = false) String timeZone,
            HttpServletRequest request) {
        return safetyFormService.getForms(
                SessionUser.requireUserId(request),
                siteId,
                userId,
                startDate,
                endDate,
                parseZone(timeZone));
    }

    @PatchMapping("/{formId}/status")
    public SafetyFormResponse updateStatus(
            @PathVariable Integer formId,
            @Valid @RequestBody SubmissionStatusRequest status,
            HttpServletRequest request) {
        return safetyFormService.updateStatus(
                formId,
                SessionUser.requireUserId(request),
                status.status());
    }

    @PostMapping("/{formId}/photos/upload")
    @ResponseStatus(HttpStatus.CREATED)
    public List<PhotoResponse> uploadPhotos(
            @PathVariable Integer formId,
            @RequestParam("photos") List<MultipartFile> photos,
            HttpServletRequest request) {
        return safetyFormService.uploadPhotos(formId, SessionUser.requireUserId(request), photos);
    }

    @GetMapping("/{formId}/photos")
    public List<PhotoResponse> getPhotos(
            @PathVariable Integer formId,
            HttpServletRequest request) {
        return safetyFormService.getPhotos(formId, SessionUser.requireUserId(request));
    }

    /** Calendar-date filters are interpreted in the caller's time zone (UTC when omitted). */
    private ZoneId parseZone(String timeZone) {
        if (timeZone == null || timeZone.isBlank()) {
            return ZoneOffset.UTC;
        }
        try {
            return ZoneId.of(timeZone);
        } catch (DateTimeException exception) {
            throw new InvalidRequestException("time_zone must be a valid IANA time zone");
        }
    }
}
