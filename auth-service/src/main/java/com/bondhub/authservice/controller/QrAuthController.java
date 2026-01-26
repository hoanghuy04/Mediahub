package com.bondhub.authservice.controller;

import com.bondhub.authservice.dto.auth.request.QrMobileRequest;
import com.bondhub.authservice.dto.auth.response.QrGenerationResponse;
import com.bondhub.authservice.dto.auth.response.QrStatusResponse;
import com.bondhub.authservice.service.auth.QrAuthenticationService;
import com.bondhub.common.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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

    @PostMapping("/scan")
    public ResponseEntity<ApiResponse<Void>> scanQr(@RequestBody @Valid QrMobileRequest request) {
        qrAuthenticationService.scanQr(request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/accept")
    public ResponseEntity<ApiResponse<Void>> acceptQr(@RequestBody @Valid QrMobileRequest request) {
        qrAuthenticationService.acceptQr(request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/reject")
    public ResponseEntity<ApiResponse<Void>> rejectQr(@RequestBody @Valid QrMobileRequest request) {
        qrAuthenticationService.rejectQr(request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
