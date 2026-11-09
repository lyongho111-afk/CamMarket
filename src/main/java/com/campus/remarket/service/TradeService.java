package com.campus.remarket.service;
import com.campus.remarket.dao.TradeDao;
import com.campus.remarket.dao.SalesDao;
import com.campus.remarket.dto.TradeDto;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
@Service
public class TradeService {
    private final TradeDao dao;
    private final SalesDao sales;
    private final CampusScope scope;
    private final demo.campus.campusremarketmember.member.repository.MemberRepository members;
    private final com.campus.remarket.notification.NotificationService notifications;
    public TradeService(TradeDao dao,SalesDao sales,com.campus.remarket.notification.NotificationService notifications,CampusScope scope,demo.campus.campusremarketmember.member.repository.MemberRepository members){this.members=members;this.scope=scope;this.dao=dao;this.sales=sales;this.notifications=notifications;}
    public List<TradeDto> selectAll(){return dao.selectAll().stream().filter(t->scope.visible(t.getSchoolId())).toList();}
    public TradeDto selectOne(String id){var dto=dao.selectOne(id);if(dto==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"거래가 없습니다.");scope.require(dto.getSchoolId());return dto;}
    @Transactional public void insert(TradeDto dto){
        if(dto.getBuyer()==null||dto.getBuyer().isBlank()||dto.getBuyer().length()>50)throw new IllegalArgumentException("구매자 ID를 입력하세요.");
        if(dto.getSelno()==null||sales.lockSale(dto.getSelno())==null)throw new IllegalArgumentException("상품을 선택하세요.");
        var sale=sales.selectOne(dto.getSelno());scope.require(sale.getSchoolId());
        if(sale.getSellerMemberId()!=null&&members.findById(sale.getSellerMemberId()).map(m->m.isWithdrawn()).orElse(false))throw new ResponseStatusException(HttpStatus.CONFLICT,"탈퇴한 판매자의 상품은 예약할 수 없습니다.");
        if(!"판매중".equals(sale.getStatus()))throw new IllegalStateException("이미 예약되었거나 완료된 상품입니다.");
        dto.setOrderId("campus_"+UUID.randomUUID().toString().replace("-",""));
        // 폼에서 보낸 금액을 사용하지 않고 DB의 현재 할인가를 약속 가격으로 저장합니다.
        dto.setAgreedPrice(sale.getCurrentPrice());
        if(dto.getMeetingPlace()!=null&&dto.getMeetingPlace().length()>150)throw new IllegalArgumentException("거래 장소는 150자 이내로 입력하세요.");
        if((dto.getLatitude()==null)!=(dto.getLongitude()==null)||dto.getLatitude()!=null&&(dto.getLatitude().abs().doubleValue()>90||dto.getLongitude().abs().doubleValue()>180))throw new IllegalArgumentException("거래 좌표를 확인하세요.");
        dao.insert(dto);sales.statusUpdate(dto.getSelno(),"예약중");
        notifications.reservation(dto,sale.getSeller(),sale.getName());
    }
    @Transactional public void cancel(String id){
        var order=dao.lockOne(id);if(order==null)throw new IllegalArgumentException("거래가 없습니다.");
        sales.lockSale(order.getSelno());
        if(dao.cancel(id)!=1)throw new IllegalStateException("결제 전 예약 상태만 취소할 수 있습니다.");
        sales.statusUpdate(order.getSelno(),"판매중");
    }

    // 대면 거래 완료 처리이며 결제 승인·송금으로 간주하지 않습니다.
    @Transactional public void complete(String id){
        var order=dao.lockOne(id);if(order==null)throw new IllegalArgumentException("거래가 없습니다.");
        sales.lockSale(order.getSelno());
        if(dao.complete(id)!=1)throw new IllegalStateException("예약 또는 결제 완료 상태에서 거래를 완료할 수 있습니다.");
        sales.statusUpdate(order.getSelno(),"거래완료");
    }
    @Transactional public void meeting(TradeDto dto){
        var order=dao.lockOne(dto.getOrderId());if(order==null)throw new IllegalArgumentException("거래가 없습니다.");
        if(dto.getMeetingPlace()==null||dto.getMeetingPlace().isBlank()||dto.getMeetingPlace().length()>150)throw new IllegalArgumentException("150자 이내의 거래 장소를 선택하세요.");
        if((dto.getLatitude()==null)!=(dto.getLongitude()==null))throw new IllegalArgumentException("위도와 경도는 함께 선택하세요.");
        if(dto.getLatitude()!=null&&(dto.getLatitude().doubleValue() < -90||dto.getLatitude().doubleValue()>90)||dto.getLongitude()!=null&&(dto.getLongitude().doubleValue() < -180||dto.getLongitude().doubleValue()>180))throw new IllegalArgumentException("지도 좌표를 확인하세요.");
        if(dao.meeting(dto)!=1)throw new IllegalStateException("결제 전 예약의 장소만 변경할 수 있습니다.");
    }
}
