package com.mecanica.oficina_api.adapters.messaging;

import com.mecanica.oficina_api.adapters.persistence.EventoProcessadoJpaEntity;
import com.mecanica.oficina_api.adapters.persistence.repository.EventoProcessadoSpringDataRepository;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionOperations;

import static com.mecanica.oficina_api.adapters.messaging.MensageriaFixture.AGORA;
import static com.mecanica.oficina_api.adapters.messaging.MensageriaFixture.envelope;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessadorEventosTest {

    private EventoProcessadoSpringDataRepository repository;
    private ManipuladorFake manipulador;
    private ProcessadorEventos processador;

    static class ManipuladorFake implements ManipuladorEvento {
        final List<EventoEnvelope> recebidos = new ArrayList<>();
        RuntimeException falha;

        @Override
        public String eventType() {
            return "OrcamentoGerado";
        }

        @Override
        public void tratar(EventoEnvelope evento) {
            if (falha != null) throw falha;
            recebidos.add(evento);
        }
    }

    @BeforeEach
    void setUp() {
        repository = mock(EventoProcessadoSpringDataRepository.class);
        manipulador = new ManipuladorFake();
        processador = new ProcessadorEventos(List.of(manipulador), repository,
                TransactionOperations.withoutTransaction(), Clock.fixed(AGORA, ZoneOffset.UTC));
    }

    @Test
    void entregaAoManipuladorDoTipoERegistraComoProcessado() {
        EventoEnvelope evento = envelope("ev-1", "OrcamentoGerado");

        assertThat(processador.processar(evento)).isEqualTo(ProcessadorEventos.Resultado.PROCESSADO);

        assertThat(manipulador.recebidos).containsExactly(evento);
        ArgumentCaptor<EventoProcessadoJpaEntity> captor = ArgumentCaptor.forClass(EventoProcessadoJpaEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getEventId()).isEqualTo("ev-1");
        assertThat(captor.getValue().getEventType()).isEqualTo("OrcamentoGerado");
        assertThat(captor.getValue().getSagaId()).isEqualTo("os-1");
        assertThat(captor.getValue().getProcessadoEm()).isEqualTo(AGORA);
    }

    @Test
    void eventoRepetidoEIgnoradoSemChamarOManipulador() {
        when(repository.existsById("ev-1")).thenReturn(true);

        assertThat(processador.processar(envelope("ev-1", "OrcamentoGerado")))
                .isEqualTo(ProcessadorEventos.Resultado.DUPLICADO);

        assertThat(manipulador.recebidos).isEmpty();
        verify(repository, never()).save(any());
    }

    @Test
    void eventoSemManipuladorERegistradoParaNaoVoltar() {
        assertThat(processador.processar(envelope("ev-1", "EventoDesconhecido")))
                .isEqualTo(ProcessadorEventos.Resultado.IGNORADO);

        verify(repository).save(any(EventoProcessadoJpaEntity.class));
    }

    @Test
    void falhaDoManipuladorPropagaENaoRegistra() {
        manipulador.falha = new IllegalStateException("transição inválida");

        assertThatThrownBy(() -> processador.processar(envelope("ev-1", "OrcamentoGerado")))
                .isInstanceOf(IllegalStateException.class);

        verify(repository, never()).save(any());
    }
}
