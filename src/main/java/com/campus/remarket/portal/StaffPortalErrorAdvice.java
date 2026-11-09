package com.campus.remarket.portal;
import java.util.Map;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
@Order(-2)
@RestControllerAdvice(assignableTypes=StaffPortalController.class)
public class StaffPortalErrorAdvice {
    @ExceptionHandler(ResponseStatusException.class) public ResponseEntity<Map<String,String>> status(ResponseStatusException e){return ResponseEntity.status(e.getStatusCode()).body(Map.of("message",e.getReason()==null?"요청을 처리하지 못했습니다.":e.getReason()));}
}
