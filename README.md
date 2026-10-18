# MercadoExpress

API de comercio eletronico desenvolvida em **Java 21** e **Spring Boot 4.1.1**, com autenticacao JWT, administracao de produtos, criacao de pedidos, controle de estoque, processamento assincrono de pagamentos e notificacoes por email.

O processamento bancario e simulado por outra aplicacao, **BancoTranquilo**, executada separadamente. O MercadoExpress utiliza **Transactional Outbox** para persistir as solicitacoes de pagamento junto com os pedidos antes de publica-las no RabbitMQ.

## Tecnologias

- Java 21, Spring Boot 4.1.1 e Maven Wrapper (`./mvnw`)
- Spring Web MVC, Spring Security e JWT
- Spring Data JPA / Hibernate e PostgreSQL 17
- RabbitMQ (mensageria, publisher confirms, retry e DLQ)
- Docker e Docker Compose
- Mailpit para visualizar emails em ambiente de desenvolvimento
- JUnit 5 e Mockito para testes automatizados

## Arquitetura e fluxo de pagamento

1. Um cliente autenticado envia um pedido para `POST /api/orders`.
2. Na **mesma transacao PostgreSQL**, o MercadoExpress grava o pedido, atualiza o estoque e registra um evento em `outbox_events` com status `PENDING`.
3. O `OutboxPublisher` procura periodicamente eventos pendentes; o `OutboxEventProcessor` publica a solicitacao na exchange do RabbitMQ, usando mensagens persistentes e publisher confirms.
4. Apos a confirmacao de publicacao, o evento passa para `PUBLISHED`. Em caso de falha, permanece `PENDING` e pode ser tentado novamente.
5. O `PaymentRequestConsumer` consulta a API HTTP do BancoTranquilo e publica o resultado recebido em uma fila de aprovacao ou rejeicao.
6. O MercadoExpress atualiza o pedido de acordo com o resultado bancario e dispara a notificacao de pagamento por email, visualizavel no Mailpit.

O BancoTranquilo implementa **idempotencia por `orderId`**, mantendo uma unica transacao para solicitacoes equivalentes, rejeitando com HTTP 409 tentativas de reutilizar o pedido com valor diferente. Isso e essencial porque eventos podem ser entregues mais de uma vez.

**Importante:** o Outbox garante a persistencia e a retomada da **solicitacao** de pagamento; ele nao oferece semantica exactly-once nem, sozinho, a mesma garantia para todas as mensagens de retorno do banco. A DLQ permite investigar mensagens que esgotaram as tentativas de processamento.

## Pre-requisitos

- Java 21 para o MercadoExpress
- PostgreSQL acessivel na maquina, com banco `mercado_express` e usuario com permissoes de criacao/alteracao de tabelas
- Docker Desktop e Docker Compose
- BancoTranquilo iniciado e acessivel na porta **8081** do host (com seu proprio banco PostgreSQL `banco_tranquilo`)

No ambiente usado no desenvolvimento, o PostgreSQL e o BancoTranquilo rodam no Mac. O container do MercadoExpress acessa os dois por `host.docker.internal`.

## Configurar as variaveis de ambiente

Crie o arquivo local de ambiente:

```bash
cp .env.example .env
```

Edite `.env` para inserir as senhas reais de PostgreSQL e RabbitMQ e um segredo JWT aleatorio e forte. Para gerar o JWT_SECRET, voce pode usar:

```bash
openssl rand -base64 48
```

O arquivo `.env` esta no `.gitignore` e **nao deve ser enviado ao Git**. O `.env.example` usa apenas valores ilustrativos.

O `DB_URL` do `.env` e utilizado ao executar a API fora do Docker. No `docker-compose.yml`, o servico `mercadoexpress` substitui essa URL por `jdbc:postgresql://host.docker.internal:5432/mercado_express`.

## Subir a infraestrutura e a API

Inicie o BancoTranquilo no host, usando as configuracoes de banco desse projeto. Na raiz do MercadoExpress:

```bash
set -a
source .env
set +a

# RabbitMQ e servidor SMTP de desenvolvimento
docker compose up -d rabbitmq mailpit

# Politica da Dead Letter Queue (idempotente)
bash scripts/setup-rabbitmq-dlq.sh

# Compilar, testar e empacotar o MercadoExpress
./mvnw clean package

# Gerar a imagem da aplicacao
docker build -t mercadoexpress-api:1.0 .

# Subir somente a API (preservando os outros containers)
docker compose up -d --no-deps --force-recreate mercadoexpress

docker compose ps
```

O Compose define `OUTBOX_ENABLED=true` e `BANK_API_BASE_URL=http://host.docker.internal:8081` no servico da API. Sem o Outbox habilitado, novos pedidos permanecem com eventos pendentes e nao seguem para pagamento.

Para examinar os logs:

```bash
docker logs --tail 100 mercadoexpress-api
```

**Atencao a duas instancias na porta 8080:** se o MercadoExpress tambem estiver aberto no IntelliJ, ele pode responder pelo IPv6 (`localhost`), enquanto o Docker responde por `127.0.0.1` (IPv4). Pare a instancia local antiga e utilize `http://127.0.0.1:8080` nos testes da API do container.

## Endpoints confirmados

