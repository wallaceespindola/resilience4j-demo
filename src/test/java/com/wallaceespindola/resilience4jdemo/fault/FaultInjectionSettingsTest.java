package com.wallaceespindola.resilience4jdemo.fault;

import com.wallaceespindola.resilience4jdemo.dto.FaultSettingsDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FaultInjectionSettings Tests")
class FaultInjectionSettingsTest {

    private FaultInjectionSettings settings;

    @BeforeEach
    void setUp() { settings = new FaultInjectionSettings(); }

    @Test
    @DisplayName("Default state is fully healthy")
    void defaultState_isHealthy() {
        assertThat(settings.getErrorRate()).isEqualTo(0);
        assertThat(settings.getFixedDelayMs()).isEqualTo(0);
        assertThat(settings.isForceHttp500()).isFalse();
        assertThat(settings.isForceTimeout()).isFalse();
        assertThat(settings.isChaosMode()).isFalse();
    }

    @Test
    @DisplayName("applyFlaky sets errorRate to 50")
    void applyFlaky_setsErrorRate50() {
        settings.applyFlaky();
        assertThat(settings.getErrorRate()).isEqualTo(50);
    }

    @Test
    @DisplayName("applySlow sets fixedDelayMs to 2000")
    void applySlow_sets2000ms() {
        settings.applySlow();
        assertThat(settings.getFixedDelayMs()).isEqualTo(2000);
    }

    @Test
    @DisplayName("applyTimeout sets forceTimeout and fixedDelayMs")
    void applyTimeout_setsTimeoutAndDelay() {
        settings.applyTimeout();
        assertThat(settings.isForceTimeout()).isTrue();
        assertThat(settings.getFixedDelayMs()).isGreaterThan(0);
    }

    @Test
    @DisplayName("applyHttp500 sets forceHttp500")
    void applyHttp500_setsFlag() {
        settings.applyHttp500();
        assertThat(settings.isForceHttp500()).isTrue();
    }

    @Test
    @DisplayName("applyChaos sets multiple fault modes")
    void applyChaos_setsMultipleFaults() {
        settings.applyChaos();
        assertThat(settings.isChaosMode()).isTrue();
        assertThat(settings.getErrorRate()).isGreaterThan(0);
        assertThat(settings.getFixedDelayMs()).isGreaterThan(0);
    }

    @Test
    @DisplayName("reset clears all fault settings")
    void reset_clearsAll() {
        settings.applyChaos();
        settings.reset();
        assertThat(settings.getErrorRate()).isEqualTo(0);
        assertThat(settings.getFixedDelayMs()).isEqualTo(0);
        assertThat(settings.isForceHttp500()).isFalse();
        assertThat(settings.isForceTimeout()).isFalse();
        assertThat(settings.isChaosMode()).isFalse();
        assertThat(settings.getTotalCallsAttempted().get()).isEqualTo(0);
    }

    @Test
    @DisplayName("toDto returns a snapshot of current settings")
    void toDto_returnsSnapshot() {
        settings.setErrorRate(30);
        settings.setFixedDelayMs(500);
        FaultSettingsDto dto = settings.toDto();
        assertThat(dto.errorRate()).isEqualTo(30);
        assertThat(dto.fixedDelayMs()).isEqualTo(500);
    }

    @Test
    @DisplayName("applyFrom applies all fields from dto")
    void applyFrom_appliesAllFields() {
        FaultSettingsDto dto = new FaultSettingsDto(75, 1000, 500, true, false, false, 3, false);
        settings.applyFrom(dto);
        assertThat(settings.getErrorRate()).isEqualTo(75);
        assertThat(settings.getFixedDelayMs()).isEqualTo(1000);
        assertThat(settings.isForceTimeout()).isTrue();
        assertThat(settings.getMaxConcurrentDownstream()).isEqualTo(3);
    }
}
