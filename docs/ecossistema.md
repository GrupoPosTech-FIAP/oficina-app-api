# 🏗️ Arquitetura do Ecossistema Oficina

Visão completa da arquitetura que atravessa todos os repositórios do projeto Oficina.
Para a arquitetura interna da aplicação (Clean Architecture, módulos Maven, data flow),
consulte [arquitetura.md](arquitetura.md).

---

## Repositórios e responsabilidades

O sistema é composto por 4 repositórios com ciclos de vida independentes:

| Repositório | Responsabilidade | Tecnologia |
|---|---|---|
| [`oficina-app-api`](https://github.com/GrupoPosTech-FIAP/oficina-app-api) | API REST da oficina mecânica, manifestos Kubernetes | Java 25, Spring Boot 4, K8s (Kustomize) |
| [`oficina-infra-cluster`](https://github.com/GrupoPosTech-FIAP/oficina-infra-cluster) | Rede (VPC, Subnets), cluster EKS, ECR | Terraform, AWS |
| [`oficina-infra-database`](https://github.com/GrupoPosTech-FIAP/oficina-infra-database) | Banco de dados RDS PostgreSQL, Security Group | Terraform, AWS RDS |
| [`oficina-auth-gateway`](https://github.com/GrupoPosTech-FIAP/oficina-auth-gateway) | Gateway de autenticação por CPF (API Gateway + Lambda) | Node.js 20, TypeScript, Terraform |

---

## Diagrama de Componentes (Visão de Nuvem)

O diagrama abaixo mostra todos os componentes provisionados na AWS e como eles se conectam.
As cores indicam qual repositório é responsável por cada componente.

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
    A["1️⃣ oficina-infra-cluster\n(VPC, EKS, ECR)"] --> B["2️⃣ oficina-infra-database\n(RDS PostgreSQL)"]
    B --> C["3️⃣ oficina-auth-gateway\n(API Gateway + Lambdas)"]
    B --> D["4️⃣ oficina-app-api\n(kubectl apply)"]
    C -.->|"Proxy HTTP"| D

    style A fill:#4361ee,color:#fff
    style B fill:#3a0ca3,color:#fff
    style C fill:#7209b7,color:#fff
    style D fill:#f72585,color:#fff
```

| Passo | Repo | Comando | Tempo | Depende de |
|---|---|---|---|---|
| 1 | `oficina-infra-cluster` | `terraform apply` | ~10-15 min | Bucket S3 criado |
| 2 | `oficina-infra-database` | `terraform apply` | ~10-15 min | Passo 1 concluído |
| 3 | `oficina-auth-gateway` | GitHub Actions (CD) | ~3-5 min | Passos 1 e 2 concluídos |
| 4 | `oficina-app-api` | `kubectl apply -k k8s/overlays/aws` | ~2-3 min | Passos 1 e 2 concluídos |

> ⚠️ A destruição deve ser feita na **ordem inversa** (4 → 3 → 2 → 1).

---

## Contexto: AWS Academy Learner Lab

Toda a infraestrutura roda em um sandbox da AWS com as seguintes restrições:

- **Orçamento virtual** de ~$100 (sem cartão de crédito)
- **Credenciais expiram a cada ~4h** — exigem atualização manual nos GitHub Secrets
- **Região fixa:** `us-east-1` (Norte da Virgínia)
- **Não é possível criar IAM Roles** — todos os repos usam a role pré-existente `LabRole`
- **Alguns serviços avançados são bloqueados** (ex: AWS WAF)

Guia completo de acesso em [`oficina-infra-cluster/docs/acesso-aws-learner-lab.md`](https://github.com/GrupoPosTech-FIAP/oficina-infra-cluster/blob/main/docs/acesso-aws-learner-lab.md).
