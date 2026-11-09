package com.campus.remarket.portal;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController
public class StaffPortalController {
    private final StaffPortalService service;
    public StaffPortalController(StaffPortalService service){this.service=service;}
    @GetMapping("/api/owner/dashboard") public Map<String,Object> owner(){return service.ownerHome();}
    @GetMapping("/api/owner/shop/history") public java.util.List<com.campus.remarket.dto.ShopChangeDto> history(){return service.history();}
    @PostMapping("/api/owner/shop") public StaffPortalService.ShopView create(@RequestBody StaffPortalService.ShopInput input,@RequestHeader(value="X-CSRF-Token",required=false)String csrf){return service.create(input,csrf);}
    @PutMapping("/api/owner/shop") public StaffPortalService.ShopView update(@RequestBody StaffPortalService.ShopInput input,@RequestHeader(value="X-CSRF-Token",required=false)String csrf){return service.update(input,csrf,true);}
    @PutMapping("/api/owner/shop/operations") public StaffPortalService.ShopView operations(@RequestBody StaffPortalService.ShopInput input,@RequestHeader(value="X-CSRF-Token",required=false)String csrf){return service.update(input,csrf,false);}
    @GetMapping("/api/admin/dashboard") public Map<String,Object> admin(){return service.adminHome();}
    @GetMapping("/api/admin/history") public java.util.List<com.campus.remarket.dto.AdminActionDto> adminHistory(){return service.adminHistory();}
    public record Status(String status,Long version){}
    @PutMapping("/api/owner/shop/status") public StaffPortalService.ShopView status(@RequestBody Status input,@RequestHeader(value="X-CSRF-Token",required=false)String csrf){return service.status(input.status(),input.version(),csrf);}
    public record Approval(Boolean approved,Long version,String note){}
    public record Assignment(Long ownerId,Long version,String note){}
    @PutMapping("/api/admin/shops/{id}/approval") public StaffPortalService.ShopView approval(@PathVariable Long id,@RequestBody Approval input,@RequestHeader(value="X-CSRF-Token",required=false)String csrf){return service.approval(id,input.approved(),input.version(),input.note(),csrf);}
    @PutMapping("/api/admin/shops/{id}/owner") public StaffPortalService.ShopView assign(@PathVariable Long id,@RequestBody Assignment input,@RequestHeader(value="X-CSRF-Token",required=false)String csrf){return service.assign(id,input.ownerId(),input.version(),input.note(),csrf);}
}
