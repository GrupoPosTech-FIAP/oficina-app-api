package com.mecanica.oficina_api.adapters.web.presenter;

import com.mecanica.oficina_api.adapters.web.dto.response.ServicoResponse;
import com.mecanica.oficina_api.domain.servico.Servico;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ServicoPresenterTest {

    private ServicoPresenter presenter;

    @BeforeEach
    void setUp() {
        presenter = new ServicoPresenter();
    }

    @Test
    void deveApresentarServicoComTodosOsCampos() {
        Servico servico = Servico.reconstituir(
                "servico-1", "Troca de óleo", "Troca completa de óleo do motor",
                new BigDecimal("150.00"), Duration.ofHours(2), true);

        ServicoResponse response = presenter.apresentar(servico);

        assertThat(response.getId()).isEqualTo("servico-1");
        assertThat(response.getNome()).isEqualTo("Troca de óleo");
        assertThat(response.getDescricao()).isEqualTo("Troca completa de óleo do motor");
        assertThat(response.getPreco()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(response.getTempoEstimadoHoras()).isEqualTo(2);
        assertThat(response.isAtivo()).isTrue();
    }

    @Test
    void deveConverterDurationEmHorasInteiras() {
        Servico servico = Servico.reconstituir(
                "servico-2", "Revisão", "Revisão geral",
                new BigDecimal("500.00"), Duration.ofMinutes(150), false);

        ServicoResponse response = presenter.apresentar(servico);

        // 150 minutos = 2h30 -> toHours() trunca para 2
        assertThat(response.getTempoEstimadoHoras()).isEqualTo(2);
        assertThat(response.isAtivo()).isFalse();
    }

    @Test
    void deveApresentarListaDeServicos() {
        Servico s1 = Servico.reconstituir("servico-1", "Troca de óleo", "Desc 1",
                new BigDecimal("150.00"), Duration.ofHours(2), true);
        Servico s2 = Servico.reconstituir("servico-2", "Alinhamento", "Desc 2",
                new BigDecimal("80.00"), Duration.ofHours(1), true);

        List<ServicoResponse> responses = presenter.apresentar(List.of(s1, s2));

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getId()).isEqualTo("servico-1");
        assertThat(responses.get(1).getId()).isEqualTo("servico-2");
        assertThat(responses.get(1).getNome()).isEqualTo("Alinhamento");
    }

    @Test
    void deveApresentarListaVazia() {
        List<ServicoResponse> responses = presenter.apresentar(List.<Servico>of());

        assertThat(responses).isEmpty();
    }
}
