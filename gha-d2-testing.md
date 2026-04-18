# X.12 Testing / Verificación de D2 — Consume and troubleshoot workflows

← [2.11 API REST de GitHub Actions](gha-d2-api-rest-ejecuciones.md) | [Índice](README.md) | [3.1 Tipos de GitHub Actions](gha-action-tipos.md) →

---

Este fichero reúne preguntas de examen estilo GH-200 y ejercicios prácticos que cubren todos los subtemas del dominio D2. Úsalo para verificar que puedes aplicar los conceptos antes de presentar la certificación. Cada pregunta incluye la respuesta correcta y una explicación de por qué las otras opciones son incorrectas.

## Preguntas de examen

**Pregunta 1 — Triggers y UI:** Un workflow tiene `on: push` con `branches: [main]` y `paths: ['src/**']`. Se hace un push a la rama `main` modificando únicamente el fichero `README.md`. ¿Qué ocurre?

A) El workflow se activa porque la rama coincide  
B) El workflow no se activa porque el path no coincide  
C) El workflow se activa pero omite los steps  
D) GitHub muestra un error de configuración  

> **Respuesta correcta: B)** — Cuando se especifican tanto `branches` como `paths`, AMBOS filtros deben coincidir para que el workflow se active. `README.md` no coincide con `src/**`, por lo que el workflow no se dispara. A) es incorrecta porque la rama sola no es suficiente. C) y D) son incorrectas porque GitHub simplemente no activa el workflow.

---

**Pregunta 2 — Logs:** ¿Qué comando de workflow escribe un grupo colapsable en los logs de la UI?

A) `echo "::debug::mensaje"`  
B) `echo "::group::Nombre del grupo"`  
C) `echo "::set-output name=key::value"`  
D) `echo "::add-mask::valor"`  

> **Respuesta correcta: B)** — `::group::` abre un grupo colapsable en la UI; se cierra con `::endgroup::`. A) escribe un mensaje de debug (solo visible con ACTIONS_STEP_DEBUG). C) es la sintaxis antigua de outputs (deprecada). D) enmascara un valor en los logs.

---

**Pregunta 3 — Diagnóstico:** Un step tiene `continue-on-error: true` y devuelve exit code 1. ¿Qué estado muestra el job en la UI?

A) failure  
B) success  
C) warning  
D) cancelled  

> **Respuesta correcta: B) success** — `continue-on-error: true` hace que el job continúe y se marque como exitoso aunque el step falle. El step individual mostrará un icono de advertencia, pero el job completo aparece como `success`. A) sería el resultado sin `continue-on-error`. C) y D) no son estados válidos para este escenario.

---

**Pregunta 4 — Debug avanzado:** ¿Cómo se activa el modo de depuración de runner en GitHub Actions?

A) Añadiendo `debug: true` en el fichero YAML del workflow  
B) Creando un secret llamado `ACTIONS_RUNNER_DEBUG` con valor `true`  
C) Usando el parámetro `--debug` en el comando `gh workflow run`  
D) Activando la opción en la configuración del repositorio  

> **Respuesta correcta: B)** — Los modos de debug se activan mediante secrets del repositorio: `ACTIONS_RUNNER_DEBUG=true` para logs del runner y `ACTIONS_STEP_DEBUG=true` para logs de steps. A) no existe esa propiedad en YAML. C) no existe ese parámetro. D) no existe esa opción de configuración.

---

**Pregunta 5 — Re-ejecución:** Un desarrollador hace clic en "Re-run failed jobs". ¿Qué SHA de commit se usa en la re-ejecución?

A) El SHA del último commit en la rama  
B) El SHA original del commit que disparó la ejecución  
C) Un nuevo SHA generado por GitHub  
D) El SHA del commit de merge  

> **Respuesta correcta: B)** — Una re-ejecución usa exactamente el mismo SHA, contexto y código que la ejecución original. No es un nuevo trigger. A) sería el comportamiento de un nuevo push. C) y D) no existen.

---

**Pregunta 6 — Artefactos:** ¿Cuál es la retención por defecto de los artefactos en GitHub Actions?

A) 30 días  
B) 60 días  
C) 90 días  
D) 400 días  

> **Respuesta correcta: C) 90 días** — La retención por defecto es 90 días, configurable hasta 400 días (o el máximo permitido por el plan). A) y B) son incorrectos. D) es el máximo posible, no el valor por defecto.

---

**Pregunta 7 — Artefactos y API:** ¿Qué endpoint de la REST API se usa para listar los artefactos de un workflow run?

