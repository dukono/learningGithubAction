# 1.5b Propiedades de control de flujo y ejecución del job

← [1.5.1 Propiedades de identidad](gha-d1-jobs-identidad.md) | → [1.6 Configuración de steps](gha-d1-steps-configuracion.md)

---

## Introducción

En pipelines complejos, raramente todos los jobs son independientes. Un job de despliegue no debe ejecutarse si las pruebas fallan; un job de notificación solo tiene sentido cuando el anterior termina, sea con éxito o no. GitHub Actions ofrece cinco propiedades que gobiernan cómo y cuándo se ejecuta un job: `needs`, `if`, `concurrency`, `timeout-minutes` y `continue-on-error`. Dominar estas propiedades es la diferencia entre un workflow frágil y uno verdaderamente fiable.

---

## Diagrama de flujo: needs y condiciones

```
  [build] ──────────────────────────────────────────────────────────────┐
      │ éxito                                                            │
      ▼                                                                  │
  [test]  ──── if: success() ────► [deploy]                             │
      │                                                                  │
      └──── if: failure() ─────► [notify-failure]  ◄────────────────────┘
                                                       (también depende de build)
```

La palabra clave `needs` define el grafo de dependencias; `if` decide si el nodo del grafo se activa o se omite una vez que sus dependencias han terminado.

---

## Conceptos clave

### `needs`: dependencias entre jobs

`needs` declara qué jobs deben completarse antes de que el job actual pueda comenzar. Sin esta propiedad, todos los jobs de un workflow arrancan en paralelo por defecto.

La sintaxis acepta un **string** cuando la dependencia es única o un **array** cuando son varias:

```yaml
# String: un solo predecesor
deploy:
  needs: build

# Array: varios predecesores
integration-test:
  needs: [build, lint]
```

Un job en estado `needs` solo arranca si todos sus predecesores terminan en estado `success` (a menos que `if` lo modifique explícitamente). Si un predecesor falla y el job dependiente no tiene un `if` especial, este se marca como `skipped`, no como `failure`.

Dentro del job puede accederse a las salidas de los predecesores mediante el contexto `needs.<job_id>.outputs.<nombre>` y comprobar su resultado con `needs.<job_id>.result`.

### `if` a nivel de job: condiciones para saltar el job entero

La propiedad `if` evalúa una expresión antes de reservar el runner. Si la expresión devuelve `false`, el job se marca como `skipped` sin consumir minutos de facturación.

```yaml
deploy:
  needs: test
  if: github.ref == 'refs/heads/main'
```

A nivel de job, las funciones de estado (`success()`, `failure()`, `cancelled()`, `always()`) evalúan el resultado del conjunto de jobs listados en `needs`, no el del workflow completo. Esto permite patrones como ejecutar un job de notificación solo cuando algo falla:

```yaml
notify:
  needs: [build, test, deploy]
  if: failure()
```

Sin `always()` o `failure()`, un job que depende de otro fallido es simplemente omitido, nunca ejecutado.

### `concurrency` a nivel de job: group y cancel-in-progress

`concurrency` permite agrupar ejecuciones del mismo job para evitar carreras de condición (por ejemplo, dos despliegues simultáneos al mismo entorno). Puede definirse con una clave `group` que identifica el grupo y, opcionalmente, `cancel-in-progress`.

```yaml
deploy:
  concurrency:
    group: deploy-${{ github.ref }}
    cancel-in-progress: true
```

Cuando `cancel-in-progress: true`, cualquier ejecución anterior del grupo que aún esté en curso se cancela en el momento en que arranca la nueva. Si se omite o es `false`, la nueva ejecución queda en cola hasta que la anterior termine.

**Diferencia con `concurrency` de workflow:** la propiedad `concurrency` en la raíz del workflow afecta a todas las ejecuciones del workflow completo; la propiedad dentro de un job afecta exclusivamente a ese job y permite que otros jobs del mismo workflow se ejecuten sin restricción. Esto es útil cuando solo el job de despliegue necesita exclusividad, pero los jobs de compilación y pruebas pueden correr en paralelo sin problema.

### `timeout-minutes`: límite máximo de duración del job

`timeout-minutes` fija el número máximo de minutos que puede ejecutarse un job. El valor por defecto es **360 minutos (6 horas)**, que es también el máximo permitido para jobs en runners alojados por GitHub.

```yaml
long-build:
  timeout-minutes: 30
```

