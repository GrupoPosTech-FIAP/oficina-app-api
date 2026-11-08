package com.mecanica.oficina_api.adapters.web.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;

/** Serviços (preço do catálogo) e insumos (preço informado) levantados no diagnóstico. */
public record ConcluirDiagnosticoRequest(
        @Valid List<Servico> servicos,
        @Valid List<Insumo> insumos,
        String observacoes) {

    public record Servico(
            @NotBlank(message = "O ID do serviço é obrigatório") String servicoId,
            @Positive(message = "A quantidade deve ser maior que zero") int quantidade) {
    }

    public record Insumo(
            @NotBlank(message = "O ID do insumo é obrigatório") String insumoId,
            @Positive(message = "A quantidade deve ser maior que zero") int quantidade,
            @NotBlank(message = "A descrição do insumo é obrigatória") String descricao,
            @NotNull(message = "O preço unitário do insumo é obrigatório")
            @Positive(message = "O preço unitário deve ser maior que zero") BigDecimal precoUnitario) {
    }
}
