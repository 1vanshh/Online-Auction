package com.example.authservice;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthController {
    @GetMapping({"/auth", "/auth/"})
    public Map<String, String> test() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Auth service работает!");
        response.put("status", "OK");
        return response;
    }
}
