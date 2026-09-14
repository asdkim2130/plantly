package project.plantly.domain.company.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * 사업자 재인증 요청. 이미 등록·인증된 회사의 대표자명·개업일자를 국세청에 다시 확인한다.
 *
 * <p><b>사업자번호 자리가 없는 것이 이 DTO 의 핵심</b>이다. 재인증 시 사업자번호는 요청으로 받지 않고
 * DB 에 저장된 값(최초 인증본에서 온 불변값)을 그대로 쓴다 — 재인증 때 번호를 새로 입력받으면 오타나
 * 타사 번호로 인증을 갈아끼워 "국세청 확인" 배지를 탈취할 수 있다. 국세청 연동은 그 회사의 존재·상태만
 * 알려줄 뿐 소유권을 증명하지 않으므로, 번호는 절대 바뀌지 않게 고정한다.
 *
 * <p>개업일자는 설립일자가 아니라 사업자등록증의 개업연월일이다. 국세청 등록 정보가 바뀌었을 수 있으므로
 * 서버는 이전 값과 비교하지 않고, 새로 입력한 두 값이 국세청 재확인을 통과하면 그대로 DB 를 갱신한다.
 *
 * <p>메시지를 적어두는 이유는 선행 인증({@link CompanyVerificationRequest})과 같다 — 생략하면 기본 문구가
 * 나가 이 폼만 말투가 튄다.
 */
public record CompanyReverificationRequest(
        @NotBlank(message = "대표자명은 필수입니다.")
        String ceoName,
        @NotNull(message = "개업일자는 필수입니다.")
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate businessStartDate
) {
}
