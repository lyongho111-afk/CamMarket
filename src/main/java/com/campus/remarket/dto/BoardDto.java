package com.campus.remarket.dto;
import lombok.Data;
import java.time.LocalDateTime;

/** 첨부 표의 게시글 데이터. SQL 컬럼은 XML resultMap에서 연결합니다. */
@Data
public class BoardDto {
    private Long schoolId;
    private Long postId;
    private Integer categoryId;
    private String categoryName;
    private String writer;
    private Long authorMemberId;
    private String authorName;
    private String schoolName;
    private int commentCount;
    private String title;
    private String content;
    private LocalDateTime regtime;
}
