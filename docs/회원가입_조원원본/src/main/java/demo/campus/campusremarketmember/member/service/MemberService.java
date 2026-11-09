package demo.campus.campusremarketmember.member.service;

import demo.campus.campusremarketmember.member.dto.ManagerDto;
import demo.campus.campusremarketmember.member.dto.OwnerSignupDto;
import demo.campus.campusremarketmember.member.dto.StudentSignupDto;
import demo.campus.campusremarketmember.member.entity.EmailVerification;
import demo.campus.campusremarketmember.member.entity.Member;
import demo.campus.campusremarketmember.member.entity.School;
import demo.campus.campusremarketmember.member.repository.EmailVerificationRepository;
import demo.campus.campusremarketmember.member.repository.MemberRepository;
import demo.campus.campusremarketmember.member.repository.SchoolRepository;
import com.campus.remarket.service.StudentMemberLink;
import com.campus.remarket.service.OwnerShopLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

/** 회원가입(학생·사장)·이메일 인증·로그인·매니저 등록 규칙. Controller는 받아서 여기로 넘기기만 한다. */
@Service
@Transactional(readOnly = true) // 기본은 읽기 전용, 저장하는 메서드에만 @Transactional 다시 붙임
public class MemberService {

    private static final Logger log = LoggerFactory.getLogger(MemberService.class);

    public static final int MAX_MANAGERS = 3;  // 사장 한 명당 매니저 최대 인원 (가게 테이블의 man1~3과 같게)

    // 입력 형식 검사 규칙 (정규식)
    private static final Pattern STUDENT_NO = Pattern.compile("\\d{6,10}");                  // 학번: 숫자 6~10자리
    private static final Pattern LOGIN_ID = Pattern.compile("[a-z0-9_]{4,20}");               // 아이디: 영어 소문자·숫자·_ 4~20자
    private static final Pattern BUSINESS_NO = Pattern.compile("\\d{3}-?\\d{2}-?\\d{5}");     // 사업자번호: 123-45-67890
    private static final Pattern PHONE = Pattern.compile("0\\d{1,2}-?\\d{3,4}-?\\d{4}");      // 010-1234-5678, 032-123-4567
    private static final Pattern EMAIL = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");     // 아이디@도메인.점
    private static final Pattern PASSWORD = Pattern.compile("(?=.*[A-Za-z])(?=.*\\d).{8,64}"); // 영문+숫자 8자 이상

    private final StudentMemberLink studentLink;
    private final OwnerShopLink ownerShopLink;             // 사장 가입 때 가게(shops) 만들기
    private final MemberRepository members;
    private final SchoolRepository schools;
    private final EmailVerificationRepository verifications;
    private final PasswordEncoder passwordEncoder;
    private final boolean mailMock;                          // true면 메일 대신 화면·콘솔에 번호 표시
    private final SecureRandom random = new SecureRandom();

    public MemberService(StudentMemberLink studentLink, OwnerShopLink ownerShopLink, MemberRepository members, SchoolRepository schools,
                         EmailVerificationRepository verifications, PasswordEncoder passwordEncoder,
                         @Value("${campus.mail.mock:true}") boolean mailMock) {
        this.studentLink = studentLink;
        this.ownerShopLink = ownerShopLink;
        this.members = members;
        this.schools = schools;
        this.verifications = verifications;
        this.passwordEncoder = passwordEncoder;
        this.mailMock = mailMock;
    }

    // ===================== 학교 =====================

    public List<School> schools() {
        return schools.findAllByOrderByNameAsc();
    }

    // ===================== 이메일 인증 (학생) =====================

    @Transactional
    public String sendCode(Long schoolId, String email) {
        School school = findSchool(schoolId);
        String mail = normalize(email);
        if (!EMAIL.matcher(mail).matches()) throw bad("이메일 형식을 확인해 주세요.");
        if (!school.allowsEmail(mail))
            throw bad(school.getName() + " 웹메일(@" + school.getEmailDomain().replace(",", ", @") + ")만 인증할 수 있어요."); // 여러 도메인이면 "@a, @b"
        if (members.existsByEmail(mail)) throw conflict("이미 가입된 이메일이에요.");

        String code = String.format("%06d", random.nextInt(1_000_000)); // 앞자리 0도 유지
        verifications.save(new EmailVerification(mail, code, LocalDateTime.now().plusMinutes(5)));
        if (mailMock) {
            log.info("[모의 메일] {} 인증번호: {}", mail, code);
            return code;
        }
        return null; // TODO: 실제 메일 발송 연결
    }

