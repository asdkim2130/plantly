package project.plantly.domain.company.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 회사 자가등록 임시저장(초안). 발행({@code POST /api/v1/companies}) 전, 작성 중인 폼 상태를 서버에 보관해
 * 재진입 시 이어쓰기를 가능하게 한다.
 *
 * <p>선행 인증({@link CompanyVerification})과 1:1 이며 verificationId 를 자연키로 삼는다 — 자가등록은
 * 인증 1건당 회사 1건이므로 초안도 인증 1건당 1개다. 발행이 성공하면 이 초안은 삭제된다
 * ({@code CompanyService.createByUser}).
 *
 * <p>payload 는 발행 요청({@code MyCompanyCreateRequest})을 통째로 직렬화한 JSON 이다. 기본 필드와 컬렉션을
 * 한 문서로 함께 보관하는 이유: 컬렉션 테이블은 {@code company_id} 없이는 행을 넣을 수 없고, 초안 시점엔
 * 아직 회사가 없다. 그래서 컬렉션도 회사가 생기기 전까지 이 문서 안에 머문다. 초안 저장은 부분 입력을
 * 허용하고(검증 없음), 필수값·마스터 검증은 발행 시점({@code POST /companies})에만 한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompanyDraft {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 선행 인증 식별자(자연키, 1:1). 초안 조회/폐기/발행 삭제는 모두 이 값으로 한다.
    @Column(nullable = false, unique = true)
    private Long verificationId;

    // 초안 소유자. 인증의 userId 와 동일하지만, 소유권을 자체로도 걸 수 있게 함께 둔다.
    @Column(nullable = false)
    private Long userId;

    // 발행 요청(MyCompanyCreateRequest)을 직렬화한 JSON. 기본 필드 + 컬렉션을 한 문서로 보관한다.
    @Column(columnDefinition = "text", nullable = false)
    private String payload;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private CompanyDraft(Long verificationId, Long userId, String payload) {
        this.verificationId = verificationId;
        this.userId = userId;
        this.payload = payload;
    }

    public static CompanyDraft create(Long verificationId, Long userId, String payload) {
        return new CompanyDraft(verificationId, userId, payload);
    }

    /** 자동저장: 새 폼 상태로 payload 를 통째 교체한다. */
    public void updatePayload(String payload) {
        this.payload = payload;
    }
}
