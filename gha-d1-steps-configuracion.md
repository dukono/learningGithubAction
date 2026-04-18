[anterior: 1.5.2 Control de flujo del job](gha-d1-jobs-control-flujo.md) | [siguiente: 1.7 Condicionales y funciones de estado](gha-d1-condicionales.md)

# 1.6 Configuración de steps

Un step es la unidad atómica de trabajo dentro de un job. Cada step se ejecuta en el mismo runner que los demás steps del job y comparte el sistema de archivos del workspace, pero es procesado de forma secuencial. Entender las propiedades disponibles para configurar cada step es fundamental para controlar qué se ejecuta, bajo qué condiciones, con qué entorno y durante cuánto tiempo.

## Árbol de propiedades de un step

Las propiedades disponibles para un step se organizan en dos grupos principales: las que determinan qué ejecuta el step (`uses` o `run`) y las que modifican su comportamiento (`name`, `id`, `if`, `env`, `with`, `continue-on-error`, `timeout-minutes`, `shell`, `working-directory`).

```
step
├── name                  # Etiqueta visible en los logs de la UI
├── id                    # Identificador para referenciar outputs/outcome
├── if                    # Condición de ejecución
├── uses                  # Referenciar una action externa o reutilizable
│   └── with              # Inputs de la action (solo con uses)
├── run                   # Comandos shell directos (alternativa a uses)
│   ├── shell             # Intérprete a usar (solo con run)
│   └── working-directory # Directorio de trabajo (solo con run)
├── env                   # Variables de entorno a nivel de step
├── continue-on-error     # Continuar el job si este step falla
└── timeout-minutes       # Tiempo máximo de ejecución del step
```

## La propiedad `uses`

La propiedad `uses` permite referenciar una action, ya sea alojada en un repositorio público de GitHub, en un repositorio privado de la misma organización o en una ruta local dentro del mismo repositorio. El formato estándar es `owner/repo@ref`, donde `ref` puede ser una etiqueta semántica (`v3`), una rama (`main`) o un SHA de commit completo (`a81bbbf8298035622992ca21370890`). Para el examen GH-200, es importante recordar que usar un SHA completo es la forma más segura porque es inmutable, mientras que las etiquetas pueden ser reasignadas. Las actions del directorio `.github/actions/` del mismo repositorio se referencian con una ruta relativa como `./github/actions/mi-action`.

## La propiedad `run`

La propiedad `run` ejecuta comandos directamente en el shell del runner, sin necesidad de una action externa. Es la alternativa a `uses` y ambas son mutuamente excluyentes: un step no puede tener `run` y `uses` al mismo tiempo. Para ejecutar múltiples comandos, se usa el operador de bloque literal YAML `|`, que preserva los saltos de línea. Si un comando individual falla (sale con código distinto de cero), el step falla inmediatamente y los comandos siguientes del mismo `run` no se ejecutan.

## La propiedad `name`

La propiedad `name` asigna una etiqueta descriptiva al step, que aparece en la interfaz de usuario de GitHub Actions dentro del log del job. Si se omite, GitHub muestra automáticamente el valor de `uses` o los primeros caracteres del bloque `run` como etiqueta. Usar `name` con descripciones claras facilita enormemente la depuración, especialmente en workflows con muchos steps similares.

## La propiedad `id`

La propiedad `id` asigna un identificador único al step dentro del job. Este identificador permite acceder al resultado y las salidas del step desde steps posteriores mediante el contexto `steps`. Las dos propiedades accesibles son `steps.<id>.outputs.<nombre>` para los valores que la action haya declarado como outputs, y `steps.<id>.outcome` para el resultado del step (`success`, `failure`, `cancelled`, `skipped`). Sin un `id`, no es posible referenciar los outputs de ese step.

## La propiedad `if`

La propiedad `if` a nivel de step acepta una expresión que se evalúa antes de ejecutar el step. Si la expresión devuelve `false`, el step se omite y su `outcome` queda como `skipped`. Es especialmente útil combinada con las funciones de estado como `success()`, `failure()`, o `always()`, que permiten ejecutar steps de limpieza o notificación incluso cuando pasos anteriores han fallado. A diferencia del `if` a nivel de job, el `if` de step no corta el flujo del job; simplemente salta ese step y continúa con el siguiente.

## La propiedad `env` a nivel de step

