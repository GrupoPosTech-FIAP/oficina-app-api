
package com.mecanica.oficina_api.adapters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mecanica.oficina_api.adapters.common.FieldError;
import com.mecanica.oficina_api.adapters.common.FileUtils;
import com.mecanica.oficina_api.adapters.common.ValidationErrorResponse;
import com.mecanica.oficina_api.adapters.web.GlobalExceptionHandler;
import com.mecanica.oficina_api.adapters.web.ServicoController;
import com.mecanica.oficina_api.adapters.web.dto.request.AlterarServicoRequest;
import com.mecanica.oficina_api.adapters.web.dto.request.CadastrarServicoRequest;
import com.mecanica.oficina_api.adapters.web.presenter.ServicoPresenter;
import com.mecanica.oficina_api.application.servico.usecase.AlterarServicoUseCase;
import com.mecanica.oficina_api.application.servico.usecase.AtivarServicoUseCase;
import com.mecanica.oficina_api.application.servico.usecase.CadastrarServicoUseCase;
import com.mecanica.oficina_api.application.servico.usecase.ConsultarServicoUseCase;
import com.mecanica.oficina_api.application.servico.usecase.InativarServicoUseCase;
import com.mecanica.oficina_api.application.servico.usecase.ListarServicosUseCase;
import com.mecanica.oficina_api.domain.servico.Servico;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import java.util.UUID;
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

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ServicoControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final FileUtils fileUtils = new  FileUtils();

    @Mock
    private CadastrarServicoUseCase cadastrarServicoUseCase;
    @Mock
    private ConsultarServicoUseCase consultarServicoUseCase;
    @Mock
    private ListarServicosUseCase listarServicosUseCase;
    @Mock
    private AlterarServicoUseCase alterarServicoUseCase;
    @Mock
    private AtivarServicoUseCase ativarServicoUseCase;
    @Mock
    private InativarServicoUseCase inativarServicoUseCase;

    private Servico servico;

    @BeforeEach
    void setUp() {
        ServicoController controller = new ServicoController(
                cadastrarServicoUseCase, consultarServicoUseCase,
                listarServicosUseCase, alterarServicoUseCase, ativarServicoUseCase,
                inativarServicoUseCase, new ServicoPresenter());
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        servico = Servico.reconstituir(
                "servico-1", "Troca de óleo", "Troca completa de óleo do motor",
                new BigDecimal("150.00"), Duration.ofHours(2), true);
    }

    @Test
    void deveCadastrarServicoERetornar201() throws Exception {
        when(cadastrarServicoUseCase.executar("Troca de óleo", "Troca completa de óleo do motor",
                new BigDecimal("150.00"), 2)).thenReturn(servico);

        mockMvc.perform(post("/servico")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cadastrarRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("servico-1"))
                .andExpect(jsonPath("$.nome").value("Troca de óleo"))
                .andExpect(jsonPath("$.descricao").value("Troca completa de óleo do motor"))
                .andExpect(jsonPath("$.preco").value(150.00))
                .andExpect(jsonPath("$.tempoEstimadoHoras").value(2))
                .andExpect(jsonPath("$.ativo").value(true));
    }

    @Test
    void deveBuscarServicoPorIdERetornar200() throws Exception {
        when(consultarServicoUseCase.executar("servico-1")).thenReturn(servico);

        mockMvc.perform(get("/servico/servico-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("servico-1"))
                .andExpect(jsonPath("$.nome").value("Troca de óleo"))
                .andExpect(jsonPath("$.tempoEstimadoHoras").value(2));
    }

    @Test
    void deveRetornar404AoBuscar_quandoServicoNaoEncontrado() throws Exception {
        when(consultarServicoUseCase.executar("inexistente"))
                .thenThrow(new IllegalArgumentException("Serviço não encontrado"));

        mockMvc.perform(get("/servico/inexistente"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveListarServicosERetornar200() throws Exception {
        when(listarServicosUseCase.executar()).thenReturn(List.of(servico));

        mockMvc.perform(get("/servico"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("servico-1"))
                .andExpect(jsonPath("$[0].nome").value("Troca de óleo"));
    }

    @Test
    void deveAlterarServicoERetornar200() throws Exception {
        Servico alterado = Servico.reconstituir(
                "servico-1", "Troca de óleo premium", "Óleo sintético",
                new BigDecimal("250.00"), Duration.ofHours(3), true);
        when(alterarServicoUseCase.executar("servico-1", "Troca de óleo premium", "Óleo sintético",
                new BigDecimal("250.00"), 3)).thenReturn(alterado);

        mockMvc.perform(put("/servico/servico-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(alterarRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Troca de óleo premium"))
                .andExpect(jsonPath("$.preco").value(250.00))
                .andExpect(jsonPath("$.tempoEstimadoHoras").value(3));
    }

    @Test
    void deveRetornar404AoAlterar_quandoServicoNaoEncontrado() throws Exception {
        when(alterarServicoUseCase.executar("inexistente", "Troca de óleo premium", "Óleo sintético",
                new BigDecimal("250.00"), 3))
                .thenThrow(new IllegalArgumentException("Serviço não encontrado"));

        mockMvc.perform(put("/servico/inexistente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(alterarRequest())))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveAtivarServicoERetornar200() throws Exception {
        when(ativarServicoUseCase.executar("servico-1")).thenReturn(servico);

        mockMvc.perform(patch("/servico/servico-1/ativar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("servico-1"))
                .andExpect(jsonPath("$.ativo").value(true));
    }

    @Test
    void deveRetornar404AoAtivar_quandoServicoNaoEncontrado() throws Exception {
        when(ativarServicoUseCase.executar("inexistente"))
                .thenThrow(new IllegalArgumentException("Serviço não encontrado"));

        mockMvc.perform(patch("/servico/inexistente/ativar"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveInativarServicoERetornar204() throws Exception {
        mockMvc.perform(delete("/servico/servico-1"))
                .andExpect(status().isNoContent());

        verify(inativarServicoUseCase).executar("servico-1");
    }

    @Test
    void deveRetornar404AoInativar_quandoServicoNaoEncontrado() throws Exception {
        doThrow(new IllegalArgumentException("Serviço não encontrado"))
                .when(inativarServicoUseCase).executar("inexistente");

        mockMvc.perform(delete("/servico/inexistente"))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @MethodSource("dadosCamposInvalidos")
    void deveRetornar400_quandoCamposEstaoInvalidosAoCriar(String filename, List<String> errors) throws Exception {
        var request = fileUtils.readResourceFile("/servicos/%s".formatted(filename));

        var mvcResult = mockMvc.perform(MockMvcRequestBuilders.post("/servico")
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

    @ParameterizedTest
    @MethodSource("dadosCamposInvalidos")
    void deveRetornar400_quandoCamposEstaoInvalidosAoAlterar(String filename, List<String> errors) throws Exception {
        var request = fileUtils.readResourceFile("/servicos/%s".formatted(filename));

        var servicoId = UUID.randomUUID();

        var mvcResult = mockMvc.perform(MockMvcRequestBuilders.put("/servico/"+servicoId)
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

    private static Stream<Arguments> dadosCamposInvalidos() {

        var erroPrecoNegativo = "O preço deve ser maior ou igual a zero";
        var erroTempoNegativo = "O tempo estimado em horas deve ser maior que zero";

        return Stream.of(
            Arguments.of("cadastrar-servico-400-unidades-negativas.json", List.of(erroPrecoNegativo, erroTempoNegativo)),
            Arguments.of("alterar-servico-400-unidades-negativas.json", List.of(erroPrecoNegativo, erroTempoNegativo))
        );
    }

    private CadastrarServicoRequest cadastrarRequest() {
        CadastrarServicoRequest request = new CadastrarServicoRequest();
        request.setNome("Troca de óleo");
        request.setDescricao("Troca completa de óleo do motor");
        request.setPreco(new BigDecimal("150.00"));
        request.setTempoEstimadoHoras(2);
        return request;
    }

    private AlterarServicoRequest alterarRequest() {
        AlterarServicoRequest request = new AlterarServicoRequest();
        request.setNome("Troca de óleo premium");
        request.setDescricao("Óleo sintético");
        request.setPreco(new BigDecimal("250.00"));
        request.setTempoEstimadoHoras(3);
        return request;
    }
}
