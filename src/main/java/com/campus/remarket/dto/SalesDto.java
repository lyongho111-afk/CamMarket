package com.campus.remarket.dto;
import lombok.Data;
import java.time.LocalDateTime;
import java.math.BigDecimal;

/** 첨부 표의 판매글 데이터. SQL 컬럼은 XML resultMap에서 연결합니다. */
@Data
public class SalesDto {
    private Long schoolId;
    private Long selno;
    private String seller;
    private Long sellerMemberId;
    private String sellerName;
    private String sellerSchoolName;
    private String category;
    private String name;
    private Integer oprice;
    private Integer buyprice;
    private Integer wishprice;
    private String detail;
    private Integer discount;
    private LocalDateTime starttime;
    private LocalDateTime endtime;
    private LocalDateTime regtime;
    private String status;
    private String photo1;
    private String photo2;
    private String photo3;
    private String location;
    private String icon;
    private BigDecimal sellerTemperature;
    private LocalDateTime bumpedAt;
    private Integer currentPrice;
    private Integer exposurePriority;
}
