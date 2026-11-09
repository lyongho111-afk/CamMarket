package com.campus.remarket.notification;

import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** Transactional outbox. No external calls occur during the reservation transaction. */
@Component @EnableScheduling
public class NotificationDispatcher {
    private final JdbcTemplate db;private final NotificationService notifications;private final Environment env;private final ObjectProvider<JavaMailSender> mail;private final ObjectMapper json;
    private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    public NotificationDispatcher(JdbcTemplate db,NotificationService notifications,Environment env,ObjectProvider<JavaMailSender> mail,ObjectMapper json){this.db=db;this.notifications=notifications;this.env=env;this.mail=mail;this.json=json;}
    private String config(String key){return env.getProperty(key,"");}
    private boolean blank(String s){return s==null||s.isBlank();}
    @Scheduled(fixedDelayString="${campus.notifications.poll-ms:5000}",initialDelayString="${campus.notifications.initial-delay-ms:5000}")
    public void dispatch(){
        var rows=db.queryForList("SELECT d.delivery_id,d.channel,n.seller_member_id,n.message,n.title,n.order_id FROM notification_deliveries d JOIN seller_notifications n ON d.notification_id=n.notification_id WHERE d.status='PENDING' ORDER BY n.created_at LIMIT 20");
        for(var row:rows){String id=(String)row.get("DELIVERY_ID"),channel=(String)row.get("CHANNEL");
            if(db.update("UPDATE notification_deliveries SET status='SENDING',attempted_at=CURRENT_TIMESTAMP WHERE delivery_id=? AND status='PENDING'",id)!=1)continue;
            if(env.matchesProfiles("local-demo")&&!channel.equals("EMAIL")){finish(id,"DISABLED",null);continue;}
            try{
                var me=notifications.recipient(((Number)row.get("SELLER_MEMBER_ID")).longValue());var prefs=notifications.settings(me);
                boolean subscribed=switch(channel){case "SMS"->prefs.smsEnabled();case "EMAIL"->prefs.emailEnabled();default->prefs.kakaoEnabled();};
                String recipient=channel.equals("EMAIL")?prefs.email():prefs.phone().replaceAll("[^0-9]","");
                if(!subscribed){finish(id,"DISABLED",null);continue;}
                if(blank(recipient)){finish(id,"CONTACT_REQUIRED",null);continue;}
                if(!prefs.deliveryEnabled()||!configured(channel)){finish(id,"SETUP_REQUIRED",null);continue;}
                if(channel.equals("EMAIL"))sendEmail(recipient,(String)row.get("TITLE"),(String)row.get("MESSAGE"));else sendSolapi(channel,recipient,(String)row.get("MESSAGE"),(String)row.get("ORDER_ID"));
                // Accepted by SMTP/provider. Delivery to the handset/inbox is not asserted.
                finish(id,"ACCEPTED",null);
            }catch(Exception e){finish(id,"FAILED","외부 서비스 접수 실패. 설정과 서비스 발송 이력을 확인한 뒤 다시 시도하세요.");}
        }
    }
    private boolean configured(String channel){if(channel.equals("EMAIL"))return mail.getIfAvailable()!=null&&!blank(config("campus.notifications.mail-from"));return !blank(config("campus.notifications.solapi-key"))&&!blank(config("campus.notifications.solapi-secret"))&&!blank(config("campus.notifications.sms-from"))&&(!channel.equals("KAKAO")||!blank(config("campus.notifications.kakao-pf-id"))&&!blank(config("campus.notifications.kakao-template-id")));}
    private void finish(String id,String status,String error){db.update("UPDATE notification_deliveries SET status=?,last_error=? WHERE delivery_id=?",status,error,id);}
    private void sendEmail(String recipient,String title,String body){var message=new SimpleMailMessage();message.setFrom(config("campus.notifications.mail-from"));message.setTo(recipient);message.setSubject("[캠퍼스 리마켓] "+title);message.setText(body);mail.getObject().send(message);}
    private void sendSolapi(String channel,String recipient,String body,String orderId)throws Exception{
        String date=Instant.now().toString(),salt=UUID.randomUUID().toString().replace("-","");var mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(config("campus.notifications.solapi-secret").getBytes(StandardCharsets.UTF_8),"HmacSHA256"));String signature=HexFormat.of().formatHex(mac.doFinal((date+salt).getBytes(StandardCharsets.UTF_8)));
        var message=new LinkedHashMap<String,Object>();message.put("to",recipient);message.put("from",config("campus.notifications.sms-from"));message.put("text",body);message.put("type",channel.equals("KAKAO")?"ATA":"LMS");
        if(channel.equals("KAKAO"))message.put("kakaoOptions",Map.of("pfId",config("campus.notifications.kakao-pf-id"),"templateId",config("campus.notifications.kakao-template-id"),"disableSms",true,"variables",Map.of("#{약속정보}",body,"#{주문번호}",orderId)));
        var req=HttpRequest.newBuilder(URI.create(config("campus.notifications.solapi-url"))).timeout(Duration.ofSeconds(10)).header("Content-Type","application/json;charset=UTF-8").header("Authorization","HMAC-SHA256 apiKey="+config("campus.notifications.solapi-key")+", date="+date+", salt="+salt+", signature="+signature).POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(Map.of("messages",List.of(message),"allowDuplicates",false)))).build();
        var response=http.send(req,HttpResponse.BodyHandlers.ofString());if(response.statusCode()/100!=2)throw new IllegalStateException("provider response");
        var result=json.readTree(response.body());if(result.path("failedMessageList").size()>0||result.path("groupInfo").path("count").path("registeredFailed").asInt(0)>0||result.path("errorCount").asInt(0)>0)throw new IllegalStateException("provider rejected");
        if(result.path("groupInfo").path("groupId").asText("").isBlank()&&result.path("groupId").asText("").isBlank())throw new IllegalStateException("provider receipt missing");
    }
}
