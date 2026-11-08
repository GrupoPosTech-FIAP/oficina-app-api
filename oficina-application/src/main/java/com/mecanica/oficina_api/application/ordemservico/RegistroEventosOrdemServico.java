package com.mecanica.oficina_api.application.ordemservico;

import com.mecanica.oficina_api.application.evento.gateway.PublicadorEventosGateway;
import com.mecanica.oficina_api.application.ordemservico.gateway.HistoricoOrdemServicoGateway;
import com.mecanica.oficina_api.domain.ordemservico.HistoricoOrdemServico;
import com.mecanica.oficina_api.domain.ordemservico.OrdemServico;
import com.mecanica.oficina_api.domain.ordemservico.OrdemServicoStatus;

import java.time.Clock;
import java.util.Map;

/** Publica um evento da Saga originado na OS e registra o passo no histórico, na mesma transação. */
public class RegistroEventosOrdemServico {

    private final PublicadorEventosGateway publicador;
    private final HistoricoOrdemServicoGateway historicoGateway;
    private final Clock clock;

    public RegistroEventosOrdemServico(PublicadorEventosGateway publicador,
                                       HistoricoOrdemServicoGateway historicoGateway,
                                       Clock clock) {
        this.publicador = publicador;
        this.historicoGateway = historicoGateway;
        this.clock = clock;
    }

    public void publicar(String evento, OrdemServico os, OrdemServicoStatus statusAnterior,
                         Map<String, Object> payload, String detalhe) {
        publicador.publicar(evento, os.getId(), payload);
        historicoGateway.registrar(new HistoricoOrdemServico(os.getId(), evento, HistoricoOrdemServico.Origem.PUBLICADO,
                statusAnterior, os.getStatus(), false, detalhe, clock.instant()));
    }
}
