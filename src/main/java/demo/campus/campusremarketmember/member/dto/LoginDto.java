package demo.campus.campusremarketmember.member.dto;

/** 로그인 화면에서 받는 값. 학생이면 schoolId가 있고, 사장·매니저·관리자는 null */
public record LoginDto(Long schoolId, String loginId, String password, String role) {}
