package project.plantly.domain.company.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import project.plantly.domain.company.repository.IndustryRepository;

@Service
@RequiredArgsConstructor
public class IndustryService {

    private final IndustryRepository industryRepository;
}
