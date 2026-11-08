package com.mecanica.oficina_api.adapters.messaging;

import com.mecanica.oficina_api.adapters.persistence.OutboxEventoJpaEntity;
import com.mecanica.oficina_api.adapters.persistence.repository.OutboxEventoSpringDataRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static com.mecanica.oficina_api.adapters.messaging.MensageriaFixture.JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OutboxPublicadorEventosPrecisaoTest {

    @Test
    void instanteETruncadoParaMicrossegundosNoEnvelopeENaOutbox() {
        // Relógio com nanossegundos (como no Linux do CI); o banco só guarda microssegundos
        Clock clock = Clock.fixed(Instant.parse("2026-11-08T01:35:26.265208899Z"), ZoneOffset.UTC);
        OutboxEventoSpringDataRepository repository = mock(OutboxEventoSpringDataRepository.class);
        OutboxPublicadorEventos publicador =
                new OutboxPublicadorEventos(repository, JSON, MensageriaFixture.properties(), clock);

        publicador.publicar("DiagnosticoConcluido", "os-1", Map.of());

        ArgumentCaptor<OutboxEventoJpaEntity> captor = ArgumentCaptor.forClass(OutboxEventoJpaEntity.class);
        verify(repository).save(captor.capture());
        Instant esperado = Instant.parse("2026-11-08T01:35:26.265208Z");
        assertThat(captor.getValue().getCriadoEm()).isEqualTo(esperado);
        assertThat(JSON.readValue(captor.getValue().getEnvelope(), EventoEnvelope.class).occurredAt())
                .isEqualTo(esperado);
    }
}
