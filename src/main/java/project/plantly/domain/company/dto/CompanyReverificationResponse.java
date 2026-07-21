package project.plantly.domain.company.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 재인증 결과. 국세청 재확인을 통과해 DB 에 반영된 검증값을 그대로 돌려준다.
 *
 * <p>화면이 갱신된 대표자명·개업일자를 읽기 전용으로 다시 그리도록 검증값을 함께 준다. verifiedAt 은 이번
 * 재인증 시각으로, 최초 인증 후 1년 재인증 주기의 기준점이 이 값으로 갱신됐음을 알린다. 사업자번호는 바뀌지
 * 않지만(요청으로 받지도 않는다), 어떤 번호로 재인증됐는지 확인용으로 함께 내려준다.
 */
public record CompanyReverificationResponse(
        String businessNumber,
        String ceoName,
        LocalDate businessStartDate,
        LocalDateTime verifiedAt
) {

    public static CompanyReverificationResponse of(String businessNumber, String ceoName,
                                                   LocalDate businessStartDate, LocalDateTime verifiedAt) {
        return new CompanyReverificationResponse(businessNumber, ceoName, businessStartDate, verifiedAt);
    }
}
