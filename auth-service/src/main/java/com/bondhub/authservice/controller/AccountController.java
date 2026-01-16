package com.bondhub.authservice.controller;

import com.bondhub.authservice.dto.account.request.AccountCreateRequest;
import com.bondhub.authservice.dto.account.response.AccountResponse;
import com.bondhub.authservice.dto.account.request.AccountUpdateRequest;
import com.bondhub.authservice.service.account.AccountService;
import com.bondhub.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AccountController {

    AccountService accountService;

    @PostMapping
    public ResponseEntity<ApiResponse<AccountResponse>> createAccount(@Valid @RequestBody AccountCreateRequest request) {
        log.info("REST request to create account with email: {}", request.email());
        ApiResponse<AccountResponse> response = accountService.createAccount(request);

        if (response.code() == 1000) {
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AccountResponse>> getAccountById(@PathVariable String id) {
        log.info("REST request to get account by id: {}", id);
        ApiResponse<AccountResponse> response = accountService.getAccountById(id);

        if (response.code() == 1000) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<ApiResponse<AccountResponse>> getAccountByEmail(@PathVariable String email) {
        log.info("REST request to get account by email: {}", email);
        ApiResponse<AccountResponse> response = accountService.getAccountByEmail(email);

        if (response.code() == 1000) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @GetMapping("/phone/{phoneNumber}")
    public ResponseEntity<ApiResponse<AccountResponse>> getAccountByPhoneNumber(@PathVariable String phoneNumber) {
        log.info("REST request to get account by phone number: {}", phoneNumber);
        ApiResponse<AccountResponse> response = accountService.getAccountByPhoneNumber(phoneNumber);

        if (response.code() == 1000) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AccountResponse>>> getAllAccounts() {
        log.info("REST request to get all accounts");
        ApiResponse<List<AccountResponse>> response = accountService.getAllAccounts();

        if (response.code() == 1000) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AccountResponse>> updateAccount(
            @PathVariable String id,
            @Valid @RequestBody AccountUpdateRequest request) {
        log.info("REST request to update account with id: {}", id);
        ApiResponse<AccountResponse> response = accountService.updateAccount(id, request);

        if (response.code() == 1000) {
            return ResponseEntity.ok(response);
        } else if (response.code() == 1003) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(@PathVariable String id) {
        log.info("REST request to delete account with id: {}", id);
        ApiResponse<Void> response = accountService.deleteAccount(id);

        if (response.code() == 1000) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @GetMapping("/exists/email/{email}")
    public ResponseEntity<ApiResponse<Boolean>> existsByEmail(@PathVariable String email) {
        log.info("REST request to check if account exists by email: {}", email);
        ApiResponse<Boolean> response = accountService.existsByEmail(email);

        if (response.code() == 1000) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @GetMapping("/exists/phone/{phoneNumber}")
    public ResponseEntity<ApiResponse<Boolean>> existsByPhoneNumber(@PathVariable String phoneNumber) {
        log.info("REST request to check if account exists by phone number: {}", phoneNumber);
        ApiResponse<Boolean> response = accountService.existsByPhoneNumber(phoneNumber);

        if (response.code() == 1000) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
