package com.campus.remarket.config;

import com.campus.remarket.dao.ShopDao;
import com.campus.remarket.dto.ShopDto;
import demo.campus.campusremarketmember.member.entity.Member;
import demo.campus.campusremarketmember.member.repository.*;
import java.math.BigDecimal;
import java.time.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Requested public places, linked to clearly named demonstration owner accounts. */
@Component
public class CampusShopSeed {
    private final ShopDao shops;
    private final MemberRepository members;
    private final SchoolRepository schools;
    private final PasswordEncoder passwords;
    private final boolean enabled;
    private final org.springframework.jdbc.core.JdbcTemplate db;
    public CampusShopSeed(ShopDao shops, MemberRepository members, SchoolRepository schools, PasswordEncoder passwords, org.springframework.jdbc.core.JdbcTemplate db,
            @Value("${campus.shops.seed-selected:true}") boolean enabled) {
        this.db=db;this.shops=shops;this.members=members;this.schools=schools;this.passwords=passwords;this.enabled=enabled;
    }
    @EventListener(ApplicationReadyEvent.class)
    @org.springframework.core.annotation.Order(100)
    @Transactional
    public void ready() {
        if(!enabled)return;
        add("owner_italy","이태리부대찌개 인하대역점","인천 미추홀구 소성로 16 인하아리스타 1층 121호","032-866-5525",
                "37.44748972","126.65154389","매일 11:00~21:00 · 라스트오더 20:30","없음","부대찌개 · 부대전골",11,21,0,false);
        var now=ZonedDateTime.now(ZoneId.of("Asia/Seoul"));boolean weekend=now.getDayOfWeek().getValue()>=6;
        add("owner_gosu","고수찜닭 본점","인천 미추홀구 경인남길30번길 34 1층","032-867-9982",
                "37.4518537","126.65629295","월~금 11:00~21:50 / 토·일 11:00~21:30 · 주문마감 평일 21:00, 주말 20:40","없음","찜닭",11,21,weekend?30:50,false);
        add("owner_mala","양궈푸마라탕 인하대점","인천 미추홀구 경인남길30번길 70 1층","0507-1493-9026",
                "37.4513726","126.65817121","일~금 12:00~22:00 · 라스트오더 21:20","매주 토요일","마라탕 · 마라샹궈",12,22,0,now.getDayOfWeek()==DayOfWeek.SATURDAY);
    }
    private void add(String login,String name,String address,String phone,String lat,String lon,String hours,String offday,String menu,
                     int opens,int closes,int minutes,boolean holiday) {
        // Preserve edits, approvals, ownership and state on subsequent starts.
        if(shops.selectAll().stream().anyMatch(s->name.equals(s.getShopname())&&address.equals(s.getAddress())))return;
        if(members.findByAccountKey("staff:"+login).isPresent())return;
        var owner=Member.owner(login,passwords.encode("campus1234!"),"매장관리 시연",login+"@campus.local","", "DEMO-"+login.substring(6).toUpperCase(java.util.Locale.ROOT));
        schools.findByName("인하대학교").ifPresent(owner::chooseSchool);
        owner=members.saveAndFlush(owner);
        var s=new ShopDto();s.setOwnerMemberId(owner.getId());s.setSchoolId(owner.getSchool().getId());s.setShopname(name);s.setAddress(address);s.setShopphone(phone);
        s.setLatitude(new BigDecimal(lat));s.setLongitude(new BigDecimal(lon));s.setBusiKind("음식점");s.setBmenu(menu);
        s.setBusitime(hours);s.setOffday(offday);s.setCeoname("매장관리 시연");s.setBusino("DEMO-"+login.substring(6).toUpperCase(java.util.Locale.ROOT));
        var time=LocalTime.now(ZoneId.of("Asia/Seoul"));
        s.setStatus(holiday?"휴무":!time.isBefore(LocalTime.of(opens,0))&&time.isBefore(LocalTime.of(closes,minutes))?"영업중":"영업종료");
        if(db.queryForObject("SELECT COUNT(*) FROM business_categories WHERE name=?",Integer.class,s.getBusiKind())==0)db.update("INSERT INTO business_categories(name) VALUES(?)",s.getBusiKind());
        shops.insert(s);
        // Approval is only for this explicitly requested reference catalog.
        db.update("UPDATE shops SET checked=TRUE WHERE shopId=?",s.getShopId());
    }
}
