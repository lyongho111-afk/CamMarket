package com.campus.remarket.notification;
import com.campus.remarket.auth.MarketMember;
import demo.campus.campusremarketmember.member.entity.Member;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/api/notifications")
public class NotificationController {
    public record SettingsInput(String email,String phone,Boolean smsEnabled,Boolean emailEnabled,Boolean kakaoEnabled){}
    private final NotificationService service;private final MarketMember access;
    public NotificationController(NotificationService service,MarketMember access){this.service=service;this.access=access;}
    private Member me(){access.studentKey();return access.current();}
    private void sameOrigin(HttpServletRequest request){String origin=request.getHeader("Origin");if("cross-site".equals(request.getHeader("Sec-Fetch-Site"))||origin!=null&&!origin.equals(request.getScheme()+"://"+request.getHeader("Host")))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"현재 사이트에서 다시 시도해 주세요.");}
    @GetMapping public List<Map<String,Object>> list(){return service.list(me().getId());}
    @GetMapping("/settings") public NotificationService.Settings settings(){return service.settings(me());}
    @PutMapping("/settings") @ResponseStatus(HttpStatus.NO_CONTENT) public void settings(@RequestBody SettingsInput input,HttpServletRequest request){sameOrigin(request);service.settings(me(),new NotificationService.Settings(input.email(),input.phone(),Boolean.TRUE.equals(input.smsEnabled()),Boolean.TRUE.equals(input.emailEnabled()),Boolean.TRUE.equals(input.kakaoEnabled()),false));}
    @PostMapping("/{id}/read") @ResponseStatus(HttpStatus.NO_CONTENT) public void read(@PathVariable String id,HttpServletRequest request){sameOrigin(request);service.read(me().getId(),id);}
    @PostMapping("/{id}/retry") @ResponseStatus(HttpStatus.NO_CONTENT) public void retry(@PathVariable String id,HttpServletRequest request){sameOrigin(request);service.retry(me().getId(),id);}
}
