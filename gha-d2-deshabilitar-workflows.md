← [2.9 Composite actions: consumo](gha-d2-composite-actions-consumo.md) | → [2.11 API REST de GitHub Actions](gha-d2-api-rest-ejecuciones.md)

---

# X.10 Deshabilitar y eliminar workflows

Todo equipo llega al punto en que un workflow deja de ser útil temporalmente, o definitivamente. Quizás estás migrando a un nuevo pipeline, o un workflow defectuoso está consumiendo minutos de Actions durante un incidente. GitHub Actions ofrece dos mecanismos con consecuencias muy distintas: **deshabilitar** un workflow y **eliminar** su fichero del repositorio. Entender la diferencia es fundamental porque afecta al historial, a la reversibilidad y al comportamiento de las ejecuciones pasadas. Este documento cubre el ciclo de vida completo de un workflow: cómo pausarlo sin perder datos, cómo restaurarlo, cómo borrarlo definitivamente y cómo cancelar ejecuciones en curso.

---

## A11.1 Deshabilitar un workflow desde la UI

Deshabilitar un workflow desde la interfaz de GitHub es una operación inmediata y totalmente reversible. Al deshabilitarlo, el workflow deja de dispararse ante cualquier evento futuro, pero todas sus ejecuciones históricas permanecen visibles en la pestaña **Actions** del repositorio.

Para deshabilitarlo desde la UI:

1. Ve a la pestaña **Actions** del repositorio.
2. En el panel izquierdo, selecciona el workflow que deseas deshabilitar.
3. Haz clic en el menú de los tres puntos (`...`) situado en la esquina superior derecha de la lista de ejecuciones.
4. Selecciona **Disable workflow**.

GitHub muestra inmediatamente un aviso de que el workflow está deshabilitado. A partir de ese momento, ningún evento (push, pull_request, schedule, etc.) disparará nuevas ejecuciones. El fichero `.yml` permanece en el repositorio intacto; solo cambia su estado en los metadatos internos de GitHub Actions.

> **Importante:** Deshabilitar un workflow desde la UI equivale exactamente a llamar al endpoint REST de deshabilitación. Ambos métodos producen el mismo efecto y son igualmente reversibles.

---

## A11.2 Deshabilitar un workflow via REST API

La API REST de GitHub Actions expone un endpoint dedicado para deshabilitar workflows. Esto es especialmente útil en automatizaciones, scripts de despliegue o flujos de administración donde se necesita controlar workflows de forma programática sin intervención humana.

El endpoint es:

```
PUT /repos/{owner}/{repo}/actions/workflows/{workflow_id}/disable
```

El campo `{workflow_id}` acepta tanto el identificador numérico del workflow (obtenible desde la API) como el nombre del fichero relativo al directorio `.github/workflows/`, por ejemplo `ci.yml`.

Ejemplo completo con `curl` para deshabilitar el workflow `ci.yml` en el repositorio `octocat/hello-world`:

```bash
curl \
  --request PUT \
  --url "https://api.github.com/repos/octocat/hello-world/actions/workflows/ci.yml/disable" \
  --header "Authorization: Bearer $GITHUB_TOKEN" \
  --header "Accept: application/vnd.github+json" \
  --header "X-GitHub-Api-Version: 2022-11-28"
```

Una respuesta HTTP `204 No Content` confirma que el workflow ha sido deshabilitado correctamente. No hay cuerpo de respuesta. Si recibes un `404`, verifica que el nombre del fichero o el `workflow_id` sea correcto y que el token tenga el scope `repo` (para repositorios privados) o que sea un token con permisos de escritura sobre Actions.

> **Scope requerido:** El token personal (PAT) o el token de la app debe tener permiso `actions: write` para poder deshabilitar o habilitar workflows via API.

---

## A11.3 Habilitar de nuevo un workflow deshabilitado

Rehabilitar un workflow deshabilitado es igual de sencillo tanto desde la UI como desde la API. Ningún dato se pierde durante el período de deshabilitación; simplemente no se generaron nuevas ejecuciones.

