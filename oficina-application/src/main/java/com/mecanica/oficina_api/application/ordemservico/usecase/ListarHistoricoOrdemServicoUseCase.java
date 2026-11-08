package com.mecanica.oficina_api.application.ordemservico.usecase;

import com.mecanica.oficina_api.application.ordemservico.gateway.HistoricoOrdemServicoGateway;
import com.mecanica.oficina_api.application.ordemservico.gateway.OrdemServicoGateway;
import com.mecanica.oficina_api.domain.ordemservico.HistoricoOrdemServico;

import java.util.List;

public class ListarHistoricoOrdemServicoUseCase {

    private final OrdemServicoGateway ordemServicoGateway;
    private final HistoricoOrdemServicoGateway historicoGateway;

    public ListarHistoricoOrdemServicoUseCase(OrdemServicoGateway ordemServicoGateway,
                                              HistoricoOrdemServicoGateway historicoGateway) {
        this.ordemServicoGateway = ordemServicoGateway;
        this.historicoGateway = historicoGateway;
    }

    public List<HistoricoOrdemServico> executar(String ordemServicoId) {
        ordemServicoGateway.encontrarOuLancar(ordemServicoId);
        return historicoGateway.listarPorOrdemServico(ordemServicoId);
    }
}
