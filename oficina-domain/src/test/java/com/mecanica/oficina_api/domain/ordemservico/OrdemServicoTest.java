package com.mecanica.oficina_api.domain.ordemservico;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrdemServicoTest {

    @Test
    void deveCriarOrdemServicoComoRecebida() {
        OrdemServico os = OrdemServico.criar("veiculo-1", "cliente-1");

        assertThat(os.getVeiculoId()).isEqualTo("veiculo-1");
        assertThat(os.getClienteId()).isEqualTo("cliente-1");
        assertThat(os.getStatus()).isEqualTo(OrdemServicoStatus.RECEBIDA);
        assertThat(os.getValorFinal()).isNull();
        assertThat(os.getDataFinal()).isNull();
    }

    @Test
    void deveLancarExcecaoQuandoVeiculoIdForNulo() {
        assertThatThrownBy(() -> OrdemServico.criar(null, "cliente-1"))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void deveLancarExcecaoQuandoClienteIdForNulo() {
        assertThatThrownBy(() -> OrdemServico.criar("veiculo-1", null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void deveIniciarDiagnosticoDeOsRecebida() {
        OrdemServico os = OrdemServico.criar("veiculo-1", "cliente-1");
        os.iniciarDiagnostico();

        assertThat(os.getStatus()).isEqualTo(OrdemServicoStatus.EM_DIAGNOSTICO);
    }

    @Test
    void deveLancarExcecaoAoIniciarDiagnosticoEmOsNaoRecebida() {
        OrdemServico os = OrdemServico.criar("veiculo-1", "cliente-1");
        os.iniciarDiagnostico();

        assertThatThrownBy(os::iniciarDiagnostico).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void deveIniciarExecucaoAPartirDeEmDiagnostico() {
        OrdemServico os = OrdemServico.criar("veiculo-1", "cliente-1");
        os.iniciarDiagnostico();
        os.iniciarExecucao();

        assertThat(os.getStatus()).isEqualTo(OrdemServicoStatus.EM_EXECUCAO);
    }

    @Test
    void deveIniciarExecucaoAPartirDeAguardandoAprovacao() {
        OrdemServico os = OrdemServico.reconstituir("os-1", "veiculo-1", "cliente-1",
                OrdemServicoStatus.AGUARDANDO_APROVACAO, null, null);
        os.iniciarExecucao();

        assertThat(os.getStatus()).isEqualTo(OrdemServicoStatus.EM_EXECUCAO);
    }

    @Test
    void deveLancarExcecaoAoIniciarExecucaoEmOsRecebida() {
        OrdemServico os = OrdemServico.criar("veiculo-1", "cliente-1");

        assertThatThrownBy(os::iniciarExecucao).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void deveFinalizarOsEmExecucaoComDataEValorFinalOpcional() {
        OrdemServico os = OrdemServico.criar("veiculo-1", "cliente-1");
        os.iniciarDiagnostico();
        os.iniciarExecucao();
        os.finalizar();

        assertThat(os.getStatus()).isEqualTo(OrdemServicoStatus.FINALIZADA);
        assertThat(os.getValorFinal()).isNull();
        assertThat(os.getDataFinal()).isNotNull();
    }

    @Test
    void devePreservarValorFinalQuandoReconstituida() {
        LocalDateTime agora = LocalDateTime.now();
        OrdemServico os = OrdemServico.reconstituir("os-1", "veiculo-1", "cliente-1",
                OrdemServicoStatus.FINALIZADA, BigDecimal.valueOf(250.00), agora);

        assertThat(os.getId()).isEqualTo("os-1");
        assertThat(os.getValorFinal()).isEqualByComparingTo(BigDecimal.valueOf(250.00));
        assertThat(os.getDataFinal()).isEqualTo(agora);
    }

    @Test
    void deveLancarExcecaoAoFinalizarOsNaoEmExecucao() {
        OrdemServico os = OrdemServico.criar("veiculo-1", "cliente-1");
        assertThatThrownBy(os::finalizar).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void deveEntregarVeiculoDeOsFinalizada() {
        OrdemServico os = OrdemServico.criar("veiculo-1", "cliente-1");
        os.iniciarDiagnostico();
        os.iniciarExecucao();
        os.finalizar();
        os.entregar();

        assertThat(os.getStatus()).isEqualTo(OrdemServicoStatus.ENTREGUE);
    }

    @Test
    void deveLancarExcecaoAoEntregarVeiculoDeOsNaoFinalizada() {
        OrdemServico os = OrdemServico.criar("veiculo-1", "cliente-1");
        assertThatThrownBy(os::entregar).isInstanceOf(IllegalStateException.class);
    }
}
