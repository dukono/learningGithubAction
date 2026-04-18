<!-- anterior: [2.3.2 Depuración avanzada](gha-d2-debug-avanzado.md) | siguiente: [2.5 Artefactos: consumo, gestión y API](gha-d2-artefactos.md) -->

# 2.4 Re-ejecución de workflows y jobs

Cuando un workflow falla, la primera reacción natural es volver a ejecutarlo. Sin embargo, disparar el evento original de nuevo —hacer un nuevo push, reabrir un PR— no siempre es posible ni deseable: puede ensuciar el historial de commits, crear artefactos duplicados o simplemente no reproducir las mismas condiciones. Para resolver esto, GitHub Actions ofrece el mecanismo de re-run: una forma de reiniciar una ejecución ya registrada sin crear un nuevo evento, manteniendo el mismo SHA, el mismo contexto y la misma entrada de datos que tuvo la ejecución original.

La re-ejecución es especialmente útil cuando el fallo no fue causado por el código en sí sino por un problema externo transitorio: un servicio de terceros intermitente, una condición de carrera en la red, o un runner que se quedó sin recursos. En esos casos, reintentar es la acción correcta, y GitHub Actions permite hacerlo desde la interfaz web, desde la CLI de GitHub o mediante la REST API.

## Tipos de re-ejecución

Existen tres modalidades principales de re-ejecución, cada una con un alcance distinto. Comprender cuál usar en cada situación es fundamental tanto para el trabajo diario como para el examen de certificación.

La siguiente tabla resume las tres modalidades, su alcance y el momento en que se recomienda cada una:

| Modalidad | Alcance | Cuándo usarla |
|---|---|---|
| Re-run all jobs | Todos los jobs del workflow | El fallo es generalizado o se quiere un estado limpio |
| Re-run failed jobs | Solo los jobs con estado `failure` o `cancelled` | El fallo fue puntual y los jobs exitosos no deben repetirse |
| Re-run a specific job | Un único job concreto | Disponible via API; desde la UI no existe esta opción directa |

## A4.1 Re-run all jobs

La opción "Re-run all jobs" reinicia el workflow completo desde cero. Todos los jobs, incluidos los que ya habían tenido éxito, se vuelven a ejecutar. Esto es útil cuando se sospecha que el estado parcialmente completado puede haber dejado efectos secundarios, o cuando se quiere garantizar un resultado totalmente limpio.

Para ejecutarla desde la interfaz web, navega a la pestaña **Actions** del repositorio, selecciona la ejecución en cuestión y haz clic en el botón **Re-run all jobs** situado en la esquina superior derecha del panel de la ejecución. GitHub solicitará una confirmación antes de proceder.

> **Importante:** Re-run all jobs consume minutos de Actions para todos los jobs, incluidos los que ya habían pasado. En repositorios privados con límite de minutos mensual, esto tiene un coste real.

## A4.2 Re-run failed jobs

La opción "Re-run failed jobs" es la más habitual en la práctica. Reinicia únicamente los jobs que terminaron en estado `failure` o `cancelled`, respetando los resultados de los jobs que ya completaron con éxito. Esto ahorra tiempo y minutos de Actions cuando el workflow tiene muchos jobs y solo una parte falló.

Desde la interfaz web, el botón **Re-run failed jobs** aparece junto a "Re-run all jobs" en el panel de la ejecución, pero solo está activo cuando la ejecución tiene al menos un job fallido. GitHub respeta las dependencias declaradas con `needs`: si un job fallido era dependencia de otro, ambos se re-ejecutan aunque el segundo no hubiera fallado directamente.

> **Nota:** Los jobs que dependen (via `needs`) de un job re-ejecutado también se vuelven a ejecutar, aunque ellos mismos no hayan fallado. Esto mantiene la coherencia del grafo de dependencias.

## A4.3 Re-run un job específico

Desde la interfaz web de GitHub no existe un botón para re-ejecutar un único job de forma arbitraria. Sin embargo, la REST API sí permite hacerlo mediante el endpoint `POST /repos/{owner}/{repo}/actions/runs/{run_id}/jobs/{job_id}/rerun`. Esta capacidad es relevante en automatizaciones avanzadas donde un sistema externo detecta el job fallido y lo reintenta sin intervención humana.

La limitación principal es que, igual que con re-run failed jobs, GitHub respeta el grafo de dependencias: si el job especificado tiene dependencias que también fallaron, esas dependencias se re-ejecutan primero.

## A4.4 Re-run con modo debug habilitado

Tanto "Re-run all jobs" como "Re-run failed jobs" ofrecen una opción adicional en el diálogo de confirmación: **Enable debug logging**. Cuando esta casilla está marcada, GitHub Actions activa automáticamente las variables de entorno `RUNNER_DEBUG=1` y `ACTIONS_STEP_DEBUG=true` para esa re-ejecución específica, sin necesidad de modificar el repositorio ni los secretos.

