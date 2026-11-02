package com.mecanica.oficina_api.adapters.web.presenter;

import com.mecanica.oficina_api.adapters.web.dto.response.OrdemServicoResponse;
import com.mecanica.oficina_api.adapters.web.dto.response.ServicoStatusResponse;
import com.mecanica.oficina_api.domain.ordemservico.OrdemServico;
import com.mecanica.oficina_api.domain.ordemservico.StatusServico;
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

    public ServicoStatusResponse apresentarServico(StatusServico s) {
        return new ServicoStatusResponse(s.getId(), s.getStatus().name(), s.getOrdemServicoId(),
                s.getServicoId(), s.getDataInicio(), s.getDataFim());
    }

    public List<ServicoStatusResponse> apresentarServicos(List<StatusServico> servicos) {
        return servicos.stream().map(this::apresentarServico).toList();
    }
}
