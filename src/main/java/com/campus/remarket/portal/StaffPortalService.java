package com.campus.remarket.portal;

import com.campus.remarket.dto.ShopDto;
import com.campus.remarket.dto.ShopChangeDto;
import com.campus.remarket.service.ShopService;
import com.campus.remarket.service.SalesService;
import com.campus.remarket.service.TradeService;
import demo.campus.campusremarketmember.member.dto.MemberView;
import demo.campus.campusremarketmember.member.entity.Member;
import demo.campus.campusremarketmember.member.repository.MemberRepository;
import java.time.LocalDate;
import java.util.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.campus.remarket.portal.StaffAccess.error;

@Service
public class StaffPortalService {
    public record ShopInput(String shopname,String shopphone,String busiKind,String bmenu,String reservtime,String busitime,String offday,String specialevent,String sizeandtype,String status,String availablespace,Long version,String address,java.math.BigDecimal latitude,java.math.BigDecimal longitude){}
    public record ShopView(Long id,Long ownerId,String ownerName,String businessNo,String name,String phone,String category,String menu,String reservation,String hours,String offday,String event,String spaceType,String status,String space,boolean approved,Long version,String address,java.math.BigDecimal latitude,java.math.BigDecimal longitude,String photo){}
    public record TradeView(String id,Long saleId,String product,String buyer,String seller,Integer amount,String status,String meetingPlace,Object meetingTime,Object createdAt){}
    private final StaffAccess access;
    private final PortalShopDao dao;
    private final ShopService shops;
    private final com.campus.remarket.service.BusinessCategoryService categories;private final com.campus.remarket.service.CampusScope scope;
    private final MemberRepository members;
    private final SalesService sales;
    private final TradeService trades;
    private final ShopChangeDao changes;
    private final AdminActionDao actions;
    public StaffPortalService(StaffAccess access,PortalShopDao dao,ShopService shops,MemberRepository members,SalesService sales,TradeService trades,ShopChangeDao changes,AdminActionDao actions,com.campus.remarket.service.BusinessCategoryService categories,com.campus.remarket.service.CampusScope scope){this.categories=categories;this.scope=scope;this.access=access;this.dao=dao;this.shops=shops;this.members=members;this.sales=sales;this.trades=trades;this.changes=changes;this.actions=actions;}
    public ShopView view(ShopDto shop){return shop==null?null:new ShopView(shop.getShopId(),shop.getOwnerMemberId(),shop.getCeoname(),shop.getBusino(),shop.getShopname(),shop.getShopphone(),shop.getBusiKind(),shop.getBmenu(),shop.getReservtime(),shop.getBusitime(),shop.getOffday(),shop.getSpecialevent(),shop.getSizeandtype(),shop.getStatus(),shop.getAvailablespace(),Boolean.TRUE.equals(shop.getChecked()),shop.getVersion(),shop.getAddress(),shop.getLatitude(),shop.getLongitude(),shop.getPhoto());}
    public Map<String,Object> ownerHome(){
        var me=access.staff();var result=new LinkedHashMap<String,Object>();
        var shop=dao.forOwner(access.ownerId());
        result.put("me",MemberView.of(me));result.put("shop",view(shop));result.put("csrfToken",access.csrf());
        var publication=shop==null?null:actions.latestPublication(shop.getShopId());
        result.put("publication",publication==null?null:Map.of("status",publication.getAfterValue(),"note",publication.getNote(),"changedAt",publication.getCreatedAt()));
        result.put("managers",me.isOwner()?members.findByOwnerIdOrderByIdAsc(me.getId()).stream().map(MemberView::of).toList():List.of());
        return result;
    }
    private String text(String input,String label,int max,boolean required){String value=input==null?"":input.trim();if(required&&value.isBlank()||value.length()>max)throw error(HttpStatus.BAD_REQUEST,label+" 입력을 확인해 주세요. (최대 "+max+"자)");return value;}
    private ShopDto fill(ShopDto shop,ShopInput input,boolean basic){
        if(basic){categories.require(input.busiKind());shop.setShopname(text(input.shopname(),"가게명",50,true));shop.setBusiKind(text(input.busiKind(),"업종",50,true));shop.setSizeandtype(text(input.sizeandtype(),"공간 안내",200,false));
            shop.setAddress(text(input.address(),"주소",200,false));
            if((input.latitude()==null)!=(input.longitude()==null)||input.latitude()!=null&&(input.latitude().abs().doubleValue()>90||input.longitude().abs().doubleValue()>180))throw error(HttpStatus.BAD_REQUEST,"위도·경도를 함께 올바르게 입력해 주세요.");
            shop.setLatitude(input.latitude());shop.setLongitude(input.longitude());}
        shop.setShopphone(text(input.shopphone(),"가게 전화번호",50,false));shop.setBmenu(text(input.bmenu(),"메뉴",400,false));shop.setReservtime(text(input.reservtime(),"예약 안내",100,false));
        shop.setBusitime(text(input.busitime(),"영업시간",100,true));shop.setOffday(text(input.offday(),"휴무일",20,false));shop.setSpecialevent(text(input.specialevent(),"이벤트",50,false));shop.setAvailablespace(text(input.availablespace(),"여유 공간",50,false));
        if(!List.of("영업중","혼잡","영업종료","휴무").contains(input.status()==null?"":input.status()))throw error(HttpStatus.BAD_REQUEST,"영업 상태를 선택해 주세요.");shop.setStatus(input.status());return shop;
    }
    private ShopDto requireShop(Long ownerId){var shop=dao.forOwner(ownerId);if(shop==null)throw error(HttpStatus.NOT_FOUND,"등록된 가게가 없습니다. 사장님 계정에서 먼저 등록해 주세요.");return shop;}
    @Transactional public ShopView create(ShopInput input,String csrf){
        var me=access.owner();access.verify(csrf);if(dao.forOwner(me.getId())!=null)throw error(HttpStatus.CONFLICT,"이미 등록한 가게가 있습니다.");
        var shop=fill(new ShopDto(),input,true);shop.setSchoolId(scope.id());shop.setOwnerMemberId(me.getId());shop.setCeoname(me.getName());shop.setBusino(me.getBusinessNo());
        try{dao.create(shop);}catch(DuplicateKeyException e){throw error(HttpStatus.CONFLICT,"이미 등록한 가게가 있습니다.");}return view(shops.selectOne(shop.getShopId()));
    }
    @Transactional public ShopView update(ShopInput input,String csrf,boolean basic){
        var actor=basic?access.owner():access.staff();access.verify(csrf);var ownerId=access.ownerId();var found=requireShop(ownerId);var shop=dao.lock(found.getShopId());
        if(!ownerId.equals(shop.getOwnerMemberId()))throw error(HttpStatus.CONFLICT,"가게 배정이 변경되었습니다. 새로고침해 주세요.");
        verifyVersion(shop,input.version());
        var before=historyFields(shop);
        fill(shop,input,basic);int changed=basic?dao.updateInfo(shop):dao.updateOperations(shop);if(changed!=1)throw error(HttpStatus.CONFLICT,"가게 정보가 변경되었습니다. 새로고침해 주세요.");
        recordChanges(shop,actor,before);
        return view(shops.selectOne(shop.getShopId()));
    }
    private void verifyVersion(ShopDto shop,Long version){
        // Legacy integrations may omit version; v13 screens always send the read version.
        if(version!=null&&!Objects.equals(version,shop.getVersion()))throw error(HttpStatus.CONFLICT,"다른 사용자가 가게 정보를 변경했습니다. 입력 내용은 유지됩니다. 최신 정보를 확인한 뒤 다시 작성해 주세요.");
    }
    private void recordChanges(ShopDto shop,Member actor,LinkedHashMap<String,String[]> before){
        var after=historyFields(shop);
        for(String key:before.keySet()){
            String previous=before.get(key)[1],next=after.get(key)[1];if(Objects.equals(previous,next))continue;
            var row=new ShopChangeDto();row.setShopId(shop.getShopId());row.setActorMemberId(actor.getId());row.setActorName(actor.getName());row.setActorRole(actor.getRole().name());
            row.setFieldKey(key);row.setFieldLabel(before.get(key)[0]);row.setBeforeValue(previous);row.setAfterValue(next);changes.insert(row);
        }
    }
    @Transactional public ShopView status(String status,Long version,String csrf){
        var actor=access.staff();access.verify(csrf);var ownerId=access.ownerId();var found=requireShop(ownerId);var shop=dao.lock(found.getShopId());
        if(!ownerId.equals(shop.getOwnerMemberId()))throw error(HttpStatus.CONFLICT,"가게 배정이 변경되었습니다. 새로고침해 주세요.");
        verifyVersion(shop,version);
        if(!List.of("영업중","혼잡","영업종료","휴무").contains(status==null?"":status))throw error(HttpStatus.BAD_REQUEST,"영업 상태를 선택해 주세요.");
        if(!Objects.equals(shop.getStatus(),status)){var before=historyFields(shop);shop.setStatus(status);dao.updateStatus(shop);recordChanges(shop,actor,before);}
        return view(shops.selectOne(shop.getShopId()));
    }
    private LinkedHashMap<String,String[]> historyFields(ShopDto s){
        var fields=new LinkedHashMap<String,String[]>();
        String[][] values={{"status","영업 상태",s.getStatus()},{"busitime","영업시간",s.getBusitime()},{"availablespace","여유 공간",s.getAvailablespace()},{"bmenu","메뉴",s.getBmenu()},{"offday","휴무일",s.getOffday()},{"specialevent","이벤트·학생 혜택",s.getSpecialevent()},{"shopphone","가게 전화번호",s.getShopphone()},{"reservtime","예약 안내",s.getReservtime()},{"shopname","가게명",s.getShopname()},{"busiKind","업종",s.getBusiKind()},{"sizeandtype","공간 소개",s.getSizeandtype()},{"address","주소",s.getAddress()},{"latitude","위도",Objects.toString(s.getLatitude(),"")},{"longitude","경도",Objects.toString(s.getLongitude(),"")}};
        for(var value:values)fields.put(value[0],new String[]{value[1],value[2]==null?"":value[2]});return fields;
    }
    public List<ShopChangeDto> history(){access.staff();return changes.forOwner(access.ownerId());}
    public Map<String,Object> adminHome(){
        var me=access.admin();var allMembers=members.findAllByOrderByIdDesc();var allShops=shops.selectAll();var allSales=sales.selectAll();var allTrades=trades.selectAll();var allRatings=ratingRows();
        var counts=new LinkedHashMap<String,Object>();counts.put("members",allMembers.size());counts.put("students",allMembers.stream().filter(Member::isStudent).count());counts.put("shops",allShops.size());counts.put("pendingShops",allShops.stream().filter(s->!Boolean.TRUE.equals(s.getChecked())).count());counts.put("sales",allSales.size());counts.put("trades",allTrades.size());counts.put("completed",allTrades.stream().filter(t->t.isCompleted()).count());counts.put("ratings",allRatings.size());
        var days=new ArrayList<Map<String,Object>>();for(int i=6;i>=0;i--){var day=LocalDate.now().minusDays(i);days.add(Map.of("day",day.toString(),"count",allTrades.stream().filter(t->t.getCreatedAt()!=null&&t.getCreatedAt().toLocalDate().equals(day)).count()));}
        var result=new LinkedHashMap<String,Object>();result.put("me",MemberView.of(me));result.put("csrfToken",access.csrf());result.put("counts",counts);result.put("members",allMembers.stream().map(MemberView::of).toList());result.put("shops",allShops.stream().map(this::view).toList());
        result.put("trades",allTrades.stream().map(t->new TradeView(t.getOrderId(),t.getSelno(),t.getSaleName(),t.getBuyer(),t.getSeller(),t.getAgreedPrice(),t.getStatus(),t.getMeetingPlace(),t.getMeetingTime(),t.getCreatedAt())).toList());result.put("ratings",allRatings);
        result.put("ratingSummary",Map.of("total",allRatings.size(),"good",allRatings.stream().filter(r->"GOOD".equals(r.get("sentiment"))).count(),"bad",allRatings.stream().filter(r->"BAD".equals(r.get("sentiment"))).count(),"neutral",allRatings.stream().filter(r->"NEUTRAL".equals(r.get("sentiment"))).count(),"stars",allRatings.stream().filter(r->"SCORE".equals(r.get("feedback"))).count()));
        result.put("days",days);return result;
    }
    private List<Map<String,Object>> ratingRows(){
        var rows=new ArrayList<Map<String,Object>>();
        for(var r:dao.ratings()){
            var m=new LinkedHashMap<String,Object>();m.put("orderId",r.getOrderId());m.put("reviewerUnisno",r.getReviewerUnisno());m.put("recipientUnisno",r.getRecipientUnisno());m.put("feedback",r.getFeedback());m.put("delta",r.getDelta());m.put("createdAt",r.getCreatedAt());rows.add(m);
        }
        for(var r:dao.reviews()){
            var m=new LinkedHashMap<String,Object>();m.put("orderId",r.getOrderId());m.put("reviewerUnisno",r.getReviewerId());m.put("recipientUnisno",r.getTargetId());m.put("feedback","SCORE");m.put("score",r.getScore());m.put("attitude",r.getAttitude());m.put("content",r.getContent());m.put("delta",r.getTemperatureDelta());m.put("createdAt",r.getCreatedAt());rows.add(m);
        }
        // Classify by the submitted score, not by temperature change: a boundary can make delta zero.
        for(var row:rows){var score=(Integer)row.get("score");row.put("sentiment",score==null?row.get("feedback"):score>=4?"GOOD":score<=2?"BAD":"NEUTRAL");}
        rows.sort(Comparator.comparing((Map<String,Object> m)->String.valueOf(m.get("createdAt"))).reversed());return rows;
    }
    public List<com.campus.remarket.dto.AdminActionDto> adminHistory(){access.admin();return actions.recent();}
    private void recordAction(ShopDto shop,Member actor,String action,String before,String after,String note){
        var row=new com.campus.remarket.dto.AdminActionDto();row.setShopId(shop.getShopId());row.setShopName(shop.getShopname());row.setActorMemberId(actor.getId());row.setActorName(actor.getName());row.setAction(action);row.setBeforeValue(before);row.setAfterValue(after);row.setNote(note);actions.insert(row);
    }
    @Transactional public ShopView approval(Long id,Boolean approved,Long version,String note,String csrf){
        var actor=access.admin();access.verify(csrf);if(approved==null)throw error(HttpStatus.BAD_REQUEST,"공개 상태를 선택해 주세요.");
        var shop=dao.lock(id);if(shop==null)throw error(HttpStatus.NOT_FOUND,"가게가 없습니다.");verifyVersion(shop,version);String reason=text(note,"처리 메모",300,false);
        if(!Objects.equals(shop.getChecked(),approved)){dao.approval(id,approved);recordAction(shop,actor,"APPROVAL",Boolean.TRUE.equals(shop.getChecked())?"공개 중":"비공개",approved?"공개 중":"비공개",reason);}return view(shops.selectOne(id));
    }
    @Transactional public ShopView assign(Long id,Long ownerId,Long version,String note,String csrf){
        var actor=access.admin();access.verify(csrf);var shop=dao.lock(id);if(shop==null)throw error(HttpStatus.NOT_FOUND,"가게가 없습니다.");verifyVersion(shop,version);String reason=text(note,"처리 메모",300,false);
        Member owner=null;if(ownerId!=null){owner=members.findById(ownerId).orElseThrow(()->error(HttpStatus.NOT_FOUND,"사장 회원이 없습니다."));if(!owner.isOwner())throw error(HttpStatus.BAD_REQUEST,"사장님 계정만 배정할 수 있습니다.");if(shop.getSchoolId()!=null&&(owner.getSchool()==null||!shop.getSchoolId().equals(owner.getSchool().getId())))throw error(HttpStatus.BAD_REQUEST,"다른 학교의 사장에게 가게를 배정할 수 없습니다.");var assigned=dao.forOwner(ownerId);if(assigned!=null&&!assigned.getShopId().equals(id))throw error(HttpStatus.CONFLICT,"해당 사장님에게 이미 다른 가게가 배정되어 있습니다.");}
        if(!Objects.equals(shop.getOwnerMemberId(),ownerId)){
            try{dao.assign(id,ownerId,owner==null?null:owner.getName(),owner==null?null:owner.getBusinessNo());}catch(DuplicateKeyException e){throw error(HttpStatus.CONFLICT,"가게 배정이 변경되었습니다. 새로고침해 주세요.");}
            recordAction(shop,actor,"ASSIGNMENT",shop.getOwnerMemberId()==null?"미배정":shop.getCeoname()+" (#"+shop.getOwnerMemberId()+")",owner==null?"미배정":owner.getName()+" (#"+ownerId+")",reason);
        }return view(shops.selectOne(id));
    }
}
