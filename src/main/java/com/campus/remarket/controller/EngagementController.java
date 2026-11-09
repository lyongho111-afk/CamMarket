package com.campus.remarket.controller;
import com.campus.remarket.service.EngagementService;
import com.campus.remarket.portal.StaffAccess;
import demo.campus.campusremarketmember.member.util.LoginSession;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/engagement")
public class EngagementController {
    private final EngagementService service;private final StaffAccess access;private final HttpServletRequest request;
    public EngagementController(EngagementService service,StaffAccess access,HttpServletRequest request){this.service=service;this.access=access;this.request=request;}
    @GetMapping("/state") public List<EngagementService.State> state(@RequestParam(defaultValue="")String targets){var s=request.getSession(false);return service.states(targets,s==null?null:LoginSession.memberId(s));}
    @GetMapping("/session") public Map<String,String> session(){access.current();return Map.of("csrfToken",access.csrf());}
    public record Input(Boolean active){}
    @PutMapping("/{type}/{id}/{kind}") public EngagementService.State set(@PathVariable String type,@PathVariable long id,@PathVariable String kind,@RequestBody Input input,@RequestHeader(value="X-CSRF-Token",required=false)String csrf){
        var me=access.current();access.verify(csrf);if(input.active()==null)throw new IllegalArgumentException("반응 상태를 선택하세요.");
        String value=switch(kind){case "like"->"LIKE";case "bookmark"->"BOOKMARK";default->throw new IllegalArgumentException("반응 종류를 확인하세요.");};
        return service.set(me.getId(),new EngagementService.Target(type,id),value,input.active());
    }
    @GetMapping({"/likes","/bookmarks"}) public Map<String,Object> bookmarks(@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="ALL")String type){return service.bookmarks(access.current().getId(),page,type);}
}
