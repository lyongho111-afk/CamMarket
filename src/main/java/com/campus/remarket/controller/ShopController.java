package com.campus.remarket.controller;
import com.campus.remarket.service.ShopService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
/** 학생용 가게 페이지: 가게 상태와 공개 정보만 조회합니다. */
@Controller @RequestMapping("/shops")
public class ShopController {
    private final ShopService service;
    public ShopController(ShopService service){this.service=service;}
    @GetMapping("/selectAll") public String selectAll(){return "redirect:/sales/selectAll#shops";}
    @GetMapping("/selectOne") public String selectOne(@RequestParam Long id,Model model){var shop=service.selectOne(id);if(!Boolean.TRUE.equals(shop.getChecked()))throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,"가게를 찾을 수 없습니다.");model.addAttribute("dto",shop);return "shops/selectOne";}
}
