package com.campus.remarket.service;
import demo.campus.campusremarketmember.member.repository.MemberRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.security.*;
@Service public class AccountService {
 public static final Map<Long,Long> REVOKED=new ConcurrentHashMap<>();
 private final Map<String,Long> requests=new ConcurrentHashMap<>();
 private final MemberRepository members;private final PasswordEncoder passwords;private final ObjectProvider<JavaMailSender> mail;private final Environment env;
 public AccountService(MemberRepository members,PasswordEncoder passwords,ObjectProvider<JavaMailSender> mail,Environment env){this.members=members;this.passwords=passwords;this.mail=mail;this.env=env;}
 public boolean recoveryReady(){return mail.getIfAvailable()!=null&&!env.getProperty("campus.notifications.mail-from","").isBlank()&&!env.getProperty("spring.mail.host","").isBlank();}
 public boolean localDemo(){return env.matchesProfiles("local-demo");}
 public record Challenge(String email,String purpose,String code,long expires,int attempts,long issued) implements java.io.Serializable{}
 public Challenge issue(String email,String purpose,Challenge previous){
  email=email==null?"":email.trim().toLowerCase();if(email.length()>100||!email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")||!Set.of("ID","RESET").contains(purpose))throw error(400,"이메일과 요청 종류를 확인하세요.");
  long now=System.currentTimeMillis();if(previous!=null&&now-previous.issued()<60000)throw error(429,"1분 뒤 다시 요청하세요.");
  String from=env.getProperty("campus.notifications.mail-from","");if(mail.getIfAvailable()==null||from.isBlank()||env.getProperty("spring.mail.host","").isBlank())throw error(503,"계정 찾기용 이메일 서비스 설정이 필요합니다. 로그인한 경우 현재 비밀번호로 변경할 수 있습니다.");
  synchronized(requests){requests.entrySet().removeIf(e->now-e.getValue()>600000);if(now-requests.getOrDefault(email,0L)<60000)throw error(429,"1분 뒤 다시 요청하세요.");requests.put(email,now);}
  String code=String.format("%06d",new SecureRandom().nextInt(1000000));var found=members.findByEmail(email);
  if(found.isPresent()){var message=new SimpleMailMessage();message.setFrom(from);message.setTo(email);message.setSubject("[캠퍼스 리마켓] 계정 확인 인증번호");message.setText("인증번호: "+code+"\n5분 동안 유효하며 타인에게 알려주지 마세요.");try{mail.getObject().send(message);}catch(Exception e){throw error(503,"인증 메일을 보내지 못했습니다. 잠시 후 다시 시도하세요.");}}
  return new Challenge(email,purpose,code,now+300000,0,now);
 }
 public boolean matches(Challenge c,String code,String purpose){return c!=null&&c.expires()>System.currentTimeMillis()&&c.attempts()<5&&c.purpose().equals(purpose)&&MessageDigest.isEqual(c.code().getBytes(java.nio.charset.StandardCharsets.UTF_8),Objects.toString(code,"").getBytes(java.nio.charset.StandardCharsets.UTF_8));}
 public Map<String,Object> identify(String email){var m=members.findByEmail(email).orElseThrow(()->error(400,"계정 정보를 확인할 수 없습니다."));return Map.of("role",m.getRole().name(),"loginId",m.getLoginId(),"school",m.getSchool()==null?"사장·매니저·관리자":m.getSchool().getName());}
 @Transactional public void reset(String email,String password){var m=members.findByEmail(email).orElseThrow(()->error(400,"계정 정보를 확인할 수 없습니다."));validate(password);m.changePassword(passwords.encode(password));members.saveAndFlush(m);REVOKED.put(m.getId(),System.nanoTime());}
 @Transactional public void change(Long memberId,String oldPassword,String password){var m=members.findById(memberId).orElseThrow(()->error(401,"로그인하세요."));if(!passwords.matches(Objects.toString(oldPassword,""),m.getPassword()))throw error(400,"현재 비밀번호가 맞지 않습니다.");validate(password);m.changePassword(passwords.encode(password));members.saveAndFlush(m);REVOKED.put(memberId,System.nanoTime());}
 private void validate(String p){if(p==null||!p.matches("(?=.*[A-Za-z])(?=.*\\d).{8,64}"))throw error(400,"새 비밀번호는 영문과 숫자를 포함한 8~64자여야 합니다.");}
 private static ResponseStatusException error(int code,String message){return new ResponseStatusException(HttpStatus.valueOf(code),message);}
}
