package com.omjadon.contractanalyzer.review;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Modifying(
            flushAutomatically = true,
            clearAutomatically = true
    )
    @Query("""
            delete from SavedReview review
            where review.id = :reviewId
              and review.owner.id = :ownerId
            """)
    int deleteOwnedReview(
            @Param("reviewId") UUID reviewId,
            @Param("ownerId") UUID ownerId
    );
}