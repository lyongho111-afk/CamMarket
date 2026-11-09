package com.campus.remarket;

import com.campus.remarket.dto.*;
import com.campus.remarket.service.*;
import java.net.*;
import java.net.http.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
 "spring.datasource.url=jdbc:h2:mem:ratingsummarytests;MODE=MySQL;DB_CLOSE_DELAY=-1",
 "campus.notifications.initial-delay-ms=3600000"})
class RatingSummaryTests {
 @LocalServerPort int port;
 @Autowired SalesService sales;
 @Autowired TradeService trades;
 @Autowired TradeReviewService reviews;
 @Autowired MannerRatingService legacy;
 @Autowired JdbcTemplate db;
 final JsonMapper json=JsonMapper.builder().build();
 HttpResponse<String> call(HttpClient client,String method,String path,String body)throws Exception{
  var request=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path));SecurityTestClient.csrf(client,request,method,path,port);if(body!=null)request.header("Content-Type","application/json");
  var response=client.send(request.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());assertEquals(200,response.statusCode(),response.body());return response;
 }
 JsonNode dashboard(HttpClient client)throws Exception{return json.readTree(call(client,"GET","/api/admin/dashboard",null).body());}
 String completed(){var sale=new SalesDto();sale.setSeller("demo-seller");sale.setCategory("전공서적");sale.setName("평가 집계 검증용 교재");sale.setOprice(1000);sales.insert(sale);var order=new TradeDto();order.setSelno(sale.getSelno());order.setBuyer("demo-buyer");trades.insert(order);trades.complete(order.getOrderId());return order.getOrderId();}
 @Test void allFiveScoresAndLegacyVotesCountEvenWhenTemperatureIsClamped()throws Exception{
  var browser=HttpClient.newBuilder().cookieHandler(new CookieManager()).build();call(browser,"POST","/api/members/login","{\"loginId\":\"admin1\",\"password\":\"campus1234!\",\"role\":\"ADMIN\"}");
  var before=dashboard(browser).get("ratingSummary");
  for(int score=1;score<=5;score++){
   db.update("UPDATE student SET temp=? WHERE unisno='demo-seller'",score>=4?990:score<=2?0:365);
   var review=new TradeReviewDto();review.setOrderId(completed());review.setReviewerId("demo-buyer");review.setScore(score);
   assertEquals(0,reviews.submitReview(review).getTemperatureDelta());
  }
  legacy.rate(completed(),"demo-buyer","GOOD");legacy.rate(completed(),"demo-buyer","BAD");
  var dashboard=dashboard(browser);var summary=dashboard.get("ratingSummary");
  assertEquals(before.get("total").asLong()+7,summary.get("total").asLong());
  assertEquals(before.get("good").asLong()+3,summary.get("good").asLong());
  assertEquals(before.get("bad").asLong()+3,summary.get("bad").asLong());
  assertEquals(before.get("neutral").asLong()+1,summary.get("neutral").asLong());
  assertEquals(before.get("stars").asLong()+5,summary.get("stars").asLong());
  assertEquals(summary.get("total").asLong(),summary.get("good").asLong()+summary.get("bad").asLong()+summary.get("neutral").asLong());
  assertEquals(summary.get("total").asInt(),dashboard.get("ratings").size());
  for(var row:dashboard.get("ratings"))if(row.has("score")){int score=row.get("score").asInt();assertEquals("SCORE",row.get("feedback").asText());assertEquals(score>=4?"GOOD":score<=2?"BAD":"NEUTRAL",row.get("sentiment").asText());}
 }
}
