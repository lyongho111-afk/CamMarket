package demo.campus.campusremarketmember.member.dto;

import demo.campus.campusremarketmember.member.entity.StudentStatus;

/** 계정 설정: 재학생/졸업생 변경 요청 ("ENROLLED" 또는 "GRADUATED") */
public record StatusDto(StudentStatus studentStatus) {}
