package demo.campus.campusremarketmember.member.config;

import demo.campus.campusremarketmember.member.entity.Member;
import demo.campus.campusremarketmember.member.entity.Role;
import demo.campus.campusremarketmember.member.entity.School;
import demo.campus.campusremarketmember.member.entity.StudentStatus;
import demo.campus.campusremarketmember.member.repository.MemberRepository;
import demo.campus.campusremarketmember.member.repository.SchoolRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 서버 시작 시 기본 데이터를 넣는다: 학교, 관리자 3명, 테스트 사장 1명 + 매니저 1명, 테스트 학생 1명.
 * 관리자는 회원가입으로 만들 수 없으므로 여기서만 생성된다.
 * 파일 DB라 재시작해도 남아 있으므로, 이미 있으면 다시 넣지 않는다.
 */
@Configuration
public class MemberSeed {

    @Bean
    @org.springframework.core.annotation.Order(0)
    CommandLineRunner seedMembers(SchoolRepository schools, MemberRepository members, PasswordEncoder encoder,org.springframework.jdbc.core.JdbcTemplate db) {
        return args -> {
            // ===== 인천 지역 학교 목록: {이름, 지역, 학생 웹메일 도메인} =====
            // ⚠️ 도메인은 각 학교 홈페이지 주소 기준이에요. 실제 학생 메일 주소가 다르면 여기만 고치면 돼요.
            //    도메인이 여러 개면 쉼표로 이어 쓰기 (예: "inha.edu,inha.ac.kr")
            String[][] incheonSchools = {
                    {"인하대학교",          "인천 미추홀구", "inha.edu,inha.ac.kr"},
                    {"인천대학교",          "인천 연수구",   "inu.ac.kr"},
                    {"연세대학교 국제캠퍼스", "인천 연수구",   "yonsei.ac.kr"},
                    {"경인교육대학교",       "인천 계양구",   "ginue.ac.kr"},
                    {"인천가톨릭대학교",     "인천 연수구",   "iccu.ac.kr"},
                    {"청운대학교 인천캠퍼스", "인천 미추홀구", "chungwoon.ac.kr"},
                    {"인하공업전문대학",     "인천 미추홀구", "inhatc.ac.kr"},
                    {"재능대학교",          "인천 동구",     "jeiu.ac.kr"},
                    {"경인여자대학교",       "인천 계양구",   "kiwu.ac.kr"}
            };
            if(db.queryForObject("SELECT COUNT(*) FROM catalog_meta WHERE name='schools-initial'",Integer.class)==0){
            for (String[] s : incheonSchools) {                                    // 하나씩 꺼내서
                if (!schools.existsByName(s[0])) {                                 // 아직 없는 학교만 (재시작해도 중복 X)
                    schools.save(new School(s[0], s[1], s[2]));
                }
            }
            db.update("INSERT INTO catalog_meta(name) VALUES('schools-initial')");}
            if (members.existsByRole(Role.ADMIN)) return;                          // 이미 넣었으면 끝

            String pw = encoder.encode("campus1234!");                            // 시연용 공통 비밀번호 (발표 전 변경)
            for (int i = 1; i <= 3; i++) {                                         // 관리자 3명
                members.save(Member.admin("admin" + i, pw, "관리자" + i, "admin" + i + "@campus.local"));
            }
            Member owner = members.save(Member.owner("owner1", pw, "김사장", "owner1@campus.local",
                    "010-1111-2222", "123-45-67890"));                               // 테스트 사장 1명
            members.save(Member.manager("manager1", pw, "매니저1", owner.getId()));    // 그 사장의 테스트 매니저 1명

            School inha = schools.findByName("인하대학교").orElseThrow();           // 위에서 넣은 인하대
            members.save(Member.student("20261234", pw, "테스트학생", "test@inha.edu",
                    "010-0000-0000", StudentStatus.ENROLLED, inha));                // 로그인 테스트용 학생
        };
    }
}
