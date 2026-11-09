package com.campus.remarket.dto;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
@Data
public class TradeDto {
    private Long schoolId;
    private String orderId;
    private Long selno;
    private String saleName;
    private String buyer;
    private String seller;
    private Integer agreedPrice;
    private String status;
    private String meetingPlace;
    @DateTimeFormat(pattern="yyyy-MM-dd'T'HH:mm")
    private LocalDateTime meetingTime;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String paymentKey;
    private LocalDateTime createdAt;
    public boolean isCompleted(){return "COMPLETED".equals(status)||"거래완료".equals(status);}
}
