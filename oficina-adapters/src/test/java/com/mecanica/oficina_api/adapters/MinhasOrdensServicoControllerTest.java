package com.mecanica.oficina_api.adapters;

import com.mecanica.oficina_api.adapters.security.UsuarioPrincipal;
import com.mecanica.oficina_api.adapters.web.MinhasOrdensServicoController;
import com.mecanica.oficina_api.adapters.web.presenter.MinhaOrdemServicoPresenter;
import com.mecanica.oficina_api.application.ordemservico.output.MinhaOrdemServicoOutput;
import com.mecanica.oficina_api.application.ordemservico.usecase.ListarPorUsuarioOrdemServicoUseCase;
import com.mecanica.oficina_api.domain.ordemservico.OrdemServicoStatus;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MinhasOrdensServicoControllerTest {

    private static final String CLIENTE_ID = "cliente-1";

    private MockMvc mockMvc;

    @Mock
    private ListarPorUsuarioOrdemServicoUseCase listarPorUsuarioOrdemServicoUseCase;

    @BeforeEach
    void setUp() {
        MinhasOrdensServicoController controller = new MinhasOrdensServicoController(
                listarPorUsuarioOrdemServicoUseCase, new MinhaOrdemServicoPresenter());
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();

        UsuarioPrincipal principal = new UsuarioPrincipal(
                "usr-1", "cliente@email.com", "x", CLIENTE_ID,
                List.of(new SimpleGrantedAuthority("ROLE_CLIENTE")));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deveListarOrdensDoClienteLogado_semFiltros() throws Exception {
        MinhaOrdemServicoOutput output = new MinhaOrdemServicoOutput(
                "os-1", "RECEBIDA", LocalDateTime.now(),
                new MinhaOrdemServicoOutput.VeiculoResumo("v-1", "ABC1D23", "Fiat", "Uno", 2020, "Prata"));
        when(listarPorUsuarioOrdemServicoUseCase.executar(eq(CLIENTE_ID), isNull(), isNull()))
                .thenReturn(List.of(output));

        mockMvc.perform(get("/cliente/minhas-os"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("os-1"))
                .andExpect(jsonPath("$[0].status").value("RECEBIDA"))
                .andExpect(jsonPath("$[0].veiculo.placa").value("ABC1D23"));
    }

    @Test
    void deveAplicarFiltrosDeStatusEPlaca() throws Exception {
        when(listarPorUsuarioOrdemServicoUseCase.executar(CLIENTE_ID, OrdemServicoStatus.FINALIZADA, "ABC1D23"))
                .thenReturn(List.of());

        mockMvc.perform(get("/cliente/minhas-os")
                        .param("status", "FINALIZADA")
                        .param("placa", "ABC1D23"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void deveRetornarListaVazia_quandoClienteSemOrdens() throws Exception {
        when(listarPorUsuarioOrdemServicoUseCase.executar(eq(CLIENTE_ID), isNull(), isNull()))
                .thenReturn(List.of());

        mockMvc.perform(get("/cliente/minhas-os"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
