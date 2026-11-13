package com.mecanica.oficina_api.infrastructure.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.mecanica.oficina_api.adapters.persistence.ClienteJpaEntity;
import com.mecanica.oficina_api.adapters.persistence.ServicoJpaEntity;
import com.mecanica.oficina_api.adapters.web.dto.request.CadastrarVeiculoRequest;
import com.mecanica.oficina_api.adapters.web.dto.request.CriarOrdemServicoRequest;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrdemServicoIntegrationTest extends BaseIntegrationTest {

    @org.springframework.beans.factory.annotation.Autowired
    private com.mecanica.oficina_api.adapters.messaging.ProcessadorEventos processadorEventos;

    private ClienteJpaEntity salvarCliente() {
        return clienteRepository.save(cliente("Ana Souza", "52998224725", "ana@teste.com"));
    }

    private String cadastrarVeiculo(String clienteId) throws Exception {
        CadastrarVeiculoRequest req = new CadastrarVeiculoRequest();
        req.setClienteId(clienteId);
        req.setPlaca(String.format("TST%04d", Math.abs(UUID.randomUUID().hashCode()) % 10000));
        req.setMarca("Toyota");
        req.setModelo("Corolla");
        req.setAno(2022);
        req.setCor("Branco");

        mockMvc.perform(comToken(post("/veiculo")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(req))))
            .andExpect(status().isCreated());

        String json = mockMvc.perform(comToken(get("/veiculo/cliente/" + clienteId)))
            .andReturn().getResponse().getContentAsString();
        List<Map<String, Object>> lista = objectMapper.readValue(json, new TypeReference<>() {});
        return (String) lista.get(0).get("id");
    }

    private String criarOs(String veiculoId, String clienteId) throws Exception {
        CriarOrdemServicoRequest req = new CriarOrdemServicoRequest();
        req.setVeiculoId(veiculoId);
        req.setClienteId(clienteId);

        mockMvc.perform(comToken(post("/ordem-servico")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(req))))
            .andExpect(status().isCreated());

        String json = mockMvc.perform(comToken(get("/ordem-servico/veiculo/" + veiculoId)))
            .andReturn().getResponse().getContentAsString();
        List<Map<String, Object>> lista = objectMapper.readValue(json, new TypeReference<>() {});
        return (String) lista.get(0).get("id");
    }

    private ServicoJpaEntity salvarServico(String nome, double preco) {
        ServicoJpaEntity servico = new ServicoJpaEntity();
        servico.setId(UUID.randomUUID().toString());
        servico.setNome(nome);
        servico.setDescricao("Servico de teste");
        servico.setPreco(BigDecimal.valueOf(preco));
        servico.setTempoEstimadoHoras(Duration.ofHours(1));
        servico.setAtivo(true);
        return servicoRepository.save(servico);
    }

    @Test
    void fluxoCompleto_criarOsDiagnosticarExecutarFinalizarRetirar() throws Exception {
        ClienteJpaEntity c = salvarCliente();
        String veiculoId = cadastrarVeiculo(c.getId());
        String osId = criarOs(veiculoId, c.getId());
        String servicoId = salvarServico("Troca de oleo", 80.0).getId();

        mockMvc.perform(comToken(get("/ordem-servico/" + osId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("RECEBIDA"));

        mockMvc.perform(comToken(patch("/ordem-servico/" + osId + "/iniciar-diagnostico")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("EM_DIAGNOSTICO"));

        // orçamento e pagamento acontecem no billing; aqui chegam como eventos da Saga
        SagaIntegrationSupport.liberarParaExecucao(mockMvc, this::comToken, processadorEventos, osId, servicoId);

        // execução e tarefas ficam no oficina-execution-service; a OS acompanha pelos eventos
        SagaIntegrationSupport.receberDaExecucao(processadorEventos, "ExecucaoIniciada", osId, "");
        mockMvc.perform(comToken(get("/ordem-servico/" + osId)))
            .andExpect(jsonPath("$.status").value("EM_EXECUCAO"));

        SagaIntegrationSupport.receberDaExecucao(processadorEventos, "ExecucaoConcluida", osId, "");
        mockMvc.perform(comToken(get("/ordem-servico/" + osId)))
            .andExpect(jsonPath("$.status").value("FINALIZADA"));

        mockMvc.perform(comToken(patch("/ordem-servico/" + osId + "/entregar")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ENTREGUE"));
    }

    @Test
    void criar_veiculoInexistente_retorna404() throws Exception {
        ClienteJpaEntity c = salvarCliente();
        CriarOrdemServicoRequest req = new CriarOrdemServicoRequest();
        req.setVeiculoId(UUID.randomUUID().toString());
        req.setClienteId(c.getId());

        mockMvc.perform(comToken(post("/ordem-servico")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(req))))
            .andExpect(status().isNotFound());
    }

    @Test
    void retirarVeiculo_osNaoFinalizada_retorna409() throws Exception {
        ClienteJpaEntity c = salvarCliente();
        String veiculoId = cadastrarVeiculo(c.getId());
        String osId = criarOs(veiculoId, c.getId());

        mockMvc.perform(comToken(patch("/ordem-servico/" + osId + "/entregar")))
            .andExpect(status().isConflict());
    }

    @Test
    void criar_semToken_retorna403() throws Exception {
        CriarOrdemServicoRequest req = new CriarOrdemServicoRequest();
        req.setVeiculoId("v");
        req.setClienteId("c");

        mockMvc.perform(post("/ordem-servico")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(req)))
            .andExpect(status().isForbidden());
    }

    @Test
    void listarPorVeiculo_semOs_retornaListaVazia() throws Exception {
        mockMvc.perform(comToken(get("/ordem-servico/veiculo/" + UUID.randomUUID())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }
}
