# 2.1 Interpretación de triggers y UI de ejecución

← [1.20 Testing / Verificación de D1](gha-d1-testing.md) | [Índice](README.md) | [2.2 Lectura e interpretación de logs](gha-d2-logs.md) →

---

## Introducción

Uno de los problemas más frecuentes al trabajar con GitHub Actions no es que un workflow falle, sino que directamente no se dispara. El desarrollador abre la pestaña Actions, no ve ninguna ejecución nueva y no sabe por qué. Este fichero responde a esa pregunta desde dos ángulos: primero, cómo funcionan los filtros de trigger que determinan si un evento activa o no un workflow; segundo, cómo leer la interfaz de GitHub para entender el estado de una ejecución en curso o pasada.

Los triggers en GitHub Actions no son simples interruptores de encendido/apagado. Cada evento admite filtros que limitan el conjunto de situaciones en las que ese evento produce una ejecución. Un `push` a una rama puede disparar el workflow o no, dependiendo de qué ramas y qué rutas estén configuradas. Entender cuándo se activa y cuándo no es esencial para la certificación GH-200 y para el trabajo diario.

La UI de Actions, por su parte, ofrece información visual densa: un grafo de dependencias entre jobs, indicadores de estado, tiempos de cola, y una diferencia entre la pestaña Summary y la pestaña de job individual que no es evidente a primera vista. Saber leer esa interfaz acelera el diagnóstico.

> [PREREQUISITO] Este fichero asume que conoces la sintaxis básica de `on:` y los tipos de eventos más comunes. Si no es así, lee primero [1.x Triggers: sintaxis y eventos](gha-d1-triggers-codigo.md).

---

## Filtros de trigger: branches, paths, tags y sus variantes negativas

Los filtros de trigger son condiciones adicionales que GitHub evalúa después de recibir un evento. Si el evento ocurre pero los filtros no coinciden, el workflow no se ejecuta y no aparece ningún registro en la pestaña Actions.

Los filtros disponibles para eventos como `push` y `pull_request` son cuatro pares simétricos:

| Filtro | Efecto | Patrón de uso |
|---|---|---|
| `branches` | Solo se activa si la rama coincide | Lista de ramas o patrones glob |
| `branches-ignore` | Se activa en todas las ramas excepto las que coinciden | Lista de ramas a excluir |
| `paths` | Solo se activa si algún archivo modificado coincide con la ruta | Lista de rutas o patrones glob |
| `paths-ignore` | Se activa salvo si TODOS los archivos modificados coinciden con la exclusión | Lista de rutas a ignorar |
| `tags` | Solo se activa si el push crea o modifica una etiqueta que coincide | Lista de patrones de tag |
| `tags-ignore` | Se activa para todos los tags excepto los que coinciden | Lista de tags a excluir |

Una restricción importante: `branches` y `branches-ignore` son mutuamente excluyentes en el mismo evento. Lo mismo aplica a `paths` y `paths-ignore`, y a `tags` y `tags-ignore`. Intentar usar ambos lados de un par en el mismo bloque de evento produce un error de validación.

Los patrones glob siguen la sintaxis de fnmatch: `*` coincide con cualquier secuencia de caracteres excepto `/`, `**` coincide incluyendo `/`, y `?` coincide con un único carácter. El patrón `feature/*` coincide con `feature/login` pero no con `feature/auth/oauth`. Para el segundo caso se necesita `feature/**`.

---

## Cuándo un workflow NO se activa

Identificar por qué un workflow no se disparó es una habilidad que el examen GH-200 evalúa explícitamente. Hay varios escenarios distintos, y cada uno tiene una causa diferente.

El escenario más común es el de filtros que no coinciden. Si un workflow tiene `branches: [main]` y se hace push a `develop`, el workflow no se ejecuta. No hay error visible, no hay entrada en la pestaña Actions para ese push concreto. El workflow simplemente no fue invocado.

Un segundo escenario es el de paths que no coinciden. Si el workflow tiene `paths: ['src/**']` y el commit solo modifica `docs/README.md`, el workflow no se ejecuta aunque la rama sea correcta. Este es quizás el caso más confuso porque el desarrollador ve que está en la rama correcta y no entiende por qué no hay ejecución.

