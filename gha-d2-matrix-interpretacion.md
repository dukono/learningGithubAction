# X.6 Interpretación de matrix strategy en ejecuciones

← [2.5 Artefactos: consumo, gestión y API](gha-d2-artefactos.md) | [Índice](README.md) | [2.7 Starter workflows](gha-d2-starter-workflows.md) →

---

La matrix strategy permite ejecutar el mismo job con distintas combinaciones de parámetros de forma paralela. Este fichero explica cómo leer e interpretar esas ejecuciones en la UI de GitHub Actions: qué significa cada job que aparece, cómo identificar cuál falló y qué efecto tienen `fail-fast` y `max-parallel`. La configuración YAML de matrix pertenece a D1 y se cubre en [gha-d1-matrix-strategy.md](gha-d1-matrix-strategy.md).

> [PREREQUISITO] Familiaridad con la configuración básica de `strategy.matrix` en YAML (ver gha-d1-matrix-strategy.md).

## Cómo la UI expande una matrix en jobs individuales

Cuando GitHub Actions procesa un workflow con `strategy.matrix`, genera automáticamente un job separado por cada combinación de valores. En la vista de ejecución, cada combinación aparece como un job independiente con un nombre que incluye los valores de la matrix entre paréntesis, por ejemplo `build (ubuntu-latest, 18)` o `build (windows-latest, 20)`. Para leer esta expansión es imprescindible entender el concepto A7.1: cada fila en la vista de jobs corresponde a una combinación específica, y puedes acceder a sus logs individuales haciendo clic sobre ella.

> [CONCEPTO] Una matrix de N×M genera N×M jobs independientes. Cada uno tiene su propio log, su propio runner y su propio resultado (success/failure/cancelled/skipped).

## fail-fast: comportamiento cuando una combinación falla

El concepto A7.2 es uno de los más evaluados en el examen. Por defecto, `fail-fast` está en `true`, lo que significa que si cualquier combinación de la matrix falla, GitHub cancela inmediatamente todas las demás combinaciones que todavía no han terminado. En la UI esto se muestra como jobs con estado `cancelled` aunque no hayan tenido ningún error propio. Si estableces `fail-fast: false`, todas las combinaciones se ejecutan hasta el final independientemente de si otras fallan, lo que permite recoger resultados de todas las plataformas o versiones antes de decidir.

> [EXAMEN] Con `fail-fast: true` (por defecto), los jobs cancelados aparecen en la UI como `cancelled`, NO como `failed`. Esto confunde en preguntas donde se pregunta cuántos jobs fallaron vs. cuántos fueron cancelados.

## max-parallel: controlar el paralelismo visible en la UI

El parámetro `max-parallel` (A7.3) limita cuántas combinaciones de la matrix se ejecutan simultáneamente. Sin él, GitHub ejecuta todas las combinaciones en paralelo (hasta los límites de runners disponibles). Al establecer `max-parallel: 2`, en la vista de ejecución solo verás 2 jobs en estado `in_progress` al mismo tiempo; el resto permanecerán en `queued` hasta que uno de los activos finalice. Esto es útil para controlar el consumo de minutos o cuando hay recursos compartidos.

## include y exclude: combinaciones personalizadas

Los modificadores `include` y `exclude` (A7.4) alteran la lista de combinaciones que GitHub genera antes de lanzar los jobs. Con `exclude` se eliminan combinaciones específicas que no tienen sentido (por ejemplo, una versión de Node.js que no es compatible con un sistema operativo concreto). Con `include` se añaden combinaciones adicionales que no surgen del producto cartesiano base, o se agregan variables extra a combinaciones existentes. En la UI, el efecto es visible directamente: las combinaciones excluidas no aparecen como jobs, y las incluidas aparecen junto al resto con su nombre compuesto.

> [ADVERTENCIA] `include` no solo añade combinaciones nuevas: si un objeto en `include` coincide parcialmente con una combinación existente, GitHub añade las propiedades extra a esa combinación en lugar de crear una nueva fila.

## Identificar el job fallido y acceder a sus logs

