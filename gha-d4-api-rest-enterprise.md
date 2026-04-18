# 4.11 REST API para Gestión de GitHub Actions (Enterprise)

← [4.10 Variables de Configuración](gha-d4-variables-configuracion.md) | [Índice](README.md) | [4.12 Auditoría y Gobernanza](gha-d4-auditoria-gobernanza.md) →

---

La REST API de GitHub permite automatizar la gestión de GitHub Actions a nivel de repositorio y de organización. En entornos enterprise, esta capacidad es fundamental para operaciones como deshabilitar workflows masivamente, mover runners entre grupos o auditar ejecuciones desde pipelines externos. Este fichero cubre los endpoints esenciales, la autenticación requerida y el uso de `gh CLI` como alternativa más legible.

> [CONCEPTO] La API distingue entre operaciones **repo-level** (`/repos/{owner}/{repo}/actions/...`) y **org-level** (`/orgs/{org}/actions/...`). Las operaciones de runner groups y self-hosted runners a nivel de organización requieren scopes adicionales.

## Autenticación y permisos

Para usar la REST API de GitHub Actions en contexto enterprise existen dos mecanismos principales. El primero es un PAT (Personal Access Token) con los scopes adecuados; el segundo es una GitHub App con los permisos correspondientes. Elegir el mecanismo correcto es crítico: los PAT tienen mayor riesgo de exposición si se filtran, mientras que las GitHub Apps permiten permisos más granulares y rotación automática de credenciales.

| Operación | PAT scope requerido | GitHub App permission |
|---|---|---|
| Listar/gestionar workflows | `repo` | `actions:read` / `actions:write` |
| Deshabilitar/habilitar workflows | `repo` | `actions:write` |
| Listar runners de organización | `admin:org` | `organization_self_hosted_runners:read` |
| Crear/gestionar runner groups | `admin:org` | `organization_self_hosted_runners:write` |
| Mover runner a grupo | `admin:org` | `organization_self_hosted_runners:write` |
| Cancelar/re-ejecutar runs | `repo` | `actions:write` |

> [ADVERTENCIA] El scope `admin:org` otorga acceso amplio a la organización. En producción, prefiere GitHub Apps con permisos mínimos necesarios. Nunca almacenes el PAT directamente en el código fuente.

## Endpoints para workflows y runs

La API permite listar, deshabilitar, habilitar y filtrar workflows y sus ejecuciones. Los filtros más útiles para `GET /repos/{owner}/{repo}/actions/runs` son `status`, `branch` y `event`, que permiten construir dashboards de estado o scripts de limpieza automatizados.

Los endpoints principales para gestión de workflows son los siguientes. Cada uno acepta paginación mediante los parámetros `per_page` (máximo 100) y `page`.

```bash
# Listar todos los workflows de un repositorio
GET /repos/{owner}/{repo}/actions/workflows

# Listar ejecuciones con filtros
GET /repos/{owner}/{repo}/actions/runs?status=failure&branch=main&event=push

# Deshabilitar un workflow (requiere workflow_id numérico o nombre de fichero)
PUT /repos/{owner}/{repo}/actions/workflows/{workflow_id}/disable

# Habilitar un workflow previamente deshabilitado
PUT /repos/{owner}/{repo}/actions/workflows/{workflow_id}/enable

# Cancelar una ejecución en curso
POST /repos/{owner}/{repo}/actions/runs/{run_id}/cancel

# Re-ejecutar todos los jobs de una ejecución fallida
POST /repos/{owner}/{repo}/actions/runs/{run_id}/rerun

# Re-ejecutar solo los jobs fallidos
POST /repos/{owner}/{repo}/actions/runs/{run_id}/rerun-failed-jobs
```

> [EXAMEN] `workflow_id` puede ser tanto el ID numérico del workflow como el nombre del fichero YAML (por ejemplo `ci.yml`). Ambos formatos son válidos en los endpoints de la API.