Un tercer escenario es el de `paths-ignore` con comportamiento asimétrico. Con `paths-ignore`, el workflow se salta únicamente si TODOS los archivos modificados están cubiertos por los patrones de exclusión. Si un commit toca `docs/guide.md` y `src/app.js`, y el filtro es `paths-ignore: ['docs/**']`, el workflow SÍ se ejecuta porque `src/app.js` no está excluido. Este comportamiento asimétrico respecto a `paths` sorprende a muchos desarrolladores.

Un cuarto escenario es el del archivo de workflow en una rama distinta a la rama por defecto. Para eventos `push` y `pull_request`, GitHub lee el archivo de workflow desde la rama que recibe el push o desde la rama head del pull request. Si el archivo no existe en esa rama, el workflow no existe para ese evento.

> [EXAMEN] Una pregunta frecuente en GH-200 es: "Se hizo push a la rama `feature/login` modificando solo `docs/changelog.md`. El workflow tiene `branches: [feature/**]` y `paths: ['src/**']`. ¿Se ejecuta?" La respuesta es no: ambos filtros deben coincidir simultáneamente, y `paths` no coincide.

---

## El evento workflow_run como trigger encadenado

El evento `workflow_run` permite que un workflow se active cuando otro workflow termina. Esto habilita patrones de encadenamiento donde, por ejemplo, un workflow de despliegue solo se inicia cuando el workflow de tests completa con éxito.

La sintaxis básica es:

```yaml
on:
  workflow_run:
    workflows: ["CI Tests"]
    types: [completed]
    branches: [main]
```

El campo `workflows` es una lista de nombres de workflow (el valor del campo `name:` del workflow padre, no el nombre del archivo). El campo `types` acepta `requested` y `completed`. El campo `branches` filtra por la rama en la que se ejecutó el workflow padre.

Este evento tiene varias limitaciones importantes que distinguen a un desarrollador experimentado de uno que lo usa por primera vez. Primera limitación: `workflow_run` solo se activa si el archivo del workflow que lo define está en la rama por defecto del repositorio. Si el workflow consumidor está solo en una rama de feature, no se ejecutará. Segunda limitación: el workflow padre debe existir en el mismo repositorio. No funciona entre repositorios distintos. Tercera limitación: si el workflow padre fue disparado por un fork, el workflow hijo se ejecuta en el contexto del repositorio base con acceso completo a los secretos, lo que tiene implicaciones de seguridad que se tratan en el módulo D5.

Una ventaja de `workflow_run` sobre simplemente poner jobs en el mismo workflow es que permite ejecutar el workflow hijo con permisos diferentes. Un workflow de PR viene de un fork y tiene acceso limitado; el `workflow_run` que se encadena tiene acceso completo al repositorio base.

> [ADVERTENCIA] Si el campo `workflows` contiene el nombre exacto de otro workflow pero ese workflow no tiene ejecuciones previas, `workflow_run` nunca se dispara. El nombre debe coincidir exactamente, incluyendo mayúsculas y espacios.

---

## Trigger manual workflow_dispatch: inputs y uso desde UI y API

El evento `workflow_dispatch` convierte un workflow en una tarea ejecutable manualmente desde la interfaz de GitHub o desde la API. Sin este evento, no aparece el botón "Run workflow" en la pestaña Actions.

Los inputs permiten parametrizar la ejecución manual. Cada input tiene un nombre, una descripción, un tipo, y opcionalmente un valor por defecto y una marca de obligatoriedad.

Los tipos de input disponibles son: `string` (texto libre), `boolean` (checkbox), `choice` (lista desplegable con opciones predefinidas), `environment` (selector de entornos configurados en el repositorio), y `number` (valor numérico, tratado como string en el contexto de expresiones).

Desde la UI, al pulsar "Run workflow", GitHub muestra un formulario generado automáticamente a partir de los inputs definidos. El usuario rellena los valores y pulsa el botón de confirmación. Los valores llegan al workflow en `github.event.inputs.nombre_del_input` o mediante el contexto `inputs.nombre_del_input` (equivalentes).

Desde la API, se hace una petición POST a `/repos/{owner}/{repo}/actions/workflows/{workflow_id}/dispatches` con un body JSON que incluye la rama y el mapa de inputs. Esto permite automatizar la ejecución manual desde scripts o desde otros sistemas.

