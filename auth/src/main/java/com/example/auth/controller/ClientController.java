package com.example.auth.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ClientController {

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/auth/")
    public String index2() {
        return "index";
    }
}
