package com.mecanica.oficina_api.adapters.messaging;

import com.mecanica.oficina_api.adapters.persistence.OutboxEventoJpaEntity;
import com.mecanica.oficina_api.adapters.persistence.repository.OutboxEventoSpringDataRepository;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;

/**
 * Lê os eventos pendentes da outbox e publica no tópico SNS do serviço, em ordem de criação.
 * Falhas não interrompem o lote: o evento fica pendente e é retentado no próximo ciclo.
 * Entrega é at-least-once — os consumidores deduplicam pelo {@code eventId}.
 */
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);
    static final String ATRIBUTO_EVENT_TYPE = "eventType";

    private final OutboxEventoSpringDataRepository repository;
    private final SnsClient snsClient;
    private final MensageriaProperties properties;
    private final Clock clock;

    public OutboxRelay(OutboxEventoSpringDataRepository repository,
                       SnsClient snsClient,
                       MensageriaProperties properties,
                       Clock clock) {
        this.repository = repository;
        this.snsClient = snsClient;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${mensageria.intervalo-outbox-ms:1000}")
    public void publicarPendentes() {
        List<OutboxEventoJpaEntity> pendentes =
                repository.findByPublicadoEmIsNullOrderByCriadoEmAsc(PageRequest.of(0, properties.loteOutbox()));
        pendentes.forEach(this::publicar);
    }

    private void publicar(OutboxEventoJpaEntity evento) {
        try {
            snsClient.publish(PublishRequest.builder()
                    .topicArn(properties.topicoArn())
                    .message(evento.getEnvelope())
                    .messageAttributes(Map.of(ATRIBUTO_EVENT_TYPE, MessageAttributeValue.builder()
                            .dataType("String")
                            .stringValue(evento.getEventType())
                            .build()))
                    .build());
            evento.setPublicadoEm(clock.instant());
            evento.setUltimoErro(null);
            log.info("[SAGA_EVENTO_PUBLICADO] event_type={} event_id={} saga_id={}",
                    evento.getEventType(), evento.getEventId(), evento.getSagaId());
        } catch (RuntimeException e) {
            evento.setTentativas(evento.getTentativas() + 1);
            evento.setUltimoErro(truncar(e.getMessage()));
            log.warn("[SAGA_EVENTO_FALHA_PUBLICACAO] event_type={} event_id={} saga_id={} tentativas={} erro={}",
                    evento.getEventType(), evento.getEventId(), evento.getSagaId(), evento.getTentativas(), e.getMessage());
        }
        repository.save(evento);
    }

    private static String truncar(String mensagem) {
        if (mensagem == null) return null;
        return mensagem.length() <= 1000 ? mensagem : mensagem.substring(0, 1000);
    }
}
