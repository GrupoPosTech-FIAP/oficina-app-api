package com.mecanica.oficina_api.adapters.web.presenter;

import com.mecanica.oficina_api.adapters.web.dto.response.OrdemServicoResponse;
import com.mecanica.oficina_api.adapters.web.dto.response.HistoricoOrdemServicoResponse;
import com.mecanica.oficina_api.domain.ordemservico.HistoricoOrdemServico;
import com.mecanica.oficina_api.domain.ordemservico.OrdemServico;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrdemServicoPresenter {

    public OrdemServicoResponse apresentar(OrdemServico os) {
        return new OrdemServicoResponse(os.getId(), os.getVeiculoId(), os.getClienteId(),
                os.getStatus().name(), os.getValorFinal(), os.getDataFinal());
    }

    public List<OrdemServicoResponse> apresentar(List<OrdemServico> oss) {
        return oss.stream().map(this::apresentar).toList();
    }

    public List<HistoricoOrdemServicoResponse> apresentarHistorico(List<HistoricoOrdemServico> historico) {
        return historico.stream()
                .map(h -> new HistoricoOrdemServicoResponse(h.evento(), h.origem().name(),
                        h.statusAnterior() == null ? null : h.statusAnterior().name(), h.statusAtual().name(),
                        h.descartado(), h.detalhe(), h.ocorridoEm()))
                .toList();
    }
}
