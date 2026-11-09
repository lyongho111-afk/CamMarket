package demo.campus.campusremarketmember.member.dto;

/** 회원가입 화면(사장님)에서 받는 값: 1단계 계정 + 2단계 가게. 사진은 Controller에서 MultipartFile로 따로 받는다. */
public record OwnerSignupDto(
        // ===== 1단계: 계정 =====
        String loginId,          // 아이디
        String password,         // 비밀번호
        String name,             // 대표자 이름
        String phone,            // 대표자 전화번호
        String email,            // 이메일
        String businessNo,       // 사업자등록번호 (UNIQUE)
        // ===== 2단계: 가게 =====
        String shopname,         // 상호 (필수)
        String busiKind,         // 업종 (필수: 카페, 음식점 ...)
        String shopphone,        // 가게 전화번호 (선택)
        String address,          // 도로명 주소 (필수, 주소 검색으로 채움)
        String addressDetail,    // 상세 주소 (선택)
        String busitime,         // 영업시간 (선택)
        String offday,           // 휴무일 (선택)
        String bmenu,            // 대표 메뉴 (선택)
        Long schoolId,
        java.math.BigDecimal latitude,
        java.math.BigDecimal longitude
) {
    public OwnerSignupDto(String loginId,String password,String name,String phone,String email,String businessNo,String shopname,String busiKind,String shopphone,String address,String addressDetail,String busitime,String offday,String bmenu){this(loginId,password,name,phone,email,businessNo,shopname,busiKind,shopphone,address,addressDetail,busitime,offday,bmenu,null,null,null);}
}
