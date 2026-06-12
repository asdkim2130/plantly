package project.plantly.domain.company.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import project.plantly.domain.company.service.CategoryService;

@RestController
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;


}
