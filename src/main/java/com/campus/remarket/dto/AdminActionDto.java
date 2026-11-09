package com.campus.remarket.dto;
import lombok.Data;
import java.time.LocalDateTime;
@Data
public class AdminActionDto {
    private Long actionId;
    private Long shopId;
    private String shopName;
    private Long actorMemberId;
    private String actorName;
    private String action;
    private String beforeValue;
    private String afterValue;
    private String note;
    private LocalDateTime createdAt;
}
