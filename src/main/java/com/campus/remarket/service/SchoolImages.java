package com.campus.remarket.service;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
@Service public class SchoolImages {
 private final Path root;public SchoolImages(@Value("${campus.upload-dir:./uploads}")String dir){root=Path.of(dir,"schools").toAbsolutePath().normalize();}
 public String save(MultipartFile file)throws java.io.IOException{if(file==null||file.isEmpty()||file.getSize()>5*1024*1024)throw new IllegalArgumentException("학교 이미지는 5MB 이하 JPG 또는 PNG 파일을 선택하세요.");String original=Objects.toString(file.getOriginalFilename(),"").toLowerCase(Locale.ROOT);String ext=original.endsWith(".png")?"png":original.endsWith(".jpg")||original.endsWith(".jpeg")?"jpg":null;if(ext==null)throw new IllegalArgumentException("JPG 또는 PNG 이미지만 등록할 수 있습니다.");try(var image=ImageIO.createImageInputStream(file.getInputStream())){var readers=ImageIO.getImageReaders(image);if(!readers.hasNext())throw new IllegalArgumentException("올바른 이미지 파일이 아닙니다.");var reader=readers.next();try{reader.setInput(image);String format=reader.getFormatName();if(!(ext.equals("png")&&format.equalsIgnoreCase("png")||ext.equals("jpg")&&format.equalsIgnoreCase("JPEG"))||(long)reader.getWidth(0)*reader.getHeight(0)>40000000)throw new IllegalArgumentException("이미지 형식과 크기를 확인하세요.");}finally{reader.dispose();}}Files.createDirectories(root);String name=UUID.randomUUID()+"."+ext;try(var stream=file.getInputStream()){Files.copy(stream,root.resolve(name));}return name;}
 public String validate(String name){if(name==null||name.isBlank())return null;if(!name.matches("[0-9a-f-]{36}\\.(jpg|png)")||!Files.isRegularFile(root.resolve(name)))throw new IllegalArgumentException("학교 이미지 파일을 다시 업로드하세요.");return name;}
 public Resource load(String name){return name.matches("[0-9a-f-]{36}\\.(jpg|png)")&&Files.isRegularFile(root.resolve(name))?new FileSystemResource(root.resolve(name)):null;}
}
