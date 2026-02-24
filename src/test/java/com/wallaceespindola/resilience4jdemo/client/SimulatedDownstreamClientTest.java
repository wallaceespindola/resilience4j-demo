package com.wallaceespindola.resilience4jdemo.client;

import com.wallaceespindola.resilience4jdemo.dto.RecordDto;
import com.wallaceespindola.resilience4jdemo.fault.FaultInjectionSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SimulatedDownstreamClient Tests")
class SimulatedDownstreamClientTest {

    @Mock private FaultInjectionSettings settings;

    @InjectMocks private SimulatedDownstreamClient client;

    private final AtomicInteger attempted = new AtomicInteger();
    private final AtomicInteger failed    = new AtomicInteger();

    @BeforeEach
    void setUp() {
        when(settings.getTotalCallsAttempted()).thenReturn(attempted);
        when(settings.getTotalCallsFailed()).thenReturn(failed);
        when(settings.isForceHttp500()).thenReturn(false);
        when(settings.getFixedDelayMs()).thenReturn(0L);
        when(settings.getRandomDelayMaxMs()).thenReturn(0L);
        when(settings.isForceTimeout()).thenReturn(false);
        when(settings.getErrorRate()).thenReturn(0);
    }

    @Test
    @DisplayName("fetchPage returns correct number of records")
    void fetchPage_noFaults_returnsCorrectSize() {
        List<RecordDto> records = client.fetchPage(0, 5);
        assertThat(records).hasSize(5);
    }

    @Test
    @DisplayName("fetchPage records have expected fields")
    void fetchPage_noFaults_recordsHaveFields() {
        List<RecordDto> records = client.fetchPage(2, 3);
        assertThat(records).allSatisfy(r -> {
            assertThat(r.externalId()).isNotBlank();
            assertThat(r.name()).isNotBlank();
            assertThat(r.category()).isNotBlank();
        });
    }

    @Test
    @DisplayName("fetchPage increments totalCallsAttempted")
    void fetchPage_incrementsAttempted() {
        client.fetchPage(0, 1);
        assertThat(attempted.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("fetchPage throws when forceHttp500 is set")
    void fetchPage_forceHttp500_throws() {
        when(settings.isForceHttp500()).thenReturn(true);
        assertThatThrownBy(() -> client.fetchPage(0, 5))
                .isInstanceOf(SimulatedServerException.class)
                .hasMessageContaining("Forced HTTP 500");
        assertThat(failed.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("fetchPage throws with errorRate=100")
    void fetchPage_errorRate100_throws() {
        when(settings.getErrorRate()).thenReturn(100);
        assertThatThrownBy(() -> client.fetchPage(0, 1))
                .isInstanceOf(SimulatedServerException.class)
                .hasMessageContaining("Random failure");
    }

    @Test
    @DisplayName("fetchMetadata returns known metadata value")
    void fetchMetadata_knownKey_returnsValue() {
        String value = client.fetchMetadata("region");
        assertThat(value).isEqualTo("EU-WEST-1");
    }

    @Test
    @DisplayName("fetchMetadata returns default for unknown key")
    void fetchMetadata_unknownKey_returnsDefault() {
        String value = client.fetchMetadata("nonexistent");
        assertThat(value).startsWith("unknown-");
    }

    @Test
    @DisplayName("fetchMetadata throws when forceHttp500 is set")
    void fetchMetadata_forceHttp500_throws() {
        when(settings.isForceHttp500()).thenReturn(true);
        assertThatThrownBy(() -> client.fetchMetadata("region"))
                .isInstanceOf(SimulatedServerException.class);
    }

    @Test
    @DisplayName("fetchMetadata increments totalCallsAttempted")
    void fetchMetadata_incrementsAttempted() {
        client.fetchMetadata("env");
        assertThat(attempted.get()).isEqualTo(1);
    }
}
