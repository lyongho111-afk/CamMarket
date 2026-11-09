package com.campus.remarket.config;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.resource.TransformedResource;
import java.nio.charset.StandardCharsets;
@Configuration
public class MemberPageConfig implements WebMvcConfigurer {
    @Override public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/login.html","/signup.html")
            .addResourceLocations("classpath:/static/").resourceChain(false)
            .addTransformer((request,resource,chain)->{
                var original=chain.transform(request,resource);
                String html;
                try(var input=original.getInputStream()){html=new String(input.readAllBytes(),StandardCharsets.UTF_8);}
                String extra=request.getRequestURI().endsWith("/signup.html")?"<script src=\"/signup-entry.js?v=20261102-2\"></script>":"";
                extra+="<link rel=\"stylesheet\" href=\"/ux.css\"><script src=\"/vendor/leaflet.js\"></script><link rel=\"stylesheet\" href=\"/vendor/leaflet.css\"><script src=\"/location-ui.js\"></script><script src=\"/account-ui.js\"></script><script src=\"/student-ux.js\"></script><script src=\"/category-select.js\"></script>";
                return new TransformedResource(original,html.replace("</body>",extra+"</body>").getBytes(StandardCharsets.UTF_8));
            });
        registry.addResourceHandler("/owner/**").addResourceLocations("classpath:/portal-sites/owner/");
        registry.addResourceHandler("/admin/**").addResourceLocations("classpath:/portal-sites/admin/");
    }
}
