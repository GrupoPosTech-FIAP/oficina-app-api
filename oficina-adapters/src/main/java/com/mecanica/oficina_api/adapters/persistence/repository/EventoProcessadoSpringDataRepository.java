package com.mecanica.oficina_api.adapters.persistence.repository;

import com.mecanica.oficina_api.adapters.persistence.EventoProcessadoJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoProcessadoSpringDataRepository extends JpaRepository<EventoProcessadoJpaEntity, String> {
}
