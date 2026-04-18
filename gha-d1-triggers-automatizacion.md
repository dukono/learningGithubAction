# 1.3 Triggers de automatización

**Ruta de estudio:** [1.2 Triggers de código](gha-d1-triggers-codigo.md) ← actual → [1.4 Triggers de eventos del repositorio](gha-d1-triggers-eventos.md)

---

## Introducción

No todos los workflows se activan por cambios en el código. Hay escenarios donde necesitas ejecutar pipelines en un horario fijo (auditorías nocturnas, reportes semanales), permitir que un operador lance una ejecución manualmente sin hacer un commit, componer pipelines reutilizables que otros workflows llaman como subrutinas, o encadenar workflows de modo que uno arranque cuando otro termina. Para estos casos GitHub Actions ofrece cuatro triggers de automatización: `schedule`, `workflow_dispatch`, `workflow_call` y `workflow_run`.

---

## Tabla comparativa

| Trigger | Activado por | Caso de uso principal | Requiere cambio de código |
|---|---|---|---|
| `schedule` | Temporizador cron (UTC) | Jobs periódicos, auditorías, reportes | No |
| `workflow_dispatch` | Persona o API REST | Despliegues manuales, rollbacks | No |
| `workflow_call` | Otro workflow (`uses:`) | Lógica reutilizable (callee) | No |
| `workflow_run` | Evento de otro workflow | Pipelines encadenados, post-CI | No |

---

## Conceptos

### `schedule` — ejecución por cron

El trigger `schedule` acepta una o varias expresiones cron en formato de cinco campos separados por espacios: `minuto hora dia-mes mes dia-semana`. Todos los valores se interpretan en **UTC**. Por ejemplo, `'0 3 * * 1'` ejecuta el workflow todos los lunes a las 03:00 UTC.

GitHub impone una limitación importante: en repositorios con **poca actividad** (sin pushes recientes), el scheduler puede omitir ejecuciones programadas para conservar recursos. Esta degradación silenciosa es una fuente frecuente de confusión en el examen. Además, la granularidad mínima garantizada es de **5 minutos**; valores más frecuentes no están soportados. El workflow debe residir en la rama por defecto del repositorio para que el schedule tenga efecto.

```yaml
on:
  schedule:
    - cron: '0 3 * * 1'   # Lunes 03:00 UTC
    - cron: '30 8 1 * *'  # Día 1 de cada mes, 08:30 UTC
```

### `workflow_dispatch` — activación manual

`workflow_dispatch` permite lanzar el workflow desde la pestaña **Actions** de la UI o mediante la API REST (`POST /repos/{owner}/{repo}/actions/workflows/{workflow_id}/dispatches`). Su valor diferencial está en los **inputs** declarados bajo `inputs:`, que presentan un formulario al operador antes de ejecutar.

Los tipos de input soportados son: `string`, `boolean`, `choice` (lista desplegable con `options:`), `environment` (selecciona un environment configurado en el repo) y `number`. Cada input puede declarar `required: true` para exigir un valor y `default:` para pre-rellenarlo. El workflow solo aparece en el selector de la UI si está en la **rama por defecto** o en la rama que el usuario selecciona manualmente.

```yaml
on:
  workflow_dispatch:
    inputs:
      environment:
        type: environment
        required: true
      version:
        type: string
        default: 'latest'
      dry_run:
        type: boolean
        default: false
      log_level:
        type: choice
        options: [debug, info, warn, error]
        default: info
```

Los valores se acceden en el job con `${{ inputs.environment }}`, `${{ inputs.dry_run }}`, etc.

### `workflow_call` — workflow reutilizable (callee)

Cuando un workflow declara `workflow_call` como trigger, se convierte en un **callee** (workflow llamable). Otro workflow puede invocarlo con `uses: org/repo/.github/workflows/deploy.yml@main`. Este trigger define la interfaz pública del workflow reutilizable.

Los `inputs` bajo `workflow_call` siguen la misma sintaxis que `workflow_dispatch` (tipos, `required`, `default`). Los `outputs` permiten devolver valores al caller declarando `value: ${{ jobs.build.outputs.artifact_id }}`. La sección `secrets` puede listar secretos explícitos que el caller debe pasar, o usar `secrets: inherit` para heredar automáticamente todos los secretos del contexto del caller — esta opción es conveniente pero reduce el control sobre qué secretos se exponen.

```yaml
on:
  workflow_call:
    inputs:
      image_tag:
        type: string
        required: true
    outputs:
      deployed_url:
        value: ${{ jobs.deploy.outputs.url }}
    secrets:
      DEPLOY_TOKEN:
        required: true
```

El consumo de este workflow desde un caller se trata en detalle en [gha-d2-reusable-workflows-consumo.md](gha-d2-reusable-workflows-consumo.md). Las políticas enterprise sobre qué orgs pueden reutilizar workflows de otras se cubren en D4.

