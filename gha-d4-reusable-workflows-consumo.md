# 4.2.2 Reusable Workflows: consumo, secrets: inherit, anidamiento y visibilidad cross-org

← [4.2.1 Reusable Workflows: autoría](gha-d4-reusable-workflows-autoria.md) | [Índice](README.md) | [4.3.1 Políticas allow-list](gha-d4-politicas-allow-list.md) →

---

La sección anterior cubrió la perspectiva del **autor del callee**: cómo declarar `on: workflow_call`, inputs, secrets y outputs. Esta sección cubre la perspectiva del **caller**: cómo invocar un reusable workflow, cómo pasarle datos, cómo leer sus outputs, y qué restricciones impone la plataforma en cuanto a anidamiento y visibilidad entre organizaciones.

> [CONCEPTO] En GH-200 la distinción entre `uses:` a nivel de **job** y `uses:` dentro de un **step** es recurrente. `uses:` en un job invoca un reusable workflow (callee con `on: workflow_call`); `uses:` en un step invoca una action (con `action.yml`). Las dos formas tienen sintaxis casi idéntica pero semánticas completamente distintas.

## uses: a nivel de job vs. uses: en step

La clave `uses:` aparece en dos lugares del YAML de un workflow y su comportamiento varía según dónde se use.

Cuando `uses:` aparece en la declaración de un job (al mismo nivel que `runs-on:`), invoca un reusable workflow completo. El callee corre en sus propios runners, con su propio contexto de ejecución, y puede contener múltiples jobs. En este caso `runs-on:` se **omite** en el caller para ese job: el runner lo define el callee.

Cuando `uses:` aparece dentro de un `step`, invoca una action (JavaScript, Docker o composite). La action se ejecuta **en el mismo runner** del job del caller, dentro del step donde se declaró. El job del caller mantiene su `runs-on:`.

```yaml
# uses: a nivel de JOB — invoca un reusable workflow
jobs:
  build:
    uses: my-org/shared/.github/workflows/build.yml@v2   # ← job-level
    with:
      environment: staging

# uses: dentro de un STEP — invoca una action
jobs:
  lint:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4                         # ← step-level
```

> [EXAMEN] Si el examen muestra un job con `uses:` y pregunta qué tipo de reutilización es, la respuesta es "reusable workflow". Si muestra un step con `uses:`, la respuesta es "action". Un job con `uses:` no puede tener `steps:` ni `runs-on:`.

## Sintaxis de invocación: with: y secrets:

Para pasar datos a un reusable workflow el caller usa las claves `with:` (inputs) y `secrets:` (secrets) dentro del bloque del job. Las claves de `with:` deben coincidir exactamente con los nombres declarados en `on.workflow_call.inputs` del callee; de lo contrario la validación falla antes de ejecutar.

```yaml
jobs:
  invoke-deploy:
    uses: my-org/platform/.github/workflows/deploy.yml@v3
    with:
      environment: production
      version: "2.1.0"
      run_smoke_tests: true
    secrets:
      DEPLOY_KEY: ${{ secrets.PROD_DEPLOY_KEY }}
      NOTIFY_WEBHOOK: ${{ secrets.SLACK_WEBHOOK }}
```

Si el callee declara un input con `required: true` y el caller no lo incluye en `with:`, GitHub reporta error de validación antes de que el workflow inicie. Si el input tiene `required: false` y un `default:`, el callee usará ese valor por defecto cuando el caller lo omita.

## secrets: inherit vs. secrets explícitos

Existen dos formas de propagar secrets al callee. La elección entre ambas determina cuántos secrets puede ver el callee y es un punto de seguridad evaluado en GH-200.

**Secrets explícitos** — el caller lista cada secret que el callee necesita, mapeando el nombre local al nombre que el callee espera. Solo los secrets listados llegan al callee. Permite remapear nombres: `DEPLOY_KEY: ${{ secrets.PROD_SSH_KEY }}` expone `DEPLOY_KEY` al callee aunque en el caller el secret se llame `PROD_SSH_KEY`.

