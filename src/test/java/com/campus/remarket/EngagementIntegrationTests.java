package com.campus.remarket;

import java.net.*;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
 "spring.datasource.url=jdbc:h2:mem:engagementtests;MODE=MySQL;DB_CLOSE_DELAY=-1",
 "campus.notifications.initial-delay-ms=3600000"})
class EngagementIntegrationTests {
 @LocalServerPort int port;@Autowired JdbcTemplate db;
 @Autowired com.campus.remarket.service.EngagementService service;
 final JsonMapper json=JsonMapper.builder().build();
 record User(HttpClient browser,String csrf){}
 HttpClient browser(){return HttpClient.newBuilder().cookieHandler(new CookieManager()).build();}
 JsonNode request(HttpClient client,String method,String path,String body,String token,int status)throws Exception{
  var b=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path));SecurityTestClient.csrf(client,b,method,path,port);if(body!=null)b.header("Content-Type","application/json");if(token!=null)b.header("X-CSRF-Token",token);
  var r=client.send(b.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());assertEquals(status,r.statusCode(),r.body());return r.body().isBlank()?null:json.readTree(r.body());
 }
 User login(String id)throws Exception{
  var c=browser();String school="null";if(id.equals("20261234"))for(var s:request(c,"GET","/api/members/schools",null,null,200))if(s.get("name").asText().equals("인하대학교"))school=s.get("id").asText();
  request(c,"POST","/api/members/login","{\"schoolId\":"+school+",\"loginId\":\""+id+"\",\"password\":\"campus1234!\"}",null,200);
  return new User(c,request(c,"GET","/api/engagement/session",null,null,200).get("csrfToken").asText());
 }
 long post(User user)throws Exception{return request(user.browser(),"POST","/api/boards","{\"categoryId\":6,\"title\":\"관심 테스트\",\"content\":\"내용\"}",user.csrf(),200).get("postId").asLong();}
 JsonNode set(User u,String target,String kind,boolean active)throws Exception{return request(u.browser(),"PUT","/api/engagement/"+target.replace(':','/')+"/"+kind,"{\"active\":"+active+"}",u.csrf(),200);}
 JsonNode state(User u,String target)throws Exception{return request(u.browser(),"GET","/api/engagement/state?targets="+target,null,null,200).get(0);}
 @Test void anonymousCountsButPrivateWritesRequireLoginCsrfAndValidTargets()throws Exception{
  var guest=browser();assertTrue(request(guest,"GET","/api/engagement/state?targets=SALE:1",null,null,200).get(0).get("available").asBoolean());
  request(guest,"GET","/api/engagement/likes",null,null,401);request(guest,"PUT","/api/engagement/SALE/1/like","{\"active\":true}",null,401);
  var u=login("20261234");request(u.browser(),"PUT","/api/engagement/SALE/1/like","{\"active\":true}",null,403);
  request(u.browser(),"PUT","/api/engagement/POST/999999/like","{\"active\":true}",u.csrf(),404);
  request(u.browser(),"PUT","/api/engagement/SALE/1/like","{}",u.csrf(),400);
  request(u.browser(),"GET","/api/engagement/state?targets=BAD:1",null,null,400);
  request(u.browser(),"GET","/api/engagement/likes?type=BAD",null,null,400);
  request(u.browser(),"GET","/api/engagement/likes?page=-1",null,null,400);
 }
 @Test void allFourContentTypesCountDistinctAccountsAndCancelIndependently()throws Exception{
  var a=login("20261234");var b=login("owner1");long id=post(a);
  long comment=request(a.browser(),"POST","/api/boards/"+id+"/comments","{\"content\":\"댓글 좋아요\"}",a.csrf(),200).get("commentId").asLong();
  // Isolated content per test; restore shared fixture likes afterwards.
  for(String key:List.of("POST:"+id,"COMMENT:"+comment,"SALE:2","SHOP:1")){
   long before=state(a,key).get("likeCount").asLong();assertEquals(before+1,set(a,key,"like",true).get("likeCount").asLong());assertEquals(before+1,set(a,key,"like",true).get("likeCount").asLong());
   assertEquals(before+2,set(b,key,"like",true).get("likeCount").asLong());var off=set(a,key,"like",false);assertEquals(before+1,off.get("likeCount").asLong());assertFalse(off.get("liked").asBoolean());assertTrue(state(b,key).get("liked").asBoolean());
   set(b,key,"like",false);
  }
 }
 @Test void likesListIsPrivatePersistentAndLegacyBookmarksUseTheSameLike()throws Exception{
  var a=login("20261234");var b=login("owner1");long id=post(a);String key="POST:"+id;
  set(a,key,"like",true);set(a,key,"bookmark",true);set(a,key,"bookmark",true);
  var fresh=login("20261234");assertTrue(state(fresh,key).get("bookmarked").asBoolean());
  var mine=request(fresh.browser(),"GET","/api/engagement/likes?type=POST",null,null,200);assertTrue(mine.get("total").asLong()>=1);boolean found=false;for(var item:mine.get("items"))if(item.get("id").asLong()==id)found=true;assertTrue(found);
  var other=request(b.browser(),"GET","/api/engagement/likes?type=POST",null,null,200);for(var item:other.get("items"))assertNotEquals(id,item.get("id").asLong());assertFalse(state(b,key).get("bookmarked").asBoolean());
  assertEquals(24,mine.get("pageSize").asInt());set(a,key,"bookmark",false);assertFalse(state(a,key).get("bookmarked").asBoolean());assertFalse(state(a,key).get("liked").asBoolean());
  var empty=request(a.browser(),"GET","/api/engagement/likes?type=POST",null,null,200);for(var item:empty.get("items"))assertNotEquals(id,item.get("id").asLong());
 }
 @Test void concurrentDuplicateRequestsAndDatabaseConstraintAllowOnlyOneLike()throws Exception{
  var u=login("20261234");long id=post(u);String key="POST:"+id;
  try(var pool=Executors.newFixedThreadPool(6)){var calls=new ArrayList<Callable<JsonNode>>();for(int i=0;i<8;i++)calls.add(()->set(u,key,"like",true));for(var future:pool.invokeAll(calls))assertTrue(future.get().get("liked").asBoolean());}
  assertEquals(1,state(u,key).get("likeCount").asInt());assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM content_reactions WHERE target_type='POST' AND target_id=? AND kind='LIKE'",Integer.class,id));
  Long member=db.queryForObject("SELECT id FROM member WHERE login_id='20261234'",Long.class);
  assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->db.update("INSERT INTO content_reactions(member_id,target_type,target_id,kind) VALUES(?,'POST',?,'LIKE')",member,id));
  assertEquals(0,set(u,key,"like",false).get("likeCount").asInt());assertEquals(0,set(u,key,"like",false).get("likeCount").asInt());
 }
 @Test void deletedAndUnpublishedContentDisappearsFromInterests()throws Exception{
  var u=login("20261234");long id=post(u);String key="POST:"+id;set(u,key,"bookmark",true);
  request(u.browser(),"DELETE","/api/boards/"+id,null,u.csrf(),200);assertFalse(state(u,key).get("available").asBoolean());
  for(var item:request(u.browser(),"GET","/api/engagement/likes?type=POST",null,null,200).get("items"))assertNotEquals(id,item.get("id").asLong());
  assertFalse(set(u,key,"bookmark",false).get("available").asBoolean());
  set(u,"SHOP:1","bookmark",true);db.update("UPDATE shops SET checked=FALSE WHERE shopId=1");try{
   assertFalse(state(u,"SHOP:1").get("available").asBoolean());for(var item:request(u.browser(),"GET","/api/engagement/likes?type=SHOP",null,null,200).get("items"))assertNotEquals(1,item.get("id").asLong());
   request(u.browser(),"PUT","/api/engagement/SHOP/1/like","{\"active\":true}",u.csrf(),404);
  }finally{db.update("UPDATE shops SET checked=TRUE WHERE shopId=1");set(u,"SHOP:1","bookmark",false);}
 }
 @Test void existingBookmarksAndLikesMergeWithoutDuplicatesAndMigrationCanRepeat()throws Exception{
  var a=login("20261234");var b=login("owner1");long first=post(a),second=post(a);
  long aid=db.queryForObject("SELECT id FROM member WHERE role='STUDENT' AND login_id='20261234'",Long.class);
  long bid=db.queryForObject("SELECT id FROM member WHERE account_key='staff:owner1'",Long.class);
  set(a,"POST:"+first,"like",true);
  for(long[] row:new long[][]{{aid,first},{aid,second},{bid,first}})db.update("INSERT INTO content_reactions(member_id,target_type,target_id,kind) VALUES(?,'POST',?,'BOOKMARK')",row[0],row[1]);
  service.consolidateBookmarks();service.consolidateBookmarks();
  assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM content_reactions WHERE kind='BOOKMARK'",Integer.class));
  assertEquals(2,state(a,"POST:"+first).get("likeCount").asInt());assertEquals(1,state(a,"POST:"+second).get("likeCount").asInt());
  assertTrue(state(a,"POST:"+second).get("liked").asBoolean());assertTrue(state(b,"POST:"+first).get("liked").asBoolean());
  var list=request(a.browser(),"GET","/api/engagement/likes?type=POST",null,null,200);var seen=new HashSet<Long>();for(var item:list.get("items"))assertTrue(seen.add(item.get("id").asLong()));assertTrue(seen.containsAll(List.of(first,second)));
  var legacy=request(a.browser(),"GET","/api/engagement/bookmarks?type=POST",null,null,200);assertEquals(list,legacy);
  assertFalse(set(a,"POST:"+first,"bookmark",false).get("liked").asBoolean());assertTrue(state(b,"POST:"+first).get("liked").asBoolean());
 }

}
