package com.omjadon.contractanalyzer.review;

import com.omjadon.contractanalyzer.account.AppUser;
import com.omjadon.contractanalyzer.account.AppUserRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class SavedReviewService {

    private final SavedReviewRepository reviews;
    private final AppUserRepository users;

    public SavedReviewService(
            SavedReviewRepository reviews,
            AppUserRepository users
    ) {
        this.reviews = reviews;
        this.users = users;
    }

    /**
     * Call this only with a review computed by the backend.
     * Do not pass review results submitted by the browser.
     */
    @Transactional
    public Summary saveComputedReview(
            Authentication authentication,
            String agreementFilename,
            String sowFilename,
            Map<String, Object> computedResult
    ) {
        AppUser owner = currentUser(authentication);

        String agreementName = validFilename(
                agreementFilename, "Agreement filename"
        );
        String sowName = validFilename(
                sowFilename, "SOW filename"
        );

        if (computedResult == null) {
            throw new IllegalArgumentException(
                    "Computed review result is required"
            );
        }

        SavedReview saved = reviews.saveAndFlush(new SavedReview(
                owner,
                agreementName,
                sowName,
                requiredString(computedResult, "agreementVersion"),
                requiredString(computedResult, "sowVersion"),
                requiredNumber(computedResult, "policyMaxDays"),
                requiredString(computedResult, "reviewStatus"),
                new LinkedHashMap<>(computedResult)
        ));

        return summary(saved);
    }

    public Page<Summary> list(Authentication authentication, int page) {
        if (page < 0) {
            throw new IllegalArgumentException(
                    "Page number must not be negative"
            );
        }

        UUID ownerId = currentUser(authentication).id();

        return reviews.findByOwner_IdOrderByCreatedAtDesc(
                ownerId,
                PageRequest.of(page, 10)
        ).map(SavedReviewService::summary);
    }

    public Optional<Detail> find(
            Authentication authentication,
            UUID reviewId
    ) {
        UUID ownerId = currentUser(authentication).id();

        return reviews.findByIdAndOwner_Id(reviewId, ownerId)
                .map(saved -> new Detail(
                        summary(saved),
                        new LinkedHashMap<>(saved.resultJson())
                ));
    }

    private AppUser currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        return users.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Account is no longer available"
                ));
    }

    private static Summary summary(SavedReview saved) {
        return new Summary(
                saved.id(),
                saved.agreementFilename(),
                saved.sowFilename(),
                saved.policyMaxCalendarDays(),
                saved.reviewStatus(),
                saved.createdAt()
        );
    }

    private static String requiredString(
            Map<String, Object> result,
            String key
    ) {
        Object value = result.get(key);

        if (!(value instanceof String text) || text.isBlank()) {
            throw new IllegalArgumentException(
                    "Computed review is missing " + key
            );
        }

        return text;
    }

    private static int requiredNumber(
            Map<String, Object> result,
            String key
    ) {
        Object value = result.get(key);

        if (!(value instanceof Integer number)) {
            throw new IllegalArgumentException(
                    "Computed review is missing " + key
            );
        }

        return number;
    }

    private static String validFilename(String name, String label) {
        if (name == null || name.isBlank() || name.length() > 255) {
            throw new IllegalArgumentException(
                    label + " must contain 1 to 255 characters"
            );
        }

        return name;
    }

    public record Summary(
            UUID id,
            String agreementFilename,
            String sowFilename,
            int policyMaxCalendarDays,
            String reviewStatus,
            Instant createdAt
    ) {
    }

    public record Detail(
            Summary summary,
            Map<String, Object> result
    ) {
    }
}