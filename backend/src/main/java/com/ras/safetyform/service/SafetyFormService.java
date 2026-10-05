package com.ras.safetyform.service;

import com.ras.safetyform.dto.PhotoCreateRequest;
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
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SafetyFormService {

    private final SafetyFormRepository formRepository;
    private final UserRepository userRepository;
    private final SiteRepository siteRepository;
    private final PhotoRepository photoRepository;

    public SafetyFormService(
            SafetyFormRepository formRepository,
            UserRepository userRepository,
            SiteRepository siteRepository,
            PhotoRepository photoRepository) {
        this.formRepository = formRepository;
        this.userRepository = userRepository;
        this.siteRepository = siteRepository;
        this.photoRepository = photoRepository;
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
    public PhotoResponse createPhoto(Integer formId, PhotoCreateRequest request) {
        SafetyForm form = findForm(formId);
        Photo photo = new Photo(
                form,
                request.storagePath(),
                request.filename(),
                request.mimeType(),
                request.fileSize(),
                LocalDateTime.now());
        return toPhotoResponse(photoRepository.save(photo));
    }

    @Transactional(readOnly = true)
    public List<PhotoResponse> getPhotos(Integer formId) {
        findForm(formId);
        return photoRepository.findBySafetyForm_IdOrderByCreatedAtAscIdAsc(formId)
                .stream()
                .map(this::toPhotoResponse)
                .toList();
    }

    private SafetyForm findForm(Integer formId) {
        return formRepository.findById(formId)
                .orElseThrow(() -> new ResourceNotFoundException("Safety form not found"));
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
                photo.getCreatedAt());
    }
}
