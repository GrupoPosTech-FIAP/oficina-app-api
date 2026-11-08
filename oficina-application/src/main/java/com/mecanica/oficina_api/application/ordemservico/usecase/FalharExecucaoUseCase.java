package com.mecanica.oficina_api.application.ordemservico.usecase;

import com.mecanica.oficina_api.application.evento.EventosSaga;
import com.mecanica.oficina_api.application.ordemservico.RegistroEventosOrdemServico;
import com.mecanica.oficina_api.application.ordemservico.gateway.NotificadorClienteGateway;
import com.mecanica.oficina_api.application.ordemservico.gateway.OrdemServicoGateway;
import com.mecanica.oficina_api.application.ordemservico.output.NotificacaoCliente;
import com.mecanica.oficina_api.domain.ordemservico.OrdemServico;
import com.mecanica.oficina_api.domain.ordemservico.OrdemServicoStatus;

import java.util.Map;

/**
 * Reparo inviável durante a execução: cancela a OS e publica {@code ExecucaoFalhou}, que dispara as
 * compensações (estorno de insumos no inventory e reembolso no billing).
 */
public class FalharExecucaoUseCase {

    private final OrdemServicoGateway ordemServicoGateway;
    private final NotificadorClienteGateway notificadorClienteGateway;
    private final RegistroEventosOrdemServico eventos;

    public FalharExecucaoUseCase(OrdemServicoGateway ordemServicoGateway,
                                 NotificadorClienteGateway notificadorClienteGateway,
                                 RegistroEventosOrdemServico eventos) {
        this.ordemServicoGateway = ordemServicoGateway;
        this.notificadorClienteGateway = notificadorClienteGateway;
        this.eventos = eventos;
    }

    public OrdemServico executar(String id, String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("O motivo da falha é obrigatório");
        }

        OrdemServico os = ordemServicoGateway.encontrarOuLancar(id);
        OrdemServicoStatus anterior = os.getStatus();
        os.falharExecucao();
        OrdemServico salva = ordemServicoGateway.atualizar(os);

        eventos.publicar(EventosSaga.EXECUCAO_FALHOU, salva, anterior,
                Map.of("ordemServicoId", salva.getId(), "motivo", motivo), motivo);
        notificadorClienteGateway.notificar(NotificacaoCliente.cancelada(salva));
        return salva;
    }
}
