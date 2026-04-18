> **Navegación:** ← [2.3.1 Diagnóstico de comportamiento](gha-d2-diagnostico-comportamiento.md) | [2.4 Re-ejecución de workflows y jobs](gha-d2-reejecutar.md) →

> **Prerequisito:** Lee [gha-d2-diagnostico-comportamiento.md](gha-d2-diagnostico-comportamiento.md) primero. Este fichero asume que conoces el vocabulario de fallo (exit codes, timeouts, mensajes de error) establecido allí.

# 2.3b Modos de depuración avanzada

Cuando los logs estándar de GitHub Actions no revelan la causa de un fallo, necesitas activar modos de depuración que exponen información interna del runner y de cada step. Este fichero explica cómo activar esos modos, qué información adicional generan, y cómo aplicar ese diagnóstico a problemas específicos de permisos, contextos, matrix y concurrencia.

## Por qué existen los modos de depuración

GitHub Actions ejecuta cada job en un runner efímero. Por defecto, los logs muestran la salida estándar de tus comandos, pero ocultan la comunicación interna entre el runner agent y los servicios de GitHub, así como la resolución de expresiones y contextos. Esto es intencional: en ejecuciones normales ese nivel de detalle añade ruido sin valor. Sin embargo, cuando un workflow falla por razones no evidentes —un token sin permisos, una expresión que evalúa a un valor inesperado, una combinación de matrix que explota silenciosamente— necesitas ver qué sucede por debajo.

GitHub proporciona dos variables de entorno especiales que, cuando están activas, aumentan la verbosidad de los logs a niveles que normalmente no son visibles. Estas variables se configuran como **secrets del repositorio**, no como variables de entorno normales del workflow, porque así están disponibles para el runner desde el inicio de la ejecución, antes de que se cargue ningún step.

## ACTIONS_RUNNER_DEBUG: depuración del runner

La variable `ACTIONS_RUNNER_DEBUG=true` activa el modo de depuración del runner agent. Cuando está habilitada, el runner emite logs detallados sobre su propio funcionamiento: la descarga y configuración de actions, la comunicación con la API de GitHub para obtener el job, el estado de los workers internos, y los eventos del ciclo de vida del job.

Este modo es útil cuando el problema no está en tu código sino en la infraestructura de ejecución. Por ejemplo, si un job falla al intentar descargar una action de un repositorio privado, o si hay un problema de red entre el runner y los servicios de GitHub, `ACTIONS_RUNNER_DEBUG` lo registra. También muestra información sobre el entorno del runner: sistema operativo, versiones de herramientas instaladas, variables de entorno del sistema.

Los logs adicionales aparecen en una sección separada llamada **"Runner Diagnostic Logs"** que se descarga como artefacto junto con los logs del workflow. No se muestran directamente en la interfaz de GitHub Actions sino que se incluyen en el archivo ZIP de diagnóstico que puedes descargar desde la página de ejecución.

## ACTIONS_STEP_DEBUG: depuración de steps

La variable `ACTIONS_STEP_DEBUG=true` activa un nivel diferente de verbosidad. En lugar de enfocarse en el runner, esta variable hace que cada step emita información de depuración adicional sobre su propia ejecución: la resolución de inputs, los valores de outputs generados, y los comandos internos que el step ejecuta.

Con `ACTIONS_STEP_DEBUG` activo, la interfaz de GitHub Actions muestra logs adicionales directamente en el panel de cada step, marcados con el prefijo `::debug::`. Muchas actions oficiales de GitHub usan este mecanismo para emitir información diagnóstica que normalmente permanece silenciosa. Por ejemplo, `actions/checkout` con step debug habilitado muestra los detalles de autenticación, la URL remota que usa, y cada operación git que realiza.

Este modo es el más útil para depurar problemas dentro de la lógica de tu workflow: valores de contexto, evaluación de expresiones, y el comportamiento interno de actions de terceros.

## Tabla comparativa

La diferencia entre ambas variables es importante para el examen de certificación:

| Aspecto | `ACTIONS_RUNNER_DEBUG` | `ACTIONS_STEP_DEBUG` |
|---|---|---|
| Qué depura | El runner agent en sí | Cada step individual |
| Dónde aparecen los logs | Artefacto ZIP descargable | Panel de steps en la UI |
| Prefijo en logs | Sin prefijo específico | `::debug::` |
| Útil para | Problemas de red, descarga de actions, infraestructura | Valores de contexto, outputs, lógica interna de actions |
| Activa `::debug::` en steps | No | Sí |
| Recomendado cuando | El job ni siquiera arranca correctamente | Un step falla con comportamiento inesperado |

