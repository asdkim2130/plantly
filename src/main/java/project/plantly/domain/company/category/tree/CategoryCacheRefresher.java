package project.plantly.domain.company.category.tree;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class CategoryCacheRefresher {

    private final CategoryTreeService treeService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)  // 트랜젝션이 성공적으로 커밋 후에만 호출. 롤백 되면 일어나지 않음
    public void on (CategoryChangedEvent event){
        treeService.reload();  //커밋 이후에만 실행 -> 스냅샷은 항상 커밋된 상태를 반영
    }
}