> [CONCEPTO] El evento `workflow_dispatch` no tiene filtros de branches/paths. Se ejecuta en la rama que el usuario selecciona en el formulario (o que se especifica en la llamada a la API). La rama seleccionada debe contener el archivo del workflow.

---

## Representación visual: flujo de evaluación de un trigger

El siguiente diagrama muestra el proceso de decisión que GitHub sigue cuando recibe un evento, desde la recepción hasta la creación (o no) de una ejecución.

```
Evento recibido (push, pull_request, etc.)
         |
         v
¿Existe archivo .github/workflows/*.yml
 en la rama correcta con ese evento en on:?
         |
    NO --+-- SÍ
    |         |
    v         v
No hay   ¿Coinciden los filtros
ejecución  branches/tags/paths?
              |
         NO --+-- SÍ
         |         |
         v         v
     No hay   ¿El workflow está
     ejecución  habilitado en el repo?
                   |
              NO --+-- SÍ
              |         |
              v         v
          No hay   Se crea workflow run
          ejecución  con estado "queued"
                         |
                         v
                  Los jobs se encolan
                  según disponibilidad
                  de runners
```

Este flujo explica por qué la ausencia de ejecuciones puede deberse a múltiples causas distintas y por qué el diagnóstico requiere verificar cada nivel por separado.

---

## Ejemplo central: workflow con filtros y dispatch manual

El siguiente workflow ilustra la combinación de filtros de trigger, un trigger manual con inputs, y la estructura que permite diagnosticar su comportamiento desde la UI.

```yaml
# .github/workflows/build-and-test.yml
name: Build and Test

on:
  push:
    branches:
      - main
      - "release/**"
    paths:
      - "src/**"
      - "tests/**"
      - "package.json"
    tags-ignore:
      - "draft-*"
  pull_request:
    branches:
      - main
    paths:
      - "src/**"
      - "tests/**"
  workflow_dispatch:
    inputs:
      environment:
        description: "Entorno de despliegue"
        required: true
        type: choice
        options:
          - staging
          - production
        default: staging
      run_integration_tests:
        description: "Ejecutar tests de integración"
        required: false
        type: boolean
        default: false
      version_tag:
        description: "Etiqueta de versión (opcional)"
        required: false
        type: string

jobs:
  lint:
    name: Lint
    runs-on: ubuntu-latest
    steps:
      - name: Checkout código
        uses: actions/checkout@v4

      - name: Configurar Node.js
        uses: actions/setup-node@v4
        with:
          node-version: "20"
          cache: "npm"

      - name: Instalar dependencias
        run: npm ci

      - name: Ejecutar linter
        run: npm run lint

  unit-tests:
    name: Unit Tests
    runs-on: ubuntu-latest
    needs: lint
    steps:
      - name: Checkout código
        uses: actions/checkout@v4

      - name: Configurar Node.js
        uses: actions/setup-node@v4
        with:
          node-version: "20"
          cache: "npm"

      - name: Instalar dependencias
        run: npm ci

      - name: Ejecutar tests unitarios
        run: npm run test:unit

      - name: Subir informe de cobertura
        uses: actions/upload-artifact@v4
        with:
          name: coverage-report
          path: coverage/
          retention-days: 7

  integration-tests:
    name: Integration Tests
    runs-on: ubuntu-latest
    needs: unit-tests
    if: >
      github.event_name == 'workflow_dispatch' &&
      github.event.inputs.run_integration_tests == 'true'
    steps:
      - name: Checkout código
        uses: actions/checkout@v4

      - name: Configurar Node.js
        uses: actions/setup-node@v4
        with:
          node-version: "20"
          cache: "npm"

      - name: Instalar dependencias
        run: npm ci

      - name: Ejecutar tests de integración
        run: npm run test:integration
        env:
          TEST_ENV: ${{ github.event.inputs.environment }}

  build:
    name: Build
    runs-on: ubuntu-latest
    needs: unit-tests
    steps:
      - name: Checkout código
        uses: actions/checkout@v4

      - name: Configurar Node.js
        uses: actions/setup-node@v4
        with:
          node-version: "20"
          cache: "npm"

      - name: Instalar dependencias
        run: npm ci

      - name: Construir aplicación
        run: npm run build
        env:
          BUILD_ENV: ${{ github.event.inputs.environment || 'staging' }}

      - name: Subir artefacto de build
        uses: actions/upload-artifact@v4
        with:
          name: build-output
          path: dist/
          retention-days: 30
```

