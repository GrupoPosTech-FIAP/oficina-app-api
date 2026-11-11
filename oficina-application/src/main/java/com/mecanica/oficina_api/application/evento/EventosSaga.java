package com.mecanica.oficina_api.application.evento;

/** Nomes dos eventos da Saga (catálogo da ADR 005) publicados ou consumidos pelo serviço de OS. */
public final class EventosSaga {

    // Publicados pela OS
    public static final String DIAGNOSTICO_CONCLUIDO = "DiagnosticoConcluido";
    public static final String EXECUCAO_INICIADA = "ExecucaoIniciada";
    public static final String EXECUCAO_CONCLUIDA = "ExecucaoConcluida";
    public static final String EXECUCAO_FALHOU = "ExecucaoFalhou";

    // billing
    public static final String ORCAMENTO_GERADO = "OrcamentoGerado";
    public static final String ORCAMENTO_APROVADO = "OrcamentoAprovado";
    public static final String ORCAMENTO_NEGADO = "OrcamentoNegado";
    public static final String ORCAMENTO_EXPIRADO = "OrcamentoExpirado";
    public static final String COBRANCA_CRIADA = "CobrancaCriada";
    public static final String PAGAMENTO_CONFIRMADO = "PagamentoConfirmado";
    public static final String PAGAMENTO_RECUSADO = "PagamentoRecusado";
    public static final String PAGAMENTO_EXPIRADO = "PagamentoExpirado";
    public static final String PAGAMENTO_ESTORNADO = "PagamentoEstornado";

    // inventory
    public static final String ESTOQUE_BAIXADO = "EstoqueBaixado";
    public static final String ESTOQUE_INSUFICIENTE = "EstoqueInsuficiente";
    public static final String ESTOQUE_ESTORNADO = "EstoqueEstornado";

    private EventosSaga() {
    }
}
