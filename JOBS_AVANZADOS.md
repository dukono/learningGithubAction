# ⚙️ GitHub Actions: Jobs Avanzados

## 📚 Índice
1. [Outputs: Flujo Completo Step → Job → Workflow](#1-outputs-flujo-completo)
2. [Concurrency: Cancelar Ejecuciones Duplicadas](#2-concurrency)
3. [Permissions: Control de Permisos](#3-permissions)
4. [Matrix: Estrategias Avanzadas](#4-matrix)
5. [Containers y Services](#5-containers-y-services)
6. [Conditions (if:) en Todos los Niveles](#6-conditions-if-en-todos-los-niveles)
7. [Timeout y Continue-on-error](#7-timeout-y-continue-on-error)
8. [Job Dependencies (needs:)](#8-job-dependencies-needs)
9. [Preguntas de Examen](#9-preguntas-de-examen)

---

## 1. Outputs: Flujo Completo

Los datos fluyen **en una sola dirección**: de step a job, de job a workflow, de job a caller (vía `needs`).

### Flujo visual

```
STEP
  │ echo "key=value" >> $GITHUB_OUTPUT
  ↓
JOB outputs:
  my-key: ${{ steps.step-id.outputs.key }}
  │
  ↓
CALLER JOB
  ${{ needs.job-id.outputs.my-key }}
```

### Ejemplo completo

```yaml
jobs:
  # JOB 1: Produce datos
  build:
    runs-on: ubuntu-latest
    outputs:                                          # ← Declara lo que expone
      version: ${{ steps.get-ver.outputs.version }}
      artifact: ${{ steps.pack.outputs.filename }}
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Obtener versión
        id: get-ver                                   # ← id necesario para referenciarlo
        run: |
          VERSION=$(cat VERSION)
          echo "version=$VERSION" >> $GITHUB_OUTPUT  # ← Guardar en GITHUB_OUTPUT
          echo "Versión: $VERSION"
      
      - name: Empaquetar
        id: pack
        run: |
          FILENAME="app-${{ steps.get-ver.outputs.version }}.tar.gz"
          tar -czf "$FILENAME" src/
          echo "filename=$FILENAME" >> $GITHUB_OUTPUT
  
  # JOB 2: Consume datos del JOB 1
  deploy:
    needs: build                                      # ← Dependencia
    runs-on: ubuntu-latest
    steps:
      - name: Desplegar
        run: |
          echo "Versión: ${{ needs.build.outputs.version }}"
          echo "Archivo: ${{ needs.build.outputs.artifact }}"
```

### GITHUB_OUTPUT vs GITHUB_ENV vs outputs:

| Mecanismo | Cómo escribir | Cómo leer | Alcance |
|---|---|---|---|
| `$GITHUB_OUTPUT` | `echo "k=v" >> $GITHUB_OUTPUT` | `${{ steps.id.outputs.k }}` | Solo steps siguientes del mismo job |
| `$GITHUB_ENV` | `echo "K=v" >> $GITHUB_ENV` | `$K` (bash) o `${{ env.K }}` | Steps siguientes del mismo job |
| `outputs:` del job | Declara en el job | `${{ needs.job.outputs.k }}` | Jobs dependientes |
| `outputs:` del workflow | Declara en `workflow_call` | `${{ needs.job.outputs.k }}` (caller) | Workflow caller |

> ⚠️ **Error común**: Usar `$GITHUB_ENV` y esperar leer con `steps.id.outputs.key`. `GITHUB_ENV` solo crea variables de entorno bash, no outputs accesibles entre jobs.

### GITHUB_STEP_SUMMARY

Genera un resumen visual en la UI de GitHub Actions:

```yaml
- name: Resumen
  run: |
    echo "## 🚀 Resultado del Build" >> $GITHUB_STEP_SUMMARY
    echo "" >> $GITHUB_STEP_SUMMARY
    echo "| Campo | Valor |" >> $GITHUB_STEP_SUMMARY
    echo "|-------|-------|" >> $GITHUB_STEP_SUMMARY
    echo "| Versión | ${{ steps.get-ver.outputs.version }} |" >> $GITHUB_STEP_SUMMARY
    echo "| Estado | ✅ Éxito |" >> $GITHUB_STEP_SUMMARY
```

El resumen aparece en la pestaña **Summary** de la ejecución del workflow en GitHub.

### GITHUB_PATH

Añade directorios al `PATH` para steps siguientes:

```yaml
- name: Instalar herramienta personalizada
  run: |
    mkdir -p /home/runner/my-tools
    cp ./bin/my-tool /home/runner/my-tools/
    echo "/home/runner/my-tools" >> $GITHUB_PATH   # ← Añadir al PATH

- name: Usar herramienta
  run: my-tool --version    # ← Funciona porque está en PATH
```

---

## 2. Concurrency

`concurrency` controla cuántas ejecuciones del mismo workflow corren simultáneamente.

### Problema que resuelve

```
Sin concurrency:
Push 1 → Workflow A inicia
Push 2 → Workflow B inicia (A todavía ejecuta)
Push 3 → Workflow C inicia (A y B todavía ejecutan)
RESULTADO: 3 deploys simultáneos → conflictos

Con concurrency:
Push 1 → Workflow A inicia
Push 2 → Workflow B queda en cola (A ejecuta)
Push 3 → Workflow C cancela B y queda en cola (A ejecuta)
RESULTADO: Solo el último deploy relevante se ejecuta
```

### Sintaxis

```yaml
# Nivel workflow
concurrency:
  group: ${{ github.workflow }}-${{ github.ref }}  # ← ID del grupo
  cancel-in-progress: true                          # ← Cancela los anteriores

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - run: echo "deploy"
```

### Grupos comunes

```yaml
# Un grupo por rama → cancelar deploys anteriores de la misma rama
concurrency:
  group: deploy-${{ github.ref }}
  cancel-in-progress: true

# Un grupo por PR → cancelar CI anterior del mismo PR
concurrency:
  group: ci-${{ github.event.pull_request.number || github.ref }}
  cancel-in-progress: true

# Sin cancelar (solo serializar) → útil para producción
concurrency:
  group: production-deploy
  cancel-in-progress: false   # ← Espera en cola pero no cancela

# Nivel job (más granular)
jobs:
  deploy:
    concurrency:
      group: deploy-${{ github.ref }}
      cancel-in-progress: true
    runs-on: ubuntu-latest
```

### Comportamiento de cancel-in-progress

```yaml
# cancel-in-progress: true
# Workflow nuevo → CANCELA el que está en progreso
# Útil para: feature branches, PRs (siempre quieres el último)

# cancel-in-progress: false  
# Workflow nuevo → ESPERA en cola a que termine el anterior
# Útil para: deploy a producción (no queremos saltarnos versiones)
```

---

## 3. Permissions

Controla qué puede hacer el `GITHUB_TOKEN` durante la ejecución.

### Principio de mínimo privilegio

```yaml
# Por defecto GitHub da permisos amplios
# Best practice: reducir al mínimo necesario

permissions:
  contents: read      # Solo leer código
  issues: write       # Puede escribir issues
  pull-requests: write # Puede comentar en PRs
```

### Todos los scopes disponibles

| Scope | Qué controla |
|---|---|
| `actions` | Workflows y artifacts |
| `checks` | Check runs y suites |
| `contents` | Contenido del repositorio (código, releases, tags) |
| `deployments` | Deployments |
| `id-token` | Token OIDC (para autenticación sin secretos) |
| `issues` | Issues |
| `discussions` | Discusiones |
| `packages` | GitHub Packages |
| `pages` | GitHub Pages |
| `pull-requests` | PRs y comentarios |
| `repository-projects` | Proyectos del repositorio |
| `security-events` | Alertas de seguridad (Code Scanning) |
| `statuses` | Estados de commits |

### Valores posibles por scope

- `read` — Solo lectura
- `write` — Lectura y escritura
- `none` — Sin acceso

### Niveles de permissions

```yaml
# Nivel workflow (aplica a todos los jobs)
permissions:
  contents: read
  issues: write

jobs:
  job1:
    runs-on: ubuntu-latest
    # Hereda permissions del workflow
    steps:
      - run: echo "usa permisos del workflow"
  
  job2:
    runs-on: ubuntu-latest
    # Sobreescribe para este job específico
    permissions:
      contents: write       # Más permiso que el workflow
      issues: none          # Menos permiso que el workflow
    steps:
      - run: echo "usa permisos del job"
```

### Permissions por tipo de evento

⚠️ El GITHUB_TOKEN tiene permisos diferentes según el evento que disparó el workflow:

| Evento | `contents` | `pull-requests` | Motivo |
|---|---|---|---|
| `push` | `write` | — | Puede crear commits |
| `pull_request` (fork) | `read` | `read` | Protección contra código malicioso |
| `pull_request` (mismo repo) | `write` | `write` | Confiable |
| `pull_request_target` | `write` | `write` | ⚠️ Cuidado: ejecuta en contexto base |
| `workflow_dispatch` | `write` | `write` | Manual, confiable |

### Ejemplo: Workflow que comenta en PR

```yaml
name: Comentar en PR

on:
  pull_request:

permissions:
  pull-requests: write    # ← Necesario para comentar
  contents: read          # ← Solo leer código

jobs:
  comment:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Comentar en PR
        uses: actions/github-script@v7
        with:
          script: |
            github.rest.issues.createComment({
              issue_number: context.issue.number,
              owner: context.repo.owner,
              repo: context.repo.repo,
              body: '✅ Tests pasaron correctamente'
            })
```

---

## 4. Matrix: Estrategias Avanzadas

La matriz ejecuta el mismo job múltiples veces con diferentes configuraciones.

### Sintaxis básica

```yaml
jobs:
  test:
    runs-on: ubuntu-latest
    strategy:
      matrix:
        node-version: ['18', '20', '22']   # ← 3 ejecuciones
    
    steps:
      - uses: actions/setup-node@v4
        with:
          node-version: ${{ matrix.node-version }}
      - run: npm test
```

### Matrix multidimensional

```yaml
strategy:
  matrix:
    os: [ubuntu-latest, windows-latest, macos-latest]
    node: ['18', '20']
    # Genera 6 combinaciones:
    # ubuntu+18, ubuntu+20, windows+18, windows+20, macos+18, macos+20

jobs:
  test:
    runs-on: ${{ matrix.os }}   # ← Usa el OS de la matrix
    steps:
      - uses: actions/setup-node@v4
        with:
          node-version: ${{ matrix.node }}
```

### include: Añadir combinaciones o propiedades

```yaml
strategy:
  matrix:
    os: [ubuntu-latest, windows-latest]
    node: ['18', '20']
    include:
      # Añadir una combinación extra que no está en la matrix
      - os: macos-latest
        node: '20'
      
      # Añadir propiedades a una combinación existente
      - os: ubuntu-latest
        node: '20'
        experimental: true    # ← Solo aplica a ubuntu+20
        extra-flag: '--verbose'
```

### exclude: Eliminar combinaciones específicas

```yaml
strategy:
  matrix:
    os: [ubuntu-latest, windows-latest, macos-latest]
    node: ['16', '18', '20']
    exclude:
      # No ejecutar Node 16 en macOS (demasiado lento)
      - os: macos-latest
        node: '16'
      # No ejecutar Node 16 en Windows
      - os: windows-latest
        node: '16'
    # Resultado: 7 combinaciones en vez de 9
```

### fail-fast: Controlar fallo en matrix

```yaml
strategy:
  fail-fast: true    # DEFAULT: Si una combinación falla, cancela las demás
  # fail-fast: false → Continúa todas las combinaciones aunque falle alguna
  matrix:
    node: ['18', '20', '22']
```

### max-parallel: Límite de ejecuciones simultáneas

```yaml
strategy:
  max-parallel: 2    # ← Solo 2 combinaciones corren a la vez
  matrix:
    node: ['18', '20', '22', '24']
    # Sin max-parallel: las 4 ejecutan en paralelo
    # Con max-parallel: 2: 18 y 20 primero, luego 22 y 24
```

### Matrix dinámica (generada en tiempo de ejecución)

```yaml
jobs:
  # Job 1: genera la matrix
  define-matrix:
    runs-on: ubuntu-latest
    outputs:
      matrix: ${{ steps.set-matrix.outputs.matrix }}
    steps:
      - id: set-matrix
        run: |
          # Genera matrix basada en archivos, API, condiciones...
          if [ "${{ github.ref }}" == "refs/heads/main" ]; then
            # En main: todos los entornos
            MATRIX='{"environment":["dev","staging","production"]}'
          else
            # En otras ramas: solo dev
            MATRIX='{"environment":["dev"]}'
          fi
          echo "matrix=$MATRIX" >> $GITHUB_OUTPUT
  
  # Job 2: usa la matrix dinámica
  deploy:
    needs: define-matrix
    runs-on: ubuntu-latest
    strategy:
      matrix: ${{ fromJSON(needs.define-matrix.outputs.matrix) }}
    steps:
      - run: echo "Desplegando en ${{ matrix.environment }}"
```

### fromJSON() para arrays dinámicos

```yaml
- id: set-versions
  run: |
    # Obtener versiones activas de una API o archivo
    VERSIONS=$(curl -s https://api.example.com/versions | jq -c '[.[].version]')
    echo "versions=$VERSIONS" >> $GITHUB_OUTPUT

# En otro job:
strategy:
  matrix:
    version: ${{ fromJSON(needs.prev-job.outputs.versions) }}
```

---

## 5. Containers y Services

### Jobs en contenedores

Un **contenedor Docker** es un entorno de ejecución aislado y reproducible que empaqueta una aplicación junto con todas sus dependencias (sistema operativo, bibliotecas, herramientas). En GitHub Actions, en lugar de ejecutar los steps directamente sobre el runner, puedes indicarle al job que corra dentro de un contenedor.

El atributo `image:` indica qué imagen Docker usar. Una **imagen** es una plantilla que define el sistema de archivos y el entorno del contenedor. El valor proviene de un **registry de imágenes**: por defecto Docker Hub (el registro público de Docker). Por ejemplo, `node:20-alpine` significa: imagen oficial de Node.js, versión 20, variante Alpine Linux (ligera).

```
Formato: nombre:tag
  node:20-alpine       → Docker Hub, imagen "node", tag "20-alpine"
  postgres:15          → Docker Hub, imagen "postgres", tag "15"
  ghcr.io/org/img:v1  → GitHub Container Registry (registry alternativo)
```

En lugar de ejecutar en el sistema del runner directamente, el job corre dentro de un contenedor Docker:

```yaml
jobs:
  test:
    runs-on: ubuntu-latest
    container:                    # ← Todo el job corre en este container
      image: node:20-alpine
      env:
        NODE_ENV: test
      options: --cpus 2           # ← Opciones de docker run
    
    steps:
      - uses: actions/checkout@v4
      - run: node --version       # ← Ejecuta dentro del container node:20-alpine
      - run: npm test
```

**¿Cuándo usar container en jobs?**
- Necesitas una versión específica de una herramienta
- Quieres reproducibilidad exacta del entorno
- Usas herramientas que no están en ubuntu-latest

### Services: Dependencias externas (DB, Redis, etc.)

**Services** son contenedores Docker que se arrancan automáticamente **antes** de que empiecen los steps del job y se eliminan cuando el job termina. Sirven para levantar bases de datos, caches u otros procesos externos que tus tests necesitan, sin tener que instalarlos manualmente en el runner.

Cada servicio tiene un **nombre** (la clave bajo `services:`), que actúa como hostname de red para conectarse a él. El atributo `image:` funciona igual que en `container:`: nombre de imagen Docker Hub u otro registry.

Las **`options`** de health check son fundamentales: GitHub Actions esperará a que el contenedor esté *listo* (no solo *arrancado*) antes de ejecutar el primer step. Sin health check el job podría intentar conectarse a la DB antes de que esté lista y fallar.

Los services corren como contenedores Docker **auxiliares** accesibles desde el job:

```yaml
jobs:
  test-with-db:
    runs-on: ubuntu-latest
    
    services:
      postgres:                           # ← Nombre del servicio (hostname)
        image: postgres:15
        env:
          POSTGRES_DB: testdb
          POSTGRES_USER: testuser
          POSTGRES_PASSWORD: testpass
        ports:
          - 5432:5432                     # ← host:container
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
      
      redis:                              # ← Otro servicio
        image: redis:7
        ports:
          - 6379:6379
        options: --health-cmd "redis-cli ping" --health-interval 10s --health-timeout 5s --health-retries 5
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Run tests
        env:
          DATABASE_URL: postgresql://testuser:testpass@localhost:5432/testdb
          REDIS_URL: redis://localhost:6379
        run: npm test
```

> 💡 **Hostname del servicio**: Si el job NO usa container, el hostname es `localhost`. Si el job SÍ usa container, el hostname es el nombre del servicio (`postgres`, `redis`).

### Container + Services juntos

```yaml
jobs:
  integration-test:
    runs-on: ubuntu-latest
    container:
      image: python:3.12         # Job corre en Python
    services:
      postgres:
        image: postgres:15
        env:
          POSTGRES_PASSWORD: pass
        # hostname accesible como "postgres" (no localhost)
    steps:
      - run: pip install psycopg2
      - run: python -c "import psycopg2; psycopg2.connect(host='postgres', password='pass')"
```

---

## 6. Conditions (if:) en Todos los Niveles

### En jobs

```yaml
jobs:
  deploy:
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'   # ← Job solo ejecuta en main
    steps:
      - run: echo "deploying"
  
  notify-failure:
    runs-on: ubuntu-latest
    if: failure()                          # ← Solo si algún job anterior falló
    steps:
      - run: echo "algo falló"
```

### En steps

```yaml
steps:
  - name: Build
    id: build
    run: npm run build
  
  - name: Solo en Linux
    if: runner.os == 'Linux'
    run: echo "Linux específico"
  
  - name: Solo si build falló
    if: failure() && steps.build.outcome == 'failure'
    run: echo "El build falló"
  
  - name: Siempre ejecutar (aunque pasos anteriores fallen)
    if: always()
    run: echo "limpieza siempre"
```

### Funciones de estado

| Función | Cuándo es true |
|---|---|
| `success()` | Todos los pasos/jobs anteriores tuvieron éxito (DEFAULT) |
| `failure()` | Al menos un paso/job anterior falló |
| `cancelled()` | El workflow fue cancelado |
| `always()` | Siempre (sin importar resultado anterior) |

### Combinaciones avanzadas

```yaml
# Ejecutar solo si es push a main O si el workflow fue disparado manualmente
if: (github.event_name == 'push' && github.ref == 'refs/heads/main') || github.event_name == 'workflow_dispatch'

# Ejecutar si falló Y es la rama principal (notificación crítica)
if: failure() && github.ref == 'refs/heads/main'

# NO ejecutar si tiene la label skip-ci
if: "!contains(github.event.pull_request.labels.*.name, 'skip-ci')"

# Ejecutar siempre EXCEPTO si fue cancelado
if: "!cancelled()"
```

> ⚠️ **`if` sin `${{ }}`**: En `if:`, las expresiones de GitHub Actions se evalúan automáticamente SIN necesidad de `${{ }}`. Las llaves son opcionales pero válidas.
> ```yaml
> if: github.ref == 'refs/heads/main'           # ✅ Sin llaves (preferido)
> if: ${{ github.ref == 'refs/heads/main' }}     # ✅ Con llaves (también válido)
> ```
>
> **Excepción**: Si el `if` empieza con `!` (NOT), DEBES usar comillas:
> ```yaml
> if: "!cancelled()"     # ✅ Entre comillas
> if: !cancelled()       # ❌ YAML interpreta ! como inicio de tag YAML
> ```

---

## 7. Timeout y Continue-on-error

### timeout-minutes

```yaml
# Nivel job: máximo 30 minutos
jobs:
  long-job:
    runs-on: ubuntu-latest
    timeout-minutes: 30       # ← El job falla si supera 30 min
    steps:
      - run: ./long-process.sh

# Nivel step
steps:
  - name: Paso con timeout
    timeout-minutes: 5        # ← El step falla si supera 5 min
    run: sleep 400             # Este paso fallará a los 5 min
```

### continue-on-error

```yaml
steps:
  - name: Paso experimental
    continue-on-error: true   # ← El workflow continúa aunque falle
    run: ./experimental.sh
  
  - name: Este sigue aunque el anterior falle
    run: echo "continúo"

# También en jobs
jobs:
  experimental-job:
    continue-on-error: true
    runs-on: ubuntu-latest
    steps:
      - run: exit 1
  
  final-job:
    needs: experimental-job
    runs-on: ubuntu-latest
    # Este job se ejecuta porque experimental-job tiene continue-on-error
    steps:
      - run: echo "el experimental falló pero continúo"
```

### outcome vs conclusion en steps

```yaml
steps:
  - name: Paso que puede fallar
    id: risky
    continue-on-error: true
    run: exit 1
  
  - name: Verificar resultado
    run: |
      echo "outcome: ${{ steps.risky.outcome }}"       # failure (el real)
      echo "conclusion: ${{ steps.risky.conclusion }}" # success (por continue-on-error)
```

| Propiedad | Valor cuando falla con `continue-on-error: true` |
|---|---|
| `steps.id.outcome` | `failure` (resultado real) |
| `steps.id.conclusion` | `success` (el workflow lo considera exitoso) |

---

## 8. Job Dependencies (needs:)

### Cadena lineal

```yaml
jobs:
  lint:
    runs-on: ubuntu-latest
    steps: [...]
  
  test:
    needs: lint       # ← Espera a lint
    runs-on: ubuntu-latest
    steps: [...]
  
  build:
    needs: test       # ← Espera a test (que esperó a lint)
    runs-on: ubuntu-latest
    steps: [...]
  
  deploy:
    needs: build      # ← Al final del todo
    runs-on: ubuntu-latest
    steps: [...]
```

### Múltiples dependencias (fan-in)

```yaml
jobs:
  test-unit:
    runs-on: ubuntu-latest
    steps: [...]
  
  test-integration:
    runs-on: ubuntu-latest
    steps: [...]
  
  test-e2e:
    runs-on: ubuntu-latest
    steps: [...]
  
  deploy:
    needs: [test-unit, test-integration, test-e2e]   # ← Espera a los 3
    runs-on: ubuntu-latest
    steps: [...]
```

### Condicional basado en resultado de needs

```yaml
jobs:
  build:
    runs-on: ubuntu-latest
    steps: [...]
  
  deploy-success:
    needs: build
    if: needs.build.result == 'success'    # ← Solo si build tuvo éxito
    runs-on: ubuntu-latest
    steps: [...]
  
  notify-failure:
    needs: build
    if: needs.build.result == 'failure'    # ← Solo si build falló
    runs-on: ubuntu-latest
    steps: [...]
  
  cleanup:
    needs: [build, deploy-success]
    if: always()                           # ← Siempre, independientemente
    runs-on: ubuntu-latest
    steps: [...]
```

### Valores de `needs.<job>.result`

| Valor | Significado |
|---|---|
| `success` | El job completó exitosamente |
| `failure` | El job falló |
| `cancelled` | El job fue cancelado |
| `skipped` | El job fue omitido (su `if:` fue false) |

---

## 9. Preguntas de Examen

**P: ¿Cuál es la diferencia entre `$GITHUB_OUTPUT` y `$GITHUB_ENV`?**
→ `GITHUB_OUTPUT` crea outputs accesibles con `steps.id.outputs.key` entre steps. `GITHUB_ENV` crea variables de entorno bash accesibles con `$KEY` en steps siguientes. Solo `GITHUB_OUTPUT` permite compartir datos entre jobs (vía `outputs:` del job).

**P: ¿Cómo cancelar ejecuciones previas del mismo workflow en la misma rama?**
→ Usar `concurrency: { group: "${{ github.workflow }}-${{ github.ref }}", cancel-in-progress: true }`

**P: ¿Qué scope de permissions necesita un workflow para comentar en un PR?**
→ `pull-requests: write`

**P: ¿Qué diferencia hay entre `outcome` y `conclusion` en un step?**
→ `outcome` es el resultado real (puede ser `failure`). `conclusion` es el resultado considerado por el workflow (con `continue-on-error: true`, puede ser `success` aunque `outcome` sea `failure`).

**P: ¿Qué hace `fail-fast: false` en una matrix?**
→ Permite que todas las combinaciones de la matrix continúen ejecutándose aunque una falle. Por defecto (`true`), si una combinación falla, las demás se cancelan.

**P: ¿Cómo crear una matrix dinámica basada en condiciones del evento?**
→ Usar un job previo que calcule la matrix y la guarde en `GITHUB_OUTPUT` como JSON, luego referenciarla con `${{ fromJSON(needs.prev.outputs.matrix) }}`.

**P: ¿Qué hostname se usa para conectar a un service cuando el job NO usa container?**
→ `localhost`

**P: ¿Cuándo hay que poner comillas en un `if:`?**
→ Cuando la expresión empieza con `!` (NOT lógico), porque YAML lo interpreta como tag de tipo. Ejemplo: `if: "!cancelled()"`

