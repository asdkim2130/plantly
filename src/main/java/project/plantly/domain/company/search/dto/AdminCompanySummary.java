package project.plantly.domain.company.search.dto;

import project.plantly.domain.company.enums.RegistrationSource;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 관리자 회사 목록 카드 1건. 공개 카드({@link CompanySummary})와 같은 회사 스칼라 + 이름 목록에
 * 운영 필드(삭제 여부 / 소유자 id / 등록 출처 / 등록 시각)를 더한다. 관리자만 보는 목록이므로
 * 어느 회사가 삭제됐는지·누구 소유인지·어떻게 등록됐는지를 카드에서 바로 구분할 수 있게 한다.
 */
public record AdminCompanySummary(
        Long id,
        String companyName,
        String introTitle,
        String logoUrl,
        String address,
        boolean verified,
        boolean featured,
        boolean spotlight,
        // ----- 운영 필드 (관리자 전용) -----
        boolean deleted,
        Long ownerUserId,                  // 소유자 미연동(관리자 등록 등)이면 null
        RegistrationSource registrationSource,
        LocalDateTime createdAt,
        // ----- 회사가 연결한 이름 목록 -----
        List<String> categoryNames,
        List<String> tagNames,
        List<String> industryNames
) {}
