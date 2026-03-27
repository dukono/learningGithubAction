# 🔄 GitHub Actions: Gestión de Ejecuciones

## Índice

1. [Omitir Ejecuciones (skip)](#1-omitir-ejecuciones-skip)
2. [Re-ejecutar Workflows](#2-re-ejecutar-workflows)
3. [Cancelar Ejecuciones](#3-cancelar-ejecuciones)
4. [Deshabilitar / Habilitar Workflows](#4-deshabilitar--habilitar-workflows)
5. [Status Badges](#5-status-badges)
6. [Límites y Expiraciones](#6-límites-y-expiraciones)
7. [Concurrencia: Cancelar Runs Anteriores](#7-concurrencia-cancelar-runs-anteriores)
8. [Preguntas de Examen](#8-preguntas-de-examen)

---

## 1. Omitir Ejecuciones (skip)

### Palabras clave en mensajes de commit

Si el mensaje del commit (o el cuerpo del commit) contiene alguna de estas cadenas, GitHub **omite todos los workflows** con triggers `push` y `pull_request`:

```
[skip ci]
[ci skip]
[no ci]
[skip actions]
[actions skip]
```

**Ejemplos:**
```bash
git commit -m "fix: typo en README [skip ci]"
git commit -m "[skip actions] actualizar documentación"
```

> ⚠️ **Importante**: Solo aplica a `push` y `pull_request`. No afecta a `workflow_dispatch`, `schedule` ni `workflow_call`.

### `skip-checks: true` en PRs

En el título o cuerpo de un Pull Request puedes añadir:
```
skip-checks: true
```

Esto indica que no se deben ejecutar checks de status para ese PR. Diferente a `[skip ci]` — este es a nivel de PR, no de commit.

---

## 2. Re-ejecutar Workflows

### Desde la UI de GitHub

En la pestaña **Actions** → seleccionar un run → botón **"Re-run jobs"**:

| Opción | Descripción |
|---|---|
| **Re-run all jobs** | Re-ejecuta todos los jobs del workflow |
| **Re-run failed jobs** | Re-ejecuta solo los jobs que fallaron |
| **Re-run a specific job** | Re-ejecuta un job individual |

> Los re-runs están disponibles hasta **30 días** después del run original.

### Re-ejecutar con debug logging

En el modal de re-run hay una opción: **"Enable debug logging"**. Al marcarla:
- Activa `ACTIONS_STEP_DEBUG=true` automáticamente
- Activa `ACTIONS_RUNNER_DEBUG=true` para logs del runner
- No necesitas crear/modificar secrets

### Desde GitHub CLI

```bash
# Re-ejecutar todos los jobs del último run fallido
gh run rerun --failed

# Re-ejecutar un run específico
gh run rerun 1234567890

# Re-ejecutar solo jobs fallidos de un run específico
gh run rerun 1234567890 --failed

# Re-ejecutar con debug logging
gh run rerun 1234567890 --debug
```

### Comportamiento de re-runs

- El re-run usa el **mismo commit SHA** que el run original (no el HEAD actual)
- Los **secrets** se vuelven a leer en el momento del re-run (pueden haber cambiado)
- Los **artifacts** del run original no se comparten con el re-run
- El re-run crea un **nuevo run ID**

---

## 3. Cancelar Ejecuciones

### Desde la UI

En la pestaña **Actions** → seleccionar un run en progreso → botón **"Cancel workflow"**.

Cuando se cancela un workflow:
1. GitHub envía señal de cancelación al runner
2. Los steps en ejecución reciben señal `SIGTERM`
3. Los steps con `if: always()` o `if: cancelled()` **sí se ejecutan**
4. Los steps con `if: success()` (default) **no se ejecutan**

**Uso del estado `cancelled()` para limpieza:**
```yaml
jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - name: Desplegar
        id: deploy
        run: ./scripts/deploy.sh

      - name: Limpieza en cancelación
        if: cancelled()
        run: |
          echo "Workflow cancelado — ejecutando limpieza"
          ./scripts/cleanup.sh

      - name: Notificar resultado
        if: always()
        run: echo "Estado final del job: ${{ job.status }}"
```

### Desde GitHub CLI

```bash
# Cancelar un run específico
gh run cancel 1234567890

# Ver runs activos y cancelar
gh run list --status in_progress
gh run cancel <run-id>
```

---

## 4. Deshabilitar / Habilitar Workflows

### Desde la UI

En la pestaña **Actions** → seleccionar el workflow en la barra lateral izquierda → botón **"..."** → **"Disable workflow"**.

El archivo `.yml` permanece en el repositorio — solo se desactiva su ejecución.

### Desde GitHub CLI

```bash
# Deshabilitar un workflow
gh workflow disable "CI Pipeline"
gh workflow disable ci.yml

# Habilitar un workflow
gh workflow enable "CI Pipeline"
gh workflow enable ci.yml

# Listar workflows con su estado
gh workflow list
```

### Auto-deshabilitación por inactividad

GitHub **deshabilita automáticamente** los workflows con trigger `schedule` si el repositorio lleva **60 días sin actividad** (sin commits). GitHub envía un email de aviso antes.

Para reactivarlo: hacer cualquier commit en el repositorio, o habilitarlo manualmente desde la UI.

---

## 5. Status Badges

Los **status badges** muestran el estado del último run de un workflow en un README u otra página.

### Formato de URL

```
https://github.com/{owner}/{repo}/actions/workflows/{workflow-file}/badge.svg
```

**Ejemplo:**
```markdown
![CI Status](https://github.com/mi-org/mi-repo/actions/workflows/ci.yml/badge.svg)
```

### Con branch específico

```
https://github.com/{owner}/{repo}/actions/workflows/{workflow-file}/badge.svg?branch=main
```

```markdown
![CI en main](https://github.com/mi-org/mi-repo/actions/workflows/ci.yml/badge.svg?branch=main)
```

### Obtener la URL desde GitHub

En la pestaña **Actions** → seleccionar el workflow → botón **"..."** → **"Create status badge"** → GitHub genera el Markdown automáticamente.

### Comportamiento

| Estado | Badge |
|---|---|
| Workflow exitoso | Muestra `passing` en verde |
| Workflow fallido | Muestra `failing` en rojo |
| Sin runs todavía | Muestra `no status` |
| Workflow deshabilitado | Muestra el último estado conocido |

> Los badges son **públicos** aunque el repositorio sea privado (muestra el estado pero no el contenido).

---

## 6. Límites y Expiraciones

| Límite | Valor |
|---|---|
| Tiempo máximo de un job | 6 horas (360 min) |
| Tiempo máximo de un workflow | 35 días |
| Tiempo máximo de un step | Hereda del job (configurable con `timeout-minutes`) |
| Re-runs disponibles | Hasta 30 días después del run original |
| Auto-deshabilitación de `schedule` | 60 días de inactividad en el repo |
| Logs disponibles | 90 días (configurable: 1-400 días en settings del repo) |
| Artifacts disponibles | 90 días por defecto (configurable: 1-400 días) |
| Jobs en cola simultáneos | 500 por repositorio |
| Workflows activos simultáneos | 20 por repositorio (en runners GitHub-hosted) |

---

## 7. Concurrencia: Cancelar Runs Anteriores

`concurrency` permite controlar qué pasa cuando múltiples runs del mismo workflow están en progreso.

```yaml
# Nivel workflow: cancela cualquier run anterior del mismo grupo
concurrency:
  group: ${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: true
```

```yaml
# Nivel job: más granular
jobs:
  deploy:
    concurrency:
      group: deploy-${{ github.ref }}
      cancel-in-progress: false  # Espera a que termine en vez de cancelar
```

**Casos de uso:**
- `cancel-in-progress: true` — en feature branches para ahorrar minutos (cancela el run anterior cuando llega uno nuevo)
- `cancel-in-progress: false` — en `main` para no cancelar un deploy a producción en curso

**Con pending:** Si `cancel-in-progress: false` y hay un run en progreso, el nuevo run espera en cola. Si llega un **tercero**, el que estaba en cola (pendiente) se cancela para cederle el lugar al nuevo.

---

## 8. Preguntas de Examen

**P: ¿Qué pasa si el mensaje del commit incluye `[skip ci]` en un workflow con `on: schedule`?**
> Nada — `[skip ci]` solo aplica a triggers `push` y `pull_request`. El workflow con `schedule` se ejecuta igualmente.

**P: ¿Un re-run de un workflow usa el código del commit original o el código actual del repo?**
> El del commit original (mismo SHA). Los secrets, sin embargo, se leen en el momento del re-run.

**P: ¿Qué función tiene `if: cancelled()` en un step?**
> Permite ejecutar un step de limpieza cuando el workflow fue cancelado manualmente. Sin esta condición, el step no se ejecutaría en una cancelación.

**P: ¿Cuántos días están disponibles los re-runs?**
> 30 días desde el run original.

**P: ¿GitHub deshabilita automáticamente los workflows con `schedule`? ¿Cuándo?**
> Sí, tras 60 días sin actividad (commits) en el repositorio.

**P: ¿Cómo obtener el status badge de un workflow?**
> URL: `https://github.com/{owner}/{repo}/actions/workflows/{archivo.yml}/badge.svg`

**P: ¿Qué diferencia hay entre `cancel-in-progress: true` y `false` en `concurrency`?**
> Con `true`: el run anterior se cancela cuando llega uno nuevo. Con `false`: el nuevo espera en cola a que termine el anterior.