## Cómo configurar las variables de depuración

Ambas variables se configuran como secrets en el repositorio, no como variables de entorno en el YAML del workflow. La razón es técnica: los secrets se inyectan en el entorno del runner antes de que comience la ejecución, mientras que las variables definidas en `env:` del workflow solo están disponibles durante la ejecución de steps.

Para activarlas: ve a tu repositorio → Settings → Secrets and variables → Actions → New repository secret. Crea un secret con nombre `ACTIONS_RUNNER_DEBUG` y valor `true`. Haz lo mismo para `ACTIONS_STEP_DEBUG` si lo necesitas.

La tabla siguiente resume las opciones de configuración:

| Variable | Nombre del secret | Valor | Efecto |
|---|---|---|---|
| Runner debug | `ACTIONS_RUNNER_DEBUG` | `true` | Logs detallados del runner agent |
| Step debug | `ACTIONS_STEP_DEBUG` | `true` | Logs `::debug::` visibles en UI |
| Desactivar | Eliminar el secret | — | Vuelve al comportamiento normal |

> **Nota de seguridad:** Los secrets de depuración pueden exponer información sensible en los logs, como valores de variables de entorno o detalles de autenticación. Actívalos solo cuando los necesites y elimínalos cuando termines la investigación. Nunca los dejes activos en repositorios públicos.

## Diagnóstico de permisos insuficientes

Los errores de permisos son uno de los fallos más frecuentes en workflows de producción y también uno de los más confusos porque el mensaje de error no siempre indica claramente qué permiso falta.

El `GITHUB_TOKEN` es el token de autenticación que GitHub inyecta automáticamente en cada ejecución de workflow. Sus permisos por defecto dependen de la configuración del repositorio y de la organización. Cuando el token no tiene los permisos necesarios para una operación, la API de GitHub responde con un código HTTP 403 (Forbidden) o 404 (Not Found, para recursos que el token no puede "ver").

Los mensajes más comunes que indican problemas de permisos son:

- `Resource not accessible by integration` — el token no tiene el scope necesario para esa operación
- `403: Forbidden` — acceso denegado explícitamente
- `404: Not Found` — el recurso existe pero el token no tiene permiso para verlo (GitHub oculta recursos en lugar de exponer 403 en algunos casos)
- `Permission denied to github-actions[bot]` — el token de GitHub Actions fue rechazado

Para diagnosticar permisos insuficientes, el primer paso es verificar los permisos declarados en el workflow. Si el workflow no declara una sección `permissions:`, hereda los permisos por defecto del repositorio u organización. Si la organización tiene configurado el modo de permisos restrictivo (`read` por defecto), muchas operaciones fallarán silenciosamente.

La sección `permissions:` puede declararse a nivel de workflow (aplica a todos los jobs) o a nivel de job individual (más preciso y recomendado). Los permisos disponibles incluyen: `contents`, `issues`, `pull-requests`, `packages`, `id-token`, `actions`, entre otros. Cada uno acepta los valores `read`, `write`, o `none`.

Un caso especial es `pull_request_target`: este evento ejecuta el workflow con los permisos del repositorio base, no del fork, lo que significa que tiene acceso de escritura incluso cuando el PR viene de un fork externo. Esto es un riesgo de seguridad documentado si el workflow usa código del fork directamente.

## Ejemplo central: activación de debug y diagnóstico de permisos

El siguiente workflow demuestra cómo activar los modos de depuración, inspeccionar el contexto de permisos, y diagnosticar un error 403 al intentar crear un release sin los permisos adecuados.

