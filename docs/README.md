# 📚 Documentação — Oficina API

Índice da documentação do projeto. Para uma visão geral rápida, comece pelo
[README principal](../README.md).

## Arquitetura e decisões
- [ecossistema.md](ecossistema.md) — Visão completa da Fase 4: microsserviços, bancos, mensageria, Saga coreografada, justificativa das tecnologias e ordem de deploy.
- [arquitetura.md](arquitetura.md) — Clean Architecture, módulos, fluxo de dados.
- [mensageria.md](mensageria.md) — SNS/SQS, outbox, consumidor idempotente e LocalStack.
- [saga.md](saga.md) — Saga da OS: máquina de estados, compensações e histórico.
- [banco-de-dados.md](banco-de-dados.md) — Justificativa do PostgreSQL, diagrama ER e ciclo de vida da OS.

## ADRs (Architecture Decision Records)
- [adr/002-escolha-opentelemetry-newrelic.md](adr/002-escolha-opentelemetry-newrelic.md) — Escolha do OpenTelemetry + New Relic para Observabilidade
- [adr/003-uso-de-hpa.md](adr/003-uso-de-hpa.md) — Uso de HPA (Horizontal Pod Autoscaler)
- [adr/004-padrao-comunicacao-rest.md](adr/004-padrao-comunicacao-rest.md) — Padrão de Comunicação via API REST
- [adr/005-saga-coreografada.md](adr/005-saga-coreografada.md) — Saga coreografada para o ciclo de vida da OS

## RFCs (Request for Comments)
- [rfc/rfc-001-escolha-nuvem-aws.md](rfc/rfc-001-escolha-nuvem-aws.md) — Escolha da Nuvem AWS como Provedor Cloud
- [rfc/rfc-002-estrategia-autenticacao.md](rfc/rfc-002-estrategia-autenticacao.md) — Estratégia de Autenticação (Lambda Edge)

## Execução
- [execucao-local.md](execucao-local.md) — rodar localmente com Docker Compose ou Minikube.
- [deploy-aws.md](deploy-aws.md) — deploy manual na AWS (EKS + RDS via Terraform).
- [cicd-github-actions.md](cicd-github-actions.md) — deploy automatizado via GitHub Actions.

## Monitoramento e Qualidade
- [monitoramento-e-observabilidade.md](monitoramento-e-observabilidade.md) — OpenTelemetry, New Relic, dashboards NRQL, alertas e logs.
- [autenticacao-e-perfis.md](autenticacao-e-perfis.md) — JWT, login e perfis de acesso.
- [testes-e-qualidade.md](testes-e-qualidade.md) — testes, BDD, Bruno, JaCoCo e SonarCloud.

## Entrega
- [status-fase-4.md](status-fase-4.md) — **comece por aqui para continuar o trabalho:** o que está pronto, decisões fechadas e o que falta para entregar.
- [entrega.md](entrega.md) — conteúdo do PDF da Fase 4: participantes, repositórios, vídeo, arquitetura, Saga e justificativas.
