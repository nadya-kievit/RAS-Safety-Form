package com.ras.safetyform.service;

import com.ras.safetyform.dto.ChecklistItemResponse;
import com.ras.safetyform.dto.ChecklistResponse;
import com.ras.safetyform.dto.SiteResponse;
import com.ras.safetyform.dto.UserResponse;
import com.ras.safetyform.model.SafetyChecklist;
import com.ras.safetyform.model.Site;
import com.ras.safetyform.model.User;
import com.ras.safetyform.repository.SafetyChecklistItemRepository;
import com.ras.safetyform.repository.SiteAssignmentRepository;
import com.ras.safetyform.repository.SiteRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SiteService {

    private final SiteRepository siteRepository;
    private final SafetyChecklistItemRepository checklistItemRepository;
    private final SiteAssignmentRepository assignmentRepository;

    public SiteService(
            SiteRepository siteRepository,
            SafetyChecklistItemRepository checklistItemRepository,
            SiteAssignmentRepository assignmentRepository) {
        this.siteRepository = siteRepository;
        this.checklistItemRepository = checklistItemRepository;
        this.assignmentRepository = assignmentRepository;
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

    @Transactional(readOnly = true)
    public List<UserResponse> getWorkers(Integer siteId, LocalDate date) {
        findSite(siteId);
        return assignmentRepository
                .findBySite_IdAndAssignmentDateOrderByUser_LastNameAscUser_FirstNameAsc(
                        siteId, date)
                .stream()
                .map(assignment -> toUserResponse(assignment.getUser()))
                .toList();
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

    private UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getUsername(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt());
    }
}
