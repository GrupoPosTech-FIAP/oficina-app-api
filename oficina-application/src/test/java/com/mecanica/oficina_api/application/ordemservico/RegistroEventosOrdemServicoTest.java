package com.mecanica.oficina_api.application.ordemservico;

import static com.mecanica.oficina_api.application.ordemservico.OrdemServicoFixture.*;
import static org.mockito.Mockito.*;

import com.mecanica.oficina_api.application.evento.gateway.PublicadorEventosGateway;
import com.mecanica.oficina_api.application.ordemservico.gateway.HistoricoOrdemServicoGateway;
import com.mecanica.oficina_api.application.ordemservico.usecase.ListarHistoricoOrdemServicoUseCase;
import com.mecanica.oficina_api.application.ordemservico.gateway.OrdemServicoGateway;
import com.mecanica.oficina_api.domain.ordemservico.HistoricoOrdemServico;
import com.mecanica.oficina_api.domain.ordemservico.OrdemServicoStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class RegistroEventosOrdemServicoTest {

    private static final Instant AGORA = Instant.parse("2026-11-08T10:00:00Z");

    @Test
    void publicaERegistraNoHistorico() {
        PublicadorEventosGateway publicador = mock(PublicadorEventosGateway.class);
        HistoricoOrdemServicoGateway historico = mock(HistoricoOrdemServicoGateway.class);
        var os = osSemOrcamento(OrdemServicoStatus.CANCELADA);

        new RegistroEventosOrdemServico(publicador, historico, Clock.fixed(AGORA, ZoneOffset.UTC))
                .publicar("ExecucaoFalhou", os, OrdemServicoStatus.EM_EXECUCAO, Map.of("motivo", "x"), "x");

        verify(publicador).publicar("ExecucaoFalhou", OS_ID, Map.of("motivo", "x"));
        verify(historico).registrar(new HistoricoOrdemServico(OS_ID, "ExecucaoFalhou", HistoricoOrdemServico.Origem.PUBLICADO,
                OrdemServicoStatus.EM_EXECUCAO, OrdemServicoStatus.CANCELADA, false, "x", AGORA));
    }

    @Test
    void listaHistoricoDeOsExistente() {
        OrdemServicoGateway ordens = mock(OrdemServicoGateway.class);
        HistoricoOrdemServicoGateway historico = mock(HistoricoOrdemServicoGateway.class);
        when(ordens.encontrarOuLancar(OS_ID)).thenReturn(osSemOrcamento(OrdemServicoStatus.RECEBIDA));
        when(historico.listarPorOrdemServico(OS_ID)).thenReturn(List.of());

        Assertions.assertThat(new ListarHistoricoOrdemServicoUseCase(ordens, historico).executar(OS_ID)).isEmpty();
    }
}
