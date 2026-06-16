package project.plantly.domain.company.domesticRegion;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DomesticRegion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 법정동코드 10자리 (자연키). 시드의 ON CONFLICT (code) 대상이므로 UNIQUE 필수.
    @NotNull
    @Column(nullable = false, unique = true, length = 10)
    private String code;

    @NotNull
    @Column(nullable = false, length = 50)
    private String name;

    // SIDO(시도) / SIGUNGU(시군구). 컬럼에는 문자열로 저장 (시드 값과 동일).
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private RegionLevel level;

    // 부모 행정구역의 code (시도는 부모 없음 → null). id 가 아닌 code 값을 참조한다.
    @Column(length = 10)
    private String parentCode;

    // 노출 활성화 여부. 시드 SQL 은 active 를 생략하므로 DB 기본값(true)으로 채워진다.
    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean active;

    public DomesticRegion(String code, String name, RegionLevel level, String parentCode) {
        this.code = code;
        this.name = name;
        this.level = level;
        this.parentCode = parentCode;
        this.active = true;
    }

    public static DomesticRegion create(String code, String name, RegionLevel level, String parentCode) {
        return new DomesticRegion(code, name, level, parentCode);
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }
}
