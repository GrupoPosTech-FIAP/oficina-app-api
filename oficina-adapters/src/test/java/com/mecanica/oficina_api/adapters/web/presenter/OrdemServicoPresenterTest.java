package com.mecanica.oficina_api.adapters.web.presenter;

import com.mecanica.oficina_api.adapters.web.dto.response.OrdemServicoResponse;
import com.mecanica.oficina_api.domain.ordemservico.OrdemServico;
import com.mecanica.oficina_api.domain.ordemservico.OrdemServicoStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrdemServicoPresenterTest {

    private final OrdemServicoPresenter presenter = new OrdemServicoPresenter();

    @Test
    void deveApresentarOrdemServico() {
        OrdemServico os = OrdemServico.reconstituir("os-1", "veiculo-1", "cliente-1",
                OrdemServicoStatus.RECEBIDA, null, null);

        OrdemServicoResponse response = presenter.apresentar(os);

        assertThat(response.getId()).isEqualTo("os-1");
        assertThat(response.getVeiculoId()).isEqualTo("veiculo-1");
        assertThat(response.getClienteId()).isEqualTo("cliente-1");
        assertThat(response.getStatus()).isEqualTo("RECEBIDA");
        assertThat(response.getValorFinal()).isNull();
        assertThat(response.getDataFinal()).isNull();
    }

    @Test
    void deveApresentarOrdemServicoFinalizadaComValorEData() {
        LocalDateTime fim = LocalDateTime.of(2026, 5, 10, 15, 30);
        OrdemServico os = OrdemServico.reconstituir("os-1", "veiculo-1", "cliente-1",
                OrdemServicoStatus.FINALIZADA, new BigDecimal("250.00"), fim);

        OrdemServicoResponse response = presenter.apresentar(os);

        assertThat(response.getStatus()).isEqualTo("FINALIZADA");
        assertThat(response.getValorFinal()).isEqualByComparingTo("250.00");
        assertThat(response.getDataFinal()).isEqualTo(fim);
    }

    @Test
    void deveApresentarListaDeOrdens() {
        OrdemServico os = OrdemServico.reconstituir("os-1", "veiculo-1", "cliente-1",
                OrdemServicoStatus.RECEBIDA, null, null);

        List<OrdemServicoResponse> responses = presenter.apresentar(List.of(os));

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getId()).isEqualTo("os-1");
    }
}
