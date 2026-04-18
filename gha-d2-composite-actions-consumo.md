> **Navegación:** ← [2.8.2 Reusable workflows: restricciones y comparativa](gha-d2-reusable-workflows-avanzado.md) | → [2.10 Deshabilitar y eliminar workflows](gha-d2-deshabilitar-workflows.md)

# 2.9 Composite actions: consumo y diferencias con reusable workflows

## Qué problema resuelve una composite action

Cuando varios jobs o workflows repiten los mismos pasos — por ejemplo, configurar el entorno, instalar dependencias o ejecutar un script de validación — la duplicación aumenta el coste de mantenimiento. Una **composite action** permite agrupar esos pasos repetidos en un único fichero `action.yml` y referenciarlos con `uses:` desde cualquier job, igual que se hace con una action pública de Marketplace.

La diferencia clave respecto a un reusable workflow es que la composite action no necesita infraestructura propia: sus pasos se ejecutan dentro del runner del job que la llama. No se crea un job separado, no hay un runner nuevo, y el job caller mantiene el control completo del entorno de ejecución.

## Cuándo elegir una composite action sobre un reusable workflow

La regla práctica es: usa una composite action cuando quieras encapsular **pasos**, y usa un reusable workflow cuando quieras encapsular un **job completo** (con su propio runner, permisos y contexto aislado).

Una composite action es la elección correcta cuando los pasos deben compartir variables de entorno, artefactos montados en el runner, o herramientas ya instaladas en el sistema. En cambio, si necesitas un runner con una configuración distinta o un contexto de permisos diferente para esa lógica, el reusable workflow es más adecuado.

## Definición: action.yml con runs.using: composite

Una composite action se define en un fichero `action.yml` (o `action.yaml`) ubicado en la raíz de un repositorio o en cualquier subdirectorio. El campo que la distingue de otros tipos de action es `runs.using: composite`.

Los campos principales del fichero son:

| Campo | Descripción |
|---|---|
| `name` | Nombre descriptivo de la action |
| `description` | Texto explicativo visible en Marketplace o al leerlo |
| `inputs` | Parámetros de entrada con `description`, `required` y `default` opcionales |
| `outputs` | Valores que la action expone al job caller, con `value` apuntando a un step output |
| `runs.using` | Debe ser exactamente `composite` |
| `runs.steps` | Lista de pasos (cada uno puede usar `run:` o `uses:`) |

> **F1 — regla de contenido:** Cada sección arranca con párrafo de contexto antes de cualquier tabla, ejemplo o bloque de código.

## Inputs y outputs en composite actions

Los inputs de una composite action funcionan de forma similar a los inputs de un workflow, pero se consumen dentro de los pasos de la action mediante la expresión `${{ inputs.nombre }}`. Cada input puede declarar si es obligatorio (`required: true`) y un valor por defecto.

Los outputs permiten que el job caller lea valores calculados dentro de la composite action. Cada output debe declarar un campo `value` que apunta a la salida de uno de sus pasos internos, usando la sintaxis `${{ steps.id-del-step.outputs.nombre }}`. En el job caller, el valor se lee como `${{ steps.id-del-uses-step.outputs.nombre }}`.

## Ejemplo completo: job que usa una composite action

El siguiente ejemplo muestra la estructura completa. Primero, la composite action definida en `actions/setup-and-lint/action.yml` dentro del mismo repositorio. Segundo, el workflow que la consume.

**Fichero: `.github/actions/setup-and-lint/action.yml`**

```yaml
name: Setup Node and Lint
description: Instala dependencias de Node.js y ejecuta el linter, devolviendo el número de advertencias.

inputs:
  node-version:
    description: Versión de Node.js a usar
    required: false
    default: '20'
  working-directory:
    description: Directorio donde ejecutar los comandos
    required: false
    default: '.'

outputs:
  lint-warnings:
    description: Número de advertencias devueltas por el linter
    value: ${{ steps.run-lint.outputs.warnings }}

runs:
  using: composite
  steps:
    - name: Configurar Node.js
      uses: actions/setup-node@v4
      with:
        node-version: ${{ inputs.node-version }}

    - name: Instalar dependencias
      shell: bash
      run: |
        cd ${{ inputs.working-directory }}
        npm ci

    - name: Ejecutar linter
      id: run-lint
      shell: bash
      run: |
        cd ${{ inputs.working-directory }}
        WARNING_COUNT=$(npm run lint -- --format json 2>/dev/null | jq '[.[] | .warningCount] | add // 0')
        echo "warnings=${WARNING_COUNT}" >> "$GITHUB_OUTPUT"
```

**Fichero: `.github/workflows/ci.yml`**

```yaml
name: CI

on:
  push:
    branches: [main]
  pull_request:

jobs:
  lint-job:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout del código
        uses: actions/checkout@v4

      - name: Ejecutar setup y lint
        id: lint
        uses: ./.github/actions/setup-and-lint
        with:
          node-version: '20'
          working-directory: './frontend'

      - name: Mostrar resultado del linter
        shell: bash
        run: |
          echo "Advertencias detectadas: ${{ steps.lint.outputs.lint-warnings }}"
```

Observa que el job `lint-job` tiene un único runner (`ubuntu-latest`). La composite action no añade ningún runner nuevo: todos sus pasos se ejecutan en el mismo contexto que el resto del job.

