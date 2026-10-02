package com.omjadon.contractanalyzer.review;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/reviews")
public class SavedReviewController {

    private final SavedReviewService savedReviews;

    public SavedReviewController(SavedReviewService savedReviews) {
        this.savedReviews = savedReviews;
    }

    @GetMapping
    public ReviewPage list(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page
    ) {
        Page<SavedReviewService.Summary> results =
                savedReviews.list(authentication, page);

        return new ReviewPage(
                results.getContent(),
                results.getNumber(),
                results.getTotalPages(),
                results.getTotalElements()
        );
    }

    @GetMapping("/{id}")
    public SavedReviewService.Detail get(
            Authentication authentication,
            @PathVariable UUID id
    ) {
        return savedReviews.find(authentication, id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Review not found"
                ));
    }

    public record ReviewPage(
            List<SavedReviewService.Summary> items,
            int page,
            int totalPages,
            long totalItems
    ) {
    }
}