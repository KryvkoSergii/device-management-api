package com.ksa.devicemanagement;

import com.ksa.devicemanagement.repository.DeviceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class DeviceApiIntegrationTest {

    private static final String DEVICES_URL = "/api/v1/devices";
    private static final MediaType MERGE_PATCH_JSON =
            MediaType.parseMediaType("application/merge-patch+json");

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired MockMvc mvc;
    @Autowired DeviceRepository repository;
    @Autowired ObjectMapper objectMapper;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("creates new device")
    void createsNewDevice() throws Exception {
        mvc.perform(post(DEVICES_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deviceJson("Router", "Cisco", "available")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        matchesPattern(".*/api/v1/devices/[0-9a-f-]{36}$")))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name", is("Router")))
                .andExpect(jsonPath("$.brand", is("Cisco")))
                .andExpect(jsonPath("$.state", is("available")))
                .andExpect(jsonPath("$.creationTime").isNotEmpty())
                .andExpect(jsonPath("$.version", is(0)));
    }

    @Test
    @DisplayName("fully updates existing device")
    void fullyUpdatesExistingDevice() throws Exception {
        String id = createDevice("Router", "Cisco", "available").get("id").asString();

        mvc.perform(put(DEVICES_URL + "/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deviceJson("Core switch", "Juniper", "inactive")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(id)))
                .andExpect(jsonPath("$.name", is("Core switch")))
                .andExpect(jsonPath("$.brand", is("Juniper")))
                .andExpect(jsonPath("$.state", is("inactive")));
    }

    @Test
    @DisplayName("partially updates only provided fields")
    void partiallyUpdatesOnlyProvidedFields() throws Exception {
        String id = createDevice("Router", "Cisco", "available").get("id").asString();

        mvc.perform(patch(DEVICES_URL + "/{id}", id)
                        .contentType(MERGE_PATCH_JSON)
                        .content("{\"brand\":\"Juniper\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(id)))
                .andExpect(jsonPath("$.name", is("Router")))
                .andExpect(jsonPath("$.brand", is("Juniper")))
                .andExpect(jsonPath("$.state", is("available")));
    }

    @Test
    @DisplayName("fetches single device")
    void fetchesSingleDevice() throws Exception {
        String id = createDevice("Access point", "Ubiquiti", "available").get("id").asString();

        mvc.perform(get(DEVICES_URL + "/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(id)))
                .andExpect(jsonPath("$.name", is("Access point")))
                .andExpect(jsonPath("$.brand", is("Ubiquiti")))
                .andExpect(jsonPath("$.state", is("available")));
    }

    @Test
    @DisplayName("fetches all devices")
    void fetchesAllDevices() throws Exception {
        createDevice("Router", "Cisco", "available");
        createDevice("Switch", "Juniper", "inactive");

        mvc.perform(get(DEVICES_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[*].name", containsInAnyOrder("Router", "Switch")))
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(20)))
                .andExpect(jsonPath("$.hasNext", is(false)));
    }

    @Test
    @DisplayName("fetches devices with maximum page size")
    void acceptsMaximumPageSize() throws Exception {
        mvc.perform(get(DEVICES_URL)
                        .param("page", "0")
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(100)));
    }

    @Test
    @DisplayName("indicates whether another slice is available")
    void reportsNextSlice() throws Exception {
        createDevice("Router", "Cisco", "available");
        createDevice("Switch", "Juniper", "inactive");
        createDevice("Firewall", "Fortinet", "available");

        mvc.perform(get(DEVICES_URL)
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.hasNext", is(true)));

        mvc.perform(get(DEVICES_URL)
                        .param("page", "1")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.hasNext", is(false)));
    }

    @Test
    @DisplayName("rejects invalid pagination parameters")
    void rejectsInvalidPaginationParameters() throws Exception {
        mvc.perform(get(DEVICES_URL).param("page", "-1"))
                .andExpect(status().isBadRequest());

        mvc.perform(get(DEVICES_URL).param("size", "0"))
                .andExpect(status().isBadRequest());

        mvc.perform(get(DEVICES_URL).param("size", "101"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("fetches devices by brand ignoring case")
    void fetchesDevicesByBrandIgnoringCase() throws Exception {
        createDevice("Router", "Cisco", "available");
        createDevice("Switch", "Juniper", "available");
        createDevice("Firewall", "Cisco", "inactive");

        mvc.perform(get(DEVICES_URL).param("brand", "cIsCo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[*].name", containsInAnyOrder("Router", "Firewall")))
                .andExpect(jsonPath("$.hasNext", is(false)));
    }

    @Test
    @DisplayName("fetches devices by state")
    void fetchesDevicesByState() throws Exception {
        createDevice("Router", "Cisco", "available");
        createDevice("Switch", "Juniper", "in-use");
        createDevice("Firewall", "Fortinet", "inactive");

        mvc.perform(get(DEVICES_URL).param("state", "in-use"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].name", is("Switch")))
                .andExpect(jsonPath("$.items[0].state", is("in-use")))
                .andExpect(jsonPath("$.hasNext", is(false)));
    }

    @Test
    @DisplayName("deletes single device")
    void deletesSingleDevice() throws Exception {
        String id = createDevice("Router", "Cisco", "available").get("id").asString();

        mvc.perform(delete(DEVICES_URL + "/{id}", id))
                .andExpect(status().isNoContent());

        mvc.perform(get(DEVICES_URL + "/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("rejects name and brand updates while device is in use")
    void rejectsNameAndBrandUpdatesWhileDeviceIsInUse() throws Exception {
        String id = createDevice("Router", "Cisco", "in-use").get("id").asString();

        mvc.perform(patch(DEVICES_URL + "/{id}", id)
                        .contentType(MERGE_PATCH_JSON)
                        .content("{\"name\":\"Changed router\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("DEVICE_IN_USE")));

        mvc.perform(patch(DEVICES_URL + "/{id}", id)
                        .contentType(MERGE_PATCH_JSON)
                        .content("{\"brand\":\"Juniper\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("DEVICE_IN_USE")));

        mvc.perform(get(DEVICES_URL + "/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Router")))
                .andExpect(jsonPath("$.brand", is("Cisco")));
    }

    @Test
    @DisplayName("rejects full replacement of name or brand while device is in use")
    void rejectsFullReplacementOfNameOrBrandWhileDeviceIsInUse() throws Exception {
        String id = createDevice("Router", "Cisco", "in-use").get("id").asString();

        mvc.perform(put(DEVICES_URL + "/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deviceJson("Changed router", "Juniper", "in-use")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("DEVICE_IN_USE")));

        mvc.perform(get(DEVICES_URL + "/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Router")))
                .andExpect(jsonPath("$.brand", is("Cisco")));
    }

    @Test
    @DisplayName("rejects deletion while device is in use")
    void rejectsDeletionWhileDeviceIsInUse() throws Exception {
        String id = createDevice("Router", "Cisco", "in-use").get("id").asString();

        mvc.perform(delete(DEVICES_URL + "/{id}", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("DEVICE_IN_USE")));

        mvc.perform(get(DEVICES_URL + "/{id}", id))
                .andExpect(status().isOk());
    }

    private JsonNode createDevice(String name, String brand, String state) throws Exception {
        String response = mvc.perform(post(DEVICES_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deviceJson(name, brand, state)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response);
    }

    private static String deviceJson(String name, String brand, String state) {
        return """
                {
                  "name": "%s",
                  "brand": "%s",
                  "state": "%s"
                }
                """.formatted(name, brand, state);
    }
}
