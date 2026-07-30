package project.plantly.domain.company.certification;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import project.plantly.domain.company.category.CompanyChild;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Certification extends CompanyChild {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(nullable = false, unique = true)
    private String certificationName;

    @NotNull
    @Column(nullable = false, unique = true)
    private String slug;

    // 경영시스템 / 산업특화 / 시장진입 그룹핑. 컬럼에는 문자열로 저장 (시드 값과 동일).
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CertificationType type;

    public Certification(String certificationName, String slug, CertificationType type) {
        this.certificationName = certificationName;
        this.slug = slug;
        this.type = type;
    }

    public static Certification create (String certificationName, String slug, CertificationType type, int displayOrder){
        Certification certification = new Certification(certificationName, slug, type);
        certification.activate();
        certification.changeDisplayOrder(displayOrder);

        return certification;
    }


}
