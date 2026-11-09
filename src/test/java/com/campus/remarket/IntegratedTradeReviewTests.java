package com.campus.remarket;
import com.campus.remarket.dto.*;
import com.campus.remarket.service.*;
import com.campus.remarket.dao.StudentDao;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest @Transactional
class IntegratedTradeReviewTests {
 @Autowired TradeService trades;
 @Autowired SalesService sales;
 @Autowired TradeReviewService reviews;
 @Autowired MannerRatingService legacy;
 @Autowired StudentDao students;
 @Autowired JdbcTemplate db;
 String completed(){var s=new SalesDto();s.setSeller("demo-seller");s.setCategory("전공서적");s.setName("통합 평가용 교재");s.setOprice(10000);sales.insert(s);var t=new TradeDto();t.setSelno(s.getSelno());t.setBuyer("demo-buyer");trades.insert(t);trades.complete(t.getOrderId());return t.getOrderId();}
 TradeReviewDto input(String order,int score){var r=new TradeReviewDto();r.setOrderId(order);r.setReviewerId("demo-buyer");r.setScore(score);r.setAttitude("시간엄수, 친절매너");r.setContent("약속 시간을 잘 지켰어요.");return r;}
 @Test void originalFiveScoreRulesAndSellerCardsUseActualStudentTemperature(){
  int[] delta={-8,-3,0,2,5};for(int score=1;score<=5;score++){db.update("UPDATE student SET temp=365 WHERE unisno='demo-seller'");String id=completed();var result=reviews.submitReview(input(id,score));assertEquals(365+delta[score-1],students.selectOne("demo-seller").getTemp());assertEquals(delta[score-1]/10.0,result.getAppliedDelta());assertEquals((365+delta[score-1])/10.0,result.getCurrentTemperature());assertTrue(legacy.state(id,"demo-buyer").submitted());assertEquals((365+delta[score-1])/10.0,sales.selectOne(1L).getSellerTemperature().doubleValue(),0.00001);}
 }
 @Test void binaryAndFiveScoreEndpointsCannotBothChangeTemperatureForSameParticipant(){
  String first=completed();legacy.rate(first,"demo-buyer","GOOD");assertThrows(IllegalStateException.class,()->reviews.submitReview(input(first,5)));assertEquals(366,students.selectOne("demo-seller").getTemp());
  String second=completed();reviews.submitReview(input(second,5));assertThrows(org.springframework.web.server.ResponseStatusException.class,()->legacy.rate(second,"demo-buyer","GOOD"));assertEquals(371,students.selectOne("demo-seller").getTemp());
 }
 @Test void originalBoundsStoreActualAppliedDeltaAndAcceptExistingKoreanCompletedStatus(){
  String first=completed();db.update("UPDATE student SET temp=988 WHERE unisno='demo-seller'");db.update("UPDATE trade_orders SET status='거래완료' WHERE order_id=?",first);var r=reviews.submitReview(input(first,5));assertEquals(99.0,r.getCurrentTemperature());assertEquals(0.2,r.getAppliedDelta());assertEquals(2,r.getTemperatureDelta());
  String second=completed();db.update("UPDATE student SET temp=3 WHERE unisno='demo-seller'");r=reviews.submitReview(input(second,1));assertEquals(0.0,r.getCurrentTemperature());assertEquals(-0.3,r.getAppliedDelta());
 }
 @Test void invalidLongReviewDoesNotChangeTemperatureOrInsertRecord(){
  String id=completed();var r=input(id,5);r.setContent("가".repeat(501));assertThrows(IllegalArgumentException.class,()->reviews.submitReview(r));assertEquals(365,students.selectOne("demo-seller").getTemp());assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM trade_reviews WHERE order_id=?",Integer.class,id));
 }
}