La propiedad `env` permite definir variables de entorno específicas para ese step. En la jerarquía de precedencia, las variables definidas a nivel de step tienen la prioridad más alta: sobreescriben a las definidas a nivel de job y a las definidas a nivel de workflow. Esto permite parametrizar steps individuales sin afectar al resto del job, o sobreescribir temporalmente una variable global para un step concreto.

## La propiedad `with`

La propiedad `with` proporciona los inputs a una action referenciada con `uses`. Cada action declara en su archivo `action.yml` los inputs que acepta, sus descripciones y sus valores por defecto. Los pares clave/valor dentro de `with` corresponden directamente a esos inputs declarados. Si se omite un input que la action marca como `required: true`, el workflow fallará en tiempo de ejecución. La propiedad `with` solo es válida junto con `uses`; no tiene efecto en un step con `run`.

## La propiedad `continue-on-error` del step

La propiedad `continue-on-error` a nivel de step permite que el job continúe ejecutándose aunque ese step falle. A diferencia del `continue-on-error` a nivel de job (que afecta a cómo se consideran los jobs dependientes en la matriz), el `continue-on-error` de step solo tiene alcance local: el step marcado como fallido tendrá `outcome: failure` pero el job no se detendrá. El `conclusion` del step, sin embargo, será `success` si `continue-on-error` está activo, lo que puede afectar a condiciones `if` que usen `steps.<id>.conclusion`.

## La propiedad `timeout-minutes` del step

La propiedad `timeout-minutes` establece un límite de tiempo máximo para la ejecución de ese step en particular, independientemente del `timeout-minutes` del job. Si el step supera ese límite, GitHub lo cancela y el step falla con un error de timeout. Esto es útil para steps que pueden bloquearse indefinidamente, como descargas de dependencias, pruebas de integración o llamadas a APIs externas. El valor por defecto, si no se especifica, es heredado del tiempo restante del job (que por defecto es de 360 minutos).

## La propiedad `shell`

La propiedad `shell` permite especificar el intérprete de comandos que se usará para ejecutar el bloque `run`. Los valores disponibles en GitHub Actions son: `bash`, `sh`, `pwsh` (PowerShell Core), `powershell` (PowerShell 5, solo Windows), `cmd` (símbolo del sistema de Windows), y `python`. El valor por defecto en runners Linux y macOS es `bash`, y en Windows es `pwsh`. Cambiar el shell es necesario cuando se quieren ejecutar scripts Python directamente en un step `run`, cuando se trabaja con runners Windows que requieren `cmd` para compatibilidad con scripts legacy, o cuando se necesita una semántica de error distinta (`bash` con `set -e` por defecto en GitHub Actions cancela el step al primer error).

## La propiedad `working-directory`

La propiedad `working-directory` cambia el directorio de trabajo para los comandos del bloque `run`. Por defecto, todos los steps ejecutan desde la raíz del workspace (`$GITHUB_WORKSPACE`). Cambiar el directorio de trabajo es útil en repositorios monorepo donde los comandos de compilación o prueba deben ejecutarse desde un subdirectorio concreto, sin necesidad de anteponer `cd` a cada comando del bloque `run`. Esta propiedad no tiene efecto en steps que usen `uses`; en ese caso, la action gestiona su propio directorio de trabajo.

## Ejemplo central: job con todas las propiedades

El siguiente workflow muestra un job de CI que combina las once propiedades de configuración de steps en un ejemplo realista y completo.

