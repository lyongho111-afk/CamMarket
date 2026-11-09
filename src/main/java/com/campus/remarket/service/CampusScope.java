package com.campus.remarket.service;
import demo.campus.campusremarketmember.member.entity.*;
import demo.campus.campusremarketmember.member.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Tenant identity is always resolved from the authenticated session, never request JSON/query fields. */
@Service public class CampusScope {
 private final MemberRepository members;private final SchoolRepository schools;
 public CampusScope(MemberRepository members,SchoolRepository schools){this.members=members;this.schools=schools;}
 public Member member(){if(!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes a))return null;var s=a.getRequest().getSession(false);if(s==null||!(s.getAttribute("loginMemberId") instanceof Long id))return null;return members.findById(id).orElse(null);}
 public boolean admin(){var m=member();return m!=null&&m.isAdmin();}
 public School school(){var m=member();if(m!=null){if(m.isManager())m=members.findById(m.getOwnerId()).orElse(null);if(m!=null&&m.getSchool()!=null){if(m.getSchool().getCampusCode()==null)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"현재 서비스 대상 학교가 아닙니다.");return m.getSchool();}if(m!=null&&!m.isAdmin())throw new ResponseStatusException(HttpStatus.FORBIDDEN,"회원의 소속 학교 정보를 확인해 주세요.");}return schools.findByCampusCode("INHA").orElseGet(()->schools.findByName("인하대학교").orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"서비스 학교가 등록되지 않았습니다.")));}
 public long id(){return school().getId();}
 public boolean visible(Long schoolId){return admin()||schoolId!=null&&schoolId.equals(id());}
 public void require(Long schoolId){if(!visible(schoolId))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"같은 학교의 콘텐츠만 확인할 수 있습니다.");}
 public String sql(){return admin()?"1=1":"t.school_id="+id();}
}
