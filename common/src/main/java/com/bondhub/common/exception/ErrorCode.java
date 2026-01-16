package com.bondhub.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    // System errors (9xxx)
    SYS_UNCATEGORIZED(HttpStatus.INTERNAL_SERVER_ERROR, 9999, "Unknown system error occurred"),

    // Authentication errors (1xxx)
    AUTH_UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, 1001, "Authentication failed or not provided"),
    AUTH_UNAUTHORIZED(HttpStatus.FORBIDDEN, 1002, "Insufficient permissions to perform this operation"),
    JWT_INVALID_TOKEN(HttpStatus.UNAUTHORIZED, 1003, "Invalid or malformed JWT token"),
    JWT_EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, 1004, "JWT token has expired"),
    JWT_SIGNATURE_INVALID(HttpStatus.UNAUTHORIZED, 1005, "JWT signature verification failed"),
    AUTH_INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, 1006, "Invalid email or password"),

    // User account errors (2xxx)
    ACC_PHONE_NUMBER_ALREADY_USED(HttpStatus.CONFLICT, 2001, "Phone number already linked with another account"),
    ACC_EMAIL_ALREADY_USED(HttpStatus.CONFLICT, 2002, "Email already linked with another account"),
    ACC_ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, 2003, "User account not found"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, 2004, "User not found"),
    INVALID_OTP(HttpStatus.BAD_REQUEST, 2005, "Invalid or expired OTP code"),
    ACC_WRONG_PASSWORD(HttpStatus.CONFLICT, 2006, "Password is not valid"),
    ACC_IS_OAUTH(HttpStatus.CONFLICT, 2007, "This account login with open authorization"),
    CIC_IS_EXIST(HttpStatus.CONFLICT, 2008, "This cic has already existed"),

    // Role and permission errors (21xx)
    ROLE_NOT_FOUND(HttpStatus.NOT_FOUND, 2101, "Role not found"),
    PERM_NOT_FOUND(HttpStatus.NOT_FOUND, 2102, "Permission not found"),
    PERMISSION_IN_USE(HttpStatus.CONFLICT, 2103,
            "Permission is currently being used by one or more roles and cannot be deleted"),

    // VALIDATION (22xx)
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, 2200, "Validation failed"),
    PROMOTION_CODE_REQUIRED(HttpStatus.BAD_REQUEST, 2201, "Promotion code is required for CODE type promotion"),
    INVALID_STATUS(HttpStatus.BAD_REQUEST, 2202, "Invalid status for this operation"),
    INVALID_DATE_ATTRIBUTE_PAIR(HttpStatus.BAD_REQUEST, 2203, "DATE_START và DATE_END phải đi cùng nhau"),
    INVALID_YEAR_ATTRIBUTE_PAIR(HttpStatus.BAD_REQUEST, 2204, "YEAR_START và YEAR_END phải đi cùng nhau"),
    INVALID_OPERATION(HttpStatus.BAD_REQUEST, 2205, "Invalid operation for current contract status"),
    INVALID_PROMOTION_CONDITION(HttpStatus.BAD_REQUEST, 2206, "Invalid promotion condition expression")

    ;

    private final HttpStatus httpStatus;
    private final int code;
    private final String message;

    ErrorCode(HttpStatus httpStatus, int code, String message) {
        this.message = message;
        this.httpStatus = httpStatus;
        this.code = code;
    }

    @Override
    public String toString() {
        return code + ": " + message;
    }

    public static ErrorCode fromCode(int code) {
        for (ErrorCode errorCode : ErrorCode.values()) {
            if (errorCode.getCode() == code) {
                return errorCode;
            }
        }
        return SYS_UNCATEGORIZED;
    }
}
