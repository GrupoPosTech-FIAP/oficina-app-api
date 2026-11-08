package com.mecanica.oficina_api.adapters.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Registro de idempotência: um evento cujo id está aqui já foi processado e é ignorado. */
@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "eventos_processados")
public class EventoProcessadoJpaEntity {
  @Id
  private String eventId;

  @Column(nullable = false)
  private String eventType;

  @Column(nullable = false)
  private String sagaId;

  @Column(nullable = false)
  private Instant processadoEm;
}