Esto es muy útil para diagnosticar fallos que no producen suficiente información en el log normal. El modo debug genera logs mucho más detallados: cada paso del runner imprime variables internas, comandos ejecutados y valores intermedios. Una vez identificado el problema, la siguiente re-ejecución puede hacerse sin debug para no generar logs innecesariamente grandes.

> **Buena práctica:** Usa "Enable debug logging" solo cuando el log normal no sea suficiente para diagnosticar el fallo. Los logs en modo debug pueden contener información sensible del entorno de ejecución.

## A4.5 Diferencia entre re-run y nuevo trigger

Este concepto es uno de los más preguntados en la certificación porque tiene implicaciones directas en el comportamiento del workflow. Cuando se hace una re-ejecución, GitHub Actions usa el SHA del commit original, no el HEAD actual del repositorio. Es decir, aunque alguien haya hecho push de nuevos commits al mismo branch entre el momento del fallo y el momento de la re-ejecución, la re-ejecución corre con el código exacto que tenía cuando se disparó la primera vez.

Además, el contexto `github` de la re-ejecución conserva los mismos valores: `github.sha`, `github.ref`, `github.actor` y `github.event` son idénticos a los de la ejecución original. Esto garantiza reproducibilidad: si el código no ha cambiado en el runner, debería comportarse igual.

Un nuevo trigger, en cambio, crea una nueva entrada en el historial de ejecuciones con un nuevo `run_id`, un nuevo `run_number`, y si hay commits nuevos en el branch, usa el SHA más reciente. No es reproducible respecto a la ejecución fallida original.

La siguiente tabla resume las diferencias clave:

| Característica | Re-run | Nuevo trigger |
|---|---|---|
| SHA utilizado | El del commit original | El HEAD actual del branch |
| `run_id` | El mismo | Nuevo |
| `run_number` | El mismo | Incrementado |
| Contexto `github.event` | Idéntico al original | Nuevo evento generado |
| Registro en historial | Actualiza la ejecución existente | Crea una entrada nueva |

> **Para el examen:** Una re-ejecución siempre usa el mismo SHA y el mismo `run_id` que la ejecución original. El contador `run_number` no cambia.

## A4.6 Re-ejecución via REST API

La REST API de GitHub permite automatizar las re-ejecuciones desde scripts, sistemas CI/CD externos o herramientas de monitoreo. Existen tres endpoints principales, cada uno correspondiente a una de las modalidades vistas anteriormente.

El endpoint para re-ejecutar todos los jobs es `POST /repos/{owner}/{repo}/actions/runs/{run_id}/rerun`. Para re-ejecutar solo los jobs fallidos se usa `POST /repos/{owner}/{repo}/actions/runs/{run_id}/rerun-failed-jobs`. Para re-ejecutar un job específico se usa `POST /repos/{owner}/{repo}/actions/jobs/{job_id}/rerun`.

Los tres endpoints aceptan un cuerpo JSON opcional con el parámetro `enable_debug_logging` (boolean) para activar el modo debug en la re-ejecución. La autenticación requiere un token con el scope `repo` o, en repositorios de organización, el permiso `actions: write`.

El siguiente ejemplo muestra una llamada completa para re-ejecutar solo los jobs fallidos de una ejecución, con debug logging activado:

```bash
curl -L \
  -X POST \
  -H "Accept: application/vnd.github+json" \
  -H "Authorization: Bearer ghp_TuTokenAqui" \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  https://api.github.com/repos/mi-org/mi-repo/actions/runs/9876543210/rerun-failed-jobs \
  -d '{"enable_debug_logging": true}'
```

La respuesta esperada cuando la solicitud es aceptada es un código HTTP `201 Created` con cuerpo vacío. Si el `run_id` no existe o el token no tiene permisos, recibirás `404 Not Found` o `403 Forbidden` respectivamente.

> **Importante:** El `run_id` es el identificador numérico de la ejecución, visible en la URL de la interfaz web: `https://github.com/{owner}/{repo}/actions/runs/{run_id}`. No confundas `run_id` con `run_number`.

## A4.7 Limitaciones de re-run

Las re-ejecuciones no están disponibles indefinidamente. GitHub impone un límite de tiempo: solo se puede re-ejecutar una ejecución que haya terminado hace menos de 30 días. Pasado ese plazo, los botones de re-run en la UI quedan deshabilitados y los endpoints de la API devuelven un error.

Adicionalmente, el repositorio debe existir y estar accesible. Si el repositorio fue archivado, las re-ejecuciones no están disponibles. En repositorios privados, el usuario que solicita la re-ejecución debe tener permisos de escritura sobre el repositorio.

Otra limitación importante es que las re-ejecuciones no reevalúan condiciones de concurrencia ni cancelan ejecuciones en curso del mismo grupo de concurrencia. Si el workflow tiene una configuración `concurrency` con `cancel-in-progress: true`, una re-ejecución puede ser cancelada si hay otra ejecución activa del mismo grupo en ese momento.

