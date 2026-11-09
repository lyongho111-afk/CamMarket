package demo.campus.campusremarketmember.member.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** 이메일 인증번호. 가입 버튼을 누르기 전(회원이 아직 없을 때) 쓰므로 회원 테이블과 따로 둔다. */
@Entity
public class EmailVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String email;                 // 인증번호를 보낸 메일 주소

    @Column(nullable = false, length = 6)
    private String code;                  // 6자리 숫자 (예: 482913)

    @Column(nullable = false)
    private LocalDateTime expiresAt;      // 만료 시각 = 발송 + 5분

    private boolean verified;             // 번호를 맞게 입력했는지 (처음엔 false)

    private int attempts;                 // 틀린 횟수 (5번 넘으면 다시 받아야 함 → 아무 숫자나 계속 넣어보는 것 방지)

    protected EmailVerification() {}

    public EmailVerification(String email, String code, LocalDateTime expiresAt) {
        this.email = email;
        this.code = code;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() { return LocalDateTime.now().isAfter(expiresAt); } // 지금이 만료 시각을 지났는지
    public boolean tooManyAttempts() { return attempts >= 5; }                    // 너무 많이 틀렸는지
    public void fail() { attempts++; }                                            // 틀릴 때마다 1 증가
    public void verify() { verified = true; }                                     // 맞으면 인증 완료

    public String getEmail() { return email; }
    public String getCode() { return code; }
    public boolean isVerified() { return verified; }
}