```yaml
name: Debug avanzado - diagnóstico de permisos

on:
  push:
    branches:
      - main

# Permisos mínimos para demostrar el diagnóstico
permissions:
  contents: read

jobs:
  diagnostico-permisos:
    runs-on: ubuntu-latest
    steps:
      # Step 1: Mostrar información del contexto de github
      - name: Inspeccionar contexto github
        env:
          GITHUB_CONTEXT: ${{ toJSON(github) }}
        run: |
          echo "=== Contexto github ==="
          echo "$GITHUB_CONTEXT"
          echo ""
          echo "=== Token actor ==="
          echo "Actor: ${{ github.actor }}"
          echo "Repository: ${{ github.repository }}"
          echo "Ref: ${{ github.ref }}"
          echo "Event: ${{ github.event_name }}"

      # Step 2: Inspeccionar permisos del token actual
      - name: Verificar permisos del token
        run: |
          echo "=== Verificando permisos del GITHUB_TOKEN ==="
          # Llamada a la API para ver los permisos del token
          curl -s -o /dev/null -w "HTTP Status: %{http_code}\n" \
            -H "Authorization: Bearer ${{ secrets.GITHUB_TOKEN }}" \
            -H "Accept: application/vnd.github+json" \
            "https://api.github.com/repos/${{ github.repository }}"

          echo ""
          echo "=== Intentando operación que requiere 'contents: write' ==="
          HTTP_STATUS=$(curl -s -o /tmp/api_response.json -w "%{http_code}" \
            -X POST \
            -H "Authorization: Bearer ${{ secrets.GITHUB_TOKEN }}" \
            -H "Accept: application/vnd.github+json" \
            -H "Content-Type: application/json" \
            -d '{"tag_name":"v0.0.0-test","name":"Test Release","body":"Release de prueba","draft":true}' \
            "https://api.github.com/repos/${{ github.repository }}/releases")

          echo "HTTP Status de crear release: $HTTP_STATUS"
          echo "Respuesta de la API:"
          cat /tmp/api_response.json

          if [ "$HTTP_STATUS" = "403" ]; then
            echo ""
            echo "DIAGNÓSTICO: Error 403 - el token no tiene permiso 'contents: write'"
            echo "Solución: añadir 'contents: write' en la sección permissions del job"
          elif [ "$HTTP_STATUS" = "201" ]; then
            echo ""
            echo "Release creado correctamente (esto no debería ocurrir con contents: read)"
          fi

      # Step 3: Emitir mensajes de debug para demostrar ACTIONS_STEP_DEBUG
      - name: Demostración de debug::
        run: |
          # Con ACTIONS_STEP_DEBUG=true en secrets, estos mensajes son visibles
          echo "::debug::Valor de github.ref = ${{ github.ref }}"
          echo "::debug::Valor de github.sha = ${{ github.sha }}"
          echo "::debug::Runner OS = ${{ runner.os }}"
          echo "::notice::Este mensaje siempre es visible"
          echo "::warning::Este warning siempre es visible"

  diagnostico-permisos-correcto:
    runs-on: ubuntu-latest
    # Permisos correctos para crear releases
    permissions:
      contents: write
    steps:
      - name: Crear release con permisos correctos
        run: |
          echo "=== Creando release con permisos correctos ==="
          HTTP_STATUS=$(curl -s -o /tmp/api_response.json -w "%{http_code}" \
            -X POST \
            -H "Authorization: Bearer ${{ secrets.GITHUB_TOKEN }}" \
            -H "Accept: application/vnd.github+json" \
            -H "Content-Type: application/json" \
            -d '{"tag_name":"v0.0.0-debug-test","name":"Debug Test Release","body":"Release de prueba para diagnóstico","draft":true}' \
            "https://api.github.com/repos/${{ github.repository }}/releases")

          echo "HTTP Status: $HTTP_STATUS"
          if [ "$HTTP_STATUS" = "201" ]; then
            echo "Release creado correctamente"
            RELEASE_ID=$(cat /tmp/api_response.json | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")
            echo "Release ID: $RELEASE_ID"

            # Limpiar: eliminar el release de prueba
            curl -s -X DELETE \
              -H "Authorization: Bearer ${{ secrets.GITHUB_TOKEN }}" \
              "https://api.github.com/repos/${{ github.repository }}/releases/$RELEASE_ID"
            echo "Release de prueba eliminado"
          fi
```

Este workflow tiene dos jobs: el primero intenta crear un release con permisos insuficientes (`contents: read`) y muestra el diagnóstico del error 403. El segundo job tiene los permisos correctos (`contents: write`) y completa la operación. La comparación entre ambos jobs hace que el diagnóstico sea visible en los logs.

## Diagnóstico de contextos: valores nulos o inesperados

Los contextos en GitHub Actions son objetos que contienen información sobre el workflow, el runner, el evento desencadenante, y más. Se accede a ellos mediante la sintaxis `${{ context.property }}`. Cuando una expresión evalúa a un valor inesperado —vacío, `null`, o un tipo incorrecto— el comportamiento del workflow puede ser sorprendente.