    @Transactional(noRollbackFor = ResponseStatusException.class) // 틀려도 "틀린 횟수 +1"은 저장
    public void verifyCode(String email, String code) {
        EmailVerification v = verifications.findTopByEmailOrderByIdDesc(normalize(email))
                .orElseThrow(() -> bad("먼저 인증번호를 받아 주세요."));
        if (v.isExpired()) throw bad("인증번호가 만료됐어요. 다시 받아 주세요.");
        if (v.tooManyAttempts()) throw bad("여러 번 틀렸어요. 인증번호를 다시 받아 주세요.");
        if (!v.getCode().equals(code == null ? "" : code.trim())) {
            v.fail();
            throw bad("인증번호가 맞지 않아요.");
        }
        v.verify();
    }

    // ===================== 회원가입: 학생 =====================


    @Transactional
    public Member signup(StudentSignupDto f) {
        School school = findSchool(f.schoolId());
        String studentNo = trim(f.studentNo());
        String email = normalize(f.email());

        if (!STUDENT_NO.matcher(studentNo).matches()) throw bad("학번은 숫자 6~10자리로 입력해 주세요.");
        checkPassword(f.password());
        String name = checkName(f.name());
        String phone = checkPhone(f.phone());
        if (f.studentStatus() == null) throw bad("재학생인지 졸업자인지 선택해 주세요.");

        EmailVerification v = verifications.findTopByEmailOrderByIdDesc(email)       // 이메일 인증을 마쳤는지
                .orElseThrow(() -> bad("이메일 인증을 먼저 해 주세요."));
        if (!v.isVerified()) throw bad("이메일 인증을 먼저 해 주세요.");

        if (members.existsByAccountKey(Member.accountKey(school.getId(), studentNo)))  // 같은 학교+학번
            throw conflict("이미 가입된 학번이에요.");
        if (members.existsByEmail(email)) throw conflict("이미 가입된 이메일이에요.");

        Member saved = members.save(Member.student(studentNo, passwordEncoder.encode(f.password()),
                name, email, phone, f.studentStatus(), school));
        studentLink.link(saved);
        verifications.deleteByEmail(email);
        return saved;
    }

    // ===================== 회원가입: 사장 =====================


    @Transactional
    public Member signupOwner(OwnerSignupDto f, MultipartFile photo) {   // 사진도 같이 받음
        String loginId = checkLoginId(f.loginId());          // 아이디 형식 + 중복 확인
        checkPassword(f.password());
        String name = checkName(f.name());
        String phone = checkPhone(f.phone());
        String email = normalize(f.email());
        if (!EMAIL.matcher(email).matches()) throw bad("이메일 형식을 확인해 주세요.");
        if (members.existsByEmail(email)) throw conflict("이미 가입된 이메일이에요.");

        String bizNo = trim(f.businessNo());
        if (!BUSINESS_NO.matcher(bizNo).matches()) throw bad("사업자번호 10자리를 확인해 주세요. (예: 123-45-67890)");
        bizNo = bizNo.replaceAll("[^0-9]", "").replaceFirst("(\\d{3})(\\d{2})(\\d{5})", "$1-$2-$3"); // 하이픈 모양 통일
        if (members.existsByBusinessNo(bizNo)) throw conflict("이미 등록된 사업자번호예요.");     // 사업자번호 UNIQUE

        Member owner = members.save(Member.owner(loginId, passwordEncoder.encode(f.password()),
                name, email, phone, bizNo));                         // ① 사장 회원 저장 (JPA) → id 생김

        Long shopId = ownerShopLink.createShop(owner, f, photo);    // ② 가게 저장 (MyBatis shops). 실패하면 ①도 같이 취소
        owner.linkShop(shopId);                                     // ③ 회원에 가게 번호 기록
        return owner;
    }

    // ===================== 매니저 (사장만) =====================


