# 1.13 Expresiones y funciones built-in

[← 1.12 Contextos de estado, datos y flujo](gha-d1-contextos-estado.md) | [1.14 Caché de dependencias →](gha-d1-cache.md)

---

## Introducción

El motor de expresiones de GitHub Actions es el mecanismo que permite evaluar lógica dinámica dentro de un workflow sin salir del YAML. Cada vez que escribes `${{ }}`, GitHub evalúa el contenido en tiempo de ejecución usando un subconjunto de JavaScript-like con acceso a contextos (`github`, `env`, `steps`, etc.) y a un conjunto de funciones built-in. Los casos de uso son amplios: decidir si un job debe ejecutarse, construir cadenas dinámicas para mensajes o rutas, generar claves de caché únicas por contenido de ficheros, o transformar estructuras de datos entre steps. Entender bien este motor es fundamental para escribir workflows robustos y eliminates ramas innecesarias de shell scripting.

---

## Sintaxis `${{ expression }}`

La sintaxis `${{ expression }}` indica a GitHub que evalúe el contenido como una expresión antes de ejecutar el step o evaluar la condición. Se puede usar en casi cualquier campo de valor del YAML: `if`, `env`, `with`, `run` (en la parte de valor del paso, no en el script en sí), `name`, `needs`, outputs de jobs y muchos más. La evaluación ocurre en el runner antes de que el shell vea el valor, lo que significa que el resultado es un valor ya resuelto —cadena, booleano, número o null— que se sustituye en el punto exacto donde aparece la sintaxis. Las expresiones no están disponibles en las claves (keys) del YAML, sólo en los valores. En contextos `if`, las llaves dobles son opcionales: `if: github.ref == 'refs/heads/main'` y `if: ${{ github.ref == 'refs/heads/main' }}` son equivalentes, aunque la segunda forma es más explícita y recomendada para evitar confusión.

```yaml
# Usos válidos de la sintaxis
name: "Build ${{ github.run_number }}"          # en el nombre del job
if: ${{ github.event_name == 'push' }}           # en condiciones
env:
  BRANCH: ${{ github.ref_name }}                 # en variables de entorno
with:
  token: ${{ secrets.GITHUB_TOKEN }}             # en inputs de actions
```

---

## Función `contains(search, item)`

`contains` comprueba si un valor está presente dentro de otro y devuelve un booleano. El parámetro `search` puede ser una cadena de texto o un array; `item` es el valor a buscar. Cuando `search` es una cadena, la búsqueda es de subcadena y es insensible a mayúsculas/minúsculas. Cuando es un array, comprueba igualdad exacta de elemento. Su caso de uso más frecuente en workflows es filtrar eventos o etiquetas: `contains(github.event.pull_request.labels.*.name, 'hotfix')` devuelve `true` si alguna etiqueta del PR se llama exactamente `hotfix`. También se usa con `toJSON` para convertir arrays de contexto a string cuando necesitas buscar en estructuras complejas.

```yaml
if: ${{ contains(github.event.pull_request.labels.*.name, 'deploy') }}
```

---

## Función `startsWith(searchString, searchValue)`

`startsWith` devuelve `true` si la cadena `searchString` comienza exactamente con `searchValue`. La comparación es insensible a mayúsculas/minúsculas. Su utilidad principal en workflows es discriminar ramas o referencias: comprobar si un push va a una rama de release, si la referencia es una etiqueta de versión, o si el nombre del runner comienza por un prefijo determinado. A diferencia de un operador `==` exacto, permite agrupar familias de valores bajo un mismo prefijo sin necesidad de expresiones regulares.

```yaml
if: ${{ startsWith(github.ref, 'refs/tags/v') }}
# true para v1.0.0, v2.3.1, vNext, etc.
```

---

## Función `endsWith(searchString, searchValue)`

