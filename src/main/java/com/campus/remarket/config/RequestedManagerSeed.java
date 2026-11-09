package com.campus.remarket.config;

import demo.campus.campusremarketmember.member.entity.Member;
import demo.campus.campusremarketmember.member.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Add only requested demo managers to an existing owner with an assigned shop. */
@Component
public class RequestedManagerSeed {
    private final MemberRepository members;private final JdbcTemplate db;private final PasswordEncoder passwords;private final boolean enabled;
    public RequestedManagerSeed(MemberRepository members,JdbcTemplate db,PasswordEncoder passwords,@Value("${campus.demo-campus.enabled:false}")boolean enabled){this.members=members;this.db=db;this.passwords=passwords;this.enabled=enabled;}
    @EventListener(ApplicationReadyEvent.class) @Order(200) @Transactional
    public void ready(){if(!enabled)return;seed();}
    @Transactional public void seed(){
        String[][] pairs={{"owner_italy","manager_italy","이태리부대찌개 매니저"},{"owner_gosu","manager_gosu","고수찜닭 매니저"},{"owner_mala","manager_mala","양궈푸마라탕 매니저"},{"mengi","manager_mengi","멘지 매니저"}};
        for(var pair:pairs){
            var owner=members.findByAccountKey("staff:"+pair[0]).orElse(null);
            if(owner==null||!owner.isOwner()||db.queryForObject("SELECT COUNT(*) FROM shops WHERE owner_member_id=?",Integer.class,owner.getId())!=1)continue;
            var existing=members.findByAccountKey("staff:"+pair[1]).orElse(null);
            if(existing!=null){if(!existing.isManager()||!owner.getId().equals(existing.getOwnerId()))throw new IllegalStateException("요청 매니저 아이디가 다른 회원과 겹칩니다.");continue;}
            if(members.countByOwnerId(owner.getId())>=3)throw new IllegalStateException("매니저 정원이 가득 찼습니다.");
            members.saveAndFlush(Member.manager(pair[1],passwords.encode("campus1234!"),pair[2],owner.getId()));
        }
    }
}
