package com.campus.remarket;

import java.net.*;
import java.net.http.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
 "spring.datasource.url=jdbc:h2:mem:teammatemerge;MODE=MySQL;DB_CLOSE_DELAY=-1","campus.notifications.initial-delay-ms=3600000"})
class TeammateMergeTests {
 @LocalServerPort int port;@Autowired JdbcTemplate db;final JsonMapper json=JsonMapper.builder().build();
 HttpClient browser(){return HttpClient.newBuilder().cookieHandler(new CookieManager()).build();}
 JsonNode call(HttpClient c,String method,String path,Object body,int expected)throws Exception{
  var b=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path));SecurityTestClient.csrf(c,b,method,path,port);
  if(body!=null)b.header("Content-Type","application/json");
  var r=c.send(b.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build(),HttpResponse.BodyHandlers.ofString());
  assertEquals(expected,r.statusCode(),r.body());return r.body().isBlank()?json.createObjectNode():json.readTree(r.body());
 }
 long school(HttpClient c)throws Exception{for(var s:call(c,"GET","/api/members/schools",null,200))if(s.get("name").asText().equals("인하대학교"))return s.get("id").asLong();throw new AssertionError();}
 void login(HttpClient c,String id)throws Exception{call(c,"POST","/api/members/login",Map.of("schoolId",school(c),"loginId",id,"password","campus1234!"),200);}
 HttpClient student(String id)throws Exception{
  var c=browser();long school=school(c);String email=id+"@inha.edu";
  var code=call(c,"POST","/api/members/email/code",Map.of("schoolId",school,"email",email),200).get("devCode").asText();
  call(c,"POST","/api/members/email/verify",Map.of("email",email,"code",code),200);
  call(c,"POST","/api/members/signup",Map.of("schoolId",school,"studentNo",id,"password","campus1234!","name","합치기 시험","email",email,"phone","01011112222","studentStatus","ENROLLED"),201);
  login(c,id);return c;
 }
 @Test void accountUpdatesAreValidatedSelfOnlyAndProtectedByCsrf()throws Exception{
  var c=student("20269801");var other=student("20269802");long id=call(c,"GET","/api/members/me",null,200).get("id").asLong();
  var unprotected=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/members/me/name")).header("Content-Type","application/json").PUT(HttpRequest.BodyPublishers.ofString("{\"name\":\"공격 이름\"}")).build();
  assertEquals(403,c.send(unprotected,HttpResponse.BodyHandlers.ofString()).statusCode());
  call(c,"PUT","/api/members/me",Map.of("phone","invalid"),400);
  assertEquals("010-2222-3333",call(c,"PUT","/api/members/me",Map.of("phone","01022223333","memberId",999),200).get("phone").asText());
  call(c,"PUT","/api/members/me/name",Map.of("name","가"),400);
  db.update("INSERT INTO board_posts(school_id,category_id,writer,title,content,author_member_id,author_name) VALUES(?,6,?,'이름 확인','본문',?,'합치기 시험')",school(c),"20269801",id);
  assertEquals("새 닉네임",call(c,"PUT","/api/members/me/name",Map.of("name","새 닉네임"),200).get("name").asText());
  assertEquals("새 닉네임",db.queryForObject("SELECT author_name FROM board_posts WHERE author_member_id=?",String.class,id));
  assertEquals("합치기 시험",call(other,"GET","/api/members/me",null,200).get("name").asText());
  assertEquals("GRADUATED",call(c,"PUT","/api/members/me/status",Map.of("studentStatus","GRADUATED"),200).get("studentStatus").asText());
  var owner=browser();call(owner,"POST","/api/members/login",Map.of("loginId","owner1","password","campus1234!"),200);
  call(owner,"PUT","/api/members/me/name",Map.of("name","금지 이름"),403);
  call(owner,"POST","/api/members/me/withdraw",Map.of("password","campus1234!"),403);
 }
 @Test void withdrawalAnonymizesContactsRevokesEverySessionAndPreservesRecords()throws Exception{
  var c=student("20269803");var second=browser();login(second,"20269803");var buyer=student("20269804");
  long id=call(c,"GET","/api/members/me",null,200).get("id").asLong();
  long sale=call(c,"POST","/api/products",Map.of("name","탈퇴 기록 확인","category","전공서적","oprice",1000),200).get("selno").asLong();
  call(c,"PUT","/api/notifications/settings",Map.of("email","withdraw@inha.edu","phone","01011112222","emailEnabled",true,"smsEnabled",true,"kakaoEnabled",true),204);
  call(c,"POST","/api/members/me/withdraw",Map.of("password","wrong"),401);
  call(c,"GET","/api/members/me",null,200);
  call(c,"POST","/api/members/me/withdraw",Map.of("password","campus1234!"),200);
  call(c,"GET","/api/members/me",null,401);call(second,"GET","/api/members/me",null,401);
  call(browser(),"POST","/api/members/login",Map.of("schoolId",school(buyer),"loginId","20269803","password","campus1234!"),403);
  var member=db.queryForMap("SELECT withdrawn,name,email,phone FROM member WHERE id=?",id);
  assertEquals(true,member.get("WITHDRAWN"));assertEquals("탈퇴한 회원",member.get("NAME"));assertNull(member.get("EMAIL"));assertNull(member.get("PHONE"));
  var prefs=db.queryForMap("SELECT email,phone,sms_enabled,email_enabled FROM notification_settings WHERE member_id=?",id);
  assertNull(prefs.get("EMAIL"));assertNull(prefs.get("PHONE"));assertEquals(false,prefs.get("SMS_ENABLED"));
  assertEquals(id,db.queryForObject("SELECT seller_member_id FROM sales WHERE selno=?",Long.class,sale));
  call(buyer,"POST","/api/trades",Map.of("selno",sale),409);
 }
 @Test void teammateSchoolAndFoodPhotosExistForAllThreeSupportedCampuses(){
  for(String name:List.of("인하대학교","인천대학교","청운대학교 인천캠퍼스")){
   assertTrue(new org.springframework.core.io.ClassPathResource("static/school-photos/"+name+".jpg").exists());
   assertTrue(new org.springframework.core.io.ClassPathResource("static/school-food/"+name+"-1.jpg").exists());
  }
 }
}
