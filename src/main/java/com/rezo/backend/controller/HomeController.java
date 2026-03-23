package com.rezo.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "REZO BACKEND OK 🚀";
    }

    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }
}
