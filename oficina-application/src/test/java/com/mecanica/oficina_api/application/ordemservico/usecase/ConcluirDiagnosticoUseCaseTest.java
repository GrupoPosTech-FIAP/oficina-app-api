package com.mecanica.oficina_api.application.ordemservico.usecase;

import static com.mecanica.oficina_api.application.ordemservico.OrdemServicoFixture.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.mecanica.oficina_api.application.ordemservico.RegistroEventosOrdemServico;
import com.mecanica.oficina_api.application.ordemservico.gateway.OrdemServicoGateway;
import com.mecanica.oficina_api.application.ordemservico.input.ConcluirDiagnosticoInput;
import com.mecanica.oficina_api.application.servico.gateway.ServicoGateway;
import com.mecanica.oficina_api.domain.ordemservico.OrdemServico;
import com.mecanica.oficina_api.domain.ordemservico.OrdemServicoStatus;
import com.mecanica.oficina_api.domain.servico.Servico;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ConcluirDiagnosticoUseCaseTest {

    private OrdemServicoGateway ordemServicoGateway;
    private ServicoGateway servicoGateway;
    private RegistroEventosOrdemServico eventos;
    private ConcluirDiagnosticoUseCase useCase;

    @BeforeEach
    void setUp() {
        ordemServicoGateway = mock(OrdemServicoGateway.class);
        servicoGateway = mock(ServicoGateway.class);
        eventos = mock(RegistroEventosOrdemServico.class);
        useCase = new ConcluirDiagnosticoUseCase(ordemServicoGateway, servicoGateway, eventos);
        when(ordemServicoGateway.encontrarOuLancar(OS_ID)).thenReturn(osSemOrcamento(OrdemServicoStatus.EM_DIAGNOSTICO));
    }

    private void servico(boolean ativo) {
        when(servicoGateway.buscarOuFalhar("srv-1")).thenReturn(Servico.reconstituir("srv-1", "Troca de óleo", "Óleo e filtro",
                new BigDecimal("80.00"), Duration.ofHours(1), ativo));
    }

    @Test
    @SuppressWarnings("unchecked")
    void publicaDiagnosticoConcluidoComServicosPrecificadosPeloCatalogo() {
        servico(true);

        OrdemServico os = useCase.executar(OS_ID, new ConcluirDiagnosticoInput(
                List.of(new ConcluirDiagnosticoInput.Servico("srv-1", 2)),
                List.of(new ConcluirDiagnosticoInput.Insumo("ins-1", 4, "Óleo 5W30", new BigDecimal("45.00"))),
                "verificar freios"));

        assertThat(os.getStatus()).isEqualTo(OrdemServicoStatus.EM_DIAGNOSTICO);
        ArgumentCaptor<Map<String, Object>> payload = ArgumentCaptor.forClass(Map.class);
        verify(eventos).publicar(eq("DiagnosticoConcluido"), eq(os), eq(OrdemServicoStatus.EM_DIAGNOSTICO), payload.capture(), isNull());
        assertThat(payload.getValue())
                .containsEntry("ordemServicoId", OS_ID)
                .containsEntry("clienteId", CLIENTE_ID)
                .containsEntry("veiculoId", VEICULO_ID)
                .containsEntry("observacoes", "verificar freios")
                .containsEntry("servicos", List.of(Map.of("servicoId", "srv-1", "quantidade", 2,
                        "descricao", "Troca de óleo", "precoUnitario", new BigDecimal("80.00"))))
                .containsEntry("insumos", List.of(Map.of("insumoId", "ins-1", "quantidade", 4,
                        "descricao", "Óleo 5W30", "precoUnitario", new BigDecimal("45.00"))));
    }

    @Test
    void exigeAoMenosUmItem() {
        assertThatThrownBy(() -> useCase.executar(OS_ID, new ConcluirDiagnosticoInput(null, List.of(), null)))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(eventos);
    }

    @Test
    void rejeitaServicoInativo() {
        servico(false);

        assertThatThrownBy(() -> useCase.executar(OS_ID, new ConcluirDiagnosticoInput(
                List.of(new ConcluirDiagnosticoInput.Servico("srv-1", 1)), List.of(), null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("inativo");
    }

    @Test
    void rejeitaInsumoSemPrecoOuQuantidadeInvalida() {
        assertThatThrownBy(() -> useCase.executar(OS_ID, new ConcluirDiagnosticoInput(List.of(),
                List.of(new ConcluirDiagnosticoInput.Insumo("ins-1", 1, "Óleo", null)), null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> useCase.executar(OS_ID, new ConcluirDiagnosticoInput(List.of(),
                List.of(new ConcluirDiagnosticoInput.Insumo(" ", 1, "Óleo", BigDecimal.ONE)), null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> useCase.executar(OS_ID, new ConcluirDiagnosticoInput(
                List.of(new ConcluirDiagnosticoInput.Servico("srv-1", 0)), List.of(), null)))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(eventos);
    }

    @Test
    void exigeOsEmDiagnostico() {
        when(ordemServicoGateway.encontrarOuLancar(OS_ID)).thenReturn(osSemOrcamento(OrdemServicoStatus.RECEBIDA));

        assertThatThrownBy(() -> useCase.executar(OS_ID, new ConcluirDiagnosticoInput(List.of(), List.of(), null)))
                .isInstanceOf(IllegalStateException.class);
    }
}
