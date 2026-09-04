package com.ksa.devicemanagement.service;

import com.ksa.devicemanagement.domain.Device;
import com.ksa.devicemanagement.domain.DeviceState;
import com.ksa.devicemanagement.dto.CreateDeviceCommand;
import com.ksa.devicemanagement.dto.PatchValue;
import com.ksa.devicemanagement.dto.ReplaceDeviceCommand;
import com.ksa.devicemanagement.dto.UpdateDeviceCommand;
import com.ksa.devicemanagement.repository.DeviceRepository;
import com.ksa.devicemanagement.validator.BusinessRulesValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceServiceImplTest {

    @Mock
    private DeviceRepository repository;
    @Mock
    private IdGenerator idGenerator;
    @Mock
    private BusinessRulesValidator validator;
    @InjectMocks
    private DeviceServiceImpl sut;

    @Test
    @DisplayName("Create a device")
    void shouldCreateDevice() {
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-09-04T12:00:00Z");
        CreateDeviceCommand command = new CreateDeviceCommand(
                "Router",
                "Cisco",
                DeviceState.AVAILABLE);

        when(idGenerator.generate()).thenReturn(id);
        when(repository.save(any(Device.class)))
                .thenAnswer(invocation -> {
                    var arg = (Device) invocation.getArgument(0);
                    arg.setCreatedAt(createdAt);
                    return arg;
                });

        Device result = sut.create(command);

        assertEquals(id, result.getId());
        assertEquals("Router", result.getName());
        assertEquals("Cisco", result.getBrand());
        assertEquals(DeviceState.AVAILABLE, result.getState());
        assertEquals(createdAt, result.getCreatedAt());
        verify(repository).save(result);
    }

    @Test
    @DisplayName("Get device by id")
    void shouldGetDevice() {
        UUID id = UUID.randomUUID();
        Device device = device(id);
        when(repository.findById(id)).thenReturn(Optional.of(device));

        Device result = sut.get(id);

        assertSame(device, result);
        verify(repository).findById(id);
    }

    @Test
    @DisplayName("Replace all mutable device fields by id")
    void shouldUpdateWithReplaceCommand() {
        UUID id = UUID.randomUUID();
        Device device = device(id);
        ReplaceDeviceCommand command = new ReplaceDeviceCommand(
                "Switch",
                "Juniper",
                DeviceState.INACTIVE);

        when(repository.findById(id)).thenReturn(Optional.of(device));
        when(repository.save(device)).thenReturn(device);

        Device result = sut.update(id, command);

        assertSame(device, result);
        assertEquals("Switch", result.getName());
        assertEquals("Juniper", result.getBrand());
        assertEquals(DeviceState.INACTIVE, result.getState());
        verify(validator).validateUpdate(device, command);
        verify(repository).save(device);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("patchCommands")
    @DisplayName("Patch only fields present in the command")
    void shouldUpdateWithPatchCommand(
            String description,
            UpdateDeviceCommand command,
            String expectedName,
            String expectedBrand,
            DeviceState expectedState
    ) {
        UUID id = UUID.randomUUID();
        Device device = device(id);

        when(repository.findById(id)).thenReturn(Optional.of(device));
        when(repository.save(device)).thenReturn(device);

        Device result = sut.update(id, command);

        assertSame(device, result);
        assertEquals(expectedName, result.getName());
        assertEquals(expectedBrand, result.getBrand());
        assertEquals(expectedState, result.getState());
        verify(validator).validateUpdate(device, command);
        verify(repository).save(device);
    }

    @Test
    @DisplayName("Delete device by id")
    void shouldDelete() {
        UUID id = UUID.randomUUID();
        Device device = device(id);
        when(repository.findById(id)).thenReturn(Optional.of(device));

        sut.delete(id);

        verify(validator).validateDelete(device);
        verify(repository).delete(device);
        verify(repository, never()).save(any());
    }

    private static Device device(UUID id) {
        return new Device(
                id,
                "Router",
                "Cisco",
                DeviceState.AVAILABLE);
    }

    private static Stream<Arguments> patchCommands() {
        return Stream.of(
                Arguments.of(
                        "updates only name",
                        new UpdateDeviceCommand(
                                PatchValue.present("Switch"),
                                PatchValue.undefined(),
                                PatchValue.undefined()),
                        "Switch",
                        "Cisco",
                        DeviceState.AVAILABLE),
                Arguments.of(
                        "updates only brand",
                        new UpdateDeviceCommand(
                                PatchValue.undefined(),
                                PatchValue.present("Juniper"),
                                PatchValue.undefined()),
                        "Router",
                        "Juniper",
                        DeviceState.AVAILABLE),
                Arguments.of(
                        "updates only state",
                        new UpdateDeviceCommand(
                                PatchValue.undefined(),
                                PatchValue.undefined(),
                                PatchValue.present(DeviceState.IN_USE)),
                        "Router",
                        "Cisco",
                        DeviceState.IN_USE),
                Arguments.of(
                        "updates brand and state",
                        new UpdateDeviceCommand(
                                PatchValue.undefined(),
                                PatchValue.present("Juniper"),
                                PatchValue.present(DeviceState.IN_USE)),
                        "Router",
                        "Juniper",
                        DeviceState.IN_USE));
    }
}
