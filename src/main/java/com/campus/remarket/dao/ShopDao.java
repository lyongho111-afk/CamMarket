package com.campus.remarket.dao;
import com.campus.remarket.dto.ShopDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
@Mapper
public interface ShopDao {
    List<ShopDto> selectAll();
    ShopDto selectOne(@Param("id") Long id);
    int insert(ShopDto dto);
    int update(ShopDto dto);
    int delete(@Param("id") Long id);
}
