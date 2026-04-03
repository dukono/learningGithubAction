# 🔒 GitHub Actions: Seguridad Avanzada

## 📚 Índice
1. [GITHUB_TOKEN: Permisos Detallados](#1-github_token-permisos-detallados)
2. [Scopes de Secrets](#2-scopes-de-secrets)
   - [Límites de Secrets](#límites-de-secrets)
3. [OIDC: Autenticación sin Secretos](#3-oidc-autenticación-sin-secretos)
4. [pull_request_target: Riesgos y Uso Seguro](#4-pull_request_target-riesgos-y-uso-seguro)
5. [Pinning de Actions por SHA](#5-pinning-de-actions-por-sha)
6. [Inyección de Código: Script Injection](#6-inyección-de-código-script-injection)
7. [Buenas Prácticas de Seguridad](#7-buenas-prácticas-de-seguridad)
8. [Check Runs y Check Suites](#8-check-runs-y-check-suites)
9. [Code Scanning con CodeQL](#9-code-scanning-con-codeql)
10. [Escaneo de Vulnerabilidades en Imagen Docker (SCA/DAST)](#10-escaneo-de-vulnerabilidades-en-imagen-docker-scadast)
11. [actions/github-script](#11-actionsgithub-script)
12. [Artifact Attestations y Build Provenance](#12-artifact-attestations-y-build-provenance)
13. [::add-mask:: para Enmascarar Valores Dinámicos](#13-add-mask-para-enmascarar-valores-dinámicos)
14. [OpenSSF Scorecards](#14-openssf-scorecards)
15. [Preguntas de Examen](#15-preguntas-de-examen)

---

## 1. GITHUB_TOKEN: Permisos Detallados

El `GITHUB_TOKEN` es un token que **GitHub genera automáticamente** para cada ejecución de workflow. No es un secreto que configuras tú — existe siempre.

### Tabla completa de permisos por scope

| Scope | Descripción | Default (repos públicos) | Default (repos privados) |
|---|---|---|---|
| `actions` | Artifacts, workflows | `write` | `read` |
| `checks` | Check runs, check suites | `write` | `read` |
| `contents` | Código, releases, branches, tags, commits | `write` | `read` |
| `deployments` | Deployments | `write` | `read` |
| `discussions` | Discusiones del repo | `write` | `read` |
| `id-token` | Token OIDC | `none` | `none` |
| `issues` | Issues y comentarios | `write` | `read` |
| `metadata` | Metadatos del repo | `read` | `read` |
| `packages` | GitHub Packages | `write` | `read` |
| `pages` | GitHub Pages | `write` | `read` |
| `pull-requests` | PRs y comentarios | `write` | `read` |
| `repository-projects` | Proyectos del repo | `write` | `read` |
| `security-events` | Code Scanning, Dependabot | `write` | `read` |
| `statuses` | Estado de commits | `write` | `read` |

> ⚠️ **Diferencia crítica**: En eventos desde **forks** o disparados por `pull_request`, el GITHUB_TOKEN tiene permisos muy reducidos por seguridad (solo `read` en la mayoría de scopes).

### Cambiar permisos del GITHUB_TOKEN

```yaml
# Nivel workflow (aplica a todos los jobs)
permissions:
  contents: read       # Solo leer código
  issues: write        # Puede crear/editar issues
  pull-requests: write # Puede comentar en PRs

# O reducir a solo-lectura todo
permissions: read-all

# O dar escritura completa (no recomendado)
permissions: write-all

# Nivel job (sobreescribe permisos del workflow para ese job)
jobs:
  mi-job:
    permissions:
      contents: write    # Este job puede escribir código
      issues: none       # Este job no puede tocar issues
```

### Usar GITHUB_TOKEN en la práctica

```yaml
steps:
  # Opción 1: Directo en run
  - name: Crear release via API
    run: |
      curl -X POST \
        -H "Authorization: Bearer ${{ secrets.GITHUB_TOKEN }}" \
        -H "Content-Type: application/json" \
        https://api.github.com/repos/${{ github.repository }}/releases \
        -d '{"tag_name":"v1.0.0","name":"Release v1.0.0"}'
  
  # Opción 2: Como variable de entorno (más seguro)
  - name: Usar token como env
    env:
      GH_TOKEN: ${{ secrets.GITHUB_TOKEN }}
    run: gh pr comment ${{ github.event.pull_request.number }} --body "Tests pasaron ✅"
  
  # Opción 3: gh cli lo usa automáticamente con GH_TOKEN
  - name: GitHub CLI
    env:
      GH_TOKEN: ${{ secrets.GITHUB_TOKEN }}
    run: gh issue list
```

---

## 2. Scopes de Secrets

Los secrets se pueden definir en tres niveles con diferente alcance:

### Niveles de secrets

```
┌──────────────────────────────────────────────┐
│  ORGANIZACIÓN                                │
│  (disponibles para todos los repos de la org)│
│                                              │
│  ┌──────────────────────────────────────┐   │
│  │  REPOSITORIO                         │   │
│  │  (disponibles para ese repo)         │   │
│  │                                      │   │
│  │  ┌────────────────────────────────┐  │   │
│  │  │  ENVIRONMENT                   │  │   │
│  │  │  (solo en jobs con ese env)    │  │   │
│  │  └────────────────────────────────┘  │   │
│  └──────────────────────────────────────┘   │
└──────────────────────────────────────────────┘
```

### Secrets de Repositorio

Configurados en: **Settings → Secrets and variables → Actions → Secrets**

```yaml
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - run: echo "Token: ${{ secrets.MY_API_TOKEN }}"
      # Disponible en cualquier job del workflow
```

### Secrets de Environment

Configurados en: **Settings → Environments → [nombre-env] → Secrets**

```yaml
jobs:
  deploy-prod:
    runs-on: ubuntu-latest
    environment: production    # ← Activa este environment
    steps:
      - run: echo "Deploy key: ${{ secrets.DEPLOY_KEY }}"
      # Solo disponible porque el job usa environment: production
```

Si otro job no tiene `environment: production`, NO puede acceder a `secrets.DEPLOY_KEY` del environment production.

### Secrets de Organización

Configurados en: **Organization Settings → Secrets**

Pueden restringirse a:
- Todos los repos de la org
- Repos específicos
- Solo repos privados

```yaml
# Acceso igual que los de repositorio
steps:
  - run: echo "${{ secrets.ORG_WIDE_TOKEN }}"
```

### Variables (vars): la contraparte pública

`vars` funciona igual que `secrets` pero **NO está encriptado** (visible en los logs si se hace echo):

```yaml
# Settings → Secrets and variables → Variables
jobs:
  build:
    environment: staging
    steps:
      - run: |
          echo "ENV: ${{ vars.ENVIRONMENT_NAME }}"    # staging
          echo "URL: ${{ vars.APP_URL }}"              # https://staging.example.com
```

### Límites de Secrets

Es importante conocer los límites técnicos que impone GitHub sobre los secrets:

| Límite | Valor |
|---|---|
| Tamaño máximo por secret | 48 KB |
| Máximo de secrets por organización | 1.000 |

**Limitaciones importantes con workflow_call:**

- Los **secrets de environment** NO se pueden pasar via `workflow_call`. Solo se pueden pasar secrets de repositorio u organización a un workflow reutilizable.
- Los secrets solo se pasan al **workflow llamado directamente**. No se propagan automáticamente en cadenas de workflows (caller → callee → callee2).

```yaml
# Caller workflow
jobs:
  llamar:
    uses: ./.github/workflows/reusable.yml
    secrets:
      MI_TOKEN: ${{ secrets.MI_TOKEN }}          # ✅ Se puede pasar
      # ${{ secrets.ENV_SECRET }}               # ❌ Si es de environment, no se puede pasar
```

```yaml
# Reusable workflow (callee)
on:
  workflow_call:
    secrets:
      MI_TOKEN:
        required: true

jobs:
  mi-job:
    runs-on: ubuntu-latest
    steps:
      - run: echo "Token disponible"
        env:
          TOKEN: ${{ secrets.MI_TOKEN }}
      # Los secrets de este workflow NO se propagan automáticamente
      # a otro workflow que este a su vez llame
```

---

## 3. OIDC: Autenticación sin Secretos

**OIDC (OpenID Connect)** permite que GitHub Actions se autentique en servicios en la nube (AWS, Azure, GCP) **sin almacenar credenciales como secrets**.

### El problema que resuelve

```
Sin OIDC:
1. Crear usuario IAM en AWS con permisos
2. Generar Access Key + Secret Key
3. Guardar como secrets en GitHub
4. Las credenciales no expiran nunca
5. Si se filtran, acceso permanente

Con OIDC:
1. Configurar trust policy en AWS (confiar en GitHub)
2. En el workflow, pedir un token OIDC
3. AWS verifica que es realmente de GitHub
4. AWS emite credenciales temporales (15 min)
5. No hay secretos que guardar ni que puedan filtrarse
```

### Requisito: Permiso id-token

```yaml
permissions:
  id-token: write    # ← OBLIGATORIO para OIDC
  contents: read
```

### Ejemplo: AWS con OIDC

**1. Configuración en AWS (una vez):**
```json
// Trust policy del IAM Role
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Principal": {
      "Federated": "arn:aws:iam::123456789:oidc-provider/token.actions.githubusercontent.com"
    },
    "Action": "sts:AssumeRoleWithWebIdentity",
    "Condition": {
      "StringEquals": {
        "token.actions.githubusercontent.com:aud": "sts.amazonaws.com",
        "token.actions.githubusercontent.com:sub": "repo:mi-org/mi-repo:ref:refs/heads/main"
      }
    }
  }]
}
```

**2. En el workflow:**
```yaml
name: Deploy a AWS

on:
  push:
    branches: [main]

permissions:
  id-token: write    # ← Necesario para OIDC
  contents: read

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Autenticarse en AWS
        uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: arn:aws:iam::123456789:role/GitHubActionsRole
          aws-region: us-east-1
          # ← No hay access-key-id ni secret-access-key
      
      - name: Deploy
        run: |
          aws s3 sync ./dist s3://mi-bucket
          aws cloudfront create-invalidation --distribution-id XXXXX --paths "/*"
```

### Ejemplo: Azure con OIDC

```yaml
permissions:
  id-token: write
  contents: read

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - name: Login en Azure
        uses: azure/login@v2
        with:
          client-id: ${{ secrets.AZURE_CLIENT_ID }}       # Solo el ID, no el secreto
          tenant-id: ${{ secrets.AZURE_TENANT_ID }}
          subscription-id: ${{ secrets.AZURE_SUBSCRIPTION_ID }}
          # ← No hay AZURE_CLIENT_SECRET
      
      - name: Deploy
        run: az webapp deploy --resource-group mi-grupo --name mi-app
```

### Ejemplo: GCP con OIDC

```yaml
permissions:
  id-token: write
  contents: read

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - name: Autenticarse en GCP
        uses: google-github-actions/auth@v2
        with:
          workload_identity_provider: 'projects/123/locations/global/workloadIdentityPools/my-pool/providers/my-provider'
          service_account: 'my-service-account@my-project.iam.gserviceaccount.com'
```

---

## 4. pull_request_target: Riesgos y Uso Seguro

### La diferencia con pull_request

| | `pull_request` | `pull_request_target` |
|---|---|---|
| Contexto de ejecución | Código de la **rama del PR** (head) | Código de la **rama destino** (base) |
| Secrets disponibles | NO (en forks) | SÍ ⚠️ |
| Permisos GITHUB_TOKEN | read-only (forks) | write ⚠️ |
| Uso típico | CI tests | Comentar en PRs de forks, deploy previews |

### El riesgo

```yaml
# ⚠️ PELIGROSO: Ejecuta código del PR con acceso a secrets
on:
  pull_request_target:

jobs:
  dangerous:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4    # ← ¿De qué rama? Del PR (head) por defecto
        with:
          ref: ${{ github.event.pull_request.head.sha }}   # ← Código del PR
      
      - run: npm test    # ← Ejecuta código del PR CON acceso a secrets = PWNED
```

Un atacante puede abrir un PR con código malicioso en `npm test` que exfiltra secrets.

### Uso seguro de pull_request_target

```yaml
on:
  pull_request_target:
    types: [opened, synchronize]

jobs:
  # ✅ SEGURO: Solo hace operaciones que NO ejecutan código del PR
  comment-on-pr:
    runs-on: ubuntu-latest
    permissions:
      pull-requests: write
    steps:
      # NO hace checkout del código del PR
      - name: Comentar en el PR
        uses: actions/github-script@v7
        with:
          script: |
            github.rest.issues.createComment({
              issue_number: context.issue.number,
              owner: context.repo.owner,
              repo: context.repo.repo,
              body: '👋 Gracias por tu contribución! Revisaremos tu PR pronto.'
            })
  
  # ✅ SEGURO: Checkout del HEAD (base), no del PR
  lint-base:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        # Sin ref: → clona la rama BASE (segura), no el PR
      - run: npm run lint   # Ejecuta código de base, no del PR
```

### Patrón seguro para CI en forks: workflow_run

```yaml
# Workflow 1: CI sin secrets (ejecuta código del PR)
# .github/workflows/ci.yml
on:
  pull_request:
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: npm test    # Sin secrets, seguro

# Workflow 2: Leer resultados y comentar (con secrets)
# .github/workflows/comment.yml
on:
  workflow_run:
    workflows: ["CI"]
    types: [completed]
jobs:
  comment:
    runs-on: ubuntu-latest
    permissions:
      pull-requests: write
    steps:
      - name: Comentar resultado
        uses: actions/github-script@v7
        with:
          script: |
            const result = context.payload.workflow_run.conclusion
            // Puede usar secrets aquí, pero NO ejecuta código del PR
```

---

## 5. Pinning de Actions por SHA

### El problema con los tags

```yaml
# ⚠️ Potencialmente inseguro
- uses: actions/checkout@v4         # v4 puede cambiar
- uses: mi-usuario/mi-action@main   # main cambia con cada commit
```

Si el autor de una action (tuya o de terceros) compromete el repositorio, un cambio en el tag puede introducir código malicioso.

### Pinning por SHA (recomendado)

```yaml
# ✅ Seguro: el SHA nunca cambia
- uses: actions/checkout@11bd71901bbe5b1630ceea73d27597364c9af683  # v4.2.2
- uses: actions/setup-node@cdca7365b2dadb8aad0a33bc7601856ffabcc48 # v4.3.0
```

El SHA del commit es inmutable. Aunque alguien reetiquete `v4`, el SHA no cambia.

### ¿Cómo obtener el SHA?

```bash
# Ver SHA de un tag específico
git ls-remote https://github.com/actions/checkout.git refs/tags/v4
# Output: 11bd71901bbe5b1630ceea73d27597364c9af683  refs/tags/v4

# O en el repositorio de la action, ver el historial del tag
```

### Herramientas automáticas

- **Dependabot**: Puede actualizar automáticamente los SHAs cuando salen nuevas versiones
```yaml
# .github/dependabot.yml
version: 2
updates:
  - package-ecosystem: github-actions
    directory: "/"
    schedule:
      interval: weekly
```

---

## 6. Inyección de Código: Script Injection

Cuando usas `${{ github.event.pull_request.title }}` directamente dentro de un bloque `run:`, GitHub sustituye la expresión **antes** de enviar el script al runner, como si fuera concatenación de strings. Si el valor contiene caracteres especiales de bash (comillas, punto y coma, paréntesis), el shell los interpreta como código.

El riesgo es real porque el título de un PR, el cuerpo de un comentario o el nombre de una rama los puede escribir **cualquier usuario externo** que abra un PR o commente en el repositorio.

### El problema

Valores del contexto pueden contener código si vienen de input externo (títulos de PR, comentarios, etc.):

```yaml
# ⚠️ VULNERABLE: Inyección de código
- name: Obtener título del PR
  run: echo "El título es: ${{ github.event.pull_request.title }}"
  # Si el título es: hello"; rm -rf /; echo "bye
  # Se convierte en: echo "El título es: hello"; rm -rf /; echo "bye"
```

### Solución: Variables de entorno intermedias

```yaml
# ✅ SEGURO: El valor pasa como variable de entorno, no como código
- name: Obtener título del PR
  env:
    PR_TITLE: ${{ github.event.pull_request.title }}  # ← Variable de entorno
  run: echo "El título es: $PR_TITLE"    # ← Referencia bash, no expresión
```

La variable de entorno es siempre un **string**, nunca se interpreta como código bash.

### Valores peligrosos (siempre usar env vars)

```yaml
# SIEMPRE usar env vars para estos valores:
env:
  PR_TITLE: ${{ github.event.pull_request.title }}
  PR_BODY: ${{ github.event.pull_request.body }}
  COMMENT: ${{ github.event.comment.body }}
  BRANCH: ${{ github.head_ref }}
  # Cualquier campo que venga de input del usuario
```

---

## 7. Buenas Prácticas de Seguridad

### Resumen de prácticas esenciales

```yaml
# ✅ 1. Permissions mínimas
permissions:
  contents: read
  # Solo añadir lo necesario

# ✅ 2. Secrets como variables de entorno
- name: Usar API
  env:
    API_KEY: ${{ secrets.API_KEY }}
  run: curl -H "Auth: $API_KEY" https://api.example.com
  # ❌ Nunca: curl -H "Auth: ${{ secrets.API_KEY }}" ... (aparece en logs si hay error)

# ✅ 3. Pinning de actions por SHA
- uses: actions/checkout@11bd71901bbe5b1630ceea73d27597364c9af683 # v4.2.2

# ✅ 4. Variables de entorno para valores de contexto no confiables
env:
  PR_TITLE: ${{ github.event.pull_request.title }}

# ✅ 5. Validar que solo se ejecuta en tu repo (no en forks con secrets)
if: github.repository == 'mi-usuario/mi-repo'

# ✅ 6. OIDC en vez de credenciales de larga duración
# (ver sección OIDC arriba)

# ✅ 7. Revisar permisos de Actions de terceros antes de usar
# (ver sus permisos en action.yml)
```

### Checklist de seguridad

- [ ] `permissions` está definido con mínimo privilegio
- [ ] Secrets no se imprimen directamente en `run:` (usar env vars)
- [ ] Actions de terceros están fijadas por SHA
- [ ] `pull_request_target` no ejecuta código del fork
- [ ] Valores de input de usuarios (títulos, comentarios) pasan por env vars
- [ ] OIDC en lugar de credenciales de larga duración cuando es posible
- [ ] Dependabot configurado para actualizar actions

---

## 8. Preguntas de Examen

**P: ¿Qué es el GITHUB_TOKEN y quién lo genera?**
→ Es un token de autenticación generado automáticamente por GitHub para cada ejecución de workflow. No necesitas configurarlo — siempre está disponible en `secrets.GITHUB_TOKEN`.

**P: ¿Qué permiso necesita un workflow para usar OIDC?**
→ `id-token: write`

**P: ¿Cuál es la diferencia entre `pull_request` y `pull_request_target`?**
→ `pull_request` ejecuta el código de la rama del PR (head), tiene permisos reducidos en forks. `pull_request_target` ejecuta el código de la rama destino (base) y tiene acceso completo a secrets aunque venga de un fork, por eso es peligroso si se hace checkout del código del PR.

**P: ¿Por qué es más seguro fijar una action por SHA que por tag?**
→ Los tags pueden ser reetiquetados a otro commit. El SHA es inmutable — el código que referencia un SHA nunca puede cambiar.

**P: ¿Cómo evitar script injection con `github.event.pull_request.title`?**
→ Pasarlo como variable de entorno en lugar de interpolarlo directamente en el script:
```yaml
env:
  TITLE: ${{ github.event.pull_request.title }}
run: echo "$TITLE"  # no echo "${{ github.event.pull_request.title }}"
```

**P: ¿En qué nivel se pueden definir secrets en GitHub?**
→ En 3 niveles: Organización (disponibles para repos de la org), Repositorio (disponibles para ese repo), y Environment (solo para jobs que usan ese environment).

**P: ¿Qué ventaja tiene OIDC sobre secretos de larga duración?**
→ No hay secretos que almacenar ni que puedan filtrarse. Las credenciales son temporales (≈15 min). Se puede restringir por rama/repo/org en la trust policy del proveedor cloud.

---

## 8. Check Runs y Check Suites

Los **Check Runs** son la representación visible en GitHub del resultado de cada job: los ticks verdes o cruces rojas que aparecen en un PR bajo "Checks". Cuando configuras **Branch protection rules** que exigen que ciertos checks pasen antes de poder mergear, estás usando Check Runs.

Normalmente GitHub Actions los crea automáticamente — no necesitas hacer nada. Esta sección es relevante si quieres **integrar herramientas externas** (linters, scanners, sistemas de CI externos) publicando sus resultados como Check Runs en GitHub, o si necesitas **crear anotaciones** (líneas del código marcadas con advertencias o errores directamente en el diff del PR).

### ¿Qué son?

Cuando GitHub Actions ejecuta un workflow, crea automáticamente objetos en la API de GitHub:

```
git push → GitHub Events → Workflow Run
                               │
                    ┌──────────┴──────────┐
                    ▼                     ▼
             Check Suite           Check Suite
             (por workflow)        (por otra tool)
                    │
          ┌─────────┼─────────┐
          ▼         ▼         ▼
       Check Run  Check Run  Check Run
       (job lint) (job test) (job build)
```

- **Check Suite**: agrupa todos los check runs de un commit para una app (ej: GitHub Actions)
- **Check Run**: resultado individual de un job — tiene estado (`queued`, `in_progress`, `completed`) y conclusión (`success`, `failure`, `neutral`, `cancelled`, `skipped`, `timed_out`, `action_required`)

### Relación con Branch Protection Rules

Los Check Runs son la base de los **status checks requeridos**:

```
Settings → Branches → Branch protection rules → Require status checks
  ✅ CI / lint         ← nombre del job en el workflow
  ✅ CI / test
  ✅ CI / build
```

Si alguno falla → no se puede mergear el PR.

> El nombre que aparece en Branch Protection es `{nombre-workflow} / {nombre-job}`.

### Crear Check Runs manualmente

Se puede crear un Check Run desde un workflow para integrar herramientas externas:

```yaml
- name: Crear check run
  uses: actions/github-script@v7
  with:
    script: |
      await github.rest.checks.create({
        owner: context.repo.owner,
        repo: context.repo.repo,
        name: 'Mi herramienta externa',
        head_sha: context.sha,
        status: 'completed',
        conclusion: 'success',
        output: {
          title: 'Análisis completado',
          summary: 'No se encontraron problemas'
        }
      })
```

---

## 9. Code Scanning con CodeQL

CodeQL es el análisis de seguridad estático de GitHub. Encuentra vulnerabilidades en el código fuente (SQL injection, XSS, etc.) y publica los resultados como alertas de seguridad en el repositorio.

**Cómo funciona**: CodeQL compila o parsea el código fuente y construye una base de datos de su estructura (flujo de datos, llamadas a funciones, uso de variables). Luego ejecuta **queries** escritas en el lenguaje QL que buscan patrones de vulnerabilidades conocidas. Los resultados se publican en `Security → Code scanning alerts` como anotaciones en el código.

Para lenguajes interpretados (Python, JavaScript) el análisis es directo. Para lenguajes compilados (C, Java, C#) CodeQL necesita compilar el código — por eso existe el step `autobuild` que intenta compilarlo automáticamente, o puedes reemplazarlo con tus propios comandos de build.

### Workflow autogenerado por GitHub

GitHub puede generar automáticamente el workflow al activar Code Scanning:

```yaml
# .github/workflows/codeql.yml
name: "CodeQL Analysis"

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]
  schedule:
    - cron: '30 1 * * 0'   # Análisis semanal (domingos a la 1:30 UTC)

jobs:
  analyze:
    name: Analyze (${{ matrix.language }})
    runs-on: ubuntu-latest
    
    permissions:
      security-events: write   # ← Necesario para publicar alertas SARIF
      actions: read
      contents: read
    
    strategy:
      fail-fast: false
      matrix:
        language: [javascript-typescript, python]
        # Lenguajes disponibles: c-cpp, csharp, go, java-kotlin,
        #                        javascript-typescript, python, ruby, swift
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Initialize CodeQL
        uses: github/codeql-action/init@v3
        with:
          languages: ${{ matrix.language }}
          # queries: +security-extended   # Añadir queries adicionales
      
      - name: Autobuild
        uses: github/codeql-action/autobuild@v3
        # Para lenguajes compilados (C, Java): intenta compilar automáticamente
        # Para interpretados (Python, JS): no necesita build
      
      - name: Perform CodeQL Analysis
        uses: github/codeql-action/analyze@v3
        with:
          category: "/language:${{ matrix.language }}"
          # Los resultados se publican como SARIF en Security → Code scanning alerts
```

### Resultados SARIF

CodeQL publica los resultados en formato **SARIF** (Static Analysis Results Interchange Format):

```
Repositorio → Security → Code scanning alerts
  ⚠️ SQL Injection en src/db.js:45
  ⚠️ XSS en src/render.js:23
```

También se pueden publicar resultados SARIF de otras herramientas:

```yaml
- name: Publicar resultados de otra herramienta
  uses: github/codeql-action/upload-sarif@v3
  with:
    sarif_file: results.sarif
```

### Permisos necesarios

```yaml
permissions:
  security-events: write   # ← Para publicar alertas SARIF
```

---

## 10. Escaneo de Vulnerabilidades en Imagen Docker (SCA/DAST)

CodeQL analiza el **código fuente** (SAST). Pero después de construir la imagen Docker existen dos análisis adicionales:

- **SCA (Software Composition Analysis):** escanea la imagen construida en busca de CVEs en las dependencias del OS, librerías, y paquetes de lenguaje.
- **DAST (Dynamic Application Security Testing):** ataca la aplicación en ejecución buscando vulnerabilidades de runtime (OWASP Top 10: SQLi, XSS, etc.).

```
Código fuente → CodeQL (SAST) → imagen Docker → Trivy (SCA) → app en ejecución → OWASP ZAP (DAST)
```

### Escaneo de imagen con Trivy

**Trivy** es el escáner de vulnerabilidades más usado en GitHub Actions. Detecta CVEs en el OS base (Alpine, Debian, Ubuntu), dependencias de lenguaje (npm, pip, Maven, Go modules) y configuraciones incorrectas.

```yaml
  scan-image:
    needs: build-push        # Necesita que la imagen ya esté construida
    runs-on: ubuntu-latest
    permissions:
      security-events: write # Para publicar en Security → Code scanning alerts

    steps:
      - name: Escanear imagen con Trivy
        uses: aquasecurity/trivy-action@master
        with:
          image-ref: ghcr.io/${{ github.repository }}:${{ needs.build-push.outputs.image-tag }}
          format: sarif            # Publica como alertas en GitHub Security
          output: trivy-results.sarif
          severity: 'CRITICAL,HIGH'
          exit-code: '1'           # ← Falla el pipeline si hay CVEs CRITICAL o HIGH

      - name: Publicar resultados en Security tab
        uses: github/codeql-action/upload-sarif@v3
        if: always()   # Publicar aunque el job haya fallado
        with:
          sarif_file: trivy-results.sarif
          category: container-scanning
```

**Formatos de salida de Trivy:**

```yaml
# Tabla legible en logs (para PR checks rápidos)
format: table
exit-code: '1'
severity: 'CRITICAL'

# SARIF (para publicar en GitHub Security → Code scanning alerts)
format: sarif
output: trivy-results.sarif

# JSON (para procesar programáticamente)
format: json
output: trivy-results.json
```

### Ignorar CVEs conocidos (trivy.yaml)

Si una CVE no tiene fix disponible o está aceptada por el equipo de seguridad, se puede ignorar con un archivo de configuración:

```yaml
# .trivy.yaml (en la raíz del repo)
vulnerability:
  ignore-unfixed: true    # No reportar CVEs sin parche disponible

# O lista explícita de CVEs ignoradas:
# .trivyignore
CVE-2023-XXXXX   # Descripción de por qué se ignora
CVE-2024-YYYYY   # Fix no disponible, aceptado hasta 2025-01-01
```

```yaml
      - name: Escanear con ignores
        uses: aquasecurity/trivy-action@master
        with:
          image-ref: ghcr.io/${{ github.repository }}:${{ needs.build-push.outputs.image-tag }}
          trivyignores: .trivyignore
          format: table
          severity: 'CRITICAL,HIGH'
          exit-code: '1'
```

### Escaneo de código fuente con Trivy (IaC y secrets)

Trivy también puede escanear el repositorio directamente (sin imagen) para detectar secretos hardcodeados y misconfiguraciones en IaC (Terraform, Kubernetes YAML):

```yaml
  scan-code:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Escanear repositorio (secrets + IaC misconfigs)
        uses: aquasecurity/trivy-action@master
        with:
          scan-type: fs              # ← filesystem scan, no imagen
          scan-ref: .
          format: table
          exit-code: '1'
          scanners: secret,misconfig # ← secrets hardcodeados + IaC misconfigs
```

### DAST con OWASP ZAP

**OWASP ZAP** ataca la aplicación **en ejecución** (no el código). Requiere que el servicio esté desplegado y accesible (staging o preview environment).

```yaml
  dast-scan:
    needs: deploy-staging    # La aplicación debe estar desplegada
    runs-on: ubuntu-latest
    permissions:
      security-events: write
    steps:
      - name: OWASP ZAP Baseline Scan
        uses: zaproxy/action-baseline@v0.12.0
        with:
          target: 'https://staging.example.com'
          # baseline: escaneo rápido (activo de sólo-lectura, sin ataques activos)
          # full-scan: escaneo completo con ataques activos
          rules_file_name: .zap/rules.tsv   # Opcional: suprimir falsos positivos
          fail_action: warn                  # warn | fail
          # fail = falla el job si encuentra alertas MEDIUM o superiores
          # warn = siempre pasa pero genera warning en el log
```

**Tipos de escaneo ZAP:**

| Action | Qué hace | Tiempo | Indicado para |
|---|---|---|---|
| `action-baseline` | Solo peticiones pasivas (no envía ataques) | ~2 min | PRs, CI rápido |
| `action-full-scan` | Activo + pasivo (lanza ataques reales) | ~15 min | Staging/UAT |
| `action-api-scan` | Diseñado para APIs (OpenAPI/Swagger) | ~5 min | APIs REST |

### Integración en el pipeline completo

```
PR abierto:
  build (sin push) → Trivy (tabla, no bloquea) → CodeQL

Merge a develop:
  build + push → Trivy SARIF (bloquea si CRITICAL) → deploy staging → ZAP Baseline

Release candidate:
  build optimizado → Trivy SARIF (bloquea si HIGH) → SBOM attest → ZAP Full Scan → UAT gate
```

```yaml
  # Resumen del orden recomendado en un pipeline completo:
  jobs:
    build:           # Construye imagen
    scan-sast:       # CodeQL sobre código fuente (en paralelo con build)
    scan-image:      # Trivy sobre imagen construida (necesita: build)
    deploy-staging:  # Deploy (necesita: scan-image)
    dast:            # OWASP ZAP (necesita: deploy-staging)
    smoke-tests:     # Smoke tests funcionales (en paralelo con dast)
```

---

## 11. actions/github-script

`actions/github-script` permite escribir JavaScript directamente en el YAML del workflow para interactuar con la API de GitHub (Octokit) sin necesidad de crear una action separada.

### ¿Cuándo usar github-script vs gh CLI?

| | `actions/github-script` | `gh` CLI |
|---|---|---|
| **Sintaxis** | JavaScript (Octokit) | Shell / CLI |
| **Complejidad** | Lógica compleja, condicionales JS | Operaciones simples |
| **API** | REST + GraphQL completo | Solo comandos disponibles en gh |
| **Outputs** | `return valor` → `steps.id.outputs.result` | `echo "..." >> $GITHUB_OUTPUT` |
| **Mejor para** | Manipulación de datos, múltiples llamadas API | Comandos puntuales |

### Sintaxis básica

```yaml
- uses: actions/github-script@v7
  with:
    script: |
      // 'github' = cliente Octokit autenticado con GITHUB_TOKEN
      // 'context' = contexto del workflow (repo, sha, actor, etc.)
      // 'core' = @actions/core (outputs, logging)
      // 'exec' = ejecutar comandos shell
      // 'io' = operaciones de filesystem
      
      const result = await github.rest.issues.list({
        owner: context.repo.owner,
        repo: context.repo.repo,
        state: 'open'
      })
      core.setOutput('count', result.data.length)
```

### Operaciones más comunes

**Comentar en un PR:**
```yaml
- uses: actions/github-script@v7
  with:
    script: |
      await github.rest.issues.createComment({
        issue_number: context.issue.number,
        owner: context.repo.owner,
        repo: context.repo.repo,
        body: `✅ Deploy completado por @${context.actor}`
      })
```

**Añadir/quitar labels:**
```yaml
- uses: actions/github-script@v7
  with:
    script: |
      // Añadir label
      await github.rest.issues.addLabels({
        issue_number: context.issue.number,
        owner: context.repo.owner,
        repo: context.repo.repo,
        labels: ['reviewed', 'ready-to-merge']
      })
      
      // Quitar label
      await github.rest.issues.removeLabel({
        issue_number: context.issue.number,
        owner: context.repo.owner,
        repo: context.repo.repo,
        name: 'in-review'
      })
```

**Crear un issue:**
```yaml
- uses: actions/github-script@v7
  with:
    script: |
      await github.rest.issues.create({
        owner: context.repo.owner,
        repo: context.repo.repo,
        title: `❌ Build fallido - Run #${context.runNumber}`,
        body: `Ver: ${context.serverUrl}/${context.repo.owner}/${context.repo.repo}/actions/runs/${context.runId}`,
        labels: ['bug', 'ci']
      })
```

**Usar GraphQL:**
```yaml
- uses: actions/github-script@v7
  with:
    script: |
      const query = `
        query($owner: String!, $repo: String!) {
          repository(owner: $owner, name: $repo) {
            issues(states: OPEN) { totalCount }
          }
        }
      `
      const result = await github.graphql(query, {
        owner: context.repo.owner,
        repo: context.repo.repo
      })
      return result.repository.issues.totalCount
```

**Obtener el resultado de `return` como output:**
```yaml
- id: mis-datos
  uses: actions/github-script@v7
  with:
    result-encoding: string   # 'string' o 'json'
    script: |
      return 'mi-valor'

- run: echo "Resultado: ${{ steps.mis-datos.outputs.result }}"
```

**Token personalizado (para acceso cross-repo):**
```yaml
- uses: actions/github-script@v7
  with:
    github-token: ${{ secrets.MI_PAT }}   # PAT en vez de GITHUB_TOKEN
    script: |
      // Puede acceder a otros repos
      await github.rest.issues.create({
        owner: 'otra-org',
        repo: 'otro-repo',
        title: 'Notificación cross-repo'
      })
```

---

## 12. Artifact Attestations y Build Provenance

Las **Artifact Attestations** son firmas digitales criptográficas que verifican **dónde y cómo se construyó** un artefacto de software. Son una pieza fundamental de la seguridad en la cadena de suministro de software (supply chain security): permiten a cualquier usuario verificar que un binario fue producido por un workflow específico en un repositorio concreto, y no fue alterado después.

### ¿Qué son?

```
Sin attestations:
  Binario publicado → ¿Quién lo construyó? ¿Con qué código? → No se puede verificar

Con attestations:
  Binario publicado + firma OIDC → Se puede verificar que:
    - Fue construido en github.com/mi-org/mi-repo
    - Por el workflow .github/workflows/build.yml
    - A partir del commit SHA abc1234
    - En la ejecución de Actions Run #42
```

GitHub firma las attestations usando su propia infraestructura OIDC, y las almacena en el registro de transparencia **Sigstore/Rekor**, que es público e inmutable.

### actions/attest-build-provenance: para binarios

Genera una attestation que describe la procedencia (provenance) del artefacto: qué workflow lo produjo, en qué commit, en qué repositorio.

```yaml
name: Build y Attest

on:
  push:
    branches: [main]

permissions:
  id-token: write       # Necesario para firmar con OIDC
  contents: read        # Leer el código
  attestations: write   # Publicar la attestation en GitHub

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Build del binario
        run: |
          go build -o mi-binario ./cmd/main.go

      - name: Generar attestation de build provenance
        uses: actions/attest-build-provenance@v2
        with:
          subject-path: mi-binario    # Archivo a firmar
```

### actions/attest-sbom: para Software Bill of Materials

Un **SBOM** (Software Bill of Materials) es un inventario completo de todos los componentes, dependencias y librerías que componen un artefacto de software. Con `attest-sbom` se firma ese inventario criptográficamente.

```yaml
      - name: Generar SBOM con syft
        uses: anchore/sbom-action@v0
        with:
          path: mi-binario
          format: spdx-json          # o cyclonedx-json
          output-file: sbom.spdx.json

      - name: Firmar y publicar SBOM
        uses: actions/attest-sbom@v2
        with:
          subject-path: mi-binario
          sbom-path: sbom.spdx.json
```

### Formatos SBOM

| Formato | Descripción | Uso habitual |
|---|---|---|
| **SPDX** | Software Package Data Exchange. Estándar ISO 5962. | Compliance legal, licencias |
| **CycloneDX** | Formato OWASP. Más orientado a seguridad. | Vulnerability management, DevSecOps |

Ambos formatos son JSON o XML y describen el mismo concepto: qué hay dentro del software.

### Permisos requeridos

```yaml
permissions:
  id-token: write       # Para firmar con OIDC (siempre necesario)
  contents: read        # Para acceder al código
  attestations: write   # Para publicar la attestation en GitHub

  # Para imágenes de contenedor (además de los anteriores):
  packages: write       # Para asociar la attestation a la imagen en GHCR
```

### Verificación: gh attestation verify

Cualquiera puede verificar la autenticidad de un artefacto:

```bash
# Verificar un binario local
gh attestation verify mi-binario --owner=mi-org

# Verificar una imagen de contenedor
gh attestation verify oci://ghcr.io/mi-org/mi-imagen:latest --owner=mi-org

# Output esperado si es válido:
# ✓ Verification succeeded!
# - Subject: mi-binario
# - Repository: mi-org/mi-repo
# - Workflow: .github/workflows/build.yml
# - Commit: abc1234...
```

Si la verificación falla (el artefacto fue modificado o no fue construido en GitHub Actions), el comando retorna error.

### Ejemplo completo: build + attest binario + imagen de contenedor

```yaml
name: Build, Push y Attest

on:
  push:
    tags: ['v*']

permissions:
  id-token: write
  contents: read
  attestations: write
  packages: write

jobs:
  build-and-attest:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Build binario
        run: go build -o mi-app ./cmd/main.go

      - name: Attest binario
        uses: actions/attest-build-provenance@v2
        with:
          subject-path: mi-app

      - name: Login GHCR
        uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Build y Push imagen
        id: push
        uses: docker/build-push-action@v5
        with:
          push: true
          tags: ghcr.io/${{ github.repository }}:${{ github.ref_name }}

      - name: Attest imagen de contenedor
        uses: actions/attest-build-provenance@v2
        with:
          subject-name: ghcr.io/${{ github.repository }}
          subject-digest: ${{ steps.push.outputs.digest }}
          push-to-registry: true    # Asocia la attestation a la imagen en GHCR
```

---

## 13. `::add-mask::` para Enmascarar Valores Dinámicos

Los **secrets** configurados en Settings se enmascaran automáticamente en los logs — GitHub los reemplaza por `***`. Sin embargo, los valores **calculados en runtime** durante la ejecución del workflow NO están enmascarados por defecto, aunque contengan información sensible.

### El problema

```yaml
steps:
  - name: Obtener JWT temporal
    run: |
      JWT=$(curl -s https://auth.example.com/token \
        -d "client_id=${{ secrets.CLIENT_ID }}" \
        -d "client_secret=${{ secrets.CLIENT_SECRET }}" \
        | jq -r '.access_token')

      echo "Token: $JWT"    # ← ¡Aparece en el log en texto claro!
      echo "jwt=$JWT" >> $GITHUB_OUTPUT
```

En este ejemplo, `CLIENT_ID` y `CLIENT_SECRET` están enmascarados (son secrets). Pero el JWT resultante no lo está — aparecerá en los logs si se imprime o si un comando lo muestra como parte de un error.

### Solución: `::add-mask::`

El comando workflow `::add-mask::` indica a GitHub Actions que enmascare cualquier aparición posterior del valor en los logs:

```bash
echo "::add-mask::$VALOR_A_OCULTAR"
```

### Casos de uso

- JWTs generados en runtime llamando a un endpoint de autenticación
- Tokens temporales de corta vida obtenidos de vaults o servicios externos
- Valores derivados de secrets (concatenaciones, transformaciones)
- Claves de sesión generadas durante el workflow

### Ejemplo práctico

```yaml
steps:
  - name: Obtener y enmascarar token temporal
    id: auth
    run: |
      # Obtener token
      TOKEN=$(curl -s https://auth.example.com/token \
        -d "client_secret=${{ secrets.CLIENT_SECRET }}" \
        | jq -r '.access_token')

      # Enmascarar ANTES de cualquier uso
      echo "::add-mask::$TOKEN"

      # A partir de aquí, $TOKEN aparece como *** en los logs
      echo "Token obtenido correctamente"   # No imprime el valor
      echo "token=$TOKEN" >> $GITHUB_OUTPUT # El output también queda enmascarado

  - name: Usar el token
    run: |
      # ${{ steps.auth.outputs.token }} aparece como *** en los logs
      curl -H "Authorization: Bearer ${{ steps.auth.outputs.token }}" \
        https://api.example.com/deploy
```

### Reglas importantes

**El orden importa:** `::add-mask::` debe ejecutarse **antes** de cualquier step que use el valor. Una vez enmascarado, el valor queda oculto en todos los steps siguientes del mismo job.

**No usar objetos JSON completos como valor a maskear.** Si el valor contiene saltos de línea o caracteres especiales, el enmascaramiento puede ser poco fiable:

```bash
# ❌ No recomendado: JSON completo como valor a maskear
echo "::add-mask::$JSON_COMPLETO"

# ✅ Recomendado: enmascarar el campo específico sensible
TOKEN=$(echo $JSON_COMPLETO | jq -r '.access_token')
echo "::add-mask::$TOKEN"
```

**Alcance:** el enmascaramiento solo aplica al job donde se ejecuta el step. Otros jobs del mismo workflow no heredan los masks.

---

## 14. OpenSSF Scorecards

**OpenSSF Scorecards** es una herramienta open source mantenida por la Open Source Security Foundation que **evalúa automáticamente las prácticas de seguridad** de un repositorio en la cadena de suministro de software. Produce una puntuación de 0 a 10 basada en decenas de checks automatizados.

### ¿Qué evalúa?

Scorecards comprueba si el repositorio sigue las mejores prácticas de seguridad conocidas:

| Check | Qué verifica |
|---|---|
| **Pinned-Dependencies** | Actions y dependencias fijadas por SHA (no por tags) |
| **Branch-Protection** | Reglas de protección de rama activas |
| **CI-Tests** | Existencia de tests automatizados en CI |
| **Code-Review** | PRs requieren revisión antes de merge |
| **Dangerous-Workflow** | Ausencia de patrones peligrosos (`pull_request_target` inseguro) |
| **Dependency-Update-Tool** | Uso de Dependabot o Renovate |
| **Maintained** | Actividad reciente en el repositorio |
| **Fuzzing** | Existencia de fuzzing automatizado |
| **SAST** | Herramientas de análisis estático activas |
| **Vulnerabilities** | Vulnerabilidades conocidas abiertas en el repo |
| **Signed-Releases** | Releases firmados digitalmente |
| **Token-Permissions** | Permisos mínimos en los workflows |

### Integración con GitHub Actions

```yaml
# .github/workflows/scorecard.yml
name: Scorecard supply-chain security

on:
  push:
    branches: [main]
  schedule:
    - cron: '30 1 * * 1'    # Análisis semanal (lunes)

permissions:
  security-events: write    # Para publicar resultados en Code Scanning
  id-token: write           # Para publicar en el dashboard de OpenSSF
  contents: read

jobs:
  analysis:
    name: Scorecard analysis
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          persist-credentials: false    # Recomendado por Scorecards

      - name: Run analysis
        uses: ossf/scorecard-action@v2.4.0
        with:
          results_file: results.sarif
          results_format: sarif
          # Publicar badge en el README (requiere repo público o GITHUB_TOKEN con permissions)
          publish_results: true

      - name: Upload SARIF results to code-scanning
        uses: github/codeql-action/upload-sarif@v3
        with:
          sarif_file: results.sarif
```

### Integración con Code Scanning de GitHub

Cuando se sube el archivo SARIF con `upload-sarif`, los resultados de Scorecards aparecen en `Security → Code scanning alerts` del repositorio, igual que los resultados de CodeQL. Esto centraliza todos los hallazgos de seguridad en un único lugar.

```
Repositorio → Security → Code scanning alerts
  ⚠️ [Scorecard] Pinned-Dependencies: score 3/10
     Actions no fijadas por SHA en 5 workflows
  ⚠️ [Scorecard] Token-Permissions: score 5/10
     3 workflows sin permissions mínimas declaradas
```

### Consultar la puntuación

```bash
# Ver la puntuación desde la CLI de Scorecards
scorecard --repo=github.com/mi-org/mi-repo

# O ver el badge en el README (si publish_results: true):
# https://api.securityscorecards.dev/projects/github.com/mi-org/mi-repo
```

### Importancia en el examen

Scorecards es relevante en el contexto del examen porque:
- Es la herramienta estándar de la industria para medir postura de supply chain security
- Evalúa precisamente las prácticas que cubre el temario (pinning, permissions, branch protection)
- Se integra con GitHub Code Scanning vía SARIF (requiere `security-events: write`)
- Requiere `id-token: write` para publicar en el dashboard de OpenSSF

---

## 15. Preguntas de Examen

**P: ¿Qué es un Check Run y cómo se relaciona con GitHub Actions?**
→ Un Check Run es el resultado de un job individual en la API de GitHub. GitHub Actions crea automáticamente uno por cada job. Son la base de los status checks requeridos en branch protection rules.

**P: ¿Qué permiso necesita GITHUB_TOKEN para publicar alertas de Code Scanning (SARIF)?**
→ `security-events: write`

**P: ¿Para qué sirve CodeQL y cómo se integra en un workflow?**
→ CodeQL es el motor de análisis estático de GitHub que busca vulnerabilidades de seguridad en el código fuente. Se integra con tres actions: `github/codeql-action/init@v3`, `github/codeql-action/autobuild@v3` y `github/codeql-action/analyze@v3`. Los resultados se publican como alertas en la pestaña Security.

**P: ¿Qué trigger además de `push` y `pull_request` se recomienda en el workflow de CodeQL y por qué?**
→ `schedule` (cron semanal). Porque permite detectar vulnerabilidades nuevas en código que no ha cambiado (cuando se descubren nuevas queries de CodeQL), sin necesitar un push.

**P: ¿Cuándo usar `actions/github-script` en lugar de `gh` CLI?**
→ Cuando necesitas lógica JavaScript compleja, múltiples llamadas a la API, acceso a GraphQL, o quieres capturar el valor de retorno directamente como output del step.

**P: ¿Cómo obtener el resultado de return en `actions/github-script` como output del step?**
→ La action escribe automáticamente el valor retornado en `steps.<id>.outputs.result`. Se puede controlar la codificación con `result-encoding: string` o `result-encoding: json`.

**P: ¿Qué es GITHUB_TOKEN?**
→ Es un token de autenticación generado automáticamente por GitHub para cada ejecución de workflow. No necesitas configurarlo — siempre está disponible en `secrets.GITHUB_TOKEN`.

**P: ¿Qué permiso necesita un workflow para usar OIDC?**
→ `id-token: write`

**P: ¿Cuál es la diferencia entre `pull_request` y `pull_request_target`?**
→ `pull_request` ejecuta el código de la rama del PR (head), tiene permisos reducidos en forks. `pull_request_target` ejecuta el código de la rama destino (base) y tiene acceso completo a secrets aunque venga de un fork, por eso es peligroso si se hace checkout del código del PR.

**P: ¿Por qué es más seguro fijar una action por SHA que por tag?**
→ Los tags pueden ser reetiquetados a otro commit. El SHA es inmutable — el código que referencia un SHA nunca puede cambiar.

**P: ¿Cómo evitar script injection con `github.event.pull_request.title`?**
→ Pasarlo como variable de entorno en lugar de interpolarlo directamente en el script:
```yaml
env:
  TITLE: ${{ github.event.pull_request.title }}
run: echo "$TITLE"  # no echo "${{ github.event.pull_request.title }}"
```

**P: ¿En qué nivel se pueden definir secrets en GitHub?**
→ En 3 niveles: Organización (disponibles para repos de la org), Repositorio (disponibles para ese repo), y Environment (solo para jobs que usan ese environment).

**P: ¿Qué ventaja tiene OIDC sobre secretos de larga duración?**
→ No hay secretos que almacenar ni que puedan filtrarse. Las credenciales son temporales (≈15 min). Se puede restringir por rama/repo/org en la trust policy del proveedor cloud.


**P: ¿Cuál es el tamaño máximo de un secret en GitHub?**
→ 48 KB.

**P: ¿Cuántos secrets puede tener como máximo una organización?**
→ 1.000 secrets.

**P: ¿Se pueden pasar secrets de environment a un workflow reutilizable via `workflow_call`?**
→ No. Los secrets de environment no pueden pasarse via `workflow_call`. Solo pueden pasarse secrets de repositorio u organización. Los secrets de environment solo están disponibles para el job que declara el `environment:` correspondiente.

**P: Si el workflow A llama al workflow B que a su vez llama al workflow C, ¿los secrets de A se propagan automáticamente hasta C?**
→ No. Los secrets solo se pasan al workflow llamado directamente. No se propagan automáticamente en cadenas de workflows.

**P: ¿Qué son las Artifact Attestations y qué problema resuelven?**
→ Son firmas digitales criptográficas que verifican dónde y cómo se construyó un artefacto de software. Resuelven el problema de supply chain security: permiten verificar que un binario fue producido por un workflow concreto en un repositorio específico y no fue alterado.

**P: ¿Qué permisos son necesarios para usar `actions/attest-build-provenance`?**
→ `id-token: write` (para firmar con OIDC), `contents: read` y `attestations: write`. Para imágenes de contenedor se necesita además `packages: write`.

**P: ¿Cómo se verifica una attestation de un artefacto?**
→ Con el comando `gh attestation verify <archivo> --owner=OWNER`. Para imágenes de contenedor: `gh attestation verify oci://ghcr.io/org/imagen:tag --owner=OWNER`.

**P: ¿Qué es un SBOM y qué formatos estándar existen?**
→ Un SBOM (Software Bill of Materials) es un inventario de todos los componentes, dependencias y librerías de un artefacto de software. Los dos formatos estándar son SPDX (ISO 5962, orientado a licencias) y CycloneDX (OWASP, orientado a seguridad y vulnerabilidades).

**P: ¿Los secrets configurados en GitHub Settings se enmascaran automáticamente en los logs?**
→ Sí. Pero los valores calculados en runtime (JWTs generados, tokens temporales, valores derivados) NO se enmascaran automáticamente. Para esos valores hay que usar `echo "::add-mask::$VALOR"` explícitamente.

**P: ¿Cuándo debe ejecutarse `::add-mask::` en relación con el uso del valor?**
→ Antes de cualquier step que use o muestre el valor. Una vez ejecutado, el valor queda enmascarado en todos los steps siguientes del mismo job.

**P: ¿Por qué no se recomienda usar un objeto JSON completo como valor de `::add-mask::`?**
→ Porque si el valor contiene saltos de línea o caracteres especiales, el enmascaramiento puede ser poco fiable. Se recomienda enmascarar únicamente el campo específico sensible extraído del JSON.

**P: ¿Qué es OpenSSF Scorecards y qué puntuación produce?**
→ Es una herramienta open source que evalúa automáticamente las prácticas de seguridad de un repositorio en la cadena de suministro. Produce una puntuación de 0 a 10 basada en checks como pinning de dependencias, branch protection, revisión de código, uso de CI, etc.

**P: ¿Qué action se usa para ejecutar OpenSSF Scorecards en un workflow?**
→ `ossf/scorecard-action`

**P: ¿Qué permiso de GITHUB_TOKEN necesita Scorecards para publicar resultados en Code Scanning?**
→ `security-events: write` (para subir el archivo SARIF con los resultados).

**P: ¿Qué dos permisos específicos necesita Scorecards además de `contents: read`?**
→ `security-events: write` (para publicar en Code Scanning vía SARIF) e `id-token: write` (para publicar en el dashboard público de OpenSSF).
