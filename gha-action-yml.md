# 3.2 El fichero action.yml: estructura y metadatos

← [3.1 Tipos de GitHub Actions (JavaScript, Docker, Composite)](gha-action-tipos.md) | [Índice](README.md) | [3.3 Inputs y outputs de una action](gha-action-inputs-outputs.md) →

---

`action.yml` es el único fichero obligatorio en cualquier GitHub Action: define qué hace la action, qué datos recibe, qué datos produce y cómo el runner debe ejecutarla. Sin este fichero en la raíz del repositorio, GitHub no reconoce el repositorio como una action válida. La DEFINICIÓN de inputs y outputs ocurre aquí; el USO desde el workflow que la invoca se documenta en [3.3](gha-action-inputs-outputs.md).

## Estructura completa de action.yml

El siguiente árbol muestra todos los campos relevantes con sus valores posibles:

```yaml
# Árbol de campos de action.yml
name: "Nombre visible de la action"          # obligatorio
description: "Descripción breve"             # obligatorio
author: "Nombre del autor"                   # opcional

inputs:                                       # opcional
  nombre-input:
    description: "Descripción del input"      # obligatorio dentro de inputs
    required: true | false
    default: "valor-por-defecto"              # opcional; ignorado si required: true

outputs:                                      # opcional
  nombre-output:
    description: "Descripción del output"     # obligatorio dentro de outputs
    value: ${{ steps.id.outputs.clave }}      # expresión (composite) o vacío (JS)

runs:                                         # obligatorio
  using: 'node20' | 'node16' | 'docker' | 'composite'
  # Para JavaScript (node20/node16):
  main: 'dist/index.js'                       # obligatorio
  pre: 'dist/setup.js'                        # opcional
  post: 'dist/cleanup.js'                     # opcional
  # Para Docker:
  image: 'Dockerfile' | 'docker://imagen:tag' # obligatorio
  entrypoint: '/entrypoint.sh'                # opcional
  args: ['arg1', 'arg2']                      # opcional
  # Para composite:
  steps: []                                   # obligatorio; lista de steps

branding:                                     # opcional; requerido para Marketplace
  icon: 'check-circle'                        # nombre de icono Feather Icons
  color: 'green'                              # white|yellow|blue|green|orange|red|purple|gray-dark
```

```mermaid
mindmap
  root((action.yml))
    (name / description / author)
    (inputs)
      nombre-input
        description
        required
        default
    (outputs)
      nombre-output
        description
        value
    (runs — obligatorio)
      using: node20 / docker / composite
      [JS] main / pre / post
      [Docker] image / entrypoint / args
      [Composite] steps
    (branding — Marketplace)
      icon
      color
```
*`runs` es el único bloque verdaderamente obligatorio junto con `name` y `description`.*

## Nombre del fichero y ubicación

El fichero debe llamarse `action.yml` o `action.yaml` (ambos son válidos) y estar en la raíz del repositorio. Si la action está en un subdirectorio, la ruta completa forma parte de la referencia `uses:` (ej: `owner/repo/path/to/action@ref`). GitHub no acepta el fichero en ninguna otra ubicación para identificar el repositorio como una action.

## Campos obligatorios: name, description, runs

Los únicos campos verdaderamente obligatorios son `name`, `description` y `runs`. Sin cualquiera de ellos, GitHub rechaza la action con error de metadatos. El campo `name` es el texto visible en la UI de GitHub cuando la action aparece en los logs de un workflow; `description` aparece en la página del Marketplace si se publica.

## inputs: declaración de parámetros de entrada

El bloque `inputs` declara los parámetros que el workflow llamante puede (o debe) pasar a la action via `with:`. Cada input tiene tres subfields: `description` (obligatorio dentro del bloque), `required` (boolean, default `false`) y `default` (valor a usar si no se proporciona el input). Cuando `required: true` y el workflow no pasa el input y no hay `default`, el runner falla el job con error explícito antes de ejecutar ningún step.

> [EXAMEN] `required: true` sin `default` + input ausente = job failure inmediato. `required: false` sin `default` + input ausente = el input llega como cadena vacía, no como `null`.

## outputs: declaración de valores de retorno