`endsWith` es el complemento simétrico de `startsWith`: devuelve `true` si `searchString` termina con `searchValue`, también sin distinción de mayúsculas. Se usa con menos frecuencia que `startsWith`, pero resulta útil para comprobar extensiones de ficheros en paths, sufijos de nombres de ramas (`-hotfix`, `-rc`) o sufijos de identificadores. Como ambas funciones son case-insensitive, `endsWith('workflow.YML', '.yml')` devuelve `true`.

```yaml
if: ${{ endsWith(github.ref_name, '-hotfix') }}
# activa lógica especial para ramas terminadas en -hotfix
```

---

## Función `format(string, ...values)` con marcadores `{0}`, `{1}`

`format` realiza interpolación de plantillas: reemplaza los marcadores posicionales `{0}`, `{1}`, `{2}`… por los valores correspondientes pasados como argumentos adicionales. El resultado es siempre una cadena. Es especialmente útil para construir mensajes, URLs o identificadores compuestos sin concatenación manual con el operador `+`. Los marcadores pueden repetirse dentro de la plantilla, lo que permite reusar un mismo valor en múltiples posiciones. Si necesitas incluir un literal `{` o `}` en la salida, escápalo duplicándolo: `{{` y `}}`.

```yaml
- name: Notify
  run: echo "${{ format('PR #{0} en rama {1} por {2}', github.event.number, github.head_ref, github.actor) }}"
# Salida: PR #42 en rama feature/login por octocat
```

---

## Función `join(array, separator)`

`join` convierte un array en una cadena concatenando sus elementos con el separador indicado. Si `separator` se omite, usa coma como separador por defecto. Es útil cuando necesitas pasar una lista de valores como argumento a un script, construir una lista legible para un mensaje de notificación, o cuando una action espera un string delimitado en lugar de un array. Combinado con la sintaxis de wildcard `*.field` de los contextos, permite extraer todos los valores de un campo de un array de objetos y unirlos en una sola cadena.

```yaml
- name: Lista de etiquetas
  run: echo "Labels: ${{ join(github.event.pull_request.labels.*.name, ', ') }}"
# Salida: Labels: bug, enhancement, deploy
```

---

## Función `toJSON(value)`

`toJSON` serializa cualquier valor —objeto, array, número, booleano— a su representación como cadena JSON. Su uso más habitual es inspeccionar el contenido completo de un contexto durante el desarrollo del workflow para entender qué datos están disponibles. También se usa para pasar objetos complejos como outputs de un job o para formatear datos antes de enviarlos a una API. Cuando necesitas buscar dentro de un array de objetos con `contains`, convertirlo primero a JSON string te permite hacer búsquedas de subcadena sobre la representación serializada.

```yaml
- name: Debug context
  run: echo '${{ toJSON(github.event) }}'

- name: Pasar matriz como output
  id: matrix-gen
  run: echo "matrix={\"include\":[{\"os\":\"ubuntu-latest\"},{\"os\":\"windows-latest\"}]}" >> $GITHUB_OUTPUT
```

---

## Función `fromJSON(value)`

`fromJSON` es el inverso de `toJSON`: parsea una cadena JSON y devuelve el objeto, array o valor primitivo correspondiente. Su caso de uso más importante es la generación dinámica de matrices (`strategy.matrix`): un step anterior genera la cadena JSON del array de variantes, la almacena en un output, y el job posterior la parsea con `fromJSON` para construir la matriz. También permite recuperar estructuras complejas pasadas como strings entre jobs. Es importante que la cadena de entrada sea JSON válido; de lo contrario, el workflow fallará con un error de parsing en tiempo de evaluación.

```yaml
jobs:
  setup:
    outputs:
      matrix: ${{ steps.gen.outputs.matrix }}
    steps:
      - id: gen
        run: echo "matrix=[\"ubuntu-latest\",\"windows-latest\"]" >> $GITHUB_OUTPUT

  build:
    needs: setup
    strategy:
      matrix:
        os: ${{ fromJSON(needs.setup.outputs.matrix) }}
    runs-on: ${{ matrix.os }}
```

---

## Función `hashFiles('**/package-lock.json')`

