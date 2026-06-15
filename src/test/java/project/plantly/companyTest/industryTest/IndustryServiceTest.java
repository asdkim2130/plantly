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
import project.plantly.domain.company.industry.dto.IndustryCreateRequest;
import project.plantly.global.exception.BusinessException;

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
        given(industryRepository.existsByIndustryCode("a")).willReturn(true);

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
        given(industryRepository.existsByIndustryCode("a")).willReturn(false);
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
        given(industryRepository.existsByIndustryCode("a")).willReturn(false);
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
}