package com.campus.remarket.controller;
import demo.campus.campusremarketmember.member.util.LoginSession;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
@Controller public class AccountPageController {
 @GetMapping("/account.html") public String settings(HttpServletRequest request){var s=request.getSession(false);return s==null||LoginSession.memberId(s)==null?"redirect:/login.html":"redirect:/mypage.html#security";}
}
