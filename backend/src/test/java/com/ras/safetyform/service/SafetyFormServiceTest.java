package com.ras.safetyform.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ras.safetyform.dto.PhotoResponse;
import com.ras.safetyform.dto.SafetyFormCreateRequest;
import com.ras.safetyform.dto.SafetyFormResponse;
import com.ras.safetyform.model.Photo;
import com.ras.safetyform.model.SafetyChecklist;
import com.ras.safetyform.model.SafetyChecklistItem;
import com.ras.safetyform.model.SafetyForm;
import com.ras.safetyform.model.Site;
import com.ras.safetyform.model.User;
import com.ras.safetyform.repository.PhotoRepository;
import com.ras.safetyform.repository.SafetyChecklistItemRepository;
import com.ras.safetyform.repository.SafetyFormRepository;
import com.ras.safetyform.repository.SiteRepository;
import com.ras.safetyform.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

class SafetyFormServiceTest {

    private final SafetyFormRepository formRepository = mock(SafetyFormRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final SiteRepository siteRepository = mock(SiteRepository.class);
    private final PhotoRepository photoRepository = mock(PhotoRepository.class);
    private final SafetyChecklistItemRepository checklistItemRepository =
            mock(SafetyChecklistItemRepository.class);
    private final PhotoStorageService photoStorageService = mock(PhotoStorageService.class);
    private final SafetyFormService service = new SafetyFormService(
            formRepository,
            userRepository,
            siteRepository,
            photoRepository,
            checklistItemRepository,
            photoStorageService);

    private final SafetyForm form = mock(SafetyForm.class);
    private final User owner = mock(User.class);
    private final User admin = mock(User.class);
    private final User otherFramer = mock(User.class);
    private final Site site = mock(Site.class);

    @BeforeEach
    void configureFixtures() {
        when(formRepository.findById(21)).thenReturn(Optional.of(form));
        when(form.getId()).thenReturn(21);
        when(form.getUser()).thenReturn(owner);
        when(owner.getId()).thenReturn(7);
        when(owner.isActive()).thenReturn(true);
        when(owner.getRole()).thenReturn("framer");
        when(userRepository.findById(7)).thenReturn(Optional.of(owner));
        when(userRepository.existsById(7)).thenReturn(true);

        when(admin.isActive()).thenReturn(true);
        when(admin.getRole()).thenReturn("admin");
        when(userRepository.findById(1)).thenReturn(Optional.of(admin));

        when(otherFramer.isActive()).thenReturn(true);
        when(otherFramer.getRole()).thenReturn("framer");
        when(userRepository.findById(8)).thenReturn(Optional.of(otherFramer));

        when(photoRepository.saveAndFlush(any(Photo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(photoStorageService.createSignedUrl(anyString()))
                .thenReturn("https://example.test/signed-photo");

        SafetyChecklist checklist = mock(SafetyChecklist.class);
        when(checklist.getId()).thenReturn(4);
        when(site.getId()).thenReturn(3);
        when(site.isActive()).thenReturn(true);
        when(site.getChecklist()).thenReturn(checklist);
        when(siteRepository.findOneById(3)).thenReturn(Optional.of(site));
        List<SafetyChecklistItem> items = List.of(checklistItem(10), checklistItem(11));
        when(checklistItemRepository.findByChecklist_IdOrderByIdAsc(4)).thenReturn(items);
        when(formRepository.saveAndFlush(any(SafetyForm.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    // --- photo uploads -------------------------------------------------------------

    @Test
    void uploadsValidatedImageAndReturnsSignedUrl() throws Exception {
        MockMultipartFile image = png("job-site.png");

        List<PhotoResponse> result = service.uploadPhotos(21, 7, List.of(image));

        assertEquals(1, result.size());
        assertEquals("job-site.png", result.getFirst().filename());
        assertEquals("image/png", result.getFirst().mimeType());
        assertEquals("https://example.test/signed-photo", result.getFirst().viewUrl());
        verify(photoStorageService).upload(
                org.mockito.ArgumentMatchers.matches(
                        "safety-forms/21/[0-9a-f-]+\\.png"),
                eq(image.getBytes()),
                eq("image/png"));
    }

    @Test
    void rejectsFileThatIsNotAnImage() {
        MockMultipartFile fakeImage = new MockMultipartFile(
                "photos",
                "fake.png",
                "image/png",
                "not an image".getBytes(StandardCharsets.UTF_8));

        assertThrows(
                InvalidRequestException.class,
                () -> service.uploadPhotos(21, 7, List.of(fakeImage)));

        verify(photoStorageService, never()).upload(anyString(), any(), anyString());
    }

    @Test
    void trustsFileContentsOverBlankOrNonStandardDeclaredTypes() {
        MockMultipartFile blankType = new MockMultipartFile(
                "photos", "IMG_0001.JPG", "", jpegBytes());
        MockMultipartFile legacyJpegType = new MockMultipartFile(
                "photos", "IMG_0002.jpg", "image/jpg", jpegBytes());

        List<PhotoResponse> result = service.uploadPhotos(
                21, 7, List.of(blankType, legacyJpegType));

        assertEquals(List.of("image/jpeg", "image/jpeg"),
                result.stream().map(PhotoResponse::mimeType).toList());
        verify(photoStorageService, org.mockito.Mockito.times(2))
                .upload(anyString(), any(), eq("image/jpeg"));
    }

    @Test
    void namesPhotosWithBlankFilenames() {
        MockMultipartFile unnamed = new MockMultipartFile("photos", "", "image/jpeg", jpegBytes());

        List<PhotoResponse> result = service.uploadPhotos(21, 7, List.of(unnamed));

        assertEquals("photo.jpg", result.getFirst().filename());
    }

    @Test
    void explainsThatHeicPhotosAreNotSupported() {
        byte[] heic = new byte[] {0, 0, 0, 24, 'f', 't', 'y', 'p', 'h', 'e', 'i', 'c', 0, 0};
        MockMultipartFile image = new MockMultipartFile("photos", "IMG.HEIC", "image/heic", heic);

        InvalidRequestException exception = assertThrows(
                InvalidRequestException.class,
                () -> service.uploadPhotos(21, 7, List.of(image)));

        assertTrue(exception.getMessage().contains("HEIC"));
    }

    @Test
    void removesPreviouslyUploadedObjectsWhenBatchFails() {
        MockMultipartFile first = png("first.png");
        MockMultipartFile second = png("second.png");
        doNothing()
                .doThrow(new StorageException("storage unavailable"))
                .when(photoStorageService)
                .upload(anyString(), any(), eq("image/png"));

        assertThrows(
                StorageException.class,
                () -> service.uploadPhotos(21, 7, List.of(first, second)));

        ArgumentCaptor<String> deletedPath = ArgumentCaptor.forClass(String.class);
        verify(photoStorageService).delete(deletedPath.capture());
        assertTrue(deletedPath.getValue().startsWith("safety-forms/21/"));
    }

    @Test
    void rejectsMoreThanFivePhotosAcrossUploads() {
        when(photoRepository.countBySafetyForm_Id(21)).thenReturn(4L);

        assertThrows(
                InvalidRequestException.class,
                () -> service.uploadPhotos(21, 7, List.of(png("a.png"), png("b.png"))));
    }

    // --- authorization -------------------------------------------------------------

    @Test
    void deniesAnotherFramerAccessToPhotos() {
        assertThrows(AuthorizationException.class, () -> service.getPhotos(21, 8));
    }

    @Test
    void deniesAnotherFramerAccessToTheSubmission() {
        assertThrows(AuthorizationException.class, () -> service.getForm(21, 8));
    }

    @Test
    void allowsOwnerAndAdministratorToReadTheSubmission() {
        stubFormResponseFields();

        assertEquals(21, service.getForm(21, 7).id());
        assertEquals(21, service.getForm(21, 1).id());
    }

    @Test
    void rejectsDeactivatedViewers() {
        when(owner.isActive()).thenReturn(false);

        assertThrows(AuthenticationException.class, () -> service.getForm(21, 7));
    }

    @Test
    void framersCannotRequestAnotherUsersSubmissions() {
        assertThrows(
                AuthorizationException.class,
                () -> service.getForms(8, null, 7, null, null, ZoneOffset.UTC));

        verify(formRepository, never()).findAllFiltered(any(), any());
    }

    @Test
    void framerListingsAreAlwaysLimitedToTheirOwnSubmissions() {
        service.getForms(8, 3, null, null, null, ZoneOffset.UTC);

        verify(formRepository).findAllFiltered(3, 8);
    }

    @Test
    void administratorsCanListEveryonesSubmissionsWithFilters() {
        service.getForms(1, null, 7, null, null, ZoneOffset.UTC);
        service.getForms(1, null, null, null, null, ZoneOffset.UTC);

        verify(formRepository).findAllFiltered(null, 7);
        verify(formRepository).findAllFiltered(null, null);
    }

    @Test
    void framersCanOnlyListTheirOwnSubmissionsByUser() {
        assertThrows(AuthorizationException.class, () -> service.getFormsForUser(7, 8));

        service.getFormsForUser(7, 7);
        service.getFormsForUser(7, 1);
        verify(formRepository, org.mockito.Mockito.times(2))
                .findByUser_IdOrderByFormDateDescSubmittedAtDesc(7);
    }

    @Test
    void onlyAdministratorsCanChangeSubmissionStatus() {
        assertThrows(
                AuthorizationException.class,
                () -> service.updateStatus(21, 7, "reviewed"));
        verify(form, never()).setStatus(anyString());

        stubFormResponseFields();
        service.updateStatus(21, 1, "reviewed");
        verify(form).setStatus("reviewed");
    }

    @Test
    void rejectsUnknownSubmissionStatus() {
        assertThrows(
                InvalidRequestException.class,
                () -> service.updateStatus(21, 1, "approved"));
    }

    // --- submitting a form ---------------------------------------------------------

    @Test
    void submitsFormWithConfirmedChecklistAndPhoto() {
        SafetyFormResponse response = service.submitForm(
                request(7, 3, Instant.now().minusSeconds(60), List.of(10, 11)),
                7,
                List.of(png("site.png")));

        assertEquals(7, response.userId());
        assertEquals("submitted", response.status());
        verify(formRepository).saveAndFlush(any(SafetyForm.class));
        verify(photoStorageService).upload(anyString(), any(), eq("image/png"));
    }

    @Test
    void rejectsSubmissionWithoutAnyPhotoBeforeSaving() {
        assertThrows(
                InvalidRequestException.class,
                () -> service.submitForm(
                        request(7, 3, Instant.now().minusSeconds(60), List.of(10, 11)),
                        7,
                        List.of()));
        assertThrows(
                InvalidRequestException.class,
                () -> service.submitForm(
                        request(7, 3, Instant.now().minusSeconds(60), List.of(10, 11)),
                        7,
                        null));

        verify(formRepository, never()).saveAndFlush(any(SafetyForm.class));
    }

    @Test
    void rejectsSubmissionWhenAnyChecklistItemIsUnconfirmed() {
        assertThrows(
                InvalidRequestException.class,
                () -> service.submitForm(
                        request(7, 3, Instant.now().minusSeconds(60), List.of(10)),
                        7,
                        List.of(png("site.png"))));

        verify(formRepository, never()).saveAndFlush(any(SafetyForm.class));
    }

    @Test
    void rejectsSubmissionForSiteWithoutChecklist() {
        when(checklistItemRepository.findByChecklist_IdOrderByIdAsc(4)).thenReturn(List.of());

        assertThrows(
                InvalidRequestException.class,
                () -> service.submitForm(
                        request(7, 3, Instant.now().minusSeconds(60), List.of()),
                        7,
                        List.of(png("site.png"))));
    }

    @Test
    void rejectsSubmissionForAnotherUser() {
        assertThrows(
                AuthorizationException.class,
                () -> service.submitForm(
                        request(7, 3, Instant.now().minusSeconds(60), List.of(10, 11)),
                        8,
                        List.of(png("site.png"))));
    }

    @Test
    void rejectsSubmissionFromDeactivatedUser() {
        when(owner.isActive()).thenReturn(false);

        assertThrows(
                AuthorizationException.class,
                () -> service.submitForm(
                        request(7, 3, Instant.now().minusSeconds(60), List.of(10, 11)),
                        7,
                        List.of(png("site.png"))));
        verify(formRepository, never()).saveAndFlush(any(SafetyForm.class));
    }

    @Test
    void rejectsSubmissionForInactiveSite() {
        when(site.isActive()).thenReturn(false);

        InvalidRequestException exception = assertThrows(
                InvalidRequestException.class,
                () -> service.submitForm(
                        request(7, 3, Instant.now().minusSeconds(60), List.of(10, 11)),
                        7,
                        List.of(png("site.png"))));

        assertEquals("This site is no longer active", exception.getMessage());
        verify(formRepository, never()).saveAndFlush(any(SafetyForm.class));
    }

    @Test
    void rejectsFutureFormDateAndTimeBeforeSaving() {
        assertThrows(
                InvalidRequestException.class,
                () -> service.submitForm(
                        request(7, 3, Instant.now().plusSeconds(24 * 3600), List.of(10, 11)),
                        7,
                        List.of(png("site.png"))));

        verify(formRepository, never()).saveAndFlush(any(SafetyForm.class));
    }

    // --- filters -------------------------------------------------------------------

    @Test
    void filtersThroughTheEntireSelectedEndDateInTheCallersTimeZone() {
        ZoneId zone = ZoneId.of("America/Vancouver");
        Instant startOfRange = LocalDate.of(2026, 10, 1).atStartOfDay(zone).toInstant();
        Instant endExclusive = LocalDate.of(2026, 10, 8).atStartOfDay(zone).toInstant();

        service.getForms(1, 3, 7, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 7), zone);

        verify(formRepository).findAllFilteredBetweenDates(3, 7, startOfRange, endExclusive);
    }

    @Test
    void rejectsStartDateAfterEndDate() {
        assertThrows(
                InvalidRequestException.class,
                () -> service.getForms(
                        1, null, null,
                        LocalDate.of(2026, 10, 8), LocalDate.of(2026, 10, 1),
                        ZoneOffset.UTC));
    }

    @Test
    void loadsAllFormsWithoutPassingUntypedNullDateParameters() {
        service.getForms(1, null, null, null, null, ZoneOffset.UTC);

        verify(formRepository).findAllFiltered(null, null);
    }

    // --- helpers -------------------------------------------------------------------

    private SafetyFormCreateRequest request(
            Integer userId, Integer siteId, Instant formDate, List<Integer> checkedItemIds) {
        return new SafetyFormCreateRequest(userId, siteId, formDate, null, checkedItemIds);
    }

    private SafetyChecklistItem checklistItem(int id) {
        SafetyChecklistItem item = mock(SafetyChecklistItem.class);
        when(item.getId()).thenReturn(id);
        return item;
    }

    private void stubFormResponseFields() {
        when(form.getSite()).thenReturn(site);
        when(form.getStatus()).thenReturn("submitted");
        when(owner.getFirstName()).thenReturn("Alex");
    }

    private MockMultipartFile png(String filename) {
        return new MockMultipartFile(
                "photos",
                filename,
                "image/png",
                new byte[] {
                    (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0x00
                });
    }

    private byte[] jpegBytes() {
        return new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff, (byte) 0xe0, 0x00, 0x10};
    }
}
