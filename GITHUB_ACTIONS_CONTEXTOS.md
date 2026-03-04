# 🎭 GitHub Actions: Contextos y Variables Completas

## 📚 Índice
1. [Contexto `github`](#contexto-github)
2. [Contexto `env`](#contexto-env)
3. [Contexto `job`](#contexto-job)
4. [Contexto `steps`](#contexto-steps)
5. [Contexto `runner`](#contexto-runner)
6. [Contexto `secrets`](#contexto-secrets)
7. [Contexto `vars`](#contexto-vars)
8. [Contexto `strategy` y `matrix`](#contexto-strategy-y-matrix)
9. [Contexto `needs`](#contexto-needs)
10. [Contexto `inputs`](#contexto-inputs)
11. [Funciones de Contexto](#funciones-de-contexto)

---

## 📦 Contexto `github`

### Propiedades Generales

```yaml
github.action              # Nombre de la acción actual
github.action_path         # Path donde está la acción
github.action_ref          # Ref de la acción (usuario/repo@ref)
github.action_repository   # Repositorio de la acción
github.action_status       # Estado de la acción (success, failure, cancelled)
github.actor               # Usuario que disparó el workflow
github.actor_id            # ID numérico del usuario
github.api_url             # https://api.github.com
github.base_ref            # Rama base del PR
github.env                 # Path al archivo de variables de entorno
github.event               # Payload completo del evento
github.event_name          # Tipo de evento (push, pull_request, etc.)
github.event_path          # Path al archivo JSON del evento
github.graphql_url         # https://api.github.com/graphql
github.head_ref            # Rama del PR
github.job                 # ID del job actual
github.job_workflow_sha    # SHA del workflow job
github.output              # Path al archivo de outputs
github.path                # Path al archivo de PATH
github.ref                 # refs/heads/main o refs/tags/v1.0.0
github.ref_name            # main o v1.0.0
github.ref_protected       # true si la rama está protegida
github.ref_type            # branch o tag
github.repository          # owner/repo
github.repository_id       # ID numérico del repositorio
github.repository_owner    # owner
github.repository_owner_id # ID numérico del owner
github.repositoryUrl       # git://github.com/owner/repo.git
github.retention_days      # Días de retención de artifacts
github.run_id              # ID único del workflow run
github.run_number          # Número secuencial del run
github.run_attempt         # Número de intento (para re-runs)
github.secret_source       # Actions, Dependabot, etc.
github.server_url          # https://github.com
github.sha                 # SHA del commit
github.token               # Token automático (si está disponible)
github.triggering_actor    # Usuario que disparó (puede diferir de actor)
github.workflow            # Nombre del workflow
github.workflow_ref        # Referencia del workflow
github.workflow_sha        # SHA del workflow
github.workspace           # Path del workspace
```

---

## 🔄 Eventos Específicos

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

```yaml
github.event.after                                     # SHA después del push
github.event.before                                    # SHA antes del push
github.event.compare                                   # URL para comparar
github.event.created                                   # true si es creación de rama/tag
github.event.deleted                                   # true si es eliminación
github.event.forced                                    # true si es force push
github.event.ref                                       # refs/heads/main

# Commits
github.event.commits                                   # Array de commits
github.event.commits[0].id                            # SHA del commit
github.event.commits[0].message                       # Mensaje
github.event.commits[0].timestamp                     # ISO 8601
github.event.commits[0].author.name                   # Nombre del autor
github.event.commits[0].author.email                  # Email
github.event.commits[0].author.username               # GitHub username
github.event.commits[0].url                           # URL del commit
github.event.commits[0].distinct                      # true si es único
github.event.commits[0].added                         # Archivos añadidos
github.event.commits[0].modified                      # Archivos modificados
github.event.commits[0].removed                       # Archivos eliminados

# Head commit (último commit)
github.event.head_commit.id                           # SHA
github.event.head_commit.message                      # Mensaje
github.event.head_commit.timestamp                    # ISO 8601
github.event.head_commit.author.name                  # Nombre
github.event.head_commit.author.email                 # Email
github.event.head_commit.committer.name               # Quien commitó
github.event.head_commit.committer.email              # Email del committer

# Pusher
github.event.pusher.name                              # Quien hizo push
github.event.pusher.email                             # Email
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

## 🌍 Contexto `env`

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

## 🏃 Contexto `runner`

Información sobre el runner (máquina que ejecuta el workflow).

```yaml
runner.name                # Nombre del runner
runner.os                  # Linux, Windows, macOS
runner.arch                # X86, X64, ARM, ARM64
runner.temp                # Path al directorio temporal
runner.tool_cache          # Path a la caché de herramientas
runner.workspace           # Path al workspace
runner.debug               # '1' si debug está habilitado
```

**Ejemplo de uso:**

```yaml
- name: Información del runner
  run: |
    echo "OS: ${{ runner.os }}"
    echo "Arquitectura: ${{ runner.arch }}"
    echo "Temp: ${{ runner.temp }}"
    echo "Workspace: ${{ runner.workspace }}"

- name: Comando específico por OS
  run: |
    if [ "${{ runner.os }}" == "Windows" ]; then
      echo "Ejecutando en Windows"
    elif [ "${{ runner.os }}" == "Linux" ]; then
      echo "Ejecutando en Linux"
    fi
```

---

## 📤 Contexto `steps`

Outputs de steps anteriores.

```yaml
steps.<step_id>.outputs.<output_name>
steps.<step_id>.outcome                # success, failure, cancelled, skipped
steps.<step_id>.conclusion             # success, failure, cancelled, skipped
```

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

## 💼 Contexto `job`

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

## 🔐 Contexto `secrets`

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

## 🔧 Contexto `vars`

Variables de configuración (no encriptadas).

```yaml
vars.ENVIRONMENT           # Variables de repositorio
vars.ORG_VAR              # Variables de organización
```

**Diferencia con secrets:**
- `secrets`: Encriptados, para datos sensibles (contraseñas, tokens)
- `vars`: No encriptados, para configuración (URLs, nombres de ambiente)

---

## 📊 Contexto `strategy` y `matrix`

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

## 🔗 Contexto `needs`

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

## 📥 Contexto `inputs`

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

## 🧮 Funciones de Contexto

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

## 📋 Tabla de Referencia Rápida

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
| `needs` | Outputs de jobs | `${{ needs.build.outputs.tag }}` |
| `inputs` | Inputs del workflow | `${{ inputs.environment }}` |

---

*Documentación completa de contextos y variables en GitHub Actions*
*Última actualización: Enero 2026*

