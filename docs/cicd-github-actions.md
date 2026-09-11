# 🔁 CI/CD — Deploy na AWS via GitHub Actions

Como acionar o deploy automatizado da Oficina API no EKS. O projeto tem **dois workflows**:

| Workflow | Arquivo | Quando roda | O que faz |
|---|---|---|---|
| **CI** | `.github/workflows/maven.yml` | push / PR na `main` (automático) | build + testes |
| **CD** | `.github/workflows/deploy.yml` | manual ("Run workflow") ou tag `v*` | build da imagem → push no ECR → deploy no EKS |

O CD **não roda em push na main de propósito** — ele depende do Learner Lab ativo (credenciais que expiram), então é acionado sob demanda para não falhar/gastar crédito a cada commit.

---

## Pré-requisitos (uma vez)

### 1. Infra provisionada
O CD **faz deploy num cluster que já existe** — ele não provisiona nada. O `terraform apply`
(cluster EKS + ECR + RDS) precisa ter rodado antes. Ver [deploy-aws.md](deploy-aws.md), passo 1.

### 2. GitHub Secrets
**Settings → Secrets and variables → Actions → New repository secret:**

| Secret | Valor | Frequência |
|---|---|---|
| `JWT_SECRET` | segredo do JWT (texto puro) | **uma vez** |
| `RDS_PASSWORD` | = `db_password` do `terraform.tfvars` (texto puro) | **uma vez** |
| `AWS_ACCESS_KEY_ID` | credencial do Learner Lab | ⚠️ **a cada sessão** |
| `AWS_SECRET_ACCESS_KEY` | credencial do Learner Lab | ⚠️ **a cada sessão** |
| `AWS_SESSION_TOKEN` | credencial do Learner Lab | ⚠️ **a cada sessão** |

O `secret.yaml` real do Kubernetes **não vai para o repositório** — o workflow o recria em
tempo de execução a partir de `JWT_SECRET` e `RDS_PASSWORD`.

---

## Passo a passo para acionar o deploy

> Faça tudo dentro da **mesma sessão** do lab (janela de ~4h), porque as credenciais expiram.

### 1. Iniciar o Learner Lab
Start Lab → aguarde a bolinha **verde**. Pegue as credenciais em **AWS Details → AWS CLI**
(ver [acesso-aws-learner-lab.md](acesso-aws-learner-lab.md)).

### 2. Atualizar os 3 GitHub Secrets da AWS
As credenciais **mudam a cada sessão**. Atualize `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`
e `AWS_SESSION_TOKEN` no GitHub com os valores novos.

> Se pular este passo, o workflow falha com **`AccessDenied ... explicit deny ... voc-cancel-cred`**
> — é a policy que a AWS Academy anexa quando as credenciais são de uma sessão encerrada.

### 3. Garantir a infra e pegar o endpoint do RDS

A infraestrutura precisa estar provisionada via os repos dedicados:

```bash
# No repo oficina-infra-cluster (se ainda não provisionou)
terraform apply

# No repo oficina-infra-database
terraform apply -var="infra_state_bucket=oficina-tfstate-SEU-NOME" -var="db_password=SuaSenha"
terraform output -raw DB_Endpoint    # copie este valor
```

### 4. Acionar o workflow
**Actions → "CD - Deploy na AWS (EKS)" → Run workflow**, e no campo
**"Endpoint do RDS"** cole o valor do `DB_Endpoint`.

> Por que informar o endpoint? Ele tem um trecho aleatório gerado pela AWS e **muda toda vez
> que o RDS é recriado**. Por isso não fica fixo no `configmap-rds.yaml`: o workflow injeta o
> valor atual no `SPRING_DATASOURCE_URL` antes do deploy.

### 5. Acompanhar a execução
Abra a run em **Actions** e veja os steps. O último (`rollout status`) deve ficar verde.

---

## O que o workflow faz (resumo dos steps)

1. **Definir tag** — usa o nome da tag git ou o SHA curto (ECR é `IMMUTABLE` → tag única por build).
2. **Credenciais AWS** — autentica o runner com os secrets do lab.
3. **Login no ECR** + **build e push** da imagem.
4. **kubeconfig** — aponta o `kubectl` do runner para o cluster.
5. **Gerar Secret** — recria o `secret.yaml` a partir dos GitHub Secrets (efêmero, só no job).
6. **Apontar imagem** — `sed` no `newTag` do `kustomization.yaml`.
7. **Apontar endpoint do RDS** — `sed` no `SPRING_DATASOURCE_URL` com o valor informado.
8. **Deploy** — `kubectl apply -k k8s/overlays/aws`.
9. **Rollout** — falha o pipeline se o pod não subir em 180s.

---

## Verificar que funcionou

Localmente, com o lab ativo e as credenciais exportadas:

```bash
# reaponte o kubeconfig — o endpoint do EKS TAMBEM muda quando o cluster e recriado
aws eks update-kubeconfig --name eks-oficina-terraform --region us-east-1

kubectl get svc oficina-api          # copie o EXTERNAL-IP (DNS do ELB)
```

Acesse na **porta 8080** (o ELB escuta na 8080, não na 80):

```bash
curl http://<EXTERNAL-IP>:8080/actuator/health          # {"status":"UP"}
```
- Swagger: `http://<EXTERNAL-IP>:8080/swagger-ui/index.html`
- Login: `POST http://<EXTERNAL-IP>:8080/auth/login` com `admin@oficina.com` / `admin123`

Confirme que o pod está na versão nova:
```bash
kubectl describe pod <pod> | grep -i image:   # tag = SHA do commit deployado
```

---

## Armadilhas conhecidas (todas já enfrentadas)

| Sintoma | Causa | Correção |
|---|---|---|
| `AccessDenied ... voc-cancel-cred` | credenciais de sessão encerrada | Start Lab + atualizar os 3 GitHub Secrets AWS |
| `rollout status` dá timeout | pod não conecta no banco | endpoint do RDS antigo (informe o atual no dispatch) ou `RDS_PASSWORD` ≠ senha do RDS |
| `no such host` no `kubectl` local | cluster recriado → endpoint do EKS mudou | rode `aws eks update-kubeconfig` de novo |
| Falha ao acessar a URL | acessando porta 80 | use `:8080` |
| Login não retorna token | banco vazio (profile sem seed) | `configmap-rds.yaml` deve ter `SPRING_PROFILES_ACTIVE: dev` |
| `tag does not exist` / `ImagePullBackOff` | tag do ECR divergente | o workflow usa o SHA; confira o `newTag` no overlay |

> 💡 Regra de ouro do ciclo Learner Lab: **toda recriação da infra muda dois endpoints** — o do
> **RDS** (informado no input do workflow) e o do **EKS** (resolvido com `aws eks update-kubeconfig`).

---

## Encerrar (evita consumir crédito)

```bash
kubectl delete -k k8s/overlays/aws     # remove o Service → derruba o ELB (antes do destroy)

# Destrua na ordem inversa:
# 1. No repo oficina-infra-database
terraform destroy -var="infra_state_bucket=oficina-tfstate-SEU-NOME" -var="db_password=SuaSenha"
# 2. No repo oficina-infra-cluster
terraform destroy
```
E clique em **End Lab**.