## Endpoints para runners y runner groups

La gestión de self-hosted runners a nivel de organización se realiza a través de endpoints org-level. Estos endpoints permiten listar runners disponibles, asignarlos a grupos y gestionar qué repositorios tienen acceso a cada grupo. Esta segmentación es clave en entornos enterprise para garantizar el principio de menor privilegio.

```bash
# Listar runners de la organización
GET /orgs/{org}/actions/runners

# Listar runner groups de la organización
GET /orgs/{org}/actions/runner-groups

# Crear un nuevo runner group
POST /orgs/{org}/actions/runner-groups
# Body: {"name": "prod-runners", "visibility": "selected", "selected_repository_ids": [123, 456]}

# Mover un runner a un runner group específico
PUT /orgs/{org}/actions/runner-groups/{group_id}/runners/{runner_id}

# Listar runners dentro de un grupo
GET /orgs/{org}/actions/runner-groups/{group_id}/runners

# Eliminar un runner de la organización
DELETE /orgs/{org}/actions/runners/{runner_id}

# Listar repositorios con acceso a un runner group
GET /orgs/{org}/actions/runner-groups/{group_id}/repositories

# Añadir un repositorio a un runner group
PUT /orgs/{org}/actions/runner-groups/{group_id}/repositories/{repository_id}
```

## Paginación de respuestas

La API de GitHub limita las respuestas a 30 elementos por página por defecto. Para operaciones en organizaciones grandes, es imprescindible implementar paginación correctamente. El parámetro `per_page` acepta hasta 100 como valor máximo. La respuesta incluye cabeceras `Link` con URLs para la página siguiente (`next`) y última (`last`).

```bash
# Obtener hasta 100 runners por página, iterando páginas
GET /orgs/{org}/actions/runners?per_page=100&page=1
GET /orgs/{org}/actions/runners?per_page=100&page=2

# Con gh CLI, la paginación se gestiona automáticamente con --paginate
gh api /orgs/{org}/actions/runners --paginate
```

> [CONCEPTO] La cabecera `Link` en la respuesta HTTP contiene las URLs de navegación. El header tiene el formato: `<https://api.github.com/...?page=2>; rel="next"`. Cuando `rel="next"` no aparece, has llegado a la última página.

## Ejemplo central

El siguiente workflow demuestra cómo usar la REST API desde un job de GitHub Actions para deshabilitar automáticamente workflows con demasiadas ejecuciones fallidas consecutivas, y cómo listar el estado de todos los runners de la organización. Este patrón es útil para sistemas de auto-remediación o dashboards de operaciones.