### `workflow_run` — encadenamiento de workflows

`workflow_run` dispara el workflow actual cuando otro workflow alcanza un determinado estado. Se configura con el parámetro `workflows:` (lista de nombres de workflows, tal como aparecen en su campo `name:`), `types:` (`completed` o `requested`) y opcionalmente `branches:` o `branches-ignore:` para filtrar por la rama que activó el workflow padre.

Una limitación crítica: `workflow_run` solo se activa desde la **rama por defecto** del repositorio receptor, independientemente de en qué rama corra el workflow origen. Además, si el workflow padre fue activado por un PR de un fork, `workflow_run` se ejecuta con los permisos del repositorio base (no del fork), lo que permite acceder a secretos de forma segura para escenarios de CI en forks.

```yaml
on:
  workflow_run:
    workflows: ['CI Build']
    types: [completed]
    branches: [main, 'release/**']
```

El campo `github.event.workflow_run.conclusion` permite al job verificar si el workflow padre tuvo éxito antes de proceder.

---

## Ejemplo central — los 4 triggers en un repositorio real

El siguiente YAML muestra cómo un repositorio puede combinar los cuatro triggers en workflows separados que cooperan entre sí.

```yaml
# .github/workflows/nightly-audit.yml
name: Nightly Audit
on:
  schedule:
    - cron: '0 2 * * *'   # Cada noche a las 02:00 UTC

jobs:
  audit:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: ./scripts/audit.sh
```

```yaml
# .github/workflows/deploy.yml
name: Manual Deploy
on:
  workflow_dispatch:
    inputs:
      target_env:
        type: environment
        required: true
        description: 'Entorno destino'
      image_tag:
        type: string
        default: 'latest'
      notify_slack:
        type: boolean
        default: true

jobs:
  deploy:
    runs-on: ubuntu-latest
    environment: ${{ inputs.target_env }}
    steps:
      - uses: actions/checkout@v4
      - run: ./scripts/deploy.sh ${{ inputs.image_tag }}
      - if: inputs.notify_slack
        run: ./scripts/notify.sh
```

```yaml
# .github/workflows/reusable-build.yml
name: Reusable Build
on:
  workflow_call:
    inputs:
      node_version:
        type: string
        default: '20'
      run_tests:
        type: boolean
        default: true
    outputs:
      artifact_name:
        value: ${{ jobs.build.outputs.artifact }}
    secrets:
      NPM_TOKEN:
        required: true

jobs:
  build:
    runs-on: ubuntu-latest
    outputs:
      artifact: ${{ steps.pack.outputs.name }}
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: ${{ inputs.node_version }}
      - run: npm ci
        env:
          NPM_TOKEN: ${{ secrets.NPM_TOKEN }}
      - if: inputs.run_tests
        run: npm test
      - id: pack
        run: echo "name=app-$(git rev-parse --short HEAD).tar.gz" >> $GITHUB_OUTPUT
```

```yaml
# .github/workflows/post-ci.yml
name: Post CI Notification
on:
  workflow_run:
    workflows: ['CI Build']
    types: [completed]
    branches: [main]

jobs:
  notify:
    runs-on: ubuntu-latest
    if: github.event.workflow_run.conclusion == 'failure'
    steps:
      - name: Alert on failure
        run: |
          echo "CI falló en ${{ github.event.workflow_run.head_branch }}"
          echo "Commit: ${{ github.event.workflow_run.head_sha }}"
```

---

## Tabla de elementos clave

| Elemento | Trigger | Valor / Comportamiento |
|---|---|---|
| `cron: '0 3 * * 1'` | `schedule` | Lunes 03:00 UTC |
| Frecuencia mínima | `schedule` | 5 minutos |
| Repos inactivos | `schedule` | Pueden omitirse ejecuciones |
| Tipos de input | `workflow_dispatch` | string, boolean, choice, environment, number |
| API endpoint | `workflow_dispatch` | `POST /repos/.../actions/workflows/.../dispatches` |
| `secrets: inherit` | `workflow_call` | Hereda todos los secretos del caller |
| `outputs.value` | `workflow_call` | Expresión que apunta a output de un job |
| `types: [completed]` | `workflow_run` | Solo cuando el workflow padre termina |
| `types: [requested]` | `workflow_run` | Cuando el workflow padre se encola |
| Rama de activación | `workflow_run` | Siempre desde la rama por defecto del callee |
| `conclusion` | `workflow_run` | `success`, `failure`, `cancelled`, `skipped` |

---

## Buenas y malas practicas

**`schedule`**

| Buena practica | Mala practica |
|---|---|
| Documentar la zona horaria equivalente en un comentario junto al cron para facilitar el mantenimiento. | Asumir que el job se ejecutara exactamente a la hora indicada en repos con baja actividad. |
| Usar `if: github.event_name == 'schedule'` para proteger pasos que solo tienen sentido en ejecucion periodica. | Confiar en `schedule` para SLAs estrictos sin un mecanismo de alerta por ejecuciones omitidas. |

