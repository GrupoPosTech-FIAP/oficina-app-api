package com.mecanica.oficina_api.adapters.persistence;

import com.mecanica.oficina_api.adapters.persistence.repository.HistoricoOrdemServicoSpringDataRepository;
import com.mecanica.oficina_api.application.ordemservico.gateway.HistoricoOrdemServicoGateway;
import com.mecanica.oficina_api.domain.ordemservico.HistoricoOrdemServico;
import com.mecanica.oficina_api.domain.ordemservico.OrdemServicoStatus;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class HistoricoOrdemServicoJpaGateway implements HistoricoOrdemServicoGateway {

    private final HistoricoOrdemServicoSpringDataRepository repository;

    public HistoricoOrdemServicoJpaGateway(HistoricoOrdemServicoSpringDataRepository repository) {
        this.repository = repository;
    }

    @Override
    public void registrar(HistoricoOrdemServico historico) {
        HistoricoOrdemServicoJpaEntity entity = new HistoricoOrdemServicoJpaEntity();
        entity.setOrdemServicoId(historico.ordemServicoId());
        entity.setEvento(historico.evento());
        entity.setOrigem(historico.origem().name());
        entity.setStatusAnterior(historico.statusAnterior() == null ? null : historico.statusAnterior().name());
        entity.setStatusAtual(historico.statusAtual().name());
        entity.setDescartado(historico.descartado());
        entity.setDetalhe(historico.detalhe());
        entity.setOcorridoEm(historico.ocorridoEm());
        repository.save(entity);
    }

    @Override
    public List<HistoricoOrdemServico> listarPorOrdemServico(String ordemServicoId) {
        return repository.findByOrdemServicoIdOrderByIdAsc(ordemServicoId).stream()
                .map(e -> new HistoricoOrdemServico(
                        e.getOrdemServicoId(),
                        e.getEvento(),
                        HistoricoOrdemServico.Origem.valueOf(e.getOrigem()),
                        e.getStatusAnterior() == null ? null : OrdemServicoStatus.valueOf(e.getStatusAnterior()),
                        OrdemServicoStatus.valueOf(e.getStatusAtual()),
                        e.isDescartado(),
                        e.getDetalhe(),
                        e.getOcorridoEm()))
                .toList();
    }
}
