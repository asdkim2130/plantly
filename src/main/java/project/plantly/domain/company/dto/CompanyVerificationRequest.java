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
 *
 * <p>세 필드 모두 메시지를 적어둔다. 생략하면 Bean Validation 기본 문구("공백일 수 없습니다" /
 * "널이어서는 안됩니다")가 그대로 나가는데, 이 API 의 다른 검증 문구는 전부 우리말 안내라 여기만 말투가
 * 튄다. 게다가 이 폼은 등록 플로우의 <b>첫 화면</b>이라 사용자가 가장 먼저 만나는 문구다.
 */
public record CompanyVerificationRequest(
        @NotBlank(message = "사업자등록번호는 필수입니다.")
        String businessNumber,
        @NotBlank(message = "대표자명은 필수입니다.")
        String ceoName,
        @NotNull(message = "개업일자는 필수입니다.")
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate businessStartDate
) {
}
