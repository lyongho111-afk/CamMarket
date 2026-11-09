package com.campus.remarket.service;

import com.campus.remarket.dao.TradeReviewDao;
import com.campus.remarket.dao.TradeDao;
import com.campus.remarket.dao.SalesDao;
import com.campus.remarket.dao.StudentDao;
import com.campus.remarket.dao.MannerRatingDao;
import com.campus.remarket.dto.TradeDto;
import com.campus.remarket.dto.SalesDto;
import com.campus.remarket.dto.TradeReviewDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class TradeReviewService {

    private final TradeReviewDao reviewDao;
    private final TradeDao tradeDao;
    private final SalesDao salesDao;
    private final StudentDao students;
    private final MannerRatingDao legacyRatings;

    public TradeReviewService(TradeReviewDao reviewDao, TradeDao tradeDao, SalesDao salesDao, StudentDao students, MannerRatingDao legacyRatings) {
        this.students=students;this.legacyRatings=legacyRatings;
        this.reviewDao = reviewDao;
        this.tradeDao = tradeDao;
        this.salesDao = salesDao;
    }

    @Transactional
    public TradeReviewDto submitReview(TradeReviewDto dto) {
        // 1. 필수값 체크
        if (dto.getOrderId() == null || dto.getOrderId().isBlank()) {
            throw new IllegalArgumentException("주문번호가 누락되었습니다.");
        }
        if (dto.getScore() == null || dto.getScore() < 1 || dto.getScore() > 5) {
            throw new IllegalArgumentException("평점은 1점에서 5점 사이여야 합니다.");
        }
        if (dto.getReviewerId() == null || dto.getReviewerId().isBlank()) {
            throw new IllegalArgumentException("평가자 ID가 누락되었습니다.");
        }

        if (dto.getOrderId().length()>50 || dto.getReviewerId().length()>50 || dto.getAttitude()!=null&&dto.getAttitude().length()>50 || dto.getContent()!=null&&dto.getContent().length()>500)
            throw new IllegalArgumentException("주문번호·태도(50자)·후기(500자) 길이를 확인하세요.");
        // 같은 거래의 두 평가 API가 동시에 제출되어도 하나만 처리합니다.
        tradeDao.lockOne(dto.getOrderId());
        // 2. 거래 조회
        TradeDto trade = tradeDao.selectOne(dto.getOrderId());
        if (trade == null) {
            throw new IllegalArgumentException("해당 거래를 찾을 수 없습니다.");
        }

        // 3. 거래 완료 상태 검증 (YS 조원이 정의한 COMPLETED 상태 확인)
        if (!trade.isCompleted()) {
            throw new IllegalStateException("거래가 최종 완료된 상태에서만 매너 평가를 진행할 수 있습니다.");
        }

        // 4. 판매자/구매자 당사자 판별 및 본인 평가 차단
        SalesDto sale = salesDao.selectOne(trade.getSelno());
        String sellerId = sale.getSeller();
        String buyerId = trade.getBuyer();

        String targetId;
        if (dto.getReviewerId().equals(buyerId)) {
            targetId = sellerId;
        } else if (dto.getReviewerId().equals(sellerId)) {
            targetId = buyerId;
        } else {
            throw new IllegalArgumentException("해당 거래의 당사자(구매자 또는 판매자)만 평가할 수 있습니다.");
        }
        dto.setTargetId(targetId);

        if (dto.getReviewerId().equals(dto.getTargetId())) {
            throw new IllegalArgumentException("자기 자신을 평가할 수 없습니다.");
        }

        // 5. 중복 평가 방지
        if (reviewDao.countByOrderAndReviewer(dto.getOrderId(), dto.getReviewerId()) > 0 || legacyRatings.selectOne(dto.getOrderId(), dto.getReviewerId()) != null) {
            throw new IllegalStateException("이미 해당 거래에 대한 매너 평가를 완료하셨습니다.");
        }

        if (students.selectOne(dto.getReviewerId()) == null) throw new IllegalStateException("평가자의 학생 정보가 없습니다.");
        var targetStudent=students.lockOne(targetId);
        if (targetStudent == null) throw new IllegalStateException("상대방의 학생 정보가 없습니다.");
        int beforeTemp=targetStudent.getTemp();
        // 6. 점수별 매너온도 계산 (5점:+0.5, 4점:+0.2, 3점:0, 2점:-0.3, 1점:-0.8)
        int delta = switch (dto.getScore()) {
            case 5 -> 5;
            case 4 -> 2;
            case 3 -> 0;
            case 2 -> -3;
            case 1 -> -8;
            default -> 0;
        };
        dto.setTemperatureDelta(delta);

        // 7. DB 업데이트
        reviewDao.updateStudentTemperature(targetId, delta);
        // 상·하한에서 실제 반영된 증감량을 기록합니다.
        Integer updatedTemp = reviewDao.getStudentTemperature(targetId);
        dto.setTemperatureDelta(updatedTemp-beforeTemp);
        reviewDao.insert(dto);

        // 8. 결과 DTO 반환
        dto.setCurrentTemperature(updatedTemp != null ? updatedTemp / 10.0 : 36.5);
        dto.setAppliedDelta(dto.getTemperatureDelta() / 10.0);

        return dto;
    }

    public List<TradeReviewDto> getReviewsForUser(String userId) {
        return reviewDao.selectByTargetId(userId);
    }
}