```yaml
name: Gestión Enterprise via API

on:
  schedule:
    - cron: '0 6 * * 1'   # Lunes a las 06:00 UTC
  workflow_dispatch:
    inputs:
      org_name:
        description: 'Nombre de la organización'
        required: true
        default: 'mi-empresa'

jobs:
  auditar-runners:
    name: Listar estado de runners
    runs-on: ubuntu-latest
    permissions:
      contents: read

    steps:
      - name: Listar runners de la organización
        env:
          GH_TOKEN: ${{ secrets.ORG_ADMIN_PAT }}
        run: |
          echo "=== Runners de la organización ==="
          gh api \
            --paginate \
            /orgs/${{ inputs.org_name || 'mi-empresa' }}/actions/runners \
            --jq '.runners[] | "\(.name) | status: \(.status) | os: \(.os)"'

      - name: Listar runner groups
        env:
          GH_TOKEN: ${{ secrets.ORG_ADMIN_PAT }}
        run: |
          echo "=== Runner Groups ==="
          gh api /orgs/${{ inputs.org_name || 'mi-empresa' }}/actions/runner-groups \
            --jq '.runner_groups[] | "\(.id) | \(.name) | visibility: \(.visibility)"'

  deshabilitar-workflows-fallidos:
    name: Deshabilitar workflows con fallos consecutivos
    runs-on: ubuntu-latest
    permissions:
      contents: read

    steps:
      - name: Verificar ejecuciones fallidas recientes
        env:
          GH_TOKEN: ${{ secrets.REPO_PAT }}
          OWNER: mi-empresa
          REPO: mi-repositorio
        run: |
          echo "=== Workflows con fallos recientes ==="

          # Obtener lista de workflows
          WORKFLOWS=$(gh api /repos/$OWNER/$REPO/actions/workflows \
            --jq '.workflows[] | "\(.id) \(.name) \(.state)"')

          echo "$WORKFLOWS"

          # Obtener las últimas 10 ejecuciones fallidas en main
          FAILED_RUNS=$(gh api \
            "/repos/$OWNER/$REPO/actions/runs?status=failure&branch=main&per_page=10" \
            --jq '.workflow_runs | length')

          echo "Ejecuciones fallidas recientes en main: $FAILED_RUNS"

          if [ "$FAILED_RUNS" -ge 5 ]; then
            echo "ALERTA: Demasiados fallos consecutivos detectados"
          fi

  gestionar-runner-group:
    name: Mover runner a grupo de producción
    runs-on: ubuntu-latest
    if: github.event_name == 'workflow_dispatch'
    permissions:
      contents: read

    steps:
      - name: Obtener runner ID por nombre
        id: get-runner
        env:
          GH_TOKEN: ${{ secrets.ORG_ADMIN_PAT }}
        run: |
          RUNNER_ID=$(gh api /orgs/${{ inputs.org_name }}/actions/runners \
            --jq '.runners[] | select(.name == "prod-runner-01") | .id')
          echo "runner_id=$RUNNER_ID" >> $GITHUB_OUTPUT

      - name: Mover runner al grupo de producción
        env:
          GH_TOKEN: ${{ secrets.ORG_ADMIN_PAT }}
          GROUP_ID: '42'
        run: |
          RUNNER_ID="${{ steps.get-runner.outputs.runner_id }}"

          if [ -z "$RUNNER_ID" ]; then
            echo "Runner no encontrado"
            exit 1
          fi

          gh api \
            --method PUT \
            /orgs/${{ inputs.org_name }}/actions/runner-groups/$GROUP_ID/runners/$RUNNER_ID

          echo "Runner $RUNNER_ID movido al grupo $GROUP_ID"
```

## Tabla de elementos clave

Los siguientes parámetros y endpoints son los más frecuentes en el examen GH-200 y en operaciones enterprise reales. La columna "Nivel" indica si el endpoint opera a nivel de repositorio (repo) u organización (org).

| Endpoint / Parámetro | Método | Nivel | Autenticación mínima | Descripción |
|---|---|---|---|---|
| `/repos/{owner}/{repo}/actions/workflows` | GET | repo | `repo` | Lista todos los workflows |
| `/repos/{owner}/{repo}/actions/runs` | GET | repo | `repo` | Lista ejecuciones con filtros |
| `/actions/workflows/{id}/disable` | PUT | repo | `repo` | Deshabilita un workflow |
| `/actions/workflows/{id}/enable` | PUT | repo | `repo` | Habilita un workflow |
| `/actions/runs/{run_id}/cancel` | POST | repo | `repo` | Cancela una ejecución |
| `/actions/runs/{run_id}/rerun-failed-jobs` | POST | repo | `repo` | Re-ejecuta solo jobs fallidos |
| `/orgs/{org}/actions/runners` | GET | org | `admin:org` | Lista runners de la org |
| `/orgs/{org}/actions/runner-groups` | GET/POST | org | `admin:org` | Lista/crea runner groups |
| `/runner-groups/{id}/runners/{id}` | PUT | org | `admin:org` | Mueve runner a grupo |
| `per_page` | param | ambos | — | Máx. 100 resultados por página |
| `--paginate` | gh CLI | ambos | — | Itera automáticamente todas las páginas |

## Buenas y malas prácticas

