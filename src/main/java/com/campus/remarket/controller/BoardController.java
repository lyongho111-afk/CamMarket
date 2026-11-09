package com.campus.remarket.controller;
import com.campus.remarket.auth.MarketMember;
import com.campus.remarket.dto.BoardDto;
import com.campus.remarket.service.BoardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
@Controller
@RequestMapping("/board")
public class BoardController {
    private final MarketMember member;
    private final BoardService service;
    public BoardController(MarketMember member,BoardService service){this.member=member;this.service=service;}
    @GetMapping("/selectAll") public String selectAll(Model model){model.addAttribute("list",service.selectAll());return "board/selectAll";}
    @GetMapping("/selectOne") public String selectOne(@RequestParam Long id,Model model){model.addAttribute("dto",service.selectOne(id));return "board/selectOne";}
    @GetMapping("/insert") public String insertForm(Model model){model.addAttribute("dto",new BoardDto());model.addAttribute("editing",false);return "board/form";}
    @PostMapping("/insert") public String insert(BoardDto dto){dto.setWriter(member.key());service.insert(dto);return "redirect:/board/selectAll";}
    @GetMapping("/update") public String updateForm(@RequestParam Long id,Model model){member.board(id);model.addAttribute("dto",service.selectOne(id));model.addAttribute("editing",true);return "board/form";}
    @PostMapping("/update") public String update(BoardDto dto){member.board(dto.getPostId());dto.setWriter(member.key());service.update(dto);return "redirect:/board/selectAll";}
    @PostMapping("/delete") public String delete(@RequestParam Long id){member.board(id);service.delete(id);return "redirect:/board/selectAll";}
}
