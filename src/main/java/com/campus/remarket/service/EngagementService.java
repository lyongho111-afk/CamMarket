package com.campus.remarket.service;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

/** One like per account/content; its private list replaces the previous bookmark feature. */
@Service
public class EngagementService {
    public record Target(String type,long id) {
        public Target {
            if(!Set.of("SALE","POST","SHOP","COMMENT").contains(type)||id<1)
                throw new IllegalArgumentException("콘텐츠 종류와 번호를 확인하세요.");
        }
    }
    public record State(String type,long id,long likeCount,boolean liked,boolean bookmarked,boolean available){}
    private final JdbcTemplate db;private final CampusScope scope;
    public EngagementService(JdbcTemplate db,CampusScope scope){this.db=db;this.scope=scope;}

    public List<State> states(String query,Long memberId){
        if(query==null||query.isBlank())return List.of();
        String[] parts=query.split(",",-1);if(parts.length>100)throw new IllegalArgumentException("한 번에 100개까지 조회할 수 있어요.");
        var targets=new LinkedHashSet<Target>();
        for(String part:parts){String[] pair=part.split(":",-1);if(pair.length!=2)throw new IllegalArgumentException("콘텐츠 번호를 확인하세요.");targets.add(new Target(pair[0],Long.parseLong(pair[1])));}
        var params=new ArrayList<Object>();StringJoiner where=new StringJoiner(" OR ");
        for(var t:targets){where.add("(t.target_type=? AND t.target_id=?)");params.add(t.type());params.add(t.id());}
        // One batch query, never trust client-provided counts or member identifiers.
        params.addFirst(memberId==null?-1L:memberId);params.addFirst(memberId==null?-1L:memberId);
        String visibility=scope.member()==null?"(t.target_type='SALE' OR ("+scope.sql()+"))":scope.sql();
        var found=db.query("SELECT t.target_type,t.target_id,COALESCE(SUM(CASE WHEN r.kind='LIKE' THEN 1 ELSE 0 END),0) likes,COALESCE(MAX(CASE WHEN r.kind='LIKE' AND r.member_id=? THEN 1 ELSE 0 END),0) liked,COALESCE(MAX(CASE WHEN r.kind='LIKE' AND r.member_id=? THEN 1 ELSE 0 END),0) bookmarked FROM engagement_targets t LEFT JOIN content_reactions r ON r.target_type=t.target_type AND r.target_id=t.target_id WHERE ("+where+")"+" AND ("+visibility+") GROUP BY t.target_type,t.target_id",
            (rs,i)->new State(rs.getString(1),rs.getLong(2),rs.getLong(3),rs.getInt(4)==1,rs.getInt(5)==1,true),params.toArray());
        Map<Target,State> index=new HashMap<>();found.forEach(s->index.put(new Target(s.type(),s.id()),s));
        return targets.stream().map(t->index.getOrDefault(t,new State(t.type(),t.id(),0,false,false,false))).toList();
    }

    @Transactional
    public State set(long memberId,Target target,String kind,boolean active){
        if(!Set.of("LIKE","BOOKMARK").contains(kind))throw new IllegalArgumentException("반응 종류를 확인하세요.");
        kind="LIKE"; // Old bookmark clients now change the same canonical like.
        // Serialize writes from the same account, including simultaneous duplicate requests.
        db.queryForObject("SELECT id FROM member WHERE id=? FOR UPDATE",Long.class,memberId);
        if(active){
            if(db.queryForObject("SELECT COUNT(*) FROM engagement_targets t WHERE target_type=? AND target_id=? AND "+scope.sql()+"",Integer.class,target.type(),target.id())!=1)
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,"삭제되었거나 공개되지 않은 콘텐츠예요.");
            if(db.queryForObject("SELECT COUNT(*) FROM content_reactions WHERE member_id=? AND target_type=? AND target_id=? AND kind=?",Integer.class,memberId,target.type(),target.id(),kind)==0)
                db.update("INSERT INTO content_reactions(member_id,target_type,target_id,kind) VALUES(?,?,?,?)",memberId,target.type(),target.id(),kind);
        }else db.update("DELETE FROM content_reactions WHERE member_id=? AND target_type=? AND target_id=? AND kind=?",memberId,target.type(),target.id(),kind);
        return states(target.type()+":"+target.id(),memberId).getFirst();
    }

    @Transactional
    public void consolidateBookmarks(){
        db.update("INSERT INTO content_reactions(member_id,target_type,target_id,kind,created_at) SELECT b.member_id,b.target_type,b.target_id,'LIKE',b.created_at FROM content_reactions b WHERE b.kind='BOOKMARK' AND NOT EXISTS (SELECT 1 FROM content_reactions l WHERE l.member_id=b.member_id AND l.target_type=b.target_type AND l.target_id=b.target_id AND l.kind='LIKE')");
        db.update("DELETE FROM content_reactions WHERE kind='BOOKMARK'");
    }

    public Map<String,Object> bookmarks(long memberId,int page,String type){
        if(page<0||page>100000)throw new IllegalArgumentException("페이지 번호를 확인하세요.");
        if(!type.equals("ALL"))new Target(type,1);
        String filter=" FROM content_reactions r JOIN engagement_targets t ON r.target_type=t.target_type AND r.target_id=t.target_id WHERE r.member_id=? AND r.kind='LIKE'"+" AND ("+scope.sql()+")"+(type.equals("ALL")?"":" AND r.target_type=?");
        List<Object> args=new ArrayList<>();args.add(memberId);if(!type.equals("ALL"))args.add(type);
        long total=db.queryForObject("SELECT COUNT(*)"+filter,Long.class,args.toArray());
        args.add(page*24);
        var items=db.query("SELECT t.*,r.created_at"+filter+" ORDER BY r.created_at DESC,r.target_type,r.target_id LIMIT 24 OFFSET ?",(rs,i)->{
            Map<String,Object> m=new LinkedHashMap<>();m.put("type",rs.getString("target_type"));m.put("id",rs.getLong("target_id"));m.put("title",rs.getString("title"));m.put("subtitle",rs.getString("subtitle"));m.put("price",rs.getObject("price"));m.put("photo",rs.getString("photo"));m.put("parentId",rs.getLong("parent_id"));m.put("savedAt",rs.getTimestamp("created_at").toLocalDateTime());return m;
        },args.toArray());
        return Map.of("items",items,"total",total,"page",page,"pageSize",24);
    }
}
