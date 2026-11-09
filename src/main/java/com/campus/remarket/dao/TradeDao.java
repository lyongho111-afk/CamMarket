package com.campus.remarket.dao;
import com.campus.remarket.dto.TradeDto;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
@Mapper
public interface TradeDao {
    List<TradeDto> selectAll();
    TradeDto selectOne(@Param("id") String id);
    TradeDto lockOne(@Param("id") String id);
    int insert(TradeDto dto);
    int paymentReady(@Param("id") String id);
    int paymentConfirmed(@Param("id") String id,@Param("key") String key);
    int complete(@Param("id") String id);
    int meeting(TradeDto dto);
    int cancel(@Param("id") String id);
}