## Cómo referenciar una composite action

La sintaxis de referencia depende de dónde esté alojada la composite action.

| Ubicación | Sintaxis `uses:` | Nota |
|---|---|---|
| Mismo repositorio, subdirectorio | `uses: ./.github/actions/nombre` | La ruta es relativa a la raíz del repo |
| Repositorio externo, raíz | `uses: owner/repo@ref` | `ref` puede ser tag, rama o SHA |
| Repositorio externo, subdirectorio | `uses: owner/repo/path/a/action@ref` | Útil en repos monorepo de actions |

Cuando la composite action está en el mismo repositorio, la referencia `./.github/actions/nombre` apunta al directorio que contiene el `action.yml`. El checkout del código debe haberse ejecutado antes para que la ruta local sea válida.

## Composite action vs reusable workflow: tabla comparativa

La siguiente tabla resume las diferencias operativas más relevantes para el examen. La tabla de tres columnas completa (composite / reusable / starter workflow) está en [gha-d2-reusable-workflows-avanzado.md](gha-d2-reusable-workflows-avanzado.md).

| Característica | Composite action | Reusable workflow |
|---|---|---|
| Runner propio | No — usa el runner del job caller | Sí — crea su propio job |
| Contexto de ejecución | Compartido con el job caller | Aislado en un job separado |
| Anidamiento máximo | 10 niveles | 4 niveles |
| Acceso a secrets del caller | Hereda automáticamente el entorno | Requiere pasar secrets explícitamente |
| Visibilidad en el log | Sus pasos aparecen dentro del job caller | Aparece como job independiente en el grafo |
| Cuándo usar | Encapsular pasos reutilizables dentro de un job | Encapsular jobs completos o pipelines |

## Buenas y malas prácticas

Las siguientes prácticas están ordenadas por impacto en mantenibilidad y claridad del examen.

**Buena práctica — declarar siempre `shell` en cada paso `run`.**
Los pasos `run` dentro de una composite action no tienen un shell por defecto heredado del runner. Omitir `shell:` causa un error de validación. Incluye siempre `shell: bash` (o `pwsh` en Windows) en cada paso que use `run`.

**Mala práctica — omitir `shell` en pasos `run` de la composite action.**
Al contrario que en un workflow normal donde el shell por defecto es `bash` en Linux, dentro de `runs.steps` de una composite action el campo `shell` es obligatorio. Olvidarlo es uno de los errores más frecuentes en la certificación.

**Buena práctica — exponer outputs con IDs de step estables.**
Si el ID de un paso interno cambia (renombrado), el `value` del output deja de funcionar. Usa IDs semánticos y estables como `id: run-lint` en lugar de IDs genéricos como `id: step1`.

**Mala práctica — usar `env` a nivel de job para pasar valores a la composite action.**
Pasar valores a través de variables de entorno del job en lugar de usar `inputs` declarados crea acoplamiento implícito. Si la composite action se mueve a otro repositorio, los consumidores no tienen documentación de qué variables espera.

**Buena práctica — versionar la composite action con tags semánticos.**
Cuando la composite action vive en un repositorio externo, referenciarla por tag (`@v1.2.0`) en lugar de por rama (`@main`) evita cambios inesperados en los workflows consumidores.

**Mala práctica — incluir lógica de negocio compleja directamente en `run` de la composite action.**
Si un paso `run` supera las 20-30 líneas, es señal de que la lógica debería estar en un script versionado en el repositorio. La composite action debe orquestar, no implementar.

## Verificación y práctica

Las siguientes preguntas están formuladas al estilo del examen GH-200.

**Pregunta 1.** Un job tiene tres pasos: checkout, instalación de dependencias y ejecución de tests. Decides extraer los tres pasos a una composite action. ¿Cuántos runners se usarán cuando el job ejecute la composite action?

> **Respuesta:** Uno. La composite action corre en el mismo runner del job caller. No se asigna ningún runner adicional.

**Pregunta 2.** En un fichero `action.yml` de tipo composite, defines un paso `run` sin el campo `shell`. ¿Qué ocurre?

> **Respuesta:** El workflow falla con un error de validación. En pasos `run` dentro de una composite action, el campo `shell` es obligatorio.

**Pregunta 3.** Tu composite action expone un output llamado `result`. El job caller lo referencia como `${{ steps.my-action.outputs.result }}`. ¿Qué campo del `action.yml` conecta ese nombre con el valor real?

> **Respuesta:** El campo `outputs.result.value`, que debe apuntar a la salida de un step interno mediante `${{ steps.<step-id>.outputs.<output-name> }}`.

**Ejercicio práctico.** Crea una composite action llamada `validate-version` en `.github/actions/validate-version/action.yml`. La action debe aceptar un input `version` (obligatorio) y verificar que sigue el formato semver (`X.Y.Z`) usando un paso `run` con bash. Si no sigue el formato, el paso debe terminar con `exit 1`. La action debe exponer un output `is-valid` con el valor `true` si la validación pasa. Escribe también un workflow mínimo que use la action y pase `version: '1.2.3'`.

> **Navegación:** ← [2.8.2 Reusable workflows: restricciones y comparativa](gha-d2-reusable-workflows-avanzado.md) | → [2.10 Deshabilitar y eliminar workflows](gha-d2-deshabilitar-workflows.md)
