package com.ksa.devicemanagement.controller;

import com.ksa.devicemanagement.domain.Device;
import com.ksa.devicemanagement.domain.DeviceState;
import com.ksa.devicemanagement.dto.PatchValue;
import com.ksa.devicemanagement.generated.model.DevicePatchRequest;
import com.ksa.devicemanagement.generated.model.DeviceRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static com.ksa.devicemanagement.generated.model.DeviceStateEnum.*;
import static com.ksa.devicemanagement.generated.model.DeviceStateEnum.AVAILABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

class DeviceMapperTest {

    private final DeviceMapper sut = Mappers.getMapper(DeviceMapper.class);

    @Test
    @DisplayName("should map device request to create command")
    void shouldMapDeviceRequestToCreateCommand() {
        DeviceRequest request = request();

        var result = sut.toCreateCommand(request);

        assertEquals("Router", result.name());
        assertEquals("Cisco", result.brand());
        assertEquals(DeviceState.AVAILABLE, result.state());
    }

    @Test
    @DisplayName("should map device request to replace command")
    void shouldMapDeviceRequestToReplaceCommand() {
        DeviceRequest request = request();

        var result = sut.toReplaceCommand(request);

        assertEquals("Router", result.name());
        assertEquals("Cisco", result.brand());
        assertEquals(DeviceState.AVAILABLE, result.state());
    }

    @Test
    @DisplayName("should map missing patch fields to undefined")
    void shouldMapMissingPatchFieldsToUndefined() {
        DevicePatchRequest request = new DevicePatchRequest();

        var result = sut.toPatchCommand(request);

        assertInstanceOf(PatchValue.Undefined.class, result.name());
        assertInstanceOf(PatchValue.Undefined.class, result.brand());
        assertInstanceOf(PatchValue.Undefined.class, result.state());
    }

    @Test
    @DisplayName("should map explicit null patch fields to present null")
    void shouldMapExplicitNullPatchFieldsToPresentNull() {
        DevicePatchRequest request = new DevicePatchRequest()
                .name(null)
                .brand(null)
                .state(null);

        var result = sut.toPatchCommand(request);

        assertPresent(null, result.name());
        assertPresent(null, result.brand());
        assertPresent(null, result.state());
    }

    @Test
    @DisplayName("should map patch values to present domain values")
    void shouldMapPatchValuesToPresentDomainValues() {
        DevicePatchRequest request = new DevicePatchRequest()
                .name("Switch")
                .brand("Juniper")
                .state(IN_USE);

        var result = sut.toPatchCommand(request);

        assertPresent("Switch", result.name());
        assertPresent("Juniper", result.brand());
        assertPresent(DeviceState.IN_USE, result.state());
    }

    @Test
    @DisplayName("should map device to api response using utc")
    void shouldMapDeviceToApiResponseUsingUtc() {
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-09-04T12:30:45Z");
        Device device = new Device(id, "Router", "Cisco", DeviceState.AVAILABLE);
        device.setCreatedAt(createdAt);
        device.setVersion(3L);

        var result = sut.toResponse(device);

        assertEquals(id, result.getId());
        assertEquals("Router", result.getName());
        assertEquals("Cisco", result.getBrand());
        assertEquals(AVAILABLE, result.getState());
        assertEquals(createdAt.atOffset(ZoneOffset.UTC), result.getCreationTime());
        assertEquals(3L, result.getVersion());
    }

    @Test
    void shouldReturnNullForNullSource() {
        assertNull(sut.toCreateCommand(null));
        assertNull(sut.toReplaceCommand(null));
        assertNull(sut.toPatchCommand(null));
        assertNull(sut.toResponse(null));
    }

    private static DeviceRequest request() {
        return new DeviceRequest(
                "Router",
                "Cisco",
                AVAILABLE);
    }

    private static void assertPresent(Object expected, PatchValue<?> actual) {
        PatchValue.Present<?> present = assertInstanceOf(PatchValue.Present.class, actual);
        assertEquals(expected, present.value());
    }
}
