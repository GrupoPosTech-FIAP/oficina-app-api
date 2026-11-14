# Tech Challenge Fase 4: estado e pendências

Atualizado em 2026-11-13. Ponto de partida para quem for continuar: o que já está pronto, as
decisões fechadas e o que falta para a entrega. Atualize este arquivo ao fechar cada item.

Documentação da entrega: [entrega.md](entrega.md) (conteúdo do PDF) e
[ecossistema.md](ecossistema.md) (arquitetura, Saga e justificativas).

---

## 1. O que está pronto (tudo na `main`)

| Requisito do PDF | Situação | Onde |
|---|---|---|
| 3+ microsserviços, repositório e banco próprios | ✅ OS (app-api), billing, inventory, execution, customer | um repositório por serviço |
| Banco SQL e NoSQL | ✅ PostgreSQL (RDS, base por serviço) e DynamoDB (execution) | `oficina-infra-database` |
| REST + mensageria | ✅ REST pela API Gateway; eventos SNS/SQS com filtro por `eventType` e DLQ | `oficina-infra-cluster/messaging.tf` |
| Nenhum serviço acessa banco de outro | ✅ | — |
| Saga com compensação | ✅ coreografada; outbox, consumidores idempotentes, estorno de estoque e reembolso no Mercado Pago | [ADR 005](adr/005-saga-coreografada.md), [saga.md](saga.md) |
| Integração Mercado Pago | ✅ Checkout Pro, webhook, reembolso | billing |
| Testes unitários em todos | ✅ | — |
| BDD de um fluxo completo | ✅ Saga em Cucumber | `oficina-app-api/oficina-infrastructure` |
| Cobertura ≥ 80% por serviço | ✅ app-api 95,0% (agregada), billing 85,3%, inventory 97,1%, execution 99,7%, customer 92,8%; o CI reprova abaixo de 80% | `maven.yml` de cada repositório |
| SonarQube ou similar no CI | ⚠️ código pronto (SonarCloud); falta configurar a conta (seção 3) | `maven.yml`, propriedades `sonar.*` no `pom.xml` |
| Branch protegida com PR e checks | ✅ ruleset "Protege a main" (PR obrigatório, check `build`, histórico linear) na app-api e nos 4 serviços; infra-cluster e infra-database têm ruleset próprio e o auth-gateway tem branch protection clássica, sem conferência de detalhes | GitHub, rulesets |
| Dockerfile e manifestos K8s | ✅ em todos os serviços (Kustomize, HPA, probes) | `k8s/` |
| Observabilidade da Fase 3 | ✅ agente OpenTelemetry → New Relic em OS, billing, inventory e execution; falta a license key (seção 3) | [monitoramento-e-observabilidade.md](monitoramento-e-observabilidade.md) (seção 6) |
| Swagger / collection | ✅ Swagger em todos; Bruno na app-api e no execution | — |

### Decisões fechadas (não reabrir)
- Saga **coreografada**, não orquestrada.
- Execution em **DynamoDB**, Java 25 + Spring Boot 4.1.2; os endpoints de execução e o tempo
  médio saíram da app-api.
- **SonarCloud** em todos os serviços.
- Merges por **squash** (histórico linear obrigatório).

---

## 2. Em andamento

---

## 3. O que falta para entregar

### Código e infraestrutura
1. ✅ **Rotas no API Gateway** (`oficina-auth-gateway/terraform/apigateway.tf`): adicionadas as rotas
   `/execution/{proxy+}` (porta 8084) e `/customer/{proxy+}` (porta 8084, conforme `k8s/base/app-service.yaml`
   e `application.yaml` do customer), junto com as variáveis `execution_service_base_url` e
   `customer_service_base_url` em `variables.tf` e no workflow `.github/workflows/deploy.yml`.
2. ✅ **Customer na AWS:** repositório ECR `oficina-customer-service` adicionado em
   `oficina-infra-cluster/ecr.tf` e workflow `.github/workflows/deploy.yml` criado em
   `oficina-customer-service`.
3. **Deploy automatizado (requisito "CI/CD automatizado")**: hoje o `deploy.yml` de cada
   serviço roda só manualmente (`workflow_dispatch`) ou com tag `v*`. Decidir se isso basta ou se
   o deploy deve disparar no push para a `main`. As credenciais do Learner Lab expiram a cada
   ~4h, então um deploy automático falharia com o laboratório desligado.
