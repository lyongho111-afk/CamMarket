package com.campus.remarket.service;

import com.campus.remarket.auth.MarketMember;
import com.campus.remarket.dao.StudentDao;
import com.campus.remarket.dto.SalesDto;
import demo.campus.campusremarketmember.member.entity.Member;
import demo.campus.campusremarketmember.member.repository.MemberRepository;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service @Transactional(readOnly=true)
public class MemberProfileService {
    public record Review(Integer score,String feedback,String sentiment,int delta,String attitude,String content,LocalDateTime createdAt){}
    public record FeedbackCount(String label,long count){}
    public record Summary(int total,long good,long bad,long neutral,long stars,Double averageScore,long completedTrades,Double satisfactionRate,List<FeedbackCount> feedback){}
    public record Listing(Long selno,String category,String name,Integer currentPrice,String status,String location,Long sellerMemberId,String sellerName,String sellerSchoolName,java.math.BigDecimal sellerTemperature){}
    public record Profile(Long memberId,String name,String schoolName,String role,Double temperature,boolean verifiedMember,boolean own,Summary summary,List<Review> reviews,List<Listing> listings){}
    private final MarketMember actor;
    private final MemberRepository members;
    private final StudentDao students;
    private final SalesService sales;
    private final JdbcTemplate db;
    public MemberProfileService(MarketMember actor,MemberRepository members,StudentDao students,SalesService sales,JdbcTemplate db){this.actor=actor;this.members=members;this.students=students;this.sales=sales;this.db=db;}
    public Profile me(){actor.key();return profile(actor.current(),null);}
    public Profile seller(Long saleId){var sale=sales.selectOne(saleId);var member=sale.getSellerMemberId()==null?null:members.findById(sale.getSellerMemberId()).orElse(null);return profile(member,sale);}
    private Profile profile(Member member,SalesDto origin){
        var current=actor.current();boolean own=member!=null&&current!=null&&member.getId().equals(current.getId());
        var student=member==null?students.selectOne(origin.getSeller()):students.forMember(member.getId());
        if(member!=null&&member.isStudent()&&student==null)throw new ResponseStatusException(HttpStatus.CONFLICT,"학생 회원 연결을 확인해 주세요.");
        String key=student==null?null:student.getUnisno();
        List<Review> received=key==null?List.of():reviews(key);
        var starScores=received.stream().filter(r->r.score()!=null).mapToInt(Review::score).summaryStatistics();
        long good=received.stream().filter(r->r.sentiment().equals("GOOD")).count();
        long completed=key==null?0:db.queryForObject("SELECT COUNT(*) FROM trade_orders o JOIN sales s ON s.selno=o.selno WHERE s.seller=? AND o.status IN ('COMPLETED','거래완료')",Long.class,key);
        var feedback=new ArrayList<FeedbackCount>();
        for(var item:List.of(Map.entry("시간엄수","시간 약속을 잘 지켜요."),Map.entry("친절매너","친절하고 배려가 넘쳐요."),Map.entry("빠른응답","답변이 빨라요."),Map.entry("상품상태좋음","상품 상태가 좋아요."))){
            long count=received.stream().filter(r->r.sentiment().equals("GOOD")&&r.attitude()!=null&&Arrays.stream(r.attitude().split("[,·\\n]")).anyMatch(tag->tag.replace(" ","").equals(item.getKey()))).count();
            if(count>0)feedback.add(new FeedbackCount(item.getValue(),count));
        }
        feedback.sort(Comparator.comparingLong(FeedbackCount::count).reversed());
        var summary=new Summary(received.size(),good,received.stream().filter(r->r.sentiment().equals("BAD")).count(),received.stream().filter(r->r.sentiment().equals("NEUTRAL")).count(),starScores.getCount(),starScores.getCount()==0?null:starScores.getAverage(),completed,received.isEmpty()?null:good*100.0/received.size(),feedback);
        var listings=sales.selectAll().stream().filter(s->member==null?s.getSellerMemberId()==null&&origin.getSeller().equals(s.getSeller()):member.getId().equals(s.getSellerMemberId())).map(s->new Listing(s.getSelno(),s.getCategory(),s.getName(),s.getCurrentPrice(),s.getStatus(),s.getLocation(),s.getSellerMemberId(),s.getSellerName(),s.getSellerSchoolName(),s.getSellerTemperature())).toList();
        Double temperature=student==null?null:student.getTemp()/10.0;
        return new Profile(member==null?null:member.getId(),member==null?"기존 시연 판매자":member.getName(),member==null||member.getSchool()==null?null:member.getSchool().getName(),member==null?"STUDENT":member.getRole().name(),temperature,member!=null,own,summary,own?received:List.of(),listings);
    }
    private List<Review> reviews(String key){
        var reviews=new ArrayList<Review>();
        reviews.addAll(db.query("SELECT score,temperature_delta,attitude,content,created_at FROM trade_reviews WHERE target_id=?",(rs,i)->{int score=rs.getInt("score");var time=rs.getTimestamp("created_at");return new Review(score,"SCORE",score>=4?"GOOD":score<=2?"BAD":"NEUTRAL",rs.getInt("temperature_delta"),rs.getString("attitude"),rs.getString("content"),time==null?null:time.toLocalDateTime());},key));
        reviews.addAll(db.query("SELECT feedback,delta,created_at FROM manner_ratings WHERE recipient_unisno=?",(rs,i)->{String feedback=rs.getString("feedback");return new Review(null,feedback,feedback,rs.getInt("delta"),null,null,rs.getTimestamp("created_at").toLocalDateTime());},key));
        reviews.sort(Comparator.comparing(Review::createdAt,Comparator.nullsLast(Comparator.reverseOrder())));return reviews;
    }
}
