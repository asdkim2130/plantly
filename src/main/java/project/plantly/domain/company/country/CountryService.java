package project.plantly.domain.company.country;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.plantly.domain.company.country.dto.CountryPublicResponse;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CountryService {

    private final CountryRepository countryRepository;

    /**
     * 공개 국가 옵션 목록 — 등록/수정 폼의 수출국 선택 소스. 250건 전체, 국가명(한글) 오름차순.
     *
     * <p>정렬을 {@code ORDER BY} 가 아니라 애플리케이션에서 한다. 국가명 정렬은 DB collation 에
     * 좌우되는데, Postgres(컨테이너 기본 로케일 en_US.utf8)에서 실제로
     * {@code 가나 · 남극 · 독일 · 베트남 · 대한민국} 순이 나온다 — "대한민국"이 "독일"보다 뒤로 간다.
     * 운영 DB 로케일이 또 다르면 순서가 다시 바뀌므로, 이식 가능한 순서를 DB 에 맡길 수 없다.
     * ({@code CountryOrderingPostgresTest} 가 이 순서를 실제 Postgres 로 고정한다)
     *
     * <p>{@code String} 자연 순서로 비교한다. 한글 음절(U+AC00~U+D7A3)은 초성-중성-종성 순으로
     * 배열되어 있어 코드포인트 순서가 곧 가나다순이고, 시드의 국가명은 전부 한글 음절이다.
     * {@code Collator} 는 쓰지 않는다 — JDK 의 로케일 데이터에 순서가 다시 묶이고, 여기서는
     * 추가로 얻는 것이 없다.
     *
     * <p>250건 전체를 메모리에서 정렬하는 비용은 무시할 수준이고, 대신 어느 DB·로케일에서도
     * 같은 순서가 나온다.
     *
     * <p>다른 마스터의 공개 목록과 달리 활성 필터가 없다 — {@code Country} 에는 active 가 없다(ISO 고정 목록).
     * 남극·무인 속령(AQ/BV/TF/HM/GS) 5건도 그대로 포함되며, 수출국으로 고를 일이 없다는 이유로
     * 서버가 임의로 빼지 않는다. 실제로 문제가 되면 그때 active 도입을 별건으로 다룬다.
     */
    @Transactional(readOnly = true)
    public List<CountryPublicResponse> getPublicList() {

        return countryRepository.findAll().stream()
                .map(CountryPublicResponse::from)
                .sorted(Comparator.comparing(CountryPublicResponse::nameKo))
                .toList();
    }
}
