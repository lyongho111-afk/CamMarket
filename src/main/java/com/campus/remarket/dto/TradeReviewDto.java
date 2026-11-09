package com.campus.remarket.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TradeReviewDto {
    private Long reviewId;
    private String orderId;
    private String reviewerId;
    private String targetId;
    private Integer score;            // 1 ~ 5
    private String attitude;          // 시간엄수, 친절 등 태도 키워드
    private String content;           // 상세 후기
    private Integer temperatureDelta; // 온도 변화치 (+5, -8 등)
    private LocalDateTime createdAt;

    // 프론트엔드 반환 편의용 필드
    private Double currentTemperature; // 변경 후 피평가자의 매너온도 (예: 37.0)
    private Double appliedDelta;       // 화면 표기용 변화량 (예: +0.5)
}