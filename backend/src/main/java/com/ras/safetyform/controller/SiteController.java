package com.ras.safetyform.controller;

import com.ras.safetyform.dto.ChecklistResponse;
import com.ras.safetyform.dto.SiteResponse;
import com.ras.safetyform.dto.UserResponse;
import com.ras.safetyform.service.SiteService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sites")
public class SiteController {

    private final SiteService siteService;

    public SiteController(SiteService siteService) {
        this.siteService = siteService;
    }

    @GetMapping
    public List<SiteResponse> getActiveSites() {
        return siteService.getActiveSites();
    }

    @GetMapping("/{siteId}")
    public SiteResponse getSite(@PathVariable Integer siteId) {
        return siteService.getSite(siteId);
    }

    @GetMapping("/{siteId}/checklist")
    public ChecklistResponse getSiteChecklist(@PathVariable Integer siteId) {
        return siteService.getSiteChecklist(siteId);
    }

    @GetMapping("/{siteId}/workers")
    public List<UserResponse> getWorkers(
            @PathVariable Integer siteId,
            @RequestParam(name = "date")
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate date) {
        return siteService.getWorkers(siteId, date);
    }
}
