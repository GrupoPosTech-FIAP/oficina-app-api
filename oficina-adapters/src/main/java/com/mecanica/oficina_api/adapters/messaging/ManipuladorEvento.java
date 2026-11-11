package com.mecanica.oficina_api.adapters.messaging;

/**
 * Adaptador de entrada para um tipo de evento — o equivalente de um controller para a fila.
 * Cada implementação traduz o payload e chama o caso de uso correspondente.
 *
 * <p>Roda dentro da transação do {@link ProcessadorEventos}: tudo que o manipulador gravar
 * (incluindo novos eventos na outbox) é confirmado junto com o registro de idempotência.
 * Lançar exceção desfaz tudo e devolve a mensagem para a fila.
 */
public interface ManipuladorEvento {

    String eventType();

    void tratar(EventoEnvelope evento);
}