Este workflow tiene comportamientos que vale la pena señalar. El job `integration-tests` tiene una condición `if:` que evalúa dos cosas: que el evento sea `workflow_dispatch` y que el input `run_integration_tests` sea `'true'` (como string, porque los inputs siempre son strings aunque el tipo declarado sea boolean). Si el workflow se dispara por push o pull_request, ese job aparecerá en la UI con estado "skipped". El job `build` usa `github.event.inputs.environment || 'staging'` para tener un valor por defecto cuando el evento no es `workflow_dispatch`.

---

## Vista de grafo de un workflow: leer dependencias entre jobs

La vista de grafo (graph view) es la representación visual principal de un workflow run en la UI de GitHub Actions. Muestra cada job como un nodo y las dependencias entre jobs como flechas dirigidas. Esta vista aparece automáticamente en la pestaña de cada ejecución.

En el workflow del ejemplo anterior, el grafo tendría esta forma:

```
[lint]
   |
   v
[unit-tests]
   |
   +--------+
   v        v
[integration-tests]  [build]
```

Los nodos se organizan en columnas verticales según el nivel de dependencia: jobs sin `needs` en la primera columna, jobs que dependen de ellos en la segunda, y así sucesivamente. Los jobs que están al mismo nivel y no tienen dependencia entre sí aparecen en paralelo en la misma columna.

El grafo es interactivo: hacer clic en un nodo abre los logs de ese job en la misma página. Cada nodo muestra el nombre del job, su estado visual mediante un icono, y la duración de ejecución una vez que completa. Cuando un job está en ejecución, el nodo muestra una animación de carga.

La vista de grafo es especialmente útil cuando un workflow tiene muchos jobs con dependencias complejas. Ver el grafo permite identificar de un vistazo cuál fue el job que falló y qué jobs posteriores quedaron bloqueados como consecuencia.

---

## Estados visuales de jobs: queued, in_progress, completed y sus variantes

Cada job en GitHub Actions tiene un estado que se refleja visualmente en el grafo y en la lista de jobs. Entender qué significa cada estado es importante tanto para el examen como para el diagnóstico diario.

La siguiente tabla describe todos los estados posibles y su representación visual en la UI:

| Estado | Icono | Significado |
|---|---|---|
| `queued` | Círculo gris | El job está esperando un runner disponible |
| `in_progress` | Círculo amarillo animado | El job está ejecutándose actualmente |
| `success` | Check verde | El job completó sin errores (todos los steps exitosos) |
| `failure` | X roja | Al menos un step falló (exit code distinto de 0) |
| `cancelled` | Círculo gris con X | La ejecución fue cancelada manualmente |
| `skipped` | Círculo gris con barra | El job fue omitido por su condición `if:` |
| `timed_out` | Reloj rojo | El job superó el `timeout-minutes` configurado |

La distinción entre `cancelled` y `skipped` es importante. Un job cancelado es uno que empezó (o estaba en cola) y fue interrumpido por una acción externa: el usuario canceló la ejecución, o un job anterior falló y el job actual tenía la condición por defecto `if: ${{ success() }}`. Un job skipped es uno que nunca empezó porque su condición `if:` evaluó a falso desde el principio.

Cuando un job falla, todos los jobs que dependen de él mediante `needs:` también se cancelan por defecto. Para evitar este comportamiento y permitir que un job posterior se ejecute incluso si su predecesor falló, se usa `if: always()` o `if: ${{ failure() }}`.

---

## Jobs saltados (skipped) por condiciones if

Un job con estado skipped es uno que GitHub decidió no ejecutar porque su condición `if:` fue evaluada como falsa. Este estado no es un error: es el comportamiento esperado cuando se usan condiciones para hacer workflows condicionales.

En el workflow de ejemplo, el job `integration-tests` tiene esta condición:

```yaml
if: >
  github.event_name == 'workflow_dispatch' &&
  github.event.inputs.run_integration_tests == 'true'
```

Cuando el workflow se dispara por push, `github.event_name` es `'push'`, la condición evalúa a falso, y el job aparece como skipped en el grafo. El grafo todavía muestra el nodo, pero con el icono de skipped, lo que indica que el job existe en la definición pero no se ejecutó en esta ocasión.

