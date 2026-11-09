package com.campus.remarket;
import com.campus.remarket.service.*;
import com.campus.remarket.controller.AccountController;
import demo.campus.campusremarketmember.member.entity.Member;
import demo.campus.campusremarketmember.member.repository.MemberRepository;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
@SpringBootTest @Transactional class LocationAccountTests {
 @Autowired MemberRepository members;@Autowired PasswordEncoder passwords;
 @AfterEach void clear(){AccountService.REVOKED.clear();}
 AccountService service(JavaMailSender sender,boolean configured){var beans=new StaticListableBeanFactory();beans.addBean("sender",sender);var e=new MockEnvironment();if(configured)e.withProperty("spring.mail.host","mail.example.test").withProperty("campus.notifications.mail-from","noreply@example.test");return new AccountService(members,passwords,beans.getBeanProvider(JavaMailSender.class),e);}
 Member owner(){return members.saveAndFlush(Member.owner("recoveryowner",passwords.encode("original123!"),"인증테스트","recovery@example.test","010-1234-5678","555-44-12345"));}
 @Test void passwordChangeRequiresOldPasswordAndRevokesExistingSessions(){var m=owner();var s=service(mock(JavaMailSender.class),false);assertThrows(Exception.class,()->s.change(m.getId(),"wrong","changed123!"));assertTrue(passwords.matches("original123!",members.findById(m.getId()).orElseThrow().getPassword()));s.change(m.getId(),"original123!","changed123!");assertTrue(passwords.matches("changed123!",members.findById(m.getId()).orElseThrow().getPassword()));assertTrue(AccountService.REVOKED.containsKey(m.getId()));}
 @Test void recoveryCodeIsMailedNotReturnedAndConsumedOnce(){owner();var sender=mock(JavaMailSender.class);var s=service(sender,true);var c=new AccountController(s);var request=new MockHttpServletRequest();var response=c.code(new AccountController.Request("recovery@example.test","ID",null,null,null),request);assertEquals(Set.of("message"),response.keySet());var capture=org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);verify(sender).send(capture.capture());String code=capture.getValue().getText().replaceAll("(?s).*인증번호: (\\d{6}).*","$1");var found=c.verify(new AccountController.Request(null,"ID",code,null,null),request);assertEquals("recoveryowner",found.get("loginId"));assertThrows(Exception.class,()->c.verify(new AccountController.Request(null,"ID",code,null,null),request));}
 @Test void resetNeedsCorrectUnexpiredCodeAndMailConfiguration(){var m=owner();var sender=mock(JavaMailSender.class);assertThrows(Exception.class,()->service(sender,false).issue("recovery@example.test","RESET",null));var s=service(sender,true);var controller=new AccountController(s);var request=new MockHttpServletRequest();controller.code(new AccountController.Request("recovery@example.test","RESET",null,null,null),request);var challenge=(AccountService.Challenge)request.getSession().getAttribute("accountChallenge");assertFalse(s.matches(challenge,"wrong","RESET"));assertFalse(s.matches(new AccountService.Challenge(challenge.email(),"RESET",challenge.code(),0,0,0),challenge.code(),"RESET"));controller.verify(new AccountController.Request(null,"RESET",challenge.code(),null,"newSecret123!"),request);assertTrue(passwords.matches("newSecret123!",members.findById(m.getId()).orElseThrow().getPassword()));}
 @Test void roadAndParcelAddressUseKakaoXYAndMissingKeyReturnsNoInventedPosition()throws Exception{var noKey=new AddressLocator(new MockEnvironment(),JsonMapper.builder().build());assertNull(noKey.find("인천 미추홀구 용현동 253"));var server=com.sun.net.httpserver.HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);var queries=new ArrayList<String>();server.createContext("/search",e->{queries.add(URLDecoder.decode(e.getRequestURI().getRawQuery(),StandardCharsets.UTF_8));assertEquals("KakaoAK test-key",e.getRequestHeaders().getFirst("Authorization"));byte[] bytes="{\"documents\":[{\"address_type\":\"REGION_ADDR\",\"x\":\"126.6537\",\"y\":\"37.4501\",\"address_name\":\"인천 미추홀구 용현동 253\"}]}".getBytes(StandardCharsets.UTF_8);e.sendResponseHeaders(200,bytes.length);e.getResponseBody().write(bytes);e.close();});server.start();try{var locator=new AddressLocator(new MockEnvironment().withProperty("campus.geocoding.kakao-key","test-key").withProperty("campus.geocoding.url","http://127.0.0.1:"+server.getAddress().getPort()+"/search"),JsonMapper.builder().build());for(String a:List.of("인천 미추홀구 인하로 100","인천 미추홀구 용현동 253")){var p=locator.find(a);assertNotNull(p);assertEquals(37.4501,p.latitude().doubleValue());assertEquals(126.6537,p.longitude().doubleValue());}assertTrue(queries.get(1).contains("용현동"));}finally{server.stop(0);}}
}
