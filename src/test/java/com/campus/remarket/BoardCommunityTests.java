package com.campus.remarket;

import java.net.CookieManager;
import java.net.URI;
import java.net.http.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
    "spring.datasource.url=jdbc:h2:mem:communitytests;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "campus.notifications.initial-delay-ms=3600000"})
class BoardCommunityTests {
    @LocalServerPort int port;
    final JsonMapper json=JsonMapper.builder().build();
    HttpClient browser(){return HttpClient.newBuilder().cookieHandler(new CookieManager()).build();}
    HttpResponse<String> send(HttpClient browser,String method,String path,String body,String csrf) throws Exception {
        var builder=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path));SecurityTestClient.csrf(browser,builder,method,path,port);
        if(body!=null)builder.header("Content-Type","application/json");
        if(csrf!=null)builder.header("X-CSRF-Token",csrf);
        return browser.send(builder.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
    }
    JsonNode call(HttpClient browser,String method,String path,String body,String csrf,int status) throws Exception {
        var response=send(browser,method,path,body,csrf);assertEquals(status,response.statusCode(),response.body());return response.body().isBlank()?null:json.readTree(response.body());
    }
    HttpClient login(String id) throws Exception {
        var client=browser();String school="null";
        if(id.equals("20261234"))for(var s:call(client,"GET","/api/members/schools",null,null,200))if(s.get("name").asText().equals("인하대학교"))school=s.get("id").asText();
        call(client,"POST","/api/members/login","{\"schoolId\":"+school+",\"loginId\":\""+id+"\",\"password\":\"campus1234!\"}",null,200);return client;
    }
    String csrf(HttpClient client)throws Exception{return call(client,"GET","/api/boards/session",null,null,200).get("csrfToken").asText();}
    String post(int category,String title,String content){return json.writeValueAsString(java.util.Map.of("categoryId",category,"title",title,"content",content,"writer","forged","authorName","forged","authorMemberId",999999));}

    @Test void everyCategoryCanPublishAndIdentityComesFromLogin() throws Exception {
        var owner=login("20261234");var token=csrf(owner);
        var me=call(owner,"GET","/api/session",null,null,200).get("member");
        for(int category=1;category<=10;category++){
            var created=call(owner,"POST","/api/boards",post(category,"  제목 "+category+"  ","  이야기  "),token,200);
            var id=created.get("postId").asLong();
            assertEquals("20261234",created.get("writer").asText());assertEquals(me.get("id").asLong(),created.get("authorMemberId").asLong());
            assertEquals(me.get("name").asText(),created.get("authorName").asText());assertEquals("이야기",created.get("content").asText());
            call(owner,"GET","/api/boards/"+id,null,null,200);
            call(owner,"DELETE","/api/boards/"+id,null,token,200);
        }
    }
    @Test void guestAndMissingCsrfAreRejected() throws Exception {
        var guest=browser();call(guest,"POST","/api/boards",post(10,"글","내용"),null,401);
        var owner=login("20261234");call(owner,"POST","/api/boards",post(10,"글","내용"),null,403);
        var token=csrf(owner);var p=call(owner,"POST","/api/boards",post(10,"글","내용"),token,200);String path="/api/boards/"+p.get("postId").asLong();
        call(guest,"POST",path+"/comments","{\"content\":\"댓글\"}",null,401);
        call(owner,"POST",path+"/comments","{\"content\":\"댓글\"}",null,403);
        call(owner,"PUT",path,post(10,"수정","수정"),null,403);call(owner,"DELETE",path,null,null,403);
        call(owner,"DELETE",path,null,token,200);
    }
    @Test void commentsPersistCountAndRespectOwnershipAndParent() throws Exception {
        var author=login("20261234");var reader=login("owner1");var authorToken=csrf(author);var readerToken=csrf(reader);
        var p=call(author,"POST","/api/boards",post(6,"맛집","추천해주세요"),authorToken,200);long id=p.get("postId").asLong();String path="/api/boards/"+id;
        var c=call(reader,"POST",path+"/comments","{\"content\":\"  후문 식당 추천해요.\\n맛있어요!  \"}",readerToken,200);long commentId=c.get("commentId").asLong();
        assertEquals("후문 식당 추천해요.\n맛있어요!",c.get("content").asText());assertEquals("김사장",c.get("authorName").asText());
        assertEquals(1,call(browser(),"GET",path+"/comments",null,null,200).size());assertEquals(1,call(browser(),"GET",path,null,null,200).get("commentCount").asInt());
        call(author,"DELETE",path+"/comments/"+commentId,null,authorToken,403);
        call(reader,"DELETE","/api/boards/1/comments/"+commentId,null,readerToken,404);
        call(reader,"PUT",path,post(6,"바꿈","바꿈"),readerToken,403);call(reader,"DELETE",path,null,readerToken,403);
        call(reader,"DELETE",path+"/comments/"+commentId,null,readerToken,200);assertEquals(0,call(browser(),"GET",path,null,null,200).get("commentCount").asInt());
        call(reader,"POST",path+"/comments","{\"content\":\"두 번째 댓글\"}",readerToken,200);
        call(author,"DELETE",path,null,authorToken,200);call(browser(),"GET",path+"/comments",null,null,404);
    }
    @Test void inputLimitsAndMissingPostsHaveUsefulErrors() throws Exception {
        var user=login("20261234");var token=csrf(user);
        for(var input:new String[]{post(6," ","내용"),post(10,"x".repeat(151),"내용"),post(10,"제목","x".repeat(3001)),post(11,"제목","내용")})call(user,"POST","/api/boards",input,token,400);
        var p=call(user,"POST","/api/boards",post(10,"제목","내용"),token,200);String path="/api/boards/"+p.get("postId").asLong();
        call(user,"POST",path+"/comments","{\"content\":\"   \"}",token,400);
        call(user,"POST",path+"/comments",json.writeValueAsString(java.util.Map.of("content","x".repeat(1001))),token,400);
        call(user,"POST",path+"/comments",json.writeValueAsString(java.util.Map.of("content","x".repeat(1000))),token,200);
        call(user,"GET","/api/boards/999999",null,null,404);call(user,"POST","/api/boards/999999/comments","{\"content\":\"댓글\"}",token,404);
        call(user,"DELETE",path,null,token,200);
    }
}
