package com.campus.remarket;
import java.net.URI;
import java.net.CookieManager;
import java.net.http.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
 properties="spring.datasource.url=jdbc:h2:mem:staffportal;MODE=MySQL;DB_CLOSE_DELAY=-1")
@org.springframework.test.context.jdbc.Sql(scripts="/fixtures/staff-shop.sql",executionPhase=org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class StaffPortalTests {
    @LocalServerPort int port;
    @org.springframework.beans.factory.annotation.Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    final JsonMapper json=JsonMapper.builder().build();
    HttpClient browser(){return HttpClient.newBuilder().cookieHandler(new CookieManager()).build();}
    HttpResponse<String> call(HttpClient client,String method,String path,String body,String token,int expected)throws Exception{
        var b=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path));SecurityTestClient.csrf(client,b,method,path,port);
        if(body!=null)b.header("Content-Type","application/json");if(token!=null)b.header("X-CSRF-Token",token);
        b.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body));
        var result=client.send(b.build(),HttpResponse.BodyHandlers.ofString());assertEquals(expected,result.statusCode(),path+": "+result.body());return result;
    }
    JsonNode data(HttpResponse<String> r){return json.readTree(r.body());}
    HttpClient login(String id)throws Exception{var client=browser();call(client,"POST","/api/members/login","{\"loginId\":\""+id+"\",\"password\":\"campus1234!\"}",null,200);return client;}
    JsonNode ownerHome(HttpClient client)throws Exception{return data(call(client,"GET","/api/owner/dashboard",null,null,200));}
    JsonNode adminHome(HttpClient client)throws Exception{return data(call(client,"GET","/api/admin/dashboard",null,null,200));}
    HttpClient newOwner(String id,String businessNo)throws Exception{
        var client=browser();call(client,"POST","/api/members/signup/owner","{\"loginId\":\""+id+"\",\"password\":\"campus1234!\",\"name\":\"추가사장\",\"phone\":\"010-2222-3333\",\"email\":\""+id+"@example.com\",\"businessNo\":\""+businessNo+"\"}",null,201);
        call(client,"POST","/api/members/login","{\"loginId\":\""+id+"\",\"password\":\"campus1234!\"}",null,200);return client;
    }
    String payload(String name){return "{\"shopname\":\""+name+"\",\"shopphone\":\"032-111-2222\",\"busiKind\":\"카페\",\"bmenu\":\"커피 3000원\",\"reservtime\":\"전화 문의\",\"busitime\":\"10:00~20:00\",\"offday\":\"일요일\",\"specialevent\":\"학생 할인\",\"sizeandtype\":\"4인석\",\"status\":\"영업중\",\"availablespace\":\"2자리\"}";}

    @Test void anonymousAndStudentCannotReadStaffData()throws Exception{
        var guest=browser();call(guest,"GET","/api/owner/dashboard",null,null,401);call(guest,"GET","/api/admin/dashboard",null,null,401);
        call(guest,"GET","/api/owner/shop/history",null,null,401);
        var schools=data(call(guest,"GET","/api/members/schools",null,null,200));long school=0;for(var s:schools)if(s.get("name").asText().equals("인하대학교"))school=s.get("id").asLong();
        call(guest,"POST","/api/members/login","{\"schoolId\":"+school+",\"loginId\":\"20261234\",\"password\":\"campus1234!\"}",null,200);
        call(guest,"GET","/api/owner/dashboard",null,null,403);call(guest,"GET","/api/admin/dashboard",null,null,403);
        call(guest,"GET","/api/owner/shop/history",null,null,403);
    }
    @Test void ownerAndAdminRolesAreSeparatedAndSensitiveFieldsAreOmitted()throws Exception{
        var owner=login("owner1");call(owner,"GET","/api/admin/dashboard",null,null,403);
        var home=ownerHome(owner);assertEquals("OWNER",home.get("me").get("role").asText());assertNotNull(home.get("shop"));
        var admin=login("admin1");call(admin,"GET","/api/owner/dashboard",null,null,403);
        var response=call(admin,"GET","/api/admin/dashboard",null,null,200);var body=response.body();assertFalse(body.contains("password"));assertFalse(body.contains("ceopw"));assertFalse(body.contains("man1pw"));assertFalse(body.contains("paymentKey"));
        assertTrue(data(response).get("counts").get("members").asInt()>=6);
    }
    @Test void managerCanUpdateOperationsButCannotEditShopIdentityOrCreateManagers()throws Exception{
        var manager=login("manager1");var home=ownerHome(manager);String token=home.get("csrfToken").asText();
        call(manager,"PUT","/api/owner/shop",payload("불가"),token,403);call(manager,"POST","/api/owner/shop",payload("불가"),token,403);
        call(manager,"POST","/api/members/managers","{\"loginId\":\"forbidden\",\"name\":\"매니저\",\"password\":\"campus1234!\"}",token,403);
        call(manager,"PUT","/api/owner/shop/operations",payload("이름은변경되지않음"),null,403);
        var updated=data(call(manager,"PUT","/api/owner/shop/operations",payload("이름은변경되지않음"),token,200));
        assertNotEquals("이름은변경되지않음",updated.get("name").asText());assertEquals("10:00~20:00",updated.get("hours").asText());
        var publicShops=data(call(browser(),"GET","/api/shops",null,null,200));assertTrue(publicShops.toString().contains("커피 3000원"));
    }
    @Test void newShopRequiresAdminApprovalAndOwnerCannotApproveSelf()throws Exception{
        var owner=newOwner("portalowner2","876-54-32101");var home=ownerHome(owner);assertTrue(home.get("shop").isNull());String token=home.get("csrfToken").asText();
        call(owner,"POST","/api/owner/shop",payload("등록가게"),null,403);
        var shop=data(call(owner,"POST","/api/owner/shop",payload("등록가게"),token,200));long id=shop.get("id").asLong();assertFalse(shop.get("approved").asBoolean());
        call(owner,"POST","/api/owner/shop",payload("중복"),token,409);call(owner,"PUT","/api/admin/shops/"+id+"/approval","{\"approved\":true}",token,403);
        assertFalse(call(browser(),"GET","/api/shops",null,null,200).body().contains("등록가게"));call(browser(),"GET","/shops/selectOne?id="+id,null,null,404);
        var admin=login("admin1");String adminToken=adminHome(admin).get("csrfToken").asText();
        call(admin,"PUT","/api/admin/shops/"+id+"/approval","{\"approved\":true}",null,403);
        call(admin,"PUT","/api/admin/shops/"+id+"/approval","{\"approved\":true}",adminToken,200);
        assertTrue(call(browser(),"GET","/api/shops",null,null,200).body().contains("등록가게"));call(browser(),"GET","/shops/selectOne?id="+id,null,null,200);
        call(admin,"PUT","/api/admin/shops/"+id+"/approval","{\"approved\":false}",adminToken,200);
        assertFalse(call(browser(),"GET","/api/shops",null,null,200).body().contains("등록가게"));
    }
    @Test void shopAssignmentCannotGrantStudentOrManagerOwnershipAndOldOwnerLosesAccess()throws Exception{
        var first=newOwner("portalowner3","876-54-32102");var second=newOwner("portalowner4","876-54-32103");
        var firstHome=ownerHome(first);String firstToken=firstHome.get("csrfToken").asText();long id=data(call(first,"POST","/api/owner/shop",payload("배정가게"),firstToken,200)).get("id").asLong();long secondId=ownerHome(second).get("me").get("id").asLong();
        var admin=login("admin1");var dashboard=adminHome(admin);String token=dashboard.get("csrfToken").asText();long student=0,manager=0;
        for(var m:dashboard.get("members")){if(m.get("role").asText().equals("STUDENT"))student=m.get("id").asLong();if(m.get("role").asText().equals("MANAGER"))manager=m.get("id").asLong();}
        for(long wrong:new long[]{student,manager})call(admin,"PUT","/api/admin/shops/"+id+"/owner","{\"ownerId\":"+wrong+"}",token,400);
        call(first,"PUT","/api/admin/shops/"+id+"/owner","{\"ownerId\":"+secondId+"}",firstToken,403);
        call(admin,"PUT","/api/admin/shops/"+id+"/owner","{\"ownerId\":"+secondId+"}",token,200);
        call(first,"PUT","/api/owner/shop/operations",payload("불가"),firstToken,404);
        assertEquals(id,ownerHome(second).get("shop").get("id").asLong());assertTrue(ownerHome(first).get("shop").isNull());
    }
    @Test void shopFieldLimitsAreValidatedBeforeDatabaseWrites()throws Exception{
        var owner=newOwner("portalowner5","876-54-32104");String token=ownerHome(owner).get("csrfToken").asText();
        call(owner,"POST","/api/owner/shop",payload("x".repeat(51)),token,400);
        call(owner,"POST","/api/owner/shop",payload("정상").replace("10:00~20:00",""),token,400);
        assertTrue(ownerHome(owner).get("shop").isNull());
    }
    @Test void historyRecordsRealChangesOnlyAndRejectsFailedEdits()throws Exception{
        var owner=newOwner("historyowner1","986-54-32101");String token=ownerHome(owner).get("csrfToken").asText();
        call(owner,"POST","/api/owner/shop",payload("이력가게"),token,200);
        assertEquals(0,data(call(owner,"GET","/api/owner/shop/history",null,null,200)).size());
        String changed=payload("이력가게").replace("영업중","혼잡");call(owner,"PUT","/api/owner/shop/operations",changed,token,200);
        var rows=data(call(owner,"GET","/api/owner/shop/history",null,null,200));assertEquals(1,rows.size());var row=rows.get(0);
        assertEquals("status",row.get("fieldKey").asText());assertEquals("영업중",row.get("beforeValue").asText());assertEquals("혼잡",row.get("afterValue").asText());assertEquals("OWNER",row.get("actorRole").asText());assertEquals("추가사장",row.get("actorName").asText());assertFalse(row.get("changedAt").isNull());
        call(owner,"PUT","/api/owner/shop/operations",changed,token,200);
        call(owner,"PUT","/api/owner/shop/operations",changed.replace("혼잡","INVALID"),token,400);
        assertEquals(1,data(call(owner,"GET","/api/owner/shop/history",null,null,200)).size());
        assertEquals("혼잡",ownerHome(owner).get("shop").get("status").asText());
    }
    @Test void managerHistoryRetainsActorAfterDeletionAndOtherOwnersCannotReadIt()throws Exception{
        var owner=newOwner("historyowner2","986-54-32102");String token=ownerHome(owner).get("csrfToken").asText();
        call(owner,"POST","/api/owner/shop",payload("매니저이력가게"),token,200);
        var manager=data(call(owner,"POST","/api/members/managers","{\"loginId\":\"historymanager\",\"password\":\"campus1234!\",\"name\":\"이력매니저\"}",token,201));
        var staff=login("historymanager");String staffToken=ownerHome(staff).get("csrfToken").asText();
        call(staff,"PUT","/api/owner/shop/operations",payload("매니저이력가게").replace("커피 3000원","신메뉴 4000원").replace("10:00~20:00","11:00~22:00"),staffToken,200);
        assertEquals(2,data(call(staff,"GET","/api/owner/shop/history",null,null,200)).size());
        call(owner,"DELETE","/api/members/managers/"+manager.get("id").asLong(),null,token,200);
        var rows=data(call(owner,"GET","/api/owner/shop/history",null,null,200));assertEquals(2,rows.size());for(var row:rows){assertEquals("이력매니저",row.get("actorName").asText());assertEquals("MANAGER",row.get("actorRole").asText());}
        var other=newOwner("historyowner3","986-54-32103");assertEquals(0,data(call(other,"GET","/api/owner/shop/history?ownerId="+ownerHome(owner).get("me").get("id").asLong(),null,null,200)).size());
        call(login("admin1"),"GET","/api/owner/shop/history",null,null,403);
    }
    @Test void statusOnlyWritesAndStaleFormsNeverOverwriteNewerData()throws Exception{
        var owner=newOwner("v13owner1","686-54-32101");String token=ownerHome(owner).get("csrfToken").asText();
        var shop=data(call(owner,"POST","/api/owner/shop",payload("동시수정가게"),token,200));long version=shop.get("version").asLong();
        call(owner,"PUT","/api/owner/shop/status","{\"status\":\"혼잡\",\"version\":"+version+"}",null,403);
        var updated=data(call(owner,"PUT","/api/owner/shop/status","{\"status\":\"혼잡\",\"version\":"+version+"}",token,200));
        assertEquals(version+1,updated.get("version").asLong());assertEquals(shop.get("menu"),updated.get("menu"));assertEquals(shop.get("hours"),updated.get("hours"));
        String stale=payload("동시수정가게").replace("}",",\"version\":"+version+"}");
        call(owner,"PUT","/api/owner/shop/operations",stale,token,409);
        call(owner,"PUT","/api/owner/shop",stale,token,409);
        call(owner,"PUT","/api/owner/shop/status","{\"status\":\"잘못된상태\"}",token,400);
        var unchanged=data(call(owner,"PUT","/api/owner/shop/status","{\"status\":\"혼잡\",\"version\":"+(version+1)+"}",token,200));
        assertEquals(version+1,unchanged.get("version").asLong());assertEquals("혼잡",ownerHome(owner).get("shop").get("status").asText());
        assertEquals(1,data(call(owner,"GET","/api/owner/shop/history",null,null,200)).size());
    }
    @Test void twoSimultaneousWritersAcceptExactlyOneVersion()throws Exception{
        var owner=newOwner("v13owner2","686-54-32102");String token=ownerHome(owner).get("csrfToken").asText();
        long version=data(call(owner,"POST","/api/owner/shop",payload("경쟁가게"),token,200)).get("version").asLong();
        var second=login("v13owner2");String secondToken=ownerHome(second).get("csrfToken").asText();
        var firstRequest=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/owner/shop/status")).header("Content-Type","application/json").header("X-CSRF-Token",token).PUT(HttpRequest.BodyPublishers.ofString("{\"status\":\"혼잡\",\"version\":"+version+"}")).build();
        var secondRequest=HttpRequest.newBuilder(firstRequest.uri()).header("Content-Type","application/json").header("X-CSRF-Token",secondToken).PUT(HttpRequest.BodyPublishers.ofString("{\"status\":\"휴무\",\"version\":"+version+"}")).build();
        var first=owner.sendAsync(firstRequest,HttpResponse.BodyHandlers.ofString());var other=second.sendAsync(secondRequest,HttpResponse.BodyHandlers.ofString());
        var codes=new java.util.ArrayList<>(java.util.List.of(first.get().statusCode(),other.get().statusCode()));java.util.Collections.sort(codes);assertEquals(java.util.List.of(200,409),codes);
        assertEquals(version+1,ownerHome(owner).get("shop").get("version").asLong());assertEquals(1,data(call(owner,"GET","/api/owner/shop/history",null,null,200)).size());
    }
    @Test void adminAuditRecordsRealChangesWithNotesAndRejectsStaleOrMalformedRequests()throws Exception{
        var owner=newOwner("v13owner3","686-54-32103");String ownerToken=ownerHome(owner).get("csrfToken").asText();
        var shop=data(call(owner,"POST","/api/owner/shop",payload("관리이력가게"),ownerToken,200));long id=shop.get("id").asLong(),version=shop.get("version").asLong();
        var admin=login("admin1");String token=adminHome(admin).get("csrfToken").asText();String base="/api/admin/shops/"+id;
        call(admin,"PUT",base+"/approval","{}",token,400);
        String approval="{\"approved\":true,\"version\":"+version+",\"note\":\"서류 확인 완료\"}";
        call(admin,"PUT",base+"/approval",approval,null,403);call(admin,"PUT",base+"/approval",approval,token,200);call(admin,"PUT",base+"/approval",approval,token,409);
        assertEquals("서류 확인 완료",ownerHome(owner).get("publication").get("note").asText());
        call(admin,"PUT",base+"/approval","{\"approved\":true,\"version\":"+(version+1)+"}",token,200);
        call(admin,"PUT",base+"/approval","{\"approved\":false,\"note\":\""+"x".repeat(301)+"\"}",token,400);
        var replacement=newOwner("v13owner4","686-54-32104");long replacementId=ownerHome(replacement).get("me").get("id").asLong();
        call(admin,"PUT",base+"/owner","{\"ownerId\":"+replacementId+",\"version\":"+(version+1)+",\"note\":\"담당자 변경\"}",token,200);
        assertTrue(ownerHome(owner).get("shop").isNull());assertEquals(id,ownerHome(replacement).get("shop").get("id").asLong());
        var rows=data(call(admin,"GET","/api/admin/history",null,null,200));int found=0;boolean noteFound=false;for(var row:rows)if(row.get("shopId").asLong()==id){found++;assertEquals("관리이력가게",row.get("shopName").asText());assertFalse(row.get("actorName").asText().isBlank());if(row.get("note").asText().equals("서류 확인 완료"))noteFound=true;}
        assertEquals(2,found);assertTrue(noteFound);
    }
    @Test void adminHistoryAndStatusRespectRoleBoundaries()throws Exception{
        call(browser(),"GET","/api/admin/history",null,null,401);call(login("owner1"),"GET","/api/admin/history",null,null,403);call(login("manager1"),"GET","/api/admin/history",null,null,403);
        var admin=login("admin1");String token=adminHome(admin).get("csrfToken").asText();call(admin,"PUT","/api/owner/shop/status","{\"status\":\"영업중\"}",token,403);
    }
    @Test void adminAuditFailureRollsBackApprovalAndVersion()throws Exception{
        var owner=newOwner("v13owner5","686-54-32105");String ownerToken=ownerHome(owner).get("csrfToken").asText();
        var shop=data(call(owner,"POST","/api/owner/shop",payload("관리원자성가게"),ownerToken,200));long id=shop.get("id").asLong();var admin=login("admin1");String token=adminHome(admin).get("csrfToken").asText();
        jdbc.execute("ALTER TABLE admin_action_logs ADD CONSTRAINT ck_admin_test CHECK (shop_id <> "+id+")");
        try{call(admin,"PUT","/api/admin/shops/"+id+"/approval","{\"approved\":true}",token,500);var current=ownerHome(owner).get("shop");assertFalse(current.get("approved").asBoolean());assertEquals(shop.get("version"),current.get("version"));assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM admin_action_logs WHERE shop_id=?",Integer.class,id));}finally{jdbc.execute("ALTER TABLE admin_action_logs DROP CONSTRAINT ck_admin_test");}
    }
    @Test void auditFailureRollsBackShopChangeInSameTransaction()throws Exception{
        var owner=newOwner("historyowner4","986-54-32104");String token=ownerHome(owner).get("csrfToken").asText();
        long id=data(call(owner,"POST","/api/owner/shop",payload("원자성가게"),token,200)).get("id").asLong();
        jdbc.execute("ALTER TABLE shop_change_logs ADD CONSTRAINT ck_history_test CHECK (shop_id <> "+id+" OR field_key <> 'status')");
        try{
            call(owner,"PUT","/api/owner/shop/operations",payload("원자성가게").replace("영업중","혼잡"),token,500);
            assertEquals("영업중",ownerHome(owner).get("shop").get("status").asText());
            assertEquals(0,data(call(owner,"GET","/api/owner/shop/history",null,null,200)).size());
        }finally{jdbc.execute("ALTER TABLE shop_change_logs DROP CONSTRAINT ck_history_test");}
    }
    @Test void shopCoordinatesValidatePersistAndRemainVisibleWithStatus()throws Exception{
        var owner=newOwner("mapowner14","876-54-32114");String csrf=ownerHome(owner).get("csrfToken").asText();
        String invalid=payload("지도 등록 업체").replace("}",",\"address\":\"인천 캠퍼스 앞\",\"latitude\":91,\"longitude\":126.655}");call(owner,"POST","/api/owner/shop",invalid,csrf,400);
        var created=data(call(owner,"POST","/api/owner/shop",invalid.replace("91,","37.4495,"),csrf,200));long id=created.get("id").asLong();assertEquals("인천 캠퍼스 앞",created.get("address").asText());
        var admin=login("admin1");String ac=adminHome(admin).get("csrfToken").asText();call(admin,"PUT","/api/admin/shops/"+id+"/approval","{\"approved\":true}",ac,200);
        boolean found=false;for(var store:data(call(browser(),"GET","/api/shops",null,null,200)))if(store.get("id").asLong()==id){found=true;assertEquals(37.4495,store.get("latitude").asDouble(),0.00001);assertEquals("영업중",store.get("status").asText());}assertTrue(found);
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME='SHOPS' AND COLUMN_NAME IN ('CEOID','CEOPW','MAN1ID','MAN1PW','MAN1NAME','MAN2ID','MAN2PW','MAN2NAME','MAN3ID','MAN3PW','MAN3NAME','REPUTATION')",Integer.class));
    }

    @Test void ownerLocationSaveChecksRoleCsrfVersionAndPersistsCoordinates()throws Exception{
        var owner=login("owner1");var home=ownerHome(owner);var shop=home.get("shop");String token=home.get("csrfToken").asText();String body="{\"latitude\":37.451234,\"longitude\":126.654321,\"version\":"+shop.get("version").asLong()+",\"address\":\"인천 미추홀구 용현동 253\"}";
        call(browser(),"PUT","/api/owner/shop/location",body,token,401);call(login("manager1"),"PUT","/api/owner/shop/location",body,token,403);call(owner,"PUT","/api/owner/shop/location",body,null,403);call(owner,"PUT","/api/owner/shop/location",body,token,200);var saved=ownerHome(owner).get("shop");assertEquals(37.451234,saved.get("latitude").asDouble(),0.000001);assertEquals("인천 미추홀구 용현동 253",saved.get("address").asText());call(owner,"PUT","/api/owner/shop/location",body,token,409);
    }
}
