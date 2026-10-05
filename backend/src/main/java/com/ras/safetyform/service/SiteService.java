package com.ras.safetyform.service;

import com.ras.safetyform.dto.ChecklistItemResponse;
import com.ras.safetyform.dto.ChecklistResponse;
import com.ras.safetyform.dto.SiteResponse;
import com.ras.safetyform.model.SafetyChecklist;
import com.ras.safetyform.model.Site;
import com.ras.safetyform.repository.SafetyChecklistItemRepository;
import com.ras.safetyform.repository.SiteRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SiteService {

    private final SiteRepository siteRepository;
    private final SafetyChecklistItemRepository checklistItemRepository;

    public SiteService(
            SiteRepository siteRepository,
            SafetyChecklistItemRepository checklistItemRepository) {
        this.siteRepository = siteRepository;
        this.checklistItemRepository = checklistItemRepository;
    }

    @Transactional(readOnly = true)
    public List<SiteResponse> getActiveSites() {
        return siteRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(this::toSiteResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SiteResponse getSite(Integer siteId) {
        return toSiteResponse(findSite(siteId));
    }

    @Transactional(readOnly = true)
    public ChecklistResponse getSiteChecklist(Integer siteId) {
        Site site = findSite(siteId);
        SafetyChecklist checklist = site.getChecklist();
        List<ChecklistItemResponse> items = checklistItemRepository
                .findByChecklist_IdOrderByIdAsc(checklist.getId())
                .stream()
                .map(item -> new ChecklistItemResponse(
                        item.getId(), item.getChecklist().getId(), item.getItem()))
                .toList();

        return new ChecklistResponse(checklist.getId(), checklist.getName(), items);
    }

    private Site findSite(Integer siteId) {
        return siteRepository.findOneById(siteId)
                .orElseThrow(() -> new ResourceNotFoundException("Site not found"));
    }

    private SiteResponse toSiteResponse(Site site) {
        return new SiteResponse(
                site.getId(),
                site.getName(),
                site.getChecklist().getId(),
                site.isActive(),
                site.getCreatedAt());
    }
}
