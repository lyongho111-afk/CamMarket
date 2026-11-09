package com.campus.remarket.controller;
import com.campus.remarket.auth.MarketMember;
import com.campus.remarket.service.PaymentService;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api")
public class IntegrationApiController {
    private final MarketMember member;
    private final PaymentService service;
    private final String mapsKey;
    public IntegrationApiController(MarketMember member,PaymentService service,@Value("${integrations.google-maps-key:}") String mapsKey){this.member=member;this.service=service;this.mapsKey=mapsKey;}
    @GetMapping("/integrations/config") public Map<String,Object> config(){return Map.of("mapsKey",mapsKey,"tossClientKey",service.clientKey(),"tossReady",service.ready());}
    public record PrepareInput(String orderId){}
    public record ConfirmInput(String orderId,String paymentKey,Integer amount){}
    @PostMapping("/payments/prepare") public Map<String,Object> prepare(@RequestBody PrepareInput input){member.buyer(input.orderId());return service.prepare(input.orderId());}
    @PostMapping("/payments/confirm") public Map<String,Object> confirm(@RequestBody ConfirmInput input){member.buyer(input.orderId());return service.confirm(input.orderId(),input.paymentKey(),input.amount());}
}
