package com.ksa.devicemanagement.dto;

import com.ksa.devicemanagement.domain.DeviceState;


public record UpdateDeviceCommand(
        PatchValue<String> name,
        PatchValue<String> brand,
        PatchValue<DeviceState> state
) {
}