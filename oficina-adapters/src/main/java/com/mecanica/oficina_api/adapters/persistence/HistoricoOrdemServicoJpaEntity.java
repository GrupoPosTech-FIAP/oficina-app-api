package com.mecanica.oficina_api.adapters.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Histórico da OS: cada evento da Saga publicado ou recebido (saga log). */
@Data
@Entity
@NoArgsConstructor
@Table(name = "historico_ordem_servico",
    indexes = @Index(name = "idx_historico_os", columnList = "ordemServicoId, id"))
public class HistoricoOrdemServicoJpaEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String ordemServicoId;

  @Column(nullable = false)
  private String evento;

  @Column(nullable = false)
  private String origem;

  private String statusAnterior;

  @Column(nullable = false)
  private String statusAtual;

  @Column(nullable = false)
  private boolean descartado;

  @Column(columnDefinition = "TEXT")
  private String detalhe;

  @Column(nullable = false)
  private Instant ocorridoEm;
}
