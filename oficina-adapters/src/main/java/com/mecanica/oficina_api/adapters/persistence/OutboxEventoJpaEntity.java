package com.mecanica.oficina_api.adapters.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Transactional Outbox: o evento é gravado na mesma transação da mudança de estado e
 * publicado no SNS depois pelo {@code OutboxRelay}.
 */
@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "outbox_eventos", indexes = @Index(name = "idx_outbox_pendentes", columnList = "publicadoEm, criadoEm"))
public class OutboxEventoJpaEntity {
  @Id
  private String eventId;

  @Column(nullable = false)
  private String eventType;

  @Column(nullable = false)
  private String sagaId;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String envelope;

  @Column(nullable = false)
  private Instant criadoEm;

  private Instant publicadoEm;

  private int tentativas;

  @Column(length = 1000)
  private String ultimoErro;
}
