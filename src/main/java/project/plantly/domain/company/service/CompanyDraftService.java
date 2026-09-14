package project.plantly.domain.company.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.plantly.domain.company.dto.CompanyDraftResponse;
import project.plantly.domain.company.dto.MyCompanyCreateRequest;
import project.plantly.domain.company.entity.CompanyDraft;
import project.plantly.domain.company.enums.VerificationStatus;
import project.plantly.domain.company.exception.CompanyErrorCode;
import project.plantly.domain.company.repository.CompanyDraftRepository;
import project.plantly.domain.company.repository.CompanyVerificationRepository;
import project.plantly.domain.company.support.BusinessNumbers;
import project.plantly.global.exception.BusinessException;

/**
 * 회사 자가등록 임시저장(초안) 서비스.
 *
 * <p>초안은 <b>사업자번호</b>로 키잉된다(인증 레코드가 아니라). 이유는 {@link CompanyDraft} 주석 참고 —
 * 인증은 만료·재발급되며 식별자가 바뀌지만 사용자가 쓴 글은 그럴 이유가 없기 때문이다.
 *
 * <p>대신 접근 권한은 여전히 인증이 준다: 매 요청마다 "이 사용자가 이 번호로 국세청을 통과한 적이 있는가"
 * 를 확인한다. 통과 이력이 없으면 아무 사업자번호로나 초안을 만들 수 없고, 남의 번호로 남의 초안을
 * 건드릴 수도 없다(조회 자체가 userId 로 스코프된다).
 *
 * <p>저장은 <b>검증하지 않는다</b> — 작성 중 부분 입력을 그대로 담아야 하기 때문이며, 필수값·마스터 검증은
 * 발행({@code POST /api/v1/companies})에서만 한다.
 */
@Service
@RequiredArgsConstructor
public class CompanyDraftService {

    private final CompanyDraftRepository draftRepository;
    private final CompanyVerificationRepository verificationRepository;
    private final ObjectMapper objectMapper;

    /**
     * 자동저장(upsert). 초안은 사용자·사업자번호당 1개라 그 짝으로 찾아 있으면 교체, 없으면 새로 만든다.
     * 이미 등록에 소비된(CONSUMED) 인증이 있으면 회사가 이미 존재하므로 초안이 아니라 수정 대상 — 저장을 거절한다.
     */
    @Transactional
    public void save(Long userId, String rawBusinessNumber, MyCompanyCreateRequest payload) {
        String businessNumber = requireVerifiedBusinessNumber(userId, rawBusinessNumber);
        if (verificationRepository.existsByUserIdAndBusinessNumberAndStatus(
                userId, businessNumber, VerificationStatus.CONSUMED)) {
            throw new BusinessException(CompanyErrorCode.VERIFICATION_ALREADY_USED);
        }

        String json = serialize(payload);
        draftRepository.findByUserIdAndBusinessNumber(userId, businessNumber)
                .ifPresentOrElse(
                        draft -> draft.updatePayload(json),
                        () -> draftRepository.save(CompanyDraft.create(userId, businessNumber, json)));
    }

    /** 재진입 시 폼 복원용. 저장된 초안이 없으면 404. */
    @Transactional(readOnly = true)
    public CompanyDraftResponse get(Long userId, String rawBusinessNumber) {
        String businessNumber = requireVerifiedBusinessNumber(userId, rawBusinessNumber);
        CompanyDraft draft = draftRepository.findByUserIdAndBusinessNumber(userId, businessNumber)
                .orElseThrow(() -> new BusinessException(CompanyErrorCode.DRAFT_NOT_FOUND));
        return new CompanyDraftResponse(deserialize(draft.getPayload()), draft.getUpdatedAt());
    }

    /** 수동 폐기. 발행 성공 시의 삭제는 {@code CompanyService} 가 담당한다. 초안이 없어도 멱등하게 성공한다. */
    @Transactional
    public void delete(Long userId, String rawBusinessNumber) {
        String businessNumber = requireVerifiedBusinessNumber(userId, rawBusinessNumber);
        draftRepository.deleteByUserIdAndBusinessNumber(userId, businessNumber);
    }

    /**
     * 정규화한 사업자번호를 돌려주되, 이 사용자가 그 번호로 국세청을 통과한 적이 없으면 거절한다.
     *
     * <p>정규화를 먼저 하는 이유는 저장 표기를 하나로 고정하기 위해서다 — 경로에 하이픈을 넣고 빼는 것만으로
     * 같은 사업자의 초안이 두 행으로 갈리면 안 된다. 형식이 아예 아닌 값은 조회하기 전에 400 으로 끊는다.
     */
    private String requireVerifiedBusinessNumber(Long userId, String rawBusinessNumber) {
        String businessNumber = BusinessNumbers.normalize(rawBusinessNumber);
        if (businessNumber == null) {
            throw new BusinessException(CompanyErrorCode.BUSINESS_NUMBER_INVALID_FORMAT);
        }
        if (!verificationRepository.existsByUserIdAndBusinessNumber(userId, businessNumber)) {
            throw new BusinessException(CompanyErrorCode.VERIFICATION_NOT_FOUND);
        }
        return businessNumber;
    }

    //Json 문자열 변환
    //ObjectMapper 로 객체를 Json문자열로 변환함(ObjectMapper.writeValueAsString())
    private String serialize(MyCompanyCreateRequest payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new BusinessException(CompanyErrorCode.DRAFT_SERIALIZATION_FAILED);
        }
    }

    private MyCompanyCreateRequest deserialize(String json) {
        try {
            return objectMapper.readValue(json, MyCompanyCreateRequest.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException(CompanyErrorCode.DRAFT_SERIALIZATION_FAILED);
        }
    }
}
