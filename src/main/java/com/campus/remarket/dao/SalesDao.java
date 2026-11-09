package com.campus.remarket.dao;
import com.campus.remarket.dto.SalesDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
@Mapper
public interface SalesDao {
    List<SalesDto> selectAll();
    SalesDto selectOne(@Param("id") Long id);
    int insert(SalesDto dto);
    int update(SalesDto dto);
    int delete(@Param("id") Long id);
    int discountStart(@Param("selno") Long selno, @Param("rate") int rate);
    int discountEnd(@Param("selno") Long selno);
    int bump(@Param("selno") Long selno);
    int statusUpdate(@Param("selno") Long selno, @Param("status") String status);
    Long lockSale(@Param("selno") Long selno);
    int orderCount(@Param("selno") Long selno);
}
