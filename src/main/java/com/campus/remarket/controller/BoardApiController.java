package com.campus.remarket.controller;
import com.campus.remarket.auth.MarketMember;
import com.campus.remarket.dto.BoardDto;
import com.campus.remarket.dto.BoardCommentDto;
import com.campus.remarket.portal.StaffAccess;
import com.campus.remarket.service.BoardService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/boards")
public class BoardApiController {
    private final MarketMember member;
    private final BoardService service;
    private final StaffAccess access;
    public BoardApiController(MarketMember member,BoardService service,StaffAccess access){this.member=member;this.service=service;this.access=access;}
    @GetMapping public List<BoardDto> selectAll(){return service.selectAll();}
    @GetMapping("/session") public Map<String,String> session(){member.key();return Map.of("csrfToken",access.csrf());}
    @GetMapping("/{id}") public BoardDto selectOne(@PathVariable Long id){return service.selectOne(id);}
    @PostMapping public BoardDto insert(@RequestBody BoardDto dto,@RequestHeader(value="X-CSRF-Token",required=false)String csrf){
        dto.setWriter(member.key());access.verify(csrf);var me=member.current();
        dto.setAuthorMemberId(me.getId());dto.setAuthorName(me.getName());dto.setSchoolName(me.getSchool()==null?"캠퍼스 이웃":me.getSchool().getName());
        service.insert(dto);return service.selectOne(dto.getPostId());
    }
    @PutMapping("/{id}") public BoardDto update(@PathVariable Long id,@RequestBody BoardDto dto,@RequestHeader(value="X-CSRF-Token",required=false)String csrf){member.board(id);access.verify(csrf);dto.setPostId(id);dto.setWriter(member.key());service.update(dto);return service.selectOne(id);}
    @DeleteMapping("/{id}") public void delete(@PathVariable Long id,@RequestHeader(value="X-CSRF-Token",required=false)String csrf){member.board(id);access.verify(csrf);service.delete(id);}
    public record CommentInput(String content){}
    @GetMapping("/{id}/comments") public List<BoardCommentDto> comments(@PathVariable Long id){return service.comments(id);}
    @PostMapping("/{id}/comments") public BoardCommentDto addComment(@PathVariable Long id,@RequestBody CommentInput input,@RequestHeader(value="X-CSRF-Token",required=false)String csrf){
        member.key();access.verify(csrf);var me=member.current();var dto=new BoardCommentDto();
        dto.setPostId(id);dto.setMemberId(me.getId());dto.setAuthorName(me.getName());dto.setSchoolName(me.getSchool()==null?"캠퍼스 이웃":me.getSchool().getName());dto.setContent(input.content());
        return service.addComment(dto);
    }
    @DeleteMapping("/{id}/comments/{commentId}") public void deleteComment(@PathVariable Long id,@PathVariable Long commentId,@RequestHeader(value="X-CSRF-Token",required=false)String csrf){member.key();access.verify(csrf);service.deleteComment(id,commentId,member.current().getId());}
}