> **Para el examen:** El límite para re-ejecutar una ejecución es de **30 días** desde que terminó la ejecución original.

## Buenas y malas prácticas

Las siguientes recomendaciones están basadas en los patrones más comunes en entornos reales y en las preguntas que aparecen en el examen de certificación.

**Buena práctica 1:** Usa "Re-run failed jobs" en lugar de "Re-run all jobs" cuando sea posible. Ahorra tiempo, minutos de Actions y reduce el riesgo de efectos secundarios en jobs que ya habían completado con éxito, como deploys o notificaciones.

**Mala práctica 1:** Re-ejecutar un workflow como primera respuesta automática a cualquier fallo sin analizar la causa. Algunos fallos son deterministas y repetirlos solo desperdicia recursos. Investiga el log antes de re-ejecutar.

**Buena práctica 2:** Activa "Enable debug logging" en la primera re-ejecución de diagnóstico cuando el log normal no dé suficiente contexto. Esto evita tener que hacer una tercera ejecución solo para obtener más logs.

**Mala práctica 2:** Dejar el modo debug activado de forma permanente mediante los secretos `RUNNER_DEBUG` y `ACTIONS_STEP_DEBUG` en el repositorio. Los logs en modo debug son voluminosos y pueden exponer información sensible del entorno. Úsalo solo cuando sea necesario.

**Buena práctica 3:** Automatizar las re-ejecuciones de jobs fallidos en pipelines críticos usando la REST API con un sistema de monitoreo externo. Establecer un número máximo de reintentos (por ejemplo, 2 re-ejecuciones automáticas) para evitar bucles infinitos ante fallos deterministas.

**Mala práctica 3:** Asumir que una re-ejecución usará el código más reciente del branch. La re-ejecución siempre usa el SHA original. Si necesitas probar con código nuevo, debes crear un nuevo trigger (push, PR update, `workflow_dispatch`, etc.).

## Verificación y práctica

Las siguientes preguntas están diseñadas para reforzar los conceptos clave y simular el formato del examen GH-200.

**Pregunta 1.** Un workflow con 5 jobs falla en el job número 3. Los jobs 4 y 5 no llegaron a ejecutarse porque dependen del job 3. Usas "Re-run failed jobs". ¿Qué jobs se re-ejecutan?

> **Respuesta:** Se re-ejecutan el job 3 (que falló) y los jobs 4 y 5 (que dependen de él y no habían podido ejecutarse). Los jobs 1 y 2, que completaron con éxito, no se re-ejecutan.

**Pregunta 2.** Una ejecución de workflow terminó hace 35 días. Un desarrollador intenta hacer clic en "Re-run all jobs" desde la interfaz web pero el botón está deshabilitado. ¿Cuál es la razón?

> **Respuesta:** GitHub solo permite re-ejecutar ejecuciones que terminaron hace menos de 30 días. La ejecución tiene 35 días, por lo que ha superado el límite y ya no es posible re-ejecutarla.

**Pregunta 3.** Un workflow se dispara con un push al SHA `abc123`. Después de que falla, alguien hace un nuevo push con el SHA `def456`. Un administrador re-ejecuta el workflow fallido. ¿Qué SHA usa la re-ejecución?

> **Respuesta:** La re-ejecución usa el SHA original `abc123`. Las re-ejecuciones siempre conservan el SHA del commit que disparó la ejecución original, independientemente de los commits que se hayan añadido después.

**Ejercicio práctico.** Tienes acceso a un repositorio con un workflow que falla intermitentemente en su job de integración. Realiza los siguientes pasos:

1. Navega a la pestaña **Actions** de tu repositorio y encuentra la última ejecución fallida.
2. Haz clic en **Re-run failed jobs** y activa la opción **Enable debug logging** en el diálogo de confirmación.
3. Observa los logs generados con debug activado y compáralos con los logs normales de una ejecución anterior.
4. Una vez identificado el problema, usa la siguiente llamada API para automatizar una futura re-ejecución desde un script:

```bash
curl -L \
  -X POST \
  -H "Accept: application/vnd.github+json" \
  -H "Authorization: Bearer $GITHUB_TOKEN" \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  https://api.github.com/repos/$OWNER/$REPO/actions/runs/$RUN_ID/rerun-failed-jobs \
  -d '{"enable_debug_logging": false}'
```

5. Verifica que la re-ejecución ha iniciado comprobando que el estado de la ejecución cambia a `in_progress` en la interfaz web o mediante:

```bash
curl -L \
  -H "Accept: application/vnd.github+json" \
  -H "Authorization: Bearer $GITHUB_TOKEN" \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  https://api.github.com/repos/$OWNER/$REPO/actions/runs/$RUN_ID
```

---

<!-- anterior: [2.3.2 Depuración avanzada](gha-d2-debug-avanzado.md) | siguiente: [2.5 Artefactos: consumo, gestión y API](gha-d2-artefactos.md) -->
