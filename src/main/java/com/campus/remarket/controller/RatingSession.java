package com.campus.remarket.controller;
import demo.campus.campusremarketmember.member.service.MemberService;
import demo.campus.campusremarketmember.member.util.LoginSession;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Component
public class RatingSession {
    private final MemberService members;
    public RatingSession(MemberService members){this.members=members;}
    public String actor(HttpServletRequest request){
        var session=request.getSession(false);
        if(session==null||LoginSession.memberId(session)==null)return null;
        var me=members.get(LoginSession.memberId(session));
        if(!me.isStudent())return null;
        Object unisno=session.getAttribute("loginUnisno");
        return members.actorKey(me).equals(unisno)?members.actorKey(me):null;
    }
    public boolean demo(HttpServletRequest request){return false;}
    public String csrf(HttpServletRequest request){
        var session=request.getSession();String token=(String)session.getAttribute("mannerCsrf");
        if(token==null){token=UUID.randomUUID().toString();session.setAttribute("mannerCsrf",token);}return token;
    }
    public void verify(HttpServletRequest request,String provided){
        var session=request.getSession(false);Object expected=session==null?null:session.getAttribute("mannerCsrf");
        if(!(expected instanceof String token)||provided==null||!MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8),provided.getBytes(StandardCharsets.UTF_8)))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"평가 화면을 다시 열고 제출해 주세요.");
    }
}
