package com.campus.remarket.controller;

import com.campus.remarket.service.MemberProfileService;
import org.springframework.web.bind.annotation.*;

@RestController
public class MemberProfileApiController {
    private final MemberProfileService profiles;
    public MemberProfileApiController(MemberProfileService profiles){this.profiles=profiles;}
    @GetMapping("/api/profiles/me") public MemberProfileService.Profile me(){return profiles.me();}
    @GetMapping("/api/products/{id}/seller-profile") public MemberProfileService.Profile seller(@PathVariable Long id){return profiles.seller(id);}
}