```yaml
name: CI Build and Test

on:
  push:
    branches: [main]
  pull_request:

env:
  NODE_VERSION: "20"
  ARTIFACT_NAME: build-output

jobs:
  build-test:
    runs-on: ubuntu-latest
    timeout-minutes: 30
    env:
      LOG_LEVEL: info

    steps:
      # Step 1: uses + with + name + id
      - name: Checkout repository
        id: checkout
        uses: actions/checkout@v4
        with:
          fetch-depth: 0
          token: ${{ secrets.GITHUB_TOKEN }}

      # Step 2: uses + with + name + id
      - name: Setup Node.js
        id: setup-node
        uses: actions/setup-node@v4
        with:
          node-version: ${{ env.NODE_VERSION }}
          cache: npm

      # Step 3: run + working-directory + shell
      - name: Install dependencies
        id: install
        shell: bash
        working-directory: ./app
        run: |
          echo "Installing with NODE_VERSION=$NODE_VERSION"
          npm ci --prefer-offline

      # Step 4: run + env a nivel de step (sobreescribe LOG_LEVEL del job)
      - name: Build project
        id: build
        shell: bash
        working-directory: ./app
        env:
          LOG_LEVEL: debug
          BUILD_MODE: production
        run: |
          npm run build
          echo "build_path=./app/dist" >> $GITHUB_OUTPUT

      # Step 5: run + continue-on-error + timeout-minutes
      - name: Run unit tests
        id: unit-tests
        shell: bash
        working-directory: ./app
        continue-on-error: true
        timeout-minutes: 10
        run: npm test -- --coverage

      # Step 6: if condicional basado en outcome de step anterior
      - name: Upload coverage report
        id: coverage
        if: steps.unit-tests.outcome == 'success'
        uses: actions/upload-artifact@v4
        with:
          name: coverage-report
          path: ./app/coverage/
          retention-days: 7

      # Step 7: if con failure() + continue-on-error para notificación siempre
      - name: Notify test failure
        id: notify-failure
        if: steps.unit-tests.outcome == 'failure'
        continue-on-error: true
        timeout-minutes: 2
        shell: bash
        env:
          SLACK_WEBHOOK: ${{ secrets.SLACK_WEBHOOK }}
        run: |
          curl -X POST "$SLACK_WEBHOOK" \
            -H 'Content-type: application/json' \
            --data '{"text":"Tests fallaron en ${{ github.ref }}"}'

      # Step 8: run + python shell + working-directory
      - name: Analyze build size
        id: analyze
        shell: python
        working-directory: ./app/dist
        run: |
          import os
          total = sum(
            os.path.getsize(os.path.join(r, f))
            for r, _, files in os.walk('.')
            for f in files
          )
          print(f"Total build size: {total / 1024:.1f} KB")

      # Step 9: uses para subir artefacto del build
      - name: Upload build artifact
        id: upload-build
        if: steps.build.outcome == 'success'
        uses: actions/upload-artifact@v4
        with:
          name: ${{ env.ARTIFACT_NAME }}
          path: ${{ steps.build.outputs.build_path }}
          retention-days: 30
```

## Tabla de propiedades del step

Las once propiedades de configuración de un step cubren todos los aspectos de su ejecución. La tabla siguiente resume cada una con su tipo, si es compatible con `uses` o `run`, y su efecto principal.

| Propiedad | Compatible con | Tipo | Descripción |
|---|---|---|---|
| `name` | `uses` y `run` | string | Etiqueta visible en los logs de la UI |
| `id` | `uses` y `run` | string | Identificador para `steps.<id>.outputs` y `.outcome` |
| `if` | `uses` y `run` | expresión | Condición de ejecución; `false` produce `skipped` |
| `uses` | — | string | Referencia a una action: `owner/repo@ref` o ruta local |
| `with` | solo `uses` | map | Inputs de la action (clave/valor) |
| `run` | — | string | Comandos shell; `\|` para múltiples líneas |
| `shell` | solo `run` | string | Intérprete: `bash`, `sh`, `pwsh`, `cmd`, `python` |
| `working-directory` | solo `run` | string | Directorio de trabajo para los comandos `run` |
| `env` | `uses` y `run` | map | Variables de entorno (mayor precedencia en la jerarquía) |
| `continue-on-error` | `uses` y `run` | boolean | El job continúa aunque el step falle |
| `timeout-minutes` | `uses` y `run` | number | Límite de tiempo individual por step |

## Política de organización y actions permitidas

Las propiedades `uses` y `with` permiten referenciar cualquier action pública de GitHub. Sin embargo, los administradores de organización pueden restringir qué actions están permitidas mediante políticas de organización: pueden limitar el uso a las actions del propio repositorio, de la organización, o de una lista de actions aprobadas explícitamente. Estas políticas se configuran en la sección de Settings de la organización y afectan a todos los repositorios bajo ella. Este tema se desarrolla con detalle en el dominio D4 (Administración y seguridad).

## Buenas y malas prácticas

Usar actions con SHA de commit completo en lugar de etiquetas semánticas (`actions/checkout@a81bbbf8298035622992ca21370890...` en vez de `actions/checkout@v4`) es la práctica recomendada en entornos de producción y seguridad porque garantiza inmutabilidad. En entornos de desarrollo o aprendizaje, las etiquetas semánticas son aceptables por su legibilidad. La mala práctica es usar referencias a ramas como `@main` porque el contenido puede cambiar sin previo aviso.

