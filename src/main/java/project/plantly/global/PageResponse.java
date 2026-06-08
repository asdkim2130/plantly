package project.plantly.global;

import lombok.Getter;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Getter
public class PageResponse<T> {

    private List<T> content;
    private PageInfo pageInfo;

    public PageResponse(List<T> content, PageInfo pageInfo){
        this.content = content;
        this.pageInfo = pageInfo;
    }

    public static <T> PageResponse<T> of (List<T> content, long totalElement, Pageable pageable){
        int pageNumber = pageable.getPageNumber() + 1;
        int size = pageable.getPageSize();
        int totalPage = (int)Math.ceil((double) totalElement / size);

        PageInfo pageInfo = new PageInfo(pageNumber, size, totalElement, totalPage);
        return new PageResponse<>(content, pageInfo);
    }
}