`hashFiles` calcula un hash SHA-256 determinista del contenido de todos los ficheros que coincidan con el patrón glob proporcionado y devuelve una cadena hexadecimal. Su propósito principal es generar claves de caché que cambien automáticamente cuando cambian los ficheros de dependencias: si `package-lock.json` no ha cambiado entre dos ejecuciones, el hash es idéntico y se puede restaurar la caché; si ha cambiado, el hash es diferente y la caché se regenera. Acepta múltiples patrones separados por comas. El hash se calcula sobre el contenido real, no sobre la fecha de modificación, lo que lo hace fiable y reproducible.

```yaml
- uses: actions/cache@v4
  with:
    path: ~/.npm
    key: ${{ runner.os }}-node-${{ hashFiles('**/package-lock.json') }}
    restore-keys: |
      ${{ runner.os }}-node-
```

---

## Tabla de funciones built-in

| Función | Firma | Entrada | Salida | Ejemplo mínimo |
|---|---|---|---|---|
| `contains` | `contains(search, item)` | string o array, any | boolean | `contains('hello world', 'world')` → `true` |
| `startsWith` | `startsWith(str, prefix)` | string, string | boolean | `startsWith('refs/tags/v1', 'refs/tags/')` → `true` |
| `endsWith` | `endsWith(str, suffix)` | string, string | boolean | `endsWith('build.yml', '.yml')` → `true` |
| `format` | `format(template, v0, v1…)` | string, any… | string | `format('Run {0}', github.run_number)` |
| `join` | `join(array, sep?)` | array, string? | string | `join(matrix.os, ' | ')` |
| `toJSON` | `toJSON(value)` | any | string | `toJSON(github.event.pull_request)` |
| `fromJSON` | `fromJSON(value)` | string | any | `fromJSON(steps.gen.outputs.matrix)` |
| `hashFiles` | `hashFiles(pattern, …)` | string… | string | `hashFiles('**/package-lock.json')` |
| `always` | `always()` | — | boolean | `if: always()` |
| `success` | `success()` | — | boolean | `if: success()` |
| `failure` | `failure()` | — | boolean | `if: failure()` |
| `cancelled` | `cancelled()` | — | boolean | `if: cancelled()` |

---

## Ejemplo central: workflow con 6 funciones en contexto real

El siguiente workflow de CI/CD combina seis de las funciones built-in en situaciones que representan patrones reales de producción: generación de matriz dinámica, construcción de claves de caché, filtrado por etiquetas, mensajes formateados y serialización de datos.

```yaml
name: CI Pipeline con expresiones

on:
  pull_request:
    branches: [main, develop]
  push:
    branches: [main]

jobs:
  # Job 1: genera la matriz de sistemas operativos de forma dinámica
  setup:
    runs-on: ubuntu-latest
    outputs:
      matrix: ${{ steps.define-matrix.outputs.matrix }}
      cache-key: ${{ steps.cache-key.outputs.key }}
    steps:
      - uses: actions/checkout@v4

      # Genera la lista de OS según si es PR con etiqueta 'full-test' o no
      - name: Definir matriz de OS
        id: define-matrix
        run: |
          if [[ "${{ contains(github.event.pull_request.labels.*.name, 'full-test') }}" == "true" ]]; then
            echo 'matrix=["ubuntu-latest","windows-latest","macos-latest"]' >> $GITHUB_OUTPUT
          else
            echo 'matrix=["ubuntu-latest"]' >> $GITHUB_OUTPUT
          fi

      # Construye la clave de caché usando hashFiles
      - name: Calcular clave de caché
        id: cache-key
        run: |
          echo "key=${{ runner.os }}-deps-${{ hashFiles('**/package-lock.json', '**/yarn.lock') }}" >> $GITHUB_OUTPUT

  # Job 2: build en cada OS de la matriz
  build:
    needs: setup
    runs-on: ${{ matrix.os }}
    strategy:
      matrix:
        # fromJSON parsea el array generado dinámicamente en el job anterior
        os: ${{ fromJSON(needs.setup.outputs.matrix) }}
    steps:
      - uses: actions/checkout@v4

      - name: Restaurar caché de dependencias
        uses: actions/cache@v4
        with:
          path: ~/.npm
          key: ${{ needs.setup.outputs.cache-key }}
          restore-keys: |
            ${{ runner.os }}-deps-

      - name: Instalar dependencias
        run: npm ci

      - name: Build
        run: npm run build

      # Construye un mensaje de resumen con format
      - name: Resumen del build
        if: always()
        run: |
          echo "${{ format('Build {0} en {1} — rama: {2} — actor: {3}', github.run_number, matrix.os, github.ref_name, github.actor) }}"

  # Job 3: notificación final sólo en push a main
  notify:
    needs: build
    runs-on: ubuntu-latest
    if: ${{ github.event_name == 'push' && startsWith(github.ref, 'refs/heads/main') }}
    steps:
      # Serializa el evento para logging/auditoría
      - name: Log del evento
        run: |
          echo "Evento completo:"
          echo '${{ toJSON(github.event.head_commit) }}'

      # Une los OS de la matriz en un string legible para el mensaje
      - name: Mensaje de despliegue
        run: |
          echo "Build exitoso. OS probados: ${{ join(fromJSON(needs.setup.outputs.matrix), ' | ') }}"
```

