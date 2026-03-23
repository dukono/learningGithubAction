# 🗺️ Mapa de Aprendizaje: GitHub Actions

Este documento es la **puerta de entrada** a toda la documentación. Describe qué aprenderás en cada archivo, el problema que resuelve y cuándo consultarlo.

---

## Orden de lectura recomendado

### 1. [`ARQUITECTURA_TECNICA.md`](ARQUITECTURA_TECNICA.md)
**Empieza aquí.** Explica los conceptos fundamentales: qué es un workflow, un job, un step, un runner, un evento. Define el vocabulario que usan todos los demás documentos.
> 📌 Consultar cuando: no entiendes un término, quieres saber cómo se comunican las partes del sistema, o necesitas entender el ciclo de vida de una ejecución.

---

### 2. [`EVENTOS.md`](EVENTOS.md)
Cubre **todos los disparadores** de workflows: `push`, `pull_request`, `schedule`, `workflow_dispatch`, `workflow_run`, `repository_dispatch` y más. Explica qué es `pull_request_target` y en qué se diferencia de `pull_request`, qué es `workflow_run` y por qué existe frente a `needs:`, y cómo usar el cron de `schedule`.
> 📌 Consultar cuando: necesitas que tu workflow se dispare bajo una condición específica o quieres entender qué datos trae cada evento.

---

### 3. [`CONTEXTOS.md`](CONTEXTOS.md)
Referencia completa de todos los **objetos de datos disponibles** en las expresiones `${{ }}`: `github`, `env`, `job`, `steps`, `runner`, `secrets`, `vars`, `matrix`, `needs`, `inputs`. Incluye qué propiedades tiene cada contexto y cuándo están disponibles.
> 📌 Consultar cuando: necesitas saber el nombre exacto de una propiedad (ej: `github.event.pull_request.draft`), o cuando una expresión devuelve vacío y no sabes por qué.

---

### 4. [`EXPRESIONES.md`](EXPRESIONES.md)
Explica la **sintaxis de expresiones** `${{ }}`, cuándo se evalúan, todos los operadores y las funciones integradas: `contains`, `startsWith`, `format`, `join`, `toJSON`, `fromJSON`, `hashFiles`, `success`, `failure`, `always`, `cancelled`.
> 📌 Consultar cuando: necesitas escribir un `if:` complejo, usar una función de string o hash, o entender la diferencia entre `outcome` y `conclusion`.

---

### 5. [`JOBS_AVANZADOS.md`](JOBS_AVANZADOS.md)
Profundiza en características avanzadas de los jobs: cómo pasar datos entre steps y entre jobs (`GITHUB_OUTPUT`, `outputs:`), `concurrency` para evitar deploys duplicados, `permissions` para el token, `matrix` para ejecutar en paralelo con distintas configuraciones, `containers` y `services` para usar Docker, condiciones `if:` en todos los niveles, y `needs:` para encadenar jobs.
> 📌 Consultar cuando: necesitas que un job use una base de datos en los tests, quieres ejecutar en varias versiones de Node/Python a la vez, o necesitas pasar el resultado de un job a otro.

---

### 6. [`WORKFLOWS_REUTILIZABLES.md`](WORKFLOWS_REUTILIZABLES.md)
Explica cómo crear workflows que se pueden **llamar desde otros workflows** (como funciones): sintaxis del callee (`workflow_call`), del caller (`uses:` a nivel de job), inputs tipados, outputs, secretos heredados, y las 7 limitaciones clave.
> 📌 Consultar cuando: tienes los mismos pasos de CI/CD copiados en varios workflows y quieres eliminar la duplicación.

---

