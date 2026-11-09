package demo.campus.campusremarketmember.member.dto;

/** 인증번호 확인 버튼: 메일과 입력한 6자리 번호 */
public record EmailVerifyDto(String email, String code) {}
