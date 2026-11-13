package com.mecanica.oficina_api.adapters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mecanica.oficina_api.adapters.common.FieldError;
import com.mecanica.oficina_api.adapters.common.FileUtils;
import com.mecanica.oficina_api.adapters.common.ValidationErrorResponse;
import com.mecanica.oficina_api.adapters.web.GlobalExceptionHandler;
import com.mecanica.oficina_api.adapters.web.OrdemServicoController;
import com.mecanica.oficina_api.adapters.web.dto.request.CriarOrdemServicoRequest;
import com.mecanica.oficina_api.adapters.web.presenter.OrdemServicoPresenter;
import com.mecanica.oficina_api.application.ordemservico.input.ConcluirDiagnosticoInput;
import com.mecanica.oficina_api.application.ordemservico.usecase.ConcluirDiagnosticoUseCase;
import com.mecanica.oficina_api.application.ordemservico.usecase.ConsultarOrdemServicoUseCase;
import com.mecanica.oficina_api.application.ordemservico.usecase.ListarHistoricoOrdemServicoUseCase;
import com.mecanica.oficina_api.domain.ordemservico.HistoricoOrdemServico;
import com.mecanica.oficina_api.application.ordemservico.usecase.CriarOrdemServicoUseCase;
import com.mecanica.oficina_api.application.ordemservico.usecase.EntregarOrdemServicoUseCase;
import com.mecanica.oficina_api.application.ordemservico.usecase.IniciarDiagnosticoOrdemServicoUseCase;
import com.mecanica.oficina_api.application.ordemservico.usecase.ListarOrdensServicoUseCase;
import com.mecanica.oficina_api.application.ordemservico.usecase.ListarPorVeiculoOrdemServicoUseCase;
import com.mecanica.oficina_api.domain.ordemservico.OrdemServico;
import com.mecanica.oficina_api.domain.ordemservico.OrdemServicoStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OrdemServicoControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final FileUtils fileUtils = new FileUtils();

    @Mock private CriarOrdemServicoUseCase criarOrdemServicoUseCase;
    @Mock private ConsultarOrdemServicoUseCase consultarOrdemServicoUseCase;
    @Mock private ListarOrdensServicoUseCase listarOrdensServicoUseCase;
    @Mock private ListarPorVeiculoOrdemServicoUseCase listarPorVeiculoOrdemServicoUseCase;
    @Mock private IniciarDiagnosticoOrdemServicoUseCase iniciarDiagnosticoOrdemServicoUseCase;
    @Mock private EntregarOrdemServicoUseCase entregarOrdemServicoUseCase;
    @Mock private ConcluirDiagnosticoUseCase concluirDiagnosticoUseCase;
    @Mock private ListarHistoricoOrdemServicoUseCase listarHistoricoOrdemServicoUseCase;

    private static final String OS_ID = "os-1";
    private static final String VEICULO_ID = "veiculo-1";
    private static final String CLIENTE_ID = "cliente-1";

    @BeforeEach
    void setUp() {
        OrdemServicoController controller = new OrdemServicoController(
                criarOrdemServicoUseCase, consultarOrdemServicoUseCase, listarOrdensServicoUseCase,
                listarPorVeiculoOrdemServicoUseCase, iniciarDiagnosticoOrdemServicoUseCase,
                entregarOrdemServicoUseCase, concluirDiagnosticoUseCase, listarHistoricoOrdemServicoUseCase,
                new OrdemServicoPresenter());
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // --- helpers de construção de domínio ---

    private OrdemServico osRecebida() {
        return OrdemServico.reconstituir(OS_ID, VEICULO_ID, CLIENTE_ID,
                OrdemServicoStatus.RECEBIDA, null, null);
    }

    private CriarOrdemServicoRequest criarRequest() {
        CriarOrdemServicoRequest request = new CriarOrdemServicoRequest();
        request.setVeiculoId(VEICULO_ID);
        request.setClienteId(CLIENTE_ID);
        return request;
    }

    // --- listar ---

    @Test
    void deveListarOrdensERetornar200() throws Exception {
        when(listarOrdensServicoUseCase.executar(null)).thenReturn(List.of(osRecebida()));

        mockMvc.perform(get("/ordem-servico"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(OS_ID))
                .andExpect(jsonPath("$[0].status").value("RECEBIDA"));
    }

    @Test
    void deveListarOrdensFiltradasPorStatusERetornar200() throws Exception {
        when(listarOrdensServicoUseCase.executar(OrdemServicoStatus.RECEBIDA))
                .thenReturn(List.of(osRecebida()));

        mockMvc.perform(get("/ordem-servico").param("status", "RECEBIDA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("RECEBIDA"));
    }

    // --- criar ---

    @Test
    void deveCriarOrdemERetornar201() throws Exception {
        when(criarOrdemServicoUseCase.executar(eq(VEICULO_ID), eq(CLIENTE_ID))).thenReturn(osRecebida());

        mockMvc.perform(post("/ordem-servico")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(criarRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(OS_ID))
                .andExpect(jsonPath("$.veiculoId").value(VEICULO_ID))
                .andExpect(jsonPath("$.clienteId").value(CLIENTE_ID))
                .andExpect(jsonPath("$.status").value("RECEBIDA"));
    }

    @Test
    void deveRetornar404AoCriar_quandoVeiculoNaoEncontrado() throws Exception {
        when(criarOrdemServicoUseCase.executar(eq(VEICULO_ID), eq(CLIENTE_ID)))
                .thenThrow(new IllegalArgumentException("Veículo não encontrado"));

        mockMvc.perform(post("/ordem-servico")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(criarRequest())))
                .andExpect(status().isNotFound());
    }

    // --- buscar por id ---

    @Test
    void deveBuscarOrdemPorIdERetornar200() throws Exception {
        when(consultarOrdemServicoUseCase.executar(OS_ID)).thenReturn(osRecebida());

        mockMvc.perform(get("/ordem-servico/" + OS_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(OS_ID))
                .andExpect(jsonPath("$.status").value("RECEBIDA"));
    }

    @Test
    void deveRetornar404AoBuscar_quandoOrdemNaoEncontrada() throws Exception {
        when(consultarOrdemServicoUseCase.executar("inexistente"))
                .thenThrow(new IllegalArgumentException("OS não encontrada"));

        mockMvc.perform(get("/ordem-servico/inexistente"))
                .andExpect(status().isNotFound());
    }

    // --- listar por veiculo ---

    @Test
    void deveListarPorVeiculoERetornar200() throws Exception {
        when(listarPorVeiculoOrdemServicoUseCase.executar(VEICULO_ID))
                .thenReturn(List.of(osRecebida()));

        mockMvc.perform(get("/ordem-servico/veiculo/" + VEICULO_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(OS_ID));
    }

    // --- transições de status ---

    @Test
    void deveIniciarDiagnosticoERetornar200() throws Exception {
        when(iniciarDiagnosticoOrdemServicoUseCase.executar(OS_ID))
                .thenReturn(OrdemServico.reconstituir(OS_ID, VEICULO_ID, CLIENTE_ID,
                        OrdemServicoStatus.EM_DIAGNOSTICO, null, null));

        mockMvc.perform(patch("/ordem-servico/" + OS_ID + "/iniciar-diagnostico"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_DIAGNOSTICO"));
    }

    @Test
    void deveRetornar409AoIniciarDiagnostico_quandoEstadoInvalido() throws Exception {
        when(iniciarDiagnosticoOrdemServicoUseCase.executar(OS_ID))
                .thenThrow(new IllegalStateException("OS deve estar RECEBIDA"));

        mockMvc.perform(patch("/ordem-servico/" + OS_ID + "/iniciar-diagnostico"))
                .andExpect(status().isConflict());
    }

    // --- Saga ---

    @Test
    void deveConcluirDiagnosticoERetornar200() throws Exception {
        var input = new ConcluirDiagnosticoInput(
                List.of(new ConcluirDiagnosticoInput.Servico("servico-1", 1)),
                List.of(new ConcluirDiagnosticoInput.Insumo("ins-1", 2, "Óleo", new BigDecimal("45.00"))),
                "trocar filtro");
        when(concluirDiagnosticoUseCase.executar(OS_ID, input))
                .thenReturn(OrdemServico.reconstituir(OS_ID, VEICULO_ID, CLIENTE_ID, OrdemServicoStatus.EM_DIAGNOSTICO, null, null));

        mockMvc.perform(patch("/ordem-servico/" + OS_ID + "/concluir-diagnostico")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"servicos":[{"servicoId":"servico-1","quantidade":1}],
                                 "insumos":[{"insumoId":"ins-1","quantidade":2,"descricao":"Óleo","precoUnitario":45.00}],
                                 "observacoes":"trocar filtro"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_DIAGNOSTICO"));
    }

    @Test
    void deveRetornar400AoConcluirDiagnosticoComInsumoSemPreco() throws Exception {
        mockMvc.perform(patch("/ordem-servico/" + OS_ID + "/concluir-diagnostico")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"insumos\":[{\"insumoId\":\"ins-1\",\"quantidade\":1,\"descricao\":\"Óleo\"}]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveListarHistoricoDaOs() throws Exception {
        when(listarHistoricoOrdemServicoUseCase.executar(OS_ID)).thenReturn(List.of(
                new HistoricoOrdemServico(OS_ID, "OrcamentoGerado", HistoricoOrdemServico.Origem.RECEBIDO,
                        OrdemServicoStatus.EM_DIAGNOSTICO, OrdemServicoStatus.AGUARDANDO_APROVACAO, false, "{}",
                        java.time.Instant.parse("2026-11-08T10:00:00Z")),
                new HistoricoOrdemServico(OS_ID, "DiagnosticoConcluido", HistoricoOrdemServico.Origem.PUBLICADO,
                        null, OrdemServicoStatus.EM_DIAGNOSTICO, false, null,
                        java.time.Instant.parse("2026-11-08T09:59:00Z"))));

        mockMvc.perform(get("/ordem-servico/" + OS_ID + "/historico"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].evento").value("OrcamentoGerado"))
                .andExpect(jsonPath("$[0].origem").value("RECEBIDO"))
                .andExpect(jsonPath("$[0].statusAnterior").value("EM_DIAGNOSTICO"))
                .andExpect(jsonPath("$[0].statusAtual").value("AGUARDANDO_APROVACAO"))
                .andExpect(jsonPath("$[1].statusAnterior").doesNotExist());
    }

    @Test
    void deveEntregarERetornar200() throws Exception {
        when(entregarOrdemServicoUseCase.executar(OS_ID))
                .thenReturn(OrdemServico.reconstituir(OS_ID, VEICULO_ID, CLIENTE_ID,
                        OrdemServicoStatus.ENTREGUE, null, null));

        mockMvc.perform(patch("/ordem-servico/" + OS_ID + "/entregar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENTREGUE"));
    }

    @ParameterizedTest
    @MethodSource("dadosCamposInvalidosOrdemServico")
    void deveRetornar400_quandoCamposEstaoInvalidosAoCadastrarOrdemServico(String filename, List<String> errors) throws Exception {
        var request = fileUtils.readResourceFile("/ordem_servicos/%s".formatted(filename));

        var mvcResult = mockMvc.perform(MockMvcRequestBuilders.post("/ordem-servico")
                .content(request)
                .contentType(MediaType.APPLICATION_JSON)
            )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andReturn()
            .getResponse()
            .getContentAsString();

        var responseError = objectMapper.readValue(mvcResult, ValidationErrorResponse.class);

        Assertions.assertThat(responseError.erros())
            .extracting(FieldError::mensagem)
            .containsExactlyInAnyOrderElementsOf(errors);
    }

    private static Stream<Arguments> dadosCamposInvalidosOrdemServico() {
        var erroVeiculoId = "O ID do veículo é obrigatório";
        var erroClienteId = "O ID do cliente é obrigatório";

        var errosComuns = List.of(erroClienteId, erroVeiculoId);

        return Stream.of(
            Arguments.of("cadastrar-ordem-servico-400-campos-vazios.json", errosComuns),
            Arguments.of("cadastrar-ordem-servico-400-campos-nulos.json", errosComuns)
        );
    }
}
