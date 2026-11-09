package demo.campus.campusremarketmember.member.controller;

import demo.campus.campusremarketmember.member.dto.EmailCodeDto;
import demo.campus.campusremarketmember.member.dto.EmailVerifyDto;
import demo.campus.campusremarketmember.member.dto.LoginDto;
import demo.campus.campusremarketmember.member.dto.ManagerDto;
import demo.campus.campusremarketmember.member.dto.MemberView;
import demo.campus.campusremarketmember.member.dto.NameDto;
import demo.campus.campusremarketmember.member.dto.PhoneDto;
import demo.campus.campusremarketmember.member.dto.StatusDto;
import demo.campus.campusremarketmember.member.dto.WithdrawDto;
import demo.campus.campusremarketmember.member.dto.OwnerSignupDto;
import demo.campus.campusremarketmember.member.dto.SchoolView;
import demo.campus.campusremarketmember.member.dto.StudentSignupDto;
import demo.campus.campusremarketmember.member.entity.Member;
import demo.campus.campusremarketmember.member.service.MemberService;
import demo.campus.campusremarketmember.member.util.LoginSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import com.campus.remarket.service.LoginAttempts;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/** 회원 REST API. 화면(login.html, signup.html)이 fetch로 호출한다. 주소는 모두 /api/members 로 시작. */
@RestController
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService service;
    private final LoginAttempts attempts;
    private final HttpSessionCsrfTokenRepository csrf;

    MemberController(MemberService service, LoginAttempts attempts, HttpSessionCsrfTokenRepository csrf) {
        this.service = service; this.attempts = attempts; this.csrf = csrf;
    }

    @GetMapping("/schools")                                  // 가입·로그인 화면의 학교 목록
    List<SchoolView> schools() {
        return service.schools().stream().map(SchoolView::of).toList();
    }

    @PostMapping("/email/code")                              // 인증번호 받기
    Map<String, String> sendCode(@RequestBody EmailCodeDto req) {
        String devCode = service.sendCode(req.schoolId(), req.email());
        return devCode == null
                ? Map.of("message", "인증번호를 메일로 보냈어요.")
                : Map.of("message", "시연 모드라 인증번호를 화면에 표시해요.", "devCode", devCode); // 모의 모드에서만 번호 포함
    }

    @PostMapping("/email/verify")                            // 인증번호 확인
    Map<String, String> verify(@RequestBody EmailVerifyDto req) {
        service.verifyCode(req.email(), req.code());
        return Map.of("message", "이메일 인증이 완료됐어요.");
    }

    @PostMapping("/signup")                                  // 회원가입: 일반회원(학생)
    @ResponseStatus(HttpStatus.CREATED)                      // 성공하면 201 Created
    MemberView signup(@RequestBody StudentSignupDto form) {
        return MemberView.of(service.signup(form));
    }

    @PostMapping(value = "/signup/owner", consumes = MediaType.MULTIPART_FORM_DATA_VALUE) // 사진이 있어서 JSON 대신 multipart
    @ResponseStatus(HttpStatus.CREATED)
    MemberView signupOwner(OwnerSignupDto form,                                    // 폼 칸 이름 = record 칸 이름 → 자동으로 담김
                           @RequestParam(value = "photo", required = false) MultipartFile photo) { // 사진은 선택
        if(form.schoolId()==null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"학교를 선택해 주세요.");
        return MemberView.of(service.signupOwner(form, photo));
    }

    // 10차 호환: 기존 JSON 계정 등록 클라이언트는 별도 가게 등록 흐름을 유지합니다.
    @PostMapping(value = "/signup/owner", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    MemberView signupOwnerJson(@RequestBody OwnerSignupDto form) {
        return MemberView.of(service.signupOwnerAccount(form));
    }

    // ===== 매니저 관리 (사장만, 아니면 403) =====
    @GetMapping("/managers")                                 // 내 매니저 목록
    List<MemberView> managers(HttpSession session) {
        return service.managers(LoginSession.requireMemberId(session)).stream().map(MemberView::of).toList();
    }

    @PostMapping("/managers")                                // 매니저 등록 (이름·아이디·비밀번호)
    @ResponseStatus(HttpStatus.CREATED)
    MemberView addManager(@RequestBody ManagerDto form, HttpSession session) {
        return MemberView.of(service.addManager(LoginSession.requireMemberId(session), form));
    }

    @DeleteMapping("/managers/{id}")                         // 매니저 삭제
    Map<String, String> deleteManager(@PathVariable Long id, HttpSession session) {
        service.deleteManager(LoginSession.requireMemberId(session), id);
        return Map.of("message", "매니저를 삭제했어요.");
    }

    @PostMapping("/login")                                   // 로그인
    MemberView login(@RequestBody LoginDto req, HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response) {
        long credentialStart=System.nanoTime();
        attempts.check(request.getRemoteAddr(), req.schoolId(), req.loginId());
        Member m;
        try { m = service.login(req.schoolId(), req.loginId(), req.password()); }
        catch (ResponseStatusException error) {
            if (error.getStatusCode().value() == 401) attempts.failed(request.getRemoteAddr(), req.schoolId(), req.loginId());
            throw error;
        }
        if(req.role()!=null&&!req.role().equals(m.getRole().name()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"선택한 로그인 유형과 계정이 일치하지 않아요. 사장님·매니저·관리자 구분을 확인해 주세요.");
        attempts.succeeded(request.getRemoteAddr(), req.schoolId(), req.loginId());
        HttpSession session = request.getSession();          // 세션이 없으면 새로 만듦
        request.changeSessionId();                           // 로그인 순간 세션 번호를 바꿔 "세션 고정 공격"을 막음
        csrf.saveToken(null, request, response);
        session.removeAttribute("loginStudent");
        session.removeAttribute("mannerCsrf");
        session.removeAttribute("staffCsrf");
        if (m.isStudent()) session.setAttribute("loginUnisno", service.actorKey(m));
        else session.removeAttribute("loginUnisno");
        session.setAttribute("credentialStart",credentialStart);
        session.setAttribute(LoginSession.KEY, m.getId());   // 세션에는 회원 번호만 저장 (객체 통째로 X)
        return MemberView.of(m);
    }

    @PostMapping("/logout")                                  // 로그아웃
    Map<String, String> logout(HttpSession session) {
        session.invalidate();                                // 세션을 통째로 지움
        return Map.of("message", "로그아웃했어요.");
    }

    @GetMapping("/me")                                       // 지금 로그인한 사람 (화면 상단 이름 표시용)
    MemberView me(HttpSession session) {
        return MemberView.of(service.get(LoginSession.requireMemberId(session))); // 로그인 안 했으면 401
    }

    @PutMapping("/me")                                       // 마이페이지: 내 전화번호 수정
    MemberView updateMe(@RequestBody PhoneDto form, HttpSession session) {
        Long id = LoginSession.requireMemberId(session);     // 로그인 안 했으면 401 (남의 정보는 바꿀 수 없음)
        return MemberView.of(service.updatePhone(id, form.phone())); // 바뀐 내 정보를 돌려줌
    }

    @PutMapping("/me/name")                                  // 계정 설정: 닉네임(이름) 수정
    MemberView updateName(@RequestBody NameDto form, HttpSession session) {
        Long id = LoginSession.requireMemberId(session);
        return MemberView.of(service.updateName(id, form.name()));
    }

    @PutMapping("/me/status")                                // 계정 설정: 재학생/졸업생 변경
    MemberView updateStatus(@RequestBody StatusDto form, HttpSession session) {
        Long id = LoginSession.requireMemberId(session);
        return MemberView.of(service.updateStudentStatus(id, form.studentStatus()));
    }

    @PostMapping("/me/withdraw")                             // 계정 설정: 회원 탈퇴
    Map<String, String> withdraw(@RequestBody WithdrawDto form, HttpSession session) {
        Long id = LoginSession.requireMemberId(session);
        service.withdraw(id, form.password());               // 비밀번호 확인 후 탈퇴 처리
        session.invalidate();                                // 바로 로그아웃
        return Map.of("message", "탈퇴했어요. 그동안 이용해 주셔서 고마워요.");
    }

    // ===== 관리자 전용: 슈퍼계정인지 확인하는 예시 =====
    @GetMapping("/admin/list")
    List<MemberView> adminList(HttpSession session) {
        Member me = service.get(LoginSession.requireMemberId(session));
        if (!me.isAdmin())                                   // 학생·사장이면 거절
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "관리자만 볼 수 있어요.");
        return service.allMembers().stream().map(MemberView::of).toList();
    }
}
