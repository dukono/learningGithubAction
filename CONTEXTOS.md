# GitHub Actions: Contextos y Variables Completas

## ¿Qué es un contexto?

Un **contexto** es un objeto JSON que GitHub inyecta automáticamente en cada ejecución de workflow. Contiene información sobre el evento que lo disparó, el repositorio, el job en curso, el runner, los secrets configurados, etc. Se accede a sus propiedades dentro de expresiones `${{ }}`.

Existen múltiples contextos separados —en lugar de uno único— porque cada uno tiene un **ámbito y disponibilidad distintos**: algunos solo existen dentro de un step concreto (`steps`), otros solo cuando hay una matrix (`matrix`), otros solo en workflows llamados (`inputs`). Esta separación también evita colisiones de nombres entre, por ejemplo, una variable de entorno (`env.VERSION`) y un output de un step (`steps.build.outputs.version`).

```
${{ github.ref }}           → información del evento y repositorio
${{ env.MI_VAR }}           → variables de entorno del workflow
${{ job.status }}           → estado actual del job
${{ steps.id.outputs.key }} → outputs de un step anterior
${{ runner.os }}            → sistema operativo del runner
${{ secrets.TOKEN }}        → secrets configurados
${{ vars.CONFIG }}          → variables de configuración (no secretas)
${{ matrix.version }}       → valor actual de la celda en una matrix
${{ needs.job.outputs.x }}  → outputs de jobs de los que dependemos
${{ inputs.param }}         → inputs de workflow_dispatch o workflow_call
```

---

