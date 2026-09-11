# 002 — Escolha do OpenTelemetry + New Relic para Observabilidade

**Status:** Aceita
**Data:** 2025-06-01

## Resumo
Para atender aos requisitos de monitoramento e observabilidade da Fase 3 do Tech Challenge,
foi decidido instrumentar a aplicação de forma agnóstica com **OpenTelemetry** (auto-instrumentação
via Java Agent) e utilizar o **New Relic** como backend para armazenamento e visualização dos dados
(traces, métricas e logs).

## Problema
A aplicação não possuía nenhuma ferramenta de monitoramento. Era necessário:
- Monitorar o ambiente e detectar gargalos em tempo real.
- Rastrear requisições de ponta a ponta (distributed tracing).
- Correlacionar logs com traces específicos.
- Criar dashboards e alertas para falhas no processamento de ordens de serviço.

## Proposta técnica
- **Instrumentação:** Utilização do Java Agent do OpenTelemetry injetado via `Dockerfile`.
  Esta abordagem permite a auto-instrumentação (captura automática de logs, métricas e traces)
  sem a necessidade de alterações no código-fonte Java/Maven.
- **Protocolo:** Exportação de dados via OTLP/Protobuf através da porta 4318 (HTTP),
  garantindo compatibilidade com redes externas e firewalls.
- **Backend:** Utilização do New Relic como receptor de dados, utilizando as credenciais de
  Ingest License Key gerenciadas via GitHub Organization Secrets para evitar exposição de chaves.
- **Ambiente de desenvolvimento:** Uso de Docker Compose com variáveis de ambiente injetadas
  via arquivo `.env` (ignorado pelo Git).

## Impacto esperado
- Visibilidade completa do rastreamento de cada requisição (distributed tracing).
- OpenTelemetry é agnóstico — é possível trocar o backend (New Relic → Datadog, Grafana, etc.)
  sem alterar a instrumentação.
- Baixo acoplamento do código: nenhuma nova dependência Maven foi inserida no projeto.
- Zero overhead de desenvolvimento: auto-instrumentação captura tudo automaticamente.

## Alternativas consideradas
- **Utilizar o agente proprietário do New Relic:** descartado pelo alto acoplamento com o
  fornecedor, violando o princípio de agnósticismo.
- **Instrumentação via SDK do OpenTelemetry:** descartada pela complexidade adicional de
  configurar cada ponto de instrumentação manualmente, quando o Java Agent já cobre todos
  os frameworks utilizados (Spring, Hibernate, JDBC).

## Pontos em aberto
- Estratégias de retenção dos logs no New Relic (plano gratuito tem limite).
- Como compartilhar o acesso ao dashboard do New Relic com outros membros do grupo,
  já que a License Key está associada a uma conta individual.