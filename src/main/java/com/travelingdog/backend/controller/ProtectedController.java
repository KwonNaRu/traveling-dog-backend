package com.travelingdog.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/protected")
public class ProtectedController {

    @GetMapping
    public ResponseEntity<String> protectedEndpoint() {
        return ResponseEntity.ok("인증된 사용자만 접근 가능한 보호된 리소스입니다.");
    }

}
