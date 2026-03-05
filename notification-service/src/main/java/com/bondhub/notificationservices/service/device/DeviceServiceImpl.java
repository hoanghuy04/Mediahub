package com.bondhub.notificationservices.service.device;

import com.bondhub.common.utils.SecurityUtil;
import com.bondhub.notificationservices.dto.request.userdevice.DeviceTokenRequest;
import com.bondhub.notificationservices.model.UserDevice;
import com.bondhub.notificationservices.repository.UserDeviceRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bondhub.notificationservices.enums.Platform;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class DeviceServiceImpl implements DeviceService {

    UserDeviceRepository userDeviceRepository;
    SecurityUtil securityUtil;

    @Override
    @Transactional
    public void registerDevice(DeviceTokenRequest request) {
        String userId = securityUtil.getCurrentUserId();
        log.info("Registering device for user: {}, platform: {}", userId, request.platform());

        userDeviceRepository.deleteByFcmTokenAndUserIdNot(request.token(), userId);

        if (request.platform() == Platform.WEB) {
            userDeviceRepository.deleteByUserIdAndPlatformIn(userId, List.of(Platform.WEB));
        } else {
            userDeviceRepository.deleteByUserIdAndPlatformIn(userId, List.of(Platform.ANDROID, Platform.IOS));
        }

        UserDevice specificDevice = UserDevice.builder()
                .userId(userId)
                .fcmToken(request.token())
                .platform(request.platform())
                .build();
        userDeviceRepository.save(specificDevice);
        log.info("Device registered successfully for user: {} on platform: {}", userId, request.platform());
    }

    @Override
    @Transactional
    public void unregisterDevice(String token) {
        String currentUserId = securityUtil.getCurrentUserId();
        log.info("Unregistering device for user: {}", currentUserId);
        userDeviceRepository.deleteByUserIdAndFcmToken(currentUserId, token);
    }

    @Override
    public List<UserDevice> getDevicesForUser(String userId) {
        return userDeviceRepository.findByUserId(userId);
    }
}
