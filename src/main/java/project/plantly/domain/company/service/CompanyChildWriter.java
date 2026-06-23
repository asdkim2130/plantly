package project.plantly.domain.company.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import project.plantly.domain.company.dto.CompanyCreateRequest;
import project.plantly.domain.company.entity.*;
import project.plantly.domain.company.repository.*;

import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.IntStream;
import java.util.stream.Stream;

// 자식(소유) 엔티티 저장 전담. 요청 값을 그대로 저장하며 displayOrder 는 리스트 인덱스로 부여한다.
@Component
@RequiredArgsConstructor
public class CompanyChildWriter {

    private final CompanyContactRepository contactRepository;
    private final CompanyImageRepository imageRepository;
    private final CompanyMaterialRepository materialRepository;
    private final CompanyEquipmentRepository equipmentRepository;
    private final CompanyTagRepository tagRepository;
    private final CompanyProjectReferenceRepository referenceRepository;

    public void write(Company company, CompanyCreateRequest request) {
        saveIndexed(request.contacts(), contactRepository,
                (c, i) -> new CompanyContact(company, c.contactName(), c.position(), c.phone(), c.email(), i));
        saveIndexed(request.images(), imageRepository,
                (img, i) -> CompanyImage.ofCompany(company, img.imageUrl(), img.imageType(), i));
        saveIndexed(request.materialNames(), materialRepository,
                (name, i) -> new CompanyMaterial(company, name, i));
        saveIndexed(request.equipmentNames(), equipmentRepository,
                (name, i) -> new CompanyEquipment(company, name, i));
        saveIndexed(request.tagNames(), tagRepository,
                (name, i) -> new CompanyTag(company, name, i));
        saveReferences(company, request);
    }

    // (요청 리스트, 저장소, "i번째 요소 + 인덱스 → 엔티티" 팩토리) 를 받아 일괄 저장. null/빈 리스트는 무시한다.
    private <T, E> void saveIndexed(List<T> source,
                                    JpaRepository<E, ?> repository,
                                    BiFunction<T, Integer, E> factory) {
        if (source == null || source.isEmpty()) {
            return;
        }
        List<E> entities = IntStream.range(0, source.size())
                .mapToObj(i -> factory.apply(source.get(i), i))
                .toList();
        repository.saveAll(entities);
    }

    // 레퍼런스는 "본체 저장 → 딸린 프로젝트 이미지 평탄화" 2단계라 별도 처리한다.
    private void saveReferences(Company company, CompanyCreateRequest request) {
        if (request.references() == null || request.references().isEmpty()) {
            return;
        }

        // 레퍼런스 본체 저장 — displayOrder 는 요청 순서(인덱스)로 부여하고, FK 확보를 위해 먼저 저장한다.
        List<CompanyProjectReference> references = IntStream.range(0, request.references().size())
                .mapToObj(i -> {
                    CompanyCreateRequest.ReferenceRequest r = request.references().get(i);
                    return new CompanyProjectReference(company, r.projectTitle(), r.achievements(), r.partners(), r.period(), i);
                })
                .toList();
        referenceRepository.saveAll(references);

        // 각 레퍼런스에 딸린 프로젝트 이미지 → CompanyImage(PROJECT) 로 연결. 이미지 displayOrder 는 레퍼런스 내 순서.
        List<CompanyImage> projectImages = IntStream.range(0, references.size())
                .boxed()
                .flatMap(i -> {
                    List<String> imageUrls = request.references().get(i).imageUrls();
                    if (imageUrls == null) {
                        return Stream.empty();
                    }
                    CompanyProjectReference reference = references.get(i);
                    return IntStream.range(0, imageUrls.size())
                            .mapToObj(j -> CompanyImage.ofProject(reference, imageUrls.get(j), j));
                })
                .toList();
        imageRepository.saveAll(projectImages);
    }
}