Asignar `id` a todos los steps que producen outputs o cuyo `outcome` sea relevante para steps posteriores es una buena práctica de diseño de workflows. La mala práctica es omitir `id` y luego intentar referenciar el resultado de un step con expresiones como `steps..outcome`, que simplemente no funcionan.

Usar `continue-on-error: true` en steps de análisis o notificación que no deben bloquear el pipeline principal es correcto. La mala práctica es aplicar `continue-on-error: true` indiscriminadamente a steps críticos (compilación, despliegue) porque enmascara fallos reales y dificulta la detección de problemas.

Definir variables sensibles en `env` a nivel de step mediante secretos (`env: API_KEY: ${{ secrets.API_KEY }}`) minimiza la exposición. La mala práctica es definir secretos como variables de entorno a nivel de workflow o job cuando solo son necesarios en un step concreto, ya que aumenta innecesariamente su superficie de exposición.

Usar `timeout-minutes` en steps que hacen llamadas a servicios externos o ejecutan pruebas de integración es una buena práctica para evitar que un runner quede bloqueado consumiendo minutos de facturación. La mala práctica es confiar únicamente en el `timeout-minutes` del job, que puede ser demasiado permisivo para steps individuales problemáticos.

## Verificación GH-200

Las siguientes preguntas representan el tipo de razonamiento que evalúa el examen sobre configuración de steps.

**Pregunta 1.** Un step tiene `continue-on-error: true` y falla. En un step posterior con `if: steps.mi-step.outcome == 'success'`, ¿se ejecuta ese step posterior?

No. La propiedad `outcome` refleja el resultado real del step antes de aplicar `continue-on-error`. Si el step falló, `outcome` es `failure` aunque `conclusion` sea `success`. Por tanto, la condición `outcome == 'success'` devuelve `false` y el step posterior se salta.

**Pregunta 2.** ¿Cuál es la diferencia entre definir `env` a nivel de workflow, de job y de step?

La jerarquía de precedencia es: step > job > workflow. Una variable definida en los tres niveles con el mismo nombre tomará el valor del nivel de step cuando ese step se ejecute. Los niveles inferiores (workflow, job) sirven como valores por defecto que los niveles superiores pueden sobreescribir.

**Pregunta 3.** ¿Puede un step tener `uses` y `run` simultáneamente?

No. Son propiedades mutuamente excluyentes. Un step define o bien una action externa con `uses`, o bien comandos directos con `run`, pero nunca ambas en el mismo step.

**Ejercicio práctico.** Escribe un step que: (1) tenga `id: lint`, (2) ejecute `npm run lint` en el directorio `./frontend`, (3) use bash explícitamente, (4) no detenga el job si falla, (5) tenga un timeout de 5 minutos, y (6) defina la variable `CI=true` solo para ese step.

```yaml
- name: Lint frontend
  id: lint
  shell: bash
  working-directory: ./frontend
  continue-on-error: true
  timeout-minutes: 5
  env:
    CI: "true"
  run: npm run lint
```

## Interacción entre propiedades: escenarios avanzados

Comprender cómo interactúan las propiedades entre sí es clave para diseñar steps robustos. Los siguientes escenarios ilustran combinaciones frecuentes en entornos reales.

### SHA vs etiqueta semántica en `uses`

Al referenciar una action con `uses`, la elección del formato de `ref` tiene implicaciones de seguridad y mantenimiento. La siguiente tabla compara los tres formatos posibles.

```yaml
# Formato 1: etiqueta semántica — legible, pero mutable
- uses: actions/checkout@v4

# Formato 2: SHA completo — inmutable, recomendado en producción
- uses: actions/checkout@11bd71901bbe5b1630ceea73d27597364c9af683

# Formato 3: referencia a rama — desaconsejado en producción
- uses: actions/checkout@main
```

Usar SHA completo es la práctica recomendada por el equipo de seguridad de GitHub porque una etiqueta puede ser reasignada por el propietario del repositorio de la action, mientras que un SHA de commit es inmutable por definición. En pipelines de CI/CD críticos o con requisitos de auditoría, el SHA completo es obligatorio.

### `id` y el contexto `steps`

Una vez que un step tiene `id`, su información queda disponible en el contexto `steps` para el resto del job. Las propiedades accesibles son distintas según el momento en que se evalúen.

