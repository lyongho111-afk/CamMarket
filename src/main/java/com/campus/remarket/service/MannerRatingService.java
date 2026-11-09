package com.campus.remarket.service;
import com.campus.remarket.dao.*;
import com.campus.remarket.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class MannerRatingService {
    public record RatingState(boolean completed,boolean canRate,boolean submitted,String reviewerUnisno,String targetUnisno,Integer temp,String message) {}
    private final TradeDao trades;
    private final StudentDao students;
    private final MannerRatingDao ratings;
    private final TradeReviewDao reviews;
    private final int goodDelta,badDelta;
    public MannerRatingService(TradeDao trades,StudentDao students,MannerRatingDao ratings,TradeReviewDao reviews,
            @Value("${campus.rating.good-delta:1}") int goodDelta,@Value("${campus.rating.bad-delta:-1}") int badDelta) {
        if(goodDelta<0||goodDelta>100||badDelta>0||badDelta< -100)throw new IllegalArgumentException("평가 증감 설정을 확인하세요.");
        this.trades=trades;this.students=students;this.ratings=ratings;this.reviews=reviews;this.goodDelta=goodDelta;this.badDelta=badDelta;
    }
    private TradeDto order(String id){var o=trades.selectOne(id);if(o==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"거래가 없습니다.");return o;}
    private String target(TradeDto order,String actor){
        if(actor==null||order.getBuyer().equals(order.getSeller()))return null;
        if(actor.equals(order.getBuyer()))return order.getSeller();
        if(actor.equals(order.getSeller()))return order.getBuyer();
        return null;
    }
    public RatingState state(String id,String actor){
        var o=order(id);String recipient=target(o,actor);
        if(!o.isCompleted())return new RatingState(false,false,false,actor,null,null,"거래가 최종 완료되면 평가할 수 있습니다.");
        if(actor==null)return new RatingState(true,false,false,null,null,null,"로그인 후 평가할 수 있습니다.");
        if(recipient==null)return new RatingState(true,false,false,actor,null,null,"거래 당사자만 상대방을 평가할 수 있습니다.");
        var student=students.selectOne(recipient);
        boolean submitted=(ratings.selectOne(id,actor)!=null||reviews.countByOrderAndReviewer(id,actor)>0);
        boolean registered=student!=null&&students.selectOne(actor)!=null;
        return new RatingState(true,!submitted&&registered,submitted,actor,recipient,student==null?null:student.getTemp(),
            submitted?"이 거래의 평가를 이미 완료했습니다.":registered?"거래 상대의 매너를 평가해 주세요.":"회원 담당의 student 등록 연결이 필요합니다.");
    }
    @Transactional public RatingState rate(String id,String actor,String feedback){
        if(actor==null)throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"로그인이 필요합니다.");
        if(!"GOOD".equals(feedback)&&!"BAD".equals(feedback))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"평가 항목을 선택하세요.");
        if(trades.lockOne(id)==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"거래가 없습니다.");
        var o=order(id);
        if(!o.isCompleted())throw new ResponseStatusException(HttpStatus.CONFLICT,"완료된 거래만 평가할 수 있습니다.");
        String recipient=target(o,actor);
        if(recipient==null)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"거래 당사자만 상대방을 평가할 수 있습니다.");
        if((ratings.selectOne(id,actor)!=null||reviews.countByOrderAndReviewer(id,actor)>0))throw new ResponseStatusException(HttpStatus.CONFLICT,"거래당 한 번만 평가할 수 있습니다.");
        if(students.selectOne(actor)==null)throw new ResponseStatusException(HttpStatus.CONFLICT,"평가자의 학생 정보가 없습니다.");
        var student=students.lockOne(recipient);
        if(student==null)throw new ResponseStatusException(HttpStatus.CONFLICT,"상대방의 학생 정보가 없습니다.");
        int delta="GOOD".equals(feedback)?goodDelta:badDelta;
        int next=Math.max(0,Math.min(990,student.getTemp()+delta));
        var rating=new MannerRatingDto();rating.setOrderId(id);rating.setReviewerUnisno(actor);rating.setRecipientUnisno(recipient);
        rating.setFeedback(feedback);rating.setDelta(next-student.getTemp());
        ratings.insert(rating);students.updateTemp(recipient,next);
        return state(id,actor);
    }
}
