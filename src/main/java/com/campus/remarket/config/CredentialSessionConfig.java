package com.campus.remarket.config;
import com.campus.remarket.service.AccountService;
import jakarta.servlet.http.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.*;
@Configuration public class CredentialSessionConfig implements WebMvcConfigurer {
 public void addInterceptors(InterceptorRegistry registry){registry.addInterceptor(new HandlerInterceptor(){public boolean preHandle(HttpServletRequest r,HttpServletResponse response,Object handler){var s=r.getSession(false);if(s!=null&&s.getAttribute("loginMemberId") instanceof Long id){long changed=AccountService.REVOKED.getOrDefault(id,0L);Object started=s.getAttribute("credentialStart");if(changed>0&&(!(started instanceof Long n)||n<changed))s.invalidate();}return true;}}).addPathPatterns("/api/**");}
}
