# 🏢 GitHub Actions: Administración de Organización y CI/CD Empresarial

## 📚 Índice
1. [Políticas de Organización](#1-políticas-de-organización)
2. [workflow_run: Workflows Encadenados](#2-workflow_run-workflows-encadenados)
3. [Estrategia de Branching con Actions](#3-estrategia-de-branching-con-actions)
4. [Testing Avanzado: Separar Suites y Publicar Resultados](#4-testing-avanzado)
5. [Versionado Semántico Automático](#5-versionado-semántico-automático)
6. [Notificaciones Externas (Slack, Teams)](#6-notificaciones-externas)
7. [Docker Avanzado: Multi-stage y Layer Cache](#7-docker-avanzado)
8. [Estrategias de Despliegue Avanzadas y Rollback](#8-estrategias-de-despliegue-avanzadas-y-rollback)
9. [Required Workflows](#9-required-workflows)
10. [Audit Logs de GitHub Actions](#10-audit-logs-de-github-actions)
11. [Variables de Organización y Repositorio](#11-variables-de-organización-y-repositorio)
12. [Workflow Templates (Starter Workflows)](#12-workflow-templates-starter-workflows)
13. [Custom Deployment Protection Rules](#13-custom-deployment-protection-rules)
14. [Opciones Adicionales de Environments](#14-opciones-adicionales-de-environments)
15. [Preguntas de Examen](#15-preguntas-de-examen)

---

## 1. Políticas de Organización

### ¿Qué puede configurar un admin de organización?

Un **Organization Owner** controla GitHub Actions en todos los repos de la org desde:
`Organization Settings → Actions → General`

### Allowed Actions: qué actions pueden usar los repos

```
Opciones:
  ○ Allow all actions and reusable workflows
    → Cualquier action de cualquier repo público

  ○ Allow enterprise, and select non-enterprise, actions and reusable workflows
    → Solo actions de la misma empresa + lista blanca

  ● Allow only actions and reusable workflows created by GitHub
    → Solo actions de github.com/actions/* y github.com/github/*

  ○ Allow specific actions and reusable workflows
    → Lista explícita: "actions/checkout@*, actions/setup-node@*"
```

**En el workflow:** Si una action no está en la lista permitida, el job falla con error de política.

### Desactivar GitHub Actions para un repo o forks

```
Repo Settings → Actions → General → Actions permissions:
  ○ Allow all actions     (default)
  ○ Disable Actions       (ningún workflow se ejecuta)
  ○ Allow local actions only (solo actions del mismo repo)
```

**Para forks de repos públicos**, el admin puede requerir aprobación antes de ejecutar workflows de contributors nuevos:

```
Org Settings → Actions → General → Fork pull request workflows:
  ○ Require approval for all outside collaborators
  ○ Require approval for first-time contributors (default)
  ○ No approval required
```

### Runners a nivel de organización

Un admin puede crear **runners de organización** disponibles para múltiples repos:

```
Org Settings → Actions → Runners → New org runner
```

Y controlar el acceso con **Runner Groups**:
```
Org Settings → Actions → Runner groups → New group
  Nombre: "production-runners"
  Acceso: Solo repos seleccionados
  Runners: prod-server-1, prod-server-2
```

### Políticas de retención de logs y artifacts

```
Org Settings → Actions → General → Artifact and log retention
Rango: 1 – 400 días (aplica como default a todos los repos de la org)
Cada repo puede sobreescribirlo en su Settings → Actions → General
```

### Secrets y Variables de Organización

```
Org Settings → Secrets and variables → Actions

Secrets de org: disponibles para repos seleccionados o todos
Variables de org: lo mismo, pero accesibles con ${{ vars.NOMBRE }}

Control de acceso:
  ○ All repositories
  ○ Private repositories
  ○ Selected repositories (lista explícita)
```

### Prioridad y sobreescritura de políticas org → repo

Las políticas de organización son el **techo máximo** permitido. Un repo puede ser más restrictivo, pero nunca más permisivo que la org:

```
Org permite: "Allow specific: actions/checkout@*, actions/setup-node@*"
  Repo puede: restringir aún más (solo actions/checkout@*)
  Repo NO puede: ampliar para permitir actions/setup-python@* (bloqueado por org)
```

Si un secret de org y un secret de repo tienen el **mismo nombre**, el secret de **repositorio tiene prioridad** (sobreescribe al de org para ese repo).

### Deshabilitar un workflow individual sin tocar la política global

En lugar de deshabilitar todos los Actions de un repo, puedes deshabilitar un workflow concreto desde la UI:

```
Repo → Actions → [nombre del workflow] → botón "..." → Disable workflow
```

Esto añade el workflow al estado `disabled_manually` — no se dispara por eventos, pero sí se puede lanzar manualmente desde la UI o API. Útil para pausar un workflow de deploy sin afectar el CI.

```bash
# También desde la API / gh CLI:
gh workflow disable nombre-workflow.yml
gh workflow enable nombre-workflow.yml
```

### Límites de uso por plan

No está documentado en el documento, pero afecta directamente a decisiones de CI/CD empresarial:

| Plan | Minutos/mes (repos privados) | Storage artifacts | Runners simultáneos |
|---|---|---|---|
| Free | 2.000 min | 500 MB | — |
| Pro | 3.000 min | 1 GB | — |
| Team | 3.000 min | 2 GB | — |
| Enterprise | 50.000 min | 50 GB | — |
| Self-hosted | Ilimitado | — | Según tu infra |

> Los repos **públicos** tienen minutos y storage ilimitados gratis. Los multiplicadores de OS (×2 Windows, ×10 macOS) aplican solo a repos privados.

---

## 2. workflow_run: Workflows Encadenados

`workflow_run` dispara un workflow cuando **otro workflow termina**. Resuelve el problema de PRs de forks que no tienen acceso a secrets.

### Caso de uso: CI seguro + comentario con token

```
Problema:
  PR de fork → pull_request → sin acceso a secrets
               → no puede comentar en el PR con GITHUB_TOKEN de write

Solución:
  PR de fork → pull_request    → CI (sin secrets, código del PR)
                    │ termina
                    ▼
               workflow_run → Comentar resultado (con secrets, código de main)
```

```yaml
# Workflow 1: CI (se dispara en el PR, ejecuta código del fork)
# .github/workflows/ci.yml
name: CI
on:
  pull_request:
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: npm test
      - name: Guardar resultado para el siguiente workflow
        if: always()
        run: echo "${{ job.status }}" > ci-result.txt
      - uses: actions/upload-artifact@v4
        if: always()
        with:
          name: ci-result
          path: ci-result.txt
```

```yaml
# Workflow 2: Comentar (se dispara cuando CI termina, tiene secrets)
# .github/workflows/comment.yml
name: Comment PR
on:
  workflow_run:
    workflows: ["CI"]        # ← Nombre exacto del workflow que lo dispara
    types: [completed]       # ← Solo cuando termina (no cuando empieza)

jobs:
  comment:
    runs-on: ubuntu-latest
    permissions:
      pull-requests: write
    steps:
      - name: Descargar resultado del CI
        uses: actions/download-artifact@v4
        with:
          name: ci-result
          github-token: ${{ secrets.GITHUB_TOKEN }}
          # Necesario especificar el run_id del workflow que lo generó:
          run-id: ${{ github.event.workflow_run.id }}
      
      - name: Comentar en el PR
        uses: actions/github-script@v7
        with:
          script: |
            const fs = require('fs')
            const status = fs.readFileSync('ci-result.txt', 'utf8').trim()
            const icon = status === 'success' ? '✅' : '❌'
            
            // Obtener el PR asociado al workflow_run
            const prs = await github.rest.pulls.list({
              owner: context.repo.owner,
              repo: context.repo.repo,
              head: `${context.payload.workflow_run.head_repository.owner.login}:${context.payload.workflow_run.head_branch}`
            })
            
            if (prs.data.length > 0) {
              await github.rest.issues.createComment({
                owner: context.repo.owner,
                repo: context.repo.repo,
                issue_number: prs.data[0].number,
                body: `${icon} CI ${status}`
              })
            }
```

### Filtros de workflow_run

```yaml
on:
  workflow_run:
    workflows: ["CI", "Build"]   # Uno o varios workflows
    types:
      - completed    # Cuando termina (con cualquier resultado)
      - requested    # Cuando se solicita (antes de ejecutar)
      - in_progress  # Cuando empieza a ejecutar
    branches:
      - main         # Solo cuando el workflow padre se ejecutó en esta rama
```

### Trampa: el nombre en `workflows:` debe coincidir exactamente con `name:`

El valor de `workflows: ["CI"]` debe coincidir **exactamente** con el campo `name:` del workflow padre en su archivo YAML — no con el nombre del archivo `.yml`. Si el workflow no tiene `name:`, se usa el nombre del archivo sin extensión:

```yaml
# ci.yml — sin campo name:
on: pull_request
jobs:
  test: ...
# → en workflow_run hay que referenciarlo como: workflows: ["ci"]  (nombre del archivo)
```

```yaml
# ci.yml — con campo name:
name: My CI Pipeline
on: pull_request
jobs:
  test: ...
# → en workflow_run hay que referenciarlo como: workflows: ["My CI Pipeline"]
```

### Comprobar la conclusión del workflow padre antes de actuar

`workflow_run` se dispara cuando el padre termina con **cualquier conclusión** (success, failure, cancelled). Sin filtrar por conclusión, el segundo workflow se ejecuta incluso cuando el CI falló:

```yaml
jobs:
  deploy:
    # ← Sin esta condición, se despliega aunque el CI haya fallado
    if: github.event.workflow_run.conclusion == 'success'
    runs-on: ubuntu-latest
    steps:
      - run: ./deploy.sh
```

Valores posibles de `conclusion`: `success`, `failure`, `cancelled`, `skipped`, `timed_out`, `action_required`, `neutral`.

### `workflow_run` NO puede encadenar más de un salto entre repos

`workflow_run` solo puede referenciar workflows del **mismo repositorio**. No se puede usar para disparar workflows en repositorios diferentes:

```
Repo A / ci.yml  →  workflow_run  →  Repo A / deploy.yml   ✅
Repo A / ci.yml  →  workflow_run  →  Repo B / deploy.yml   ❌ No es posible

Para repos distintos → usar repository_dispatch (API HTTP)
```

### Acceder al contexto del workflow padre

```yaml
steps:
  - run: |
      echo "Workflow que lo disparó: ${{ github.event.workflow_run.name }}"
      echo "Conclusión: ${{ github.event.workflow_run.conclusion }}"
      echo "Run ID del padre: ${{ github.event.workflow_run.id }}"
      echo "Branch del padre: ${{ github.event.workflow_run.head_branch }}"
      echo "SHA del padre: ${{ github.event.workflow_run.head_sha }}"
      echo "Actor del padre: ${{ github.event.workflow_run.triggering_actor.login }}"
```

---

## 3. Estrategia de Branching con Actions

### GitFlow completo con GitHub Actions

```
feature/* → push → lint + test
develop   → push → lint + test + build + deploy-staging
main      → push → lint + test + build + deploy-prod (con approval)
```

```yaml
name: GitFlow CI/CD

on:
  push:
    branches:
      - main
      - develop
      - 'feature/**'
      - 'hotfix/**'
  pull_request:
    branches: [main, develop]

jobs:
  lint:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with: { node-version: '20', cache: 'npm' }
      - run: npm ci && npm run lint

  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with: { node-version: '20', cache: 'npm' }
      - run: npm ci && npm test

  build:
    needs: [lint, test]
    # Solo en develop y main, no en feature branches
    if: github.ref == 'refs/heads/main' || github.ref == 'refs/heads/develop'
    runs-on: ubuntu-latest
    outputs:
      version: ${{ steps.ver.outputs.version }}
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with: { node-version: '20', cache: 'npm' }
      - run: npm ci && npm run build
      - id: ver
        run: echo "version=$(node -p "require('./package.json').version")" >> $GITHUB_OUTPUT
      - uses: actions/upload-artifact@v4
        with:
          name: build-${{ github.sha }}
          path: dist/

  deploy-staging:
    needs: build
    if: github.ref == 'refs/heads/develop'
    runs-on: ubuntu-latest
    environment:
      name: staging
      url: https://staging.example.com
    steps:
      - uses: actions/download-artifact@v4
        with:
          name: build-${{ github.sha }}
      - run: ./deploy.sh staging
        env:
          DEPLOY_KEY: ${{ secrets.STAGING_DEPLOY_KEY }}

  deploy-prod:
    needs: build
    if: github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    environment:
      name: production        # ← Tiene Required Reviewers configurado
      url: https://example.com
    steps:
      - uses: actions/download-artifact@v4
        with:
          name: build-${{ github.sha }}
      - run: ./deploy.sh production
        env:
          DEPLOY_KEY: ${{ secrets.PROD_DEPLOY_KEY }}
```

### Trampa: el job `build` queda `skipped` en feature branches

En el ejemplo anterior, `build` tiene `if: github.ref == 'refs/heads/main' || ...develop`. Cuando se hace push a una feature branch, `build` se salta. Pero `deploy-staging` y `deploy-prod` dependen de `build` — si `build` es `skipped`, los jobs de deploy **también se saltan automáticamente**, incluso si no tienen `if:` propio:

```
push a feature/mi-rama:
  lint   → ✅ success
  test   → ✅ success
  build  → ⏭️ skipped  (if: es false)
  deploy-staging → ⏭️ skipped automáticamente (needs: build → skipped)
  deploy-prod    → ⏭️ skipped automáticamente
```

Esto es el comportamiento correcto aquí, pero hay que tenerlo en cuenta si se añaden jobs de notificación con `if: always()` — recibirán `skipped` en `needs.build.result`, no `success`.

### Alternativa: Trunk-based Development

GitFlow es complejo. En equipos que hacen deploys frecuentes se usa **trunk-based**: una sola rama principal (`main`) con feature flags, y ramas de vida muy corta (horas, no días):

```
feature/* → PR → merge a main → deploy automático a producción
                                (feature flags controlan la visibilidad)
```

```yaml
name: Trunk-based CI/CD

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  ci:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: npm ci && npm run lint && npm test

  deploy:
    needs: ci
    if: github.ref == 'refs/heads/main' && github.event_name == 'push'
    runs-on: ubuntu-latest
    environment:
      name: production
    steps:
      - run: ./deploy.sh
```

| | GitFlow | Trunk-based |
|---|---|---|
| Ramas long-lived | `main`, `develop`, `release/*` | Solo `main` |
| Frecuencia de deploy | Sprints / releases | Múltiples veces al día |
| Complejidad del workflow | Alta | Baja |
| Feature isolation | Por rama | Por feature flag |
| Indicado para | Releases versionadas, software empaquetado | SaaS, apps web |

### Patrón Release Candidate: rama `release/*`

En GitFlow, cuando las funcionalidades del sprint están completas se crea una rama `release/*` para congelarlas y preparar la producción. Esta rama dispara su propio pipeline dedicado — distinto del de `develop` y del de `main`:

```
feature/* → develop → release/1.2.0 → main (tag v1.2.0)
                              ↑
              congelar features, tests de regresión, staging, UAT
```

```yaml
# .github/workflows/cd-release.yml
name: Release Candidate Pipeline

on:
  # Opción A: cuando se crea/actualiza la rama release/*
  push:
    branches:
      - 'release/**'
  # Opción B: cuando se publica un tag RC
  push:
    tags:
      - 'v*-rc*'    # v1.2.0-rc1, v1.2.0-rc2 ...

jobs:
  build-rc:
    runs-on: ubuntu-latest
    outputs:
      image-tag: ${{ steps.meta.outputs.version }}
    steps:
      - uses: actions/checkout@v4

      - name: Extraer versión del nombre de rama o tag
        id: ver
        run: |
          # Para rama: release/1.2.0  → version=1.2.0-rc
          # Para tag:  v1.2.0-rc1     → version=1.2.0-rc1
          REF="${GITHUB_REF#refs/*/}"
          VERSION="${REF#release/}-rc"
          echo "version=${VERSION}" >> $GITHUB_OUTPUT

      - name: Build de producción (optimizado, sin debug)
        run: npm ci && npm run build:prod

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Login a GHCR
        uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Build y Push imagen RC
        id: meta
        uses: docker/metadata-action@v5
        with:
          images: ghcr.io/${{ github.repository }}
          tags: |
            type=raw,value=${{ steps.ver.outputs.version }}
            type=sha,prefix=rc-

      - uses: docker/build-push-action@v5
        with:
          push: true
          tags: ${{ steps.meta.outputs.tags }}
          cache-from: type=gha
          cache-to: type=gha,mode=max

  regression-tests:
    needs: build-rc
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: npm ci && npm run test:regression   # suite completa

  deploy-staging:
    needs: regression-tests
    runs-on: ubuntu-latest
    environment:
      name: staging
      url: https://staging.example.com
    steps:
      - run: |
          ./deploy.sh staging ghcr.io/${{ github.repository }}:${{ needs.build-rc.outputs.image-tag }}

  # Gate manual: QA o Product Owner deben aprobar en la UI de GitHub
  # antes de que este job se ejecute (configurado en el environment "uat")
  approve-uat:
    needs: deploy-staging
    runs-on: ubuntu-latest
    environment:
      name: uat          # ← Protection rule: Required reviewers configurado aquí
    steps:
      - name: Promover imagen a candidate
        run: |
          docker pull ghcr.io/${{ github.repository }}:${{ needs.build-rc.outputs.image-tag }}
          docker tag  ghcr.io/${{ github.repository }}:${{ needs.build-rc.outputs.image-tag }} \
                      ghcr.io/${{ github.repository }}:candidate
          docker push ghcr.io/${{ github.repository }}:candidate
          echo "## Release Candidate aprobado" >> $GITHUB_STEP_SUMMARY
          echo "Imagen: \`ghcr.io/${{ github.repository }}:candidate\`" >> $GITHUB_STEP_SUMMARY
          echo "Aprobado por: ${{ github.actor }}" >> $GITHUB_STEP_SUMMARY
```

**Flujo visual:**

```
push release/1.2.0
  ↓
build-rc    → imagen: ghcr.io/org/app:1.2.0-rc
  ↓
regression-tests
  ↓
deploy-staging  → auto deploy a staging
  ↓
approve-uat     ← PAUSA (espera aprobación de QA en GitHub UI)
  ↓ (aprobado)
imagen promovida a :candidate  → lista para cd-production.yml
```

**Diferencia clave con el pipeline de `develop`:**

| | `develop` | `release/*` |
|---|---|---|
| Build | Con flags de debug | Optimizado (producción) |
| Tests | Integración | Regresión completa |
| Imagen tag | `develop-<sha>` | `1.2.0-rc`, `candidate` |
| Gate | Automático | Aprobación manual (UAT) |
| Secretos | Dev secrets | Staging/UAT secrets |

---

## 4. Testing Avanzado

### Separar suites de tests en jobs paralelos

```yaml
jobs:
  unit-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: npm ci && npm run test:unit -- --reporter=junit --output=unit-results.xml
      - uses: actions/upload-artifact@v4
        if: always()
        with:
          name: unit-test-results
          path: unit-results.xml

  integration-tests:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:15
        env: { POSTGRES_PASSWORD: test }
        ports: ['5432:5432']
        options: --health-cmd pg_isready --health-interval 5s --health-retries 5
    steps:
      - uses: actions/checkout@v4
      - run: npm ci && npm run test:integration -- --reporter=junit --output=integration-results.xml
      - uses: actions/upload-artifact@v4
        if: always()
        with:
          name: integration-test-results
          path: integration-results.xml

  e2e-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with: { node-version: '20' }
      - run: npm ci
      - run: npx playwright install --with-deps chromium
      - run: npm run test:e2e
      - uses: actions/upload-artifact@v4
        if: always()
        with:
          name: playwright-report
          path: playwright-report/
```

### Publicar resultados como annotations en el PR

```yaml
  publish-results:
    needs: [unit-tests, integration-tests]
    if: always()
    runs-on: ubuntu-latest
    permissions:
      checks: write          # ← Necesario para crear check runs con anotaciones
      pull-requests: write
    steps:
      - uses: actions/download-artifact@v4
        with:
          name: unit-test-results

      - name: Publicar resultados como Check Run
        uses: dorny/test-reporter@v1
        with:
          name: Unit Tests
          path: unit-results.xml
          reporter: java-junit          # junit, mocha-json, dotnet-trx...
          fail-on-error: true           # Falla el job si hay tests fallidos
```

### Cobertura de código: publicar como comentario en PR

```yaml
  coverage:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: npm ci && npm run test:coverage   # genera coverage/lcov.info

      - name: Publicar cobertura como comentario en PR
        uses: romeovs/lcov-reporter-action@v0.3.1
        with:
          lcov-file: ./coverage/lcov.info
          github-token: ${{ secrets.GITHUB_TOKEN }}
          # Muestra diff de cobertura entre la rama del PR y main
```

### Tests de integración con docker-compose (stack completo)

Cuando tus tests necesitan un stack completo (tu app + sus dependencias) en lugar de solo las dependencias:

```yaml
  integration-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Login al registry (si las imágenes son privadas)
        uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Arrancar stack completo
        run: docker compose -f docker-compose.test.yml up -d

      - name: Esperar a que los servicios estén listos
        run: |
          timeout 120 bash -c '
            until docker compose -f docker-compose.test.yml ps | grep -E "(healthy|running)"; do
              echo "Esperando servicios..."; sleep 5
            done'

      - name: Ejecutar tests de integración
        run: npm run test:integration
        env:
          API_URL: http://localhost:3000

      - name: Logs en caso de fallo
        if: failure()
        run: docker compose -f docker-compose.test.yml logs

      - name: Teardown
        if: always()
        run: docker compose -f docker-compose.test.yml down -v
```

> 💡 **`docker-compose.test.yml` separado**: Es buena práctica tener un compose de test independiente del de desarrollo, que use imágenes construidas desde el `Dockerfile` local del repo en lugar de imágenes de producción:
> ```yaml
> # docker-compose.test.yml
> services:
>   app:
>     build: .          # ← Construye la imagen local en cada test
>     ...
> ```

### Trampa: `dorny/test-reporter` falla en PRs de forks

`dorny/test-reporter` necesita `checks: write`, pero en PRs de forks el `GITHUB_TOKEN` solo tiene `read`. Solución: usar `workflow_run` para publicar los resultados en un segundo workflow con permisos completos:

```yaml
# Workflow 1: tests (sin permisos write)
name: Tests
on: pull_request
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - run: npm test -- --reporter=junit --output=results.xml
      - uses: actions/upload-artifact@v4
        if: always()
        with: { name: test-results, path: results.xml }

# Workflow 2: publicar resultados (con permisos write)
name: Publish Test Results
on:
  workflow_run:
    workflows: ["Tests"]
    types: [completed]
jobs:
  report:
    runs-on: ubuntu-latest
    permissions:
      checks: write
    steps:
      - uses: actions/download-artifact@v4
        with:
          name: test-results
          run-id: ${{ github.event.workflow_run.id }}
          github-token: ${{ secrets.GITHUB_TOKEN }}
      - uses: dorny/test-reporter@v1
        with:
          name: Test Results
          path: results.xml
          reporter: java-junit
```

### Smoke Tests contra entorno desplegado

Los smoke tests (tests de humo) validan que el servicio recién desplegado responde correctamente **desde fuera**, atacando la URL real del entorno. Son ligeros (5–10 peticiones clave) y bloquean el pipeline si el entorno no está sano.

```yaml
  smoke-tests:
    needs: deploy-staging          # Se ejecuta justo después del deploy
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Esperar a que el entorno esté listo
        run: |
          # Reintentar hasta 60s antes de fallar
          timeout 60 bash -c '
            until curl -sf https://staging.example.com/health; do
              echo "Esperando servicio..."; sleep 5
            done'

      - name: Smoke test: health check
        run: |
          STATUS=$(curl -sf -o /dev/null -w "%{http_code}" https://staging.example.com/health)
          if [ "$STATUS" != "200" ]; then
            echo "❌ Health check falló: HTTP $STATUS"
            exit 1
          fi
          echo "✅ Health check OK"

      - name: Smoke test: endpoint principal
        run: |
          curl -sf https://staging.example.com/api/v1/ping \
            -H "Authorization: Bearer ${{ secrets.STAGING_API_TOKEN }}" \
            | jq '.status == "ok"'

      - name: Smoke test: endpoint crítico de negocio
        run: |
          RESPONSE=$(curl -sf https://staging.example.com/api/v1/products?limit=1 \
            -H "Authorization: Bearer ${{ secrets.STAGING_API_TOKEN }}")
          echo "$RESPONSE" | jq '.data | length > 0'

      - name: Publicar resultado en Job Summary
        if: always()
        run: |
          echo "## Smoke Tests — Staging" >> $GITHUB_STEP_SUMMARY
          echo "URL: https://staging.example.com" >> $GITHUB_STEP_SUMMARY
          echo "SHA: ${{ github.sha }}" >> $GITHUB_STEP_SUMMARY
          echo "Resultado: ${{ job.status }}" >> $GITHUB_STEP_SUMMARY
```

**Patrón con rollback automático si los smoke tests fallan:**

```yaml
  smoke-tests:
    needs: deploy-staging
    runs-on: ubuntu-latest
    steps:
      - name: Smoke tests
        id: smoke
        run: |
          curl -sf https://staging.example.com/health || exit 1
          curl -sf https://staging.example.com/api/v1/ping || exit 1

  rollback-on-failure:
    needs: smoke-tests
    if: failure()               # ← Solo si smoke-tests falló
    runs-on: ubuntu-latest
    steps:
      - name: Revertir a versión anterior
        run: |
          # Obtener el artifact del run exitoso anterior
          PREV_SHA=$(gh run list --workflow=cd-develop.yml --status=success \
            --limit=2 --json headSha --jq '.[1].headSha')
          ./deploy.sh staging ghcr.io/${{ github.repository }}:${PREV_SHA}
          echo "⚠️ Rollback ejecutado a SHA: ${PREV_SHA}" >> $GITHUB_STEP_SUMMARY
        env:
          GH_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

> ⚠️ Los smoke tests deben ejecutarse **contra la URL real del entorno** desplegado, no contra localhost. Para ello la URL se obtiene del campo `url:` del environment o de un output del job de deploy.

---

## 5. Versionado Semántico Automático

### release-please: releases automáticos por tipo de commit

**Commits convencionales** es una convención de formato para los mensajes de commit: `tipo: descripción`. Los tipos más comunes son `feat` (nueva funcionalidad), `fix` (corrección de bug) y `chore`/`docs` (mantenimiento sin cambio funcional). Un `!` después del tipo indica un cambio incompatible (`feat!:`).

Esta convención permite a herramientas como `release-please` **leer el historial de commits y decidir automáticamente** qué versión publicar siguiente (semver: MAJOR.MINOR.PATCH) sin intervención manual:

```
feat: nueva funcionalidad     → bump MINOR  (1.0.0 → 1.1.0)
fix: corrección de bug        → bump PATCH  (1.0.0 → 1.0.1)
feat!: cambio incompatible    → bump MAJOR  (1.0.0 → 2.0.0)
chore: mantenimiento          → sin release
docs: documentación           → sin release
```

`release-please` analiza los commits convencionales (`feat:`, `fix:`, `chore:`) y:
1. Crea/actualiza automáticamente un PR de release con el CHANGELOG generado
2. Al mergearlo, crea el tag semver y el GitHub Release

```yaml
# .github/workflows/release-please.yml
name: Release Please

on:
  push:
    branches: [main]

permissions:
  contents: write
  pull-requests: write

jobs:
  release-please:
    runs-on: ubuntu-latest
    steps:
      - uses: googleapis/release-please-action@v4
        id: release
        with:
          token: ${{ secrets.GITHUB_TOKEN }}
          release-type: node    # node, python, go, java, simple...

      # Solo ejecutar si se creó un release (se mergeó el PR de release)
      - uses: actions/checkout@v4
        if: ${{ steps.release.outputs.release_created }}

      - name: Build y publicar
        if: ${{ steps.release.outputs.release_created }}
        run: |
          echo "Nueva versión: ${{ steps.release.outputs.tag_name }}"
          npm run build
          npm publish
        env:
          NODE_AUTH_TOKEN: ${{ secrets.NPM_TOKEN }}
```

**Outputs de release-please:**

| Output | Descripción |
|--------|-------------|
| `release_created` | `true` si se creó un release en este push |
| `tag_name` | Tag creado, ej: `v1.2.3` |
| `major` / `minor` / `patch` | Componentes del semver |
| `pr` | Número del PR de release (si está abierto) |

### Commits convencionales → versión

```
feat: nueva funcionalidad     → bump MINOR  (1.0.0 → 1.1.0)
fix: corrección de bug        → bump PATCH  (1.0.0 → 1.0.1)
feat!: cambio incompatible    → bump MAJOR  (1.0.0 → 2.0.0)
chore: mantenimiento          → sin release
docs: documentación           → sin release
```

### Scope en commits convencionales

El scope es información opcional entre paréntesis que indica el módulo afectado. `release-please` lo usa para organizar el CHANGELOG por secciones:

```
feat(auth): añadir login con Google      → CHANGELOG sección "auth"
fix(api): corregir timeout en /users     → CHANGELOG sección "api"
feat!: cambiar formato de respuesta      → BREAKING CHANGE, bump MAJOR
```

### Trampa: PRs de release NO deben mergearse con squash

`release-please` lee el historial de commits de `main` para generar el CHANGELOG. Si el PR de release se mergea con **squash**, los commits individuales desaparecen y `release-please` no puede calcular la siguiente versión correctamente.

```
✅ Merge commit (--no-ff):  conserva todos los commits individuales
❌ Squash and merge:        aplana todo en 1 commit → release-please pierde el historial
❌ Rebase and merge:        puede funcionar, pero cambia los SHAs

→ Configurar branch protection de main para requerir "Merge commit"
```

### release-please en monorepos (múltiples paquetes)

Para repositorios con varios paquetes independientes, `release-please` puede gestionar versiones por paquete con `release-please-config.json`:

```json
// release-please-config.json
{
  "packages": {
    "packages/api": {
      "release-type": "node",
      "package-name": "@mi-org/api"
    },
    "packages/ui": {
      "release-type": "node",
      "package-name": "@mi-org/ui"
    }
  }
}
```

```yaml
# El workflow no cambia — release-please detecta la config automáticamente
- uses: googleapis/release-please-action@v4
  with:
    token: ${{ secrets.GITHUB_TOKEN }}
    # No necesita release-type cuando hay release-please-config.json
```

Genera PRs de release **independientes** por paquete cuando hay cambios en su directorio.

---

## 6. Notificaciones Externas

### Notificar a Slack en fallo/éxito

```yaml
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - run: npm run build

  notify:
    needs: build
    if: always()
    runs-on: ubuntu-latest
    steps:
      - name: Notificar a Slack
        uses: slackapi/slack-github-action@v1
        with:
          payload: |
            {
              "text": "${{ needs.build.result == 'success' && '✅' || '❌' }} Build ${{ needs.build.result }} en `${{ github.ref_name }}`",
              "blocks": [
                {
                  "type": "section",
                  "text": {
                    "type": "mrkdwn",
                    "text": "*${{ github.repository }}* - ${{ github.workflow }}\n${{ needs.build.result == 'success' && '✅ Éxito' || '❌ Fallo' }} en `${{ github.ref_name }}`\n<${{ github.server_url }}/${{ github.repository }}/actions/runs/${{ github.run_id }}|Ver ejecución>"
                  }
                }
              ]
            }
        env:
          SLACK_WEBHOOK_URL: ${{ secrets.SLACK_WEBHOOK_URL }}
          SLACK_WEBHOOK_TYPE: INCOMING_WEBHOOK
```

### Notificar solo en fallo en main

```yaml
  notify-failure:
    needs: [lint, test, build]
    if: failure() && github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    steps:
      - name: Slack en fallo
        uses: slackapi/slack-github-action@v1
        with:
          payload: |
            {"text": "❌ Pipeline fallido en main: <${{ github.server_url }}/${{ github.repository }}/actions/runs/${{ github.run_id }}|Ver logs>"}
        env:
          SLACK_WEBHOOK_URL: ${{ secrets.SLACK_WEBHOOK_URL }}
          SLACK_WEBHOOK_TYPE: INCOMING_WEBHOOK
```

### Webhook genérico (Teams, Discord, etc.)

```yaml
      - name: Notificar via webhook
        run: |
          curl -X POST "${{ secrets.WEBHOOK_URL }}" \
            -H "Content-Type: application/json" \
            -d '{
              "text": "Deploy ${{ job.status }} en ${{ github.ref_name }}",
              "runUrl": "${{ github.server_url }}/${{ github.repository }}/actions/runs/${{ github.run_id }}"
            }'
```

### Trampa: inyección en el payload de Slack con valores del contexto

Si el título del PR o el mensaje de commit contienen comillas o caracteres JSON especiales, el payload se rompe. Nunca interpolar directamente valores de contexto que vienen del usuario en JSON inline:

```yaml
# ⚠️ ROTO si github.event.head_commit.message contiene " o \
- run: |
    curl -X POST "${{ secrets.SLACK_URL }}" \
      -d '{"text": "Commit: ${{ github.event.head_commit.message }}"}'

# ✅ SEGURO: pasar como variable de entorno y construir el JSON con jq
- run: |
    jq -n \
      --arg msg "$COMMIT_MSG" \
      --arg url "$RUN_URL" \
      '{"text": ("Commit: " + $msg), "runUrl": $url}' | \
    curl -X POST "${{ secrets.SLACK_URL }}" \
      -H "Content-Type: application/json" \
      -d @-
  env:
    COMMIT_MSG: ${{ github.event.head_commit.message }}
    RUN_URL: ${{ github.server_url }}/${{ github.repository }}/actions/runs/${{ github.run_id }}
```

### Notificar al autor del commit (no al canal general)

Útil para notificar solo a quien rompió el build, no a todo el equipo:

```yaml
  notify-author:
    needs: build
    if: failure()
    runs-on: ubuntu-latest
    steps:
      - name: Obtener email del autor
        id: author
        run: |
          echo "email=$(git log -1 --format='%ae' ${{ github.sha }})" >> $GITHUB_OUTPUT

      - name: Enviar email al autor
        uses: dawidd6/action-send-mail@v3
        with:
          server_address: smtp.gmail.com
          server_port: 587
          username: ${{ secrets.MAIL_USER }}
          password: ${{ secrets.MAIL_PASSWORD }}
          to: ${{ steps.author.outputs.email }}
          subject: "❌ Build fallido en ${{ github.repository }}"
          body: |
            Tu commit ${{ github.sha }} ha roto el build en ${{ github.ref_name }}.
            Ver detalles: ${{ github.server_url }}/${{ github.repository }}/actions/runs/${{ github.run_id }}
```

### Notificaciones con estado diferenciado por job

En lugar de notificar el resultado del workflow completo, notificar qué job específico falló:

```yaml
  notify:
    needs: [lint, test, build, deploy]
    if: always() && contains(needs.*.result, 'failure')
    runs-on: ubuntu-latest
    steps:
      - name: Construir mensaje con jobs fallidos
        id: msg
        run: |
          FAILED=""
          [ "${{ needs.lint.result }}" = "failure" ]   && FAILED="$FAILED lint"
          [ "${{ needs.test.result }}" = "failure" ]   && FAILED="$FAILED test"
          [ "${{ needs.build.result }}" = "failure" ]  && FAILED="$FAILED build"
          [ "${{ needs.deploy.result }}" = "failure" ] && FAILED="$FAILED deploy"
          echo "jobs=$FAILED" >> $GITHUB_OUTPUT

      - uses: slackapi/slack-github-action@v1
        with:
          payload: '{"text": "❌ Fallaron:${{ steps.msg.outputs.jobs }}"}'
        env:
          SLACK_WEBHOOK_URL: ${{ secrets.SLACK_WEBHOOK_URL }}
          SLACK_WEBHOOK_TYPE: INCOMING_WEBHOOK
```

---

## 7. Docker Avanzado

### Multi-stage build para reducir tamaño

Cuando construyes una imagen Docker para una aplicación Node.js, necesitas el compilador, todas las `devDependencies`, el código fuente y las herramientas de build durante la compilación — pero en producción solo necesitas el resultado compilado (`dist/`) y las dependencias de runtime. Si usas un único `FROM`, la imagen final incluye todo ese peso innecesario: fácilmente 800 MB–1 GB.

Un **multi-stage build** usa varios bloques `FROM` en el mismo Dockerfile. Cada bloque es un "stage" independiente. El stage final solo copia lo que necesita de los stages anteriores. El resultado: una imagen de producción ligera (~100–200 MB) sin código fuente ni herramientas de desarrollo, con menor superficie de ataque.

```
Sin multi-stage:
  imagen = compilador + devDeps + fuentes + build + runtime → ~800 MB

Con multi-stage:
  stage builder → solo se usa durante la construcción, se descarta
  stage production → solo dist/ + runtime deps                → ~150 MB
```

```dockerfile
# Dockerfile multi-stage
# Stage 1: Build
FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# Stage 2: Production (solo lo necesario)
FROM node:20-alpine AS production
WORKDIR /app
COPY --from=builder /app/dist ./dist
COPY --from=builder /app/node_modules ./node_modules
EXPOSE 3000
CMD ["node", "dist/index.js"]

# Resultado: imagen de ~150MB en vez de ~800MB
```

### Build con caché de layers en GitHub Actions

```yaml
jobs:
  build-push:
    runs-on: ubuntu-latest
    permissions:
      packages: write
    steps:
      - uses: actions/checkout@v4

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Login a GHCR
        uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Extraer metadata (tags)
        id: meta
        uses: docker/metadata-action@v5
        with:
          images: ghcr.io/${{ github.repository }}
          tags: |
            type=sha                          # ghcr.io/org/repo:abc1234
            type=ref,event=branch             # ghcr.io/org/repo:main
            type=semver,pattern={{version}}   # ghcr.io/org/repo:1.2.3
            type=semver,pattern={{major}}.{{minor}}  # ghcr.io/org/repo:1.2
            type=raw,value=latest,enable=${{ github.ref == 'refs/heads/main' }}

      - name: Build y Push
        uses: docker/build-push-action@v5
        with:
          context: .
          target: production      # ← Stage del multi-stage build
          push: true
          tags: ${{ steps.meta.outputs.tags }}
          labels: ${{ steps.meta.outputs.labels }}
          # Cache de layers usando GitHub Actions Cache
          cache-from: type=gha
          cache-to: type=gha,mode=max
          # O cache usando el propio registry:
          # cache-from: type=registry,ref=ghcr.io/${{ github.repository }}:cache
          # cache-to: type=registry,ref=ghcr.io/${{ github.repository }}:cache,mode=max
```

### Por qué usar cache de layers

```
Sin cache:
  Run 1: npm ci (2 min) + build (1 min) = 3 min total por layer
  Run 2: npm ci (2 min) + build (1 min) = 3 min (desde cero)

Con cache (type=gha):
  Run 1: npm ci (2 min) + build (1 min) = 3 min (se guarda en cache)
  Run 2: npm ci (cache hit, 10 seg) + build (solo diff) = ~30 seg
```

### Trampa: `docker/setup-buildx-action` es obligatorio para `cache-from`

El ejemplo anterior usa `cache-from: type=gha` pero **sin el step de setup-buildx** el cache falla silenciosamente (o con error críptico). El Docker engine estándar del runner no soporta cache externo ni multi-platform — Buildx es el motor extendido que los habilita. El orden correcto es siempre:

```yaml
steps:
  - uses: docker/setup-buildx-action@v3    # ← 1. Primero SIEMPRE
  - uses: docker/login-action@v3           # ← 2. Login
  - uses: docker/metadata-action@v5        # ← 3. Tags
  - uses: docker/build-push-action@v5      # ← 4. Build (puede usar cache)
    with:
      cache-from: type=gha
      cache-to: type=gha,mode=max
```

### `type=gha` vs `type=registry` para cache de layers

| | `type=gha` | `type=registry` |
|---|---|---|
| Dónde se guarda | GitHub Actions Cache (límite 10 GB del repo) | En el registry como imagen especial de cache |
| Disponibilidad | Solo en el mismo repo | Cualquier máquina con acceso al registry |
| Comparte límite con | `actions/cache` de dependencias | Solo el espacio del registry |
| Ideal para | Repos normales con un registry | Self-hosted runners, múltiples repos compartiendo cache |

```yaml
# type=registry: útil cuando el límite de 10 GB de GHA cache es insuficiente
cache-from: type=registry,ref=ghcr.io/${{ github.repository }}:buildcache
cache-to: type=registry,ref=ghcr.io/${{ github.repository }}:buildcache,mode=max
```

### Login a registries privados (Artifactory, ECR, ACR, GCR)

El ejemplo del documento solo muestra GHCR. Para registries corporativos habituales:

```yaml
      # Artifactory / JFrog
      - uses: docker/login-action@v3
        with:
          registry: mi-empresa.jfrog.io
          username: ${{ secrets.ARTIFACTORY_USER }}
          password: ${{ secrets.ARTIFACTORY_TOKEN }}

      # AWS ECR (requiere credenciales AWS previas — OIDC recomendado)
      - uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: ${{ secrets.AWS_ROLE_ARN }}
          aws-region: eu-west-1
      - uses: aws-actions/amazon-ecr-login@v2
        # No necesita username/password — usa las credenciales AWS configuradas

      # Azure Container Registry
      - uses: docker/login-action@v3
        with:
          registry: miregistro.azurecr.io
          username: ${{ secrets.ACR_USERNAME }}
          password: ${{ secrets.ACR_PASSWORD }}

      # Google Artifact Registry (requiere auth de GCP previa)
      - uses: google-github-actions/auth@v2
        with:
          workload_identity_provider: ${{ secrets.GCP_WORKLOAD_IDENTITY }}
          service_account: ${{ secrets.GCP_SERVICE_ACCOUNT }}
      - run: gcloud auth configure-docker europe-docker.pkg.dev --quiet
```

> ⚠️ Para Artifactory y otros registries privados, si la imagen se usa en `container:` o `services:` del job, las credenciales deben estar también en el bloque `credentials:` de esos campos — el login del step no es suficiente para que GitHub haga el pull automático.

### Multi-platform build (amd64 + arm64)

Sin especificar `platforms:`, Docker solo construye para la arquitectura del runner (`linux/amd64`). Para imágenes que deben correr en ARM (Apple Silicon, servidores ARM, Raspberry Pi):

```yaml
      - name: Set up QEMU  # Emulación para cross-compile
        uses: docker/setup-qemu-action@v3

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Build y Push multi-platform
        uses: docker/build-push-action@v5
        with:
          platforms: linux/amd64,linux/arm64
          push: true
          tags: ${{ steps.meta.outputs.tags }}
          cache-from: type=gha
          cache-to: type=gha,mode=max
```

> 💡 `setup-qemu-action` instala emuladores de CPU. La construcción para ARM en un runner amd64 es más lenta (emulación), pero produce una imagen nativa que corre eficientemente en ARM. Para builds muy lentos se puede usar un runner ARM nativo.

### Build sin push (solo verificar que la imagen construye)

En PRs puede interesar verificar que el Dockerfile compila sin subir la imagen:

```yaml
      - name: Build (sin push)
        uses: docker/build-push-action@v5
        with:
          context: .
          push: false          # ← Solo build, no push
          tags: mi-app:test
          cache-from: type=gha
          cache-to: type=gha,mode=max
          # load: true         # ← Si además necesitas usar la imagen localmente en steps siguientes
```

### Usar la imagen construida en steps siguientes del mismo job

```yaml
      - name: Build local (sin push)
        uses: docker/build-push-action@v5
        with:
          context: .
          push: false
          load: true           # ← Carga la imagen en el Docker local del runner
          tags: mi-app:test

      - name: Test con la imagen local
        run: docker run --rm mi-app:test npm test

      - name: Scan de seguridad de la imagen
        uses: aquasecurity/trivy-action@master
        with:
          image-ref: mi-app:test
          format: table
          exit-code: '1'       # ← Falla el job si encuentra vulnerabilidades críticas
```

> ⚠️ `load: true` y `push: true` son **mutuamente excluyentes** con multi-platform (`platforms: linux/amd64,linux/arm64`). Con multi-platform solo se puede hacer push al registry, no cargar localmente.

---

## 8. Estrategias de Despliegue Avanzadas y Rollback

Las estrategias de despliegue controlan **cómo** se sustituye la versión antigua por la nueva en producción. El objetivo es minimizar el downtime y el riesgo: si algo falla, se puede revertir rápido y con impacto mínimo.

```
Sin estrategia (Big Bang):
  versión antigua → STOP → versión nueva → START
  Downtime garantizado + rollback = volver a desplegar

Con estrategia:
  old y new corren en paralelo → tráfico se mueve gradualmente
  Si new falla → old sigue activo, rollback instantáneo
```

### Rolling Update

Actualiza las instancias (pods/containers) **de una en una** o en pequeños grupos. En Kubernetes es la estrategia por defecto.

```
Instancia 1: v1 → v2  ✅
Instancia 2: v1 → v2  ✅
Instancia 3: v1 → v2  ✅   ← Si falla aquí, se para y se revierte
```

```yaml
  deploy-rolling:
    needs: build
    runs-on: ubuntu-latest
    environment:
      name: production
    steps:
      - name: Rolling update en Kubernetes
        run: |
          kubectl set image deployment/my-app \
            app=ghcr.io/${{ github.repository }}:${{ needs.build.outputs.image-tag }} \
            --record

          # Esperar a que el rollout complete (con timeout)
          kubectl rollout status deployment/my-app --timeout=5m

      - name: Verificar health tras rollout
        id: health-check
        run: |
          sleep 15   # Grace period
          STATUS=$(curl -sf -o /dev/null -w "%{http_code}" https://app.example.com/health)
          echo "status=${STATUS}" >> $GITHUB_OUTPUT
          [ "$STATUS" = "200" ] || exit 1

      - name: Rollback si health check falla
        if: failure() && steps.health-check.conclusion == 'failure'
        run: |
          kubectl rollout undo deployment/my-app
          echo "⚠️ Rollback ejecutado automáticamente" >> $GITHUB_STEP_SUMMARY
```

### Blue/Green

Mantiene **dos entornos idénticos** (blue = activo, green = nuevo). El tráfico cambia de golpe del blue al green. El blue queda en stand-by para rollback instantáneo.

```
           ┌─────────────┐
Load       │  Blue (v1)  │ ← activo hasta el switch
Balancer → ├─────────────┤
           │  Green (v2) │ ← recibe tráfico 0% hasta que se aprueba
           └─────────────┘

Switch → Load Balancer apunta a Green
Rollback → Load Balancer vuelve a Blue (< 1 segundo)
```

```yaml
  deploy-blue-green:
    needs: build
    runs-on: ubuntu-latest
    environment:
      name: production
    steps:
      - name: Determinar slot activo
        id: slot
        run: |
          ACTIVE=$(kubectl get svc my-app-svc -o jsonpath='{.spec.selector.slot}')
          INACTIVE=$([ "$ACTIVE" = "blue" ] && echo "green" || echo "blue")
          echo "active=${ACTIVE}" >> $GITHUB_OUTPUT
          echo "inactive=${INACTIVE}" >> $GITHUB_OUTPUT

      - name: Desplegar en slot inactivo
        run: |
          kubectl set image deployment/my-app-${{ steps.slot.outputs.inactive }} \
            app=ghcr.io/${{ github.repository }}:${{ needs.build.outputs.image-tag }}
          kubectl rollout status deployment/my-app-${{ steps.slot.outputs.inactive }} --timeout=5m

      - name: Smoke tests contra slot inactivo
        run: |
          # El slot inactivo tiene su propia URL interna para testing pre-switch
          curl -sf https://${{ steps.slot.outputs.inactive }}.internal.example.com/health

      - name: Switch de tráfico al slot nuevo
        run: |
          kubectl patch svc my-app-svc \
            -p '{"spec":{"selector":{"slot":"${{ steps.slot.outputs.inactive }}"}}}'
          echo "Tráfico movido a: ${{ steps.slot.outputs.inactive }}" >> $GITHUB_STEP_SUMMARY

      - name: Verificar tras switch
        id: post-switch
        run: |
          sleep 30
          curl -sf https://app.example.com/health || exit 1

      - name: Rollback automático si falla post-switch
        if: failure() && steps.post-switch.conclusion == 'failure'
        run: |
          kubectl patch svc my-app-svc \
            -p '{"spec":{"selector":{"slot":"${{ steps.slot.outputs.active }}"}}}'
          echo "⚠️ Rollback a ${{ steps.slot.outputs.active }}" >> $GITHUB_STEP_SUMMARY
```

### Canary

Despliega la nueva versión solo a un **porcentaje pequeño de usuarios** (ej. 10%). Se monitorean errores y latencia. Si todo va bien, se sube el porcentaje gradualmente.

```
100% tráfico → v1
    ↓
10% → v2, 90% → v1     (fase canary)
    ↓
50% → v2, 50% → v1     (si métricas OK)
    ↓
100% → v2              (promoción completa)
```

```yaml
  canary-deploy:
    needs: build
    runs-on: ubuntu-latest
    environment:
      name: production
    steps:
      - name: Desplegar canary (10% del tráfico)
        run: |
          # En Kubernetes: escalar el deployment canary a 1 réplica de 10 totales
          kubectl scale deployment/my-app-canary --replicas=1
          kubectl set image deployment/my-app-canary \
            app=ghcr.io/${{ github.repository }}:${{ needs.build.outputs.image-tag }}
          kubectl rollout status deployment/my-app-canary --timeout=5m

      - name: Observar métricas durante 10 minutos
        run: |
          echo "Observando métricas del canary..."
          sleep 600   # 10 min de soak time

          # Consultar tasa de errores (ejemplo con Prometheus/Grafana API)
          ERROR_RATE=$(curl -sf "${{ vars.PROMETHEUS_URL }}/api/v1/query" \
            --data-urlencode 'query=rate(http_requests_total{status=~"5..",deployment="canary"}[5m])' \
            | jq '.data.result[0].value[1] // "0"' -r)

          echo "Tasa de errores canary: ${ERROR_RATE}"
          # Fallar si error rate > 1%
          awk "BEGIN { exit ($ERROR_RATE > 0.01) }"

      - name: Promover canary al 100%
        run: |
          kubectl scale deployment/my-app-canary --replicas=0
          kubectl set image deployment/my-app-stable \
            app=ghcr.io/${{ github.repository }}:${{ needs.build.outputs.image-tag }}
          kubectl rollout status deployment/my-app-stable --timeout=5m
          echo "✅ Canary promovido al 100%" >> $GITHUB_STEP_SUMMARY

      - name: Rollback si métricas fallan
        if: failure()
        run: |
          kubectl scale deployment/my-app-canary --replicas=0
          echo "⚠️ Canary retirado — versión estable activa" >> $GITHUB_STEP_SUMMARY
```

### Resumen comparativo

| Estrategia | Downtime | Complejidad | Rollback | Indicada para |
|---|---|---|---|---|
| Rolling | Ninguno | Baja | Automático (`rollout undo`) | Apps stateless con varias réplicas |
| Blue/Green | Ninguno | Media | Instantáneo (< 1s) | Cambios de BD, APIs con breaking changes |
| Canary | Ninguno | Alta | Automático (retirar réplicas) | Alto riesgo, necesitas datos reales |

> 💡 En GitHub Actions estas estrategias se orquestan con llamadas a `kubectl`, scripts de Helm, o herramientas como Argo Rollouts. GitHub solo gestiona el **cuándo** (triggers, gates, aprobaciones); el **cómo** lo decide el orquestador de la infraestructura.

---

## 9. Required Workflows

Los **required workflows** son workflows configurados a nivel de organización que se añaden automáticamente como checks obligatorios en todos los repositorios (o en un subconjunto seleccionado). No es necesario modificar cada repositorio: el check aparece automáticamente en los pull requests.

### ¿Para qué sirven?

```
SIN required workflows:
  repo-a/.github/workflows/ci.yml    ← cada equipo define sus propios checks
  repo-b/.github/workflows/ci.yml    ← puede faltar seguridad, lint, etc.
  repo-c/                             ← sin workflows

CON required workflows:
  org/.github/workflows/security-scan.yml  ← se ejecuta en TODOS los repos
  org/.github/workflows/compliance.yml     ← obligatorio en todos los PRs
```

**Casos de uso habituales:**
- Escaneo de seguridad / SAST obligatorio en todos los repos
- Comprobaciones de licencias o compliance
- Validación de estructura de commits (Conventional Commits)
- Verificación de cobertura mínima de tests

### Configuración

```
Organization Settings → Actions → General → Required workflows
→ Add workflow
  - Repository: el repo de la org donde está el workflow (ej: .github)
  - Workflow: ruta al archivo (ej: .github/workflows/security-scan.yml)
  - Applies to: All repositories / Selected repositories
```

**Requisitos:**
- El workflow fuente debe estar en un repositorio de la organización
- Debe usar el trigger `pull_request` (es el más común) o `push`
- El repositorio fuente debe ser accesible para todos los repos destino

### Comportamiento

```yaml
# Workflow en org/.github/workflows/security-scan.yml
name: Security Scan
on:
  pull_request:
    branches: [main]

jobs:
  scan:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Run security scan
        run: echo "Scanning..."
```

- Se ejecuta en cada PR hacia `main` en **todos los repos de la org** automáticamente
- Aparece como check requerido aunque el repo no lo defina en su propio `.github/workflows/`
- Si el workflow falla, el PR no puede mergearse (si está configurado como required check en branch protection)
- El workflow se ejecuta con el código del **repositorio destino**, no del repositorio fuente

### Diferencia con branch protection rules

| | Required Workflow | Branch Protection (required checks) |
|---|---|---|
| Configuración | A nivel de org | A nivel de repo/rama |
| Propagación | Automática a todos los repos | Manual en cada repo |
| Workflow | Definido en repo centralizado | Definido en el mismo repo |
| Caso de uso | Políticas corporativas | Checks de proyecto específico |

---

## 10. Audit Logs de GitHub Actions

Los **audit logs** registran todas las acciones administrativas relacionadas con GitHub Actions en una organización o empresa. Son esenciales para auditorías de seguridad y cumplimiento.

### Dónde encontrarlos

```
Organization → Settings → Audit log
Enterprise → Settings → Audit log

Filtros útiles:
  action:workflows.*          → eventos de workflows
  action:org.update_actions_* → cambios en políticas de Actions
  action:runner.*             → registro/eliminación de runners
  action:secret.*             → acceso/modificación de secrets
```

### Eventos registrados relevantes a Actions

| Categoría | Eventos |
|---|---|
| **Workflows** | Creación, ejecución, cancelación, borrado de workflows |
| **Runners** | Registro y eliminación de self-hosted runners, cambios en runner groups |
| **Secrets** | Creación, actualización y eliminación de secrets (no el valor, solo el nombre) |
| **Políticas** | Cambios en `allowed actions`, `fork workflow policies`, `required workflows` |
| **Artifacts** | Descarga de artifacts |
| **Environments** | Creación/modificación de environments y protection rules |

### Exportar audit logs

```bash
# Via API REST (requiere token con permiso audit_log:read)
curl -H "Authorization: Bearer TOKEN" \
  "https://api.github.com/orgs/ORG/audit-log?phrase=action:workflows&per_page=100"

# Via GitHub CLI
gh api /orgs/ORG/audit-log --paginate \
  -f phrase="action:workflows.completed_workflow_run"
```

### Retención

| Plan | Retención |
|---|---|
| Free / Pro / Team | 90 días |
| GitHub Enterprise Cloud | 180 días |
| GitHub Enterprise Cloud + audit log streaming | Indefinida (streaming a SIEM externo) |

### Audit log streaming (Enterprise)

Permite enviar eventos en tiempo real a sistemas externos (AWS S3, Azure Blob, Splunk, Datadog):

```
Enterprise Settings → Audit log → Log streaming
→ Configurar destino (S3, Azure Blob Storage, Google Cloud Storage, Splunk, Datadog)
```

### Reporte de Auditoría por Deploy

El audit log de GitHub registra **qué ocurrió** a nivel de organización, pero no genera por sí solo un reporte legible de "qué imagen está en producción ahora". Este patrón genera ese reporte al final de cada deploy, con trazabilidad completa.

```yaml
  audit-deploy:
    needs: [deploy-production, smoke-tests]
    if: always()
    runs-on: ubuntu-latest
    permissions:
      contents: write        # Para escribir en el repositorio si se persiste
      pull-requests: read
    steps:
      - uses: actions/checkout@v4

      - name: Obtener aprobador del environment
        id: approver
        uses: actions/github-script@v7
        with:
          script: |
            // Obtener revisiones del environment para este run
            const reviews = await github.rest.actions.listEnvironmentApprovals({
              owner: context.repo.owner,
              repo: context.repo.repo,
              run_id: context.runId
            })
            const approved = reviews.data.find(r => r.state === 'approved')
            return approved ? approved.reviewer.login : 'auto-approved'
          result-encoding: string

      - name: Generar reporte de auditoría
        run: |
          cat >> $GITHUB_STEP_SUMMARY << EOF
          ## Reporte de Deploy — Producción

          | Campo | Valor |
          |---|---|
          | Fecha | $(date -u '+%Y-%m-%d %H:%M:%S UTC') |
          | Entorno | production |
          | Commit | \`${{ github.sha }}\` |
          | Rama | \`${{ github.ref_name }}\` |
          | Imagen | \`ghcr.io/${{ github.repository }}:${{ needs.deploy-production.outputs.image-tag }}\` |
          | Aprobado por | ${{ steps.approver.outputs.result }} |
          | Ejecutado por | ${{ github.actor }} |
          | Run ID | [${{ github.run_id }}](${{ github.server_url }}/${{ github.repository }}/actions/runs/${{ github.run_id }}) |
          | Smoke tests | ${{ needs.smoke-tests.result }} |
          EOF

      - name: Persistir reporte en fichero del repo
        run: |
          mkdir -p audit/deploys
          cat > audit/deploys/$(date -u '+%Y%m%d-%H%M%S')-production.json << EOF
          {
            "timestamp": "$(date -u '+%Y-%m-%dT%H:%M:%SZ')",
            "environment": "production",
            "commit_sha": "${{ github.sha }}",
            "branch": "${{ github.ref_name }}",
            "image_tag": "${{ needs.deploy-production.outputs.image-tag }}",
            "approved_by": "${{ steps.approver.outputs.result }}",
            "actor": "${{ github.actor }}",
            "run_id": "${{ github.run_id }}",
            "smoke_tests": "${{ needs.smoke-tests.result }}"
          }
          EOF

          git config user.name  "github-actions[bot]"
          git config user.email "github-actions[bot]@users.noreply.github.com"
          git add audit/
          git commit -m "audit: deploy a production ${{ github.sha }}" || true
          git push || true   # No falla el pipeline si el push no puede hacerse
```

**Qué información captura:**

| Dato | Por qué importa |
|---|---|
| `commit_sha` | Trazabilidad exacta del código en producción |
| `image_tag` | La imagen binaria que se está ejecutando, no solo el código |
| `approved_by` | Quién dio el visto bueno (responsabilidad legal/SOC2) |
| `actor` | Quién disparó el workflow |
| `smoke_tests` | Si la verificación post-deploy tuvo éxito |
| `run_id` | Link directo al log completo de la ejecución |

> 💡 Para entornos regulados (SOC2, ISO 27001), este reporte combinado con el audit log de GitHub cubre los controles de **"evidencia de despliegue"** y **"aprobación de cambios en producción"**.

---

## 11. Variables de Organización y Repositorio

Las **variables** (`vars.*`) son valores de configuración no sensibles que pueden definirse a tres niveles y heredarse hacia abajo.

### Niveles de variables

```
Enterprise (vars.*)
    └── Organization (vars.*)
            └── Repository (vars.*)
                    └── Environment (vars.*)
```

**Regla de precedencia:** el nivel más específico sobreescribe al más general.

```yaml
# Si existe vars.DEPLOY_URL a nivel de org y también a nivel de repo,
# el workflow usará el valor del repo (más específico).
```

### Configuración

```
# Variables de repositorio
Repo → Settings → Secrets and variables → Actions → Variables → New repository variable

# Variables de organización
Org → Settings → Secrets and variables → Actions → Variables → New organization variable
  → Visibility: All repositories / Private repositories / Selected repositories
```

### Diferencia entre secrets y variables

| | `secrets.*` | `vars.*` |
|---|---|---|
| Encriptación | ✅ Sí | ❌ No |
| Visible en logs | ❌ Nunca (maskeado) | ✅ Sí (valor visible) |
| Uso | Tokens, contraseñas, claves | URLs, nombres de entorno, flags |
| Disponible en | `secrets.NOMBRE` | `vars.NOMBRE` |

### Ejemplo práctico

```yaml
# Variables definidas:
# Org level:   vars.REGISTRY_URL = "ghcr.io/mi-org"
# Repo level:  vars.APP_NAME = "my-service"
# Env level:   vars.DEPLOY_TARGET = "k8s-prod"

jobs:
  deploy:
    environment: production
    runs-on: ubuntu-latest
    steps:
      - name: Deploy
        run: |
          echo "Registry: ${{ vars.REGISTRY_URL }}"    # de la org
          echo "App: ${{ vars.APP_NAME }}"              # del repo
          echo "Target: ${{ vars.DEPLOY_TARGET }}"      # del environment
          docker pull ${{ vars.REGISTRY_URL }}/${{ vars.APP_NAME }}:latest
```

### Variables predefinidas vs variables de usuario

No confundir `vars.*` con las **variables de entorno automáticas** (`$GITHUB_SHA`, `$GITHUB_REF`…):

| Origen | Cómo acceder | Quién las define |
|---|---|---|
| Variables de usuario | `${{ vars.MI_VAR }}` | Admin (Settings) |
| Env vars automáticas | `$GITHUB_SHA`, `$RUNNER_OS`… | GitHub automáticamente |
| Env vars del workflow | `${{ env.MI_VAR }}` | El propio workflow YAML |

---

## 12. Workflow Templates (Starter Workflows)

Los **Workflow Templates** (también llamados Starter Workflows) son plantillas de workflows que una organización puede publicar para que aparezcan como sugerencias cuando alguien crea un nuevo workflow en cualquier repositorio de esa organización. Permiten estandarizar la forma en que los equipos comienzan a usar GitHub Actions.

### Repositorio `.github` de la organización

Las plantillas se almacenan en un repositorio especial llamado `.github` dentro de la organización:

```
github.com/mi-org/.github/
└── workflow-templates/
    ├── ci-nodejs.yml                  # El workflow plantilla
    ├── ci-nodejs.properties.json      # Metadatos de la plantilla
    ├── security-scan.yml
    └── security-scan.properties.json
```

El repositorio `.github` de la organización es distinto del directorio `.github/` de cada repositorio. Es un repositorio dedicado a configuraciones compartidas de la organización.

### Estructura: el archivo `.yml`

El archivo de workflow es un workflow YAML normal, con un placeholder especial:

```yaml
# workflow-templates/ci-nodejs.yml
name: Node.js CI

on:
  push:
    branches: [$default-branch]    # ← Placeholder: se reemplaza por la rama por defecto del repo
  pull_request:
    branches: [$default-branch]

jobs:
  build:
    runs-on: ubuntu-latest

    strategy:
      matrix:
        node-version: [18.x, 20.x]

    steps:
      - uses: actions/checkout@v4
      - name: Usar Node.js ${{ matrix.node-version }}
        uses: actions/setup-node@v4
        with:
          node-version: ${{ matrix.node-version }}
          cache: 'npm'
      - run: npm ci
      - run: npm run build --if-present
      - run: npm test
```

**Placeholder `$default-branch`:** GitHub lo reemplaza automáticamente por el nombre de la rama por defecto del repositorio destino (`main`, `master`, etc.) cuando el usuario aplica la plantilla.

### Estructura: el archivo `.properties.json`

Cada plantilla necesita un archivo de metadatos con el mismo nombre base:

```json
// workflow-templates/ci-nodejs.properties.json
{
  "name": "Node.js CI",
  "description": "Build and test a Node.js project with npm.",
  "iconName": "nodejs",
  "categories": [
    "JavaScript",
    "Node"
  ],
  "filePatterns": [
    "package.json$",
    "^package.json$"
  ]
}
```

| Campo | Descripción |
|---|---|
| `name` | Nombre visible de la plantilla en la UI |
| `description` | Descripción corta que aparece bajo el nombre |
| `iconName` | Nombre del icono (de la lista de iconos de GitHub) |
| `categories` | Categorías para filtrar plantillas (lenguajes, herramientas) |
| `filePatterns` | Regex de archivos en la raíz del repo que activan la sugerencia |

### Cómo aparece al usuario

Cuando un desarrollador va a `Repo → Actions → New workflow`, GitHub muestra las plantillas de la organización junto a las plantillas públicas de GitHub:

```
Suggested for this repository
  ┌─────────────────────────────┐
  │ [nodejs icon] Node.js CI    │
  │ Build and test a Node.js    │
  │ project with npm.           │
  │                  [Configure]│
  └─────────────────────────────┘
```

Las sugerencias basadas en `filePatterns` aparecen destacadas: si el repositorio contiene `package.json` en la raíz, la plantilla de Node.js se sugiere automáticamente al inicio de la lista.

### Sugerencias inteligentes con `filePatterns`

```json
// Plantilla de Python: sugiere si hay requirements.txt o setup.py
"filePatterns": [
  "requirements.txt$",
  "setup.py$",
  "pyproject.toml$"
]

// Plantilla de Java: sugiere si hay pom.xml o build.gradle
"filePatterns": [
  "^pom.xml$",
  "^build.gradle$",
  "^build.gradle.kts$"
]

// Plantilla genérica: aparece siempre (sin restricción)
"filePatterns": []
```

---

## 13. Custom Deployment Protection Rules

Las **Custom Deployment Protection Rules** permiten integrar sistemas externos en el proceso de aprobación de deployments a un environment. En lugar de (o además de) los revisores manuales de GitHub, una GitHub App puede aprobar o rechazar automáticamente un deployment basándose en lógica externa.

### ¿Qué son?

Son reglas de protección de environment implementadas a través de **GitHub Apps** (integraciones externas). Cuando un job intenta hacer deploy a un environment protegido, GitHub llama a la GitHub App para que evalúe si el deployment puede proceder.

```
Job intenta deploy a environment "production"
    │
    ▼
GitHub envía evento deployment_protection_rule a la GitHub App
    │
    ▼
GitHub App evalúa condiciones externas:
  - ¿Existe ticket de Jira aprobado?
  - ¿Pasó el compliance check?
  - ¿ServiceNow tiene aprobación?
  - ¿No hay incidente activo en PagerDuty?
    │
    ├── ✅ Aprueba → deployment continúa
    └── ❌ Rechaza → job falla, deployment bloqueado
```

### Diferencia con las built-in protection rules

| | Built-in Rules | Custom Rules (GitHub Apps) |
|---|---|---|
| **Required Reviewers** | Personas o equipos de GitHub | Sistema externo (Jira, ServiceNow...) |
| **Wait Timer** | Tiempo fijo de espera | Lógica arbitraria |
| **Configuración** | En Settings del environment | GitHub App instalada + configurada |
| **Casos de uso** | Aprobación manual del equipo | Compliance automático, ticketing |

### Casos de uso habituales

- **Jira integration:** El deployment solo puede proceder si existe un ticket de Change Request aprobado en Jira.
- **ServiceNow:** Validación de change management window y aprobación de CAB (Change Advisory Board).
- **PagerDuty:** Bloquear deployments si hay un incidente activo de P1/P2 en el servicio.
- **Compliance check externo:** Verificar que el artefacto pasó un scan de seguridad en una herramienta externa antes de desplegarlo.

### Cómo funciona

La GitHub App debe estar instalada en la organización o repositorio y configurada con el permiso `deployments`. Cuando GitHub envía el evento, la App tiene un tiempo límite para responder via API:

```
# La GitHub App aprueba el deployment via API
POST /repos/{owner}/{repo}/actions/runs/{run_id}/deployment_protection_rule
{
  "environment_name": "production",
  "state": "approved",           // o "rejected"
  "comment": "Change ticket JIRA-1234 aprobado"
}
```

### Configuración

```
Repo Settings → Environments → [nombre del environment] → Deployment protection rules
  → Add rule
    ○ Required reviewers    (built-in: personas de GitHub)
    ○ Wait timer            (built-in: tiempo de espera)
    ● [Mi GitHub App]       (custom: integración externa)
```

Las custom rules aparecen en la lista solo si la GitHub App está instalada y configurada para ese repositorio u organización.

---

## 14. Opciones Adicionales de Environments

Además de las protection rules básicas (Required Reviewers, Wait Timer), los environments de GitHub tienen opciones adicionales relevantes para entornos empresariales.

### Prevent Self-Review

Cuando `Prevent self-review` está activado, **el usuario que disparó el workflow no puede aprobar su propio deployment**.

```
Escenario sin Prevent Self-Review:
  Alice hace push a main → workflow pide aprobación
  Alice aprueba su propio deployment → deploya sin supervisión real

Escenario con Prevent Self-Review activado:
  Alice hace push a main → workflow pide aprobación
  Alice NO puede aprobar → debe esperar a Bob o Carol
```

**Configuración:**
```
Repo Settings → Environments → [env name] → Deployment protection rules
  ☑ Prevent self-review
```

Casos de uso: equipos pequeños donde el mismo desarrollador podría ser el único revisor disponible, entornos de producción que requieren four-eyes principle (dos personas distintas).

### Allow Administrators to Bypass

Por defecto, los administradores del repositorio pueden **saltarse** las protection rules de un environment y forzar un deployment sin aprobación. Esta opción puede deshabilitarse:

```
Repo Settings → Environments → [env name] → Deployment protection rules
  ☑ Allow administrators to bypass configured protection rules   (activado por defecto)
  ☐ [desactivar]  →  incluso los admins deben pasar por el proceso de aprobación
```

**Casos de uso para deshabilitar:**
- Entornos regulados (finanzas, sanidad) donde ningún individuo puede saltarse controles
- Compliance que requiere que todos los deployments, sin excepción, tengan aprobación documentada
- Separación de duties: quien tiene acceso de admin al repo no debe poder hacer bypass en producción

### Eliminación de un Environment

Cuando se elimina un environment desde Settings, ocurre lo siguiente automáticamente:

```
Al eliminar environment "production":

1. Se eliminan TODOS los secrets del environment
   → secrets.PROD_DB_PASSWORD, secrets.PROD_API_KEY, etc. desaparecen

2. Se eliminan TODAS las variables del environment
   → vars.DEPLOY_TARGET, vars.APP_URL, etc. desaparecen

3. Se eliminan TODAS las protection rules del environment
   → Required reviewers, wait timers, custom rules

4. Jobs en espera de aprobación de ese environment FALLAN automáticamente
   → Si hay un deployment esperando aprobación en "production"
      y se borra el environment, el job falla inmediatamente
```

**Consecuencia práctica:** Si se necesita recrear un environment eliminado accidentalmente, todos los secrets y variables deben reconfigurarse manualmente. No hay forma de recuperarlos una vez eliminados.

---

## 15. Preguntas de Examen

**P: ¿Cómo restringe un admin de organización qué actions pueden usar los repos?**
→ En `Organization Settings → Actions → General → Actions permissions`. Puede permitir todas, solo las de GitHub, o una lista explícita de patrones (`actions/checkout@*, mi-org/*`).

**P: Si org y repo tienen un secret con el mismo nombre, ¿cuál tiene prioridad?**
→ El secret de **repositorio** sobreescribe al de organización para ese repo concreto.

**P: ¿Puede un repo ser más permisivo que la política de su organización?**
→ No. La política de org es el techo máximo. El repo solo puede ser igual o más restrictivo.

**P: ¿Cómo deshabilitar un workflow individual sin afectar al resto?**
→ Desde la UI: `Actions → [workflow] → "..." → Disable workflow`. O con `gh workflow disable nombre.yml`. El workflow queda en estado `disabled_manually` y no se dispara por eventos.

**P: ¿Para qué sirve `workflow_run` y qué problema de seguridad resuelve?**
→ Dispara un workflow cuando otro termina. Resuelve el problema de PRs de forks: el CI se ejecuta sin secrets (código del fork), y `workflow_run` ejecuta un segundo workflow con secrets (código de main) para comentar el resultado.

**P: ¿El nombre en `workflows: ["CI"]` debe coincidir con el archivo o con el campo `name:`?**
→ Con el campo `name:` del archivo YAML. Si no tiene `name:`, se usa el nombre del archivo sin extensión.

**P: `workflow_run` se dispara con cualquier conclusión del padre — ¿cómo evitar que se ejecute cuando el CI falló?**
→ Añadir `if: github.event.workflow_run.conclusion == 'success'` en el job.

**P: ¿Puede `workflow_run` encadenar workflows de repositorios diferentes?**
→ No. Solo puede referenciar workflows del mismo repositorio. Para repos distintos hay que usar `repository_dispatch`.

**P: ¿Cuántos niveles de aprobación de forks puede configurar una organización?**
→ Tres: todos los colaboradores externos, solo contribuidores nuevos (default), o sin aprobación.

**P: ¿Qué pasa con los jobs que dependen de un job `skipped` en GitFlow?**
→ Se saltan automáticamente (`skipped`), aunque no tengan `if:` propio. Un job de notificación con `if: always()` recibirá `skipped` en `needs.build.result`, no `success`.

**P: ¿Cuál es la principal diferencia entre GitFlow y Trunk-based development?**
→ GitFlow usa múltiples ramas long-lived (`develop`, `release/*`) y deploys por sprints. Trunk-based usa solo `main` con ramas de vida muy corta y deploys continuos, controlando visibilidad con feature flags.

**P: ¿Qué hace `release-please` y qué tipo de commits analiza?**
→ Automatiza CHANGELOGs y releases semánticos analizando commits convencionales (`feat:` → minor, `fix:` → patch, `feat!:` → major). Crea un PR de release que al mergearse genera el tag y el GitHub Release.

**P: ¿Por qué los PRs de release de `release-please` no deben mergearse con squash?**
→ Porque `release-please` lee el historial individual de commits para calcular la siguiente versión. El squash aplana todos los commits en uno y pierde esa información.

**P: ¿Qué scope de GITHUB_TOKEN necesita `dorny/test-reporter` para publicar anotaciones?**
→ `checks: write`

**P: ¿Por qué `dorny/test-reporter` falla en PRs de forks y cómo solucionarlo?**
→ Porque en forks el `GITHUB_TOKEN` solo tiene `read` y `checks: write` no está disponible. Solución: usar `workflow_run` para publicar los resultados en un segundo workflow que sí tiene permisos completos.

**P: ¿Cuál es la ventaja de un multi-stage Dockerfile?**
→ La imagen final solo incluye artefactos del stage de producción, sin herramientas de build ni código fuente. Reduce tamaño (~800 MB → ~150 MB) y superficie de ataque.

**P: ¿Es `docker/setup-buildx-action` obligatorio para usar `cache-from: type=gha`?**
→ Sí. El Docker engine estándar del runner no soporta cache externo. Buildx es el motor extendido que lo habilita y debe configurarse antes del step de build.

**P: ¿Qué hace `type=gha` en `cache-from`/`cache-to` y cuándo usar `type=registry` en su lugar?**
→ `type=gha` usa GitHub Actions Cache (límite 10 GB compartido con `actions/cache`). `type=registry` guarda el cache en el propio registry como imagen especial — útil cuando el límite de 10 GB es insuficiente o se usan self-hosted runners sin acceso al cache de GitHub.

**P: ¿Son compatibles `load: true` y `platforms: linux/amd64,linux/arm64` en `docker/build-push-action`?**
→ No. `load: true` (cargar imagen localmente) es incompatible con multi-platform. Con multi-platform solo se puede hacer `push: true` al registry.

**P: ¿Cómo evitar inyección de caracteres especiales en el payload JSON de Slack?**
→ Nunca interpolar directamente valores del contexto (títulos de PR, mensajes de commit) en JSON inline. Pasarlos como variables de entorno y construir el JSON con `jq`.

**P: ¿Cómo notificar qué job específico falló en lugar del resultado global del workflow?**
→ En el job de notificación, comprobar `needs.*.result` individualmente para cada job y construir el mensaje con los nombres de los jobs fallidos.

**P: ¿Qué son los Required Workflows y en qué se diferencian de los required checks?**
→ Los required workflows son workflows definidos a nivel de organización que se ejecutan automáticamente como checks en todos los repos (sin que cada repo los defina). Los required checks son checks que el admin configura en branch protection de un repo concreto.

**P: ¿Dónde se define un required workflow y con qué código se ejecuta?**
→ En un repositorio de la organización (típicamente `.github`). Se ejecuta con el código del **repositorio destino** (el repo del PR), no del repositorio donde está definido el workflow.

**P: ¿Cuánto tiempo se retienen los audit logs en GitHub Free/Team vs Enterprise Cloud?**
→ Free/Team: 90 días. Enterprise Cloud: 180 días. Con audit log streaming activo: indefinido (se envían a un SIEM externo en tiempo real).

**P: ¿Los audit logs registran el valor de un secret cuando se modifica?**
→ No. Solo registran el evento (creación, actualización, eliminación) y el nombre del secret. El valor nunca queda registrado.

**P: ¿Cuál es la diferencia entre `vars.*` y `secrets.*`?**
→ `vars.*` son variables no encriptadas, visibles en logs, para configuración no sensible (URLs, nombres). `secrets.*` son encriptadas, nunca aparecen en logs, para datos sensibles (tokens, contraseñas).

**P: Si una variable `vars.URL` está definida a nivel de org y también a nivel de repo, ¿cuál usa el workflow?**
→ La del repositorio — el nivel más específico sobreescribe al más general.

**P: ¿Pueden los required workflows acceder a los secrets del repo destino?**
→ Sí, si el required workflow usa `secrets: inherit` o referencia secretos explícitamente. El workflow tiene acceso al contexto del repositorio destino donde se ejecuta el PR.


**P: ¿En qué repositorio se almacenan los Workflow Templates de una organización?**
→ En el repositorio especial `.github` de la organización (`github.com/mi-org/.github`), dentro del directorio `workflow-templates/`.

**P: ¿Qué dos archivos son necesarios por cada Workflow Template?**
→ Un archivo `.yml` con el workflow y un archivo `.properties.json` con los metadatos (nombre, descripción, icono, categorías y filePatterns).

**P: ¿Qué hace el placeholder `$default-branch` en un Workflow Template?**
→ Se reemplaza automáticamente por el nombre de la rama por defecto del repositorio destino cuando el usuario aplica la plantilla. Evita hardcodear `main` o `master`.

**P: ¿Para qué sirve el campo `filePatterns` en el `.properties.json` de una plantilla?**
→ Define patrones regex de archivos en la raíz del repositorio que activan la sugerencia de la plantilla. Si el repo contiene un archivo que coincide con algún patrón, la plantilla aparece destacada como sugerencia.

**P: ¿Qué son las Custom Deployment Protection Rules?**
→ Son reglas de protección de environment implementadas mediante GitHub Apps. Permiten integrar sistemas externos (Jira, ServiceNow, PagerDuty) en el proceso de aprobación de deployments: la GitHub App recibe el evento y aprueba o rechaza el deployment via API.

**P: ¿Cuál es la diferencia entre Required Reviewers y una Custom Deployment Protection Rule?**
→ Required Reviewers requiere la aprobación manual de personas o equipos de GitHub. Las Custom Rules delegan la decisión en una GitHub App que puede implementar cualquier lógica externa (tickets, compliance, ventanas de cambio).

**P: ¿Qué evita la opción `Prevent Self-Review` en un environment?**
→ Evita que el usuario que disparó el workflow sea quien aprueba su propio deployment. Garantiza que la aprobación la dé una persona diferente (four-eyes principle).

**P: ¿Los administradores del repositorio pueden saltarse las protection rules de un environment por defecto?**
→ Sí. Por defecto, `Allow administrators to bypass` está activado. Se puede desactivar para que incluso los admins deban pasar por el proceso de aprobación — útil en entornos regulados.

**P: ¿Qué ocurre con los secrets y variables de un environment cuando este se elimina?**
→ Se eliminan permanentemente todos los secrets, variables y protection rules del environment. No se pueden recuperar. Además, cualquier job que estuviera esperando aprobación de ese environment falla automáticamente.