El problema más frecuente con contextos es intentar acceder a una propiedad que no existe en el evento que disparó el workflow. Por ejemplo, `github.event.pull_request.number` solo tiene valor cuando el workflow se dispara con el evento `pull_request`. Si el mismo workflow se ejecuta con `push`, esa expresión evalúa a una cadena vacía, no a un error. Esto puede causar que steps posteriores fallen con mensajes confusos porque reciben un valor vacío donde esperaban un número.

Para diagnosticar valores de contexto, la técnica más directa es volcar el contexto completo con `toJSON()`. Esta función convierte cualquier objeto de contexto a una cadena JSON legible. Puedes hacer esto en un step dedicado de diagnóstico al inicio del workflow:

```yaml
- name: Volcar contextos para diagnóstico
  env:
    GITHUB_CONTEXT: ${{ toJSON(github) }}
    ENV_CONTEXT: ${{ toJSON(env) }}
    VARS_CONTEXT: ${{ toJSON(vars) }}
    JOB_CONTEXT: ${{ toJSON(job) }}
  run: |
    echo "=== github context ==="
    echo "$GITHUB_CONTEXT"
    echo "=== env context ==="
    echo "$ENV_CONTEXT"
    echo "=== vars context ==="
    echo "$VARS_CONTEXT"
    echo "=== job context ==="
    echo "$JOB_CONTEXT"
```

> **Advertencia de seguridad:** No vuelques `secrets` con `toJSON(secrets)` en los logs. GitHub enmascara los valores de secrets conocidos, pero un secret cargado en una variable de entorno y luego transformado puede escapar al enmascaramiento. Usa `toJSON()` solo con contextos que no contengan datos sensibles.

Otro problema frecuente es el uso de expresiones en condiciones `if:` que evalúan a un valor truthy/falsy inesperado. En GitHub Actions, la expresión `${{ github.event.inputs.deploy }}` en una condición `if:` no funciona como se espera cuando el valor del input es la cadena `"false"` — porque la cadena no vacía `"false"` es truthy en el contexto de evaluación de expresiones. La forma correcta es `${{ github.event.inputs.deploy == 'true' }}`.

## Diagnóstico de matrix: identificar qué combinación falló

Las estrategias de matrix ejecutan el mismo job con múltiples combinaciones de variables. Cuando una combinación falla, el log de GitHub Actions identifica el job con la combinación específica en el título (por ejemplo, `build (ubuntu-latest, node-18)`), lo que facilita la identificación. Sin embargo, hay situaciones donde el diagnóstico es más complejo.

El primer caso problemático es cuando múltiples combinaciones fallan por razones diferentes. Por defecto, `fail-fast: true` en una estrategia de matrix cancela todas las combinaciones pendientes en cuanto una falla. Esto significa que solo ves el error de la primera combinación fallida, no de todas. Para diagnosticar todas las combinaciones, debes establecer `fail-fast: false`, que permite que todas las combinaciones se ejecuten hasta completarse independientemente de los fallos.

El segundo caso es cuando quieres excluir combinaciones problemáticas o añadir combinaciones especiales. La clave `exclude:` permite filtrar combinaciones específicas de la matrix, mientras que `include:` permite añadir combinaciones adicionales o sobrescribir propiedades de combinaciones existentes. Un error frecuente es confundir `include` con "añadir a la matrix completa" — en realidad, si `include` contiene propiedades que no existen en la matrix base, crea una nueva combinación independiente.

Para identificar qué combinación estás ejecutando dentro de un step, puedes acceder al contexto `matrix`:

```yaml
- name: Identificar combinación de matrix
  run: |
    echo "=== Combinación actual de matrix ==="
    echo "OS: ${{ matrix.os }}"
    echo "Node version: ${{ matrix.node-version }}"
    echo "Full matrix context: ${{ toJSON(matrix) }}"
```

El contexto `matrix` solo existe dentro de jobs que usan una estrategia `matrix:`. En jobs sin matrix, acceder a `matrix` produce un valor vacío sin generar error.

## Diagnóstico de concurrencia: workflows cancelados o en espera

La configuración `concurrency:` en GitHub Actions controla cuántas ejecuciones simultáneas puede tener un workflow o job para un grupo determinado. Cuando hay más ejecuciones que el límite permitido, las ejecuciones adicionales se ponen en cola o se cancelan según la configuración de `cancel-in-progress`.

