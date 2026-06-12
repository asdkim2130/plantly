package project.plantly.domain.company.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import project.plantly.domain.company.repository.CertificationRepository;

@Service
@RequiredArgsConstructor
public class CertificationService {

    private final CertificationRepository certificationRepository;
}
