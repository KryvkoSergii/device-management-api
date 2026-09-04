package com.ksa.devicemanagement.exception;

import lombok.Getter;

@Getter
public class DeviceException extends RuntimeException {

    private final String code;

    public DeviceException(String code, String message) {
        super(message);
        this.code = code;
    }
}
