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

---

## 5. Versionado Semántico Automático

### release-please: releases automáticos por tipo de commit

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

---

## 7. Docker Avanzado

### Multi-stage build para reducir tamaño

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

---

## 8. Preguntas de Examen

**P: ¿Cómo restringe un admin de organización qué actions pueden usar los repos?**
→ En `Organization Settings → Actions → General → Actions permissions`. Puede permitir todas, solo las de GitHub, o una lista explícita de patrones (`actions/checkout@*, mi-org/*`).

**P: ¿Para qué sirve `workflow_run` y qué problema de seguridad resuelve?**
→ Dispara un workflow cuando otro termina. Resuelve el problema de PRs de forks: el workflow de CI se ejecuta sin secrets (código del fork), y `workflow_run` ejecuta un segundo workflow con secrets (código de main) para comentar el resultado.

**P: ¿Cuántos niveles de aprobación de forks puede configurar una organización?**
→ Tres: requerir aprobación para todos los colaboradores externos, solo para contribuidores nuevos (default), o sin aprobación.

**P: ¿Qué hace `release-please` y qué tipo de commits analiza?**
→ Automatiza la creación de releases y CHANGELOGs analizando commits convencionales (`feat:` → minor, `fix:` → patch, `feat!:` → major). Crea un PR de release que al mergearse genera el tag y el GitHub Release.

**P: ¿Qué scope de GITHUB_TOKEN necesita `dorny/test-reporter` para publicar anotaciones?**
→ `checks: write`

**P: ¿Cuál es la ventaja de un multi-stage Dockerfile?**
→ La imagen final solo incluye los artefactos del stage de producción, sin herramientas de build, dependencias de desarrollo ni código fuente. Reduce drásticamente el tamaño de la imagen y la superficie de ataque.

**P: ¿Qué hace `type=gha` en el campo `cache-from`/`cache-to` de `docker/build-push-action`?**
→ Usa GitHub Actions Cache como backend para cachear los layers de Docker entre ejecuciones, evitando reconstruir layers que no han cambiado.

**P: ¿Cómo filtrar `workflow_run` para que solo se dispare cuando el workflow padre terminó en una rama específica?**
→ Usar el filtro `branches:` en el trigger `workflow_run`:
```yaml
on:
  workflow_run:
    workflows: ["CI"]
    types: [completed]
    branches: [main]
```

**P: ¿Cómo obtener el PR asociado a un `workflow_run` que vino de un fork?**
→ Usando la API de GitHub con `github.rest.pulls.list` filtrando por `head` con el owner y branch del `github.event.workflow_run.head_repository.owner.login` y `github.event.workflow_run.head_branch`.

