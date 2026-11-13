package com.mecanica.oficina_api.infrastructure.bdd;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mecanica.oficina_api.adapters.messaging.EventoEnvelope;
import com.mecanica.oficina_api.adapters.messaging.ProcessadorEventos;
import com.mecanica.oficina_api.adapters.persistence.ClienteJpaEntity;
import com.mecanica.oficina_api.adapters.persistence.OrdemServicoJpaEntity;
import com.mecanica.oficina_api.adapters.persistence.OutboxEventoJpaEntity;
import com.mecanica.oficina_api.adapters.persistence.ServicoJpaEntity;
import com.mecanica.oficina_api.adapters.persistence.UsuarioJpaEntity;
import com.mecanica.oficina_api.adapters.persistence.VeiculoJpaEntity;
import com.mecanica.oficina_api.adapters.persistence.repository.ClienteSpringDataRepository;
import com.mecanica.oficina_api.adapters.persistence.repository.OrdemServicoSpringDataRepository;
import com.mecanica.oficina_api.adapters.persistence.repository.OutboxEventoSpringDataRepository;
import com.mecanica.oficina_api.adapters.persistence.repository.ServicoSpringDataRepository;
import com.mecanica.oficina_api.adapters.persistence.repository.UsuarioSpringDataRepository;
import com.mecanica.oficina_api.adapters.persistence.repository.VeiculoSpringDataRepository;
import com.mecanica.oficina_api.domain.usuario.Perfil;
import io.cucumber.java.Before;
import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.E;
import io.cucumber.java.pt.Então;
import io.cucumber.java.pt.Quando;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Passos da feature {@code saga_ordem_servico.feature}. Os eventos de billing e inventory vão
 * direto ao {@link ProcessadorEventos}, o mesmo que o consumidor SQS chama.
 */
public class SagaOrdemServicoSteps {

    private static final JsonMapper ENVELOPE_JSON = JsonMapper.builder().build();
    private static final Map<String, String> PRODUTORES = Map.of(
            "billing", "oficina-billing-service",
            "inventory", "oficina-inventory-service",
            "execution", "oficina-execution-service");

    @Autowired private WebApplicationContext context;
    @Autowired private ProcessadorEventos processador;
    @Autowired private OutboxEventoSpringDataRepository outbox;
    @Autowired private UsuarioSpringDataRepository usuarioRepository;
    @Autowired private ClienteSpringDataRepository clienteRepository;
    @Autowired private VeiculoSpringDataRepository veiculoRepository;
    @Autowired private OrdemServicoSpringDataRepository ordemServicoRepository;
    @Autowired private ServicoSpringDataRepository servicoRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;
    private String token;
    private String osId;
    private String servicoId;
    private List<ProcessadorEventos.Resultado> entregas;

    @Before
    public void limparBaseEAutenticar() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        ordemServicoRepository.deleteAll();
        servicoRepository.deleteAllInBatch();
        veiculoRepository.deleteAllInBatch();
        clienteRepository.deleteAllInBatch();
        usuarioRepository.deleteAllInBatch();

        UsuarioJpaEntity admin = new UsuarioJpaEntity();
        admin.setId(UUID.randomUUID().toString());
        admin.setNome("Admin BDD");
        admin.setEmail("admin@bdd.test");
        admin.setSenha(passwordEncoder.encode("admin123"));
        admin.setPerfil(Perfil.ADMIN);
        admin.setDataCadastro(LocalDateTime.now());
        admin.setDataAtualizacao(LocalDateTime.now());
        admin.setAtivo(true);
        usuarioRepository.save(admin);

