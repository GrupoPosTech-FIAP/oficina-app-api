# 005 — Saga Coreografada para o Ciclo de Vida da Ordem de Serviço

**Status:** Proposta
**Data:** 2026-11-07

## Resumo
O fluxo transacional da Ordem de Serviço (diagnóstico → orçamento → aprovação → baixa de
estoque → pagamento → execução) atravessa quatro microsserviços com bancos isolados. Para
garantir consistência sem transação distribuída, adotamos o **Saga Pattern coreografado**:
cada serviço executa sua transação local, publica um evento de domínio e reage aos eventos
dos demais. Falhas disparam **eventos de compensação** que desfazem os passos anteriores
(estorno de estoque, estorno do pagamento, cancelamento da OS).

A mensageria é **Amazon SNS (fan-out) + Amazon SQS (uma fila por consumidor)**, com
**Transactional Outbox**, **consumidores idempotentes** e **DLQ** em todas as filas.

Esta ADR substitui parcialmente a [004](004-padrao-comunicacao-rest.md): REST continua sendo o
padrão para a borda (cliente → API Gateway → serviços) e para consultas síncronas pontuais;
a integração entre serviços passa a ser assíncrona.

## Problema
Na Fase 4 a aplicação foi dividida em microsserviços, cada um com banco próprio:

| Serviço | Responsabilidade na Saga | Banco |
|---|---|---|
| `oficina-app-api` (OS Service) | Dono do ciclo de vida e do histórico da OS | `oficina_db` |
| `oficina-billing-service` | Orçamento, aprovação, cobrança (Mercado Pago) e estorno | `billing_db` |
| `oficina-inventory-service` | Baixa e estorno de insumos | `inventory_db` |
| `oficina-execution-service` *(a criar)* | Fila de execução, diagnóstico/reparo, conclusão | próprio |

O requisito do Tech Challenge proíbe acesso direto ao banco de outro serviço e exige
rollback seguro caso qualquer etapa falhe. Uma única transação ACID não é mais possível,
e chamadas REST encadeadas criariam acoplamento temporal (se o billing estiver fora, a OS
trava) e não oferecem compensação.

## Proposta técnica

### 1. Estilo: coreografia
Não há orquestrador central. Cada serviço conhece apenas os eventos que consome e os que
publica. O `oficina-app-api` é a **fonte da verdade do estado da OS** e registra cada evento
recebido no histórico (`historico_ordem_servico`), o que atende "consulta de status e
histórico" e serve de *saga log* para auditoria e para o vídeo de demonstração.

### 2. Infraestrutura de mensageria

```
            ┌──────────────────────┐
 os-service │ SNS oficina-os-events│──┐
            └──────────────────────┘  │   assinaturas com
 billing    ┌───────────────────────────┐ │   filter policy
            │ SNS oficina-billing-events│─┤   por eventType
            └───────────────────────────┘ │
 inventory  ┌─────────────────────────────┐│     ┌─────────────────┐   ┌──────────────┐
            │ SNS oficina-inventory-events│├────►│ SQS <servico>-q │──►│ SQS <serv>-dlq│
            └─────────────────────────────┘│     └─────────────────┘   └──────────────┘
 execution  ┌─────────────────────────────┐│      (uma fila por serviço consumidor)
            │ SNS oficina-execution-events│┘
            └─────────────────────────────┘
```

- **Um tópico SNS por serviço produtor**, **uma fila SQS por serviço consumidor**. Cada fila
  assina os tópicos de que precisa com *filter policy* sobre o atributo `eventType`.
- Cada fila tem DLQ com `maxReceiveCount = 5` e alarme no CloudWatch quando a DLQ fica
  com mensagens.
- Provisionamento via Terraform em um módulo novo (`oficina-infra-messaging` ou dentro do
  `oficina-infra-cluster`), exportando ARNs/URLs por remote state como os demais repos.
- Filas **Standard** (não FIFO): a ordem é garantida pelas regras de transição de estado
  (item 5), e não pela fila.

### 3. Envelope padrão dos eventos

Todos os eventos usam o mesmo envelope JSON. O `sagaId` é o próprio `ordemServicoId`.

```json
{
  "eventId": "8f1c2a3e-6d0b-4b8e-9a51-2f7e4c9d1a00",
  "eventType": "OrcamentoAprovado",
  "eventVersion": 1,
  "occurredAt": "2026-11-07T14:32:10.512Z",
  "producer": "oficina-billing-service",
  "sagaId": "3c9a7e1b-...-ordemServicoId",
  "payload": { }
}
```

Atributos de mensagem SNS/SQS: `eventType` (usado no filtro) e `traceparent` (W3C Trace
Context, para o rastreamento distribuído no New Relic).

### 4. Catálogo de eventos

#### Fluxo principal

