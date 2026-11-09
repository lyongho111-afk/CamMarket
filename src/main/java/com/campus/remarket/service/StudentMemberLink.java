package com.campus.remarket.service;
import com.campus.remarket.dao.StudentDao;
import com.campus.remarket.dto.StudentDto;
import demo.campus.campusremarketmember.member.entity.Member;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

/** 회원가입/로그인의 JPA 트랜잭션 안에서 같은 DataSource의 MyBatis 학생 레코드를 연결한다. */
@Service
public class StudentMemberLink {
    private final StudentDao students;
    public StudentMemberLink(StudentDao students){this.students=students;}
    @Transactional public void link(Member member){
        if(!member.isStudent())return;
        if(students.forMember(member.getId())!=null)return;
        // Keep legacy Inha identities; other campuses use the globally unique member ID internally.
        String unisno=member.getSchool()!=null&&!"INHA".equals(member.getSchool().getCampusCode())?"member:"+member.getId():member.getLoginId();
        var student=students.lockOne(unisno);
        if(student==null){
            var fresh=new StudentDto();fresh.setUnisno(unisno);fresh.setMemberId(member.getId());
            try{students.insert(fresh);}catch(DuplicateKeyException e){throw collision();}
        }else if(!member.getId().equals(student.getMemberId())){
            // student.unisno is a global PK: do not grant another school's same-number account ownership.
            throw collision();
        }
        // Existing temperature is never reset on repeated login.
    }
    public String key(Member member){var s=students.forMember(member.getId());if(s==null)throw new IllegalStateException("학생 회원 연결을 확인해 주세요.");return s.getUnisno();}
    private ResponseStatusException collision(){return new ResponseStatusException(HttpStatus.CONFLICT,
        "이 학번은 다른 회원 또는 기존 학생 정보에 연결되어 있어요. 학교별 학번 정책을 확인해 주세요.");}
}
