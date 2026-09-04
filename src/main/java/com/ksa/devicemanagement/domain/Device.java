package com.ksa.devicemanagement.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "devices")
@EntityListeners(AuditingEntityListener.class)
public class Device {
    @Id
    private UUID id;
    @Column(nullable = false, length = 200)
    private String name;
    @Column(nullable = false, length = 100)
    private String brand;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeviceState state;
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Version
    private long version;

    public Device(UUID id, String name, String brand, DeviceState state) {
        this.id = id;
        this.name = name;
        this.brand = brand;
        this.state = state;
    }
}