| Metodo | Rota | Funcao |
| --- | --- | --- |
| POST | `/api/auth/register` | Cadastrar usuario |
| POST | `/api/auth/login` | Autenticar e receber JWT (`accessToken`) |
| POST | `/api/orders` | Criar pedido autenticado |

A API tambem contem controllers para produtos publicos, produtos administrativos e dashboard. Para consultar as rotas e os modelos exatos, utilize a documentacao OpenAPI disponivel no ambiente local:

- Swagger UI: `http://127.0.0.1:8080/swagger-ui.html`
- OpenAPI JSON: `http://127.0.0.1:8080/v3/api-docs`

### Exemplo: criar pedido

Envie um JWT de cliente no cabecalho `Authorization: Bearer <TOKEN>`:

```http
POST /api/orders
Content-Type: application/json
Authorization: Bearer <TOKEN>

{
  "items": [
    { "productId": 2, "quantity": 1 }
  ]
}
```

A criacao retorna HTTP 201 e o estado inicial `PENDING_PAYMENT`. O resultado final do pedido e atualizado assincronamente.

## Infraestrutura local

| Servico | Endereco |
| --- | --- |
| MercadoExpress | `http://127.0.0.1:8080` |
| BancoTranquilo (host) | `http://127.0.0.1:8081` |
| RabbitMQ Management | `http://127.0.0.1:15672` |
| Mailpit | `http://127.0.0.1:8025` |
| PostgreSQL (host) | `localhost:5432` |

O usuario e a senha do painel RabbitMQ sao os definidos no `.env`.

## Configuracao do RabbitMQ e DLQ

O `RabbitMQConfig.java` declara exchanges, filas duraveis e bindings para os eventos de solicitacao e resposta:

| Fila | Finalidade |
| --- | --- |
| `mercadoexpress.payment.queue` | Solicitacoes enviadas ao BancoTranquilo |
| `mercadoexpress.payment.approved.queue` | Respostas de pagamentos aprovados |
| `mercadoexpress.payment.rejected.queue` | Respostas de pagamentos recusados |
| `mercadoexpress.payment.dlq` | Mensagens que falharam no processamento |

Exchange principal: `mercadoexpress.payment.exchange`.

Exchange de dead letter: `mercadoexpress.payment.dlx`, com routing key `payment.failed`.

A politica de DLQ precisa existir no broker; ela e configurada pelo script:

```bash
bash scripts/setup-rabbitmq-dlq.sh
```

O script associa as tres filas de pagamento a exchange de dead letter e pode ser executado novamente apos recriar o ambiente. Os parametros de retry dos listeners estao definidos em `src/main/resources/application.properties`.

## Transactional Outbox

Eventos de pagamento sao gravados na tabela `outbox_events`. Os campos principais sao `order_id`, `payload`, `status`, `attempts`, `created_at` e `published_at`.

```bash
psql -h localhost -U "$DB_USERNAME" -d mercado_express \
  -c 'SELECT id, order_id, status, attempts, published_at FROM outbox_events ORDER BY id DESC LIMIT 10;'
```

- `PENDING`: evento ainda nao publicado com confirmacao do broker
- `PUBLISHED`: RabbitMQ confirmou a publicacao
- `attempts`: tentativas de publicacao com falha registradas

O agendador consulta os pendentes periodicamente (configuracao local: 5 segundos). O status `PUBLISHED` nao significa, isoladamente, que o pagamento foi aprovado; verifique o status final do pedido e a transacao bancaria.

## Testes e validacoes realizadas

Para executar os testes do MercadoExpress:

```bash
set -a
source .env
set +a
./mvnw clean test
```

**Resultados obtidos no ambiente de desenvolvimento (18/10/2026):**

- MercadoExpress: **48 testes**, sem falhas
- BancoTranquilo: **11 testes**, sem falhas
- Criacao de pedido, aprovacao bancaria, alteracao de estoque e notificacao de pagamento verificados
- Idempotencia do BancoTranquilo validada com solicitacoes repetidas e cinco solicitacoes simultaneas; apenas uma transacao persistida por `orderId`
- RabbitMQ com Dead Letter Queue validada por teste controlado de mensagem invalida
- Pedido #8: evento `PUBLISHED`, pedido `PAID`, valor R$ 3.200, estoque reduzido e notificacao recebida no Mailpit
- Pedido #9: criado durante indisponibilidade do RabbitMQ; evento permaneceu `PENDING`. Apos a retomada do broker, foi para `PUBLISHED` e o pedido terminou `PAID`, com uma falha de publicacao registrada em `attempts`

O envio pelo Mailpit e uma simulacao de SMTP: o recebimento na caixa do Mailpit nao comprova entrega em uma caixa de email externa.

## Observacoes de ambiente

- O BancoTranquilo e o PostgreSQL nao estao definidos como servicos neste Compose; precisam estar executando separadamente no host.
- `spring.jpa.hibernate.ddl-auto=update` e adequado para a demonstracao local, mas migracoes versionadas (como Flyway) sao recomendadas em producao.
- O `RabbitMQConfig.java` define as filas e bindings; o script define a politica no broker, necessaria para o roteamento de mensagens com falha para a DLQ.
- O historico do projeto inclui um pedido antigo `PENDING_PAYMENT` anterior ao Outbox; ele nao deve ser republicado sem verificar previamente a eventual transacao bancaria.
- Nunca versione `.env`, tokens JWT ou credenciais.
