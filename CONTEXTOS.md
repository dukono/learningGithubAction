# GitHub Actions — Contextos

Cada contexto es un objeto que GitHub inyecta en cada ejecución. Se accede con `${{ contexto.campo }}`.

> Despliega cada sección para ver la estructura completa.

**Cobertura:**
Los contextos `env`, `job`, `steps`, `runner`, `secrets`, `vars`, `strategy`, `matrix`, `needs`, `inputs` y `jobs` están **completos**.
Para `github.event` se documentan todos los campos **relevantes** de cada evento (se omiten `node_id`, URLs internas de API y campos de bajo nivel que no se usan en workflows).

---

## Índice

- [github — propiedades generales](#github--propiedades-generales)
- [github.event — campos comunes a casi todos los eventos](#githubEvent--campos-comunes)
- [github.event — por evento](#githubEvent--por-evento)
  - [push](#push) · [create](#create) · [delete](#delete)
  - [pull\_request / pull\_request\_target](#pull_request--pull_request_target)
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
- [jobs — solo reusable workflows](#jobs--solo-reusable-workflows)
- [Arrays y operador .*](#arrays-y-operador-)
- [Funciones](#funciones)

---

## `github` — propiedades generales

Disponibles en **todos** los eventos.

<details>
<summary>Ver JSON</summary>

```jsonc
{
  "action":               "__run",                    // ID del step. En una action: "actions/checkout"
  "action_path":          "/home/runner/work/_actions/actions/checkout/v4", // ruta instalada de la action
  "action_ref":           "v4",                       // versión/ref de la action actual
  "action_repository":    "actions/checkout",         // repo de la action actual
  "action_status":        "success",                  // estado de la action actual

  "actor":                "dukono",                   // usuario que disparó el workflow
  "actor_id":             "12345678",                 // ID numérico del usuario
  "triggering_actor":     "dukono",                   // quien disparó ESTA ejecución — difiere de actor en re-runs

  "token":                "ghs_xxxxxxxxxxxxxxxxxxxx", // token automático = secrets.GITHUB_TOKEN

  "repository":           "dukono/my-app",            // owner/repo
  "repository_id":        "987654321",
  "repository_owner":     "dukono",
  "repository_owner_id":  "12345678",
  "repositoryUrl":        "git://github.com/dukono/my-app.git",

  "ref":          "refs/heads/main",   // ref completa. En PR: refs/pull/11/merge (NO refs/heads/feature)
  "ref_name":     "main",             // nombre corto: rama, tag, o "11/merge" si es PR
  "ref_type":     "branch",           // "branch" o "tag"
  "ref_protected": false,             // true si la rama tiene branch protection

  "head_ref":     "feature/oauth",    // ⚠️ SOLO en pull_request — rama ORIGEN del PR
  "base_ref":     "main",             // ⚠️ SOLO en pull_request — rama DESTINO del PR

  "sha":          "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2", // SHA del commit que disparó el evento

  "event_name":   "push",             // nombre del evento: push, pull_request, release, schedule, ...
  "event_path":   "/home/runner/work/_temp/_github_workflow/event.json", // ruta al JSON del payload

  "workflow":     "CI Pipeline",      // campo name: del YAML del workflow
  "workflow_ref": "dukono/my-app/.github/workflows/ci.yml@refs/heads/main",
  "workflow_sha": "a1b2c3d4e5f6...",

  "job":          "build",            // id: del job actual en el YAML

  "run_id":       "9876543210",       // ID único global de esta ejecución
  "run_number":   "42",               // número secuencial de ejecuciones de este workflow en el repo
  "run_attempt":  "1",                // número de reintento — "2" si se pulsó re-run

  "secret_source":    "Actions",      // origen de secrets: "Actions", "Dependabot", "None"
  "retention_days":   "90",           // días que se guardan artifacts y logs

  "server_url":   "https://github.com",
  "api_url":      "https://api.github.com",
  "graphql_url":  "https://api.github.com/graphql",

  "workspace":    "/home/runner/work/my-app/my-app",  // directorio donde checkout clona el repo

  // archivos especiales del runner (se usan con echo >> $GITHUB_ENV, etc.)
  "env":          "/home/runner/work/_temp/_runner_file_commands/set_env_abc",
  "output":       "/home/runner/work/_temp/_runner_file_commands/set_output_abc",
  "path":         "/home/runner/work/_temp/_runner_file_commands/add_path_abc",

  "job_workflow_sha": "a1b2c3d4e5f6..."
}
```

</details>

---

## `github.event` — campos comunes

Estos objetos aparecen en **casi todos los eventos**. No se repiten en cada sección individual.

<details>
<summary>Ver github.event.repository</summary>

```jsonc
{
  "repository": {
    "id":           987654321,
    "name":         "my-app",               // solo el nombre, sin el owner
    "full_name":      "dukono/my-app",        // owner/repo
    "private":      false,
    "fork":         false,                  // true si este repo es un fork de otro
    "archived":     false,
    "disabled":     false,
    "visibility":   "public",              // "public", "private", "internal"
    "default_branch": "main",
    "description":  "Mi aplicación",       // null si vacío
    "language":     "TypeScript",          // lenguaje principal detectado
    "topics":       ["docker", "ci"],      // array de tópicos
    "homepage":     "https://example.com", // null si no tiene

    "size":               1024,            // en KB
    "stargazers_count":   42,
    "watchers_count":     42,
    "forks_count":        8,
    "open_issues_count":  5,

    "allow_forking":  true,
    "is_template":    false,
    "has_issues":     true,
    "has_projects":   true,
    "has_wiki":       true,
    "has_pages":      false,
    "has_downloads":  true,
    "has_discussions": false,

    "owner": {
      "login":      "dukono",
      "id":         12345678,
      "type":       "User",               // "User" u "Organization"
      "site_admin": false
    },

    "html_url":   "https://github.com/dukono/my-app",
    "clone_url":  "https://github.com/dukono/my-app.git",  // HTTPS
    "ssh_url":    "git@github.com:dukono/my-app.git",
    "git_url":    "git://github.com/dukono/my-app.git",
    "svn_url":    "https://github.com/dukono/my-app",
    "url":        "https://api.github.com/repos/dukono/my-app",

    "license": {                           // null si no tiene licencia
      "key":  "mit",
      "name": "MIT License",
      "url":  "https://api.github.com/licenses/mit"
    },

    "created_at": "2020-01-01T00:00:00Z",
    "updated_at": "2026-04-22T10:00:00Z",
    "pushed_at":  "2026-04-22T10:30:00Z"
  }
}
```

</details>

<details>
<summary>Ver github.event.sender</summary>

Usuario o bot que realizó la acción que disparó el evento.

```jsonc
{
  "sender": {
    "login":      "dukono",
    "id":         12345678,
    "type":       "User",                 // "User" o "Bot"
    "site_admin": false,                  // true si es administrador de GitHub.com
    "avatar_url": "https://avatars.githubusercontent.com/u/12345678",
    "html_url":   "https://github.com/dukono",
    "url":        "https://api.github.com/users/dukono"
  }
}
```

> `sender` y `github.actor` normalmente son iguales. Difieren si una GitHub App actúa en nombre de un usuario.

</details>

<details>
<summary>Ver github.event.organization — solo en repos de organización</summary>

Solo existe cuando el repo pertenece a una **organización**.

```jsonc
{
  "organization": {
    "login":       "mi-empresa",
    "id":          99887766,
    "description": "Empresa de software",   // null si no tiene
    "url":         "https://api.github.com/orgs/mi-empresa",
    "repos_url":   "https://api.github.com/orgs/mi-empresa/repos",
    "events_url":  "https://api.github.com/orgs/mi-empresa/events",
    "members_url": "https://api.github.com/orgs/mi-empresa/members{/member}",
    "avatar_url":  "https://avatars.githubusercontent.com/u/99887766"
  }
}
```

</details>

<details>
<summary>Ver github.event.installation — solo cuando lo dispara una GitHub App</summary>

Solo existe cuando el evento es disparado por una **GitHub App instalada**.

```jsonc
{
  "installation": {
    "id":      12345678,
    "node_id": "MDIzOkludGVncmF0aW9uSW5zdGFsbGF0aW9u..."
  }
}
```

</details>

---

## `github.event` — por evento

---

### `push`

<details>
<summary>Ver JSON</summary>

```jsonc
{
  "ref":     "refs/heads/main",      // rama o tag que recibió el push
  "before":  "83108f7a2e3b1c9d...", // SHA del HEAD antes del push
  "after":   "9cf3dea4b5e8f1a2...", // SHA del HEAD después del push
  "compare": "https://github.com/dukono/my-app/compare/83108f7...9cf3dea",
  "created": false,                  // true si este push CREÓ la rama
  "deleted": false,                  // true si este push ELIMINÓ la rama
  "forced":  false,                  // true si fue git push --force
  "base_ref": null,                  // rama base si el push es a un tag (normalmente null)

  "pusher": {
    "name":  "dukono",
    "email": "dukono@example.com"
  },

  "commits": [                       // array de todos los commits del push
    {
      "id":        "a1b2c3d4e5f6...",
      "tree_id":   "b2c3d4e5f6a1...",
      "distinct":  true,             // false si el commit ya existía en el repo (merge)
      "message":   "fix: bug en login",
      "timestamp": "2026-04-22T10:30:00+02:00",
      "url":       "https://github.com/dukono/my-app/commit/a1b2c3d4",
      "author": {
        "name":     "Vitaly Neborachok",
        "email":    "vitaly@example.com",
        "username": "dukono"
      },
      "committer": {
        "name":     "GitHub",
        "email":    "noreply@github.com",
        "username": "web-flow"
      },
      "added":    ["src/new-file.ts"],     // archivos añadidos en este commit
      "removed":  ["src/old-file.ts"],     // archivos eliminados
      "modified": ["src/app.ts", "README.md"]  // archivos modificados
    }
  ],

  "head_commit": {                   // último commit del push (el nuevo HEAD)
    "id":        "9cf3dea4b5e8f1a2...",
    "message":   "fix: bug en login",
    "timestamp": "2026-04-22T10:30:00+02:00",
    "url":       "https://github.com/dukono/my-app/commit/9cf3dea4",
    "author": {
      "name":     "Vitaly Neborachok",
      "email":    "vitaly@example.com",
      "username": "dukono"
    },
    "committer": {
      "name":     "GitHub",
      "email":    "noreply@github.com",
      "username": "web-flow"
    },
    "added":    [],
    "removed":  [],
    "modified": ["src/app.ts"]
  }

  // + repository, sender (ver sección "campos comunes")
}
```

> ⚠️ `before` / `after` **solo existen en `push`**. En `pull_request` usa:
> `github.event.pull_request.base.sha` y `github.event.pull_request.head.sha`

</details>

---

### `create`

<details>
<summary>Ver JSON</summary>

Se dispara cuando se **crea** una rama o tag.

```jsonc
{
  "ref":           "feature/my-branch", // nombre de la rama o tag creado
  "ref_type":      "branch",            // "branch" o "tag"
  "master_branch": "main",              // rama principal del repo
  "description":   null,
  "pusher_type":   "user"               // "user" o "deploy_key"

  // + repository, sender
}
```

</details>

---

### `delete`

<details>
<summary>Ver JSON</summary>

Se dispara cuando se **elimina** una rama o tag.

```jsonc
{
  "ref":         "feature/old-branch",  // nombre de la rama o tag eliminado
  "ref_type":    "branch",              // "branch" o "tag"
  "pusher_type": "user"                 // "user" o "deploy_key"

  // + repository, sender
}
```

</details>

---

### `pull_request` / `pull_request_target`

> **`pull_request`** → ejecuta con el código de la **rama origen** — sin `secrets` si es un fork externo.
> **`pull_request_target`** → ejecuta con el código de la **rama base** — con `secrets` aunque sea un fork.

<details>
<summary>Ver JSON</summary>

```jsonc
{
  // qué acción ocurrió:
  // opened | synchronize | closed | reopened
  // labeled | unlabeled | assigned | unassigned
  // review_requested | review_request_removed
  // ready_for_review | converted_to_draft
  // auto_merge_enabled | auto_merge_disabled
  "action": "opened",

  "number": 42,             // número del PR (igual que pull_request.number)

  "pull_request": {
    "id":     1234567890,
    "number": 42,
    "title":  "feat: añadir autenticación OAuth",
    "body":   "Descripción del PR...\n\nCierres #99",  // null si vacío

    "state":  "open",       // "open" o "closed"
    "locked": false,
    "draft":  false,        // true si es borrador

    "merged":           false,   // true si ya se mergeó
    "mergeable":        true,    // true | false | null (null = GitHub aún calculando)
    "rebaseable":       true,
    "mergeable_state":  "clean", // clean | dirty(conflictos) | unstable(checks) | blocked | behind | unknown
    "merge_commit_sha": null,    // SHA del merge commit (null si no mergeado)
    "merged_by":        null,    // objeto usuario que mergeó, o null

    "auto_merge": null,          // null si no está activo. Si activo:
    // "auto_merge": {
    //   "enabled_by":     { "login": "dukono" },
    //   "merge_method":   "squash",   // merge | squash | rebase
    //   "commit_title":   "feat: añadir autenticación OAuth (#42)",
    //   "commit_message": ""
    // }

    "user": {
      "login":      "dukono",
      "id":         12345678,
      "type":       "User",     // "User" o "Bot"
      "site_admin": false
    },

    // OWNER | MEMBER | COLLABORATOR | CONTRIBUTOR | FIRST_TIME_CONTRIBUTOR | FIRST_TIMER | NONE
    "author_association": "OWNER",

    "head": {                   // rama ORIGEN del PR
      "label": "dukono:feature/oauth",  // "usuario:rama" — útil para PRs de forks
      "ref":   "feature/oauth",
      "sha":   "a1b2c3d4e5f6...",
      "repo": {
        "id":             987654321,
        "full_name":      "dukono/my-app",
        "private":        false,
        "default_branch": "main"
      }
    },

    "base": {                   // rama DESTINO del PR
      "label": "dukono:main",
      "ref":   "main",
      "sha":   "83108f7a2e3b...",
      "repo": {
        "id":             987654321,
        "full_name":      "dukono/my-app",
        "private":        false,
        "default_branch": "main"
      }
    },

    "labels": [                 // array de etiquetas
      {
        "id":          123456,
        "name":        "bug",
        "color":       "d73a4a",
        "description": "Something is not working",
        "default":     true     // true si es etiqueta por defecto de GitHub
      }
    ],

    "assignees": [              // array de usuarios asignados
      { "login": "user1", "id": 11111111, "type": "User" }
    ],

    "requested_reviewers": [    // array de revisores individuales solicitados
      { "login": "reviewer1", "id": 22222222, "type": "User" }
    ],

    "requested_teams": [        // array de equipos revisores solicitados
      { "id": 999, "name": "backend-team", "slug": "backend-team" }
    ],

    "milestone": null,          // null si no tiene. Si tiene:
    // "milestone": { "id": 5678901, "number": 3, "title": "v2.0.0", "state": "open" }

    // estadísticas — TODOS son enteros, NO listas
    "changed_files":    7,     // ⚠️ número de archivos, no la lista
    "additions":        142,
    "deletions":        38,
    "commits":          3,
    "review_comments":  2,     // comentarios inline en revisiones
    "comments":         1,     // comentarios generales

    // URLs
    "html_url":             "https://github.com/dukono/my-app/pull/42",
    "diff_url":             "https://github.com/dukono/my-app/pull/42.diff",
    "patch_url":            "https://github.com/dukono/my-app/pull/42.patch",
    "issue_url":            "https://api.github.com/repos/dukono/my-app/issues/42",
    "commits_url":          "https://api.github.com/repos/dukono/my-app/pulls/42/commits",
    "review_comments_url":  "https://api.github.com/repos/dukono/my-app/pulls/42/comments",
    "url":                  "https://api.github.com/repos/dukono/my-app/pulls/42",

    // fechas
    "created_at": "2026-04-20T09:00:00Z",
    "updated_at": "2026-04-22T10:30:00Z",
    "closed_at":  null,   // null si está abierto
    "merged_at":  null    // null si no se ha mergeado
  }

  // + repository, sender
}
```

> `changed_files` es un **entero** (contador). Para obtener la lista de archivos modificados necesitas la API o una action como `tj-actions/changed-files`.

</details>

---

### `pull_request_review`

<details>
<summary>Ver JSON</summary>

Se dispara cuando alguien **envía una revisión** (aprueba, pide cambios o comenta).

```jsonc
{
  // "submitted" | "edited" | "dismissed"
  "action": "submitted",

  "review": {
    "id":    987654321,
    "state": "approved",   // "approved" | "changes_requested" | "commented"
    "body":  "LGTM 👍",    // null si no hay texto

    "commit_id":    "a1b2c3d4...",  // SHA del commit revisado
    "submitted_at": "2026-04-22T10:00:00Z",

    // OWNER | MEMBER | COLLABORATOR | CONTRIBUTOR | FIRST_TIME_CONTRIBUTOR | NONE
    "author_association": "MEMBER",

    "user": {
      "login": "reviewer1",
      "id":    22222222,
      "type":  "User"
    },

    "html_url":          "https://github.com/dukono/my-app/pull/42#pullrequestreview-987654321",
    "pull_request_url":  "https://api.github.com/repos/dukono/my-app/pulls/42"
  },

  "pull_request": {
    "number": 42,
    "title":  "feat: añadir autenticación OAuth",
    "state":  "open",
    "draft":  false,
    "user":   { "login": "dukono", "id": 12345678 },
    "head":   { "ref": "feature/oauth", "sha": "a1b2c3d4..." },
    "base":   { "ref": "main",          "sha": "83108f7a..." }
  }

  // + repository, sender
}
```

</details>

---

### `pull_request_review_comment`

<details>
<summary>Ver JSON</summary>

Se dispara cuando se añade un **comentario inline** sobre una línea de código del PR.

```jsonc
{
  // "created" | "edited" | "deleted"
  "action": "created",

  "comment": {
    "id":   111222333,
    "body": "¿Por qué usas var aquí?",

    "path":          "src/auth/login.ts",    // archivo comentado
    "line":          42,                      // línea actual
    "original_line": 40,                      // línea antes de rebase/force push
    "position":      5,                       // posición en el diff
    "side":          "RIGHT",  // "RIGHT" = línea nueva, "LEFT" = línea eliminada

    "diff_hunk": "@@ -38,6 +40,8 @@\n   const user = getUser();\n+  const token = ...",

    "commit_id":            "a1b2c3d4e5f6...",
    "original_commit_id":   "a1b2c3d4e5f6...",
    "pull_request_review_id": 987654321,

    "author_association": "MEMBER",

    "user": {
      "login": "reviewer1",
      "id":    22222222,
      "type":  "User"
    },

    "created_at": "2026-04-22T09:30:00Z",
    "updated_at": "2026-04-22T09:30:00Z",
    "html_url":   "https://github.com/dukono/my-app/pull/42#discussion_r111222333",
    "url":        "https://api.github.com/repos/dukono/my-app/pulls/comments/111222333"
  },

  "pull_request": {
    "number": 42,
    "title":  "feat: añadir autenticación OAuth",
    "state":  "open",
    "head":   { "ref": "feature/oauth", "sha": "a1b2c3d4..." },
    "base":   { "ref": "main" }
  }

  // + repository, sender
}
```

</details>

---

### `issues`

<details>
<summary>Ver JSON</summary>

```jsonc
{
  // opened | closed | edited | reopened
  // labeled | unlabeled | assigned | unassigned
  // milestoned | demilestoned | locked | unlocked
  // transferred | pinned | unpinned | deleted
  "action": "opened",

  "issue": {
    "id":     2345678901,
    "number": 99,
    "title":  "Bug en login",
    "body":   "Pasos para reproducir:\n1. Abrir Firefox\n2. Ir a /login...", // null si vacío

    "state":        "open",       // "open" o "closed"
    "state_reason": null,         // "completed" | "not_planned" | "reopened" | null
    "locked":       false,

    "author_association": "OWNER", // OWNER | MEMBER | COLLABORATOR | CONTRIBUTOR | NONE

    "user": {
      "login":      "dukono",
      "id":         12345678,
      "type":       "User",
      "site_admin": false
    },

    "labels": [
      {
        "id":          123456,
        "name":        "bug",
        "color":       "d73a4a",
        "description": "Something is not working",
        "default":     true
      }
    ],

    "assignees": [
      { "login": "user1", "id": 11111111, "type": "User" }
    ],

    "milestone": null,   // null si no tiene. Si tiene:
    // "milestone": {
    //   "id": 5678901, "number": 3, "title": "v2.0.0",
    //   "state": "open", "due_on": "2026-06-01T00:00:00Z"
    // }

    "comments":   0,
    "created_at": "2026-04-18T08:00:00Z",
    "updated_at": "2026-04-22T10:00:00Z",
    "closed_at":  null,
    "html_url":   "https://github.com/dukono/my-app/issues/99",
    "url":        "https://api.github.com/repos/dukono/my-app/issues/99"
  },

  // ⚠️ Solo cuando action == "labeled" o "unlabeled":
  "label": { "id": 123456, "name": "bug", "color": "d73a4a" },

  // ⚠️ Solo cuando action == "assigned" o "unassigned":
  "assignee": { "login": "user1", "id": 11111111 }

  // + repository, sender
}
```

</details>

---

### `issue_comment`

<details>
<summary>Ver JSON</summary>

Se dispara en comentarios de **issues y también de PRs** (internamente un PR es un issue).

```jsonc
{
  // "created" | "edited" | "deleted"
  "action": "created",

  "issue": {
    "number": 99,
    "title":  "Bug en login",
    "state":  "open",
    "user":   { "login": "dukono", "id": 12345678 },
    "labels": [{ "name": "bug", "color": "d73a4a" }],

    // ⚠️ Si el issue ES un PR, este campo existe con la URL del PR.
    //    Si es un issue normal, es null.
    "pull_request": null
    // "pull_request": { "html_url": "https://github.com/dukono/my-app/pull/42" }
  },

  "comment": {
    "id":   555666777,
    "body": "He podido reproducirlo también en Chrome",

    "author_association": "CONTRIBUTOR",

    "user": {
      "login": "contributor1",
      "id":    33333333,
      "type":  "User"
    },

    "created_at": "2026-04-22T10:15:00Z",
    "updated_at": "2026-04-22T10:15:00Z",
    "html_url":   "https://github.com/dukono/my-app/issues/99#issuecomment-555666777",
    "url":        "https://api.github.com/repos/dukono/my-app/issues/comments/555666777"
  }

  // + repository, sender
}
```

</details>

---

### `discussion`

<details>
<summary>Ver JSON</summary>

```jsonc
{
  // created | edited | deleted | pinned | unpinned | locked | unlocked
  // transferred | answered | unanswered | labeled | unlabeled | category_changed
  "action": "created",

  "discussion": {
    "id":     3456789012,
    "number": 15,
    "title":  "¿Cómo usar caché?",
    "body":   "Tengo problemas configurando...",

    "state":            "open",   // "open" | "closed" | "locked"
    "answer_html_url":  null,     // URL de la respuesta marcada como solución (null si no hay)

    "category": {
      "id":           12345,
      "name":         "Q&A",         // "Q&A" | "Ideas" | "General" | "Show and tell" | ...
      "slug":         "q-a",
      "emoji":        ":grey_question:",
      "description":  "Ask the community for help",
      "is_answerable": true          // true = permite marcar una respuesta como solución
    },

    "user": {
      "login": "dukono",
      "id":    12345678,
      "type":  "User"
    },

    "labels": [
      { "id": 789, "name": "question", "color": "cc317c" }
    ],

    "created_at": "2026-04-20T08:00:00Z",
    "updated_at": "2026-04-22T10:00:00Z",
    "html_url":   "https://github.com/dukono/my-app/discussions/15"
  }

  // + repository, sender
}
```

</details>

---

### `discussion_comment`

<details>
<summary>Ver JSON</summary>

```jsonc
{
  // "created" | "edited" | "deleted"
  "action": "created",

  "comment": {
    "id":        777888999,
    "body":      "Puedes usar actions/cache con esta key...",
    "parent_id": null,    // null si es comentario raíz. ID del padre si es una respuesta.

    "author_association": "CONTRIBUTOR",

    "user": {
      "login": "contributor1",
      "id":    33333333,
      "type":  "User"
    },

    "created_at": "2026-04-22T09:00:00Z",
    "updated_at": "2026-04-22T09:00:00Z",
    "html_url":   "https://github.com/dukono/my-app/discussions/15#discussioncomment-777888999"
  },

  "discussion": {
    "number": 15,
    "title":  "¿Cómo usar caché?",
    "state":  "open",
    "category": {
      "name":          "Q&A",
      "is_answerable": true
    }
  }

  // + repository, sender
}
```

</details>

---

### `release`

<details>
<summary>Ver JSON</summary>

```jsonc
{
  // published | created | edited | deleted | prereleased | released | unpublished
  // El más usado en CI/CD es "published"
  "action": "published",

  "release": {
    "id":          123456789,
    "tag_name":    "v1.2.0",
    "name":        "Release 1.2.0 — Nuevas características",  // null si no tiene nombre
    "body":        "## Cambios\n\n### Features\n- feat: OAuth\n\n### Fixes\n- fix: login",
    "draft":       false,    // true si es borrador (no publicado)
    "prerelease":  false,    // true si está marcado como pre-release

    "target_commitish": "main",   // rama o SHA desde donde se creó la release

    "author": {
      "login": "dukono",
      "id":    12345678,
      "type":  "User"
    },

    "assets": [             // archivos adjuntos a la release
      {
        "id":           9876543,
        "name":         "my-app-linux-amd64",
        "label":        "",
        "state":        "uploaded",
        "content_type": "application/octet-stream",
        "size":         15728640,       // bytes
        "download_count": 42,
        "browser_download_url": "https://github.com/dukono/my-app/releases/download/v1.2.0/my-app-linux-amd64",
        "created_at": "2026-04-22T10:05:00Z",
        "updated_at": "2026-04-22T10:05:00Z"
      }
    ],

    "html_url":     "https://github.com/dukono/my-app/releases/tag/v1.2.0",
    "upload_url":   "https://uploads.github.com/repos/dukono/my-app/releases/123456789/assets{?name,label}",
    "tarball_url":  "https://api.github.com/repos/dukono/my-app/tarball/v1.2.0",
    "zipball_url":  "https://api.github.com/repos/dukono/my-app/zipball/v1.2.0",
    "url":          "https://api.github.com/repos/dukono/my-app/releases/123456789",

    "created_at":   "2026-04-22T10:00:00Z",
    "published_at": "2026-04-22T10:05:00Z"  // null si es borrador
  }

  // + repository, sender
}
```

</details>

---

### `registry_package`

<details>
<summary>Ver JSON</summary>

Se dispara cuando se publica o actualiza un paquete en **GitHub Packages**.

```jsonc
{
  // "published" | "updated"
  "action": "published",

  "package": {
    "id":           456789,
    "name":         "my-app",

    // "container" | "npm" | "maven" | "rubygems" | "docker" | "nuget"
    "package_type": "container",

    "html_url":   "https://github.com/dukono/my-app/pkgs/container/my-app",
    "created_at": "2026-04-22T10:00:00Z",
    "updated_at": "2026-04-22T10:00:00Z",

    "owner": {
      "login": "dukono",
      "id":    12345678
    },

    "package_version": {
      "id":      111222,
      "version": "1.2.0",
      "name":    "1.2.0",

      "target_commitish": "main",               // rama asociada
      "target_oid":       "a1b2c3d4e5f6...",    // SHA del commit asociado

      "html_url":   "https://github.com/dukono/my-app/pkgs/container/my-app/111222",
      "created_at": "2026-04-22T10:00:00Z",
      "updated_at": "2026-04-22T10:00:00Z",

      "container_metadata": {              // solo si package_type == "container"
        "tags": ["latest", "1.2.0", "1.2"],
        "labels": {
          "org.opencontainers.image.revision": "a1b2c3d4",
          "org.opencontainers.image.source":   "https://github.com/dukono/my-app"
        },
        "manifest": {
          "digest":     "sha256:037fb5afaa44c5cde96a4f4e6b118d3f9d202dec0b84f6eccbe82e3217d4f018",
          "media_type": "application/vnd.oci.image.manifest.v1+json"
        }
      }
    }
  }

  // + repository, sender
}
```

</details>

---

### `check_run`

<details>
<summary>Ver JSON</summary>

Se dispara cuando una **GitHub App** de CI crea o actualiza un check run.

```jsonc
{
  // "created" | "rerequested" | "completed" | "requested_action"
  "action": "completed",

  "check_run": {
    "id":          9876543210,
    "name":        "Unit Tests",
    "head_sha":    "a1b2c3d4e5f6...",
    "external_id": "build-123",  // ID asignado por la app externa (null si no tiene)

    // "queued" | "in_progress" | "completed"
    "status": "completed",

    // "success" | "failure" | "neutral" | "cancelled" | "skipped" | "timed_out" | "action_required"
    // null mientras no ha completado
    "conclusion": "success",

    "started_at":   "2026-04-22T10:00:00Z",
    "completed_at": "2026-04-22T10:05:00Z",  // null si no completó

    "output": {
      "title":             "Tests passed",
      "summary":           "All 42 tests passed",
      "text":              null,
      "annotations_count": 0
    },

    "check_suite": {
      "id":         1234567890,
      "head_branch": "feature/oauth",
      "head_sha":   "a1b2c3d4..."
    },

    "app": {
      "id":   15368,
      "name": "GitHub Actions",
      "slug": "github-actions"
    },

    "pull_requests": [              // PRs asociados a este check
      {
        "number": 42,
        "head":   { "ref": "feature/oauth", "sha": "a1b2c3d4..." },
        "base":   { "ref": "main",          "sha": "83108f7a..." }
      }
    ],

    "html_url": "https://github.com/dukono/my-app/runs/9876543210"
  },

  // ⚠️ Solo cuando action == "requested_action" (usuario pulsó un botón del check):
  "requested_action": { "identifier": "fix-it" }

  // + repository, sender
}
```

</details>

---

### `check_suite`

<details>
<summary>Ver JSON</summary>

Un check suite agrupa varios check runs del mismo commit.

```jsonc
{
  // "completed" | "requested" | "rerequested"
  "action": "completed",

  "check_suite": {
    "id":          1234567890,
    "head_branch": "feature/oauth",
    "head_sha":    "a1b2c3d4e5f6...",
    "before":      "83108f7a2e3b...",
    "after":       "a1b2c3d4e5f6...",

    // "queued" | "in_progress" | "completed"
    "status": "completed",

    // "success" | "failure" | "neutral" | "cancelled" | ... | null
    "conclusion": "success",

    "app": {
      "id":   15368,
      "name": "GitHub Actions",
      "slug": "github-actions"
    },

    "pull_requests": [{ "number": 42 }],

    "created_at": "2026-04-22T10:00:00Z",
    "updated_at": "2026-04-22T10:05:00Z"
  }

  // + repository, sender
}
```

</details>

---

### `deployment`

<details>
<summary>Ver JSON</summary>

Se dispara cuando se **crea un deployment** (via API o una action).

```jsonc
{
  "deployment": {
    "id":  789012345,
    "sha": "a1b2c3d4e5f6...",       // SHA del commit a desplegar
    "ref": "main",                  // rama o tag desde donde se despliega
    "task": "deploy",               // siempre "deploy" por defecto

    "environment": "production",    // "production" | "staging" | "development" | ...

    "description": "Deploy v1.2.0 to production",  // null si no tiene

    "production_environment": true, // true si el environment está marcado como producción
    "transient_environment":  false, // true si es entorno efímero (PR previews, etc.)

    "payload": {                    // JSON personalizado enviado al crear el deployment
      "version": "1.2.0",
      "target":  "us-east-1"
    },

    "creator": {
      "login": "dukono",
      "id":    12345678
    },

    "statuses_url": "https://api.github.com/repos/dukono/my-app/deployments/789012345/statuses",
    "created_at":   "2026-04-22T10:00:00Z",
    "updated_at":   "2026-04-22T10:00:00Z"
  }

  // + repository, sender
}
```

</details>

---

### `deployment_status`

<details>
<summary>Ver JSON</summary>

Se dispara cuando cambia el **estado** de un deployment.

```jsonc
{
  "deployment_status": {
    "id": 111222333,

    // "pending" | "in_progress" | "queued" | "success" | "failure" | "error" | "inactive"
    "state": "success",

    "description":      "Deploy succeeded",           // null si no tiene
    "environment":      "production",
    "environment_url":  "https://my-app.example.com", // null si no tiene
    "log_url":          "https://github.com/dukono/my-app/actions/runs/9876543210", // null si no tiene

    "creator": {
      "login": "github-actions[bot]",
      "id":    41898282,
      "type":  "Bot"
    },

    "created_at": "2026-04-22T10:01:00Z",
    "updated_at": "2026-04-22T10:06:00Z"
  },

  "deployment": {                  // deployment al que pertenece este estado
    "id":          789012345,
    "sha":         "a1b2c3d4...",
    "ref":         "main",
    "task":        "deploy",
    "environment": "production",
    "payload":     { "version": "1.2.0" }
  }

  // + repository, sender
}
```

</details>

---

### `workflow_run`

<details>
<summary>Ver JSON</summary>

Se dispara cuando otro workflow **completa, inicia o se solicita**.

```jsonc
{
  // "completed" | "requested" | "in_progress"
  "action": "completed",

  "workflow_run": {
    "id":            9876543210,
    "name":          "CI Pipeline",
    "display_title": "fix: bug en login",  // título visible (normalmente el mensaje del commit)

    // "completed" | "in_progress" | "queued"
    "status": "completed",

    // "success" | "failure" | "neutral" | "cancelled" | "skipped" | "timed_out" | "action_required"
    // null si no ha completado
    "conclusion": "success",

    "head_sha":    "a1b2c3d4e5f6...",
    "head_branch": "feature/oauth",

    "head_commit": {
      "id":      "a1b2c3d4e5f6...",
      "message": "fix: bug en login",
      "author":  { "name": "Vitaly", "email": "vitaly@example.com" }
    },

    "workflow_id":  12345678,   // ID del workflow (no de la ejecución)
    "run_number":   42,
    "run_attempt":  1,

    // ⚠️ ESTE es el evento que disparó el WORKFLOW ORIGEN, no el evento actual
    "event": "push",             // "push" | "pull_request" | "schedule" | ...

    "actor": {
      "login": "dukono",
      "id":    12345678
    },
    "triggering_actor": {
      "login": "dukono",
      "id":    12345678
    },

    "pull_requests": [],         // PRs asociados (puede estar vacío aunque haya PR)

    "repository": {
      "id":        987654321,
      "full_name": "dukono/my-app"
    },

    // URLs para acceder a datos de la ejecución origen via API
    "artifacts_url":   "https://api.github.com/repos/dukono/my-app/actions/runs/9876543210/artifacts",
    "jobs_url":        "https://api.github.com/repos/dukono/my-app/actions/runs/9876543210/jobs",
    "logs_url":        "https://api.github.com/repos/dukono/my-app/actions/runs/9876543210/logs",
    "check_suite_url": "https://api.github.com/repos/dukono/my-app/check-suites/1234567890",
    "html_url":        "https://github.com/dukono/my-app/actions/runs/9876543210",

    "created_at":     "2026-04-22T10:00:00Z",
    "updated_at":     "2026-04-22T10:05:00Z",
    "run_started_at": "2026-04-22T10:00:05Z"
  },

  "workflow": {
    "id":   12345678,
    "name": "CI Pipeline",
    "path": ".github/workflows/ci.yml"
  }

  // + repository, sender
}
```

</details>

---

### `workflow_dispatch`

<details>
<summary>Ver JSON</summary>

Se dispara cuando alguien ejecuta el workflow **manualmente** desde la UI o la API.

```jsonc
{
  "inputs": {           // todos los valores llegan como STRINGS aunque el tipo declarado sea boolean/number
    "environment": "production",
    "version":     "1.2.0",
    "dry_run":     "false",   // boolean → string "true" o "false"
    "replicas":    "3"        // number  → string "3"
  },

  "ref":      "refs/heads/main",         // rama desde la que se ejecutó
  "workflow": ".github/workflows/deploy.yml",

  "sender": { "login": "dukono", "id": 12345678 }

  // + repository
}
```

> Cómo usar cada tipo de input:
>
> | Tipo declarado | Valor real | Cómo comparar/usar |
> |---|---|---|
> | `string` | `"production"` | `inputs.env == 'production'` |
> | `boolean` | `"true"` / `"false"` | `inputs.dry_run == 'true'` — es string, no booleano |
> | `number` | `"3"` | `fromJSON(inputs.replicas)` para operar |
> | `choice` | `"production"` | `inputs.env == 'production'` |
> | `environment` | `"prod-env"` | `inputs.target == 'prod-env'` |

</details>

---

### `repository_dispatch`

<details>
<summary>Ver JSON</summary>

Se dispara con una **llamada a la API REST** desde un sistema externo.

```jsonc
{
  "action": "deploy",     // el "event_type" enviado en la llamada API

  "client_payload": {     // JSON personalizado — puede tener cualquier estructura
    "environment": "prod",
    "version":     "1.2.3",
    "force":       true,
    "replicas":    3
  },

  "installation": { "id": 12345 },  // solo si fue disparado por una GitHub App

  "sender": { "login": "dukono", "id": 12345678 }

  // + repository
}
```

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
<summary>Ver JSON</summary>

```jsonc
{
  "schedule": "0 9 * * 1-5"   // la expresión cron que disparó esta ejecución
}
```

> El evento solo tiene este campo. El resto del contexto (`github.sha`, `github.repository`, etc.) toma los valores del último commit de la rama por defecto.

</details>

---

### `fork`

<details>
<summary>Ver JSON</summary>

Se dispara cuando alguien hace un **fork** del repositorio.

```jsonc
{
  "forkee": {                           // el fork recién creado
    "id":             9999999,
    "name":           "my-app",
    "full_name":      "otheruser/my-app",
    "private":        false,
    "fork":           true,
    "html_url":       "https://github.com/otheruser/my-app",
    "clone_url":      "https://github.com/otheruser/my-app.git",
    "default_branch": "main",
    "owner": {
      "login": "otheruser",
      "id":    99999999,
      "type":  "User"
    },
    "created_at": "2026-04-22T11:00:00Z",
    "pushed_at":  "2026-04-22T11:00:00Z"
  }

  // + repository (el repo original), sender
}
```

</details>

---

### `watch`

<details>
<summary>Ver JSON</summary>

Se dispara cuando alguien da ⭐ **star** al repositorio.
El nombre del evento (`watch`) es confuso — **no** tiene que ver con "seguir el repo".

```jsonc
{
  "action": "started"   // único valor posible

  // + repository (que incluye stargazers_count actualizado), sender
}
```

> Para saber cuántas estrellas hay: `github.event.repository.stargazers_count`

</details>

---

### `milestone`

<details>
<summary>Ver JSON</summary>

```jsonc
{
  // "created" | "closed" | "opened" | "edited" | "deleted"
  "action": "created",

  "milestone": {
    "id":          5678901,
    "number":      3,
    "title":       "v2.0.0",
    "description": "Major release with new features",  // null si vacío
    "state":       "open",     // "open" o "closed"

    "open_issues":   12,
    "closed_issues": 38,

    "due_on": "2026-06-01T00:00:00Z",  // null si no tiene fecha límite

    "creator": {
      "login": "dukono",
      "id":    12345678,
      "type":  "User"
    },

    "html_url": "https://github.com/dukono/my-app/milestone/3",
    "url":      "https://api.github.com/repos/dukono/my-app/milestones/3",

    "created_at": "2026-01-01T00:00:00Z",
    "updated_at": "2026-04-22T10:00:00Z",
    "closed_at":  null
  }

  // + repository, sender
}
```

</details>

---

### `page_build`

<details>
<summary>Ver JSON</summary>

Se dispara cuando se construye o falla **GitHub Pages**.

```jsonc
{
  "id": 123456789,

  "build": {
    "url":    "https://api.github.com/repos/dukono/my-app/pages/builds/123456789",

    // "building" | "built" | "errored"
    "status": "built",

    "error": { "message": null },  // null si no hubo error. Si falló: { "message": "..." }

    "pusher": {
      "login": "dukono",
      "email": "dukono@example.com"
    },

    "commit":   "a1b2c3d4e5f6...",
    "branch":   "gh-pages",
    "duration": 90000,              // milisegundos

    "created_at": "2026-04-22T10:00:00Z",
    "updated_at": "2026-04-22T10:01:30Z"
  }

  // + repository, sender
}
```

</details>

---

## `env`

<details>
<summary>Ver JSON</summary>

```jsonc
{
  // definidas en el YAML a nivel de workflow, job o step
  // todas son strings
  "NODE_ENV":   "production",
  "APP_VERSION": "1.2.3",
  "DEBUG":      "false",
  "API_URL":    "https://api.example.com"
}
```

> - En bash: `$NODE_ENV`
> - En expresiones: `${{ env.NODE_ENV }}`
> - El scope más cercano tiene prioridad: **step > job > workflow**

</details>

---

## `job`

<details>
<summary>Ver JSON</summary>

```jsonc
{
  // "success" | "failure" | "cancelled"
  "status": "success",

  // solo existe si el job tiene "container:"
  "container": {
    "id":      "abc123def456abc123def456abc123def456abc1",
    "network": "github_network_abc123def456"
  },

  // solo existe si el job tiene "services:"
  "services": {
    "postgres": {
      "id":      "def456abc123def456abc123def456abc123def4",
      "network": "github_network_abc123def456",
      "ports": {
        "5432": "54321"   // clave = puerto INTERNO, valor = puerto en el HOST
      }
    },
    "redis": {
      "id":      "fff111aaa222fff111aaa222fff111aaa222fff1",
      "network": "github_network_abc123def456",
      "ports": {
        "6379": "32768"
      }
    }
  }
}
```

> **`ports`**: si declaras `ports: ["5432/tcp"]` (dinámico), GitHub asigna un puerto aleatorio en el host — `ports["5432"]` te dice cuál es. Si declaras `ports: ["5432:5432"]` (fijado), siempre vale `"5432"`.

> **`job.status` vs `needs.<job>.result`**:
>
> | | `job.status` | `needs.<job>.result` |
> |---|---|---|
> | Dónde | Dentro del mismo job | En un job que depende de otro |
> | Cuándo | Mientras corre (refleja lo que pasó hasta ahora) | Cuando el job ya terminó completamente |

</details>

---

## `steps`

<details>
<summary>Ver JSON</summary>

```jsonc
{
  "checkout": {         // id: del step en el YAML
    "outputs": {},      // vacío si el step no usó $GITHUB_OUTPUT
    "outcome":    "success",
    "conclusion": "success"
  },

  "build": {
    "outputs": {
      "version":  "1.2.3",        // echo "version=1.2.3" >> $GITHUB_OUTPUT
      "artifact": "my-app.zip"
    },
    "outcome":    "success",
    "conclusion": "success"
  },

  "linter": {
    "outputs": {},
    "outcome":    "failure",  // ← falló de verdad
    "conclusion": "success"   // ← pero tenía continue-on-error: true → error absorbido
  }
}
```

> **`outcome` vs `conclusion`** — solo difieren con `continue-on-error: true`:
>
> | Escenario | `outcome` | `conclusion` |
> |---|---|---|
> | Step OK | `"success"` | `"success"` |
> | Step falla, sin `continue-on-error` | `"failure"` | `"failure"` |
> | Step falla, **con** `continue-on-error: true` | `"failure"` | `"success"` ← absorbido |
> | Step omitido por `if:` falso | `"skipped"` | `"skipped"` |
>
> Para detectar un fallo real en un step con `continue-on-error`:
> `steps.linter.outcome == 'failure'` ✅
> `steps.linter.conclusion == 'failure'` ❌ siempre false

</details>

---

## `runner`

<details>
<summary>Ver JSON — github-hosted Linux</summary>

```jsonc
{
  "name":         "GitHub Actions 2",
  "os":           "Linux",
  "arch":         "X64",
  "temp":         "/home/runner/work/_temp",
  "tool_cache":   "/opt/hostedtoolcache",
  "workspace":    "/home/runner/work/my-app",
  "environment":  "github-hosted",
  "debug":        ""    // "" = inactivo, "1" = activo
}
```

</details>

<details>
<summary>Ver JSON — github-hosted Windows</summary>

```jsonc
{
  "name":         "GitHub Actions 3",
  "os":           "Windows",
  "arch":         "X64",
  "temp":         "D:\\a\\_temp",
  "tool_cache":   "C:\\hostedtoolcache\\windows",
  "workspace":    "D:\\a\\my-app",
  "environment":  "github-hosted",
  "debug":        ""
}
```

</details>

<details>
<summary>Ver JSON — self-hosted</summary>

```jsonc
{
  "name":         "my-self-hosted-runner",
  "os":           "Linux",
  "arch":         "ARM64",
  "temp":         "/opt/actions-runner/_work/_temp",
  "tool_cache":   "/opt/actions-runner/_work/_tool",
  "workspace":    "/opt/actions-runner/_work/my-app",
  "environment":  "self-hosted",
  "debug":        ""
}
```

</details>

> `runner.debug` es un **string** — comparar con `== '1'`, nunca con `== true`.
> Se activa con el secret `ACTIONS_RUNNER_DEBUG=true` o pulsando "Enable debug logging" al re-ejecutar.

---

## `secrets`

<details>
<summary>Ver JSON</summary>

```jsonc
{
  "GITHUB_TOKEN":          "ghs_xxxxxxxxxxxxxxxxxxxx",   // automático, siempre disponible
  "DOCKER_PASSWORD":       "mi-contraseña-docker",
  "AWS_ACCESS_KEY_ID":     "AKIAIOSFODNN7EXAMPLE",
  "AWS_SECRET_ACCESS_KEY": "wJalrXUtnFEMI/K7MDENG/...",
  "DB_CONNECTION_STRING":  "postgres://user:pass@host:5432/mydb"
}
```

> - En los logs, GitHub enmascara todos los valores con `***`
> - Si el secret no existe devuelve `""` — no lanza error
> - Los secrets de organización solo están disponibles si el repo tiene permiso

</details>

---

## `vars`

<details>
<summary>Ver JSON</summary>

```jsonc
{
  "ENVIRONMENT":        "production",
  "API_URL":            "https://api.example.com",
  "MAX_REPLICAS":       "5",
  "FEATURE_FLAG_OAUTH": "true",
  "DEPLOY_REGION":      "eu-west-1"
}
```

> - Todos los valores son **strings**
> - Visibles en logs (no encriptados) — para datos sensibles usar `secrets`
> - Si no existe devuelve `""` — no lanza error

</details>

---

## `strategy` y `matrix`

<details>
<summary>Ver JSON</summary>

```jsonc
// Ejemplo con matrix: os x python (3x3 = 9 combinaciones), corriendo el job-index 4
{
  "strategy": {
    "fail-fast":    true,   // si true, cancela todos cuando uno falla
    "job-index":    4,      // índice 0-based de esta combinación (0..8)
    "job-total":    9,      // total de combinaciones
    "max-parallel": 4       // máximo de jobs simultáneos
  },
  "matrix": {
    "os":           "ubuntu-latest",   // valor de esta celda
    "python":       "3.11",
    "experimental": false              // campo añadido con "include:"
  }
}
```

> Si la matriz es `os: [ubuntu, windows, macos]` × `python: [3.9, 3.10, 3.11]` → `job-total = 9`, `job-index` va de `0` a `8`.

</details>

---

## `needs`

<details>
<summary>Ver JSON</summary>

```jsonc
// Solo contiene los jobs declarados en needs: del job actual
{
  "build": {
    "result": "success",      // "success" | "failure" | "cancelled" | "skipped"
    "outputs": {
      "version":   "1.2.3",   // lo que el job escribió en $GITHUB_OUTPUT
      "artifact":  "my-app-1.2.3.zip",
      "image-tag": "ghcr.io/dukono/my-app:1.2.3"
    }
  },
  "test": {
    "result": "success",
    "outputs": {
      "coverage": "87"
    }
  }
}
```

</details>

---

## `inputs`

<details>
<summary>Ver JSON</summary>

```jsonc
// Disponible en workflow_dispatch y workflow_call
// Todos los valores son STRINGS independientemente del tipo declarado
{
  "environment": "production",   // type: choice   → string
  "version":     "1.2.0",        // type: string   → string
  "dry_run":     "false",        // type: boolean  → string "true" o "false"
  "replicas":    "3",            // type: number   → string "3"
  "target_env":  "prod-env"      // type: environment → string
}
```

> Cómo usar cada tipo:
>
> | Tipo declarado | Comparar/usar |
> |---|---|
> | `string` / `choice` / `environment` | `inputs.env == 'production'` |
> | `boolean` | `inputs.dry_run == 'true'` — es string, no booleano |
> | `number` | `fromJSON(inputs.replicas)` para operar como número |

> `inputs.<nombre>` y `github.event.inputs.<nombre>` son equivalentes en `workflow_dispatch`.
> En `workflow_call`, **solo existe `inputs`**.

</details>

---

## `jobs` — solo reusable workflows

<details>
<summary>Ver JSON</summary>

Solo disponible en workflows con `on: workflow_call`. **No existe en workflows normales.**

```jsonc
{
  "build": {
    "result": "success",     // "success" | "failure" | "cancelled" | "skipped"
    "outputs": {
      "artifact": "my-app-1.2.3.zip",
      "image":    "ghcr.io/dukono/my-app:1.2.3"
    }
  },
  "test": {
    "result": "success",
    "outputs": {
      "coverage": "87",
      "passed":   "true"
    }
  },
  "deploy": {
    "result": "success",
    "outputs": {
      "url":       "https://my-app.example.com",
      "deploy-id": "dep-abc123"
    }
  }
}
```

> **`jobs` vs `needs`**:
>
> | | `needs` | `jobs` |
> |---|---|---|
> | Disponible en | Cualquier workflow, dentro de un job | Solo reusable workflows |
> | Accede a | Solo jobs en el `needs:` del job actual | **Todos** los jobs del workflow |
> | Válido en sección `outputs:` del workflow | ❌ | ✅ |
>
> ```yaml
> on:
>   workflow_call:
>     outputs:
>       url:
>         value: ${{ jobs.deploy.outputs.url }}  # ← jobs, no needs
> ```

</details>

---

## Arrays y operador `.*`

<details>
<summary>Ver explicación y uso</summary>

Varios campos del contexto son **arrays de objetos**:

```jsonc
// github.event.pull_request.labels
[
  { "id": 123456, "name": "bug",    "color": "d73a4a", "description": "...", "default": true  },
  { "id": 789012, "name": "urgent", "color": "e11d48", "description": "...", "default": false }
]

// github.event.pull_request.assignees
[
  { "login": "user1", "id": 11111111, "type": "User", "site_admin": false },
  { "login": "user2", "id": 22222222, "type": "User", "site_admin": false }
]

// github.event.pull_request.requested_reviewers
[{ "login": "reviewer1", "id": 33333333, "type": "User" }]

// github.event.pull_request.requested_teams
[{ "id": 999, "name": "backend-team", "slug": "backend-team" }]

// github.event.commits (en push)
[{ "id": "a1b2...", "message": "fix: login", "added": [], "modified": ["src/login.ts"], "removed": [] }]

// github.event.release.assets
[{ "id": 9876543, "name": "my-app-linux-amd64", "size": 15728640, "download_count": 42, "browser_download_url": "..." }]
```

**Operador `.*`** — extrae el mismo campo de **todos** los elementos:
```
labels.*.name         → ["bug", "urgent"]
assignees.*.login     → ["user1", "user2"]
commits.*.message     → ["fix: login", "test: add tests"]
```

**Acceso por índice** — un elemento concreto:
```
labels[0].name        → "bug"
commits[0].id         → "a1b2c3..."
```

**Con `contains()`**:
```yaml
contains(github.event.pull_request.labels.*.name, 'bug')
contains(github.event.pull_request.assignees.*.login, 'dukono')
contains(github.event.pull_request.requested_reviewers.*.login, 'reviewer1')
```

</details>

---

## Funciones

<details>
<summary>Ver todas las funciones disponibles en expresiones ${{ }}</summary>

```jsonc
// contains(buscar_en, valor) — true si buscar_en contiene valor (string o array)
contains('hello world', 'world')                              // → true
contains(github.event.pull_request.labels.*.name, 'bug')     // → true/false

// startsWith(texto, prefijo)
startsWith('refs/heads/main', 'refs/heads/')                  // → true
startsWith(github.event.pull_request.title, 'feat:')          // → true/false

// endsWith(texto, sufijo)
endsWith('my-branch-dev', '-dev')                             // → true
endsWith(github.ref_name, '-dev')                             // → true/false

// format(plantilla, arg0, arg1, ...)  — sustituye {0}, {1}, ...
format('v{0}.{1}', '1', '2')                                  // → "v1.2"
format('Deploy {0} to {1}', inputs.version, inputs.env)       // → "Deploy 1.2.0 to production"

// join(array, separador) — une elementos con el separador
join(github.event.pull_request.labels.*.name, ', ')           // → "bug, urgent"
join(github.event.pull_request.assignees.*.login, ' ')        // → "user1 user2"
join(github.event.commits.*.message, '\n')                    // → "fix: login\nfeat: tests"

// toJSON(valor) — serializa a string JSON
toJSON(github.event.pull_request.labels)                      // → "[{\"id\":123,...}]"
toJSON(github.event)                                           // → payload completo como string

// fromJSON(string) — parsea un string JSON a objeto o valor
fromJSON('{"a":1}').a                                          // → 1
fromJSON(inputs.replicas)                                      // → 3 (número, no string)

// hashFiles(patrón) — SHA-256 de los archivos que coinciden
hashFiles('**/package-lock.json')                              // → "abc123..."
hashFiles('**/pom.xml', '**/build.gradle')                     // → hash combinado

// Funciones de estado — se usan en if: de steps
success()    // true si todos los steps anteriores pasaron (comportamiento por defecto)
failure()    // true si algún step anterior falló
cancelled()  // true si el workflow fue cancelado
always()     // siempre true — garantiza ejecución pase lo que pase
```

> `success()` es el comportamiento por defecto si no hay `if:`.
> `always()` es la única forma de garantizar ejecución incluso tras cancelación.

</details>
