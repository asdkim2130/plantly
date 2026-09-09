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
 * <p><b>키는 (userId, businessNumber) 다 — 선행 인증 레코드가 아니다.</b> 예전에는 verificationId 를
 * 자연키로 삼았는데, 그러면 인증 레코드의 수명이 곧 작성분의 수명이 된다. 인증은 국세청 판정이라 시간이
 * 지나면 낡는 것이 맞고 만료·재발급되면 식별자가 바뀌지만, 사용자가 친 소개글은 사업자 상태가 바뀌었다고
 * 무효가 되지 않는다. 두 수명을 묶어두면 만료를 만난 사용자가 재인증하는 순간 초안 키가 달라져
 * 작성분에 닿을 수 없게 된다(그리고 그 행은 아무도 못 읽는 고아로 남는다).
 *
 * <p>사업자번호로 키잉하면 인증이 몇 번을 만료·재발급되든 초안은 그대로 살아 있고, 사용자가 기기를 바꿔
 * 인증 식별자를 잃어버려도 등록 폼 첫 칸에 자기 사업자번호를 넣는 것만으로 이어쓰기가 성립한다.
 * 접근 권한은 여전히 인증이 준다 — "이 사용자가 이 번호로 국세청을 통과한 적이 있는가" 를 매 요청마다
 * 확인하므로({@code CompanyDraftService}), 아무 번호로나 초안을 만들 수는 없다.
 *
 * <p>사업자번호는 하이픈 없는 숫자 10자리로 정규화해 저장한다({@code BusinessNumbers}). 표기가 섞이면
 * 같은 사업자의 초안이 두 행으로 갈린다.
 *
 * <p>payload 는 발행 요청({@code MyCompanyCreateRequest})을 통째로 직렬화한 JSON 이다. 기본 필드와 컬렉션을
 * 한 문서로 함께 보관하는 이유: 컬렉션 테이블은 {@code company_id} 없이는 행을 넣을 수 없고, 초안 시점엔
 * 아직 회사가 없다. 그래서 컬렉션도 회사가 생기기 전까지 이 문서 안에 머문다. 초안 저장은 부분 입력을
 * 허용하고(검증 없음), 필수값·마스터 검증은 발행 시점({@code POST /companies})에만 한다.
 *
 * <p>payload 안의 {@code verificationId} 는 저장 당시의 값이라 낡을 수 있다. 복원한 폼을 발행할 때는
 * 클라이언트가 현재 유효한 인증 식별자로 덮어써 보내야 한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(uniqueConstraints = @UniqueConstraint(
        name = "uk_company_draft_user_business_number", columnNames = {"userId", "businessNumber"}))
public class CompanyDraft {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 초안 소유자. 조회/폐기/발행 삭제는 모두 (userId, businessNumber) 짝으로 한다.
    @Column(nullable = false)
    private Long userId;

    // 어느 사업자의 초안인지. 하이픈 없는 숫자 10자리(정규화 후) 로만 들어온다.
    @Column(nullable = false, length = 10)
    private String businessNumber;

    // 발행 요청(MyCompanyCreateRequest)을 직렬화한 JSON. 기본 필드 + 컬렉션을 한 문서로 보관한다.
    @Column(columnDefinition = "text", nullable = false)
    private String payload;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private CompanyDraft(Long userId, String businessNumber, String payload) {
        this.userId = userId;
        this.businessNumber = businessNumber;
        this.payload = payload;
    }

    public static CompanyDraft create(Long userId, String businessNumber, String payload) {
        return new CompanyDraft(userId, businessNumber, payload);
    }

    /** 자동저장: 새 폼 상태로 payload 를 통째 교체한다. */
    public void updatePayload(String payload) {
        this.payload = payload;
    }
}
