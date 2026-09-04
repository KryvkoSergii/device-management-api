package com.ksa.devicemanagement.exception;

import java.util.UUID;

public class DeviceNotFoundException extends DeviceException {
    public DeviceNotFoundException(UUID id) {
        super("DEVICE_NOT_FOUND", "Device %s was not found".formatted(id));
    }
}
