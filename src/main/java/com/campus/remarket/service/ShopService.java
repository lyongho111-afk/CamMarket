package com.campus.remarket.service;
import com.campus.remarket.dao.ShopDao;
import com.campus.remarket.dto.ShopDto;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
@Service
public class ShopService {
    private final ShopDao dao;
    private final CampusScope scope;
    public ShopService(ShopDao dao,CampusScope scope){this.dao=dao;this.scope=scope;}
    public List<ShopDto> selectAll(){return dao.selectAll().stream().filter(x->scope.visible(x.getSchoolId())).toList();}
    public ShopDto selectOne(Long id){var dto=dao.selectOne(id);if(dto==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"가게을 찾을 수 없습니다.");scope.require(dto.getSchoolId());return dto;}
    private void validate(ShopDto dto){requireText(dto.getShopname(),"가게명");if(dto.getStatus()==null||dto.getStatus().isBlank())dto.setStatus("영업종료");if(!List.of("영업중","혼잡","영업종료","휴무").contains(dto.getStatus()))throw new IllegalArgumentException("가게 상태를 확인하세요.");}
    private void requireText(String value,String label){if(value==null||value.isBlank())throw new IllegalArgumentException(label+"을 입력하세요.");}
    @Transactional public void insert(ShopDto dto){if(dto.getSchoolId()==null)dto.setSchoolId(scope.id());validate(dto);dao.insert(dto);}
    @Transactional public void update(ShopDto dto){validate(dto);selectOne(dto.getShopId());dao.update(dto);}
    @Transactional public void delete(Long id){selectOne(id);dao.delete(id);}
}
