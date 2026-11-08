package com.mecanica.oficina_api.application.ordemservico.output;

/** Texto estável para filtro em log (ex.: {@code tipo_evento=envio_orcamento}). */
public enum TipoNotificacaoCliente {
    RECEBIDA,
    EM_DIAGNOSTICO,
    ENVIO_ORCAMENTO,
    EM_EXECUCAO,
    FINALIZACAO_OS,
    ENTREGUE,
    CANCELADA;

    public String chaveLog() {
        return switch (this) {
            case RECEBIDA -> "inicializacao_os";
            case EM_DIAGNOSTICO -> "em_diagnostico";
            case ENVIO_ORCAMENTO -> "envio_orcamento";
            case EM_EXECUCAO -> "execucao_os";
            case FINALIZACAO_OS -> "finalizacao_os";
            case ENTREGUE -> "entraga_veiculo";
            case CANCELADA -> "cancelamento_os";
        };
    }
}
