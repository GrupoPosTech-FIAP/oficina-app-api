package com.mecanica.oficina_api.adapters.messaging;

import com.mecanica.oficina_api.adapters.persistence.EventoProcessadoJpaEntity;
import com.mecanica.oficina_api.adapters.persistence.repository.EventoProcessadoSpringDataRepository;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Entrega um evento recebido ao manipulador do seu tipo, garantindo idempotência: o
 * processamento e o registro em {@code eventos_processados} acontecem na mesma transação.
 */
@Component
public class ProcessadorEventos {

    private static final Logger log = LoggerFactory.getLogger(ProcessadorEventos.class);

    public enum Resultado { PROCESSADO, DUPLICADO, IGNORADO }

    private final Map<String, ManipuladorEvento> manipuladores;
    private final EventoProcessadoSpringDataRepository repository;
    private final TransactionOperations transacao;
    private final Clock clock;

    public ProcessadorEventos(List<ManipuladorEvento> manipuladores,
                              EventoProcessadoSpringDataRepository repository,
                              TransactionOperations transacao,
                              Clock clock) {
        this.manipuladores = manipuladores.stream()
                .flatMap(m -> m.eventTypes().stream().map(tipo -> Map.entry(tipo, m)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        this.repository = repository;
        this.transacao = transacao;
        this.clock = clock;
    }

    public Resultado processar(EventoEnvelope evento) {
        return transacao.execute(status -> {
            if (repository.existsById(evento.eventId())) {
                log.info("[SAGA_EVENTO_DUPLICADO] event_type={} event_id={} saga_id={}",
                        evento.eventType(), evento.eventId(), evento.sagaId());
                return Resultado.DUPLICADO;
            }

            ManipuladorEvento manipulador = manipuladores.get(evento.eventType());
            if (manipulador == null) {
                log.warn("[SAGA_EVENTO_SEM_MANIPULADOR] event_type={} event_id={} saga_id={}",
                        evento.eventType(), evento.eventId(), evento.sagaId());
            } else {
                manipulador.tratar(evento);
            }

            repository.save(new EventoProcessadoJpaEntity(
                    evento.eventId(), evento.eventType(), evento.sagaId(), clock.instant()));
            log.info("[SAGA_EVENTO_CONSUMIDO] event_type={} event_id={} saga_id={} producer={}",
                    evento.eventType(), evento.eventId(), evento.sagaId(), evento.producer());
            return manipulador == null ? Resultado.IGNORADO : Resultado.PROCESSADO;
        });
    }
}
