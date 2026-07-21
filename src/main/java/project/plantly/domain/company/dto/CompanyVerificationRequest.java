package project.plantly.domain.company.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * 사업자 인증 요청. 회사 등록 폼 앞단에서 아이디 중복 확인처럼 먼저 수행한다.
 *
 * <p>사업자번호는 하이픈이 있어도 되고 없어도 된다(서버가 정규화한다). 개업일자는 <b>설립일자가 아니라</b>
 * 사업자등록증의 개업연월일이다 — 법인은 등기 설립일과 다를 수 있고, 국세청은 개업일자로만 대조한다.
 */
public record CompanyVerificationRequest(
        @NotBlank
        String businessNumber,
        @NotBlank
        String ceoName,
        @NotNull
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate businessStartDate
) {
}
