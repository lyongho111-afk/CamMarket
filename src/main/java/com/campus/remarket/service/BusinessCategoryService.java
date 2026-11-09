package com.campus.remarket.service;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
@Service public class BusinessCategoryService {
 public record Category(long id,String name,long version,long shops){}
 private final JdbcTemplate db;public BusinessCategoryService(JdbcTemplate db){this.db=db;}
 public List<Category> list(){return db.query("SELECT c.*, (SELECT COUNT(*) FROM shops s WHERE s.busi_kind=c.name) AS shop_count FROM business_categories c ORDER BY c.category_id",(r,i)->new Category(r.getLong("category_id"),r.getString("name"),r.getLong("version"),r.getLong("shop_count")));}
 public void require(String name){if(name==null||db.query("SELECT category_id FROM business_categories WHERE name=? FOR UPDATE",(r,i)->r.getLong(1),name.strip()).isEmpty())throw new IllegalArgumentException("등록된 업종을 선택해 주세요. 목록을 새로고침하세요.");}
 @Transactional public void save(Long id,String name,Long version){if(name==null||name.strip().isEmpty()||name.strip().length()>50)throw new IllegalArgumentException("업종명은 1~50자로 입력하세요.");name=name.strip();if(id==null){db.update("INSERT INTO business_categories(name) VALUES(?)",name);return;}var old=db.queryForList("SELECT * FROM business_categories WHERE category_id=? FOR UPDATE",id);if(old.isEmpty())throw error(404,"업종이 없습니다.");if(!Objects.equals(((Number)old.getFirst().get("VERSION")).longValue(),version))throw error(409,"업종이 변경되었습니다. 새로고침하세요.");String previous=(String)old.getFirst().get("NAME");db.update("UPDATE business_categories SET name=?,version=version+1 WHERE category_id=?",name,id);db.update("UPDATE shops SET busi_kind=?,version=version+1 WHERE busi_kind=?",name,previous);}
 @Transactional public void delete(long id,Long version){var old=db.queryForList("SELECT * FROM business_categories WHERE category_id=? FOR UPDATE",id);if(old.isEmpty())throw error(404,"업종이 없습니다.");if(!Objects.equals(((Number)old.getFirst().get("VERSION")).longValue(),version))throw error(409,"업종이 변경되었습니다. 새로고침하세요.");if(db.queryForObject("SELECT COUNT(*) FROM shops WHERE busi_kind=?",Long.class,old.getFirst().get("NAME"))>0)throw error(409,"가게에서 사용 중인 업종은 삭제할 수 없습니다. 가게 업종을 먼저 변경하세요.");db.update("DELETE FROM business_categories WHERE category_id=?",id);}
 private ResponseStatusException error(int code,String text){return new ResponseStatusException(HttpStatus.valueOf(code),text);}
}
