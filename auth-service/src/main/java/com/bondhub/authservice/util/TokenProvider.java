package com.bondhub.authservice.util;

import com.bondhub.authservice.dto.auth.response.TokenResponse;
import com.bondhub.authservice.enums.DeviceType;
import com.bondhub.authservice.model.Account;
import com.bondhub.authservice.service.token.TokenStoreService;
import com.bondhub.common.utils.JwtUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TokenProvider {

    JwtUtil jwtUtil;
    TokenStoreService tokenStoreService;

    public TokenResponse generateFullTokenResponse(Account account, String deviceId, DeviceType deviceType,
                                                   String userAgent, String ipAddress) {
        String sessionId = UUID.randomUUID().toString();

        long refreshExpirationMs = (deviceType == DeviceType.MOBILE)
                ? jwtUtil.getMobileRefreshExpirationMs()
                : jwtUtil.getWebRefreshExpirationMs();

        String accessToken = jwtUtil.generateAccessToken(account.getId(), account.getEmail(), account.getRole(),
                sessionId);
        String refreshToken = jwtUtil.generateRefreshToken(account.getId(), sessionId, refreshExpirationMs);

        tokenStoreService.createRefreshSession(
                sessionId,
                account.getId(),
                account.getPhoneNumber(),
                deviceId,
                deviceType,
                refreshToken,
                userAgent,
                ipAddress,
                refreshExpirationMs / 1000);

        return TokenResponse.of(accessToken, refreshToken, refreshExpirationMs);
    }
}