| # | Evento | Produtor | Consumidores | Payload principal |
|---|---|---|---|---|
| 1 | `DiagnosticoConcluido` | os-service | billing | `ordemServicoId`, `clienteId`, `veiculoId`, `servicos[{servicoId, quantidade}]`, `insumos[{insumoId, quantidade}]` |
| 2 | `OrcamentoGerado` | billing | os | `ordemServicoId`, `orcamentoId`, `valorTotal` |
| 3 | `OrcamentoAprovado` | billing | inventory, os | `ordemServicoId`, `orcamentoId`, `insumos[{insumoId, quantidade}]` |
| 4 | `EstoqueBaixado` | inventory | billing, os | `ordemServicoId`, `itens[{insumoId, quantidade}]` |
| 5 | `CobrancaCriada` | billing | os | `ordemServicoId`, `cobrancaId`, `valor`, `checkoutUrl` (Checkout Pro) |
| 6 | `PagamentoConfirmado` | billing (webhook MP) | execution, os | `ordemServicoId`, `cobrancaId`, `mercadoPagoPaymentId`, `valor` |
| 7 | `ExecucaoIniciada` | execution | os | `ordemServicoId`, `iniciadaEm` |
| 8 | `ExecucaoConcluida` | execution | os | `ordemServicoId`, `concluidaEm` |

`EstoqueBaixado` é publicado mesmo quando a OS não usa insumos (lista vazia), para que a
cadeia continue sem caminho especial.

#### Eventos de falha e compensação

| Evento | Produtor | Consumidores | Gatilho |
|---|---|---|---|
| `OrcamentoNegado` | billing | os | Cliente recusa o orçamento |
| `OrcamentoExpirado` | billing | os | Sem resposta dentro do prazo (`orcamento.expiracao`) |
| `EstoqueInsuficiente` | inventory | os | Baixa falhou (saldo < quantidade) — nada foi baixado |
| `PagamentoRecusado` | billing (webhook MP) | inventory, os | Pagamento rejeitado pelo Mercado Pago |
| `PagamentoExpirado` | billing (scheduler) | inventory, os | Cobrança `PENDENTE` além do prazo (`cobranca.expiracao`) |
| `ExecucaoFalhou` | execution | billing, inventory, os | Reparo inviável durante a execução |
| `EstoqueEstornado` | inventory | os | Confirmação da compensação de estoque |
| `PagamentoEstornado` | billing | os | Confirmação do reembolso no Mercado Pago |

### 5. Máquina de estados da OS (os-service)

Estados novos em `OrdemServicoStatus`: `AGUARDANDO_PAGAMENTO`, `AGUARDANDO_EXECUCAO` e
`CANCELADA`.

```mermaid
stateDiagram-v2
    [*] --> RECEBIDA
    RECEBIDA --> EM_DIAGNOSTICO: iniciar diagnóstico (REST)
    EM_DIAGNOSTICO --> AGUARDANDO_APROVACAO: OrcamentoGerado
    AGUARDANDO_APROVACAO --> AGUARDANDO_PAGAMENTO: CobrancaCriada
    AGUARDANDO_APROVACAO --> CANCELADA: OrcamentoNegado / OrcamentoExpirado / EstoqueInsuficiente
    AGUARDANDO_PAGAMENTO --> AGUARDANDO_EXECUCAO: PagamentoConfirmado
    AGUARDANDO_PAGAMENTO --> CANCELADA: PagamentoRecusado / PagamentoExpirado
    AGUARDANDO_EXECUCAO --> EM_EXECUCAO: ExecucaoIniciada
    EM_EXECUCAO --> FINALIZADA: ExecucaoConcluida
    EM_EXECUCAO --> CANCELADA: ExecucaoFalhou
    FINALIZADA --> ENTREGUE: entregar (REST)
    CANCELADA --> [*]
    ENTREGUE --> [*]
```

Um evento que não corresponde a uma transição válida a partir do estado atual é
**registrado no histórico e descartado** (sem erro). Isso cobre entrega fora de ordem e
reentregas da fila Standard.

### 6. Fluxo principal

```mermaid
sequenceDiagram
    autonumber
    actor Cli as Cliente
    participant OS as os-service
    participant B as billing-service
    participant I as inventory-service
    participant MP as Mercado Pago
    participant E as execution-service

    OS->>B: DiagnosticoConcluido
    B->>B: gera orçamento (preços via CatalogoGateway)
    B->>OS: OrcamentoGerado → AGUARDANDO_APROVACAO
    Cli->>B: POST /billing/orcamentos/{id}/aprovar
    B->>I: OrcamentoAprovado
    I->>I: baixa insumos (transação local)
    I->>B: EstoqueBaixado
    B->>MP: cria preferência (Checkout Pro)
    B->>OS: CobrancaCriada → AGUARDANDO_PAGAMENTO
    Cli->>MP: paga
    MP->>B: webhook (HMAC validado)
    B->>E: PagamentoConfirmado
    B->>OS: PagamentoConfirmado → AGUARDANDO_EXECUCAO
    E->>OS: ExecucaoIniciada → EM_EXECUCAO
    E->>OS: ExecucaoConcluida → FINALIZADA
```

### 7. Compensações

