# 🧮 GitHub Actions: Expresiones y Funciones

## 📚 Índice
1. [Sintaxis de Expresiones](#sintaxis-de-expresiones)
2. [Operadores](#operadores)
3. [Funciones de Estado](#funciones-de-estado)
4. [Funciones de String](#funciones-de-string)
5. [Funciones de Array](#funciones-de-array)
6. [Funciones de Objeto](#funciones-de-objeto)
7. [Funciones de Hash](#funciones-de-hash)
8. [Funciones de Path](#funciones-de-path)
9. [Expresiones Avanzadas](#expresiones-avanzadas)

---

## 💬 Sintaxis de Expresiones

Las **expresiones** son fragmentos de código evaluados por los servidores de GitHub **antes** de que el job llegue al runner. Cuando GitHub procesa el archivo YAML del workflow, sustituye cada `${{ }}` por su valor calculado. Esto significa que el runner nunca ve la expresión en sí — ya recibe el valor resuelto.

Se pueden usar en casi cualquier campo del workflow: `if:`, `run:`, `env:`, `with:`, `name:`, etc. Combinan acceso a **contextos** (objetos con datos del evento, el job, el runner…) con **funciones** y **operadores**.

### Uso básico

Las expresiones se escriben con `${{ }}`:

```yaml
# En run
- run: echo "${{ github.repository }}"

# En if
- if: ${{ github.ref == 'refs/heads/main' }}

# En env
env:
  MY_VAR: ${{ secrets.API_KEY }}

# En with
- uses: actions/checkout@v4
  with:
    ref: ${{ github.head_ref }}
```

### Contextos disponibles

```yaml
${{ github }}          # Contexto de GitHub
${{ env }}             # Variables de entorno
${{ job }}             # Contexto del job
${{ steps }}           # Steps anteriores
${{ runner }}          # Información del runner
${{ secrets }}         # Secretos
${{ vars }}            # Variables de configuración
${{ strategy }}        # Estrategia de matriz
${{ matrix }}          # Valores de matriz
${{ needs }}           # Jobs dependientes
${{ inputs }}          # Inputs del workflow
```

---

## 🔣 Operadores

### Operadores de Comparación

```yaml
# Igualdad
if: github.ref == 'refs/heads/main'

# Desigualdad
if: github.event_name != 'schedule'

# Mayor que
if: github.run_number > 100

# Menor que
if: github.run_attempt < 3

# Mayor o igual
if: github.run_number >= 1

# Menor o igual
if: github.run_attempt <= 3
```

### Operadores Lógicos

```yaml
# AND (&&)
if: github.event_name == 'push' && github.ref == 'refs/heads/main'

# OR (||)
if: github.event_name == 'push' || github.event_name == 'pull_request'

# NOT (!)
if: "!cancelled()"

# Agrupación con paréntesis
if: (github.event_name == 'push' && github.ref == 'refs/heads/main') || github.event_name == 'workflow_dispatch'
```

### Nota sobre Strings

⚠️ En `if`, las expresiones con `!` al inicio deben ir entre comillas:

```yaml
# ✅ Correcto
if: "!cancelled()"

# ❌ Incorrecto (error de sintaxis YAML)
if: !cancelled()
```

---

## ✅ Funciones de Estado

### `success()`

Retorna `true` si ningún paso anterior falló o fue cancelado.

```yaml
- name: Siempre después de éxito
  if: success()
  run: echo "Todo fue bien"
```

**Por defecto**, si no especificas `if`, se asume `if: success()`.

### `always()`

Siempre se ejecuta, incluso si hay fallos o cancelaciones.

```yaml
- name: Cleanup
  if: always()
  run: rm -rf temp/
```

### `failure()`

Se ejecuta solo si un paso anterior falló.

```yaml
- name: Notificar fallo
  if: failure()
  run: |
    curl -X POST https://hooks.slack.com/... \
      -d '{"text":"❌ Build falló"}'
```

### `cancelled()`

Se ejecuta solo si el workflow fue cancelado.

```yaml
- name: Cleanup por cancelación
  if: cancelled()
  run: echo "Workflow cancelado"
```

### Combinaciones

```yaml
# Ejecutar en éxito o fallo (no en cancelación)
- if: success() || failure()
  run: echo "Workflow terminó"

# Ejecutar solo si no fue cancelado
- if: "!cancelled()"
  run: echo "No fue cancelado"

# Ejecutar si un step específico falló
- if: steps.test.outcome == 'failure'
  run: echo "Los tests fallaron"
```

> **`outcome` vs `conclusion` en un step**: cuando un step tiene `continue-on-error: true` y falla, el workflow lo "perdona" y sigue adelante. `outcome` refleja lo que realmente pasó (`failure`), mientras que `conclusion` refleja lo que el workflow considera que pasó (`success`). Usa `outcome` cuando necesites saber el resultado real; usa `conclusion` para saber si el workflow continuó o no.

---

## 📝 Funciones de String

### `contains(search, item)`

Verifica si un string contiene otro.

```yaml
# Verificar en string
- if: contains(github.ref, 'feature')
  run: echo "Es una rama feature"

# Verificar en array
- if: contains(github.event.pull_request.labels.*.name, 'bug')
  run: echo "PR tiene label 'bug'"

# Case sensitive
- if: contains(github.event.head_commit.message, 'BREAKING CHANGE')
  run: echo "Cambio que rompe compatibilidad"
```

### `startsWith(searchString, searchValue)`

Verifica si un string empieza con un valor.

```yaml
# Verificar prefijo de rama
- if: startsWith(github.ref, 'refs/heads/feature/')
  run: echo "Rama feature"

# Verificar título de PR
- if: startsWith(github.event.pull_request.title, 'feat:')
  run: echo "Feature PR"

# Verificar tag
- if: startsWith(github.ref, 'refs/tags/v')
  run: echo "Tag de versión"
```

### `endsWith(searchString, searchValue)`

Verifica si un string termina con un valor.

```yaml
# Verificar sufijo
- if: endsWith(github.ref_name, '-dev')
  run: echo "Rama de desarrollo"

# Verificar extensión de archivo
- if: endsWith(matrix.file, '.js')
  run: npm test ${{ matrix.file }}
```

### `format(string, replaceValue0, replaceValue1, ...)`

Formatea un string con marcadores `{N}`.

```yaml
- name: Mensaje formateado
  run: |
    MESSAGE="${{ format('Hola {0}, tu PR #{1} está listo', github.actor, github.event.pull_request.number) }}"
    echo "$MESSAGE"

# Output: Hola octocat, tu PR #42 está listo

- name: URL formateada
  env:
    URL: ${{ format('https://api.example.com/{0}/{1}', github.repository_owner, github.event.repository.name) }}
  run: curl "$URL"
```

### `join(array, separator)`

Une elementos de un array en un string.

```yaml
- name: Listar labels
  run: |
    LABELS="${{ join(github.event.pull_request.labels.*.name, ', ') }}"
    echo "Labels: $LABELS"
    # Output: Labels: bug, enhancement, documentation

- name: Listar ramas
  run: echo "${{ join(github.event.pull_request.*.ref, ' | ') }}"
```

---

## 📋 Funciones de Array

### `toJSON(value)`

Convierte un valor a JSON.

```yaml
- name: Ver evento completo
  env:
    EVENT: ${{ toJSON(github.event) }}
  run: echo "$EVENT"

- name: Ver labels como JSON
  run: echo '${{ toJSON(github.event.pull_request.labels) }}'

- name: Pretty print
  shell: python
  run: |
    import json
    event = json.loads('''${{ toJSON(github.event) }}''')
    print(json.dumps(event, indent=2))
```

### `fromJSON(value)`

Parsea un string JSON a objeto.

```yaml
- name: Usar JSON
  run: |
    KEY='${{ fromJSON('{"api": "value", "env": "prod"}').api }}'
    echo "API: $KEY"

- name: Parsear configuración
  env:
    CONFIG: '{"debug": true, "port": 3000}'
  run: |
    DEBUG='${{ fromJSON(env.CONFIG).debug }}'
    PORT='${{ fromJSON(env.CONFIG).port }}'
    echo "Debug: $DEBUG, Port: $PORT"
```

### Acceso a arrays

```yaml
# Primer elemento
${{ github.event.commits[0].message }}

# Propiedad de todos los elementos (splat)
${{ github.event.pull_request.labels.*.name }}

# Longitud
${{ github.event.commits.length }}
```

---

## 🗂️ Funciones de Objeto

### Acceso a propiedades

```yaml
# Notación punto
${{ github.event.pull_request.number }}

# Notación corchetes (para nombres con caracteres especiales)
${{ github.event['pull_request']['user']['login'] }}

# Encadenamiento
${{ github.event.pull_request.head.repo.full_name }}
```

### Verificar existencia

```yaml
# Verificar si una propiedad existe
- if: github.event.pull_request
  run: echo "Es un evento de PR"

# Usar con default
- run: echo "${{ github.event.pull_request.title || 'Sin título' }}"
```

---

## #️⃣ Funciones de Hash

### `hashFiles(path, ...)`

Calcula una huella digital (hash SHA-256) del **contenido** de uno o varios archivos. El resultado es siempre el mismo para el mismo contenido, y cambia si cambia cualquier byte del archivo.

Su uso principal es generar **claves de caché que se invalidan automáticamente** cuando cambian las dependencias. Por ejemplo: si `package-lock.json` no ha cambiado entre dos ejecuciones, el hash es idéntico → se reutiliza la caché. Si se añade o actualiza un paquete, el hash cambia → se descarta la caché y se reinstalan las dependencias.

```yaml
# Hash de un archivo
- uses: actions/cache@v4
  with:
    path: ~/.cache/pip
    key: ${{ runner.os }}-pip-${{ hashFiles('requirements.txt') }}

# Hash de múltiples archivos
- uses: actions/cache@v4
  with:
    path: node_modules
    key: ${{ runner.os }}-node-${{ hashFiles('package-lock.json', 'yarn.lock') }}

# Hash con wildcards
- uses: actions/cache@v4
  with:
    path: ~/.m2/repository
    key: ${{ runner.os }}-maven-${{ hashFiles('**/pom.xml') }}

# Hash de múltiples directorios
- uses: actions/cache@v4
  with:
    path: ~/.cache
    key: ${{ runner.os }}-cache-${{ hashFiles('**/*.lock', '**/*.sum') }}
```

**Características:**
- Retorna una cadena hexadecimal de 64 caracteres
- Retorna string vacío si no encuentra archivos
- Soporta wildcards (`*`, `**`)
- Calcula hash de contenido, no de nombres

---

## 📁 Funciones de Path

### Patterns en paths

```yaml
# Todos los archivos en un directorio
'src/**'

# Archivos con extensión específica
'**.js'
'**/*.py'

# Múltiples extensiones
'**/*.{js,ts,jsx,tsx}'

# Excluir archivos
'!**.md'
'!**/test/**'

# Directorios específicos
'src/**/*.js'
'packages/*/src/**'
```

### Ejemplos en triggers

```yaml
on:
  push:
    paths:
      - 'src/**'              # Cualquier archivo en src/
      - '**.js'               # Cualquier archivo .js
      - 'config/*.json'       # JSON en config/
      - '!**.md'              # Excluir Markdown
      - '!**/test/**'         # Excluir directorio test
```

---

## 🧪 Expresiones Avanzadas

### Expresiones complejas en `if`

```yaml
# Múltiples condiciones
- if: |
    github.event_name == 'push' &&
    github.ref == 'refs/heads/main' &&
    !contains(github.event.head_commit.message, '[skip ci]')
  run: echo "Deploy a producción"

# Con funciones anidadas
- if: |
    (github.event_name == 'pull_request' && 
     github.event.pull_request.draft == false &&
     contains(github.event.pull_request.labels.*.name, 'deploy')) ||
    github.event_name == 'workflow_dispatch'
  run: echo "Desplegar"

# Verificar múltiples ramas
- if: |
    startsWith(github.ref, 'refs/heads/feature/') ||
    startsWith(github.ref, 'refs/heads/bugfix/') ||
    github.ref == 'refs/heads/develop'
  run: echo "Rama de desarrollo"
```

### Valores por defecto

```yaml
# Usar || para valores por defecto
env:
  ENVIRONMENT: ${{ github.event.inputs.environment || 'development' }}
  VERSION: ${{ github.event.release.tag_name || 'latest' }}
  BRANCH: ${{ github.head_ref || github.ref_name }}
```

### Expresiones en outputs

```yaml
- name: Calcular versión
  id: version
  run: |
    VERSION="${{ github.ref_name }}"
    # Remover 'v' del inicio si existe
    VERSION="${VERSION#v}"
    echo "version=$VERSION" >> $GITHUB_OUTPUT

- name: Determinar ambiente
  id: env
  run: |
    if [[ "${{ github.ref }}" == "refs/heads/main" ]]; then
      echo "environment=production" >> $GITHUB_OUTPUT
    elif [[ "${{ github.ref }}" == "refs/heads/develop" ]]; then
      echo "environment=staging" >> $GITHUB_OUTPUT
    else
      echo "environment=development" >> $GITHUB_OUTPUT
    fi

- name: Usar outputs
  run: |
    echo "Versión: ${{ steps.version.outputs.version }}"
    echo "Ambiente: ${{ steps.env.outputs.environment }}"
```

### Matrices dinámicas

```yaml
jobs:
  setup:
    runs-on: ubuntu-latest
    outputs:
      matrix: ${{ steps.set-matrix.outputs.matrix }}
    steps:
      - id: set-matrix
        shell: python
        run: |
          import json
          import os
          
          # Determinar matriz según el evento
          if "${{ github.event_name }}" == "push":
              matrix = ["3.9", "3.10", "3.11"]
          else:
              matrix = ["3.11"]  # Solo última versión en PRs
          
          output = json.dumps({"python-version": matrix})
          with open(os.getenv('GITHUB_OUTPUT'), 'a') as f:
              f.write(f'matrix={output}\n')
  
  test:
    needs: setup
    runs-on: ubuntu-latest
    strategy:
      matrix: ${{ fromJSON(needs.setup.outputs.matrix) }}
    steps:
      - run: python${{ matrix.python-version }} --version
```

### Ternario (usando &&/||)

```yaml
# Simular ternario con &&/||
env:
  LABEL: ${{ github.event_name == 'pull_request' && github.event.pull_request.number || 'main' }}
  # Si es PR: usa número, sino: usa 'main'

# Múltiples niveles
env:
  ENVIRONMENT: ${{ 
    github.ref == 'refs/heads/main' && 'production' ||
    github.ref == 'refs/heads/develop' && 'staging' ||
    'development' }}
```

### Verificar tipos

```yaml
# Verificar si es PR (no push)
- if: github.event.pull_request
  run: echo "Es un Pull Request"

# Verificar si existe un valor
- if: github.event.inputs.version
  run: echo "Versión especificada: ${{ github.event.inputs.version }}"

# Verificar array vacío
- if: github.event.pull_request.labels.length > 0
  run: echo "El PR tiene labels"
```

---

## 📊 Tabla de Referencia Rápida

### Funciones de Estado

| Función | Descripción | Uso común |
|---------|-------------|-----------|
| `success()` | Steps anteriores exitosos | Condicional por defecto |
| `always()` | Siempre ejecutar | Cleanup, notificaciones |
| `failure()` | Algo falló | Manejo de errores |
| `cancelled()` | Workflow cancelado | Cleanup de recursos |

### Funciones de String

| Función | Descripción | Ejemplo |
|---------|-------------|---------|
| `contains()` | Contiene substring | `contains(github.ref, 'feature')` |
| `startsWith()` | Empieza con | `startsWith(github.ref, 'refs/tags/')` |
| `endsWith()` | Termina con | `endsWith(github.ref_name, '-dev')` |
| `format()` | Formatear string | `format('v{0}.{1}', 1, 2)` |
| `join()` | Unir array | `join(array, ', ')` |

### Funciones de Objeto

| Función | Descripción | Ejemplo |
|---------|-------------|---------|
| `toJSON()` | Convertir a JSON | `toJSON(github.event)` |
| `fromJSON()` | Parsear JSON | `fromJSON('{"key": "val"}')` |
| `hashFiles()` | Hash de archivos | `hashFiles('**/package-lock.json')` |

### Operadores

| Operador | Descripción | Ejemplo |
|----------|-------------|---------|
| `==` | Igualdad | `github.ref == 'refs/heads/main'` |
| `!=` | Desigualdad | `github.event_name != 'schedule'` |
| `<`, `>` | Comparación | `github.run_number > 100` |
| `&&` | AND lógico | `a && b` |
| `||` | OR lógico / default | `a || 'default'` |
| `!` | NOT lógico | `"!cancelled()"` |

---

## 💡 Mejores Prácticas

### 1. Usar comillas para expresiones con `!`

```yaml
# ✅ Correcto
if: "!cancelled()"

# ❌ Incorrecto
if: !cancelled()
```

### 2. Valores por defecto

```yaml
# ✅ Bueno
env:
  NODE_ENV: ${{ github.event.inputs.environment || 'development' }}

# ❌ Puede fallar si no existe
env:
  NODE_ENV: ${{ github.event.inputs.environment }}
```

### 3. Expresiones multi-línea

```yaml
# ✅ Legible
- if: |
    github.event_name == 'push' &&
    github.ref == 'refs/heads/main' &&
    !contains(github.event.head_commit.message, '[skip ci]')

# ❌ Difícil de leer
- if: github.event_name == 'push' && github.ref == 'refs/heads/main' && !contains(github.event.head_commit.message, '[skip ci]')
```

### 4. Verificar existencia antes de acceder

```yaml
# ✅ Seguro
- if: github.event.pull_request && github.event.pull_request.draft == false

# ❌ Puede fallar en eventos que no son PR
- if: github.event.pull_request.draft == false
```

---

*Documentación completa de expresiones y funciones en GitHub Actions*
*Última actualización: Enero 2026*

