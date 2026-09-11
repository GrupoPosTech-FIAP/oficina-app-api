# 🚀 Deploy na AWS (Learner Lab + EKS + RDS)

Guia passo a passo para subir a Oficina API na AWS. Para rodar localmente (Docker Compose
ou Minikube), ver [execucao-local.md](execucao-local.md). Para o deploy **automatizado** via
GitHub Actions, ver [cicd-github-actions.md](cicd-github-actions.md).

A aplicação usa **Kustomize**: um `base/` comum e dois overlays. O overlay `aws` usa RDS
PostgreSQL (via Terraform), imagem no ECR e Service `LoadBalancer` (ELB).

| Diferença | Minikube | AWS |
|---|---|---|
| Imagem | Docker Hub (`henriquespecian/oficina-api`) | ECR |
| Banco | Postgres in-cluster (Deployment + PVC) | RDS PostgreSQL (Terraform) |
| Service | `NodePort` | `LoadBalancer` (ELB) |
| Secret | senha do Postgres local | senha do RDS |

> 🔐 Os `secret.yaml` reais estão no `.gitignore`. Cada overlay tem um
> `secret.example.yaml` versionado como modelo. Copie e preencha antes de aplicar:
> `cp secret.example.yaml secret.yaml`.

> As credenciais do Learner Lab expiram a cada sessão (máx. 4h). Como iniciar o
> lab e exportar as chaves está documentado em
> [acesso-aws-learner-lab.md](acesso-aws-learner-lab.md). **Faça isso antes** dos passos abaixo.

### Pré-requisitos
- AWS CLI, Terraform, Docker e `kubectl` instalados.
- Lab iniciado e credenciais exportadas (ver doc acima).
- Validar: `aws sts get-caller-identity`.

---

## 1 — Provisionar a infraestrutura (Terraform)

