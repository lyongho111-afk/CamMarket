package demo.campus.campusremarketmember.member.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 회원 테이블. 학생·사장·매니저·관리자를 한 테이블에 저장하고 role로 구분한다.
 * - 학생: 회원가입 (학교 + 학번)
 * - 사장: 회원가입 (아이디 + 사업자번호)
 * - 매니저: 사장이 등록 (이름·아이디·비밀번호만)
 * - 관리자: 서버 시작 시 미리 생성
 */
@Entity
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                       // 회원 번호: 상품 판매자·게시글 작성자 등은 이 번호로 연결

    @Column(name = "login_id", nullable = false, length = 30)
    private String loginId;                // 학생 = 학번, 사장·매니저·관리자 = 직접 정한 아이디

    @Column(name = "account_key", nullable = false, unique = true, length = 50) // 이 값 하나로 중복을 막음 (UNIQUE)
    private String accountKey;             // 학생 "학교번호:학번"(예: 1:20261234), 그 외 "staff:아이디"(예: staff:owner1)

    @Column(nullable = false, length = 100)
    private String password;               // BCrypt로 암호화된 값 (원래 비밀번호는 저장하지 않음)

    @Column(nullable = false, length = 20)
    private String name;                   // 이름 (사장은 대표자명)

    @Column(unique = true, length = 100)
    private String email;                  // 이메일 (중복 금지). 매니저는 없어서 null 허용

    @Column(length = 20)
    private String phone;                  // 전화번호. 매니저는 없음

    @Enumerated(EnumType.STRING)           // "STUDENT"처럼 글자로 저장 → enum 순서가 바뀌어도 안전
    @Column(nullable = false, length = 20)
    private Role role;                     // 회원 종류

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private StudentStatus studentStatus;   // 재학/졸업 (학생만)

    @ManyToOne(fetch = FetchType.LAZY)     // 회원 여러 명 → 학교 하나 (FK를 가진 쪽)
    @JoinColumn(name = "school_id")
    private School school;                 // 학생만, 나머지는 null

    @Column(name = "business_no", unique = true, length = 12) // 사업자번호 중복 금지 (UNIQUE)
    private String businessNo;             // 사장만: 123-45-67890 모양으로 저장, 나머지는 null

    @Column(name = "owner_id")
    private Long ownerId;                  // 매니저만: 나를 등록한 사장의 회원 번호, 나머지는 null

    private Long shopId;                   // 가게 테이블이 생기면 연결 (지금은 null)

    @Column(nullable = false)
    private LocalDateTime createdAt;       // 가입(등록) 시각

    protected Member() {}                  // JPA용 기본 생성자

    private Member(String loginId, String password, String name, String email, String phone, Role role) { // 공통 부분만
        this.loginId = loginId;
        this.password = password;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.accountKey = accountKey(null, loginId); // 기본은 "staff:아이디" (학생은 아래 student()에서 다시 정함)
        this.createdAt = LocalDateTime.now();
    }

    // ===================== 종류별로 만드는 메서드 (role을 밖에서 마음대로 못 정하게) =====================

    /** 학생: 회원가입 화면에서 "일반회원" */
    public static Member student(String studentNo, String encodedPassword, String name, String email,
                                 String phone, StudentStatus status, School school) {
        Member m = new Member(studentNo, encodedPassword, name, email, phone, Role.STUDENT);
        m.studentStatus = status;
        m.school = school;
        m.accountKey = accountKey(school.getId(), studentNo); // 학생은 "학교번호:학번"
        return m;
    }

    /** 사장: 회원가입 화면에서 "사장님" */
    public static Member owner(String loginId, String encodedPassword, String name, String email,
                               String phone, String businessNo) {
        Member m = new Member(loginId, encodedPassword, name, email, phone, Role.OWNER);
        m.businessNo = businessNo;
        return m;
    }

    /** 매니저: 로그인한 사장이 등록 (이름·아이디·비밀번호만) */
    public static Member manager(String loginId, String encodedPassword, String name, Long ownerId) {
        Member m = new Member(loginId, encodedPassword, name, null, null, Role.MANAGER); // 이메일·전화 없음
        m.ownerId = ownerId;                                                            // 어느 사장의 매니저인지
        return m;
    }

    /** 관리자: 서버 시작 시 미리 넣는 용도 */
    public static Member admin(String loginId, String encodedPassword, String name, String email) {
        return new Member(loginId, encodedPassword, name, email, null, Role.ADMIN);
    }

    /**
     * 로그인·중복 검사에 쓰는 합친 글자. 가입 때와 로그인 때 같은 규칙을 쓰려고 한 곳에 둠.
     * 학생: "학교번호:학번", 그 외(사장·매니저·관리자): "staff:아이디"
     */
    public static String accountKey(Long schoolId, String loginId) {
        return (schoolId == null ? "staff" : schoolId.toString()) + ":" + loginId;
    }

    // ===================== 권한 확인 =====================
    public boolean isAdmin()   { return role == Role.ADMIN; }    // 슈퍼계정(운영자)인지
    public boolean isStudent() { return role == Role.STUDENT; }  // 일반 학생인지
    public boolean isOwner()   { return role == Role.OWNER; }    // 사장인지
    public boolean isManager() { return role == Role.MANAGER; }  // 매니저인지

    /** 사장 가입 때 만든 가게 번호를 기록 (setter 대신 이름 있는 메서드) */
    public void linkShop(Long shopId) {
        this.shopId = shopId;     // 처음부터 만들어 둔 shopId 칸을 드디어 사용 (트랜잭션 안이라 자동 저장)
    }

    public Long getId() { return id; }
    public String getLoginId() { return loginId; }
    public String getAccountKey() { return accountKey; }
    public String getPassword() { return password; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public Role getRole() { return role; }
    public StudentStatus getStudentStatus() { return studentStatus; }
    public School getSchool() { return school; }
    public String getBusinessNo() { return businessNo; }
    public Long getOwnerId() { return ownerId; }
    public Long getShopId() { return shopId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