**Hacer:**
- Usar `gh api --paginate` en lugar de iterar manualmente páginas — razón: evita errores de paginación y simplifica el código de automatización
- Usar GitHub Apps en lugar de PAT para operaciones org-level — razón: permisos más granulares, sin expiración manual y mejor trazabilidad en el audit log
- Filtrar ejecuciones con `status`, `branch` y `event` antes de procesar — razón: reduce el volumen de datos y mejora el rendimiento de los scripts
- Verificar el `state` del workflow antes de deshabilitar — razón: llamar a `/disable` sobre un workflow ya deshabilitado no falla, pero genera confusión en los logs

**Evitar:**
- Hardcodear PAT en el código del workflow — razón: cualquier persona con acceso al repositorio puede leer el token y comprometer toda la organización
- Ignorar la paginación en organizaciones con muchos runners — razón: solo verás los primeros 30 resultados y tomarás decisiones sobre datos incompletos
- Usar `DELETE /runners/{runner_id}` sin verificar que el runner está offline — razón: eliminar un runner activo puede interrumpir ejecuciones en curso
- Confundir deshabilitar un workflow (API) con eliminar el fichero YAML — razón: deshabilitar via API es reversible y no modifica el repositorio; eliminar el fichero es destructivo

> [ADVERTENCIA] El endpoint `PUT /disable` no elimina ni modifica el fichero `.yml` del workflow. El workflow sigue existiendo en el repositorio pero no se disparará ante nuevos eventos hasta que se habilite de nuevo con `PUT /enable`.

## Verificación y práctica

**P1: ¿Cuál es el scope mínimo de PAT necesario para mover un self-hosted runner a un runner group de organización?**
a) `repo`
b) `workflow`
c) `admin:org`
d) `read:org`

**Respuesta: c)** — Las operaciones sobre runner groups de organización requieren `admin:org`. El scope `repo` solo cubre operaciones a nivel de repositorio. `workflow` permite modificar ficheros de workflow pero no gestionar runners. `read:org` solo da acceso de lectura a datos de la organización.

---

**P2: Un workflow tiene `state: disabled_manually`. ¿Qué endpoint restaura su funcionamiento?**
a) `POST /repos/{owner}/{repo}/actions/workflows/{id}/enable`
b) `PUT /repos/{owner}/{repo}/actions/workflows/{id}/enable`
c) `PATCH /repos/{owner}/{repo}/actions/workflows/{id}`
d) `DELETE /repos/{owner}/{repo}/actions/workflows/{id}/disable`

**Respuesta: b)** — El endpoint correcto es `PUT .../enable`. El método HTTP es `PUT`, no `POST` ni `PATCH`. No existe un endpoint `DELETE .../disable`.

---

**P3: Al paginar resultados con `gh api`, ¿qué flag obtiene automáticamente todas las páginas?**
a) `--all-pages`
b) `--max-results 1000`
c) `--paginate`
d) `--page all`

**Respuesta: c)** — El flag `--paginate` en `gh CLI` itera automáticamente todas las páginas siguiendo las cabeceras `Link` de la respuesta. Los otros flags no existen en `gh CLI`.

---

**Ejercicio práctico:** Escribe un step de workflow que liste todos los runners offline de la organización `acme-corp` usando `gh CLI` con paginación y filtrando con `jq`.

```yaml
- name: Listar runners offline
  env:
    GH_TOKEN: ${{ secrets.ORG_ADMIN_PAT }}
  run: |
    echo "Runners offline en acme-corp:"
    gh api \
      --paginate \
      /orgs/acme-corp/actions/runners \
      --jq '.runners[] | select(.status == "offline") | "\(.id) | \(.name) | \(.os)"'
```

---

← [4.10 Variables de Configuración](gha-d4-variables-configuracion.md) | [Índice](README.md) | [4.12 Auditoría y Gobernanza](gha-d4-auditoria-gobernanza.md) →
