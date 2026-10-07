package com.ras.safetyform.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ras.safetyform.dto.PhotoResponse;
import com.ras.safetyform.dto.SafetyFormCreateRequest;
import com.ras.safetyform.model.Photo;
import com.ras.safetyform.model.SafetyForm;
import com.ras.safetyform.model.User;
import com.ras.safetyform.repository.PhotoRepository;
import com.ras.safetyform.repository.SafetyFormRepository;
import com.ras.safetyform.repository.SiteRepository;
import com.ras.safetyform.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
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
    private final PhotoStorageService photoStorageService = mock(PhotoStorageService.class);
    private final SafetyFormService service = new SafetyFormService(
            formRepository,
            userRepository,
            siteRepository,
            photoRepository,
            photoStorageService);

    private final SafetyForm form = mock(SafetyForm.class);
    private final User owner = mock(User.class);

    @BeforeEach
    void configureFormOwner() {
        when(formRepository.findById(21)).thenReturn(Optional.of(form));
        when(form.getId()).thenReturn(21);
        when(form.getUser()).thenReturn(owner);
        when(owner.getId()).thenReturn(7);
        when(owner.isActive()).thenReturn(true);
        when(owner.getRole()).thenReturn("framer");
        when(userRepository.findById(7)).thenReturn(Optional.of(owner));
        when(photoRepository.saveAndFlush(any(Photo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(photoStorageService.createSignedUrl(anyString()))
                .thenReturn("https://example.test/signed-photo");
    }

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
    void rejectsFileWhoseContentsDoNotMatchItsMimeType() {
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
        org.junit.jupiter.api.Assertions.assertTrue(
                deletedPath.getValue().startsWith("safety-forms/21/"));
    }

    @Test
    void deniesAnotherFramerAccessToPhotos() {
        User otherFramer = mock(User.class);
        when(otherFramer.isActive()).thenReturn(true);
        when(otherFramer.getRole()).thenReturn("framer");
        when(userRepository.findById(8)).thenReturn(Optional.of(otherFramer));

        assertThrows(
                AuthorizationException.class,
                () -> service.getPhotos(21, 8));
    }

    @Test
    void rejectsFutureFormDateAndTimeBeforeSaving() {
        SafetyFormCreateRequest request = new SafetyFormCreateRequest(
                7,
                3,
                LocalDateTime.now().plusDays(1),
                null);

        assertThrows(InvalidRequestException.class, () -> service.createForm(request));

        verify(formRepository, never()).saveAndFlush(any(SafetyForm.class));
    }

    @Test
    void filtersThroughTheEntireSelectedEndDate() {
        LocalDateTime startOfRange = LocalDateTime.of(2026, 10, 1, 0, 0);
        LocalDateTime endExclusive = LocalDateTime.of(2026, 10, 8, 0, 0);

        service.getForms(3, 7, startOfRange.toLocalDate(), endExclusive.minusDays(1).toLocalDate());

        verify(formRepository).findAllFiltered(3, 7, startOfRange, endExclusive);
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
}
