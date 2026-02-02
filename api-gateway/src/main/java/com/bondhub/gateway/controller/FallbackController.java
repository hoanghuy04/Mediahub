package com.bondhub.gateway.controller;

import com.bondhub.common.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Fallback controller to handle service unavailability and circuit breaker fallbacks.
 * Provides graceful degradation when downstream services are unavailable.
 */
@Slf4j
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @RequestMapping("/qr-wait")
    public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> qrWaitFallback(ServerHttpRequest request) {
        log.warn("QR Wait service fallback triggered for request: {} {}", 
                request.getMethod(), request.getPath());
        
        Map<String, Object> data = new HashMap<>();
        data.put("status", "PENDING");
        data.put("message", "Service is temporarily unavailable. Your request is being processed.");
        data.put("timestamp", LocalDateTime.now().format(FORMATTER));
        
        return Mono.just(ResponseEntity.ok(ApiResponse.success(data)));
    }

    @RequestMapping("/auth-service")
    public Mono<ResponseEntity<ApiResponse<Object>>> authServiceFallback(ServerHttpRequest request) {
        log.error("Auth service fallback triggered for request: {} {}", 
                request.getMethod(), request.getPath());
        
        Map<String, String> errorDetails = createErrorDetails(
                "Authentication Service",
                "The authentication service is temporarily unavailable. Please try again in a few moments.",
                request
        );
        
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error(503, "Authentication Service Unavailable", errorDetails)));
    }

    @RequestMapping("/user-service")
    public Mono<ResponseEntity<ApiResponse<Object>>> userServiceFallback(ServerHttpRequest request) {
        log.error("User service fallback triggered for request: {} {}", 
                request.getMethod(), request.getPath());
        
        Map<String, String> errorDetails = createErrorDetails(
                "User Service",
                "The user service is temporarily unavailable. Please try again later.",
                request
        );
        
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error(503, "User Service Unavailable", errorDetails)));
    }

    @RequestMapping("/message-service")
    public Mono<ResponseEntity<ApiResponse<Object>>> messageServiceFallback(ServerHttpRequest request) {
        log.error("Message service fallback triggered for request: {} {}", 
                request.getMethod(), request.getPath());
        
        Map<String, String> errorDetails = createErrorDetails(
                "Message Service",
                "The messaging service is temporarily unavailable. Your messages are safe and will be accessible shortly.",
                request
        );
        
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error(503, "Message Service Unavailable", errorDetails)));
    }

    @RequestMapping("/notification-service")
    public Mono<ResponseEntity<ApiResponse<Object>>> notificationServiceFallback(ServerHttpRequest request) {
        log.error("Notification service fallback triggered for request: {} {}", 
                request.getMethod(), request.getPath());
        
        Map<String, String> errorDetails = createErrorDetails(
                "Notification Service",
                "The notification service is temporarily unavailable. Notifications will be delivered once service is restored.",
                request
        );
        
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error(503, "Notification Service Unavailable", errorDetails)));
    }

    /**
     * Fallback for general gateway errors
     */
    @RequestMapping("/general")
    public Mono<ResponseEntity<ApiResponse<Object>>> generalFallback(ServerHttpRequest request) {
        log.error("General fallback triggered for request: {} {}", 
                request.getMethod(), request.getPath());
        
        Map<String, String> errorDetails = createErrorDetails(
                "Gateway",
                "The requested service is temporarily unavailable. Please try again later.",
                request
        );
        
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error(503, "Service Unavailable", errorDetails)));
    }

    /**
     * Fallback for timeout errors
     */
    @RequestMapping("/timeout")
    public Mono<ResponseEntity<ApiResponse<Object>>> timeoutFallback(ServerHttpRequest request) {
        log.error("Timeout fallback triggered for request: {} {}", 
                request.getMethod(), request.getPath());
        
        Map<String, String> errorDetails = new HashMap<>();
        errorDetails.put("service", "Downstream Service");
        errorDetails.put("reason", "Request timeout");
        errorDetails.put("message", "The request took too long to complete. Please try again with a simpler request or contact support if the issue persists.");
        errorDetails.put("timestamp", LocalDateTime.now().format(FORMATTER));
        errorDetails.put("path", request.getPath().toString());
        
        return Mono.just(ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
                .body(ApiResponse.error(504, "Request Timeout", errorDetails)));
    }

    private Map<String, String> createErrorDetails(String serviceName, String message, ServerHttpRequest request) {
        Map<String, String> details = new HashMap<>();
        details.put("service", serviceName);
        details.put("message", message);
        details.put("timestamp", LocalDateTime.now().format(FORMATTER));
        details.put("path", request.getPath().toString());
        details.put("suggestion", "Please retry your request. If the problem persists, contact support.");
        return details;
    }
}
