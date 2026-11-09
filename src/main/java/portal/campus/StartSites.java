package portal.campus;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;

/** One-click local launcher. Only processes created by this launcher are stopped on Ctrl+C. */
public class StartSites {
    public static void main(String[] args)throws Exception{
        for(int port:new int[]{8080,8081,8082})try(var socket=new Socket()){
            socket.connect(new InetSocketAddress("127.0.0.1",port),300);
            System.err.println("포트 "+port+"이 사용 중입니다. 기존 서버를 종료한 뒤 다시 실행해 주세요.");return;
        }catch(java.io.IOException available){}
        Path jar=Path.of("app/campus-remarket.jar").toRealPath(),root=jar.getParent().getParent(),logs=root.resolve("logs");Files.createDirectories(logs);
        String java=Path.of(System.getProperty("java.home"),"bin",System.getProperty("os.name").startsWith("Windows")?"java.exe":"java").toString();
        List<Process> owned=new ArrayList<>();Runtime.getRuntime().addShutdownHook(new Thread(()->owned.forEach(Process::destroy)));
        try{
            var common=new ArrayList<>(List.of(java,"-Xmx384m","-Duser.timezone=Asia/Seoul","-jar",jar.toString(),"--server.port=8080"));
            owned.add(new ProcessBuilder(common).directory(root.toFile()).redirectErrorStream(true).redirectOutput(logs.resolve("student.log").toFile()).start());
            ready("http://127.0.0.1:8080/api/session",owned.getFirst());
            for(String site:List.of("owner","admin")){
                int port=site.equals("owner")?8081:8082;
                var command=List.of(java,"-Xmx256m","-Duser.timezone=Asia/Seoul","-Dloader.main=portal.campus.PortalGatewayApplication","-cp",jar.toString(),"org.springframework.boot.loader.launch.PropertiesLauncher","--portal.site="+site,"--server.port="+port,"--portal.backend=http://127.0.0.1:8080");
                var process=new ProcessBuilder(command).directory(root.toFile()).redirectErrorStream(true).redirectOutput(logs.resolve(site+".log").toFile()).start();owned.add(process);ready("http://127.0.0.1:"+port+"/",process);
            }
            System.out.println("학생 사이트: http://localhost:8080/");System.out.println("사장·매니저센터: http://localhost:8080/owner/");System.out.println("관리자 사이트: http://localhost:8080/admin/");System.out.println("이 창에서 Ctrl+C를 누르면 이번에 시작한 세 사이트를 종료합니다.");
            for(Process process:owned){int code=process.waitFor();throw new IllegalStateException("사이트 서버가 종료되었습니다. 종료 코드: "+code+" (logs 폴더 확인)");}
        }finally{owned.forEach(Process::destroy);}
    }
    private static void ready(String url,Process process)throws Exception{
        var client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();long deadline=System.nanoTime()+Duration.ofSeconds(60).toNanos();
        while(System.nanoTime()<deadline){if(!process.isAlive())throw new IllegalStateException("서버 시작에 실패했습니다. logs 폴더를 확인해 주세요.");
            try{var result=client.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(2)).build(),HttpResponse.BodyHandlers.discarding());if(result.statusCode()==200||result.statusCode()==302)return;}catch(java.io.IOException ignored){}Thread.sleep(300);
        }throw new IllegalStateException("서버 시작 시간이 초과되었습니다. logs 폴더를 확인해 주세요.");
    }
}
