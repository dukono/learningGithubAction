← [README](README.md) | → [1.2 Triggers de código](gha-d1-triggers-codigo.md)

# 1.1 Estructura raíz del workflow y YAML anchors

Cuando un equipo empieza a crecer en GitHub Actions, el primer problema no es la lógica de los jobs sino el propio fichero de workflow: ¿qué propiedades van en la raíz? ¿En qué orden tiene sentido declararlas? ¿Cómo evitar repetir bloques idénticos de configuración en diez workflows distintos? Este documento responde a esas preguntas cubriendo todas las propiedades que GitHub Actions acepta en el nivel raíz de un fichero `.github/workflows/*.yml` y la mecánica de YAML anchors para reutilizar bloques dentro del mismo fichero.

## Árbol de propiedades raíz

Las propiedades que GitHub Actions reconoce en el nivel más alto del fichero son las siguientes. Las marcadas con `*` son obligatorias para que el workflow sea válido.

```
workflow.yml
├── name                   # Nombre visible en la UI de Actions
├── on *                   # Eventos que disparan el workflow
├── env                    # Variables de entorno para todos los jobs
├── defaults               # Shell y working-directory globales
│   └── run
│       ├── shell
│       └── working-directory
├── concurrency            # Control de ejecuciones simultáneas
│   ├── group
│   └── cancel-in-progress
├── permissions            # Permisos del GITHUB_TOKEN para todo el workflow
└── jobs *                 # Definición de los jobs (mínimo uno)
```

## La propiedad `name`

La propiedad `name` asigna el título que aparece en la pestaña Actions del repositorio y en las notificaciones por correo. Es opcional: si se omite, GitHub muestra la ruta relativa del fichero (por ejemplo `CI / .github/workflows/ci.yml`). Conviene ponerla siempre porque facilita identificar el workflow en repositorios con muchos ficheros. El valor es una cadena de texto plano; puede contener expresiones `${{ }}` aunque en la práctica raramente se usan aquí porque el contexto `github` aún no tiene valores de ejecución en el momento en que se muestra el nombre en la lista de workflows.

```yaml
name: Integración continua
```

## La propiedad `on`

La propiedad `on` es la única estrictamente obligatoria junto con `jobs`. Define qué eventos disparan el workflow. Acepta tres formas: un evento simple como cadena, una lista de eventos, o un mapa de eventos con filtros. Los eventos más habituales son `push`, `pull_request`, `workflow_dispatch` y `schedule`. Cada evento puede tener sub-claves propias (`branches`, `paths`, `types`, `cron`, etc.) que se detallan en el nodo 1.2. Aquí solo se muestra la sintaxis estructural para que quede clara su posición en la raíz.

```yaml
on:
  push:
    branches:
      - main
      - "release/**"
  pull_request:
    branches:
      - main
  workflow_dispatch:
```

## La propiedad `env` a nivel de workflow

La propiedad `env` en la raíz declara variables de entorno disponibles en todos los steps de todos los jobs del workflow. Es un mapa clave-valor donde todos los valores se tratan como cadenas. Estas variables pueden ser sobreescritas por `env` a nivel de job o de step: la resolución sigue el orden de precedencia de menor a mayor especificidad (workflow → job → step). Un caso de uso típico es definir la versión de una herramienta en un único lugar para que cualquier job que la necesite la lea del entorno sin duplicar literales.

```yaml
env:
  NODE_VERSION: "20"
  REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository }}
```

## La propiedad `defaults`

La propiedad `defaults` establece valores predeterminados para todos los pasos `run` del workflow. Solo tiene una sub-clave reconocida: `run`, que a su vez acepta `shell` y `working-directory`. Definir `shell: bash` en la raíz garantiza que todos los pasos usen Bash independientemente del sistema operativo del runner, lo que evita sorpresas en runners Windows donde el shell por defecto es `pwsh`. Definir `working-directory` es útil en monorepos donde casi todos los comandos deben ejecutarse desde un subdirectorio concreto. Al igual que `env`, estos valores pueden sobreescribirse a nivel de job o de step individual.

```yaml
defaults:
  run:
    shell: bash
    working-directory: ./app
```

## La propiedad `concurrency`

La propiedad `concurrency` controla cuántas ejecuciones del workflow (o de un grupo de workflows) pueden estar activas simultáneamente. Se define con dos sub-claves: `group` y `cancel-in-progress`. El valor de `group` es una cadena que identifica el grupo de concurrencia; dos ejecuciones con el mismo grupo no corren en paralelo. Lo habitual es incluir expresiones que incluyan la rama o el PR para que el límite sea por rama y no global. Cuando `cancel-in-progress` es `true`, al llegar una nueva ejecución se cancela automáticamente la que estuviera en curso en ese mismo grupo, lo que evita acumular colas de ejecuciones obsoletas en ramas con muchos commits seguidos.

