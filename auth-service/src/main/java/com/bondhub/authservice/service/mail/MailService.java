package com.bondhub.authservice.service.mail;

public interface MailService {
    /**
     * Send OTP verification email to user (Registration)
     *
     * @param email     Recipient email address
     * @param otp       The 6-digit OTP code to send
     * @param accountId The account ID (optional, for logging/reference)
     */
    void sendOtpEmail(String email, String otp, String accountId);

    /**
     * Send OTP for password reset
     *
     * @param email Recipient email address
     * @param otp   The 6-digit OTP code to send
     */
    void sendPasswordResetOtpEmail(String email, String otp);
}
