package com.ksa.devicemanagement.config;

import com.ksa.devicemanagement.generated.model.DeviceStateEnum;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class DeviceStateEnumConverter implements Converter<String, DeviceStateEnum> {

    @Override
    public DeviceStateEnum convert(String source) {
        return DeviceStateEnum.fromValue(source);
    }
}