A) `GET /repos/{owner}/{repo}/actions/artifacts`  
B) `GET /repos/{owner}/{repo}/actions/runs/{run_id}/artifacts`  
C) `GET /repos/{owner}/{repo}/artifacts/{run_id}`  
D) `GET /repos/{owner}/{repo}/actions/jobs/{job_id}/artifacts`  

> **Respuesta correcta: A)** — El endpoint correcto para listar artefactos de un repositorio (filtrables por run) es `GET /repos/{owner}/{repo}/actions/artifacts`. B) no es la ruta correcta aunque parezca lógica. C) y D) no son endpoints válidos de la API de Actions.

---

**Pregunta 8 — Matrix:** Una matrix tiene `fail-fast: true` (valor por defecto) y 6 combinaciones. La tercera combinación falla. ¿Cuántas combinaciones aparecen como `cancelled` en la UI?

A) 0  
B) 2  
C) 3  
D) Depende de si las otras ya habían empezado  

> **Respuesta correcta: D)** — Con `fail-fast: true`, GitHub cancela las combinaciones que aún no han terminado cuando una falla. Si las combinaciones 4, 5 y 6 no habían terminado, aparecen como `cancelled` (3 canceladas). Pero si alguna ya había terminado antes del fallo, no se puede cancelar. El número exacto depende del estado de ejecución en ese momento.

---

**Pregunta 9 — Starter workflows:** ¿En qué repositorio de la organización deben almacenarse los starter workflows?

A) En cualquier repositorio público de la organización  
B) En el repositorio especial `.github` de la organización  
C) En el repositorio `.github` de cada usuario miembro  
D) En un repositorio llamado `workflow-templates`  

> **Respuesta correcta: B)** — Los starter workflows se almacenan en la carpeta `.github/workflow-templates/` del repositorio especial `.github` de la organización. A) es incorrecto porque debe ser el repositorio específico `.github`. C) es incorrecto porque es el repositorio de la organización, no de usuarios. D) no existe ese nombre de repositorio especial.

---

**Pregunta 10 — Reusable workflows:** ¿Cuál es la sintaxis correcta para invocar un reusable workflow desde otro workflow?

A) `uses: owner/repo/.github/workflows/ci.yml@main` a nivel de step  
B) `uses: owner/repo/.github/workflows/ci.yml@main` a nivel de job  
C) `action: owner/repo/.github/workflows/ci.yml@main` a nivel de job  
D) `workflow: owner/repo/.github/workflows/ci.yml@main` a nivel de job  

> **Respuesta correcta: B)** — Los reusable workflows se invocan con `uses:` a nivel de **job** (no de step). A) es incorrecto porque `uses:` a nivel de step invoca una action, no un reusable workflow. C) y D) no son palabras clave válidas.

---

**Pregunta 11 — Reusable workflows:** ¿Cuántos niveles de anidamiento de reusable workflows permite GitHub Actions?

A) 2 niveles  
B) 3 niveles  
C) 4 niveles  
D) Sin límite  

> **Respuesta correcta: C) 4 niveles** — GitHub permite un máximo de 4 niveles de anidamiento (el workflow caller cuenta como el primer nivel). D) es incorrecto porque el límite existe. A) y B) son incorrectos.

---

**Pregunta 12 — Composite actions:** ¿En qué contexto de ejecución corre una composite action?

A) En un runner propio creado por la composite action  
B) En el runner del job que la invoca  
C) En un contenedor Docker aislado  
D) En los servidores de GitHub, independientemente del runner del job  

> **Respuesta correcta: B)** — Una composite action se ejecuta en el mismo runner del job caller, compartiendo su entorno, variables y sistema de ficheros. A diferencia de un reusable workflow, no crea un job propio. A), C) y D) describen comportamientos incorrectos.

---

**Pregunta 13 — Deshabilitar workflows:** ¿Cuál es la diferencia principal entre deshabilitar y eliminar un workflow?

A) No hay diferencia; ambas acciones producen el mismo resultado  
B) Deshabilitar es reversible y conserva el historial; eliminar el fichero es permanente para ejecuciones futuras pero conserva el historial existente  
C) Deshabilitar elimina el historial; eliminar el fichero lo conserva  
D) Solo se puede deshabilitar via API; eliminar requiere hacerlo desde la UI  

