package com.omjadon.contractanalyzer.review;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavedReviewRepository
        extends JpaRepository<SavedReview, UUID> {

    Page<SavedReview> findByOwner_IdOrderByCreatedAtDesc(
            UUID ownerId,
            Pageable pageable
    );

    Optional<SavedReview> findByIdAndOwner_Id(
            UUID reviewId,
            UUID ownerId
    );
}