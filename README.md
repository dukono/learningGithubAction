# 🚀 GitHub Actions: Formación Completa

Proyecto de aprendizaje estructurado para dominar GitHub Actions desde cero hasta nivel avanzado/certificación.

---

## 🗺️ Ruta de Aprendizaje

Sigue este orden. Cada documento construye sobre el anterior.

```
BLOQUE 1 — FUNDAMENTOS (leer primero)
  └── ARQUITECTURA_TECNICA.md
        Qué es GitHub Actions, cómo funciona internamente,
        jerarquía (workflow → job → step → action),
        ciclo de vida, eventos, runners.
        ★ LEE ESTO PRIMERO. Todo lo demás se construye sobre esto.

BLOQUE 2 — LOS TRES PILARES (dominar bien)
  ├── CONTEXTOS.md
  │     Variables disponibles en el workflow:
  │     github.*, env, steps, needs, matrix, secrets...
  │     Cuándo existe cada una. Arrays con .* y contains().
  │
  ├── EVENTOS.md
  │     Todos los triggers: push, pull_request, schedule,
  │     workflow_dispatch, workflow_call y sus propiedades.
  │
  └── EXPRESIONES.md
        Sintaxis ${{ }}, operadores, funciones:
        contains, startsWith, format, join, toJSON, hashFiles,
        success/failure/always/cancelled.

BLOQUE 3 — CARACTERÍSTICAS AVANZADAS (para workflows reales)
  ├── JOBS_AVANZADOS.md
  │     Outputs (step→job→caller), concurrency,
  │     permissions, matrix dinámica, containers, services,
  │     if: en todos los niveles, timeout, continue-on-error.
  │
  ├── WORKFLOWS_REUTILIZABLES.md
  │     workflow_call, inputs/outputs/secrets,
  │     secrets: inherit, limitaciones (anidamiento, matrix).
  │
  ├── ACTIONS_PERSONALIZADAS.md
  │     Composite actions (shell: obligatorio),
  │     JavaScript actions (ncc build), Docker actions.
  │
  └── SEGURIDAD_AVANZADA.md
        GITHUB_TOKEN permisos por scope, secrets por niveles,
        OIDC sin credenciales, pull_request_target riesgos,
        pinning por SHA, script injection.

BLOQUE 4 — ECOSISTEMA (completar formación)
  ├── CACHE_ARTIFACTS_DEPLOYMENT.md
  │     actions/cache (key, restore-keys), artifacts,
  │     Environments con protection rules, GitHub Pages,
  │     GitHub Packages (GHCR), Dependabot.
  │
  ├── RUNNERS_DEBUGGING.md
  │     GitHub-hosted vs self-hosted, runners efímeros (--ephemeral),
  │     ARC en Kubernetes, facturación y límites por plan,
  │     runner groups, gh CLI, debug logging, tmate, act local.
  │
  └── ADMINISTRACION_Y_CICD.md
        Políticas de organización (allowed actions, fork approvals),
        workflow_run (encadenar workflows, patrón fork seguro),
        GitFlow con Actions, testing avanzado (suites separadas,
        annotations), versionado semántico (release-please),
        notificaciones Slack/Teams, Docker multi-stage + layer cache.

REFERENCIA RÁPIDA (para examen/consulta)
  └── GUIA_RAPIDA_EXAMEN.md
        Cheatsheet completo, flujos de datos visuales,
        10 errores comunes y soluciones,
        20+ preguntas tipo certificación.
```

---

## 🔬 Practicar: Workflows Ejecutables

Todos en `.github/workflows/`. Dispáralos con **workflow_dispatch** desde la pestaña Actions de GitHub, o haciendo push a sus paths.

| # | Workflow | Trigger | Qué aprenderás ejecutándolo |
|---|----------|---------|----------------------------|
| — | `push.yml` | pull_request | Contexto `pull_request` completo, git con GITHUB_TOKEN, expresiones |
| 01 | `01-outputs-artifacts.yml` | workflow_dispatch / push | GITHUB_OUTPUT, GITHUB_ENV, GITHUB_PATH, GITHUB_STEP_SUMMARY, artifacts entre jobs |
| 02 | `02-matrix.yml` | workflow_dispatch / push | Matrix básica, multidimensional, include/exclude, matrix dinámica con fromJSON |
| 03 | `03-reusable-callee.yml` | *(solo llamable por 04)* | — es el workflow reutilizable (callee) |
| 04 | `04-reusable-caller.yml` | workflow_dispatch / push | Llamar workflow reutilizable, pasar inputs/secrets, usar outputs |
| 05 | `05-concurrency-conditions.yml` | workflow_dispatch / push | concurrency, if en jobs/steps, outcome vs conclusion, needs con resultados |
| 06 | `06-containers-services.yml` | workflow_dispatch / push | Container en jobs, services postgres+redis, hostname localhost vs nombre service |
| 07 | `07-cache.yml` | workflow_dispatch / push | Cache hit/miss, restore-keys fallback, cache integrado en setup-node/python |
| 08 | `08-contextos-expresiones.yml` | workflow_dispatch / push | Todos los contextos impresos, todas las funciones de expresión |
| 09 | `09-composite-action.yml` | workflow_dispatch / push | Uso de la composite action local `.github/actions/setup-and-cache/` |

