package project.plantly.domain.company.industry;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.plantly.domain.company.industry.dto.IndustryCreateRequest;
import project.plantly.global.exception.BusinessException;

@Service
@RequiredArgsConstructor
public class IndustryService {

    private final IndustryRepository industryRepository;

    @Transactional
    public Long createIndustry (IndustryCreateRequest request){

        if(industryRepository.existsByIndustryName(request.industryName())){
            throw new BusinessException(IndustryErrorCode.DUPLICATE_INDUSTRY_NAME);
        }

        if(industryRepository.existsByIndustryCode(request.industryCode())){
            throw new BusinessException(IndustryErrorCode.DUPLICATE_INDUSTRY_CODE);
        }

        // displayOrder값이 null이면 할당
        int displayOrder = (request.displayOrder() != null)
                ? request.displayOrder()
                : industryRepository.findMaxDisplayOrder() + 1;

        Industry industry = Industry.create(request.industryName(), request.industryCode(), request.iconUrl(), request.description(), displayOrder);
        return industryRepository.save(industry).getId();

    }
}