Un detalle relevante: los jobs skipped no bloquean los jobs que dependen de ellos. Si `build` dependiera de `integration-tests` con `needs: integration-tests`, y `integration-tests` fue skipped, `build` también sería skipped por defecto. Para que `build` se ejecute independientemente, necesitaría `if: always()` o una condición explícita que lo permita.

> [EXAMEN] La diferencia entre un job con `if: false` (siempre skipped) y un job con una condición que depende del evento es conceptualmente la misma, pero la segunda es la forma útil. Un job permanentemente skipped generalmente indica un error de configuración.

---

## Indicadores de tiempo: queue time vs run time

La UI de GitHub Actions muestra dos métricas de tiempo que tienen significados distintos y son relevantes para diagnosticar problemas de rendimiento.

El queue time (tiempo de cola) es el tiempo que el job esperó desde que fue creado hasta que un runner empezó a ejecutarlo. Un queue time alto indica que no hay runners disponibles: el repositorio puede haber alcanzado el límite de jobs concurrentes, o los runners autoalojados (self-hosted) están todos ocupados o sin conexión.

El run time (tiempo de ejecución) es el tiempo que el job tardó en ejecutarse una vez que el runner lo tomó. Este es el tiempo que normalmente se optimiza mediante caché de dependencias, paralelización de steps, o selección de runners más potentes.

En la vista de grafo, el tiempo que se muestra en cada nodo es el run time. Para ver el queue time, es necesario abrir el job individual y mirar los metadatos en la parte superior, donde aparece "Queued" con una marca de tiempo.

Una situación común en equipos que usan runners self-hosted es que el queue time sea muy alto durante las horas pico porque todos los runners están ocupados. La solución típica es aumentar el número de runners o usar auto-scaling con grupos de runners.

---

## Pestaña Summary vs pestaña de job individual

Cuando se abre una ejecución de workflow en la UI, hay dos vistas principales que ofrecen información diferente y complementaria.

La pestaña Summary (Resumen) muestra una visión general de toda la ejecución. Contiene el grafo de jobs con sus estados, metadatos de la ejecución (quién la disparó, qué evento, qué commit, qué rama, cuándo empezó y terminó), los artefactos generados por la ejecución (si los hay), y cualquier anotación de error o advertencia que los jobs hayan generado mediante workflow commands.

La pestaña de job individual muestra los logs detallados de ese job específico. Cada step aparece como una sección colapsable con su nombre, duración, y resultado. Los steps fallidos se expanden automáticamente para mostrar el error. Los steps exitosos están colapsados por defecto para no abrumar con información. Al hacer clic en el timestamp de una línea de log se genera una URL directa a esa línea, útil para compartir con compañeros al reportar un problema.

Una diferencia importante: las anotaciones de error que genera GitHub a partir de los logs de los steps aparecen en la pestaña Summary, no solo en los logs del job. Esto facilita ver de un vistazo qué salió mal sin tener que navegar por los logs completos.

> [CONCEPTO] Los step groups creados con `::group::` y `::endgroup::` en workflow commands solo son visibles en la pestaña de job individual, no en el Summary. El Summary solo muestra anotaciones, artefactos, y el grafo.

---

## Badges de estado del workflow: sintaxis y cómo embeber en README

Un badge de estado es una imagen dinámica que refleja el resultado de la última ejecución de un workflow en una rama específica. Es habitual verlos en la parte superior de los README de repositorios open source.

La URL de un badge sigue este patrón:

```
https://github.com/{owner}/{repo}/actions/workflows/{workflow-file}.yml/badge.svg
```

Para mostrar el badge en un README en formato Markdown, se envuelve en un enlace que lleva a la página de ejecuciones del workflow:

```markdown
[![Build and Test](https://github.com/mi-org/mi-repo/actions/workflows/build-and-test.yml/badge.svg)](https://github.com/mi-org/mi-repo/actions/workflows/build-and-test.yml)
```

Por defecto, el badge muestra el estado de la última ejecución en la rama por defecto del repositorio. Para mostrar el estado de una rama específica, se añade el parámetro `branch`:

```
https://github.com/{owner}/{repo}/actions/workflows/{workflow-file}.yml/badge.svg?branch=develop
```

Para filtrar por evento, se añade el parámetro `event`:

