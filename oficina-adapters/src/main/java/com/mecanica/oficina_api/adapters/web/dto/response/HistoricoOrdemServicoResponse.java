package com.mecanica.oficina_api.adapters.web.dto.response;

import java.time.Instant;

public record HistoricoOrdemServicoResponse(
        String evento,
        String origem,
        String statusAnterior,
        String statusAtual,
        boolean descartado,
        String detalhe,
        Instant ocorridoEm) {
}
