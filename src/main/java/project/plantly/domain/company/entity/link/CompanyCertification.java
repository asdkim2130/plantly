package project.plantly.domain.company.entity.link;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import project.plantly.domain.company.certification.Certification;
import project.plantly.domain.company.certification.CertificationType;
import project.plantly.domain.company.entity.Company;
import project.plantly.domain.company.exception.CompanyErrorCode;
import project.plantly.global.exception.BusinessException;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
// 패싯 필터는 certification_id IN (...) → company_id 의 역방향 조회이므로 전용 인덱스를 둔다.
// company_id 를 포함시켜 heap 접근 없는 index-only scan 이 되게 한다.
// (검색 섬의 company_category_closure 가 idx_ccc_category 로 같은 처방을 해둔 것과 동일한 이유)
//
// 유일성 제약이 여기 없는 이유 — 인증은 다른 링크와 달리 (company_id, certification_id) 만으로 유일하지 않다.
// '기타'(ETC) 마스터는 시드에 없는 인증을 회사가 직접 적어 넣는 앵커라, 같은 마스터에 custom_name 만
// 다른 링크가 여러 건 붙는다. 그래서 유일성이 두 갈래로 갈리고(일반 = 마스터당 1건, 기타 = 이름당 1건)
// JPA 로 표현할 수 없는 **부분** 유니크 인덱스가 된다 → {@link project.plantly.global.config.CompanyCertificationIndexInitializer}
// 가 소유한다(사업자번호 부분 인덱스와 같은 처방).
@Table(indexes = @Index(name = "idx_company_cert_facet", columnList = "certification_id, company_id"))
public class CompanyCertification {

    // 직접 입력한 인증명의 최대 길이. 인증명이지 소개글이 아니라서 짧게 잡는다.
    public static final int CUSTOM_NAME_MAX_LENGTH = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "certification_id", nullable = false)
    private Certification certification;

    // 마스터 목록에 없는 인증을 회사가 직접 적은 이름. '기타'(ETC) 마스터에 붙을 때만 값이 있고,
    // 그 외에는 언제나 null 이다(아래 생성자 불변식). 화면에 뜨는 이름은 이 값이 있으면 이 값이다.
    @Column(length = CUSTOM_NAME_MAX_LENGTH)
    private String customName;

    // 회사가 등록 시 선택한 순서(요청 순서). 조회/노출은 이 순서를 따른다.
    @Column(nullable = false, columnDefinition = "integer default 0")
    private int displayOrder;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 마스터 인증 링크(직접 입력 없음). 기존 호출부(테스트·검색 시더)가 쓰던 형태를 그대로 둔다.
     */
    public CompanyCertification(Company company, Certification certification, int displayOrder) {
        this(company, certification, null, displayOrder);
    }

    /**
     * 링크 생성. customName 과 마스터 종류(ETC)는 반드시 짝이 맞아야 한다 —
     * <b>customName 이 있으면 마스터는 ETC 여야 하고, 없으면 ETC 가 아니어야 한다.</b>
     *
     * <p>이 규칙을 DB CHECK 로 옮기려면 링크에 type 을 비정규화해 복합 FK 를 걸어야 하는데, 그러면 마스터의
     * 분류가 링크 테이블에 복제된다. 링크가 마스터를 이미 손에 쥐고 있으니 여기서 막는 편이 싸고, H2 로 도는
     * 슬라이스 테스트에서도 똑같이 동작한다(부분 유니크 인덱스는 Postgres 에서만 걸리는 것과 대비된다).
     *
     * <p>customName 은 앞뒤 공백을 털고 빈 문자열은 null 로 접는다 — "기타를 골라놓고 이름을 비운" 요청이
     * 공백 한 칸짜리 인증으로 저장되지 않고 아래 불변식에 걸리게 하기 위해서다.
     */
    public CompanyCertification(Company company, Certification certification, String customName, int displayOrder) {
        String normalized = normalizeCustomName(customName);
        validate(certification, normalized);

        this.company = company;
        this.certification = certification;
        this.customName = normalized;
        this.displayOrder = displayOrder;
    }

    /** 화면에 그대로 쓰는 인증명. 직접 입력한 이름이 있으면 그것이고, 없으면 마스터의 이름이다. */
    public String displayName() {
        return customName != null ? customName : certification.getCertificationName();
    }

    public static String normalizeCustomName(String customName) {
        if (customName == null) {
            return null;
        }
        String trimmed = customName.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static void validate(Certification certification, String normalizedCustomName) {
        boolean etc = certification.getType() == CertificationType.ETC;

        if (etc && normalizedCustomName == null) {
            throw new BusinessException(CompanyErrorCode.CERTIFICATION_CUSTOM_NAME_REQUIRED);
        }
        if (!etc && normalizedCustomName != null) {
            throw new BusinessException(CompanyErrorCode.CERTIFICATION_CUSTOM_NAME_NOT_ALLOWED);
        }
        if (normalizedCustomName != null && normalizedCustomName.length() > CUSTOM_NAME_MAX_LENGTH) {
            throw new BusinessException(CompanyErrorCode.CERTIFICATION_CUSTOM_NAME_TOO_LONG);
        }
    }
}
