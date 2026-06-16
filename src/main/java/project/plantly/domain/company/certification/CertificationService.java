package project.plantly.domain.company.certification;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.plantly.domain.company.DisplayOrders;
import project.plantly.domain.company.certification.dto.CertificationCreateRequest;
import project.plantly.global.exception.BusinessException;

@Service
@RequiredArgsConstructor
public class CertificationService {

    private final CertificationRepository certificationRepository;

    @Transactional
    public Long createCertification (CertificationCreateRequest request){

        if(certificationRepository.existsByCertificationName(request.name())){
            throw new BusinessException(CertificationExceptionError.DUPLICATE_CERTIFICATION_NAME);
        }

        // displayOrder값이 null이면 할당
        int displayOrder = DisplayOrders.resolve(
                request.displayOrder(), certificationRepository::findMaxDisplayOrder);

        Certification certification = Certification.create(request.name(), displayOrder);
        return certificationRepository.save(certification).getId();
    }
}
