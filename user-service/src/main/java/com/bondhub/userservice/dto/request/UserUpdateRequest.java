package com.bondhub.userservice.dto.request;

import com.bondhub.userservice.model.enums.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserUpdateRequest {
    @NotBlank(message = "{user.update.fullNameRequired}")
    String fullName;

    @PastOrPresent(message = "{user.update.dobInvalid}")
    LocalDate dob;

    String bio;
    Gender gender;
}