| Falha em | O que já aconteceu | Compensação | Estado final da OS |
|---|---|---|---|
| Orçamento negado/expirado | Orçamento criado | Nenhuma (orçamento fica `NEGADO`) | `CANCELADA` |
| Baixa de estoque (`EstoqueInsuficiente`) | Orçamento aprovado | Nenhuma no estoque (baixa é atômica, tudo ou nada) | `CANCELADA` |
| Pagamento recusado/expirado | Estoque baixado | inventory estorna os insumos → `EstoqueEstornado` | `CANCELADA` |
| Execução (`ExecucaoFalhou`) | Estoque baixado + pagamento confirmado | inventory estorna insumos; billing reembolsa no MP (`CobrancaStatus.ESTORNADO`) → `PagamentoEstornado` | `CANCELADA` |

Exemplo de falha durante a execução:

```mermaid
sequenceDiagram
    autonumber
    participant E as execution-service
    participant OS as os-service
    participant I as inventory-service
    participant B as billing-service
    participant MP as Mercado Pago

    E->>OS: ExecucaoFalhou → CANCELADA
    E->>I: ExecucaoFalhou
    E->>B: ExecucaoFalhou
    I->>I: estorna insumos
    I->>OS: EstoqueEstornado (histórico)
    B->>MP: POST /v1/payments/{id}/refunds
    B->>OS: PagamentoEstornado (histórico)
```

Compensações são **idempotentes** e **sempre avançam** (nunca há "compensação da
compensação"). Se o reembolso no Mercado Pago falhar após as tentativas, a mensagem vai
para a DLQ do billing e o alarme sinaliza intervenção manual.

### 8. Garantias de entrega

- **Transactional Outbox** em cada serviço: a mudança de estado e o registro do evento na
  tabela `outbox` são gravados na mesma transação local. Um publicador agendado
  (`@Scheduled`) lê a `outbox` e publica no SNS, marcando como enviado. Evita o caso
  "salvou no banco mas não publicou" (ou o inverso).
- **Consumidor idempotente**: tabela `eventos_processados (event_id PK)` gravada na mesma
  transação do processamento. Evento repetido é ignorado.
- **At-least-once** ponta a ponta: o SQS pode entregar duplicado; outbox + idempotência
  tornam isso seguro.
- **Timeouts** (`OrcamentoExpirado`, `PagamentoExpirado`) gerados por scheduler no billing.
  Para a demonstração, os prazos são configuráveis via variável de ambiente.

### 9. Observabilidade
- O agente OpenTelemetry (já usado na Fase 3) instrumenta o AWS SDK. O `traceparent` é
  propagado nos atributos da mensagem, então um único trace no New Relic mostra a Saga
  inteira atravessando os serviços.
- Logs estruturados com `sagaId` e `eventType` em todo consumo/publicação.
- Dashboard no New Relic: OS por estado, compensações por tipo, profundidade das filas e
  das DLQs.

## Impacto esperado
- **Ganhos:** serviços desacoplados no tempo (billing fora do ar não perde eventos, eles
  ficam na fila); rollback explícito e testável; novo consumidor (ex.: notificações) entra
  só assinando um tópico; o histórico da OS vira trilha de auditoria.
- **Riscos:** consistência eventual (a OS pode ficar alguns segundos em estado
  intermediário); o fluxo fica distribuído no código, mitigado por este catálogo e pelo
  trace distribuído; mais infraestrutura para provisionar no Learner Lab a cada sessão.
- **Restrições:** depende de SNS/SQS liberados para a `LabRole` do AWS Academy (validar
  antes de implementar).

## Alternativas consideradas
- **Saga orquestrada (orquestrador próprio ou AWS Step Functions):** centraliza o fluxo e
  facilita a leitura, mas cria mais um serviço para manter e um ponto único de falha.
  Step Functions tem restrições no Learner Lab. Com quatro serviços e um fluxo linear, a
  coreografia cobre o caso com menos peças.
- **REST síncrono encadeado:** acoplamento temporal, sem compensação natural e com risco
  de cascata de falhas. Mantido apenas para consultas (ex.: `CatalogoGateway` do billing).
- **Kafka (MSK) / RabbitMQ:** o MSK é caro e pesado para o Learner Lab; RabbitMQ no próprio
  EKS exigiria operar persistência e alta disponibilidade. SNS/SQS são gerenciados e já
  integram com a conta. RabbitMQ no cluster fica como plano B se a `LabRole` bloquear SNS/SQS.
- **Filas FIFO:** garantiriam ordem por `MessageGroupId = ordemServicoId`, mas exigem
  SNS FIFO + SQS FIFO e limitam o throughput. As regras de transição já tornam a ordem
  irrelevante.

## Pontos em aberto
- `OrcamentoNegado` cancela a OS ou a devolve para `EM_DIAGNOSTICO`? O `GerarOrcamentoUseCase`
  já aceita gerar novo orçamento após `NEGADO`.
- Criar o `oficina-execution-service` ou, como etapa intermediária, manter execução no
  os-service publicando `ExecucaoIniciada/Concluida/Falhou`.
- Papel do `oficina-customer-service`: se assumir clientes, o `auth-gateway` deixa de ler a
  tabela `clientes` do `oficina_db`.
- Onde ficam o Terraform das filas/tópicos e a ordem de deploy atualizada.
- Cenário BDD (Cucumber) do fluxo completo e de pelo menos uma compensação, para o requisito
  de testes.
