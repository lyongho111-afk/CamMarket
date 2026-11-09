package com.campus.remarket.controller;
import com.campus.remarket.auth.MarketMember;
import com.campus.remarket.dto.TradeDto;
import com.campus.remarket.service.TradeService;
import java.util.List;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/trades")
public class TradeApiController {
    private final MarketMember member;
    private final TradeService service;
    public TradeApiController(MarketMember member,TradeService service){this.member=member;this.service=service;}
    @GetMapping public List<TradeDto> selectAll(){return member.visibleTrades();}
    @PostMapping public TradeDto insert(@RequestBody TradeDto dto){member.reserve(dto.getSelno());dto.setBuyer(member.studentKey());service.insert(dto);return service.selectOne(dto.getOrderId());}
    @PostMapping("/{id}/cancel") public TradeDto cancel(@PathVariable String id){member.trade(id);service.cancel(id);return service.selectOne(id);}
    @PostMapping("/{id}/complete") public TradeDto complete(@PathVariable String id){member.trade(id);service.complete(id);return service.selectOne(id);}
    @PutMapping("/{id}/meeting") public TradeDto meeting(@PathVariable String id,@RequestBody TradeDto dto){member.trade(id);dto.setOrderId(id);service.meeting(dto);return service.selectOne(id);}
}
