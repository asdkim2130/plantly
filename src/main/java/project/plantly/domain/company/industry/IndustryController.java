package project.plantly.domain.company.industry;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import project.plantly.domain.company.industry.dto.IndustryAdminResponse;
import project.plantly.domain.company.industry.dto.IndustryCreateRequest;
import project.plantly.domain.company.industry.dto.IndustryPublicResponse;
import project.plantly.global.response.ApiResponse;
import project.plantly.global.response.IdResponse;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class IndustryController {

    private final IndustryService industryService;

    @PostMapping("/api/v1/admin/industries")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<IdResponse> createIndustry (@Valid @RequestBody IndustryCreateRequest request){

        Long id = industryService.createIndustry(request);
        return ApiResponse.success("산업군 생성이 완료되었습니다.", new IdResponse(id));
    }

    @GetMapping("/api/v1/admin/industries")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<IndustryAdminResponse>> getAll (){

        List<IndustryAdminResponse> all = industryService.getAll();
        return ApiResponse.success(all);
    }

    /**
     * 공개 산업 옵션 목록. 회사 등록 폼과 검색 필터 패널의 드롭다운 소스로, 비로그인 사용자도 검색 화면을
     * 쓰므로 인증을 요구하지 않는다(SecurityConfig 에서 permitAll).
     *
     * <p>검색의 산업 패싯(industryIds)도 이 id 를 그대로 실어 보낸다. 인증과 달리 type 구분이 없어
     * 서버가 묶을 것이 없고, 차원 내 OR 로만 판정한다.
     */
    @GetMapping("/api/v1/industries")
    public ApiResponse<List<IndustryPublicResponse>> getPublicList (){

        return ApiResponse.success(industryService.getPublicList());
    }
}
