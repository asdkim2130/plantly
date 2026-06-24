package project.plantly.domain.company.dto;

import project.plantly.domain.company.category.Category;
import project.plantly.domain.company.certification.Certification;
import project.plantly.domain.company.country.Country;
import project.plantly.domain.company.domesticRegion.DomesticRegion;
import project.plantly.domain.company.entity.Company;
import project.plantly.domain.company.entity.CompanyContact;
import project.plantly.domain.company.entity.CompanyEquipment;
import project.plantly.domain.company.entity.CompanyImage;
import project.plantly.domain.company.entity.CompanyMaterial;
import project.plantly.domain.company.entity.CompanyProjectReference;
import project.plantly.domain.company.entity.CompanyTag;
import project.plantly.domain.company.industry.Industry;

import java.util.List;

// 상세 응답 조립을 위해 회사 본체 + 부속(자식/링크)을 한 번에 묶은 내부 전용 묶음.
// 조회 서비스가 채워 DTO 의 from(...) 으로 넘긴다. (응답 형태(공개/상세)에 독립적인 '원자료' 계층)
//
// 연락처/레퍼런스는 하위 필드를 가진 컬렉션이라 초기 버전은 대표 1건만 상세에 싣는다(없으면 null).
// representativeReferenceImages 는 그 대표 레퍼런스에 딸린 이미지(순서대로)다.
// 갤러리 이미지(projectReference == null)는 galleryImages 로 따로 담는다.
public record CompanyAggregate(
        Company company,
        CompanyContact representativeContact,
        List<CompanyImage> galleryImages,
        CompanyProjectReference representativeReference,
        List<CompanyImage> representativeReferenceImages,
        List<CompanyMaterial> materials,
        List<CompanyEquipment> equipment,
        List<CompanyTag> tags,
        List<Category> categories,
        List<Certification> certifications,
        List<Country> countries,
        List<DomesticRegion> regions,
        List<Industry> industries
) {
}
