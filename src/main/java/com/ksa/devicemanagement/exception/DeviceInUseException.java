package com.ksa.devicemanagement.exception;

public class DeviceInUseException extends DeviceException {
    public DeviceInUseException(String message) {
        super("DEVICE_IN_USE", message);
    }
}
