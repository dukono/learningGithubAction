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
Run 3: npm install (2 min) → node_modules/ descartada

Con cache:
Run 1: npm install (2 min) → node_modules/ guardada en cache
Run 2: cache hit (5 seg)   → node_modules/ restaurada
Run 3: cache hit (5 seg)   → node_modules/ restaurada
```

### Sintaxis básica

```yaml
- name: Cache node_modules
  uses: actions/cache@v4
  with:
    path: node_modules              # ← Qué guardar en caché
    key: ${{ runner.os }}-npm-${{ hashFiles('package-lock.json') }}
    # ↑ Clave única: si package-lock.json cambia, la clave cambia
    restore-keys: |                 # ← Claves de fallback (orden de prioridad)
      ${{ runner.os }}-npm-
```

### Estrategia de keys

La `key` debe ser **única** para el contenido cacheado. Si la key no existe, busca en `restore-keys`.

```yaml
# Ejemplo completo: key específica + restore-keys como fallback
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

### Cache por lenguaje/herramienta

**Node.js (npm)**
```yaml
- uses: actions/cache@v4
  with:
    path: ~/.npm
    key: ${{ runner.os }}-npm-${{ hashFiles('**/package-lock.json') }}
    restore-keys: ${{ runner.os }}-npm-

# O más sencillo, usando setup-node con cache integrado:
- uses: actions/setup-node@v4
  with:
    node-version: '20'
    cache: 'npm'     # ← Maneja la caché automáticamente
```

**Python (pip)**
```yaml
- uses: actions/cache@v4
  with:
    path: ~/.cache/pip
    key: ${{ runner.os }}-pip-${{ hashFiles('**/requirements*.txt') }}
    restore-keys: ${{ runner.os }}-pip-

# O con setup-python:
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

**Docker layers**
```yaml
- uses: actions/cache@v4
  with:
    path: /tmp/.buildx-cache
    key: ${{ runner.os }}-buildx-${{ github.sha }}
    restore-keys: ${{ runner.os }}-buildx-
```


→ Abre PRs automáticamente para actualizar dependencias cuando hay nuevas versiones disponibles.
**P: ¿Qué hace Dependabot?**

→ Permite que los deploys esperen en cola (no se cancelan) — importante para producción para no saltarse versiones.
**P: ¿Qué hace `cancel-in-progress: false` en el group "pages"?**

→ `pages: write` e `id-token: write`
**P: ¿Qué permissions son necesarios para deployar a GitHub Pages?**

→ Un environment es una configuración de despliegue que puede tener: secrets propios (solo accesibles para jobs que lo usen), variables, y protection rules (revisión manual requerida, timer de espera, restricciones de branch).
**P: ¿Qué es un Environment y qué añade respecto a un job normal?**

→ `packages: write`
**P: ¿Qué permiso necesita GITHUB_TOKEN para publicar en GitHub Packages?**

→ Se busca la caché más reciente que coincida con los prefijos en `restore-keys`, en orden de prioridad. Si no hay coincidencia, cache miss y se ejecuta el paso normalmente.
**P: ¿Qué pasa si la `key` de la caché no existe pero hay `restore-keys`?**

→ 7 días sin uso. El tamaño máximo total por repo es 10 GB.
**P: ¿Cuánto tiempo persiste la caché por defecto?**

→ La caché persiste entre ejecuciones del workflow (para reutilizar dependencias). Los artifacts comparten archivos entre jobs del mismo run (para pasar builds, reportes, etc.).
**P: ¿Cuál es la diferencia entre caché y artifacts en GitHub Actions?**

## 7. Preguntas de Examen

---

```
          GH_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        env:
        run: gh pr merge --auto --merge "${{ github.event.pull_request.html_url }}"
          steps.metadata.outputs.update-type == 'version-update:semver-minor'
          steps.metadata.outputs.update-type == 'version-update:semver-patch' ||
        if: |
      - name: Automerge si es patch o minor
      
          github-token: ${{ secrets.GITHUB_TOKEN }}
        with:
        uses: dependabot/fetch-metadata@v2
        id: metadata
      - name: Fetch metadata del PR
    steps:
    if: github.actor == 'dependabot[bot]'   # ← Solo para Dependabot
    runs-on: ubuntu-latest
  automerge:
