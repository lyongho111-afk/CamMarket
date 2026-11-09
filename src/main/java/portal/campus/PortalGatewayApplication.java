package portal.campus;
import java.util.Arrays;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;

/** Independent web process serving only its own frontend; database and authentication remain in the shared API. */
@SpringBootApplication(exclude=DataSourceAutoConfiguration.class)
public class PortalGatewayApplication {
    public static void main(String[] args){
        String site=Arrays.stream(args).filter(a->a.startsWith("--portal.site=")).map(a->a.substring(14)).findFirst().orElse("owner");
        if(!site.equals("owner")&&!site.equals("admin"))throw new IllegalArgumentException("portal.site must be owner or admin");
        System.setProperty("portal.site",site);System.setProperty("server.port",site.equals("owner")?"8081":"8082");
        System.setProperty("spring.application.name","campus-remarket-"+site);
        System.setProperty("spring.web.resources.static-locations","classpath:/portal-sites/"+site+"/");
        System.setProperty("spring.h2.console.enabled","false");System.setProperty("spring.sql.init.mode","never");
        SpringApplication.run(PortalGatewayApplication.class,args);
    }
}