```yaml
concurrency:
  group: ${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: true
```

Un patrón más refinado protege la rama principal de cancelaciones accidentales usando una expresión condicional:

```yaml
concurrency:
  group: ${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: ${{ github.ref != 'refs/heads/main' }}
```

## La propiedad `permissions` a nivel de workflow

La propiedad `permissions` restringe o amplía los permisos del token `GITHUB_TOKEN` que GitHub genera automáticamente para cada ejecución. Declarar permisos explícitos en la raíz del workflow es una buena práctica de seguridad porque impide que un job comprometido abuse de permisos que no necesita. El valor puede ser un mapa de permisos individuales o uno de los dos atajos globales. Los permisos individuales disponibles incluyen `contents`, `issues`, `pull-requests`, `packages`, `id-token`, `actions`, entre otros. Cada permiso acepta los valores `read`, `write` o `none`. La gestión granular del token y sus implicaciones de seguridad avanzadas se tratan en el dominio D5.

```yaml
permissions:
  contents: read
  pull-requests: write
  issues: none
```

## Los atajos `write-all` y `read-all`

GitHub Actions ofrece dos valores especiales que afectan a todos los ámbitos del token de una vez. Usar `permissions: write-all` concede permisos de escritura sobre todos los ámbitos disponibles, lo que equivale al comportamiento por defecto en repositorios donde la opción de seguridad no se ha restringido. Usar `permissions: read-all` concede solo lectura en todos los ámbitos. Estos atajos son útiles como punto de partida para depurar problemas de permisos, pero en producción es preferible declarar permisos granulares para seguir el principio de mínimo privilegio. Nunca se debe usar `write-all` en workflows que procesen código de pull requests de forks externos, ya que ese código no es de confianza.

```yaml
# Atajo de solo lectura (más seguro para la mayoría de workflows de CI)
permissions: read-all
```

```yaml
# Atajo de escritura total (solo para workflows internos de confianza)
permissions: write-all
```

## YAML anchors: sintaxis y propósito

Los YAML anchors son una característica del propio lenguaje YAML, no de GitHub Actions, que permite definir un bloque una sola vez y reutilizarlo en cualquier otro punto del mismo fichero. Esto resulta especialmente útil en workflows con múltiples jobs que comparten configuración de steps como la instalación de dependencias, la configuración de credenciales o los parámetros de entorno. La sintaxis tiene tres partes: el anchor `&nombre` que marca el bloque que se quiere reutilizar, el alias `*nombre` que lo inserta, y la merge key `<<:` que fusiona las claves de un anchor con las del bloque receptor permitiendo sobreescribir valores individuales.

El anchor se define junto a un nodo YAML con el símbolo `&` seguido de un identificador sin espacios. El alias `*nombre` inserta una copia del nodo anclado en el punto donde aparece. La merge key `<<: *nombre` solo funciona con mappings (objetos) y añade las claves del anchor al mapping actual sin reemplazar las que ya existen en él.

```yaml
# Definición del anchor (puede ir en cualquier parte del fichero,
# pero por convención se agrupa al inicio bajo una clave ficticia)
x-node-setup: &node-setup
  - name: Checkout
    uses: actions/checkout@v4
  - name: Setup Node
    uses: actions/setup-node@v4
    with:
      node-version: ${{ env.NODE_VERSION }}
      cache: npm

jobs:
  lint:
    runs-on: ubuntu-latest
    steps:
      - *node-setup          # alias: inserta los dos steps del anchor
      - name: Lint
        run: npm run lint

  test:
    runs-on: ubuntu-latest
    steps:
      - *node-setup          # misma reutilización sin duplicar
      - name: Test
        run: npm test
```

La merge key `<<:` se usa cuando el anchor define un mapping y se quiere extenderlo con claves adicionales o sobreescribir alguna:

```yaml
x-runner-defaults: &runner-defaults
  runs-on: ubuntu-latest
  timeout-minutes: 30
  env:
    CI: "true"

jobs:
  build:
    <<: *runner-defaults       # hereda runs-on, timeout-minutes y env
    name: Build job
    steps:
      - uses: actions/checkout@v4
      - run: npm run build

  deploy:
    <<: *runner-defaults       # hereda las mismas propiedades base
    name: Deploy job
    timeout-minutes: 60        # sobreescribe timeout-minutes del anchor
    steps:
      - uses: actions/checkout@v4
      - run: npm run deploy
```

## Limitación crítica de los YAML anchors

