package project.plantly.domain.company.category;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    // 전체를 displayOrder 순으로 평탄하게 로드 — SELECT * ... ORDER BY display_order, 쿼리 1번
    List<Category> findAllByOrderByDisplayOrderAsc();
    boolean existsByCategoryCode(String categoryCode);   // 중복 슬러그 사전 체크
    boolean existsByParentId(Long parentId);             // 자식 존재 여부 (삭제 가드용)

    // 같은 부모(대분류는 parentId=null)의 형제 중 최대 displayOrder — 없으면 -1 (자동 순번 부여용)
    @Query("select coalesce(max(c.displayOrder), -1) from Category c " +
           "where (:parentId is null and c.parentId is null) or c.parentId = :parentId")
    int findMaxDisplayOrderByParentId(@Param("parentId") Long parentId);

}
