package com.campus.remarket.controller;
import com.campus.remarket.auth.MarketMember;
import com.campus.remarket.dto.SalesDto;
import com.campus.remarket.service.SalesService;
import java.util.List;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/products")
public class ProductApiController {
    private final MarketMember member;
    private final SalesService service;
    public ProductApiController(MarketMember member,SalesService service){this.member=member;this.service=service;}
    @GetMapping public List<SalesDto> selectAll(){return service.selectAll();}
    @GetMapping("/{id}") public SalesDto selectOne(@PathVariable Long id){return service.selectOne(id);}
    @PostMapping public SalesDto insert(@RequestBody SalesDto dto){dto.setSeller(member.studentKey());dto.setSellerMemberId(member.current().getId());service.insert(dto);return service.selectOne(dto.getSelno());}
    @PutMapping("/{id}") public SalesDto update(@PathVariable Long id,@RequestBody SalesDto dto){member.sale(id);dto.setSelno(id);dto.setSeller(member.studentKey());service.update(dto);return service.selectOne(id);}
    @DeleteMapping("/{id}") public void delete(@PathVariable Long id){member.sale(id);service.delete(id);}
    public record DiscountInput(int rate){}
    @PostMapping("/{id}/discount") public SalesDto discount(@PathVariable Long id,@RequestBody DiscountInput input){member.sale(id);service.startDiscount(id,input.rate());return service.selectOne(id);}
    @PostMapping("/{id}/discount-end") public SalesDto discountEnd(@PathVariable Long id){member.sale(id);service.endDiscount(id);return service.selectOne(id);}
    @PostMapping("/{id}/bump") public SalesDto bump(@PathVariable Long id){member.sale(id);service.bump(id);return service.selectOne(id);}
}
