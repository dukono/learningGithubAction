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
9. [run-name: Nombrar Ejecuciones Individuales](#9-run-name-nombrar-ejecuciones-individuales)
10. [defaults: Configuración por Defecto](#10-defaults-configuración-por-defecto)
11. [environment.url en Jobs](#11-environmenturl-en-jobs)
12. [YAML Anchors y Aliases](#12-yaml-anchors-y-aliases)
13. [Timeouts: Límites Importantes](#13-timeouts-límites-importantes)
14. [Preguntas de Examen](#14-preguntas-de-examen)

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

### Outputs con valores multilínea

El formato `key=value` en una sola línea no funciona si el valor contiene saltos de línea. Para valores multilínea se usa un delimitador:

```yaml
- name: Output multilínea
  id: datos
  run: |
    # Formato heredoc: EOF puede ser cualquier string único
    echo "json<<EOF" >> $GITHUB_OUTPUT
    echo '{"status":"ok","count":42}' >> $GITHUB_OUTPUT
    echo "EOF" >> $GITHUB_OUTPUT

- name: Usar el output
  run: echo '${{ steps.datos.outputs.json }}'
  # Imprime: {"status":"ok","count":42}
```

### Límites de outputs

- **Tamaño máximo por output**: 1 MB
- **Número máximo de outputs por step/job**: sin límite documentado oficial, pero outputs muy grandes degradan el rendimiento
- **Outputs de jobs de matrix**: cada combinación de matrix expone sus propios outputs; no se pueden agregar automáticamente — hay que usar artifacts para consolidar resultados de múltiples combinaciones

```yaml
# ⚠️ Este patrón NO funciona para agregar outputs de matrix
jobs:
  test:
    strategy:
      matrix:
        env: [dev, staging, prod]
    outputs:
      # Solo el último job de la matrix que termine escribe aquí
      # → resultado no determinista si varios escriben la misma key
      result: ${{ steps.r.outputs.result }}
```

```yaml
# ✅ Para agregar resultados de matrix → usar artifacts
jobs:
  test:
    strategy:
      matrix:
        env: [dev, staging, prod]
    steps:
      - id: r
        run: echo "result=ok" >> $GITHUB_OUTPUT
      - uses: actions/upload-artifact@v4
        with:
          name: result-${{ matrix.env }}
          path: result.txt

  aggregate:
    needs: test
    steps:
      - uses: actions/download-artifact@v4
        with:
          pattern: result-*     # ← Descarga todos los artifacts del pattern
          merge-multiple: true  # ← Los fusiona en un solo directorio
```

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

### Patrón avanzado: cancelar CI pero no deploy

Si tu workflow tiene jobs de CI (cancelables) y de deploy (no cancelables), usa `concurrency` a nivel de **job**, no de workflow:

```yaml
jobs:
  test:
    runs-on: ubuntu-latest
    concurrency:
      group: ci-${{ github.ref }}
      cancel-in-progress: true    # ← El test anterior se cancela
    steps:
      - run: npm test

  deploy:
    needs: test
    runs-on: ubuntu-latest
    concurrency:
      group: deploy-${{ github.ref }}
      cancel-in-progress: false   # ← El deploy anterior NO se cancela
    steps:
      - run: ./deploy.sh
```

### Trampa: concurrency y el `github.head_ref`

En un `pull_request`, `github.ref` es `refs/pull/N/merge` — única por PR. Pero si el grupo usa `github.head_ref` (nombre de la rama), dos PRs de la misma rama compartirían grupo de concurrencia. Usar `github.ref` es más seguro:

```yaml
concurrency:
  group: ${{ github.workflow }}-${{ github.ref }}   # ✅ Único por PR
  # group: ${{ github.workflow }}-${{ github.head_ref }}  # ⚠️ Puede colisionar
  cancel-in-progress: true
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

### Matrix solo con `include` (sin variables base)

`include` puede usarse **sin** definir variables base cuando cada combinación es completamente diferente y no tiene sentido el producto cartesiano:

```yaml
strategy:
  matrix:
    include:
      - name: "Test Chrome"
        browser: chrome
        os: ubuntu-latest
      - name: "Test Firefox"
        browser: firefox
        os: ubuntu-latest
      - name: "Test Safari"
        browser: safari
        os: macos-latest
```

### Nombre personalizado del job en la UI con matrix

Por defecto el job aparece como `test (18)`, `test (20)`, etc. Puedes personalizar el nombre:

```yaml
jobs:
  test:
    name: "Tests en Node ${{ matrix.node }} / ${{ matrix.os }}"  # ← Nombre en UI
    strategy:
      matrix:
        node: ['18', '20']
        os: [ubuntu-latest, windows-latest]
    runs-on: ${{ matrix.os }}
```

### `continue-on-error` por combinación en matrix

Puedes marcar combinaciones específicas como experimentales que no bloqueen el resultado global:

```yaml
strategy:
  matrix:
    node: ['18', '20', '22']
    include:
      - node: '22'
        experimental: true   # ← Propiedad extra
jobs:
  test:
    continue-on-error: ${{ matrix.experimental == true }}  # ← Solo falla si no es experimental
    strategy:
      matrix: ...
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

### Container con registry privado

Si la imagen no es pública (Docker Hub privado, Artifactory, GHCR privado), hay que autenticarse **antes** de que GitHub intente hacer pull. Esto se hace con `credentials:` dentro de `container:`:

```yaml
jobs:
  test:
    runs-on: ubuntu-latest
    container:
      image: ghcr.io/mi-org/mi-imagen:latest        # ← Imagen privada
      credentials:
        username: ${{ github.actor }}
        password: ${{ secrets.GITHUB_TOKEN }}        # ← Para GHCR
      # Para Artifactory u otro registry privado:
      # credentials:
      #   username: ${{ secrets.ARTIFACTORY_USER }}
      #   password: ${{ secrets.ARTIFACTORY_TOKEN }}
    steps:
      - run: echo "corriendo en imagen privada"
```

> ⚠️ Sin `credentials:`, el job falla con `pull access denied` si la imagen es privada. El `GITHUB_TOKEN` solo sirve para `ghcr.io` — para otros registries necesitas secrets propios.

### Container con volumes

Puedes montar directorios del runner dentro del container:

```yaml
jobs:
  test:
    runs-on: ubuntu-latest
    container:
      image: node:20-alpine
      volumes:
        - /var/run/docker.sock:/var/run/docker.sock  # ← Docker-in-Docker
        - ${{ github.workspace }}:/app               # ← Montar el workspace
      options: --user root
    steps:
      - uses: actions/checkout@v4
      - run: npm test
```

### Alternativa: docker compose directamente en steps

Cuando necesitas orquestar múltiples contenedores con configuración compleja (redes personalizadas, depends_on, tu propio `docker-compose.yml`), usa `docker compose` como comando en steps en lugar de `services:`:

```yaml
jobs:
  integration-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Login al registry privado (si las imágenes son privadas)
        uses: docker/login-action@v3
        with:
          registry: ghcr.io                          # o tu registry
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Arrancar servicios
        run: docker compose up -d

      - name: Esperar a que estén listos
        run: |
          timeout 120 bash -c 'until docker compose ps | grep -q "healthy"; do sleep 3; done'
          # O para un servicio específico:
          # docker compose exec db pg_isready

      - name: Ejecutar tests
        run: npm test
        env:
          DB_HOST: localhost
          DB_PORT: 5432

      - name: Logs en caso de fallo
        if: failure()
        run: docker compose logs

      - name: Limpieza
        if: always()
        run: docker compose down -v
```

| Usar `services:` | Usar `docker compose` |
|---|---|
| Imágenes públicas simples (postgres, redis) | Tu propio `docker-compose.yml` ya existe |
| No tienes `docker-compose.yml` | Imágenes de registry privado con credenciales |
| Quieres health check automático integrado | Necesitas `depends_on`, redes, configs complejas |

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

> ⚠️ **`ports:` solo es necesario sin `container:`**: Cuando el job usa `container:`, los servicios están en la misma red Docker y son accesibles directamente por nombre — no hace falta mapear puertos al host. Los `ports:` solo son necesarios cuando el job corre directamente en el runner (sin `container:`), para exponer el puerto del contenedor al host.

### Services con registry privado

Al igual que en `container:`, si la imagen del service es privada se necesita `credentials:`:

```yaml
services:
  mi-app:
    image: ghcr.io/mi-org/mi-servicio:latest
    credentials:
      username: ${{ github.actor }}
      password: ${{ secrets.GITHUB_TOKEN }}
    ports:
      - 8080:8080
```

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

> 💡 **Timeout por defecto**: Si no especificas `timeout-minutes`, el límite por defecto de GitHub es **6 horas** por job. Para workflows de runners self-hosted el límite sube a **35 días**. Siempre es buena práctica definirlo explícitamente para detectar bucles infinitos o cuelgues.

### `continue-on-error` a nivel de job y su efecto en `needs`

Cuando un job tiene `continue-on-error: true` y falla, los jobs dependientes **sí se ejecutan**, pero `needs.job.result` devuelve `failure` — no `success`. Esto es distinto de `skipped`:

```yaml
jobs:
  flaky-job:
    continue-on-error: true
    runs-on: ubuntu-latest
    steps:
      - run: exit 1   # Falla, pero el workflow continúa

  next-job:
    needs: flaky-job
    if: always()
    runs-on: ubuntu-latest
    steps:
      - run: |
          echo "${{ needs.flaky-job.result }}"
          # Imprime "failure" — NO "success"
          # continue-on-error no cambia el result del job, solo permite continuar
```

```
continue-on-error: true en un job →
  ✅ Los jobs dependientes SE ejecutan
  ⚠️ needs.job.result sigue siendo "failure"
  ✅ El workflow completo se marca como exitoso
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

### Trampa: `needs` + `if: always()` y el resultado `skipped`

Cuando un job tiene `if: always()` y depende de otro que fue **skipped**, el resultado de `needs.job.result` es `skipped` — no `success`. Esto provoca errores silenciosos si no se tiene en cuenta:

```yaml
jobs:
  build:
    if: github.ref == 'refs/heads/main'   # ← Solo en main
    runs-on: ubuntu-latest
    steps:
      - run: npm run build

  notify:
    needs: build
    if: always()                           # ← Siempre ejecutar
    runs-on: ubuntu-latest
    steps:
      - run: |
          echo "Resultado: ${{ needs.build.result }}"
          # En una rama feature: imprime "skipped", no "success" ni "failure"
          # ← Hay que contemplar "skipped" en la lógica
```

```yaml
# ✅ Patrón correcto: contemplar los 3 casos posibles
notify:
  needs: build
  if: always()
  steps:
    - if: needs.build.result == 'success'
      run: echo "✅ Build OK"
    - if: needs.build.result == 'failure'
      run: echo "❌ Build falló"
    - if: needs.build.result == 'skipped'
      run: echo "⏭️ Build no ejecutado (rama no es main)"
```

### `needs` transitivo: acceder a outputs de jobs no directos

Un job solo puede leer outputs de los jobs que declara explícitamente en su `needs:`. Si `C` depende de `B` que depende de `A`, `C` **no puede** leer `needs.A.outputs` a menos que también declare `needs: [A, B]`:

```yaml
jobs:
  A:
    outputs:
      version: ${{ steps.v.outputs.version }}
    ...

  B:
    needs: A
    ...

  C:
    needs: [A, B]           # ← Debe declarar A explícitamente
    steps:
      - run: echo "${{ needs.A.outputs.version }}"   # ✅ Funciona
      # Si solo fuera needs: B → needs.A.outputs.version estaría vacío ❌
```

### Fan-out: un job dispara múltiples en paralelo

```yaml
jobs:
  build:                          # ← 1 job de entrada
    runs-on: ubuntu-latest
    outputs:
      artifact: ${{ steps.b.outputs.name }}
    steps:
      - id: b
        run: echo "name=app.zip" >> $GITHUB_OUTPUT

  deploy-eu:                      # ← Paralelo
    needs: build
    runs-on: ubuntu-latest
    steps:
      - run: echo "Deploy EU con ${{ needs.build.outputs.artifact }}"

  deploy-us:                      # ← Paralelo
    needs: build
    runs-on: ubuntu-latest
    steps:
      - run: echo "Deploy US con ${{ needs.build.outputs.artifact }}"

  deploy-ap:                      # ← Paralelo
    needs: build
    runs-on: ubuntu-latest
    steps:
      - run: echo "Deploy AP con ${{ needs.build.outputs.artifact }}"

  smoke-tests:                    # ← Espera a los 3 deploys
    needs: [deploy-eu, deploy-us, deploy-ap]
    runs-on: ubuntu-latest
    steps:
      - run: echo "Todos los deploys listos"
```

---

## 9. run-name: Nombrar Ejecuciones Individuales

El campo `run-name` a nivel de workflow personaliza el nombre que aparece en la pestaña **Actions** de GitHub para cada ejecución individual. Es diferente de `name`, que identifica al workflow en general.

- Se define a nivel de workflow, junto a `name` y `on`
- Soporta expresiones con contextos como `${{ github.actor }}`, `${{ github.ref_name }}`, etc.
- Por defecto (sin `run-name`), GitHub muestra el mensaje del commit o el nombre del trigger que disparó el workflow

```yaml
# Ejemplo 1: Deploy desde una rama, realizado por un usuario
name: Deploy Workflow
run-name: Deploy ${{ github.ref_name }} by ${{ github.actor }}

on:
  push:
    branches: [main, staging]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - run: echo "deploying"
```

```yaml
# Ejemplo 2: Nombre descriptivo para un PR
name: PR Checks
run-name: "PR #${{ github.event.pull_request.number }}: ${{ github.event.pull_request.title }}"

on:
  pull_request:
```

```yaml
# Ejemplo 3: Con workflow_dispatch, incluyendo el input seleccionado
name: Release
run-name: "Release ${{ inputs.version }} to ${{ inputs.environment }}"

on:
  workflow_dispatch:
    inputs:
      version:
        required: true
        type: string
      environment:
        required: true
        type: choice
        options: [staging, production]
```

> ⚠️ `run-name` es evaluado como expresión en tiempo de ejecución — puede incluir cualquier contexto disponible al inicio del workflow. Si la expresión falla o produce una cadena vacía, GitHub usa el nombre por defecto.

---

## 10. defaults: Configuración por Defecto

`defaults.run` evita repetir `shell:` y `working-directory:` en cada step. Se puede definir a nivel de workflow (aplica a todos los jobs) o a nivel de job (sobreescribe el del workflow para ese job).

### Shells disponibles

| Shell | Descripción |
|---|---|
| `bash` | Bash (predeterminado en Linux/macOS) |
| `sh` | POSIX sh |
| `pwsh` | PowerShell Core (multiplataforma) |
| `python` | Ejecuta el bloque como script Python |
| `cmd` | Command Prompt de Windows |
| `powershell` | Windows PowerShell (legacy) |

### Ejemplo a nivel de workflow

```yaml
name: Python Build

defaults:
  run:
    shell: bash
    working-directory: ./src   # Todos los steps empiezan en ./src

on: [push]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: python --version       # Ejecuta desde ./src con bash
      - run: pip install -r requirements.txt  # Idem
```

### Ejemplo a nivel de job (sobreescribe el del workflow)

```yaml
defaults:
  run:
    shell: bash
    working-directory: ./

jobs:
  build-linux:
    runs-on: ubuntu-latest
    # Hereda defaults del workflow

  build-windows:
    runs-on: windows-latest
    defaults:
      run:
        shell: pwsh               # Sobreescribe shell para este job
        working-directory: ./src  # Sobreescribe working-directory
    steps:
      - run: Get-Location         # Ejecuta con PowerShell Core desde ./src
```

> `defaults` no aplica a steps de tipo `uses:` (actions). Solo afecta a steps `run:`.

---

## 11. environment.url en Jobs

El campo `environment` en un job puede tener tanto `name` como `url`. La URL aparece como enlace clickable en la pestaña **Deployments** de GitHub, facilitando el acceso directo al entorno desplegado.

```yaml
jobs:
  deploy-production:
    runs-on: ubuntu-latest
    environment:
      name: production
      url: https://my-app.com    # ← Aparece como link en la UI de deployments
    steps:
      - run: ./deploy.sh
```

### Con URL dinámica generada en el deploy

```yaml
jobs:
  deploy-preview:
    runs-on: ubuntu-latest
    outputs:
      preview-url: ${{ steps.deploy.outputs.url }}
    environment:
      name: preview
      url: ${{ steps.deploy.outputs.url }}   # ← URL calculada en tiempo real
    steps:
      - name: Deploy
        id: deploy
        run: |
          URL=$(./deploy-preview.sh)
          echo "url=$URL" >> $GITHUB_OUTPUT
```

### Diferencia entre `name` y `url` en `environment`

| Campo | Efecto |
|---|---|
| `environment.name` | Activa las protection rules del entorno (required reviewers, wait timers, etc.) |
| `environment.url` | Solo visual: aparece como enlace en la UI de deployments, no afecta la ejecución |

---

## 12. YAML Anchors y Aliases

YAML nativo soporta **anchors** (`&nombre`) y **aliases** (`*nombre`) para reutilizar bloques sin repetir código. No son una característica de GitHub Actions sino de YAML en sí, por lo que funcionan en cualquier archivo `.yml`.

### Sintaxis básica

```yaml
# &nombre define el anchor (bloque reutilizable)
# *nombre referencia el anchor (alias)
# <<: fusiona el bloque en el contexto actual (merge key)
```

### Ejemplo: steps repetidos en múltiples jobs

```yaml
# Definir el anchor al principio (puede ser en cualquier lugar del YAML)
x-checkout-steps: &checkout-steps
  - uses: actions/checkout@v4
  - name: Setup Node
    uses: actions/setup-node@v4
    with:
      node-version: '20'
      cache: 'npm'

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - *checkout-steps             # ← Alias: inserta los steps aquí
      - run: npm run build

  test:
    runs-on: ubuntu-latest
    steps:
      - *checkout-steps             # ← Mismo bloque reutilizado
      - run: npm test

  lint:
    runs-on: ubuntu-latest
    steps:
      - *checkout-steps             # ← Idem
      - run: npm run lint
```

### Merge key (`<<:`) para combinar bloques

```yaml
x-base-job: &base-job
  runs-on: ubuntu-latest
  timeout-minutes: 15
  env:
    NODE_ENV: test

jobs:
  unit-tests:
    <<: *base-job                   # ← Fusiona el bloque base
    steps:
      - run: npm test

  integration-tests:
    <<: *base-job                   # ← Fusiona el mismo base
    timeout-minutes: 30             # ← Sobreescribe solo este campo
    steps:
      - run: npm run test:integration
```

### Limitaciones importantes

- Los anchors solo funcionan **dentro del mismo archivo** YAML. No se pueden compartir entre workflows distintos.
- El valor del anchor se evalúa en tiempo de parseo, no en tiempo de ejecución — no puede contener expresiones `${{ }}` que dependan de contexto de ejecución.
- Si el bloque referenciado contiene `id:` de steps, habría colisión si se repite en el mismo job.

### Cuándo preferir composite actions sobre anchors

| Usar YAML anchors | Usar composite actions |
|---|---|
| Repetición dentro del mismo archivo | Reutilización entre múltiples workflows o repositorios |
| Equipos pequeños, archivos simples | Necesitas versionar y publicar la lógica |
| No necesitas lógica condicional compleja | Quieres inputs/outputs parametrizables |
| Refactor rápido sin crear nuevos archivos | Mantenimiento a largo plazo, mejor legibilidad |

---

## 13. Timeouts: Límites Importantes

### Valores máximos en GitHub-hosted runners

| Ámbito | Timeout máximo |
|---|---|
| Por step (`timeout-minutes` en step) | Sin límite impuesto, pero acotado por el del job |
| Por job (`timeout-minutes` en job) | **360 minutos (6 horas)** |
| Por workflow completo | **35 días** |

> En self-hosted runners los límites son diferentes: el timeout por defecto del job sube a 6 horas y el máximo del workflow puede ser de 35 días.

### timeout-minutes a nivel de step

```yaml
steps:
  - name: Operación lenta con límite
    timeout-minutes: 10           # ← El step falla si supera 10 minutos
    run: ./slow-operation.sh

  - name: Este continúa normalmente
    run: echo "siguiente step"
```

### timeout-minutes a nivel de job

```yaml
jobs:
  build:
    runs-on: ubuntu-latest
    timeout-minutes: 30           # ← El job falla si supera 30 minutos
    steps:
      - run: npm run build
```

### Por qué definir timeout siempre

Si no se especifica `timeout-minutes`:
- En **GitHub-hosted**: el job puede ejecutar hasta 6 horas antes de cancelarse automáticamente
- El tiempo de runner se sigue consumiendo (y facturando) durante todo ese tiempo

Definir `timeout-minutes` explícitamente protege contra bucles infinitos, procesos colgados y gastos inesperados de minutos de runner.

### Combinación de timeouts step + job

El timeout del step es independiente del del job. Si el step agota su timeout, el step falla, pero el job puede continuar (con `continue-on-error: true` en el step) o fallar (comportamiento por defecto). El timeout del job es un límite superior absoluto que cancela todo lo que quede pendiente.

```yaml
jobs:
  example:
    runs-on: ubuntu-latest
    timeout-minutes: 20           # ← Límite absoluto del job
    steps:
      - name: Intento con timeout corto
        timeout-minutes: 5        # ← Si supera 5 min, falla este step
        continue-on-error: true   # ← Pero el job continúa
        run: ./maybe-slow.sh

      - name: Siguiente step ejecuta siempre que el job tenga tiempo
        run: echo "continuando"
```

---

## 14. Preguntas de Examen

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

**P: ¿Cómo autenticarse para usar una imagen privada en `container:` o `services:`?**
→ Con `credentials: { username: ..., password: ... }` dentro del bloque `container:` o del service. Para GHCR usar `github.actor` y `secrets.GITHUB_TOKEN`. Para otros registries (Artifactory, etc.) usar secrets propios.

**P: ¿Por qué no hacen falta `ports:` en un service cuando el job usa `container:`?**
→ Porque el job y los services comparten la misma red Docker interna. Los services son accesibles por su nombre (hostname). `ports:` solo es necesario sin `container:`, para exponer el puerto al runner host.

**P: ¿Qué resultado tiene `needs.job.result` cuando ese job fue saltado (`if:` era false)?**
→ `skipped`. Hay que contemplar este caso en jobs con `if: always()` que dependen de jobs condicionales.

**P: ¿Puede el job C leer `needs.A.outputs` si solo declara `needs: B` y B depende de A?**
→ No. C debe declarar explícitamente `needs: [A, B]` para poder acceder a los outputs de A.

**P: ¿Qué ocurre con `needs.job.result` cuando el job tiene `continue-on-error: true` y falla?**
→ El valor sigue siendo `failure`. `continue-on-error` permite que el workflow continúe y se marque como exitoso globalmente, pero no cambia el valor de `result` del job.

**P: ¿Cómo escribir un output multilínea en `$GITHUB_OUTPUT`?**
→ Usando formato heredoc: `echo "key<<EOF" >> $GITHUB_OUTPUT`, luego el valor, luego `echo "EOF" >> $GITHUB_OUTPUT`.

**P: ¿Cuál es el timeout por defecto de un job si no se especifica `timeout-minutes`?**
→ 6 horas en GitHub-hosted runners. 35 días en self-hosted runners.

**P: ¿Puede una matrix usar `include` sin definir variables base?**
→ Sí. `include` sin variables base permite definir combinaciones completamente independientes, útil cuando las combinaciones no forman un producto cartesiano.

**P: ¿Qué diferencia hay entre `name` y `run-name` en un workflow?**
→ `name` es el nombre del workflow (aparece en la lista de workflows de la pestaña Actions). `run-name` es el nombre de cada ejecución individual dentro de ese workflow (aparece en la lista de runs). `run-name` soporta expresiones para mostrar información dinámica como la rama o el actor.

**P: ¿Dónde se puede definir `defaults.run` y qué afecta?**
→ Se puede definir a nivel de workflow y a nivel de job. Solo afecta a steps `run:`, no a steps `uses:`. El del job sobreescribe al del workflow para ese job concreto. Permite configurar `shell` y `working-directory` una sola vez.

**P: ¿Para qué sirve `environment.url` en un job?**
→ Para mostrar un enlace clickable en la UI de deployments de GitHub. Es puramente visual y no afecta la ejecución del job. Puede ser una URL estática o dinámica (usando un output de step). `environment.name` es lo que activa las protection rules del entorno.

**P: ¿Qué es un YAML anchor y cuál es su principal limitación frente a las composite actions?**
→ Un anchor (`&nombre`) define un bloque YAML reutilizable dentro del mismo archivo; un alias (`*nombre`) lo referencia. Su principal limitación es que solo funciona dentro del mismo archivo — no se puede compartir entre workflows distintos ni versionar. Las composite actions son la alternativa para reutilización cross-workflow o cross-repositorio.

**P: ¿Cuál es el timeout máximo por job en GitHub-hosted runners si no se especifica `timeout-minutes`?**
→ 360 minutos (6 horas). El timeout máximo de un workflow completo es de 35 días. Definir `timeout-minutes` explícitamente es una buena práctica para detectar procesos colgados y evitar consumo innecesario de minutos de runner.

**P: ¿Puede `timeout-minutes` definirse tanto a nivel de step como a nivel de job?**
→ Sí. El timeout de step es independiente: si el step lo supera, el step falla (el job puede continuar con `continue-on-error: true`). El timeout de job es un límite absoluto que cancela todo el job y sus steps pendientes si se supera.

