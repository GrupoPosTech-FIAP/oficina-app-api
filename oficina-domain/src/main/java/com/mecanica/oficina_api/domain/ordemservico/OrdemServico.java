package com.mecanica.oficina_api.domain.ordemservico;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

public class OrdemServico {

    private String id;
    private final String veiculoId;
    private final String clienteId;
    private OrdemServicoStatus status;
    private BigDecimal valorFinal;
    private LocalDateTime dataFinal;

    protected OrdemServico(String veiculoId, String clienteId) {
        this.veiculoId = Objects.requireNonNull(veiculoId, "Veículo é obrigatório");
        this.clienteId = Objects.requireNonNull(clienteId, "Cliente é obrigatório");
        this.status = OrdemServicoStatus.RECEBIDA;
    }

    public static OrdemServico criar(String veiculoId, String clienteId) {
        return new OrdemServico(veiculoId, clienteId);
    }

    public static OrdemServico reconstituir(String id, String veiculoId, String clienteId,
                                            OrdemServicoStatus status,
                                            BigDecimal valorFinal, LocalDateTime dataFinal) {
        OrdemServico os = new OrdemServico(veiculoId, clienteId);
        os.id = id;
        os.status = status;
        os.valorFinal = valorFinal;
        os.dataFinal = dataFinal;
        return os;
    }

    // RECEBIDA → EM_DIAGNOSTICO
    public void iniciarDiagnostico() {
        if (status != OrdemServicoStatus.RECEBIDA)
            throw new IllegalStateException("OS deve estar RECEBIDA para iniciar diagnóstico");
        this.status = OrdemServicoStatus.EM_DIAGNOSTICO;
    }

    // Fim do diagnóstico: a OS continua EM_DIAGNOSTICO até o billing gerar o orçamento (Saga, ADR 005)
    public void concluirDiagnostico() {
        exigir(OrdemServicoStatus.EM_DIAGNOSTICO, "concluir o diagnóstico");
    }

    // EM_DIAGNOSTICO → AGUARDANDO_APROVACAO (OrcamentoGerado)
    public void aguardarAprovacao() {
        exigir(OrdemServicoStatus.EM_DIAGNOSTICO, "aguardar aprovação do orçamento");
        this.status = OrdemServicoStatus.AGUARDANDO_APROVACAO;
    }

    // AGUARDANDO_APROVACAO → AGUARDANDO_PAGAMENTO (CobrancaCriada)
    public void aguardarPagamento() {
        exigir(OrdemServicoStatus.AGUARDANDO_APROVACAO, "aguardar pagamento");
        this.status = OrdemServicoStatus.AGUARDANDO_PAGAMENTO;
    }

    // AGUARDANDO_PAGAMENTO → AGUARDANDO_EXECUCAO (PagamentoConfirmado)
    public void aguardarExecucao() {
        exigir(OrdemServicoStatus.AGUARDANDO_PAGAMENTO, "aguardar execução");
        this.status = OrdemServicoStatus.AGUARDANDO_EXECUCAO;
    }

    // AGUARDANDO_EXECUCAO → EM_EXECUCAO: só depois do pagamento confirmado
    public void iniciarExecucao() {
        exigir(OrdemServicoStatus.AGUARDANDO_EXECUCAO, "iniciar execução");
        this.status = OrdemServicoStatus.EM_EXECUCAO;
    }

    // AGUARDANDO_APROVACAO | AGUARDANDO_PAGAMENTO → CANCELADA
    // (orçamento negado/expirado, estoque insuficiente, pagamento recusado/expirado)
    public void cancelar() {
        if (status != OrdemServicoStatus.AGUARDANDO_APROVACAO && status != OrdemServicoStatus.AGUARDANDO_PAGAMENTO)
            throw new IllegalStateException("OS deve estar AGUARDANDO_APROVACAO ou AGUARDANDO_PAGAMENTO para ser cancelada, mas está " + status);
        this.status = OrdemServicoStatus.CANCELADA;
    }

    // EM_EXECUCAO → CANCELADA: reparo inviável; estoque e pagamento são estornados pela Saga
    public void falharExecucao() {
        exigir(OrdemServicoStatus.EM_EXECUCAO, "registrar falha na execução");
        this.status = OrdemServicoStatus.CANCELADA;
    }

    // Execução concluída → FINALIZADA, registra data (valorFinal permanece opcional)
    public void finalizar() {
        if (status != OrdemServicoStatus.EM_EXECUCAO)
            throw new IllegalStateException("OS deve estar EM_EXECUCAO para ser finalizada");
        this.dataFinal = LocalDateTime.now();
        this.status = OrdemServicoStatus.FINALIZADA;
    }

    // Cliente retira o veículo → ENTREGUE
    public void entregar() {
        if (status != OrdemServicoStatus.FINALIZADA)
            throw new IllegalStateException("OS deve estar FINALIZADA para registrar entrega");
        this.status = OrdemServicoStatus.ENTREGUE;
    }

    private void exigir(OrdemServicoStatus esperado, String acao) {
        if (status != esperado)
            throw new IllegalStateException("OS deve estar " + esperado + " para " + acao + ", mas está " + status);
    }

    public String getId() { return id; }
    public String getVeiculoId() { return veiculoId; }
    public String getClienteId() { return clienteId; }
    public OrdemServicoStatus getStatus() { return status; }
    public BigDecimal getValorFinal() { return valorFinal; }
    public LocalDateTime getDataFinal() { return dataFinal; }
}
