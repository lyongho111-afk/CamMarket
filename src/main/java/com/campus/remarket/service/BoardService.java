package com.campus.remarket.service;
import com.campus.remarket.dao.BoardDao;
import com.campus.remarket.dto.BoardDto;
import com.campus.remarket.dto.BoardCommentDto;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
@Service
public class BoardService {
    private final BoardDao dao;
    private final CampusScope scope;
    public BoardService(BoardDao dao,CampusScope scope){this.dao=dao;this.scope=scope;}
    public List<BoardDto> selectAll(){return dao.selectAll().stream().filter(x->scope.visible(x.getSchoolId())).toList();}
    public BoardDto selectOne(Long id){var dto=dao.selectOne(id);if(dto==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"게시글을 찾을 수 없습니다.");scope.require(dto.getSchoolId());return dto;}
    private void validate(BoardDto dto){
        requireText(dto.getWriter(),"작성자 ID");
        dto.setTitle(text(dto.getTitle(),"제목",150));
        dto.setContent(text(dto.getContent(),"내용",3000));
        if(dto.getCategoryId()==null||!dao.categoryExists(dto.getCategoryId()))throw new IllegalArgumentException("게시판 분류를 선택해 주세요.");
    }
    private String text(String value,String label,int max){requireText(value,label);var trimmed=value.strip();if(trimmed.length()>max)throw new IllegalArgumentException(label+"은 "+max+"자까지 입력할 수 있어요.");return trimmed;}
    private void requireText(String value,String label){if(value==null||value.isBlank())throw new IllegalArgumentException(label+"을 입력하세요.");}
    @Transactional public void insert(BoardDto dto){dto.setSchoolId(scope.id());validate(dto);dao.insert(dto);}
    @Transactional public void update(BoardDto dto){validate(dto);selectOne(dto.getPostId());dao.update(dto);}
    @Transactional public void delete(Long id){selectOne(id);dao.delete(id);}
    public List<BoardCommentDto> comments(Long id){selectOne(id);return dao.comments(id);}
    @Transactional public BoardCommentDto addComment(BoardCommentDto dto){
        selectOne(dto.getPostId());
        if(dto.getMemberId()==null)throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"로그인이 필요해요.");
        dto.setContent(text(dto.getContent(),"댓글",1000));
        dao.insertComment(dto);return dao.comment(dto.getCommentId());
    }
    @Transactional public void deleteComment(Long postId,Long commentId,Long memberId){
        selectOne(postId);var comment=dao.comment(commentId);
        if(comment==null||!postId.equals(comment.getPostId()))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"댓글을 찾을 수 없어요.");
        if(!comment.getMemberId().equals(memberId))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"내가 쓴 댓글만 삭제할 수 있어요.");
        dao.deleteComment(commentId,memberId);
    }
}
