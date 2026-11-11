package com.mecanica.oficina_api.application.evento.gateway;

import java.util.Map;

/**
 * Porta de saída para publicar eventos de integração da Saga (ADR 005).
 *
 * <p>A implementação grava o evento na outbox dentro da transação corrente; a entrega ao
 * broker acontece depois, de forma assíncrona. Por isso o evento só "existe" se a mudança
 * de estado que o originou for confirmada.
 */
public interface PublicadorEventosGateway {

    /**
     * @param eventType nome do evento do catálogo da ADR 005 (ex.: {@code DiagnosticoConcluido})
     * @param sagaId    identificador da Saga — o id da ordem de serviço
     * @param payload   dados do evento, serializados como JSON
     */
    void publicar(String eventType, String sagaId, Map<String, Object> payload);
}
