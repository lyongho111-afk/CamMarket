package com.campus.remarket.controller;
import com.campus.remarket.service.AccountService;
import demo.campus.campusremarketmember.member.util.LoginSession;
import jakarta.servlet.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.*;
@RestController @RequestMapping("/api/account") public class AccountController {
 private final AccountService service;public AccountController(AccountService service){this.service=service;}
 public record Request(String email,String purpose,String code,String oldPassword,String password){}
 @GetMapping("/status") public Map<String,Object> status(){return Map.of("emailRecoveryAvailable",service.recoveryReady(),"localDemo",service.localDemo());}
 private void origin(HttpServletRequest r){if("cross-site".equals(r.getHeader("Sec-Fetch-Site"))||r.getHeader("Origin")!=null&&!r.getHeader("Origin").equals(r.getScheme()+"://"+r.getHeader("Host")))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"현재 사이트에서 요청하세요.");}
 @PostMapping("/code") public Map<String,String> code(@RequestBody Request input,HttpServletRequest request){origin(request);var s=request.getSession();synchronized(s){var prev=(AccountService.Challenge)s.getAttribute("accountChallenge");s.setAttribute("accountChallenge",service.issue(input.email(),input.purpose(),prev));}return Map.of("message","가입 정보가 일치하면 인증 메일이 발송됩니다.");}
 @PostMapping("/verify") public Map<String,Object> verify(@RequestBody Request input,HttpServletRequest request){origin(request);var s=request.getSession();synchronized(s){var c=(AccountService.Challenge)s.getAttribute("accountChallenge");if(!service.matches(c,input.code(),input.purpose())){if(c!=null)s.setAttribute("accountChallenge",new AccountService.Challenge(c.email(),c.purpose(),c.code(),c.expires(),c.attempts()+1,c.issued()));throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"인증번호가 맞지 않거나 만료되었습니다.");}if("ID".equals(c.purpose())){var result=service.identify(c.email());s.removeAttribute("accountChallenge");return result;}service.reset(c.email(),input.password());s.invalidate();return Map.of("message","비밀번호를 변경했습니다. 다시 로그인하세요.");}}
 @PostMapping("/password") public Map<String,String> password(@RequestBody Request input,HttpServletRequest request){origin(request);var s=request.getSession(false);if(s==null)throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"먼저 로그인하세요.");service.change(LoginSession.requireMemberId(s),input.oldPassword(),input.password());s.invalidate();return Map.of("message","비밀번호를 변경했습니다. 다시 로그인하세요.");}
}
