package com.campus.remarket.controller;
import com.campus.remarket.auth.MarketMember;
import com.campus.remarket.dto.SalesDto;
import com.campus.remarket.service.SalesService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
@Controller
@RequestMapping("/sales")
public class SalesController {
    private final MarketMember member;
    private final SalesService service;
    public SalesController(MarketMember member,SalesService service){this.member=member;this.service=service;}
    @GetMapping("/selectAll") public String selectAll(Model model){return "market/index";}
    @GetMapping("/manage") public String manage(Model model){model.addAttribute("list",service.selectAll());return "sales/selectAll";}
    @GetMapping("/selectOne") public String selectOne(@RequestParam Long id,Model model){model.addAttribute("dto",service.selectOne(id));return "sales/selectOne";}
    @GetMapping("/insert") public String insertForm(Model model){model.addAttribute("dto",new SalesDto());model.addAttribute("editing",false);return "sales/form";}
    @PostMapping("/insert") public String insert(SalesDto dto){dto.setSeller(member.studentKey());dto.setSellerMemberId(member.current().getId());service.insert(dto);return "redirect:/sales/selectAll";}
    @GetMapping("/update") public String updateForm(@RequestParam Long id,Model model){member.sale(id);model.addAttribute("dto",service.selectOne(id));model.addAttribute("editing",true);return "sales/form";}
    @PostMapping("/update") public String update(SalesDto dto){member.sale(dto.getSelno());dto.setSeller(member.studentKey());service.update(dto);return "redirect:/sales/selectAll";}
    @PostMapping("/delete") public String delete(@RequestParam Long id){member.sale(id);service.delete(id);return "redirect:/sales/selectAll";}

    @PostMapping("/discount") public String discount(@RequestParam Long selno,@RequestParam int rate){member.sale(selno);service.startDiscount(selno,rate);return "redirect:/sales/selectOne?id="+selno;}
    @PostMapping("/discountEnd") public String discountEnd(@RequestParam Long selno){member.sale(selno);service.endDiscount(selno);return "redirect:/sales/selectOne?id="+selno;}
    @PostMapping("/bump") public String bump(@RequestParam Long selno){member.sale(selno);service.bump(selno);return "redirect:/sales/selectOne?id="+selno;}
}
