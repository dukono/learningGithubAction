# GitHub Actions — Contextos

Cada contexto es un objeto que GitHub inyecta en cada ejecución. Se accede con `${{ contexto.campo }}`.

> Despliega cada sección para ver todos los campos disponibles.

**¿Están todos los campos?**
Los contextos `env`, `job`, `steps`, `runner`, `secrets`, `vars`, `strategy`, `matrix`, `needs`, `inputs` y `jobs` están **completos** — tienen todos sus campos posibles.
El contexto `github` (propiedades generales) está **completo**.
Para `github.event`, se documentan todos los campos **relevantes** de cada evento. Los payloads de webhook de GitHub tienen decenas de campos adicionales de bajo nivel (`node_id`, URLs internas de la API, campos de billing, etc.) que no se usan en workflows y se omiten deliberadamente.

---

## Índice

- [github — propiedades generales](#github--propiedades-generales)
- [github.event según el evento](#githubEvent-según-el-evento)
  - [Campos comunes a casi todos los eventos](#campos-comunes-a-casi-todos-los-eventos): `repository`, `sender`, `organization`, `installation`
  - [push](#push) · [create](#create) · [delete](#delete)
  - [pull\_request y pull\_request\_target](#pull_request-y-pull_request_target)
  - [pull\_request\_review](#pull_request_review)
  - [pull\_request\_review\_comment](#pull_request_review_comment)
  - [issues](#issues)
  - [issue\_comment](#issue_comment)
  - [discussion](#discussion)
  - [discussion\_comment](#discussion_comment)
  - [release](#release)
  - [registry\_package](#registry_package)
  - [check\_run](#check_run)
  - [check\_suite](#check_suite)
  - [deployment](#deployment)
  - [deployment\_status](#deployment_status)
  - [workflow\_run](#workflow_run)
  - [workflow\_dispatch](#workflow_dispatch)
  - [repository\_dispatch](#repository_dispatch)
  - [schedule](#schedule)
  - [fork](#fork)
  - [watch](#watch)
  - [milestone](#milestone)
  - [page\_build](#page_build)
- [env](#env)
- [job](#job)
- [steps](#steps)
- [runner](#runner)
- [secrets](#secrets)
- [vars](#vars)
- [strategy y matrix](#strategy-y-matrix)
- [needs](#needs)
- [inputs](#inputs)
- [jobs — solo workflows reutilizables](#jobs--solo-workflows-reutilizables)
- [Arrays y operador .*](#arrays-y-operador-)
- [Funciones](#funciones)

---

## `github` — propiedades generales

Disponibles en **todos** los eventos, independientemente del trigger.

<details>
<summary>Ver campos</summary>

| Campo | Ejemplo | Descripción |
|---|---|---|
| `github.actor` | `"dukono"` | Usuario que disparó el workflow |
| `github.actor_id` | `"12345678"` | ID numérico del usuario |
| `github.triggering_actor` | `"dukono"` | Quien disparó **esta ejecución concreta** — difiere de `actor` si alguien pulsó re-run |
| `github.token` | `"ghs_xxxxx"` | Token automático, equivale a `secrets.GITHUB_TOKEN` |
| `github.repository` | `"dukono/my-app"` | Nombre completo del repo |
| `github.repository_id` | `"987654321"` | ID numérico del repo |
| `github.repository_owner` | `"dukono"` | Solo el propietario (organización o usuario) |
| `github.repository_owner_id` | `"12345678"` | ID numérico del propietario |
| `github.ref` | `"refs/heads/main"` | Ref completa. En PR vale `refs/pull/11/merge` (rama virtual), no `refs/heads/feature` |
| `github.ref_name` | `"main"` | Nombre corto: rama, tag o `"11/merge"` si es PR |
| `github.ref_type` | `"branch"` | `"branch"` o `"tag"` |
| `github.ref_protected` | `false` | `true` si la rama está protegida en GitHub |
| `github.head_ref` | `"feature/oauth"` | ⚠️ Solo en `pull_request` — nombre de la rama **origen** del PR |
| `github.base_ref` | `"main"` | ⚠️ Solo en `pull_request` — nombre de la rama **destino** del PR |
| `github.sha` | `"a1b2c3d4..."` | SHA completo del commit que disparó el evento |
| `github.event_name` | `"push"` | Nombre del evento: `push`, `pull_request`, `release`, `schedule`, ... |
| `github.workflow` | `"CI Pipeline"` | El campo `name:` del archivo YAML del workflow |
| `github.workflow_ref` | `"dukono/my-app/.github/workflows/ci.yml@refs/heads/main"` | Ruta completa al archivo del workflow |
| `github.workflow_sha` | `"a1b2c3d4..."` | SHA del archivo del workflow |
| `github.job` | `"build"` | El `id:` del job actual en el YAML |
| `github.run_id` | `"9876543210"` | ID único global de esta ejecución del workflow |
| `github.run_number` | `"42"` | Número secuencial de ejecuciones de este workflow en el repo |
| `github.run_attempt` | `"1"` | Número de reintento — `"2"` si se pulsó re-run |
| `github.secret_source` | `"Actions"` | Origen de los secrets: `"Actions"`, `"Dependabot"`, `"None"` |
| `github.retention_days` | `"90"` | Días que se guardan los artifacts y logs |
| `github.server_url` | `"https://github.com"` | URL base de GitHub |
| `github.api_url` | `"https://api.github.com"` | URL de la API REST |
| `github.graphql_url` | `"https://api.github.com/graphql"` | URL de la API GraphQL |
| `github.repositoryUrl` | `"git://github.com/dukono/my-app.git"` | URL git del repo |
| `github.workspace` | `"/home/runner/work/my-app/my-app"` | Directorio donde `actions/checkout` clona el repo |
| `github.action` | `"__run"` | ID del step actual. En actions es el nombre de la action (`actions/checkout`) |
| `github.action_path` | `"/home/runner/work/_actions/actions/checkout/v4"` | Ruta donde está instalada la action actual |
| `github.action_ref` | `"v4"` | Versión/ref de la action actual |
| `github.action_repository` | `"actions/checkout"` | Repositorio de la action actual |
| `github.env` | `"/home/runner/work/_temp/set_env_abc"` | Path al archivo `$GITHUB_ENV` en el runner |
| `github.output` | `"/home/runner/work/_temp/set_output_abc"` | Path al archivo `$GITHUB_OUTPUT` en el runner |
| `github.path` | `"/home/runner/work/_temp/add_path_abc"` | Path al archivo `$GITHUB_PATH` en el runner |
| `github.event_path` | `"/home/runner/work/_temp/event.json"` | Path al JSON completo del payload del evento |
| `github.job_workflow_sha` | `"a1b2c3d4..."` | SHA del archivo del workflow para el job actual |

</details>

---

## `github.event` según el evento

El campo `github.event` contiene el payload completo del webhook. Su estructura cambia según el evento.

---

### Campos comunes a casi todos los eventos

Estos objetos aparecen en el payload de **casi todos los eventos** (push, pull_request, issues, release, etc.). No se repiten en cada sección individual para no duplicar.

<details>
<summary>Ver github.event.repository — el repositorio donde ocurrió el evento</summary>

| Campo | Ejemplo | Descripción |
|---|---|---|
| `github.event.repository.id` | `987654321` | ID numérico del repo |
| `github.event.repository.name` | `"my-app"` | Nombre del repo (sin el owner) |
| `github.event.repository.full_name` | `"dukono/my-app"` | Nombre completo `owner/repo` |
| `github.event.repository.private` | `false` | Si el repo es privado |
| `github.event.repository.fork` | `false` | Si el repo es un fork |
| `github.event.repository.archived` | `false` | Si el repo está archivado |
| `github.event.repository.disabled` | `false` | Si el repo está deshabilitado |
| `github.event.repository.visibility` | `"public"` | `"public"`, `"private"`, `"internal"` |
| `github.event.repository.default_branch` | `"main"` | Rama principal |
| `github.event.repository.description` | `"Mi aplicación"` | Descripción del repo (`null` si vacío) |
| `github.event.repository.language` | `"TypeScript"` | Lenguaje principal detectado |
| `github.event.repository.topics` | `["docker","ci"]` | Array de tópicos del repo |
| `github.event.repository.homepage` | `"https://example.com"` | URL de la homepage (`null` si no tiene) |
| `github.event.repository.size` | `1024` | Tamaño en KB |
| `github.event.repository.stargazers_count` | `42` | Número de estrellas |
| `github.event.repository.watchers_count` | `42` | Número de watchers |
| `github.event.repository.forks_count` | `8` | Número de forks |
| `github.event.repository.open_issues_count` | `5` | Issues abiertos |
| `github.event.repository.allow_forking` | `true` | Si se permite hacer fork |
| `github.event.repository.is_template` | `false` | Si el repo es una plantilla |
| `github.event.repository.has_issues` | `true` | Si el repo tiene issues activos |
| `github.event.repository.has_projects` | `true` | Si el repo tiene projects activos |
| `github.event.repository.has_wiki` | `true` | Si el repo tiene wiki activo |
| `github.event.repository.has_pages` | `false` | Si el repo tiene GitHub Pages activo |
| `github.event.repository.has_downloads` | `true` | Si el repo tiene descargas activas |
| `github.event.repository.has_discussions` | `false` | Si el repo tiene Discussions activo |
| `github.event.repository.owner.login` | `"dukono"` | Username del propietario |
| `github.event.repository.owner.id` | `12345678` | ID del propietario |
| `github.event.repository.owner.type` | `"User"` | `"User"` u `"Organization"` |
| `github.event.repository.owner.site_admin` | `false` | Si el propietario es admin de GitHub |
| `github.event.repository.html_url` | `"https://github.com/dukono/my-app"` | URL web del repo |
| `github.event.repository.clone_url` | `"https://github.com/dukono/my-app.git"` | URL HTTPS para clonar |
| `github.event.repository.ssh_url` | `"git@github.com:dukono/my-app.git"` | URL SSH para clonar |
| `github.event.repository.git_url` | `"git://github.com/dukono/my-app.git"` | URL git del repo |
| `github.event.repository.svn_url` | `"https://github.com/dukono/my-app"` | URL SVN del repo |
| `github.event.repository.url` | `"https://api.github.com/repos/dukono/my-app"` | URL de la API |
| `github.event.repository.forks_url` | `"https://api.github.com/repos/dukono/my-app/forks"` | URL de la API de forks |
| `github.event.repository.created_at` | `1609459200` | Timestamp Unix de creación (o ISO 8601 según el evento) |
| `github.event.repository.updated_at` | `"2026-04-22T10:00:00Z"` | Última actualización |
| `github.event.repository.pushed_at` | `"2026-04-22T10:30:00Z"` | Último push |
| `github.event.repository.license` | `null` | Licencia del repo (`null` si no tiene) |
| `github.event.repository.license.key` | `"mit"` | Clave de la licencia |
| `github.event.repository.license.name` | `"MIT License"` | Nombre de la licencia |

</details>

<details>
<summary>Ver github.event.sender — usuario que realizó la acción</summary>

El objeto `sender` está presente en casi todos los eventos e identifica al usuario (o bot) que realizó la acción que disparó el evento.

| Campo | Ejemplo | Descripción |
|---|---|---|
| `github.event.sender.login` | `"dukono"` | Username |
| `github.event.sender.id` | `12345678` | ID numérico |
| `github.event.sender.type` | `"User"` | `"User"` o `"Bot"` |
| `github.event.sender.site_admin` | `false` | Si es administrador de GitHub |
| `github.event.sender.avatar_url` | `"https://avatars.githubusercontent.com/u/12345678"` | URL del avatar |
| `github.event.sender.html_url` | `"https://github.com/dukono"` | URL del perfil |
| `github.event.sender.url` | `"https://api.github.com/users/dukono"` | URL de la API del usuario |

> `sender` y `github.actor` normalmente tienen el mismo valor, pero pueden diferir si una GitHub App actúa en nombre de un usuario.

</details>

<details>
<summary>Ver github.event.organization — solo en repos de organización</summary>

Solo existe cuando el repo pertenece a una **organización** (no a un usuario personal).

| Campo | Ejemplo | Descripción |
|---|---|---|
| `github.event.organization.login` | `"mi-empresa"` | Nombre de la organización |
| `github.event.organization.id` | `99887766` | ID numérico de la organización |
| `github.event.organization.description` | `"Empresa de software"` | Descripción (`null` si vacío) |
| `github.event.organization.url` | `"https://api.github.com/orgs/mi-empresa"` | URL de la API |
| `github.event.organization.repos_url` | `"https://api.github.com/orgs/mi-empresa/repos"` | URL de repos de la org |
| `github.event.organization.events_url` | `"https://api.github.com/orgs/mi-empresa/events"` | URL de eventos de la org |
| `github.event.organization.members_url` | `"https://api.github.com/orgs/mi-empresa/members{/member}"` | URL de miembros |
| `github.event.organization.avatar_url` | `"https://avatars.githubusercontent.com/u/99887766"` | URL del avatar de la org |
| `github.event.organization.hooks_url` | `"https://api.github.com/orgs/mi-empresa/hooks"` | URL de webhooks de la org |

</details>

<details>
<summary>Ver github.event.installation — solo cuando lo dispara una GitHub App</summary>

Solo existe cuando el evento es disparado por una **GitHub App instalada**, no por un usuario directo.

| Campo | Ejemplo | Descripción |
|---|---|---|
| `github.event.installation.id` | `12345678` | ID de la instalación de la app |
| `github.event.installation.node_id` | `"MDIzOkludGVncmF0aW9uSW5zdGFsbGF0aW9u..."` | Node ID (para la API GraphQL) |

</details>

---

### `push`

<details>
<summary>Ver campos de github.event en push</summary>

| Campo | Ejemplo | Descripción |
|---|---|---|
| `github.event.ref` | `"refs/heads/main"` | Rama o tag que recibió el push |
| `github.event.before` | `"83108f7a..."` | SHA del commit anterior (el HEAD antes del push) |
| `github.event.after` | `"9cf3dea4..."` | SHA del commit nuevo (el HEAD después del push) |
| `github.event.compare` | `"https://github.com/.../compare/83108f7...9cf3dea"` | URL para ver la diferencia en GitHub |
| `github.event.created` | `false` | `true` si este push **creó** la rama |
| `github.event.deleted` | `false` | `true` si este push **eliminó** la rama |
| `github.event.forced` | `false` | `true` si fue un `git push --force` |
| `github.event.pusher.name` | `"dukono"` | Username de quien hizo el push |
| `github.event.pusher.email` | `"dukono@example.com"` | Email de quien hizo el push |
| `github.event.commits` | `[...]` | Array con todos los commits incluidos en el push |
| `github.event.commits[0].id` | `"a1b2c3d4..."` | SHA del commit |
| `github.event.commits[0].message` | `"fix: bug en login"` | Mensaje del commit |
| `github.event.commits[0].timestamp` | `"2026-04-22T10:30:00Z"` | Fecha y hora |
| `github.event.commits[0].author.name` | `"Vitaly Neborachok"` | Nombre del autor |
| `github.event.commits[0].author.email` | `"vitaly@example.com"` | Email del autor |
| `github.event.commits[0].author.username` | `"dukono"` | Username de GitHub del autor |
| `github.event.commits[0].url` | `"https://github.com/.../commit/a1b2c3d4"` | URL del commit en GitHub |
| `github.event.commits[0].distinct` | `true` | `true` si el commit es nuevo (no estaba ya en el repo) |
| `github.event.commits[0].added` | `["src/new.ts"]` | Archivos **añadidos** en este commit |
| `github.event.commits[0].modified` | `["src/app.ts"]` | Archivos **modificados** en este commit |
| `github.event.commits[0].removed` | `["src/old.ts"]` | Archivos **eliminados** en este commit |
| `github.event.head_commit.id` | `"9cf3dea4..."` | SHA del último commit del push (el nuevo HEAD) |
| `github.event.head_commit.message` | `"fix: bug en login"` | Mensaje del último commit |
| `github.event.head_commit.author.name` | `"Vitaly Neborachok"` | Autor del último commit |
| `github.event.head_commit.committer.name` | `"GitHub"` | Committer del último commit |
| `github.event.repository.full_name` | `"dukono/my-app"` | Nombre completo del repo |
| `github.event.repository.default_branch` | `"main"` | Rama principal del repo |
| `github.event.repository.private` | `false` | Si el repo es privado |
| `github.event.sender.login` | `"dukono"` | Usuario que hizo el push |

> ⚠️ `before` y `after` **solo existen en `push`**. En `pull_request`, usa `github.event.pull_request.base.sha` y `github.event.pull_request.head.sha`.

</details>

---

### `create`

<details>
<summary>Ver campos de github.event en create</summary>

Se dispara cuando se **crea** una rama o un tag.

| Campo | Ejemplo | Descripción |
|---|---|---|
| `github.event.ref` | `"feature/my-branch"` | Nombre de la rama o tag creado |
| `github.event.ref_type` | `"branch"` | `"branch"` o `"tag"` |
| `github.event.master_branch` | `"main"` | Rama principal del repo |
| `github.event.pusher_type` | `"user"` | `"user"` o `"deploy_key"` |
| `github.event.sender.login` | `"dukono"` | Usuario que creó la rama o tag |
| `github.event.repository.full_name` | `"dukono/my-app"` | Nombre completo del repo |

</details>

---

### `delete`

<details>
<summary>Ver campos de github.event en delete</summary>

Se dispara cuando se **elimina** una rama o un tag.

| Campo | Ejemplo | Descripción |
|---|---|---|
| `github.event.ref` | `"feature/old-branch"` | Nombre de la rama o tag eliminado |
| `github.event.ref_type` | `"branch"` | `"branch"` o `"tag"` |
| `github.event.pusher_type` | `"user"` | `"user"` o `"deploy_key"` |
| `github.event.sender.login` | `"dukono"` | Usuario que eliminó la rama o tag |

</details>

---

### `pull_request` y `pull_request_target`

> **`pull_request`** ejecuta con el código de la **rama origen** (la rama del PR).
> Sin acceso a `secrets` si el PR viene de un fork externo.
>
> **`pull_request_target`** ejecuta con el código de la **rama base** (la rama destino, ej: `main`).
> Tiene acceso a `secrets` aunque el PR sea de un fork externo. Usar con cuidado.

<details>
<summary>Ver campos de github.event en pull_request</summary>

| Campo | Ejemplo | Descripción |
|---|---|---|
| `github.event.action` | `"opened"` | Qué ocurrió: `opened`, `synchronize` (nuevo commit), `closed`, `reopened`, `labeled`, `unlabeled`, `assigned`, `unassigned`, `review_requested`, `review_request_removed`, `ready_for_review`, `converted_to_draft`, `auto_merge_enabled`, `auto_merge_disabled` |
| `github.event.number` | `42` | Número del PR (igual que `pull_request.number`) |
| `github.event.pull_request.number` | `42` | Número del PR |
| `github.event.pull_request.title` | `"feat: OAuth"` | Título del PR |
| `github.event.pull_request.body` | `"Descripción..."` | Cuerpo/descripción del PR (`null` si vacío) |
| `github.event.pull_request.state` | `"open"` | `"open"` o `"closed"` |
| `github.event.pull_request.draft` | `false` | `true` si el PR es un borrador |
| `github.event.pull_request.locked` | `false` | `true` si el PR está bloqueado |
| `github.event.pull_request.merged` | `false` | `true` si el PR ya fue mergeado |
| `github.event.pull_request.mergeable` | `true` | `true`/`false`/`null` — `null` significa que GitHub aún lo está calculando |
| `github.event.pull_request.mergeable_state` | `"clean"` | `clean`, `dirty` (conflictos), `unstable` (checks fallando), `blocked` (requiere aprobación), `behind` (rama desactualizada), `unknown` |
| `github.event.pull_request.rebaseable` | `true` | Si se puede hacer rebase |
| `github.event.pull_request.merged_by` | `null` | Objeto usuario que hizo merge (o `null` si no se ha mergeado) |
| `github.event.pull_request.merged_by.login` | `"dukono"` | Username de quien mergeó |
| `github.event.pull_request.user.login` | `"dukono"` | Autor del PR |
| `github.event.pull_request.user.id` | `12345678` | ID del autor |
| `github.event.pull_request.user.type` | `"User"` | `"User"` o `"Bot"` |
| `github.event.pull_request.author_association` | `"OWNER"` | Relación del autor con el repo: `OWNER`, `MEMBER`, `COLLABORATOR`, `CONTRIBUTOR`, `FIRST_TIME_CONTRIBUTOR`, `FIRST_TIMER`, `NONE` |
| `github.event.pull_request.head.ref` | `"feature/oauth"` | Nombre de la **rama origen** del PR |
| `github.event.pull_request.head.sha` | `"a1b2c3d4..."` | SHA del commit más reciente de la rama origen |
| `github.event.pull_request.head.label` | `"dukono:feature/oauth"` | `"usuario:rama"` — útil para identificar PRs de forks |
| `github.event.pull_request.head.repo.full_name` | `"dukono/my-app"` | Repo de origen (distinto al base si es un fork) |
| `github.event.pull_request.base.ref` | `"main"` | Nombre de la **rama destino** |
| `github.event.pull_request.base.sha` | `"83108f7a..."` | SHA del commit de la rama destino |
| `github.event.pull_request.base.label` | `"dukono:main"` | `"usuario:rama"` de la base |
| `github.event.pull_request.base.repo.full_name` | `"dukono/my-app"` | Repo de destino |
| `github.event.pull_request.labels` | `[{id, name, color, ...}]` | Array de etiquetas asignadas al PR |
| `github.event.pull_request.assignees` | `[{login, id, ...}]` | Array de usuarios asignados |
| `github.event.pull_request.requested_reviewers` | `[{login, id, ...}]` | Array de revisores solicitados |
| `github.event.pull_request.requested_teams` | `[{id, name, slug}]` | Array de equipos revisores solicitados |
| `github.event.pull_request.milestone` | `null` | Milestone asignado (o `null`) |
| `github.event.pull_request.milestone.title` | `"v2.0.0"` | Título del milestone |
| `github.event.pull_request.changed_files` | `7` | **Número entero** de archivos cambiados — no es una lista |
| `github.event.pull_request.additions` | `142` | Líneas añadidas |
| `github.event.pull_request.deletions` | `38` | Líneas eliminadas |
| `github.event.pull_request.commits` | `3` | Número de commits |
| `github.event.pull_request.review_comments` | `2` | Comentarios inline en revisiones |
| `github.event.pull_request.comments` | `1` | Comentarios generales |
| `github.event.pull_request.auto_merge` | `null` | `null` si no está activo |
| `github.event.pull_request.auto_merge.enabled_by.login` | `"dukono"` | Quien activó el auto-merge |
| `github.event.pull_request.auto_merge.merge_method` | `"squash"` | Método: `merge`, `squash`, `rebase` |
| `github.event.pull_request.html_url` | `"https://github.com/.../pull/42"` | URL web del PR |
| `github.event.pull_request.diff_url` | `"https://github.com/.../pull/42.diff"` | URL del diff en texto plano |
| `github.event.pull_request.patch_url` | `"https://github.com/.../pull/42.patch"` | URL del patch |
| `github.event.pull_request.issue_url` | `"https://api.github.com/.../issues/42"` | URL del issue asociado (un PR es un issue) |
| `github.event.pull_request.commits_url` | `"https://api.github.com/.../pulls/42/commits"` | URL de la API para listar commits |
| `github.event.pull_request.review_comments_url` | `"https://api.github.com/.../pulls/42/comments"` | URL de la API para listar comentarios inline |
| `github.event.pull_request.created_at` | `"2026-04-20T09:00:00Z"` | Fecha de creación |
| `github.event.pull_request.updated_at` | `"2026-04-22T10:30:00Z"` | Fecha de última actualización |
| `github.event.pull_request.closed_at` | `null` | Fecha de cierre (`null` si está abierto) |
| `github.event.pull_request.merged_at` | `null` | Fecha de merge (`null` si no se ha mergeado) |
| `github.event.sender.login` | `"dukono"` | Usuario que disparó el evento |

> **`changed_files` no es una lista de archivos** — solo el contador. Para obtener la lista necesitas la API o una action como `tj-actions/changed-files`.

</details>

---

### `pull_request_review`

<details>
<summary>Ver campos de github.event en pull_request_review</summary>

Se dispara cuando alguien **envía una revisión** en un PR (aprueba, pide cambios o comenta).

| Campo | Ejemplo | Descripción |
|---|---|---|
| `github.event.action` | `"submitted"` | `"submitted"`, `"edited"`, `"dismissed"` |
| `github.event.review.id` | `987654321` | ID de la revisión |
| `github.event.review.state` | `"approved"` | `"approved"`, `"changes_requested"`, `"commented"` |
| `github.event.review.body` | `"LGTM 👍"` | Comentario de la revisión (`null` si no hay texto) |
| `github.event.review.commit_id` | `"a1b2c3d4..."` | SHA del commit que se revisó |
| `github.event.review.submitted_at` | `"2026-04-22T10:00:00Z"` | Fecha de envío |
| `github.event.review.user.login` | `"reviewer1"` | Username del revisor |
| `github.event.review.user.id` | `22222222` | ID del revisor |
| `github.event.review.author_association` | `"MEMBER"` | Relación del revisor con el repo |
| `github.event.review.html_url` | `"https://github.com/.../pull/42#pullrequestreview-987654321"` | URL de la revisión |
| `github.event.pull_request.number` | `42` | Número del PR revisado |
| `github.event.pull_request.title` | `"feat: OAuth"` | Título del PR |
| `github.event.pull_request.state` | `"open"` | Estado del PR |
| `github.event.pull_request.user.login` | `"dukono"` | Autor del PR |
| `github.event.pull_request.head.ref` | `"feature/oauth"` | Rama origen del PR |
| `github.event.pull_request.base.ref` | `"main"` | Rama destino del PR |

</details>

---

### `pull_request_review_comment`

<details>
<summary>Ver campos de github.event en pull_request_review_comment</summary>

Se dispara cuando se añade un **comentario inline** sobre una línea de código específica del PR.

| Campo | Ejemplo | Descripción |
|---|---|---|
| `github.event.action` | `"created"` | `"created"`, `"edited"`, `"deleted"` |
| `github.event.comment.id` | `111222333` | ID del comentario |
| `github.event.comment.body` | `"¿Por qué usas var aquí?"` | Texto del comentario |
| `github.event.comment.path` | `"src/auth/login.ts"` | Archivo donde se hizo el comentario |
| `github.event.comment.line` | `42` | Línea del archivo donde está el comentario |
| `github.event.comment.original_line` | `40` | Línea original antes de rebase/force push |
| `github.event.comment.side` | `"RIGHT"` | `"RIGHT"` = línea nueva, `"LEFT"` = línea eliminada |
| `github.event.comment.diff_hunk` | `"@@ -38,6 +40,8 @@..."` | Fragmento del diff donde está el comentario |
| `github.event.comment.pull_request_review_id` | `987654321` | ID de la revisión a la que pertenece |
| `github.event.comment.user.login` | `"reviewer1"` | Autor del comentario |
| `github.event.comment.user.id` | `22222222` | ID del autor |
| `github.event.comment.author_association` | `"MEMBER"` | Relación del autor con el repo |
| `github.event.comment.created_at` | `"2026-04-22T09:30:00Z"` | Fecha de creación |
| `github.event.comment.html_url` | `"https://github.com/.../pull/42#discussion_r111222333"` | URL del comentario |
| `github.event.pull_request.number` | `42` | Número del PR revisado |
| `github.event.pull_request.head.ref` | `"feature/oauth"` | Rama origen del PR |

</details>

---

### `issues`

<details>
<summary>Ver campos de github.event en issues</summary>

| Campo | Ejemplo | Descripción |
|---|---|---|
| `github.event.action` | `"opened"` | Qué ocurrió: `opened`, `closed`, `edited`, `reopened`, `labeled`, `unlabeled`, `assigned`, `unassigned`, `milestoned`, `demilestoned`, `locked`, `unlocked`, `transferred`, `pinned`, `unpinned`, `deleted` |
| `github.event.issue.number` | `99` | Número del issue |
| `github.event.issue.title` | `"Bug en login"` | Título |
| `github.event.issue.body` | `"Pasos para reproducir..."` | Descripción (`null` si vacío) |
| `github.event.issue.state` | `"open"` | `"open"` o `"closed"` |
| `github.event.issue.state_reason` | `"completed"` | Por qué se cerró: `"completed"`, `"not_planned"`, `"reopened"`, `null` |
| `github.event.issue.locked` | `false` | Si el issue está bloqueado |
| `github.event.issue.user.login` | `"dukono"` | Autor del issue |
| `github.event.issue.author_association` | `"OWNER"` | Relación del autor con el repo |
| `github.event.issue.labels` | `[{id, name, color}]` | Array de etiquetas |
| `github.event.issue.assignees` | `[{login, id}]` | Array de usuarios asignados |
| `github.event.issue.milestone` | `null` | Milestone asignado (o `null`) |
| `github.event.issue.milestone.title` | `"v2.0.0"` | Título del milestone |
| `github.event.issue.comments` | `5` | Número de comentarios |
| `github.event.issue.created_at` | `"2026-04-18T08:00:00Z"` | Fecha de creación |
| `github.event.issue.updated_at` | `"2026-04-22T10:00:00Z"` | Última actualización |
| `github.event.issue.closed_at` | `null` | Fecha de cierre (`null` si está abierto) |
| `github.event.issue.html_url` | `"https://github.com/.../issues/99"` | URL del issue |
| `github.event.label.name` | `"bug"` | ⚠️ Solo cuando `action == "labeled"` o `"unlabeled"` — la etiqueta que se añadió/quitó |
| `github.event.assignee.login` | `"user1"` | ⚠️ Solo cuando `action == "assigned"` o `"unassigned"` — el usuario asignado/desasignado |
| `github.event.sender.login` | `"dukono"` | Usuario que realizó la acción |

</details>

---

### `issue_comment`

<details>
<summary>Ver campos de github.event en issue_comment</summary>

Se dispara en comentarios de **issues y también en PRs** (un PR internamente es un issue).

| Campo | Ejemplo | Descripción |
|---|---|---|
| `github.event.action` | `"created"` | `"created"`, `"edited"`, `"deleted"` |
| `github.event.issue.number` | `99` | Número del issue o PR donde se comentó |
| `github.event.issue.title` | `"Bug en login"` | Título del issue o PR |
| `github.event.issue.state` | `"open"` | Estado del issue o PR |
| `github.event.issue.pull_request` | `null` | Si el issue **es** un PR, este campo existe y tiene `html_url`. Si es un issue normal, es `null` |
| `github.event.issue.pull_request.html_url` | `"https://github.com/.../pull/42"` | URL del PR (solo si es un PR) |
| `github.event.comment.id` | `555666777` | ID del comentario |
| `github.event.comment.body` | `"He podido reproducirlo"` | Texto del comentario |
| `github.event.comment.user.login` | `"contributor1"` | Autor del comentario |
| `github.event.comment.author_association` | `"CONTRIBUTOR"` | Relación del autor con el repo |
| `github.event.comment.created_at` | `"2026-04-22T10:15:00Z"` | Fecha de creación |
| `github.event.comment.updated_at` | `"2026-04-22T10:15:00Z"` | Fecha de edición |
| `github.event.comment.html_url` | `"https://github.com/.../issues/99#issuecomment-555666777"` | URL del comentario |

</details>

---

### `discussion`

<details>
<summary>Ver campos de github.event en discussion</summary>

| Campo | Ejemplo | Descripción |
|---|---|---|
| `github.event.action` | `"created"` | `created`, `edited`, `deleted`, `pinned`, `unpinned`, `locked`, `unlocked`, `transferred`, `answered`, `unanswered`, `labeled`, `unlabeled`, `category_changed` |
| `github.event.discussion.number` | `15` | Número de la discusión |
| `github.event.discussion.title` | `"¿Cómo usar caché?"` | Título |
| `github.event.discussion.body` | `"Tengo problemas..."` | Contenido |
| `github.event.discussion.state` | `"open"` | `"open"`, `"closed"`, `"locked"` |
| `github.event.discussion.answer_html_url` | `null` | URL de la respuesta marcada como solución (o `null` si no hay) |
| `github.event.discussion.category.name` | `"Q&A"` | Nombre de la categoría: `"Q&A"`, `"Ideas"`, `"General"`, `"Show and tell"`, ... |
| `github.event.discussion.category.slug` | `"q-a"` | Slug de la categoría |
| `github.event.discussion.category.is_answerable` | `true` | `true` si permite marcar una respuesta como solución (solo Q&A) |
| `github.event.discussion.user.login` | `"dukono"` | Autor de la discusión |
| `github.event.discussion.labels` | `[{name, color}]` | Etiquetas |
| `github.event.discussion.created_at` | `"2026-04-20T08:00:00Z"` | Fecha de creación |
| `github.event.discussion.html_url` | `"https://github.com/.../discussions/15"` | URL de la discusión |

</details>

---

### `discussion_comment`

<details>
<summary>Ver campos de github.event en discussion_comment</summary>

| Campo | Ejemplo | Descripción |
|---|---|---|
| `github.event.action` | `"created"` | `"created"`, `"edited"`, `"deleted"` |
| `github.event.comment.id` | `777888999` | ID del comentario |
| `github.event.comment.body` | `"Puedes usar actions/cache..."` | Texto del comentario |
| `github.event.comment.parent_id` | `null` | `null` si es un comentario raíz. Si es una respuesta, tiene el ID del comentario padre |
| `github.event.comment.user.login` | `"contributor1"` | Autor |
| `github.event.comment.author_association` | `"CONTRIBUTOR"` | Relación con el repo |
| `github.event.comment.created_at` | `"2026-04-22T09:00:00Z"` | Fecha |
| `github.event.comment.html_url` | `"https://github.com/.../discussions/15#discussioncomment-777888999"` | URL |
| `github.event.discussion.number` | `15` | Número de la discusión donde se comentó |
| `github.event.discussion.title` | `"¿Cómo usar caché?"` | Título de la discusión |
| `github.event.discussion.category.name` | `"Q&A"` | Categoría |

</details>

---

### `release`

<details>
<summary>Ver campos de github.event en release</summary>

| Campo | Ejemplo | Descripción |
|---|---|---|
| `github.event.action` | `"published"` | `published`, `created`, `edited`, `deleted`, `prereleased`, `released`, `unpublished`. El más usado en CI/CD es `published` |
| `github.event.release.tag_name` | `"v1.2.0"` | Tag de la release |
| `github.event.release.name` | `"Release 1.2.0"` | Nombre/título de la release (`null` si no tiene) |
| `github.event.release.body` | `"## Cambios\n- feat: OAuth"` | Release notes (`null` si vacío) |
| `github.event.release.draft` | `false` | `true` si es un borrador (no publicado aún) |
| `github.event.release.prerelease` | `false` | `true` si está marcado como pre-release |
| `github.event.release.target_commitish` | `"main"` | Rama o SHA desde donde se creó la release |
| `github.event.release.author.login` | `"dukono"` | Usuario que creó la release |
| `github.event.release.assets` | `[...]` | Array de archivos adjuntos a la release |
| `github.event.release.assets[0].name` | `"my-app-linux-amd64"` | Nombre del archivo |
| `github.event.release.assets[0].size` | `15728640` | Tamaño en bytes |
| `github.event.release.assets[0].download_count` | `42` | Número de descargas |
| `github.event.release.assets[0].browser_download_url` | `"https://github.com/.../releases/download/v1.2.0/my-app-linux-amd64"` | URL de descarga |
| `github.event.release.html_url` | `"https://github.com/.../releases/tag/v1.2.0"` | URL web de la release |
| `github.event.release.upload_url` | `"https://uploads.github.com/.../assets{?name,label}"` | URL para subir assets a la release via API |
| `github.event.release.tarball_url` | `"https://api.github.com/.../tarball/v1.2.0"` | URL del tarball del código fuente |
| `github.event.release.zipball_url` | `"https://api.github.com/.../zipball/v1.2.0"` | URL del zip del código fuente |
| `github.event.release.created_at` | `"2026-04-22T10:00:00Z"` | Fecha de creación |
| `github.event.release.published_at` | `"2026-04-22T10:05:00Z"` | Fecha de publicación (`null` si es borrador) |

</details>

---

### `registry_package`

<details>
<summary>Ver campos de github.event en registry_package</summary>

Se dispara cuando se publica o actualiza un paquete en **GitHub Packages**.

| Campo | Ejemplo | Descripción |
|---|---|---|
| `github.event.action` | `"published"` | `"published"` o `"updated"` |
| `github.event.package.name` | `"my-app"` | Nombre del paquete |
| `github.event.package.package_type` | `"container"` | Tipo: `container`, `npm`, `maven`, `rubygems`, `docker`, `nuget` |
| `github.event.package.html_url` | `"https://github.com/.../pkgs/container/my-app"` | URL del paquete en GitHub |
| `github.event.package.owner.login` | `"dukono"` | Propietario del paquete |
| `github.event.package.package_version.version` | `"1.2.0"` | Versión publicada |
| `github.event.package.package_version.name` | `"1.2.0"` | Nombre de la versión |
| `github.event.package.package_version.target_commitish` | `"main"` | Rama asociada a esta versión |
| `github.event.package.package_version.target_oid` | `"a1b2c3d4..."` | SHA del commit asociado |
| `github.event.package.package_version.container_metadata.tags` | `["latest", "1.2.0", "1.2"]` | Tags del contenedor (solo si es container) |
| `github.event.package.package_version.container_metadata.manifest.digest` | `"sha256:037fb5af..."` | Digest del manifiesto del contenedor |
| `github.event.package.package_version.html_url` | `"https://github.com/.../pkgs/container/my-app/111222"` | URL de esta versión |

</details>

---

### `check_run`

<details>
<summary>Ver campos de github.event en check_run</summary>

Se dispara cuando una **GitHub App** (CI externo) crea o actualiza un check run.

| Campo | Ejemplo | Descripción |
|---|---|---|
| `github.event.action` | `"completed"` | `created`, `rerequested`, `completed`, `requested_action` |
| `github.event.check_run.name` | `"Unit Tests"` | Nombre del check |
| `github.event.check_run.status` | `"completed"` | `"queued"`, `"in_progress"`, `"completed"` |
| `github.event.check_run.conclusion` | `"success"` | `success`, `failure`, `neutral`, `cancelled`, `skipped`, `timed_out`, `action_required`, `null` (si no completó aún) |
| `github.event.check_run.head_sha` | `"a1b2c3d4..."` | SHA del commit que se chequeó |
| `github.event.check_run.external_id` | `"build-123"` | ID externo asignado por la app (`null` si no tiene) |
| `github.event.check_run.started_at` | `"2026-04-22T10:00:00Z"` | Cuándo empezó |
| `github.event.check_run.completed_at` | `"2026-04-22T10:05:00Z"` | Cuándo terminó (`null` si no completó) |
| `github.event.check_run.output.title` | `"Tests passed"` | Título del resultado |
| `github.event.check_run.output.summary` | `"42 tests passed"` | Resumen |
| `github.event.check_run.output.annotations_count` | `0` | Número de anotaciones |
| `github.event.check_run.app.name` | `"GitHub Actions"` | Nombre de la GitHub App que creó el check |
| `github.event.check_run.app.slug` | `"github-actions"` | Slug de la app |
| `github.event.check_run.check_suite.id` | `1234567890` | ID del check suite al que pertenece |
| `github.event.check_run.pull_requests` | `[{number: 42}]` | PRs asociados a este check |
| `github.event.check_run.html_url` | `"https://github.com/.../runs/9876543210"` | URL del check |
| `github.event.requested_action.identifier` | `"fix-it"` | ⚠️ Solo cuando `action == "requested_action"` — identificador del botón pulsado |

</details>

---

### `check_suite`

<details>
<summary>Ver campos de github.event en check_suite</summary>

Un check suite agrupa varios check runs del mismo commit.

| Campo | Ejemplo | Descripción |
|---|---|---|
| `github.event.action` | `"completed"` | `"completed"`, `"requested"`, `"rerequested"` |
| `github.event.check_suite.id` | `1234567890` | ID del suite |
| `github.event.check_suite.head_branch` | `"feature/oauth"` | Rama del commit |
| `github.event.check_suite.head_sha` | `"a1b2c3d4..."` | SHA del commit |
| `github.event.check_suite.before` | `"83108f7a..."` | SHA anterior |
| `github.event.check_suite.after` | `"a1b2c3d4..."` | SHA posterior |
| `github.event.check_suite.status` | `"completed"` | `"queued"`, `"in_progress"`, `"completed"` |
| `github.event.check_suite.conclusion` | `"success"` | `success`, `failure`, `neutral`, `cancelled`, ... `null` si no terminó |
| `github.event.check_suite.app.name` | `"GitHub Actions"` | Nombre de la app |
| `github.event.check_suite.pull_requests` | `[{number: 42}]` | PRs asociados |
| `github.event.check_suite.created_at` | `"2026-04-22T10:00:00Z"` | Fecha de creación |
| `github.event.check_suite.updated_at` | `"2026-04-22T10:05:00Z"` | Fecha de última actualización |

</details>

---

### `deployment`

<details>
<summary>Ver campos de github.event en deployment</summary>

Se dispara cuando se **crea un deployment** (via API o una action).

| Campo | Ejemplo | Descripción |
|---|---|---|
| `github.event.deployment.id` | `789012345` | ID del deployment |
| `github.event.deployment.sha` | `"a1b2c3d4..."` | SHA del commit a desplegar |
| `github.event.deployment.ref` | `"main"` | Rama o tag desde donde se despliega |
| `github.event.deployment.task` | `"deploy"` | Tarea: por defecto siempre `"deploy"` |
| `github.event.deployment.environment` | `"production"` | Nombre del entorno: `"production"`, `"staging"`, `"development"`, ... |
| `github.event.deployment.description` | `"Deploy v1.2.0"` | Descripción del deployment (`null` si no tiene) |
| `github.event.deployment.production_environment` | `true` | `true` si el environment está marcado como producción |
| `github.event.deployment.transient_environment` | `false` | `true` si es un entorno efímero (pr previews, etc.) |
| `github.event.deployment.payload` | `{"version":"1.2.0"}` | JSON personalizado enviado al crear el deployment |
| `github.event.deployment.creator.login` | `"dukono"` | Usuario o bot que creó el deployment |
| `github.event.deployment.statuses_url` | `"https://api.github.com/.../deployments/789012345/statuses"` | URL para consultar o actualizar el estado del deployment |
| `github.event.deployment.created_at` | `"2026-04-22T10:00:00Z"` | Fecha de creación |

</details>

---

### `deployment_status`

<details>
<summary>Ver campos de github.event en deployment_status</summary>

Se dispara cuando cambia el **estado** de un deployment.

| Campo | Ejemplo | Descripción |
|---|---|---|
| `github.event.deployment_status.id` | `111222333` | ID del estado |
| `github.event.deployment_status.state` | `"success"` | Estado: `pending`, `in_progress`, `queued`, `success`, `failure`, `error`, `inactive` |
| `github.event.deployment_status.description` | `"Deploy succeeded"` | Descripción del estado (`null` si no tiene) |
| `github.event.deployment_status.environment` | `"production"` | Nombre del entorno |
| `github.event.deployment_status.environment_url` | `"https://my-app.example.com"` | URL del entorno desplegado (`null` si no tiene) |
| `github.event.deployment_status.log_url` | `"https://github.com/.../runs/9876543210"` | URL de los logs (`null` si no tiene) |
| `github.event.deployment_status.creator.login` | `"github-actions[bot]"` | Quien actualizó el estado |
| `github.event.deployment_status.created_at` | `"2026-04-22T10:01:00Z"` | Fecha |
| `github.event.deployment.id` | `789012345` | ID del deployment al que pertenece este estado |
| `github.event.deployment.sha` | `"a1b2c3d4..."` | SHA del commit del deployment |
| `github.event.deployment.ref` | `"main"` | Rama del deployment |
| `github.event.deployment.environment` | `"production"` | Entorno del deployment |
| `github.event.deployment.payload` | `{"version":"1.2.0"}` | Payload del deployment |

</details>

---

### `workflow_run`

<details>
<summary>Ver campos de github.event en workflow_run</summary>

Se dispara cuando otro workflow **completa, inicia o se solicita**.

| Campo | Ejemplo | Descripción |
|---|---|---|
| `github.event.action` | `"completed"` | `"completed"`, `"requested"`, `"in_progress"` |
| `github.event.workflow_run.id` | `9876543210` | ID de la ejecución del workflow origen |
| `github.event.workflow_run.name` | `"CI Pipeline"` | Nombre del workflow origen |
| `github.event.workflow_run.display_title` | `"fix: bug en login"` | Título de la ejecución (normalmente el mensaje del commit) |
| `github.event.workflow_run.status` | `"completed"` | `"completed"`, `"in_progress"`, `"queued"` |
| `github.event.workflow_run.conclusion` | `"success"` | `success`, `failure`, `neutral`, `cancelled`, `skipped`, `timed_out`, `action_required`, `null` |
| `github.event.workflow_run.head_sha` | `"a1b2c3d4..."` | SHA del commit que ejecutó el workflow origen |
| `github.event.workflow_run.head_branch` | `"feature/oauth"` | Rama del workflow origen |
| `github.event.workflow_run.workflow_id` | `12345678` | ID del workflow (no de la ejecución, del workflow en sí) |
| `github.event.workflow_run.run_number` | `42` | Número de ejecución del workflow origen |
| `github.event.workflow_run.run_attempt` | `1` | Número de reintento de la ejecución origen |
| `github.event.workflow_run.event` | `"push"` | Evento que **disparó el workflow origen** — no el evento actual |
| `github.event.workflow_run.actor.login` | `"dukono"` | Usuario que disparó el workflow origen |
| `github.event.workflow_run.triggering_actor.login` | `"dukono"` | Usuario que disparó esta ejecución concreta (puede diferir en re-runs) |
| `github.event.workflow_run.pull_requests` | `[]` | PRs asociados al workflow origen (puede estar vacío) |
| `github.event.workflow_run.repository.full_name` | `"dukono/my-app"` | Repo del workflow origen |
| `github.event.workflow_run.artifacts_url` | `"https://api.github.com/.../runs/9876543210/artifacts"` | URL de la API para listar artifacts de esa ejecución |
| `github.event.workflow_run.jobs_url` | `"https://api.github.com/.../runs/9876543210/jobs"` | URL de la API para listar jobs |
| `github.event.workflow_run.logs_url` | `"https://api.github.com/.../runs/9876543210/logs"` | URL de los logs |
| `github.event.workflow_run.html_url` | `"https://github.com/.../actions/runs/9876543210"` | URL web de la ejecución |
| `github.event.workflow_run.created_at` | `"2026-04-22T10:00:00Z"` | Cuándo se creó |
| `github.event.workflow_run.run_started_at` | `"2026-04-22T10:00:05Z"` | Cuándo empezó a ejecutarse |
| `github.event.workflow.name` | `"CI Pipeline"` | Nombre del workflow (igual que `workflow_run.name`) |
| `github.event.workflow.path` | `".github/workflows/ci.yml"` | Ruta al archivo del workflow |

</details>

---

### `workflow_dispatch`

<details>
<summary>Ver campos de github.event en workflow_dispatch</summary>

Se dispara cuando alguien ejecuta el workflow **manualmente** desde la UI o la API.

| Campo | Ejemplo | Descripción |
|---|---|---|
| `github.event.inputs` | `{"environment":"production","version":"1.2.0"}` | Objeto con todos los inputs enviados |
| `github.event.inputs.<nombre>` | `"production"` | Valor de un input concreto — **siempre llega como string** |
| `github.event.ref` | `"refs/heads/main"` | Rama desde la que se ejecutó el workflow |
| `github.event.workflow` | `".github/workflows/deploy.yml"` | Ruta del archivo del workflow |
| `github.event.sender.login` | `"dukono"` | Usuario que ejecutó el workflow |

> Todos los inputs son **strings** aunque el tipo declarado sea `boolean` o `number`:
>
> | Tipo declarado | Cómo llega | Cómo usar |
> |---|---|---|
> | `string` | `"production"` | `inputs.env == 'production'` |
> | `boolean` | `"true"` o `"false"` | `inputs.dry_run == 'true'` |
> | `number` | `"3"` | `fromJSON(inputs.replicas)` para operar como número |
> | `choice` | `"production"` | `inputs.env == 'production'` |
> | `environment` | `"prod-env"` | `inputs.target == 'prod-env'` |

</details>

---

### `repository_dispatch`

<details>
<summary>Ver campos de github.event en repository_dispatch</summary>

Se dispara con una **llamada a la API REST** desde un sistema externo.

| Campo | Ejemplo | Descripción |
|---|---|---|
| `github.event.action` | `"deploy"` | El `event_type` enviado en la llamada a la API |
| `github.event.client_payload` | `{"environment":"prod","version":"1.2.3"}` | JSON personalizado enviado en la llamada — puede tener cualquier estructura |
| `github.event.client_payload.<campo>` | `"prod"` | Acceso directo a cualquier campo del payload |
| `github.event.sender.login` | `"dukono"` | Usuario que hizo la llamada API |
| `github.event.installation.id` | `12345` | ID de la GitHub App (si fue disparado por una app) |

Llamada API para dispararlo:
```bash
curl -X POST \
  -H "Authorization: Bearer TOKEN" \
  -H "Accept: application/vnd.github+json" \
  https://api.github.com/repos/OWNER/REPO/dispatches \
  -d '{"event_type":"deploy","client_payload":{"environment":"prod","version":"1.2.3"}}'
```

</details>

---

### `schedule`

<details>
<summary>Ver campos de github.event en schedule</summary>

Se dispara automáticamente según una **expresión cron**.

| Campo | Ejemplo | Descripción |
|---|---|---|
| `github.event.schedule` | `"0 9 * * 1-5"` | La expresión cron definida en el workflow que disparó esta ejecución |

> El evento solo tiene este campo. El resto de la información del contexto viene de `github.*` (sha, repo, etc.) con los valores del último commit de la rama por defecto.

</details>

---

### `fork`

<details>
<summary>Ver campos de github.event en fork</summary>

Se dispara cuando alguien hace un **fork** del repositorio.

| Campo | Ejemplo | Descripción |
|---|---|---|
| `github.event.forkee.full_name` | `"otheruser/my-app"` | Nombre completo del fork creado |
| `github.event.forkee.html_url` | `"https://github.com/otheruser/my-app"` | URL del fork |
| `github.event.forkee.private` | `false` | Si el fork es privado |
| `github.event.forkee.owner.login` | `"otheruser"` | Propietario del fork |
| `github.event.forkee.owner.id` | `99999999` | ID del propietario del fork |
| `github.event.forkee.created_at` | `"2026-04-22T11:00:00Z"` | Cuándo se creó el fork |
| `github.event.sender.login` | `"otheruser"` | Usuario que hizo el fork |

</details>

---

### `watch`

<details>
<summary>Ver campos de github.event en watch</summary>

Se dispara cuando alguien da ⭐ **star** al repositorio.
El nombre del evento (`watch`) es confuso — no tiene nada que ver con "seguir el repo".

| Campo | Ejemplo | Descripción |
|---|---|---|
| `github.event.action` | `"started"` | Siempre `"started"` — es el único valor posible |
| `github.event.repository.stargazers_count` | `128` | Total de estrellas tras esta acción |
| `github.event.sender.login` | `"stargazer1"` | Usuario que dio la estrella |
| `github.event.sender.id` | `44444444` | ID del usuario |

</details>

---

### `milestone`

<details>
<summary>Ver campos de github.event en milestone</summary>

| Campo | Ejemplo | Descripción |
|---|---|---|
| `github.event.action` | `"created"` | `created`, `closed`, `opened`, `edited`, `deleted` |
| `github.event.milestone.number` | `3` | Número del milestone |
| `github.event.milestone.title` | `"v2.0.0"` | Título |
| `github.event.milestone.description` | `"Major release"` | Descripción (`null` si vacío) |
| `github.event.milestone.state` | `"open"` | `"open"` o `"closed"` |
| `github.event.milestone.open_issues` | `12` | Issues abiertos dentro del milestone |
| `github.event.milestone.closed_issues` | `38` | Issues cerrados dentro del milestone |
| `github.event.milestone.due_on` | `"2026-06-01T00:00:00Z"` | Fecha límite (`null` si no tiene) |
| `github.event.milestone.creator.login` | `"dukono"` | Usuario que creó el milestone |
| `github.event.milestone.html_url` | `"https://github.com/.../milestone/3"` | URL |
| `github.event.milestone.created_at` | `"2026-01-01T00:00:00Z"` | Fecha de creación |
| `github.event.milestone.updated_at` | `"2026-04-22T10:00:00Z"` | Última actualización |
| `github.event.milestone.closed_at` | `null` | Fecha de cierre (`null` si está abierto) |
| `github.event.sender.login` | `"dukono"` | Usuario que realizó la acción |

</details>

---

### `page_build`

<details>
<summary>Ver campos de github.event en page_build</summary>

Se dispara cuando se construye o falla **GitHub Pages**.

| Campo | Ejemplo | Descripción |
|---|---|---|
| `github.event.id` | `123456789` | ID de la construcción |
| `github.event.build.status` | `"built"` | `"building"`, `"built"`, `"errored"` |
| `github.event.build.commit` | `"a1b2c3d4..."` | SHA del commit que disparó el build |
| `github.event.build.branch` | `"gh-pages"` | Rama de GitHub Pages |
| `github.event.build.duration` | `90000` | Duración en **milisegundos** |
| `github.event.build.error.message` | `null` | Mensaje de error (`null` si no falló) |
| `github.event.build.pusher.login` | `"dukono"` | Usuario que hizo el push |
| `github.event.build.created_at` | `"2026-04-22T10:00:00Z"` | Inicio |
| `github.event.build.updated_at` | `"2026-04-22T10:01:30Z"` | Fin |
| `github.event.build.url` | `"https://api.github.com/.../pages/builds/123456789"` | URL de la API de este build |

</details>

---

## `env`

<details>
<summary>Ver campos</summary>

Variables de entorno definidas a nivel de **workflow**, **job** o **step**.

| Campo | Ejemplo | Descripción |
|---|---|---|
| `env.<NOMBRE>` | `"production"` | Valor de la variable. Todos son **strings** |

> - En bash: `$NOMBRE` directamente
> - En expresiones: `${{ env.NOMBRE }}`
> - El scope más cercano tiene prioridad: step > job > workflow

</details>

---

## `job`

<details>
<summary>Ver campos</summary>

| Campo | Ejemplo | Descripción |
|---|---|---|
| `job.status` | `"success"` | Estado actual del job: `"success"`, `"failure"`, `"cancelled"` |
| `job.container.id` | `"abc123def456..."` | ID del container del job. Solo existe si el job tiene `container:` |
| `job.container.network` | `"github_network_abc123"` | Red del container. Solo si el job tiene `container:` |
| `job.services.<id>.id` | `"def456abc123..."` | ID del container del servicio. Solo si el job tiene `services:` |
| `job.services.<id>.network` | `"github_network_abc123"` | Red del servicio |
| `job.services.<id>.ports['<puerto_interno>']` | `"54321"` | **Puerto en el host** asignado al puerto interno del servicio |

> **`job.services.<id>.ports`**: cuando declaras `ports: ["5432/tcp"]`, GitHub asigna un puerto aleatorio en el host y este campo te dice cuál es. Si declaras `ports: ["5432:5432"]` (fijado), siempre es `"5432"`.

> **`job.status` vs `needs.<job>.result`**:
>
> | | `job.status` | `needs.<job>.result` |
> |---|---|---|
> | Dónde se usa | Dentro del **mismo** job | En un job que depende de otro |
> | Cuándo tiene valor | Mientras el job corre | Cuando el job ya terminó |

</details>

---

## `steps`

<details>
<summary>Ver campos</summary>

| Campo | Ejemplo | Descripción |
|---|---|---|
| `steps.<id>.outputs.<clave>` | `"1.2.3"` | Valor guardado por el step con `echo "clave=valor" >> $GITHUB_OUTPUT` |
| `steps.<id>.outcome` | `"failure"` | Resultado **real** del step — lo que pasó de verdad |
| `steps.<id>.conclusion` | `"success"` | Resultado **visible al workflow** — puede diferir si el step tiene `continue-on-error: true` |

> **`outcome` vs `conclusion`** — la diferencia solo importa con `continue-on-error: true`:
>
> | Escenario | `outcome` | `conclusion` |
> |---|---|---|
> | Step OK | `"success"` | `"success"` |
> | Step falla sin `continue-on-error` | `"failure"` | `"failure"` |
> | Step falla **con** `continue-on-error: true` | `"failure"` | `"success"` ← error absorbido |
> | Step omitido por `if:` | `"skipped"` | `"skipped"` |
>
> → Para detectar si un step con `continue-on-error` falló realmente, usa **`outcome`**:
> `if: steps.linter.outcome == 'failure'` ✅
> `if: steps.linter.conclusion == 'failure'` ❌ siempre false

</details>

---

## `runner`

<details>
<summary>Ver campos (GitHub-hosted y self-hosted)</summary>

| Campo | Linux hosted | Windows hosted | self-hosted |
|---|---|---|---|
| `runner.name` | `"GitHub Actions 2"` | `"GitHub Actions 3"` | `"my-runner"` |
| `runner.os` | `"Linux"` | `"Windows"` | `"Linux"` |
| `runner.arch` | `"X64"` | `"X64"` | `"ARM64"` |
| `runner.temp` | `"/home/runner/work/_temp"` | `"D:\a\_temp"` | `"/opt/runner/_work/_temp"` |
| `runner.tool_cache` | `"/opt/hostedtoolcache"` | `"C:\hostedtoolcache\windows"` | `"/opt/runner/_work/_tool"` |
| `runner.workspace` | `"/home/runner/work/my-app"` | `"D:\a\my-app"` | `"/opt/runner/_work/my-app"` |
| `runner.environment` | `"github-hosted"` | `"github-hosted"` | `"self-hosted"` |
| `runner.debug` | `""` (inactivo) / `"1"` (activo) | `""` / `"1"` | `""` / `"1"` |

> `runner.debug` es un **string** — comparar con `== '1'`, nunca con `== true`.
> Se activa con el secret `ACTIONS_RUNNER_DEBUG=true` o pulsando "Enable debug logging" al re-ejecutar.

</details>

---

## `secrets`

<details>
<summary>Ver campos</summary>

| Campo | Ejemplo | Descripción |
|---|---|---|
| `secrets.GITHUB_TOKEN` | `"ghs_xxxxxxxxxxxx"` | Token automático siempre disponible — no hay que crearlo |
| `secrets.<NOMBRE>` | `"mi-valor-secreto"` | Cualquier secret configurado en el repo, organización o ambiente |

> - En los logs, GitHub **enmascara** todos los secrets con `***`
> - Si el secret no existe, devuelve `""` — no lanza error
> - Los secrets de organización solo están disponibles si el repo tiene permiso para usarlos

</details>

---

## `vars`

<details>
<summary>Ver campos</summary>

| Campo | Ejemplo | Descripción |
|---|---|---|
| `vars.<NOMBRE>` | `"production"` | Cualquier variable de configuración definida en el repo, organización o ambiente |

> - Todos los valores son **strings**
> - A diferencia de `secrets`, son **visibles en logs**
> - Para datos sensibles (contraseñas, tokens) usar `secrets`, no `vars`
> - Si la variable no existe, devuelve `""` — no lanza error

</details>

---

## `strategy` y `matrix`

<details>
<summary>Ver campos</summary>

Solo disponibles en jobs que usan `strategy.matrix`.

| Campo | Ejemplo | Descripción |
|---|---|---|
| `strategy.fail-fast` | `true` | Si `true`, cancela todos los jobs de la matriz cuando uno falla |
| `strategy.job-index` | `4` | Índice (empezando en 0) de esta combinación dentro de la matriz |
| `strategy.job-total` | `9` | Total de combinaciones en la matriz |
| `strategy.max-parallel` | `4` | Máximo de jobs corriendo simultáneamente |
| `matrix.<propiedad>` | `"ubuntu-latest"` | Valor de esa propiedad en la combinación actual |

> Si la matriz es `os: [ubuntu, windows, macos]` × `python: [3.9, 3.10, 3.11]` → `job-total = 9`, `job-index` va de `0` a `8`.

</details>

---

## `needs`

<details>
<summary>Ver campos</summary>

Acceso a los **resultados y outputs** de los jobs de los que depende el job actual.

| Campo | Ejemplo | Descripción |
|---|---|---|
| `needs.<job_id>.result` | `"success"` | Resultado final del job: `"success"`, `"failure"`, `"cancelled"`, `"skipped"` |
| `needs.<job_id>.outputs.<clave>` | `"1.2.3"` | Valor publicado por el job en su sección `outputs:` |

> Solo contiene los jobs declarados en `needs:` del job actual — no los jobs de los que dependían esos jobs.

</details>

---

## `inputs`

<details>
<summary>Ver campos</summary>

Disponible en `workflow_dispatch` (ejecución manual) y `workflow_call` (workflow reutilizable).

| Campo | Ejemplo | Descripción |
|---|---|---|
| `inputs.<nombre>` | `"production"` | Valor del input — **siempre llega como string** |

> Tipos de input y cómo llegan:
>
> | Tipo declarado | Valor en `inputs.<nombre>` | Cómo comparar o usar |
> |---|---|---|
> | `string` | `"production"` | `inputs.env == 'production'` |
> | `boolean` | `"true"` / `"false"` | `inputs.dry_run == 'true'` — es string, no booleano |
> | `number` | `"3"` | `fromJSON(inputs.replicas)` para operar como número |
> | `choice` | `"production"` | `inputs.env == 'production'` |
> | `environment` | `"prod-env"` | `inputs.target == 'prod-env'` |

> `inputs.<nombre>` y `github.event.inputs.<nombre>` son equivalentes en `workflow_dispatch`.
> En `workflow_call`, **solo existe `inputs`** — `github.event.inputs` no existe.

</details>

---

## `jobs` — solo workflows reutilizables

<details>
<summary>Ver campos</summary>

Solo disponible en workflows con `on: workflow_call`. **No existe en workflows normales.**

| Campo | Ejemplo | Descripción |
|---|---|---|
| `jobs.<job_id>.result` | `"success"` | Resultado del job: `"success"`, `"failure"`, `"cancelled"`, `"skipped"` |
| `jobs.<job_id>.outputs.<clave>` | `"https://my-app.example.com"` | Output del job |

> **`jobs` vs `needs`**:
>
> | | `needs` | `jobs` |
> |---|---|---|
> | Disponible en | Cualquier workflow, dentro de un job | Solo en reusable workflows (`workflow_call`) |
> | Accede a | Solo los jobs en `needs:` del job actual | **Todos** los jobs del workflow |
> | Válido en `outputs:` del workflow | ❌ No | ✅ Sí |
>
> Ejemplo de uso en la sección `outputs:` del workflow reutilizable (fuera de cualquier job):
> ```yaml
> on:
>   workflow_call:
>     outputs:
>       url:
>         value: ${{ jobs.deploy.outputs.url }}   # ← 'jobs', no 'needs'
> ```
> Si intentas usar `needs` en ese nivel, da error — `needs` no existe fuera de un job.

</details>

---

## Arrays y operador `.*`

<details>
<summary>Ver explicación y uso</summary>

Varios campos del contexto son **arrays de objetos**. Los más comunes:

| Campo | Estructura de cada elemento |
|---|---|
| `github.event.pull_request.labels` | `{ id, name, color, description, default }` |
| `github.event.pull_request.assignees` | `{ login, id, type, site_admin }` |
| `github.event.pull_request.requested_reviewers` | `{ login, id, type }` |
| `github.event.pull_request.requested_teams` | `{ id, name, slug }` |
| `github.event.issue.labels` | `{ id, name, color, description, default }` |
| `github.event.issue.assignees` | `{ login, id }` |
| `github.event.commits` | `{ id, message, timestamp, author, committer, added, modified, removed }` |
| `github.event.release.assets` | `{ id, name, size, download_count, browser_download_url }` |

**Acceso por índice — un elemento concreto:**
```
github.event.pull_request.labels[0].name    → "bug"
github.event.commits[0].message             → "fix: bug en login"
```

**Operador `.*` — un campo de todos los elementos a la vez:**
```
github.event.pull_request.labels.*.name     → ["bug", "urgent", "preview"]
github.event.pull_request.assignees.*.login → ["user1", "user2"]
github.event.commits.*.message              → ["fix: login", "test: añadir tests"]
```

Equivale a `.map(x => x.campo)` en JavaScript. **Solo funciona dentro de expresiones `${{ }}`**.

**Uso típico con `contains()`:**
```yaml
# ¿Tiene la etiqueta "bug"?
if: contains(github.event.pull_request.labels.*.name, 'bug')

# ¿Está asignado a dukono?
if: contains(github.event.pull_request.assignees.*.login, 'dukono')

# ¿Se ha solicitado revisión a reviewer1?
if: contains(github.event.pull_request.requested_reviewers.*.login, 'reviewer1')
```

</details>

---

## Funciones

<details>
<summary>Ver todas las funciones disponibles en expresiones ${{ }}</summary>

| Función | Qué hace | Ejemplo |
|---|---|---|
| `contains(buscar_en, valor)` | `true` si `buscar_en` contiene `valor` — funciona con strings y arrays | `contains('hello world', 'world')` → `true` |
| `startsWith(texto, prefijo)` | `true` si `texto` empieza con `prefijo` | `startsWith(github.ref, 'refs/heads/')` → `true` |
| `endsWith(texto, sufijo)` | `true` si `texto` termina con `sufijo` | `endsWith(github.ref_name, '-dev')` → `true` |
| `format(plantilla, arg0, arg1, ...)` | Sustituye `{0}`, `{1}`, etc. en la plantilla | `format('v{0}.{1}', '1', '2')` → `"v1.2"` |
| `join(array, separador)` | Une los elementos del array con el separador | `join(github.event.pull_request.labels.*.name, ', ')` → `"bug, urgent"` |
| `toJSON(valor)` | Convierte a string JSON — útil para debuggear o pasar datos | `toJSON(github.event)` → `'{"action":"push",...}'` |
| `fromJSON(string)` | Parsea un string JSON a objeto o valor | `fromJSON('{"a":1}').a` → `1` |
| `hashFiles(patrón)` | SHA-256 de los archivos que coinciden con el patrón | `hashFiles('**/package-lock.json')` → `"abc123..."` |
| `success()` | `true` si todos los steps anteriores han pasado | Se usa en `if:` de steps |
| `failure()` | `true` si algún step anterior falló | Se usa en `if:` de steps de limpieza |
| `cancelled()` | `true` si el workflow fue cancelado | Se usa en `if:` de steps |
| `always()` | Siempre `true` — ejecuta el step pase lo que pase | Se usa en `if:` de steps de notificación/limpieza |

> `success()` es el comportamiento **por defecto** si no hay `if:`. Es decir, no poner `if:` equivale a `if: success()`.

> `always()` no tiene equivalente con `job.status` — es la única forma de garantizar que un step corra incluso si el workflow fue cancelado.

</details>
