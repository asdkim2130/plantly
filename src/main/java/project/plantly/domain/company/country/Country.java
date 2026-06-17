package project.plantly.domain.company.country;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Country {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ISO 3166-1 alpha-2 (자연키). 시드의 ON CONFLICT (code) 대상이므로 UNIQUE 필수.
    @NotNull
    @Column(nullable = false, unique = true, length = 2)
    private String code;

    // ISO 3166-1 alpha-3
    @NotNull
    @Column(nullable = false, length = 3)
    private String alpha3;

    // ISO 3166-1 numeric (선행 0 보존 위해 문자열). 코소보(XK)처럼 미배정 시 null.
    @Column(length = 3)
    private String numericCode;

    @NotNull
    @Column(nullable = false, length = 80)
    private String nameKo;

    @NotNull
    @Column(nullable = false, length = 80)
    private String nameEn;

    // 대륙 구분. 컬럼에는 문자열로 저장 (시드 값과 동일).
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Continent continent;

    public Country(String code, String alpha3, String numericCode,
                   String nameKo, String nameEn, Continent continent) {
        this.code = code;
        this.alpha3 = alpha3;
        this.numericCode = numericCode;
        this.nameKo = nameKo;
        this.nameEn = nameEn;
        this.continent = continent;
    }

    public static Country create(String code, String alpha3, String numericCode,
                                 String nameKo, String nameEn, Continent continent) {
        return new Country(code, alpha3, numericCode, nameKo, nameEn, continent);
    }
}
