package com.campus.remarket;

import java.net.URI;
import java.net.CookieManager;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import com.campus.remarket.dao.StudentDao;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties={"spring.datasource.url=jdbc:h2:mem:memberintegration;MODE=MySQL;DB_CLOSE_DELAY=-1","campus.notifications.initial-delay-ms=3600000"})
class MemberIntegrationTests {
    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    @Autowired StudentDao students;
    final JsonMapper json=JsonMapper.builder().build();
    HttpClient browser(){return HttpClient.newBuilder().cookieHandler(new CookieManager()).build();}
    HttpResponse<String> call(HttpClient client,String method,String path,String body,int expected) throws Exception {
        var builder=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path));SecurityTestClient.csrf(client,builder,method,path,port);
        if(body!=null) builder.header("Content-Type","application/json");
        if(path.startsWith("/api/boards")&&!method.equals("GET")) {
            var token=client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/boards/session")).GET().build(),HttpResponse.BodyHandlers.ofString());
            if(token.statusCode()==200)builder.header("X-CSRF-Token",json.readTree(token.body()).get("csrfToken").asText());
        }
        builder.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body));
        var response=client.send(builder.build(),HttpResponse.BodyHandlers.ofString());
        assertEquals(expected,response.statusCode(),path+": "+response.body());
        return response;
    }
    JsonNode data(HttpResponse<String> response){return json.readTree(response.body());}
    long school(HttpClient client) throws Exception {
        for(var school:data(call(client,"GET","/api/members/schools",null,200)))
            if(school.get("name").asText().equals("인하대학교"))return school.get("id").asLong();
        throw new AssertionError("인하대학교 없음");
    }
    void login(HttpClient client,Long schoolId,String login) throws Exception {
        call(client,"POST","/api/members/login","{\"schoolId\":"+schoolId+",\"loginId\":\""+login+"\",\"password\":\"campus1234!\"}",200);
    }
    void signup(HttpClient client,long schoolId,String unisno,String email,int expected) throws Exception {
        String code=data(call(client,"POST","/api/members/email/code","{\"schoolId\":"+schoolId+",\"email\":\""+email+"\"}",200)).get("devCode").asText();
        call(client,"POST","/api/members/email/verify","{\"email\":\""+email+"\",\"code\":\""+code+"\"}",200);
        call(client,"POST","/api/members/signup","{\"schoolId\":"+schoolId+",\"studentNo\":\""+unisno+"\",\"password\":\"campus1234!\",\"name\":\"연동학생\",\"email\":\""+email+"\",\"phone\":\"010-1111-2222\",\"studentStatus\":\"ENROLLED\"}",expected);
    }
    HttpClient student(String unisno) throws Exception {
        var client=browser();long school=school(client);signup(client,school,unisno,"s"+unisno+"@inha.edu",201);login(client,school,unisno);return client;
    }
    JsonNode sale(HttpClient client) throws Exception {
        return data(call(client,"POST","/api/products","{\"name\":\"평가 거래 책\",\"category\":\"전공서적\",\"oprice\":10000,\"seller\":\"forged\"}",200));
    }
    JsonNode reserve(HttpClient client,long id) throws Exception {
        return data(call(client,"POST","/api/trades","{\"selno\":"+id+",\"buyer\":\"forged\"}",200));
    }
    HttpResponse<String> feedback(HttpClient client,String orderId,String csrf,String value,int expected) throws Exception {
        var req=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/trades/"+orderId+"/rating"))
            .header("Content-Type","application/json").header("X-CSRF-Token",csrf)
            .POST(HttpRequest.BodyPublishers.ofString("{\"feedback\":\""+value+"\"}" )).build();
        var response=client.send(req,HttpResponse.BodyHandlers.ofString());
        assertEquals(expected,response.statusCode(),response.body());return response;
    }

    @Test void loginSessionLogoutAndOriginalPages() throws Exception {
        var client=browser();
        assertFalse(data(call(client,"GET","/api/account/status",null,200)).path("localDemo").asBoolean());
        call(client,"GET","/api/local-demo/mailbox",null,404);
        call(client,"GET","/local-demo/inbox",null,404);
        assertFalse(data(call(client,"GET","/api/session",null,200)).get("authenticated").asBoolean());
        call(client,"GET","/api/members/me",null,401);
        call(client,"POST","/api/members/login","{\"loginId\":\"owner1\",\"password\":\"wrong\"}",401);
        login(client,school(client),"20261234");
        var session=data(call(client,"GET","/api/session",null,200));
        assertEquals("STUDENT",session.get("member").get("role").asText());
        assertEquals("20261234",session.get("actorKey").asText());
        assertFalse(session.get("member").has("password"));
        for(String path:new String[]{"/","/sales/selectAll","/login.html","/signup.html","/manager.html","/preview/index.html"}) {
            var response=call(client,"GET",path,null,(path.equals("/")||path.equals("/manager.html"))?302:200);
            if(path.equals("/login.html")||path.equals("/signup.html"))
                assertFalse(response.body().contains("/member-navigation.js"));
        }
        call(client,"POST","/api/members/logout",null,200);
        call(client,"GET","/api/members/me",null,401);
        assertFalse(data(call(client,"GET","/api/session",null,200)).get("authenticated").asBoolean());
    }

    @Test void verifiedSignupAndDuplicateProtection() throws Exception {
        var client=browser();long school=school(client);
        String code=data(call(client,"POST","/api/members/email/code","{\"schoolId\":"+school+",\"email\":\"integration@inha.edu\"}",200)).get("devCode").asText();
        String form="{\"schoolId\":"+school+",\"studentNo\":\"20269999\",\"password\":\"campus1234!\",\"name\":\"통합학생\",\"email\":\"integration@inha.edu\",\"phone\":\"010-9999-8888\",\"studentStatus\":\"ENROLLED\"}";
        call(client,"POST","/api/members/signup",form,400);
        call(client,"POST","/api/members/email/verify","{\"email\":\"integration@inha.edu\",\"code\":\""+code+"\"}",200);
        assertEquals("STUDENT",data(call(client,"POST","/api/members/signup",form,201)).get("role").asText());
        login(client,school,"20269999");
        call(client,"POST","/api/members/email/code","{\"schoolId\":"+school+",\"email\":\"integration@inha.edu\"}",409);
        call(client,"POST","/api/members/email/code","{\"schoolId\":"+school+",\"email\":\"invalid@example.com\"}",400);
    }

    @Test void managersAndAdminKeepOriginalPermissions() throws Exception {
        var owner=browser();login(owner,null,"owner1");
        var manager=data(call(owner,"POST","/api/members/managers","{\"loginId\":\"newmanager\",\"password\":\"campus1234!\",\"name\":\"새매니저\"}",201));
        var staff=browser();login(staff,null,"newmanager");
        call(staff,"GET","/api/members/managers",null,403);
        call(staff,"GET","/api/members/admin/list",null,403);
        call(owner,"DELETE","/api/members/managers/"+manager.get("id").asLong(),null,200);
        call(staff,"GET","/api/members/me",null,401);
        var admin=browser();login(admin,null,"admin1");
        assertTrue(data(call(admin,"GET","/api/members/admin/list",null,200)).isArray());
        var newOwner=browser();
        String form="{\"loginId\":\"integrationowner\",\"password\":\"campus1234!\",\"name\":\"통합사장\",\"phone\":\"010-1111-3333\",\"email\":\"integrationowner@example.com\",\"businessNo\":\"987-65-43210\"}";
        call(newOwner,"POST","/api/members/signup/owner",form,201);
        call(newOwner,"POST","/api/members/signup/owner",form,409);
        login(newOwner,null,"integrationowner");
    }

    @Test void marketIdentityOwnershipAndTradeFlow() throws Exception {
        var guest=browser();var seller=browser();var buyer=browser();var other=browser();
        login(seller,school(seller),"20261234");buyer=student("20262222");other=student("20263333");
        String product="{\"name\":\"로그인 통합 책\",\"category\":\"전공서적\",\"oprice\":10000,\"seller\":\"forged\"}";
        call(guest,"POST","/api/products",product,401);
        var created=data(call(seller,"POST","/api/products",product,200));long id=created.get("selno").asLong();
        assertEquals(data(call(seller,"GET","/api/session",null,200)).get("actorKey").asText(),created.get("seller").asText());
        for(String action:new String[]{"discount","discount-end","bump"})
            call(buyer,"POST","/api/products/"+id+"/"+action,"{\"rate\":20}",403);
        call(buyer,"PUT","/api/products/"+id,product,403);
        call(buyer,"DELETE","/api/products/"+id,null,403);
        call(seller,"POST","/api/products/"+id+"/discount","{\"rate\":20}",200);
        call(seller,"POST","/api/trades","{\"selno\":"+id+"}",403);
        var order=data(call(buyer,"POST","/api/trades","{\"selno\":"+id+",\"buyer\":\"forged\",\"agreedPrice\":1}",200));
        assertEquals(8000,order.get("agreedPrice").asInt());
        assertEquals(data(call(buyer,"GET","/api/session",null,200)).get("actorKey").asText(),order.get("buyer").asText());
        String orderId=order.get("orderId").asText();
        assertEquals(0,data(call(guest,"GET","/api/trades",null,200)).size());
        assertEquals(0,data(call(other,"GET","/api/trades",null,200)).size());
        assertEquals(1,data(call(seller,"GET","/api/trades",null,200)).size());
        call(other,"POST","/api/trades/"+orderId+"/cancel",null,403);
        call(other,"POST","/api/payments/prepare","{\"orderId\":\""+orderId+"\"}",403);
        call(other,"POST","/api/payments/confirm","{\"orderId\":\""+orderId+"\",\"paymentKey\":\"fake\",\"amount\":8000}",403);
        call(buyer,"PUT","/api/trades/"+orderId+"/meeting","{\"meetingPlace\":\"학생회관\"}",200);
        call(seller,"POST","/api/trades/"+orderId+"/complete",null,200);
        assertEquals("거래완료",data(call(seller,"GET","/api/products/"+id,null,200)).get("status").asText());
        String board="{\"categoryId\":1,\"title\":\"회원 글\",\"content\":\"통합 확인\",\"writer\":\"forged\"}";
        var post=data(call(seller,"POST","/api/boards",board,200));
        String postId=post.get("postId").asText();
        call(buyer,"PUT","/api/boards/"+postId,board,403);
        call(buyer,"DELETE","/api/boards/"+postId,null,403);
        call(seller,"PUT","/api/boards/"+postId,board,200);
        call(seller,"DELETE","/api/boards/"+postId,null,200);
        call(seller,"POST","/api/members/logout",null,200);
        call(seller,"POST","/api/products",product,401);
    }

    @Test void signupPreservesLeadingZerosAndSeparatesSameNumberAcrossSchools() throws Exception {
        var first=student("00123457");
        var record=students.selectOne("00123457");assertNotNull(record);assertEquals(365,record.getTemp());
        assertNotNull(record.getMemberId());
        jdbc.update("UPDATE student SET temp=422 WHERE unisno='00123457'");
        call(first,"POST","/api/members/logout",null,200);login(first,school(first),"00123457");
        assertEquals(422,students.selectOne("00123457").getTemp());
        assertEquals("00123457",data(call(first,"GET","/api/session",null,200)).get("actorKey").asText());
        long otherSchool=0;
        for(var row:data(call(first,"GET","/api/members/schools",null,200)))
            if(row.get("name").asText().equals("인천대학교"))otherSchool=row.get("id").asLong();
        assertTrue(otherSchool>0);
        var second=browser();signup(second,otherSchool,"00123457","collision@inu.ac.kr",201);login(second,otherSchool,"00123457");assertNotEquals("00123457",data(call(second,"GET","/api/session",null,200)).get("actorKey").asText());
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM member WHERE account_key=?",Integer.class,otherSchool+":00123457"));
        assertEquals(record.getMemberId(),students.selectOne("00123457").getMemberId());
    }

    @Test void realStudentTradeRatingSupportsBothCompletionStatusesHtmlAndSessionCsrf() throws Exception {
        var seller=student("00102030");var buyer=student("00908070");var other=student("00909090");var guest=browser();
        long saleId=sale(seller).get("selno").asLong();String orderId=reserve(buyer,saleId).get("orderId").asText();
        String path="/api/trades/"+orderId+"/rating";
        var state=data(call(buyer,"GET",path,null,200));assertFalse(state.get("demo").asBoolean());assertFalse(state.get("rating").get("canRate").asBoolean());
        String token=state.get("csrfToken").asText();
        feedback(buyer,orderId,token,"GOOD",409);
        call(guest,"GET",path,null,401);call(other,"GET",path,null,403);
        feedback(other,orderId,token,"GOOD",403);
        call(buyer,"POST",path,"{\"feedback\":\"GOOD\"}",403);
        call(seller,"POST","/api/trades/"+orderId+"/complete",null,200);
        assertTrue(call(buyer,"GET","/trades/selectOne?id="+orderId,null,200).body().contains("manner-rating-form"));
        assertTrue(data(call(buyer,"GET",path,null,200)).get("rating").get("canRate").asBoolean());
        feedback(buyer,orderId,token,"INVALID",400);
        var result=data(feedback(buyer,orderId,token,"GOOD",200));
        assertEquals("00908070",result.get("rating").get("reviewerUnisno").asText());
        assertEquals("00102030",result.get("rating").get("targetUnisno").asText());
        assertEquals(366,students.selectOne("00102030").getTemp());
        assertEquals(36.6,data(call(buyer,"GET","/api/products/"+saleId,null,200)).get("sellerTemperature").asDouble(),0.001);
        feedback(buyer,orderId,token,"GOOD",409);
        assertFalse(call(buyer,"GET","/trades/selectOne?id="+orderId,null,200).body().contains("manner-rating-form"));
        jdbc.update("UPDATE trade_orders SET status='거래완료' WHERE order_id=?",orderId);
        var sellerState=data(call(seller,"GET",path,null,200));assertTrue(sellerState.get("rating").get("canRate").asBoolean());
        String form="id="+URLEncoder.encode(orderId,StandardCharsets.UTF_8)+"&feedback=BAD&csrf="+sellerState.get("csrfToken").asText();
        var req=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/trades/rating")).header("Content-Type","application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(form)).build();
        assertEquals(302,seller.send(req,HttpResponse.BodyHandlers.ofString()).statusCode());
        assertEquals(364,students.selectOne("00908070").getTemp());
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM manner_ratings WHERE order_id=?",Integer.class,orderId));
        call(buyer,"POST","/api/members/logout",null,200);feedback(buyer,orderId,token,"GOOD",401);
        login(buyer,school(buyer),"00908070");assertEquals(364,students.selectOne("00908070").getTemp());
        feedback(buyer,orderId,token,"GOOD",403); // token was rotated at login
        var owner=browser();login(owner,null,"owner1");saleForbiddenForStaff(owner);
    }
    void saleForbiddenForStaff(HttpClient owner) throws Exception {
        call(owner,"POST","/api/products","{\"name\":\"사장 판매\",\"category\":\"전공서적\",\"oprice\":10000}",403);
    }

    @Test void concurrentDuplicateRatingStoresOnlyOneDelta() throws Exception {
        var seller=student("00202020");var buyer=student("00303030");
        long id=sale(seller).get("selno").asLong();String orderId=reserve(buyer,id).get("orderId").asText();
        call(seller,"POST","/api/trades/"+orderId+"/complete",null,200);
        String token=data(call(buyer,"GET","/api/trades/"+orderId+"/rating",null,200)).get("csrfToken").asText();
        var req=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/trades/"+orderId+"/rating"))
            .header("Content-Type","application/json").header("X-CSRF-Token",token)
            .POST(HttpRequest.BodyPublishers.ofString("{\"feedback\":\"GOOD\"}")).build();
        var first=buyer.sendAsync(req,HttpResponse.BodyHandlers.ofString());
        var second=buyer.sendAsync(req,HttpResponse.BodyHandlers.ofString());
        assertEquals(List.of(200,409),List.of(first.join().statusCode(),second.join().statusCode()).stream().sorted().toList());
        assertEquals(366,students.selectOne("00202020").getTemp());
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM manner_ratings WHERE order_id=?",Integer.class,orderId));
    }

    @Autowired com.campus.remarket.notification.NotificationDispatcher notificationDispatcher;
    @Autowired demo.campus.campusremarketmember.member.repository.MemberRepository notificationMembers;
    void drainNotificationOutbox(){
        while(jdbc.queryForObject("SELECT COUNT(*) FROM notification_deliveries WHERE status='PENDING'",Integer.class)>0)notificationDispatcher.dispatch();
    }

    @Test void reservationNotifiesBothParticipantsWithSeparateOwnershipAndNoDuplicateReservation()throws Exception{
        var seller=student("20267701");var buyer=student("20267702");long saleId=sale(seller).get("selno").asLong();
        var order=reserve(buyer,saleId);String id=order.get("orderId").asText();
        var list=data(call(seller,"GET","/api/notifications",null,200));assertEquals(1,list.size());assertEquals(3,list.get(0).get("deliveries").size());
        var buyerList=data(call(buyer,"GET","/api/notifications",null,200));assertEquals(1,buyerList.size());
        assertEquals(3,buyerList.get(0).get("deliveries").size());
        assertEquals("판매 거래 약속이 잡혔어요",list.get(0).get("title").asText());
        assertEquals("구매 거래 약속이 잡혔어요",buyerList.get(0).get("title").asText());
        assertEquals(list.get(0).get("message"),buyerList.get(0).get("message"));
        String notificationId=list.get(0).get("id").asText(),buyerNotificationId=buyerList.get(0).get("id").asText();
        assertNotEquals(notificationId,buyerNotificationId);
        call(seller,"POST","/api/notifications/"+buyerNotificationId+"/read",null,404);
        call(buyer,"POST","/api/notifications/"+notificationId+"/read",null,404);call(browser(),"GET","/api/notifications",null,401);
        call(buyer,"POST","/api/trades","{\"selno\":"+saleId+"}",400);
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM seller_notifications WHERE order_id=?",Integer.class,id));
        drainNotificationOutbox();
        assertEquals(3,jdbc.queryForObject("SELECT COUNT(*) FROM notification_deliveries WHERE notification_id=? AND status='SETUP_REQUIRED'",Integer.class,notificationId));
        call(seller,"POST","/api/notifications/"+notificationId+"/read",null,204);
        assertFalse(data(call(seller,"GET","/api/notifications",null,200)).get(0).get("readAt").isNull());
        assertTrue(data(call(buyer,"GET","/api/notifications",null,200)).get(0).get("readAt").isNull());
        call(buyer,"POST","/api/notifications/"+buyerNotificationId+"/read",null,204);
        assertFalse(data(call(buyer,"GET","/api/notifications",null,200)).get(0).get("readAt").isNull());
    }
    @Test void notificationSettingsValidateContactsAndRespectOptOut()throws Exception{
        var seller=student("20267703");var buyer=student("20267704");
        call(seller,"PUT","/api/notifications/settings","{\"email\":\"invalid\",\"phone\":\"01012345678\"}",400);
        call(seller,"PUT","/api/notifications/settings","{\"email\":\"notify@inha.edu\",\"phone\":\"010-1234-5678\",\"smsEnabled\":false,\"emailEnabled\":false,\"kakaoEnabled\":false}",204);
        var prefs=data(call(seller,"GET","/api/notifications/settings",null,200));assertEquals("01012345678",prefs.get("phone").asText());assertFalse(prefs.get("deliveryEnabled").asBoolean());
        reserve(buyer,sale(seller).get("selno").asLong());drainNotificationOutbox();
        var n=data(call(seller,"GET","/api/notifications",null,200)).get(0);for(var d:n.get("deliveries"))assertEquals("DISABLED",d.get("status").asText());
    }
    @Test void configuredAdaptersUseEachParticipantsOwnContactsAndProviderReceipts()throws Exception{
        var seller=student("20267705");var buyer=student("20267706");reserve(buyer,sale(seller).get("selno").asLong());
        call(buyer,"PUT","/api/notifications/settings","{\"email\":\"buyer-notify@inha.edu\",\"phone\":\"01033334444\",\"smsEnabled\":true,\"emailEnabled\":true,\"kakaoEnabled\":true}",204);
        var n=data(call(seller,"GET","/api/notifications",null,200)).get(0);String notificationId=n.get("id").asText();
        String buyerNotificationId=data(call(buyer,"GET","/api/notifications",null,200)).get(0).get("id").asText();
        drainNotificationOutbox();
        var received=new java.util.concurrent.CopyOnWriteArrayList<String>();var auth=new java.util.concurrent.CopyOnWriteArrayList<String>();
        var provider=com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1",0),0);
        provider.createContext("/send",exchange->{received.add(new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8));auth.add(exchange.getRequestHeaders().getFirst("Authorization"));byte[] response="{\"groupInfo\":{\"groupId\":\"test-receipt\"},\"failedMessageList\":[]}".getBytes(StandardCharsets.UTF_8);exchange.getResponseHeaders().set("Content-Type","application/json");exchange.sendResponseHeaders(200,response.length);exchange.getResponseBody().write(response);exchange.close();});provider.start();
        try{
            var env=new org.springframework.mock.env.MockEnvironment().withProperty("campus.notifications.solapi-key","test-key").withProperty("campus.notifications.solapi-secret","test-secret").withProperty("campus.notifications.sms-from","0212345678").withProperty("campus.notifications.kakao-pf-id","test-channel").withProperty("campus.notifications.kakao-template-id","test-template").withProperty("campus.notifications.mail-from","sender@example.com").withProperty("campus.notifications.solapi-url","http://127.0.0.1:"+provider.getAddress().getPort()+"/send");
            var sender=org.mockito.Mockito.mock(org.springframework.mail.javamail.JavaMailSender.class);var beans=new org.springframework.beans.factory.support.StaticListableBeanFactory();beans.addBean("mail",sender);
            var enabled=new com.campus.remarket.notification.NotificationService(jdbc,students,notificationMembers,true);
            var worker=new com.campus.remarket.notification.NotificationDispatcher(jdbc,enabled,env,beans.getBeanProvider(org.springframework.mail.javamail.JavaMailSender.class),json);
            jdbc.update("UPDATE notification_deliveries SET status='PENDING' WHERE notification_id IN (?,?)",notificationId,buyerNotificationId);worker.dispatch();
            assertEquals(4,received.size());assertTrue(auth.stream().allMatch(a->a.startsWith("HMAC-SHA256 apiKey=test-key,")));
            var toCounts=new java.util.HashMap<String,Integer>();
            for(String body:received){var msg=json.readTree(body).get("messages").get(0);toCounts.merge(msg.get("to").asText(),1,Integer::sum);if(msg.get("type").asText().equals("ATA")){assertTrue(msg.get("kakaoOptions").get("disableSms").asBoolean());assertEquals("test-template",msg.get("kakaoOptions").get("templateId").asText());}}
            assertEquals(java.util.Map.of("01011112222",2,"01033334444",2),toCounts);
            var capture=org.mockito.ArgumentCaptor.forClass(org.springframework.mail.SimpleMailMessage.class);org.mockito.Mockito.verify(sender,org.mockito.Mockito.times(2)).send(capture.capture());
            assertEquals(java.util.Set.of("s20267705@inha.edu","buyer-notify@inha.edu"),capture.getAllValues().stream().map(m->m.getTo()[0]).collect(java.util.stream.Collectors.toSet()));
            assertEquals(3,jdbc.queryForObject("SELECT COUNT(*) FROM notification_deliveries WHERE notification_id=? AND status='ACCEPTED'",Integer.class,notificationId));
            assertEquals(3,jdbc.queryForObject("SELECT COUNT(*) FROM notification_deliveries WHERE notification_id=? AND status='ACCEPTED'",Integer.class,buyerNotificationId));
        }finally{provider.stop(0);}
    }

    @Test void failedNotificationCanRetryWithoutResendingAcceptedDeliveries()throws Exception{
        var seller=student("20267707");var buyer=student("20267708");reserve(buyer,sale(seller).get("selno").asLong());
        String id=data(call(seller,"GET","/api/notifications",null,200)).get(0).get("id").asText();drainNotificationOutbox();
        var fail=new java.util.concurrent.atomic.AtomicBoolean(true);var requests=new java.util.concurrent.atomic.AtomicInteger();
        var provider=com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1",0),0);
        provider.createContext("/send",e->{requests.incrementAndGet();e.getRequestBody().readAllBytes();String body=fail.get()?"{\"groupInfo\":{\"groupId\":\"mock\"},\"failedMessageList\":[{\"message\":\"rejected\"}]}":"{\"groupInfo\":{\"groupId\":\"mock\"},\"failedMessageList\":[]}";byte[] bytes=body.getBytes(StandardCharsets.UTF_8);e.sendResponseHeaders(200,bytes.length);e.getResponseBody().write(bytes);e.close();});provider.start();
        try{
            var env=new org.springframework.mock.env.MockEnvironment().withProperty("campus.notifications.solapi-key","test-key").withProperty("campus.notifications.solapi-secret","test-secret").withProperty("campus.notifications.sms-from","0212345678").withProperty("campus.notifications.kakao-pf-id","test-channel").withProperty("campus.notifications.kakao-template-id","test-template").withProperty("campus.notifications.mail-from","sender@example.test").withProperty("campus.notifications.solapi-url","http://127.0.0.1:"+provider.getAddress().getPort()+"/send");
            var sender=org.mockito.Mockito.mock(org.springframework.mail.javamail.JavaMailSender.class);org.mockito.Mockito.doThrow(new org.springframework.mail.MailSendException("mock failure")).when(sender).send(org.mockito.ArgumentMatchers.any(org.springframework.mail.SimpleMailMessage.class));
            var beans=new org.springframework.beans.factory.support.StaticListableBeanFactory();beans.addBean("mail",sender);
            var enabled=new com.campus.remarket.notification.NotificationService(jdbc,students,notificationMembers,true);
            var worker=new com.campus.remarket.notification.NotificationDispatcher(jdbc,enabled,env,beans.getBeanProvider(org.springframework.mail.javamail.JavaMailSender.class),json);
            call(buyer,"POST","/api/notifications/"+id+"/retry",null,404);call(seller,"POST","/api/notifications/"+id+"/retry",null,204);worker.dispatch();
            assertEquals(3,jdbc.queryForObject("SELECT COUNT(*) FROM notification_deliveries WHERE notification_id=? AND status='FAILED'",Integer.class,id));
            fail.set(false);org.mockito.Mockito.reset(sender);call(seller,"POST","/api/notifications/"+id+"/retry",null,204);worker.dispatch();
            assertEquals(3,jdbc.queryForObject("SELECT COUNT(*) FROM notification_deliveries WHERE notification_id=? AND status='ACCEPTED'",Integer.class,id));
            int sent=requests.get();call(seller,"POST","/api/notifications/"+id+"/retry",null,204);worker.dispatch();assertEquals(sent,requests.get());org.mockito.Mockito.verify(sender,org.mockito.Mockito.times(1)).send(org.mockito.ArgumentMatchers.any(org.springframework.mail.SimpleMailMessage.class));
        }finally{provider.stop(0);}
    }

    HttpResponse<String> review(HttpClient client,String orderId,String csrf,String body,int expected)throws Exception{
        var b=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/reviews")).header("Content-Type","application/json");if(csrf!=null)b.header("X-CSRF-Token",csrf);
        var r=client.send(b.POST(HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());assertEquals(expected,r.statusCode(),r.body());return r;
    }
    String reviewPayload(String id){return "{\"orderId\":\""+id+"\",\"score\":5,\"reviewerId\":\"forged\",\"targetId\":\"forged\",\"temperatureDelta\":999,\"attitude\":\"시간엄수\",\"content\":\"친절한 거래였습니다.\"}";}
    @Test void fiveScoreReviewUsesLoggedInActorAndRequiresCompletedTradeAndCsrf()throws Exception{
        var seller=student("20267901");var buyer=student("20267902");var outsider=student("20267903");long saleId=sale(seller).get("selno").asLong();String id=reserve(buyer,saleId).get("orderId").asText();String token=data(call(buyer,"GET","/api/trades/"+id+"/rating",null,200)).get("csrfToken").asText();
        review(browser(),id,token,reviewPayload(id),401);review(outsider,id,token,reviewPayload(id),403);review(buyer,id,null,reviewPayload(id),403);review(buyer,id,token,reviewPayload(id),400);
        call(buyer,"POST","/api/trades/"+id+"/complete",null,200);var saved=data(review(buyer,id,token,reviewPayload(id),200));assertEquals("20267902",saved.get("reviewerId").asText());assertEquals("20267901",saved.get("targetId").asText());assertEquals(0.5,saved.get("appliedDelta").asDouble());assertEquals(370,students.selectOne("20267901").getTemp());
        review(buyer,id,token,reviewPayload(id),400);var legacyState=data(call(buyer,"GET","/api/trades/"+id+"/rating",null,200));assertTrue(legacyState.get("rating").get("submitted").asBoolean());feedback(buyer,id,token,"GOOD",409);
        assertEquals(1,data(call(buyer,"GET","/api/reviews/user/20267901",null,200)).size());call(browser(),"GET","/api/reviews/user/20267901",null,401);
        var admin=browser();login(admin,null,"admin1");var adminRatings=data(call(admin,"GET","/api/admin/dashboard",null,200)).get("ratings");boolean found=false;for(var r:adminRatings)if(r.get("orderId").asText().equals(id)){found=true;assertEquals(5,r.get("score").asInt());assertEquals("친절한 거래였습니다.",r.get("content").asText());}assertTrue(found);
    }
    @Test void simultaneousFiveScoreSubmissionsApplyTemperatureOnce()throws Exception{
        var seller=student("20267904");var buyer=student("20267905");String id=reserve(buyer,sale(seller).get("selno").asLong()).get("orderId").asText();call(buyer,"POST","/api/trades/"+id+"/complete",null,200);String token=data(call(buyer,"GET","/api/trades/"+id+"/rating",null,200)).get("csrfToken").asText();
        var request=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/reviews")).header("Content-Type","application/json").header("X-CSRF-Token",token).POST(HttpRequest.BodyPublishers.ofString(reviewPayload(id))).build();
        var first=buyer.sendAsync(request,HttpResponse.BodyHandlers.ofString());var second=buyer.sendAsync(request,HttpResponse.BodyHandlers.ofString());var results=java.util.stream.Stream.of(first.join().statusCode(),second.join().statusCode()).sorted().toList();assertEquals(java.util.List.of(200,400),results);assertEquals(370,students.selectOne("20267904").getTemp());assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM trade_reviews WHERE order_id=?",Integer.class,id));
    }
}

