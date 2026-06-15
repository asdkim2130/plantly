package project.plantly.domain.company.category;


import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.plantly.domain.company.category.dto.CategoryCreateRequest;
import project.plantly.domain.company.category.dto.CategoryTreeResponse;
import project.plantly.domain.company.category.exception.CategoryErrorException;
import project.plantly.domain.company.category.tree.CategoryChangedEvent;
import project.plantly.domain.company.category.tree.CategoryTreeService;
import project.plantly.global.exception.BusinessException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryAdminService {

    private final CategoryRepository categoryRepository;
    private final ApplicationEventPublisher events;
    private final CategoryTreeService treeService;

    @Transactional
    public Long create (CategoryCreateRequest request){
        if(categoryRepository.existsByCategoryCode(request.categoryCode())){
            throw new BusinessException(CategoryErrorException.DUPLICATE_CATEGORY_CODE);
        }

        // 입력이 있으면 그대로, 없으면 같은 부모 형제의 마지막 순번 + 1 (첫 항목은 0)
        int displayOrder = (request.displayOrder() != null)
                ? request.displayOrder()
                : categoryRepository.findMaxDisplayOrderByParentId(request.parentId()) + 1;

        Category category = (request.parentId() == null) ?
                Category.createRoot(request.categoryName(), request.categoryCode(), request.iconUrl(), request.description(), displayOrder)
                : Category.createChild(
                categoryRepository.findById(request.parentId()).orElseThrow(
                        () -> new BusinessException(CategoryErrorException.PARENT_CATEGORY_NOT_FOUND)
                ),
                request.categoryName(), request.categoryCode(), request.iconUrl(), request.description(), displayOrder
        );

        Long id = categoryRepository.save(category).getId();
        events.publishEvent(new CategoryChangedEvent());  //커밋 후 reload 트리거

        return id;

    }

    // 관리자용 카테고리 조회
    public List<CategoryTreeResponse> getTree (){

        return treeService.getRoots().stream().map(
                CategoryTreeResponse::from
        ).toList();

    }


}
