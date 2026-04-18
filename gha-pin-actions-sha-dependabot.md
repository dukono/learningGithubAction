# 5.5.2 Pin de Actions a SHA — Dependabot y Políticas de Organización

← [5.5.1 Pin SHA — Conceptos](gha-pin-actions-sha-conceptos.md) | [Índice](README.md) | [5.6 Enforcement de Policies](gha-enforcement-policies-seguridad.md) →

---

Mantener los SHA pins actualizados manualmente en workflows con muchas actions es una tarea costosa. **Dependabot** resuelve este problema automatizando la detección de nuevas versiones y la creación de PRs con los SHAs actualizados. Complementariamente, las políticas de organización en GitHub Enterprise permiten imponer el SHA pinning como requisito técnico, no solo como convención de equipo.

> [CONCEPTO] Dependabot para GitHub Actions no solo actualiza las referencias de versión: actualiza el SHA de commit al correspondiente a la nueva versión, manteniendo el comentario del tag legible que se añadió originalmente.

## Dependabot para GitHub Actions — Configuración Base

Dependabot se configura mediante el archivo `.github/dependabot.yml` en el repositorio. Para monitorizar actions, se usa `package-ecosystem: github-actions`. A diferencia de otros ecosistemas, no requiere especificar un `directory` de manifiestos porque Dependabot escanea automáticamente todos los archivos en `.github/workflows/`.

El campo `schedule.interval` acepta `daily`, `weekly` o `monthly`. Para workflows de producción con actions críticas se recomienda `weekly`; para repositorios con muchos workflows y alta frecuencia de actualizaciones de actions puede generar mucho ruido con `daily`.

> [EXAMEN] El valor de `package-ecosystem` para GitHub Actions es exactamente `github-actions` (con guión). Un error tipográfico en este campo hace que Dependabot no procese los workflows.

## Cómo Dependabot Actualiza los SHA Pins

Cuando Dependabot detecta una nueva versión de una action, crea un PR que modifica la línea `uses:` en el workflow. Si la línea tenía un SHA con comentario de tag (formato recomendado), Dependabot actualiza el SHA al commit correspondiente a la nueva versión y actualiza también el comentario. Por ejemplo:

Antes: `uses: actions/checkout@11bd71901bbe5b1630ceea73d27597364c9af683 # v4.2.2`
Después: `uses: actions/checkout@<nuevo-sha> # v4.3.0`

Si la referencia original era un tag sin SHA (por ejemplo `@v4`), Dependabot puede proponer actualizarla a un SHA. Este comportamiento es uno de los argumentos para adoptar SHA pins desde el inicio: Dependabot los mantiene actualizados sin esfuerzo manual.

## Actualización Manual de SHA Pins

Cuando no se tiene Dependabot configurado o se necesita actualizar un SHA específico fuera del ciclo automático, el proceso manual es el siguiente. Primero, identificar la nueva versión de la action. Segundo, obtener el SHA del commit correspondiente con `gh api` o `git ls-remote`. Tercero, actualizar la línea `uses:` en el workflow con el nuevo SHA y el comentario del tag actualizado.

> [ADVERTENCIA] Nunca actualizar el comentario del tag sin actualizar el SHA, ni el SHA sin actualizar el comentario. Una discrepancia entre ambos genera confusión en revisiones de código y puede enmascarar un SHA incorrecto.

## Política de Organización — Require Pinned Versions

En GitHub Enterprise, los administradores de organización pueden configurar políticas que restringen qué actions pueden usarse en los repositorios de la organización. La política **"Require pinned versions"** (o equivalente en la configuración de Actions de la organización) bloquea la ejecución de workflows que referencien actions sin SHA pin.

Esta política se aplica desde `Organization Settings > Actions > General > Actions permissions`. Combinada con la opción de permitir solo actions de la organización propia o de creadores verificados, forma una defensa en profundidad contra supply chain attacks.

> [EXAMEN] La diferencia entre SHA pinning para seguridad (objetivo D5) y tag pinning para comodidad es que el primero garantiza inmutabilidad técnica; el segundo es solo una convención de equipo sin garantía técnica.

## Ejemplo central

El siguiente ejemplo muestra una configuración completa de Dependabot para GitHub Actions con opciones de schedule, reviewers y límite de PRs abiertos simultáneos, seguido de un workflow que combina SHA pins con comentarios de tag.

```yaml
# .github/dependabot.yml
version: 2
updates:
  # Mantener actions pineadas a SHA actualizadas automáticamente
  - package-ecosystem: github-actions
    directory: /
    schedule:
      interval: weekly
      day: monday
      time: "09:00"
      timezone: "Europe/Madrid"
    # Limite de PRs abiertos simultáneos para no saturar el equipo
    open-pull-requests-limit: 5
    # Revisor obligatorio para PRs de actualizacion de actions
    reviewers:
      - security-team
    # Agrupar actualizaciones de actions para reducir el numero de PRs
    groups:
      github-actions:
        patterns:
          - "actions/*"
          - "github/*"
      third-party-actions:
        patterns:
          - "*"
        exclude-patterns:
          - "actions/*"
          - "github/*"
    # Prefijo del mensaje de commit para trazabilidad
    commit-message:
      prefix: "chore(deps)"
      include: scope
```

