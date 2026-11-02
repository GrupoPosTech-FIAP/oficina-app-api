package com.mecanica.oficina_api.application.ordemservico.output;

import java.time.LocalDateTime;

// Criado aqui porque não faz parte do domínio, é apenas uma classe achatada
public record MinhaOrdemServicoOutput(
        String id,
        String status,
        LocalDateTime dataCriacao,
        VeiculoResumo veiculo) {
    public record VeiculoResumo(String id, String placa, String marca, String modelo, int ano, String cor) {}

    public String getStatus() {
        return status;
    }

    public String getPlaca() {
        return veiculo.placa();
    }
}