```yaml
jobs:
  demo-context:
    runs-on: ubuntu-latest
    steps:
      - name: Generar versión
        id: version
        run: |
          VERSION="1.$(date +%Y%m%d).$(echo $GITHUB_SHA | cut -c1-7)"
          echo "tag=$VERSION" >> $GITHUB_OUTPUT
          echo "branch=${GITHUB_REF_NAME}" >> $GITHUB_OUTPUT

      - name: Mostrar outputs del step anterior
        run: |
          echo "Tag generado: ${{ steps.version.outputs.tag }}"
          echo "Rama: ${{ steps.version.outputs.branch }}"
          echo "Resultado del step: ${{ steps.version.outcome }}"
          echo "Conclusión del step: ${{ steps.version.conclusion }}"
```

La diferencia entre `outcome` y `conclusion` es relevante cuando `continue-on-error: true` está activo. El `outcome` refleja el resultado real (`success`, `failure`, `cancelled`, `skipped`) mientras que `conclusion` refleja el resultado considerando `continue-on-error` (si el step falló pero tiene `continue-on-error: true`, `conclusion` será `success`).

### `env` a nivel de step y secretos

El nivel de step es el lugar más apropiado para inyectar secretos como variables de entorno porque minimiza su exposición. Un secreto definido en `env` a nivel de workflow está disponible en todos los steps, incluidos los que no lo necesitan.

```yaml
jobs:
  deploy:
    runs-on: ubuntu-latest
    env:
      # Variable general disponible en todos los steps
      ENVIRONMENT: production

    steps:
      - name: Deploy application
        # El secreto solo existe en este step
        env:
          DEPLOY_TOKEN: ${{ secrets.DEPLOY_TOKEN }}
          DATABASE_URL: ${{ secrets.DATABASE_URL }}
        run: |
          ./scripts/deploy.sh \
            --env "$ENVIRONMENT" \
            --token "$DEPLOY_TOKEN" \
            --db "$DATABASE_URL"

      - name: Verify deployment
        # Este step NO tiene acceso a DEPLOY_TOKEN ni DATABASE_URL
        run: curl --fail https://api.example.com/health
```

### `shell: python` para scripts de análisis inline

El shell `python` permite ejecutar scripts Python directamente en un bloque `run` sin necesidad de crear un archivo `.py` separado. GitHub Actions ejecuta el contenido del bloque `run` como un script Python temporal.

```yaml
- name: Parse test results
  id: parse-results
  shell: python
  run: |
    import json
    import os

    with open('test-results.json') as f:
      data = json.load(f)

    total = data['numTotalTests']
    passed = data['numPassedTests']
    failed = data['numFailedTests']

    print(f"Total: {total}, Passed: {passed}, Failed: {failed}")

    # Escribir outputs para steps posteriores
    with open(os.environ['GITHUB_OUTPUT'], 'a') as out:
      out.write(f"total={total}\n")
      out.write(f"passed={passed}\n")
      out.write(f"failed={failed}\n")
      out.write(f"success={'true' if failed == 0 else 'false'}\n")
```

### `working-directory` en monorepos

En repositorios monorepo con múltiples paquetes o servicios, `working-directory` evita tener que anteponer rutas a cada comando. Esto mejora la legibilidad y reduce errores de ruta.

```yaml
jobs:
  test-services:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Test auth service
        id: test-auth
        shell: bash
        working-directory: ./services/auth
        run: |
          npm ci
          npm test

      - name: Test payment service
        id: test-payment
        shell: bash
        working-directory: ./services/payment
        run: |
          npm ci
          npm test

      - name: Build frontend
        id: build-frontend
        shell: bash
        working-directory: ./packages/frontend
        run: |
          npm ci
          npm run build
          ls -la dist/
```

### `timeout-minutes` en steps de descarga y red

Los steps que dependen de red son candidatos naturales para `timeout-minutes` porque pueden bloquearse silenciosamente si el servicio externo no responde.

```yaml
steps:
  - name: Download external dataset
    id: download
    timeout-minutes: 5
    continue-on-error: true
    run: |
      wget --timeout=60 --tries=3 \
        https://data.example.com/large-dataset.tar.gz \
        -O dataset.tar.gz

  - name: Use cached dataset if download failed
    if: steps.download.outcome == 'failure'
    run: |
      echo "Descarga fallida, usando caché local"
      cp /tmp/cached-dataset.tar.gz dataset.tar.gz

  - name: Process dataset
    run: tar -xzf dataset.tar.gz && python process.py
```

### `if` combinado con outputs de steps anteriores

