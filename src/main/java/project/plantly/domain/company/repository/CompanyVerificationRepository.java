package project.plantly.domain.company.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.plantly.domain.company.entity.CompanyVerification;
import project.plantly.domain.company.enums.VerificationStatus;

import java.util.Optional;

public interface CompanyVerificationRepository extends JpaRepository<CompanyVerification, Long> {

    /** 회사 등록 시 인증을 소비하는 경로. userId 를 함께 걸어 타인의 인증을 쓰지 못하게 한다. */
    Optional<CompanyVerification> findByIdAndUserId(Long id, Long userId);

    /** 관리자 인증 회수 / 상태 조회. 등록에 소비된 인증은 companyId 로 찾는다. */
    Optional<CompanyVerification> findByCompanyId(Long companyId);

    /**
     * 초안 접근 권한의 근거. 초안은 인증 <b>레코드</b>가 아니라 사업자번호로 키잉되므로
     * ({@code CompanyDraft} 주석 참고) "이 사용자가 이 번호로 국세청을 통과한 적이 있는가" 만 물으면 된다.
     * 인증이 만료돼 여러 번 재발급됐어도 그 이력 중 하나만 있으면 자기 초안에 계속 닿을 수 있다.
     */
    boolean existsByUserIdAndBusinessNumber(Long userId, String businessNumber);

    /**
     * 이미 회사 등록에 소비된 인증이 있는지. 있으면 그 사업자번호는 초안이 아니라 수정 대상이다.
     * (status 로 묻는 이유: 재발급 이력 때문에 같은 번호로 여러 행이 있을 수 있어 단건 조회로는 판정할 수 없다)
     */
    boolean existsByUserIdAndBusinessNumberAndStatus(Long userId, String businessNumber, VerificationStatus status);
}
