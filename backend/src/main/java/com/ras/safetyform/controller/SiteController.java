package com.ras.safetyform.controller;

import com.ras.safetyform.config.SessionUser;
import com.ras.safetyform.dto.ChecklistResponse;
import com.ras.safetyform.dto.ChecklistUpdateRequest;
import com.ras.safetyform.dto.SiteActivationRequest;
import com.ras.safetyform.dto.SiteCreateRequest;
import com.ras.safetyform.dto.SiteResponse;
import com.ras.safetyform.dto.SiteUpdateRequest;
import com.ras.safetyform.service.SiteService;
import com.ras.safetyform.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sites")
public class SiteController {

    private final SiteService siteService;
    private final UserService userService;

    public SiteController(SiteService siteService, UserService userService) {
        this.siteService = siteService;
        this.userService = userService;
    }

    @GetMapping
    public List<SiteResponse> getSites(
            @RequestParam(name = "include_inactive", defaultValue = "false") boolean includeInactive,
            HttpServletRequest request) {
        Integer userId = SessionUser.requireUserId(request);
        if (includeInactive) {
            userService.requireAdmin(userId);
            return siteService.getAllSites();
        }
        return siteService.getActiveSites();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SiteResponse createSite(
            @Valid @RequestBody SiteCreateRequest site,
            HttpServletRequest request) {
        userService.requireAdmin(SessionUser.requireUserId(request));
        return siteService.createSite(site);
    }

    @GetMapping("/{siteId}")
    public SiteResponse getSite(@PathVariable Integer siteId, HttpServletRequest request) {
        SessionUser.requireUserId(request);
        return siteService.getSite(siteId);
    }

    @PatchMapping("/{siteId}")
    public SiteResponse renameSite(
            @PathVariable Integer siteId,
            @Valid @RequestBody SiteUpdateRequest site,
            HttpServletRequest request) {
        userService.requireAdmin(SessionUser.requireUserId(request));
        return siteService.renameSite(siteId, site.name());
    }

    @PatchMapping("/{siteId}/active")
    public SiteResponse setSiteActive(
            @PathVariable Integer siteId,
            @Valid @RequestBody SiteActivationRequest activation,
            HttpServletRequest request) {
        userService.requireAdmin(SessionUser.requireUserId(request));
        return siteService.setSiteActive(siteId, activation.active());
    }

    @GetMapping("/{siteId}/checklist")
    public ChecklistResponse getSiteChecklist(
            @PathVariable Integer siteId,
            HttpServletRequest request) {
        SessionUser.requireUserId(request);
        return siteService.getSiteChecklist(siteId);
    }

    @PutMapping("/{siteId}/checklist")
    public ChecklistResponse updateChecklist(
            @PathVariable Integer siteId,
            @Valid @RequestBody ChecklistUpdateRequest checklist,
            HttpServletRequest request) {
        userService.requireAdmin(SessionUser.requireUserId(request));
        return siteService.updateChecklist(siteId, checklist);
    }
}
