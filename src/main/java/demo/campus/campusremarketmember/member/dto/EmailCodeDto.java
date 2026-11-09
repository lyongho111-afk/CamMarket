package demo.campus.campusremarketmember.member.dto;

/** 인증번호 받기 버튼: 어느 학교의 어떤 메일인지 */
public record EmailCodeDto(Long schoolId, String email) {}