Cuando se alcanza el límite, GitHub Actions cancela el job de forma forzada: los steps en curso se interrumpen, no se ejecutan los steps posteriores y el job queda en estado `cancelled`. Cualquier job que dependía de él (vía `needs`) y no tenga `if: always()` o `if: cancelled()` pasará a `skipped`.

Establecer un timeout explícito y ajustado a la realidad del job es una buena práctica de seguridad: evita que un runner quede bloqueado indefinidamente consumiendo minutos de facturación si un proceso cuelga.

### `continue-on-error` del job: efecto en el resultado del workflow

`continue-on-error: true` permite que el workflow continúe incluso si el job falla. El job aparece en la interfaz como `failed`, pero esa condición no se propaga como fallo al resultado global del workflow.

```yaml
experimental-feature:
  continue-on-error: true
```

Es especialmente útil para jobs opcionales —por ejemplo, pruebas con una versión canary de una dependencia o análisis de calidad no bloqueantes— en los que un fallo no debe impedir el despliegue principal.

**Distinción importante:** `continue-on-error` a nivel de job difiere del mismo atributo a nivel de step. A nivel de step, el fallo del step no marca el job como fallido; a nivel de job, el job sí se marca como fallido, pero el workflow no hereda ese estado de fallo.

---

## Ejemplo central: pipeline de tres jobs

```yaml
name: Pipeline CI/CD

on:
  push:
    branches: [main, develop]

jobs:
  build:
    runs-on: ubuntu-latest
    timeout-minutes: 20
    outputs:
      artifact-id: ${{ steps.upload.outputs.artifact-id }}
    steps:
      - uses: actions/checkout@v4
      - name: Compile
        run: make build
      - id: upload
        uses: actions/upload-artifact@v4
        with:
          name: dist
          path: dist/

  test:
    needs: build
    runs-on: ubuntu-latest
    timeout-minutes: 15
    concurrency:
      group: test-${{ github.ref }}
      cancel-in-progress: true
    steps:
      - uses: actions/checkout@v4
      - uses: actions/download-artifact@v4
        with:
          name: dist
      - run: make test

  deploy:
    needs: test
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    timeout-minutes: 10
    concurrency:
      group: deploy-production
      cancel-in-progress: false
    environment: production
    steps:
      - uses: actions/checkout@v4
      - uses: actions/download-artifact@v4
        with:
          name: dist
      - run: make deploy
        env:
          DEPLOY_TOKEN: ${{ secrets.DEPLOY_TOKEN }}

  notify-failure:
    needs: [build, test, deploy]
    runs-on: ubuntu-latest
    if: failure()
    steps:
      - name: Send alert
        run: |
          echo "Pipeline failed for ${{ github.sha }}"
          # Llamada a webhook de Slack, etc.
```

En este ejemplo:
- `build` no tiene dependencias y corre en paralelo con nada más al inicio.
- `test` espera a `build` y cancela runs anteriores del mismo grupo si llega un push nuevo.
- `deploy` solo corre en `main`, espera a `test` y no cancela deploys en curso (evita interrumpir un despliegue activo).
- `notify-failure` corre si cualquiera de los tres anteriores falla, gracias a `if: failure()`.

---

## Tabla de referencia rápida

| Propiedad | Tipo | Valor por defecto | Efecto principal |
|---|---|---|---|
| `needs` | string \| array | — (sin dependencia) | Ordena la ejecución; job queda `skipped` si predecesor falla |
| `if` | expresión | `true` | Salta el job sin consumir runner si evalúa `false` |
| `concurrency.group` | string | — | Agrupa ejecuciones para evitar paralelismo no deseado |
| `concurrency.cancel-in-progress` | boolean | `false` | Cancela ejecuciones anteriores del mismo grupo |
| `timeout-minutes` | número | `360` | Cancela el job si supera el límite |
| `continue-on-error` | boolean | `false` | El fallo del job no marca el workflow como fallido |

---

## Buenas y malas prácticas

