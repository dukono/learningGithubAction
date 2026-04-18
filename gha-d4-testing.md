# D4 — Verificación: Manage GitHub Actions for the Enterprise

← [4.12 Auditoría y Gobernanza](gha-d4-auditoria-gobernanza.md) | [Índice](README.md) | [5.1 Environment Protections](gha-environment-protections-security.md) →

---

## Preguntas de examen (estilo GH-200)

**P1: ¿Qué política de organización impide que los workflows usen acciones de repositorios externos no aprobados?**

a) `required_workflows`
b) `allowed_actions: selected`
c) `permissions: read-all`
d) `default_workflow_permissions: read`

**Respuesta: B** — La política `allowed_actions: selected` con `allow_list` restringe qué acciones pueden ejecutarse; solo se permiten las explícitamente aprobadas.

---

**P2: Un workflow reutilizable define `on: workflow_call`. ¿Qué keyword usa el workflow llamante para invocarlo?**

a) `uses:` dentro de un step
b) `uses:` dentro de un job
c) `run:` con la ruta del workflow
d) `trigger: reusable`

**Respuesta: B** — `uses:` se especifica a nivel de job (no de step) para invocar un workflow reutilizable, p. ej. `uses: org/repo/.github/workflows/ci.yml@main`.

---

**P3: ¿Cuál es el alcance máximo predeterminado del `GITHUB_TOKEN` en una organización con política restrictiva `read`?**

a) Escritura en todos los repositorios de la organización
b) Solo lectura en el repositorio que ejecuta el workflow
c) Lectura en todos los repositorios de la organización
d) Sin acceso a la API de GitHub

**Respuesta: B** — Con `default_workflow_permissions: read`, el token tiene permisos de lectura únicamente en el repositorio actual; no tiene acceso a otros repos de la organización.

---

**P4: ¿En qué nivel se configuran los Runner Groups para restringir qué repositorios pueden usar self-hosted runners?**

a) Solo a nivel de repositorio
b) A nivel de organización y empresa
c) Solo a nivel de empresa
d) A nivel de workflow mediante `runs-on`

**Respuesta: B** — Los Runner Groups se administran en la configuración de organización y empresa; desde ahí se define qué repositorios o workflows tienen acceso a cada grupo.

---

**P5: Un Workflow Template se almacena en un repositorio especial de la organización. ¿Cuál es ese repositorio?**

a) `.github-templates`
b) `workflow-templates`
c) `.github`
d) `actions-templates`

**Respuesta: C** — Los Workflow Templates se almacenan en el repositorio `.github` de la organización, dentro del directorio `workflow-templates/`.

---

**P6: ¿Qué archivo acompaña obligatoriamente a cada Workflow Template para que aparezca en la interfaz de "Suggested workflows"?**

a) Un archivo `README.md`
b) Un archivo `.properties.json` con el mismo nombre base
c) Un archivo `action.yml`
d) Un archivo `template.json`

**Respuesta: B** — Cada template `.yml` debe tener un archivo `.properties.json` homónimo (p. ej. `ci.properties.json`) con campos `name`, `description` y `iconName`.

---

**P7: ¿Qué mecanismo permite pasar secrets desde un workflow llamante a un workflow reutilizable sin enumerarlos explícitamente?**

a) `secrets: read-all`
b) `inherit` en el bloque `secrets:`
c) Exportarlos como variables de entorno
d) `permissions: secrets: write`

**Respuesta: B** — La directiva `secrets: inherit` propaga automáticamente todos los secrets del contexto del workflow llamante al workflow reutilizable invocado.

---

**P8: Las IP Allow Lists de GitHub Enterprise bloquean el tráfico entrante. ¿Qué ocurre con los self-hosted runners cuando se habilita esta función?**

a) Los runners dejan de funcionar automáticamente
b) Las IPs de los runners deben añadirse manualmente a la lista
c) GitHub añade las IPs de los runners de forma automática
d) Solo afecta a GitHub-hosted runners

**Respuesta: B** — Al habilitar IP Allow Lists, las IPs de los self-hosted runners deben añadirse explícitamente; de lo contrario, no podrán conectarse a la API de GitHub.

---

**P9: ¿Qué componente de ARC (Actions Runner Controller) gestiona el ciclo de vida de los pods de runner en Kubernetes?**

a) `RunnerSet`
b) `RunnerDeployment`
c) El controlador ARC (`actions-runner-controller`)
d) `HorizontalRunnerAutoscaler`

**Respuesta: C** — El controlador ARC es el operador de Kubernetes que reconcilia los recursos `RunnerDeployment`/`RunnerSet` y gestiona la creación y eliminación de pods runner.

---

**P10: Un repositorio necesita un secret con diferente valor en producción y en staging. ¿Cuál es la solución recomendada?**

a) Crear dos secrets con nombres distintos en el repositorio
b) Usar Environment Secrets vinculados a los environments `production` y `staging`
c) Codificar los valores en variables de entorno del workflow
d) Usar un secret de organización con `selected_repositories`

**Respuesta: B** — Los Environment Secrets permiten definir el mismo nombre de secret con distintos valores por environment, y solo son accesibles cuando el job referencia ese environment.

---

**P11: ¿Qué endpoint REST permite listar todas las ejecuciones de workflow de un repositorio filtradas por estado `failure`?**