Cuando `cancel-in-progress: true` está configurado, una nueva ejecución del mismo grupo de concurrencia cancela cualquier ejecución pendiente o en progreso del mismo grupo. Esto es útil en ramas de desarrollo donde solo te importa el resultado de la última ejecución, pero puede ser confuso cuando un deployment es cancelado sin que nadie lo haya cancelado manualmente.

El síntoma visible es que un workflow aparece con estado "Cancelled" sin ninguna acción manual. Para diagnosticarlo, busca en la lista de ejecuciones si hay una ejecución más reciente del mismo workflow en la misma rama: esa ejecución más reciente es la que causó la cancelación de la anterior.

La configuración `concurrency:` puede definirse con expresiones para crear grupos dinámicos:

```yaml
concurrency:
  group: ${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: true
```

En este ejemplo, el grupo es único por combinación de nombre de workflow y rama. Dos pushes seguidos a la misma rama comparten el mismo grupo, y el segundo push cancela el workflow del primero. Dos pushes a ramas diferentes no se afectan entre sí.

Cuando `cancel-in-progress: false` (el valor por defecto), las ejecuciones nuevas del mismo grupo esperan a que termine la ejecución en curso. Esto puede generar una cola de ejecuciones. Para ver el estado de la cola, la interfaz de GitHub Actions muestra el estado "Waiting" con un mensaje indicando que el workflow está esperando a que un concurrent run termine.

Un caso problemático frecuente es usar `cancel-in-progress: true` en workflows de deployment. Si un deployment está en curso y se hace un nuevo push, el deployment en curso se cancela a mitad, dejando potencialmente el entorno en un estado inconsistente. Para deployments, la práctica recomendada es usar `cancel-in-progress: false` y gestionar la cola manualmente, o usar un grupo de concurrencia específico por entorno:

```yaml
concurrency:
  group: deploy-${{ github.ref }}-${{ vars.ENVIRONMENT }}
  cancel-in-progress: false
```

## Buenas y malas prácticas

La aplicación correcta de las técnicas de depuración avanzada marca la diferencia entre resolver problemas rápidamente y pasar horas sin avanzar.

**Buena práctica 1 — Activar solo el modo de debug necesario.** Si el problema parece estar en la lógica de un step, activa `ACTIONS_STEP_DEBUG`. Si el problema parece ser infraestructural (el job no arranca, hay problemas de red), activa `ACTIONS_RUNNER_DEBUG`. Activar ambos simultáneamente genera una cantidad de logs que puede ser abrumadora.

**Mala práctica 1 — Dejar los secrets de debug activos permanentemente.** Los modos de depuración exponen información sensible: valores de variables de entorno, detalles de autenticación, configuraciones internas. En repositorios públicos esto es un riesgo de seguridad serio. En repositorios privados genera ruido innecesario en todos los logs. Elimina los secrets de debug cuando termines la investigación.

**Buena práctica 2 — Declarar permisos explícitamente en cada job.** En lugar de depender de los permisos por defecto del repositorio u organización (que pueden cambiar), declara exactamente qué permisos necesita cada job con la sección `permissions:`. Usa el principio de mínimo privilegio: solo los permisos que el job realmente necesita. Esto además documenta las intenciones del workflow.

**Mala práctica 2 — Usar `permissions: write-all` para resolver errores 403.** Cuando aparece un 403, la solución fácil parece ser dar todos los permisos posibles. Pero esto es una mala práctica de seguridad. En su lugar, lee el mensaje de error con `ACTIONS_STEP_DEBUG` activo para identificar exactamente qué permiso falta y añade solo ese.

**Buena práctica 3 — Establecer `fail-fast: false` durante el diagnóstico de matrix.** Cuando estás investigando por qué fallan ciertas combinaciones de matrix, establece temporalmente `fail-fast: false` para ver el comportamiento completo de todas las combinaciones. Una vez identificado y resuelto el problema, puedes volver a `fail-fast: true` si es apropiado para tu caso.

**Mala práctica 3 — Usar `cancel-in-progress: true` en workflows de deployment sin considerarlo bien.** La cancelación en progreso es útil para workflows de CI donde solo importa el resultado del último commit, pero en deployments puede dejar el entorno en un estado inconsistente. Evalúa cuidadosamente el impacto antes de usar esta configuración en workflows que modifiquen estado externo.

