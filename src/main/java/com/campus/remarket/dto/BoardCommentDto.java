package com.campus.remarket.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BoardCommentDto {
    private Long commentId;
    private Long postId;
    private Long memberId;
    private String authorName;
    private String schoolName;
    private String content;
    private LocalDateTime regtime;
}
