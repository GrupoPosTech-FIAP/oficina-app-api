package com.mecanica.oficina_api.adapters.persistence.repository;

import com.mecanica.oficina_api.adapters.persistence.HistoricoOrdemServicoJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoricoOrdemServicoSpringDataRepository extends JpaRepository<HistoricoOrdemServicoJpaEntity, Long> {
    List<HistoricoOrdemServicoJpaEntity> findByOrdemServicoIdOrderByIdAsc(String ordemServicoId);
}
