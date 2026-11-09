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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Reproduce the two requested existing reference stores in a fresh demo, without replacing live rows. */
@Component
public class RequestedStoreSeed {
    private final ShopDao shops;private final MemberRepository members;private final SchoolRepository schools;private final JdbcTemplate db;private final PasswordEncoder passwords;private final boolean enabled;
    public RequestedStoreSeed(ShopDao shops,MemberRepository members,SchoolRepository schools,JdbcTemplate db,PasswordEncoder passwords,@Value("${campus.demo-campus.enabled:false}")boolean enabled){this.shops=shops;this.members=members;this.schools=schools;this.db=db;this.passwords=passwords;this.enabled=enabled;}
    @EventListener(ApplicationReadyEvent.class) @Order(150) @Transactional
    public void ready(){if(!enabled)return;
        add("owner1","포케품은초밥 인하대점","인천 미추홀구 인하로77번길 6-29 1층","37.4512722","126.6580021","맛집","라이언킹 롤",null);
        add("mengi","멘지","인천 미추홀구 인하로77번길 6-19 1층 102호","37.4516266","126.6575682","음식점","라멘 8000원","26ee48ce-7c77-4e05-ad47-9bb4a2a6cead.png");
    }
    private void add(String login,String name,String address,String lat,String lon,String kind,String menu,String photo){
        if(shops.selectAll().stream().anyMatch(s->name.equals(s.getShopname())&&address.equals(s.getAddress())))return;
        var school=schools.findByCampusCode("INHA").orElseThrow();
        var owner=members.findByAccountKey("staff:"+login).orElse(null);
        if(owner==null){owner=Member.owner(login,passwords.encode("campus1234!"),"멘지 시연사장",login+"@campus.local","","DEMO-MENGI");owner.chooseSchool(school);owner=members.saveAndFlush(owner);}
        if(!owner.isOwner()||db.queryForObject("SELECT COUNT(*) FROM shops WHERE owner_member_id=?",Integer.class,owner.getId())>0)return;
        if(owner.getSchool()==null){owner.chooseSchool(school);members.saveAndFlush(owner);}
        var s=new ShopDto();s.setOwnerMemberId(owner.getId());s.setSchoolId(school.getId());s.setShopname(name);s.setAddress(address);s.setLatitude(new BigDecimal(lat));s.setLongitude(new BigDecimal(lon));s.setBusiKind(kind);s.setBmenu(menu);s.setBusitime("09:00~21:00");s.setStatus("영업중");s.setPhoto(photo);s.setCeoname(owner.getName());s.setBusino(owner.getBusinessNo());shops.insert(s);db.update("UPDATE shops SET checked=TRUE WHERE shopId=?",s.getShopId());
    }
}
