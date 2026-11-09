package com.campus.remarket.controller;
import com.campus.remarket.auth.MarketMember;
import com.campus.remarket.service.MannerRatingService;
import jakarta.servlet.http.HttpServletRequest;
import com.campus.remarket.dto.TradeDto;
import com.campus.remarket.service.TradeService;
import com.campus.remarket.service.SalesService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
@Controller @RequestMapping("/trades")
public class TradeController {
    private final MarketMember member;
    private final TradeService service;
    private final SalesService sales;
    private final MannerRatingService ratings;
    private final RatingSession ratingSession;
    public TradeController(MarketMember member,TradeService service,SalesService sales,MannerRatingService ratings,RatingSession ratingSession){this.ratings=ratings;this.ratingSession=ratingSession;this.member=member;this.service=service;this.sales=sales;}
    @GetMapping("/selectAll") public String list(Model model){model.addAttribute("list",member.visibleTrades());return "trades/selectAll";}
    @GetMapping("/selectOne") public String detail(@RequestParam String id,Model model,HttpServletRequest request){member.trade(id);model.addAttribute("dto",service.selectOne(id));model.addAttribute("rating",ratings.state(id,ratingSession.actor(request)));model.addAttribute("ratingCsrf",ratingSession.csrf(request));model.addAttribute("ratingDemo",false);return "trades/selectOne";}
    @GetMapping("/insert") public String form(@RequestParam Long selno,Model model){var dto=new TradeDto();dto.setSelno(selno);model.addAttribute("dto",dto);model.addAttribute("sale",sales.selectOne(selno));return "trades/form";}
    @PostMapping("/insert") public String insert(TradeDto dto){member.reserve(dto.getSelno());dto.setBuyer(member.studentKey());service.insert(dto);return "redirect:/trades/selectOne?id="+dto.getOrderId();}
    @PostMapping("/cancel") public String cancel(@RequestParam String id){member.trade(id);service.cancel(id);return "redirect:/trades/selectOne?id="+id;}
}
