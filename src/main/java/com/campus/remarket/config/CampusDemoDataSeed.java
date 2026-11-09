package com.campus.remarket.config;

import com.campus.remarket.dao.StudentDao;
import com.campus.remarket.service.StudentMemberLink;
import demo.campus.campusremarketmember.member.entity.*;
import demo.campus.campusremarketmember.member.repository.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/** Requested, one-time synthetic campus data. Never resets data on subsequent starts. */
@Component @Order(50)
public class CampusDemoDataSeed implements CommandLineRunner {
    private static final String MARKER="demo-campus-accounts-20261105-v1";
    private final JdbcTemplate db;private final MemberRepository members;private final SchoolRepository schools;
    private final StudentDao students;private final StudentMemberLink links;private final PasswordEncoder passwords;
    private final TransactionTemplate transaction;private final boolean enabled;
    public CampusDemoDataSeed(JdbcTemplate db,MemberRepository members,SchoolRepository schools,StudentDao students,
        StudentMemberLink links,PasswordEncoder passwords,TransactionTemplate transaction,
        @Value("${campus.demo-campus.enabled:false}")boolean enabled){
        this.db=db;this.members=members;this.schools=schools;this.students=students;this.links=links;
        this.passwords=passwords;this.transaction=transaction;this.enabled=enabled;
    }
    @Override public void run(String...args){if(enabled)seed();}
    public void seed(){transaction.executeWithoutResult(status->{
        if(db.queryForObject("SELECT COUNT(*) FROM catalog_meta WHERE name=?",Integer.class,MARKER)>0)return;
        var inha=schools.findByCampusCode("INHA").orElseThrow();
        var a=account(inha,"20261231","인하대 판매자 A","demo-seller");
        var b=account(inha,"20261232","인하대 판매자 B","demo-seller-2");
        var buyer=account(inha,"20261234","인하대 구매자 A",null);
        var buyerB=account(inha,"20261235","인하대 구매자 B","demo-seller-3");
        var originals=db.queryForList("SELECT * FROM sales WHERE school_id=? AND selno BETWEEN 1 AND 8 ORDER BY selno",inha.getId());
        if(originals.size()!=8)throw new IllegalStateException("기존 인하대 시연 상품 8개를 확인하세요.");
        Set<Long> demoOwners=Set.of(a.getId(),b.getId(),buyerB.getId());
        for(int i=0;i<8;i++){
            var row=originals.get(i);var owner=i<4?a:b;long id=((Number)row.get("SELNO")).longValue();
            long oldOwner=((Number)row.get("SELLER_MEMBER_ID")).longValue();
            if(!demoOwners.contains(oldOwner))throw new IllegalStateException("다른 회원의 상품은 변경하지 않습니다.");
            if(oldOwner!=owner.getId()&&db.queryForObject("SELECT COUNT(*) FROM trade_orders WHERE selno=?",Integer.class,id)>0)
                throw new IllegalStateException("거래가 연결된 상품의 판매자는 변경하지 않습니다.");
            db.update("UPDATE sales SET seller=?,seller_member_id=? WHERE selno=?",links.key(owner),owner.getId(),id);
        }
        // Link only the original unclaimed synthetic buyer to the existing demo buyer.
        String buyerKey=links.key(buyer);
        db.update("UPDATE trade_orders SET buyer=? WHERE buyer='demo-buyer'",buyerKey);
        for(String field:List.of("reviewer_unisno","recipient_unisno"))db.update("UPDATE manner_ratings SET "+field+"=? WHERE "+field+"='demo-buyer'",buyerKey);
        for(String field:List.of("reviewer_id","target_id"))db.update("UPDATE trade_reviews SET "+field+"=? WHERE "+field+"='demo-buyer'",buyerKey);
        addCampus("INU",new String[]{"20231231","20231232","20231234","20221235"},"인천대",originals,2000);
        addCampus("CHUNGWOON",new String[]{"20251231","20251232","20251235","20261235"},"청운대",originals,4000);
        db.update("UPDATE shops SET latitude=37.4512722,longitude=126.6580021,version=version+1 WHERE shopname=? AND address LIKE ? AND (latitude=-11 AND longitude=-1 OR latitude IS NULL OR longitude IS NULL)",
            "포케품은초밥 인하대점","%인하로77번길 6-29%");
        db.update("INSERT INTO catalog_meta(name) VALUES(?)",MARKER);
    });}
    private Member account(School school,String number,String name,String legacy){
        Member member=null;
        if(legacy!=null){var link=students.selectOne(legacy);if(link!=null&&link.getMemberId()!=null)member=members.findById(link.getMemberId()).orElseThrow();}
        var target=members.findByAccountKey(Member.accountKey(school.getId(),number));
        if(target.isPresent()){
            if(member!=null&&!member.getId().equals(target.get().getId()))throw new IllegalStateException("시연 학번이 다른 회원과 겹칩니다.");
            if(member==null&&!number.equals("20261234")&&!target.get().getName().equals(name))throw new IllegalStateException("기존 회원의 학번은 덮어쓰지 않습니다.");
            member=target.get();
        }
        if(member==null){
            member=members.saveAndFlush(Member.student(number,passwords.encode("campus1234!"),name,
                "demo-"+school.getCampusCode().toLowerCase(Locale.ROOT)+"-"+number+"@example.invalid",null,StudentStatus.ENROLLED,school));
            // Synthetic accounts have no real recipients. App notifications remain available.
            db.update("INSERT INTO notification_settings(member_id,email,phone,sms_enabled,email_enabled,kakao_enabled) VALUES(?,?,?,FALSE,FALSE,FALSE)",member.getId(),member.getEmail(),"");
        }
        if(!member.isStudent()||!member.getSchool().getId().equals(school.getId()))throw new IllegalStateException("회원 소속을 확인하세요.");
        member.renameDemoStudent(number,name);members.saveAndFlush(member);links.link(member);return member;
    }
    private void addCampus(String code,String[] numbers,String label,List<Map<String,Object>> templates,int adjustment){
        var school=schools.findByCampusCode(code).orElseThrow();
        if(db.queryForObject("SELECT COUNT(*) FROM sales WHERE school_id=?",Integer.class,school.getId())!=0)
            throw new IllegalStateException("기존 다른 학교 물품을 임의로 변경하지 않습니다.");
        var a=account(school,numbers[0],label+" 판매자 A",null);var b=account(school,numbers[1],label+" 판매자 B",null);
        account(school,numbers[2],label+" 구매자 A",null);account(school,numbers[3],label+" 구매자 B",null);
        for(int i=0;i<templates.size();i++){
            var t=templates.get(i);var owner=i<4?a:b;
            db.update("INSERT IGNORE INTO sales(school_id,seller,seller_member_id,category,name,oprice,detail,location,icon,photo1,photo2,photo3,status) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)",
                school.getId(),links.key(owner),owner.getId(),t.get("CATEGORY"),t.get("NAME"),((Number)t.get("OPRICE")).intValue()+adjustment,
                label+" 시연용 상품입니다. 실제 판매 글이 아닙니다.",school.getName()+" 정문",t.get("ICON"),t.get("PHOTO1"),t.get("PHOTO2"),t.get("PHOTO3"),"판매중");
        }
    }
}
