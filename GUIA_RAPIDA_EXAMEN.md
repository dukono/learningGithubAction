# 📋 GitHub Actions: Referencia Rápida y Preparación para Examen

## 📚 Índice
1. [Mapa de Documentos](#1-mapa-de-documentos)
2. [Cheatsheet de Sintaxis](#2-cheatsheet-de-sintaxis)
3. [Flujos de Datos: Esquemas Visuales](#3-flujos-de-datos-esquemas-visuales)
4. [Errores Comunes y Soluciones](#4-errores-comunes-y-soluciones)
5. [Tablas Comparativas](#5-tablas-comparativas)
6. [Preguntas Tipo Certificación](#6-preguntas-tipo-certificación)

---

## 1. Mapa de Documentos

| Documento | Temas clave |
|-----------|-------------|
| [`ARQUITECTURA_TECNICA.md`](ARQUITECTURA_TECNICA.md) | Jerarquía workflow→job→step, ciclo de vida, eventos internos, runners, ciclo de ejecución |
| [`CONTEXTOS.md`](CONTEXTOS.md) | `github.*`, `env`, `job`, `steps`, `runner`, `secrets`, `vars`, `strategy`, `matrix`, `needs`, `inputs`, arrays `.*` |
| [`EVENTOS.md`](EVENTOS.md) | `push`, `pull_request`, `pull_request_target`, `schedule`, `workflow_dispatch`, `workflow_call`, `workflow_run`, filtros |
| [`EXPRESIONES.md`](EXPRESIONES.md) | `${{ }}`, operadores, `contains/startsWith/endsWith/format/join/toJSON/fromJSON/hashFiles`, `success/failure/always/cancelled` |
| [`JOBS_AVANZADOS.md`](JOBS_AVANZADOS.md) | Outputs `GITHUB_OUTPUT→job→needs`, `GITHUB_ENV/PATH/STEP_SUMMARY`, concurrency, permissions, matrix, containers, services, `if:` |
| [`WORKFLOWS_REUTILIZABLES.md`](WORKFLOWS_REUTILIZABLES.md) | `workflow_call`, inputs tipados, outputs de workflow, `secrets: inherit`, 7 limitaciones |
| [`ACTIONS_PERSONALIZADAS.md`](ACTIONS_PERSONALIZADAS.md) | Composite (`shell:` obligatorio), JavaScript (ncc, `@actions/core`), Docker, Marketplace |
| [`SEGURIDAD_AVANZADA.md`](SEGURIDAD_AVANZADA.md) | `GITHUB_TOKEN` scopes, secrets 3 niveles, OIDC, `pull_request_target`, SHA pinning, script injection, Check Runs, CodeQL, `github-script` |
| [`CACHE_ARTIFACTS_DEPLOYMENT.md`](CACHE_ARTIFACTS_DEPLOYMENT.md) | `actions/cache`, key/restore-keys, artifacts, Environments, Pages, GHCR, Dependabot |
| [`RUNNERS_DEBUGGING.md`](RUNNERS_DEBUGGING.md) | GitHub-hosted specs, self-hosted, runners efímeros (`--ephemeral`), ARC/K8s, facturación/límites por plan, debug logging, `act` |
| [`ADMINISTRACION_Y_CICD.md`](ADMINISTRACION_Y_CICD.md) | Políticas de org, `workflow_run`, GitFlow, testing avanzado, release-please, Slack/Teams, Docker multi-stage |

---

## 2. Cheatsheet de Sintaxis

> Sintaxis exacta lista para copiar. Si necesitas entender **por qué** funciona algo, ve al documento correspondiente del índice de arriba.

### Estructura mínima

```yaml
name: Mi Workflow
on: push
jobs:
  mi-job:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: echo "Hola"
```

### Variables de entorno especiales (ficheros de comunicación)

```bash
# Paso de datos entre steps del mismo job
echo "key=valor"        >> $GITHUB_OUTPUT     # → ${{ steps.id.outputs.key }}
echo "VAR=valor"        >> $GITHUB_ENV         # → $VAR en steps siguientes
echo "/ruta/bin"        >> $GITHUB_PATH        # → añade al $PATH
echo "## Título"        >> $GITHUB_STEP_SUMMARY  # → pestaña Summary en UI

# Variables automáticas de entorno (siempre disponibles en run:)
$GITHUB_SHA             # Hash del commit
$GITHUB_REF             # refs/heads/main
$GITHUB_REF_NAME        # main
$GITHUB_REPOSITORY      # owner/repo
$GITHUB_ACTOR           # usuario que disparó el workflow
$GITHUB_RUN_ID          # ID único de la ejecución
$GITHUB_RUN_NUMBER      # número secuencial
$GITHUB_WORKSPACE       # /home/runner/work/repo/repo
$GITHUB_TOKEN           # token automático (= secrets.GITHUB_TOKEN)
$RUNNER_OS              # Linux / Windows / macOS
```

### Flujo de outputs (paso de datos)

```yaml
# STEP → GITHUB_OUTPUT (con id obligatorio)
- id: mi-step
  run: echo "version=1.2.3" >> $GITHUB_OUTPUT

# STEP OUTPUT → JOB OUTPUT
jobs:
  build:
    outputs:
      version: ${{ steps.mi-step.outputs.version }}

# JOB OUTPUT → otro JOB (via needs)
- run: echo "${{ needs.build.outputs.version }}"
```

### Concurrency

```yaml
concurrency:
  group: ${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: true   # cancela ejecución anterior del mismo grupo
```

### Permissions

```yaml
permissions:
  contents: read
  issues: write
  pull-requests: write
  packages: write
  id-token: write      # para OIDC
  security-events: write  # para CodeQL / SARIF
  checks: write        # para Check Runs / test-reporter
```

### Matrix

```yaml
strategy:
  fail-fast: false
  max-parallel: 4
  matrix:
    os: [ubuntu-latest, windows-latest]
    node: ['18', '20']
    include:
      - os: ubuntu-latest
        node: '22'
        extra: valor
    exclude:
      - os: windows-latest
        node: '18'
```

### Matrix dinámica (generada en runtime)

```yaml
jobs:
  gen:
    outputs:
      matrix: ${{ steps.set.outputs.matrix }}
    steps:
      - id: set
        run: echo 'matrix={"env":["dev","staging"]}' >> $GITHUB_OUTPUT
  use:
    needs: gen
    strategy:
      matrix: ${{ fromJSON(needs.gen.outputs.matrix) }}
```

### Workflow reutilizable — callee

```yaml
on:
  workflow_call:
    inputs:
      env: { type: string, required: true }
      flag: { type: boolean, default: true }
    secrets:
      token: { required: false }
    outputs:
      url: { value: ${{ jobs.job1.outputs.url }} }
```

### Workflow reutilizable — caller

```yaml
jobs:
  mi-job:
    uses: ./.github/workflows/reusable.yml
    with:
      env: staging
    secrets: inherit          # o secrets: { token: ${{ secrets.MY_TOKEN }} }
```

### Composite action (action.yml)

```yaml
runs:
  using: composite
  steps:
    - shell: bash             # ← OBLIGATORIO en cada run: de composite
      run: echo "hola"
    - uses: actions/checkout@v4
```

### Funciones de expresión

```yaml
${{ contains(github.ref, 'main') }}                        # boolean
${{ startsWith(github.ref, 'refs/heads/') }}               # boolean
${{ endsWith(github.ref, '/main') }}                       # boolean
${{ format('v{0}.{1}', '1', '2') }}                       # 'v1.2'
${{ join(github.event.pull_request.labels.*.name, ', ') }} # 'bug, urgent'
${{ toJSON(github.event) }}                                # string JSON
${{ fromJSON('{"k":"v"}').k }}                             # 'v'
${{ hashFiles('**/package-lock.json') }}                   # SHA-256 hex
${{ inputs.val || 'default' }}                             # operador ||
```

### Funciones de estado (en if:)

```yaml
if: success()     # todos los pasos anteriores OK (DEFAULT)
if: failure()     # al menos uno falló
if: cancelled()   # fue cancelado
if: always()      # siempre, sin importar el resultado
```

---

## 3. Flujos de Datos: Esquemas Visuales

> Úsalos cuando necesites recordar **en qué dirección viajan los datos** y qué mecanismo usar en cada caso (outputs, artifacts, caché, caller/callee).

```
STEP → STEP (mismo job)
  echo "k=v" >> $GITHUB_OUTPUT  (step con id: A)
  ${{ steps.A.outputs.k }}      (step siguiente)

  echo "K=v" >> $GITHUB_ENV
  $K                             (step siguiente, bash)

JOB → JOB
  job A:
    outputs:
      k: ${{ steps.x.outputs.k }}   ← declara
  job B (needs: A):
    ${{ needs.A.outputs.k }}        ← consume

ARCHIVOS entre jobs → Artifacts
  job A: actions/upload-artifact → name: build
  job B: actions/download-artifact ← name: build

WORKFLOWS entre runs → Cache
  Run 1: actions/cache (cache miss) → guarda ~/.npm
  Run 2: actions/cache (cache hit)  → restaura ~/.npm

CALLER → CALLEE → CALLER
  caller: uses: ./.github/workflows/reusable.yml
          with: { env: staging }
  callee: inputs.env
  callee outputs: url: ${{ jobs.j.outputs.url }}
  caller: ${{ needs.mi-job.outputs.url }}
```

---

## 4. Errores Comunes y Soluciones

> Los 10 errores más frecuentes en GitHub Actions. Si tu workflow falla y no sabes por qué, revisa primero esta lista.

**1. `steps.id.outputs.key` vacío**
```yaml
# ❌ GITHUB_ENV no crea outputs entre jobs
run: echo "v=1" >> $GITHUB_ENV

# ✅ GITHUB_OUTPUT + declarar en job outputs
run: echo "v=1" >> $GITHUB_OUTPUT     # step con id: mi-step
# en el job:
outputs:
  v: ${{ steps.mi-step.outputs.v }}
```

**2. `if: !cancelled()` — error de YAML**
```yaml
if: "!cancelled()"   # ✅ Entre comillas
if: !cancelled()     # ❌ YAML interpreta ! como tipo
```

**3. Composite action sin `shell:`**
```yaml
# ❌ Falta shell
- run: echo "hola"

# ✅
- shell: bash
  run: echo "hola"
```

**4. `uses:` + `runs-on:` en el mismo job**
```yaml
# ❌ Son mutuamente excluyentes
jobs:
  mi-job:
    uses: ./.github/workflows/reusable.yml
    runs-on: ubuntu-latest   # ERROR

# ✅ El callee define su propio runs-on
jobs:
  mi-job:
    uses: ./.github/workflows/reusable.yml
```

**5. Script injection**
```yaml
# ❌ VULNERABLE
run: echo "${{ github.event.pull_request.title }}"

# ✅ SEGURO — pasar como variable de entorno
env:
  TITLE: ${{ github.event.pull_request.title }}
run: echo "$TITLE"
```

**6. Hostname de service con `container:` en el job**
```yaml
# Sin container: → hostname = localhost
# Con container: → hostname = nombre del service

services:
  postgres:          # ← hostname: "postgres", NO "localhost"
    image: postgres:15
container:
  image: python:3.12
```

**7. `matrix` en job que usa `uses:`**
```yaml
# ❌ No se puede combinar
jobs:
  test:
    strategy:
      matrix:
        v: ['18', '20']
    uses: ./.github/workflows/reusable.yml   # ERROR
```

**8. `GITHUB_HEAD_REF` vacío en push**
```yaml
# GITHUB_HEAD_REF solo existe en pull_request
# En push usar GITHUB_SHA o GITHUB_REF_NAME
- run: git checkout $GITHUB_SHA   # ✅ siempre disponible
```

**9. Cache `npm` sin `package-lock.json`**
```yaml
# setup-node con cache: 'npm' falla si no hay package-lock.json
# Crear uno mínimo si no existe:
- run: |
    [ -f package-lock.json ] || echo '{"lockfileVersion":3,"packages":{}}' > package-lock.json
- uses: actions/setup-node@v4
  with: { node-version: '20', cache: 'npm' }
```

**10. Anidamiento de workflows reutilizables > 4 niveles**
```
Caller → Callee1 → Callee2 → Callee3 → Callee4 → ❌ ERROR (máx 4 niveles)
```

---

## 5. Tablas Comparativas

> Para los conceptos que se confunden entre sí. Consultar cuando necesitas elegir entre dos opciones y recordar sus diferencias clave.

### Cache vs Artifacts

| | Cache | Artifacts |
|---|---|---|
| **Propósito** | Reutilizar deps entre runs | Compartir archivos entre jobs |
| **Persistencia** | Entre runs (7 días sin uso) | Solo el run actual (90 días default) |
| **Tamaño máx** | 10 GB por repo | 10 GB por archivo |
| **Descargable UI** | No | Sí |
| **Action** | `actions/cache@v4` | `actions/upload/download-artifact@v4` |

### GITHUB_TOKEN vs PAT vs OIDC

| | GITHUB_TOKEN | PAT | OIDC |
|---|---|---|---|
| **Generado por** | GitHub automáticamente | El usuario | GitHub + proveedor cloud |
| **Duración** | Solo el run | Hasta revocar (meses) | ~15 minutos |
| **Alcance** | Solo el repo actual | Repos con acceso | Proveedor cloud (AWS/Azure/GCP) |
| **Riesgo si se filtra** | Bajo (expira solo) | Alto (acceso permanente) | Nulo (ya expiró) |
| **Config necesaria** | Ninguna | Crear y guardar como secret | Trust policy en el proveedor |

### `pull_request` vs `pull_request_target`

| | `pull_request` | `pull_request_target` |
|---|---|---|
| **Código ejecutado** | Rama del PR (head) | Rama destino (base) |
| **Secrets en forks** | ❌ No | ✅ Sí ⚠️ |
| **Riesgo** | Bajo | Alto si se hace checkout del PR |
| **Uso seguro** | CI tests del código del PR | Comentarios/labels sin ejecutar código del PR |

### Composite vs Reusable Workflow vs JS Action

| | Composite | Reusable Workflow | JS Action |
|---|---|---|---|
| **Nivel** | Step | Workflow completo | Step |
| **Define runner** | ❌ (usa el del job) | ✅ | ❌ |
| **`shell:` obligatorio** | ✅ | ❌ | ❌ |
| **Lenguaje lógica** | Shell/YAML | YAML | Node.js |
| **Publicable en Marketplace** | ✅ | ❌ | ✅ |

### GitHub-hosted vs Self-hosted vs Efímero

| | GitHub-hosted | Self-hosted persistente | Self-hosted efímero |
|---|---|---|---|
| **Quién lo mantiene** | GitHub | Tú | Tú |
| **Estado entre jobs** | VM nueva siempre | Persiste (puede acumular) | Limpio siempre |
| **Seguridad** | Alta | Media | Alta |
| **Flag** | — | — | `--ephemeral` |
| **Uso en repos públicos** | ✅ Seguro | ⚠️ Riesgo | ✅ Seguro |

---

## 6. Preguntas Tipo Certificación

> Preguntas organizadas por tema. Las respuestas son concisas y orientadas al examen — para la explicación completa consulta el documento correspondiente.

### Fundamentos

**P: ¿Qué directorio debe contener los workflows?**
→ `.github/workflows/`

**P: ¿Cuál es la unidad de paralelización en GitHub Actions?**
→ El **job** — cada job puede ejecutarse en paralelo en un runner diferente.

**P: ¿Qué comparten los steps de un job que los jobs NO comparten?**
→ El filesystem, variables de entorno del shell y procesos en background.

**P: ¿Sin `actions/checkout`, qué hay en el workspace?**
→ El directorio existe pero está vacío — no hay código del repositorio.

**P: ¿Qué hace `cancel-in-progress: true` en `concurrency:`?**
→ Cancela la ejecución anterior del mismo grupo cuando llega una nueva.

### Contextos y Variables

**P: ¿Qué diferencia hay entre `GITHUB_OUTPUT` y `GITHUB_ENV`?**
→ `GITHUB_OUTPUT` crea outputs accesibles por id de step (`steps.id.outputs.k`). `GITHUB_ENV` crea variables de entorno bash accesibles directamente (`$MI_VAR`) en steps siguientes del mismo job.

**P: ¿`GITHUB_BASE_REF` está disponible en un evento `push`?**
→ No. Solo existe en `pull_request`. En push está vacío.

**P: ¿Cuándo se evalúan las expresiones `${{ }}`?**
→ En los servidores de GitHub, **antes** de enviar el job al runner. El runner recibe los valores ya resueltos.

**P: ¿Qué es `steps.id.conclusion` vs `steps.id.outcome`?**
→ `outcome`: resultado real del step. `conclusion`: lo que el workflow considera (puede ser `success` aunque `outcome` sea `failure` si hay `continue-on-error: true`).

### Jobs Avanzados

**P: ¿Puede un job que usa `uses:` tener también `runs-on:` y `steps:`?**
→ No. `uses:` es mutuamente excluyente con `runs-on:` y `steps:`.

**P: ¿Puede un job con `uses:` (workflow reutilizable) tener `strategy.matrix`?**
→ No. No se puede combinar `uses:` con `strategy.matrix` en el mismo job.

**P: ¿Cuántos niveles máximos puede tener el anidamiento de workflows reutilizables?**
→ 4 niveles.

**P: En un job con `container:` y un service llamado `postgres`, ¿cuál es el hostname?**
→ `postgres` (el nombre del service). Sin `container:`, sería `localhost`.

**P: ¿Qué hace `fail-fast: false` en una matrix?**
→ Permite que el resto de combinaciones continúen aunque una falle. Sin esto (default `true`), si una falla, todas se cancelan.

### Seguridad

**P: ¿Qué permiso de GITHUB_TOKEN es necesario para usar OIDC?**
→ `id-token: write`

**P: ¿Qué permiso necesita un workflow para publicar alertas de Code Scanning (SARIF)?**
→ `security-events: write`

**P: ¿Por qué es más seguro usar SHA en lugar de tag para fijar una action?**
→ Los tags son mutables (pueden reetiquetarse). El SHA es inmutable.

**P: ¿En qué 3 niveles se pueden configurar secrets en GitHub?**
→ Organización, Repositorio, y Environment.

**P: ¿Qué es un Check Run y cómo se relaciona con las branch protection rules?**
→ Un Check Run es el resultado de un job en la API de GitHub. Las branch protection rules pueden requerir que check runs específicos pasen antes de permitir el merge.

**P: ¿Cuándo usar `actions/github-script` en lugar de `gh` CLI?**
→ Cuando se necesita lógica JavaScript compleja, múltiples llamadas API encadenadas, acceso a GraphQL, o capturar el valor de retorno directamente como output del step.

### Runners y Administración

**P: ¿Cuánto consume en minutos un job de 10 minutos en macOS (repo privado)?**
→ 100 minutos (multiplicador ×10).

**P: ¿Cuántos minutos incluye el plan Free de GitHub Actions?**
→ 2.000 minutos/mes para repos privados. Repos públicos: ilimitado gratis.

**P: ¿Qué es un runner efímero y cómo se activa?**
→ Un runner self-hosted que se registra, ejecuta un único job y se da de baja automáticamente. Se activa con el flag `--ephemeral` en el comando `config.sh`.

**P: ¿Cómo restringe un admin de organización qué actions pueden usar los repos?**
→ En `Organization Settings → Actions → General → Actions permissions`. Puede permitir todas, solo las de GitHub, o una lista explícita de patrones.

**P: ¿Por qué no se deben usar self-hosted runners persistentes en repos públicos?**
→ Código malicioso de PRs de extraños podría ejecutarse en tu máquina con acceso a tu red interna.

### CI/CD Empresarial

**P: ¿Para qué sirve `workflow_run` y qué problema de seguridad resuelve?**
→ Dispara un workflow cuando otro termina. Resuelve el problema de PRs de forks: el CI se ejecuta sin secrets (código del fork), y `workflow_run` dispara un segundo workflow con secrets (código de main) para comentar el resultado de forma segura.

**P: ¿Qué hace `release-please` y con qué convención de commits funciona?**
→ Automatiza CHANGELOGs y releases semánticos analizando commits convencionales (`feat:` → minor, `fix:` → patch, `feat!:` → major). Crea un PR de release que al mergearse genera el tag y el GitHub Release.

**P: ¿Qué ventaja tiene un multi-stage Dockerfile?**
→ La imagen final solo incluye artefactos del stage de producción, sin herramientas de build ni código fuente. Reduce tamaño e imagen de ataque.

**P: ¿Qué hace `cache-from: type=gha` en `docker/build-push-action`?**
→ Usa GitHub Actions Cache para cachear layers de Docker entre ejecuciones, evitando reconstruir layers que no han cambiado.

**P: ¿Qué scope necesita GITHUB_TOKEN para que `dorny/test-reporter` publique annotations?**
→ `checks: write`

