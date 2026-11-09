package demo.campus.campusremarketmember.member.dto;

import demo.campus.campusremarketmember.member.entity.StudentStatus;

/** 회원가입 화면(일반회원·학생)에서 받는 값 */
public record StudentSignupDto(
        Long schoolId,                 // 고른 학교 번호
        String studentNo,              // 학번 (= 로그인 아이디)
        String password,               // 비밀번호 (서비스에서 암호화)
        String name,                   // 이름
        String email,                  // 인증 마친 학교 웹메일
        String phone,                  // 전화번호
        StudentStatus studentStatus    // 재학 / 졸업
) {}
