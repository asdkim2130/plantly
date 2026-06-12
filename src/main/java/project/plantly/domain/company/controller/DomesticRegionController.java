package project.plantly.domain.company.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import project.plantly.domain.company.service.DomesticRegionService;

@RestController
@RequiredArgsConstructor
public class DomesticRegionController {

    private final DomesticRegionService domesticRegionService;


}
