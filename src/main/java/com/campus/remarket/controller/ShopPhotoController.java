package com.campus.remarket.controller;

import com.campus.remarket.service.OwnerShopLink;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** 상점 사진 보여 주기: <img src="/api/shops/photo/{저장이름}"> (start16의 /displayFile과 같은 역할) */
@RestController
public class ShopPhotoController {

    private final OwnerShopLink link;

    public ShopPhotoController(OwnerShopLink link) { this.link = link; }

    @GetMapping("/api/shops/photo/{name}")
    public ResponseEntity<Resource> photo(@PathVariable String name) {
        Resource r = link.loadPhoto(name);
        if (r == null) return ResponseEntity.notFound().build();          // 없으면 404
        MediaType type = MediaTypeFactory.getMediaType(r)                   // 확장자로 image/jpeg 등 판단
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok().contentType(type).body(r);
    }
}
