package com.campus.remarket.portal;
import demo.campus.campusremarketmember.member.repository.MemberRepository;
import com.campus.remarket.dao.ShopDao;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
/** 기존 공개 예시 가게 한 곳을 시연 사장에게 명시적으로 배정한다. 실제 신규 가게는 관리자 승인이 필요하다. */
@Component
public class PortalDemoSeed {
    private final MemberRepository members;private final ShopDao shops;private final PortalShopDao portal;private final boolean enabled;
    public PortalDemoSeed(MemberRepository members,ShopDao shops,PortalShopDao portal,@Value("${campus.portal.seed-demo-shop:true}")boolean enabled){this.members=members;this.shops=shops;this.portal=portal;this.enabled=enabled;}
    @EventListener(ApplicationReadyEvent.class) @Transactional public void ready(){if(!enabled)return;var owner=members.findByAccountKey("staff:owner1").orElse(null);if(owner==null||portal.forOwner(owner.getId())!=null)return;shops.selectAll().stream().filter(s->s.getOwnerMemberId()==null&&"캠퍼스 카페 (예시)".equals(s.getShopname())).findFirst().ifPresent(s->portal.assign(s.getShopId(),owner.getId(),owner.getName(),owner.getBusinessNo()));}
}
