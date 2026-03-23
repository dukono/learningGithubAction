# 📦 GitHub Actions: Cache, Artifacts y Deployment

## 📚 Índice
1. [Cache con actions/cache](#1-cache-con-actionscache)
2. [Artifacts: Compartir Archivos entre Jobs](#2-artifacts-compartir-archivos-entre-jobs)
3. [Environments y Protection Rules](#3-environments-y-protection-rules)
4. [Deploy a GitHub Pages](#4-deploy-a-github-pages)
5. [GitHub Packages (GHCR)](#5-github-packages-ghcr)
6. [Dependabot](#6-dependabot)
7. [Preguntas de Examen](#7-preguntas-de-examen)

---

## 1. Cache con actions/cache

### ¿Para qué sirve la caché?

La caché **persiste archivos entre ejecuciones** del workflow para no tener que descargar/compilar dependencias cada vez.

```
Sin cache:
Run 1: npm install (2 min) → node_modules/ descartada
Run 2: npm install (2 min) → node_modules/ descartada

Con cache:
Run 1: npm install (2 min) → node_modules/ guardada en cache
Run 2: cache hit (5 seg)   → node_modules/ restaurada
```

### Sintaxis básica

```yaml
- name: Cache node_modules
  uses: actions/cache@v4
  with:
    path: node_modules
    key: ${{ runner.os }}-npm-${{ hashFiles('package-lock.json') }}
    restore-keys: |
      ${{ runner.os }}-npm-
```

### Verificar si hubo cache hit

```yaml
- uses: actions/cache@v4
  id: cache
  with:
    path: node_modules
    key: ${{ runner.os }}-${{ hashFiles('package-lock.json') }}

- name: Instalar solo si no hay cache
  if: steps.cache.outputs.cache-hit != 'true'
  run: npm ci
```

### Estrategia de keys

```yaml
- uses: actions/cache@v4
  id: npm-cache
  with:
    path: ~/.npm
    key: ${{ runner.os }}-node-${{ matrix.node }}-${{ hashFiles('**/package-lock.json') }}
    restore-keys: |
      ${{ runner.os }}-node-${{ matrix.node }}-
      ${{ runner.os }}-node-
      ${{ runner.os }}-
```

**Proceso de búsqueda:**
1. Busca `linux-node-20-abc123hash` → exacta
2. Si no: busca el más reciente que empiece con `linux-node-20-`
3. Si no: busca el más reciente que empiece con `linux-node-`
4. Si no: busca el más reciente que empiece con `linux-`
5. Si no: cache miss → instalar desde cero

### Trampa: restore parcial no equivale a hit exacto

Cuando se restaura por `restore-keys` (coincidencia parcial), el cache puede estar desactualizado. El paso de instalación debe ejecutarse igualmente. Solo se omite con `cache-hit == 'true'` (coincidencia exacta):

```yaml
- uses: actions/cache@v4
  id: cache
  with:
    path: ~/.npm
    key: ${{ runner.os }}-npm-${{ hashFiles('**/package-lock.json') }}
    restore-keys: ${{ runner.os }}-npm-

- name: Instalar dependencias
  if: steps.cache.outputs.cache-hit != 'true'
  run: npm ci
```

### Cuándo se guarda la caché

La caché se guarda al **final del job**, incluso si algún step falla. Si el runner se cancela abruptamente (no fallo normal), la caché NO se guarda.

### Límites de caché

| Límite | Valor |
|---|---|
| Tamaño máximo por entrada | 10 GB |
| Tamaño total por repo | 10 GB |
| Retención sin uso | 7 días |
| Accesibilidad | Solo mismo branch o branch default (main) |

> ⚠️ La caché es **branch-aware**: una feature branch puede restaurar caché de sí misma y de main, pero no de otras feature branches.

### Cache entre branches: qué puede restaurar qué

```
main        → puede restaurar: main
feature/x   → puede restaurar: feature/x, main
feature/x   NO puede restaurar: feature/y, develop
```

### Invalidar caché manualmente

```bash
# Cambiar el prefijo de versión en la key
key: v2-${{ runner.os }}-npm-${{ hashFiles('package-lock.json') }}

# Borrar via gh CLI
gh cache list
gh cache delete CACHE_ID
gh cache delete --all
```

### Cache por lenguaje/herramienta

**Node.js (npm)**
```yaml
- uses: actions/setup-node@v4
  with:
    node-version: '20'
    cache: 'npm'     # ← Maneja la caché automáticamente
```

**Node.js (yarn)**
```yaml
- uses: actions/setup-node@v4
  with:
    node-version: '20'
    cache: 'yarn'
```

**Node.js (pnpm)**
```yaml
- uses: pnpm/action-setup@v4
  with:
    version: 9
- uses: actions/setup-node@v4
  with:
    node-version: '20'
    cache: 'pnpm'
```

**Python (pip)**
```yaml
- uses: actions/setup-python@v5
  with:
    python-version: '3.12'
    cache: 'pip'
```

**Java (Maven)**
```yaml
- uses: actions/cache@v4
  with:
    path: ~/.m2
    key: ${{ runner.os }}-maven-${{ hashFiles('**/pom.xml') }}
    restore-keys: ${{ runner.os }}-maven-
```

**Java (Gradle)**
```yaml
- uses: actions/cache@v4
  with:
    path: |
      ~/.gradle/caches
      ~/.gradle/wrapper
    key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
    restore-keys: ${{ runner.os }}-gradle-
```

**Go**
```yaml
- uses: actions/cache@v4
  with:
    path: |
      ~/go/pkg/mod
      ~/.cache/go-build
    key: ${{ runner.os }}-go-${{ hashFiles('**/go.sum') }}
    restore-keys: ${{ runner.os }}-go-
```

**Rust (cargo)**
```yaml
- uses: actions/cache@v4
  with:
    path: |
      ~/.cargo/bin/
      ~/.cargo/registry/index/
      ~/.cargo/registry/cache/
      ~/.cargo/git/db/
      target/
    key: ${{ runner.os }}-cargo-${{ hashFiles('**/Cargo.lock') }}
```

**Docker layers (manual)**
```yaml
- uses: actions/cache@v4
  with:
    path: /tmp/.buildx-cache
    key: ${{ runner.os }}-buildx-${{ github.sha }}
    restore-keys: ${{ runner.os }}-buildx-
```

### Trampa: cache explosion con Docker layers

La caché manual de Docker layers se acumula indefinidamente. Solución con caché rotante:

```yaml
  - uses: docker/build-push-action@v5
    with:
      cache-from: type=local,src=/tmp/.buildx-cache
      cache-to: type=local,dest=/tmp/.buildx-cache-new,mode=max

  - name: Rotar cache
    run: |
      rm -rf /tmp/.buildx-cache
      mv /tmp/.buildx-cache-new /tmp/.buildx-cache
```

> 💡 **Alternativa preferida**: Usar `cache-from: type=gha` en `docker/build-push-action` — evita el problema completamente. Ver sección GHCR.

---

## 2. Artifacts: Compartir Archivos entre Jobs

Cada job corre en una **VM nueva y aislada**. El filesystem no se comparte entre jobs — cuando el job termina, todo desaparece.

Los **artifacts** permiten subir archivos al almacenamiento de GitHub para que otro job los descargue.

```
Sin artifacts:
  Job build  → genera dist/ → VM se destruye → dist/ desaparece
  Job deploy → ¿de dónde saca el build? → ❌ No puede

Con artifacts:
  Job build  → genera dist/ → upload-artifact → guardado en GitHub
  Job deploy → download-artifact → tiene dist/ → ✅ puede desplegar
```

### ¿En qué se diferencia de la caché?

| | Cache | Artifacts |
|---|---|---|
| Propósito | Reutilizar deps entre **runs** | Compartir archivos entre **jobs** del mismo run |
| Persistencia | 7 días sin uso (entre runs) | 90 días por defecto (dentro del run) |
| Uso típico | `node_modules`, `.m2`, pip cache | Build outputs, test reports, binaries |
| Descargable desde UI | No | Sí, desde la pestaña Actions del run |
| Límite de tamaño | 10 GB total por repo | 10 GB por archivo individual |

### Upload: Subir artifact

```yaml
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: npm run build

      - name: Subir artifact
        uses: actions/upload-artifact@v4
        with:
          name: build-output
          path: dist/
          retention-days: 7          # ← Cuántos días guardar (default: 90)
          if-no-files-found: error   # ← error | warn | ignore
```

**Subir múltiples paths:**
```yaml
- uses: actions/upload-artifact@v4
  with:
    name: test-results
    path: |
      coverage/
      test-results/
      *.log
      !*.tmp                    # ← Excluir archivos .tmp
```

### Download: Descargar artifact en otro job

```yaml
jobs:
  deploy:
    needs: build
    runs-on: ubuntu-latest
    steps:
      - uses: actions/download-artifact@v4
        with:
          name: build-output         # ← Mismo nombre que al subir
          path: dist/
      - run: ./deploy.sh dist/
```

### Descargar todos los artifacts de una vez

```yaml
- uses: actions/download-artifact@v4
  with:
    path: all-artifacts/
    # Sin 'name:' descarga TODOS los artifacts del run actual
    # Crea: all-artifacts/build-output/, all-artifacts/test-results/, etc.
```

### Descargar artifacts de otro run o workflow

```yaml
- uses: actions/download-artifact@v4
  with:
    name: build-output
    run-id: ${{ github.event.workflow_run.id }}
    github-token: ${{ secrets.GITHUB_TOKEN }}   # ← Requerido para runs externos
```

### Patrón: Compartir resultado entre jobs

```yaml
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - run: npm test -- --json > test-results.json
      - uses: actions/upload-artifact@v4
        if: always()
        with:
          name: test-results
          path: test-results.json

  report:
    needs: test
    runs-on: ubuntu-latest
    if: always()
    steps:
      - uses: actions/download-artifact@v4
        with:
          name: test-results
      - run: cat test-results.json | jq '.numPassedTests'
```

### Trampa: nombre de artifact duplicado en matrix

Varios jobs de matrix no pueden subir artifacts con el mismo nombre — se sobreescriben:

```yaml
- uses: actions/upload-artifact@v4
  with:
    name: test-results-${{ matrix.os }}   # ← Nombre único por combinación
    path: test-results.xml
```

### Descargar artifacts de matrix con `pattern:`

```yaml
- uses: actions/download-artifact@v4
  with:
    pattern: test-results-*    # ← Descarga todos los que coincidan
    merge-multiple: true       # ← Los fusiona en un solo directorio
```

### Trampa: artifacts de runs anteriores requieren `run-id`

Sin `run-id`, solo descarga artifacts del run actual. Para artifacts de ejecuciones anteriores:

```yaml
- name: Obtener run-id del último build exitoso de main
  id: get-run
  uses: actions/github-script@v7
  with:
    script: |
      const runs = await github.rest.actions.listWorkflowRuns({
        owner: context.repo.owner,
        repo: context.repo.repo,
        workflow_id: 'build.yml',
        branch: 'main',
        status: 'success',
        per_page: 1
      })
      return runs.data.workflow_runs[0].id

- uses: actions/download-artifact@v4
  with:
    name: build-output
    run-id: ${{ steps.get-run.outputs.result }}
    github-token: ${{ secrets.GITHUB_TOKEN }}
```

### Retención de artifacts

| Configuración | Valor |
|---|---|
| Default del repo | 90 días |
| Mínimo | 1 día |
| Máximo | 400 días |
| `retention-days: 0` | Usa el default del repo |

La retención global se configura en `Settings → Actions → General → Artifact and log retention`.

---

## 3. Environments y Protection Rules

Un **environment** representa un destino de despliegue (dev, staging, producción). Permite separar la configuración y los permisos por entorno, añadir controles antes de desplegar, y registrar el historial de deployments.

Sin environments, cualquier job puede acceder a todos los secrets del repositorio. Con environments, puedes restringir secrets sensibles solo a jobs que declaren ese environment, y requerir aprobación manual antes de ejecutar.

### Qué es un Environment

Un environment tiene:
- **Secrets propios** — solo accesibles para jobs que declaren ese environment
- **Variables propias** — accesibles con `${{ vars.NOMBRE }}`
- **Protection rules** — aprobación manual, timers de espera, restricciones de rama

**Settings → Environments → New Environment**

### Configurar environments

```yaml
jobs:
  deploy-prod:
    runs-on: ubuntu-latest
    environment: production          # ← Forma corta
    steps:
      - run: echo "Key: ${{ secrets.DEPLOY_KEY }}"   # ← secret del environment

# Forma larga: con URL visible en la UI
jobs:
  deploy-prod:
    runs-on: ubuntu-latest
    environment:
      name: production
      url: https://myapp.com
    steps:
      - run: ./deploy-prod.sh
```

### Protection rules

**1. Required reviewers:** Necesita aprobación manual. GitHub notifica a los revisores y el job espera.

**2. Wait timer:** Espera N minutos antes de ejecutar, independientemente de aprobaciones.
`Settings → Environments → [nombre] → Wait timer`

**3. Deployment branches:** Solo ramas/tags específicos pueden deployar.
- `All branches` — cualquier rama (default)
- `Protected branches only`
- `Selected branches` — patrones (ej: `main`, `release/*`)

### Trampa: typo en nombre de environment crea uno sin protección

```yaml
environment: produccion   # ← typo: "produccion" en lugar de "production"
# GitHub crea un environment "produccion" sin reglas
# el deploy se ejecuta sin esperar aprobación
```

### Trampa: secrets de repositorio no están protegidos por environments

```yaml
# ⚠️ Secret de repositorio → cualquier job lo ve sin importar el environment
secrets.PROD_DB_PASSWORD  # en: Settings → Secrets → Repository secrets

# ✅ Secret de environment → solo accesible con environment: production
secrets.PROD_DB_PASSWORD  # en: Settings → Environments → production → Secrets
```

> Los secrets sensibles deben estar a nivel de environment para que las protection rules sean efectivas.

### Deployment history

Cada uso de un environment registra un deployment visible en `Repositorio → Environments`.
Muestra: quién desplegó, desde qué rama/SHA, resultado y URL.

### Ejemplo completo: CI/CD con environments

```yaml
name: CI/CD
on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: npm test

  deploy-staging:
    needs: test
    runs-on: ubuntu-latest
    environment:
      name: staging
      url: https://staging.myapp.com
    steps:
      - uses: actions/checkout@v4
      - run: |
          echo "URL: ${{ vars.APP_URL }}"
          echo "Key: ${{ secrets.API_KEY }}"
          ./deploy.sh staging

  deploy-prod:
    needs: deploy-staging
    if: github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    environment:
      name: production
      url: https://myapp.com
    steps:
      - uses: actions/checkout@v4
      - run: ./deploy.sh production
```

---

## 4. Deploy a GitHub Pages

GitHub Pages sirve **archivos estáticos**. El método moderno usa Actions para construir y desplegar en lugar de depender de una rama específica.

### Configuración previa

1. `Settings → Pages → Source` → seleccionar **GitHub Actions** (no "Deploy from branch")

### Método moderno (recomendado)

```yaml
name: Deploy to GitHub Pages
on:
  push:
    branches: [main]

permissions:
  contents: read
  pages: write
  id-token: write

concurrency:
  group: "pages"
  cancel-in-progress: false   # ← No cancelar deploys en curso

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/configure-pages@v5
      - name: Build
        run: |
          mkdir -p _site
          cp -r public/* _site/
          # Jekyll: bundle exec jekyll build --destination _site
          # Vue/React: npm run build && cp -r dist/* _site/
      - uses: actions/upload-pages-artifact@v3
        with:
          path: _site/

  deploy:
    needs: build
    runs-on: ubuntu-latest
    environment:
      name: github-pages
      url: ${{ steps.deployment.outputs.page_url }}
    steps:
      - id: deployment
        uses: actions/deploy-pages@v4
```

### Deploy de Vue/React

```yaml
jobs:
  build-and-deploy:
    runs-on: ubuntu-latest
    permissions:
      pages: write
      id-token: write
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'npm'
      - run: npm ci && npm run build
      - uses: actions/configure-pages@v5
      - uses: actions/upload-pages-artifact@v3
        with:
          path: dist/
      - id: deployment
        uses: actions/deploy-pages@v4
```

### Trampa: SPA necesita configurar `base` según el nombre del repo

Cuando la app se despliega en `usuario.github.io/mi-repo/` (no en la raíz):

```javascript
// vite.config.js
export default {
  base: '/mi-repo/',   // ← Nombre exacto del repositorio
}
```

Sin esto los assets se intentan cargar desde `/` y la app se rompe.

### Trampa: Pages solo sirve archivos estáticos

No puede ejecutar código del lado del servidor. Si necesitas backend, usa Vercel, Netlify, o un servidor propio desplegado vía Actions.

### Trampa: `cancel-in-progress: false` en el group "pages"

Cancelar un deploy en curso puede dejar Pages en estado inconsistente. El nuevo deploy debe esperar al anterior.

---

## 5. GitHub Packages (GHCR)

Un **container registry** almacena y distribuye imágenes Docker. En lugar de construir la imagen en cada máquina, se construye una vez, se sube, y cualquier job la descarga.

**GHCR** (`ghcr.io`) está integrado con GitHub: permisos siguiendo el repo, autenticación con `GITHUB_TOKEN`, imágenes visibles en la pestaña "Packages".

### Build y push de imagen Docker

```yaml
name: Build and Push Docker
on:
  push:
    branches: [main]
    tags: ['v*']

env:
  REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository }}

jobs:
  build-push:
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write

    steps:
      - uses: actions/checkout@v4

      - name: Set up Docker Buildx         # ← OBLIGATORIO para cache y multi-platform
        uses: docker/setup-buildx-action@v3

      - name: Login a GHCR
        uses: docker/login-action@v3
        with:
          registry: ${{ env.REGISTRY }}
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Extraer metadata (tags, labels)
        id: meta
        uses: docker/metadata-action@v5
        with:
          images: ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}
          tags: |
            type=ref,event=branch
            type=ref,event=tag
            type=sha
            type=raw,value=latest,enable=${{ github.ref == 'refs/heads/main' }}

      - name: Build y Push
        uses: docker/build-push-action@v5
        with:
          context: .
          push: true
          tags: ${{ steps.meta.outputs.tags }}
          labels: ${{ steps.meta.outputs.labels }}   # ← Incluye org.opencontainers.image.source
          cache-from: type=gha
          cache-to: type=gha,mode=max
```

### Trampa: `docker/setup-buildx-action` es obligatorio para `cache-from: type=gha`

Sin Buildx el cache falla silenciosamente. Orden correcto siempre:

```yaml
- uses: docker/setup-buildx-action@v3    # ← 1. Siempre primero
- uses: docker/login-action@v3           # ← 2. Login
- uses: docker/metadata-action@v5        # ← 3. Tags y labels
- uses: docker/build-push-action@v5      # ← 4. Build con cache
```

### Trampa: imagen nueva no aparece vinculada al repositorio

Sin la label OCI la imagen no aparece en "Packages" del repo. `docker/metadata-action` la añade automáticamente. O manualmente en el Dockerfile:

```dockerfile
LABEL org.opencontainers.image.source="https://github.com/mi-org/mi-repo"
```

### Usar la imagen publicada en otro job

```yaml
jobs:
  test:
    runs-on: ubuntu-latest
    container:
      image: ghcr.io/mi-org/mi-app:main
      credentials:
        username: ${{ github.actor }}
        password: ${{ secrets.GITHUB_TOKEN }}
    steps:
      - run: /app/run-tests.sh
```

### Trampa: para imágenes privadas en `container:` o `services:` el login del step no es suficiente

GitHub hace el pull antes de ejecutar steps. Hay que añadir `credentials:` en el bloque:

```yaml
container:
  image: ghcr.io/mi-org/mi-imagen:latest
  credentials:
    username: ${{ github.actor }}
    password: ${{ secrets.GITHUB_TOKEN }}

services:
  mi-servicio:
    image: mi-empresa.jfrog.io/mi-imagen:latest
    credentials:
      username: ${{ secrets.ARTIFACTORY_USER }}
      password: ${{ secrets.ARTIFACTORY_TOKEN }}
```

### Login a registries privados (Artifactory, ECR, ACR)

```yaml
# Artifactory / JFrog
- uses: docker/login-action@v3
  with:
    registry: mi-empresa.jfrog.io
    username: ${{ secrets.ARTIFACTORY_USER }}
    password: ${{ secrets.ARTIFACTORY_TOKEN }}

# AWS ECR (con OIDC)
- uses: aws-actions/configure-aws-credentials@v4
  with:
    role-to-assume: ${{ secrets.AWS_ROLE_ARN }}
    aws-region: eu-west-1
- uses: aws-actions/amazon-ecr-login@v2

# Azure Container Registry
- uses: docker/login-action@v3
  with:
    registry: miregistro.azurecr.io
    username: ${{ secrets.ACR_USERNAME }}
    password: ${{ secrets.ACR_PASSWORD }}
```

### Build sin push (verificar Dockerfile en PRs)

```yaml
- uses: docker/build-push-action@v5
  with:
    context: .
    push: false
    load: true           # ← Cargar localmente para usar en steps siguientes
    tags: mi-app:test
    cache-from: type=gha
    cache-to: type=gha,mode=max
```

### Usar imagen construida localmente en steps siguientes

```yaml
- uses: docker/build-push-action@v5
  with:
    push: false
    load: true
    tags: mi-app:test

- run: docker run --rm mi-app:test npm test

- uses: aquasecurity/trivy-action@master
  with:
    image-ref: mi-app:test
    format: table
    exit-code: '1'       # ← Falla si hay vulnerabilidades críticas
```

> ⚠️ `load: true` es incompatible con `platforms: linux/amd64,linux/arm64`. Con multi-platform solo se puede hacer `push: true`.

### Multi-platform build (amd64 + arm64)

```yaml
- uses: docker/setup-qemu-action@v3     # ← Emulación para cross-compile
- uses: docker/setup-buildx-action@v3

- uses: docker/build-push-action@v5
  with:
    platforms: linux/amd64,linux/arm64
    push: true
    tags: ${{ steps.meta.outputs.tags }}
    cache-from: type=gha
    cache-to: type=gha,mode=max
```

### `type=gha` vs `type=registry` para cache de layers

| | `type=gha` | `type=registry` |
|---|---|---|
| Almacenamiento | GitHub Actions Cache (10 GB del repo) | En el registry como imagen especial |
| Disponibilidad | Solo mismo repo | Cualquier máquina con acceso al registry |
| Comparte límite con | `actions/cache` de dependencias | Solo el espacio del registry |
| Ideal para | Repos normales | Self-hosted runners, múltiples repos |

### Eliminar imágenes antiguas automáticamente

```yaml
- uses: actions/delete-package-versions@v5
  with:
    package-name: 'mi-app'
    package-type: 'container'
    min-versions-to-keep: 10
    delete-only-untagged-versions: true
```

---

## 6. Dependabot

Dependabot actualiza automáticamente dependencias (npm, Maven, Docker, Actions, etc.) abriendo PRs cuando hay nuevas versiones disponibles.

### Configuración básica

```yaml
# .github/dependabot.yml
version: 2
updates:
  - package-ecosystem: github-actions
    directory: "/"
    schedule:
      interval: weekly
    labels:
      - "dependencies"
      - "github-actions"

  - package-ecosystem: npm
    directory: "/"
    schedule:
      interval: weekly
    open-pull-requests-limit: 10     # ← Máximo PRs abiertos (default: 5)
    ignore:
      - dependency-name: "some-package"
        versions: [">=2.0.0"]

  - package-ecosystem: docker
    directory: "/"
    schedule:
      interval: monthly
```

### Automerge de Dependabot

```yaml
# .github/workflows/dependabot-automerge.yml
name: Dependabot Auto-merge
on:
  pull_request:

permissions:
  contents: write
  pull-requests: write

jobs:
  automerge:
    runs-on: ubuntu-latest
    if: github.actor == 'dependabot[bot]'
    steps:
      - name: Fetch metadata del PR
        id: metadata
        uses: dependabot/fetch-metadata@v2
        with:
          github-token: ${{ secrets.GITHUB_TOKEN }}

      - name: Automerge si es patch o minor
        if: |
          steps.metadata.outputs.update-type == 'version-update:semver-patch' ||
          steps.metadata.outputs.update-type == 'version-update:semver-minor'
        run: gh pr merge --auto --merge "${{ github.event.pull_request.html_url }}"
        env:
          GH_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

### Trampa: `open-pull-requests-limit` por defecto es 5

Si hay 5 PRs abiertos, Dependabot no abre más aunque haya actualizaciones. Puede parecer que dejó de funcionar. Aumentar el límite si el proyecto tiene muchas dependencias.

### Trampa: Dependabot no puede usar secrets del repositorio por defecto

Los PRs de Dependabot tienen un `GITHUB_TOKEN` de solo lectura. Si el CI necesita un secret (ej: `NPM_TOKEN`), fallará. Opciones:
1. Configurar el secret en `Organization Settings → Secrets → Grant Dependabot access`
2. Usar el workflow de automerge para pasos que necesiten secrets

### Dependabot con monorepo

```yaml
version: 2
updates:
  - package-ecosystem: npm
    directory: "/packages/api"
    schedule:
      interval: weekly

  - package-ecosystem: npm
    directory: "/packages/ui"
    schedule:
      interval: weekly

  - package-ecosystem: docker
    directory: "/services/backend"
    schedule:
      interval: monthly
```

---

## 7. Preguntas de Examen

**P: ¿Cuál es la diferencia entre caché y artifacts en GitHub Actions?**
→ La caché persiste entre ejecuciones del workflow (reutilizar dependencias). Los artifacts comparten archivos entre jobs del mismo run (pasar builds, reportes, etc.).

**P: ¿Cuánto tiempo persiste la caché por defecto?**
→ 7 días sin uso. Tamaño máximo total: 10 GB por repo.

**P: ¿Qué pasa si la `key` no existe pero hay `restore-keys`?**
→ Se busca la caché más reciente que coincida con los prefijos en `restore-keys`. Si no hay ninguna, cache miss.

**P: ¿Es un restore parcial (por `restore-keys`) igual que un hit exacto?**
→ No. Con restore parcial el contenido puede estar desactualizado — hay que ejecutar el paso de instalación igualmente. Solo se omite con `cache-hit == 'true'`.

**P: ¿Cuándo se guarda la caché al terminar un job?**
→ Siempre al finalizar (incluso si falla), excepto si el runner se cancela abruptamente.

**P: ¿Puede una feature branch usar la caché de otra feature branch?**
→ No. Solo puede usar su propia caché o la del branch default (main).

**P: ¿Cómo invalidar la caché manualmente?**
→ Cambiar el prefijo de la `key` (ej: `v1-` → `v2-`), o con `gh cache delete --all`.

**P: ¿Qué es un Environment y qué añade respecto a un job normal?**
→ Un environment tiene secrets propios, variables y protection rules (aprobación manual, timer de espera, restricción de ramas). Sus secrets solo son accesibles para jobs que lo declaren.

**P: ¿Qué pasa si declaras `environment: production` con un typo?**
→ GitHub crea automáticamente un environment sin protection rules — el deploy se ejecuta sin aprobación.

**P: ¿Por qué los secrets sensibles deben estar a nivel de environment y no de repositorio?**
→ Los secrets de repositorio son accesibles para cualquier job sin pasar por las protection rules. Solo los de environment requieren que el job declare ese environment y pase sus controles.

**P: ¿Qué permissions son necesarios para deployar a GitHub Pages?**
→ `pages: write` e `id-token: write`.

**P: ¿Por qué `cancel-in-progress: false` en el concurrency group "pages"?**
→ Cancelar un deploy en curso puede dejar Pages en estado inconsistente. El nuevo deploy debe esperar al anterior.

**P: ¿Qué permiso necesita GITHUB_TOKEN para publicar en GitHub Packages?**
→ `packages: write`.

**P: ¿Es obligatorio `docker/setup-buildx-action` para `cache-from: type=gha`?**
→ Sí. El Docker engine estándar no soporta cache externo. Buildx lo habilita.

**P: ¿Cómo vincular una imagen de GHCR al repositorio?**
→ Con la label `org.opencontainers.image.source`, o usando `docker/metadata-action` que la añade automáticamente.

**P: Para imágenes privadas en `container:` o `services:`, ¿es suficiente el step de login?**
→ No. GitHub hace el pull antes de ejecutar steps. Hay que añadir `credentials:` en el bloque `container:` o en el service.

**P: ¿Son compatibles `load: true` y multi-platform en `docker/build-push-action`?**
→ No. Con multi-platform solo se puede hacer `push: true`.

**P: ¿Qué hace Dependabot?**
→ Abre PRs automáticamente para actualizar dependencias cuando hay nuevas versiones disponibles.

**P: ¿Por qué Dependabot deja de abrir PRs aunque haya actualizaciones?**
→ Límite de 5 PRs abiertos simultáneamente por defecto. Aumentar con `open-pull-requests-limit`.

**P: ¿Cómo evitar nombres duplicados de artifacts en matrix?**
→ Incluir el valor de la variable de matrix en el nombre: `name: results-${{ matrix.os }}`.

**P: ¿Puede `actions/download-artifact@v4` descargar artifacts de runs anteriores?**
→ Sí, especificando `run-id:` y `github-token:`. Sin `run-id` solo descarga del run actual.

**P: ¿Cuál es el problema del cache explosion con Docker layers y cómo se evita?**
→ Buildx acumula entradas sin eliminar las antiguas. Se evita con caché rotante o usando `cache-from: type=gha` directamente en `docker/build-push-action`.