**`workflow_dispatch`**

| Buena practica | Mala practica |
|---|---|
| Declarar `type: environment` para que GitHub valide que el entorno existe y aplique sus reglas de proteccion. | Usar inputs de tipo `string` sin validacion para valores criticos como la version a desplegar. |
| Combinar `workflow_dispatch` con `push` para que el mismo workflow pueda ejecutarse tanto en CI como manualmente. | Omitir `required: true` en inputs que el workflow necesita obligatoriamente, generando errores en tiempo de ejecucion. |

**`workflow_call`**

| Buena practica | Mala practica |
|---|---|
| Preferir secretos declarados explicitamente frente a `secrets: inherit` para minimizar la superficie de exposicion. | Declarar logica de negocio especifica de un equipo en un workflow reutilizable pensado para ser compartido entre multiples equipos. |
| Versionar el callee con tags (`uses: org/repo/.github/workflows/build.yml@v2`) para evitar cambios rupturistas involuntarios. | Usar `@main` en llamadas a workflows criticos de produccion sin un proceso de validacion previo. |

**`workflow_run`**

| Buena practica | Mala practica |
|---|---|
| Verificar `github.event.workflow_run.conclusion` antes de ejecutar pasos dependientes del resultado del workflow padre. | Encadenar mas de dos niveles de `workflow_run` creando dependencias dificiles de depurar. |
| Aprovechar que `workflow_run` corre con permisos del repo base para acceder a secretos en escenarios de PR desde forks. | Esperar que `workflow_run` se active desde ramas distintas a la rama por defecto del repositorio callee. |

---

## Verificacion GH-200

**Pregunta 1.** Un repositorio tiene un workflow con `schedule: cron: '*/2 * * * *'`. El repositorio no ha recibido commits en 60 dias. Que ocurrira con las ejecuciones programadas?

- A) Se ejecutaran cada 2 minutos sin problema
- B) GitHub rechazara la expresion cron por frecuencia invalida y el workflow no se activara
- C) GitHub puede omitir las ejecuciones al detectar inactividad en el repositorio
- D) El workflow se deshabilitara automaticamente y debera reactivarse manualmente

**Respuesta correcta: C.** La frecuencia de 2 minutos tampoco esta soportada (minimo 5), pero el motivo principal por el que las ejecuciones se omitiran es la inactividad del repositorio (opcion C cubre ambas casuisticas relevantes para el examen).

---

**Pregunta 2.** Un workflow callee declara `secrets: inherit` bajo `workflow_call`. Cual es la implicacion de seguridad correcta?

- A) El callee solo puede acceder a los secretos que el caller le pase explicitamente en `with:`
- B) El callee hereda automaticamente todos los secretos disponibles en el contexto del caller
- C) `secrets: inherit` solo funciona dentro de la misma organizacion
- D) El callee recibe los secretos de nivel de repositorio pero no los de nivel de organizacion

**Respuesta correcta: B.** `secrets: inherit` propaga todos los secretos del contexto del caller al callee sin que el caller los liste uno a uno.

---

**Pregunta 3.** Un workflow con trigger `workflow_run` esta definido en la rama `feature/notifications`. El workflow padre se ejecuto en la rama `main`. El callee se activara?

- A) Si, porque el workflow padre corrio en `main` y eso coincide con la rama por defecto
- B) No, porque `workflow_run` no puede filtrar por ramas
- C) Solo si el campo `branches: [main]` esta declarado en el callee
- D) No, porque `workflow_run` solo busca el archivo del callee en la rama por defecto del repositorio

**Respuesta correcta: D.** GitHub busca el archivo del workflow con `workflow_run` en la rama por defecto del repositorio. Si el archivo solo existe en `feature/notifications`, el trigger nunca se activara.

---

**Ejercicio practico**

Dado este objetivo: ejecutar un script de limpieza de artefactos cada domingo a las 00:00 UTC, pero tambien permitir que un administrador lo lance manualmente indicando si debe hacer un dry-run o no.

Escribe el bloque `on:` completo que satisfaga ambos requisitos en un unico workflow, asegurandote de que el input `dry_run` sea de tipo booleano con valor por defecto `false`.

```yaml
# Solucion
on:
  schedule:
    - cron: '0 0 * * 0'   # Domingo 00:00 UTC
  workflow_dispatch:
    inputs:
      dry_run:
        type: boolean
        default: false
        description: 'Simular sin eliminar artefactos'
```

---

**Navegacion:** [1.2 Triggers de codigo](gha-d1-triggers-codigo.md) | **1.3 Triggers de automatizacion** | [1.4 Triggers de eventos del repositorio](gha-d1-triggers-eventos.md)
