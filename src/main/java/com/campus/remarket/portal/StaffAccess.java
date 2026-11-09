package com.campus.remarket.portal;

import demo.campus.campusremarketmember.member.entity.Member;
import demo.campus.campusremarketmember.member.service.MemberService;
import demo.campus.campusremarketmember.member.util.LoginSession;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class StaffAccess {
    private final HttpServletRequest request;
    private final MemberService members;
    public StaffAccess(HttpServletRequest request,MemberService members){this.request=request;this.members=members;}
    public Member current(){
        var session=request.getSession(false);
        if(session==null||LoginSession.memberId(session)==null)throw error(HttpStatus.UNAUTHORIZED,"로그인이 필요합니다.");
        return members.get(LoginSession.memberId(session));
    }
    public Member staff(){var me=current();if(!me.isOwner()&&!me.isManager())throw error(HttpStatus.FORBIDDEN,"사장님·매니저 전용 사이트입니다.");return me;}
    public Member owner(){var me=current();if(!me.isOwner())throw error(HttpStatus.FORBIDDEN,"사장님만 변경할 수 있습니다.");return me;}
    public Member admin(){var me=current();if(!me.isAdmin())throw error(HttpStatus.FORBIDDEN,"관리자 전용 사이트입니다.");return me;}
    public Long ownerId(){var me=staff();return me.isOwner()?me.getId():me.getOwnerId();}
    public String csrf(){
        var session=request.getSession();var value=(String)session.getAttribute("staffCsrf");
        if(value==null){value=UUID.randomUUID().toString();session.setAttribute("staffCsrf",value);}return value;
    }
    public void verify(String provided){
        var session=request.getSession(false);var expected=session==null?null:session.getAttribute("staffCsrf");
        if(!(expected instanceof String value)||provided==null||!MessageDigest.isEqual(value.getBytes(StandardCharsets.UTF_8),provided.getBytes(StandardCharsets.UTF_8)))
            throw error(HttpStatus.FORBIDDEN,"화면을 새로고침한 뒤 다시 저장해 주세요.");
    }
    public static ResponseStatusException error(HttpStatus status,String message){return new ResponseStatusException(status,message);}
}
