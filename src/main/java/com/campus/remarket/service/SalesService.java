package com.campus.remarket.service;
import com.campus.remarket.dao.SalesDao;
import com.campus.remarket.dao.StudentDao;
import com.campus.remarket.dto.SalesDto;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
@Service
public class SalesService {
    private final SalesDao dao;
    private final CampusScope scope;
    private final StudentDao students;
    public SalesService(SalesDao dao,StudentDao students,CampusScope scope){this.dao=dao;this.students=students;this.scope=scope;}
    public List<SalesDto> selectAll(){boolean guest=scope.member()==null;return dao.selectAll().stream().filter(x->guest||scope.visible(x.getSchoolId())).toList();}
    public SalesDto selectOne(Long id){var dto=dao.selectOne(id);if(dto==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"판매글을 찾을 수 없습니다.");if(scope.member()!=null)scope.require(dto.getSchoolId());return dto;}
    private void validate(SalesDto dto){requireText(dto.getSeller(),"판매자 ID");requireText(dto.getCategory(),"카테고리");requireText(dto.getName(),"상품명");if(dto.getOprice()==null||dto.getOprice()<0||dto.getOprice()>99999999)throw new IllegalArgumentException("표시가는 0~99,999,999원으로 입력하세요.");if(dto.getBuyprice()!=null&&dto.getBuyprice()<0||dto.getWishprice()!=null&&dto.getWishprice()<0)throw new IllegalArgumentException("구입가·희망 매가는 음수일 수 없습니다.");if(dto.getLocation()==null||dto.getLocation().isBlank())dto.setLocation(scope.school().getName());dto.setIcon("book");}
    private void requireText(String value,String label){if(value==null||value.isBlank())throw new IllegalArgumentException(label+"을 입력하세요.");}
    @Transactional public void insert(SalesDto dto){dto.setSchoolId(scope.id());validate(dto);var student=students.selectOne(dto.getSeller());if(dto.getSellerMemberId()!=null&&(student==null||!dto.getSellerMemberId().equals(student.getMemberId())))throw new IllegalArgumentException("판매자와 회원 연결이 일치하지 않습니다.");if(student!=null)dto.setSellerMemberId(student.getMemberId());dao.insert(dto);}
    @Transactional public void update(SalesDto dto){validate(dto);dao.lockSale(dto.getSelno());var existing=selectOne(dto.getSelno());if(!existing.getOprice().equals(dto.getOprice())&&(!"판매중".equals(existing.getStatus())||existing.getDiscount()>0&&existing.getEndtime()!=null&&existing.getEndtime().isAfter(java.time.LocalDateTime.now())))throw new IllegalStateException("예약·완료 또는 할인 중인 글의 가격은 변경할 수 없습니다.");dao.update(dto);}
    @Transactional public void delete(Long id){dao.lockSale(id);selectOne(id);if(dao.orderCount(id)>0)throw new IllegalStateException("거래 기록이 있는 글은 삭제할 수 없습니다.");dao.delete(id);}

    @Transactional public void startDiscount(Long id,int rate){
        SalesDto dto=selectOne(id);if(rate<1||rate>99)throw new IllegalArgumentException("할인율은 1~99% 정수입니다.");
        if((long)dto.getOprice()*(100-rate)/100<1)throw new IllegalArgumentException("할인 후 가격은 1원 이상이어야 합니다.");
        if(dao.discountStart(id,rate)!=1)throw new IllegalStateException("판매 중인 글만 할인할 수 있고, 진행 중인 할인은 연장할 수 없습니다.");
    }
    @Transactional public void endDiscount(Long id){selectOne(id);if(dao.discountEnd(id)!=1)throw new IllegalStateException("판매 중인 글만 할인 종료할 수 있습니다.");}
    @Transactional public void bump(Long id){selectOne(id);if(dao.bump(id)!=1)throw new IllegalStateException("판매 중이며 최근 24시간 내 끌어올리지 않은 글만 가능합니다.");}
}