> **Respuesta correcta: B)** — Deshabilitar un workflow detiene nuevas ejecuciones pero es reversible (se puede habilitar de nuevo). Eliminar el fichero del repositorio impide nuevas ejecuciones permanentemente, pero el historial de ejecuciones anteriores permanece visible en la UI. A) es incorrecto. C) invierte la realidad. D) es incorrecto: ambas acciones están disponibles via UI y API.

---

**Pregunta 14 — API REST:** ¿Qué respuesta HTTP devuelve la API al disparar un `workflow_dispatch` correctamente?

A) 200 OK con el run_id del nuevo workflow  
B) 201 Created con la URL de la ejecución  
C) 204 No Content  
D) 202 Accepted con el estado del workflow  

> **Respuesta correcta: C) 204 No Content** — La API devuelve 204 cuando el dispatch se acepta correctamente, sin cuerpo de respuesta. No devuelve el run_id directamente; hay que consultarlo con una llamada posterior a `/actions/runs`. A), B) y D) son incorrectos.

---

**Pregunta 15 — Logs y API:** ¿Qué ocurre cuando intentas descargar los logs de un job via API y el log ha expirado?

A) La API devuelve 404 Not Found  
B) La API devuelve 410 Gone  
C) La API devuelve un fichero ZIP vacío  
D) La API devuelve 200 con el mensaje "Log expired"  

> **Respuesta correcta: B) 410 Gone** — Cuando los logs han expirado o sido eliminados, la API devuelve 410 Gone, indicando que el recurso existió pero ya no está disponible. A) 404 indicaría que el job no existe. C) y D) no son el comportamiento real de la API.

---

## Ejercicios prácticos

**Ejercicio 1 — Diagnóstico de trigger:** El siguiente workflow no se activa al hacer push a la rama `feature/login` con cambios en `src/auth/login.ts`. Identifica el problema y corrígelo.

```yaml
name: CI

on:
  push:
    branches:
      - main
      - develop
    paths:
      - 'src/**'

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: npm test
```

**Solución:** El problema es que la rama `feature/login` no está incluida en el filtro `branches`. Solo `main` y `develop` activan el workflow. Para incluir ramas de feature, añade un patrón:

```yaml
name: CI

on:
  push:
    branches:
      - main
      - develop
      - 'feature/**'
    paths:
      - 'src/**'

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: npm test
```

---

**Ejercicio 2 — Re-ejecución con debug:** Un workflow falló pero los logs no muestran suficiente información. Describe los pasos para obtener logs de debug en la próxima re-ejecución sin modificar el fichero YAML.

**Solución:**

Opción A — Re-run con debug desde la UI:
1. Ve a la pestaña **Actions** del repositorio.
2. Haz clic en la ejecución fallida.
3. Haz clic en **Re-run failed jobs** (o **Re-run all jobs**).
4. En el diálogo de confirmación, activa la casilla **Enable debug logging**.
5. Haz clic en **Re-run jobs**.

Opción B — Activar debug permanentemente via secrets:
1. Ve a **Settings → Secrets and variables → Actions**.
2. Crea un secret llamado `ACTIONS_RUNNER_DEBUG` con valor `true`.
3. Opcionalmente crea `ACTIONS_STEP_DEBUG` con valor `true` para logs de steps.
4. Lanza una nueva ejecución; los logs adicionales aparecerán automáticamente.

La diferencia: la opción A activa debug solo para esa re-ejecución. La opción B activa debug para todas las ejecuciones hasta que se elimine el secret.

---

**Ejercicio 3 — Reusable workflow:** Completa el workflow caller para que invoque el reusable workflow del repositorio `mi-org/shared-workflows`, en el fichero `.github/workflows/deploy.yml` en la rama `main`, pasando el input `environment` con valor `production` y heredando todos los secrets automáticamente.

```yaml
name: Deploy

on:
  push:
    branches: [main]

jobs:
  deploy:
    # Completa aquí
```

**Solución:**

```yaml
name: Deploy

on:
  push:
    branches: [main]

jobs:
  deploy:
    uses: mi-org/shared-workflows/.github/workflows/deploy.yml@main
    with:
      environment: production
    secrets: inherit
```

Los puntos clave: `uses:` va directamente en el job (no en un step), la ruta incluye `.github/workflows/`, la referencia `@main` apunta a la rama, `with:` pasa los inputs declarados en el callee, y `secrets: inherit` propaga automáticamente todos los secrets del caller sin declararlos individualmente.

---

← [2.11 API REST de GitHub Actions](gha-d2-api-rest-ejecuciones.md) | [Índice](README.md) | [3.1 Tipos de GitHub Actions](gha-action-tipos.md) →
