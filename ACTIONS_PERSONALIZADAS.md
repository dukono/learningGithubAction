# 🚀 GitHub Actions: Actions Personalizadas

## 📚 Índice
1. [Tipos de Actions](#1-tipos-de-actions)
2. [Composite Actions](#2-composite-actions)
3. [JavaScript Actions](#3-javascript-actions)
4. [Docker Actions](#4-docker-actions)
5. [Cuándo Usar Cada Tipo](#5-cuándo-usar-cada-tipo)
6. [Publicar en el Marketplace](#6-publicar-en-el-marketplace)
7. [Versionado Semántico de Actions](#7-versionado-semántico-de-actions)
8. [Testing de JavaScript Actions](#8-testing-de-javascript-actions)
9. [Preguntas de Examen](#9-preguntas-de-examen)

---

## 1. Tipos de Actions

Una **action** es una unidad reutilizable de código que se puede usar en steps con `uses:`. Cuando escribes `uses: actions/checkout@v4` en un step, estás usando una action publicada en GitHub.

Las actions encapsulan lógica repetible —instalar dependencias, autenticarse en un cloud, publicar artefactos— detrás de una interfaz clara de `inputs` y `outputs` declarada en un archivo `action.yml`. En lugar de copiar los mismos 20 pasos en 5 workflows distintos, creas una action y la llamas con `uses:` en cada uno.

La diferencia con un **workflow reutilizable** es el nivel de operación: una action trabaja a nivel de **step** (se usa dentro de un job), mientras que un workflow reutilizable trabaja a nivel de **job** completo.

| Tipo | Tecnología | Velocidad | Dónde corre | Cuándo usar |
|---|---|---|---|---|
| **Composite** | Shell/YAML | Rápido | En el runner directamente | Combinar steps existentes |
| **JavaScript** | Node.js | Rápido | En el runner directamente | Lógica compleja, API calls |
| **Docker** | Cualquier lenguaje | Más lento (startup) | En un container | Dependencias específicas, Python, Ruby... |

### Estructura de un repositorio de action

```
mi-action/
├── action.yml          ← OBLIGATORIO: metadata de la action
├── README.md
│
├── (Composite) Los pasos están en action.yml
│
├── (JavaScript)
│   ├── src/
│   │   └── index.js    ← Código fuente
│   ├── dist/
│   │   └── index.js    ← Código compilado (lo que realmente se ejecuta)
│   └── package.json
│
└── (Docker)
    ├── Dockerfile
    └── entrypoint.sh   ← Script que ejecuta Docker
```

---

## 2. Composite Actions

Las composite actions combinan múltiples steps en una sola action reutilizable. **No necesitan compilación** y son las más fáciles de crear.

### Estructura de action.yml

```yaml
# action.yml (en la raíz del repo o en cualquier directorio)
name: 'Setup Node y Cache'
description: 'Instala Node.js con cache de npm optimizado'
author: 'Mi Organización'

# Parámetros de entrada
inputs:
  node-version:
    description: 'Versión de Node.js a instalar'
    required: false
    default: '20'
  working-directory:
    description: 'Directorio de trabajo'
    required: false
    default: '.'

# Lo que devuelve la action
outputs:
  cache-hit:
    description: 'Si se restauró la caché'
    value: ${{ steps.cache.outputs.cache-hit }}

# Tipo composite
runs:
  using: 'composite'                # ← Define que es composite
  steps:
    - name: Setup Node
      uses: actions/setup-node@v4
      with:
        node-version: ${{ inputs.node-version }}
    
    - name: Cache node_modules
      id: cache
      uses: actions/cache@v4
      with:
        path: ${{ inputs.working-directory }}/node_modules
        key: ${{ runner.os }}-node-${{ inputs.node-version }}-${{ hashFiles('**/package-lock.json') }}
        restore-keys: |
          ${{ runner.os }}-node-${{ inputs.node-version }}-
    
    - name: Install dependencies
      if: steps.cache.outputs.cache-hit != 'true'
      shell: bash                   # ← OBLIGATORIO en composite para run:
      working-directory: ${{ inputs.working-directory }}
      run: npm ci
```

> ⚠️ **Regla importante**: En composite actions, cada step con `run:` **DEBE** especificar `shell:`. No se hereda automáticamente.

### Uso de la composite action

```yaml
# En un workflow del mismo repositorio:
steps:
  - uses: actions/checkout@v4
  - uses: ./.github/actions/setup-node-cache   # ← Ruta relativa
    with:
      node-version: '20'
  - run: npm test

# Desde otro repositorio:
steps:
  - uses: mi-org/mi-repo-de-actions/setup-node-cache@v1
    with:
      node-version: '20'
```

### Composite action con lógica compleja

```yaml
name: 'Deploy Validator'
description: 'Valida y prepara un deployment'

inputs:
  environment:
    description: 'Entorno destino'
    required: true
  version:
    description: 'Versión a desplegar'
    required: true

outputs:
  is-valid:
    description: 'Si el deploy es válido'
    value: ${{ steps.validate.outputs.valid }}
  deploy-message:
    description: 'Mensaje del proceso'
    value: ${{ steps.validate.outputs.message }}

runs:
  using: composite
  steps:
    - name: Verificar formato de versión
      id: validate
      shell: bash
      run: |
        VERSION="${{ inputs.version }}"
        ENV="${{ inputs.environment }}"
        
        # Validar formato semver
        if [[ "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
          VALID=true
          MSG="✅ Versión $VERSION válida para $ENV"
        else
          VALID=false
          MSG="❌ Versión $VERSION inválida (esperado: X.Y.Z)"
        fi
        
        echo "valid=$VALID" >> $GITHUB_OUTPUT
        echo "message=$MSG" >> $GITHUB_OUTPUT
    
    - name: Fallar si no es válido
      if: steps.validate.outputs.valid != 'true'
      shell: bash
      run: |
        echo "${{ steps.validate.outputs.message }}"
        exit 1
```

### Composite action en subdirectorio

Puedes tener múltiples actions en un repo:

```
.github/
  actions/
    setup-env/
      action.yml      ← uses: ./.github/actions/setup-env
    run-tests/
      action.yml      ← uses: ./.github/actions/run-tests
    deploy/
      action.yml      ← uses: ./.github/actions/deploy
```

---

## 3. JavaScript Actions

Corren directamente en el runner usando Node.js. Más potentes que composite pero requieren más setup.

### Estructura

```
mi-js-action/
├── action.yml
├── package.json
├── src/
│   └── index.js      ← Código fuente (con ES modules si quieres)
└── dist/
    └── index.js      ← Bundle compilado (ESTE es el que GitHub ejecuta)
```

### action.yml para JavaScript action

```yaml
name: 'Mi JavaScript Action'
description: 'Una action que hace algo con Node.js'

inputs:
  who-to-greet:
    description: 'A quién saludar'
    required: true
    default: 'World'

outputs:
  random-number:
    description: 'Un número aleatorio'

runs:
  using: 'node20'              # ← node20 o node16
  main: 'dist/index.js'        # ← Archivo a ejecutar
  pre: 'dist/cleanup.js'       # ← Opcional: se ejecuta ANTES del job
  post: 'dist/cleanup.js'      # ← Opcional: se ejecuta DESPUÉS del job
```

### src/index.js

```javascript
const core = require('@actions/core');
const github = require('@actions/github');

async function run() {
  try {
    // Leer inputs
    const whoToGreet = core.getInput('who-to-greet');
    
    // Hacer algo
    console.log(`Hello ${whoToGreet}!`);
    
    // Acceder al contexto de GitHub
    const context = github.context;
    console.log(`Repositorio: ${context.repo.owner}/${context.repo.repo}`);
    console.log(`Evento: ${context.eventName}`);
    
    // Generar un output
    const randomNumber = Math.floor(Math.random() * 100);
    core.setOutput('random-number', randomNumber.toString());
    
    // Añadir al resumen
    await core.summary
      .addHeading('Resultado')
      .addTable([
        [{data: 'Campo', header: true}, {data: 'Valor', header: true}],
        ['Saludo a', whoToGreet],
        ['Número', randomNumber.toString()]
      ])
      .write();
    
  } catch (error) {
    // Fallar la action con mensaje de error
    core.setFailed(error.message);
  }
}

run();
```

### package.json

```json
{
  "name": "mi-js-action",
  "version": "1.0.0",
  "main": "dist/index.js",
  "scripts": {
    "build": "ncc build src/index.js -o dist",
    "test": "jest"
  },
  "dependencies": {
    "@actions/core": "^1.10.0",
    "@actions/github": "^6.0.0"
  },
  "devDependencies": {
    "@vercel/ncc": "^0.38.0",
    "jest": "^29.0.0"
  }
}
```

### Compilar para producción

Cuando GitHub ejecuta una JavaScript action, descarga el repositorio de la action al runner. El runner **no ejecuta `npm install`** automáticamente — solo ejecuta el archivo indicado en `main:` del `action.yml`. Por eso hay que usar `@vercel/ncc`: empaqueta el código fuente y todas sus dependencias de `node_modules` en un **único archivo `dist/index.js`**. Ese archivo es el que el runner puede ejecutar directamente sin necesitar ninguna dependencia instalada.

Por eso el directorio `dist/` debe hacerse **commit** en el repositorio de la action (aunque normalmente se ignora en proyectos normales).

```bash
# Instalar @vercel/ncc para compilar todo en un solo archivo
npm install @vercel/ncc --save-dev

# Compilar (genera dist/index.js con todas las dependencias embebidas)
npx ncc build src/index.js -o dist

# El dist/index.js NO necesita node_modules
# Hacer commit del dist/ es obligatorio
git add dist/
git commit -m "build: compile action"
```

### APIs del toolkit (@actions/core)

```javascript
const core = require('@actions/core');

// Inputs/Outputs
core.getInput('my-input')                     // Leer input
core.getMultilineInput('my-input')            // Input multilínea como array
core.getBooleanInput('my-input')              // Input como boolean
core.setOutput('my-output', 'value')          // Establecer output

// Logging
core.info('Mensaje informativo')              // Log normal
core.warning('Advertencia')                   // Log amarillo
core.error('Error', { file: 'x.js', line: 5 }) // Log rojo
core.debug('Solo visible en modo debug')      // Solo si ACTIONS_STEP_DEBUG=true

// Control de flujo
core.setFailed('Mensaje de error')            // Falla la action
core.isDebug()                                // ¿Está en modo debug?

// Secrets (ocultar de logs)
core.setSecret('mi-valor-secreto')            // Enmascara en logs

// Variables de entorno
core.exportVariable('MI_VAR', 'valor')        // Equivale a GITHUB_ENV
core.addPath('/mi/directorio')                // Equivale a GITHUB_PATH

// Grupos en logs
core.startGroup('Nombre del grupo')
// ... logs aquí ...
core.endGroup()

// Summary
await core.summary.addHeading('Título').addRaw('contenido').write()
```

---

## 4. Docker Actions

La action corre dentro de un contenedor Docker. Permite usar cualquier lenguaje.

**Flujo de ejecución**: cuando el runner encuentra un step con una Docker action, construye la imagen Docker (si `image: 'Dockerfile'`) o la descarga (si es una imagen pública), crea un contenedor, monta el directorio de trabajo del runner dentro del contenedor, y ejecuta el `ENTRYPOINT`. Los inputs del `action.yml` se pasan como argumentos al entrypoint (listados en `args:`). Las variables de entorno de GitHub (`GITHUB_OUTPUT`, `GITHUB_WORKSPACE`, etc.) están disponibles dentro del contenedor porque el runner las inyecta automáticamente.

> ⚠️ Las Docker actions **no funcionan en runners de Windows** de GitHub porque Docker no está disponible en esos runners.

### Estructura

```
mi-docker-action/
├── action.yml
├── Dockerfile
└── entrypoint.sh    ← Script principal
```

### action.yml para Docker action

```yaml
name: 'Mi Docker Action'
description: 'Una action que corre en Docker'

inputs:
  who-to-greet:
    description: 'A quién saludar'
    required: true
    default: 'World'

outputs:
  random-number:
    description: 'Número generado'

runs:
  using: 'docker'
  image: 'Dockerfile'        # ← Construye desde Dockerfile local
  # O imagen pública:
  # image: 'docker://alpine:3.18'
  args:
    - ${{ inputs.who-to-greet }}   # ← Pasa inputs como args al entrypoint
```

### Dockerfile

```dockerfile
FROM alpine:3.18

RUN apk add --no-cache bash curl jq

COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

ENTRYPOINT ["/entrypoint.sh"]
```

### entrypoint.sh

```bash
#!/bin/bash
set -e

WHO_TO_GREET="$1"   # Primer argumento = primer input

echo "Hello $WHO_TO_GREET!"

# Acceder a variables de entorno de GitHub
echo "Repositorio: $GITHUB_REPOSITORY"
echo "SHA: $GITHUB_SHA"

# Generar output
RANDOM_NUMBER=$((RANDOM % 100))
echo "random-number=$RANDOM_NUMBER" >> $GITHUB_OUTPUT

# Añadir al summary
echo "## Resultado" >> $GITHUB_STEP_SUMMARY
echo "Saludé a: $WHO_TO_GREET" >> $GITHUB_STEP_SUMMARY
```

### Docker action con imagen pública (sin Dockerfile)

```yaml
runs:
  using: 'docker'
  image: 'docker://python:3.12-slim'   # ← Imagen de Docker Hub
  args:
    - python
    - -c
    - "print('Hello from Python in Docker!')"
```

---

## 5. Cuándo Usar Cada Tipo

```
¿Necesitas combinar steps existentes?
  → Composite Action ✅

¿Necesitas lógica compleja con Node.js?
  → JavaScript Action ✅

¿Necesitas Python, Ruby, Go, etc.?
  → Docker Action ✅

¿Velocidad de startup es crítica?
  → Composite o JavaScript (Docker es más lento)

¿El runner puede ser Windows?
  → NO usar Docker Action (Docker no corre en Windows runners de GitHub)

¿Necesitas acceso al sistema de archivos del runner entre steps?
  → Composite o JavaScript (Docker tiene filesystem aislado)
```

### Comparativa detallada

| Característica | Composite | JavaScript | Docker |
|---|---|---|---|
| Lenguajes soportados | Shell | Node.js | Cualquiera |
| Compilación necesaria | ❌ No | ✅ Sí (ncc build) | ❌ No |
| Startup time | Rápido | Rápido | Lento |
| Windows support | ✅ | ✅ | ❌ |
| Dependencias externas | Solo lo del runner | npm packages (bundled) | Cualquier cosa en Docker |
| Complejidad de creación | Baja | Media | Media |
| Acceso a contexto GitHub | Limitado | Completo (@actions/github) | Via env vars |

---

## 6. Publicar en el Marketplace

### Requisitos para publicar

1. La action debe estar en la **raíz del repositorio** (o en una subcarpeta)
2. Tener un `action.yml` válido con `name`, `description`, `runs`
3. El repo debe ser **público**
4. Tener un `README.md` explicando cómo usarla
5. Crear un **release** con tag semver (ej: `v1.0.0`)

### Versionado de actions

```yaml
# Convenio de versionado para actions:
# v1.2.3 → Versión exacta (más seguro)
# v1.2   → Último patch de 1.2
# v1     → Último minor/patch de 1 (más cómodo)

# El mantenedor crea tags:
git tag -a v1.2.3 -m "Release v1.2.3"
git tag -f v1.2 v1.2.3    # Mover tag flotante
git tag -f v1 v1.2.3      # Mover tag flotante
git push --tags --force
```

### action.yml con branding para Marketplace

```yaml
name: 'Deploy to AWS'
description: 'Despliega aplicaciones a AWS con OIDC'
author: 'Mi Empresa'

branding:
  icon: 'upload-cloud'      # ← Icono de Feather Icons
  color: 'orange'           # ← Color del icono

inputs:
  # ...
```

---

## 7. Versionado Semántico de Actions

Cuando publicas una action (o la usas internamente), el **versionado** controla qué código ejecutará el workflow al referenciarla con `uses:`.

### Formas de referenciar una action

```yaml
# 1. Tag semántico mayor (recomendado para usuarios)
- uses: actions/checkout@v4          # → apunta al tag más reciente v4.x.x

# 2. Tag semántico específico (más reproducible)
- uses: actions/checkout@v4.1.1      # → versión exacta

# 3. SHA completo del commit (máxima seguridad)
- uses: actions/checkout@11bd71901bbe5b1630ceea73d27597364c9af683  # v4.2.2

# 4. Rama (no recomendado en producción — mutable)
- uses: actions/checkout@main

# 5. Action local (en el mismo repositorio)
- uses: ./.github/actions/mi-action
```

### Estrategia de versionado para publicadores

La convención estándar del ecosistema GitHub Actions usa **tags móviles** para el major:

```
v1.0.0  ← tag inmutable (SHA concreto)
v1.0.1  ← tag inmutable
v1.1.0  ← tag inmutable
v1      ← tag MÓVIL que siempre apunta al último v1.x.x
```

```bash
# Al publicar v1.2.3, actualizar el tag móvil v1:
git tag -fa v1 -m "Update v1 tag"
git push origin v1 --force
```

**¿Por qué `@v4` y no `@v4.2.1`?**

| Referencia | Ventaja | Desventaja |
|---|---|---|
| `@v4` | Recibe bugfixes automáticamente | Puede romper si hay breaking change |
| `@v4.1.0` | Reproducible, no cambia | No recibe patches de seguridad |
| `@SHA` | Máxima seguridad, inmutable | Difícil de leer, hay que actualizar manualmente |

**Recomendación oficial de GitHub Security:**
> Usar SHA completo para actions de terceros en workflows de producción. Los tags pueden moverse (force push).

### Semver en action.yml

El archivo `action.yml` **no** tiene un campo de versión — la versión la gestionan los tags de git del repositorio de la action.

### Cómo actualizar una action en workflows

```bash
# Usando Dependabot (recomendado para mantener actions actualizadas)
# .github/dependabot.yml
version: 2
updates:
  - package-ecosystem: github-actions
    directory: /
    schedule:
      interval: weekly
```

---

## 8. Testing de JavaScript Actions

Las JavaScript actions se pueden testear localmente antes de publicarlas usando **Jest** y mocks de `@actions/core`.

### Setup del proyecto

```bash
npm install --save-dev jest @jest/globals
npm install @actions/core @actions/github
```

```json
// package.json
{
  "scripts": {
    "test": "jest",
    "build": "npx ncc build src/index.js -o dist"
  },
  "jest": {
    "testEnvironment": "node"
  }
}
```

### Estructura de archivos

```
mi-action/
├── src/
│   └── index.js        ← lógica de la action
├── __tests__/
│   └── index.test.js   ← tests
├── dist/
│   └── index.js        ← compilado (git tracked)
├── action.yml
└── package.json
```

### La action (src/index.js)

```javascript
const core = require('@actions/core');

async function run() {
  try {
    const name = core.getInput('name', { required: true });
    const greeting = `Hello, ${name}!`;
    core.setOutput('greeting', greeting);
    core.info(greeting);
  } catch (error) {
    core.setFailed(error.message);
  }
}

run();
```

### El test (__tests__/index.test.js)

```javascript
const core = require('@actions/core');

// Mock de @actions/core ANTES de importar la action
jest.mock('@actions/core');

describe('greeting action', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test('genera saludo correcto', async () => {
    // Arrange: simular input
    core.getInput.mockReturnValue('GitHub');

    // Act: ejecutar la action
    await require('../src/index');

    // Assert: verificar outputs y logs
    expect(core.setOutput).toHaveBeenCalledWith('greeting', 'Hello, GitHub!');
    expect(core.setFailed).not.toHaveBeenCalled();
  });

  test('falla si name está vacío', async () => {
    core.getInput.mockImplementation(() => {
      throw new Error('Input required: name');
    });

    await require('../src/index');

    expect(core.setFailed).toHaveBeenCalledWith('Input required: name');
  });
});
```

### Variables de entorno para tests

`@actions/core` lee los inputs de variables de entorno con el formato `INPUT_<NOMBRE_EN_MAYÚSCULAS>`:

```javascript
// Alternativa sin mock: usar variables de entorno
process.env['INPUT_NAME'] = 'GitHub';
```

### Workflow de CI para la propia action

```yaml
# .github/workflows/ci.yml en el repo de la action
name: CI

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
      - run: npm ci
      - run: npm test
      - run: npm run build   # verificar que dist/ se compila correctamente

  # Probar la action contra sí misma
  self-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: ./               # ← usar la action del propio repo
        with:
          name: 'World'
```

---

## 9. Preguntas de Examen

**P: ¿Cuáles son los tres tipos de actions personalizadas?**
→ Composite (pasos YAML/shell), JavaScript (Node.js), Docker (cualquier lenguaje en container).

**P: ¿Qué campo OBLIGATORIO deben tener los steps `run:` en una composite action?**
→ `shell:` (ej: `shell: bash`). No se hereda automáticamente.

**P: ¿Qué herramienta se usa para compilar una JavaScript action a un único archivo?**
→ `@vercel/ncc` (`npx ncc build src/index.js -o dist`). El `dist/` generado debe hacerse commit.

**P: ¿Pueden las Docker actions ejecutarse en runners de Windows?**
→ No. Docker no está disponible en los runners de Windows de GitHub.

**P: ¿Dónde debe estar el `action.yml` para una action publicada en el Marketplace?**
→ En la raíz del repositorio.

**P: ¿Cómo lee una JavaScript action sus inputs?**
→ Con `core.getInput('nombre-del-input')` del paquete `@actions/core`.

**P: ¿Qué diferencia hay entre una composite action y un workflow reutilizable?**
→ Composite action opera a nivel de **step** (se usa con `uses:` en un step). Workflow reutilizable opera a nivel de **job** (se usa con `uses:` en un job) y puede tener múltiples jobs con sus propios runners.

**P: ¿Cómo establece outputs una Docker action?**
→ Escribiendo en la variable de entorno `$GITHUB_OUTPUT`:
```bash
echo "nombre-output=valor" >> $GITHUB_OUTPUT
```

**P: ¿Qué diferencia hay entre referenciar una action con `@v4` vs `@v4.1.0` vs `@SHA`?**
→ `@v4` es un tag móvil que el autor puede actualizar para apuntar al último 4.x.x (recibe patches automáticamente pero puede cambiar). `@v4.1.0` es un tag inmutable. `@SHA` es el commit exacto — inmutable y la referencia más segura para producción.

**P: ¿Por qué GitHub recomienda usar SHA completo en lugar de tags para actions de terceros?**
→ Los tags pueden moverse con `git push --force`. Un atacante que comprometa el repositorio de la action podría cambiar el tag para que apunte a código malicioso. El SHA es inmutable.

**P: ¿Cómo se convierte en fixture el input de una JavaScript action en un test con Jest?**
→ Mockeando `@actions/core` con `jest.mock('@actions/core')` y usando `core.getInput.mockReturnValue('valor')`. Alternativamente, seteando la variable de entorno `INPUT_<NOMBRE>`.

**P: ¿Por qué hay que hacer commit del directorio `dist/` en una JavaScript action?**
→ Porque GitHub ejecuta directamente el archivo compilado indicado en `action.yml` (`main: dist/index.js`). No ejecuta `npm install` ni compila en tiempo de ejecución. Si `dist/` no está en el repo, la action falla.

**P: ¿Cómo se puede usar Dependabot para mantener actualizadas las actions de un repo?**
→ Añadiendo en `.github/dependabot.yml` un entry con `package-ecosystem: github-actions`. Dependabot creará PRs automáticos cuando haya nuevas versiones de las actions usadas en los workflows.