## Índice
1. [Contexto `github`](#contexto-github)
   - [Eventos Específicos por Trigger](#eventos-específicos)
2. [Contexto `env`](#contexto-env)
3. [Contexto `job`](#contexto-job)
4. [Contexto `steps`](#contexto-steps)
5. [Contexto `runner`](#contexto-runner)
6. [Contexto `secrets`](#contexto-secrets)
7. [Contexto `vars`](#contexto-vars)
8. [Contexto `strategy` y `matrix`](#contexto-strategy-y-matrix)
9. [Contexto `needs`](#contexto-needs)
10. [Contexto `inputs`](#contexto-inputs)
11. [Contexto `jobs` (solo reusable workflows)](#contexto-jobs-solo-reusable-workflows)
12. [Arrays en el Contexto: labels, assignees y similares](#arrays-en-el-contexto-labels-assignees-y-similares)
13. [Funciones de Contexto](#funciones-de-contexto)
14. [Tabla de Referencia Rápida](#tabla-de-referencia-rápida)

---

## Contexto `github`

### Propiedades Generales

| Propiedad | Descripción | Disponible en |
|---|---|---|
| `github.action` | ID del step actual o de la action en ejecución | Siempre |
| `github.action_path` | Path donde está instalada la action actual | Solo en steps de tipo `uses:` |
| `github.action_ref` | Ref de la action (ej: `v4`) | Solo en steps de tipo `uses:` |
| `github.action_repository` | Repositorio de la action (ej: `actions/checkout`) | Solo en steps de tipo `uses:` |
| `github.action_status` | Estado de la action actual | Siempre |
| `github.actor` | Usuario que disparó el workflow | Siempre |
| `github.actor_id` | ID numérico del usuario que disparó el workflow | Siempre |
| `github.api_url` | URL de la API REST de GitHub (`https://api.github.com`) | Siempre |
| `github.base_ref` | Nombre de la rama destino del PR (sin `refs/heads/`) | ⚠️ Solo en `pull_request` |
| `github.env` | Path al archivo `$GITHUB_ENV` en el runner | Siempre |
| `github.event` | Payload completo del evento en formato JSON | Siempre |
| `github.event_name` | Nombre del evento que disparó el workflow (`push`, `pull_request`, etc.) | Siempre |
| `github.event_path` | Path al archivo JSON del payload del evento en el runner | Siempre |
| `github.graphql_url` | URL de la API GraphQL (`https://api.github.com/graphql`) | Siempre |
| `github.head_ref` | Nombre de la rama origen del PR (sin `refs/heads/`) | ⚠️ Solo en `pull_request` |
| `github.job` | ID del job actual (el `id` definido en el YAML) | Siempre |
| `github.job_workflow_sha` | SHA del archivo del workflow para el job actual | Siempre |
| `github.output` | Path al archivo `$GITHUB_OUTPUT` en el runner | Siempre |
| `github.path` | Path al archivo `$GITHUB_PATH` en el runner | Siempre |
| `github.ref` | Ref completa que disparó el evento (`refs/heads/main`, `refs/pull/11/merge`) | Siempre |
| `github.ref_name` | Nombre corto de la rama o tag (`main`, `v1.0.0`) | Siempre |
| `github.ref_protected` | `true` si la rama está protegida en GitHub | Siempre |
| `github.ref_type` | Tipo de ref: `branch` o `tag` | Siempre |
| `github.repository` | Nombre completo del repo (`owner/repo`) | Siempre |
| `github.repository_id` | ID numérico del repositorio | Siempre |
| `github.repository_owner` | Propietario del repositorio (`owner`) | Siempre |
| `github.repository_owner_id` | ID numérico del propietario | Siempre |
| `github.repositoryUrl` | URL git del repo (`git://github.com/owner/repo.git`) | Siempre |
| `github.retention_days` | Días de retención de artifacts y logs | Siempre |
| `github.run_id` | ID único global de la ejecución del workflow | Siempre |
| `github.run_number` | Número secuencial de ejecuciones del workflow en el repo | Siempre |
| `github.run_attempt` | Número de reintento de la ejecución (1 si no se reintentó) | Siempre |
| `github.secret_source` | Origen de los secretos (`Actions`, `Dependabot`, etc.) | Siempre |
| `github.server_url` | URL del servidor GitHub (`https://github.com`) | Siempre |
| `github.sha` | SHA completo del commit que disparó el evento | Siempre |
| `github.token` | Token de autenticación automático (equivale a `secrets.GITHUB_TOKEN`) | Siempre |
| `github.triggering_actor` | Usuario que disparó la ejecución (puede diferir de `actor` en re-runs) | Siempre |
| `github.workflow` | Nombre del workflow (campo `name:` del YAML) | Siempre |
| `github.workflow_ref` | Ref completa del archivo del workflow | Siempre |
| `github.workflow_sha` | SHA del archivo del workflow | Siempre |
| `github.workspace` | Path absoluto del directorio de trabajo en el runner (donde `actions/checkout` clona el repo) | Siempre |

> **`github.actor` vs `github.triggering_actor`**: Normalmente son iguales. Difieren cuando se hace un **re-run**: `actor` es quien hizo el push/PR original, `triggering_actor` es quien pulsó "Re-run jobs".

> **`github.ref` en `pull_request`**: No es `refs/heads/feature2` sino `refs/pull/11/merge`, una referencia virtual que GitHub crea automáticamente representando el merge hipotético del PR. Por eso `github.head_ref` y `github.base_ref` existen: para tener los nombres reales de las ramas del PR.

---

## Eventos Específicos

### Evento: `pull_request` / `pull_request_target`

```yaml
github.event.action                                    # opened, synchronize, closed, etc.
github.event.number                                    # Número del PR
github.event.pull_request.id                          # ID numérico del PR
github.event.pull_request.number                      # Número del PR
github.event.pull_request.title                       # Título
github.event.pull_request.body                        # Descripción
github.event.pull_request.state                       # open, closed
github.event.pull_request.locked                      # true/false
github.event.pull_request.draft                       # true/false ⭐ IMPORTANTE
github.event.pull_request.merged                      # true/false
github.event.pull_request.mergeable                   # true/false/null
github.event.pull_request.mergeable_state             # clean, dirty, unstable, blocked
github.event.pull_request.merged_by                   # Usuario que hizo merge
github.event.pull_request.rebaseable                  # true/false

# Autor del PR
github.event.pull_request.user.login                  # Username
github.event.pull_request.user.id                     # ID
github.event.pull_request.user.type                   # User, Bot
github.event.pull_request.user.site_admin             # true/false
github.event.pull_request.author_association          # OWNER, CONTRIBUTOR, etc.

# Ramas
github.event.pull_request.head.ref                    # Rama origen (feature-branch)
github.event.pull_request.head.sha                    # SHA del commit origen
github.event.pull_request.head.label                  # user:feature-branch
github.event.pull_request.head.repo.full_name        # owner/repo
github.event.pull_request.base.ref                    # Rama destino (main)
github.event.pull_request.base.sha                    # SHA del commit destino
github.event.pull_request.base.label                  # owner:main
github.event.pull_request.base.repo.full_name        # owner/repo

# Revisiones y asignaciones
github.event.pull_request.assignees                   # Array de asignados
github.event.pull_request.requested_reviewers         # Array de revisores
github.event.pull_request.requested_teams             # Array de equipos
github.event.pull_request.labels                      # Array de etiquetas
github.event.pull_request.milestone                   # Milestone asignado

# Estadísticas
github.event.pull_request.changed_files               # Número de archivos
github.event.pull_request.additions                   # Líneas añadidas
github.event.pull_request.deletions                   # Líneas eliminadas
github.event.pull_request.commits                     # Número de commits
github.event.pull_request.review_comments             # Comentarios de revisión
github.event.pull_request.comments                    # Comentarios generales

# URLs
github.event.pull_request.html_url                    # URL web del PR
github.event.pull_request.url                         # URL de la API
github.event.pull_request.diff_url                    # URL del diff
github.event.pull_request.patch_url                   # URL del patch
github.event.pull_request.issue_url                   # URL del issue asociado
github.event.pull_request.commits_url                 # URL de commits
github.event.pull_request.review_comments_url         # URL de comentarios

# Fechas
github.event.pull_request.created_at                  # ISO 8601
github.event.pull_request.updated_at                  # ISO 8601
github.event.pull_request.closed_at                   # ISO 8601 o null
github.event.pull_request.merged_at                   # ISO 8601 o null

# Auto-merge
github.event.pull_request.auto_merge                  # Configuración de auto-merge
github.event.pull_request.auto_merge.enabled_by       # Usuario que habilitó
github.event.pull_request.auto_merge.merge_method     # merge, squash, rebase
```

**Ejemplo completo de uso:**

```yaml
name: PR Analysis

on:
  pull_request:
    types: [opened, synchronize, reopened, ready_for_review, converted_to_draft]

jobs:
  analyze:
    runs-on: ubuntu-latest
    # Solo ejecutar si NO es draft
    if: github.event.pull_request.draft == false
    
    steps:
      - name: Información del PR
        run: |
          echo "🔢 PR #${{ github.event.pull_request.number }}"
          echo "📝 Título: ${{ github.event.pull_request.title }}"
          echo "👤 Autor: ${{ github.event.pull_request.user.login }}"
          echo "📊 Estado: ${{ github.event.pull_request.state }}"
          echo "📋 Draft: ${{ github.event.pull_request.draft }}"
          echo "✅ Mergeable: ${{ github.event.pull_request.mergeable }}"
          echo "🔀 De: ${{ github.event.pull_request.head.ref }}"
          echo "🎯 A: ${{ github.event.pull_request.base.ref }}"
          echo "📄 Archivos: ${{ github.event.pull_request.changed_files }}"
          echo "➕ Adiciones: ${{ github.event.pull_request.additions }}"
          echo "➖ Eliminaciones: ${{ github.event.pull_request.deletions }}"

  draft-warning:
    runs-on: ubuntu-latest
    # Solo ejecutar si ES draft
    if: github.event.pull_request.draft == true
    
    steps:
      - name: Advertencia de draft
        run: echo "⚠️ Este PR es un borrador y no se ejecutarán los tests completos"
```

### Evento: `push`

| Campo | Descripción | Ejemplo |
|---|---|---|
| `github.event.after` | SHA del commit **después** del push (el nuevo HEAD de la rama) | `9cf3dea...` |
| `github.event.before` | SHA del commit **antes** del push (el HEAD anterior) | `83108f7...` |
| `github.event.compare` | URL para comparar los cambios en GitHub | `https://github.com/owner/repo/compare/before...after` |
| `github.event.created` | `true` si el push **creó** una nueva rama o tag | `true` / `false` |
| `github.event.deleted` | `true` si el push **eliminó** una rama o tag | `true` / `false` |
| `github.event.forced` | `true` si fue un **force push** (`git push --force`) | `true` / `false` |
| `github.event.ref` | Ref completa que recibió el push (`refs/heads/main`) | `refs/heads/main` |
| `github.event.pusher.name` | Nombre del usuario que hizo el push | `dukono` |
| `github.event.pusher.email` | Email del usuario que hizo el push | `dukono@example.com` |

> ⚠️ **`github.event.before` y `github.event.after` solo existen en eventos `push`**. En `pull_request` están vacíos. Para un PR usa `github.event.pull_request.base.sha` y `github.event.pull_request.head.sha`.

**Commits del push:**

```yaml
github.event.commits                    # Array con todos los commits del push
github.event.commits[0].id             # SHA del commit
github.event.commits[0].message        # Mensaje del commit
github.event.commits[0].timestamp      # ISO 8601
github.event.commits[0].author.name    # Nombre del autor
github.event.commits[0].author.email   # Email del autor
github.event.commits[0].author.username # GitHub username del autor
github.event.commits[0].url            # URL del commit en GitHub
github.event.commits[0].distinct       # true si es un commit único (no ya en el repo)
github.event.commits[0].added          # Array de archivos añadidos
github.event.commits[0].modified       # Array de archivos modificados
github.event.commits[0].removed        # Array de archivos eliminados

# Head commit (último commit del push)
github.event.head_commit.id            # SHA del último commit
github.event.head_commit.message       # Mensaje del último commit
github.event.head_commit.timestamp     # ISO 8601
github.event.head_commit.author.name   # Nombre del autor
github.event.head_commit.author.email  # Email del autor
github.event.head_commit.committer.name  # Nombre del committer
github.event.head_commit.committer.email # Email del committer
```

### Evento: `issues`

```yaml
github.event.action                                    # opened, closed, edited, etc.
github.event.issue.number                             # Número del issue
github.event.issue.title                              # Título
github.event.issue.body                               # Descripción
github.event.issue.state                              # open, closed
github.event.issue.locked                             # true/false
github.event.issue.user.login                         # Autor
github.event.issue.assignees                          # Array de asignados
github.event.issue.labels                             # Array de etiquetas
github.event.issue.comments                           # Número de comentarios
github.event.issue.created_at                         # Fecha de creación
github.event.issue.updated_at                         # Última actualización
github.event.issue.closed_at                          # Fecha de cierre
github.event.issue.html_url                           # URL del issue
```

### Evento: `issue_comment`

```yaml
github.event.action                                    # created, edited, deleted
github.event.issue.number                             # Número del issue/PR
github.event.comment.id                               # ID del comentario
github.event.comment.body                             # Contenido
github.event.comment.user.login                       # Autor
github.event.comment.created_at                       # Fecha
github.event.comment.html_url                         # URL del comentario
```

### Evento: `release`

```yaml
github.event.action                                    # published, created, edited, etc.
github.event.release.tag_name                         # v1.0.0
github.event.release.name                             # Nombre del release
github.event.release.body                             # Release notes
github.event.release.draft                            # true/false
github.event.release.prerelease                       # true/false
github.event.release.created_at                       # Fecha
github.event.release.published_at                     # Fecha de publicación
github.event.release.html_url                         # URL del release
github.event.release.upload_url                       # URL para subir assets
github.event.release.assets                           # Array de archivos
```

### Evento: `workflow_dispatch`

```yaml
github.event.inputs                                    # Objeto con los inputs
github.event.inputs.nombre_input                      # Valor de un input específico
```

**Ejemplo:**
```yaml
on:
  workflow_dispatch:
    inputs:
      environment:
        description: 'Ambiente a desplegar'
        required: true
        type: choice
        options:
          - development
          - staging
          - production
      version:
        description: 'Versión a desplegar'
        required: true
        default: 'latest'

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - name: Deploy
        run: |
          echo "Desplegando en: ${{ github.event.inputs.environment }}"
          echo "Versión: ${{ github.event.inputs.version }}"
```

### Evento: `schedule`

```yaml
github.event.schedule                                  # Expresión cron que disparó
```

---

## Contexto `env`

Variables de entorno definidas en el workflow.

**Niveles de scope:**

```yaml
# Nivel workflow (disponible en todos los jobs)
env:
  GLOBAL_VAR: "valor global"

jobs:
  mi-job:
    # Nivel job (disponible en todos los steps del job)
    env:
      JOB_VAR: "valor job"
    
    steps:
      # Nivel step (solo en este step)
      - name: Mi step
        env:
          STEP_VAR: "valor step"
        run: |
          echo "Global: $GLOBAL_VAR"
          echo "Job: $JOB_VAR"
          echo "Step: $STEP_VAR"
```

**Acceso en diferentes contextos:**

```yaml
# En comandos shell
run: echo "$MI_VARIABLE"

# En expresiones
run: echo "${{ env.MI_VARIABLE }}"

# En Python
shell: python
run: |
  import os
  valor = os.getenv('MI_VARIABLE')
```

---

## Contexto `runner`

Información sobre el runner (máquina que ejecuta el workflow).

```yaml
runner.name                # Nombre del runner
runner.os                  # Linux, Windows, macOS
runner.arch                # X86, X64, ARM, ARM64
runner.temp                # Path al directorio temporal
runner.tool_cache          # Path a la caché de herramientas
runner.workspace           # Path al workspace
runner.environment         # 'github-hosted' o 'self-hosted'
runner.debug               # '1' si debug logging está habilitado (string, no booleano)
```

**Ejemplo de uso:**

```yaml
- name: Información del runner
  run: |
    echo "OS: ${{ runner.os }}"
    echo "Arquitectura: ${{ runner.arch }}"
    echo "Temp: ${{ runner.temp }}"
    echo "Workspace: ${{ runner.workspace }}"
    echo "Entorno: ${{ runner.environment }}"
    echo "Debug: ${{ runner.debug }}"

- name: Comando específico por OS
  run: |
    if [ "${{ runner.os }}" == "Windows" ]; then
      echo "Ejecutando en Windows"
    elif [ "${{ runner.os }}" == "Linux" ]; then
      echo "Ejecutando en Linux"
    fi

- name: Step solo en self-hosted
  if: runner.environment == 'self-hosted'
  run: echo "Corriendo en runner propio"

- name: Info de debug
  if: runner.debug == '1'
  run: echo "Debug activo — logging detallado habilitado"
```

> ⚠️ `runner.debug` es un **string** (`'1'`), no un booleano. Comparar con `== '1'`, no con `== true`. Se activa al habilitar "Enable debug logging" en la UI o seteando el secret `ACTIONS_RUNNER_DEBUG=true`.

---

## Contexto `steps`

Outputs de steps anteriores.

```yaml
steps.<step_id>.outputs.<output_name>
steps.<step_id>.outcome                # success, failure, cancelled, skipped
steps.<step_id>.conclusion             # success, failure, cancelled, skipped
```

> ⚠️ **`steps.<id>.outputs` solo funciona si guardas el valor en `$GITHUB_OUTPUT`**, NO en `$GITHUB_ENV`.
>
> | Mecanismo | Cómo acceder | Requiere `id` |
> |---|---|---|
> | `echo "key=val" >> $GITHUB_OUTPUT` | `${{ steps.<id>.outputs.key }}` | ✅ Sí |
> | `echo "key=val" >> $GITHUB_ENV` | `$key` (variable bash) | ❌ No |
>
> ```yaml
> # ✅ Accesible con steps.<id>.outputs.ref
> echo "ref=${{ github.ref }}" >> $GITHUB_OUTPUT
>
> # ❌ NO accesible con steps.<id>.outputs.ref (solo como $ref en bash)
> echo "ref=${{ github.ref }}" >> $GITHUB_ENV
> ```
>
> Si necesitas acceder al valor **tanto** con `steps.<id>.outputs` como con `$VAR` en bash, escribe en ambos archivos:
> ```yaml
> echo "ref=${{ github.ref }}" >> $GITHUB_OUTPUT   # para steps.getter.outputs.ref
> echo "ref=${{ github.ref }}" >> $GITHUB_ENV       # para $ref en bash
> ```

**Ejemplo:**

```yaml
steps:
  - name: Calcular versión
    id: version
    run: |
      VERSION="1.2.3"
      echo "version=$VERSION" >> $GITHUB_OUTPUT
      echo "major=1" >> $GITHUB_OUTPUT
      echo "minor=2" >> $GITHUB_OUTPUT
  
  - name: Usar versión
    run: |
      echo "Versión completa: ${{ steps.version.outputs.version }}"
      echo "Major: ${{ steps.version.outputs.major }}"
      echo "Minor: ${{ steps.version.outputs.minor }}"
  
  - name: Verificar estado
    if: steps.version.outcome == 'success'
    run: echo "El paso anterior fue exitoso"
```

---

## Contexto `job`

Información sobre el job actual.

```yaml
job.container.id           # ID del container (si usa container)
job.container.network      # Network del container
job.services               # Servicios definidos
job.status                 # success, failure, cancelled
```

**Ejemplo:**

```yaml
- name: Verificar estado del job
  if: always()
  run: |
    echo "Estado del job: ${{ job.status }}"
    if [ "${{ job.status }}" == "failure" ]; then
      echo "❌ El job falló"
    fi
```

---

## Contexto `secrets`

Secretos encriptados configurados en GitHub.

```yaml
secrets.GITHUB_TOKEN       # Token automático
secrets.MI_SECRETO         # Secretos personalizados
```

**Uso:**

```yaml
- name: Usar secreto
  env:
    API_KEY: ${{ secrets.API_KEY }}
  run: |
    # El secreto está disponible como variable de entorno
    # NUNCA se imprime en los logs
    curl -H "Authorization: Bearer $API_KEY" https://api.example.com
```

**Secretos heredados de organización:**

```yaml
secrets.INHERITED_SECRET   # Secretos de la organización
```

---

## Contexto `vars`

Variables de configuración (no encriptadas).

```yaml
vars.ENVIRONMENT           # Variables de repositorio
vars.ORG_VAR              # Variables de organización
```

**Diferencia con secrets:**
- `secrets`: Encriptados, para datos sensibles (contraseñas, tokens)
- `vars`: No encriptados, para configuración (URLs, nombres de ambiente)

---

## Contexto `strategy` y `matrix`

Para jobs con matrices.

```yaml
strategy.fail-fast         # true/false
strategy.job-index         # Índice del job en la matriz
strategy.job-total         # Total de jobs en la matriz
strategy.max-parallel      # Máximo de jobs en paralelo

matrix.<property>          # Valor actual de la propiedad de matriz
```

**Ejemplo:**

```yaml
strategy:
  matrix:
    os: [ubuntu-latest, windows-latest, macos-latest]
    python: ['3.9', '3.10', '3.11']
    include:
      - os: ubuntu-latest
        experimental: true

steps:
  - name: Info
    run: |
      echo "OS: ${{ matrix.os }}"
      echo "Python: ${{ matrix.python }}"
      echo "Experimental: ${{ matrix.experimental }}"
      echo "Job index: ${{ strategy.job-index }}"
      echo "Total jobs: ${{ strategy.job-total }}"
```

---

## Contexto `needs`

Outputs de jobs dependientes.

```yaml
needs.<job_id>.outputs.<output_name>
needs.<job_id>.result      # success, failure, cancelled, skipped
```

**Ejemplo:**

```yaml
jobs:
  build:
    runs-on: ubuntu-latest
    outputs:
      version: ${{ steps.version.outputs.version }}
      artifact-name: ${{ steps.build.outputs.artifact }}
    steps:
      - id: version
        run: echo "version=1.2.3" >> $GITHUB_OUTPUT
      - id: build
        run: echo "artifact=app-1.2.3.zip" >> $GITHUB_OUTPUT
  
  deploy:
    needs: build
    runs-on: ubuntu-latest
    steps:
      - name: Deploy
        run: |
          echo "Versión: ${{ needs.build.outputs.version }}"
          echo "Artifact: ${{ needs.build.outputs.artifact }}"
          echo "Estado del build: ${{ needs.build.result }}"
```

---

## Contexto `inputs`

Inputs de workflows reutilizables o manuales.

```yaml
inputs.<input_name>        # Valor del input
```

**Para workflow_dispatch:**

```yaml
on:
  workflow_dispatch:
    inputs:
      logLevel:
        description: 'Log level'
        required: true
        default: 'warning'
        type: choice
        options:
          - info
          - warning
          - debug

jobs:
  log:
    runs-on: ubuntu-latest
    steps:
      - run: echo "Log level: ${{ inputs.logLevel }}"
```

**Para workflows reutilizables:**

```yaml
# workflow-llamado.yml
on:
  workflow_call:
    inputs:
      username:
        required: true
        type: string
      environment:
        required: true
        type: string

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - run: |
          echo "Usuario: ${{ inputs.username }}"
          echo "Ambiente: ${{ inputs.environment }}"
```

---

## Contexto `jobs` (solo reusable workflows)

El contexto `jobs` es especial: **solo está disponible dentro de un reusable workflow** (un workflow con `on: workflow_call`). No existe en workflows normales.

```yaml
jobs.<job_id>.result               # success, failure, cancelled, skipped
jobs.<job_id>.outputs.<output_name> # Output de un job concreto
```

### Para qué sirve

Permite acceder a outputs y resultados de **todos los jobs del workflow reutilizable**, no solo de los que están en `needs` directo. Esto es especialmente útil en el contexto de `outputs:` del workflow para exponer resultados al caller.

### Diferencia clave entre `jobs` y `needs`

| Contexto | Disponible en | Accede a |
|---|---|---|
| `needs` | Cualquier workflow | Solo jobs declarados explícitamente en `needs:` del job actual |
| `jobs` | Solo reusable workflows (`workflow_call`) | Todos los jobs del workflow, independientemente de dependencias directas |

### Ejemplo: reusable workflow con `jobs` context

```yaml
# reusable-workflow.yml (on: workflow_call)
on:
  workflow_call:
    outputs:
      final-result:
        description: "Resultado consolidado"
        value: ${{ jobs.final-step.outputs.result }}   # ← usa contexto jobs

jobs:
  build:
    runs-on: ubuntu-latest
    outputs:
      artifact: ${{ steps.b.outputs.name }}
    steps:
      - id: b
        run: echo "name=app.zip" >> $GITHUB_OUTPUT

  test:
    needs: build
    runs-on: ubuntu-latest
    outputs:
      passed: ${{ steps.t.outputs.passed }}
    steps:
      - id: t
        run: echo "passed=true" >> $GITHUB_OUTPUT

  final-step:
    needs: [build, test]
    runs-on: ubuntu-latest
    outputs:
      result: ${{ steps.r.outputs.summary }}
    steps:
      - id: r
        run: |
          echo "summary=build=${{ needs.build.outputs.artifact }} test=${{ needs.test.outputs.passed }}" >> $GITHUB_OUTPUT
```

```yaml
# caller-workflow.yml
jobs:
  call-reusable:
    uses: ./.github/workflows/reusable-workflow.yml

  use-result:
    needs: call-reusable
    runs-on: ubuntu-latest
    steps:
      - run: echo "${{ needs.call-reusable.outputs.final-result }}"
```

### Acceso a `jobs` dentro del reusable workflow para definir outputs

En la sección `outputs:` del workflow reutilizable, se usa `jobs.<id>.outputs.<key>` para referenciar outputs de cualquier job del workflow, incluso si no hay una relación directa de `needs` en ese nivel:

```yaml
on:
  workflow_call:
    outputs:
      deploy-url:
        value: ${{ jobs.deploy.outputs.url }}   # ← jobs context, no needs
      test-status:
        value: ${{ jobs.test.outputs.status }}  # ← jobs context, no needs
```

> Si intentas usar `needs.deploy.outputs.url` en la sección `outputs:` del workflow (fuera de un job), obtendrás un error — en ese nivel no existe el contexto `needs`. Solo `jobs` funciona ahí.

---

## Arrays en el Contexto: labels, assignees y similares

Varios campos del contexto `github.event` son **arrays de objetos**, no valores simples. Los más comunes son:

| Campo | Qué contiene |
|---|---|
| `github.event.pull_request.labels` | Etiquetas del PR |
| `github.event.pull_request.assignees` | Usuarios asignados al PR |
| `github.event.pull_request.requested_reviewers` | Revisores solicitados |
| `github.event.pull_request.requested_teams` | Equipos revisores |
| `github.event.issue.labels` | Etiquetas del issue |
| `github.event.issue.assignees` | Usuarios asignados al issue |

**Estructura de cada elemento de `labels`:**
```yaml
github.event.pull_request.labels[0].id          # ID numérico de la etiqueta
github.event.pull_request.labels[0].name        # Nombre: "bug", "urgent", etc.
github.event.pull_request.labels[0].color       # Color hex: "d73a4a"
github.event.pull_request.labels[0].description # Descripción de la etiqueta
github.event.pull_request.labels[0].default     # true si es etiqueta por defecto de GitHub
```

---

### El operador `.*` — Extraer un campo de todos los elementos

Como `labels` es un array de objetos, para acceder al `name` de **todas** las etiquetas a la vez se usa el operador `.*`:

```
github.event.pull_request.labels        → array de objetos label completos
github.event.pull_request.labels.*.name → array de solo los nombres ["bug", "urgent"]
```

```yaml
# Sin .*  → array de objetos completos (no útil para comparar directamente)
github.event.pull_request.labels
# → [{ id: 123, name: "bug", color: "d73a4a" }, { id: 456, name: "urgent", ... }]

# Con .*  → array de solo el campo "name" (útil para contains())
github.event.pull_request.labels.*.name
# → ["bug", "urgent"]
```

> `.*` significa: **para cada elemento del array, dame el campo que viene después**. Es equivalente a un `.map()` en JavaScript.

---

### Usar `contains()` con arrays

La función `contains()` acepta un array como primer argumento y comprueba si algún elemento coincide:

```yaml
# ¿Tiene la etiqueta "bug"?
if: contains(github.event.pull_request.labels.*.name, 'bug')

# ¿Tiene la etiqueta "urgent"?
if: contains(github.event.pull_request.labels.*.name, 'urgent')

# ¿Está asignado a un usuario concreto?
if: contains(github.event.pull_request.assignees.*.login, 'dukono')

# ¿Se ha solicitado revisión a alguien concreto?
if: contains(github.event.pull_request.requested_reviewers.*.login, 'dukono')
```

---

### Ejemplo completo con labels

```yaml
name: Gestión por Labels

on:
  pull_request:
    types: [labeled, unlabeled, opened, synchronize]

jobs:
  deploy-preview:
    # Ejecutar solo si el PR tiene la etiqueta "preview"
    if: contains(github.event.pull_request.labels.*.name, 'preview')
    runs-on: ubuntu-latest
    steps:
      - name: Desplegar preview
        run: echo "Desplegando preview del PR #${{ github.event.pull_request.number }}"

  skip-ci:
    # Ejecutar solo si NO tiene la etiqueta "skip-ci"
    if: "!contains(github.event.pull_request.labels.*.name, 'skip-ci')"
    runs-on: ubuntu-latest
    steps:
      - name: Ejecutar tests
        run: echo "Ejecutando tests..."

  notify-urgent:
    # Si tiene la etiqueta "urgent"
    if: contains(github.event.pull_request.labels.*.name, 'urgent')
    runs-on: ubuntu-latest
    steps:
      - name: Notificación urgente
        run: echo "⚠️ PR urgente: ${{ github.event.pull_request.title }}"

  show-all-labels:
    runs-on: ubuntu-latest
    steps:
      - name: Mostrar todas las etiquetas
        run: |
          # join() une los nombres con un separador
          echo "Labels: ${{ join(github.event.pull_request.labels.*.name, ', ') }}"
          # Ejemplo de salida: "Labels: bug, urgent, preview"
```

---

### ⚠️ Limitación importante: `.*` solo funciona en expresiones `${{ }}`

El operador `.*` es exclusivo de las **expresiones de GitHub Actions**, no es Bash ni JavaScript:

```yaml
# ✅ Correcto: dentro de expresión ${{ }}
if: contains(github.event.pull_request.labels.*.name, 'bug')

# ✅ Correcto: dentro de expresión ${{ }}
run: echo "${{ join(github.event.pull_request.labels.*.name, ', ') }}"

# ❌ No puedes hacer esto en Bash directamente
run: echo "${github.event.pull_request.labels.*.name}"  # No funciona
```

Si necesitas iterar sobre los labels en un script Bash, usa `toJSON()` y `jq`:

```yaml
- name: Iterar labels en Bash
  run: |
    LABELS='${{ toJSON(github.event.pull_request.labels) }}'
    echo "$LABELS" | jq -r '.[].name'   # Imprime cada nombre en una línea
    
    # Comprobar si existe un label concreto
    HAS_BUG=$(echo "$LABELS" | jq -r '[.[].name] | contains(["bug"])')
    echo "Tiene bug: $HAS_BUG"
```

---

## Funciones de Contexto

### `contains()`

Verifica si un string o array contiene un valor.

```yaml
# Verificar si una rama contiene "feature"
if: contains(github.ref, 'feature')

# Verificar si hay una etiqueta específica
if: contains(github.event.pull_request.labels.*.name, 'bug')

# Verificar en array
if: contains(fromJSON('["dev", "prod"]'), github.ref_name)
```

### `startsWith()`

Verifica si un string empieza con un prefijo.

```yaml
# Si la rama empieza con "feature/"
if: startsWith(github.ref, 'refs/heads/feature/')

# Si el título del PR empieza con "feat:"
if: startsWith(github.event.pull_request.title, 'feat:')
```

### `endsWith()`

Verifica si un string termina con un sufijo.

```yaml
# Si la rama termina con "-dev"
if: endsWith(github.ref_name, '-dev')
```

### `format()`

Formatea un string.

```yaml
# Ejemplo
run: echo "${{ format('Hola {0} {1}', 'GitHub', 'Actions') }}"
# Output: Hola GitHub Actions
```

### `join()`

Une elementos de un array.

```yaml
# Unir labels con comas
run: echo "${{ join(github.event.pull_request.labels.*.name, ', ') }}"
```

### `toJSON()`

Convierte a JSON.

```yaml
- name: Ver evento completo
  env:
    EVENT_JSON: ${{ toJSON(github.event) }}
  run: echo "$EVENT_JSON"
```

### `fromJSON()`

Parsea JSON.

```yaml
- name: Parsear JSON
  run: |
    VALUE=$(echo '${{ fromJSON('{"key": "value"}').key }}')
    echo $VALUE
```

### `hashFiles()`

Calcula hash de archivos (útil para cache).

```yaml
- uses: actions/cache@v4
  with:
    path: ~/.cache/pip
    key: ${{ runner.os }}-pip-${{ hashFiles('**/requirements.txt') }}
```

### `success()`, `failure()`, `cancelled()`, `always()`

Estados de ejecución.

```yaml
- name: Siempre ejecutar
  if: always()
  run: echo "Se ejecuta siempre"

- name: Solo si tiene éxito
  if: success()
  run: echo "Solo si los pasos anteriores tuvieron éxito"

- name: Solo si falla
  if: failure()
  run: echo "Solo si algo falló"

- name: Solo si se cancela
  if: cancelled()
  run: echo "Solo si se canceló"
```

---

## Tabla de Referencia Rápida

| Contexto | Uso Principal | Ejemplo |
|----------|---------------|---------|
| `github` | Info del repo/evento | `${{ github.repository }}` |
| `env` | Variables de entorno | `${{ env.NODE_ENV }}` |
| `job` | Estado del job | `${{ job.status }}` |
| `steps` | Outputs de pasos | `${{ steps.build.outputs.version }}` |
| `runner` | Info del runner | `${{ runner.os }}` |
| `secrets` | Datos sensibles | `${{ secrets.API_KEY }}` |
| `vars` | Configuración | `${{ vars.ENVIRONMENT }}` |
| `strategy` | Info de matriz | `${{ strategy.job-index }}` |
| `matrix` | Valor de matriz | `${{ matrix.node-version }}` |
| `needs` | Outputs de jobs dependientes | `${{ needs.build.outputs.tag }}` |
| `inputs` | Inputs del workflow | `${{ inputs.environment }}` |
| `jobs` | Outputs de todos los jobs (solo reusable) | `${{ jobs.deploy.outputs.url }}` |

---

## Preguntas de Examen

**P: ¿Qué devuelve `runner.environment` y para qué sirve?**
→ Devuelve `'github-hosted'` si el runner es de GitHub o `'self-hosted'` si es propio. Permite escribir steps condicionales que solo se ejecutan en un tipo de runner concreto con `if: runner.environment == 'self-hosted'`.

**P: ¿`runner.debug` es un booleano o un string? ¿Cómo se compara correctamente?**
→ Es un string. Cuando el debug logging está activo vale `'1'`; si no está activo, la propiedad no existe (está vacía). Comparar siempre con `== '1'`, no con `== true`. Se activa con el secret `ACTIONS_RUNNER_DEBUG=true` o desde la UI al re-ejecutar un workflow.

**P: ¿En qué contextos está disponible el contexto `jobs`?**
→ Solo en reusable workflows (workflows con `on: workflow_call`). No existe en workflows normales ni en jobs dentro de un workflow caller.

**P: ¿Cuál es la diferencia entre `jobs` y `needs` para acceder a outputs?**
→ `needs` solo accede a outputs de los jobs declarados explícitamente en `needs:` del job actual. `jobs` accede a outputs y resultados de todos los jobs del workflow reutilizable sin necesidad de declararlos como dependencia. `jobs` también es el único contexto válido en la sección `outputs:` de nivel de workflow (donde `needs` no existe).

**P: Si quieres exponer el output de un job en la sección `outputs:` del workflow reutilizable, ¿qué contexto debes usar?**
→ El contexto `jobs`. Por ejemplo: `value: ${{ jobs.deploy.outputs.url }}`. No se puede usar `needs` en ese nivel porque está fuera de cualquier job.

---

*Documentación completa de contextos y variables en GitHub Actions*
*Última actualización: Marzo 2026*

