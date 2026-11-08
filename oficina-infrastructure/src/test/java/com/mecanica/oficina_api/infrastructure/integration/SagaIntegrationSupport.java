package com.mecanica.oficina_api.infrastructure.integration;

import com.mecanica.oficina_api.adapters.messaging.EventoEnvelope;
import com.mecanica.oficina_api.adapters.messaging.ProcessadorEventos;
import java.time.Instant;
import java.util.UUID;
import java.util.function.UnaryOperator;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.json.JsonMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Simula as respostas de billing e inventory entregando os eventos direto ao processador da fila. */
final class SagaIntegrationSupport {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private SagaIntegrationSupport() {
    }

    static ProcessadorEventos.Resultado receber(ProcessadorEventos processador, String eventType, String osId,
                                                String produtor, String payload) {
        return processador.processar(new EventoEnvelope(UUID.randomUUID().toString(), eventType, 1, Instant.now(),
                produtor, osId, JSON.readTree(payload)));
    }

    static void concluirDiagnostico(MockMvc mockMvc, UnaryOperator<MockHttpServletRequestBuilder> comToken,
                                    String osId, String servicoId) throws Exception {
        mockMvc.perform(comToken.apply(patch("/ordem-servico/" + osId + "/concluir-diagnostico")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"servicos":[{"servicoId":"%s","quantidade":1}],
                                 "insumos":[{"insumoId":"ins-1","quantidade":2,"descricao":"Óleo 5W30","precoUnitario":45.00}]}
                                """.formatted(servicoId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_DIAGNOSTICO"));
    }

    /** EM_DIAGNOSTICO → AGUARDANDO_EXECUCAO pelo caminho feliz da Saga. */
    static void liberarParaExecucao(MockMvc mockMvc, UnaryOperator<MockHttpServletRequestBuilder> comToken,
                                    ProcessadorEventos processador, String osId, String servicoId) throws Exception {
        concluirDiagnostico(mockMvc, comToken, osId, servicoId);
        String os = "{\"ordemServicoId\":\"" + osId + "\"}";
        receber(processador, "OrcamentoGerado", osId, "oficina-billing-service", os);
        receber(processador, "OrcamentoAprovado", osId, "oficina-billing-service", os);
        receber(processador, "EstoqueBaixado", osId, "oficina-inventory-service", os);
        receber(processador, "CobrancaCriada", osId, "oficina-billing-service",
                "{\"ordemServicoId\":\"" + osId + "\",\"checkoutUrl\":\"https://mp/checkout\"}");
        receber(processador, "PagamentoConfirmado", osId, "oficina-billing-service", os);
    }
}
