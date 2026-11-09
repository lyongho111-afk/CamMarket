package demo.campus.campusremarketmember.member.dto;

import demo.campus.campusremarketmember.member.entity.Member;
import demo.campus.campusremarketmember.member.entity.Role;
import demo.campus.campusremarketmember.member.entity.StudentStatus;

/** 화면으로 보내는 회원 정보. 비밀번호 같은 값은 절대 넣지 않는다 (Entity를 그대로 보내지 않는 이유) */
public record MemberView(Long id, String loginId, String name, String email, String phone,
                         Role role, StudentStatus studentStatus, String schoolName, String businessNo, boolean admin) {

    public static MemberView of(Member m) {               // Entity → 화면용 모양으로 바꾸기
        return new MemberView(m.getId(), m.getLoginId(), m.getName(), m.getEmail(), m.getPhone(),
                m.getRole(), m.getStudentStatus(),
                m.getSchool() == null ? null : m.getSchool().getName(), // 학생만 학교 있음
                m.getBusinessNo(),                                      // 사장만 사업자번호 있음
                m.isAdmin());                                           // 관리자 메뉴를 보일지 판단
    }
}
