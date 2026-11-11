package com.mecanica.oficina_api.application.ordemservico.usecase;

import java.util.ArrayList;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.mecanica.oficina_api.application.evento.EventosSaga;
import com.mecanica.oficina_api.application.ordemservico.RegistroEventosOrdemServico;
import com.mecanica.oficina_api.application.ordemservico.gateway.NotificadorClienteGateway;
import com.mecanica.oficina_api.application.ordemservico.gateway.OrdemServicoGateway;
import com.mecanica.oficina_api.application.ordemservico.output.NotificacaoCliente;
import com.mecanica.oficina_api.application.servico.gateway.StatusServicoGateway;
import com.mecanica.oficina_api.domain.ordemservico.OrdemServico;
import com.mecanica.oficina_api.domain.ordemservico.OrdemServicoStatus;
import com.mecanica.oficina_api.domain.ordemservico.StatusServico;

/** AGUARDANDO_EXECUCAO → EM_EXECUCAO e publica {@code ExecucaoIniciada} (histórico da Saga). */
public class IniciarExecucaoUseCase {
    private final OrdemServicoGateway ordemServicoGateway;
    private final NotificadorClienteGateway notificadorClienteGateway;
    private final StatusServicoGateway statusServicoGateway;
    private final RegistroEventosOrdemServico eventos;
    private final Clock clock;

    public IniciarExecucaoUseCase(OrdemServicoGateway ordemServicoGateway,
                                  NotificadorClienteGateway notificadorClienteGateway,
                                  StatusServicoGateway statusServicoGateway,
                                  RegistroEventosOrdemServico eventos,
                                  Clock clock) {
        this.ordemServicoGateway = ordemServicoGateway;
        this.notificadorClienteGateway = notificadorClienteGateway;
        this.statusServicoGateway = statusServicoGateway;
        this.eventos = eventos;
        this.clock = clock;
    }

    public OrdemServico executar(String id) {
        return executar(id, List.of());
    }

    public OrdemServico executar(String id, List<String> servicoIds) {
        OrdemServico os = ordemServicoGateway.encontrarOuLancar(id);
        OrdemServicoStatus anterior = os.getStatus();
        os.iniciarExecucao();
        criarStatusIndividuaisPorServico(servicoIds, os.getId());
        OrdemServico salva = ordemServicoGateway.atualizar(os);

        eventos.publicar(EventosSaga.EXECUCAO_INICIADA, salva, anterior,
                Map.of("ordemServicoId", salva.getId(), "iniciadaEm", clock.instant().toString()), null);

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
