package demo.campus.campusremarketmember.member.dto;

/** 매니저 등록 화면(사장 전용)에서 받는 값: 이름·아이디·비밀번호만 */
public record ManagerDto(String name, String loginId, String password) {}
