package com.grow.lyy.common.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Serves the frontend entry page from Spring Boot's static resources. */
@Controller
public class PageController {

    @GetMapping({"/login", "/garden", "/garden/emotions", "/garden/letters", "/garden/letters/{id}", "/garden/account", "/user/login", "/admin/letters"})
    public String loginPage() {
        return "forward:/index.html";
    }
}
