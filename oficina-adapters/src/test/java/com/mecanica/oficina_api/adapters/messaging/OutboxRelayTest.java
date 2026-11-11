package com.mecanica.oficina_api.adapters.messaging;

import com.mecanica.oficina_api.adapters.persistence.OutboxEventoJpaEntity;
import com.mecanica.oficina_api.adapters.persistence.repository.OutboxEventoSpringDataRepository;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.sns.model.SnsException;

import static com.mecanica.oficina_api.adapters.messaging.MensageriaFixture.AGORA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutboxRelayTest {

    private OutboxEventoSpringDataRepository repository;
    private SnsClient snsClient;
    private OutboxRelay relay;

    @BeforeEach
    void setUp() {
        repository = mock(OutboxEventoSpringDataRepository.class);
        snsClient = mock(SnsClient.class);
        relay = new OutboxRelay(repository, snsClient, MensageriaFixture.properties(),
                Clock.fixed(AGORA, ZoneOffset.UTC));
    }

    private static OutboxEventoJpaEntity pendente(String id, String tipo) {
        return new OutboxEventoJpaEntity(id, tipo, "os-1", "{\"eventId\":\"" + id + "\"}", AGORA, null, 0, null);
    }

    @Test
    void publicaNoTopicoComAtributoEventTypeEMarcaComoPublicado() {
        OutboxEventoJpaEntity evento = pendente("ev-1", "DiagnosticoConcluido");
        when(repository.findByPublicadoEmIsNullOrderByCriadoEmAsc(any(Pageable.class))).thenReturn(List.of(evento));
        when(snsClient.publish(any(PublishRequest.class))).thenReturn(PublishResponse.builder().messageId("m-1").build());

        relay.publicarPendentes();

        ArgumentCaptor<PublishRequest> captor = ArgumentCaptor.forClass(PublishRequest.class);
        verify(snsClient).publish(captor.capture());
        PublishRequest request = captor.getValue();
        assertThat(request.topicArn()).isEqualTo(MensageriaFixture.properties().topicoArn());
        assertThat(request.message()).isEqualTo(evento.getEnvelope());
        assertThat(request.messageAttributes().get(OutboxRelay.ATRIBUTO_EVENT_TYPE).stringValue())
                .isEqualTo("DiagnosticoConcluido");

        assertThat(evento.getPublicadoEm()).isEqualTo(AGORA);
        verify(repository).save(evento);
    }

    @Test
    void falhaEmUmEventoNaoImpedeOsDemaisEFicaPendente() {
        OutboxEventoJpaEntity falha = pendente("ev-1", "DiagnosticoConcluido");
        OutboxEventoJpaEntity ok = pendente("ev-2", "OrcamentoGerado");
        when(repository.findByPublicadoEmIsNullOrderByCriadoEmAsc(any(Pageable.class))).thenReturn(List.of(falha, ok));
        when(snsClient.publish(any(PublishRequest.class)))
                .thenThrow(SnsException.builder().message("throttled").build())
                .thenReturn(PublishResponse.builder().messageId("m-2").build());

        relay.publicarPendentes();

        assertThat(falha.getPublicadoEm()).isNull();
        assertThat(falha.getTentativas()).isEqualTo(1);
        assertThat(falha.getUltimoErro()).contains("throttled");
        assertThat(ok.getPublicadoEm()).isEqualTo(AGORA);
        verify(repository).save(falha);
        verify(repository).save(ok);
    }

    @Test
    void erroLongoETruncado() {
        OutboxEventoJpaEntity evento = pendente("ev-1", "DiagnosticoConcluido");
        when(repository.findByPublicadoEmIsNullOrderByCriadoEmAsc(any(Pageable.class))).thenReturn(List.of(evento));
        when(snsClient.publish(any(PublishRequest.class)))
                .thenThrow(new IllegalStateException("x".repeat(2000)));

        relay.publicarPendentes();

        assertThat(evento.getUltimoErro()).hasSize(1000);
    }

    @Test
    void semPendentesNaoChamaOSns() {
        when(repository.findByPublicadoEmIsNullOrderByCriadoEmAsc(any(Pageable.class))).thenReturn(List.of());

        relay.publicarPendentes();

        verify(snsClient, never()).publish(any(PublishRequest.class));
    }
}
