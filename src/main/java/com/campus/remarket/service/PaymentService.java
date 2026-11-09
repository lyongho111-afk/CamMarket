package com.campus.remarket.service;
import com.campus.remarket.dao.TradeDao;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
@Service
public class PaymentService {
    private final TradeDao dao;
    private final String clientKey,secretKey;
    public PaymentService(TradeDao dao,@Value("${integrations.toss-client-key:}") String clientKey,@Value("${integrations.toss-secret-key:}") String secretKey){this.dao=dao;this.clientKey=clientKey;this.secretKey=secretKey;}
    public boolean ready(){return clientKey.startsWith("test_ck_")&&secretKey.startsWith("test_sk_");}
    public String clientKey(){return ready()?clientKey:"";}
    @Transactional public Map<String,Object> prepare(String id){
        if(!ready())throw new IllegalStateException("토스페이먼츠 테스트 클라이언트·시크릿 키 설정이 필요합니다.");
        var order=dao.lockOne(id);if(order==null)throw new IllegalArgumentException("예약을 먼저 확정하세요.");
        if(!java.util.List.of("RESERVED","PAYMENT_READY").contains(order.getStatus()))throw new IllegalStateException("결제를 진행할 수 없는 거래입니다.");
        if(order.getAgreedPrice()<1)throw new IllegalArgumentException("무료 나눔은 결제를 진행하지 않습니다.");
        dao.paymentReady(id);
        return Map.of("orderId",id,"amount",order.getAgreedPrice(),"orderName",dao.selectOne(id).getSaleName());
    }
    @Transactional public Map<String,Object> confirm(String id,String key,Integer amount){
        if(!ready())throw new IllegalStateException("토스페이먼츠 테스트 키를 설정하세요.");
        var order=dao.lockOne(id);if(order==null)throw new IllegalArgumentException("주문을 찾을 수 없습니다.");
        if(key==null||key.isBlank()||key.length()>200||!order.getAgreedPrice().equals(amount))throw new IllegalArgumentException("주문 금액 또는 결제 정보를 확인하세요.");
        if("PAID".equals(order.getStatus())&&key.equals(order.getPaymentKey()))return Map.of("status","DONE","orderId",id);
        if(!"PAYMENT_READY".equals(order.getStatus()))throw new IllegalStateException("결제 준비 상태가 아닙니다.");
        try{
            Map<?,?> result=RestClient.create().post().uri("https://api.tosspayments.com/v1/payments/confirm")
                .header("Authorization","Basic "+Base64.getEncoder().encodeToString((secretKey+":").getBytes(StandardCharsets.UTF_8)))
                .header("Idempotency-Key",id).contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(Map.of("paymentKey",key,"orderId",id,"amount",order.getAgreedPrice())).retrieve().body(Map.class);
            if(result==null||!id.equals(result.get("orderId"))||!(result.get("totalAmount") instanceof Number n)||n.intValue()!=amount||!"DONE".equals(result.get("status")))throw new IllegalStateException("토스 결제 승인 결과를 확인하지 못했습니다.");
            dao.paymentConfirmed(id,key);
            return Map.of("status","DONE","orderId",id);
        }catch(RestClientException error){throw new IllegalStateException("토스 테스트 승인이 완료되지 않았습니다. 결제 내역을 확인하고 다시 시도하세요.");}
    }
}
