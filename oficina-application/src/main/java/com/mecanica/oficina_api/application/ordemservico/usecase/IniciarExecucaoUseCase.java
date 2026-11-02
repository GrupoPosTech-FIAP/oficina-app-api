package com.mecanica.oficina_api.application.ordemservico.usecase;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.mecanica.oficina_api.application.ordemservico.gateway.NotificadorClienteGateway;
import com.mecanica.oficina_api.application.ordemservico.gateway.OrdemServicoGateway;
import com.mecanica.oficina_api.application.ordemservico.output.NotificacaoCliente;
import com.mecanica.oficina_api.application.servico.gateway.StatusServicoGateway;
import com.mecanica.oficina_api.domain.ordemservico.OrdemServico;
import com.mecanica.oficina_api.domain.ordemservico.StatusServico;

public class IniciarExecucaoUseCase {
    private final OrdemServicoGateway ordemServicoGateway;
    private final NotificadorClienteGateway notificadorClienteGateway;
    private final StatusServicoGateway statusServicoGateway;

    public IniciarExecucaoUseCase(OrdemServicoGateway ordemServicoGateway,
                                  NotificadorClienteGateway notificadorClienteGateway,
                                  StatusServicoGateway statusServicoGateway) {
        this.ordemServicoGateway = ordemServicoGateway;
        this.notificadorClienteGateway = notificadorClienteGateway;
        this.statusServicoGateway = statusServicoGateway;
    }

    public OrdemServico executar(String id) {
        return executar(id, List.of());
    }

    public OrdemServico executar(String id, List<String> servicoIds) {
        OrdemServico os = ordemServicoGateway.encontrarOuLancar(id);
        os.iniciarExecucao();
        criarStatusIndividuaisPorServico(servicoIds, os.getId());
        OrdemServico salva = ordemServicoGateway.atualizar(os);

        notificadorClienteGateway.notificar(NotificacaoCliente.emExecucao(salva));

        return salva;
    }

    private List<StatusServico> criarStatusIndividuaisPorServico(List<String> servicoIds, String ordemServicoId) {
        if (servicoIds == null || servicoIds.isEmpty()) {
            return List.of();
        }
        List<StatusServico> statusServicoList = new ArrayList<>();
        servicoIds.forEach(servicoId -> {
            if (!Objects.isNull(servicoId) && !servicoId.isBlank()) {
                StatusServico statusServico = StatusServico.criar(ordemServicoId, servicoId);
                statusServicoList.add(statusServico);
            }
        });

        if (statusServicoList.isEmpty()) {
            return List.of();
        }
        return statusServicoGateway.salvarLista(statusServicoList);
    }
}
