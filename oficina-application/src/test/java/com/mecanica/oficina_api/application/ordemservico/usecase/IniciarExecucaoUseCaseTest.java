package com.mecanica.oficina_api.application.ordemservico.usecase;

import static com.mecanica.oficina_api.application.ordemservico.OrdemServicoFixture.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.mecanica.oficina_api.application.ordemservico.gateway.NotificadorClienteGateway;
import com.mecanica.oficina_api.application.ordemservico.gateway.OrdemServicoGateway;
import com.mecanica.oficina_api.application.servico.gateway.StatusServicoGateway;
import com.mecanica.oficina_api.domain.ordemservico.OrdemServico;
import com.mecanica.oficina_api.domain.ordemservico.OrdemServicoStatus;
import com.mecanica.oficina_api.domain.ordemservico.ServicoStatus;
import com.mecanica.oficina_api.domain.ordemservico.StatusServico;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
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

    @InjectMocks
    private IniciarExecucaoUseCase useCase;

    @Test
    void deveIniciarExecucaoECriarStatusParaServicosInformados() {
        OrdemServico os = osSemOrcamento(OrdemServicoStatus.EM_DIAGNOSTICO);
        when(ordemServicoGateway.encontrarOuLancar(OS_ID)).thenReturn(os);
        when(ordemServicoGateway.atualizar(any())).thenAnswer(inv -> inv.getArgument(0));

        OrdemServico resultado = useCase.executar(OS_ID, List.of("serv-1", "serv-2"));

        assertThat(resultado.getStatus()).isEqualTo(OrdemServicoStatus.EM_EXECUCAO);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<StatusServico>> captor = ArgumentCaptor.forClass(List.class);
        verify(statusServicoGateway).salvarLista(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
        assertThat(captor.getValue()).extracting(StatusServico::getServicoId).containsExactly("serv-1", "serv-2");
        assertThat(captor.getValue()).allSatisfy(s -> assertThat(s.getStatus()).isEqualTo(ServicoStatus.AGUARDANDO));
    }

    @Test
    void deveIniciarExecucaoSemServicosQuandoListaVaziaOuNula() {
        OrdemServico os = osSemOrcamento(OrdemServicoStatus.AGUARDANDO_APROVACAO);
        when(ordemServicoGateway.encontrarOuLancar(OS_ID)).thenReturn(os);
        when(ordemServicoGateway.atualizar(any())).thenAnswer(inv -> inv.getArgument(0));

        OrdemServico resultado = useCase.executar(OS_ID);

        assertThat(resultado.getStatus()).isEqualTo(OrdemServicoStatus.EM_EXECUCAO);
        verify(statusServicoGateway, never()).salvarLista(any());
    }

    @Test
    void deveLancarExcecao_quandoOsNaoEstaEmDiagnosticoNemAguardandoAprovacao() {
        OrdemServico os = osSemOrcamento(OrdemServicoStatus.RECEBIDA);
        when(ordemServicoGateway.encontrarOuLancar(OS_ID)).thenReturn(os);

        assertThatThrownBy(() -> useCase.executar(OS_ID, List.of("serv-1")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("OS deve estar EM_DIAGNOSTICO ou AGUARDANDO_APROVACAO");

        verify(statusServicoGateway, never()).salvarLista(any());
    }
}
