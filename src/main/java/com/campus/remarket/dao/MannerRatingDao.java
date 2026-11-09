package com.campus.remarket.dao;
import com.campus.remarket.dto.MannerRatingDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
@Mapper
public interface MannerRatingDao {
    MannerRatingDto selectOne(@Param("orderId") String orderId,@Param("reviewer") String reviewer);
    int insert(MannerRatingDto dto);
}
