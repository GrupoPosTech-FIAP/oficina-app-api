package com.mecanica.oficina_api.adapters.messaging;

import com.mecanica.oficina_api.application.ordemservico.usecase.ProcessarEventoSagaUseCase;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Eventos de billing e inventory que movem a OS na máquina de estados da Saga. */
@Component
public class EventosSagaOrdemServicoManipulador implements ManipuladorEvento {

    private static final Logger log = LoggerFactory.getLogger(EventosSagaOrdemServicoManipulador.class);

    private final ProcessarEventoSagaUseCase useCase;

    public EventosSagaOrdemServicoManipulador(ProcessarEventoSagaUseCase useCase) {
        this.useCase = useCase;
    }

    @Override
    public Set<String> eventTypes() {
        return ProcessarEventoSagaUseCase.EVENTOS_CONSUMIDOS;
    }

    @Override
    public void tratar(EventoEnvelope evento) {
        String detalhe = evento.payload() == null ? null : evento.payload().toString();
        ProcessarEventoSagaUseCase.Resultado resultado = useCase.executar(evento.eventType(), evento.sagaId(), detalhe);

        if (resultado == ProcessarEventoSagaUseCase.Resultado.TRANSICAO_IGNORADA
                || resultado == ProcessarEventoSagaUseCase.Resultado.ORDEM_SERVICO_INEXISTENTE) {
            log.warn("[SAGA_EVENTO_DESCARTADO] event_type={} event_id={} saga_id={} motivo={}",
                    evento.eventType(), evento.eventId(), evento.sagaId(), resultado);
        }
    }
}
