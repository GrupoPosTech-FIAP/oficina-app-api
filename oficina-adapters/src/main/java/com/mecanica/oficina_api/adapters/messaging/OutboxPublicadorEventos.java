package com.mecanica.oficina_api.adapters.messaging;

import com.mecanica.oficina_api.adapters.persistence.OutboxEventoJpaEntity;
import com.mecanica.oficina_api.adapters.persistence.repository.OutboxEventoSpringDataRepository;
import com.mecanica.oficina_api.application.evento.gateway.PublicadorEventosGateway;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * Grava o evento na outbox. Exige uma transação aberta pelo chamador: o evento precisa ser
 * confirmado junto com a mudança de estado que o originou, nunca isolado.
 */
@Component
public class OutboxPublicadorEventos implements PublicadorEventosGateway {

    private final OutboxEventoSpringDataRepository repository;
    private final JsonMapper jsonMapper;
    private final MensageriaProperties properties;
    private final Clock clock;

    public OutboxPublicadorEventos(OutboxEventoSpringDataRepository repository,
                                   JsonMapper jsonMapper,
                                   MensageriaProperties properties,
                                   Clock clock) {
        this.repository = repository;
        this.jsonMapper = jsonMapper;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publicar(String eventType, String sagaId, Map<String, Object> payload) {
        // Microssegundos: é a precisão do timestamp no banco, então envelope e outbox registram o mesmo instante
        Instant agora = clock.instant().truncatedTo(ChronoUnit.MICROS);
        EventoEnvelope envelope = new EventoEnvelope(
                UUID.randomUUID().toString(),
                eventType,
                EventoEnvelope.VERSAO_ATUAL,
                agora,
                properties.produtor(),
                sagaId,
                jsonMapper.valueToTree(payload == null ? Map.of() : payload));

        OutboxEventoJpaEntity entity = new OutboxEventoJpaEntity();
        entity.setEventId(envelope.eventId());
        entity.setEventType(eventType);
        entity.setSagaId(sagaId);
        entity.setEnvelope(jsonMapper.writeValueAsString(envelope));
        entity.setCriadoEm(agora);
        repository.save(entity);
    }
}
