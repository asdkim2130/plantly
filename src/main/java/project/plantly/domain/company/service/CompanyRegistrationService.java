package project.plantly.domain.company.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import project.plantly.domain.company.dto.MyCompanyCreateRequest;

/**
 * 유저 자가등록의 진입점. 발행 전에 인증 상태를 정리한 뒤 실제 등록으로 넘긴다.
 *
 * <p><b>이 빈이 따로 있는 이유는 트랜잭션 경계 하나 때문이다.</b> 만료된 인증의 자동 재질의
 * ({@link CompanyVerificationService#refreshIfExpired})에는 국세청 HTTP 호출이 들어 있고, 등록
 * ({@link CompanyService#createByUser})은 트랜잭션 안에서 돌아간다. 둘을 한 메서드에 넣으면 외부 응답을
 * 기다리는 내내 DB 커넥션을 붙잡게 되어, 국세청이 느려지는 순간 커넥션 풀이 말라 서비스 전체가 멈춘다.
 * 그래서 트랜잭션이 <b>없는</b> 이 빈이 순서만 잡고, 각 단계가 자기 트랜잭션을 연다
 * ({@link CompanyVerificationService}/{@link CompanyVerificationWriter} 가 쓰는 것과 같은 분리다).
 *
 * <p>같은 클래스 안에서 나눠 부르면 자기 호출이라 프록시를 타지 않아 트랜잭션 경계가 아예 생기지 않는다 —
 * 별도 빈으로 나와야 하는 실질적인 이유이기도 하다.
 *
 * <p>순서가 이 방향인 것도 의도다. 인증을 먼저 살려두면 등록은 "유효한 인증이 있다" 는 전제로만 돌면 되고,
 * 등록 경로가 만료 처리를 알 필요가 없다. 재질의가 실패하면 등록 트랜잭션은 시작조차 하지 않는다.
 */
@Service
@RequiredArgsConstructor
public class CompanyRegistrationService {

    private final CompanyVerificationService verificationService;
    private final CompanyService companyService;

    public Long register(Long userId, MyCompanyCreateRequest request) {
        // 만료됐으면 저장값 그대로 국세청에 다시 물어 되살린다. 만료가 아니면 아무 일도 하지 않는다.
        // 사용자에게 다시 받을 값이 없으므로 정상 사용자는 이 단계를 인지하지 못한다.
        verificationService.refreshIfExpired(userId, request.verificationId());

        return companyService.createByUser(userId, request);
    }
}
