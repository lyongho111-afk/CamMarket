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
    "spring.datasource.url=jdbc:h2:mem:sharedlogintests;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "campus.notifications.initial-delay-ms=3600000"})
class SharedLoginTests {
    @LocalServerPort int port;
    final JsonMapper json=JsonMapper.builder().build();
    HttpClient browser(){return HttpClient.newBuilder().cookieHandler(new CookieManager()).build();}
    HttpResponse<String> call(HttpClient client,String method,String path,String body,int expected)throws Exception{
        var request=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path));SecurityTestClient.csrf(client,request,method,path,port);
        if(body!=null)request.header("Content-Type","application/json");
        var response=client.send(request.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
        assertEquals(expected,response.statusCode(),path+": "+response.body());return response;
    }
    JsonNode data(HttpResponse<String> response){return json.readTree(response.body());}
    String credentials(String login,String role){return json.writeValueAsString(java.util.Map.of("loginId",login,"password","campus1234!","role",role));}

    @Test void oneLoginWorksAcrossMarketAndMatchingPortalAndLogoutIsShared()throws Exception{
        for(String role:new String[]{"OWNER","MANAGER","ADMIN"}){
            var client=browser();String login=role.toLowerCase()+"1",portal=role.equals("ADMIN")?"admin":"owner";
            var response=call(client,"POST","/api/members/login",credentials(login,role),200);
            assertTrue(response.headers().allValues("set-cookie").stream().anyMatch(value->value.startsWith("JSESSIONID=")&&value.contains("Path=/")&&value.toLowerCase().contains("httponly")));
            long memberId=data(response).get("id").asLong();
            assertEquals(memberId,data(call(client,"GET","/api/session",null,200)).get("member").get("id").asLong());
            assertTrue(call(client,"GET","/"+portal+"/",null,200).body().contains("id=\"app\""));
            assertEquals(role,data(call(client,"GET","/api/"+portal+"/dashboard",null,200)).get("me").get("role").asText());
            call(client,"GET","/sales/selectAll",null,200);
            assertEquals(memberId,data(call(client,"GET","/api/session",null,200)).get("member").get("id").asLong());
            call(client,"POST","/api/members/logout",null,200);
            assertFalse(data(call(client,"GET","/api/session",null,200)).get("authenticated").asBoolean());
            call(client,"GET","/api/"+portal+"/dashboard",null,401);
        }
    }
    @Test void roleSelectionCannotChangeAccountRoleOrReplaceExistingSessionOnFailure()throws Exception{
        var client=browser();call(client,"POST","/api/members/login",credentials("owner1","OWNER"),200);
        for(String role:new String[]{"MANAGER","ADMIN","STUDENT","invalid"}){
            call(client,"POST","/api/members/login",credentials("owner1",role),403);
            assertEquals("OWNER",data(call(client,"GET","/api/session",null,200)).get("member").get("role").asText());
        }
        call(client,"GET","/api/admin/dashboard",null,403);
        var guest=browser();call(guest,"POST","/api/members/login",credentials("manager1","OWNER"),403);
        assertFalse(data(call(guest,"GET","/api/session",null,200)).get("authenticated").asBoolean());
    }
    @Test void canonicalPagesAssetsAndLegacyManagerRedirectAreAvailable()throws Exception{
        var client=browser();var links=data(call(client,"GET","/api/portals",null,200));
        assertEquals("/",links.get("student").asText());assertEquals("/owner/",links.get("owner").asText());assertEquals("/admin/",links.get("admin").asText());
        for(String site:new String[]{"owner","admin"}){
            call(client,"GET","/"+site,null,302);
            call(client,"GET","/"+site+"/",null,200);
            for(String asset:new String[]{"app.js","common.js","styles.css"})call(client,"GET","/"+site+"/"+asset,null,200);
        }
        var redirect=call(client,"GET","/manager.html",null,302);
        assertTrue(redirect.headers().firstValue("location").orElseThrow().endsWith("/owner/#managers"));
        call(client,"GET","/api/owner/dashboard",null,401);call(client,"GET","/api/admin/dashboard",null,401);
    }
    @Test void oldGatewayEntryRedirectsToSameSessionOrigin(){
        for(String site:new String[]{"owner","admin"}){
            var gateway=new portal.campus.PortalGatewayController("http://127.0.0.1:8080",site,"https://campus.example/");
            assertEquals(302,gateway.entry().getStatusCode().value());
            assertEquals("https://campus.example/"+site+"/",gateway.entry().getHeaders().getLocation().toString());
        }
    }
}
