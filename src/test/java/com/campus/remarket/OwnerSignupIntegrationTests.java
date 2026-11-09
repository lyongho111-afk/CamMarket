package com.campus.remarket;

import com.campus.remarket.service.OwnerShopLink;
import demo.campus.campusremarketmember.member.dto.OwnerSignupDto;
import demo.campus.campusremarketmember.member.repository.MemberRepository;
import demo.campus.campusremarketmember.member.service.MemberService;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"spring.datasource.url=jdbc:h2:mem:ownersignup;MODE=MySQL;DB_CLOSE_DELAY=-1","campus.upload-dir=build/test-signup-uploads"})
class OwnerSignupIntegrationTests {
 @LocalServerPort int port;
 @Autowired MemberRepository repository;
 @Autowired MemberService members;
 @Autowired OwnerShopLink link;
 @Autowired PlatformTransactionManager transactions;
 final JsonMapper json=JsonMapper.builder().build();
 final byte[] png=Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+j6iUAAAAASUVORK5CYII=");
 HttpClient browser(){return HttpClient.newBuilder().cookieHandler(new CookieManager()).build();}
 URI url(String path){return URI.create("http://127.0.0.1:"+port+path);}
 JsonNode call(HttpClient client,String method,String path,String body,String csrf,int expected)throws Exception{
  var b=HttpRequest.newBuilder(url(path));SecurityTestClient.csrf(client,b,method,path,port);if(body!=null)b.header("Content-Type","application/json");if(csrf!=null)b.header("X-CSRF-Token",csrf);
  var r=client.send(b.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());assertEquals(expected,r.statusCode(),r.body());return r.body().isBlank()?null:json.readTree(r.body());
 }
 Map<String,String> form(String id,String biz){var f=new LinkedHashMap<String,String>();f.put("loginId",id);f.put("password","campus1234!");f.put("name","가입사장");f.put("phone","010-1234-5678");f.put("email",id+"@example.com");f.put("businessNo",biz);f.put("shopname","회원가입 가게 "+id);f.put("busiKind","카페");f.put("address","인천 미추홀구 인하로 100");f.put("addressDetail","1층 101호");f.put("bmenu","아메리카노 2500원");return f;}
 JsonNode signup(Map<String,String> fields,byte[] photo,String filename,String type,int expected)throws Exception{
  if(!fields.containsKey("schoolId")){var schools=call(browser(),"GET","/api/members/schools",null,null,200);for(var school:schools)if(school.get("name").asText().equals("인하대학교"))fields.put("schoolId",school.get("id").asText());}
  fields.put("latitude","37.45");fields.put("longitude","126.65");
  String boundary="campus-"+UUID.randomUUID();var parts=new ArrayList<HttpRequest.BodyPublisher>();
  for(var entry:fields.entrySet())parts.add(HttpRequest.BodyPublishers.ofString("--"+boundary+"\r\nContent-Disposition: form-data; name=\""+entry.getKey()+"\"\r\n\r\n"+entry.getValue()+"\r\n",StandardCharsets.UTF_8));
  if(photo!=null){parts.add(HttpRequest.BodyPublishers.ofString("--"+boundary+"\r\nContent-Disposition: form-data; name=\"photo\"; filename=\""+filename+"\"\r\nContent-Type: "+type+"\r\n\r\n"));parts.add(HttpRequest.BodyPublishers.ofByteArray(photo));parts.add(HttpRequest.BodyPublishers.ofString("\r\n"));}
  parts.add(HttpRequest.BodyPublishers.ofString("--"+boundary+"--\r\n"));
  var client=browser();var signupRequest=HttpRequest.newBuilder(url("/api/members/signup/owner"));SecurityTestClient.csrf(client,signupRequest,"POST","/api/members/signup/owner",port);
  var r=client.send(signupRequest.header("Content-Type","multipart/form-data; boundary="+boundary).POST(HttpRequest.BodyPublishers.concat(parts.toArray(HttpRequest.BodyPublisher[]::new))).build(),HttpResponse.BodyHandlers.ofString());assertEquals(expected,r.statusCode(),r.body());return r.body().isBlank()?null:json.readTree(r.body());
 }
 HttpClient login(String id)throws Exception{var c=browser();call(c,"POST","/api/members/login","{\"loginId\":\""+id+"\",\"password\":\"campus1234!\"}",null,200);return c;}
 @Test void multipartCreatesLinkedPendingShopWithOriginalPhotoAndManagerAccess()throws Exception{
  var owner=signup(form("signupstore14","765-43-21014"),png,"store.png","image/png",201);assertEquals("OWNER",owner.get("role").asText());assertEquals("인하대학교",owner.get("schoolName").asText());
  var c=login("signupstore14");var home=call(c,"GET","/api/owner/dashboard",null,null,200);assertEquals("인하대학교",home.get("me").get("schoolName").asText());var shop=home.get("shop");assertFalse(shop.get("approved").asBoolean());assertEquals(37.45,shop.get("latitude").asDouble());assertEquals(126.65,shop.get("longitude").asDouble());assertEquals("인천 미추홀구 인하로 100 1층 101호",shop.get("address").asText());assertEquals("아메리카노 2500원",shop.get("menu").asText());assertEquals(shop.get("id").asLong(),repository.findByAccountKey("staff:signupstore14").orElseThrow().getShopId());
  var photo=browser().send(HttpRequest.newBuilder(url("/api/shops/photo/"+shop.get("photo").asText())).build(),HttpResponse.BodyHandlers.ofByteArray());assertEquals(200,photo.statusCode());assertEquals("image/png",photo.headers().firstValue("content-type").orElse(""));assertArrayEquals(png,photo.body());
  assertFalse(call(browser(),"GET","/api/shops",null,null,200).toString().contains("signupstore14"));
  call(c,"POST","/api/members/managers","{\"loginId\":\"signupmanager14\",\"password\":\"campus1234!\",\"name\":\"가입매니저\"}",home.get("csrfToken").asText(),201);
  assertEquals(shop.get("id"),call(login("signupmanager14"),"GET","/api/owner/dashboard",null,null,200).get("shop").get("id"));
  var admin=login("admin1");var ac=call(admin,"GET","/api/admin/dashboard",null,null,200).get("csrfToken").asText();call(admin,"PUT","/api/admin/shops/"+shop.get("id").asLong()+"/approval","{\"approved\":true}",ac,200);assertTrue(call(browser(),"GET","/api/shops",null,null,200).toString().contains(shop.get("photo").asText()));
 }
 @Test void optionalShopFieldsCanBeOmittedAndInvalidRegistrationRollsBack()throws Exception{
  var f=form("signupinvalid14","765-43-21015");f.remove("address");signup(f,null,null,null,400);assertTrue(repository.findByAccountKey("staff:signupinvalid14").isEmpty());f.put("address","인천");signup(f,null,null,null,201);
 }
 @Test void rejectsUnsupportedAndOversizedPhotosBeforeCreatingAccount()throws Exception{
  signup(form("signupbad14","765-43-21016"),"text".getBytes(StandardCharsets.UTF_8),"test.txt","text/plain",400);assertTrue(repository.findByAccountKey("staff:signupbad14").isEmpty());
  signup(form("signuplarge14","765-43-21017"),new byte[5*1024*1024+1],"large.png","image/png",413);assertTrue(repository.findByAccountKey("staff:signuplarge14").isEmpty());assertNull(link.loadPhoto("../secret.png"));
 }
 long photoCount()throws Exception{var dir=Path.of("build/test-signup-uploads/shops");if(!Files.exists(dir))return 0;try(var files=Files.list(dir)){return files.count();}}
 @Test void rollbackAfterPhotoSaveRemovesBothPhotoAndMember()throws Exception{
  long before=photoCount();var f=new OwnerSignupDto("signuprollback14","campus1234!","가입사장","010-1234-5678","rollback14@example.com","765-43-21018","롤백 가게","카페",null,"인천",null,null,null,null);
  assertThrows(IllegalStateException.class,()->new TransactionTemplate(transactions).executeWithoutResult(status->{members.signupOwner(f,new MockMultipartFile("photo","store.png","image/png",png));throw new IllegalStateException("rollback test");}));assertTrue(repository.findByAccountKey("staff:signuprollback14").isEmpty());assertEquals(before,photoCount());
 }
}
