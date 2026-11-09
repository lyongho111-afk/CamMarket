package portal.campus;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class PortalGatewayController {
    private final URI backend;private final String site;private final String cookie;
    private final URI publicEntry;
    private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build();
    public PortalGatewayController(@Value("${portal.backend:http://127.0.0.1:8080}")String backend,@Value("${portal.site:owner}")String site,@Value("${portal.public-base:http://localhost:8080}")String publicBase){this.backend=URI.create(backend);this.site=site;this.cookie=site.equals("owner")?"CAMPUS_OWNER":"CAMPUS_ADMIN";this.publicEntry=URI.create(publicBase.replaceAll("/+$","")+"/"+site+"/");}
    @GetMapping({"/","/index.html"}) public ResponseEntity<Void> entry(){return ResponseEntity.status(302).header("Cache-Control","no-store").location(publicEntry).build();}
    @RequestMapping("/api/**") public ResponseEntity<byte[]> api(HttpServletRequest request) throws Exception {
        boolean reading=Set.of("GET","HEAD","OPTIONS").contains(request.getMethod());
        String origin=request.getHeader("Origin");
        if(!reading&&origin!=null&&!origin.equals(request.getScheme()+"://"+request.getHeader("Host")))return json(403,"다른 사이트에서는 변경할 수 없습니다.");
        if("cross-site".equals(request.getHeader("Sec-Fetch-Site"))&&!reading)return json(403,"전용 사이트에서 다시 제출해 주세요.");
        String path=request.getRequestURI();if(path.contains("..")||path.contains("\\"))return json(400,"잘못된 주소입니다.");
        if(site.equals("owner")&&path.startsWith("/api/admin/")||site.equals("admin")&&path.startsWith("/api/owner/"))return json(403,"다른 사이트의 관리 기능입니다.");
        URI target=backend.resolve(path+(request.getQueryString()==null?"":"?"+request.getQueryString()));
        if(!target.getAuthority().equals(backend.getAuthority()))return json(400,"잘못된 주소입니다.");
        byte[] body=request.getInputStream().readNBytes(1_048_577);if(body.length>1_048_576)return json(413,"요청 크기가 너무 큽니다.");
        var builder=HttpRequest.newBuilder(target).timeout(Duration.ofSeconds(20)).method(request.getMethod(),body.length==0?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofByteArray(body));
        for(String header:new String[]{"Content-Type","Accept","X-CSRF-Token"})if(request.getHeader(header)!=null)builder.header(header,request.getHeader(header));
        if(request.getCookies()!=null)for(var value:request.getCookies())if(value.getName().equals(cookie))builder.header("Cookie","JSESSIONID="+value.getValue());
        try{
            var response=client.send(builder.build(),HttpResponse.BodyHandlers.ofByteArray());var headers=new HttpHeaders();
            response.headers().firstValue("content-type").ifPresent(value->headers.set("Content-Type",value));headers.set("Cache-Control","no-store");
            for(String value:response.headers().allValues("set-cookie"))if(value.startsWith("JSESSIONID=")){
                String scoped=cookie+value.substring("JSESSIONID".length());
                if(request.isSecure()&&!scoped.toLowerCase(java.util.Locale.ROOT).contains("; secure"))scoped+="; Secure";
                headers.add("Set-Cookie",scoped);
            }
            return new ResponseEntity<>(response.body(),headers,org.springframework.http.HttpStatusCode.valueOf(response.statusCode()));
        }catch(java.io.IOException e){return json(502,"공통 서버에 연결할 수 없습니다. 학생 사이트의 서버가 실행 중인지 확인해 주세요.");}
    }
    private ResponseEntity<byte[]> json(int status,String message){return ResponseEntity.status(status).header("Content-Type","application/json;charset=UTF-8").body(("{\"message\":\""+message+"\"}").getBytes(StandardCharsets.UTF_8));}
}
