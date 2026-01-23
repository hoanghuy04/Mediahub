package com.bondhub.userservice.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AccountResponse {
    private String id;
    private String phoneNumber;
    private String email;
    private String role;
}