Este ejemplo muestra: `contains` para filtrar por etiqueta, `hashFiles` para la clave de caché, `fromJSON` para construir la matriz, `format` para el mensaje de resumen, `startsWith` para filtrar la rama, `toJSON` para el log del evento, y `join` para listar los OS en el mensaje final.

---

## Mitigación de script injection

Cuando un contexto como `github.event.pull_request.title` (controlado por el usuario externo) se interpola directamente dentro de un bloque `run:`, se abre una vulnerabilidad de **script injection**: un atacante puede inyectar comandos arbitrarios en el shell. La mitigación correcta es asignar el valor del contexto a una variable de entorno del step y leerla desde el shell — nunca interpolarla con `${{ }}` dentro del script. Este patrón se cubre en profundidad en el tema de Seguridad Avanzada (D5).

```yaml
# INSEGURO — script injection posible
- run: echo "Title: ${{ github.event.pull_request.title }}"

# SEGURO — valor asignado a env var, nunca interpolado en el script
- env:
    PR_TITLE: ${{ github.event.pull_request.title }}
  run: echo "Title: $PR_TITLE"
```

---

## Buenas y malas practicas

**Par 1 — Condiciones legibles vs. scripting innecesario**

Buena practica: usar expresiones built-in directamente en `if` para condiciones simples.
```yaml
# Correcto: expresivo, evaluado antes de entrar al shell
if: ${{ startsWith(github.ref, 'refs/tags/') && github.event_name == 'push' }}
```

Mala practica: delegar la condición al shell cuando la expresión de GitHub la resuelve de forma más eficiente y sin levantar el proceso.
```yaml
# Evitar: el step se ejecuta siempre, la lógica está oculta en el script
- run: |
    if [[ "$GITHUB_REF" == refs/tags/* ]]; then
      echo "Es una etiqueta"
    fi
```

**Par 2 — Claves de caché deterministas vs. claves fijas**

Buena practica: incluir `hashFiles` en la clave para que la caché se invalide automáticamente cuando cambian los ficheros de bloqueo.
```yaml
key: ${{ runner.os }}-node-${{ hashFiles('**/package-lock.json') }}
```

Mala practica: usar una clave fija que nunca cambia, lo que provoca que se restaure una caché obsoleta aunque las dependencias hayan cambiado.
```yaml
# Evitar: la caché nunca se invalida correctamente
key: node-modules-cache
```

**Par 3 — Script injection: variable de entorno vs. interpolación directa**

Buena practica: asignar valores de contexto controlados por el usuario a variables de entorno del step.
```yaml
- env:
    TITLE: ${{ github.event.pull_request.title }}
  run: echo "PR title: $TITLE"
```

Mala practica: interpolar directamente el valor de un contexto externo dentro del bloque `run`, lo que permite inyección de comandos de shell.
```yaml
# PELIGROSO: si el título contiene `; rm -rf /`, se ejecuta
- run: echo "PR title: ${{ github.event.pull_request.title }}"
```

