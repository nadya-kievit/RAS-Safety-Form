package com.ras.safetyform.service;

import com.ras.safetyform.dto.SiteAssignmentResponse;
import com.ras.safetyform.dto.SiteResponse;
import com.ras.safetyform.dto.UserResponse;
import com.ras.safetyform.model.Site;
import com.ras.safetyform.model.User;
import com.ras.safetyform.repository.SiteAssignmentRepository;
import com.ras.safetyform.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final SiteAssignmentRepository assignmentRepository;

    public UserService(
            UserRepository userRepository,
            SiteAssignmentRepository assignmentRepository) {
        this.userRepository = userRepository;
        this.assignmentRepository = assignmentRepository;
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(Integer userId) {
        return toUserResponse(findUser(userId));
    }

    @Transactional(readOnly = true)
    public List<SiteAssignmentResponse> getAssignments(Integer userId) {
        findUser(userId);
        return assignmentRepository.findByUser_IdOrderByAssignmentDateDescIdAsc(userId)
                .stream()
                .map(assignment -> new SiteAssignmentResponse(
                        assignment.getId(),
                        assignment.getUser().getId(),
                        assignment.getSite().getId(),
                        assignment.getAssignmentDate(),
                        toSiteResponse(assignment.getSite())))
                .toList();
    }

    private User findUser(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
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

    private SiteResponse toSiteResponse(Site site) {
        return new SiteResponse(
                site.getId(),
                site.getName(),
                site.getChecklist().getId(),
                site.isActive(),
                site.getCreatedAt());
    }
}
