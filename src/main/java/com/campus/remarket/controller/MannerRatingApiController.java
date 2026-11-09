package com.campus.remarket.controller;
import com.campus.remarket.service.MannerRatingService;
import com.campus.remarket.auth.MarketMember;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/trades/{id}/rating")
public class MannerRatingApiController {
    public record RatingPage(MannerRatingService.RatingState rating,String csrfToken,boolean demo) {}
    public record Feedback(String feedback) {}
    private final MarketMember member;
    private final MannerRatingService service;private final RatingSession session;
    public MannerRatingApiController(MarketMember member,MannerRatingService service,RatingSession session){this.member=member;this.service=service;this.session=session;}
    @GetMapping public RatingPage state(@PathVariable String id,HttpServletRequest request){member.trade(id);return new RatingPage(service.state(id,session.actor(request)),session.csrf(request),session.demo(request));}
    @PostMapping public RatingPage rate(@PathVariable String id,@RequestBody Feedback input,@RequestHeader(value="X-CSRF-Token",required=false) String csrf,HttpServletRequest request){
        member.trade(id);session.verify(request,csrf);return new RatingPage(service.rate(id,session.actor(request),input.feedback()),session.csrf(request),session.demo(request));
    }
}
