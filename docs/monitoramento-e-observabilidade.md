# 📊 Monitoramento e Observabilidade (OpenTelemetry + New Relic)

Guia completo da implementação de observabilidade da **Oficina API**, cobrindo métricas, traces distribuídos, logs estruturados correlacionados, monitoramento de infraestrutura (Kubernetes) e configuração de dashboards e alertas no **New Relic**.

---

## 🏛️ 1. Arquitetura da Solução

A observabilidade foi implementada utilizando o padrão **OpenTelemetry (OTel)** com o agente Java injetado via container ([Dockerfile](../Dockerfile)), exportando dados via **OTLP (OpenTelemetry Protocol)** diretamente para a **New Relic**.

```mermaid
flowchart LR
    subgraph AppContainer["Container oficina-api"]
        App["Spring Boot (Java 25)"]
        Agent["OpenTelemetry Java Agent"]
        App -->|Auto-Instrumentação| Agent
    end

    subgraph K8s["Cluster Kubernetes / Docker Compose"]
        Health["/actuator/health (Probes)"]
        Metrics["Métricas de CPU / Memória"]
    end

    subgraph NewRelic["New Relic Cloud"]
        OTLP["OTLP Endpoint (otlp.nr-data.net:4318)"]
        APM["APM & Distributed Tracing"]
        Logs["Logs in Context"]
        Dashboards["Dashboards & Métricas"]
        Alerts["Alertas de Falha"]
    end

    Agent -->|Traces, Métricas e Logs (HTTP/Protobuf)| OTLP
    OTLP --> APM
    OTLP --> Logs
    OTLP --> Dashboards
    OTLP --> Alerts
```

### Principais Vantagens:
- **Agnóstico:** A aplicação não depende do SDK proprietário da New Relic no código Java.
- **Logs in Context:** Os logs do Spring Boot / Logback são enriquecidos automaticamente com `trace_id` e `span_id`.
- **Zero Overhead de Código:** Auto-instrumentação via `opentelemetry-javaagent.jar` no runtime.

---

## 🚀 2. Como Executar e Testar Localmente

### Pré-requisitos
- Preencher a variável `NEW_RELIC_LICENSE_KEY` no arquivo `.env`:
  ```env
  POSTGRES_USER=postgres
  POSTGRES_PASSWORD=abc123
  POSTGRES_DB=oficina_db
  NEW_RELIC_LICENSE_KEY=sua_chave_ingest_licence_key_aqui
  ```

### Executar via Docker Compose
```bash
docker compose up --build
# ou
make start
```

### Gerar Tráfego de Teste (Popular Métricas)
1. **Via Bruno:** Abra a pasta [`src/test/bruno/caminho-feliz`](../src/test/bruno/caminho-feliz) e execute a suíte de requests (criação de OS, orçamentos, aprovações, execuções e finalizações).
2. **Via Swagger:** Acesse [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) e realize operações autenticadas.
3. **Via Script de Carga Rápida (PowerShell):**
   ```powershell
   # Healthcheck
   1..20 | ForEach-Object { Invoke-RestMethod -Uri "http://localhost:8080/actuator/health" }

   # Login & Operações
   $auth = Invoke-RestMethod -Uri "http://localhost:8080/auth/login" -Method Post `
     -ContentType "application/json" -Body '{"email":"admin@oficina.com","senha":"admin123"}'
   $headers = @{ Authorization = "Bearer $($auth.token)" }

   1..10 | ForEach-Object {
       Invoke-RestMethod -Uri "http://localhost:8080/ordem-servico" -Headers $headers
   }
   ```

---

## 📈 3. Consultas NRQL para o Dashboard

No New Relic, crie um novo **Dashboard** e adicione os seguintes widgets utilizando as queries **NRQL** abaixo:

### 3.1. Latência das APIs
- **Tipo de Widget:** Line Chart / Area Chart
- **Objetivo:** Acompanhar tempo de resposta médio, percentil 95 (P95) e 99 (P99).
```sql
SELECT average(duration) AS 'Latência Média (s)', 
       percentile(duration, 95) AS 'P95', 
       percentile(duration, 99) AS 'P99' 
FROM Transaction 
WHERE appName = 'oficina-api' 
TIMESERIES
```

### 3.2. Consumo de Recursos (CPU e Memória)
- **Tipo de Widget:** Line Chart
- **Objetivo:** Monitorar consumo de memória e utilização de CPU da aplicação.
```sql
SELECT average(process.cpu.utilization) * 100 AS '% CPU', 
       average(jvm.memory.used) / 1e6 AS 'Memória Usada (MB)' 
