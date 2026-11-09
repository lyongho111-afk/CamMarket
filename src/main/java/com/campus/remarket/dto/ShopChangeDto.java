package com.campus.remarket.dto;
import lombok.Data;
import java.time.LocalDateTime;
@Data
public class ShopChangeDto {
    private Long changeId;
    private Long shopId;
    private Long actorMemberId;
    private String actorName;
    private String actorRole;
    private String fieldKey;
    private String fieldLabel;
    private String beforeValue;
    private String afterValue;
    private LocalDateTime changedAt;
}
