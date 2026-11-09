package com.campus.remarket.controller;

import com.campus.remarket.dto.TradeReviewDto;
import com.campus.remarket.service.TradeReviewService;
import org.springframework.web.bind.annotation.*;
import com.campus.remarket.auth.MarketMember;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/reviews")
public class TradeReviewApiController {

    private final TradeReviewService reviewService;
    private final MarketMember member;
    private final RatingSession ratingSession;

    public TradeReviewApiController(TradeReviewService reviewService, MarketMember member, RatingSession ratingSession) {
        this.member=member; this.ratingSession=ratingSession;
        this.reviewService = reviewService;
    }

    /**
     * 매너온도 평가 등록 API
     * POST /api/reviews
     */
    @PostMapping
    public TradeReviewDto submitReview(@RequestBody TradeReviewDto dto, @RequestHeader(value="X-CSRF-Token",required=false) String csrf, HttpServletRequest request) {
        member.trade(dto.getOrderId());
        ratingSession.verify(request,csrf);
        dto.setReviewerId(member.studentKey());
        return reviewService.submitReview(dto);
    }

    /**
     * 특정 사용자가 받은 후기 목록 조회 API
     * GET /api/reviews/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public List<TradeReviewDto> getUserReviews(@PathVariable String userId) {
        member.studentKey();
        return reviewService.getReviewsForUser(userId);
    }
}