4. **Remover duplicação de `Cliente` (`Customer`) da `oficina-app-api`**: enquanto Billing (PR #37),
   Inventory (PR #38) e Execution (PR #42) já saíram da `oficina-app-api`, o módulo de clientes
   (`ClienteController`, casos de uso de `cliente`, `ClienteJpaEntity`, `ClienteJpaGateway` e tabela
   `clientes`) ainda está duplicado entre a `oficina-app-api` e o `oficina-customer-service`.
   Pontos acoplados que precisam ser tratados para concluir a extração:
   - **Endpoints REST (`ClienteController`):** remover `/cliente` da `oficina-app-api` para que fique
     exposto apenas pelo `oficina-customer-service`.
   - **Relacionamentos JPA (`@ManyToOne`):** `VeiculoJpaEntity` e `OrdemServicoJpaEntity` têm FK
     (`@JoinColumn(name = "cliente_id")`) apontando para `ClienteJpaEntity` em `oficina_db` (trocar
     para `String clienteId` desacoplado ou manter projeção local).
   - **Validação em casos de uso:** `CriarOrdemServicoUseCase` e `CadastrarVeiculoUseCase` chamam
     `clienteGateway.findByIdAtivo(clienteId)` localmente.
   - **Notificação por e-mail:** `EmailNotificadorCliente` consulta o e-mail em
     `ClienteSpringDataRepository`.
   - **Seed (`DevDataLoader`) e Auth Gateway (`oficina-auth-gateway`):** o `DevDataLoader` da
     `oficina-app-api` semeia os clientes de teste (incluindo o CPF `52998224725` da Ana Souza) em
     `oficina_db`, que é onde a Lambda `auth-handler` (`rds_database = "oficina_db"`) consulta
     `SELECT ativo FROM clientes WHERE documento = $1`. Se a tabela `clientes` sair de `oficina_db`,
     o seed de clientes deve ir para o `oficina-customer-service` e o `oficina-auth-gateway` deve
     apontar para `customer_db`.

### Configuração (precisa de alguém com acesso de admin)
5. **SonarCloud:**
   - Entrar em sonarcloud.io com o GitHub e importar a organização `GrupoPosTech-FIAP` (a chave
     precisa ser `grupopostech-fiap`).
   - Criar os projetos `GrupoPosTech-FIAP_<repo>` para app-api, billing, inventory, execution e
     customer.
   - Desligar a análise automática nesses projetos.
   - Criar o secret `SONAR_TOKEN` na organização do GitHub.
   - Sem o secret, o CI pula a análise com um aviso.
6. **New Relic:** secret `NEW_RELIC_LICENSE_KEY` na organização. Sem ele, o deploy sobe com
   `OTEL_SDK_DISABLED=true`.
7. **Secrets de deploy** de cada repositório (AWS do Learner Lab, `JWT_SECRET`, `RDS_PASSWORD`,
   tokens do Mercado Pago no billing). O execution precisa de `AWS_ACCESS_KEY_ID`,
   `AWS_SECRET_ACCESS_KEY`, `AWS_SESSION_TOKEN` e `JWT_SECRET`.
8. **Dependency graph** desligado no `oficina-execution-service` e talvez no customer (Settings →
   Code security). O CI não falha por isso.

### Subir e validar na AWS (nada disso foi testado de verdade ainda)
9. Ordem de deploy:
   1. `oficina-infra-cluster` (`terraform apply`)
   2. `oficina-infra-database` (`terraform apply`)
   3. execution, billing, inventory e customer (`deploy.yml` de cada um)
   4. **app-api por último**: a `main` já não tem execução, então a OS depende do execution no ar
   5. `oficina-auth-gateway`
10. Testar a Saga ponta a ponta na AWS: caminho feliz, pagamento recusado e `ExecucaoFalhou` com
    estorno e reembolso. Pontos ainda não exercitados de verdade:
    - Terraform (não roda nesta máquina);
    - lock pessimista do Postgres na baixa de estoque;
    - chamadas reais ao SDK 3.x do Mercado Pago (sandbox);
    - envio de telemetria para o New Relic.

### Entrega
11. **Vídeo** (até 15 min, YouTube/Vimeo, público ou não listado). O PDF pede que mostre:
    - o fluxo completo de uma OS pelos microsserviços;
    - a Saga e o tratamento de falha (por exemplo `PATCH /execucoes/{id}/falhar`, mostrando o
      estorno e o reembolso);
    - o deploy automatizado de um serviço com os testes passando no CI;
    - o rastreamento no New Relic (consulta por `saga_id`, seção 6 do guia de monitoramento).
12. **PDF** a partir do [entrega.md](entrega.md), com o link do vídeo no lugar do placeholder.

---

## 4. Pendências menores
- Branches já mergeadas continuam no GitHub (o bloqueio de comandos impediu apagar).
- Erro de digitação `entraga_veiculo` em `TipoNotificacaoCliente.chaveLog` (app-api); mudar
  altera a chave de log.
- O agente do OpenTelemetry é baixado como `latest` no Dockerfile (hoje 2.33.1), como já era na
  app-api; fixar a versão daria builds reproduzíveis.
- O relay do outbox gera spans a cada segundo; se o New Relic gratuito apertar, amostrar com
  `OTEL_TRACES_SAMPLER=parentbased_traceidratio`.

## 5. Ambiente
- Java 25 (Temurin) para rodar `./mvnw verify`; os testes de integração usam Docker
  (Testcontainers 1.21.4, compatível com Docker 29).
- `kubectl kustomize k8s/overlays/aws` renderiza os manifestos sem aplicar no cluster.
- `docker compose up` em cada serviço sobe a aplicação com LocalStack (SNS, SQS e DynamoDB).
