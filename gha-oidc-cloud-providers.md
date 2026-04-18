# 5.4.2 OIDC — Configuración por Cloud Provider y Troubleshooting

← [5.4.1 OIDC — Fundamentos](gha-oidc-fundamentos-claims.md) | [Índice](README.md) | [5.5.1 Pin SHA — Conceptos](gha-pin-actions-sha-conceptos.md) →

---

Una vez entendidos los claims OIDC, el paso práctico es configurar la **trust policy** en cada cloud provider para que acepte tokens emitidos por GitHub Actions. Cada provider tiene su propia terminología y pasos, pero el principio es el mismo: registrar el issuer de GitHub, declarar qué claims se aceptan como condición de acceso y asociar esa confianza a un rol o cuenta de servicio.

> [CONCEPTO] La trust policy es la configuración del lado del cloud que define qué tokens OIDC son válidos para asumir un rol. Sin ella correctamente configurada, el intercambio de token fallará con 403.

## AWS — OIDC Provider en IAM y Role con Condition

En AWS el proceso tiene dos pasos: crear el OIDC Provider y crear un IAM Role cuya trust policy incluya una condición sobre el claim `sub`. El OIDC Provider registra el issuer `https://token.actions.githubusercontent.com` y su thumbprint. El IAM Role usa `sts:AssumeRoleWithWebIdentity` como acción y la condición filtra qué workflows pueden asumir ese rol.

La action oficial es `aws-actions/configure-aws-credentials@v4`. Recibe el ARN del role, la región y automáticamente solicita el token OIDC y realiza el intercambio con AWS STS.

> [EXAMEN] La condición en la trust policy de AWS puede usar `StringEquals` para repos exactos o `StringLike` con wildcard `*` para branches dinámicas. Para producción se recomienda `StringEquals` con rama específica.

## Azure — Workload Identity Federation en Microsoft Entra ID

En Azure (Microsoft Entra ID, anteriormente Azure AD) el mecanismo se llama **Workload Identity Federation**. Se crea una App Registration o Managed Identity y se añade una **federated credential** que especifica el issuer de GitHub, el subject claim exacto y la audiencia (`api://AzureADTokenExchange`).

La action oficial es `azure/login@v2`. Recibe `client-id`, `tenant-id` y `subscription-id`. No requiere `client-secret` cuando se usa OIDC.

> [ADVERTENCIA] En Azure el subject claim debe coincidir exactamente. No soporta wildcards en la federated credential, a diferencia de AWS. Un cambio de rama o entorno invalida la credencial configurada.

## GCP — Workload Identity Pool y Provider

En Google Cloud Platform el proceso involucra crear un **Workload Identity Pool**, añadir un **Provider** de tipo OIDC apuntando a GitHub, y vincular una cuenta de servicio al pool con una condición de atributo que filtre el claim `sub` o `repository`.

La action oficial es `google-github-actions/auth@v2`. Recibe el `workload_identity_provider` (formato `projects/PROJECT_NUMBER/locations/global/workloadIdentityPools/POOL/providers/PROVIDER`) y el `service_account`.

## OIDC en Reusable Workflows — Cambio del Subject Claim

Cuando un workflow usa un reusable workflow (llamado con `uses:`), el claim `sub` incluye además el campo `job_workflow_ref`, que apunta al workflow reutilizable, no al workflow llamador. Esto significa que la trust policy configurada para el workflow llamador **no funcionará sin ajuste** cuando se migra a reusable workflow.

> [EXAMEN] En reusable workflows el subject claim es `repo:ORG/REPO:job_workflow_ref:ORG/REPO/.github/workflows/REUSABLE.yml@refs/heads/BRANCH`. La trust policy debe actualizarse para reflejar este formato.

## Ejemplo central

El siguiente workflow completo muestra autenticación OIDC contra AWS, Azure y GCP en jobs separados, con permisos mínimos declarados.

```yaml
name: OIDC Multi-Cloud Auth

on:
  push:
    branches: [main]

permissions:
  id-token: write   # OBLIGATORIO para solicitar el token OIDC
  contents: read

jobs:
  aws-auth:
    runs-on: ubuntu-latest
    steps:
      - name: Configure AWS credentials via OIDC
        uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: arn:aws:iam::123456789012:role/GitHubActionsRole
          aws-region: us-east-1

      - name: Verify AWS identity
        run: aws sts get-caller-identity

  azure-auth:
    runs-on: ubuntu-latest
    steps:
      - name: Azure Login via OIDC
        uses: azure/login@v2
        with:
          client-id: ${{ secrets.AZURE_CLIENT_ID }}
          tenant-id: ${{ secrets.AZURE_TENANT_ID }}
          subscription-id: ${{ secrets.AZURE_SUBSCRIPTION_ID }}

      - name: Verify Azure identity
        run: az account show

  gcp-auth:
    runs-on: ubuntu-latest
    steps:
      - name: Authenticate to GCP via OIDC
        uses: google-github-actions/auth@v2
        with:
          workload_identity_provider: >-
            projects/123456789/locations/global/workloadIdentityPools/github-pool/providers/github-provider
          service_account: github-actions@my-project.iam.gserviceaccount.com

      - name: Verify GCP identity
        run: gcloud auth list
```

## Tabla de elementos clave

Los siguientes parámetros y configuraciones son los que el examen GH-200 evalúa con mayor frecuencia en relación a OIDC por cloud provider.

