package com.ksa.devicemanagement.dto;

import com.ksa.devicemanagement.domain.DeviceState;

public record ReplaceDeviceCommand(
        String name,
        String brand,
        DeviceState state
) {}