```yaml
# .github/workflows/ci.yml
# Workflow con SHA pins mantenidos por Dependabot
name: CI Pipeline

on:
  push:
    branches: [main, develop]
  pull_request:

permissions:
  contents: read

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      # SHA pins: Dependabot actualizara estos SHAs cuando salgan nuevas versiones
      - name: Checkout
        uses: actions/checkout@11bd71901bbe5b1630ceea73d27597364c9af683 # v4.2.2

      - name: Setup Node.js
        uses: actions/setup-node@39370e3970a6d050c480ffad4ff0ed4d3fdee5af # v4.1.0
        with:
          node-version: '20'
          cache: 'npm'

      - name: Install and test
        run: npm ci && npm test

      - name: Upload artifacts
        uses: actions/upload-artifact@6f51ac03b9356f520e9adb1b1b7802705f340c2b # v4.5.0
        with:
          name: test-results
          path: test-results/
          retention-days: 14
```

## Tabla de elementos clave

Los siguientes parámetros del archivo `dependabot.yml` son los más relevantes para la gestión de SHA pins en GitHub Actions.

| Campo | Tipo | Obligatorio | Default | Descripción |
|---|---|---|---|---|
| `package-ecosystem` | string | Si | — | Debe ser `github-actions` para workflows |
| `directory` | string | Si | — | Usar `/` para escanear todos los workflows |
| `schedule.interval` | string | Si | — | `daily`, `weekly` o `monthly` |
| `schedule.day` | string | No | Aleatorio | Dia de la semana para `weekly` |
| `schedule.time` | string | No | Aleatorio | Hora en formato HH:MM |
| `schedule.timezone` | string | No | UTC | Zona horaria para el schedule |
| `open-pull-requests-limit` | integer | No | 5 | Maximo de PRs abiertos simultaneamente |
| `reviewers` | lista | No | — | Usuarios o equipos revisores de los PRs |
| `groups` | mapa | No | — | Agrupa actualizaciones en un unico PR |
| `commit-message.prefix` | string | No | — | Prefijo para los commits de Dependabot |

## Buenas y malas prácticas

**Hacer:** configurar Dependabot con `package-ecosystem: github-actions` desde el inicio del proyecto — razón: el coste de actualizar SHAs manualmente crece con el numero de actions y workflows; automatizarlo elimina la deuda de mantenimiento.

**Hacer:** configurar `reviewers` en Dependabot para PRs de actions — razón: los PRs de actualizacion de actions deben revisarse para verificar el changelog y detectar cambios de comportamiento antes de mergear.

**Hacer:** usar `groups` para agrupar actualizaciones de actions del mismo proveedor en un solo PR — razón: reduce el ruido de notificaciones y permite revisar actualizaciones relacionadas juntas.

**Evitar:** mergear PRs de Dependabot para actions sin revisar el changelog — razón: aunque el SHA garantiza inmutabilidad del codigo a ejecutar, una actualizacion de version puede introducir cambios de comportamiento que rompan el workflow.

**Evitar:** deshabilitar Dependabot para reducir el numero de PRs sin configurar `open-pull-requests-limit` — razón: la alternativa correcta es limitar el numero de PRs abiertos y agrupar actualizaciones, no desactivar la herramienta de seguridad.

**Evitar:** depender unicamente de la politica de organizacion sin SHA pins en el codigo — razón: las politicas de organizacion pueden desactivarse o cambiar; el SHA pin en el workflow es una garantia tecnica que persiste independientemente de la politica.

## Verificación y práctica

**Pregunta 1:** ¿Qué valor debe tener el campo `package-ecosystem` en `dependabot.yml` para que Dependabot actualice las actions referenciadas en los workflows de GitHub Actions?

*Respuesta:* El valor debe ser `github-actions` (con guión). Dependabot usa este valor para identificar el ecosistema y saber que debe escanear los archivos en `.github/workflows/`. Valores como `actions`, `github_actions` o `github actions` no son validos y Dependabot ignorara la configuracion.

**Pregunta 2:** Un equipo tiene SHA pins en todos sus workflows. Dependabot esta configurado con `schedule.interval: weekly`. Sale una vulnerabilidad critica en una action que usan. ¿Que deben hacer para actualizar el SHA inmediatamente sin esperar al ciclo semanal?

*Respuesta:* Tienen dos opciones: (1) actualizar manualmente el SHA en el workflow buscando el nuevo SHA del commit correspondiente a la version parcheada y creando un PR; (2) usar la opcion de Dependabot en la UI de GitHub para forzar una comprobacion inmediata desde `Insights > Dependency graph > Dependabot`. La opcion manual es mas rapida en una emergencia.

**Pregunta 3:** ¿En que se diferencia la configuracion de politica de organizacion "Require pinned versions" del SHA pinning en el codigo del workflow?

*Respuesta:* La politica de organizacion es una restriccion externa que impide ejecutar workflows con references no pineadas, pero puede ser cambiada por un administrador. El SHA pinning en el codigo del workflow es una garantia tecnica inmutable desde el punto de vista del workflow: aunque la politica cambie o desaparezca, el codigo siempre referenciara el mismo commit. La defensa en profundidad requiere ambas capas.

**Ejercicio:** Escribe un `.github/dependabot.yml` que configure Dependabot para actualizar actions semanalmente los lunes, con un maximo de 3 PRs abiertos, revisados por el equipo `platform-security`, y que agrupe todas las actions de `actions/*` en un solo PR.

*Solución:*

```yaml
version: 2
updates:
  - package-ecosystem: github-actions
    directory: /
    schedule:
      interval: weekly
      day: monday
    open-pull-requests-limit: 3
    reviewers:
      - platform-security
    groups:
      github-official-actions:
        patterns:
          - "actions/*"
```

---

← [5.5.1 Pin SHA — Conceptos](gha-pin-actions-sha-conceptos.md) | [Índice](README.md) | [5.6 Enforcement de Policies](gha-enforcement-policies-seguridad.md) →
