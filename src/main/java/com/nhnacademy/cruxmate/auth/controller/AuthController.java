package com.nhnacademy.cruxmate.auth.controller;

import com.nhnacademy.cruxmate.auth.dto.LoginRequest;
import com.nhnacademy.cruxmate.auth.dto.LoginResponse;
import com.nhnacademy.cruxmate.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public LoginResponse login(
            @Valid @RequestBody LoginRequest request
    ){
        return authService.login(
                request.email(),
                request.password()
        );
    }
}
