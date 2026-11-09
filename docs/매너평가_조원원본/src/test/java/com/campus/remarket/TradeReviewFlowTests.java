package com.campus.remarket;

import com.campus.remarket.dto.*;
import com.campus.remarket.service.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class TradeReviewFlowTests {

    @Autowired TradeService tradeService;
    @Autowired SalesService salesService;
    @Autowired TradeReviewService reviewService;

    @Test
    @DisplayName("거래 완료 후 5점 평가 시 매너온도가 36.5도에서 37.0도로 0.5도 상승한다")
    void reviewIncreasesMannerTemperature() {
        // 1. 판매글 생성
        SalesDto sale = new SalesDto();
        sale.setSeller("student");
        sale.setCategory("전공서적");
        sale.setName("자바 완벽 가이드");
        sale.setOprice(30000);
        salesService.insert(sale);

        // 2. 구매 예약 생성
        TradeDto trade = new TradeDto();
        trade.setSelno(sale.getSelno());
        trade.setBuyer("buyer");
        tradeService.insert(trade);

        // 3. YS 조원의 실제 서비스 메서드로 대면 거래 완료 처리! (COMPLETED 상태)
        tradeService.complete(trade.getOrderId());

        // 4. 구매자가 판매자(student)에게 5점 평가 제출
        TradeReviewDto review = new TradeReviewDto();
        review.setOrderId(trade.getOrderId());
        review.setReviewerId("buyer");
        review.setScore(5);
        review.setContent("시간 약속도 칼같고 정말 친절하십니다!");

        TradeReviewDto result = reviewService.submitReview(review);

        // 5. 검증: 온도 37.0도 반영 확인
        assertEquals(37.0, result.getCurrentTemperature());
        assertEquals(0.5, result.getAppliedDelta());
    }

    @Test
    @DisplayName("동일 거래에 대해 2회 중복 평가 시 예외가 발생한다")
    void duplicateReviewThrowsException() {
        SalesDto sale = new SalesDto();
        sale.setSeller("student");
        sale.setCategory("전공서적");
        sale.setName("운영체제");
        sale.setOprice(25000);
        salesService.insert(sale);

        TradeDto trade = new TradeDto();
        trade.setSelno(sale.getSelno());
        trade.setBuyer("buyer");
        tradeService.insert(trade);

        tradeService.complete(trade.getOrderId());

        TradeReviewDto r1 = new TradeReviewDto();
        r1.setOrderId(trade.getOrderId());
        r1.setReviewerId("buyer");
        r1.setScore(4);
        reviewService.submitReview(r1);

        // 2차 평가 시도 -> IllegalStateException 발생 확인
        assertThrows(IllegalStateException.class, () -> reviewService.submitReview(r1));
    }
}