El concepto A7.5 es práctico: cuando una matrix falla, la vista de jobs muestra el estado de cada combinación con su color correspondiente (rojo para failure, amarillo para cancelled). Para acceder a los logs del job que falló, haz clic directamente sobre su nombre en la vista de grafo. El nombre incluye los valores de la matrix, lo que te permite identificar inmediatamente qué combinación causó el problema (por ejemplo, `test (windows-latest, 16)` frente a `test (ubuntu-latest, 16)`). Si `fail-fast` estaba activo, los demás jobs aparecerán como `cancelled` y sus logs mostrarán que fueron interrumpidos.

## Límite de 256 combinaciones

GitHub impone un límite de 256 jobs generados por matrix en un único workflow (A7.6). Si el producto cartesiano de los valores supera ese número, el workflow fallará con un error de validación antes de ejecutar ningún job. En la UI este error aparece en la pestaña de la ejecución antes de mostrar ningún job. Para matrices grandes es importante calcular el número de combinaciones y usar `exclude` para reducirlas.

> [EXAMEN] El límite es 256 combinaciones por matrix, no 256 jobs totales en el workflow. Un workflow puede tener múltiples jobs con matrix, cada uno con su propio límite de 256.

## Representación visual: fail-fast vs. sin fail-fast

La siguiente tabla muestra el efecto de `fail-fast` en la UI cuando la combinación `(ubuntu-latest, 16)` falla en una matrix de 4 combinaciones:

| Combinación | fail-fast: true | fail-fast: false |
|---|---|---|
| ubuntu-latest, 16 | failure | failure |
| ubuntu-latest, 18 | cancelled | success |
| windows-latest, 16 | cancelled | failure (si también falla) |
| windows-latest, 18 | cancelled | success |

Con `fail-fast: true`, la UI muestra 1 failure y 3 cancelled. Con `fail-fast: false`, la UI muestra los resultados reales de cada combinación.

## Ejemplo central

El siguiente workflow demuestra una matrix con tres dimensiones (`os`, `node`, `include`/`exclude`), `fail-fast: false` y `max-parallel: 2`. Observa cómo la UI mostrará como máximo 2 jobs activos simultáneamente.

```yaml
# .github/workflows/matrix-test.yml
name: Matrix Test

on:
  push:
    branches: [main]

jobs:
  test:
    name: Test (${{ matrix.os }}, Node ${{ matrix.node }})
    runs-on: ${{ matrix.os }}
    strategy:
      fail-fast: false
      max-parallel: 2
      matrix:
        os: [ubuntu-latest, windows-latest, macos-latest]
        node: [18, 20]
        exclude:
          - os: macos-latest
            node: 18
        include:
          - os: ubuntu-latest
            node: 20
            experimental: true
    steps:
      - uses: actions/checkout@v4
      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: ${{ matrix.node }}
      - name: Install dependencies
        run: npm ci
      - name: Run tests
        run: npm test
      - name: Mark as experimental
        if: ${{ matrix.experimental == true }}
        run: echo "This combination is marked experimental"
```

Con esta configuración, GitHub genera las combinaciones del producto cartesiano (3 OS × 2 versiones = 6), elimina `(macos-latest, 18)` por el `exclude`, y añade la propiedad `experimental: true` a `(ubuntu-latest, 20)` por el `include`. El resultado son 5 jobs. En la UI verás máximo 2 en `in_progress` al mismo tiempo.

## Tabla de elementos clave

La siguiente tabla resume los parámetros de matrix strategy más relevantes para la interpretación en la UI:

| Parámetro | Tipo | Default | Efecto en la UI |
|---|---|---|---|
| `strategy.fail-fast` | boolean | `true` | Si `true`, cancela el resto al primer fallo |
| `strategy.max-parallel` | number | sin límite | Limita los jobs en `in_progress` simultáneos |
| `strategy.matrix` | map | — | Define las variables y sus valores |
| `matrix.include` | list | `[]` | Añade combinaciones o propiedades extra |
| `matrix.exclude` | list | `[]` | Elimina combinaciones del producto cartesiano |
| límite total | — | 256 | Error de validación si se supera |

## Buenas y malas prácticas