| Elemento | AWS | Azure | GCP |
|---|---|---|---|
| Registro del provider | IAM OIDC Provider | Federated credential en App Registration | Workload Identity Pool + Provider |
| Condición sobre claim | `StringLike`/`StringEquals` en `sub` | Subject exacto (sin wildcards) | Atributo de condición `attribute.repository` |
| Action oficial | `aws-actions/configure-aws-credentials@v4` | `azure/login@v2` | `google-github-actions/auth@v2` |
| Permiso requerido | `id-token: write` | `id-token: write` | `id-token: write` |
| Audiencia (audience) | `sts.amazonaws.com` (default) | `api://AzureADTokenExchange` | por defecto del provider |
| Soporte wildcard en trust | Si (`StringLike`) | No | Si (en condición de atributo) |

## Buenas y malas prácticas

**Hacer:** declarar `permissions: id-token: write` solo en el job que lo necesita, no a nivel de workflow — razón: el principio de mínimo privilegio aplica también al scope del permiso OIDC.

**Hacer:** usar `StringEquals` con rama específica en la trust policy de AWS para producción — razón: limita qué branches pueden asumir el rol, reduciendo la superficie de ataque ante PRs de forks.

**Hacer:** imprimir el token OIDC decodificado en troubleshooting para verificar el claim `sub` real antes de actualizar la trust policy — razón: el 90% de los errores 403 se deben a que el sub claim no coincide exactamente con la condición.

**Evitar:** configurar la trust policy con `*` como condición en producción — razón: cualquier workflow del repositorio, incluyendo PRs de forks, podría asumir el rol.

**Evitar:** almacenar las credenciales del cloud provider como secretos cuando OIDC es una opción disponible — razón: los secretos tienen vida larga y riesgo de filtración; los tokens OIDC expiran en minutos.

**Evitar:** olvidar actualizar la trust policy al migrar un job a reusable workflow — razón: el claim `sub` cambia y la autenticación fallará con 403 sin mensaje claro del motivo.

## Depuración de errores frecuentes

El error más habitual en OIDC es un 403 durante el intercambio de token. El diagnóstico correcto requiere comparar el claim `sub` real del token con la condición en la trust policy.

Para depurar, se puede imprimir el payload del token OIDC decodificado con el siguiente paso antes de la acción de autenticación. Requiere instalar `jq`.

```yaml
- name: Debug OIDC token claims
  run: |
    TOKEN=$(curl -sH "Authorization: bearer $ACTIONS_ID_TOKEN_REQUEST_TOKEN" \
      "$ACTIONS_ID_TOKEN_REQUEST_URL&audience=sts.amazonaws.com" | jq -r '.value')
    echo "$TOKEN" | cut -d'.' -f2 | base64 -d 2>/dev/null | jq .
```

> [ADVERTENCIA] Este paso imprime claims en el log. Nunca incluir en workflows de producción permanentes. Usar solo en ramas de depuración y eliminar después.

## Verificación y práctica

**Pregunta 1:** Un workflow usa OIDC para autenticarse en AWS. Funciona correctamente en el job del workflow principal, pero al mover ese job a un reusable workflow falla con 403. ¿Cuál es la causa?

*Respuesta:* El claim `sub` cambia cuando el job se ejecuta dentro de un reusable workflow. Pasa de `repo:ORG/REPO:ref:refs/heads/main` a incluir `job_workflow_ref:ORG/REPO/.github/workflows/reusable.yml@refs/heads/main`. La trust policy de AWS debe actualizarse para usar `StringLike` con el nuevo formato de `sub`.

**Pregunta 2:** ¿Qué permiso a nivel de job es obligatorio para que un workflow pueda solicitar un token OIDC?

*Respuesta:* `id-token: write`. Sin este permiso, la llamada a la URL de solicitud del token retorna un error de autorización. El permiso `contents: read` es independiente y no sustituye a `id-token: write`.

**Pregunta 3:** En Azure, ¿qué ocurre si el subject claim del token no coincide exactamente con el configurado en la federated credential?

*Respuesta:* La autenticación falla. Azure no soporta wildcards en el subject de la federated credential, por lo que cualquier diferencia (rama distinta, entorno distinto, uso en reusable workflow) genera un error de autenticación. Se deben crear federated credentials separadas para cada combinación de repo/branch/environment que necesite acceso.

**Ejercicio:** Escribe la trust policy para un IAM Role en AWS que permita que solo el workflow del repositorio `myorg/myrepo` ejecutándose en la rama `main` pueda asumir el rol.

```yaml
# Trust Policy JSON para IAM Role (no es YAML de workflow, es política IAM)
# Se configura en AWS Console o CLI, no en el workflow
#
# {
#   "Version": "2012-10-17",
#   "Statement": [
#     {
#       "Effect": "Allow",
#       "Principal": {
#         "Federated": "arn:aws:iam::123456789012:oidc-provider/token.actions.githubusercontent.com"
#       },
#       "Action": "sts:AssumeRoleWithWebIdentity",
#       "Condition": {
#         "StringEquals": {
#           "token.actions.githubusercontent.com:aud": "sts.amazonaws.com",
#           "token.actions.githubusercontent.com:sub": "repo:myorg/myrepo:ref:refs/heads/main"
#         }
#       }
#     }
#   ]
# }

# Workflow que usa ese role:
name: Deploy to AWS
on:
  push:
    branches: [main]

permissions:
  id-token: write
  contents: read

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: arn:aws:iam::123456789012:role/GitHubActionsRole
          aws-region: us-east-1
```

---

← [5.4.1 OIDC — Fundamentos](gha-oidc-fundamentos-claims.md) | [Índice](README.md) | [5.5.1 Pin SHA — Conceptos](gha-pin-actions-sha-conceptos.md) →
