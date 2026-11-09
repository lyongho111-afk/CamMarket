package com.campus.remarket.dao;

import com.campus.remarket.dto.TradeReviewDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface TradeReviewDao {
    int insert(TradeReviewDto dto);
    int countByOrderAndReviewer(@Param("orderId") String orderId, @Param("reviewerId") String reviewerId);
    Integer getStudentTemperature(@Param("userId") String userId);
    int updateStudentTemperature(@Param("userId") String userId, @Param("delta") int delta);
    List<TradeReviewDto> selectByTargetId(@Param("targetId") String targetId);
}