FROM Metric 
WHERE appName = 'oficina-api' 
TIMESERIES
```
*(Para ambiente Kubernetes com New Relic K8s Integration: `SELECT average(containerCpuPercent), average(containerMemoryUsageBytes) / 1e6 FROM K8sContainerSample WHERE containerName = 'oficina-api' TIMESERIES`)*

### 3.3. Healthchecks e Uptime
- **Tipo de Widget:** Billboard / Gauge
- **Objetivo:** Monitorar a taxa de sucesso e disponibilidade dos endpoints de saúde (`/actuator/health`).
```sql
SELECT percentage(count(*), WHERE httpResponseCode = 200) AS '% Uptime (Healthcheck)' 
FROM Transaction 
WHERE request.uri = '/actuator/health' 
TIMESERIES
```

### 3.4. Volume Diário de Ordens de Serviço
- **Tipo de Widget:** Bar Chart / Billboard
- **Objetivo:** Contabilizar a quantidade diária de novas ordens de serviço criadas com sucesso (HTTP 201).
```sql
SELECT count(*) AS 'Total de OS Criadas' 
FROM Transaction 
WHERE request.uri = '/ordem-servico' 
  AND http.method = 'POST' 
  AND httpResponseCode = 201 
SINCE 7 days ago 
TIMESERIES 1 day
```

### 3.5. Tempo Médio de Execução por Status
- **Tipo de Widget:** Table / Multi-Line Chart
- **Objetivo:** Acompanhar a duração das transições de status da OS (Diagnóstico, Execução e Finalização).
```sql
SELECT average(duration) AS 'Tempo Médio (s)', count(*) AS 'Qtd Chamadas' 
FROM Transaction 
WHERE request.uri IN (
  '/ordem-servico/{id}/iniciar-diagnostico',
  '/ordem-servico/{id}/iniciar-execucao',
  '/ordem-servico/{id}/finalizar',
  '/ordem-servico/{id}/entregar'
) 
FACET request.uri 
TIMESERIES
```

### 3.6. Erros e Falhas nas Integrações / Webhooks
- **Tipo de Widget:** Pie Chart / Table
- **Objetivo:** Exibir falhas de integrações externas (Webhooks, Banco de Dados, Envio de E-mail).
```sql
SELECT count(*) AS 'Total Falhas' 
FROM TransactionError 
WHERE appName = 'oficina-api' 
FACET error.class, error.message, `response.status` 
TIMESERIES
```
*(Para falhas específicas de Webhooks: `SELECT count(*) FROM Transaction WHERE request.uri LIKE '%/webhook%' AND httpResponseCode >= 400 FACET httpResponseCode, request.uri TIMESERIES`)*

---

## 🔔 4. Alertas de Falha no Processamento de OS

Para atender ao requisito de **Alertas para falhas no processamento de ordens de serviço**:

1. No New Relic, vá em **Alerts & AI** $\rightarrow$ **Alert Conditions (Policies)** $\rightarrow$ **Create Policy**.
2. **Condição 1: Taxa de Erro em Ordens de Serviço (Crítico)**:
   - **NRQL:**
     ```sql
     SELECT count(*) FROM TransactionError WHERE request.uri LIKE '%/ordem-servico%'
     ```
   - **Threshold:** Disparar alerta se o valor for `> 0` por `5 minutos consecutivos`.
3. **Condição 2: Falha no Healthcheck / Pod Down**:
   - **NRQL:**
     ```sql
     SELECT count(*) FROM Transaction WHERE request.uri = '/actuator/health' AND httpResponseCode != 200
     ```
   - **Threshold:** Disparar alerta se o valor for `> 2` em `1 minuto`.
4. **Destinos de Notificação:** Configurar E-mail, Slack ou Webhook em **Destinations**.

---

## 🪵 5. Logs Estruturados (JSON) e Correlação de Requisições

### Como funciona a correlação (Distributed Tracing + Logs in Context)
1. Quando uma requisição HTTP chega na API, o **OpenTelemetry Java Agent** gera um `trace_id` e um `span_id` únicos.
2. Esses identificadores são propagados no contexto da thread (**MDC**).
3. Todas as mensagens de log emitidas pela aplicação recebem automaticamente os campos estruturados:
   - `trace.id`
   - `span.id`
   - `service.name: oficina-api`
   - `log.level`
4. **Visualização no New Relic:**
   - Ao inspecionar uma transação com erro em **APM $\rightarrow$ Distributed Tracing**, basta clicar em **Logs in context** para ver todos os logs exatos gerados por aquela requisição sem misturar com logs de outras chamadas concorrentes.
