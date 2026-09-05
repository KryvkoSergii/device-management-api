package com.ksa.devicemanagement.performance;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

import java.time.Duration;
import java.util.UUID;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class DeviceApiSimulation extends Simulation {

    private static final String BASE_URL =
            System.getProperty("perf.baseUrl", "http://localhost:8080");
    private static final double READ_REQUESTS_PER_SECOND =
            doubleProperty("perf.readRps", 200.0);
    private static final double WRITE_REQUESTS_PER_SECOND =
            doubleProperty("perf.writeRps", 100.0);
    private static final Duration RAMP_DURATION =
            Duration.ofSeconds(longProperty("perf.rampSeconds", 60));
    private static final Duration TEST_DURATION =
            Duration.ofSeconds(longProperty("perf.durationSeconds", 600));

    private final HttpProtocolBuilder httpProtocol = http
            .baseUrl(BASE_URL)
            .acceptHeader("application/json")
            .userAgentHeader("device-management-gatling");

    private final ScenarioBuilder reads = scenario("Read workload")
            .exec(http("List devices by brand")
                    .get("/api/v1/devices")
                    .queryParam("brand", "Cisco")
                    .queryParam("page", "0")
                    .queryParam("size", "20")
                    .check(status().is(200)))
            .exec(http("List devices by state")
                    .get("/api/v1/devices")
                    .queryParam("state", "available")
                    .queryParam("page", "0")
                    .queryParam("size", "20")
                    .check(status().is(200)));

    private final ScenarioBuilder writes = scenario("Write workload")
            .exec(session -> session.set("deviceName", "Gatling-" + UUID.randomUUID()))
            .exec(http("Create device")
                    .post("/api/v1/devices")
                    .header("Content-Type", "application/json")
                    .body(StringBody("""
                            {
                              "name": "#{deviceName}",
                              "brand": "Cisco",
                              "state": "available"
                            }
                            """))
                    .check(status().is(201))
                    .check(jsonPath("$.id").saveAs("deviceId")))
            .exitHereIfFailed()
            .exec(http("Update device")
                    .patch("/api/v1/devices/#{deviceId}")
                    .header("Content-Type", "application/merge-patch+json")
                    .body(StringBody("{\"state\":\"inactive\"}"))
                    .check(status().is(200)));

    public DeviceApiSimulation() {
        // split to POST and PATCH.
        double writeIterationsPerSecond = WRITE_REQUESTS_PER_SECOND / 2.0;
        // split by filters
        double readIterationsPerSecond = READ_REQUESTS_PER_SECOND / 2.0;

        setUp(
                reads.injectOpen(
                        rampUsersPerSec(1.0)
                                .to(readIterationsPerSecond)
                                .during(RAMP_DURATION),
                        constantUsersPerSec(readIterationsPerSecond)
                                .during(TEST_DURATION)),
                writes.injectOpen(
                        rampUsersPerSec(1.0)
                                .to(writeIterationsPerSecond)
                                .during(RAMP_DURATION),
                        constantUsersPerSec(writeIterationsPerSecond)
                                .during(TEST_DURATION))
        )
                .protocols(httpProtocol)
                .assertions(
                        global().successfulRequests().percent().gt(99.9),
                        global().responseTime().percentile(95.0).lt(1_000),
                        details("List devices by brand")
                                .responseTime().percentile(95.0).lt(1_000),
                        details("List devices by state")
                                .responseTime().percentile(95.0).lt(1_000),
                        details("Create device")
                                .responseTime().percentile(95.0).lt(1_000),
                        details("Update device")
                                .responseTime().percentile(95.0).lt(1_000)
                );
    }

    private static double doubleProperty(String name, double defaultValue) {
        return Double.parseDouble(System.getProperty(name, Double.toString(defaultValue)));
    }

    private static long longProperty(String name, long defaultValue) {
        return Long.parseLong(System.getProperty(name, Long.toString(defaultValue)));
    }
}