        String login = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@bdd.test\",\"senha\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        token = (String) objectMapper.readValue(login, Map.class).get("token");
    }

    // ---------- Dado ----------

    @Dado("^uma ordem de serviço em diagnóstico para o serviço \"([^\"]*)\" de R\\$ ([\\d.,]+)$")
    public void umaOrdemDeServicoEmDiagnostico(String servico, String preco) {
        ClienteJpaEntity c = new ClienteJpaEntity();
        c.setId(UUID.randomUUID().toString());
        c.setNome("Carla Dias");
        c.setDocumento("52998224725");
        c.setEmail("carla@bdd.test");
        c.setTelefone("11999990000");
        c.setDataCadastro(LocalDateTime.now());
        c.setAtivo(true);
        c = clienteRepository.save(c);

        VeiculoJpaEntity v = new VeiculoJpaEntity();
        v.setCliente(c);
        v.setPlaca("BDD" + String.format("%04d", Math.abs(UUID.randomUUID().hashCode()) % 10000));
        v.setMarca("Honda");
        v.setModelo("Civic");
        v.setAno(2019);
        v.setCor("Preto");
        v.setAtivo(true);
        v = veiculoRepository.save(v);

        ServicoJpaEntity s = new ServicoJpaEntity();
        s.setId(UUID.randomUUID().toString());
        s.setNome(servico);
        s.setDescricao(servico);
        s.setPreco(valor(preco));
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

    @Dado("que o diagnóstico foi concluído")
    public void queODiagnosticoFoiConcluido() throws Exception {
        concluirDiagnostico(2, "Óleo 5W30", new BigDecimal("45.00"));
    }

    /** Conclui o diagnóstico e entrega, em ordem, os eventos que os outros serviços já teriam publicado. */
    @Dado("que a saga já recebeu {string}")
    public void queASagaJaRecebeu(String eventos) throws Exception {
        queODiagnosticoFoiConcluido();
        for (String evento : lista(eventos)) {
            entregar(produtorDe(evento), evento, UUID.randomUUID().toString());
        }
    }

    // ---------- Quando ----------

    @Quando("^o mecânico conclui o diagnóstico com (\\d+) unidades do insumo \"([^\"]*)\" a R\\$ ([\\d.,]+)$")
    public void oMecanicoConcluiODiagnostico(int quantidade, String insumo, String preco) throws Exception {
        concluirDiagnostico(quantidade, insumo, valor(preco));
    }

    @Quando("o {word} publica {string}")
    public void oServicoPublica(String servico, String evento) {
        entregar(PRODUTORES.get(servico), evento, UUID.randomUUID().toString());
    }

    @Quando("o {word} entrega duas vezes o mesmo {string}")
    public void oServicoEntregaDuasVezes(String servico, String evento) {
        String eventId = UUID.randomUUID().toString();
        entregas = List.of(entregar(PRODUTORES.get(servico), evento, eventId),
                entregar(PRODUTORES.get(servico), evento, eventId));
    }

    @E("a execução falha com o motivo {string}")
    public void aExecucaoFalha(String motivo) {
        entregar(PRODUTORES.get("execution"), "ExecucaoFalhou", UUID.randomUUID().toString(),
                "{\"ordemServicoId\":\"" + osId + "\",\"motivo\":\"" + motivo + "\"}");
    }

    // ---------- Então ----------

    @Então("a ordem de serviço fica {string}")
    public void aOrdemDeServicoFica(String status) throws Exception {
        String json = executar(get("/ordem-servico/" + osId)).getResponse().getContentAsString();
        assertThat(objectMapper.readValue(json, Map.class).get("status")).isEqualTo(status);
    }

    @Então("o evento {string} é publicado com o serviço e o insumo precificados")
    public void oEventoEPublicadoComItensPrecificados(String evento) throws Exception {
        Map<?, ?> payload = payloadPublicado(evento);
        assertThat(payload.get("servicos").toString()).contains(servicoId, "80");
        assertThat(payload.get("insumos").toString()).contains("Óleo 5W30", "45");
    }

    @E("o histórico guarda o motivo {string} em {string}")
    public void oHistoricoGuardaOMotivo(String motivo, String evento) throws Exception {
        assertThat(historico()).filteredOn(h -> evento.equals(h.get("evento"))).singleElement()
                .satisfies(h -> assertThat(String.valueOf(h.get("detalhe"))).contains(motivo));
    }

    @E("os eventos publicados são {string}")
    public void osEventosPublicadosSao(String eventos) {
        assertThat(publicados()).extracting(OutboxEventoJpaEntity::getEventType).containsExactlyElementsOf(lista(eventos));
    }

    @E("o histórico registra {string} levando de {string} para {string}")
    public void oHistoricoRegistraATransicao(String evento, String anterior, String atual) throws Exception {
        assertThat(historico()).filteredOn(h -> evento.equals(h.get("evento"))).singleElement().satisfies(h -> {
            assertThat(h).containsEntry("statusAnterior", anterior);
            assertThat(h).containsEntry("statusAtual", atual);
            assertThat(h).containsEntry("descartado", false);
        });
    }

    @E("o histórico termina com {string}")
    public void oHistoricoTerminaCom(String eventos) throws Exception {
        List<Object> registrados = historico().stream().map(h -> h.get("evento")).toList();
        assertThat(registrados).endsWith(lista(eventos).toArray());
    }

    @E("o histórico marca {string} como descartado")
    public void oHistoricoMarcaComoDescartado(String evento) throws Exception {
        assertThat(historico()).filteredOn(h -> evento.equals(h.get("evento"))).singleElement()
                .satisfies(h -> assertThat(h).containsEntry("descartado", true));
    }

    @E("o histórico tem {int} registro de {string}")
    public void oHistoricoTemRegistros(int quantidade, String evento) throws Exception {
        assertThat(historico()).filteredOn(h -> evento.equals(h.get("evento"))).hasSize(quantidade);
    }

    @Então("a segunda entrega é ignorada como duplicada")
    public void aSegundaEntregaEIgnorada() {
        assertThat(entregas).containsExactly(ProcessadorEventos.Resultado.PROCESSADO, ProcessadorEventos.Resultado.DUPLICADO);
    }

    // ---------- apoio ----------

    private void concluirDiagnostico(int quantidade, String insumo, BigDecimal preco) throws Exception {
        exigirOk(executar(patch("/ordem-servico/" + osId + "/concluir-diagnostico")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "servicos", List.of(Map.of("servicoId", servicoId, "quantidade", 1)),
                        "insumos", List.of(Map.of("insumoId", "ins-1", "quantidade", quantidade,
                                "descricao", insumo, "precoUnitario", preco)))))));
    }

    private ProcessadorEventos.Resultado entregar(String produtor, String evento, String eventId) {
        String payload = "CobrancaCriada".equals(evento)
                ? "{\"ordemServicoId\":\"" + osId + "\",\"checkoutUrl\":\"https://mp/checkout\"}"
                : "{\"ordemServicoId\":\"" + osId + "\"}";
        return entregar(produtor, evento, eventId, payload);
    }

    private ProcessadorEventos.Resultado entregar(String produtor, String evento, String eventId, String payload) {
        return processador.processar(new EventoEnvelope(eventId, evento, 1, Instant.now(), produtor, osId,
                ENVELOPE_JSON.readTree(payload)));
    }

    private static String produtorDe(String evento) {
        if (evento.startsWith("Estoque")) return PRODUTORES.get("inventory");
        if (evento.startsWith("Execucao")) return PRODUTORES.get("execution");
        return PRODUTORES.get("billing");
    }

    private MvcResult executar(MockHttpServletRequestBuilder req) throws Exception {
        return mockMvc.perform(req.header("Authorization", "Bearer " + token)).andReturn();
    }

    private static void exigirOk(MvcResult resultado) throws Exception {
        assertThat(resultado.getResponse().getStatus())
                .as(resultado.getResponse().getContentAsString())
                .isEqualTo(200);
    }

    private List<OutboxEventoJpaEntity> publicados() {
        return outbox.findAll().stream()
                .filter(e -> e.getSagaId().equals(osId))
                .sorted(Comparator.comparing(OutboxEventoJpaEntity::getCriadoEm))
                .toList();
    }

    private Map<?, ?> payloadPublicado(String evento) throws Exception {
        OutboxEventoJpaEntity publicado = publicados().stream()
                .filter(e -> e.getEventType().equals(evento))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Evento não publicado: " + evento));
        return (Map<?, ?>) objectMapper.readValue(publicado.getEnvelope(), Map.class).get("payload");
    }

    private List<Map<String, Object>> historico() throws Exception {
        MvcResult resultado = executar(get("/ordem-servico/" + osId + "/historico"));
        exigirOk(resultado);
        return objectMapper.readValue(resultado.getResponse().getContentAsString(), new TypeReference<>() {});
    }

    private static List<String> lista(String valores) {
        return Arrays.stream(valores.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    private static BigDecimal valor(String reais) {
        return new BigDecimal(reais.replace(".", "").replace(",", "."));
    }
}
