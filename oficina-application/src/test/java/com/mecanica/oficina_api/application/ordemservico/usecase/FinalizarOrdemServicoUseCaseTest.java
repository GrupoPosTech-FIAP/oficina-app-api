package com.mecanica.oficina_api.application.ordemservico.usecase;

import static com.mecanica.oficina_api.application.ordemservico.OrdemServicoFixture.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.mecanica.oficina_api.application.ordemservico.RegistroEventosOrdemServico;
import com.mecanica.oficina_api.application.ordemservico.gateway.NotificadorClienteGateway;
import com.mecanica.oficina_api.application.ordemservico.gateway.OrdemServicoGateway;
import com.mecanica.oficina_api.application.ordemservico.output.NotificacaoCliente;
import com.mecanica.oficina_api.application.ordemservico.output.TipoNotificacaoCliente;
import com.mecanica.oficina_api.application.servico.gateway.StatusServicoGateway;
import com.mecanica.oficina_api.domain.ordemservico.OrdemServico;
import com.mecanica.oficina_api.domain.ordemservico.OrdemServicoStatus;
import com.mecanica.oficina_api.domain.ordemservico.ServicoStatus;
import com.mecanica.oficina_api.domain.ordemservico.StatusServico;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FinalizarOrdemServicoUseCaseTest {

    @Mock
    private OrdemServicoGateway ordemServicoGateway;

    @Mock
    private StatusServicoGateway statusServicoGateway;

    @Mock
    private NotificadorClienteGateway notificadorClienteGateway;

    @Mock
    private RegistroEventosOrdemServico eventos;

    @InjectMocks
    private FinalizarOrdemServicoUseCase useCase;

    private StatusServico servicoFinalizado(String id) {
        return StatusServico.recriar(id, ServicoStatus.FINALIZADO, OS_ID, "serv-" + id,
            LocalDateTime.now().minusHours(2), LocalDateTime.now().minusHours(1));
    }

    @Test
    void deveFinalizarOsENotificarCliente_quandoTodosServicosFinalizados() {
        OrdemServico os = osSemOrcamento(OrdemServicoStatus.EM_EXECUCAO);
        when(ordemServicoGateway.encontrarOuLancar(OS_ID)).thenReturn(os);
        when(statusServicoGateway.listarServicosPorOS(OS_ID))
            .thenReturn(List.of(servicoFinalizado("ss-1"), servicoFinalizado("ss-2")));
        when(ordemServicoGateway.atualizar(any())).thenAnswer(inv -> inv.getArgument(0));

        OrdemServico resultado = useCase.executar(OS_ID);

        assertThat(resultado.getStatus()).isEqualTo(OrdemServicoStatus.FINALIZADA);
        assertThat(resultado.getValorFinal()).isNull();
        assertThat(resultado.getDataFinal()).isNotNull();

        ArgumentCaptor<NotificacaoCliente> captor = ArgumentCaptor.forClass(NotificacaoCliente.class);
        verify(notificadorClienteGateway).notificar(captor.capture());
        assertThat(captor.getValue().tipo()).isEqualTo(TipoNotificacaoCliente.FINALIZACAO_OS);
        assertThat(captor.getValue().ordemServicoId()).isEqualTo(OS_ID);

        verify(eventos).publicar("ExecucaoConcluida", resultado, OrdemServicoStatus.EM_EXECUCAO,
                java.util.Map.of("ordemServicoId", OS_ID, "concluidaEm", resultado.getDataFinal().toString()), null);
    }

    @Test
    void deveLancarExcecao_quandoHaServicosPendentes() {
        OrdemServico os = osSemOrcamento(OrdemServicoStatus.EM_EXECUCAO);
        StatusServico pendente = StatusServico.recriar("ss-2", ServicoStatus.INICIADO, OS_ID, "serv-2",
            LocalDateTime.now(), null);
        when(ordemServicoGateway.encontrarOuLancar(OS_ID)).thenReturn(os);
        when(statusServicoGateway.listarServicosPorOS(OS_ID))
            .thenReturn(List.of(servicoFinalizado("ss-1"), pendente));

        assertThatThrownBy(() -> useCase.executar(OS_ID))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Ainda há serviços para serem realizados");

        verify(ordemServicoGateway, never()).atualizar(any());
        verify(notificadorClienteGateway, never()).notificar(any());
    }

    @Test
    void deveLancarExcecao_quandoOsNaoEstaEmExecucao() {
        OrdemServico os = osSemOrcamento(OrdemServicoStatus.RECEBIDA);
        when(ordemServicoGateway.encontrarOuLancar(OS_ID)).thenReturn(os);

        assertThatThrownBy(() -> useCase.executar(OS_ID))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("OS deve estar EM_EXECUCAO");

        verify(notificadorClienteGateway, never()).notificar(any());
    }
}
