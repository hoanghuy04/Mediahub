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
        log.info("Registering device for user: {}, type: {}", userId, request.platform());

        userDeviceRepository.deleteByFcmTokenAndUserIdNot(request.token(), userId);

        userDeviceRepository.findByUserIdAndFcmToken(userId, request.token())
                .ifPresentOrElse(
                        existingDevice -> {
                            log.info("Device already exists for user: {}", userId);
                        },
                        () -> {
                            UserDevice specificDevice = UserDevice.builder()
                                    .userId(userId)
                                    .fcmToken(request.token())
                                    .platform(request.platform())
                                    .build();
                            userDeviceRepository.save(specificDevice);
                            log.info("New device registered for user: {}", userId);
                        }
                );
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
