package demo.campus.campusremarketmember.member.repository;

import demo.campus.campusremarketmember.member.entity.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {
    Optional<EmailVerification> findTopByEmailOrderByIdDesc(String email); // 여러 번 요청했으면 가장 최근 것만 유효
    void deleteByEmail(String email);                                      // 가입이 끝나면 인증 기록 정리
}
