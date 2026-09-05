package com.ksa.devicemanagement.service;

import com.ksa.devicemanagement.domain.Device;
import com.ksa.devicemanagement.domain.DeviceState;
import com.ksa.devicemanagement.dto.CreateDeviceCommand;
import com.ksa.devicemanagement.dto.ReplaceDeviceCommand;
import com.ksa.devicemanagement.dto.UpdateDeviceCommand;
import com.ksa.devicemanagement.exception.DeviceNotFoundException;
import com.ksa.devicemanagement.repository.DeviceRepository;
import com.ksa.devicemanagement.repository.DeviceSpecifications;
import com.ksa.devicemanagement.validator.BusinessRulesValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@RequiredArgsConstructor
@Service
@Slf4j
public class DeviceServiceImpl implements DeviceService {
    private final DeviceRepository repository;
    private final IdGenerator idGenerator;
    private final BusinessRulesValidator validator;

    @Override
    @Transactional
    public Device create(CreateDeviceCommand command) {
        Objects.requireNonNull(command, "Create command must not be null");

        log.info("Creating device: name={}, brand={}, state={}", command.name(), command.brand(), command.state());

        var device = new Device(idGenerator.generate(),
                command.name(),
                command.brand(),
                command.state());

        device = repository.save(device);

        log.info("Device created successfully with id={}", device.getId());
        return device;
    }

    @Override
    @Transactional(readOnly = true)
    public Device get(UUID id) {
        Objects.requireNonNull(id, "Device id must not be null");
        log.debug("Getting by id={}", id);
        return repository.findById(id)
                .orElseThrow(() -> new DeviceNotFoundException(id));
    }

    @Override
    @Transactional
    public Device update(UUID id, ReplaceDeviceCommand command) {
        Objects.requireNonNull(id, "Device id must not be null");
        Objects.requireNonNull(command, "Update command must not be null");

        log.info("Updating device: id={}, name={}, brand={}, state={}",
                id,
                command.name(),
                command.brand(),
                command.state());

        Device device = repository.findById(id)
                .orElseThrow(() -> new DeviceNotFoundException(id));

        validator.validateUpdate(device, command);

        device.setName(command.name());
        device.setBrand(command.brand());
        device.setState(command.state());

        var updated = repository.save(device);

        log.info("Updated device: id={}", updated.getId());
        return updated;
    }

    @Override
    @Transactional
    public Device update(UUID id, UpdateDeviceCommand command) {
        Objects.requireNonNull(id, "Device id must not be null");
        Objects.requireNonNull(command, "Update command must not be null");

        log.info("Updating device partially: id={}, name={}, brand={}, state={}",
                id,
                command.name(),
                command.brand(),
                command.state());

        Device device = repository.findById(id).orElseThrow(() -> new DeviceNotFoundException(id));

        validator.validateUpdate(device, command);

        command.name().ifPresent(device::setName);
        command.brand().ifPresent(device::setBrand);
        command.state().ifPresent(device::setState);

        var updated = repository.save(device);

        log.info("Updated device partially: id={}", updated.getId());
        return device;
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Objects.requireNonNull(id, "Device id must not be null");

        log.info("Deleting device: id={}", id);

        Device device = repository.findById(id).orElseThrow(() -> new DeviceNotFoundException(id));

        validator.validateDelete(device);

        repository.delete(device);

        log.info("Deleted device: id={}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<Device> find(String brand, DeviceState state, int page, int size) {
        log.debug("Search by brand={} state={}", brand, state);
        Specification<Device> specification =
                Specification.where(DeviceSpecifications.hasBrand(brand))
                        .and(DeviceSpecifications.hasState(state));

        return repository.findBy(
                specification,
                query -> query.slice(PageRequest.of(page, size, Sort.by("id").ascending()))
        );
    }
}
