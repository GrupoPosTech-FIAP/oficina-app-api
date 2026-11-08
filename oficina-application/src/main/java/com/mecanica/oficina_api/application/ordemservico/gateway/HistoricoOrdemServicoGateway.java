package com.mecanica.oficina_api.application.ordemservico.gateway;

import com.mecanica.oficina_api.domain.ordemservico.HistoricoOrdemServico;

import java.util.List;

public interface HistoricoOrdemServicoGateway {
    void registrar(HistoricoOrdemServico historico);

    /** Em ordem cronológica. */
    List<HistoricoOrdemServico> listarPorOrdemServico(String ordemServicoId);
}
