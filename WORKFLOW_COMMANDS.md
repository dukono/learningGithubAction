# ⚙️ GitHub Actions: Workflow Commands

## Índice

1. [¿Qué son los Workflow Commands?](#1-qué-son-los-workflow-commands)
2. [Comandos de Logging](#2-comandos-de-logging)
   - [::debug::](#debug)
   - [::notice::](#notice)
   - [::warning::](#warning)
   - [::error::](#error)
3. [Comandos de Agrupación](#3-comandos-de-agrupación)
4. [Comandos de Seguridad](#4-comandos-de-seguridad)
   - [::add-mask::](#add-mask)
   - [::stop-commands::](#stop-commands)
5. [Archivos de Entorno Especiales](#5-archivos-de-entorno-especiales)
   - [GITHUB_OUTPUT](#github_output)
   - [GITHUB_ENV](#github_env)
   - [GITHUB_PATH](#github_path)
   - [GITHUB_STEP_SUMMARY](#github_step_summary)
   - [GITHUB_STATE](#github_state)
6. [Equivalentes en Actions JavaScript](#6-equivalentes-en-actions-javascript)
7. [Preguntas de Examen](#7-preguntas-de-examen)

---

## 1. ¿Qué son los Workflow Commands?

Los **workflow commands** son mensajes especiales que un step puede escribir en `stdout` para comunicarse con el runner. El runner los interpreta y ejecuta acciones: crear anotaciones, enmascarar valores, agrupar logs, etc.

**Formato general:**
```
::nombre-comando param1=valor1,param2=valor2::contenido
```

> ⚠️ **Importante**: Los workflow commands se procesan **en tiempo de ejecución**, no en tiempo de parseo del YAML. Se pueden emitir desde cualquier script (`run:`) o desde una Action JavaScript.

---

## 2. Comandos de Logging

Permiten crear **anotaciones** en la UI de GitHub (pestaña "Summary" y diff de PR).

### `::debug::`

Imprime un mensaje en el log solo si el **debug logging** está activado.

```yaml
- name: Log de debug
  run: echo "::debug::Valor de la variable: $MY_VAR"
```

Para activar debug logging: crear secret `ACTIONS_STEP_DEBUG = true` o re-ejecutar el workflow con la opción "Enable debug logging".

---

### `::notice::`

Crea una anotación de tipo **notice** (informativa) visible en la UI.

```yaml
- name: Anotación notice
  run: echo "::notice file=app.js,line=10,col=5::Función deprecada, usar v2"
```

**Parámetros opcionales:**
| Parámetro | Descripción |
|---|---|
| `file` | Archivo al que hace referencia la anotación |
| `line` | Línea dentro del archivo |
| `endLine` | Línea final (para rangos) |
| `col` | Columna inicial |
| `endColumn` | Columna final |
| `title` | Título de la anotación |

---

### `::warning::`

Crea una anotación de tipo **warning**. Aparece en amarillo en la UI.

```yaml
- name: Anotación warning
  run: |
    echo "::warning file=config.yml,line=45::Valor de timeout muy alto"
```

---

### `::error::`

Crea una anotación de tipo **error**. Aparece en rojo. **No detiene la ejecución del step** — solo crea la anotación.

```yaml
- name: Anotación de error
  run: |
    if [ "$DEPLOY_ENV" = "prod" ] && [ -z "$APPROVAL" ]; then
      echo "::error title=Approval Required::Deployment a producción requiere aprobación"
      exit 1  # Esto sí detiene el step
    fi
```

> **Diferencia clave**: `::error::` crea una anotación visual. Para **fallar el step** debes usar `exit 1` (en bash) o `core.setFailed()` (en JS).

---

## 3. Comandos de Agrupación

### `::group::` / `::endgroup::`

Crea secciones **plegables** en los logs de la UI.

```yaml
- name: Instalar dependencias
  run: |
    echo "::group::Instalando dependencias de producción"
    npm install --production
    echo "::endgroup::"

    echo "::group::Instalando dependencias de desarrollo"
    npm install --only=dev
    echo "::endgroup::"

    echo "Instalación completa"
```

Los grupos se pueden **anidar**. Todo lo que se imprime entre `::group::` y `::endgroup::` queda colapsado por defecto en la UI.

---

## 4. Comandos de Seguridad

### `::add-mask::`

**Enmascara un valor** en todos los logs subsiguientes de ese workflow. El valor se reemplaza por `***`.

```yaml
- name: Obtener token dinámico
  run: |
    TOKEN=$(curl -s https://api.example.com/token | jq -r '.access_token')
    echo "::add-mask::$TOKEN"         # A partir de aquí TOKEN queda enmascarado
    echo "TOKEN=$TOKEN" >> $GITHUB_ENV  # Aunque lo guardemos, el valor sigue enmascarado
```

> **Cuándo usar**: Los `secrets` del repositorio se enmascaran automáticamente. Usa `::add-mask::` para valores secretos que **obtienes dinámicamente** durante la ejecución (tokens generados, claves efímeras, etc.).

**Comportamiento importante:**
- La máscara aplica desde ese momento en adelante, **no retroactivamente**
- Si el valor enmascarado aparece en un log previo a `::add-mask::`, no queda enmascarado
- La máscara se aplica en todos los steps siguientes del mismo job

---

### `::stop-commands::`

Detiene temporalmente el procesamiento de workflow commands. Útil cuando el output de un script contiene texto con formato `::comando::` que no quieres que sea interpretado.

```yaml
- name: Output que contiene ::texto:: literal
  run: |
    echo "::stop-commands::mi-token-unico-123"
    # Todo lo de aquí no se interpreta como workflow command:
    echo "::debug::esto NO es un debug command, es texto literal"
    echo "::error::esto tampoco es un error"
    echo "::mi-token-unico-123::"  # Reactiva el procesamiento
    echo "::debug::esto SÍ es un debug command de nuevo"
```

El token puede ser cualquier string único. Para reactivar: `::token::` (sin contenido).

---

## 5. Archivos de Entorno Especiales

El runner pone a disposición **archivos especiales** a través de variables de entorno. Escribir en ellos es el mecanismo moderno (desde 2022) para comunicarse con el runner, reemplazando los comandos `::set-output::` y `::add-path::` que están deprecados.

### GITHUB_OUTPUT

Pasa datos entre steps del **mismo job**.

```yaml
- name: Generar valor
  id: gen
  run: |
    VERSION=$(git describe --tags --abbrev=0)
    echo "version=$VERSION" >> $GITHUB_OUTPUT
    echo "timestamp=$(date -u +%Y%m%d%H%M%S)" >> $GITHUB_OUTPUT

- name: Usar el valor
  run: |
    echo "Versión: ${{ steps.gen.outputs.version }}"
    echo "Timestamp: ${{ steps.gen.outputs.timestamp }}"
```

**Valores multilínea:**
```yaml
- run: |
    echo "CHANGELOG<<EOF" >> $GITHUB_OUTPUT
    git log --oneline -5
    echo "EOF" >> $GITHUB_OUTPUT
```

---

### GITHUB_ENV

Define variables de entorno disponibles en todos los steps **siguientes** del mismo job.

```yaml
- name: Configurar entorno
  run: |
    echo "APP_VERSION=2.1.0" >> $GITHUB_ENV
    echo "DEPLOY_REGION=eu-west-1" >> $GITHUB_ENV

- name: Usar variables
  run: |
    echo "Desplegando versión $APP_VERSION en $DEPLOY_REGION"
```

> **Diferencia con GITHUB_OUTPUT**: `GITHUB_ENV` hace la variable disponible como variable de entorno directa (`$VAR`), no como expresión `${{ steps.id.outputs.var }}`.

---

### GITHUB_PATH

Añade directorios al `PATH` para steps siguientes.

```yaml
- name: Añadir herramienta al PATH
  run: |
    mkdir -p $HOME/.local/bin
    cp ./my-tool $HOME/.local/bin/
    echo "$HOME/.local/bin" >> $GITHUB_PATH

- name: Usar la herramienta
  run: my-tool --version  # Funciona porque está en PATH
```

---

### GITHUB_STEP_SUMMARY

Añade contenido Markdown al **resumen del workflow** (pestaña Summary en GitHub).

```yaml
- name: Generar resumen de tests
  run: |
    echo "## Resultados de Tests 🧪" >> $GITHUB_STEP_SUMMARY
    echo "" >> $GITHUB_STEP_SUMMARY
    echo "| Suite | Passed | Failed |" >> $GITHUB_STEP_SUMMARY
    echo "|-------|--------|--------|" >> $GITHUB_STEP_SUMMARY
    echo "| Unit  | 42     | 0      |" >> $GITHUB_STEP_SUMMARY
    echo "| E2E   | 15     | 2      |" >> $GITHUB_STEP_SUMMARY
    echo "" >> $GITHUB_STEP_SUMMARY
    echo "> Tests E2E fallidos en ambiente de staging, no bloquean merge." >> $GITHUB_STEP_SUMMARY
```

- Máximo **1 MiB** de contenido por step summary
- El contenido se **acumula** a lo largo del workflow
- Soporta Markdown completo (tablas, listas, código, etc.)

---

### GITHUB_STATE

Comparte estado entre las fases **pre**, **main** y **post** de una **Action JavaScript**.

```javascript
// En la fase `pre:` de action.yml
const core = require('@actions/core');
core.saveState('myToken', 'abc123');  // Escribe en GITHUB_STATE

// En la fase `post:` de action.yml
const token = core.getState('myToken');  // Lee de GITHUB_STATE
console.log(`Token recuperado: ${token}`);
```

**Cuándo se usa**: Cuando una action necesita hacer limpieza en `post:` con datos que obtuvo en `main:` (por ejemplo, guardar un token para revocar en la fase de limpieza).

> `GITHUB_STATE` solo funciona entre fases de la **misma action**, no entre diferentes actions o steps.

---

## 6. Equivalentes en Actions JavaScript

La librería `@actions/core` proporciona funciones que emiten estos workflow commands internamente:

| Workflow Command | Equivalente JS |
|---|---|
| `::debug::msg` | `core.debug('msg')` |
| `::notice::msg` | `core.notice('msg')` |
| `::warning::msg` | `core.warning('msg')` |
| `::error::msg` | `core.error('msg')` |
| `::add-mask::val` | `core.setSecret('val')` |
| `::group::name` | `core.startGroup('name')` |
| `::endgroup::` | `core.endGroup()` |
| `echo "key=val" >> $GITHUB_OUTPUT` | `core.setOutput('key', 'val')` |
| `echo "VAR=val" >> $GITHUB_ENV` | `core.exportVariable('VAR', 'val')` |
| `echo "path" >> $GITHUB_PATH` | `core.addPath('path')` |
| `core.saveState('k', 'v')` | `core.saveState('k', 'v')` |
| `core.getState('k')` | `core.getState('k')` |
| Fallar el step | `core.setFailed('mensaje')` |

---

## 7. Preguntas de Examen

**P: ¿Qué diferencia hay entre `::error::` y `exit 1`?**
> `::error::` crea una anotación visual en la UI pero no detiene la ejecución. `exit 1` falla el step y (según `continue-on-error`) puede fallar el job.

**P: Un step genera un token dinámicamente. ¿Cómo evitar que aparezca en los logs?**
> Usar `::add-mask::$TOKEN` inmediatamente después de obtenerlo, antes de cualquier `echo` o comando que lo exponga.

**P: ¿Para qué sirve `GITHUB_STATE`?**
> Para compartir datos entre las fases `pre:`, `main:` y `post:` de una Action JavaScript. No funciona entre steps normales ni entre distintas actions.

**P: ¿Qué comando reemplaza al deprecado `::set-output name=foo::bar`?**
> `echo "foo=bar" >> $GITHUB_OUTPUT`

**P: ¿Cómo habilitar los mensajes `::debug::` en los logs?**
> Crear el secret `ACTIONS_STEP_DEBUG` con valor `true`, o al re-ejecutar el workflow marcar "Enable debug logging".

**P: ¿Cuál es la diferencia entre `GITHUB_ENV` y `GITHUB_OUTPUT`?**
> `GITHUB_ENV` expone valores como variables de entorno `$VAR` en steps siguientes. `GITHUB_OUTPUT` expone valores accesibles como `${{ steps.id.outputs.key }}` y también pueden usarse entre jobs (via `needs`).
