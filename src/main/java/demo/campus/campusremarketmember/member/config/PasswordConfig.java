package demo.campus.campusremarketmember.member.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** 비밀번호 암호화 도구를 스프링에 등록한다. (Spring Security 전체가 아니라 암호화 부품만 사용) */
@Configuration
public class PasswordConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // 같은 비밀번호도 매번 다른 값으로 저장되고, 되돌릴 수 없는 방식
    }
}