    /** 로그인한 사람이 사장인지 확인하고 돌려줌 (매니저 기능 공통) */
    private Member requireOwner(Long memberId) {
        Member me = get(memberId);
        if (!me.isOwner()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "사장님만 매니저를 관리할 수 있어요.");
        return me;
    }

    public List<Member> managers(Long ownerId) {
        requireOwner(ownerId);
        return members.findByOwnerIdOrderByIdAsc(ownerId);  // 내 매니저만
    }

    @Transactional
    public Member addManager(Long ownerId, ManagerDto f) {
        requireOwner(ownerId);
        if (members.countByOwnerId(ownerId) >= MAX_MANAGERS)
            throw bad("매니저는 최대 " + MAX_MANAGERS + "명까지 등록할 수 있어요.");
        String name = checkName(f.name());
        String loginId = checkLoginId(f.loginId());
        checkPassword(f.password());
        return members.save(Member.manager(loginId, passwordEncoder.encode(f.password()), name, ownerId));
    }

    @Transactional
    public void deleteManager(Long ownerId, Long managerId) {
        requireOwner(ownerId);
        Member m = members.findById(managerId).orElseThrow(() -> bad("매니저를 찾을 수 없어요."));
        if (!m.isManager() || !ownerId.equals(m.getOwnerId()))    // 남의 매니저나 다른 회원은 못 지움
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "내 매니저만 삭제할 수 있어요.");
        members.delete(m);
    }

    // ===================== 로그인 =====================

    /** schoolId가 있으면 학생(학교+학번), 없으면 사장·매니저·관리자(아이디) */
    @Transactional
    public Member login(Long schoolId, String loginId, String password) {
        Member m = members.findByAccountKey(Member.accountKey(schoolId, trim(loginId))).orElse(null);
        if (m == null || password == null || !passwordEncoder.matches(password, m.getPassword()))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 맞지 않아요."); // 어느 쪽이 틀렸는지 안 알려줌
        studentLink.link(m);
        return m;
    }

    public Member get(Long memberId) {
        return members.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요해요."));
    }

    public List<Member> allMembers() {
        return members.findAllByOrderByIdDesc();
    }

    // ===================== 공통 검사 도우미 =====================

    private String checkLoginId(String raw) {                 // 사장·매니저 아이디
        String id = trim(raw).toLowerCase();
        if (!LOGIN_ID.matcher(id).matches()) throw bad("아이디는 영어 소문자·숫자·_ 로 4~20자 입력해 주세요.");
        if (members.existsByAccountKey(Member.accountKey(null, id))) throw conflict("이미 사용 중인 아이디예요.");
        return id;
    }
    private void checkPassword(String pw) {
        if (pw == null || !PASSWORD.matcher(pw).matches()) throw bad("비밀번호는 영문과 숫자를 섞어 8자 이상으로 입력해 주세요.");
    }
    private String checkName(String raw) {
        String name = trim(raw);
        if (name.length() < 2 || name.length() > 20) throw bad("이름을 2~20자로 입력해 주세요.");
        return name;
    }
    private String checkPhone(String raw) {                   // 형식 확인 후 010-1234-5678 모양으로 통일
        String phone = trim(raw);
        if (!PHONE.matcher(phone).matches()) throw bad("전화번호를 확인해 주세요. (예: 010-1234-5678)");
        String d = phone.replaceAll("[^0-9]", "");
        return d.startsWith("02")
                ? d.replaceFirst("(02)(\\d{3,4})(\\d{4})", "$1-$2-$3")
                : d.replaceFirst("(\\d{3})(\\d{3,4})(\\d{4})", "$1-$2-$3");
    }
    private School findSchool(Long schoolId) {
        if (schoolId == null) throw bad("학교를 선택해 주세요.");
        return schools.findById(schoolId).orElseThrow(() -> bad("학교를 다시 선택해 주세요."));
    }
    private static String trim(String s) { return s == null ? "" : s.trim(); }
    private static String normalize(String email) { return trim(email).toLowerCase(); }
    private static ResponseStatusException bad(String m) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, m); }   // 400
    private static ResponseStatusException conflict(String m) { return new ResponseStatusException(HttpStatus.CONFLICT, m); } // 409
}