jobs:

  contents: write
  pull-requests: write
permissions:

  pull_request:
on:

name: Dependabot Auto-merge
# .github/workflows/dependabot-automerge.yml
```yaml

### Automerge de Dependabot

```
      interval: monthly
    schedule:
    directory: "/"
  - package-ecosystem: docker
  # Actualizar Docker
  
        versions: [">=2.0.0"]        # ← Ignorar versiones específicas
      - dependency-name: "some-package"
    ignore:
    open-pull-requests-limit: 10     # ← Máximo PRs abiertos
      interval: weekly
    schedule:
    directory: "/"
  - package-ecosystem: npm
  # Actualizar npm
  
      - "github-actions"
      - "dependencies"
    labels:
      interval: weekly               # ← daily, weekly, monthly
    schedule:
    directory: "/"                    # ← Dónde buscar workflows
  - package-ecosystem: github-actions
  # Actualizar GitHub Actions
updates:
version: 2
# .github/dependabot.yml
```yaml

### Configuración básica

Dependabot actualiza automáticamente dependencias (npm, Maven, Docker, Actions, etc.).

## 6. Dependabot

---

```
      - run: /app/run-tests.sh
    steps:
        password: ${{ secrets.GITHUB_TOKEN }}
        username: ${{ github.actor }}
      credentials:
      image: ghcr.io/mi-org/mi-app:main
    container:
    runs-on: ubuntu-latest
  test:
jobs:
```yaml

### Usar la imagen en otro job

```
          cache-to: type=gha,mode=max
          cache-from: type=gha           # ← Cache de layers en GitHub Actions
          labels: ${{ steps.meta.outputs.labels }}
          tags: ${{ steps.meta.outputs.tags }}
          push: true
          context: .
        with:
        uses: docker/build-push-action@v5
      - name: Build y Push
      
            type=sha                      # → ghcr.io/owner/repo:abc1234
            type=ref,event=tag            # → ghcr.io/owner/repo:v1.0.0
            type=ref,event=branch         # → ghcr.io/owner/repo:main
          tags: |
          images: ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}
        with:
        uses: docker/metadata-action@v5
        id: meta
      - name: Extraer metadata (tags, labels)
      
          password: ${{ secrets.GITHUB_TOKEN }}   # ← GITHUB_TOKEN tiene packages:write
          username: ${{ github.actor }}
          registry: ${{ env.REGISTRY }}
        with:
        uses: docker/login-action@v3
      - name: Login a GHCR
      
      - uses: actions/checkout@v4
    steps:
    
      packages: write                      # ← Necesario para publicar paquetes
      contents: read
    permissions:
    runs-on: ubuntu-latest
  build-push:
jobs:

  IMAGE_NAME: ${{ github.repository }}    # ← owner/repo-name
  REGISTRY: ghcr.io
env:

    tags: ['v*']
    branches: [main]
  push:
on:

name: Build and Push Docker
```yaml

### Build y push de imagen Docker

GitHub Container Registry (ghcr.io) permite almacenar imágenes Docker en GitHub.

## 5. GitHub Packages (GHCR)

---

```
        uses: actions/deploy-pages@v4
        id: deployment
      - name: Deploy
      
          path: dist/
        with:
        uses: actions/upload-pages-artifact@v3
      - name: Upload artifact
      
        uses: actions/configure-pages@v5
      - name: Configure Pages
      
      - run: npm run build       # Genera ./dist/
      - run: npm ci
      
          cache: 'npm'
          node-version: '20'
        with:
      - uses: actions/setup-node@v4
      
      - uses: actions/checkout@v4
    steps:
    
      id-token: write
      pages: write
    permissions:
    runs-on: ubuntu-latest
  build-and-deploy:
