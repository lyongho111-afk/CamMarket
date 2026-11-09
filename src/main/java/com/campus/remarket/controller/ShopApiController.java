package com.campus.remarket.controller;
import com.campus.remarket.service.ShopService;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/shops")
public class ShopApiController {
    private final ShopService service;
    public ShopApiController(ShopService service){this.service=service;}
    // 공개 화면에는 가게의 공개 정보만 전송합니다. 비밀번호·사업자 정보는 제외합니다.
    @GetMapping public List<Map<String,Object>> selectAll(){return service.selectAll().stream().filter(s->Boolean.TRUE.equals(s.getChecked())).map(s->{Map<String,Object> m=new LinkedHashMap<>();m.put("id",s.getShopId());m.put("title",s.getShopname());m.put("phone",s.getShopphone());m.put("category",s.getBusiKind());m.put("menu",s.getBmenu());m.put("hours",s.getBusitime());m.put("offday",s.getOffday());m.put("event",s.getSpecialevent());m.put("status",s.getStatus());m.put("space",s.getAvailablespace());m.put("photo",s.getPhoto());m.put("address",s.getAddress());m.put("latitude",s.getLatitude());m.put("longitude",s.getLongitude());return m;}).toList();}
}
