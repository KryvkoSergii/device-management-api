package com.ksa.devicemanagement.validator;

import com.ksa.devicemanagement.domain.Device;
import com.ksa.devicemanagement.domain.DeviceState;
import com.ksa.devicemanagement.dto.PatchValue;
import com.ksa.devicemanagement.dto.ReplaceDeviceCommand;
import com.ksa.devicemanagement.dto.UpdateDeviceCommand;
import com.ksa.devicemanagement.exception.DeviceInUseException;
import org.springframework.stereotype.Component;

@Component
public class BusinessRulesValidator {

    public void validateUpdate(Device existing, ReplaceDeviceCommand command) {
        if (existing.getState() == DeviceState.IN_USE
                && (!existing.getName().equals(command.name()) || !existing.getBrand().equals(command.brand()))) {
            throw new DeviceInUseException("Name and brand cannot be changed while the device is in-use");
        }
    }

    public void validateUpdate(Device existing, UpdateDeviceCommand command) {

        if (isUndefined(command.name()) && isUndefined(command.brand()) && isUndefined(command.state())) {
            throw new IllegalArgumentException("Must contain at least one field");
        }

        if (existing.getState() == DeviceState.IN_USE) {
            if (command.name() instanceof PatchValue.Present<String>(String name)
                    && !existing.getName().equals(name)) {
                throw new DeviceInUseException("Name cannot be changed while the device is in-use");
            }

            if (command.brand() instanceof PatchValue.Present<String>(String brand)
                    && !existing.getBrand().equals(brand)) {
                throw new DeviceInUseException("Brand cannot be changed while the device is in-use");
            }
        }
    }

    public void validateDelete(Device existing) {
        if (existing.getState() == DeviceState.IN_USE) {
            throw new DeviceInUseException("Device cannot be deleted while it is in-use");
        }
    }

    private static boolean isUndefined(PatchValue<?> value) {
        if (value == null) {
            throw new IllegalArgumentException("Patch value must not be null");
        }
        return value instanceof PatchValue.Undefined<?>;
    }
}
