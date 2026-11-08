package com.mecanica.oficina_api.application.ordemservico.usecase;

import com.mecanica.oficina_api.application.evento.EventosSaga;
import com.mecanica.oficina_api.application.ordemservico.gateway.HistoricoOrdemServicoGateway;
import com.mecanica.oficina_api.application.ordemservico.gateway.NotificadorClienteGateway;
import com.mecanica.oficina_api.application.ordemservico.gateway.OrdemServicoGateway;
import com.mecanica.oficina_api.application.ordemservico.output.NotificacaoCliente;
import com.mecanica.oficina_api.domain.ordemservico.HistoricoOrdemServico;
import com.mecanica.oficina_api.domain.ordemservico.OrdemServico;
import com.mecanica.oficina_api.domain.ordemservico.OrdemServicoStatus;

import java.time.Clock;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Aplica à OS um evento da Saga vindo de outro serviço (máquina de estados da ADR 005, seção 5) e
 * registra o evento no histórico.
 *
 * <p>Evento que não corresponde a uma transição válida a partir do status atual — fora de ordem ou
 * repetido — é registrado e descartado, sem erro, para não prender a mensagem na fila.
 */
public class ProcessarEventoSagaUseCase {

    public enum Resultado { TRANSICAO_APLICADA, SOMENTE_HISTORICO, TRANSICAO_IGNORADA, ORDEM_SERVICO_INEXISTENTE }

    private static final Map<String, Consumer<OrdemServico>> TRANSICOES = Map.ofEntries(
            Map.entry(EventosSaga.ORCAMENTO_GERADO, OrdemServico::aguardarAprovacao),
            Map.entry(EventosSaga.COBRANCA_CRIADA, OrdemServico::aguardarPagamento),
            Map.entry(EventosSaga.PAGAMENTO_CONFIRMADO, OrdemServico::aguardarExecucao),
            Map.entry(EventosSaga.ORCAMENTO_NEGADO, OrdemServico::cancelar),
            Map.entry(EventosSaga.ORCAMENTO_EXPIRADO, OrdemServico::cancelar),
            Map.entry(EventosSaga.ESTOQUE_INSUFICIENTE, OrdemServico::cancelar),
            Map.entry(EventosSaga.PAGAMENTO_RECUSADO, OrdemServico::cancelar),
            Map.entry(EventosSaga.PAGAMENTO_EXPIRADO, OrdemServico::cancelar),
            // vindos de um futuro oficina-execution-service; hoje a execução é feita aqui mesmo (REST)
            Map.entry(EventosSaga.EXECUCAO_INICIADA, OrdemServico::iniciarExecucao),
            Map.entry(EventosSaga.EXECUCAO_CONCLUIDA, OrdemServico::finalizar),
            Map.entry(EventosSaga.EXECUCAO_FALHOU, OrdemServico::falharExecucao));

    /** Passos de outros serviços que não mudam o status da OS, só entram no histórico. */
    private static final Set<String> SOMENTE_HISTORICO = Set.of(
            EventosSaga.ORCAMENTO_APROVADO,
            EventosSaga.ESTOQUE_BAIXADO,
            EventosSaga.ESTOQUE_ESTORNADO,
            EventosSaga.PAGAMENTO_ESTORNADO);

    public static final Set<String> EVENTOS_CONSUMIDOS;

    static {
        var todos = new HashSet<>(TRANSICOES.keySet());
        todos.addAll(SOMENTE_HISTORICO);
        EVENTOS_CONSUMIDOS = Set.copyOf(todos);
    }

    private final OrdemServicoGateway ordemServicoGateway;
    private final HistoricoOrdemServicoGateway historicoGateway;
    private final NotificadorClienteGateway notificadorClienteGateway;
    private final Clock clock;

    public ProcessarEventoSagaUseCase(OrdemServicoGateway ordemServicoGateway,
                                      HistoricoOrdemServicoGateway historicoGateway,
                                      NotificadorClienteGateway notificadorClienteGateway,
                                      Clock clock) {
        this.ordemServicoGateway = ordemServicoGateway;
        this.historicoGateway = historicoGateway;
        this.notificadorClienteGateway = notificadorClienteGateway;
        this.clock = clock;
    }

    /**
     * @param detalhe payload do evento, guardado no histórico
     */
    public Resultado executar(String evento, String ordemServicoId, String detalhe) {
        if (!EVENTOS_CONSUMIDOS.contains(evento)) {
            throw new IllegalArgumentException("Evento não tratado pela OS: " + evento);
        }

        Optional<OrdemServico> encontrada = ordemServicoGateway.buscar(ordemServicoId);
        if (encontrada.isEmpty()) {
            return Resultado.ORDEM_SERVICO_INEXISTENTE;
        }

        OrdemServico os = encontrada.get();
        OrdemServicoStatus anterior = os.getStatus();
        Resultado resultado = aplicar(evento, os);

        if (resultado == Resultado.TRANSICAO_APLICADA) {
            os = ordemServicoGateway.atualizar(os);
            if (os.getStatus() == OrdemServicoStatus.CANCELADA) {
                notificadorClienteGateway.notificar(NotificacaoCliente.cancelada(os));
            }
        }

        historicoGateway.registrar(new HistoricoOrdemServico(ordemServicoId, evento, HistoricoOrdemServico.Origem.RECEBIDO,
                anterior, os.getStatus(), resultado == Resultado.TRANSICAO_IGNORADA, detalhe, clock.instant()));
        return resultado;
    }

    private static Resultado aplicar(String evento, OrdemServico os) {
        Consumer<OrdemServico> transicao = TRANSICOES.get(evento);
        if (transicao == null) {
            return Resultado.SOMENTE_HISTORICO;
        }
        try {
            transicao.accept(os);
            return Resultado.TRANSICAO_APLICADA;
        } catch (IllegalStateException transicaoInvalida) {
            return Resultado.TRANSICAO_IGNORADA;
        }
    }
}