> **Nota sobre el 03:** El `03-reusable-callee.yml` solo tiene `on: workflow_call`, nunca se dispara solo.
> Para ejecutarlo, dispara el `04-reusable-caller.yml` con workflow_dispatch.

---

## 💡 Cómo Usar Este Material

**Si estás empezando:**
→ Lee en orden el Bloque 1, luego el 2, luego el 3.
→ Después de cada documento, ejecuta el workflow relacionado para verlo en práctica.

**Si preparas un examen o evaluación de empresa:**
→ Lee `GUIA_RAPIDA_EXAMEN.md` — tiene el cheatsheet y las preguntas tipo examen.
→ Repasa los errores comunes (sección 4 de ese documento).

**Si buscas algo concreto:**
→ Usa la tabla de más abajo para encontrar el documento correcto.
→ Cada documento tiene una sección "Preguntas de Examen" al final.

---

## 📋 Todos los Documentos

| Documento | Contenido |
|-----------|-----------|
| [`ARQUITECTURA_TECNICA.md`](ARQUITECTURA_TECNICA.md) | Qué es GA, jerarquía, ciclo de vida, eventos, runners, cómo funciona internamente |
| [`CONTEXTOS.md`](CONTEXTOS.md) | github.*, env, job, steps, runner, secrets, vars, strategy, matrix, needs, inputs, arrays `.*`, funciones |
| [`EVENTOS.md`](EVENTOS.md) | push, pull_request, pull_request_target, issues, release, schedule, workflow_dispatch, workflow_call, filtros |
| [`EXPRESIONES.md`](EXPRESIONES.md) | `${{ }}`, operadores, contains/startsWith/endsWith/format/join/toJSON/fromJSON/hashFiles, success/failure/always |
| [`JOBS_AVANZADOS.md`](JOBS_AVANZADOS.md) | Outputs (GITHUB_OUTPUT→job outputs→needs), GITHUB_ENV/PATH/STEP_SUMMARY, concurrency, permissions, matrix, containers, services, if: |
| [`WORKFLOWS_REUTILIZABLES.md`](WORKFLOWS_REUTILIZABLES.md) | workflow_call, inputs tipados, outputs de workflow, secrets: inherit vs explícito, 7 limitaciones |
| [`ACTIONS_PERSONALIZADAS.md`](ACTIONS_PERSONALIZADAS.md) | Composite (shell obligatorio), JavaScript (ncc build, @actions/core), Docker (Dockerfile), cuándo usar cada tipo |
| [`SEGURIDAD_AVANZADA.md`](SEGURIDAD_AVANZADA.md) | GITHUB_TOKEN por scope, secrets repo/env/org, OIDC AWS/Azure/GCP, pull_request_target, SHA pinning, script injection |
| [`CACHE_ARTIFACTS_DEPLOYMENT.md`](CACHE_ARTIFACTS_DEPLOYMENT.md) | actions/cache, key/restore-keys, artifacts upload/download, Environments, Pages, GHCR, Dependabot |
| [`RUNNERS_DEBUGGING.md`](RUNNERS_DEBUGGING.md) | GitHub-hosted specs/multiplicadores, self-hosted, runners efímeros (--ephemeral), ARC/K8s, facturación y límites, runner groups, gh CLI, ACTIONS_STEP_DEBUG, tmate, act |
| [`ADMINISTRACION_Y_CICD.md`](ADMINISTRACION_Y_CICD.md) | Políticas de org (allowed actions, fork approvals), workflow_run, GitFlow, testing avanzado, release-please, Slack/Teams, Docker multi-stage + layer cache |
| [`GUIA_RAPIDA_EXAMEN.md`](GUIA_RAPIDA_EXAMEN.md) | ⭐ Cheatsheet + flujos de datos + errores comunes + preguntas tipo certificación |
