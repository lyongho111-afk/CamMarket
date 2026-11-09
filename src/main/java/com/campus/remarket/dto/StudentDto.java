package com.campus.remarket.dto;
import lombok.Data;

/** 로그인 담당과 공유하는 학생 식별자와 온도 규격. 365 = 36.5도. */
@Data
public class StudentDto {
    private String unisno;
    private Integer temp = 365;
    private Long memberId;
}
