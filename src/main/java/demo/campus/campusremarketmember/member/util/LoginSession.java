package demo.campus.campusremarketmember.member.util;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** 세션에 로그인 정보를 넣고 꺼내는 곳을 한 군데로 모은다. 다른 팀원도 이 클래스만 쓰면 된다. */
public final class LoginSession {

    public static final String KEY = "loginMemberId"; // 세션에 저장하는 이름 (오타 방지용 상수)

    private LoginSession() {}                          // 객체를 만들 필요가 없는 도구 클래스라 생성자를 막음

    /** 로그인한 회원 번호. 로그인 안 했으면 null. */
    public static Long memberId(HttpSession session) {
        return (Long) session.getAttribute(KEY);
    }

    /** 로그인이 꼭 필요한 기능에서 호출: 안 했으면 401 에러. (예: 상품 등록, 거래 요청) */
    public static Long requireMemberId(HttpSession session) {
        Long id = memberId(session);
        if (id == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요해요.");
        return id;
    }
}
