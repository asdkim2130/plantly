package project.plantly.domain.company.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.plantly.domain.company.dto.CompanyDraftResponse;
import project.plantly.domain.company.dto.MyCompanyCreateRequest;
import project.plantly.domain.company.entity.CompanyDraft;
import project.plantly.domain.company.entity.CompanyVerification;
import project.plantly.domain.company.enums.VerificationStatus;
import project.plantly.domain.company.exception.CompanyErrorCode;
import project.plantly.domain.company.repository.CompanyDraftRepository;
import project.plantly.domain.company.repository.CompanyVerificationRepository;
import project.plantly.global.exception.BusinessException;

/**
 * 회사 자가등록 임시저장(초안) 서비스.
 *
 * <p>초안은 선행 인증과 1:1 이라 모든 진입점을 verificationId 로 키잉하고, 매 요청마다 그 인증이 <b>본인 것</b>인지
 * 확인한다(남의 verificationId 로 남의 초안을 건드리는 경로 차단). 저장은 <b>검증하지 않는다</b> — 작성 중 부분
 * 입력을 그대로 담아야 하기 때문이며, 필수값·마스터 검증은 발행({@code POST /api/v1/companies})에서만 한다.
 */
@Service
@RequiredArgsConstructor
public class CompanyDraftService {

    private final CompanyDraftRepository draftRepository;
    private final CompanyVerificationRepository verificationRepository;
    private final ObjectMapper objectMapper;

    /**
     * 자동저장(upsert). 초안은 인증 1건당 1개라 verificationId 로 찾아 있으면 교체, 없으면 새로 만든다.
     * 이미 등록에 소비된(CONSUMED) 인증은 회사가 이미 존재하므로 초안이 아니라 수정 대상 — 저장을 거절한다.
     */
    @Transactional
    public void save(Long userId, Long verificationId, MyCompanyCreateRequest payload) {
        CompanyVerification verification = requireOwnedVerification(userId, verificationId);
        if (verification.getStatus() == VerificationStatus.CONSUMED) {
            throw new BusinessException(CompanyErrorCode.VERIFICATION_ALREADY_USED);
        }

        String json = serialize(payload);
        draftRepository.findByVerificationId(verificationId)
                .ifPresentOrElse(
                        draft -> draft.updatePayload(json),
                        () -> draftRepository.save(CompanyDraft.create(verificationId, userId, json)));
    }

    /** 재진입 시 폼 복원용. 저장된 초안이 없으면 404. */
    @Transactional(readOnly = true)
    public CompanyDraftResponse get(Long userId, Long verificationId) {
        requireOwnedVerification(userId, verificationId);
        CompanyDraft draft = draftRepository.findByVerificationId(verificationId)
                .orElseThrow(() -> new BusinessException(CompanyErrorCode.DRAFT_NOT_FOUND));
        return new CompanyDraftResponse(deserialize(draft.getPayload()), draft.getUpdatedAt());
    }

    /** 수동 폐기. 발행 성공 시의 삭제는 {@code CompanyService} 가 담당한다. 초안이 없어도 멱등하게 성공한다. */
    @Transactional
    public void delete(Long userId, Long verificationId) {
        requireOwnedVerification(userId, verificationId);
        draftRepository.deleteByVerificationId(verificationId);
    }

    // 본인이 받은 인증인지 확인한다. 인증 조회에 userId 를 함께 걸어 타인의 인증 식별자를 주워 쓰는 경로를 막는다
    // (CompanyService 의 등록 경로와 동일한 방어).
    private CompanyVerification requireOwnedVerification(Long userId, Long verificationId) {
        return verificationRepository.findByIdAndUserId(verificationId, userId)
                .orElseThrow(() -> new BusinessException(CompanyErrorCode.VERIFICATION_NOT_FOUND));
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