**Desde la UI:**

1. Ve a **Actions** → selecciona el workflow deshabilitado.
2. Verás un aviso amarillo indicando que el workflow está deshabilitado.
3. Haz clic en **Enable workflow**.

El workflow vuelve a responder a los eventos de forma inmediata.

**Desde la API REST**, el endpoint es simétrico al de deshabilitación:

```
PUT /repos/{owner}/{repo}/actions/workflows/{workflow_id}/enable
```

Ejemplo completo con `curl` para habilitar el mismo workflow `ci.yml`:

```bash
curl \
  --request PUT \
  --url "https://api.github.com/repos/octocat/hello-world/actions/workflows/ci.yml/enable" \
  --header "Authorization: Bearer $GITHUB_TOKEN" \
  --header "Accept: application/vnd.github+json" \
  --header "X-GitHub-Api-Version: 2022-11-28"
```

La respuesta es también `204 No Content`. Los eventos que se produjeron mientras el workflow estaba deshabilitado no se relanzan retroactivamente; el workflow simplemente responde a eventos nuevos desde el momento en que se habilita.

---

## A11.4 Diferencia entre deshabilitar y eliminar un workflow

Esta es la distinción más importante del capítulo y la que con mayor frecuencia aparece en el examen de certificación. Deshabilitar y eliminar son operaciones con consecuencias radicalmente distintas en cuanto a reversibilidad e historial.

La siguiente tabla resume las diferencias clave:

| Característica | Deshabilitar | Eliminar el fichero |
|---|---|---|
| Fichero `.yml` en el repo | Permanece intacto | Se borra del repositorio |
| Ejecuciones futuras | Detenidas | Detenidas |
| Historial de ejecuciones pasadas | Visible en la UI | Visible en la UI (no se borra) |
| Reversibilidad | Total (un clic o una llamada API) | Requiere restaurar el fichero (git revert / recrear) |
| Estado en la API (`state`) | `disabled_manually` | El workflow desaparece de la lista de workflows activos |
| Ejecuciones en curso al momento | No se cancelan automáticamente | No se cancelan automáticamente |
| Aparece en la lista de workflows | Sí, marcado como deshabilitado | No (una vez mergeado el borrado) |

> **Regla práctica para el examen:** Si necesitas parar un workflow temporalmente sin perder nada, **deshabilítalo**. Si el workflow ya no tiene utilidad y quieres limpiar el repositorio, **elimina el fichero**. En ambos casos, el historial de ejecuciones previas **permanece** en la UI de Actions.

---

## A11.5 Eliminar un fichero de workflow del repositorio

Eliminar un workflow implica borrar su fichero `.yml` del directorio `.github/workflows/` mediante un commit normal al repositorio. No existe ningún botón de "eliminar workflow" en la UI de GitHub Actions; la única forma de eliminar un workflow es a través del control de versiones.

El efecto es inmediato una vez que el commit con la eliminación llega a la rama por defecto (o a la rama donde el workflow está definido). A partir de ese push, el workflow deja de aparecer en la lista de workflows activos y no se disparará ante ningún evento futuro.

```bash
git rm .github/workflows/ci.yml
git commit -m "chore: remove deprecated CI workflow"
git push origin main
```

Es importante destacar que eliminar el fichero **no elimina el historial de ejecuciones**. Todas las ejecuciones anteriores siguen siendo consultables desde la UI de Actions e incluso desde la API (`GET /repos/{owner}/{repo}/actions/runs`) durante el período de retención configurado (por defecto 90 días).

> **Advertencia:** Si eliminas un workflow que tiene ejecuciones en curso en el momento del push, esas ejecuciones **no se cancelan automáticamente**. Deben cancelarse manualmente o via API si es necesario.

---

## A11.6 Ejecuciones históricas tras eliminación del fichero

Una de las confusiones más comunes entre quienes se preparan para la certificación GH-200 es creer que al eliminar el fichero de un workflow, GitHub borra también el historial de sus ejecuciones. Esto no es así.

