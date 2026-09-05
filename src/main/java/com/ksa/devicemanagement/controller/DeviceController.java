package com.ksa.devicemanagement.controller;

import com.ksa.devicemanagement.generated.api.DevicesApi;
import com.ksa.devicemanagement.generated.model.*;
import com.ksa.devicemanagement.service.DeviceService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.UUID;

@RequiredArgsConstructor
@RestController
public class DeviceController implements DevicesApi {

    private final DeviceService service;
    private final DeviceMapper mapper;

    @Override
    public ResponseEntity<DeviceResponse> createDevice(DeviceRequest request) {
        var device = service.create(mapper.toCreateCommand(request));
        var response = mapper.toResponse(device);
        return ResponseEntity
                .created(ServletUriComponentsBuilder
                        .fromCurrentRequest()
                        .path("/{id}")
                        .buildAndExpand(response.getId())
                        .toUri()
                )
                .body(response);
    }

    @Override
    public ResponseEntity<DeviceResponse> getDevice(UUID id) {
        return ResponseEntity.ok(mapper.toResponse(service.get(id)));
    }

    @Override
    public ResponseEntity<DeviceSlice> listDevices(@Nullable String brand,
                                                  @Nullable DeviceStateEnum state,
                                                  Integer page, Integer size) {
        var result = service.find(brand, mapper.toDomainState(state), page, size);
        var response = new DeviceSlice(
                result.getContent().stream().map(mapper::toResponse).toList(),
                result.getNumber(),
                result.getSize(),
                result.hasNext());
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<DeviceResponse> replaceDevice(UUID id, DeviceRequest request) {
        var response = service.update(id, mapper.toReplaceCommand(request));
        return ResponseEntity.ok(mapper.toResponse(response));
    }

    @Override
    public ResponseEntity<DeviceResponse> updateDevice(UUID id, DevicePatchRequest request) {
        var response = service.update(id, mapper.toPatchCommand(request));
        return ResponseEntity.ok(mapper.toResponse(response));
    }

    @Override
    public ResponseEntity<Void> deleteDevice(UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
