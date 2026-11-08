package com.mecanica.oficina_api.adapters.persistence.repository;

import com.mecanica.oficina_api.adapters.persistence.OrdemServicoJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OrdemServicoSpringDataRepository extends JpaRepository<OrdemServicoJpaEntity, String> {

    List<OrdemServicoJpaEntity> findByVeiculoId(String veiculoId);

    List<OrdemServicoJpaEntity> findByClienteId(String clienteId);

    List<OrdemServicoJpaEntity> findByStatus(String status);

    @Query("SELECT o FROM OrdemServicoJpaEntity o " +
           "WHERE o.status NOT IN ('FINALIZADA', 'ENTREGUE', 'CANCELADA') " +
           "ORDER BY CASE o.status " +
           "  WHEN 'EM_EXECUCAO' THEN 1 " +
           "  WHEN 'AGUARDANDO_EXECUCAO' THEN 2 " +
           "  WHEN 'AGUARDANDO_PAGAMENTO' THEN 3 " +
           "  WHEN 'AGUARDANDO_APROVACAO' THEN 4 " +
           "  WHEN 'EM_DIAGNOSTICO' THEN 5 " +
           "  WHEN 'RECEBIDA' THEN 6 " +
           "  ELSE 7 END ASC, o.dataCriacao ASC")
    List<OrdemServicoJpaEntity> findAtivasOrdenadas();
}