```
https://github.com/{owner}/{repo}/actions/workflows/{workflow-file}.yml/badge.svg?event=push
```

Los valores posibles del badge son: "passing" (verde), "failing" (rojo), "no status" (gris, cuando no hay ejecuciones en esa rama), y "unknown" (cuando no se puede determinar el estado).

> [ADVERTENCIA] El nombre del archivo en la URL es el nombre del archivo YAML en `.github/workflows/`, no el valor del campo `name:` del workflow. Si el archivo se llama `ci.yml`, la URL usa `ci.yml`, no el nombre legible del workflow.

---

## Relación entre workflow run y check run

En GitHub, un workflow run y un check run son dos entidades distintas aunque relacionadas. Entender esta relación es importante para interpretar correctamente la UI y para la certificación.

Un workflow run es la ejecución completa de un archivo de workflow. Aparece en la pestaña Actions del repositorio y tiene un ID numérico propio. Contiene todos los jobs definidos en ese workflow para esa ejecución concreta.

Un check run es una unidad de verificación asociada a un commit específico. Cada job dentro de un workflow run genera automáticamente un check run. Estos check runs aparecen en la interfaz de pull requests como los "checks" que se muestran en la sección de estado del PR, y son los que determinan si el PR puede ser mergeado cuando hay branch protection rules configuradas.

La relación jerárquica es:

```
Workflow Run (1)
   |
   +-- Job "lint"         --> Check Run (con nombre "Build and Test / lint")
   |
   +-- Job "unit-tests"   --> Check Run (con nombre "Build and Test / unit-tests")
   |
   +-- Job "build"        --> Check Run (con nombre "Build and Test / build")
```

El nombre de cada check run se compone del nombre del workflow (campo `name:`) seguido del nombre del job. Las branch protection rules hacen referencia a estos nombres de check run, no al nombre del workflow run completo. Por eso, si se cambia el nombre del workflow o el nombre de un job, hay que actualizar también la branch protection rule correspondiente.

Desde la API, los workflow runs se consultan en `/repos/{owner}/{repo}/actions/runs` y los check runs en `/repos/{owner}/{repo}/commits/{ref}/check-runs`. Son endpoints distintos aunque representen el mismo trabajo.

> [EXAMEN] Una pregunta frecuente es: "¿Qué aparece en la sección de checks de un pull request?" La respuesta es: los check runs generados por los jobs de los workflows que se ejecutaron en el commit head del PR. No aparece el workflow run completo, sino cada job individualmente.

---

## Tabla de elementos clave

Esta tabla resume los parámetros de filtro de trigger y los campos de `workflow_dispatch` que aparecen con mayor frecuencia en el examen GH-200.

| Elemento | Tipo | Obligatorio | Default | Descripción |
|---|---|---|---|---|
| `branches` | lista de strings/glob | No | (todos) | Activa el workflow solo en las ramas que coinciden |
| `branches-ignore` | lista de strings/glob | No | (ninguno) | Excluye las ramas que coinciden; incompatible con `branches` |
| `paths` | lista de strings/glob | No | (todos) | Activa el workflow solo si algún archivo modificado coincide |
| `paths-ignore` | lista de strings/glob | No | (ninguno) | Salta el workflow solo si TODOS los archivos modificados coinciden; incompatible con `paths` |
| `tags` | lista de strings/glob | No | (todos) | Activa el workflow solo para tags que coinciden |
| `tags-ignore` | lista de strings/glob | No | (ninguno) | Excluye los tags que coinciden; incompatible con `tags` |
| `workflow_dispatch.inputs[].description` | string | No | `""` | Texto descriptivo que aparece en el formulario de la UI |
| `workflow_dispatch.inputs[].required` | boolean | No | `false` | Si es true, el campo es obligatorio en la UI |
| `workflow_dispatch.inputs[].type` | string | No | `string` | Tipo del input: `string`, `boolean`, `choice`, `environment`, `number` |
| `workflow_dispatch.inputs[].default` | string/boolean | No | `""` | Valor por defecto mostrado en el formulario |
| `workflow_dispatch.inputs[].options` | lista de strings | Sí (si type=choice) | — | Opciones disponibles en el desplegable |
| `workflow_run.workflows` | lista de strings | Sí | — | Nombres de los workflows padre (campo `name:`, no nombre de archivo) |
| `workflow_run.types` | lista de strings | No | `[completed]` | Tipos de evento del workflow padre: `requested`, `completed` |
| `workflow_run.branches` | lista de strings/glob | No | (todos) | Filtra por la rama en la que se ejecutó el workflow padre |

