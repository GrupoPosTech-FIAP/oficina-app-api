# 📚 Documentação — Oficina API

Índice da documentação do projeto. Para uma visão geral rápida, comece pelo
[README principal](../README.md).

## Arquitetura e decisões
- [ecossistema.md](ecossistema.md) — Visão completa (diagrama de nuvem, fluxo de autenticação, dependências Terraform).
- [arquitetura.md](arquitetura.md) — Clean Architecture, módulos, fluxo de dados.
- [banco-de-dados.md](banco-de-dados.md) — Justificativa do PostgreSQL, diagrama ER e ciclo de vida da OS.

## ADRs (Architecture Decision Records)
- [adr/002-escolha-opentelemetry-newrelic.md](adr/002-escolha-opentelemetry-newrelic.md) — Escolha do OpenTelemetry + New Relic para Observabilidade
- [adr/003-uso-de-hpa.md](adr/003-uso-de-hpa.md) — Uso de HPA (Horizontal Pod Autoscaler)
- [adr/004-padrao-comunicacao-rest.md](adr/004-padrao-comunicacao-rest.md) — Padrão de Comunicação via API REST

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
- [testes-e-qualidade.md](testes-e-qualidade.md) — testes, Bruno, JaCoCo e SonarQube.

## Entrega
- [entrega.md](entrega.md) — dados do grupo e links da entrega (Tech Challenge FIAP).