### 7. [`ACTIONS_PERSONALIZADAS.md`](ACTIONS_PERSONALIZADAS.md)
Explica qué es una **action** (la unidad reutilizable que se usa con `uses:` en un step) y los tres tipos: Composite (YAML/shell), JavaScript (Node.js con `@actions/core`) y Docker (cualquier lenguaje). Incluye cómo publicar en el Marketplace.
> 📌 Consultar cuando: quieres crear tu propia action reutilizable, entender cómo funciona una action que estás usando, o necesitas empaquetar lógica compleja en un step.

---

### 8. [`SEGURIDAD_AVANZADA.md`](SEGURIDAD_AVANZADA.md)
Cubre las principales superficies de ataque y cómo protegerse: permisos del `GITHUB_TOKEN`, niveles de secrets, OIDC para autenticarse en clouds sin credenciales almacenadas, los riesgos de `pull_request_target`, pinning de actions por SHA, y script injection.
> 📌 Consultar cuando: tu workflow interactúa con servicios externos, acepta PRs de forks, o quieres revisar las mejores prácticas de seguridad antes de publicar un workflow.

---

### 9. [`CACHE_ARTIFACTS_DEPLOYMENT.md`](CACHE_ARTIFACTS_DEPLOYMENT.md)
Explica cómo acelerar workflows con **caché** de dependencias, cómo compartir archivos entre jobs con **artifacts**, cómo usar **Environments** para controlar deploys con aprobación manual, cómo desplegar en **GitHub Pages**, y cómo publicar imágenes Docker en **GHCR**.
> 📌 Consultar cuando: tus workflows son lentos por reinstalar dependencias, necesitas pasar un build de un job a otro, o quieres hacer deploy con aprobación manual.

---

### 10. [`RUNNERS_DEBUGGING.md`](RUNNERS_DEBUGGING.md)
Explica los tipos de **runners** (hosted vs self-hosted), cuándo usar cada uno, cómo organizarlos en grupos, cómo usar la **GitHub CLI** en workflows, cómo activar logs de debug, y cómo probar workflows localmente con **`act`**.
> 📌 Consultar cuando: necesitas hardware especial, tus workflows fallan con errores poco claros, o quieres iterar rápido probando sin hacer push.

---

### 11. [`ADMINISTRACION_Y_CICD.md`](ADMINISTRACION_Y_CICD.md)
Cubre temas de nivel organizativo y pipelines completos: políticas de org (qué actions pueden usar los repos), `workflow_run` para encadenar workflows distintos de forma segura, estrategia GitFlow, separación de suites de tests, versionado semántico automático con `release-please` y commits convencionales, notificaciones a Slack/Teams, y Docker multi-stage.
> 📌 Consultar cuando: gestionas Actions en una organización, quieres automatizar releases, o necesitas construir un pipeline CI/CD completo de nivel empresarial.

---

### 12. [`GUIA_RAPIDA_EXAMEN.md`](GUIA_RAPIDA_EXAMEN.md)
**Cheatsheet de repaso.** Sintaxis mínima, flujos de datos, errores comunes, tablas comparativas y preguntas tipo certificación. No es para aprender — es para repasar antes de un examen o como referencia rápida durante el trabajo.
> 📌 Consultar cuando: ya conoces el tema y necesitas recordar la sintaxis exacta o comparar opciones rápidamente.

---

## Relación entre documentos

```
ARQUITECTURA_TECNICA  ← base conceptual para todos

EVENTOS ──────────────┐
CONTEXTOS ────────────┤→ EXPRESIONES  (sintaxis para usar todo lo anterior)
                      │
JOBS_AVANZADOS ───────┤
WORKFLOWS_REUTILIZABLES┤→ ADMINISTRACION_Y_CICD  (patrones completos)
ACTIONS_PERSONALIZADAS ┤
                      │
SEGURIDAD_AVANZADA ───┤→ aplica a cualquier workflow
CACHE_ARTIFACTS ──────┘

RUNNERS_DEBUGGING ←── útil en cualquier momento del desarrollo

GUIA_RAPIDA_EXAMEN ←── resumen de todo lo anterior
```
