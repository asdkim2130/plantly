package project.plantly.domain.company.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.plantly.domain.company.entity.Company;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    // 좋아요/즐겨찾기 등 상호작용 대상 검증용. 미삭제(공개 가능) 회사만 true. 소프트 삭제는 미존재로 취급한다.
    boolean existsByIdAndDeletedFalse(Long id);

    // 활성(미삭제) 회사 중 동일 사업자번호 존재 여부. 등록·복구 시 부분 유니크(활성 행끼리)를 앱 레벨에서도 사전 검증한다.
    // (DB 부분 유니크 인덱스가 최종 정합성을 보장하고, 이 조회는 친화적 에러 + H2 테스트 커버리지를 위한 belt-and-suspenders)
    boolean existsByBusinessNumberAndDeletedFalse(String businessNumber);
}