a) `GET /repos/{owner}/{repo}/actions/runs?conclusion=failure`
b) `GET /repos/{owner}/{repo}/actions/workflows/runs?status=failure`
c) `GET /repos/{owner}/{repo}/actions/runs?status=failure`
d) `POST /repos/{owner}/{repo}/actions/runs/filter`

**Respuesta: C** — El endpoint `GET /repos/{owner}/{repo}/actions/runs` acepta el parámetro `status` (o `conclusion`) para filtrar; `status=failure` devuelve las ejecuciones fallidas.

---

**P12: ¿Qué permiso mínimo de GITHUB_TOKEN se requiere para que un job pueda escribir en el registro de paquetes (GHCR)?**

a) `contents: write`
b) `packages: write`
c) `deployments: write`
d) `id-token: write`

**Respuesta: B** — Para publicar imágenes en GitHub Container Registry se necesita el permiso `packages: write` en el bloque `permissions:` del job o workflow.

---

**P13: En GitHub Enterprise Cloud, ¿qué audit log event indica que un workflow fue habilitado manualmente desde la UI?**

a) `workflow.enable`
b) `workflows.enabled`
c) `actions.enable_workflow`
d) `repo.actions_workflow_enabled`

**Respuesta: A** — El evento `workflow.enable` queda registrado en el audit log cuando un administrador o propietario habilita un workflow deshabilitado.

---

**P14: ¿Cómo se restringe un workflow reutilizable para que solo pueda ser invocado desde repositorios dentro de la misma organización?**

a) Añadiendo `org-only: true` en el archivo del workflow
b) Configurando la visibilidad del repositorio como privado y la política de acceso de Actions a `selected repos`
c) Usando `if: github.repository_owner == 'org-name'` en todos los jobs
d) Habilitando `required_workflow` en la organización

**Respuesta: B** — Mantener el repositorio privado y limitar el acceso a Actions mediante la política de organización impide que repos externos invoquen el workflow reutilizable.

---

**P15: ¿Qué campo del archivo `action.yml` de una acción compuesta declara los parámetros que recibe?**

a) `env:`
b) `args:`
c) `inputs:`
d) `params:`

**Respuesta: C** — El campo `inputs:` del `action.yml` declara los parámetros de entrada con sus descripciones, valores por defecto y si son obligatorios.

---

**P16: Un self-hosted runner lleva 30 días sin conectarse. ¿Qué estado muestra la UI de GitHub y qué acción procede?**

a) `offline`; se elimina automáticamente tras 14 días de inactividad
b) `idle`; debe eliminarse manualmente
c) `offline`; debe eliminarse manualmente o via API
d) `stale`; GitHub lo desregistra automáticamente tras 30 días

**Respuesta: C** — GitHub marca el runner como `offline` pero no lo elimina automáticamente; debe retirarse manualmente desde Settings o con `DELETE /repos/{owner}/{repo}/actions/runners/{runner_id}`.

---

**P17: ¿Qué política de empresa permite que workflows de un repositorio público usen acciones solo de repositorios verificados y de la misma organización?**

a) `allowed_actions: local_only`
b) `allowed_actions: selected` con `verified_allowed: true` y `patterns_allowed`
c) `allow_github_owned_actions: true`
d) `enterprise_actions_policy: restricted`

**Respuesta: B** — La política `allowed_actions: selected` permite combinar `github_owned_allowed`, `verified_allowed` y `patterns_allowed` para un control granular sobre qué acciones están permitidas.

---

**P18: ¿Qué herramienta CLI permite registrar interactivamente un nuevo self-hosted runner en un repositorio?**

a) `gh runner add`
b) El script `config.sh` descargado desde la página de configuración del runner
c) `actions-runner register --url`
d) `gh actions runner configure`

**Respuesta: B** — El script `config.sh` (o `config.cmd` en Windows) es el instalador oficial que registra el runner contra la API de GitHub usando un token de registro de un solo uso.

---

## Checklist de conocimiento D4

- Configurar políticas `allowed_actions` (all / local_only / selected) a nivel organización y empresa
- Entender `secrets: inherit` vs secrets explícitos en workflows reutilizables
- Distinguir los niveles de secrets: repositorio, environment y organización
- Saber que `GITHUB_TOKEN` tiene alcance limitado al repo actual; para cross-repo usar PAT o GitHub App
- Gestionar Runner Groups: creación, asignación de repos y etiquetas en `runs-on`
- Conocer el ciclo de vida de runners: online / offline / eliminación via API
- Entender ARC: controlador Kubernetes, `RunnerDeployment`, `HorizontalRunnerAutoscaler`
- Crear Workflow Templates en `.github/workflow-templates/` con su `.properties.json`
- Invocar workflows reutilizables con `uses:` a nivel de job y pasar `inputs:` y `secrets:`
- Configurar IP Allow Lists y añadir las IPs de self-hosted runners
- Usar la REST API para listar, cancelar y re-ejecutar workflow runs
- Leer audit logs para eventos `workflow.enable`, `workflow.disable`, `secret.create`
- Aplicar `default_workflow_permissions` para endurecer el alcance del GITHUB_TOKEN
- Diferenciar variables de entorno (`vars` context) de secrets (`secrets` context)
- Validar que runners ephemeral (ARC) no persisten estado entre ejecuciones

---

← [4.12 Auditoría y Gobernanza](gha-d4-auditoria-gobernanza.md) | [Índice](README.md) | [5.1 Environment Protections](gha-environment-protections-security.md) →
