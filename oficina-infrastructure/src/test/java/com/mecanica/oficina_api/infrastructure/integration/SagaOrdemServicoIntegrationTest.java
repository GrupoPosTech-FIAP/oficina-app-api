package com.mecanica.oficina_api.infrastructure.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.mecanica.oficina_api.adapters.messaging.ProcessadorEventos;
import com.mecanica.oficina_api.adapters.persistence.ClienteJpaEntity;
import com.mecanica.oficina_api.adapters.persistence.OrdemServicoJpaEntity;
import com.mecanica.oficina_api.adapters.persistence.OutboxEventoJpaEntity;
import com.mecanica.oficina_api.adapters.persistence.ServicoJpaEntity;
import com.mecanica.oficina_api.adapters.persistence.VeiculoJpaEntity;
import com.mecanica.oficina_api.adapters.persistence.repository.OutboxEventoSpringDataRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Saga do ponto de vista da OS, com Postgres real: eventos publicados vão para a outbox e eventos
 * recebidos (simulando billing/inventory) movem a OS e entram no histórico.
 */
class SagaOrdemServicoIntegrationTest extends BaseIntegrationTest {

    @Autowired private ProcessadorEventos processador;
    @Autowired private OutboxEventoSpringDataRepository outbox;

    private String osId;
    private String servicoId;

    @BeforeEach
    void criarOsEmDiagnostico() throws Exception {
        ClienteJpaEntity c = clienteRepository.save(cliente("Bruno Lima", "52998224725", "bruno@teste.com"));

        VeiculoJpaEntity v = new VeiculoJpaEntity();
        v.setCliente(c);
        v.setPlaca("SAG" + String.format("%04d", Math.abs(UUID.randomUUID().hashCode()) % 10000));
        v.setMarca("Fiat");
        v.setModelo("Uno");
        v.setAno(2015);
        v.setCor("Prata");
        v.setAtivo(true);
        v = veiculoRepository.save(v);

        ServicoJpaEntity s = new ServicoJpaEntity();
        s.setId(UUID.randomUUID().toString());
        s.setNome("Troca de óleo");
        s.setDescricao("Troca de óleo e filtro");
        s.setPreco(new BigDecimal("80.00"));
        s.setTempoEstimadoHoras(Duration.ofHours(1));
        s.setAtivo(true);
        servicoId = servicoRepository.save(s).getId();

        OrdemServicoJpaEntity os = new OrdemServicoJpaEntity();
        os.setVeiculoId(v.getId());
        os.setClienteId(c.getId());
        os.setStatus("EM_DIAGNOSTICO");
        os.setDataCriacao(LocalDateTime.now());
        osId = ordemServicoRepository.save(os).getId();
    }

    private String statusDaOs() throws Exception {
        String json = mockMvc.perform(comToken(get("/ordem-servico/" + osId)))
                .andReturn().getResponse().getContentAsString();
        return (String) objectMapper.readValue(json, Map.class).get("status");
    }

    private List<OutboxEventoJpaEntity> publicados() {
        return outbox.findAll().stream()
                .filter(e -> e.getSagaId().equals(osId))
                .sorted(Comparator.comparing(OutboxEventoJpaEntity::getCriadoEm))
                .toList();
    }

    private List<Map<String, Object>> historico() throws Exception {
        String json = mockMvc.perform(comToken(get("/ordem-servico/" + osId + "/historico")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(json, new TypeReference<>() {});
    }

    private void receber(String eventType) {
        SagaIntegrationSupport.receber(processador, eventType, osId, "oficina-billing-service",
                "{\"ordemServicoId\":\"" + osId + "\"}");
    }

    @Test
    void concluirDiagnosticoPublicaItensPrecificadosParaOBilling() throws Exception {
        SagaIntegrationSupport.concluirDiagnostico(mockMvc, this::comToken, osId, servicoId);

        List<OutboxEventoJpaEntity> eventos = publicados();
        assertThat(eventos).extracting(OutboxEventoJpaEntity::getEventType).containsExactly("DiagnosticoConcluido");
        Map<?, ?> envelope = objectMapper.readValue(eventos.getFirst().getEnvelope(), Map.class);
        Map<?, ?> payload = (Map<?, ?>) envelope.get("payload");
        assertThat(payload.get("servicos").toString()).contains(servicoId, "Troca de óleo", "80");
        assertThat(payload.get("insumos").toString()).contains("ins-1", "45");
        assertThat(statusDaOs()).isEqualTo("EM_DIAGNOSTICO");
    }

    @Test
    void falhaNaExecucaoCancelaEPublicaExecucaoFalhouParaAsCompensacoes() throws Exception {
        SagaIntegrationSupport.liberarParaExecucao(mockMvc, this::comToken, processador, osId, servicoId);
        assertThat(statusDaOs()).isEqualTo("AGUARDANDO_EXECUCAO");

        mockMvc.perform(comToken(patch("/ordem-servico/" + osId + "/iniciar-execucao")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_EXECUCAO"));
        mockMvc.perform(comToken(patch("/ordem-servico/" + osId + "/falhar-execucao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Bloco do motor trincado\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADA"));

        receber("EstoqueEstornado");
        receber("PagamentoEstornado");

        assertThat(publicados()).extracting(OutboxEventoJpaEntity::getEventType)
                .containsExactly("DiagnosticoConcluido", "ExecucaoIniciada", "ExecucaoFalhou");
        assertThat(historico()).extracting(h -> h.get("evento")).containsExactly(
                "DiagnosticoConcluido", "OrcamentoGerado", "OrcamentoAprovado", "EstoqueBaixado", "CobrancaCriada",
                "PagamentoConfirmado", "ExecucaoIniciada", "ExecucaoFalhou", "EstoqueEstornado", "PagamentoEstornado");
        assertThat(historico().get(7)).containsEntry("statusAtual", "CANCELADA").containsEntry("detalhe", "Bloco do motor trincado");
    }

    @Test
    void estoqueInsuficienteCancelaAOs() throws Exception {
        SagaIntegrationSupport.concluirDiagnostico(mockMvc, this::comToken, osId, servicoId);
        receber("OrcamentoGerado");
        receber("OrcamentoAprovado");
        receber("EstoqueInsuficiente");

        assertThat(statusDaOs()).isEqualTo("CANCELADA");
    }

    @Test
    void eventoForaDeOrdemEDescartadoERegistrado() throws Exception {
        receber("PagamentoConfirmado");

        assertThat(statusDaOs()).isEqualTo("EM_DIAGNOSTICO");
        assertThat(historico()).singleElement().satisfies(h -> {
            assertThat(h).containsEntry("evento", "PagamentoConfirmado");
            assertThat(h).containsEntry("descartado", true);
        });
    }

    @Test
    void naoIniciaExecucaoSemPagamento() throws Exception {
        mockMvc.perform(comToken(patch("/ordem-servico/" + osId + "/iniciar-execucao")))
                .andExpect(status().isConflict());
    }
}