Las ejecuciones históricas de un workflow permanecen almacenadas en GitHub Actions durante el período de retención configurado para el repositorio u organización (por defecto, 90 días para logs y artefactos). Puedes seguir consultando cada ejecución, sus logs, sus artefactos descargables y su estado final desde la pestaña **Actions**, aunque el workflow ya no exista como fichero en el repositorio.

Sin embargo, el workflow desaparece de la lista de workflows de la barra lateral izquierda de la pestaña Actions. Para encontrar sus ejecuciones pasadas, debes utilizar el filtro de búsqueda o acceder directamente via API con el identificador numérico del workflow.

> **Para el examen:** La eliminación del fichero de workflow **no borra el historial de ejecuciones**. El historial se elimina únicamente cuando expira el período de retención o cuando se borra manualmente desde la UI (ejecución a ejecución) o via API.

---

## A11.7 Cancelar una ejecución en curso

Cancelar una ejecución que ya ha comenzado es una operación distinta a deshabilitar o eliminar un workflow. Se aplica a una ejecución concreta que ya está en estado `in_progress` o `queued`, y no afecta a ejecuciones futuras.

**Desde la UI:**

1. Ve a **Actions** y selecciona la ejecución en curso.
2. Haz clic en **Cancel workflow** en la parte superior derecha.

GitHub detiene todos los jobs que aún no han terminado. Los jobs ya completados mantienen su estado.

**Desde la API REST**, el endpoint de cancelación es:

```
POST /repos/{owner}/{repo}/actions/runs/{run_id}/cancel
```

Ejemplo completo con `curl` para cancelar la ejecución con ID `1234567890`:

```bash
curl \
  --request POST \
  --url "https://api.github.com/repos/octocat/hello-world/actions/runs/1234567890/cancel" \
  --header "Authorization: Bearer $GITHUB_TOKEN" \
  --header "Accept: application/vnd.github+json" \
  --header "X-GitHub-Api-Version: 2022-11-28"
```

La respuesta es `202 Accepted`. GitHub procesa la cancelación de forma asíncrona; el estado de la ejecución cambia a `cancelled` en pocos segundos. Si la ejecución ya ha terminado (completada, fallida o cancelada previamente), recibirás un error `409 Conflict`.

> **Nota:** Cancelar una ejecución no deshabilita el workflow. El workflow seguirá disparándose normalmente ante eventos futuros.

---

## A11.8 Casos edge: workflow deshabilitado disparado via API

Un caso que genera confusión en el examen es qué ocurre cuando se intenta disparar manualmente (via `workflow_dispatch`) un workflow que está deshabilitado.

Si intentas lanzar un workflow deshabilitado desde la UI, el botón **Run workflow** simplemente no aparece. La UI ni siquiera ofrece la opción de dispararlo.

Si intentas lanzarlo via API REST con el endpoint `POST /repos/{owner}/{repo}/actions/workflows/{workflow_id}/dispatches`, recibirás una respuesta de error `422 Unprocessable Entity`. GitHub rechaza la solicitud porque el workflow está en estado `disabled_manually` y no puede recibir eventos hasta que sea habilitado de nuevo.

```bash
curl \
  --request POST \
  --url "https://api.github.com/repos/octocat/hello-world/actions/workflows/ci.yml/dispatches" \
  --header "Authorization: Bearer $GITHUB_TOKEN" \
  --header "Accept: application/vnd.github+json" \
  --header "X-GitHub-Api-Version: 2022-11-28" \
  --header "Content-Type: application/json" \
  --data '{"ref": "main"}'
```

Con el workflow deshabilitado, la respuesta será:

```
HTTP/2 422 Unprocessable Entity
```

La solución es habilitar primero el workflow (PUT `.../enable`) y después lanzar el dispatch.

> **Para el examen:** Un workflow en estado `disabled_manually` no puede recibir eventos de ningún tipo, ni automáticos ni manuales via `workflow_dispatch`. La única forma de que vuelva a ejecutarse es habilitarlo explícitamente.