**`secrets: inherit`** — en lugar de un diccionario, el caller escribe la palabra clave `inherit`. GitHub propaga automáticamente **todos** los secrets accesibles en el contexto del caller (organización, repositorio y entorno) al callee con los mismos nombres. Es conveniente para equipos internos con alta confianza, pero viola el principio de mínimo privilegio cuando el callee es externo.

```yaml
# Opción A — explícito (mínimo privilegio)
jobs:
  deploy:
    uses: my-org/infra/.github/workflows/terraform.yml@v2
    secrets:
      TF_API_TOKEN: ${{ secrets.TF_CLOUD_TOKEN }}

# Opción B — inherit (todos los secrets del caller)
jobs:
  deploy:
    uses: my-org/infra/.github/workflows/terraform.yml@v2
    secrets: inherit
```

> [ADVERTENCIA] `secrets: inherit` con un callee de un repositorio público o de una organización externa expone potencialmente credenciales sensibles. El callee puede leer cualquier secret que el caller tenga disponible. Usar siempre secrets explícitos en ese escenario.

## Acceder a outputs del callee: jobs.[id].outputs

Los outputs que el callee declara en `on.workflow_call.outputs` son accesibles en el caller mediante la expresión `${{ needs.<job-id>.outputs.<key> }}`. Para que esto funcione, el job del caller que consume el output debe declarar el job callee en su campo `needs:`.

La cadena de propagación tiene tres niveles —todos obligatorios— y ocurre completamente dentro del callee antes de que el caller pueda leer el valor:

1. El **step** del callee escribe el valor: `echo "key=value" >> "$GITHUB_OUTPUT"`
2. El **job** del callee lo eleva en su bloque `outputs:`: `key: ${{ steps.id.outputs.key }}`
3. El **workflow** callee lo expone en `on.workflow_call.outputs`: `value: ${{ jobs.job-id.outputs.key }}`

Si cualquiera de los tres niveles falta, el caller recibirá una cadena vacía sin error explícito.

```yaml
# En el caller: consumir el output del callee
jobs:
  call-build:
    uses: my-org/shared/.github/workflows/build.yml@v2
    with:
      app: my-service

  notify:
    needs: call-build
    runs-on: ubuntu-latest
    steps:
      - name: Mostrar artefacto generado
        run: echo "Artefacto en ${{ needs.call-build.outputs.artifact-path }}"
```

## Límite de anidamiento: 4 niveles

GitHub Actions permite que un reusable workflow invoque a su vez otro reusable workflow, pero impone un límite estricto de **4 niveles de anidamiento** contando el workflow raíz (caller).

```
Nivel 1 — ci.yml (caller original)
  Nivel 2 — build.yml (callee 1)
    Nivel 3 — compile.yml (callee 2)
      Nivel 4 — lint.yml  (callee 3)  ← límite máximo
```

Si se intenta un quinto nivel, la ejecución falla en tiempo de validación con el error: _"Reusable workflows can only be nested to 4 levels deep"_.

> [EXAMEN] El límite es 4 niveles en total, lo que equivale a 3 callees anidados dentro de un caller. El examen puede preguntar "¿cuántos niveles de callees se pueden anidar?" — la respuesta es **3** (dentro de un total de 4 contando el caller raíz).

La implicación de diseño es clara: si una cadena de pipelines necesita más profundidad, parte de la lógica debe implementarse con **composite actions**, que no consumen niveles de anidamiento de reusable workflows.

## Visibilidad y restricciones cross-org

La posibilidad de invocar un callee de otro repositorio depende de la visibilidad del repositorio que contiene ese callee y de la configuración de la organización.

| Repositorio del callee | Caller mismo repo | Caller misma org | Caller org externa |
|---|---|---|---|
| **Público** | Sí | Sí | Sí |
| **Internal** | Sí | Sí, si "Allow reusable workflows" está activo en la org | No |
| **Privado** | Sí | Solo si el repo callee activa "Allow reusable workflows" | No |

