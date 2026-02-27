package com.bondhub.notificationservices.service.device;

import com.bondhub.notificationservices.dto.request.userdevice.DeviceTokenRequest;
import com.bondhub.notificationservices.model.UserDevice;

import java.util.List;

public interface DeviceService {
    void registerDevice(DeviceTokenRequest request);
    void unregisterDevice(String userId, String token);
    List<UserDevice> getDevicesForUser(String userId);
}
