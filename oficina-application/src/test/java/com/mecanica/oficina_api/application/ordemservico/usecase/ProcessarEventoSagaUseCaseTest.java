package com.mecanica.oficina_api.application.ordemservico.usecase;

import static com.mecanica.oficina_api.application.ordemservico.OrdemServicoFixture.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.mecanica.oficina_api.application.ordemservico.gateway.HistoricoOrdemServicoGateway;
import com.mecanica.oficina_api.application.ordemservico.gateway.NotificadorClienteGateway;
import com.mecanica.oficina_api.application.ordemservico.gateway.OrdemServicoGateway;
import com.mecanica.oficina_api.application.ordemservico.output.NotificacaoCliente;
import com.mecanica.oficina_api.application.ordemservico.output.TipoNotificacaoCliente;
import com.mecanica.oficina_api.application.ordemservico.usecase.ProcessarEventoSagaUseCase.Resultado;
import com.mecanica.oficina_api.domain.ordemservico.HistoricoOrdemServico;
import com.mecanica.oficina_api.domain.ordemservico.OrdemServicoStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

class ProcessarEventoSagaUseCaseTest {

    private static final Instant AGORA = Instant.parse("2026-11-08T10:00:00Z");

    private OrdemServicoGateway ordemServicoGateway;
    private HistoricoOrdemServicoGateway historicoGateway;
    private NotificadorClienteGateway notificador;
    private ProcessarEventoSagaUseCase useCase;

    @BeforeEach
    void setUp() {
        ordemServicoGateway = mock(OrdemServicoGateway.class);
        historicoGateway = mock(HistoricoOrdemServicoGateway.class);
        notificador = mock(NotificadorClienteGateway.class);
        useCase = new ProcessarEventoSagaUseCase(ordemServicoGateway, historicoGateway, notificador,
                Clock.fixed(AGORA, ZoneOffset.UTC));
        when(ordemServicoGateway.atualizar(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private void osEm(OrdemServicoStatus status) {
        when(ordemServicoGateway.buscar(OS_ID)).thenReturn(Optional.of(osSemOrcamento(status)));
    }

    private HistoricoOrdemServico historicoRegistrado() {
        ArgumentCaptor<HistoricoOrdemServico> captor = ArgumentCaptor.forClass(HistoricoOrdemServico.class);
        verify(historicoGateway).registrar(captor.capture());
        return captor.getValue();
    }

    @ParameterizedTest
    @CsvSource({
            "OrcamentoGerado, EM_DIAGNOSTICO, AGUARDANDO_APROVACAO",
            "CobrancaCriada, AGUARDANDO_APROVACAO, AGUARDANDO_PAGAMENTO",
            "PagamentoConfirmado, AGUARDANDO_PAGAMENTO, AGUARDANDO_EXECUCAO",
            "OrcamentoNegado, AGUARDANDO_APROVACAO, CANCELADA",
            "OrcamentoExpirado, AGUARDANDO_APROVACAO, CANCELADA",
            "EstoqueInsuficiente, AGUARDANDO_APROVACAO, CANCELADA",
            "PagamentoRecusado, AGUARDANDO_PAGAMENTO, CANCELADA",
            "PagamentoExpirado, AGUARDANDO_PAGAMENTO, CANCELADA",
            "ExecucaoIniciada, AGUARDANDO_EXECUCAO, EM_EXECUCAO",
            "ExecucaoConcluida, EM_EXECUCAO, FINALIZADA",
            "ExecucaoFalhou, EM_EXECUCAO, CANCELADA"
    })
    void aplicaATransicaoDoEventoERegistraNoHistorico(String evento, OrdemServicoStatus de, OrdemServicoStatus para) {
        osEm(de);

        assertThat(useCase.executar(evento, OS_ID, "{\"x\":1}")).isEqualTo(Resultado.TRANSICAO_APLICADA);

        verify(ordemServicoGateway).atualizar(argThat(os -> os.getStatus() == para));
        assertThat(historicoRegistrado()).isEqualTo(new HistoricoOrdemServico(OS_ID, evento,
                HistoricoOrdemServico.Origem.RECEBIDO, de, para, false, "{\"x\":1}", AGORA));
    }

    @Test
    void cancelamentoNotificaOCliente() {
        osEm(OrdemServicoStatus.AGUARDANDO_PAGAMENTO);

        useCase.executar("PagamentoRecusado", OS_ID, null);

        ArgumentCaptor<NotificacaoCliente> captor = ArgumentCaptor.forClass(NotificacaoCliente.class);
        verify(notificador).notificar(captor.capture());
        assertThat(captor.getValue().tipo()).isEqualTo(TipoNotificacaoCliente.CANCELADA);
    }

    @Test
    void eventoForaDeOrdemSoEntraNoHistorico() {
        osEm(OrdemServicoStatus.EM_DIAGNOSTICO);

        assertThat(useCase.executar("PagamentoConfirmado", OS_ID, null)).isEqualTo(Resultado.TRANSICAO_IGNORADA);

        verify(ordemServicoGateway, never()).atualizar(any());
        verifyNoInteractions(notificador);
        HistoricoOrdemServico h = historicoRegistrado();
        assertThat(h.descartado()).isTrue();
        assertThat(h.statusAtual()).isEqualTo(OrdemServicoStatus.EM_DIAGNOSTICO);
    }

    @Test
    void passosQueNaoMudamOStatusSoEntramNoHistorico() {
        osEm(OrdemServicoStatus.AGUARDANDO_APROVACAO);

        assertThat(useCase.executar("EstoqueBaixado", OS_ID, null)).isEqualTo(Resultado.SOMENTE_HISTORICO);

        verify(ordemServicoGateway, never()).atualizar(any());
        HistoricoOrdemServico h = historicoRegistrado();
        assertThat(h.evento()).isEqualTo("EstoqueBaixado");
        assertThat(h.descartado()).isFalse();
    }

    @Test
    void ordemServicoInexistenteEIgnorada() {
        when(ordemServicoGateway.buscar(OS_ID)).thenReturn(Optional.empty());

        assertThat(useCase.executar("OrcamentoGerado", OS_ID, null)).isEqualTo(Resultado.ORDEM_SERVICO_INEXISTENTE);

        verifyNoInteractions(historicoGateway);
    }

    @Test
    void eventoForaDoCatalogoDaOsELancaErro() {
        assertThatThrownBy(() -> useCase.executar("DiagnosticoConcluido", OS_ID, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(ProcessarEventoSagaUseCase.EVENTOS_CONSUMIDOS)
                .contains("OrcamentoGerado", "PagamentoEstornado", "EstoqueEstornado")
                .doesNotContain("DiagnosticoConcluido");
    }
}
