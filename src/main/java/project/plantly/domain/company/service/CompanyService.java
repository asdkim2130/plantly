package project.plantly.domain.company.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.plantly.domain.company.dto.CompanyCreateRequest;
import project.plantly.domain.company.entity.Company;
import project.plantly.domain.company.entity.link.CompanyMember;
import project.plantly.domain.company.policy.CompanyRegistrationContext;
import project.plantly.domain.company.policy.CompanyRegistrationPolicy;
import project.plantly.domain.company.repository.CompanyMemberRepository;
import project.plantly.domain.company.repository.CompanyRepository;
import project.plantly.domain.user.enums.UserGrade;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;

    // 부속 엔티티 저장 위임. 자식(소유) / 링크(M:N) 각각 전담 컴포넌트가 소유한다.
    private final CompanyChildWriter childWriter;
    private final CompanyLinkWriter linkWriter;

    // 회사-유저 멤버십. 자가등록 시 등록자를 OWNER 로 1건 기록한다.
    private final CompanyMemberRepository companyMemberRepository;

    // 등록 정책 모음. 정책 내용은 각 구현체가 소유하며, 서비스는 주입받은 정책들을 실행만 한다.
    // 새 정책은 CompanyRegistrationPolicy 구현 @Component 추가만으로 자동 합류한다.
    private final List<CompanyRegistrationPolicy> registrationPolicies;

    // 유저 자가등록: 등록 즉시 소유자 = 본인. 정책은 본인 등급 기준으로 적용된다.
    @Transactional
    public Long createByUser(Long userId, UserGrade grade, CompanyCreateRequest request) {
        Company company = Company.createByUser(
                userId,
                request.businessNumber(), request.companyName(), request.ceoName(), request.establishmentDate(),
                request.postalCode(), request.address(), request.detailAddress(), request.website(), request.logoUrl(),
                request.introTitle(), request.content(), request.trlLevel(), request.videoUrl(), request.leadTime(), request.asInfo(), request.pricingType(), request.brandColor());

        Long companyId = persist(company, request, CompanyRegistrationContext.ofUser(grade));

        // 자가등록자 = OWNER. 관리자 등록(createByAdmin)은 소유자 미연동이라 멤버를 만들지 않는다.
        // (Company.userId 와 병행 기록 — 추후 멤버십이 단일 진실원이 되면 userId 는 정리)
        companyMemberRepository.save(CompanyMember.owner(companyId, userId));
        return companyId;
    }

    // 관리자 등록: 소유자 미연동(userId=null) 상태로 시작. registeredBy = 등록한 admin id.
    @Transactional
    public Long createByAdmin(Long adminId, CompanyCreateRequest request) {
        Company company = Company.createByAdmin(
                adminId,
                request.businessNumber(), request.companyName(), request.ceoName(), request.establishmentDate(),
                request.postalCode(), request.address(), request.detailAddress(), request.website(), request.logoUrl(),
                request.introTitle(), request.content(), request.trlLevel(), request.videoUrl(), request.leadTime(), request.asInfo(), request.pricingType(), request.brandColor());

        return persist(company, request, CompanyRegistrationContext.ofAdmin());
    }

    // 공통 코어: 등록 정책 일괄 검증 후, 본체 INSERT(=id 확보) → 부속(자식/링크)을 같은 트랜잭션으로 저장한다.
    private Long persist(Company company, CompanyCreateRequest request, CompanyRegistrationContext context) {
        registrationPolicies.forEach(policy -> policy.apply(company, request, context));

        companyRepository.save(company);
        childWriter.write(company, request);
        linkWriter.write(company, request);
        return company.getId();
    }
}