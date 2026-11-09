package com.campus.remarket;
import com.campus.remarket.dto.*;
import com.campus.remarket.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest @Transactional
class MyBatisFlowTests {
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    @Autowired SalesService sales;
    @Autowired ShopService shops;
    @Autowired BoardService board;
    @Autowired TradeService trades;
    @Test void xmlQueriesAndJoin(){assertEquals(8,sales.selectAll().size());assertEquals(3,shops.selectAll().size());assertFalse(shops.selectAll().stream().anyMatch(s->"캠퍼스 카페 (예시)".equals(s.getShopname())));assertEquals("맛집 추천",board.selectOne(1L).getCategoryName());assertNotNull(trades.selectOne("demo-reserved-2").getSaleName());}
    @Test void saleCrudDiscountAndBump(){var dto=new SalesDto();dto.setSeller("student");dto.setCategory("전공서적");dto.setName("검증용 책");dto.setOprice(28000);sales.insert(dto);assertNotNull(dto.getSelno());sales.startDiscount(dto.getSelno(),20);assertEquals(22400,sales.selectOne(dto.getSelno()).getCurrentPrice());assertThrows(IllegalStateException.class,()->sales.startDiscount(dto.getSelno(),30));sales.endDiscount(dto.getSelno());sales.bump(dto.getSelno());assertThrows(IllegalStateException.class,()->sales.bump(dto.getSelno()));dto.setName("수정 책");sales.update(dto);assertEquals("수정 책",sales.selectOne(dto.getSelno()).getName());sales.delete(dto.getSelno());}
    @Test void reservationUsesServerPriceAndKeepsIt(){var dto=new SalesDto();dto.setSeller("student");dto.setCategory("전공서적");dto.setName("예약 검증");dto.setOprice(28000);sales.insert(dto);sales.startDiscount(dto.getSelno(),20);var order=new TradeDto();order.setSelno(dto.getSelno());order.setBuyer("buyer");order.setAgreedPrice(1);trades.insert(order);assertEquals(22400,trades.selectOne(order.getOrderId()).getAgreedPrice());assertEquals("예약중",sales.selectOne(dto.getSelno()).getStatus());assertThrows(IllegalStateException.class,()->sales.delete(dto.getSelno()));trades.cancel(order.getOrderId());assertEquals("판매중",sales.selectOne(dto.getSelno()).getStatus());sales.endDiscount(dto.getSelno());assertEquals(22400,trades.selectOne(order.getOrderId()).getAgreedPrice());}
    @Test void shopAndBoardCrud(){var s=new ShopDto();s.setShopname("추가 카페");shops.insert(s);s.setBmenu("커피");shops.update(s);assertEquals("커피",shops.selectOne(s.getShopId()).getBmenu());shops.delete(s.getShopId());var b=new BoardDto();b.setCategoryId(5);b.setWriter("student");b.setTitle("제안");b.setContent("검증");board.insert(b);b.setTitle("수정 제안");board.update(b);assertEquals("제안",board.selectOne(b.getPostId()).getCategoryName());board.delete(b.getPostId());}
    @Test void invalidRatesRejected(){assertThrows(IllegalArgumentException.class,()->sales.startDiscount(7L,100));assertThrows(IllegalArgumentException.class,()->sales.startDiscount(7L,0));}

    @Test void meetingAndCompletionAreStored(){var dto=new SalesDto();dto.setSeller("student");dto.setCategory("전공서적");dto.setName("장소 검증");dto.setOprice(28000);sales.insert(dto);var order=new TradeDto();order.setBuyer("buyer");order.setSelno(dto.getSelno());trades.insert(order);order.setMeetingPlace("학생회관");order.setLatitude(new java.math.BigDecimal("37.4507"));order.setLongitude(new java.math.BigDecimal("126.6538"));trades.meeting(order);assertEquals("학생회관",trades.selectOne(order.getOrderId()).getMeetingPlace());trades.complete(order.getOrderId());assertEquals("COMPLETED",trades.selectOne(order.getOrderId()).getStatus());assertEquals("거래완료",sales.selectOne(dto.getSelno()).getStatus());assertThrows(IllegalStateException.class,()->trades.cancel(order.getOrderId()));}
    @Test void noPaymentApprovalWithoutKeys(){var payments=new PaymentService(null,"","");assertFalse(payments.ready());assertThrows(IllegalStateException.class,()->payments.prepare("demo-reserved-2"));}

    @Test void expiredDiscountRestoresPriceAndAllowsNewDay(){var dto=new SalesDto();dto.setSeller("student");dto.setCategory("전공서적");dto.setName("만료 검증");dto.setOprice(28000);sales.insert(dto);sales.startDiscount(dto.getSelno(),20);jdbc.update("UPDATE sales SET starttime=DATEADD('HOUR',-25,CURRENT_TIMESTAMP),endtime=DATEADD('HOUR',-1,CURRENT_TIMESTAMP) WHERE selno=?",dto.getSelno());assertEquals(28000,sales.selectOne(dto.getSelno()).getCurrentPrice());sales.startDiscount(dto.getSelno(),30);assertEquals(19600,sales.selectOne(dto.getSelno()).getCurrentPrice());}
    @Test void exposureOrderMatchesPolicy(){assertEquals(java.util.List.of(8L,7L,6L,5L,4L,3L,2L,1L),sales.selectAll().stream().map(SalesDto::getSelno).toList());}
}
