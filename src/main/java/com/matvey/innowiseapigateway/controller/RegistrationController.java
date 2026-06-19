package com.matvey.innowiseapigateway.controller;

import com.matvey.innowiseapigateway.dto.AdminRegisterRequest;
import com.matvey.innowiseapigateway.dto.AuthResponse;
import com.matvey.innowiseapigateway.dto.RegisterRequest;
import com.matvey.innowiseapigateway.service.RegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class RegistrationController {

    private final RegistrationService registrationService;

    @PostMapping("/register")
    public Mono<ResponseEntity<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return registrationService.register(request)
                .map(authResponse -> ResponseEntity.status(HttpStatus.CREATED).body(authResponse));
    }

    @PostMapping("/admin/register")
    public Mono<ResponseEntity<AuthResponse>> registerAdmin(@Valid @RequestBody AdminRegisterRequest request) {
        return registrationService.registerAdmin(request)
                .map(authResponse -> ResponseEntity.status(HttpStatus.CREATED).body(authResponse));
    }
}
