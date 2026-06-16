package project.plantly.companyTest.certificationTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import project.plantly.domain.company.certification.Certification;
import project.plantly.domain.company.certification.CertificationRepository;
import project.plantly.domain.company.certification.CertificationService;
import project.plantly.domain.company.certification.dto.CertificationCreateRequest;
import project.plantly.global.exception.BusinessException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;


@ExtendWith(MockitoExtension.class)
public class CertificationServiceTest {

    @Mock CertificationRepository certificationRepository;
    @InjectMocks CertificationService certificationService;

    @Test
    @DisplayName("이름이 중복이면 예외가 발생하고 저장하지 않음")
    public void create_duplicateName (){
        CertificationCreateRequest request = new CertificationCreateRequest("a", null);
        given(certificationRepository.existsByCertificationName("a")).willReturn(true);

        assertThatThrownBy(
                () -> certificationService.createCertification(request)
        ).isInstanceOf(BusinessException.class);

        verify(certificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("displayOrder 미입력 시 최대값 +1로 저장")
    public void create_autoDisplayOrder (){
        CertificationCreateRequest request = new CertificationCreateRequest("a", null);

        given(certificationRepository.existsByCertificationName("a")).willReturn(false);
        given(certificationRepository.findMaxDisplayOrder()).willReturn(2);
        given(certificationRepository.save(any(Certification.class))).willAnswer(
                inv -> {
                    Certification c = inv.getArgument(0);
                    ReflectionTestUtils.setField(c, "id", 1L);
                    return c;
                }
        );

        Long id = certificationService.createCertification(request);

        assertThat(id).isEqualTo(1L);

        ArgumentCaptor<Certification> captor = ArgumentCaptor.forClass(Certification.class);
        verify(certificationRepository).save(captor.capture());
        assertThat(captor.getValue().getDisplayOrder()).isEqualTo(3);
    }

    @Test
    @DisplayName("displayOrder 입력 시 입력값 그대로 저장하고 최대값을 조회하지 않음")
    public void create_manualDisplayOrder (){
        CertificationCreateRequest request = new CertificationCreateRequest("a", 5);

        given(certificationRepository.existsByCertificationName("a")).willReturn(false);
        given(certificationRepository.save(any(Certification.class))).willAnswer(
                inv -> {
                    Certification c = inv.getArgument(0);
                    ReflectionTestUtils.setField(c, "id", 1L);
                    return c;
                }
        );

        Long id = certificationService.createCertification(request);

        assertThat(id).isEqualTo(1L);

        ArgumentCaptor<Certification> captor = ArgumentCaptor.forClass(Certification.class);
        verify(certificationRepository).save(captor.capture());
        assertThat(captor.getValue().getDisplayOrder()).isEqualTo(5);
        verify(certificationRepository, never()).findMaxDisplayOrder();
    }
}