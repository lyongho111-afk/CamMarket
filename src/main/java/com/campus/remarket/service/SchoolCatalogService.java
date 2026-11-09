package com.campus.remarket.service;
import demo.campus.campusremarketmember.member.entity.School;
import demo.campus.campusremarketmember.member.repository.SchoolRepository;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
@Service public class SchoolCatalogService {
 public record Input(String name,String region,String address,BigDecimal latitude,BigDecimal longitude,String emailDomain,String photo,String mapImage,Long version){}
 public record View(Long id,String name,String region,String address,BigDecimal latitude,BigDecimal longitude,String emailDomain,String photo,String mapImage,String campusCode,Long version,long members){}
 private final SchoolRepository schools;private final SchoolImages images;private final JdbcTemplate db;
 public SchoolCatalogService(SchoolRepository schools,SchoolImages images,JdbcTemplate db){this.schools=schools;this.images=images;this.db=db;}
 public View view(School s){return new View(s.getId(),s.getName(),s.getRegion(),s.getAddress(),s.getLatitude(),s.getLongitude(),s.getEmailDomain(),s.getPhoto(),s.getMapImage(),s.getCampusCode(),s.getVersion(),db.queryForObject("SELECT COUNT(*) FROM member WHERE school_id=?",Long.class,s.getId()));}
 public List<View> list(){return schools.findAllByOrderByNameAsc().stream().map(this::view).toList();}
 private String text(String s,int max,boolean required){String v=Objects.toString(s,"").strip();if(required&&v.isBlank()||v.length()>max)throw new IllegalArgumentException("학교명·주소·지역 입력 길이를 확인하세요.");return v;}
 public static String domains(String value){String input=Objects.toString(value,"").strip().toLowerCase(Locale.ROOT);if(input.isEmpty()||input.length()>100)throw new IllegalArgumentException("학교 인증용 이메일 도메인을 100자 이내로 입력하세요.");var result=new LinkedHashSet<String>();for(String part:input.split(",",-1)){String d=part.strip();if(!d.matches("(?=.{1,253}$)(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z]{2,63}"))throw new IllegalArgumentException("이메일 주소가 아닌 도메인만 입력하세요. 예: inha.edu,inha.ac.kr");result.add(d);}return String.join(",",result);}
 @Transactional public View save(Long id,Input in){String name=text(in.name(),50,true),address=text(in.address(),200,true),region=text(in.region(),50,false);if(region.isEmpty())region=address.substring(0,Math.min(50,address.length()));AddressLocator.validate(in.latitude(),in.longitude());if(in.latitude()==null)throw new IllegalArgumentException("학교의 위도·경도를 함께 입력하세요.");String domain=domains(in.emailDomain()),photo=images.validate(in.photo()),map=images.validate(in.mapImage());School school;
 if(id==null){school=new School(name,region,domain);school.campus(switch(name){case "인하대학교"->"INHA";case "인천대학교"->"INU";case "청운대학교 인천캠퍼스"->"CHUNGWOON";default->null;});}else{school=schools.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"학교가 없습니다."));db.queryForObject("SELECT id FROM school WHERE id=? FOR UPDATE",Long.class,id);if(!Objects.equals(school.getVersion(),in.version()))throw new ResponseStatusException(HttpStatus.CONFLICT,"학교 정보가 변경되었습니다. 새로고침하세요.");}school.update(name,region,domain,address,in.latitude(),in.longitude(),photo,map);school=schools.saveAndFlush(school);db.update("UPDATE board_posts SET school_name=? WHERE school_id=?",school.getName(),school.getId());return view(school);}
 @Transactional public void delete(long id,Long version){var school=schools.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"학교가 없습니다."));db.queryForObject("SELECT id FROM school WHERE id=? FOR UPDATE",Long.class,id);if(!Objects.equals(school.getVersion(),version))throw new ResponseStatusException(HttpStatus.CONFLICT,"학교 정보가 변경되었습니다. 새로고침하세요.");long used=db.queryForObject("SELECT (SELECT COUNT(*) FROM member WHERE school_id=?)+(SELECT COUNT(*) FROM sales WHERE school_id=?)+(SELECT COUNT(*) FROM board_posts WHERE school_id=?)+(SELECT COUNT(*) FROM shops WHERE school_id=?)",Long.class,id,id,id,id);if(used>0)throw new ResponseStatusException(HttpStatus.CONFLICT,"회원·게시글·가게에서 사용 중인 학교는 삭제할 수 없습니다.");schools.delete(school);schools.flush();}
}
