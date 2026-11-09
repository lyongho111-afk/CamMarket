package com.campus.remarket;

import java.net.*;
import java.net.http.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
 "spring.datasource.url=jdbc:h2:mem:memberprofiletests;MODE=MySQL;DB_CLOSE_DELAY=-1",
 "campus.notifications.initial-delay-ms=3600000"})
class MemberProfileTests {
 @LocalServerPort int port;
 final JsonMapper json=JsonMapper.builder().build();
 HttpClient browser(){return HttpClient.newBuilder().cookieHandler(new CookieManager()).build();}
 JsonNode call(HttpClient client,String method,String path,String body,String csrf,int status)throws Exception{
  var request=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path));SecurityTestClient.csrf(client,request,method,path,port);if(body!=null)request.header("Content-Type","application/json");if(csrf!=null)request.header("X-CSRF-Token",csrf);
  var response=client.send(request.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());assertEquals(status,response.statusCode(),path+": "+response.body());return response.body().isBlank()?null:json.readTree(response.body());
 }
 HttpClient login(String id)throws Exception{
  var client=browser();String school="null";if(!id.equals("owner1"))for(var s:call(client,"GET","/api/members/schools",null,null,200))if(s.get("name").asText().equals("인하대학교"))school=s.get("id").asText();
  call(client,"POST","/api/members/login","{\"schoolId\":"+school+",\"loginId\":\""+id+"\",\"password\":\"campus1234!\"}",null,200);return client;
 }
 String saleBody(){return "{\"name\":\"회원별 온도 검증\",\"category\":\"전공서적\",\"oprice\":1000,\"seller\":\"demo-seller-2\",\"sellerMemberId\":999999}";}
 JsonNode get(HttpClient client,String path)throws Exception{return call(client,"GET",path,null,null,200);}
 @Test void privateProfileIsAuthenticatedAndPublicSellerProfileOmitsPrivateAccountFields()throws Exception{
  var guest=browser();call(guest,"GET","/api/profiles/me",null,null,401);
  var seller=login("demo-seller-2");var me=get(seller,"/api/profiles/me");assertTrue(me.get("own").asBoolean());assertEquals("시연판매자 B",me.get("name").asText());assertEquals(36.5,me.get("temperature").asDouble());assertTrue(me.get("verifiedMember").asBoolean());
  var listing=call(seller,"POST","/api/products",saleBody(),null,200);long id=listing.get("selno").asLong();assertEquals(me.get("memberId").asLong(),listing.get("sellerMemberId").asLong());
  String likeToken=get(seller,"/api/engagement/session").get("csrfToken").asText();
  call(seller,"PUT","/api/engagement/SALE/"+id+"/like","{\"active\":true}",likeToken,200);
  var liked=get(seller,"/api/engagement/likes?type=SALE");assertTrue(liked.get("items").toString().contains("회원별 온도 검증"));
  assertEquals(3,get(guest,"/api/shops").size());
  var profile=get(guest,"/api/products/"+id+"/seller-profile");assertEquals("시연판매자 B",profile.get("name").asText());assertFalse(profile.get("own").asBoolean());assertEquals(0,profile.get("reviews").size());
  for(String field:new String[]{"email","phone","loginId","password","businessNo"})assertFalse(profile.has(field));
  for(var sale:profile.get("listings"))assertFalse(sale.has("seller"));
  var owner=login("owner1");assertTrue(get(owner,"/api/profiles/me").get("temperature").isNull());
  call(guest,"GET","/api/products/999999/seller-profile",null,null,404);
 }
 @Test void evaluatingOneMemberUpdatesTheirOtherSaleButNeverAnotherMembersTemperature()throws Exception{
  var sellerA=login("demo-seller");var sellerB=login("demo-seller-2");var buyer=login("20261234");
  var a1=call(sellerA,"POST","/api/products",saleBody(),null,200);var a2=call(sellerA,"POST","/api/products",saleBody(),null,200);var b=call(sellerB,"POST","/api/products",saleBody(),null,200);
  assertNotEquals(a1.get("sellerMemberId").asLong(),b.get("sellerMemberId").asLong());assertEquals("demo-seller",a1.get("seller").asText());
  double aBefore=a1.get("sellerTemperature").asDouble(),bBefore=b.get("sellerTemperature").asDouble();
  var order=call(buyer,"POST","/api/trades","{\"selno\":"+a1.get("selno").asLong()+"}",null,200);String orderId=order.get("orderId").asText();
  call(sellerA,"POST","/api/trades/"+orderId+"/complete",null,null,200);
  String token=get(buyer,"/api/trades/"+orderId+"/rating").get("csrfToken").asText();
  String review=json.writeValueAsString(java.util.Map.of("orderId",orderId,"score",5,"reviewerId","forged","targetId","demo-seller-2","content","거래가 좋았어요."));
  call(buyer,"POST","/api/reviews",review,token,200);
  assertEquals(aBefore+0.5,get(sellerA,"/api/products/"+a1.get("selno").asLong()).get("sellerTemperature").asDouble());
  assertEquals(aBefore+0.5,get(sellerA,"/api/products/"+a2.get("selno").asLong()).get("sellerTemperature").asDouble());
  assertEquals(bBefore,get(sellerB,"/api/products/"+b.get("selno").asLong()).get("sellerTemperature").asDouble());
  var own=get(sellerA,"/api/profiles/me");assertEquals(aBefore+0.5,own.get("temperature").asDouble());assertEquals(1,own.get("summary").get("total").asInt());assertEquals(1,own.get("reviews").size());
  var publicView=get(buyer,"/api/products/"+a1.get("selno").asLong()+"/seller-profile");assertEquals(1,publicView.get("summary").get("good").asInt());assertEquals(0,publicView.get("reviews").size());assertTrue(publicView.get("summary").get("completedTrades").asInt()>=1);assertEquals(5.0,publicView.get("summary").get("averageScore").asDouble());assertEquals(100.0,publicView.get("summary").get("satisfactionRate").asDouble());
  call(sellerB,"PUT","/api/products/"+a2.get("selno").asLong(),saleBody(),null,403);
  call(sellerA,"PUT","/api/products/"+a2.get("selno").asLong(),saleBody(),null,200);
  assertEquals(a1.get("sellerMemberId").asLong(),get(sellerA,"/api/products/"+a2.get("selno").asLong()).get("sellerMemberId").asLong());
  call(buyer,"POST","/api/reviews",review,token,400);
  assertEquals(aBefore+0.5,get(sellerA,"/api/profiles/me").get("temperature").asDouble());
 }
}
