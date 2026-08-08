package project.plantly.companyTest.searchTest;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import project.plantly.companyTest.support.PostgresContainerTest;
import project.plantly.domain.company.entity.Address;
import project.plantly.domain.company.entity.Company;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 사업자번호 활성 유니크(부분 인덱스) 통합 테스트. soft delete 하에서 삭제된 회사의 번호는 재사용 가능하고
 * 활성 회사끼리는 유일해야 한다는 계약을, 실제 Postgres 에서 {@link project.plantly.global.config.CompanyBusinessNumberIndexInitializer}
 * 가 만든 부분 인덱스로 검증한다. (H2 는 부분 인덱스 미지원이라 이 계약은 PG 통합 테스트 전용)
 */
@Transactional
@DisplayName("company.business_number: 활성 행 부분 유니크")
class CompanyBusinessNumberUniquenessTest extends PostgresContainerTest {

    @Autowired
    EntityManager em;

    // 참고: @GeneratedValue(IDENTITY) 라 persist 시점에 즉시 INSERT 가 나간다(id 확보). 따라서 제약 위반은
    // flush 가 아니라 persist 에서 던져진다.

    @Test
    @DisplayName("활성 회사 둘이 같은 사업자번호면 부분 유니크 인덱스가 막는다")
    void activeDuplicateRejected() {
        em.persist(company("111-11-11111"));

        // 정확한 예외 타입(Hibernate/JPA 래핑)에 결합하지 않고, 우리가 만든 부분 유니크 인덱스가 막았음을 메시지로 확인.
        assertThatThrownBy(() -> em.persist(company("111-11-11111")))
                .hasMessageContaining("ux_company_business_number_active");
    }

    @Test
    @DisplayName("삭제된 회사의 사업자번호는 재사용할 수 있다(활성 유니크에서 빠짐)")
    void reusableAfterSoftDelete() {
        Company first = company("222-22-22222");
        em.persist(first);

        first.delete();   // soft delete → 부분 인덱스(WHERE deleted=false)에서 제외
        em.flush();       // deleted=true 를 DB 에 반영해야 새 활성 행이 인덱스에 안 걸린다

        assertThatCode(() -> em.persist(company("222-22-22222")))   // 같은 번호로 새 활성 회사
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("NULL 사업자번호(초안)는 활성 다건이어도 허용된다")
    void multipleNullAllowed() {
        assertThatCode(() -> {
            em.persist(company(null));
            em.persist(company(null));
        }).doesNotThrowAnyException();
    }

    private Company company(String businessNumber) {
        return Company.createByUser(1L, businessNumber, "회사", "대표", null,
                Address.of("06236", "서울 강남구", null, "테헤란로 1"), null, "logo", null,
                "요약", "본문", null, null, null, null, null, null);
    }
}
