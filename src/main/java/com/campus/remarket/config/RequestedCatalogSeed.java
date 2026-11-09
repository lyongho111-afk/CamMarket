package com.campus.remarket.config;

import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

/** One-time update of the explicitly requested 24 demonstration listings, preserving IDs and trades. */
@Component @Order(60)
public class RequestedCatalogSeed implements CommandLineRunner {
    private static final String MARKER="demo-seller-photos-20261105-v1";
    private final JdbcTemplate db;private final TransactionTemplate transaction;private final boolean enabled;
    public RequestedCatalogSeed(JdbcTemplate db,TransactionTemplate transaction,@Value("${campus.demo-campus.enabled:false}")boolean enabled){this.db=db;this.transaction=transaction;this.enabled=enabled;}
    public void run(String...args){if(enabled)seed();}
    public void seed(){
        final tools.jackson.databind.JsonNode rows;
        try(var in=new ClassPathResource("campus-demo-products-20261105.json").getInputStream()){rows=JsonMapper.builder().build().readTree(in);}
        catch(Exception e){throw new IllegalStateException("상품 사진 목록을 읽을 수 없습니다.",e);}
        transaction.executeWithoutResult(status->{
            if(db.queryForObject("SELECT COUNT(*) FROM catalog_meta WHERE name=?",Integer.class,MARKER)>0)return;
            for(var row:rows){
                var matches=db.queryForList("SELECT s.selno FROM sales s JOIN member m ON m.id=s.seller_member_id JOIN school c ON c.id=s.school_id WHERE c.campus_code=? AND m.login_id=? AND m.role='STUDENT' AND m.school_id=c.id AND s.name=?",Long.class,row.get("school").asText(),row.get("seller").asText(),row.get("oldName").asText());
                if(matches.size()!=1)throw new IllegalStateException("시연 상품의 학교·판매자·상품명을 확인하세요: "+row.get("oldName").asText());
                db.update("UPDATE sales SET name=?,category=?,detail=?,photo1=?,photo2=NULL,photo3=NULL WHERE selno=?",row.get("name").asText(),row.get("category").asText(),row.get("name").asText()+". 학교별 시연 상품이며 사진은 AI로 제작한 예시입니다.",row.get("photo").asText(),matches.getFirst());
            }
            db.update("INSERT INTO catalog_meta(name) VALUES(?)",MARKER);
        });
    }
}
