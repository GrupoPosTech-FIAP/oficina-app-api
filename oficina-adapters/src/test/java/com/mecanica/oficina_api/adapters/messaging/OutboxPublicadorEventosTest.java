package com.mecanica.oficina_api.adapters.messaging;

import com.mecanica.oficina_api.adapters.persistence.OutboxEventoJpaEntity;
import com.mecanica.oficina_api.adapters.persistence.repository.OutboxEventoSpringDataRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Import({MensageriaConfig.class, OutboxPublicadorEventos.class})
class OutboxPublicadorEventosTest {

    @Autowired
    private OutboxPublicadorEventos publicador;

    @Autowired
    private OutboxEventoSpringDataRepository repository;

    @Autowired
    private TransactionTemplate transacao;

    @Autowired
    private JsonMapper jsonMapper;

    @AfterEach
    void limpar() {
        repository.deleteAll();
    }

    @Test
    void gravaEventoPendenteNaOutboxComEnvelopeCompleto() {
        transacao.executeWithoutResult(s ->
                publicador.publicar("DiagnosticoConcluido", "os-1", Map.of("clienteId", "cli-1")));

        List<OutboxEventoJpaEntity> eventos = repository.findAll();
        assertThat(eventos).hasSize(1);
        OutboxEventoJpaEntity evento = eventos.getFirst();
        assertThat(evento.getEventType()).isEqualTo("DiagnosticoConcluido");
        assertThat(evento.getSagaId()).isEqualTo("os-1");
        assertThat(evento.getPublicadoEm()).isNull();
        assertThat(evento.getTentativas()).isZero();

        EventoEnvelope envelope = jsonMapper.readValue(evento.getEnvelope(), EventoEnvelope.class);
        assertThat(envelope.eventId()).isEqualTo(evento.getEventId());
        assertThat(envelope.eventType()).isEqualTo("DiagnosticoConcluido");
        assertThat(envelope.eventVersion()).isEqualTo(EventoEnvelope.VERSAO_ATUAL);
        assertThat(envelope.producer()).isEqualTo("oficina-app-api");
        assertThat(envelope.sagaId()).isEqualTo("os-1");
        assertThat(envelope.occurredAt()).isEqualTo(evento.getCriadoEm());
        assertThat(envelope.payload().get("clienteId").asString()).isEqualTo("cli-1");
    }

    @Test
    void payloadNuloViraObjetoVazio() {
        transacao.executeWithoutResult(s -> publicador.publicar("OrcamentoGerado", "os-2", null));

        EventoEnvelope envelope = jsonMapper.readValue(
                repository.findAll().getFirst().getEnvelope(), EventoEnvelope.class);
        assertThat(envelope.payload().isObject()).isTrue();
        assertThat(envelope.payload().isEmpty()).isTrue();
    }

    @Test
    void eventoNaoSobreviveAoRollbackDaTransacao() {
        transacao.executeWithoutResult(s -> {
            publicador.publicar("DiagnosticoConcluido", "os-1", Map.of());
            s.setRollbackOnly();
        });

        assertThat(repository.count()).isZero();
    }

    @Test
    void exigeTransacaoAberta() {
        assertThatThrownBy(() -> publicador.publicar("DiagnosticoConcluido", "os-1", Map.of()))
                .isInstanceOf(IllegalTransactionStateException.class);
        assertThat(repository.count()).isZero();
    }
}
