package com.campus.remarket.portal;
import java.util.Map;
import java.util.Properties;
import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
@RestController
public class PortalDirectory {
    private final Path file;
    public PortalDirectory(@Value("${campus.portal.links-file:config/portal-links.properties}")String path){file=Path.of(path);}
    @GetMapping("/api/portals") public Map<String,String> links(){var values=new Properties();if(Files.isRegularFile(file))try(var input=Files.newInputStream(file)){values.load(input);}catch(IOException ignored){}return Map.of("student",values.getProperty("student","/"),"owner",values.getProperty("owner","/owner/"),"admin",values.getProperty("admin","/admin/"));}
}
