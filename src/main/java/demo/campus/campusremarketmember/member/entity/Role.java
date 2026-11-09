package demo.campus.campusremarketmember.member.entity;

/** 회원 종류. DB에는 글자(STUDENT 등)로 저장된다. */
public enum Role {
    STUDENT,   // 일반회원(학생): 회원가입 화면에서 "일반회원" 선택
    OWNER,     // 사장: 회원가입 화면에서 "사장님" 선택 (사업자번호 필요)
    MANAGER,   // 매니저: 회원가입 X, 사장이 로그인해서 "매니저 등록" 화면에서 직접 만듦
    ADMIN      // 캠퍼스리마켓 운영자 = 슈퍼계정 (서버 시작 시 미리 생성, 가입 불가)
}
