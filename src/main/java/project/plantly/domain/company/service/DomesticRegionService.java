package project.plantly.domain.company.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import project.plantly.domain.company.repository.DomesticRegionRepository;

@Service
@RequiredArgsConstructor
public class DomesticRegionService {

    private final DomesticRegionRepository domesticRegionRepository;


}