---

## Buenas y malas prácticas

Las siguientes prácticas están basadas en errores comunes que causan confusión en equipos que trabajan con GitHub Actions, especialmente cuando se diagnostican problemas de triggers y se interpreta la UI.

**Hacer: usar `paths` para evitar ejecuciones innecesarias**

Cuando un repositorio tiene múltiples directorios con propósitos distintos (frontend, backend, docs), añadir filtros `paths` a cada workflow evita que un cambio en la documentación dispare el pipeline de CI del backend. Esto reduce el consumo de minutos de Actions y acelera el feedback loop.

**Evitar: mezclar `branches` y `branches-ignore` en el mismo evento**

GitHub no permite usar `branches` y `branches-ignore` simultáneamente en el mismo bloque de evento. El archivo de workflow no se valida correctamente y el comportamiento es indefinido. Elige uno de los dos según si es más fácil listar las ramas incluidas o las excluidas.

**Hacer: nombrar los workflows con nombres descriptivos y estables**

El campo `name:` de un workflow es el identificador que aparece en los check runs de los pull requests y en los badges. Si se usa `workflow_run` para encadenar workflows, el campo `workflows` hace referencia a este nombre. Cambiar el nombre rompe las branch protection rules y los workflows encadenados. Usa nombres que describan el propósito y que no necesites cambiar frecuentemente.

**Evitar: depender de `workflow_run` sin verificar que el workflow padre está en la rama por defecto**

El workflow que usa `workflow_run` como trigger solo funciona si está definido en la rama por defecto del repositorio. Si pruebas el workflow en una rama de feature, simplemente no se ejecutará. Esto confunde a los desarrolladores que asumen que el comportamiento debería ser el mismo que con otros triggers.

**Hacer: documentar qué inputs son obligatorios y qué valores aceptan**

Los inputs de `workflow_dispatch` aparecen como formulario en la UI, pero si no tienen descripción, el usuario no sabe qué valor poner. Añadir siempre el campo `description` y usar `type: choice` cuando los valores son un conjunto finito reduce los errores de invocación manual.

**Evitar: asumir que un job skipped significa que algo falló**

Un job con estado skipped es el comportamiento correcto cuando la condición `if:` se evalúa como falsa. No es un error. Sin embargo, muchos desarrolladores ven el icono gris de skipped y asumen que hay un problema. Si un job debería haberse ejecutado pero apareció como skipped, el diagnóstico correcto es revisar la condición `if:` y los valores de contexto en ese momento de la ejecución.

**Hacer: usar la URL directa a una línea de log al reportar problemas**

Al compartir un error con un compañero, en lugar de pegar texto del log, haz clic en el timestamp de la línea de error para obtener una URL que lleva directamente a esa línea. Esto elimina la ambigüedad sobre qué log y qué ejecución concreta se está discutiendo.

---

## Verificación y práctica

### Preguntas de examen estilo GH-200

**Pregunta 1**

Un workflow tiene la siguiente configuración:

```yaml
on:
  push:
    branches:
      - main
    paths:
      - "src/**"
```

Se hace un push a la rama `main` que modifica únicamente el archivo `README.md`. ¿Qué sucede?

A) El workflow se ejecuta porque la rama coincide  
B) El workflow no se ejecuta porque el path no coincide  
C) El workflow falla con un error de configuración  
D) El workflow se ejecuta con estado "skipped"

**Respuesta correcta: B.** Los filtros `branches` y `paths` son aditivos: ambos deben coincidir para que el workflow se ejecute. La rama es correcta, pero ningún archivo modificado coincide con `src/**`, así que el workflow no se dispara.

---

**Pregunta 2**

Un workflow usa `workflow_run` para ejecutarse cuando otro workflow completa. El equipo prueba el nuevo workflow en una rama de feature llamada `feature/chain-test`. Después de que el workflow padre completa en `main`, el workflow hijo no se ejecuta. ¿Cuál es la causa más probable?

