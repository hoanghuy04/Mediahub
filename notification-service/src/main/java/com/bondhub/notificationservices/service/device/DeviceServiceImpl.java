package com.bondhub.notificationservices.service.device;

import com.bondhub.notificationservices.dto.request.DeviceTokenRequest;
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

    @Override
    @Transactional
    public void registerDevice(DeviceTokenRequest request) {
        log.info("Registering device for user: {}, type: {}", request.userId(), request.platform());
        
        userDeviceRepository.findByUserIdAndFcmToken(request.userId(), request.token())
                .ifPresentOrElse(
                        existingDevice -> {
                            log.info("Device already exists for user: {}", request.userId());
                        },
                        () -> {
                            UserDevice specificDevice = UserDevice.builder()
                                    .userId(request.userId())
                                    .fcmToken(request.token())
                                    .platform(request.platform())
                                    .build();
                            userDeviceRepository.save(specificDevice);
                            log.info("New device registered for user: {}", request.userId());
                        }
                );
    }

    @Override
    @Transactional
    public void unregisterDevice(String userId, String token) {
        log.info("Unregistering device for user: {}", userId);
        userDeviceRepository.deleteByUserIdAndFcmToken(userId, token);
    }

    @Override
    public List<UserDevice> getDevicesForUser(String userId) {
        return userDeviceRepository.findByUserId(userId);
    }
}
