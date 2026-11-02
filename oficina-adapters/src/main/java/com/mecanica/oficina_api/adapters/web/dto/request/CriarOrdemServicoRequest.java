package com.mecanica.oficina_api.adapters.web.dto.request;

import jakarta.validation.constraints.NotBlank;

public class CriarOrdemServicoRequest {

    @NotBlank(message = "O ID do veículo é obrigatório")
    private String veiculoId;

    @NotBlank(message = "O ID do cliente é obrigatório")
    private String clienteId;

    public CriarOrdemServicoRequest() {}

    public String getVeiculoId() { return veiculoId; }
    public void setVeiculoId(String veiculoId) { this.veiculoId = veiculoId; }
    public String getClienteId() { return clienteId; }
    public void setClienteId(String clienteId) { this.clienteId = clienteId; }
}
