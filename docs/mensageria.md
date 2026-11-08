# 📨 Mensageria (SNS + SQS)

Infraestrutura de eventos da Saga coreografada descrita na
[ADR 005](adr/005-saga-coreografada.md). Este documento cobre **como funciona e como usar**;
o catálogo de eventos e as compensações estão na ADR.

## Topologia

| Recurso | Nome | Criado por |
|---|---|---|
| Tópico SNS por produtor | `oficina-{os,billing,inventory,execution}-events` | `oficina-infra-cluster/messaging.tf` |
| Fila SQS por consumidor | `oficina-{os,billing,inventory,execution}-queue` | idem |
| DLQ por consumidor | `oficina-{...}-dlq` (após 5 tentativas) | idem |
| Assinaturas | fila ← tópico, com *filter policy* por `eventType` e *raw delivery* | idem |

Este serviço **publica** em `oficina-os-events` e **consome** `oficina-os-queue`.

## Como funciona neste serviço

```
caso de uso ──► PublicadorEventosGateway ──► outbox_eventos (mesma transação)
                                                    │
                                     OutboxRelay (@Scheduled, 1s)
                                                    ▼
                                           SNS oficina-os-events

SQS oficina-os-queue ──► SqsConsumidorEventos (long polling)
                               │
                               ▼
                       ProcessadorEventos ── eventos_processados (idempotência)
                               │
                               ▼
                       ManipuladorEvento do eventType ──► caso de uso
```

| Classe | Papel |
|---|---|
| `application/evento/gateway/PublicadorEventosGateway` | Porta de saída usada pelos casos de uso |
| `adapters/messaging/OutboxPublicadorEventos` | Grava o envelope na outbox. Exige transação aberta (`MANDATORY`) |
| `adapters/messaging/OutboxRelay` | Publica pendentes no SNS com o atributo `eventType`. Falha fica pendente e é retentada |
| `adapters/messaging/SqsConsumidorEventos` | Recebe da fila; só apaga a mensagem depois de processada |
| `adapters/messaging/ProcessadorEventos` | Deduplica por `eventId` e chama o manipulador na mesma transação |
| `adapters/messaging/ManipuladorEvento` | Interface dos manipuladores (um por `eventType`) |

Garantias: entrega **at-least-once**; o consumidor descarta duplicados; mensagem que falha
volta para a fila e, após 5 tentativas, vai para a DLQ.

## Publicando um evento

No caso de uso, receba `PublicadorEventosGateway` pelo construtor e publique depois de
alterar o estado. A chamada precisa acontecer dentro de uma transação (o evento só vale se
a mudança de estado for confirmada):

```java
publicadorEventos.publicar("DiagnosticoConcluido", os.getId(), Map.of(
        "clienteId", os.getClienteId(),
        "veiculoId", os.getVeiculoId()));
```

## Consumindo um evento

Crie um `@Component` em `adapters/messaging` implementando `ManipuladorEvento`. Ele roda
dentro da transação do `ProcessadorEventos`; lançar exceção desfaz tudo e devolve a
mensagem para a fila.

```java
@Component
class OrcamentoGeradoManipulador implements ManipuladorEvento {
    public Set<String> eventTypes() { return Set.of("OrcamentoGerado"); }
    public void tratar(EventoEnvelope evento) {
        // traduz evento.payload() e chama o caso de uso
    }
}
```

Na OS, um único `EventosSagaOrdemServicoManipulador` trata todos os eventos recebidos e
delega ao `ProcessarEventoSagaUseCase` (ver [saga.md](saga.md)).

Lembre de incluir o `eventType` na assinatura da fila em `messaging.tf` e em
`localstack/init-mensageria.sh`.

## Configuração

| Variável | Padrão | Descrição |
|---|---|---|
| `MENSAGERIA_HABILITADA` | `false` | Liga relay e consumidor. Desligada, eventos ficam só na outbox |
| `MENSAGERIA_TOPICO_ARN` | — | Tópico onde o serviço publica |
| `MENSAGERIA_FILA_URL` | — | Fila que o serviço consome |
| `MENSAGERIA_ENDPOINT` | — | Endpoint alternativo (LocalStack) |
| `AWS_REGION` | `us-east-1` | Região |

Credenciais seguem a cadeia padrão do AWS SDK: no EKS, a role do nó (`LabRole`); no
docker-compose, `test/test` do LocalStack.

## Rodando localmente

`docker-compose up --build` sobe também o LocalStack (`localstack/localstack:4.4.0`) e cria
toda a topologia via [`localstack/init-mensageria.sh`](../localstack/init-mensageria.sh).
Comandos úteis (dentro do container `oficina_localstack`):

```bash
# publicar um evento como se viesse do billing
awslocal sns publish --topic-arn arn:aws:sns:us-east-1:000000000000:oficina-billing-events \
  --message '{"eventId":"1","eventType":"OrcamentoGerado","eventVersion":1,"occurredAt":"2026-11-08T01:00:00Z","producer":"oficina-billing-service","sagaId":"<os-id>","payload":{}}' \
  --message-attributes '{"eventType":{"DataType":"String","StringValue":"OrcamentoGerado"}}'

# ver o que o os-service publicou para o billing
awslocal sqs receive-message --queue-url http://localhost:4566/000000000000/oficina-billing-queue

# inspecionar a DLQ
awslocal sqs receive-message --queue-url http://localhost:4566/000000000000/oficina-os-dlq
```

## AWS

O `deploy.yml` resolve o ARN do tópico e a URL da fila pelo nome (contêm o account ID da
sessão do Learner Lab) e injeta no `configmap-rds.yaml`. Por isso o `oficina-infra-cluster`
precisa ter sido aplicado antes do deploy deste serviço.