**Par 4 — fromJSON para matrices dinamicas vs. matrices hardcodeadas**

Buena practica: generar la matriz en un step previo con `fromJSON` para adaptar las variantes según el contexto (rama, etiquetas, cambios en ficheros).
```yaml
os: ${{ fromJSON(needs.setup.outputs.matrix) }}
```

Mala practica: hardcodear la matriz en el YAML, lo que obliga a editar el fichero del workflow cada vez que se añade o elimina un entorno.
```yaml
# Evitar cuando la lista debe ser dinámica
matrix:
  os: [ubuntu-latest, windows-latest, macos-latest]
```

---

## Verificacion GH-200

**Pregunta 1.** ¿Cuál de las siguientes expresiones devuelve `true` si el evento es un `push` a una rama cuyo nombre empieza por `release/`?

- A) `github.event_name == 'push' && contains(github.ref, 'release/')`
- B) `github.event_name == 'push' && startsWith(github.ref_name, 'release/')`
- C) `github.event_name == 'push' && endsWith(github.ref, '/release')`
- D) `startsWith(github.ref_name, 'release/') || github.event_name == 'push'`

**Respuesta correcta: B.** `startsWith(github.ref_name, 'release/')` comprueba el prefijo exacto del nombre corto de la rama. La opción A usa `contains`, que también devolvería `true` para ramas como `hotfix/release-fix`, lo que es menos preciso.

---

**Pregunta 2.** Un workflow necesita una clave de caché que se invalide exactamente cuando cambia `package-lock.json` pero que no dependa del SO para compartir caché entre runners Linux. ¿Qué expresión es la más adecuada?

- A) `${{ hashFiles('package-lock.json') }}`
- B) `${{ runner.os }}-${{ hashFiles('**/package-lock.json') }}`
- C) `node-${{ hashFiles('**/package-lock.json') }}`
- D) `${{ github.sha }}-node`

**Respuesta correcta: C.** Omite `runner.os` para compartir la caché entre runners Linux y Mac si el contenido es el mismo. La opción D usa el SHA del commit, que cambia en cada push aunque las dependencias no hayan variado.

---

**Pregunta 3.** ¿Cuál es el resultado de la expresión `format('Deploy {0} en {0}/{1}', 'main', '42')`?

- A) `Deploy main en {0}/42`
- B) `Deploy main en main/42`
- C) `Deploy {0} en main/42`
- D) Error de sintaxis

**Respuesta correcta: B.** El marcador `{0}` puede repetirse y siempre se sustituye por el primer argumento extra (`'main'`). El marcador `{1}` se sustituye por `'42'`.

---

**Ejercicio practico**

Tienes un workflow de PR que recibe etiquetas como `deploy:staging`, `deploy:prod` o `skip-deploy`. Escribe una condición `if` que active el step de despliegue solo cuando:
1. El evento es `pull_request`.
2. Alguna etiqueta del PR empieza por `deploy:`.
3. No existe la etiqueta `skip-deploy`.

Pistas: usa `join` + `contains` + `startsWith`, o combina `contains` con la lista de etiquetas directamente. Comprueba que tu expresión es insensible a mayúsculas en el prefijo.

<details>
<summary>Solución</summary>

```yaml
if: >
  ${{
    github.event_name == 'pull_request' &&
    contains(join(github.event.pull_request.labels.*.name, ','), 'deploy:') &&
    !contains(github.event.pull_request.labels.*.name, 'skip-deploy')
  }}
```

`join` convierte el array de etiquetas en una cadena separada por comas; `contains` sobre esa cadena busca la subcadena `deploy:` de forma case-insensitive. El segundo `contains` opera sobre el array original para comprobar la etiqueta exacta `skip-deploy`.
</details>

---

[← 1.12 Contextos de estado, datos y flujo](gha-d1-contextos-estado.md) | [1.14 Caché de dependencias →](gha-d1-cache.md)
