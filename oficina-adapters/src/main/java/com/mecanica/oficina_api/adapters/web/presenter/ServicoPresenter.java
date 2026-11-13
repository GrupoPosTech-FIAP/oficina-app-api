package com.mecanica.oficina_api.adapters.web.presenter;

import com.mecanica.oficina_api.adapters.web.dto.response.ServicoResponse;
import com.mecanica.oficina_api.domain.servico.Servico;
import org.springframework.stereotype.Component;

import java.util.List;

/** Converte a entidade de domínio {@link Servico} em DTOs de resposta HTTP. */
@Component
public class ServicoPresenter {

    public ServicoResponse apresentar(Servico s) {
        return new ServicoResponse(
                s.getId(), s.getNome(), s.getDescricao(), s.getPreco(),
                (int) s.getTempoEstimadoHoras().toHours(), s.isAtivo());
    }

    public List<ServicoResponse> apresentar(List<Servico> servicos) {
        return servicos.stream().map(this::apresentar).toList();
    }
}
