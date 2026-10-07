package com.ras.safetyform.service;

import com.ras.safetyform.dto.ChecklistItemResponse;
import com.ras.safetyform.dto.ChecklistResponse;
import com.ras.safetyform.dto.ChecklistUpdateRequest;
import com.ras.safetyform.dto.SiteCreateRequest;
import com.ras.safetyform.dto.SiteResponse;
import com.ras.safetyform.model.SafetyChecklist;
import com.ras.safetyform.model.SafetyChecklistItem;
import com.ras.safetyform.model.Site;
import com.ras.safetyform.repository.SafetyChecklistItemRepository;
import com.ras.safetyform.repository.SafetyChecklistRepository;
import com.ras.safetyform.repository.SiteRepository;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SiteService {

    private static final int NAME_MAX_LENGTH = 150;

    private final SiteRepository siteRepository;
    private final SafetyChecklistRepository checklistRepository;
    private final SafetyChecklistItemRepository checklistItemRepository;

    public SiteService(
            SiteRepository siteRepository,
            SafetyChecklistRepository checklistRepository,
            SafetyChecklistItemRepository checklistItemRepository) {
        this.siteRepository = siteRepository;
        this.checklistRepository = checklistRepository;
        this.checklistItemRepository = checklistItemRepository;
    }

    @Transactional(readOnly = true)
    public List<SiteResponse> getActiveSites() {
        return siteRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(this::toSiteResponse)
                .toList();
    }

    /** Every site, including inactive ones, for administrators managing sites. */
    @Transactional(readOnly = true)
    public List<SiteResponse> getAllSites() {
        return siteRepository.findAllByOrderByNameAsc().stream()
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
        return toChecklistResponse(site.getChecklist());
    }

    @Transactional
    public SiteResponse createSite(SiteCreateRequest request) {
        String name = normalizeName(request.name());
        if (siteRepository.existsByNameIgnoreCase(name)) {
            throw new InvalidRequestException("A site with that name already exists");
        }
        List<String> items = normalizeItems(request.checklistItems());

        SafetyChecklist checklist = checklistRepository.save(
                new SafetyChecklist(checklistNameFor(name)));
        for (String item : items) {
            checklistItemRepository.save(new SafetyChecklistItem(checklist, item));
        }
        return toSiteResponse(siteRepository.save(new Site(name, checklist)));
    }

    @Transactional
    public SiteResponse renameSite(Integer siteId, String requestedName) {
        Site site = findSite(siteId);
        String name = normalizeName(requestedName);
        if (siteRepository.existsByNameIgnoreCaseAndIdNot(name, siteId)) {
            throw new InvalidRequestException("A site with that name already exists");
        }
        site.setName(name);
        return toSiteResponse(site);
    }

    @Transactional
    public SiteResponse setSiteActive(Integer siteId, boolean active) {
        Site site = findSite(siteId);
        site.setActive(active);
        return toSiteResponse(site);
    }

    /**
     * Replaces a site's checklist items. A checklist shared with other sites is copied
     * first so the edit only affects the site being edited.
     */
    @Transactional
    public ChecklistResponse updateChecklist(Integer siteId, ChecklistUpdateRequest request) {
        Site site = findSite(siteId);
        String checklistName = normalizeName(request.name());
        List<ChecklistUpdateRequest.ChecklistItemInput> inputs = request.items().stream()
                .map((input) -> new ChecklistUpdateRequest.ChecklistItemInput(
                        input.id(), normalizeItem(input.item())))
                .toList();

        SafetyChecklist checklist = site.getChecklist();
        boolean isShared = siteRepository.countByChecklist_Id(checklist.getId()) > 1;
        if (isShared) {
            checklist = checklistRepository.save(new SafetyChecklist(checklistName));
            for (ChecklistUpdateRequest.ChecklistItemInput input : inputs) {
                checklistItemRepository.save(new SafetyChecklistItem(checklist, input.item()));
            }
            site.setChecklist(checklist);
            return toChecklistResponse(checklist);
        }

        checklist.setName(checklistName);
        Map<Integer, SafetyChecklistItem> existing = new LinkedHashMap<>();
        for (SafetyChecklistItem item
                : checklistItemRepository.findByChecklist_IdOrderByIdAsc(checklist.getId())) {
            existing.put(item.getId(), item);
        }

        Set<Integer> keptIds = new HashSet<>();
        for (ChecklistUpdateRequest.ChecklistItemInput input : inputs) {
            if (input.id() == null) {
                checklistItemRepository.save(new SafetyChecklistItem(checklist, input.item()));
                continue;
            }
            SafetyChecklistItem item = existing.get(input.id());
            if (item == null || !keptIds.add(input.id())) {
                throw new InvalidRequestException("Checklist items do not belong to this site");
            }
            item.setItem(input.item());
        }
        List<SafetyChecklistItem> removed = existing.values().stream()
                .filter((item) -> !keptIds.contains(item.getId()))
                .toList();
        checklistItemRepository.deleteAll(removed);
        checklistItemRepository.flush();
        return toChecklistResponse(checklist);
    }

    private Site findSite(Integer siteId) {
        return siteRepository.findOneById(siteId)
                .orElseThrow(() -> new ResourceNotFoundException("Site not found"));
    }

    private String normalizeName(String value) {
        String name = value == null ? "" : value.strip();
        if (name.isEmpty() || name.length() > NAME_MAX_LENGTH) {
            throw new InvalidRequestException("Name must be between 1 and 150 characters");
        }
        return name;
    }

    private String normalizeItem(String value) {
        String item = value == null ? "" : value.strip();
        if (item.isEmpty()) {
            throw new InvalidRequestException("Checklist items cannot be blank");
        }
        return item;
    }

    private List<String> normalizeItems(List<String> values) {
        if (values == null || values.isEmpty()) {
            throw new InvalidRequestException("A checklist needs at least one item");
        }
        return values.stream().map(this::normalizeItem).toList();
    }

    private String checklistNameFor(String siteName) {
        String suffix = " Safety Checklist";
        String base = siteName.length() + suffix.length() > NAME_MAX_LENGTH
                ? siteName.substring(0, NAME_MAX_LENGTH - suffix.length())
                : siteName;
        return base + suffix;
    }

    private ChecklistResponse toChecklistResponse(SafetyChecklist checklist) {
        List<ChecklistItemResponse> items = checklistItemRepository
                .findByChecklist_IdOrderByIdAsc(checklist.getId())
                .stream()
                .map(item -> new ChecklistItemResponse(
                        item.getId(), item.getChecklist().getId(), item.getItem()))
                .toList();

        return new ChecklistResponse(checklist.getId(), checklist.getName(), items);
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
