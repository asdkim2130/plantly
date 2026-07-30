package project.plantly.companyTest.industryTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import project.plantly.domain.company.industry.Industry;
import project.plantly.domain.company.industry.IndustryRepository;
import project.plantly.domain.company.industry.IndustryService;
import project.plantly.domain.company.industry.dto.IndustryAdminResponse;
import project.plantly.domain.company.industry.dto.IndustryCreateRequest;
import project.plantly.domain.company.industry.dto.IndustryPublicResponse;
import project.plantly.global.exception.BusinessException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;


@ExtendWith(MockitoExtension.class)
public class IndustryServiceTest {

    @Mock IndustryRepository industryRepository;
    @InjectMocks IndustryService industryService;

    @Test
    @DisplayName("이름이 중복이면 예외가 발생하고 저장하지 않음")
    public void create_duplicateName (){
        IndustryCreateRequest request = new IndustryCreateRequest("a", "a", null, null, null);
        given(industryRepository.existsByIndustryName("a")).willReturn(true);

        assertThatThrownBy(
                () -> industryService.createIndustry(request)
        ).isInstanceOf(BusinessException.class);

        verify(industryRepository, never()).save(any());
    }

    @Test
    @DisplayName("코드가 중복이면 예외가 발생하고 저장하지 않음")
    public void create_duplicateCode (){
        IndustryCreateRequest request = new IndustryCreateRequest("a", "a", null, null, null);
        given(industryRepository.existsByIndustryName("a")).willReturn(false);
        given(industryRepository.existsBySlug("a")).willReturn(true);

        assertThatThrownBy(
                () -> industryService.createIndustry(request)
        ).isInstanceOf(BusinessException.class);

        verify(industryRepository, never()).save(any());
    }

    @Test
    @DisplayName("displayOrder 미입력 시 최대값 +1로 저장")
    public void create_autoDisplayOrder (){
        IndustryCreateRequest request = new IndustryCreateRequest("a", "a", null, null, null);

        given(industryRepository.existsByIndustryName("a")).willReturn(false);
        given(industryRepository.existsBySlug("a")).willReturn(false);
        given(industryRepository.findMaxDisplayOrder()).willReturn(2);
        given(industryRepository.save(any(Industry.class))).willAnswer(
                inv -> {
                    Industry i = inv.getArgument(0);
                    ReflectionTestUtils.setField(i, "id", 1L);
                    return i;
                }
        );

        Long id = industryService.createIndustry(request);

        assertThat(id).isEqualTo(1L);

        ArgumentCaptor<Industry> captor = ArgumentCaptor.forClass(Industry.class);
        verify(industryRepository).save(captor.capture());
        assertThat(captor.getValue().getDisplayOrder()).isEqualTo(3);
    }

    @Test
    @DisplayName("displayOrder 입력 시 입력값 그대로 저장하고 최대값을 조회하지 않음")
    public void create_manualDisplayOrder (){
        IndustryCreateRequest request = new IndustryCreateRequest("a", "a", null, null, 5);

        given(industryRepository.existsByIndustryName("a")).willReturn(false);
        given(industryRepository.existsBySlug("a")).willReturn(false);
        given(industryRepository.save(any(Industry.class))).willAnswer(
                inv -> {
                    Industry i = inv.getArgument(0);
                    ReflectionTestUtils.setField(i, "id", 1L);
                    return i;
                }
        );

        Long id = industryService.createIndustry(request);

        assertThat(id).isEqualTo(1L);

        ArgumentCaptor<Industry> captor = ArgumentCaptor.forClass(Industry.class);
        verify(industryRepository).save(captor.capture());
        assertThat(captor.getValue().getDisplayOrder()).isEqualTo(5);
        verify(industryRepository, never()).findMaxDisplayOrder();
    }

