package com.campus.remarket.controller;
import demo.campus.campusremarketmember.member.controller.MemberController;
import org.springframework.web.bind.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;
@Order(-1)
@RestControllerAdvice(assignableTypes={MemberController.class,MemberIntegrationController.class})
public class MemberErrorAdvice {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String,String>> status(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(Map.of("message",e.getReason()==null?"요청을 확인해 주세요.":e.getReason()));
    }
}
