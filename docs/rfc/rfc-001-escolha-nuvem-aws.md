# RFC 001 — Escolha da Nuvem AWS como Provedor Cloud

**Status:** Aceita
**Data:** 2024-05-10

## Resumo
Proposta para adoção da Amazon Web Services (AWS) como provedor oficial de nuvem
para a implantação de todo o ecossistema Oficina (aplicação, banco e infraestrutura).

## Contexto e Motivação
O projeto necessita de um ambiente em nuvem para execução escalável e segura. As opções
principais no mercado são AWS, Azure e Google Cloud Platform (GCP). O fator limitante e
decisivo para a fase de aprendizado/avaliação é a disponibilidade de um sandbox acadêmico
pela instituição de ensino (AWS Academy Learner Lab).

## Opções Avaliadas

### 1. Amazon Web Services (AWS) — [ESCOLHIDA]
- **Infraestrutura disponível:** EKS (Kubernetes gerenciado), RDS (PostgreSQL gerenciado),
  API Gateway e AWS Lambda.
- **Ecossistema:** Ferramentas maduras (Terraform AWS Provider é o mais utilizado).
- **Acesso:** Disponibilização de contas AWS Academy Learner Lab (orçamento de ~$100).
- **Restrições:** IAM restrito (impossibilidade de criar Roles), sessões de 4h.

### 2. Google Cloud Platform (GCP)
- **Infraestrutura disponível:** GKE (melhor Kubernetes do mercado), Cloud SQL.
- **Acesso:** Não há ambiente sandbox acadêmico fornecido para este escopo. Seria
  necessário o uso de cartões de crédito pessoais dos alunos.

### 3. Microsoft Azure
- **Infraestrutura disponível:** AKS, Azure Database for PostgreSQL.
- **Acesso:** Azure for Students possui restrições severas em instâncias de Kubernetes
  (limite de cota de CPU) que muitas vezes impedem a criação do cluster.

## Detalhamento da Escolha
A AWS foi selecionada principalmente pela disponibilidade do ambiente AWS Academy, que
elimina custos pessoais. Os serviços adotados mapeiam exatamente as necessidades:
- **Compute:** AWS EKS para rodar o Spring Boot via Kubernetes.
- **Database:** AWS RDS para gerenciamento do banco transacional.
- **Borda/Segurança:** API Gateway + Lambda para resolver autenticação antes do tráfego
  entrar no cluster.

## Impactos da Decisão
- Necessidade de lidar com a limitação de expiração de sessão a cada 4h (resolvido
  atualizando os GitHub Secrets antes de rodar os pipelines de CI/CD).
- Arquitetura amarrada a serviços AWS específicos na borda (API Gateway, Lambda), embora o
  core (Spring Boot/Kubernetes e PostgreSQL) permaneça agnóstico de nuvem.