Para los repositorios `internal` y `private`, la organización propietaria del callee debe habilitar la opción **"Allow reusable workflows"** en la configuración de Actions (Settings → Actions → General). Sin esa configuración, el caller de otro repositorio de la misma organización recibirá un error 404 al intentar referenciar el callee.

La invocación de callees de una organización externa a la del caller no está soportada, incluso si el repositorio es público y el caller tiene tokens de acceso válidos a ese repositorio. La restricción es de plataforma, no de permisos individuales.

> [ADVERTENCIA] Las restricciones de visibilidad cross-org aplican también a los forks. Un workflow de un fork que intenta usar un callee del repositorio upstream puede fallar si el repositorio upstream es privado e interno y el fork es de un actor externo. En ese escenario, GitHub no propaga secrets a los workflows de PRs de forks por defecto.

## Contextos disponibles en el caller al invocar un callee

Cuando el caller invoca un callee con `uses:` a nivel de job, los contextos disponibles en ese bloque son más limitados que en un job normal. En particular, el contexto `env:` del caller no se propaga al callee automáticamente: las variables de entorno deben pasarse como `inputs`.

Los contextos que el caller puede usar en las claves `with:` y `secrets:` de la invocación son: `github`, `needs`, `strategy`, `matrix`, `secrets` e `inputs`. El contexto `steps` no está disponible a nivel de job (solo dentro de steps), y `env` solo está disponible si se define a nivel de workflow o job pero no se propaga.

```yaml
jobs:
  call-deploy:
    uses: my-org/platform/.github/workflows/deploy.yml@v2
    with:
      # github context: disponible
      sha: ${{ github.sha }}
      # needs context: disponible si este job tiene needs:
      version: ${{ needs.build.outputs.version }}
      # env context: NOT propagado al callee — pasar como input explícito
      node_env: production
    secrets:
      # secrets context: disponible
      API_KEY: ${{ secrets.PROD_API_KEY }}
```

## YAML completo funcional: caller con with:, secrets: inherit y output

El siguiente ejemplo muestra un workflow caller que invoca un reusable workflow de despliegue, usa `secrets: inherit` para propagar credenciales, y accede al output producido por el callee en un job de notificación posterior.

```yaml
# .github/workflows/release-pipeline.yml  (CALLER)
name: Release Pipeline

on:
  push:
    tags:
      - "v*.*.*"

jobs:
  # Job 1: invoca el reusable workflow de build y despliegue
  call-deploy:
    uses: my-org/platform/.github/workflows/deploy-reusable.yml@v4
    with:
      environment: production
      version: ${{ github.ref_name }}
      upload_artifact: true
    secrets: inherit
    # No se declara runs-on: — lo define el callee

  # Job 2: usa el output del callee para notificar
  notify:
    needs: call-deploy
    runs-on: ubuntu-latest
    steps:
      - name: Verificar deploy completado
        run: |
          echo "Deployment URL: ${{ needs.call-deploy.outputs.deploy-url }}"
          echo "Artifact path: ${{ needs.call-deploy.outputs.artifact-path }}"

      - name: Publicar summary
        run: |
          echo "## Release ${{ github.ref_name }} desplegado" >> "$GITHUB_STEP_SUMMARY"
          echo "- URL: ${{ needs.call-deploy.outputs.deploy-url }}" >> "$GITHUB_STEP_SUMMARY"
```

El callee correspondiente (en `my-org/platform`) declara `on: workflow_call` con los inputs `environment`, `version` y `upload_artifact`, acepta todos los secrets del caller vía `inherit` (declarándolos con `required: false`) y expone `deploy-url` y `artifact-path` como outputs siguiendo la cadena de tres niveles.

## Tabla de elementos clave