El bloque `outputs` declara los valores que la action producirá y que el workflow llamante podrá leer en `steps.<id>.outputs.<nombre>`. En JavaScript actions, el campo `value` se deja vacío porque el valor se produce en runtime mediante `GITHUB_OUTPUT`. En composite actions, `value` debe ser una expresión que referencie el output de un step interno: `${{ steps.<id>.outputs.<clave> }}`.

> [CONCEPTO] La DECLARACIÓN del output en `action.yml` (`outputs:`) y la PRODUCCIÓN del valor en runtime (`echo "clave=valor" >> $GITHUB_OUTPUT`) son dos operaciones separadas. Sin la declaración, el valor producido no es accesible desde el workflow llamante.

## runs.using: el tipo de ejecución

El campo `runs.using` determina el tipo de action y qué subfields de `runs` son relevantes. Los valores válidos son `'node20'`, `'node16'` (JavaScript actions), `'docker'` (Docker container actions) y `'composite'`. El valor `'node20'` es el recomendado para nuevas JavaScript actions.

## runs.main, runs.pre, runs.post para JavaScript actions

En JavaScript actions, `runs.main` es el path al archivo JS que el runner ejecuta como cuerpo principal de la action (obligatorio). `runs.pre` y `runs.post` son opcionales y permiten declarar scripts que se ejecutan antes del primer step del job y después del último step respectivamente.

## runs.image para Docker actions

En Docker actions, `runs.image` es el path al `Dockerfile` del repositorio o una referencia a una imagen pública (`docker://alpine:3.18`). El runner usa este campo para construir o descargar el contenedor. El desarrollo interno del `Dockerfile` está fuera del alcance del examen GH-200; el campo relevante para el examen es cómo se declara en `action.yml`.

## runs.steps para composite actions

En composite actions, `runs.steps` es una lista de steps con la misma estructura que los steps de un job normal: pueden usar `run:` (con `shell:` obligatorio) o `uses:` (invocar otra action). La diferencia con un job es que no soportan `services:` ni `if:` en los steps individuales.

> [ADVERTENCIA] En composite actions, cada step `run:` debe declarar explícitamente `shell: bash` (o `shell: pwsh`, etc.). Sin `shell:`, el step falla porque composite actions no heredan el shell por defecto del job.

## branding: icon y color

