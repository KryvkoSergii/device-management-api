package com.ksa.devicemanagement.service;

import com.ksa.devicemanagement.domain.Device;
import com.ksa.devicemanagement.domain.DeviceState;
import com.ksa.devicemanagement.dto.CreateDeviceCommand;
import com.ksa.devicemanagement.dto.ReplaceDeviceCommand;
import com.ksa.devicemanagement.dto.UpdateDeviceCommand;
import org.springframework.data.domain.Page;

import java.util.UUID;

/**
 * Service interface for managing devices.
 */
public interface DeviceService {
    /**
     * Creates a new device.
     *
     * @param command The command containing device information.
     * @return The created device.
     */
    Device create(CreateDeviceCommand command);

    /**
     * Retrieves a device by its Id.
     *
     * @param id The ID of the device to retrieve.
     * @return The retrieved device.
     */
    Device get(UUID id);

    /**
     * Entirely updates a device by its Id.
     *
     * @param id The Id of the device to update.
     * @param command The command containing updated device information.
     * @return The updated device.
     */
    Device update(UUID id, ReplaceDeviceCommand command);

    /**
     * Partially updates a device by its Id.
     *
     * @param id The Id of the device to update.
     * @param command The command containing updated device information.
     * @return The updated device.
     */
    Device update(UUID id, UpdateDeviceCommand command);

    /**
     * Deletes a device by its Id.
     *
     * @param id The Id of the device to delete.
     */
    void delete(UUID id);

    /**
     * Finds devices based on brand and state.
     *
     * @param brand The brand of the device.
     * @param state The state of the device.
     * @param page The page number.
     * @param size The number of items per page.
     * @return A page of devices matching the criteria.
     */
    Page<Device> find(String brand, DeviceState state, int page, int size);
}
