package com.campus.remarket.controller;
import java.util.Map;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
@Order(0)
@RestControllerAdvice(assignableTypes={CatalogController.class,MemberProfileApiController.class,EngagementController.class,TradeReviewApiController.class,LocationController.class,AccountController.class,ProductApiController.class,BoardApiController.class,TradeApiController.class,ShopApiController.class,IntegrationApiController.class,MannerRatingApiController.class,com.campus.remarket.notification.NotificationController.class})
public class ApiErrorAdvice {
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<Map<String,String>> conflict(){return ResponseEntity.status(409).body(Map.of("message","이름 또는 서비스 학교가 중복되거나 다른 데이터에서 사용 중입니다."));}
    @ExceptionHandler({IllegalArgumentException.class,IllegalStateException.class})
    public ResponseEntity<Map<String,String>> invalid(RuntimeException error){return ResponseEntity.badRequest().body(Map.of("message",error.getMessage()));}
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String,String>> status(ResponseStatusException error){return ResponseEntity.status(error.getStatusCode()).body(Map.of("message",error.getReason()==null?"요청을 처리하지 못했습니다.":error.getReason()));}
}
