package com.mecanica.oficina_api.adapters.web.presenter;

import com.mecanica.oficina_api.adapters.web.dto.response.MinhaOrdemServicoResponse;
import com.mecanica.oficina_api.application.ordemservico.output.MinhaOrdemServicoOutput;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MinhaOrdemServicoPresenterTest {

    private final MinhaOrdemServicoPresenter presenter = new MinhaOrdemServicoPresenter();

    @Test
    void deveApresentar_comVeiculoNulo() {
        MinhaOrdemServicoOutput output = new MinhaOrdemServicoOutput(
                "os-1", "RECEBIDA", LocalDateTime.now(), null);

        MinhaOrdemServicoResponse response = presenter.apresentar(output);

        assertThat(response.id()).isEqualTo("os-1");
        assertThat(response.status()).isEqualTo("RECEBIDA");
        assertThat(response.veiculo()).isNull();
    }

    @Test
    void deveApresentar_comVeiculoPreenchido() {
        MinhaOrdemServicoOutput.VeiculoResumo veiculo = new MinhaOrdemServicoOutput.VeiculoResumo(
                "veiculo-1", "ABC1D23", "Ford", "Ka", 2020, "Prata");
        MinhaOrdemServicoOutput output = new MinhaOrdemServicoOutput(
                "os-1", "EM_DIAGNOSTICO", LocalDateTime.now(), veiculo);

        MinhaOrdemServicoResponse response = presenter.apresentar(output);

        assertThat(response.id()).isEqualTo("os-1");
        assertThat(response.status()).isEqualTo("EM_DIAGNOSTICO");
        assertThat(response.veiculo()).isNotNull();
        assertThat(response.veiculo().id()).isEqualTo("veiculo-1");
        assertThat(response.veiculo().placa()).isEqualTo("ABC1D23");
        assertThat(response.veiculo().marca()).isEqualTo("Ford");
        assertThat(response.veiculo().modelo()).isEqualTo("Ka");
        assertThat(response.veiculo().ano()).isEqualTo(2020);
        assertThat(response.veiculo().cor()).isEqualTo("Prata");
    }

    @Test
    void deveApresentarLista() {
        MinhaOrdemServicoOutput output = new MinhaOrdemServicoOutput(
                "os-1", "RECEBIDA", LocalDateTime.now(), null);

        List<MinhaOrdemServicoResponse> responses = presenter.apresentar(List.of(output));

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).id()).isEqualTo("os-1");
    }
}