**Buena práctica 4 — Volcar el contexto completo al inicio del workflow durante depuración.** Cuando no entiendes por qué una expresión evalúa a un valor inesperado, añade un step de diagnóstico al inicio que vuelque los contextos relevantes con `toJSON()`. Esto te muestra exactamente qué datos tiene disponibles el workflow antes de que comience a fallar.

**Mala práctica 4 — Confiar en `null` como valor seguro en expresiones condicionales.** Una expresión como `${{ github.event.pull_request.number }}` en un contexto que no es `pull_request` no genera error: devuelve una cadena vacía. Si usas ese valor en una condición `if:` sin verificar que existe, el comportamiento puede ser inesperado. Usa siempre `github.event_name == 'pull_request'` como guardia antes de acceder a propiedades del evento de pull_request.

## Verificación y práctica

Antes de continuar, verifica que has comprendido los conceptos clave de este fichero.

**Pregunta 1 — Estilo certificación GH-200:**

Tienes un workflow que falla con el mensaje `Resource not accessible by integration` al intentar comentar en un pull request. El workflow tiene la siguiente configuración de permisos:

```yaml
permissions:
  contents: read
```

¿Cuál es la corrección más apropiada siguiendo el principio de mínimo privilegio?

A) Cambiar a `permissions: write-all`
B) Añadir `pull-requests: write` a la sección `permissions:`
C) Usar un Personal Access Token en lugar de `GITHUB_TOKEN`
D) Activar `ACTIONS_RUNNER_DEBUG=true` para obtener más información

**Respuesta correcta: B.** El permiso específico para comentar en pull requests es `pull-requests: write`. La opción A es excesiva e insegura. La opción C es innecesaria cuando el problema es solo de permisos del token. La opción D no soluciona el problema, solo proporciona más información.

**Pregunta 2 — Estilo certificación GH-200:**

Un workflow de deployment está configurado con:

```yaml
concurrency:
  group: deploy-production
  cancel-in-progress: true
```

Un desarrollador hace dos pushes seguidos a `main`. El primer deployment comienza a ejecutarse. ¿Qué ocurre cuando el segundo push dispara el workflow?

A) El segundo workflow espera a que termine el primero
B) El segundo workflow falla con un error de concurrencia
C) El primer workflow es cancelado y el segundo comienza
D) Ambos workflows se ejecutan en paralelo

**Respuesta correcta: C.** Con `cancel-in-progress: true`, una nueva ejecución del mismo grupo cancela la ejecución en progreso. La respuesta A sería correcta con `cancel-in-progress: false`. Las respuestas B y D no corresponden al comportamiento de `concurrency:`.

**Pregunta 3 — Estilo certificación GH-200:**

¿Dónde aparecen los logs adicionales generados por `ACTIONS_RUNNER_DEBUG=true`?

A) Directamente en el panel de cada step en la interfaz de GitHub Actions
B) Como mensajes `::debug::` en los logs del step
C) En un artefacto ZIP descargable en la página de ejecución del workflow
D) En la consola del runner, inaccesible desde GitHub

**Respuesta correcta: C.** Los logs del runner debug se incluyen en el artefacto ZIP de diagnóstico. Los mensajes `::debug::` visibles en la UI son generados por `ACTIONS_STEP_DEBUG`, no por `ACTIONS_RUNNER_DEBUG`.

**Ejercicio práctico:**

Crea un workflow en tu repositorio que haga lo siguiente:

1. Se dispara con `workflow_dispatch` y tiene un input llamado `environment` con opciones `staging` y `production`.
2. Tiene una estrategia de matrix con tres sistemas operativos: `ubuntu-latest`, `windows-latest`, y `macos-latest`.
3. Configura `fail-fast: false` en la matrix.
4. En el primer step, vuelca el contexto `github` completo usando `toJSON()`.
5. En el segundo step, usa la condición `if: matrix.os == 'ubuntu-latest'` para ejecutar un bloque solo en Ubuntu.
6. En el tercer step, imprime el valor del input `environment` y verifica que no es vacío con `if: github.event.inputs.environment != ''`.
7. Configura `concurrency:` con un grupo dinámico basado en el nombre del workflow y el ref, con `cancel-in-progress: false`.

Una vez creado, activa `ACTIONS_STEP_DEBUG=true` como secret y observa la diferencia en los logs del primer step con `toJSON()`.

> **Navegación:** ← [2.3.1 Diagnóstico de comportamiento](gha-d2-diagnostico-comportamiento.md) | [2.4 Re-ejecución de workflows y jobs](gha-d2-reejecutar.md) →

