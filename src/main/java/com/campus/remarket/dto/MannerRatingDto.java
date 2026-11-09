package com.campus.remarket.dto;
import lombok.Data;
import java.time.LocalDateTime;
@Data
public class MannerRatingDto {
    private Long ratingId;
    private String orderId;
    private String reviewerUnisno;
    private String recipientUnisno;
    private String feedback;
    private Integer delta;
    private LocalDateTime createdAt;
}
