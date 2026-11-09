package com.campus.remarket.notification;

import com.campus.remarket.dao.StudentDao;
import com.campus.remarket.dto.TradeDto;
import demo.campus.campusremarketmember.member.entity.Member;
import demo.campus.campusremarketmember.member.repository.MemberRepository;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NotificationService {
    public record Settings(String email,String phone,boolean smsEnabled,boolean emailEnabled,boolean kakaoEnabled,boolean deliveryEnabled){}
    private final JdbcTemplate db;private final StudentDao students;private final MemberRepository members;private final boolean enabled;
    public NotificationService(JdbcTemplate db,StudentDao students,MemberRepository members,@Value("${campus.notifications.enabled:false}")boolean enabled){this.db=db;this.students=students;this.members=members;this.enabled=enabled;}
    // Called inside reservation transaction: rollback also removes the outbox.
    public void reservation(TradeDto trade,String seller,String product){
        String message=product+"\n거래 장소: "+Objects.toString(trade.getMeetingPlace(),"장소 미정")+"\n약속 시간: "+Objects.toString(trade.getMeetingTime(),"시간 미정")+"\n약속 가격: "+trade.getAgreedPrice()+"원";
        var recipients=new LinkedHashMap<Long,String>();
        var sellerStudent=students.selectOne(seller);
        if(sellerStudent!=null&&sellerStudent.getMemberId()!=null)
            recipients.put(sellerStudent.getMemberId(),"판매 거래 약속이 잡혔어요");
        var buyerStudent=students.selectOne(trade.getBuyer());
        if(buyerStudent!=null&&buyerStudent.getMemberId()!=null)
            recipients.putIfAbsent(buyerStudent.getMemberId(),"구매 거래 약속이 잡혔어요");
        // Legacy table/column names are retained to preserve existing notifications.
        // seller_member_id now identifies the recipient, whether seller or buyer.
        recipients.forEach((memberId,title)->{
            String id=UUID.randomUUID().toString();
            db.update("INSERT INTO seller_notifications(notification_id,order_id,seller_member_id,sale_id,title,message) VALUES(?,?,?,?,?,?)",id,trade.getOrderId(),memberId,trade.getSelno(),title,message);
            for(String channel:List.of("SMS","EMAIL","KAKAO"))db.update("INSERT INTO notification_deliveries(delivery_id,notification_id,channel,status) VALUES(?,?,?,'PENDING')",UUID.randomUUID().toString(),id,channel);
        });
    }
    public Settings settings(Member me){
        var rows=db.queryForList("SELECT * FROM notification_settings WHERE member_id=?",me.getId());
        if(rows.isEmpty())return new Settings(Objects.toString(me.getEmail(),""),Objects.toString(me.getPhone(),""),true,true,true,enabled);
        var r=rows.getFirst();return new Settings((String)r.get("EMAIL"),(String)r.get("PHONE"),(Boolean)r.get("SMS_ENABLED"),(Boolean)r.get("EMAIL_ENABLED"),(Boolean)r.get("KAKAO_ENABLED"),enabled);
    }
    @Transactional public void settings(Member me,Settings input){
        String email=Objects.toString(input.email(),"").trim(),phone=Objects.toString(input.phone(),"").replaceAll("[^0-9]","");
        if(email.length()>100||!email.isEmpty()&&!email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")||!phone.isEmpty()&&!phone.matches("0[0-9]{8,10}"))throw new IllegalArgumentException("이메일과 전화번호를 확인해 주세요.");
        if(db.update("UPDATE notification_settings SET email=?,phone=?,sms_enabled=?,email_enabled=?,kakao_enabled=? WHERE member_id=?",email,phone,input.smsEnabled(),input.emailEnabled(),input.kakaoEnabled(),me.getId())==0)
            db.update("INSERT INTO notification_settings(member_id,email,phone,sms_enabled,email_enabled,kakao_enabled) VALUES(?,?,?,?,?,?)",me.getId(),email,phone,input.smsEnabled(),input.emailEnabled(),input.kakaoEnabled());
    }
    public List<Map<String,Object>> list(long memberId){
        return db.query("SELECT * FROM seller_notifications WHERE seller_member_id=? ORDER BY created_at DESC,notification_id DESC LIMIT 100",(rs,i)->{
            Map<String,Object> n=new LinkedHashMap<>();n.put("id",rs.getString("notification_id"));n.put("saleId",rs.getLong("sale_id"));n.put("title",rs.getString("title"));n.put("message",rs.getString("message"));n.put("createdAt",rs.getTimestamp("created_at").toLocalDateTime());var read=rs.getTimestamp("read_at");n.put("readAt",read==null?null:read.toLocalDateTime());
            n.put("deliveries",db.query("SELECT channel,status FROM notification_deliveries WHERE notification_id=? ORDER BY channel",(d,j)->Map.of("channel",d.getString(1),"status",d.getString(2)),rs.getString("notification_id")));return n;
        },memberId);
    }
    private void own(long memberId,String id){if(db.queryForObject("SELECT COUNT(*) FROM seller_notifications WHERE notification_id=? AND seller_member_id=?",Integer.class,id,memberId)!=1)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"알림을 찾을 수 없습니다.");}
    public void read(long memberId,String id){own(memberId,id);db.update("UPDATE seller_notifications SET read_at=CURRENT_TIMESTAMP WHERE notification_id=? AND seller_member_id=?",id,memberId);}
    public void retry(long memberId,String id){own(memberId,id);db.update("UPDATE notification_deliveries SET status='PENDING',last_error=NULL WHERE notification_id=? AND status IN ('FAILED','SETUP_REQUIRED','CONTACT_REQUIRED','DISABLED')",id);}
    public Member recipient(long id){return members.findById(id).orElseThrow();}
}
