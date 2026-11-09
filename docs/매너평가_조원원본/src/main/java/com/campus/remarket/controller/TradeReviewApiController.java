package com.campus.remarket.controller;

import com.campus.remarket.dto.TradeReviewDto;
import com.campus.remarket.service.TradeReviewService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/reviews")
public class TradeReviewApiController {

    private final TradeReviewService reviewService;

    public TradeReviewApiController(TradeReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /**
     * 매너온도 평가 등록 API
     * POST /api/reviews
     */
    @PostMapping
    public TradeReviewDto submitReview(@RequestBody TradeReviewDto dto) {
        return reviewService.submitReview(dto);
    }

    /**
     * 특정 사용자가 받은 후기 목록 조회 API
     * GET /api/reviews/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public List<TradeReviewDto> getUserReviews(@PathVariable String userId) {
        return reviewService.getReviewsForUser(userId);
    }
}