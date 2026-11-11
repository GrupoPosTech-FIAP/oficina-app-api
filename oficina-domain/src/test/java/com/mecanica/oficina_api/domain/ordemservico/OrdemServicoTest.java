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

    /** Caminho feliz da Saga até a OS ficar liberada para execução. */
    private static OrdemServico aguardandoExecucao() {
        OrdemServico os = OrdemServico.criar("veiculo-1", "cliente-1");
        os.iniciarDiagnostico();
        os.concluirDiagnostico();
        os.aguardarAprovacao();
        os.aguardarPagamento();
        os.aguardarExecucao();
        return os;
    }

    private static OrdemServico em(OrdemServicoStatus status) {
        return OrdemServico.reconstituir("os-1", "veiculo-1", "cliente-1", status, null, null);
    }

    @Test
    void devePercorrerAsEtapasDaSagaAteIniciarExecucao() {
        OrdemServico os = aguardandoExecucao();
        assertThat(os.getStatus()).isEqualTo(OrdemServicoStatus.AGUARDANDO_EXECUCAO);

        os.iniciarExecucao();

        assertThat(os.getStatus()).isEqualTo(OrdemServicoStatus.EM_EXECUCAO);
    }

    @Test
    void naoDeveIniciarExecucaoAntesDoPagamento() {
        assertThatThrownBy(em(OrdemServicoStatus.EM_DIAGNOSTICO)::iniciarExecucao)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AGUARDANDO_EXECUCAO");
        assertThatThrownBy(em(OrdemServicoStatus.AGUARDANDO_PAGAMENTO)::iniciarExecucao)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void transicoesDaSagaExigemOStatusAnterior() {
        assertThatThrownBy(em(OrdemServicoStatus.RECEBIDA)::concluirDiagnostico).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(em(OrdemServicoStatus.RECEBIDA)::aguardarAprovacao).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(em(OrdemServicoStatus.EM_DIAGNOSTICO)::aguardarPagamento).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(em(OrdemServicoStatus.AGUARDANDO_APROVACAO)::aguardarExecucao).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void deveCancelarAguardandoAprovacaoOuPagamento() {
        OrdemServico aprovacao = em(OrdemServicoStatus.AGUARDANDO_APROVACAO);
        OrdemServico pagamento = em(OrdemServicoStatus.AGUARDANDO_PAGAMENTO);

        aprovacao.cancelar();
        pagamento.cancelar();

        assertThat(aprovacao.getStatus()).isEqualTo(OrdemServicoStatus.CANCELADA);
        assertThat(pagamento.getStatus()).isEqualTo(OrdemServicoStatus.CANCELADA);
    }

    @Test
    void naoDeveCancelarDepoisDoPagamentoConfirmado() {
        assertThatThrownBy(em(OrdemServicoStatus.AGUARDANDO_EXECUCAO)::cancelar).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(em(OrdemServicoStatus.CANCELADA)::cancelar).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void falhaNaExecucaoCancelaAOs() {
        OrdemServico os = em(OrdemServicoStatus.EM_EXECUCAO);

        os.falharExecucao();

        assertThat(os.getStatus()).isEqualTo(OrdemServicoStatus.CANCELADA);
        assertThatThrownBy(em(OrdemServicoStatus.FINALIZADA)::falharExecucao).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void deveLancarExcecaoAoIniciarExecucaoEmOsRecebida() {
        OrdemServico os = OrdemServico.criar("veiculo-1", "cliente-1");

        assertThatThrownBy(os::iniciarExecucao).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void deveFinalizarOsEmExecucaoComDataEValorFinalOpcional() {
        OrdemServico os = aguardandoExecucao();
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
        OrdemServico os = aguardandoExecucao();
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
