package com.mecanica.oficina_api.application.ordemservico.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.mecanica.oficina_api.application.ordemservico.gateway.OrdemServicoGateway;
import com.mecanica.oficina_api.application.ordemservico.output.MinhaOrdemServicoOutput;
import com.mecanica.oficina_api.domain.ordemservico.OrdemServicoStatus;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListarPorUsuarioOrdemServicoUseCaseTest {

    @Mock
    private OrdemServicoGateway ordemServicoGateway;

    @InjectMocks
    private ListarPorUsuarioOrdemServicoUseCase useCase;

    private MinhaOrdemServicoOutput osRecebida;
    private MinhaOrdemServicoOutput osEmExecucao;

    @BeforeEach
    void setUp() {
        osRecebida = new MinhaOrdemServicoOutput("os-1", "RECEBIDA", LocalDateTime.now(),
            new MinhaOrdemServicoOutput.VeiculoResumo("veic-1", "ABC1D23", "Fiat", "Uno", 2020, "Prata"));
        osEmExecucao = new MinhaOrdemServicoOutput("os-2", "EM_EXECUCAO", LocalDateTime.now(),
            new MinhaOrdemServicoOutput.VeiculoResumo("veic-2", "XYZ9A88", "VW", "Gol", 2021, "Preto"));
    }

    @Test
    void deveRetornarTodasAsOrdens_quandoSemFiltros() {
        when(ordemServicoGateway.buscarPorCliente("cli-1")).thenReturn(List.of(osRecebida, osEmExecucao));

        List<MinhaOrdemServicoOutput> resultado = useCase.executar("cli-1", null, null);

        assertThat(resultado).hasSize(2);
    }

    @Test
    void deveFiltrarPorStatus() {
        when(ordemServicoGateway.buscarPorCliente("cli-1")).thenReturn(List.of(osRecebida, osEmExecucao));

        List<MinhaOrdemServicoOutput> resultado = useCase.executar("cli-1", OrdemServicoStatus.EM_EXECUCAO, null);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).id()).isEqualTo("os-2");
    }

    @Test
    void deveFiltrarPorPlaca() {
        when(ordemServicoGateway.buscarPorCliente("cli-1")).thenReturn(List.of(osRecebida, osEmExecucao));

        List<MinhaOrdemServicoOutput> resultado = useCase.executar("cli-1", null, "ABC1D23");

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).id()).isEqualTo("os-1");
    }

    @Test
    void deveIgnorarFiltroDePlacaEmBranco() {
        when(ordemServicoGateway.buscarPorCliente("cli-1")).thenReturn(List.of(osRecebida, osEmExecucao));

        List<MinhaOrdemServicoOutput> resultado = useCase.executar("cli-1", null, "  ");

        assertThat(resultado).hasSize(2);
    }

    @Test
    void deveRetornarListaVazia_quandoClienteSemOrdens() {
        when(ordemServicoGateway.buscarPorCliente("cli-1")).thenReturn(List.of());

        List<MinhaOrdemServicoOutput> resultado = useCase.executar("cli-1", null, null);

        assertThat(resultado).isEmpty();
    }

    @Test
    void deveIgnorarOrdensComStatusFinalizadaOuEntregue() {
        MinhaOrdemServicoOutput osFinalizada = new MinhaOrdemServicoOutput("os-fin", "FINALIZADA", LocalDateTime.now(),
            new MinhaOrdemServicoOutput.VeiculoResumo("veic-1", "ABC1D23", "Fiat", "Uno", 2020, "Prata"));
        MinhaOrdemServicoOutput osEntregue = new MinhaOrdemServicoOutput("os-ent", "ENTREGUE", LocalDateTime.now(),
            new MinhaOrdemServicoOutput.VeiculoResumo("veic-1", "ABC1D23", "Fiat", "Uno", 2020, "Prata"));

        when(ordemServicoGateway.buscarPorCliente("cli-1")).thenReturn(List.of(osRecebida, osFinalizada, osEmExecucao, osEntregue));

        List<MinhaOrdemServicoOutput> resultado = useCase.executar("cli-1", null, null);

        assertThat(resultado).hasSize(2);
        assertThat(resultado).extracting(MinhaOrdemServicoOutput::id).containsExactly("os-2", "os-1"); // sorted: EM_EXECUCAO then RECEBIDA
    }

    @Test
    void deveOrdenarOrdensPorPrioridadeEDataCriacao() {
        LocalDateTime agora = LocalDateTime.now();
        MinhaOrdemServicoOutput osAguardandoRecente = new MinhaOrdemServicoOutput("os-aguard-1", "AGUARDANDO_APROVACAO", agora.plusMinutes(10),
            new MinhaOrdemServicoOutput.VeiculoResumo("veic-1", "ABC1D23", "Fiat", "Uno", 2020, "Prata"));
        MinhaOrdemServicoOutput osAguardandoAntiga = new MinhaOrdemServicoOutput("os-aguard-2", "AGUARDANDO_APROVACAO", agora,
            new MinhaOrdemServicoOutput.VeiculoResumo("veic-1", "ABC1D23", "Fiat", "Uno", 2020, "Prata"));
        MinhaOrdemServicoOutput osDiagnostico = new MinhaOrdemServicoOutput("os-diag", "EM_DIAGNOSTICO", agora,
            new MinhaOrdemServicoOutput.VeiculoResumo("veic-1", "ABC1D23", "Fiat", "Uno", 2020, "Prata"));

        when(ordemServicoGateway.buscarPorCliente("cli-1")).thenReturn(List.of(osRecebida, osDiagnostico, osAguardandoRecente, osEmExecucao, osAguardandoAntiga));

        List<MinhaOrdemServicoOutput> resultado = useCase.executar("cli-1", null, null);

        // Priority order: EM_EXECUCAO ("os-2"), AGUARDANDO_APROVACAO (older first: "os-aguard-2" then "os-aguard-1"), EM_DIAGNOSTICO ("os-diag"), RECEBIDA ("os-1")
        assertThat(resultado).extracting(MinhaOrdemServicoOutput::id)
            .containsExactly("os-2", "os-aguard-2", "os-aguard-1", "os-diag", "os-1");
    }
}
