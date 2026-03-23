# 🏢 GitHub Actions: Administración de Organización y CI/CD Empresarial

## 📚 Índice
1. [Políticas de Organización](#1-políticas-de-organización)
2. [workflow_run: Workflows Encadenados](#2-workflow_run-workflows-encadenados)
3. [Estrategia de Branching con Actions](#3-estrategia-de-branching-con-actions)
4. [Testing Avanzado: Separar Suites y Publicar Resultados](#4-testing-avanzado)
5. [Versionado Semántico Automático](#5-versionado-semántico-automático)
6. [Notificaciones Externas (Slack, Teams)](#6-notificaciones-externas)
7. [Docker Avanzado: Multi-stage y Layer Cache](#7-docker-avanzado)
8. [Preguntas de Examen](#8-preguntas-de-examen)

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

## 8. Preguntas de Examen

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

