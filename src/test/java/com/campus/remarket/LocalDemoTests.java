package com.campus.remarket;

import com.campus.remarket.config.LocalDemoMailConfig.Mailbox;
import com.campus.remarket.notification.NotificationDispatcher;
import com.campus.remarket.service.StudentMemberLink;
import demo.campus.campusremarketmember.member.entity.*;
import demo.campus.campusremarketmember.member.repository.*;
import java.net.*;
import java.net.http.*;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("local-demo")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, properties={"campus.notifications.initial-delay-ms=3600000"})
class LocalDemoTests {
    @LocalServerPort int port;
    @Autowired Mailbox inbox;
    @Autowired MemberRepository members;
    @Autowired SchoolRepository schools;
    @Autowired StudentMemberLink links;
    @Autowired PasswordEncoder encoder;
    @Autowired NotificationDispatcher dispatcher;
    final JsonMapper json=JsonMapper.builder().build();
    HttpClient browser(){return HttpClient.newBuilder().cookieHandler(new CookieManager()).build();}
    JsonNode call(HttpClient client,String method,String path,Object body,int expected)throws Exception {
        var req=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path));SecurityTestClient.csrf(client,req,method,path,port);
        if(body!=null)req.header("Content-Type","application/json");
        req.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
        var result=client.send(req.build(),HttpResponse.BodyHandlers.ofString());
        assertEquals(expected,result.statusCode(),path+": "+result.body());
        return result.body().isBlank()?json.createObjectNode():json.readTree(result.body());
    }
    String code(String email){return inbox.messages().stream().filter(m->m.to().equals(email)).findFirst().orElseThrow().body().replaceAll("(?s).*인증번호: (\\d{6}).*","$1");}

    @Test void accountRecoveryGoesToLocalInboxAndCodeIsConsumedOnce()throws Exception {
        var client=browser();var status=call(client,"GET","/api/account/status",null,200);
        assertTrue(status.path("localDemo").asBoolean());assertTrue(status.path("emailRecoveryAvailable").asBoolean());
        var response=call(client,"POST","/api/account/code",Map.of("email","owner1@campus.local","purpose","ID"),200);
        assertFalse(response.has("code"));
        assertEquals("owner1",call(client,"POST","/api/account/verify",Map.of("purpose","ID","code",code("owner1@campus.local")),200).path("loginId").asText());
        call(client,"POST","/api/account/verify",Map.of("purpose","ID","code",code("owner1@campus.local")),400);
    }

    @Test void passwordResetChangesRealLoginAndInvalidatesEarlierSession()throws Exception {
        var old=browser();call(old,"POST","/api/members/login",Map.of("loginId","admin3","password","campus1234!"),200);
        var client=browser();call(client,"POST","/api/account/code",Map.of("email","admin3@campus.local","purpose","RESET"),200);
        call(client,"POST","/api/account/verify",Map.of("purpose","RESET","code","invalid","password","newSecret123!"),400);
        call(client,"POST","/api/account/verify",Map.of("purpose","RESET","code",code("admin3@campus.local"),"password","newSecret123!"),200);
        call(old,"GET","/api/members/me",null,401);
        call(client,"POST","/api/members/login",Map.of("loginId","admin3","password","campus1234!"),401);
        call(client,"POST","/api/members/login",Map.of("loginId","admin3","password","newSecret123!"),200);
    }

    @Test void localInboxRejectsProxyAccessAndDoesNotAllowBrowserCaching()throws Exception {
        var client=browser();var uri=URI.create("http://127.0.0.1:"+port+"/api/local-demo/mailbox");
        var direct=client.send(HttpRequest.newBuilder(uri).GET().build(),HttpResponse.BodyHandlers.ofString());
        assertEquals(200,direct.statusCode());assertTrue(direct.headers().firstValue("Cache-Control").orElse("").contains("no-store"));
        for(String header:new String[]{"X-Forwarded-For","X-Forwarded-Host","Forwarded","Sec-Fetch-Site"}){
            var proxied=client.send(HttpRequest.newBuilder(uri).header(header,header.equals("Sec-Fetch-Site")?"cross-site":"outside.example").GET().build(),HttpResponse.BodyHandlers.ofString());
            assertEquals(404,proxied.statusCode());assertFalse(proxied.body().contains("인증번호:"));
        }
    }

    @Test void reservationNotifiesBothParticipantsWithLocalEmailWithoutPaidChannels()throws Exception {
        var school=schools.findByName("인하대학교").orElseThrow();
        for(String id:new String[]{"localSeller","localBuyer"}){var m=members.saveAndFlush(Member.student(id,encoder.encode("campus1234!"),id,id+"@inha.edu","010-1111-2222",StudentStatus.ENROLLED,school));links.link(m);}
        var seller=browser();var buyer=browser();
        call(seller,"POST","/api/members/login",Map.of("schoolId",school.getId(),"loginId","localSeller","password","campus1234!"),200);
        call(buyer,"POST","/api/members/login",Map.of("schoolId",school.getId(),"loginId","localBuyer","password","campus1234!"),200);
        var product=call(seller,"POST","/api/products",Map.of("name","무료 알림 확인 책","category","전공서적","oprice",10000),200);
        call(buyer,"POST","/api/trades",Map.of("selno",product.path("selno").asLong(),"meetingPlace","학교 정문"),200);
        dispatcher.dispatch();
        var notices=call(seller,"GET","/api/notifications",null,200);assertEquals(1,notices.size());
        var buyerNotices=call(buyer,"GET","/api/notifications",null,200);assertEquals(1,buyerNotices.size());
        assertTrue(inbox.messages().stream().anyMatch(m->m.to().equals("localSeller@inha.edu")&&m.body().contains("학교 정문")));
        assertTrue(inbox.messages().stream().anyMatch(m->m.to().equals("localBuyer@inha.edu")&&m.body().contains("학교 정문")));
        for(var delivery:notices.get(0).path("deliveries"))assertEquals(delivery.path("channel").asText().equals("EMAIL")?"ACCEPTED":"DISABLED",delivery.path("status").asText());
        for(var delivery:buyerNotices.get(0).path("deliveries"))assertEquals(delivery.path("channel").asText().equals("EMAIL")?"ACCEPTED":"DISABLED",delivery.path("status").asText());
    }
}
