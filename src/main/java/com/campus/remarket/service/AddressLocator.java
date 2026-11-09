package com.campus.remarket.service;
import java.math.BigDecimal;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;
@Service public class AddressLocator {
 public record Point(BigDecimal latitude,BigDecimal longitude,String address){}
 private final Environment env;private final ObjectMapper json;
 private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
 public AddressLocator(Environment env,ObjectMapper json){this.env=env;this.json=json;}
 public boolean ready(){return !env.getProperty("campus.geocoding.kakao-key","").isBlank();}
 public Point find(String address){
  if(address==null||address.isBlank()||address.length()>200)throw new IllegalArgumentException("주소를 200자 이내로 입력하세요.");
  if(!ready())return null;
  try{var req=HttpRequest.newBuilder(URI.create(env.getProperty("campus.geocoding.url","https://dapi.kakao.com/v2/local/search/address.json")+"?query="+URLEncoder.encode(address.trim(),java.nio.charset.StandardCharsets.UTF_8))).timeout(Duration.ofSeconds(5)).header("Authorization","KakaoAK "+env.getProperty("campus.geocoding.kakao-key")).GET().build();
   var response=client.send(req,HttpResponse.BodyHandlers.ofString());if(response.statusCode()!=200)return null;
   var docs=json.readTree(response.body()).path("documents");for(var doc:docs){String type=doc.path("address_type").asText("");if(!Set.of("ROAD_ADDR","REGION_ADDR").contains(type))continue;var p=new Point(new BigDecimal(doc.path("y").asText()),new BigDecimal(doc.path("x").asText()),doc.path("address_name").asText());validate(p.latitude(),p.longitude());return p;}return null;
  }catch(Exception e){return null;}
 }
 public static void validate(BigDecimal lat,BigDecimal lng){if((lat==null)!=(lng==null)||lat!=null&&(lat.abs().doubleValue()>90||lng.abs().doubleValue()>180))throw new IllegalArgumentException("위도·경도를 함께 올바르게 지정하세요.");}
}
