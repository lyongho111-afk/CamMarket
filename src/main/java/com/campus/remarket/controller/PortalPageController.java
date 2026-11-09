package com.campus.remarket.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** All three workspaces share the main application's HttpOnly login session. */
@Controller
public class PortalPageController {
    @GetMapping("/owner") public String ownerSlash(){return "redirect:/owner/";}
    @GetMapping("/owner/") public String owner(){return "forward:/owner/index.html";}
    @GetMapping("/admin") public String adminSlash(){return "redirect:/admin/";}
    @GetMapping("/admin/") public String admin(){return "forward:/admin/index.html";}
    @GetMapping("/manager.html") public String legacyManagers(){return "redirect:/owner/#managers";}
}
