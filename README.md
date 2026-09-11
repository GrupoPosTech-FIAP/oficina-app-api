# Tech Challenge 15SOAT — Oficina API

API REST para gerenciamento de clientes, veículos e ordens de serviço de uma oficina
mecânica, desenvolvida como Tech Challenge da pós-graduação SOAT (Software Architecture) da FIAP.

## Stack

Java 25 · Spring Boot 4.1.0 · Spring Data JPA · Spring Security + JWT · PostgreSQL 15 ·
OpenAPI/Swagger · JaCoCo · Maven multi-módulo · Docker / Kubernetes (Kustomize).

## Arquitetura

**Maven multi-módulo** seguindo **Clean Architecture** pura — as dependências fluem sempre
de fora para dentro:

```
oficina-infrastructure → oficina-adapters → oficina-application → oficina-domain
```

Regras obrigatórias: nenhum módulo interno importa o externo; a camada `application` usa
**use cases** (não services); regras de negócio vivem no domínio; DTOs não entram no domínio.

📖 Detalhes, diagramas e a justificativa do PostgreSQL em
[docs/arquitetura.md](docs/arquitetura.md).

## Funcionalidades

- **Clientes** — cadastro, consulta, alteração e exclusão
- **Veículos** — cadastro e gestão, vinculados a clientes
- **Usuários** — gestão de usuários e perfis de acesso
- **Serviços** — catálogo de serviços com tempo médio de execução
- **Insumos** — controle de estoque, compras e movimentação
- **Ordens de Serviço** — fluxo completo com orçamento e ciclo de vida de status:

```
RECEBIDA → EM_DIAGNOSTICO → AGUARDANDO_APROVACAO → EM_EXECUCAO → FINALIZADA → ENTREGUE
```

## Como rodar

Pré-requisitos: Docker e Docker Compose (`make` opcional).

```bash
make start          # limpa tudo, reconstrói a imagem e sobe app + PostgreSQL com logs
# ou:
docker-compose up --build
```

- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

Login padrão em ambiente dev: `admin@email.com` / `123456`
(ver [docs/autenticacao-e-perfis.md](docs/autenticacao-e-perfis.md)).

Para Minikube e AWS, ver os guias de execução em [docs/](docs/README.md).

## Documentação

| Documento | Assunto |
|---|---|
| [docs/arquitetura.md](docs/arquitetura.md) | Clean Architecture, módulos, fluxo de dados, stack, PostgreSQL |
| [docs/execucao-local.md](docs/execucao-local.md) | Rodar localmente (Docker Compose / Minikube) |
| [docs/deploy-aws.md](docs/deploy-aws.md) | Deploy manual na AWS (EKS + RDS via repos de infra) |
| [docs/cicd-github-actions.md](docs/cicd-github-actions.md) | Deploy automatizado via GitHub Actions |
| [docs/autenticacao-e-perfis.md](docs/autenticacao-e-perfis.md) | JWT, login e perfis de acesso |
| [docs/testes-e-qualidade.md](docs/testes-e-qualidade.md) | Testes, Bruno, JaCoCo e SonarQube |
| [docs/monitoramento-e-observabilidade.md](docs/monitoramento-e-observabilidade.md) | Monitoramento, New Relic, OpenTelemetry, Dashboards e Alertas |
| [docs/entrega.md](docs/entrega.md) | Dados do grupo e links da entrega |

## Testes

```bash
./mvnw test
```

Os testes usam H2 em memória — não é necessário ter o PostgreSQL rodando. Detalhes da
estratégia por módulo e cobertura em [docs/testes-e-qualidade.md](docs/testes-e-qualidade.md).

## Licença

Distribuído sob a licença [MIT](https://opensource.org/licenses/MIT).