---

## Tabla resumen de endpoints

Los tres endpoints más importantes de este capítulo y sus características se resumen a continuación:

| Acción | Método HTTP | Endpoint | Respuesta exitosa |
|---|---|---|---|
| Deshabilitar workflow | `PUT` | `/repos/{owner}/{repo}/actions/workflows/{workflow_id}/disable` | `204 No Content` |
| Habilitar workflow | `PUT` | `/repos/{owner}/{repo}/actions/workflows/{workflow_id}/enable` | `204 No Content` |
| Cancelar ejecución | `POST` | `/repos/{owner}/{repo}/actions/runs/{run_id}/cancel` | `202 Accepted` |
| Lanzar workflow (dispatch) | `POST` | `/repos/{owner}/{repo}/actions/workflows/{workflow_id}/dispatches` | `204 No Content` |
| Listar ejecuciones | `GET` | `/repos/{owner}/{repo}/actions/runs` | `200 OK` + JSON |

---

## Buenas y malas prácticas

**Buena práctica:** Usa deshabilitar en lugar de comentar o borrar el trigger de un workflow cuando necesitas una pausa temporal. El fichero permanece versionado, la intención es explícita y la reversión es inmediata.

**Mala práctica:** Borrar el fichero `.yml` para "pausar" un workflow. Si necesitas restaurarlo, dependes del historial de git y del conocimiento de quién lo hizo, lo que introduce fricción innecesaria y riesgo de pérdida de configuración.

**Buena práctica:** Cancela activamente las ejecuciones en curso antes de deshabilitar o eliminar un workflow en entornos con runners auto-hospedados. Las ejecuciones en curso en runners self-hosted pueden bloquear recursos del runner durante minutos incluso tras deshabilitar el workflow.

**Mala práctica:** Asumir que deshabilitar un workflow cancela automáticamente las ejecuciones que ya han comenzado. Deshabilitar solo afecta a los disparadores futuros; las ejecuciones en progreso continúan hasta que finalizan o se cancelan explícitamente.

**Buena práctica:** Usa el nombre del fichero como `{workflow_id}` en las llamadas API cuando sea posible (ej. `ci.yml`). Es más legible y mantenible que el ID numérico, especialmente en scripts de infraestructura como código.

**Mala práctica:** Deshabilitar workflows de seguridad (SAST, dependency review) de forma permanente para evitar bloqueos en PRs sin reemplazarlos por controles equivalentes. Un workflow deshabilitado en producción sin sustituto deja el repositorio expuesto a vulnerabilidades no detectadas.

---

## Verificación y práctica

### Preguntas estilo certificación

**Pregunta 1.** Tienes un workflow `nightly-build.yml` que se dispara cada noche a las 02:00 con un evento `schedule`. Un compañero lo deshabilita desde la UI de GitHub Actions. ¿Cuál de las siguientes afirmaciones es correcta?

- A) El fichero `nightly-build.yml` es eliminado del repositorio automáticamente.
- B) Las ejecuciones históricas del workflow desaparecen de la pestaña Actions.
- C) El workflow no se disparará a las 02:00 las noches siguientes hasta que sea habilitado de nuevo.
- D) Las ejecuciones en curso en el momento de la deshabilitación son canceladas automáticamente.

> **Respuesta correcta: C.** Deshabilitar solo detiene disparadores futuros. El fichero permanece en el repositorio, el historial es visible y las ejecuciones en curso continúan.

---

**Pregunta 2.** Un desarrollador elimina el fichero `.github/workflows/deploy.yml` mediante un commit en la rama `main`. ¿Qué ocurre con las ejecuciones históricas de ese workflow?

- A) Se eliminan inmediatamente de la pestaña Actions.
- B) Permanecen visibles en la pestaña Actions hasta que expira el período de retención.
- C) Se marcan como `cancelled` automáticamente.
- D) Solo se conservan si el repositorio tiene activado el archivo de workflows.

