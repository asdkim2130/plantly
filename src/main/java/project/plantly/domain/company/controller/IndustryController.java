package project.plantly.domain.company.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import project.plantly.domain.company.industry.IndustryService;

@RestController
@RequiredArgsConstructor
public class IndustryController {

    private final IndustryService industryService;
}
