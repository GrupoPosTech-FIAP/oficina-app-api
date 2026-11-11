#!/bin/bash
# Cria no LocalStack a mesma topologia de mensageria do Terraform
# (oficina-infra-cluster/messaging.tf): um tópico SNS por produtor, uma fila SQS + DLQ
# por consumidor e as assinaturas com filter policy por eventType.
# Executado automaticamente pelo LocalStack quando o container fica pronto (ready.d).
set -euo pipefail

REGIAO=us-east-1
CONTA=000000000000

for servico in os billing inventory execution; do
  awslocal sns create-topic --name "oficina-${servico}-events" --region "$REGIAO" >/dev/null
done

for servico in os billing inventory execution; do
  awslocal sqs create-queue --queue-name "oficina-${servico}-dlq" --region "$REGIAO" >/dev/null
  awslocal sqs create-queue --queue-name "oficina-${servico}-queue" --region "$REGIAO" \
    --attributes "{\"RedrivePolicy\":\"{\\\"deadLetterTargetArn\\\":\\\"arn:aws:sqs:${REGIAO}:${CONTA}:oficina-${servico}-dlq\\\",\\\"maxReceiveCount\\\":\\\"5\\\"}\",\"VisibilityTimeout\":\"60\"}" >/dev/null
done

# assinar <produtor> <fila> <eventos separados por vírgula>
assinar() {
  local produtor=$1 fila=$2 eventos=$3
  local filtro
  # cada evento vira \"Nome\", já escapado para ficar dentro da string FilterPolicy
  filtro=$(printf '\\"%s\\",' ${eventos//,/ })
  awslocal sns subscribe --region "$REGIAO" \
    --topic-arn "arn:aws:sns:${REGIAO}:${CONTA}:oficina-${produtor}-events" \
    --protocol sqs \
    --notification-endpoint "arn:aws:sqs:${REGIAO}:${CONTA}:oficina-${fila}-queue" \
    --attributes "{\"RawMessageDelivery\":\"true\",\"FilterPolicy\":\"{\\\"eventType\\\":[${filtro%,}]}\"}" >/dev/null
}

# enquanto não existe o oficina-execution-service, a execução roda na OS e ExecucaoFalhou sai do tópico da OS
assinar os        billing   "DiagnosticoConcluido,ExecucaoFalhou"
assinar inventory billing   "EstoqueBaixado"
assinar execution billing   "ExecucaoFalhou"

assinar billing   inventory "OrcamentoAprovado,PagamentoRecusado,PagamentoExpirado"
assinar os        inventory "ExecucaoFalhou"
assinar execution inventory "ExecucaoFalhou"

assinar billing   os        "OrcamentoGerado,OrcamentoAprovado,OrcamentoNegado,OrcamentoExpirado,CobrancaCriada,PagamentoConfirmado,PagamentoRecusado,PagamentoExpirado,PagamentoEstornado"
assinar inventory os        "EstoqueBaixado,EstoqueInsuficiente,EstoqueEstornado"
assinar execution os        "ExecucaoIniciada,ExecucaoConcluida,ExecucaoFalhou"

assinar billing   execution "PagamentoConfirmado"

touch /tmp/mensageria-pronta # usado pelo healthcheck do docker-compose
echo "[mensageria] tópicos, filas e assinaturas criados"
