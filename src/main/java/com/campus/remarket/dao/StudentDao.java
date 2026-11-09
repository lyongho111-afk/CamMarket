package com.campus.remarket.dao;
import com.campus.remarket.dto.StudentDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
@Mapper
public interface StudentDao {
    StudentDto selectOne(@Param("unisno") String unisno);
    StudentDto forMember(@Param("memberId") Long memberId);
    StudentDto lockOne(@Param("unisno") String unisno);
    int insert(StudentDto dto);
    int updateTemp(@Param("unisno") String unisno,@Param("temp") int temp);
}
