package com.campus.remarket.config;

import demo.campus.campusremarketmember.member.entity.*;
import demo.campus.campusremarketmember.member.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

/** Give the existing synthetic sellers real member identities; preserve all transaction ownership. */
@Configuration
@ConditionalOnProperty(name="campus.demo-sellers.enabled",havingValue="true",matchIfMissing=true)
public class DemoSellerSeed {
    @Bean @Order(10)
    CommandLineRunner linkDemoSellers(MemberRepository members,SchoolRepository schools,PasswordEncoder encoder,JdbcTemplate db,TransactionTemplate transaction,@org.springframework.beans.factory.annotation.Value("${campus.demo-sellers.split-example-listings:false}") boolean splitExamples){
        return args->transaction.executeWithoutResult(status->{
            var school=schools.findByCampusCode("INHA").or(()->schools.findByName("인하대학교")).orElseThrow();
            String[][] accounts={{"demo-seller","시연판매자 A"},{"demo-seller-2","시연판매자 B"},{"demo-seller-3","시연판매자 C"}};
            for(var account:accounts){
                var existing=db.queryForList("SELECT member_id FROM student WHERE unisno=?",account[0]);
                if(!existing.isEmpty()&&existing.getFirst().get("MEMBER_ID")!=null)continue;
                var member=members.findByAccountKey(Member.accountKey(school.getId(),account[0])).orElseGet(()->members.saveAndFlush(Member.student(account[0],encoder.encode("campus1234!"),account[1],account[0]+"@campus.local",null,StudentStatus.ENROLLED,school)));
                if(existing.isEmpty())db.update("INSERT INTO student(unisno,member_id,temp) VALUES(?,?,365)",account[0],member.getId());
                else db.update("UPDATE student SET member_id=? WHERE unisno=? AND member_id IS NULL",member.getId(),account[0]);
            }
            db.update("UPDATE sales SET seller_member_id=(SELECT st.member_id FROM student st WHERE st.unisno=sales.seller) WHERE seller_member_id IS NULL AND EXISTS(SELECT 1 FROM student st WHERE st.unisno=sales.seller AND st.member_id IS NOT NULL)");
            if(splitExamples){
                String[][] examples={{"3","한식조리 실습용 조리복 · L","demo-seller-2"},{"4","건축설계 기초 전공책 2권","demo-seller-2"},{"7","조리 실습복 세트 · M 사이즈","demo-seller-2"},{"5","LG 그램 14인치 · 충전기 포함","demo-seller-3"},{"6","실내건축산업기사 제도도구 세트","demo-seller-3"},{"8","자료구조 & 알고리즘 전공서적","demo-seller-3"}};
                for(var example:examples)db.update("UPDATE sales SET seller=?,seller_member_id=(SELECT member_id FROM student WHERE unisno=?) WHERE selno=? AND name=? AND seller='demo-seller' AND seller_member_id=(SELECT member_id FROM student WHERE unisno='demo-seller') AND NOT EXISTS(SELECT 1 FROM trade_orders o WHERE o.selno=sales.selno)",example[2],example[2],Long.valueOf(example[0]),example[1]);
            }
        });
    }
}
