package com.campus.remarket;
import com.campus.remarket.dao.*;
import com.campus.remarket.service.*;
import com.campus.remarket.dto.*;
import com.campus.remarket.controller.RatingSession;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest @Transactional
class MannerRatingTests {
    @Autowired MannerRatingService ratings;
    @Autowired StudentDao students;
    @Autowired MannerRatingDao dao;
    @Autowired TradeService trades;
    @Autowired SalesService sales;
    @Autowired JdbcTemplate jdbc;
    @Autowired RatingSession ratingSession;
    @Autowired demo.campus.campusremarketmember.member.service.MemberService members;
    @Test void studentIdPreservesLeadingZerosAndTemperatureDefaults(){
        jdbc.update("INSERT INTO student(unisno) VALUES (?)","00123456");
        assertEquals("00123456",students.selectOne("00123456").getUnisno());assertEquals(365,students.selectOne("00123456").getTemp());
    }
    @Test void completedTradeRatesOnceAndUpdatesAllSellerCards(){
        assertTrue(ratings.state("demo-completed-1","demo-buyer").canRate());
        var result=ratings.rate("demo-completed-1","demo-buyer","GOOD");
        assertTrue(result.submitted());assertFalse(result.canRate());assertEquals(366,students.selectOne("demo-seller").getTemp());
        assertEquals(36.6,sales.selectOne(1L).getSellerTemperature().doubleValue(),0.0001);
        assertThrows(ResponseStatusException.class,()->ratings.rate("demo-completed-1","demo-buyer","GOOD"));
        assertEquals(366,students.selectOne("demo-seller").getTemp());
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM manner_ratings WHERE order_id='demo-completed-1'",Integer.class));
    }
    @Test void koreanCompletionStatusAlsoActivatesRating(){
        jdbc.update("UPDATE trade_orders SET status='거래완료' WHERE order_id='demo-completed-1'");
        assertTrue(trades.selectOne("demo-completed-1").isCompleted());
        assertTrue(ratings.state("demo-completed-1","demo-buyer").canRate());ratings.rate("demo-completed-1","demo-buyer","BAD");
        assertEquals(364,students.selectOne("demo-seller").getTemp());
    }
    @Test void serverRejectsPrematureOutsiderAndInvalidRatings(){
        assertFalse(ratings.state("demo-reserved-2","demo-buyer").canRate());
        assertThrows(ResponseStatusException.class,()->ratings.rate("demo-reserved-2","demo-buyer","GOOD"));
        assertThrows(ResponseStatusException.class,()->ratings.rate("demo-completed-1","outsider","GOOD"));
        assertThrows(ResponseStatusException.class,()->ratings.rate("demo-completed-1","demo-buyer","UNEXPECTED"));
        assertEquals(365,students.selectOne("demo-seller").getTemp());assertNull(dao.selectOne("demo-completed-1","demo-buyer"));
    }
    @Test void participantsCanEachRateTheirCounterpartyOnce(){
        ratings.rate("demo-completed-1","demo-buyer","GOOD");ratings.rate("demo-completed-1","demo-seller","BAD");
        assertEquals("demo-seller",dao.selectOne("demo-completed-1","demo-buyer").getRecipientUnisno());
        assertEquals("demo-buyer",dao.selectOne("demo-completed-1","demo-seller").getRecipientUnisno());
        assertEquals(364,students.selectOne("demo-buyer").getTemp());
    }
    @Test void completingThroughTradeServiceActivatesRating(){
        trades.complete("demo-reserved-2");assertTrue(ratings.state("demo-reserved-2","demo-buyer").canRate());
        ratings.rate("demo-reserved-2","demo-buyer","GOOD");assertEquals(366,students.selectOne("demo-seller").getTemp());
    }
    @Test void temperatureBoundsAreRespected(){
        jdbc.update("UPDATE student SET temp=990 WHERE unisno='demo-seller'");ratings.rate("demo-completed-1","demo-buyer","GOOD");
        assertEquals(990,students.selectOne("demo-seller").getTemp());assertEquals(0,dao.selectOne("demo-completed-1","demo-buyer").getDelta());
    }
    @Test void realLoginModeUsesSessionIdentityAndRequiresCsrf(){
        var session=ratingSession;var request=new MockHttpServletRequest();assertNull(session.actor(request));
        assertThrows(ResponseStatusException.class,()->session.verify(request,"fake"));
        var student=new StudentDto();student.setUnisno("00123456");request.getSession().setAttribute("loginStudent",student);
        assertNull(session.actor(request)); // legacy attributes alone cannot impersonate a real logged-in member
        var school=members.schools().stream().filter(s->s.getName().equals("인하대학교")).findFirst().orElseThrow();
        var me=members.login(school.getId(),"20261234","campus1234!");
        request.getSession().setAttribute("loginMemberId",me.getId());
        request.getSession().setAttribute("loginUnisno",me.getLoginId());
        assertEquals("20261234",session.actor(request));String csrf=session.csrf(request);session.verify(request,csrf);
        request.getSession().setAttribute("loginUnisno","forged");assertNull(session.actor(request));
        assertFalse(session.demo(request));
    }
}
