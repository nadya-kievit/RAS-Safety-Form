package com.ras.safetyform.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ras.safetyform.dto.ChecklistResponse;
import com.ras.safetyform.dto.ChecklistUpdateRequest;
import com.ras.safetyform.dto.ChecklistUpdateRequest.ChecklistItemInput;
import com.ras.safetyform.dto.SiteCreateRequest;
import com.ras.safetyform.dto.SiteResponse;
import com.ras.safetyform.model.SafetyChecklist;
import com.ras.safetyform.model.SafetyChecklistItem;
import com.ras.safetyform.model.Site;
import com.ras.safetyform.repository.SafetyChecklistItemRepository;
import com.ras.safetyform.repository.SafetyChecklistRepository;
import com.ras.safetyform.repository.SiteRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SiteServiceTest {

    private final SiteRepository siteRepository = mock(SiteRepository.class);
    private final SafetyChecklistRepository checklistRepository = mock(SafetyChecklistRepository.class);
    private final SafetyChecklistItemRepository itemRepository =
            mock(SafetyChecklistItemRepository.class);
    private final SiteService service =
            new SiteService(siteRepository, checklistRepository, itemRepository);

    private final Site site = mock(Site.class);
    private final SafetyChecklist checklist = mock(SafetyChecklist.class);

    @BeforeEach
    void configureSite() {
        when(checklist.getId()).thenReturn(4);
        when(checklist.getName()).thenReturn("Kestrel Safety Checklist");
        when(site.getId()).thenReturn(3);
        when(site.getName()).thenReturn("Kestrel Ridge");
        when(site.isActive()).thenReturn(true);
        when(site.getChecklist()).thenReturn(checklist);
        when(site.getCreatedAt()).thenReturn(Instant.parse("2026-01-01T00:00:00Z"));
        when(siteRepository.findOneById(3)).thenReturn(Optional.of(site));
        when(checklistRepository.save(any(SafetyChecklist.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(siteRepository.save(any(Site.class))).thenAnswer(invocation -> {
            Site saved = invocation.getArgument(0);
            return saved;
        });
    }

    @Test
    void createsSiteWithItsOwnChecklist() {
        SiteResponse response = service.createSite(
                new SiteCreateRequest(" Harbour View ", List.of(" Hard hat worn ", "Vest worn")));

        assertEquals("Harbour View", response.name());
        assertEquals(true, response.active());
        ArgumentCaptor<SafetyChecklist> savedChecklist = ArgumentCaptor.forClass(SafetyChecklist.class);
        verify(checklistRepository).save(savedChecklist.capture());
        assertEquals("Harbour View Safety Checklist", savedChecklist.getValue().getName());
        ArgumentCaptor<SafetyChecklistItem> items = ArgumentCaptor.forClass(SafetyChecklistItem.class);
        verify(itemRepository, org.mockito.Mockito.times(2)).save(items.capture());
        assertEquals(
                List.of("Hard hat worn", "Vest worn"),
                items.getAllValues().stream().map(SafetyChecklistItem::getItem).toList());
    }

    @Test
    void rejectsDuplicateSiteNames() {
        when(siteRepository.existsByNameIgnoreCase("Kestrel Ridge")).thenReturn(true);

        assertThrows(
                InvalidRequestException.class,
                () -> service.createSite(new SiteCreateRequest("Kestrel Ridge", List.of("Item"))));
        verify(siteRepository, never()).save(any(Site.class));
    }

    @Test
    void rejectsSiteWithoutChecklistItems() {
        assertThrows(
                InvalidRequestException.class,
                () -> service.createSite(new SiteCreateRequest("New site", List.of(" "))));
    }

    @Test
    void renamesSiteAndRejectsNamesUsedByAnotherSite() {
        SiteResponse renamed = service.renameSite(3, "  Kestrel Heights ");
        verify(site).setName("Kestrel Heights");
        assertEquals(3, renamed.id());

        when(siteRepository.existsByNameIgnoreCaseAndIdNot("Harbour View", 3)).thenReturn(true);
        assertThrows(InvalidRequestException.class, () -> service.renameSite(3, "Harbour View"));
    }

    @Test
    void activatesAndDeactivatesSites() {
        service.setSiteActive(3, false);
        verify(site).setActive(false);

        service.setSiteActive(3, true);
        verify(site).setActive(true);
    }

    @Test
    void unknownSitesAreNotFound() {
        assertThrows(ResourceNotFoundException.class, () -> service.renameSite(99, "Name"));
        assertThrows(ResourceNotFoundException.class, () -> service.setSiteActive(99, false));
    }

    @Test
    void updatesAddsAndRemovesChecklistItemsInPlace() {
        SafetyChecklistItem keep = item(10, "Hard hat");
        SafetyChecklistItem drop = item(11, "Old item");
        when(siteRepository.countByChecklist_Id(4)).thenReturn(1L);
        when(itemRepository.findByChecklist_IdOrderByIdAsc(4))
                .thenReturn(List.of(keep, drop))
                .thenReturn(List.of(keep));

        ChecklistResponse response = service.updateChecklist(3, new ChecklistUpdateRequest(
                "Updated checklist",
                List.of(new ChecklistItemInput(10, " Hard hat worn "), new ChecklistItemInput(null, "Vest"))));

        verify(checklist).setName("Updated checklist");
        verify(keep).setItem("Hard hat worn");
        ArgumentCaptor<SafetyChecklistItem> added = ArgumentCaptor.forClass(SafetyChecklistItem.class);
        verify(itemRepository).save(added.capture());
        assertEquals("Vest", added.getValue().getItem());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<SafetyChecklistItem>> removed = ArgumentCaptor.forClass(Iterable.class);
        verify(itemRepository).deleteAll(removed.capture());
        List<SafetyChecklistItem> removedItems = new ArrayList<>();
        removed.getValue().forEach(removedItems::add);
        assertEquals(List.of(drop), removedItems);
        assertEquals(4, response.id());
    }

    @Test
    void copiesAChecklistSharedWithOtherSitesInsteadOfEditingIt() {
        when(siteRepository.countByChecklist_Id(4)).thenReturn(3L);

        service.updateChecklist(3, new ChecklistUpdateRequest(
                "Kestrel only", List.of(new ChecklistItemInput(10, "Hard hat"))));

        verify(checklist, never()).setName(any());
        verify(itemRepository, never()).deleteAll(any());
        ArgumentCaptor<SafetyChecklist> copy = ArgumentCaptor.forClass(SafetyChecklist.class);
        verify(site).setChecklist(copy.capture());
        assertEquals("Kestrel only", copy.getValue().getName());
    }

    @Test
    void rejectsChecklistItemsThatBelongToAnotherChecklist() {
        when(siteRepository.countByChecklist_Id(4)).thenReturn(1L);
        List<SafetyChecklistItem> items = List.of(item(10, "Hard hat"));
        when(itemRepository.findByChecklist_IdOrderByIdAsc(4)).thenReturn(items);

        assertThrows(
                InvalidRequestException.class,
                () -> service.updateChecklist(3, new ChecklistUpdateRequest(
                        "Checklist", List.of(new ChecklistItemInput(999, "Foreign item")))));
        verify(itemRepository, never()).deleteAll(any());
    }

    private SafetyChecklistItem item(int id, String text) {
        SafetyChecklistItem item = mock(SafetyChecklistItem.class);
        when(item.getId()).thenReturn(id);
        when(item.getItem()).thenReturn(text);
        when(item.getChecklist()).thenReturn(checklist);
        return item;
    }
}