Los YAML anchors solo funcionan dentro del mismo fichero. No es posible definir un anchor en un fichero y referenciarlo desde otro. Esta limitación es fundamental y distingue los anchors de otros mecanismos de reutilización de GitHub Actions como los workflows reutilizables (`workflow_call`) o las composite actions. Si se necesita compartir lógica entre múltiples ficheros de workflow, la solución correcta son esos mecanismos de Actions, no YAML anchors. Intentar usar `*nombre` en un fichero diferente al que define `&nombre` produce un error de parseo YAML y el workflow falla antes de ejecutarse.

## Tabla de propiedades raíz

Las propiedades raíz de un workflow, su tipo, obligatoriedad, valor por defecto y descripción resumida son las siguientes.

| Propiedad | Tipo | Obligatorio | Default | Descripción |
|---|---|---|---|---|
| `name` | string | No | ruta del fichero | Título visible en la UI de Actions |
| `on` | string / list / map | Sí | — | Eventos que disparan el workflow |
| `env` | map string→string | No | `{}` | Variables de entorno para todos los jobs |
| `defaults.run.shell` | string | No | según SO | Shell por defecto para pasos `run` |
| `defaults.run.working-directory` | string | No | raíz del repo | Directorio de trabajo para pasos `run` |
| `concurrency.group` | string | No | — | Identificador del grupo de concurrencia |
| `concurrency.cancel-in-progress` | boolean / expression | No | `false` | Cancela ejecuciones en curso del mismo grupo |
| `permissions` | map / `read-all` / `write-all` | No | permisos del repo | Ámbitos del GITHUB_TOKEN |
| `jobs` | map | Sí | — | Definición de los jobs del workflow |

## Ejemplo central: fichero `ci.yml` completo

El siguiente fichero integra todas las propiedades raíz estudiadas y usa YAML anchors para evitar repetición entre jobs. Es un workflow de integración continua real que ejecuta lint, tests y construcción de imagen Docker.

```yaml
name: Integración continua

on:
  push:
    branches:
      - main
      - "release/**"
  pull_request:
    branches:
      - main
  workflow_dispatch:

env:
  NODE_VERSION: "20"
  REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository }}

defaults:
  run:
    shell: bash
    working-directory: .

concurrency:
  group: ${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: ${{ github.ref != 'refs/heads/main' }}

permissions:
  contents: read
  packages: write
  pull-requests: write

# Bloques reutilizables mediante YAML anchors
x-checkout-node: &checkout-node
  - name: Checkout del repositorio
    uses: actions/checkout@v4
  - name: Configurar Node.js
    uses: actions/setup-node@v4
    with:
      node-version: ${{ env.NODE_VERSION }}
      cache: npm
  - name: Instalar dependencias
    run: npm ci

x-job-defaults: &job-defaults
  runs-on: ubuntu-latest
  timeout-minutes: 20

jobs:
  lint:
    <<: *job-defaults
    name: Análisis estático
    permissions:
      contents: read
    steps:
      - *checkout-node
      - name: Ejecutar ESLint
        run: npm run lint
      - name: Ejecutar type-check
        run: npm run type-check

  test:
    <<: *job-defaults
    name: Tests unitarios e integración
    permissions:
      contents: read
    steps:
      - *checkout-node
      - name: Ejecutar tests
        run: npm test -- --coverage
      - name: Subir reporte de cobertura
        uses: actions/upload-artifact@v4
        with:
          name: coverage-report
          path: coverage/
          retention-days: 7

  build-image:
    <<: *job-defaults
    name: Construir imagen Docker
    needs:
      - lint
      - test
    permissions:
      contents: read
      packages: write
    steps:
      - name: Checkout del repositorio
        uses: actions/checkout@v4
      - name: Iniciar sesión en el registry
        uses: docker/login-action@v3
        with:
          registry: ${{ env.REGISTRY }}
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}
      - name: Extraer metadatos de la imagen
        id: meta
        uses: docker/metadata-action@v5
        with:
          images: ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}
      - name: Construir y publicar imagen
        uses: docker/build-push-action@v6
        with:
          context: .
          push: ${{ github.event_name != 'pull_request' }}
          tags: ${{ steps.meta.outputs.tags }}
          labels: ${{ steps.meta.outputs.labels }}
```

## Buenas y malas prácticas

Las siguientes prácticas resumen los errores más comunes al configurar propiedades raíz y sus alternativas correctas.

**Concurrencia sin proteger `main`**

Malo: usar `cancel-in-progress: true` sin condición cancela también ejecuciones en `main`, lo que puede interrumpir despliegues en curso.

```yaml
# MAL: cancela cualquier ejecución incluyendo deploys de main
concurrency:
  group: ${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: true
```

```yaml
# BIEN: solo cancela en ramas que no sean main
concurrency:
  group: ${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: ${{ github.ref != 'refs/heads/main' }}
```

