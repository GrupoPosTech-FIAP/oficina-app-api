package com.mecanica.oficina_api.adapters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mecanica.oficina_api.adapters.common.FieldError;
import com.mecanica.oficina_api.adapters.common.FileUtils;
import com.mecanica.oficina_api.adapters.common.ValidationErrorResponse;
import com.mecanica.oficina_api.adapters.web.GlobalExceptionHandler;
import com.mecanica.oficina_api.adapters.web.OrdemServicoController;
import com.mecanica.oficina_api.adapters.web.dto.request.CriarOrdemServicoRequest;
import com.mecanica.oficina_api.adapters.web.dto.request.IniciarExecucaoRequest;
import com.mecanica.oficina_api.adapters.web.presenter.OrdemServicoPresenter;
import com.mecanica.oficina_api.application.ordemservico.usecase.ConsultarOrdemServicoUseCase;
import com.mecanica.oficina_api.application.ordemservico.usecase.CriarOrdemServicoUseCase;
import com.mecanica.oficina_api.application.ordemservico.usecase.EntregarOrdemServicoUseCase;
import com.mecanica.oficina_api.application.ordemservico.usecase.FinalizarOrdemServicoUseCase;
import com.mecanica.oficina_api.application.ordemservico.usecase.FinalizarServicoUseCase;
import com.mecanica.oficina_api.application.ordemservico.usecase.IniciarDiagnosticoOrdemServicoUseCase;
import com.mecanica.oficina_api.application.ordemservico.usecase.IniciarExecucaoUseCase;
import com.mecanica.oficina_api.application.ordemservico.usecase.IniciarServicoUseCase;
import com.mecanica.oficina_api.application.ordemservico.usecase.ListarOrdensServicoUseCase;
import com.mecanica.oficina_api.application.ordemservico.usecase.ListarPorVeiculoOrdemServicoUseCase;
import com.mecanica.oficina_api.application.ordemservico.usecase.ListarServicosPorOSUseCase;
import com.mecanica.oficina_api.domain.ordemservico.OrdemServico;
import com.mecanica.oficina_api.domain.ordemservico.OrdemServicoStatus;
import com.mecanica.oficina_api.domain.ordemservico.StatusServico;

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
    @Mock private IniciarExecucaoUseCase iniciarExecucaoUseCase;
    @Mock private FinalizarOrdemServicoUseCase finalizarOrdemServicoUseCase;
    @Mock private EntregarOrdemServicoUseCase entregarOrdemServicoUseCase;
    @Mock private ListarServicosPorOSUseCase listarServicosPorOSUseCase;
    @Mock private IniciarServicoUseCase iniciarServicoUseCase;
    @Mock private FinalizarServicoUseCase finalizarServicoUseCase;

    private static final String OS_ID = "os-1";
    private static final String VEICULO_ID = "veiculo-1";
    private static final String CLIENTE_ID = "cliente-1";

    @BeforeEach
    void setUp() {
        OrdemServicoController controller = new OrdemServicoController(
                criarOrdemServicoUseCase, consultarOrdemServicoUseCase, listarOrdensServicoUseCase,
                listarPorVeiculoOrdemServicoUseCase, iniciarDiagnosticoOrdemServicoUseCase,
                iniciarExecucaoUseCase, finalizarOrdemServicoUseCase, entregarOrdemServicoUseCase,
                listarServicosPorOSUseCase, iniciarServicoUseCase, finalizarServicoUseCase,
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

    private StatusServico statusServico() {
        return StatusServico.recriar("status-1",
                com.mecanica.oficina_api.domain.ordemservico.ServicoStatus.AGUARDANDO,
                OS_ID, "servico-1", null, null);
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

    @Test
    void deveIniciarExecucaoSemBodyERetornar200() throws Exception {
        when(iniciarExecucaoUseCase.executar(OS_ID, List.of()))
                .thenReturn(OrdemServico.reconstituir(OS_ID, VEICULO_ID, CLIENTE_ID,
                        OrdemServicoStatus.EM_EXECUCAO, null, null));

        mockMvc.perform(patch("/ordem-servico/" + OS_ID + "/iniciar-execucao"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_EXECUCAO"));
    }

    @Test
    void deveIniciarExecucaoComServicosERetornar200() throws Exception {
        IniciarExecucaoRequest req = new IniciarExecucaoRequest(List.of("servico-1", "servico-2"));
        when(iniciarExecucaoUseCase.executar(OS_ID, List.of("servico-1", "servico-2")))
                .thenReturn(OrdemServico.reconstituir(OS_ID, VEICULO_ID, CLIENTE_ID,
                        OrdemServicoStatus.EM_EXECUCAO, null, null));

        mockMvc.perform(patch("/ordem-servico/" + OS_ID + "/iniciar-execucao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_EXECUCAO"));
    }

    @Test
    void deveFinalizarERetornar200() throws Exception {
        when(finalizarOrdemServicoUseCase.executar(OS_ID))
                .thenReturn(OrdemServico.reconstituir(OS_ID, VEICULO_ID, CLIENTE_ID,
                        OrdemServicoStatus.FINALIZADA, new BigDecimal("100.00"), null));

        mockMvc.perform(patch("/ordem-servico/" + OS_ID + "/finalizar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINALIZADA"))
                .andExpect(jsonPath("$.valorFinal").value(100.00));
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

    // --- serviços ---

    @Test
    void deveListarServicosERetornar200() throws Exception {
        when(listarServicosPorOSUseCase.executar(OS_ID)).thenReturn(List.of(statusServico()));

        mockMvc.perform(get("/ordem-servico/" + OS_ID + "/servico/listar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("status-1"))
                .andExpect(jsonPath("$[0].status").value("AGUARDANDO"))
                .andExpect(jsonPath("$[0].servicoId").value("servico-1"));
    }

    @Test
    void deveIniciarServicoERetornar200() throws Exception {
        StatusServico iniciado = StatusServico.recriar("status-1",
                com.mecanica.oficina_api.domain.ordemservico.ServicoStatus.INICIADO,
                OS_ID, "servico-1", null, null);
        when(iniciarServicoUseCase.executar("status-1")).thenReturn(iniciado);

        mockMvc.perform(patch("/ordem-servico/servico/status-1/iniciar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INICIADO"));
    }

    @Test
    void deveRetornar404AoIniciarServico_quandoNaoEncontrado() throws Exception {
        when(iniciarServicoUseCase.executar("inexistente"))
                .thenThrow(new IllegalArgumentException("Serviço não encontrado"));

        mockMvc.perform(patch("/ordem-servico/servico/inexistente/iniciar"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveFinalizarServicoERetornar200() throws Exception {
        StatusServico finalizado = StatusServico.recriar("status-1",
                com.mecanica.oficina_api.domain.ordemservico.ServicoStatus.FINALIZADO,
                OS_ID, "servico-1", null, null);
        when(finalizarServicoUseCase.executar("status-1")).thenReturn(finalizado);

        mockMvc.perform(patch("/ordem-servico/servico/status-1/finalizar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINALIZADO"));
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
