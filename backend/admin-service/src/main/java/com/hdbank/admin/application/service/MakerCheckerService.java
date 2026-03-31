package com.hdbank.admin.application.service;

import com.hdbank.admin.adapter.out.persistence.entity.ConfigChangeJpaEntity;
import com.hdbank.admin.adapter.out.persistence.repository.ConfigChangeJpaRepository;
import com.hdbank.admin.application.dto.ConfigChangeRequest;
import com.hdbank.admin.application.dto.ReviewRequest;
import com.hdbank.common.dto.PageResponse;
import com.hdbank.common.exception.BusinessException;
import com.hdbank.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MakerCheckerService {

    private final ConfigChangeJpaRepository configChangeRepo;

    /**
     * Maker creates a config change request (status = PENDING).
     */
    @Transactional
    public ConfigChangeJpaEntity requestChange(ConfigChangeRequest request) {
        ConfigChangeJpaEntity entity = ConfigChangeJpaEntity.builder()
                .entityType(request.getEntityType())
                .entityId(request.getEntityId())
                .changeType(request.getChangeType())
                .oldValue(request.getOldValue())
                .newValue(request.getNewValue())
                .requestedBy(request.getRequestedBy())
                .requesterName(request.getRequesterName())
                .build();
        ConfigChangeJpaEntity saved = configChangeRepo.save(entity);
        log.info("Config change requested: {} on {} id={} by {}",
                request.getChangeType(), request.getEntityType(), request.getEntityId(),
                request.getRequesterName());
        return saved;
    }

    /**
     * Checker approves a pending change. Reviewer must not be the requester.
     * After approval, the actual change should be applied by the caller.
     */
    @Transactional
    public ConfigChangeJpaEntity approveChange(UUID changeId, ReviewRequest review) {
        ConfigChangeJpaEntity change = findPendingChange(changeId);

        if (change.getRequestedBy().equals(review.getReviewerId())) {
            throw new BusinessException("SELF_APPROVAL",
                    "Người duyệt không được trùng với người yêu cầu");
        }

        change.setStatus("APPROVED");
        change.setReviewedBy(review.getReviewerId());
        change.setReviewerName(review.getReviewerName());
        change.setReviewComment(review.getComment());
        change.setReviewedAt(Instant.now());

        ConfigChangeJpaEntity saved = configChangeRepo.save(change);
        log.info("Config change {} approved by {}", changeId, review.getReviewerName());
        return saved;
    }

    /**
     * Checker rejects a pending change with a comment.
     */
    @Transactional
    public ConfigChangeJpaEntity rejectChange(UUID changeId, ReviewRequest review) {
        ConfigChangeJpaEntity change = findPendingChange(changeId);

        change.setStatus("REJECTED");
        change.setReviewedBy(review.getReviewerId());
        change.setReviewerName(review.getReviewerName());
        change.setReviewComment(review.getComment());
        change.setReviewedAt(Instant.now());

        ConfigChangeJpaEntity saved = configChangeRepo.save(change);
        log.info("Config change {} rejected by {} - reason: {}", changeId,
                review.getReviewerName(), review.getComment());
        return saved;
    }

    /**
     * List all pending changes for the checker to review.
     */
    @Transactional(readOnly = true)
    public List<ConfigChangeJpaEntity> getPendingChanges() {
        return configChangeRepo.findByStatusOrderByCreatedAtDesc("PENDING");
    }

    /**
     * Paginated history of all config changes.
     */
    @Transactional(readOnly = true)
    public PageResponse<ConfigChangeJpaEntity> getHistory(int page, int size) {
        Page<ConfigChangeJpaEntity> result = configChangeRepo
                .findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
        return PageResponse.<ConfigChangeJpaEntity>builder()
                .content(result.getContent())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    private ConfigChangeJpaEntity findPendingChange(UUID changeId) {
        ConfigChangeJpaEntity change = configChangeRepo.findById(changeId)
                .orElseThrow(() -> new ResourceNotFoundException("ConfigChange", changeId.toString()));
        if (!"PENDING".equals(change.getStatus())) {
            throw new BusinessException("NOT_PENDING",
                    "Yêu cầu thay đổi không ở trạng thái chờ duyệt. Trạng thái hiện tại: " + change.getStatus());
        }
        return change;
    }
}
