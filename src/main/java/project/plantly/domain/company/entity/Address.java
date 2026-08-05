package project.plantly.domain.company.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회사 주소 값 객체(@Embeddable). Daum 우편번호 서비스(프론트 전용)가 채워 넘기는 네 축을 하나로 묶는다.
 * Company 본체의 세 평면 필드(postalCode/address/detailAddress)를 대체하며, 도로명 단일 주소를
 * 도로명(roadAddress)·지번(jibunAddress) 두 축으로 분리했다.
 *
 * <p>불변(immutable): 생성은 {@link #of}, 부분 수정은 {@link #merged}(새 인스턴스 반환)로만 한다.
 * 컬럼명은 필드명 기본 규칙(postal_code / road_address / jibun_address / detail_address)을 따른다 —
 * 기존 postal_code / detail_address 는 그대로, 기존 address 는 road_address 로 바뀌고 jibun_address 가 새로 는다.
 */
@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Address {

    // 우편번호. 5자리 숫자(국가기초구역번호) — DTO 를 우회하는 진입 경로(연동/배치/직접 생성)까지 persist 시점에 방어한다.
    @NotNull
    @Pattern(regexp = "\\d{5}", message = "우편번호는 5자리 숫자여야 합니다.")
    @Column(nullable = false)
    private String postalCode;

    // 도로명 전체 주소. 카드·목록의 대표 표시 주소이자 검색 대상. (기존 Company.address)
    @NotNull
    @Column(nullable = false)
    private String roadAddress;

    // 지번 주소. 도로명만 부여된 신주소는 비어올 수 있어 nullable. 검색 텍스트에는 도로명과 함께 접어 넣는다.
    @Column
    private String jibunAddress;

    // 사용자가 직접 입력하는 상세주소(동/호수 등). (기존 Company.detailAddress 와 동일 제약)
    @NotNull
    @Column(nullable = false)
    private String detailAddress;

    private Address(String postalCode, String roadAddress, String jibunAddress, String detailAddress) {
        this.postalCode = postalCode;
        this.roadAddress = roadAddress;
        this.jibunAddress = jibunAddress;
        this.detailAddress = detailAddress;
    }

    // 선택 필드인 jibunAddress 는 blank("") 를 null 로 접는다 — 지번이 부여되지 않은 주소에서 우편번호 서비스가
    // 빈 문자열을 내려주기 때문. 그대로 저장하면 검색 도큐먼트의 concat_ws 가 null-skip 을 못 타 공백이 남고,
    // 같은 값을 merged 로 수정했을 때(= null)와 저장 결과가 갈린다. 두 생성 경로의 규약을 여기서 맞춘다.
    public static Address of(String postalCode, String roadAddress, String jibunAddress, String detailAddress) {
        return new Address(postalCode, roadAddress, blankToNull(jibunAddress), detailAddress);
    }

    // 부분 수정(PATCH) 병합: null = 미변경.
    // 필수(postalCode/roadAddress/detailAddress)는 blank 를 요청 DTO(@Size(min=1)) 가 거르므로 값이 오면 교체만.
    // 선택(jibunAddress)은 blank("") = 비움(null) 규약을 적용한다.
    public Address merged(String postalCode, String roadAddress, String jibunAddress, String detailAddress) {
        return new Address(
                postalCode != null ? postalCode : this.postalCode,
                roadAddress != null ? roadAddress : this.roadAddress,
                jibunAddress != null ? blankToNull(jibunAddress) : this.jibunAddress,
                detailAddress != null ? detailAddress : this.detailAddress);
    }

    // null 도 그대로 null 로 흘린다 — of 는 값 없이도 호출되고(임시저장 호환), merged 는 호출 전에
    // null(=미변경)을 이미 걸러내므로 양쪽 의미가 어긋나지 않는다.
    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