A) El campo `workflows` tiene el nombre incorrecto del workflow padre  
B) El workflow hijo no está en la rama por defecto del repositorio  
C) El evento `workflow_run` solo funciona en repositorios públicos  
D) Falta la permission `actions: read` en el workflow hijo

**Respuesta correcta: B.** Los workflows que usan `workflow_run` como trigger solo son leídos por GitHub desde la rama por defecto del repositorio. El workflow en `feature/chain-test` es ignorado.

---

**Pregunta 3**

En la vista de grafo de un workflow run, el job `deploy` aparece con estado "cancelled" aunque el job `build` que le precede aparece con estado "success". ¿Cuál es la explicación más probable?

A) El job `deploy` tiene un `timeout-minutes` muy bajo  
B) Un usuario canceló manualmente la ejecución mientras `deploy` estaba en cola  
C) El job `deploy` tiene una condición `if: failure()` que evaluó a verdadero  
D) El runner asignado a `deploy` se desconectó

**Respuesta correcta: B.** Si el job previo fue exitoso y el job posterior aparece como cancelled (no como skipped), lo más probable es que la ejecución fue cancelada manualmente mientras el job estaba en cola. Un job con `if: failure()` y un predecesor exitoso aparecería como skipped, no como cancelled.

---

### Ejercicio práctico

Crea un workflow que cumpla los siguientes requisitos:

1. Se dispara en push a ramas que empiecen por `release/`, pero solo si se modifican archivos en `src/` o `config/`
2. Se puede disparar manualmente con un input de tipo `choice` llamado `target_region` con opciones `eu-west-1`, `us-east-1`, y `ap-southeast-1`; el valor por defecto es `eu-west-1`
3. Tiene tres jobs: `validate`, `package`, y `notify`
4. `package` depende de `validate`
5. `notify` siempre se ejecuta, incluso si `validate` o `package` fallan
6. `package` solo se ejecuta si el trigger fue un push (no dispatch manual)

**Solución:**

```yaml
# .github/workflows/release-deploy.yml
name: Release Deploy

on:
  push:
    branches:
      - "release/**"
    paths:
      - "src/**"
      - "config/**"
  workflow_dispatch:
    inputs:
      target_region:
        description: "Región de despliegue"
        required: true
        type: choice
        options:
          - eu-west-1
          - us-east-1
          - ap-southeast-1
        default: eu-west-1

jobs:
  validate:
    name: Validate
    runs-on: ubuntu-latest
    steps:
      - name: Checkout código
        uses: actions/checkout@v4

      - name: Validar configuración
        run: |
          echo "Validando configuración para región: ${{ github.event.inputs.target_region || 'eu-west-1' }}"
          echo "Trigger: ${{ github.event_name }}"

  package:
    name: Package
    runs-on: ubuntu-latest
    needs: validate
    if: github.event_name == 'push'
    steps:
      - name: Checkout código
        uses: actions/checkout@v4

      - name: Empaquetar artefacto
        run: |
          echo "Empaquetando para rama: ${{ github.ref_name }}"
          mkdir -p dist
          echo "build-$(date +%Y%m%d%H%M%S)" > dist/version.txt

      - name: Subir artefacto
        uses: actions/upload-artifact@v4
        with:
          name: release-package
          path: dist/
          retention-days: 14

  notify:
    name: Notify
    runs-on: ubuntu-latest
    needs: [validate, package]
    if: always()
    steps:
      - name: Determinar resultado
        run: |
          echo "Estado de validate: ${{ needs.validate.result }}"
          echo "Estado de package: ${{ needs.package.result }}"
          echo "Trigger: ${{ github.event_name }}"

      - name: Enviar notificación
        run: |
          if [[ "${{ needs.validate.result }}" == "success" ]]; then
            echo "Pipeline completado exitosamente"
          else
            echo "Pipeline falló en la fase de validación"
          fi
```

En esta solución, `notify` usa `if: always()` para ejecutarse independientemente del resultado de los jobs anteriores. El job `package` tiene `if: github.event_name == 'push'`, por lo que aparecerá como skipped cuando el workflow se dispare manualmente. `notify` todavía se ejecuta porque `if: always()` ignora el estado de los predecesores.

---

← [1.20 Testing / Verificación de D1](gha-d1-testing.md) | [Índice](README.md) | [2.2 Lectura e interpretación de logs](gha-d2-logs.md) →