    @Test
    @DisplayName("전체 산업군을 displayOrder 순으로 조회해 DTO로 매핑")
    public void getAll_mapToDto (){
        Industry first = industry(1L, "농업", "agri", "icon-agri", "농업 설명", 0);
        Industry second = industry(2L, "제조업", "manu", "icon-manu", "제조업 설명", 1);
        given(industryRepository.findAllByOrderByDisplayOrderAsc()).willReturn(List.of(first, second));

        List<IndustryAdminResponse> result = industryService.getAll();

        assertThat(result).hasSize(2);

        IndustryAdminResponse firstDto = result.get(0);
        assertThat(firstDto.id()).isEqualTo(1L);
        assertThat(firstDto.industryName()).isEqualTo("농업");
        assertThat(firstDto.slug()).isEqualTo("agri");
        assertThat(firstDto.iconUrl()).isEqualTo("icon-agri");   // slug와 분리되어 올바르게 매핑되는지 검증
        assertThat(firstDto.description()).isEqualTo("농업 설명");
        assertThat(firstDto.displayOrder()).isEqualTo(0);
        assertThat(firstDto.active()).isTrue();

        assertThat(result.get(1).id()).isEqualTo(2L);
    }

    @Test
    @DisplayName("관리자 목록은 비활성 산업까지 포함한다 (공개 목록과 조회 범위가 다르다)")
    public void getAll_includesInactive (){
        Industry active = industry(1L, "농업", "agri", null, null, 0);
        Industry retired = industry(2L, "폐기된 산업", "retired", null, null, 1);
        retired.deactivate();
        given(industryRepository.findAllByOrderByDisplayOrderAsc()).willReturn(List.of(active, retired));

        List<IndustryAdminResponse> result = industryService.getAll();

        // 운영자는 폐기한 산업도 보고 되살릴 수 있어야 하므로 목록에서 빠지면 안 된다.
        assertThat(result).hasSize(2);
        assertThat(result.get(0).active()).isTrue();
        assertThat(result.get(1).industryName()).isEqualTo("폐기된 산업");
        assertThat(result.get(1).active()).isFalse();

        // 공개 목록용 활성 전용 쿼리를 관리자 경로에 잘못 끌어다 쓰면 안 된다.
        verify(industryRepository, never()).findAllByActiveTrueOrderByDisplayOrderAsc();
    }

    @Test
    @DisplayName("공개 목록은 활성 산업만 조회하는 쿼리를 쓰고 운영 필드 없이 매핑한다")
    public void getPublicList_mapToDto (){
        Industry first = industry(1L, "농업", "agri", "icon-agri", "농업 설명", 0);
        Industry second = industry(2L, "제조업", "manu", null, null, 1);
        given(industryRepository.findAllByActiveTrueOrderByDisplayOrderAsc()).willReturn(List.of(first, second));

        List<IndustryPublicResponse> result = industryService.getPublicList();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(0).industryName()).isEqualTo("농업");
        assertThat(result.get(0).slug()).isEqualTo("agri");
        assertThat(result.get(0).iconUrl()).isEqualTo("icon-agri");
        // 시드가 iconUrl 을 넣지 않아 대부분 null 이다 — 매핑이 터지지 않고 그대로 내려가야 한다.
        assertThat(result.get(1).iconUrl()).isNull();

        // 전체 조회(관리자용)가 아니라 활성만 조회하는 쿼리를 써야 한다 — 폐기 산업이 옵션에 남으면 안 된다.
        verify(industryRepository, never()).findAllByOrderByDisplayOrderAsc();
    }

    @Test
    @DisplayName("산업군이 없으면 빈 리스트를 반환")
    public void getAll_empty (){
        given(industryRepository.findAllByOrderByDisplayOrderAsc()).willReturn(List.of());

        assertThat(industryService.getAll()).isEmpty();
    }


    // 테스트 헬퍼 — create 로 만든 뒤 id 만 리플렉션으로 주입
    private Industry industry (Long id, String name, String code, String iconUrl, String description, int displayOrder){
        Industry industry = Industry.create(name, code, iconUrl, description, displayOrder);
        ReflectionTestUtils.setField(industry, "id", id);
        return industry;
    }
}