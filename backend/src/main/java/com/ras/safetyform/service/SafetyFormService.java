package com.ras.safetyform.service;

import com.ras.safetyform.dto.PhotoResponse;
import com.ras.safetyform.dto.SafetyFormCreateRequest;
import com.ras.safetyform.dto.SafetyFormResponse;
import com.ras.safetyform.dto.SiteResponse;
import com.ras.safetyform.dto.UserResponse;
import com.ras.safetyform.model.Photo;
import com.ras.safetyform.model.SafetyChecklistItem;
import com.ras.safetyform.model.SafetyForm;
import com.ras.safetyform.model.Site;
import com.ras.safetyform.model.User;
import com.ras.safetyform.repository.PhotoRepository;
import com.ras.safetyform.repository.SafetyChecklistItemRepository;
import com.ras.safetyform.repository.SafetyFormRepository;
import com.ras.safetyform.repository.SiteRepository;
import com.ras.safetyform.repository.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class SafetyFormService {

    private static final int MAX_PHOTOS = 5;
    private static final long MAX_PHOTO_SIZE = 10L * 1024 * 1024;
    // Tolerates a phone clock that runs slightly ahead of the server.
    private static final Duration FUTURE_DATE_TOLERANCE = Duration.ofMinutes(5);
    private static final Set<String> ALLOWED_STATUSES = Set.of(
            SafetyForm.STATUS_SUBMITTED,
            SafetyForm.STATUS_REVIEWED);

    private final SafetyFormRepository formRepository;
    private final UserRepository userRepository;
    private final SiteRepository siteRepository;
    private final PhotoRepository photoRepository;
    private final SafetyChecklistItemRepository checklistItemRepository;
    private final PhotoStorageService photoStorageService;

    public SafetyFormService(
            SafetyFormRepository formRepository,
            UserRepository userRepository,
            SiteRepository siteRepository,
            PhotoRepository photoRepository,
            SafetyChecklistItemRepository checklistItemRepository,
            PhotoStorageService photoStorageService) {
        this.formRepository = formRepository;
        this.userRepository = userRepository;
        this.siteRepository = siteRepository;
        this.photoRepository = photoRepository;
        this.checklistItemRepository = checklistItemRepository;
        this.photoStorageService = photoStorageService;
    }

    @Transactional
    public SafetyFormResponse submitForm(
            SafetyFormCreateRequest request,
            Integer authenticatedUserId,
            List<MultipartFile> files) {
        if (!request.userId().equals(authenticatedUserId)) {
            throw new AuthorizationException("You can only submit safety forms for your own account");
        }
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!user.isActive()) {
            throw new AuthorizationException("Your account has been deactivated");
        }
        Site site = siteRepository.findOneById(request.siteId())
                .orElseThrow(() -> new ResourceNotFoundException("Site not found"));
        if (!site.isActive()) {
            throw new InvalidRequestException("This site is no longer active");
        }

        validateFormDateTime(request);
        validateChecklistConfirmed(site, request.checkedItemIds());
        List<ValidatedPhoto> photos = validatePhotos(0, files);

        SafetyForm form = formRepository.saveAndFlush(new SafetyForm(
                user,
                site,
                request.formDate(),
                request.notes(),
                Instant.now()));
        storePhotos(form, photos);
        return toFormResponse(form);
    }

    private void validateFormDateTime(SafetyFormCreateRequest request) {
        if (request.formDate().isAfter(Instant.now().plus(FUTURE_DATE_TOLERANCE))) {
            throw new InvalidRequestException("The safety form date and time cannot be in the future");
        }
    }

    private void validateChecklistConfirmed(Site site, List<Integer> checkedItemIds) {
        Set<Integer> requiredIds = new HashSet<>();
        for (SafetyChecklistItem item : checklistItemRepository
                .findByChecklist_IdOrderByIdAsc(site.getChecklist().getId())) {
            requiredIds.add(item.getId());
        }
        if (requiredIds.isEmpty()) {
            throw new InvalidRequestException("This site does not have a checklist configured");
        }
        Set<Integer> confirmedIds = checkedItemIds == null
                ? Set.of()
                : new HashSet<>(checkedItemIds);
        if (!confirmedIds.containsAll(requiredIds)) {
            throw new InvalidRequestException("Every checklist item must be confirmed before submission");
        }
    }

    @Transactional(readOnly = true)
    public SafetyFormResponse getForm(Integer formId, Integer viewerId) {
        return toFormResponse(findFormForViewer(formId, viewerId));
    }

    /** Administrators see every submission; framers see only their own. */
    @Transactional(readOnly = true)
    public List<SafetyFormResponse> getForms(
            Integer viewerId,
            Integer siteId,
            Integer userId,
            LocalDate startDate,
            LocalDate endDate,
            ZoneId zone) {
        User viewer = findActiveViewer(viewerId);
        Integer effectiveUserId = userId;
        if (!isAdmin(viewer)) {
            if (userId != null && !userId.equals(viewerId)) {
                throw new AuthorizationException("You can only view your own safety forms");
            }
            effectiveUserId = viewerId;
        }
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new InvalidRequestException("start_date must be on or before end_date");
        }
        Instant startInstant = startDate == null ? null : startDate.atStartOfDay(zone).toInstant();
        Instant endInstantExclusive = endDate == null
                ? null
                : endDate.plusDays(1).atStartOfDay(zone).toInstant();
        List<SafetyForm> forms;
        if (startInstant != null && endInstantExclusive != null) {
            forms = formRepository.findAllFilteredBetweenDates(
                    siteId, effectiveUserId, startInstant, endInstantExclusive);
        } else if (startInstant != null) {
            forms = formRepository.findAllFilteredFromDate(siteId, effectiveUserId, startInstant);
        } else if (endInstantExclusive != null) {
            forms = formRepository.findAllFilteredUntilDate(
                    siteId, effectiveUserId, endInstantExclusive);
        } else {
            forms = formRepository.findAllFiltered(siteId, effectiveUserId);
        }
        return forms
                .stream()
                .map(this::toFormResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SafetyFormResponse> getFormsForUser(Integer userId, Integer viewerId) {
        User viewer = findActiveViewer(viewerId);
        if (!userId.equals(viewerId) && !isAdmin(viewer)) {
            throw new AuthorizationException("You can only view your own safety forms");
        }
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found");
        }
        return formRepository.findByUser_IdOrderByFormDateDescSubmittedAtDesc(userId)
                .stream()
                .map(this::toFormResponse)
                .toList();
    }

    @Transactional
    public SafetyFormResponse updateStatus(Integer formId, Integer viewerId, String status) {
        User viewer = findActiveViewer(viewerId);
        if (!isAdmin(viewer)) {
            throw new AuthorizationException("Administrator access required");
        }
        if (!ALLOWED_STATUSES.contains(status)) {
            throw new InvalidRequestException("Status must be one of: submitted, reviewed");
        }
        SafetyForm form = findForm(formId);
        form.setStatus(status);
        return toFormResponse(form);
    }

    @Transactional
    public List<PhotoResponse> uploadPhotos(
            Integer formId,
            Integer viewerId,
            List<MultipartFile> files) {
        SafetyForm form = findFormForViewer(formId, viewerId);
        List<ValidatedPhoto> photos = validatePhotos(
                photoRepository.countBySafetyForm_Id(formId),
                files);
        return storePhotos(form, photos);
    }

    private List<PhotoResponse> storePhotos(
            SafetyForm form,
            List<ValidatedPhoto> photos) {
        Integer formId = form.getId();
        List<String> uploadedPaths = new ArrayList<>();

        try {
            List<PhotoResponse> responses = new ArrayList<>();
            for (ValidatedPhoto validated : photos) {
                String objectPath = "safety-forms/"
                        + formId
                        + "/"
                        + UUID.randomUUID()
                        + validated.extension();
                photoStorageService.upload(
                        objectPath,
                        validated.content(),
                        validated.contentType());
                uploadedPaths.add(objectPath);

                Photo photo = new Photo(
                        form,
                        objectPath,
                        validated.filename(),
                        validated.contentType(),
                        validated.content().length,
                        Instant.now());
                Photo saved = photoRepository.saveAndFlush(photo);
                responses.add(toPhotoResponse(saved));
            }
            return responses;
        } catch (RuntimeException exception) {
            deleteUploadedPhotos(uploadedPaths);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public List<PhotoResponse> getPhotos(Integer formId, Integer viewerId) {
        findFormForViewer(formId, viewerId);
        return photoRepository.findBySafetyForm_IdOrderByCreatedAtAscIdAsc(formId)
                .stream()
                .map(this::toPhotoResponse)
                .toList();
    }

    private SafetyForm findForm(Integer formId) {
        return formRepository.findById(formId)
                .orElseThrow(() -> new ResourceNotFoundException("Safety form not found"));
    }

    private User findActiveViewer(Integer viewerId) {
        User viewer = userRepository.findById(viewerId)
                .orElseThrow(() -> new AuthenticationException("Authentication required"));
        if (!viewer.isActive()) {
            throw new AuthenticationException("Your account has been deactivated");
        }
        return viewer;
    }

    private boolean isAdmin(User user) {
        return "admin".equals(user.getRole());
    }

    /** Returns the form to its owner or to an administrator, and hides it from everyone else. */
    private SafetyForm findFormForViewer(Integer formId, Integer viewerId) {
        SafetyForm form = findForm(formId);
        User viewer = findActiveViewer(viewerId);
        boolean ownsForm = form.getUser().getId().equals(viewerId);
        if (!ownsForm && !isAdmin(viewer)) {
            throw new AuthorizationException("You are not authorized to access this safety form");
        }
        return form;
    }

    private List<ValidatedPhoto> validatePhotos(long existingPhotoCount, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new InvalidRequestException("Select at least one photo");
        }
        if (files.size() > MAX_PHOTOS) {
            throw new InvalidRequestException("A maximum of 5 photos can be uploaded");
        }
        if (existingPhotoCount + files.size() > MAX_PHOTOS) {
            throw new InvalidRequestException("A safety form can have a maximum of 5 photos");
        }
        return files.stream().map(this::validatePhoto).toList();
    }

    private ValidatedPhoto validatePhoto(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidRequestException("Photos cannot be empty");
        }
        if (file.getSize() > MAX_PHOTO_SIZE) {
            throw new InvalidRequestException("Each photo must be 10 MB or smaller");
        }

        try {
            byte[] content = file.getBytes();
            // The file contents decide the type: phones and browsers often send a blank or
            // non-standard declared type (for example image/jpg) for perfectly valid photos.
            String contentType = detectImageType(content);
            String extension = extensionFor(contentType);
            return new ValidatedPhoto(
                    filenameFor(file.getOriginalFilename(), extension),
                    contentType,
                    extension,
                    content);
        } catch (java.io.IOException exception) {
            throw new InvalidRequestException("Could not read uploaded photo");
        }
    }

    private String filenameFor(String originalFilename, String extension) {
        String filename = originalFilename == null ? "" : originalFilename.strip();
        if (filename.isEmpty()) {
            return "photo" + extension;
        }
        if (filename.length() > 255) {
            throw new InvalidRequestException("Photo filenames must be 255 characters or fewer");
        }
        return filename;
    }

    private String detectImageType(byte[] content) {
        if (startsWith(content, new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff})) {
            return "image/jpeg";
        }
        if (startsWith(content, new byte[] {
                (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a})) {
            return "image/png";
        }
        if (content.length >= 12
                && startsWith(content, "RIFF".getBytes(java.nio.charset.StandardCharsets.US_ASCII))
                && Arrays.equals(
                        Arrays.copyOfRange(content, 8, 12),
                        "WEBP".getBytes(java.nio.charset.StandardCharsets.US_ASCII))) {
            return "image/webp";
        }
        if (startsWith(content, "GIF87a".getBytes(java.nio.charset.StandardCharsets.US_ASCII))
                || startsWith(content, "GIF89a".getBytes(java.nio.charset.StandardCharsets.US_ASCII))) {
            return "image/gif";
        }
        if (isHeif(content)) {
            throw new InvalidRequestException(
                    "HEIC/HEIF photos are not supported. Choose a JPEG, PNG, WebP, or GIF photo.");
        }
        throw new InvalidRequestException("The uploaded file is not a supported image");
    }

    private boolean isHeif(byte[] content) {
        if (content.length < 12
                || !"ftyp".equals(new String(
                        Arrays.copyOfRange(content, 4, 8),
                        java.nio.charset.StandardCharsets.US_ASCII))) {
            return false;
        }
        String brand = new String(
                Arrays.copyOfRange(content, 8, 12),
                java.nio.charset.StandardCharsets.US_ASCII);
        return Set.of("heic", "heix", "hevc", "hevx", "mif1", "msf1").contains(brand);
    }

    private boolean startsWith(byte[] content, byte[] signature) {
        return content.length >= signature.length
                && Arrays.equals(Arrays.copyOf(content, signature.length), signature);
    }

    private String extensionFor(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> throw new InvalidRequestException("Unsupported photo type");
        };
    }

    private void deleteUploadedPhotos(List<String> uploadedPaths) {
        for (String uploadedPath : uploadedPaths) {
            try {
                photoStorageService.delete(uploadedPath);
            } catch (StorageException ignored) {
                // Preserve the original failure; orphan cleanup can be retried operationally.
            }
        }
    }

    private SafetyFormResponse toFormResponse(SafetyForm form) {
        User user = form.getUser();
        Site site = form.getSite();
        UserResponse userResponse = new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getUsername(),
                user.getRole(),
                user.isMustChangePassword(),
                user.isActive(),
                user.getCreatedAt());
        SiteResponse siteResponse = new SiteResponse(
                site.getId(),
                site.getName(),
                site.getChecklist().getId(),
                site.isActive(),
                site.getCreatedAt());
        return new SafetyFormResponse(
                form.getId(),
                user.getId(),
                site.getId(),
                form.getFormDate(),
                form.getNotes(),
                form.getSubmittedAt(),
                form.getStatus(),
                userResponse,
                siteResponse);
    }

    private PhotoResponse toPhotoResponse(Photo photo) {
        return new PhotoResponse(
                photo.getId(),
                photo.getSafetyForm().getId(),
                photo.getStoragePath(),
                photo.getFilename(),
                photo.getMimeType(),
                photo.getFileSize(),
                photoStorageService.createSignedUrl(photo.getStoragePath()),
                photo.getCreatedAt());
    }

    private record ValidatedPhoto(
            String filename,
            String contentType,
            String extension,
            byte[] content) {
    }
}
