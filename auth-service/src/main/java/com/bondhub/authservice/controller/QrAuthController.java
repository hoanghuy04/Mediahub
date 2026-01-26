package com.bondhub.authservice.controller;

import com.bondhub.authservice.dto.auth.response.QrGenerationResponse;
import com.bondhub.authservice.dto.auth.response.QrStatusResponse;
import com.bondhub.authservice.service.auth.QrAuthenticationService;
import com.bondhub.common.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth/qr")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class QrAuthController {

    QrAuthenticationService qrAuthenticationService;

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<QrGenerationResponse>> generateQr(
            @RequestHeader(value = "X-Device-Id", required = false, defaultValue = "web-client") String deviceId,
            @RequestHeader(value = "User-Agent", required = false) String userAgent,
            HttpServletRequest request) {
        
        String ipAddress = request.getRemoteAddr();
        return ResponseEntity.ok(ApiResponse.success(qrAuthenticationService.generateQr(deviceId, userAgent, ipAddress)));
    }

    @GetMapping("/check/{qrId}")
    public ResponseEntity<ApiResponse<QrStatusResponse>> checkStatus(@PathVariable String qrId) {
        return ResponseEntity.ok(ApiResponse.success(qrAuthenticationService.checkStatus(qrId)));
    }
}
