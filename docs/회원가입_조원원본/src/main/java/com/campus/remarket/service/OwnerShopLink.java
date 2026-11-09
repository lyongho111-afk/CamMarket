package com.campus.remarket.service;

import com.campus.remarket.dto.ShopDto;
import demo.campus.campusremarketmember.member.dto.OwnerSignupDto;
import demo.campus.campusremarketmember.member.entity.Member;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 사장 회원가입(JPA)과 같은 트랜잭션 안에서 MyBatis shops에 가게를 만들고 상점 사진을 저장한다.
 * StudentMemberLink(학생 ↔ student)와 같은 방식. 여기서 에러가 나면 사장 회원가입도 함께 취소된다.
 */
@Service
public class OwnerShopLink {

    private static final Set<String> KINDS = Set.of("카페", "음식점", "주점", "스터디카페", "인쇄·문구", "기타"); // 업종 목록 (화면 select와 같게)
    private static final Set<String> IMAGE_EXT = Set.of("jpg", "jpeg", "png", "webp", "gif");             // 허용 사진 확장자
    private static final Pattern SAVED_NAME =
            Pattern.compile("[0-9a-f\\-]{36}\\.(jpg|jpeg|png|webp|gif)");  // 우리가 만든 이름 모양만 꺼내 주기 (경로 조작 공격 차단)

    private final ShopService shops;          // 가게 담당 서비스 재사용 (가게명 검사·상태 기본값 포함)
    private final Path uploadDir;             // 사진 저장 폴더

    public OwnerShopLink(ShopService shops,
                         @Value("${campus.upload-dir:./uploads}") String dir) {   // 설정이 없으면 ./uploads
        this.shops = shops;
        this.uploadDir = Paths.get(dir, "shops").toAbsolutePath().normalize();     // 실제 저장 위치: ./uploads/shops
    }

    /** 사장 회원 저장 직후 호출. 새 가게 번호(shopId)를 돌려준다. */
    @Transactional
    public Long createShop(Member owner, OwnerSignupDto f, MultipartFile photo) {
        if (!owner.isOwner()) throw new IllegalStateException("사장 회원만 가게를 만들 수 있어요."); // 잘못 부르는 실수 방지
        String kind = trim(f.busiKind());
        if (!KINDS.contains(kind)) throw bad("업종을 선택해 주세요.");
        String address = required(f.address(), "가게 주소", 150);
        String detail = trim(f.addressDetail());
        if (detail.length() > 50) throw bad("상세 주소는 50자까지 입력할 수 있어요.");

        ShopDto dto = new ShopDto();
        dto.setShopname(required(f.shopname(), "상호", 50));                        // 필수
        dto.setBusiKind(kind);
        dto.setShopphone(optional(f.shopphone(), "가게 전화번호", 50));                // 선택 칸은 비어 있으면 null
        dto.setAddress(detail.isEmpty() ? address : address + " " + detail);          // 도로명 + 상세주소 합치기
        dto.setBusitime(optional(f.busitime(), "영업시간", 20));
        dto.setOffday(optional(f.offday(), "휴무일", 20));
        dto.setBmenu(optional(f.bmenu(), "대표 메뉴", 400));
        dto.setCeoname(owner.getName());               // 대표자 이름은 1단계 회원 정보에서 (두 번 입력 X)
        dto.setBusino(owner.getBusinessNo());          // 사업자번호도 회원 정보에서
        dto.setOwnerMemberId(owner.getId());           // 이 가게의 사장 = 방금 가입한 회원
        dto.setStatus("영업종료");                       // 승인 전이니까 영업종료로 시작 (checked는 DB 기본값 FALSE = 승인 대기)

        String saved = savePhoto(photo);               // 사진 저장 (없으면 null)
        dto.setPhoto(saved);
        try {
            shops.insert(dto);                         // MyBatis insert → useGeneratedKeys로 dto.shopId에 새 번호
        } catch (RuntimeException e) {
            deleteQuietly(saved);                      // DB 저장이 실패하면 방금 저장한 사진도 지움
            throw e;
        }
        return dto.getShopId();
    }

    /** 사진 파일 저장. 이름은 UUID + 확장자 (한글·공백·같은 이름 걱정 없음) */
    private String savePhoto(MultipartFile photo) {
        if (photo == null || photo.isEmpty()) return null;               // 사진은 선택
        String original = photo.getOriginalFilename() == null ? "" : photo.getOriginalFilename();
        String ext = original.contains(".")
                ? original.substring(original.lastIndexOf('.') + 1).toLowerCase() : "";   // 확장자만 꺼내기
        String type = photo.getContentType();
        if (!IMAGE_EXT.contains(ext) || type == null || !type.startsWith("image/"))      // 이름과 종류 둘 다 확인
            throw bad("사진은 jpg, png, webp, gif 파일만 올릴 수 있어요.");
        String savedName = UUID.randomUUID() + "." + ext;
        try {
            Files.createDirectories(uploadDir);                // 폴더가 없으면 만들기
            photo.transferTo(uploadDir.resolve(savedName));    // start16의 transferTo와 같음
        } catch (IOException e) {
            throw new UncheckedIOException("사진 저장에 실패했어요.", e);
        }
        return savedName;
    }

    /** 사진 꺼내기 (ShopPhotoController가 사용). 모양이 다르거나 없으면 null */
    public Resource loadPhoto(String name) {
        if (name == null || !SAVED_NAME.matcher(name).matches()) return null;   // ../ 같은 이상한 경로 차단
        Path file = uploadDir.resolve(name);
        return Files.exists(file) ? new FileSystemResource(file) : null;
    }

    private void deleteQuietly(String name) {
        if (name == null) return;
        try { Files.deleteIfExists(uploadDir.resolve(name)); } catch (IOException ignored) {}
    }
    private static String trim(String s) { return s == null ? "" : s.trim(); }
    private static String required(String s, String label, int max) {      // 필수 칸: 비었거나 너무 길면 400
        String v = trim(s);
        if (v.isEmpty()) throw bad(label + "을(를) 입력해 주세요.");
        if (v.length() > max) throw bad(label + "은(는) " + max + "자까지 입력할 수 있어요.");
        return v;
    }
    private static String optional(String s, String label, int max) {      // 선택 칸: 비었으면 null
        String v = trim(s);
        if (v.length() > max) throw bad(label + "은(는) " + max + "자까지 입력할 수 있어요.");
        return v.isEmpty() ? null : v;
    }
    private static ResponseStatusException bad(String m) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, m); }
}
