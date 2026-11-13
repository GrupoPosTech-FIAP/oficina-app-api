# Documento de entrega — Fase 4

Conteúdo do PDF entregue no portal (Tech Challenge SOAT, Fase 4: microsserviços com Saga Pattern).

## Nome do grupo

Grupo ABC

## Participantes

```
Henrique Specian Leite - henriquespecian (rm374176)
Gabriel Machado - g4abriel_plinio (rm370711)
Weyler Kleber Venancio Santos - weylersantos (rm372581)
Romario serafim da Silva - romarioss4143 (rm371940)
Matheus Fuzari - fuzari (rm372366)
```

## Link do vídeo

```
<link do vídeo da Fase 4 (até 15 minutos)>
```

## Links dos repositórios

Microsserviços (um repositório por serviço, cada um com Dockerfile, manifestos Kubernetes,
pipeline de CI/CD, evidência de cobertura no README e Swagger):

```
https://github.com/GrupoPosTech-FIAP/oficina-app-api            (OS Service)
https://github.com/GrupoPosTech-FIAP/oficina-billing-service    (Billing Service)
https://github.com/GrupoPosTech-FIAP/oficina-inventory-service  (Inventory Service)
https://github.com/GrupoPosTech-FIAP/oficina-execution-service  (Execution Service)
https://github.com/GrupoPosTech-FIAP/oficina-customer-service   (Customer Service)
```

Infraestrutura e borda:

```
https://github.com/GrupoPosTech-FIAP/oficina-auth-gateway
https://github.com/GrupoPosTech-FIAP/oficina-infra-cluster
https://github.com/GrupoPosTech-FIAP/oficina-infra-database
```

## Diagrama geral da arquitetura

Microsserviços, bancos e comunicação (REST síncrono pela API Gateway e eventos SNS/SQS entre os
serviços): [ecossistema.md — Diagrama geral da arquitetura](ecossistema.md#diagrama-geral-da-arquitetura-fase-4).

Resumo:

| Serviço | Banco | Publica | Consome |
|---|---|---|---|
| OS Service (`oficina-app-api`) | PostgreSQL | `DiagnosticoConcluido` | eventos de billing, inventory e execution (status e histórico da OS) |
| Billing Service | PostgreSQL | `OrcamentoGerado`, `OrcamentoAprovado`, `OrcamentoNegado`, `OrcamentoExpirado`, `CobrancaCriada`, `PagamentoConfirmado`, `PagamentoRecusado`, `PagamentoExpirado`, `PagamentoEstornado` | `DiagnosticoConcluido`, `EstoqueBaixado`, `ExecucaoFalhou` |
| Inventory Service | PostgreSQL | `EstoqueBaixado`, `EstoqueInsuficiente`, `EstoqueEstornado` | `OrcamentoAprovado`, `PagamentoRecusado`, `PagamentoExpirado`, `ExecucaoFalhou` |
| Execution Service | **DynamoDB** | `ExecucaoIniciada`, `ExecucaoConcluida`, `ExecucaoFalhou` | `DiagnosticoConcluido`, `PagamentoConfirmado` |
| Customer Service | PostgreSQL | — | — |

Nenhum serviço acessa o banco de outro.

## Estratégia do Saga Pattern: coreografia

A Saga da OS é **coreografada** via eventos SNS/SQS, sem orquestrador central:

1. O OS Service conclui o diagnóstico e publica `DiagnosticoConcluido`.
2. O Billing gera o orçamento (`OrcamentoGerado`); o cliente aprova (`OrcamentoAprovado`).
3. O Inventory baixa os insumos, tudo ou nada (`EstoqueBaixado` ou `EstoqueInsuficiente`).
4. O Billing cria a cobrança no Mercado Pago (`CobrancaCriada`) e, pelo webhook, confirma o
   pagamento (`PagamentoConfirmado`).
5. O Execution coloca a OS na fila, o mecânico executa as tarefas e o serviço publica
   `ExecucaoIniciada` e `ExecucaoConcluida`; o OS Service finaliza a OS.

**Rollback e compensação:**

| Falha | Compensação |
|---|---|
| Orçamento negado ou expirado, estoque insuficiente | OS cancelada (nada a desfazer) |
| Pagamento recusado ou expirado | Inventory devolve os insumos (`EstoqueEstornado`); OS cancelada |
| Falha na execução (`ExecucaoFalhou`) | Inventory devolve os insumos e Billing reembolsa no Mercado Pago (`PagamentoEstornado`); OS cancelada |

Todos os serviços usam transactional outbox, consumidores idempotentes e DLQ por fila. Decisão
completa e alternativas: [ADR 005](adr/005-saga-coreografada.md); máquina de estados:
[saga.md](saga.md).

**Por que coreografia:** cada serviço já é dono da sua regra; um orquestrador concentraria as
regras de todos e seria um ponto único de falha e de deploy. A visão do fluxo, que a coreografia
não dá por si, vem do histórico da Saga no OS Service e dos logs com `saga_id` no New Relic.

## Justificativa da divisão dos microsserviços e das tecnologias

**Divisão.** Os serviços seguem as áreas da oficina: atendimento e acompanhamento da OS (OS
Service), orçamento e pagamento (Billing), peças e insumos (Inventory), produção na oficina
(Execution) e cadastro de clientes (Customer). Cada área muda por motivos próprios e tem dados
próprios, então cada serviço tem repositório, pipeline, imagem e banco independentes.

**Tecnologias.**
- **Java 25 + Spring Boot 4 com Clean Architecture** em todos os serviços.
- **PostgreSQL** onde há transação e restrição forte (OS, cobrança, estoque, cadastro) e
  **DynamoDB** na execução, acessada sempre pela OS e guardada como um agregado num item.
- **SNS + SQS** para os eventos da Saga: gerenciados, com filtro por tipo de evento e DLQ.
- **Mercado Pago Checkout Pro** para cobrança, webhook e reembolso.
- **EKS + Kustomize + HPA** para rodar os serviços; **Terraform** para a infraestrutura.
- **GitHub Actions**: CI em todo PR com build, testes, cobertura mínima de 80% (JaCoCo) e
  SonarCloud; deploy no EKS por serviço. A `main` de cada repositório só aceita PR com o check
  `build` verde.
- **OpenTelemetry + New Relic** para traces, métricas e logs de todos os serviços.

Mais detalhes: [ecossistema.md](ecossistema.md#justificativa-da-divisão-e-das-tecnologias).

## Testes e qualidade

| Serviço | Cobertura de linhas | BDD |
|---|---|---|
| OS Service | 95,0% (agregada dos módulos) | Saga completa em Cucumber (`oficina-infrastructure`) |
| Billing | 85,3% | — |
| Inventory | 97,1% | — |
| Execution | 99,7% | — |
| Customer | 92,8% | — |

Medições de novembro de 2026; o CI de cada repositório recalcula a cobertura a cada execução e
reprova abaixo de 80%. Detalhes: [testes-e-qualidade.md](testes-e-qualidade.md).
