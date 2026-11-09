package com.campus.remarket.config;

import com.campus.remarket.dao.ShopDao;
import com.campus.remarket.dto.ShopDto;
import demo.campus.campusremarketmember.member.entity.Member;
import demo.campus.campusremarketmember.member.repository.*;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** User-requested INU/Chungwoon catalog and separate demo owners. Never resets existing data. */
@Component
public class AdditionalCampusShopsSeed {
    private static final String MARKER="additional-campus-shops-20261106-v1";
    private final ShopDao shops;private final MemberRepository members;private final SchoolRepository schools;
    private final JdbcTemplate db;private final PasswordEncoder passwords;private final boolean enabled;
    public AdditionalCampusShopsSeed(ShopDao shops,MemberRepository members,SchoolRepository schools,JdbcTemplate db,PasswordEncoder passwords,
        @Value("${campus.shops.seed-campus-20261106:${campus.demo-campus.enabled:false}}")boolean enabled){
        this.shops=shops;this.members=members;this.schools=schools;this.db=db;this.passwords=passwords;this.enabled=enabled;
    }
    @EventListener(ApplicationReadyEvent.class) @Order(250) @Transactional
    public void ready(){if(enabled)seed();}

    @Transactional public void seed(){
        if(db.queryForObject("SELECT COUNT(*) FROM catalog_meta WHERE name=?",Integer.class,MARKER)>0)return;
        final JsonNode rows;
        try(var in=new ClassPathResource("campus-shops-20261106.json").getInputStream()){rows=JsonMapper.builder().build().readTree(in);}
        catch(Exception e){throw new IllegalStateException("추가 가게 목록을 읽을 수 없습니다.",e);}
        for(var row:rows)add(row);
        db.update("INSERT INTO catalog_meta(name) VALUES(?)",MARKER);
    }
    private void add(JsonNode row){
        String code=text(row,"school"),login=text(row,"loginId"),name=text(row,"name");
        var school=schools.findByCampusCode(code).orElseThrow(()->new IllegalStateException("서비스 학교를 찾을 수 없습니다: "+code));
        String address=text(row,"address")+" "+text(row,"addressDetail");
        var matches=shops.selectAll().stream().filter(s->name.equals(s.getShopname())&&address.equals(s.getAddress())).toList();
        var owner=members.findByAccountKey("staff:"+login).orElse(null);
        if(!matches.isEmpty()){
            if(matches.size()!=1||owner==null||!school.getId().equals(matches.getFirst().getSchoolId())||!owner.getId().equals(matches.getFirst().getOwnerMemberId()))
                throw new IllegalStateException("기존 가게의 소유권·학교를 임의로 변경하지 않습니다: "+name);
            return;
        }
        if(owner!=null)throw new IllegalStateException("추가 사장 아이디가 기존 회원과 겹칩니다: "+login);
        String kind=text(row,"category");
        if(db.queryForObject("SELECT COUNT(*) FROM business_categories WHERE name=?",Integer.class,kind)!=1)
            throw new IllegalStateException("기존 업종을 확인해 주세요: "+kind);
        String label=code.equals("INU")?"인천대":"청운대";
        // Synthetic identifiers and non-deliverable email; these are not the real business owners' accounts.
        String businessNo="DEMO26-"+(code.equals("INU")?"I":"C")+login.substring(login.length()-2);
        owner=Member.owner(login,passwords.encode("campus1234!"),label+" 시연사장 "+login.substring(login.length()-2),login+"@example.invalid",null,businessNo);
        owner.chooseSchool(school);owner=members.saveAndFlush(owner);
        BigDecimal latitude=new BigDecimal(text(row,"latitude")),longitude=new BigDecimal(text(row,"longitude"));
        com.campus.remarket.service.AddressLocator.validate(latitude,longitude);
        if(latitude.doubleValue()<37||latitude.doubleValue()>38||longitude.doubleValue()<126||longitude.doubleValue()>127)
            throw new IllegalStateException("인천 지역 좌표를 확인해 주세요: "+name);
        var s=new ShopDto();s.setSchoolId(school.getId());s.setOwnerMemberId(owner.getId());s.setShopname(name);
        s.setShopphone(text(row,"phone"));s.setAddress(address);s.setLatitude(latitude);s.setLongitude(longitude);s.setPhoto(null);
        s.setBusiKind(kind);s.setBmenu(text(row,"menu"));s.setBusitime(text(row,"hours"));s.setOffday(text(row,"offday"));
        s.setSpecialevent(row.path("note").asText(""));s.setCeoname(owner.getName());s.setBusino(businessNo);
        // Initial demo state only. Existing owner-center controls remain authoritative after registration.
        s.setStatus("영업종료");shops.insert(s);
        // Public reference catalog requested by the user; ordinary owner signups still need approval.
        db.update("UPDATE shops SET checked=TRUE WHERE shopId=?",s.getShopId());
        db.update("INSERT INTO notification_settings(member_id,email,phone,sms_enabled,email_enabled,kakao_enabled) VALUES(?,?,?,FALSE,FALSE,FALSE)",owner.getId(),owner.getEmail(),"");
    }
    private static String text(JsonNode row,String key){return row.path(key).asText("");}
}
