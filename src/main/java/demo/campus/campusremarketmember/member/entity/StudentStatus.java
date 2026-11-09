package demo.campus.campusremarketmember.member.entity;

/** 학생의 재학 상태. 관리자·사장은 값이 없다(null). */
public enum StudentStatus {
    ENROLLED,  // 재학생
    GRADUATED  // 졸업자 (나중에 "선배 인증 배지"에 활용 가능)
}
