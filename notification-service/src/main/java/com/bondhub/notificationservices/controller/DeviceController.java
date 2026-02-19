package com.bondhub.notificationservices.controller;

import com.bondhub.common.dto.ApiResponse;
import com.bondhub.notificationservices.dto.request.DeviceTokenRequest;
import com.bondhub.notificationservices.service.device.DeviceService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications/devices")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DeviceController {

    DeviceService deviceService;

    @PostMapping
    public ResponseEntity<ApiResponse<String>> registerDevice(@Valid @RequestBody DeviceTokenRequest request) {
        deviceService.registerDevice(request);
        return ResponseEntity.ok(ApiResponse.success("Device registered successfully"));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<String>> unregisterDevice(@RequestParam String userId, @RequestParam String token) {
        deviceService.unregisterDevice(userId, token);
        return ResponseEntity.ok(ApiResponse.success("Device unregistered successfully"));
    }
}
