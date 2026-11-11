package com.mecanica.oficina_api.application.ordemservico.input;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resultado do diagnóstico: serviços do catálogo e insumos necessários. O preço dos serviços vem
 * do catálogo da OS; o dos insumos é informado aqui (o catálogo de insumos fica no inventory).
 */
public record ConcluirDiagnosticoInput(
        List<Servico> servicos,
        List<Insumo> insumos,
        String observacoes) {

    public record Servico(String servicoId, int quantidade) {
    }

    public record Insumo(String insumoId, int quantidade, String descricao, BigDecimal precoUnitario) {
    }
}
