package com.campus.remarket.config;
import demo.campus.campusremarketmember.member.repository.*;
import demo.campus.campusremarketmember.member.entity.School;
import java.math.BigDecimal;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
@Component @Order(30) public class CatalogSeed implements CommandLineRunner {
 private final JdbcTemplate db;private final SchoolRepository schools;
 public CatalogSeed(JdbcTemplate db,SchoolRepository schools){this.db=db;this.schools=schools;}
 @Transactional public void run(String... args){
  if(db.queryForObject("SELECT COUNT(*) FROM catalog_meta WHERE name='school-details-v1'",Integer.class)==0){
   String[][] values={{"INHA","인하대학교","인천 미추홀구","inha.edu,inha.ac.kr","인천광역시 미추홀구 인하로 100","37.4501","126.6537"},{"INU","인천대학교","인천 연수구","inu.ac.kr","인천광역시 연수구 아카데미로 119","37.3751446","126.6323415"},{"CHUNGWOON","청운대학교 인천캠퍼스","인천 미추홀구","chungwoon.ac.kr","인천광역시 미추홀구 숙골로 113","37.4719042","126.6608009"}};
   for(var v:values){var s=schools.findByName(v[1]).orElseGet(()->new School(v[1],v[2],v[3]));s.campus(v[0]);if(s.getAddress()==null)s.update(s.getName(),s.getRegion(),s.getEmailDomain(),v[4],new BigDecimal(v[5]),new BigDecimal(v[6]),null,null);schools.saveAndFlush(s);}
   db.update("INSERT INTO catalog_meta(name) VALUES('school-details-v1')");
  }
  long inha=schools.findByCampusCode("INHA").orElseThrow().getId();
  db.update("UPDATE member SET school_id=? WHERE role='OWNER' AND login_id='owner1' AND school_id IS NULL",inha);
  db.update("UPDATE sales SET school_id=(SELECT m.school_id FROM member m WHERE m.id=sales.seller_member_id) WHERE school_id IS NULL AND seller_member_id IS NOT NULL");
  db.update("UPDATE sales SET school_id=(SELECT m.school_id FROM member m JOIN student st ON st.member_id=m.id WHERE st.unisno=sales.seller) WHERE school_id IS NULL AND EXISTS(SELECT 1 FROM student st WHERE st.unisno=sales.seller)");
  db.update("UPDATE board_posts SET school_id=(SELECT m.school_id FROM member m WHERE m.id=board_posts.author_member_id) WHERE school_id IS NULL AND author_member_id IS NOT NULL");
  db.update("UPDATE board_posts SET school_id=?,school_name=(SELECT name FROM school WHERE id=?) WHERE school_id IS NULL AND writer='demo-student' AND author_member_id IS NULL",inha,inha);
  db.update("UPDATE shops SET school_id=(SELECT m.school_id FROM member m WHERE m.id=shops.owner_member_id) WHERE school_id IS NULL AND owner_member_id IS NOT NULL");
  if(db.queryForObject("SELECT COUNT(*) FROM catalog_meta WHERE name='categories-v1'",Integer.class)==0){for(String name:new String[]{"카페","편의점","중식","양식","디저트","호프","주점","당구장","탁구장"})if(db.queryForObject("SELECT COUNT(*) FROM business_categories WHERE name=?",Integer.class,name)==0)db.update("INSERT INTO business_categories(name) VALUES(?)",name);db.update("INSERT INTO catalog_meta(name) VALUES('categories-v1')");}
  // Existing used categories remain valid; do not recreate unused categories deleted by an administrator.
  for(String name:db.queryForList("SELECT DISTINCT busi_kind FROM shops WHERE busi_kind IS NOT NULL AND TRIM(busi_kind)<>''",String.class))if(db.queryForObject("SELECT COUNT(*) FROM business_categories WHERE name=?",Integer.class,name)==0)db.update("INSERT INTO business_categories(name) VALUES(?)",name);
 }
}