jobs:
```yaml

### Deploy de Vue/React a Pages

2. El workflow hace el deploy automáticamente
1. Settings → Pages → Source: **GitHub Actions** (no Branch)

### Configuración previa en GitHub

```
        uses: actions/deploy-pages@v4
        id: deployment
      - name: Deploy to GitHub Pages
    steps:
      url: ${{ steps.deployment.outputs.page_url }}
      name: github-pages
    environment:
    runs-on: ubuntu-latest
    needs: build
  deploy:
  
          path: _site/          # ← Directorio a publicar
        with:
        uses: actions/upload-pages-artifact@v3
      - name: Upload Pages artifact
      
          # npm run build && cp -r dist/* _site/
          # Para Vue/React:
          
          # bundle exec jekyll build --destination _site
          # Para Jekyll:
          
          cp -r public/* _site/
          mkdir -p _site
          # Para sites estáticos simples:
        run: |
      - name: Build
      
        uses: actions/configure-pages@v5
      - name: Setup Pages
      
      - uses: actions/checkout@v4
    steps:
    runs-on: ubuntu-latest
  build:
jobs:

  cancel-in-progress: false  # ← No cancelar deploys a producción
  group: "pages"
concurrency:
# Un solo deploy a la vez

  id-token: write           # ← Necesario para OIDC de Pages
  pages: write              # ← Necesario para Pages
  contents: read
permissions:

    branches: [main]
  push:
on:

name: Deploy to GitHub Pages
```yaml

### Método moderno (recomendado): actions/deploy-pages

## 4. Deploy a GitHub Pages

---

```
      - run: ./deploy.sh production
      - uses: actions/checkout@v4
    steps:
      url: https://myapp.com
      name: production             # ← Tiene Required Reviewers configurado
    environment:
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    needs: deploy-staging
  deploy-prod:
  
          ./deploy.sh staging
          echo "API Key: ${{ secrets.API_KEY }}"    # ← secret del environment
          echo "Deploy URL: ${{ vars.APP_URL }}"    # ← var del environment
      - run: |
      - uses: actions/checkout@v4
    steps:
      url: https://staging.myapp.com
      name: staging
    environment:
    runs-on: ubuntu-latest
    needs: test
  deploy-staging:
  
      - run: npm test
      - uses: actions/checkout@v4
    steps:
    runs-on: ubuntu-latest
  test:
jobs:

  pull_request:
    branches: [main]
  push:
on:

name: CI/CD
```yaml

### Ejemplo completo: CI/CD con environments

Cada vez que un job usa un environment, GitHub registra el deployment. Visible en la interfaz del repositorio bajo "Environments".

### Deployment history

- `Protected branches only` → Solo ramas protegidas
- `Selected branches: main` → Solo la rama main puede usar este environment
Configurable en la UI:

**3. Deployment branches:** Solo ramas/tags específicos pueden deployar

Configurable en la UI: Settings → Environments → production → Wait timer: 10 minutos

**2. Wait timer:** Espera N minutos antes de ejecutar

```
      # ← GitHub pausará aquí esperando aprobación antes de ejecutar
      - run: ./deploy-prod.sh
    steps:
    runs-on: ubuntu-latest
      url: https://myapp.com        # ← URL mostrada en la UI
      name: production
    environment:
  deploy-prod:
jobs:
```yaml

**1. Required reviewers:** Necesita aprobación manual antes de ejecutar

### Protection rules

```
          # DEPLOY_KEY es un secret del environment production
          echo "Deploy key: ${{ secrets.DEPLOY_KEY }}"
      - run: |
    steps:
    environment: production          # ← Activa el environment
    runs-on: ubuntu-latest
  deploy-prod:
jobs:
# En el workflow, activar un environment en un job
```yaml

**Settings → Environments → New Environment**

### Configurar environments

- **Protection rules**: aprobación manual, esperas, restricciones
- Variables propias
- Secrets propios (solo accesibles para jobs que usen ese env)
Un **environment** es una configuración de despliegue con:

### ¿Qué es un Environment?

## 3. Environments y Protection Rules

---

```
      - run: cat test-results.json | jq '.numPassedTests'
          name: test-results
        with:
      - uses: actions/download-artifact@v4
    steps:
    if: always()
    runs-on: ubuntu-latest
    needs: test
  report:
  
          path: test-results.json
          name: test-results
        with:
        if: always()              # ← Subir aunque los tests fallen
      - uses: actions/upload-artifact@v4
      - run: npm test -- --json > test-results.json
    steps:
    runs-on: ubuntu-latest
  test:
jobs:
```yaml

### Patrón: Compartir resultado entre jobs con artifact

```
    # Crea: all-artifacts/build-output/, all-artifacts/test-results/, etc.
    path: all-artifacts/
  with:
  # Sin 'name:' descarga TODOS los artifacts
- uses: actions/download-artifact@v4
```yaml

### Descargar todos los artifacts de una vez

```
          ./deploy.sh dist/
          ls -la dist/
        run: |
      - name: Desplegar
      
          path: dist/                        # ← Dónde descargar
          name: build-output                 # ← Mismo nombre
        with:
        uses: actions/download-artifact@v4
      - name: Descargar artifact
    steps:
    runs-on: ubuntu-latest
    needs: build                              # ← Debe depender del job que subió
  deploy:
  
          path: dist/
          name: build-output
        with:
      - uses: actions/upload-artifact@v4
      - run: npm run build
    steps:
    runs-on: ubuntu-latest
  build:
jobs:
```yaml

### Download: Descargar artifact en otro job

```
      !*.tmp                    # ← Excluir archivos .tmp
      *.log
      test-results/
      coverage/
    path: |
    name: test-results
  with:
- uses: actions/upload-artifact@v4
```yaml
**Subir múltiples paths:**

```
          if-no-files-found: error           # ← error|warn|ignore
          retention-days: 7                  # ← Cuántos días guardar (default: 90)
          path: dist/                        # ← Qué subir
          name: build-output                 # ← Nombre del artifact
        with:
        uses: actions/upload-artifact@v4
      - name: Subir artifact
      
      - run: npm run build         # Genera ./dist/
      - uses: actions/checkout@v4
    steps:
    runs-on: ubuntu-latest
  build:
jobs:
```yaml

### Upload: Subir artifact

| Descargable | No directamente | Sí, desde la UI de GitHub |
| Uso típico | `node_modules`, `.m2`, pip | Build outputs, test reports, binaries |
| Persistencia | Entre ejecuciones (7 días) | En el mismo run (configurable) |
| Propósito | Reutilizar deps entre **runs** | Compartir archivos entre **jobs** del mismo run |
|---|---|---|
| | Cache | Artifacts |

### ¿En qué se diferencia de la caché?

## 2. Artifacts: Compartir Archivos entre Jobs

---

> ⚠️ La caché es **branch-aware**: por defecto, un branch puede restaurar caché de sí mismo y del branch default (main). No puede acceder a caché de otros branches.

| Accesibilidad | Solo dentro del mismo branch, o desde el branch default |
| Retención | 7 días sin uso |
| Tamaño total por repo | 10 GB |
| Tamaño máximo por entrada | 10 GB |
|---|---|
| Límite | Valor |

### Límites de caché

```
  run: npm ci
  if: steps.cache.outputs.cache-hit != 'true'
- name: Instalar solo si no hay cache

    key: ${{ runner.os }}-${{ hashFiles('package-lock.json') }}
    path: node_modules
  with:
  id: cache              # ← Necesita id para leer el output
- uses: actions/cache@v4
```yaml

### Verificar si hubo cache hit
