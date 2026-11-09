package demo.campus.campusremarketmember.member.entity;

import jakarta.persistence.*;

/** 학교 정보. 학생 회원이 가입할 때 목록에서 고른다. */
@Entity
public class School {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 번호 자동 증가 (Product와 같은 방식)
    private Long id;

    @Column(nullable = false, unique = true, length = 50) // 같은 학교가 두 번 등록되지 않게 막음
    private String name;                                  // 예: 인하대학교

    @Column(nullable = false, length = 50)
    private String region;                                // 예: 인천 미추홀구

    @Column(nullable = false, length = 100)
    private String emailDomain;                           // 예: "inha.edu" 또는 여러 개면 "inha.edu,inha.ac.kr" (쉼표로 구분)
    @Column(length=200) private String address;
    @Column(precision=10,scale=7) private java.math.BigDecimal latitude;
    @Column(precision=10,scale=7) private java.math.BigDecimal longitude;
    @Column(length=100) private String photo;
    @Column(length=100) private String mapImage;
    @Column(length=20,unique=true) private String campusCode;
    @Version private Long version=0L;

    protected School() {}                                 // JPA가 DB에서 꺼낼 때 쓰는 기본 생성자 (밖에서는 못 쓰게 protected)

    public School(String name, String region, String emailDomain) { // 새 학교를 만들 때 쓰는 생성자
        this.name = name;
        this.region = region;
        this.emailDomain = emailDomain;
    }

    public boolean allowsEmail(String email) {            // 이 학교 웹메일인지 확인 (예: abc@inha.edu)
        if (email == null) return false;
        String mail = email.toLowerCase();
        for (String domain : emailDomain.split(",")) {    // 도메인이 여러 개면 하나씩 비교
            if (mail.endsWith("@" + domain.trim())) return true; // 하나라도 맞으면 통과
        }
        return false;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getRegion() { return region; }
    public String getEmailDomain() { return emailDomain; }
    public String getAddress(){return address;}
    public java.math.BigDecimal getLatitude(){return latitude;}
    public java.math.BigDecimal getLongitude(){return longitude;}
    public String getPhoto(){return photo;}
    public String getMapImage(){return mapImage;}
    public String getCampusCode(){return campusCode;}
    public Long getVersion(){return version;}
    public void campus(String code){campusCode=code;}
    public void update(String name,String region,String domain,String address,java.math.BigDecimal lat,java.math.BigDecimal lng,String photo,String mapImage){this.name=name;this.region=region;emailDomain=domain;this.address=address;latitude=lat;longitude=lng;this.photo=photo;this.mapImage=mapImage;}
}
