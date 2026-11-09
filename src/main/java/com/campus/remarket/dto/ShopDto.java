package com.campus.remarket.dto;
import lombok.Data;

/** 첨부 표의 가게 데이터. SQL 컬럼은 XML resultMap에서 연결합니다. */
@Data
public class ShopDto {
    private Long schoolId;
    private Long version;
    private Long shopId;
    private Long ownerMemberId;
    private String shopname;
    private String shopphone;
    private String address;
    private String photo;
    private java.math.BigDecimal latitude;
    private java.math.BigDecimal longitude;
    private String busino;
    private String ceoname;
    private String busiKind;
    private String bmenu;
    private String reservtime;
    private String busitime;
    private String offday;
    private String specialevent;
    private String sizeandtype;
    private String status;
    private String availablespace;
    private Boolean checked;
}
