package com.ras.safetyform.service;

import com.ras.safetyform.dto.PhotoResponse;
import com.ras.safetyform.dto.SafetyFormCreateRequest;
import com.ras.safetyform.dto.SafetyFormResponse;
import com.ras.safetyform.dto.SiteResponse;
import com.ras.safetyform.dto.UserResponse;
import com.ras.safetyform.model.Photo;
import com.ras.safetyform.model.SafetyForm;
import com.ras.safetyform.model.Site;
import com.ras.safetyform.model.User;
import com.ras.safetyform.repository.PhotoRepository;
import com.ras.safetyform.repository.SafetyFormRepository;
import com.ras.safetyform.repository.SiteRepository;
import com.ras.safetyform.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class SafetyFormService {

    private static final int MAX_PHOTOS = 5;
    private static final long MAX_PHOTO_SIZE = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif");

    private final SafetyFormRepository formRepository;
    private final UserRepository userRepository;
    private final SiteRepository siteRepository;
    private final PhotoRepository photoRepository;
    private final PhotoStorageService photoStorageService;

    public SafetyFormService(
            SafetyFormRepository formRepository,
            UserRepository userRepository,
            SiteRepository siteRepository,
            PhotoRepository photoRepository,
            PhotoStorageService photoStorageService) {
        this.formRepository = formRepository;
        this.userRepository = userRepository;
        this.siteRepository = siteRepository;
        this.photoRepository = photoRepository;
        this.photoStorageService = photoStorageService;
    }

    @Transactional
    public SafetyFormResponse createForm(SafetyFormCreateRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Site site = siteRepository.findOneById(request.siteId())
                .orElseThrow(() -> new ResourceNotFoundException("Site not found"));
        LocalDateTime now = LocalDateTime.now();
        SafetyForm form = new SafetyForm(
                user,
                site,
                request.formDate(),
                request.notes(),
                now,
                now);
        return toFormResponse(formRepository.save(form));
    }

    @Transactional(readOnly = true)
    public SafetyFormResponse getForm(Integer formId) {
        return toFormResponse(findForm(formId));
    }

    @Transactional(readOnly = true)
    public List<SafetyFormResponse> getForms(
            Integer siteId,
            Integer userId,
            LocalDate startDate,
            LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new InvalidRequestException("start_date must be on or before end_date");
        }
        return formRepository.findAllFiltered(siteId, userId, startDate, endDate)
                .stream()
                .map(this::toFormResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SafetyFormResponse> getFormsForUser(Integer userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found");
        }
        return formRepository.findByUser_IdOrderByFormDateDescSubmittedAtDesc(userId)
                .stream()
                .map(this::toFormResponse)
                .toList();
    }

    @Transactional
    public List<PhotoResponse> uploadPhotos(
            Integer formId,
            Integer viewerId,
            List<MultipartFile> files) {
        SafetyForm form = findFormForViewer(formId, viewerId);
        validatePhotoList(formId, files);
        List<String> uploadedPaths = new ArrayList<>();

        try {
            List<PhotoResponse> responses = new ArrayList<>();
            for (MultipartFile file : files) {
                ValidatedPhoto validated = validatePhoto(file);
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
                        LocalDateTime.now());
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

    private SafetyForm findFormForViewer(Integer formId, Integer viewerId) {
        SafetyForm form = findForm(formId);
        User viewer = userRepository.findById(viewerId)
                .orElseThrow(() -> new AuthenticationException("Authentication required"));
        boolean ownsForm = form.getUser().getId().equals(viewerId);
        if (!viewer.isActive() || (!ownsForm && !"admin".equals(viewer.getRole()))) {
            throw new AuthorizationException("You are not authorized to access these photos");
        }
        return form;
    }

    private void validatePhotoList(Integer formId, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new InvalidRequestException("Select at least one photo");
        }
        if (files.size() > MAX_PHOTOS) {
            throw new InvalidRequestException("A maximum of 5 photos can be uploaded");
        }
        if (photoRepository.countBySafetyForm_Id(formId) + files.size() > MAX_PHOTOS) {
            throw new InvalidRequestException("A safety form can have a maximum of 5 photos");
        }
    }

    private ValidatedPhoto validatePhoto(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidRequestException("Photos cannot be empty");
        }
        if (file.getSize() > MAX_PHOTO_SIZE) {
            throw new InvalidRequestException("Each photo must be 10 MB or smaller");
        }

        String contentType = file.getContentType() == null
                ? ""
                : file.getContentType().toLowerCase(Locale.ROOT);
        if (!ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new InvalidRequestException("Only JPEG, PNG, WebP, and GIF photos are supported");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank() || filename.length() > 255) {
            throw new InvalidRequestException("Photo filenames must be between 1 and 255 characters");
        }

        try {
            byte[] content = file.getBytes();
            String detectedType = detectImageType(content);
            if (!contentType.equals(detectedType)) {
                throw new InvalidRequestException("Photo contents do not match the declared file type");
            }
            return new ValidatedPhoto(
                    filename,
                    contentType,
                    extensionFor(contentType),
                    content);
        } catch (java.io.IOException exception) {
            throw new InvalidRequestException("Could not read uploaded photo");
        }
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
        throw new InvalidRequestException("The uploaded file is not a supported image");
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
                form.getUpdatedAt(),
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