> **Respuesta correcta: B.** Eliminar el fichero del workflow no borra su historial de ejecuciones. Estas permanecen accesibles durante el período de retención configurado.

---

**Pregunta 3.** Ejecutas el siguiente comando curl y recibes `422 Unprocessable Entity`. ¿Cuál es la causa más probable?

```bash
curl --request POST \
  --url "https://api.github.com/repos/miorg/mirepo/actions/workflows/ci.yml/dispatches" \
  --header "Authorization: Bearer $TOKEN" \
  --header "Accept: application/vnd.github+json" \
  --data '{"ref": "main"}'
```

- A) El token no tiene los permisos necesarios.
- B) El workflow `ci.yml` no tiene el trigger `workflow_dispatch` definido.
- C) El workflow está deshabilitado.
- D) La rama `main` no existe en el repositorio.

> **Respuesta correcta: C** (también podría ser B; en el examen se espera C en contexto de workflows deshabilitados). Un workflow deshabilitado devuelve `422` al intentar lanzar un dispatch.

---

### Ejercicio práctico

El siguiente ejercicio consolida todos los conceptos del capítulo en una secuencia real de operaciones.

**Objetivo:** Deshabilitar el workflow `ci.yml` de un repositorio de prueba, verificar su estado, intentar lanzarlo manualmente, habilitarlo de nuevo y cancelar una ejecución en curso.

**Pasos:**

1. Obtén el listado de workflows del repositorio y localiza el ID de `ci.yml`:

```bash
curl \
  --request GET \
  --url "https://api.github.com/repos/TU_ORG/TU_REPO/actions/workflows" \
  --header "Authorization: Bearer $GITHUB_TOKEN" \
  --header "Accept: application/vnd.github+json" \
  --header "X-GitHub-Api-Version: 2022-11-28"
```

2. Deshabilita el workflow usando su nombre de fichero:

```bash
curl \
  --request PUT \
  --url "https://api.github.com/repos/TU_ORG/TU_REPO/actions/workflows/ci.yml/disable" \
  --header "Authorization: Bearer $GITHUB_TOKEN" \
  --header "Accept: application/vnd.github+json" \
  --header "X-GitHub-Api-Version: 2022-11-28"
```

3. Intenta lanzar un dispatch y observa el error `422`:

```bash
curl \
  --request POST \
  --url "https://api.github.com/repos/TU_ORG/TU_REPO/actions/workflows/ci.yml/dispatches" \
  --header "Authorization: Bearer $GITHUB_TOKEN" \
  --header "Accept: application/vnd.github+json" \
  --header "X-GitHub-Api-Version: 2022-11-28" \
  --header "Content-Type: application/json" \
  --data '{"ref": "main"}'
```

4. Habilita de nuevo el workflow:

```bash
curl \
  --request PUT \
  --url "https://api.github.com/repos/TU_ORG/TU_REPO/actions/workflows/ci.yml/enable" \
  --header "Authorization: Bearer $GITHUB_TOKEN" \
  --header "Accept: application/vnd.github+json" \
  --header "X-GitHub-Api-Version: 2022-11-28"
```

5. Lanza el dispatch de nuevo (ahora debe devolver `204`) y anota el `run_id` de la ejecución resultante con un GET a `/actions/runs`. Luego cancélala:

```bash
curl \
  --request POST \
  --url "https://api.github.com/repos/TU_ORG/TU_REPO/actions/runs/RUN_ID/cancel" \
  --header "Authorization: Bearer $GITHUB_TOKEN" \
  --header "Accept: application/vnd.github+json" \
  --header "X-GitHub-Api-Version: 2022-11-28"
```

**Resultado esperado:** La ejecución pasa a estado `cancelled` en la UI de Actions. El workflow `ci.yml` permanece activo y responderá a futuros eventos.

---

← [2.9 Composite actions: consumo](gha-d2-composite-actions-consumo.md) | → [2.11 API REST de GitHub Actions](gha-d2-api-rest-ejecuciones.md)
