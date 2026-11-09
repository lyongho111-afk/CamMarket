package com.campus.remarket.controller;
import com.campus.remarket.auth.MarketMember;
import demo.campus.campusremarketmember.member.dto.MemberView;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController
public class MemberIntegrationController {
    private final MarketMember member;
    public MemberIntegrationController(MarketMember member) {this.member=member;}
    @GetMapping("/api/session") public Map<String,Object> session() {
        var me=member.current();
        return me==null ? Map.of("authenticated",false) :
            Map.of("authenticated",true,"member",MemberView.of(me),"actorKey",member.key());
    }
}
