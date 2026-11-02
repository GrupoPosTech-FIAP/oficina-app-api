package com.mecanica.oficina_api.adapters.web.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OrdemServicoResponse {

    private final String id;
    private final String veiculoId;
    private final String clienteId;
    private final String status;
    private final BigDecimal valorFinal;
    private final LocalDateTime dataFinal;

    public OrdemServicoResponse(String id, String veiculoId, String clienteId, String status,
                                BigDecimal valorFinal, LocalDateTime dataFinal) {
        this.id = id;
        this.veiculoId = veiculoId;
        this.clienteId = clienteId;
        this.status = status;
        this.valorFinal = valorFinal;
        this.dataFinal = dataFinal;
    }

    public String getId() { return id; }
    public String getVeiculoId() { return veiculoId; }
    public String getClienteId() { return clienteId; }
    public String getStatus() { return status; }
    public BigDecimal getValorFinal() { return valorFinal; }
    public LocalDateTime getDataFinal() { return dataFinal; }
}
