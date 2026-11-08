package com.mecanica.oficina_api.application.ordemservico.usecase;

import static com.mecanica.oficina_api.application.ordemservico.OrdemServicoFixture.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.mecanica.oficina_api.application.ordemservico.RegistroEventosOrdemServico;
import com.mecanica.oficina_api.application.ordemservico.gateway.NotificadorClienteGateway;
import com.mecanica.oficina_api.application.ordemservico.gateway.OrdemServicoGateway;
import com.mecanica.oficina_api.domain.ordemservico.OrdemServico;
import com.mecanica.oficina_api.domain.ordemservico.OrdemServicoStatus;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FalharExecucaoUseCaseTest {

    private OrdemServicoGateway ordemServicoGateway;
    private NotificadorClienteGateway notificador;
    private RegistroEventosOrdemServico eventos;
    private FalharExecucaoUseCase useCase;

    @BeforeEach
    void setUp() {
        ordemServicoGateway = mock(OrdemServicoGateway.class);
        notificador = mock(NotificadorClienteGateway.class);
        eventos = mock(RegistroEventosOrdemServico.class);
        useCase = new FalharExecucaoUseCase(ordemServicoGateway, notificador, eventos);
        when(ordemServicoGateway.atualizar(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void cancelaPublicaExecucaoFalhouENotifica() {
        when(ordemServicoGateway.encontrarOuLancar(OS_ID)).thenReturn(osSemOrcamento(OrdemServicoStatus.EM_EXECUCAO));

        OrdemServico os = useCase.executar(OS_ID, "peça indisponível");

        assertThat(os.getStatus()).isEqualTo(OrdemServicoStatus.CANCELADA);
        verify(eventos).publicar("ExecucaoFalhou", os, OrdemServicoStatus.EM_EXECUCAO,
                Map.of("ordemServicoId", OS_ID, "motivo", "peça indisponível"), "peça indisponível");
        verify(notificador).notificar(any());
    }

    @Test
    void exigeMotivoEOsEmExecucao() {
        assertThatThrownBy(() -> useCase.executar(OS_ID, " ")).isInstanceOf(IllegalArgumentException.class);

        when(ordemServicoGateway.encontrarOuLancar(OS_ID)).thenReturn(osSemOrcamento(OrdemServicoStatus.AGUARDANDO_EXECUCAO));
        assertThatThrownBy(() -> useCase.executar(OS_ID, "motivo")).isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(eventos, notificador);
    }
}
