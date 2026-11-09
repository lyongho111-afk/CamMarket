package com.campus.remarket.dao;
import com.campus.remarket.dto.BoardDto;
import com.campus.remarket.dto.BoardCommentDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
@Mapper
public interface BoardDao {
    List<BoardDto> selectAll();
    BoardDto selectOne(@Param("id") Long id);
    int insert(BoardDto dto);
    int update(BoardDto dto);
    int delete(@Param("id") Long id);
    boolean categoryExists(@Param("id") Integer id);
    List<BoardCommentDto> comments(@Param("postId") Long postId);
    BoardCommentDto comment(@Param("id") Long id);
    int insertComment(BoardCommentDto dto);
    int deleteComment(@Param("id") Long id, @Param("memberId") Long memberId);
}
