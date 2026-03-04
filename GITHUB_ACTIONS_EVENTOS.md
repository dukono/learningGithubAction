# 🎯 GitHub Actions: Eventos y Triggers Completos

## 📚 Índice
1. [Eventos de Código](#eventos-de-código)
2. [Eventos de Issues y PRs](#eventos-de-issues-y-prs)
3. [Eventos de Releases y Tags](#eventos-de-releases-y-tags)
4. [Eventos de Colaboración](#eventos-de-colaboración)
5. [Eventos Programados](#eventos-programados)
6. [Eventos Manuales](#eventos-manuales)
7. [Eventos de Workflows](#eventos-de-workflows)
8. [Filtros y Opciones](#filtros-y-opciones)
9. [Combinación de Eventos](#combinación-de-eventos)

---

## 📝 Eventos de Código

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

**Tipos de actividad:**
```yaml
on:
  pull_request:
    types:
      - opened              # PR abierto
      - edited              # Título o descripción editados
      - closed              # PR cerrado (merged o no)
      - reopened            # PR reabierto
      - synchronize         # Nuevos commits añadidos
      - assigned            # Alguien asignado
      - unassigned          # Alguien desasignado
      - labeled             # Etiqueta añadida
      - unlabeled           # Etiqueta removida
      - locked              # Conversación bloqueada
      - unlocked            # Conversación desbloqueada
      - review_requested    # Revisión solicitada
      - review_request_removed # Revisión removida
      - ready_for_review    # Marcado como listo (saliendo de draft)
      - converted_to_draft  # Convertido a draft
      - auto_merge_enabled  # Auto-merge habilitado
      - auto_merge_disabled # Auto-merge deshabilitado
```

**Por defecto (sin `types`):**
Se dispara en: `opened`, `synchronize`, `reopened`

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

Similar a `pull_request` pero se ejecuta en el contexto de la rama base (no la del PR).

**⚠️ IMPORTANTE:** Tiene acceso a secretos incluso si el PR viene de un fork.

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

## 🎫 Eventos de Issues y PRs

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

## 🏷️ Eventos de Releases y Tags

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

## 👥 Eventos de Colaboración

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

## ⏰ Eventos Programados

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

## 🎛️ Eventos Manuales

### `workflow_dispatch`

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

## 🔗 Eventos de Workflows

### `workflow_call`

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

## 🎚️ Filtros y Opciones Avanzadas

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

## 🔀 Combinación de Eventos

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

## 📊 Tabla de Referencia Rápida

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

