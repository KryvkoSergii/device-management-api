package com.ksa.devicemanagement.validator;

import com.ksa.devicemanagement.domain.Device;
import com.ksa.devicemanagement.domain.DeviceState;
import com.ksa.devicemanagement.dto.PatchValue;
import com.ksa.devicemanagement.dto.ReplaceDeviceCommand;
import com.ksa.devicemanagement.dto.UpdateDeviceCommand;
import com.ksa.devicemanagement.exception.DeviceInUseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BusinessRulesValidatorTest {

    private final BusinessRulesValidator sut = new BusinessRulesValidator();

    @ParameterizedTest(name = "in-use device: {0}")
    @MethodSource("forbiddenReplacementCommands")
    @DisplayName("Reject replacing name or brand of in-use device")
    void shouldRejectReplacingNameOrBrandOfInUseDevice(String description, ReplaceDeviceCommand command) {
        Device device = device(DeviceState.IN_USE);

        var msg = assertThrows(DeviceInUseException.class, () -> sut.validateUpdate(device, command));
        assertThat(msg.getMessage()).isEqualTo("Name and brand cannot be changed while the device is in-use");
    }

    @Test
    @DisplayName("Allow replacing only state of in-use device")
    void shouldAllowReplacingOnlyStateOfInUseDevice() {
        Device device = device(DeviceState.IN_USE);
        ReplaceDeviceCommand command = new ReplaceDeviceCommand("Router", "Cisco", DeviceState.INACTIVE);

        assertDoesNotThrow(() -> sut.validateUpdate(device, command));
    }

    @ParameterizedTest
    @EnumSource(value = DeviceState.class, names = "IN_USE", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("Allow replacing device that is not in use")
    void shouldAllowReplacingDeviceThatIsNotInUse(DeviceState currentState) {
        Device device = device(currentState);
        ReplaceDeviceCommand command = new ReplaceDeviceCommand("Switch", "Juniper", DeviceState.IN_USE);

        assertDoesNotThrow(() -> sut.validateUpdate(device, command));
    }

    @Test
    @DisplayName("Reject empty update command")
    void shouldRejectEmptyPatch() {
        Device device = device(DeviceState.AVAILABLE);
        UpdateDeviceCommand command = new UpdateDeviceCommand(
                PatchValue.undefined(),
                PatchValue.undefined(),
                PatchValue.undefined());

        var msg = assertThrows(IllegalArgumentException.class, () -> sut.validateUpdate(device, command));
        assertThat(msg.getMessage()).isEqualTo("Must contain at least one field");
    }

    @ParameterizedTest(name = "in-use device: {0}")
    @MethodSource("forbiddenPatchCommands")
    @DisplayName("Reject patching name or brand of in-use device")
    void shouldRejectPatchingNameOrBrandOfInUseDevice(String description,
                                                      UpdateDeviceCommand command,
                                                      String errorMessage) {
        Device device = device(DeviceState.IN_USE);

        var msg = assertThrows(DeviceInUseException.class, () -> sut.validateUpdate(device, command));
        assertThat(msg.getMessage()).isEqualTo(errorMessage);
    }

    @Test
    @DisplayName("Allow patching only state of in-use device")
    void shouldAllowPatchingOnlyStateOfInUseDevice() {
        Device device = device(DeviceState.IN_USE);
        UpdateDeviceCommand command = new UpdateDeviceCommand(
                PatchValue.undefined(),
                PatchValue.undefined(),
                PatchValue.present(DeviceState.INACTIVE));

        assertDoesNotThrow(() -> sut.validateUpdate(device, command));
    }

    @Test
    @DisplayName("Reject deleting in-use device")
    void shouldRejectDeletingInUseDevice() {
        Device device = device(DeviceState.IN_USE);

        var msg = assertThrows(DeviceInUseException.class, () -> sut.validateDelete(device));
        assertThat(msg.getMessage()).isEqualTo("Device cannot be deleted while it is in-use");
    }

    @ParameterizedTest
    @EnumSource(value = DeviceState.class, names = "IN_USE", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("Allow deleting device that is not in use")
    void shouldAllowDeletingDeviceThatIsNotInUse(DeviceState currentState) {
        Device device = device(currentState);

        assertDoesNotThrow(() -> sut.validateDelete(device));
    }

    private static Stream<Arguments> forbiddenReplacementCommands() {
        return Stream.of(
                Arguments.of(
                        "name changed",
                        new ReplaceDeviceCommand("Switch", "Cisco", DeviceState.IN_USE)),
                Arguments.of(
                        "brand changed",
                        new ReplaceDeviceCommand("Router", "Juniper", DeviceState.IN_USE)));
    }

    private static Stream<Arguments> forbiddenPatchCommands() {
        return Stream.of(
                Arguments.of(
                        "name changed",
                        new UpdateDeviceCommand(
                                PatchValue.present("Switch"),
                                PatchValue.undefined(),
                                PatchValue.undefined()),
                        "Name cannot be changed while the device is in-use"),
                Arguments.of(
                        "brand changed",
                        new UpdateDeviceCommand(
                                PatchValue.undefined(),
                                PatchValue.present("Juniper"),
                                PatchValue.undefined()),
                        "Brand cannot be changed while the device is in-use"));
    }

    private static Device device(DeviceState state) {
        return new Device(
                UUID.randomUUID(),
                "Router",
                "Cisco",
                state);
    }
}
