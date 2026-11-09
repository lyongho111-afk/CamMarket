package com.campus.remarket.config;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/** 팀원 회원 소스의 package/import를 바꾸지 않고 현재 애플리케이션에 등록한다. */
@Configuration
@ComponentScan("demo.campus.campusremarketmember.member")
@EntityScan("demo.campus.campusremarketmember.member.entity")
@EnableJpaRepositories("demo.campus.campusremarketmember.member.repository")
public class MemberIntegrationConfig {}
