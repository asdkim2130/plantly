package project.plantly.domain.company.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import project.plantly.domain.company.service.CertificationService;

@RestController
@RequiredArgsConstructor
public class CertificationController {

    private final CertificationService certificationService;
}
