package com.bondhub.authservice.dto.device.response;

import java.util.List;

/**
 * Data Transfer Object for grouped device responses.
 * 
 * @param currentDevice the devices with the active session
 * @param otherDevices  the remaining devices
 */
public record DeviceListResponse(
        List<DeviceResponse> currentDevice,
        List<DeviceResponse> otherDevices) {
}
