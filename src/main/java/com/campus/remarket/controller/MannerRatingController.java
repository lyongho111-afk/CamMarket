package com.campus.remarket.controller;
import com.campus.remarket.service.MannerRatingService;
import com.campus.remarket.auth.MarketMember;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
@Controller
public class MannerRatingController {
    private final MarketMember member;
    private final MannerRatingService service;private final RatingSession session;
    public MannerRatingController(MarketMember member,MannerRatingService service,RatingSession session){this.member=member;this.service=service;this.session=session;}
    @PostMapping("/trades/rating")
    public String rate(@RequestParam String id,@RequestParam String feedback,@RequestParam String csrf,HttpServletRequest request,RedirectAttributes redirect){
        member.trade(id);session.verify(request,csrf);service.rate(id,session.actor(request),feedback);redirect.addFlashAttribute("ratingSuccess","매너온도 평가를 저장했습니다.");
        return "redirect:/trades/selectOne?id="+URLEncoder.encode(id,StandardCharsets.UTF_8);
    }
}
