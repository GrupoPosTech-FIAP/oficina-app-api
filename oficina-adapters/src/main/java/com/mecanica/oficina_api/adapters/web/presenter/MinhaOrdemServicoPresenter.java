package com.mecanica.oficina_api.adapters.web.presenter;

import com.mecanica.oficina_api.adapters.web.dto.response.MinhaOrdemServicoResponse;
import com.mecanica.oficina_api.application.ordemservico.output.MinhaOrdemServicoOutput;
import org.springframework.stereotype.Component;

import java.util.List;

/** Converte o output de aplicação {@link MinhaOrdemServicoOutput} em DTOs de resposta HTTP. */
@Component
public class MinhaOrdemServicoPresenter {

    public MinhaOrdemServicoResponse apresentar(MinhaOrdemServicoOutput o) {
        MinhaOrdemServicoResponse.VeiculoResumo veiculo = o.veiculo() == null ? null
                : new MinhaOrdemServicoResponse.VeiculoResumo(
                        o.veiculo().id(), o.veiculo().placa(), o.veiculo().marca(),
                        o.veiculo().modelo(), o.veiculo().ano(), o.veiculo().cor());
        return new MinhaOrdemServicoResponse(o.id(), o.status(), veiculo);
    }

    public List<MinhaOrdemServicoResponse> apresentar(List<MinhaOrdemServicoOutput> outputs) {
        return outputs.stream().map(this::apresentar).toList();
    }
}
