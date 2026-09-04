package com.ksa.devicemanagement.repository;

import com.ksa.devicemanagement.domain.Device;
import com.ksa.devicemanagement.domain.DeviceState;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

public final class DeviceSpecifications {

    private DeviceSpecifications() {
    }

    public static Specification<Device> hasBrand(String brand) {
        return (root, query, builder) -> {
            if (brand == null || brand.isBlank()) {
                return builder.conjunction();
            }

            return builder.equal(
                    builder.lower(root.get("brand")),
                    brand.trim().toLowerCase(Locale.ROOT)
            );
        };
    }

    public static Specification<Device> hasState(DeviceState state) {
        return (root, query, builder) -> {
            if (state == null) {
                return builder.conjunction();
            }

            return builder.equal(root.get("state"), state);
        };
    }
}
