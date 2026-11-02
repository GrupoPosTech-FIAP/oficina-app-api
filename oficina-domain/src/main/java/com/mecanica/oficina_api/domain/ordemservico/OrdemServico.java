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

    // EM_DIAGNOSTICO | AGUARDANDO_APROVACAO → EM_EXECUCAO
    public void iniciarExecucao() {
        if (status != OrdemServicoStatus.EM_DIAGNOSTICO && status != OrdemServicoStatus.AGUARDANDO_APROVACAO)
            throw new IllegalStateException("OS deve estar EM_DIAGNOSTICO ou AGUARDANDO_APROVACAO para iniciar execução");
        this.status = OrdemServicoStatus.EM_EXECUCAO;
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

    public String getId() { return id; }
    public String getVeiculoId() { return veiculoId; }
    public String getClienteId() { return clienteId; }
    public OrdemServicoStatus getStatus() { return status; }
    public BigDecimal getValorFinal() { return valorFinal; }
    public LocalDateTime getDataFinal() { return dataFinal; }
}
