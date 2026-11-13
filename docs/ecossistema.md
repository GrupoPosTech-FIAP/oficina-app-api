# 🏗️ Arquitetura do Ecossistema Oficina

Visão completa da arquitetura que atravessa todos os repositórios do projeto Oficina (Fase 4:
microsserviços com Saga coreografada). Para a arquitetura interna do serviço de OS (Clean
Architecture, módulos Maven, data flow), consulte [arquitetura.md](arquitetura.md).

---

## Repositórios e responsabilidades

Cada microsserviço tem repositório, pipeline, imagem e banco próprios. A infraestrutura
compartilhada fica em repositórios de Terraform.

| Repositório | Responsabilidade | Banco | Tecnologia |
|---|---|---|---|
| [`oficina-app-api`](https://github.com/GrupoPosTech-FIAP/oficina-app-api) | **OS Service**: abertura e diagnóstico da OS, status e histórico da Saga, catálogo de serviços, veículos, notificações ao cliente | PostgreSQL (`oficina_db`) | Java 25, Spring Boot 4, Clean Architecture multi-módulo |
| [`oficina-billing-service`](https://github.com/GrupoPosTech-FIAP/oficina-billing-service) | **Billing Service**: orçamento, aprovação, cobrança no Mercado Pago (Checkout Pro), webhook de pagamento, timeouts e reembolso | PostgreSQL (`billing_db`) | Java 25, Spring Boot 4, SDK Mercado Pago |
| [`oficina-inventory-service`](https://github.com/GrupoPosTech-FIAP/oficina-inventory-service) | **Inventory Service**: catálogo de insumos, baixa de estoque por OS (tudo ou nada) e estorno | PostgreSQL (`inventory_db`) | Java 25, Spring Boot 4 |
| [`oficina-execution-service`](https://github.com/GrupoPosTech-FIAP/oficina-execution-service) | **Execution Service**: fila de execução, tarefas por serviço, andamentos (diagnóstico, reparo, teste), conclusão ou falha e tempo médio | **DynamoDB** (NoSQL) | Java 25, Spring Boot 4, AWS SDK v2 |
| [`oficina-customer-service`](https://github.com/GrupoPosTech-FIAP/oficina-customer-service) | **Customer Service**: cadastro de clientes (CPF/CNPJ, soft delete) | PostgreSQL (`customer_db`) | Java 25, Spring Boot 4 |
| [`oficina-auth-gateway`](https://github.com/GrupoPosTech-FIAP/oficina-auth-gateway) | Porta de entrada: autenticação por CPF (Lambda), autorizador JWT e roteamento para os serviços | DynamoDB (rate limit) | Node.js 20, TypeScript, Terraform |
| [`oficina-infra-cluster`](https://github.com/GrupoPosTech-FIAP/oficina-infra-cluster) | VPC, cluster EKS, repositórios ECR e a mensageria da Saga (tópicos SNS, filas SQS e DLQs) | — | Terraform |
| [`oficina-infra-database`](https://github.com/GrupoPosTech-FIAP/oficina-infra-database) | Instância RDS PostgreSQL e as tabelas DynamoDB do execution | — | Terraform |

**Banco por serviço:** os serviços relacionais usam bases lógicas separadas na mesma instância RDS
(cada serviço cria e usa só a sua base), e o execution usa DynamoDB.
Nenhum serviço lê o banco de outro: tudo que um serviço precisa de outro chega por evento.

---

## Diagrama geral da arquitetura (Fase 4)

Microsserviços, bancos e comunicação. As setas cheias são chamadas síncronas (REST); as
tracejadas são eventos da Saga (SNS → SQS).

```mermaid
graph TB
    CLIENTE((Cliente / Atendente / Mecânico))
    MP["💳 Mercado Pago\n(Checkout Pro + webhook)"]

    subgraph AWS["☁️ AWS us-east-1"]
        APIGW["🔀 API Gateway + Lambdas\n(oficina-auth-gateway)\nPOST /auth · JWT authorizer"]

        subgraph EKS["🖥️ Cluster EKS (pods com HPA)"]
            OS["📋 OS Service\noficina-app-api"]
            BILL["💰 Billing Service"]
            INV["📦 Inventory Service"]
            EXE["🔧 Execution Service"]
            CUS["👤 Customer Service"]
        end

        subgraph DADOS["🗄️ Bancos (um por serviço)"]
            RDS_OS[("PostgreSQL\noficina_db")]
            RDS_BILL[("PostgreSQL\nbilling_db")]
            RDS_INV[("PostgreSQL\ninventory_db")]
            RDS_CUS[("PostgreSQL\ncustomer_db")]
            DDB[("DynamoDB\noficina-execucoes\n+ outbox")]
        end

        subgraph MSG["📨 Mensageria da Saga"]
            T_OS{{"SNS oficina-os-events"}}
            T_BILL{{"SNS oficina-billing-events"}}
            T_INV{{"SNS oficina-inventory-events"}}
            T_EXE{{"SNS oficina-execution-events"}}
            Q["SQS: uma fila + DLQ por serviço\n(filter policy por eventType)"]
        end

        NR["🔭 New Relic\n(OpenTelemetry OTLP)"]
    end

    CLIENTE -->|"REST + JWT"| APIGW
    APIGW -->|"/app"| OS
    APIGW -->|"/billing"| BILL
    APIGW -->|"/inventory"| INV
    BILL <-->|"preferência / reembolso"| MP
    MP -->|"POST /webhooks/mercadopago"| APIGW

    OS --- RDS_OS
    BILL --- RDS_BILL
    INV --- RDS_INV
    CUS --- RDS_CUS
    EXE --- DDB

    OS -.->|DiagnosticoConcluido| T_OS
    BILL -.->|"Orcamento*, Cobranca*, Pagamento*"| T_BILL
    INV -.->|"EstoqueBaixado / Insuficiente / Estornado"| T_INV
    EXE -.->|"ExecucaoIniciada / Concluida / Falhou"| T_EXE
    T_OS & T_BILL & T_INV & T_EXE -.-> Q
    Q -.-> OS & BILL & INV & EXE

    EKS -.->|"traces, métricas, logs"| NR

    style OS fill:#f72585,color:#fff
    style BILL fill:#f72585,color:#fff
    style INV fill:#f72585,color:#fff
    style EXE fill:#f72585,color:#fff
    style CUS fill:#f72585,color:#fff
    style APIGW fill:#7209b7,color:#fff
    style DDB fill:#3a0ca3,color:#fff
    style RDS_OS fill:#3a0ca3,color:#fff
    style RDS_BILL fill:#3a0ca3,color:#fff
    style RDS_INV fill:#3a0ca3,color:#fff
    style RDS_CUS fill:#3a0ca3,color:#fff
```

### Quem consome o quê

Um tópico SNS por serviço produtor e uma fila SQS por serviço consumidor; cada fila assina só os
eventos de que precisa (`messaging.tf` em `oficina-infra-cluster`).

| Fila | Eventos consumidos |
|---|---|
| `oficina-os-queue` | todos os eventos de billing, inventory e execution (o OS Service é dono do status e do histórico) |
| `oficina-billing-queue` | `DiagnosticoConcluido`, `EstoqueBaixado`, `ExecucaoFalhou` |
| `oficina-inventory-queue` | `OrcamentoAprovado`, `PagamentoRecusado`, `PagamentoExpirado`, `ExecucaoFalhou` |
| `oficina-execution-queue` | `DiagnosticoConcluido`, `PagamentoConfirmado` |

---

## Saga Pattern: coreografia

A Saga da OS é **coreografada**: não existe orquestrador. Cada serviço reage a eventos e publica
o resultado do seu passo; as falhas disparam compensações nos serviços que já agiram. Decisão e
alternativas na [ADR 005](adr/005-saga-coreografada.md); máquina de estados e compensações em
[saga.md](saga.md).

```mermaid
sequenceDiagram
    autonumber
    participant OS as OS Service
    participant BI as Billing
    participant IN as Inventory
    participant EX as Execution

    OS-->>BI: DiagnosticoConcluido
    OS-->>EX: DiagnosticoConcluido (tarefas planejadas)
    BI-->>OS: OrcamentoGerado
    Note over BI: cliente aprova o orçamento
    BI-->>IN: OrcamentoAprovado
    IN-->>BI: EstoqueBaixado
    BI-->>OS: CobrancaCriada (link do Mercado Pago)
    Note over BI: webhook do Mercado Pago
    BI-->>OS: PagamentoConfirmado
    BI-->>EX: PagamentoConfirmado (entra na fila)
    EX-->>OS: ExecucaoIniciada
    EX-->>OS: ExecucaoConcluida → OS FINALIZADA

    Note over OS,EX: Falha na execução (compensação)
    EX-->>IN: ExecucaoFalhou → estorna insumos (EstoqueEstornado)
    EX-->>BI: ExecucaoFalhou → reembolsa no Mercado Pago (PagamentoEstornado)
    EX-->>OS: ExecucaoFalhou → OS CANCELADA
```

**Por que coreografia:** são quatro serviços com passos bem definidos e cada um já é dono da sua
regra (o billing decide sobre pagamento, o inventory sobre estoque). Um orquestrador central
concentraria regras de todos e viraria ponto único de falha e de deploy. A coreografia mantém os
serviços independentes; o custo, que é perder a visão central do fluxo, é coberto pelo histórico
da Saga no OS Service e pelos logs com `saga_id` no New Relic.

**Garantias de consistência em todos os serviços:**
- **Transactional outbox:** o evento é gravado na mesma transação da mudança de estado (no
  DynamoDB, com `TransactWriteItems`) e um relay publica no SNS.
- **Consumidor idempotente:** eventos repetidos ou fora de ordem são descartados sem erro.
- **Compensações idempotentes:** o estorno de estoque e o reembolso só acontecem uma vez por OS.
- **DLQ** em cada fila, com alarme no CloudWatch.

---

## Justificativa da divisão e das tecnologias

**Divisão pelos limites do negócio.** Os serviços seguem as áreas da oficina citadas no desafio:
atendimento e acompanhamento da OS (OS Service), orçamento e pagamento (Billing), peças e
insumos (Inventory), produção na oficina (Execution) e cadastro de clientes (Customer). Cada um
muda por motivos diferentes: o billing por regras de cobrança e do Mercado Pago, o inventory por
regras de estoque, o execution pelo processo da oficina.

| Tecnologia | Por quê |
|---|---|
| Java 25 + Spring Boot 4 | Mesma base nos serviços, que já era a da Fase 3; Clean Architecture em todos |
| PostgreSQL (RDS) | OS, cobrança, estoque e cadastro precisam de transação ACID e restrições (baixa de estoque tudo ou nada, documento único, pagamento não duplicado) |
| DynamoDB | Exigência de NoSQL. A execução é acessada sempre pela OS e guarda o agregado inteiro (tarefas e andamentos) num item; gerenciado e barato no Learner Lab |
| SNS + SQS | Mensageria gerenciada da AWS: fan-out por tópico, filtro por tipo de evento, DLQ e retentativa sem operar broker |
| Mercado Pago Checkout Pro | Exigência do desafio para pagamentos; webhook confirma o pagamento e a API faz o reembolso na compensação |
| API Gateway + Lambda | Porta única com autenticação por CPF e autorizador JWT, já da Fase 3 |
| EKS + Kustomize + HPA | Cada serviço com Deployment, Service e autoescala; overlays para AWS |
| Terraform | Infraestrutura versionada em repositórios separados (cluster e mensageria, bancos, gateway) |
| GitHub Actions | CI em todo PR (build, testes, cobertura mínima de 80% e SonarCloud) e deploy no EKS por serviço |
| OpenTelemetry + New Relic | Mesma observabilidade da Fase 3, agora em todos os serviços da Saga |

---

## Diagrama de Componentes (Fase 3)

O diagrama abaixo é o da Fase 3, com a aplicação monolítica, e mostra em detalhe a camada de
borda (autenticação) e o cluster. As cores indicam qual repositório é responsável por cada
componente.

```mermaid
graph TB
    subgraph INTERNET["🌐 Internet"]
        CLIENTE((Cliente))
    end

    subgraph AWS["☁️ AWS us-east-1"]
        subgraph EDGE["Camada de Borda"]
            APIGW["🔀 API Gateway HTTP\n(oficina-auth-gateway)"]
            CW_LOGS["📋 CloudWatch Logs\n(Access Logs)"]
            DYNAMO["⏱️ DynamoDB\n(Rate Limiting)"]
        end

        subgraph LAMBDA_LAYER["Camada Serverless"]
            AUTH_FN["⚡ Lambda: auth-handler\n(Autenticação por CPF)"]
            AUTHZ_FN["⚡ Lambda: authorizer\n(Validação JWT)"]
        end

        subgraph VPC["🔒 VPC 10.0.0.0/16"]
            subgraph SUBNETS["Subnets Públicas (us-east-1a/b/c)"]
                subgraph EKS_CLUSTER["🖥️ Cluster EKS"]
                    POD_API["🫛 Pod: oficina-api\n(Spring Boot)"]
                    HPA["📊 HPA\n(CPU 70% / Mem 80%)"]
                    SVC["🔗 Service LoadBalancer\n(ELB porta 8080)"]
                end
            end

            subgraph DB_LAYER["Camada de Dados"]
                SG_RDS["🔒 Security Group RDS\n(porta 5432 ← EKS SG)"]
                RDS[("🗄️ RDS PostgreSQL 15\nofficina_db\ndb.t3.micro")]
            end
        end

        subgraph ECR_LAYER["Registro de Imagens"]
            ECR["📦 ECR: oficina-api\n(IMMUTABLE tags)"]
        end

        subgraph S3_LAYER["Armazenamento de Estado"]
            S3_STATE["🪣 S3 Bucket\n(terraform.tfstate)"]
        end

        subgraph MONITORING["📊 Monitoramento"]
            OTEL["OpenTelemetry Agent\n(Auto-instrumentação)"]
            NR["🔭 New Relic\n(OTLP endpoint)"]
        end
    end

    CLIENTE -->|"POST /auth (CPF)"| APIGW
    CLIENTE -->|"ANY /app/* + Bearer JWT"| APIGW
    APIGW -->|"Rota /auth"| AUTH_FN
    APIGW -->|"Lambda Authorizer"| AUTHZ_FN
    APIGW -->|"Proxy HTTP (se autorizado)"| SVC
    APIGW --> CW_LOGS
    AUTH_FN -->|"Rate limit check"| DYNAMO
    AUTH_FN -->|"SELECT FROM clientes"| RDS
    AUTH_FN -.->|"Assina JWT (HS256)"| AUTHZ_FN
    SVC --> POD_API
    HPA -->|"Escala 1-5 réplicas"| POD_API
    POD_API -->|"JDBC porta 5432"| RDS
    POD_API --> OTEL
    OTEL -->|"Traces, Métricas, Logs\n(OTLP/Protobuf)"| NR
    ECR -.->|"Pull image"| POD_API
    SG_RDS --> RDS

    %% Cores por responsabilidade
    style APIGW fill:#7209b7,color:#fff
    style AUTH_FN fill:#7209b7,color:#fff
    style AUTHZ_FN fill:#7209b7,color:#fff
    style CW_LOGS fill:#7209b7,color:#fff
    style DYNAMO fill:#7209b7,color:#fff
    style EKS_CLUSTER fill:#4361ee,color:#fff
    style ECR fill:#4361ee,color:#fff
    style SG_RDS fill:#3a0ca3,color:#fff
    style RDS fill:#3a0ca3,color:#fff
    style POD_API fill:#f72585,color:#fff
    style HPA fill:#f72585,color:#fff
    style SVC fill:#f72585,color:#fff
    style OTEL fill:#f72585,color:#fff
```

**Legenda de cores:**
- 🟣 Roxo escuro (`oficina-auth-gateway`) — API Gateway, Lambdas, Rate Limiting
- 🔵 Azul (`oficina-infra-cluster`) — VPC, EKS, ECR
- 🟣 Roxo (`oficina-infra-database`) — RDS, Security Group do banco
- 🩷 Rosa (`oficina-app-api`) — Pod da aplicação, HPA, Service, Monitoramento

---

## Diagrama de Sequência — Autenticação por CPF

O fluxo de autenticação por CPF é implementado inteiramente no `oficina-auth-gateway`,
sem alterações na `oficina-app-api`. O cliente primeiro obtém um JWT e depois o usa para
acessar a API.

```mermaid
sequenceDiagram
    autonumber
    actor Cliente as Cliente
    participant APIGW as API Gateway
    participant RL as DynamoDB<br/>(Rate Limit)
    participant AuthFn as Lambda<br/>auth-handler
    participant DB as RDS PostgreSQL<br/>(oficina_db)
    participant JWT as JWT (HS256)

    Note over Cliente,JWT: Fase 1 — Obter o token

    Cliente->>APIGW: POST /auth { "cpf": "529.982.247-25" }
    APIGW->>AuthFn: Integração Lambda

    AuthFn->>RL: Registrar tentativa (IP origem)
    RL-->>AuthFn: Permitido (< limite)

    AuthFn->>AuthFn: Validar formato CPF (dígitos verificadores)

    AuthFn->>DB: SELECT ativo FROM clientes<br/>WHERE documento = '52998224725'
    DB-->>AuthFn: { encontrado: true, ativo: true }

    AuthFn->>JWT: Assinar token (sub: CPF, exp: 1h)
    JWT-->>AuthFn: eyJhbGciOiJIUzI1NiJ9...

    AuthFn-->>APIGW: 200 { "token": "<jwt>" }
    APIGW-->>Cliente: 200 { "token": "<jwt>" }

    Note over Cliente,JWT: Fase 2 — Usar o token para acessar a API

    Cliente->>APIGW: GET /app/ordem-servico<br/>Authorization: Bearer <jwt>
    
    rect rgb(240, 240, 250)
        Note over APIGW,JWT: Lambda Authorizer (REQUEST, simple response)
        APIGW->>JWT: Validar assinatura + expiração
        JWT-->>APIGW: { isAuthorized: true, context: { cpf } }
    end

    APIGW->>APIGW: Proxy HTTP para oficina-app-api (ELB)
    Note over APIGW: Repassa a requisição original<br/>para o LoadBalancer do EKS

    APIGW-->>Cliente: 200 (resposta da oficina-app-api)
```

**Cenários de erro:**

| Situação | Código | Onde o fluxo para |
|---|---|---|
| IP excedeu limite de tentativas | `429` | Lambda `auth-handler` (step 3) |
| CPF mal formado | `400` | Lambda `auth-handler` (step 4) |
| CPF não cadastrado | `404` | Lambda `auth-handler` (step 5-6) |
| Cliente inativo | `403` | Lambda `auth-handler` (step 6) |
| Token ausente/inválido/expirado | `401` | Lambda Authorizer (step 14) |

---

## Diagrama de Sequência — Abertura de Ordem de Serviço

Este fluxo mostra o caminho de uma requisição desde o cliente autenticado até a persistência
no banco, passando por todas as camadas da Clean Architecture.

```mermaid
sequenceDiagram
    autonumber
    actor Cliente as Cliente Autenticado
    participant APIGW as API Gateway
    participant Authz as Lambda Authorizer
    participant ELB as ELB (Service K8s)
    participant Ctrl as OrdemServicoController<br/>(oficina-adapters)
    participant UC as CriarOrdemServicoUseCase<br/>(oficina-application)
    participant Dom as OrdemServico<br/>(oficina-domain)
    participant GW as OrdemServicoJpaGateway<br/>(oficina-adapters)
    participant DB as RDS PostgreSQL

    Cliente->>APIGW: POST /app/ordem-servico<br/>Authorization: Bearer <jwt><br/>{ veiculoId, clienteId }

    APIGW->>Authz: Validar JWT
    Authz-->>APIGW: ✅ isAuthorized

    APIGW->>ELB: Proxy HTTP → :8080/ordem-servico
    ELB->>Ctrl: POST /ordem-servico (JSON)

    Note over Ctrl,UC: oficina-adapters → oficina-application
    Ctrl->>Ctrl: Converter DTO → parâmetros do Use Case
    Ctrl->>UC: executar(veiculoId, clienteId)

    Note over UC,Dom: oficina-application → oficina-domain
    UC->>Dom: new OrdemServico(veiculoId, clienteId)
    Dom->>Dom: Validar regras de negócio<br/>(veículo existe, cliente ativo)
    Dom->>Dom: Status inicial: RECEBIDA
    Dom-->>UC: Instância válida

    Note over UC,GW: Use Case → Gateway (via interface)
    UC->>GW: salvar(ordemServico)

    Note over GW,DB: oficina-adapters → PostgreSQL
    GW->>GW: Converter OrdemServico → OrdemServicoJpaEntity
    GW->>DB: INSERT INTO ordens_servico (...)
    DB-->>GW: Registro salvo
    GW-->>UC: OrdemServico persistida

    UC-->>Ctrl: OrdemServico criada
    Ctrl-->>Cliente: 201 Created (OrdemServicoResponse)

    Note over Cliente,DB: Ciclo de vida da OS
    Note right of Dom: RECEBIDA → EM_DIAGNOSTICO →<br/>AGUARDANDO_APROVACAO →<br/>EM_EXECUCAO → FINALIZADA → ENTREGUE
```

---

## Dependências entre repositórios (Terraform Remote State)

Os repositórios de infraestrutura formam uma cadeia de dependências via `terraform_remote_state`.
Os outputs de um repositório são consumidos como data sources pelo próximo.

```mermaid
graph LR
    subgraph S3["🪣 Bucket S3 (compartilhado)"]
        S1["cluster/s3/terraform.tfstate"]
        S2["database/s3/terraform.tfstate"]
        S3_AUTH["auth-gateway/s3/terraform.tfstate"]
    end

    CLUSTER["oficina-infra-cluster"] -->|"Grava state"| S1
    S1 -->|"Lê: VPC_ID, SUBNET_ID,\nEKS_Security_Group_Id"| DB["oficina-infra-database"]
    DB -->|"Grava state"| S2
    S1 -->|"Lê: VPC_ID, SUBNET_ID"| AUTH["oficina-auth-gateway"]
    S2 -->|"Lê: DB_Endpoint,\nDB_Secret_Arn, RDS_SG_Id"| AUTH
    AUTH -->|"Grava state"| S3_AUTH

    style CLUSTER fill:#4361ee,color:#fff
    style DB fill:#3a0ca3,color:#fff
    style AUTH fill:#7209b7,color:#fff
```

---

## Ordem obrigatória de deploy

A infraestrutura **deve** ser provisionada nesta ordem. Cada etapa depende dos outputs da anterior.

```mermaid
graph LR
    A["1️⃣ oficina-infra-cluster\n(VPC, EKS, ECR, SNS/SQS)"] --> B["2️⃣ oficina-infra-database\n(RDS PostgreSQL, DynamoDB)"]
    B --> S["3️⃣ Serviços da Saga\n(execution, billing, inventory)"]
    S --> D["4️⃣ oficina-app-api\n(OS Service)"]
    B --> C["5️⃣ oficina-auth-gateway\n(API Gateway + Lambdas)"]
    C -.->|"Proxy HTTP"| D

    style A fill:#4361ee,color:#fff
    style B fill:#3a0ca3,color:#fff
    style S fill:#f72585,color:#fff
    style C fill:#7209b7,color:#fff
    style D fill:#f72585,color:#fff
```

| Passo | Repo | Como | Depende de |
|---|---|---|---|
| 1 | `oficina-infra-cluster` | `terraform apply` (~10-15 min) | Bucket S3 do state |
| 2 | `oficina-infra-database` | `terraform apply` (~10-15 min) | Passo 1 |
| 3 | `oficina-execution-service`, `oficina-billing-service`, `oficina-inventory-service` | workflow `deploy.yml` de cada repositório (build, imagem no ECR e `kubectl apply`) | Passos 1 e 2 |
| 4 | `oficina-app-api` | workflow `deploy.yml` | Passo 3: a OS depende dos eventos do execution |
| 5 | `oficina-auth-gateway` | GitHub Actions (CD), com as URLs dos LoadBalancers dos serviços | Passos 1 a 4 |

> ⚠️ A destruição deve ser feita na **ordem inversa**.

---

## Contexto: AWS Academy Learner Lab

Toda a infraestrutura roda em um sandbox da AWS com as seguintes restrições:

- **Orçamento virtual** de ~$100 (sem cartão de crédito)
- **Credenciais expiram a cada ~4h** — exigem atualização manual nos GitHub Secrets
- **Região fixa:** `us-east-1` (Norte da Virgínia)
- **Não é possível criar IAM Roles** — todos os repos usam a role pré-existente `LabRole`
- **Alguns serviços avançados são bloqueados** (ex: AWS WAF)

Guia completo de acesso em [`oficina-infra-cluster/docs/acesso-aws-learner-lab.md`](https://github.com/GrupoPosTech-FIAP/oficina-infra-cluster/blob/main/docs/acesso-aws-learner-lab.md).
