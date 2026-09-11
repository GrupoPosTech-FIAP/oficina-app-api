# 003 — Uso de HPA (Horizontal Pod Autoscaler)

**Status:** Aceita
**Data:** 2024-05-20

## Resumo
Foi decidido implementar o Horizontal Pod Autoscaler (HPA) no cluster Kubernetes para o
Deployment da `oficina-api`, com escalabilidade baseada no consumo de CPU (target 70%) e
Memória (target 80%), permitindo variar entre 1 e 5 réplicas dinamicamente.

## Problema
O tráfego de uma oficina mecânica não é constante. Existem picos de acesso (ex: início da
manhã quando os clientes deixam os carros, e final da tarde na retirada).
Manter um número fixo elevado de pods consumiria recursos desnecessários do cluster
(desperdício de dinheiro), enquanto manter apenas um pod poderia causar lentidão ou
indisponibilidade durante os picos.

## Proposta técnica
- Configuração do HPA (`k8s/base/app-hpa.yaml`) apontando para o Deployment `oficina-api`.
- Definição de limites claros no contêiner (requests: CPU 250m / Mem 512Mi; limits: CPU 500m / Mem 768Mi).
- Escalonamento configurado para:
  - Mínimo de 1 réplica (para reduzir custos em horários de baixo tráfego).
  - Máximo de 5 réplicas (limite seguro para o cluster atual `t3.medium`).
  - Escalonar ("scale out") quando CPU média atingir 70% ou Memória atingir 80%.

## Impacto esperado
- **Ganhos:** Alta disponibilidade nos picos de tráfego, resiliência a picos repentinos
  e otimização de custo nos períodos ociosos.
- **Riscos:** Se a aplicação não for stateless, o balanceamento de carga entre réplicas
  causaria erros (mitigado: a aplicação é stateless, sessão é via JWT e banco é externo).
- **Restrições:** Exige que o Metrics Server esteja instalado no EKS para coletar as
  métricas de CPU e Memória.

## Alternativas consideradas
- **Vertical Pod Autoscaler (VPA):** descartado porque reiniciar o pod para dar mais CPU/RAM
  causa downtime momentâneo, e o Spring Boot demora alguns segundos para inicializar.
- **Escalonamento estático (sempre 3 réplicas):** descartado pelo desperdício de recursos
  de madrugada e fins de semana.

## Pontos em aberto
- Nenhum. O Metrics Server deve ser habilitado na infraestrutura para o HPA funcionar.