| Buena practica | Mala practica |
|---|---|
| Ajustar `timeout-minutes` al tiempo real esperado del job (p. ej. 20 min para builds) | Dejar el timeout por defecto de 6 horas: un proceso colgado puede consumir minutos durante horas |
| Usar `if: failure()` en jobs de notificación para garantizar que se ejecutan cuando algo sale mal | Omitir `if: failure()` en jobs de notificación: si un predecesor falla, el notificador queda `skipped` y el equipo no se entera |
| Usar `cancel-in-progress: true` en entornos de staging para descartar runs obsoletos | Usar `cancel-in-progress: true` en producción: puede interrumpir un despliegue activo y dejar el entorno en estado inconsistente |
| Limitar `continue-on-error: true` a jobs explícitamente experimentales o no bloqueantes | Aplicar `continue-on-error: true` de forma genérica para "silenciar" fallos: enmascara problemas reales |
| Definir `concurrency` solo en los jobs que realmente necesitan exclusividad | Definir `concurrency` al nivel del workflow completo cuando solo un job lo requiere: bloquea innecesariamente los jobs de CI |
| Documentar con comentarios YAML por qué un job tiene `if` o `continue-on-error` | Dejar condiciones `if` sin comentario: dificulta el mantenimiento y el diagnóstico |

---

## Patrones avanzados y casos de uso

### Acceder al resultado de un predecesor con `needs.<id>.result`

Cuando un job tiene `continue-on-error: true`, su resultado en el contexto `needs` puede ser `success` aunque internamente haya fallado. Para distinguirlo se usa `needs.<id>.result`:

```yaml
post-process:
  needs: experimental
  if: needs.experimental.result == 'success'
```

Los valores posibles de `.result` son: `success`, `failure`, `cancelled` y `skipped`.

### Encadenamiento condicional para entornos multistage

Un patrón habitual en pipelines de entrega continua es encadenar jobs de despliegue con `if` que comprueban la rama o el entorno anterior:

```yaml
deploy-staging:
  needs: test
  if: github.ref == 'refs/heads/develop'
  environment: staging

deploy-production:
  needs: deploy-staging
  if: github.ref == 'refs/heads/main'
  environment: production
```

En este caso `deploy-production` nunca corre desde `develop` porque `deploy-staging` quedaria `skipped` al no cumplirse su `if`, y un job con predecesor `skipped` tambien queda `skipped` por defecto.

### Timeout diferenciado por tipo de job

No todos los jobs tienen la misma naturaleza. Una buena heuristica:

| Tipo de job | Timeout recomendado |
|---|---|
| Linting / format check | 5 min |
| Compilacion / build | 15-30 min |
| Tests unitarios | 10-20 min |
| Tests de integracion / E2E | 30-60 min |
| Despliegue | 10-15 min |
| Jobs de infraestructura (Terraform) | 20-45 min |

Definir timeouts ajustados reduce el riesgo de que un runner quede bloqueado y permite al equipo detectar regresiones de rendimiento en la propia CI.

---

## Referencias externas

Para interpretar por qué un job quedó en `skipped` o `cancelled`, o para leer los logs de un job que superó su timeout, consulta la guia de **diagnóstico y comportamiento del workflow** → [D2: Diagnóstico de comportamiento](gha-d2-diagnostico-comportamiento.md).

---

## Verificacion GH-200

**P1.** Un job `deploy` tiene `needs: test`. El job `test` falla. Sin ninguna otra configuracion, ¿en que estado quedara `deploy`?

<details>
<summary>Respuesta</summary>

`skipped`. GitHub Actions omite el job porque su predecesor no termino en `success`. Para que `deploy` se ejecute igualmente, deberia incluir `if: always()` o `if: failure()`.

</details>

**P2.** Tienes dos jobs que comparten `concurrency.group: deploy-prod`. El primero esta en curso y llega el segundo. Con `cancel-in-progress: false`, ¿que ocurre?

<details>
<summary>Respuesta</summary>

El segundo job queda en cola (`queued`) hasta que el primero termine. No se cancela ninguna ejecucion en curso.

</details>

**P3.** Un job tiene `continue-on-error: true` y falla. ¿Como aparece en la interfaz de GitHub y cual es el estado final del workflow?

<details>
<summary>Respuesta</summary>

El job aparece como `failed` (en rojo) en la interfaz, pero el workflow no hereda ese estado: si el resto de jobs tiene exito, el workflow se marca como `success`.

</details>

**Ejercicio practico:** Escribe un workflow con tres jobs: `lint`, `build` (depende de `lint`) y `report` (se ejecuta siempre, depende de ambos, e indica en su unico step si el pipeline tuvo exito o fallo). Establece un timeout de 10 minutos para `build` y asegurate de que un fallo en `build` no impida la ejecucion de `report`.

---

← [1.5.1 Propiedades de identidad](gha-d1-jobs-identidad.md) | → [1.6 Configuración de steps](gha-d1-steps-configuracion.md)
