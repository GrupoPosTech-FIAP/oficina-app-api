package com.mecanica.oficina_api.application.ordemservico.usecase;

import static com.mecanica.oficina_api.application.ordemservico.OrdemServicoFixture.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.mecanica.oficina_api.application.ordemservico.RegistroEventosOrdemServico;
import com.mecanica.oficina_api.application.ordemservico.gateway.NotificadorClienteGateway;
import com.mecanica.oficina_api.application.ordemservico.gateway.OrdemServicoGateway;
import com.mecanica.oficina_api.application.servico.gateway.StatusServicoGateway;
import com.mecanica.oficina_api.domain.ordemservico.OrdemServico;
import com.mecanica.oficina_api.domain.ordemservico.OrdemServicoStatus;
import com.mecanica.oficina_api.domain.ordemservico.ServicoStatus;
import com.mecanica.oficina_api.domain.ordemservico.StatusServico;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IniciarExecucaoUseCaseTest {

    @Mock
    private OrdemServicoGateway ordemServicoGateway;
    @Mock
    private NotificadorClienteGateway notificadorClienteGateway;

    @Mock
    private StatusServicoGateway statusServicoGateway;

    @Mock
    private RegistroEventosOrdemServico eventos;

    private IniciarExecucaoUseCase useCase;

    private static final Instant AGORA = Instant.parse("2026-11-08T10:00:00Z");

    @BeforeEach
    void setUp() {
        useCase = new IniciarExecucaoUseCase(ordemServicoGateway, notificadorClienteGateway, statusServicoGateway,
                eventos, Clock.fixed(AGORA, ZoneOffset.UTC));
    }

    @Test
    void deveIniciarExecucaoECriarStatusParaServicosInformados() {
        OrdemServico os = osSemOrcamento(OrdemServicoStatus.AGUARDANDO_EXECUCAO);
        when(ordemServicoGateway.encontrarOuLancar(OS_ID)).thenReturn(os);
        when(ordemServicoGateway.atualizar(any())).thenAnswer(inv -> inv.getArgument(0));

        OrdemServico resultado = useCase.executar(OS_ID, List.of("serv-1", "serv-2"));

        assertThat(resultado.getStatus()).isEqualTo(OrdemServicoStatus.EM_EXECUCAO);
        verify(eventos).publicar("ExecucaoIniciada", os, OrdemServicoStatus.AGUARDANDO_EXECUCAO,
                Map.of("ordemServicoId", OS_ID, "iniciadaEm", "2026-11-08T10:00:00Z"), null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<StatusServico>> captor = ArgumentCaptor.forClass(List.class);
        verify(statusServicoGateway).salvarLista(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
        assertThat(captor.getValue()).extracting(StatusServico::getServicoId).containsExactly("serv-1", "serv-2");
        assertThat(captor.getValue()).allSatisfy(s -> assertThat(s.getStatus()).isEqualTo(ServicoStatus.AGUARDANDO));
    }

    @Test
    void deveIniciarExecucaoSemServicosQuandoListaVaziaOuNula() {
        OrdemServico os = osSemOrcamento(OrdemServicoStatus.AGUARDANDO_EXECUCAO);
        when(ordemServicoGateway.encontrarOuLancar(OS_ID)).thenReturn(os);
        when(ordemServicoGateway.atualizar(any())).thenAnswer(inv -> inv.getArgument(0));

        OrdemServico resultado = useCase.executar(OS_ID);

        assertThat(resultado.getStatus()).isEqualTo(OrdemServicoStatus.EM_EXECUCAO);
        verify(statusServicoGateway, never()).salvarLista(any());
    }

    @Test
    void deveLancarExcecao_quandoPagamentoAindaNaoFoiConfirmado() {
        OrdemServico os = osSemOrcamento(OrdemServicoStatus.AGUARDANDO_PAGAMENTO);
        when(ordemServicoGateway.encontrarOuLancar(OS_ID)).thenReturn(os);

        assertThatThrownBy(() -> useCase.executar(OS_ID, List.of("serv-1")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("OS deve estar AGUARDANDO_EXECUCAO");

        verify(statusServicoGateway, never()).salvarLista(any());
        verifyNoInteractions(eventos);
    }
}