| Elemento | Nivel | Descripción |
|---|---|---|
| `uses: owner/repo/.github/workflows/file.yml@ref` | job (caller) | Invoca un reusable workflow externo; `ref` puede ser rama, tag o SHA |
| `uses: ./.github/workflows/file.yml` | job (caller) | Invoca un callee del mismo repositorio con ruta relativa |
| `with:` | job (caller) | Diccionario de inputs; las claves deben coincidir con `workflow_call.inputs` del callee |
| `secrets:` (explícito) | job (caller) | Lista los secrets a pasar; permite remapear nombres |
| `secrets: inherit` | job (caller) | Propaga todos los secrets del caller al callee automáticamente |
| `needs.<id>.outputs.<key>` | step o `if:` (caller) | Accede al output expuesto por el callee en `workflow_call.outputs` |
| Límite de anidamiento | plataforma | Máximo 4 niveles totales (caller + 3 callees anidados) |
| Visibilidad cross-org | plataforma | Solo posible si el callee es público; repos `internal`/`private` requieren configuración de la org |

## Buenas y malas prácticas

**Hacer:** anclar siempre la referencia del callee a un SHA completo o a un tag semántico inmutable cuando el callee es externo. Usar `@main` expone el caller a cambios no controlados en el callee.

> Buena práctica: `uses: my-org/shared/.github/workflows/build.yml@v2.1.0`
> Mala práctica: `uses: my-org/shared/.github/workflows/build.yml@main`

**Hacer:** usar `secrets: inherit` solo con callees del mismo repositorio o de la misma organización con confianza plena. Para callees públicos o de organizaciones externas, siempre usar secrets explícitos.

**Hacer:** verificar que los tres niveles de la cadena de outputs (step → job `outputs:` → `workflow_call.outputs`) están declarados en el callee antes de asumir que el valor llegará al caller.

**Hacer:** cuando la profundidad de anidamiento se acerca a 4 niveles, reemplazar el callee más profundo con una composite action. Las composite actions no consumen niveles de anidamiento de reusable workflows.

**Evitar:** declarar `runs-on:` en un job que usa `uses:` a nivel de job. El runner lo define el callee y añadir `runs-on:` en el caller produce un error de validación.

**Evitar:** pasar variables de entorno (`env:`) del caller esperando que el callee las reciba automáticamente. Deben convertirse en `inputs` y pasarse explícitamente con `with:`.

## Verificación y práctica

**Pregunta 1.** Un workflow tiene esta estructura: `ci.yml` llama a `build.yml`, que llama a `test.yml`, que llama a `scan.yml`, que intenta llamar a `report.yml`. ¿Qué ocurre al ejecutar `ci.yml`?

> Respuesta: La ejecución falla en tiempo de validación. La cadena tiene 5 niveles (ci → build → test → scan → report) y el límite de la plataforma es 4. El error indica _"Reusable workflows can only be nested to 4 levels deep"_.

**Pregunta 2.** ¿Cuál es la diferencia entre `uses:` a nivel de job y `uses:` dentro de un step?

> Respuesta: `uses:` a nivel de job invoca un reusable workflow completo que corre en sus propios runners; el job caller no puede tener `steps:` ni `runs-on:`. `uses:` dentro de un step invoca una action (JavaScript, Docker o composite) que se ejecuta en el mismo runner del job caller. El callee de un job-level `uses` debe declarar `on: workflow_call`; una action define su interfaz en `action.yml`.

**Pregunta 3.** El job `notify` del caller usa `${{ needs.call-build.outputs.image-tag }}` pero el valor está vacío. El step del callee escribe correctamente en `$GITHUB_OUTPUT`. ¿Cuál es la causa más probable y cómo se soluciona?

> Respuesta: El job `call-build` en el callee no eleva el output del step en su propio bloque `outputs:`. La cadena requiere tres niveles: el step escribe en `$GITHUB_OUTPUT`, el job lo eleva con `outputs: image-tag: ${{ steps.<id>.outputs.image-tag }}`, y el callee lo expone en `on.workflow_call.outputs`. Si el nivel intermedio (bloque `outputs:` del job) falta, el valor no se propaga aunque el step escriba correctamente.

---

← [4.2.1 Reusable Workflows: autoría](gha-d4-reusable-workflows-autoria.md) | [Índice](README.md) | [4.3.1 Políticas allow-list](gha-d4-politicas-allow-list.md) →

