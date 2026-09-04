package com.ksa.devicemanagement.config;

import com.ksa.devicemanagement.generated.model.DeviceStateEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DeviceStateEnumConverterTest {

    private final DeviceStateEnumConverter converter = new DeviceStateEnumConverter();

    @ParameterizedTest
    @CsvSource({
            "available, AVAILABLE",
            "in-use, IN_USE",
            "inactive, INACTIVE"
    })
    @DisplayName("converts generated model to DeviceStateEnum")
    void convertsOpenApiWireValue(String value, DeviceStateEnum expected) {
        assertEquals(expected, converter.convert(value));
    }
}