> A infraestrutura foi separada em repositórios dedicados. Provisione na ordem abaixo
> **antes** de continuar com o deploy da aplicação:
>
> 1. **[oficina-infra-cluster](https://github.com/GrupoPosTech-FIAP/oficina-infra-cluster)** — VPC, Subnets, EKS, ECR (~10-15 min)
> 2. **[oficina-infra-database](https://github.com/GrupoPosTech-FIAP/oficina-infra-database)** — RDS PostgreSQL (~10-15 min)
>
> Consulte o README de cada repositório para instruções detalhadas.

A senha definida no `db_password` do `oficina-infra-database` é a senha **master do RDS**. Ela precisa
ser a mesma usada depois no Secret do Kubernetes (passo 4) e no GitHub Secret
`RDS_PASSWORD` (CI/CD) — se divergir, o app não conecta no banco.

Ao terminar, anote os outputs de cada repositório (`terraform output`):
- `EKS_Cluster_Name` = `eks-oficina-terraform` (do `oficina-infra-cluster`)
- `Repository_URL`   = URL do ECR (do `oficina-infra-cluster`)
- `DB_Endpoint`      = host do RDS (do `oficina-infra-database`)

> ⚠️ Particularidade do Learner Lab: não é possível criar IAM Roles. O Terraform
> usa a role pré-existente **`LabRole`**.

## 2 — Conectar o kubectl ao EKS

```bash
aws eks update-kubeconfig --name eks-oficina-terraform --region us-east-1
kubectl config current-context      # deve apontar para o cluster eks-oficina-terraform
kubectl get nodes                    # nó(s) devem aparecer como Ready
```

## 3 — Publicar a imagem no ECR

> Rode a partir da **raiz do projeto** (onde está o `Dockerfile`).
> O `docker build` é que **cria** a tag local; o `push` só envia uma tag que já existe —
> se buildar na pasta errada, o push falha com `tag does not exist`.

```bash
cd c:/Source/Tech-Challenge-15SOAT     # raiz do projeto (tem o Dockerfile)

# autentica o Docker no ECR (troque a URL pela do seu output)
aws ecr get-login-password --region us-east-1 \
  | docker login --username AWS --password-stdin 029166075159.dkr.ecr.us-east-1.amazonaws.com

# build + push (tag IMMUTABLE: use uma tag nova a cada versão)
docker build -t 029166075159.dkr.ecr.us-east-1.amazonaws.com/oficina-api:1.0 .
docker push 029166075159.dkr.ecr.us-east-1.amazonaws.com/oficina-api:1.0

# confirme que a imagem chegou
aws ecr list-images --repository-name oficina-api --region us-east-1
```

## 4 — Ajustar o overlay `aws`

- Confirme o `image` (registry/tag) no `k8s/overlays/aws/kustomization.yaml`.
- Confirme o host do RDS em `configmap-rds.yaml` (= output `DB_Endpoint`).
- **Para popular o banco com o usuário admin e dados de exemplo**, o `configmap-rds.yaml`
  deve incluir `SPRING_PROFILES_ACTIVE: dev`. O seed (`DevDataLoader`) só roda no profile
  `dev` — sem ele o RDS sobe **vazio** e o login não devolve token (ver Troubleshooting).
  O seed é idempotente (só insere se as tabelas estiverem vazias).
- Crie o Secret com a senha do RDS:
  ```bash
  cp k8s/overlays/aws/secret.example.yaml k8s/overlays/aws/secret.yaml
  # gere o base64 (sem quebra de linha):
  printf '%s' 'senha-do-rds' | base64
  ```
  Cole o resultado em `SPRING_DATASOURCE_PASSWORD`. A senha tem que ser **igual**
  à `db_password` do `terraform.tfvars`.

> 🔴 **Não** coloque neste arquivo o template do CI (`$(echo ... ${{ secrets.X }} ...)`).
> Aquilo só é resolvido pelo runner do GitHub Actions; num `secret.yaml` estático vira
> texto literal e o apply falha com `illegal base64 data at input byte 0`.
>
> 💡 Alternativa sem base64 manual: use `stringData:` no lugar de `data:` e coloque os
> valores em **texto puro** — o Kubernetes codifica sozinho.

## 5 — Deploy no EKS

```bash
kubectl kustomize k8s/overlays/aws     # valida o render (opcional)
kubectl apply -k k8s/overlays/aws
kubectl get pods -w                     # aguarde 1/1 Running
```

> Se o Deployment tiver subido antes do Secret existir, o pod fica em
> `CreateContainerConfigError`. Depois de criar o Secret, force um restart:
> ```bash
> kubectl rollout restart deploy/oficina-api
> ```

## 6 — Acessar a API

```bash
kubectl get svc oficina-api            # aguarde o EXTERNAL-IP (DNS do ELB) aparecer
```

O Service expõe a **porta 8080** (`8080:xxxxx/TCP`) — o ELB escuta na 8080, **não na 80**.
Sempre inclua `:8080` na URL, senão a conexão falha (`Could not connect`). O ELB também
leva ~2-3 min após criado para registrar o alvo e responder.

- **Swagger UI:** `http://<EXTERNAL-IP>:8080/swagger-ui/index.html`
- **Health check:**
  ```bash
  curl http://<EXTERNAL-IP>:8080/actuator/health     # {"status":"UP"}
  ```

**Login (obter o token JWT):** use o usuário admin criado pelo seed. As credenciais dos
usuários de exemplo estão em [autenticacao-e-perfis.md](autenticacao-e-perfis.md).

```bash
curl -X POST http://<EXTERNAL-IP>:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@oficina.com","senha":"admin123"}'
```

Nos demais endpoints, envie o header `Authorization: Bearer <token>`.

## 7 — Encerrar (evita consumir crédito)

Destrua a infraestrutura na **ordem inversa** do provisionamento:

```bash
# 1. No repo oficina-infra-database
terraform destroy -var="infra_state_bucket=oficina-tfstate-SEU-NOME" -var="db_password=SuaSenha"

# 2. No repo oficina-infra-cluster
terraform destroy
```
E clique em **End Lab** no Learner Lab.

---

## Roteiro de teste ponta a ponta

Cada etapa tem uma verificação. Se uma falhar, pare nela antes de seguir.

| # | Ação | Verificação | Esperado |
|---|---|---|---|
| 0 | Iniciar Learner Lab + exportar chaves | `aws sts get-caller-identity` | mostra a conta |
| 1 | `terraform apply` | `terraform output` | `EKS_Cluster_Name`, `DB_Endpoint`, `Repository_URL` preenchidos |
| 2 | Conectar kubectl | `kubectl get nodes` | nó(s) `Ready` |
| 3 | Push da imagem | `aws ecr list-images --repository-name oficina-api --region us-east-1` | aparece a tag `1.0` |
| 4 | Preparar overlay | `kubectl kustomize k8s/overlays/aws` | render com Secret, ConfigMap (host RDS), imagem ECR, Service `LoadBalancer` |
| 5 | Deploy | `kubectl apply -k k8s/overlays/aws` | recursos `created` |
| 6 | Pods | `kubectl get pods -w` | `oficina-api-... 1/1 Running` |
| 7 | Expor | `kubectl get svc oficina-api` | `EXTERNAL-IP` vira DNS do ELB (~1-2 min) |
| 8 | Health | `curl http://<EXTERNAL-IP>:8080/actuator/health` | `{"status":"UP"}` |
| 9 | Login | `POST http://<EXTERNAL-IP>:8080/auth/login` (admin@oficina.com / admin123) | retorna um `token` JWT |
| 10 | Encerrar | `terraform destroy` + End Lab | recursos removidos |

## Troubleshooting

Comandos de diagnóstico:
```bash
kubectl get pods
kubectl describe pod <nome-do-pod>   # eventos no final explicam Pending / erro de imagem
kubectl logs <nome-do-pod>           # log da aplicação (ex.: erro de conexão com o RDS)
```

| Sintoma | Causa provável | Correção |
|---|---|---|
| `AccessDenied ... voc-cancel-cred` | credenciais de sessão encerrada | Start Lab + reexportar as chaves (ver [acesso-aws-learner-lab.md](acesso-aws-learner-lab.md)) |
| Overlay aplicado no cluster errado | contexto do kubectl trocado | `kubectl config current-context` e ajuste antes do apply |
| `no such host` no `kubectl` local | cluster recriado → endpoint do EKS mudou | rode `aws eks update-kubeconfig` de novo |
| `pod has unbound immediate PersistentVolumeClaims` | overlay `minikube` (com PVC) aplicado no EKS | aplique o overlay `aws` no EKS; o `minikube` só no Minikube |
| `illegal base64 data at input byte 0` | template do CI (`$(...)`/`${{ }}`) num `secret.yaml` estático | ponha valores base64 reais, ou use `stringData` com texto puro |
| `tag does not exist` no `docker push` | build rodado fora da raiz / não rodado | `docker build` a partir da raiz do projeto |
| `CreateContainerConfigError` | pod subiu sem o Secret existir | crie o Secret e `kubectl rollout restart deploy/oficina-api` |
| `CrashLoopBackOff` | senha do Secret ≠ senha do RDS, ou host errado no ConfigMap | confira `kubectl logs`; alinhe `secret.yaml` / `configmap-rds.yaml` com o RDS |
| `ImagePullBackOff` | imagem/tag inexistente no ECR ou nome divergente | confira `aws ecr list-images` e o `newTag` no `kustomization.yaml` |
| Login não retorna token | banco vazio: profile `prod` não roda o seed (`DevDataLoader`) | adicione `SPRING_PROFILES_ACTIVE: dev` no `configmap-rds.yaml`, reaplique e `rollout restart` |
| Falha ao conectar na URL do ELB | acessando na porta 80 em vez de 8080 | use `http://<EXTERNAL-IP>:8080/...` |

> 💡 Regra de ouro do ciclo Learner Lab: **toda recriação da infra muda dois endpoints** — o do
> **RDS** e o do **EKS** (resolvido com `aws eks update-kubeconfig`).

## Observações

- **Contexto do kubectl é crítico.** Rode sempre `kubectl config current-context`
  antes de um `apply`. Aplicar o overlay `minikube` (com Postgres + PVC) no EKS
  falha, porque o EKS não tem StorageClass default para o PVC.
- **Learner Lab:** não permite criar IAM Roles (usa-se a `LabRole`) e as credenciais
  expiram a cada sessão (~4h) — no CI/CD é preciso reatualizar os GitHub Secrets de AWS
  a cada nova sessão do lab.
- **Deploy automatizado:** o pipeline de CI/CD executa os passos 3 e 5
  automaticamente e injeta o Secret a partir dos GitHub Secrets — o `secret.yaml`
  real nunca vai para o repositório. Ver [cicd-github-actions.md](cicd-github-actions.md).
