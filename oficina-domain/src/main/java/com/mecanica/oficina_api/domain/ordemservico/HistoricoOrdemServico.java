package com.mecanica.oficina_api.domain.ordemservico;

import java.time.Instant;
import java.util.Objects;

/**
 * Entrada do histórico da OS: um evento da Saga publicado ou recebido por este serviço e o efeito
 * dele no status. Serve de trilha de auditoria (saga log) e de consulta de andamento.
 *
 * @param descartado {@code true} quando o evento não corresponde a uma transição válida a partir do
 *                   status atual (fora de ordem ou repetido) e por isso não mudou a OS
 * @param detalhe    dados do evento (payload em JSON) ou motivo informado
 */
public record HistoricoOrdemServico(
        String ordemServicoId,
        String evento,
        Origem origem,
        OrdemServicoStatus statusAnterior,
        OrdemServicoStatus statusAtual,
        boolean descartado,
        String detalhe,
        Instant ocorridoEm) {

    public enum Origem { PUBLICADO, RECEBIDO }

    public HistoricoOrdemServico {
        Objects.requireNonNull(ordemServicoId, "O ID da ordem de serviço é obrigatório");
        Objects.requireNonNull(evento, "O evento é obrigatório");
        Objects.requireNonNull(origem, "A origem é obrigatória");
        Objects.requireNonNull(statusAtual, "O status atual é obrigatório");
        Objects.requireNonNull(ocorridoEm, "A data do evento é obrigatória");
    }
}