**Permisos demasiado amplios**

Malo: usar `write-all` como punto de partida y olvidar restringirlos después expone el repositorio a que código malicioso de un fork eleve privilegios.

```yaml
# MAL: permisos de escritura total sin necesidad
permissions: write-all
```

```yaml
# BIEN: solo los ámbitos necesarios, en el nivel mínimo necesario
permissions:
  contents: read
  pull-requests: write
```

**`env` con secretos en texto plano**

Malo: poner valores sensibles directamente en `env` a nivel de workflow los expone en los logs y en la interfaz de GitHub.

```yaml
# MAL: el token queda expuesto en la definición del workflow
env:
  API_TOKEN: "ghp_abc123secreto"
```

```yaml
# BIEN: referenciar el secreto desde el almacén cifrado
env:
  API_TOKEN: ${{ secrets.API_TOKEN }}
```

**YAML anchors para compartir entre ficheros**

Malo: intentar usar un alias definido en otro fichero produce error de parseo YAML y el workflow nunca se ejecuta.

```yaml
# MAL: *checkout-node está definido en otro fichero, esto falla
steps:
  - *checkout-node
```

```yaml
# BIEN: para reutilizar entre ficheros usar una composite action
steps:
  - uses: ./.github/actions/checkout-node
```

**`defaults` sin `shell` explícito en runners mixtos**

Malo: no declarar `shell` en `defaults` y asumir que el entorno siempre es Linux provoca fallos silenciosos en runners Windows donde el shell por defecto es `pwsh`.

```yaml
# MAL: omitir defaults.run.shell en un workflow que puede correr en Windows
defaults:
  run:
    working-directory: ./src
```

```yaml
# BIEN: declarar shell explícito para comportamiento consistente
defaults:
  run:
    shell: bash
    working-directory: ./src
```

## Verificación y práctica

Las siguientes preguntas tienen el estilo de los exámenes GH-200. Conviene responderlas antes de avanzar al nodo siguiente.

**Pregunta 1.** Un workflow tiene `concurrency.group: ci-${{ github.ref }}` y `cancel-in-progress: true`. Hay una ejecución en curso en `main` cuando se hace push a `main` de nuevo. ¿Qué ocurre?

A) La nueva ejecución espera a que termine la actual.
B) La ejecución en curso se cancela y arranca la nueva.
C) Ambas ejecuciones corren en paralelo.
D) El workflow falla porque `main` está protegida.

*Respuesta correcta: B. Con `cancel-in-progress: true` la ejecución más antigua del grupo se cancela al llegar una nueva, sin importar la rama.*

**Pregunta 2.** Se declara `permissions: read-all` en la raíz del workflow. Un job concreto necesita escribir en packages. ¿Cuál es la forma correcta de conseguirlo sin ampliar permisos globalmente?

A) Añadir `permissions: write-all` en ese job.
B) Eliminar `permissions` de la raíz.
C) Declarar `permissions.packages: write` dentro del propio job.
D) Usar `GITHUB_TOKEN` con un PAT de sustitución.

*Respuesta correcta: C. Los permisos declarados a nivel de job sobreescriben los del workflow para ese job concreto.*

**Pregunta 3.** Un desarrollador define un YAML anchor `&setup` en `ci.yml` y quiere usar el alias `*setup` en `deploy.yml`. ¿Qué resultado obtendrá?

A) Funciona si ambos ficheros están en `.github/workflows/`.
B) Error de parseo YAML porque los anchors no cruzan ficheros.
C) El alias se ignora silenciosamente y el step se omite.
D) GitHub Actions resuelve el anchor usando el fichero más reciente.

*Respuesta correcta: B. Los anchors YAML tienen ámbito de fichero. Cruzar ficheros no está soportado por la especificación YAML.*

**Ejercicio práctico.** Crea el fichero `.github/workflows/pr-checks.yml` con las siguientes características:

- Nombre: "Verificación de Pull Request"
- Se activa solo en eventos `pull_request` hacia `main` con tipos `opened`, `synchronize` y `reopened`
- Variable de entorno global: `PYTHON_VERSION` con valor `"3.12"`
- Shell por defecto: `bash`; directorio de trabajo: `./backend`
- Concurrencia por PR sin cancelar `main`
- Permisos: solo lectura de contenidos y escritura en pull-requests
- Un anchor `&python-setup` con tres steps: checkout, setup-python (usando `env.PYTHON_VERSION` y cache pip) e instalación de dependencias con `pip install -r requirements.txt`
- Dos jobs (`lint` y `test`) que usen el anchor y añadan sus propios steps de análisis y ejecución de tests respectivamente

← [README](README.md) | → [1.2 Triggers de código](gha-d1-triggers-codigo.md)
