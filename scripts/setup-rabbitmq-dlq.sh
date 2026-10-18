#!/usr/bin/env bash
set -euo pipefail

CONTAINER="${RABBITMQ_CONTAINER:-mercadoexpress-rabbitmq}"
POLICY_NAME="mercadoexpress-dlx"
QUEUE_PATTERN='^mercadoexpress\.payment\.(queue|approved\.queue|rejected\.queue)$'
POLICY_DEFINITION='{"dead-letter-exchange":"mercadoexpress.payment.dlx","dead-letter-routing-key":"payment.failed"}'

if ! docker inspect --format '{{.State.Running}}' "$CONTAINER" 2>/dev/null | grep -qx true; then
  echo "Erro: o container RabbitMQ '$CONTAINER' nao esta em execucao." >&2
  echo "Inicie primeiro com: docker compose up -d rabbitmq" >&2
  exit 1
fi

echo "Configurando politica DLQ no container: $CONTAINER"
docker exec "$CONTAINER" rabbitmqctl set_policy -p / \
  --apply-to queues \
  "$POLICY_NAME" \
  "$QUEUE_PATTERN" \
  "$POLICY_DEFINITION"

echo
echo 'Politicas configuradas no RabbitMQ:'
docker exec "$CONTAINER" rabbitmqctl list_policies -p /

echo
echo 'Politica aplicada. Verifique se as exchanges e filas da aplicacao foram declaradas.'
