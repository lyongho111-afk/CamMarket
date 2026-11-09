package demo.campus.campusremarketmember.member.repository;

import demo.campus.campusremarketmember.member.entity.School;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SchoolRepository extends JpaRepository<School, Long> {
    List<School> findAllByOrderByNameAsc();   // 가입 화면의 학교 목록을 가나다순으로
    boolean existsByName(String name);        // 서버 재시작 때 같은 학교를 또 넣지 않으려고 확인
    Optional<School> findByName(String name); // 이름으로 학교 찾기 (기본 데이터 넣을 때)
    Optional<School> findByCampusCode(String code);
    List<School> findByCampusCodeIsNotNullOrderByNameAsc();
}