**Hacer:**
- **Usar `fail-fast: false`** cuando necesitas comparar resultados entre plataformas — razón: con el valor por defecto `true`, un fallo en Windows cancela Ubuntu antes de obtener sus resultados, ocultando información valiosa.
- **Nombrar los jobs con los valores de matrix** usando `name: Test (${{ matrix.os }}, ${{ matrix.node }})` — razón: el nombre por defecto es difícil de leer en la UI cuando hay muchas combinaciones.
- **Usar `exclude` para eliminar combinaciones inválidas** en lugar de añadir condiciones `if:` en los steps — razón: `exclude` evita que se cree el job, ahorrando minutos; las condiciones `if:` crean el job pero lo dejan inactivo.

**Evitar:**
- **Superar 256 combinaciones** sin revisarlo antes — razón: el workflow falla en validación antes de ejecutar ningún job, sin logs útiles.
- **Confundir `cancelled` con `failed`** al revisar resultados con `fail-fast: true` activo — razón: los jobs cancelados por `fail-fast` no tienen errores propios; el único job fallido es el que disparó la cancelación.
- **Usar `max-parallel: 1`** salvo en casos muy específicos de recursos compartidos — razón: elimina el beneficio de la paralelización y aumenta el tiempo total de ejecución.

## Verificación y práctica

**Pregunta 1:** Un workflow tiene una matrix de 4 combinaciones y `fail-fast` con su valor por defecto. La segunda combinación falla. ¿Qué estado muestran las combinaciones 3 y 4 en la UI?

A) failure  
B) skipped  
C) cancelled  
D) queued  

> **Respuesta correcta: C) cancelled** — Con `fail-fast: true` (valor por defecto), GitHub cancela las combinaciones restantes cuando una falla. Aparecen como `cancelled`, no como `failed` (A, que sería su propio fallo) ni como `skipped` (B, que requiere una condición `if:` que no se cumple) ni como `queued` (D, que es el estado antes de empezar a ejecutar).

**Pregunta 2:** ¿Cuál es el número máximo de jobs que puede generar una matrix strategy en GitHub Actions?

A) 100  
B) 256  
C) 500  
D) Sin límite  

> **Respuesta correcta: B) 256** — GitHub impone un límite de 256 combinaciones por matrix. Si el producto cartesiano supera este número, el workflow falla en validación. Las opciones A (100) y C (500) son incorrectas; D es incorrecta porque el límite existe.

**Pregunta 3:** Tienes una matrix con `os: [ubuntu-latest, windows-latest]` y `node: [16, 18, 20]`. Añades `exclude: [{os: windows-latest, node: 16}]`. ¿Cuántos jobs se generan?

A) 6  
B) 5  
C) 4  
D) 3  

> **Respuesta correcta: B) 5** — El producto cartesiano es 2×3 = 6 combinaciones. El `exclude` elimina exactamente 1 combinación `(windows-latest, 16)`, dejando 5 jobs.

**Ejercicio práctico:** Dado el siguiente workflow que falla en producción porque la combinación `(macos-latest, node 16)` no es compatible, modifícalo para: (1) eliminar esa combinación, (2) que el resto de combinaciones siempre terminen aunque una falle, (3) ejecutar máximo 3 jobs en paralelo.

```yaml
jobs:
  test:
    runs-on: ${{ matrix.os }}
    strategy:
      matrix:
        os: [ubuntu-latest, windows-latest, macos-latest]
        node: [16, 18, 20]
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: ${{ matrix.node }}
      - run: npm test
```

**Solución:**

```yaml
jobs:
  test:
    name: Test (${{ matrix.os }}, Node ${{ matrix.node }})
    runs-on: ${{ matrix.os }}
    strategy:
      fail-fast: false
      max-parallel: 3
      matrix:
        os: [ubuntu-latest, windows-latest, macos-latest]
        node: [16, 18, 20]
        exclude:
          - os: macos-latest
            node: 16
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: ${{ matrix.node }}
      - run: npm test
```

Los cambios: `fail-fast: false` garantiza que todas las combinaciones restantes terminen; `max-parallel: 3` limita el paralelismo; `exclude` elimina la combinación inválida, reduciendo los jobs de 9 a 8.

---

← [2.5 Artefactos: consumo, gestión y API](gha-d2-artefactos.md) | [Índice](README.md) | [2.7 Starter workflows](gha-d2-starter-workflows.md) →
