package com.bondhub.authservice.service.auth;

import com.bondhub.authservice.dto.auth.response.QrGenerationResponse;
import com.bondhub.authservice.dto.auth.response.QrStatusResponse;
import com.bondhub.authservice.enums.QrSessionStatus;
import com.bondhub.authservice.model.redis.QrSession;
import com.bondhub.authservice.repository.AccountRepository;
import com.bondhub.authservice.repository.redis.QrSessionRepository;
import com.bondhub.authservice.service.token.TokenStoreService;
import com.bondhub.authservice.util.SecurityUtil;
import com.bondhub.common.exception.AppException;
import com.bondhub.common.exception.ErrorCode;
import com.bondhub.common.utils.JwtUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class QrAuthenticationServiceImpl implements QrAuthenticationService {

    QrSessionRepository qrSessionRepository;
    AccountRepository accountRepository;
    SecurityUtil securityUtil;
    JwtUtil jwtUtil;
    TokenStoreService tokenStoreService;

    @Value("${qr.expiration-seconds:30}")
    @NonFinal
    long qrTtl;

    @Value("${qr.content-prefix:bondhub://qr/}")
    @NonFinal
    String qrContentPrefix;

    @Override
    public QrGenerationResponse generateQr(String deviceId, String userAgent, String ipAddress) {
        String qrId = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(qrTtl);

        QrSession session = QrSession.builder()
                .id(qrId)
                .status(QrSessionStatus.PENDING)
                .ttl(qrTtl)
                .deviceId(deviceId)
                .userAgent(userAgent)
                .ipAddress(ipAddress)
                .build();

        qrSessionRepository.save(session);
        log.info("Generated QR session: {} for device: {}", qrId, deviceId);

        return QrGenerationResponse.builder()
                .qrId(qrId)
                .expiresAt(expiresAt)
                .qrContent(qrContentPrefix + qrId)
                .build();
    }

    @Override
    public QrStatusResponse checkStatus(String qrId) {
        QrSession session = qrSessionRepository.findById(qrId)
                .orElseThrow(() -> new AppException(ErrorCode.QR_SESSION_EXPIRED));
        return QrStatusResponse.builder()
                .status(session.getStatus())
                .accessToken(session.getWebAccessToken())
                .refreshToken(session.getWebRefreshToken())
                .userAvatar(session.getUserAvatar())
                .userFullName(session.getUserFullName())
                .build();
    }
}
