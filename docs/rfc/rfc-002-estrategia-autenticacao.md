# RFC 002 — Estratégia de Autenticação

**Status:** Aceita
**Data:** 2024-05-15

## Resumo
Proposta para implementar um modelo de autenticação baseado apenas em CPF para os clientes da
oficina mecânica, com geração de token JWT. A lógica de autenticação e autorização
será externalizada da aplicação principal (Spring Boot) e executada na camada de borda
(API Gateway + Lambda).

## Contexto e Motivação
A aplicação possui três domínios principais: clientes, veículos e ordens de serviço.
O requisito do desafio especifica que **o cliente deve se identificar apenas pelo CPF**
("CPF entra, JWT sai"), sem a criação de senhas complexas ou segundo fator de autenticação.

Além disso, a arquitetura moderna propõe proteger os recursos computacionais. Deixar que a
aplicação no cluster EKS receba todas as requisições não autorizadas para validá-las
internamente consumiria CPU e Memória, prejudicando o HPA em momentos de pico de ataques.

## Opções Avaliadas

### 1. Autenticação interna (Spring Security)
- **Como funciona:** A rota `/auth/login` e o filtro de JWT residem dentro da aplicação
  Spring Boot no EKS.
- **Prós:** Tudo no mesmo código, fácil de testar localmente.
- **Contras:** O cluster recebe tráfego lixo/não-autenticado, exigindo que o pod desperdice
  recursos rejeitando requisições com 401. Dificulta a aplicação de Rate Limiting por IP
  direto no banco de dados, pois o tráfego de borda bateria no Load Balancer.

### 2. Autenticação na borda (API Gateway + Lambda) — [ESCOLHIDA]
- **Como funciona:** O API Gateway da AWS é a porta de entrada. Uma função Lambda (Node.js)
  recebe o CPF, consulta o banco, assina o JWT e o devolve. Para as demais rotas da API,
  um Lambda Authorizer valida o JWT antes de repassar a requisição ao EKS.
- **Prós:** O cluster só recebe tráfego legítimo (100% autorizado). A API Gateway absorve
  ataques de negação de serviço e requisições maliciosas. Rate limiting é facilmente
  implementado na borda com DynamoDB.
- **Contras:** Aumenta a complexidade de infraestrutura (necessidade de outro repositório,
  Terraform próprio) e exige que os Secrets (como a chave do JWT) sejam sincronizados entre
  os repositórios.

## Detalhamento da Escolha
A Opção 2 foi escolhida pois protege o cluster EKS, adere a padrões arquiteturais corporativos
e implementa separação de responsabilidades (Gateway cuida de segurança, Spring Boot cuida das
regras de negócio da oficina).

O contrato proposto é:
- Rota pública `POST /auth`: recebe `{"cpf": "..."}` e retorna `{"token": "..."}`.
- Rota protegida `ANY /app/*`: exige header `Authorization: Bearer <token>`. O API Gateway
  valida o token; se válido, encaminha a requisição via Proxy HTTP para a porta 8080 do
  LoadBalancer do cluster.

## Impactos da Decisão
- Necessidade de criar e manter o repositório `oficina-auth-gateway`.
- A Lambda responsável pelo login precisa ser implantada **dentro da VPC** para conseguir
  acessar o banco RDS e consultar o CPF.
- O Lambda Authorizer deve ser implantado **fora da VPC** para minimizar o cold start e
  latência, já que a validação de JWT é uma operação 100% CPU (matemática), sem I/O de rede.
