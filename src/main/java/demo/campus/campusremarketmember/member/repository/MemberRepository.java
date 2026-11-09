package demo.campus.campusremarketmember.member.repository;

import demo.campus.campusremarketmember.member.entity.Member;
import demo.campus.campusremarketmember.member.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByAccountKey(String accountKey);       // 로그인: "학교번호:학번" 또는 "staff:아이디"로 찾기
    boolean existsByAccountKey(String accountKey);              // 가입·매니저 등록 시 아이디(학번) 중복 확인
    Optional<Member> findByEmail(String email);
    boolean existsByEmail(String email);                        // 이메일 중복 확인
    boolean existsByBusinessNo(String businessNo);              // 사장 가입 시 사업자번호 중복 확인
    boolean existsByRole(Role role);                            // 기본 계정을 이미 넣었는지 확인
    List<Member> findByOwnerIdOrderByIdAsc(Long ownerId);       // 사장의 매니저 목록 (등록 순)
    long countByOwnerId(Long ownerId);                          // 사장의 매니저 수 (최대 3명 확인)
    List<Member> findAllByOrderByIdDesc();                      // 관리자 화면: 최근 가입 순
}
