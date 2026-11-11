package com.mecanica.oficina_api.adapters.web.dto.request;

import jakarta.validation.constraints.NotBlank;

public record FalharExecucaoRequest(@NotBlank(message = "O motivo da falha é obrigatório") String motivo) {
}
