# 004 — Padrão de Comunicação via API REST

**Status:** Aceita
**Data:** 2024-05-20

## Resumo
O ecossistema Oficina foi projetado utilizando o padrão arquitetural de APIs RESTful
com JSON sobre HTTP para a comunicação síncrona entre o cliente (front-end/mobile) e o
backend, bem como para integrações externas.

## Problema
Para a fase atual do projeto, o sistema exige interações síncronas diretas de
criação, leitura, atualização e exclusão (CRUD) de ordens de serviço, clientes e veículos.
Foi necessário definir um padrão de comunicação que fosse universal, interoperável
e de fácil consumo.

## Proposta técnica
- Adoção de **REST** Nível 2 de Maturidade de Richardson (uso de verbos HTTP adequados:
  `GET`, `POST`, `PUT`, `PATCH`, `DELETE`).
- Payloads padronizados em **JSON**.
- Códigos de status HTTP semânticos (200, 201, 400, 401, 403, 404, 429, 500).
- Documentação interativa via **Swagger/OpenAPI 3.0** (`/swagger-ui.html`).
- A comunicação entre o API Gateway e a aplicação no EKS ocorre via Proxy HTTP direto,
  preservando headers (como o `Authorization: Bearer`).

## Impacto esperado
- **Ganhos:** Facilidade de integração com qualquer tecnologia de front-end, clareza no
  contrato da API via Swagger e baixa curva de aprendizado para a equipe.
- **Riscos:** Chamadas excessivas podem sobrecarregar a API (mitigado pelo API Gateway).

## Alternativas consideradas
- **GraphQL:** descartado pela complexidade desnecessária para o domínio atual, onde as
  telas (clientes, ordens de serviço) exigem conjuntos de dados fixos.
- **gRPC / Protobuf:** excelente para comunicação interna (usado no OpenTelemetry), mas
  descartado para a borda externa por ser mais complexo para consumo web/mobile padrão e
  exigir clientes gerados.
- **Mensageria Assíncrona (RabbitMQ/SQS):** para as requisições de usuário, a resposta deve
  ser imediata (síncrona). Mensageria será introduzida em fases futuras se houver processos
  de longa duração.

## Pontos em aberto
- Padronização do formato de erro (Error Responses) na API (ex: adotar a RFC 7807 - Problem Details).
