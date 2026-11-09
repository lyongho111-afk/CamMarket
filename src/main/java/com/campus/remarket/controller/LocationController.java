package com.campus.remarket.controller;
import com.campus.remarket.service.AddressLocator;
import com.campus.remarket.portal.StaffAccess;
import com.campus.remarket.portal.PortalShopDao;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
@RestController public class LocationController {
 private final AddressLocator locator;private final StaffAccess access;private final PortalShopDao shops;private final com.campus.remarket.portal.ShopChangeDao changes;
 public LocationController(AddressLocator locator,StaffAccess access,PortalShopDao shops,com.campus.remarket.portal.ShopChangeDao changes){this.changes=changes;this.locator=locator;this.access=access;this.shops=shops;}
 @GetMapping("/api/location/search") public Map<String,Object> search(@RequestParam String address){var p=locator.find(address);var r=new LinkedHashMap<String,Object>();r.put("configured",locator.ready());r.put("found",p!=null);r.put("point",p);r.put("message",p!=null?"주소 위치를 찾았습니다.":locator.ready()?"주소를 찾지 못했습니다. 도로명·지번 주소를 확인하거나 지도에서 직접 지정하세요.":"자동 검색 키가 설정되지 않았습니다. 지도에서 직접 위치를 지정하세요.");return r;}
 public record Input(BigDecimal latitude,BigDecimal longitude,Long version,String address){}
 @PutMapping("/api/owner/shop/location") @Transactional public Map<String,Object> save(@RequestBody Input input,@RequestHeader(value="X-CSRF-Token",required=false)String csrf){access.owner();access.verify(csrf);AddressLocator.validate(input.latitude(),input.longitude());if(input.latitude()==null)throw new IllegalArgumentException("지도에서 위치를 선택하세요.");var shop=shops.forOwner(access.ownerId());if(shop==null)throw new IllegalArgumentException("먼저 가게를 등록하세요.");shop=shops.lock(shop.getShopId());if(!Objects.equals(shop.getVersion(),input.version()))throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,"가게 정보가 변경되었습니다. 새로고침하세요.");String address=input.address()==null?shop.getAddress():input.address().trim();if(address!=null&&address.length()>200)throw new IllegalArgumentException("주소는 200자 이내로 입력하세요.");shops.location(shop.getShopId(),input.latitude(),input.longitude(),address);
 String[][] fields={{"address","주소",shop.getAddress(),address},{"latitude","위도",Objects.toString(shop.getLatitude(),""),input.latitude().toString()},{"longitude","경도",Objects.toString(shop.getLongitude(),""),input.longitude().toString()}};
 for(var field:fields){if(Objects.equals(field[2],field[3]))continue;var row=new com.campus.remarket.dto.ShopChangeDto();row.setShopId(shop.getShopId());row.setActorMemberId(access.owner().getId());row.setActorName(access.owner().getName());row.setActorRole("OWNER");row.setFieldKey(field[0]);row.setFieldLabel(field[1]);row.setBeforeValue(Objects.toString(field[2],""));row.setAfterValue(Objects.toString(field[3],""));changes.insert(row);}
 return Map.of("saved",true);}
}
