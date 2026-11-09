package com.campus.remarket.controller;
import java.util.Map;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.*;
// Multipart parsing can fail before a controller is selected, so this handler is unscoped.
@Order(-2) @RestControllerAdvice
public class SignupUploadErrorAdvice {
    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<Map<String,String>> multipart(MultipartException error) {
        boolean large=error instanceof MaxUploadSizeExceededException;
        return ResponseEntity.status(large?413:400).body(Map.of("message",large?"사진은 5MB 이하만 올릴 수 있어요.":"첨부 파일을 확인한 뒤 다시 시도해 주세요."));
    }
}
