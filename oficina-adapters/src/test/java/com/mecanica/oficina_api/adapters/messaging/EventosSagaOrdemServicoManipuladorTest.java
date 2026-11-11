package com.mecanica.oficina_api.adapters.messaging;

import com.mecanica.oficina_api.application.ordemservico.usecase.ProcessarEventoSagaUseCase;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EventosSagaOrdemServicoManipuladorTest {

    private final ProcessarEventoSagaUseCase useCase = mock(ProcessarEventoSagaUseCase.class);
    private final EventosSagaOrdemServicoManipulador manipulador = new EventosSagaOrdemServicoManipulador(useCase);

    private static EventoEnvelope evento(String payload) {
        return new EventoEnvelope("ev-1", "CobrancaCriada", 1, Instant.now(), "oficina-billing-service", "os-1",
                payload == null ? null : JsonMapper.builder().build().readTree(payload));
    }

    @Test
    void declaraOsEventosDoCatalogoDaOs() {
        assertThat(manipulador.eventTypes()).isEqualTo(ProcessarEventoSagaUseCase.EVENTOS_CONSUMIDOS);
    }

    @Test
    void repassaTipoOsEPayloadComoDetalhe() {
        when(useCase.executar("CobrancaCriada", "os-1", "{\"checkoutUrl\":\"https://mp\"}"))
                .thenReturn(ProcessarEventoSagaUseCase.Resultado.TRANSICAO_APLICADA);

        manipulador.tratar(evento("{\"checkoutUrl\":\"https://mp\"}"));

        verify(useCase).executar("CobrancaCriada", "os-1", "{\"checkoutUrl\":\"https://mp\"}");
    }

    @Test
    void eventoDescartadoNaoLancaErro() {
        when(useCase.executar("CobrancaCriada", "os-1", null))
                .thenReturn(ProcessarEventoSagaUseCase.Resultado.TRANSICAO_IGNORADA);

        manipulador.tratar(evento(null));

        verify(useCase).executar("CobrancaCriada", "os-1", null);
    }
}
