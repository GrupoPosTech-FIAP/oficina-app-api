package com.mecanica.oficina_api.adapters.messaging;

import java.util.Set;

/**
 * Adaptador de entrada para um ou mais tipos de evento — o equivalente de um controller para a fila.
 * Cada implementação traduz o payload e chama o caso de uso correspondente.
 *
 * <p>Roda dentro da transação do {@link ProcessadorEventos}: tudo que o manipulador gravar
 * (incluindo novos eventos na outbox) é confirmado junto com o registro de idempotência.
 * Lançar exceção desfaz tudo e devolve a mensagem para a fila.
 */
public interface ManipuladorEvento {

    /** Tipos de evento (catálogo da ADR 005) tratados por este manipulador. */
    Set<String> eventTypes();

    void tratar(EventoEnvelope evento);
}