El `if` de un step puede evaluar cualquier expresión válida de GitHub Actions, incluyendo outputs de steps anteriores. Esto permite crear flujos de decisión dentro del job sin necesidad de jobs separados.

```yaml
steps:
  - name: Check if version changed
    id: check-version
    run: |
      PREV=$(git show HEAD~1:package.json | python3 -c "import sys,json; print(json.load(sys.stdin)['version'])")
      CURR=$(python3 -c "import json; print(json.load(open('package.json'))['version'])")
      if [ "$PREV" != "$CURR" ]; then
        echo "changed=true" >> $GITHUB_OUTPUT
        echo "prev_version=$PREV" >> $GITHUB_OUTPUT
        echo "new_version=$CURR" >> $GITHUB_OUTPUT
      else
        echo "changed=false" >> $GITHUB_OUTPUT
      fi

  - name: Create release tag
    if: steps.check-version.outputs.changed == 'true'
    run: |
      git tag "v${{ steps.check-version.outputs.new_version }}"
      git push origin "v${{ steps.check-version.outputs.new_version }}"

  - name: Publish to npm
    if: steps.check-version.outputs.changed == 'true'
    env:
      NPM_TOKEN: ${{ secrets.NPM_TOKEN }}
    run: |
      echo "//registry.npmjs.org/:_authToken=$NPM_TOKEN" > .npmrc
      npm publish

  - name: Skip message
    if: steps.check-version.outputs.changed == 'false'
    run: echo "Versión sin cambios, no se publica"
```

## Referencia rápida: cuándo usar cada propiedad

El siguiente esquema resume los casos de uso típicos para decidir qué propiedad aplicar en cada situación.

```
¿Qué quiero ejecutar?
├── Una action de GitHub Marketplace o reutilizable → uses: owner/repo@ref
│   └── ¿La action necesita inputs? → with: { input: value }
└── Comandos de shell propios → run: |
    ├── ¿Necesito un intérprete distinto de bash? → shell: python / pwsh / cmd
    └── ¿Estoy en un monorepo? → working-directory: ./subpackage

¿Cómo identifico/controlo el step?
├── ¿Necesito referenciar su resultado o outputs? → id: mi-step
├── ¿Debe ejecutarse condicionalmente? → if: <expresión>
├── ¿Qué aparece en los logs? → name: Descripción clara
└── ¿Necesito variables de entorno específicas? → env: { VAR: value }

¿Cómo manejo fallos y tiempo?
├── ¿El job debe continuar si falla? → continue-on-error: true
└── ¿Puede bloquearse indefinidamente? → timeout-minutes: N
```

## Relación entre `uses` y el archivo `action.yml`

Cuando se especifica `uses`, GitHub descarga el repositorio de la action en la referencia indicada y busca el archivo `action.yml` (o `action.yaml`) en la raíz o en la ruta especificada. Este archivo declara los inputs que acepta la action, y los valores en `with` deben corresponderse con esos inputs. Si se pasa un input no declarado, GitHub lo ignora silenciosamente. Si se omite un input declarado como `required: true`, el workflow falla con un error descriptivo en el log del step.

```yaml
# Ejemplo de action.yml de una action hipotética
inputs:
  environment:
    description: "Target deployment environment"
    required: true
  dry-run:
    description: "Run without making changes"
    required: false
    default: "false"

# Su uso correcto en un workflow
- uses: myorg/deploy-action@v2
  with:
    environment: production   # required — debe estar
    dry-run: "true"           # optional — sobreescribe el default
```

## Flujo de evaluación de un step

GitHub Actions evalúa las propiedades de cada step en un orden definido. Primero resuelve las expresiones en `if` para decidir si el step se ejecuta. Si `if` es falso, el step se marca como `skipped` y se pasa al siguiente. Si `if` es verdadero, se resuelven las expresiones en `env` y `with`, se establece el `working-directory` si se especificó, y se inicia la ejecución del comando `run` o de la action `uses` con el `shell` indicado. El `timeout-minutes` comienza a contar desde el inicio de la ejecución. Si el step termina antes del timeout, se registra su `outcome`. Si `continue-on-error` está activo y el step falló, `conclusion` se establece como `success` para que el job continúe.

---

[anterior: 1.5.2 Control de flujo del job](gha-d1-jobs-control-flujo.md) | [siguiente: 1.7 Condicionales y funciones de estado](gha-d1-condicionales.md)
