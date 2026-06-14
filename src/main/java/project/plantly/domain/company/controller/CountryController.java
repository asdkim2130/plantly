package project.plantly.domain.company.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import project.plantly.domain.company.service.CountryService;

@RestController
@RequiredArgsConstructor
public class CountryController {

    private final CountryService countryService;


}