El bloque `branding` define el icono y el color que aparecen en la página del Marketplace. No afectan al funcionamiento de la action. El campo `icon` acepta nombres de iconos de la librería [Feather Icons](https://feathericons.com/); `color` acepta ocho valores predefinidos. Aunque opcionales para el funcionamiento, son **obligatorios para publicar en el Marketplace**.

## Campos opcionales: author

El campo `author` es opcional y sirve para identificar al creador o mantenedor de la action. Aparece en la página del Marketplace y en los metadatos de la action.

## Ejemplo central

Los tres ejemplos siguientes muestran `action.yml` completo para cada tipo de action:

```yaml
# action.yml — JavaScript action
name: Versión del paquete
description: Lee la versión desde package.json y la expone como output
author: "Mi Organización"

inputs:
  package-path:
    description: Ruta al directorio con package.json
    required: false
    default: "."

outputs:
  version:
    description: Versión del paquete leída de package.json

branding:
  icon: tag
  color: blue

runs:
  using: node20
  main: dist/index.js
  post: dist/cleanup.js
```

```yaml
# action.yml — Docker container action
name: Validador de YAML
description: Valida ficheros YAML usando un contenedor con yamllint

inputs:
  path:
    description: Ruta a validar
    required: true

runs:
  using: docker
  image: Dockerfile
  args:
    - ${{ inputs.path }}
```

```yaml
# action.yml — Composite action
name: Setup y Test
description: Configura Node.js y ejecuta los tests

inputs:
  node-version:
    description: Versión de Node.js
    required: false
    default: "20"

outputs:
  test-status:
    description: passed o failed
    value: ${{ steps.run-tests.outputs.status }}

branding:
  icon: check-circle
  color: green

runs:
  using: composite
  steps:
    - name: Setup Node
      uses: actions/setup-node@v4
      with:
        node-version: ${{ inputs.node-version }}

    - name: Instalar y testear
      id: run-tests
      shell: bash
      run: |
        npm ci
        if npm test; then
          echo "status=passed" >> $GITHUB_OUTPUT
        else
          echo "status=failed" >> $GITHUB_OUTPUT
        fi
```

## Tabla de elementos clave

| Campo | Obligatorio | Aplica a | Descripción |
|-------|:-----------:|----------|-------------|
| `name` | Sí | Todos | Nombre visible en UI y Marketplace |
| `description` | Sí | Todos | Descripción breve |
| `author` | No | Todos | Nombre del creador |
| `inputs.<id>.description` | Sí (si hay inputs) | Todos | Descripción del input |
| `inputs.<id>.required` | No | Todos | Default: `false` |
| `inputs.<id>.default` | No | Todos | Valor si no se provee el input |
| `outputs.<id>.value` | Sí (composite) | Composite | Expresión que produce el valor |
| `runs.using` | Sí | Todos | `node20`, `node16`, `docker`, `composite` |
| `runs.main` | Sí (JS) | JS | Path al script principal |
| `runs.pre` / `runs.post` | No | JS | Scripts de ciclo de vida |
| `runs.image` | Sí (Docker) | Docker | Dockerfile o imagen pública |
| `runs.steps` | Sí (composite) | Composite | Lista de steps |
| `branding.icon` | No* | Todos | Icono Feather Icons (*obligatorio para Marketplace) |
| `branding.color` | No* | Todos | Color del badge (*obligatorio para Marketplace) |

## Buenas y malas prácticas

**Hacer:**
- **Declarar `description` en cada input** — razón: GitHub muestra este texto en la UI cuando alguien usa la action; sin descripción, el consumidor no sabe qué pasar.
- **Usar `value: ${{ steps.<id>.outputs.<clave> }}` en outputs de composite actions** — razón: sin esta expresión el output no se propaga al workflow llamante aunque el step lo haya producido.
- **Especificar `shell:` en cada step `run:` de una composite action** — razón: composite actions no heredan el shell por defecto del job; sin `shell:` el step falla.

**Evitar:**
- **Poner `action.yml` en un subdirectorio sin reflejarlo en la referencia `uses:`** — razón: si la action está en `actions/build/action.yml`, la referencia debe ser `owner/repo/actions/build@ref`, no `owner/repo@ref`.
- **Definir `required: true` en un input sin documentar el error que ocurre si falta** — razón: el mensaje de error del runner no siempre es claro; el README debe indicar qué inputs son obligatorios.
- **Omitir `branding` y luego intentar publicar en el Marketplace** — razón: GitHub rechaza la publicación si falta el bloque `branding` con `icon` y `color` válidos.

## Verificación y práctica

### Preguntas de examen

**Pregunta 1.** En una composite action, un output declara `value: ${{ steps.build.outputs.artifact-path }}` pero el step `build` no escribe ese key en `$GITHUB_OUTPUT`. ¿Qué ocurre?

- A) La action falla con error de output no definido
- **B) El output se propaga como cadena vacía al workflow llamante** ✅
- C) GitHub usa el valor del input con el mismo nombre como fallback
- D) El job falla automáticamente por output nulo

*A es incorrecta*: un output vacío no produce error. *C y D son incorrectas*: no hay fallback ni fallo automático por output vacío.

---

**Pregunta 2.** ¿Cuál es el valor correcto de `runs.using` para una JavaScript action que usa Node.js 20?

- A) `'javascript'`
- **B) `'node20'`** ✅
- C) `'nodejs'`
- D) `'node'`

*A, C y D son incorrectas*: el único valor válido para Node.js 20 es la cadena literal `'node20'`.

---

**Ejercicio práctico.** Escribe el `action.yml` mínimo válido para una Docker container action que: (1) se llama "Security Scanner", (2) acepta un input `severity` con default `"HIGH"`, (3) usa el `Dockerfile` del repositorio como imagen.

```yaml
name: Security Scanner
description: Escanea vulnerabilidades de seguridad en el proyecto

inputs:
  severity:
    description: Nivel mínimo de severidad a reportar
    required: false
    default: "HIGH"

runs:
  using: docker
  image: Dockerfile
  args:
    - ${{ inputs.severity }}
```

---

← [3.1 Tipos de GitHub Actions (JavaScript, Docker, Composite)](gha-action-tipos.md) | [Índice](README.md) | [3.3 Inputs y outputs de una action](gha-action-inputs-outputs.md) →
