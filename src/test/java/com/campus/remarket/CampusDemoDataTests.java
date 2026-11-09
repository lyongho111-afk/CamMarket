package com.campus.remarket;

import com.campus.remarket.config.CampusDemoDataSeed;
import java.net.*;
import java.net.http.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
    "spring.datasource.url=jdbc:h2:mem:campusdemodata;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "campus.notifications.initial-delay-ms=3600000"})
class CampusDemoDataTests {
    @Autowired com.campus.remarket.config.RequestedCatalogSeed photos;
    @Autowired CampusDemoDataSeed seed;@Autowired JdbcTemplate db;@LocalServerPort int port;
    final JsonMapper json=JsonMapper.builder().build();
    HttpResponse<String> request(HttpClient c,String method,String path,Object data)throws Exception{
        var b=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path));SecurityTestClient.csrf(c,b,method,path,port);
        if(data!=null)b.header("Content-Type","application/json");
        return c.send(b.method(method,data==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json.writeValueAsString(data))).build(),HttpResponse.BodyHandlers.ofString());
    }
    @Test void twelveNumericAccountsAndEightListingsPerSchoolPreserveHistoryAndDoNotDuplicate()throws Exception{
        var original=db.queryForList("SELECT selno,name,oprice,status,discount,starttime,endtime,regtime,photo1,photo2,photo3 FROM sales ORDER BY selno");
        var memberIds=db.queryForList("SELECT member_id FROM student WHERE unisno LIKE 'demo-seller%' ORDER BY unisno",Long.class);
        db.update("INSERT INTO shops(shopname,address,latitude,longitude) VALUES(?,?, -11,-1)","포케품은초밥 인하대점","인천 미추홀구 인하로77번길 6-29 1층");
        seed.seed();
        assertEquals(original,db.queryForList("SELECT selno,name,oprice,status,discount,starttime,endtime,regtime,photo1,photo2,photo3 FROM sales WHERE selno BETWEEN 1 AND 8 ORDER BY selno"));
        assertEquals(memberIds,db.queryForList("SELECT member_id FROM student WHERE unisno LIKE 'demo-seller%' ORDER BY unisno",Long.class));
        assertEquals(12,db.queryForObject("SELECT COUNT(*) FROM member WHERE role='STUDENT'",Integer.class));
        assertEquals(24,db.queryForObject("SELECT COUNT(*) FROM sales",Integer.class));
        assertEquals(2,db.queryForObject("SELECT COUNT(*) FROM trade_orders",Integer.class));
        assertEquals(2,db.queryForObject("SELECT COUNT(*) FROM trade_orders WHERE buyer='20261234'",Integer.class));
        var coords=db.queryForMap("SELECT latitude,longitude FROM shops WHERE shopname=?","포케품은초밥 인하대점");
        assertEquals(37.4512722,((Number)coords.get("LATITUDE")).doubleValue(),0.0000001);
        assertEquals(126.6580021,((Number)coords.get("LONGITUDE")).doubleValue(),0.0000001);
        String[][] groups={{"INHA","20261231","20261232","20261234","20261235"},
            {"INU","20231231","20231232","20231234","20221235"},{"CHUNGWOON","20251231","20251232","20251235","20261235"}};
        for(var group:groups){
            long school=db.queryForObject("SELECT id FROM school WHERE campus_code=?",Long.class,group[0]);
            assertEquals(4,db.queryForObject("SELECT COUNT(*) FROM member WHERE role='STUDENT' AND school_id=?",Integer.class,school));
            var ownership=db.queryForList("SELECT COUNT(*) AS n FROM sales WHERE school_id=? GROUP BY seller_member_id",Integer.class,school);
            assertEquals(List.of(4,4),ownership.stream().sorted().toList());
            for(int i=1;i<group.length;i++){
                var client=HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
                var login=request(client,"POST","/api/members/login",Map.of("schoolId",school,"loginId",group[i],"password","campus1234!"));
                assertEquals(200,login.statusCode(),login.body());
                var sales=request(client,"GET","/api/products",null);assertEquals(200,sales.statusCode());
                var rows=json.readTree(sales.body());assertEquals(8,rows.size());
                for(var row:rows)assertEquals(school,row.get("schoolId").asLong());
                long other=db.queryForObject("SELECT MIN(selno) FROM sales WHERE school_id<>?",Long.class,school);
                assertEquals(404,request(client,"GET","/api/products/"+other,null).statusCode());
                assertEquals(200,request(client,"GET","/api/profiles/me",null).statusCode());
            }
        }
        var guest=HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
        assertEquals(24,json.readTree(request(guest,"GET","/api/products",null).body()).size());
        long outsideInha=db.queryForObject("SELECT MAX(selno) FROM sales",Long.class);
        assertTrue(json.readTree(request(guest,"GET","/api/engagement/state?targets=SALE:"+outsideInha,null).body()).get(0).get("available").asBoolean());
        assertEquals(200,request(guest,"GET","/api/products/1",null).statusCode());
        assertEquals(401,request(guest,"POST","/api/products",Map.of("name","unauthorized")).statusCode());
        var identity=db.queryForList("SELECT selno,seller_member_id,oprice,status FROM sales ORDER BY selno");
        photos.seed();photos.seed();
        assertEquals(identity,db.queryForList("SELECT selno,seller_member_id,oprice,status FROM sales ORDER BY selno"));
        assertEquals(24,db.queryForObject("SELECT COUNT(*) FROM sales WHERE photo1 LIKE '/preview/images/products/sellers/%'",Integer.class));
        assertEquals(24,db.queryForObject("SELECT COUNT(DISTINCT name) FROM sales",Integer.class));
        assertEquals(24,db.queryForObject("SELECT COUNT(DISTINCT photo1) FROM sales",Integer.class));
        seed.seed();assertEquals(24,db.queryForObject("SELECT COUNT(*) FROM sales",Integer.class));
        assertEquals(12,db.queryForObject("SELECT COUNT(*) FROM member WHERE role='STUDENT'",Integer.class));
    }
}
