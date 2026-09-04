package com.ksa.devicemanagement.controller;

import com.ksa.devicemanagement.domain.Device;
import com.ksa.devicemanagement.domain.DeviceState;
import com.ksa.devicemanagement.dto.CreateDeviceCommand;
import com.ksa.devicemanagement.dto.PatchValue;
import com.ksa.devicemanagement.dto.ReplaceDeviceCommand;
import com.ksa.devicemanagement.dto.UpdateDeviceCommand;
import com.ksa.devicemanagement.generated.model.DevicePatchRequest;
import com.ksa.devicemanagement.generated.model.DeviceRequest;
import com.ksa.devicemanagement.generated.model.DeviceResponse;
import com.ksa.devicemanagement.generated.model.DeviceStateEnum;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.openapitools.jackson.nullable.JsonNullable;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Mapper(componentModel = "spring")
public interface DeviceMapper {

    CreateDeviceCommand toCreateCommand(DeviceRequest request);

    ReplaceDeviceCommand toReplaceCommand(DeviceRequest request);

    default UpdateDeviceCommand toPatchCommand(DevicePatchRequest request) {
        if (request == null) {
            return null;
        }

        return new UpdateDeviceCommand(
                toPatchValue(request.getName()),
                toPatchValue(request.getBrand()),
                toDomainStatePatchValue(request.getState()));
    }

    default <T> PatchValue<T> toPatchValue(JsonNullable<T> source) {
        if (source == null || !source.isPresent()) {
            return PatchValue.undefined();
        }

        return PatchValue.present(source.orElse(null));
    }

    default PatchValue<com.ksa.devicemanagement.domain.DeviceState> toDomainStatePatchValue(
            JsonNullable<DeviceStateEnum> source
    ) {
        if (source == null || !source.isPresent()) {
            return PatchValue.undefined();
        }

        var state = source.orElse(null);
        return PatchValue.present(state == null
                ? null
                : com.ksa.devicemanagement.domain.DeviceState.fromValue(state.getValue()));
    }

    @Mapping(source = "createdAt", target = "creationTime")
    DeviceResponse toResponse(Device device);

    default DeviceState toDomainState(DeviceStateEnum state) {
        return state == null ? null : DeviceState.fromValue(state.getValue());
    }

    default OffsetDateTime toOffsetDateTime(Instant value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }

}
