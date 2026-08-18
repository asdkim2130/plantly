package project.plantly.global.seed;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import project.plantly.domain.company.dto.CompanyCreateRequest;
import project.plantly.domain.company.dto.MyCompanyCreateRequest;
import project.plantly.domain.company.entity.Company;
import project.plantly.domain.company.entity.CompanySubscription;
import project.plantly.domain.company.enums.CompanyGrade;
import project.plantly.domain.company.enums.SubscriptionStatus;
import project.plantly.domain.company.repository.CompanyRepository;
import project.plantly.domain.company.repository.CompanySubscriptionRepository;
import project.plantly.domain.company.service.CompanyService;

import java.time.LocalDate;

/**
 * 회사 생성·상태 조정.
 *
 * <p>생성은 반드시 {@link CompanyService} 의 실제 등록 경로를 탄다. 인증 소비, 초안 삭제, 소유자 멤버십,
 * 구독 1건, 검색 도큐먼트·카테고리 closure 동기화가 전부 그 안에서 일어나므로, 리포지토리로 직접
 * 저장하면 "DB 엔 있는데 목록·검색엔 안 나오는" 회사가 생긴다.
 *
 * <p>등록 이후의 상태(등급 교체·삭제·운영 플래그)는 관리자 API 가 하는 것과 같은 도메인 메서드로 바꾼다.
 * 등록 시점에는 만들 수 없는 상태(만료된 구독, 소프트 삭제)를 케이스로 만들려면 이 단계가 필요하다.
 */
@Component
@Profile(SeedConfig.PROFILE)
@RequiredArgsConstructor
public class SeedCompanyFactory {

    private final CompanyService companyService;
    private final CompanyRepository companyRepository;
    private final CompanySubscriptionRepository subscriptionRepository;

    /** 관리자 등록. 소유자 미연동(CompanyMember 0건) + ADMIN_EXEMPT 구독으로 시작한다. */
    public Long createByAdmin(Long adminId, CompanyCreateRequest request) {
        return companyService.createByAdmin(adminId, request);
    }

    /** 자가등록. 선행 인증을 소비하고 소유자 멤버십·businessVerified 까지 실제 경로로 채운다. */
    public Long createByUser(Long userId, MyCompanyCreateRequest request) {
        return companyService.createByUser(userId, request);
    }

    /**
     * 구독 교체. 관리자 구독 변경 API 와 같은 경로({@code changeByAdmin})다.
     *
     * <p>등록 시점에는 등급을 고를 수 없다 — 자가등록은 항상 체험 ENTERPRISE, 관리자 등록은 항상 ADMIN_EXEMPT 다.
     * 등급별 화면 차이와 만료 강등을 보려면 등록 후에 바꾸는 이 경로가 유일하다.
     */
    @Transactional
    public void changeSubscription(Long companyId, CompanyGrade grade, SubscriptionStatus status, LocalDate expiresAt) {
        CompanySubscription subscription = subscriptionRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new IllegalStateException("구독이 없습니다. companyId=" + companyId));
        subscription.changeByAdmin(grade, status, expiresAt);
    }

    /** 관리자 운영 플래그. 등록 시엔 전부 false 로 시작하므로 목표 상태를 그대로 지정한다. */
    @Transactional
    public void applyFlags(Long companyId, boolean verified, boolean featured, boolean spotlight, int spotlightOrder) {
        Company company = load(companyId);
        company.changeVerified(verified);
        company.changeFeatured(featured);
        if (spotlight) {
            company.turnOnSpotlight(spotlightOrder);
        } else {
            company.turnOffSpotlight();
        }
    }

    /** 소프트 삭제. 공개 목록·상세에서 빠지고 관리자 목록에만 남는다(복구 대상). */
    @Transactional
    public void softDelete(Long companyId) {
        load(companyId).delete();
    }

    private Company load(Long companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalStateException("회사가 없습니다. companyId=" + companyId));
    }
}
