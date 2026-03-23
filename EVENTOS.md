# 🎯 GitHub Actions: Eventos y Triggers Completos

## Índice

### Eventos de Código
- [`push`](#push)
- [`pull_request`](#pull_request)
- [`pull_request_target`](#pull_request_target)
- [`create`](#create)
- [`delete`](#delete)

### Eventos de Issues y PRs
- [`issues`](#issues)
- [`issue_comment`](#issue_comment)
- [`pull_request_review`](#pull_request_review)
- [`pull_request_review_comment`](#pull_request_review_comment)

### Eventos de Releases y Tags
- [`release`](#release)
- [`workflow_run`](#workflow_run)

### Eventos de Colaboración
- [`fork`](#fork)
- [`star` / `watch`](#star--watch)
- [`member`](#member)

### Eventos Programados
- [`schedule`](#schedule)

### Eventos Manuales
- [`workflow_dispatch`](#workflow_dispatch)
- [`repository_dispatch`](#repository_dispatch)

### Eventos de Workflows
- [`workflow_call`](#workflow_call)

### Referencia
- [Filtros y Opciones Avanzadas](#filtros-y-opciones-avanzadas)
- [Combinación de Eventos](#combinación-de-eventos)
- [Tabla de Referencia Rápida](#tabla-de-referencia-rápida)

---

## Eventos de Código

### `push`

Se dispara cuando se hace push a un repositorio.

**Sintaxis básica:**
```yaml
on: push
```

**Con filtros:**
```yaml
on:
  push:
    branches:
      - main
      - develop
      - 'releases/**'     # Cualquier rama que empiece con releases/
    branches-ignore:
      - 'docs/**'          # Ignorar ramas que empiecen con docs/
    tags:
      - v1.*               # Tags que empiecen con v1.
      - v2.*
    tags-ignore:
      - '*-beta'           # Ignorar tags que terminen con -beta
    paths:
      - 'src/**'           # Solo si cambian archivos en src/
      - '**.js'            # Solo archivos JavaScript
      - '!**.md'           # Excluir archivos Markdown
    paths-ignore:
      - 'docs/**'
      - '**.md'
```

**Información disponible:**
- `github.event.ref` - Rama o tag completo
- `github.event.before` - SHA antes del push
- `github.event.after` - SHA después del push
- `github.event.created` - `true` si es nueva rama/tag
- `github.event.deleted` - `true` si se eliminó rama/tag
- `github.event.forced` - `true` si es force push
- `github.event.commits` - Array de commits
- `github.event.head_commit` - Último commit

**Ejemplo completo:**
```yaml
name: CI en Push

on:
  push:
    branches:
      - main
      - 'feature/**'
    paths:
      - 'src/**'
      - 'tests/**'
      - 'package.json'

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Info del push
        run: |
          echo "Rama: ${{ github.ref_name }}"
          echo "Commits: ${{ github.event.commits.length }}"
          echo "Mensaje: ${{ github.event.head_commit.message }}"
          echo "Autor: ${{ github.event.head_commit.author.name }}"
```

---

### `pull_request`

Se dispara en eventos de Pull Request.

**Tipos de actividad (`types`) y cuándo salta cada uno:**

| Type | Cuándo salta | Notas |
|---|---|---|
| `opened` | Al crear el PR por primera vez | Solo salta una vez por PR |
| `edited` | Al editar el **título o descripción** del PR | No salta por nuevos commits |
| `closed` | Al cerrar el PR, tanto si se mergea como si no | Usar `github.event.pull_request.merged` para distinguir |
| `reopened` | Al reabrir un PR cerrado | |
| `synchronize` | Al añadir nuevos commits a la rama del PR | Es el más común en CI: salta en cada `git push` de la rama |
| `assigned` | Al asignar un usuario al PR | |
| `unassigned` | Al quitar un usuario asignado | |
| `labeled` | Al añadir una etiqueta al PR | |
| `unlabeled` | Al quitar una etiqueta del PR | |
| `locked` | Al bloquear la conversación del PR | |
| `unlocked` | Al desbloquear la conversación del PR | |
| `review_requested` | Al solicitar revisión a alguien | |
| `review_request_removed` | Al quitar una solicitud de revisión | |
| `ready_for_review` | Al sacar el PR del estado **draft** (listo para revisar) | Solo salta al cambiar de draft → listo |
| `converted_to_draft` | Al convertir el PR a **draft** | Solo salta al cambiar de listo → draft |
| `auto_merge_enabled` | Al activar el auto-merge en el PR | |
| `auto_merge_disabled` | Al desactivar el auto-merge en el PR | |

**Por defecto (sin especificar `types`):**
Si no indicas `types:`, GitHub solo dispara en: `opened`, `synchronize`, `reopened`

```yaml
# Equivalente a no poner types:
on:
  pull_request:
    types: [opened, synchronize, reopened]
```

**Con filtros:**
```yaml
on:
  pull_request:
    types: [opened, synchronize, reopened]
    branches:
      - main                # Solo PRs hacia main
      - develop
    branches-ignore:
      - 'experimental/**'
    paths:
      - 'src/**'
      - 'tests/**'
```

**Información importante disponible:**

```yaml
github.event.action                           # Tipo de acción
github.event.number                           # Número del PR
github.event.pull_request.draft               # true/false ⭐
github.event.pull_request.merged              # true/false
github.event.pull_request.state               # open/closed
github.event.pull_request.title               # Título
github.event.pull_request.body                # Descripción
github.event.pull_request.user.login          # Autor
github.event.pull_request.head.ref            # Rama origen
github.event.pull_request.base.ref            # Rama destino
github.event.pull_request.labels              # Array de labels
github.event.pull_request.assignees           # Array de asignados
github.event.pull_request.requested_reviewers # Array de revisores
```

**Ejemplo con draft:**
```yaml
name: PR Checks

on:
  pull_request:
    types: [opened, synchronize, ready_for_review, converted_to_draft]

jobs:
  # Solo ejecutar si NO es draft
  full-tests:
    runs-on: ubuntu-latest
    if: github.event.pull_request.draft == false
    steps:
      - name: Tests completos
        run: npm test

  # Solo ejecutar si ES draft
  draft-check:
    runs-on: ubuntu-latest
    if: github.event.pull_request.draft == true
    steps:
      - name: Validación básica
        run: npm run lint

  # Ejecutar cuando sale de draft
  ready:
    runs-on: ubuntu-latest
    if: github.event.action == 'ready_for_review'
    steps:
      - name: Notificar
        run: echo "PR #${{ github.event.number }} listo para revisión"
```

**Ejemplo con labels:**
```yaml
name: Label Actions

on:
  pull_request:
    types: [labeled, unlabeled]

jobs:
  deploy-preview:
    if: contains(github.event.pull_request.labels.*.name, 'preview')
    runs-on: ubuntu-latest
    steps:
      - name: Desplegar preview
        run: echo "Desplegando preview..."

  urgent:
    if: contains(github.event.pull_request.labels.*.name, 'urgent')
    runs-on: ubuntu-latest
    steps:
      - name: Notificación urgente
        run: echo "⚠️ PR urgente detectado"
```

---

### `pull_request_target`

Cuando un PR viene de un **fork** (un repositorio ajeno), el evento `pull_request` ejecuta el workflow usando el código de la rama del PR — código externo que no controlas. Por seguridad, GitHub le da permisos muy reducidos y sin acceso a los secrets del repositorio.

`pull_request_target` resuelve el caso en que necesitas acceso a secrets (por ejemplo, para comentar en el PR usando el `GITHUB_TOKEN` con permisos de escritura), pero **a cambio cambia el contexto de ejecución**: el workflow corre usando el código de la **rama destino (base)**, no el del PR. Así el código externo del fork no puede ejecutarse con tus credenciales.

| | `pull_request` | `pull_request_target` |
|---|---|---|
| Código que se ejecuta | Rama del PR (head) | Rama destino (base) |
| Acceso a secrets | ❌ No (en forks) | ✅ Sí ⚠️ |
| GITHUB_TOKEN | Solo lectura (forks) | Escritura ⚠️ |

**⚠️ IMPORTANTE:** Tiene acceso a secretos incluso si el PR viene de un fork. Por eso **nunca hagas checkout del código del PR** en este evento sin validación explícita — ver `SEGURIDAD_AVANZADA.md`.

**Uso seguro:**
```yaml
on:
  pull_request_target:
    types: [opened, synchronize]

jobs:
  comment:
    runs-on: ubuntu-latest
    steps:
      # NO hacer checkout del código del PR sin validación
      - name: Comentar en PR
        uses: actions/github-script@v7
        with:
          script: |
            github.rest.issues.createComment({
              issue_number: context.issue.number,
              owner: context.repo.owner,
              repo: context.repo.repo,
              body: '¡Gracias por tu contribución!'
            })
```

---

### `create`

Se dispara cuando se crea una rama o tag.

```yaml
on: create

jobs:
  notify:
    runs-on: ubuntu-latest
    steps:
      - name: Notificar creación
        run: |
          echo "Tipo: ${{ github.ref_type }}"
          echo "Nombre: ${{ github.ref_name }}"
```

---

### `delete`

Se dispara cuando se elimina una rama o tag.

```yaml
on: delete

jobs:
  cleanup:
    runs-on: ubuntu-latest
    steps:
      - name: Limpiar recursos
        run: echo "Limpiando ${{ github.ref_name }}"
```

---

## Eventos de Issues y PRs

### `issues`

Se dispara en eventos de issues.

```yaml
on:
  issues:
    types:
      - opened              # Issue abierto
      - edited              # Editado
      - deleted             # Eliminado
      - transferred         # Transferido a otro repo
      - pinned              # Fijado
      - unpinned            # Desfijado
      - closed              # Cerrado
      - reopened            # Reabierto
      - assigned            # Asignado
      - unassigned          # Desasignado
      - labeled             # Etiqueta añadida
      - unlabeled           # Etiqueta removida
      - locked              # Bloqueado
      - unlocked            # Desbloqueado
      - milestoned          # Milestone añadido
      - demilestoned        # Milestone removido
```

**Ejemplo:**
```yaml
name: Auto Label Issues

on:
  issues:
    types: [opened]

jobs:
  label:
    runs-on: ubuntu-latest
    steps:
      - name: Añadir etiqueta
        uses: actions/github-script@v7
        with:
          script: |
            const title = context.payload.issue.title.toLowerCase();
            let labels = [];
            
            if (title.includes('bug')) labels.push('bug');
            if (title.includes('feature')) labels.push('enhancement');
            
            if (labels.length > 0) {
              github.rest.issues.addLabels({
                issue_number: context.issue.number,
                owner: context.repo.owner,
                repo: context.repo.repo,
                labels: labels
              });
            }
```

---

### `issue_comment`

Se dispara cuando se comenta en issue o PR.

```yaml
on:
  issue_comment:
    types:
      - created             # Comentario creado
      - edited              # Comentario editado
      - deleted             # Comentario eliminado
```

**Información disponible:**
```yaml
github.event.action                    # created, edited, deleted
github.event.issue.number              # Número del issue/PR
github.event.issue.pull_request        # Objeto si es PR, undefined si es issue
github.event.comment.body              # Contenido del comentario
github.event.comment.user.login        # Autor del comentario
```

**Ejemplo - Bot de comandos:**
```yaml
name: Bot de Comandos

on:
  issue_comment:
    types: [created]

jobs:
  bot:
    # Solo ejecutar si el comentario empieza con /command
    if: startsWith(github.event.comment.body, '/command')
    runs-on: ubuntu-latest
    steps:
      - name: Procesar comando
        run: |
          COMMENT="${{ github.event.comment.body }}"
          echo "Comando recibido: $COMMENT"
          
          if [[ "$COMMENT" == "/deploy"* ]]; then
            echo "Desplegando..."
          elif [[ "$COMMENT" == "/test"* ]]; then
            echo "Ejecutando tests..."
          fi
```

---

### `pull_request_review`

Se dispara cuando se envía una revisión de PR.

```yaml
on:
  pull_request_review:
    types:
      - submitted           # Revisión enviada
      - edited              # Revisión editada
      - dismissed           # Revisión descartada
```

**Información disponible:**
```yaml
github.event.review.state              # approved, changes_requested, commented
github.event.review.body               # Comentario de la revisión
github.event.review.user.login         # Revisor
```

---

### `pull_request_review_comment`

Se dispara en comentarios de revisión de código.

```yaml
on:
  pull_request_review_comment:
    types:
      - created
      - edited
      - deleted
```

---

## Eventos de Releases y Tags

### `release`

Se dispara en eventos de releases.

```yaml
on:
  release:
    types:
      - published           # Release publicado
      - unpublished         # Release no publicado
      - created             # Release creado (puede ser draft)
      - edited              # Release editado
      - deleted             # Release eliminado
      - prereleased         # Pre-release publicado
      - released            # Pre-release convertido a release
```

**Información disponible:**
```yaml
github.event.release.tag_name          # v1.0.0
github.event.release.name              # Nombre del release
github.event.release.body              # Release notes
github.event.release.draft             # true/false
github.event.release.prerelease        # true/false
github.event.release.html_url          # URL del release
```

**Ejemplo:**
```yaml
name: Publicar en NPM

on:
  release:
    types: [published]

jobs:
  publish:
    # Solo releases NO pre-release
    if: github.event.release.prerelease == false
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Publicar
        run: |
          echo "Publicando versión ${{ github.event.release.tag_name }}"
          npm publish
        env:
          NODE_AUTH_TOKEN: ${{ secrets.NPM_TOKEN }}
```

---

### `workflow_run`

Cada archivo `.yml` en `.github/workflows/` es un workflow independiente. El mecanismo `needs:` solo funciona para encadenar **jobs dentro del mismo workflow**, no entre workflows distintos. `workflow_run` cubre ese caso: permite que un workflow se dispare cuando **otro workflow completo termina**.

El uso más habitual es el patrón CI seguro para forks: un primer workflow ejecuta los tests con el código del fork (sin secrets), y cuando termina, `workflow_run` dispara un segundo workflow que tiene acceso a secrets y puede comentar el resultado en el PR, sin haber ejecutado código externo.

```
workflow_run vs needs:
  needs:        → encadena jobs del MISMO workflow
  workflow_run  → encadena WORKFLOWS distintos (archivos .yml distintos)
```

Se dispara cuando otro workflow se completa.

```yaml
on:
  workflow_run:
    workflows:
      - "CI"                # Nombre del workflow
      - "Build"
    types:
      - completed           # Workflow completado
      - requested           # Workflow solicitado
      - in_progress         # Workflow en progreso
    branches:
      - main
```

**Ejemplo:**
```yaml
name: Deploy después de CI

on:
  workflow_run:
    workflows: ["CI"]
    types: [completed]
    branches: [main]

jobs:
  deploy:
    # Solo si el workflow anterior fue exitoso
    if: github.event.workflow_run.conclusion == 'success'
    runs-on: ubuntu-latest
    steps:
      - name: Deploy
        run: echo "Desplegando..."
```

---

## Eventos de Colaboración

### `fork`

Se dispara cuando alguien hace fork del repositorio.

```yaml
on: fork

jobs:
  thank:
    runs-on: ubuntu-latest
    steps:
      - name: Agradecer
        run: echo "¡Gracias por el fork!"
```

---

### `star` / `watch`

Se dispara cuando alguien da estrella o watch al repo.

```yaml
on:
  star:
    types:
      - created             # Estrella añadida
      - deleted             # Estrella removida

jobs:
  notify:
    runs-on: ubuntu-latest
    steps:
      - name: Notificar
        run: echo "Nueva estrella de ${{ github.event.sender.login }}"
```

---

### `member`

Se dispara cuando se añade o modifica un colaborador.

```yaml
on:
  member:
    types:
      - added               # Colaborador añadido
      - removed             # Colaborador removido
      - edited              # Permisos modificados
```

---

## Eventos Programados

### `schedule`

Se ejecuta según un horario (formato cron).

```yaml
on:
  schedule:
    - cron: '0 0 * * *'     # Diario a medianoche UTC
    - cron: '*/15 * * * *'  # Cada 15 minutos
```

**Formato cron:**
```
┌───────────── minuto (0-59)
│ ┌───────────── hora (0-23)
│ │ ┌───────────── día del mes (1-31)
│ │ │ ┌───────────── mes (1-12)
│ │ │ │ ┌───────────── día de la semana (0-6, 0=domingo)
│ │ │ │ │
* * * * *
```

**Ejemplos comunes:**
```yaml
'0 */6 * * *'      # Cada 6 horas
'0 0 * * 0'        # Todos los domingos a medianoche
'0 9 * * 1-5'      # Días laborables a las 9am
'0 0 1 * *'        # Primer día de cada mes
'0 0 1 1 *'        # Año nuevo
'*/30 * * * *'     # Cada 30 minutos
```

**⚠️ Limitaciones:**
- Horario en UTC
- Mínimo cada 5 minutos
- Puede retrasarse en repos muy activos
- No se ejecuta si el repo está inactivo >60 días

**Ejemplo:**
```yaml
name: Cleanup Diario

on:
  schedule:
    - cron: '0 2 * * *'  # 2am UTC diario

jobs:
  cleanup:
    runs-on: ubuntu-latest
    steps:
      - name: Limpiar artefactos antiguos
        uses: actions/github-script@v7
        with:
          script: |
            const artifacts = await github.rest.actions.listArtifactsForRepo({
              owner: context.repo.owner,
              repo: context.repo.repo
            });
            
            const old = artifacts.data.artifacts.filter(a => {
              const age = Date.now() - new Date(a.created_at).getTime();
              return age > 30 * 24 * 60 * 60 * 1000; // >30 días
            });
            
            for (const artifact of old) {
              await github.rest.actions.deleteArtifact({
                owner: context.repo.owner,
                repo: context.repo.repo,
                artifact_id: artifact.id
              });
            }
```

---

## Eventos Manuales

### `workflow_dispatch`

Permite lanzar un workflow **manualmente** desde la interfaz de GitHub (pestaña Actions → seleccionar workflow → botón "Run workflow"), o desde la API REST de GitHub. Es el equivalente a tener un botón de "ejecutar" en la UI.

Los `inputs` definen un formulario que aparece en esa pantalla: el usuario puede elegir valores antes de lanzar la ejecución. Son opcionales — si no se definen, simplemente se puede lanzar el workflow sin parámetros.

Permite ejecutar workflows manualmente desde GitHub UI.

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
        default: 'development'
      
      version:
        description: 'Versión a desplegar'
        required: true
        type: string
        default: 'latest'
      
      debug:
        description: 'Habilitar modo debug'
        required: false
        type: boolean
        default: false
      
      region:
        description: 'Región de despliegue'
        required: false
        type: choice
        options:
          - us-east-1
          - eu-west-1
          - ap-southeast-1
```

**Tipos de inputs:**
- `string` - Texto libre
- `choice` - Lista de opciones
- `boolean` - true/false
- `environment` - Ambiente de GitHub

**Acceso a inputs:**
```yaml
jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - name: Deploy
        run: |
          echo "Ambiente: ${{ inputs.environment }}"
          echo "Versión: ${{ inputs.version }}"
          echo "Debug: ${{ inputs.debug }}"
          echo "Región: ${{ inputs.region }}"
      
      - name: Debug info
        if: inputs.debug == true
        run: echo "Modo debug habilitado"
```

---

### `repository_dispatch`

Permite disparar un workflow desde **fuera de GitHub** mediante una llamada HTTP a la API REST. Es útil para integraciones con sistemas externos (Jenkins, un webhook de tu servidor, un script de CI/CD externo) que necesiten desencadenar un workflow de GitHub.

El `event_type` es un identificador personalizado que tú defines — no es un evento predefinido de GitHub, sino un nombre libre que acuerdas entre el llamador externo y el workflow. El `client_payload` es un objeto JSON libre que puedes usar para pasar datos al workflow.

Permite disparar workflows desde la API de GitHub.

```yaml
on:
  repository_dispatch:
    types:
      - webhook               # Tipos personalizados
      - deployment
```

**Disparar desde API:**
```bash
curl -X POST \
  -H "Accept: application/vnd.github+json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  https://api.github.com/repos/OWNER/REPO/dispatches \
  -d '{"event_type":"webhook","client_payload":{"key":"value"}}'
```

**Acceso al payload:**
```yaml
jobs:
  process:
    runs-on: ubuntu-latest
    steps:
      - name: Procesar
        run: |
          echo "Tipo: ${{ github.event.action }}"
          echo "Payload: ${{ toJson(github.event.client_payload) }}"
```

---

## Eventos de Workflows

### `workflow_call`

Hace que un workflow sea **reutilizable**: otro workflow puede llamarlo como si fuera una función, pasándole inputs y recibiendo outputs. El workflow con `workflow_call` no se puede disparar directamente por un evento de GitHub — solo puede ser invocado por otro workflow con `uses:` a nivel de job.

Ver `WORKFLOWS_REUTILIZABLES.md` para la documentación completa de inputs, outputs, secrets y limitaciones.

Para workflows reutilizables.

```yaml
# workflow-reutilizable.yml
on:
  workflow_call:
    inputs:
      username:
        required: true
        type: string
      environment:
        required: true
        type: string
    secrets:
      token:
        required: true
    outputs:
      result:
        description: "Resultado del workflow"
        value: ${{ jobs.build.outputs.result }}

jobs:
  build:
    runs-on: ubuntu-latest
    outputs:
      result: ${{ steps.build.outputs.result }}
    steps:
      - name: Build
        id: build
        run: |
          echo "result=success" >> $GITHUB_OUTPUT
```

**Llamar al workflow:**
```yaml
# main-workflow.yml
jobs:
  call-reusable:
    uses: ./.github/workflows/workflow-reutilizable.yml
    with:
      username: "admin"
      environment: "production"
    secrets:
      token: ${{ secrets.MY_TOKEN }}
```

---

## Filtros y Opciones Avanzadas

### Activity types

Especificar qué acciones disparar:

```yaml
on:
  pull_request:
    types:
      - opened
      - synchronize
      - ready_for_review
```

### Branches

Filtrar por ramas:

```yaml
on:
  push:
    branches:
      - main
      - 'releases/**'       # Wildcard
      - '!releases/alpha'   # Excluir
```

### Branches-ignore

Ignorar ramas:

```yaml
on:
  push:
    branches-ignore:
      - 'docs/**'
      - 'experimental'
```

**⚠️ No se puede usar `branches` y `branches-ignore` juntos.**

### Tags

Filtrar por tags:

```yaml
on:
  push:
    tags:
      - v1.*
      - v2.*
```

### Paths

Filtrar por archivos modificados:

```yaml
on:
  push:
    paths:
      - 'src/**'
      - '**.js'
      - '!**.md'            # Excluir
```

### Paths-ignore

Ignorar archivos:

```yaml
on:
  push:
    paths-ignore:
      - 'docs/**'
      - '**.md'
```

---

## Combinación de Eventos

### Múltiples eventos

```yaml
on:
  push:
    branches:
      - main
  pull_request:
    branches:
      - main
  schedule:
    - cron: '0 0 * * *'
  workflow_dispatch:
```

### Eventos con diferentes configuraciones

```yaml
on:
  push:
    branches:
      - main
    paths:
      - 'src/**'
  
  pull_request:
    branches:
      - main
      - develop
    types:
      - opened
      - synchronize
  
  release:
    types:
      - published
```

### Condicionales complejos

```yaml
jobs:
  build:
    runs-on: ubuntu-latest
    # Ejecutar solo si:
    # - Es push a main, O
    # - Es PR NO draft hacia main
    if: |
      (github.event_name == 'push' && github.ref == 'refs/heads/main') ||
      (github.event_name == 'pull_request' && 
       github.event.pull_request.draft == false &&
       github.event.pull_request.base.ref == 'main')
    steps:
      - run: echo "Ejecutando build"
```

---

## Tabla de Referencia Rápida

| Evento | Cuándo se dispara | Uso común |
|--------|-------------------|-----------|
| `push` | Push a repositorio | CI/CD |
| `pull_request` | Eventos de PR | Tests, linting |
| `pull_request_target` | PR (contexto base) | Comentarios seguros |
| `issues` | Eventos de issues | Automatización |
| `issue_comment` | Comentarios | Bots de comandos |
| `release` | Publicación de release | Deploy a producción |
| `schedule` | Horario programado | Tareas periódicas |
| `workflow_dispatch` | Manual | Deploy on-demand |
| `repository_dispatch` | API externa | Webhooks |
| `workflow_run` | Otro workflow termina | Pipeline en cadena |

---

*Documentación completa de eventos y triggers en GitHub Actions*
*Última actualización: Enero 2026*

