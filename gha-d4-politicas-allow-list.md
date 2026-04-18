← [4.2.2 Reusable Workflows: consumo](gha-d4-reusable-workflows-consumo.md) | [Índice](README.md) | [4.3.2 Permisos GITHUB_TOKEN y retención](gha-d4-politicas-token-retencion.md) →

# 4.3.1 Políticas de allow-list de actions y fork PR policies

Cuando una organización crece, el riesgo de que alguien introduzca una action maliciosa del marketplace crece con ella. GitHub ofrece controles granulares que permiten definir exactamente qué actions pueden ejecutarse en los repositorios de la organización. Estos controles, combinados con las políticas para pull requests desde forks, forman la primera línea de defensa del supply chain de CI/CD.

## Dónde se configuran las políticas

Las políticas de actions se encuentran en **Settings > Actions > General**, disponible tanto a nivel de organización como de repositorio individual. El nivel de organización establece el techo: un repositorio puede ser más restrictivo que la política org, pero nunca más permisivo. Si la organización prohíbe una action, ningún repositorio puede habilitarla.

La ruta completa en la UI es:

- Organización: `github.com/organizations/<org>/settings/actions`
- Repositorio: `github.com/<owner>/<repo>/settings/actions`

## Las cuatro opciones de política

GitHub ofrece exactamente cuatro opciones para controlar qué actions pueden usarse. Elegir la opción incorrecta es uno de los errores de configuración más frecuentes en equipos que escalan.

| Opción | Qué permite | Cuándo usarla |
|---|---|---|
| **Allow all actions** | Cualquier action del marketplace, repositorios externos y locales | Solo en entornos de experimentación o repos personales. Riesgo máximo en orgs. |
| **Allow local actions only** | Solo actions definidas en el mismo repositorio (rutas `./`) | Máxima restricción; adecuado para repositorios con información altamente sensible. |
| **Allow GitHub-created + verified Marketplace** | Actions de la organización `actions/*` más las actions con insignia de verificación del Marketplace | Balance entre seguridad y utilidad. Cubre la mayoría de casos de uso habituales. |
| **Allow select actions** | Lista explícita con soporte de comodines; el administrador controla cada entrada | Control total. Requiere mantenimiento activo de la lista pero ofrece la mayor precisión. |

> **Trampa de examen frecuente:** "Allow GitHub-created" se refiere a actions bajo la organización `actions` en GitHub (como `actions/checkout`, `actions/cache`). "Allow Marketplace actions by verified creators" abarca publishers que han pasado el proceso de verificación de GitHub pero que no son la propia organización `actions`. Son dos conjuntos distintos. La opción combinada habilita ambos; seleccionar solo una de las dos submarcas del checkbox limita el alcance de forma diferente.

## Allow select: sintaxis de comodines

Cuando se elige "Allow select actions", la interfaz muestra un campo de texto donde se introduce una lista separada por comas o saltos de línea. Los comodines siguen el patrón `*` para cualquier secuencia de caracteres dentro de un segmento:

```
actions/*
octokit/*
docker/*
aws-actions/configure-aws-credentials@*
```

La entrada `actions/*` permite todas las actions del namespace `actions` independientemente del nombre o versión. La entrada `aws-actions/configure-aws-credentials@*` permite esa action específica con cualquier tag o SHA. No existe soporte para `**` (doble comodín de directorio): el comodín solo opera dentro del segmento `owner/repo`.

Un error común es escribir `actions/checkout` sin `@*` pensando que permite todas las versiones: en realidad, si no se incluye la referencia, GitHub infiere que solo se permite sin especificador, lo que en la práctica bloquea las referencias con tag. La forma segura es siempre usar `owner/repo@*` para cubrir todas las versiones.

## Fork PR policies: control de ejecuciones desde forks

Cuando un colaborador externo abre un pull request desde un fork, GitHub no ejecuta los workflows automáticamente por defecto en ciertas condiciones. Las fork PR policies determinan cuándo se requiere aprobación manual antes de que comience la ejecución.

Existen dos niveles de política configurable en Settings > Actions > General:

**Require approval for first-time contributors:** solo se requiere aprobación la primera vez que un usuario externo abre un PR hacia el repositorio. Una vez aprobada la primera ejecución, sus PRs futuros se ejecutan automáticamente.

**Require approval for all outside collaborators:** todos los PRs desde cuentas que no sean miembros del repositorio (o de la organización, según el contexto) requieren aprobación manual en cada ejecución. Es la opción más conservadora.

El flujo de aprobación en la UI funciona así: el maintainer ve el PR con un banner amarillo que indica que los workflows están pendientes de aprobación. Un botón "Approve and run" dispara la ejecución. Sin esa aprobación, ningún runner procesa el workflow aunque el código esté correctamente configurado.

> **Punto clave de diseño:** las fork PR policies no afectan a contributors que ya son miembros de la organización o tienen el rol de collaborator en el repo. Solo aplican a cuentas externas que envían PRs desde forks. Un collaborator con permiso `write` no activa el flujo de aprobación.

## Ejemplo central: workflow que demuestra el impacto de la allow-list

El siguiente workflow usa una action del marketplace y llama a un reusable workflow interno. Dependiendo de la política activa en la organización, alguna de estas líneas puede ser bloqueada antes de que el runner siquiera arranque.

```yaml
# .github/workflows/ci-con-politica.yml
name: CI con política allow-list

on:
  pull_request:
    branches:
      - main

jobs:
  lint:
    runs-on: ubuntu-latest
    permissions:
      contents: read
    steps:
      - name: Checkout del código
        uses: actions/checkout@v4          # permitido si: allow all / allow GitHub-created / allow select con "actions/*"

      - name: Ejecutar linter de terceros
        uses: super-linter/super-linter@v6 # bloqueado si: allow local only / allow GitHub-created only
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}

  security-scan:
    uses: ./.github/workflows/security-reusable.yml  # siempre permitido (local)
    with:
      environment: staging
    secrets: inherit

  notify:
    needs: [lint, security-scan]
    runs-on: ubuntu-latest
    if: failure()
    steps:
      - name: Notificar Slack
        uses: slackapi/slack-github-action@v1  # bloqueado si: allow local only / allow GitHub-created only
        with:
          payload: '{"text":"Pipeline fallido en ${{ github.repository }}"}'
        env:
          SLACK_WEBHOOK_URL: ${{ secrets.SLACK_WEBHOOK }}
```

Con la política **Allow local actions only**, el workflow falla en el step de `super-linter/super-linter` y en `slackapi/slack-github-action` antes de ejecutarse, porque ambas son actions externas. El reusable workflow interno (`./.github/workflows/security-reusable.yml`) nunca llega a ejecutarse porque el job `security-scan` depende implícitamente del contexto de ejecución del workflow completo, que GitHub bloquea al detectar actions no permitidas en otros jobs.

Con **Allow select actions** y la lista `actions/*, slackapi/*`, el job `lint` puede ejecutar `actions/checkout` pero `super-linter/super-linter` sigue bloqueada porque `super-linter/*` no está en la lista.

## Tabla comparativa de las cuatro opciones

| Dimensión | Allow all | Local only | GitHub-created + verified | Allow select |
|---|---|---|---|---|
| **Actions de `actions/*`** | Sí | No | Sí | Solo si en lista |
| **Marketplace verificadas** | Sí | No | Sí | Solo si en lista |
| **Marketplace no verificadas** | Sí | No | No | Solo si en lista |
| **Actions de terceros** | Sí | No | No | Solo si en lista |
| **Actions locales (`./`)** | Sí | Sí | Sí | Sí |
| **Reusable workflows externos** | Sí | No | No | Solo si en lista |
| **Mantenimiento requerido** | Ninguno | Ninguno | Bajo | Alto |
| **Riesgo de supply chain** | Máximo | Mínimo | Bajo | Configurable |

## Buenas prácticas

**Nivel organización:** usar "Allow GitHub-created + verified Marketplace" como punto de partida para la mayoría de organizaciones. Migrar a "Allow select" cuando se necesite control granular sobre vendors específicos.

**Nivel repositorio:** nunca configurar un repositorio como más permisivo que la política org. Si la org usa "Allow select", el repositorio puede restringir más su propia lista, pero no puede añadir entries que la org no permite.

**Fork PR policies:** en repositorios públicos que aceptan contribuciones externas, la opción "Require approval for first-time contributors" es el balance recomendado: protege contra el abuso de cuentas nuevas sin friction excesiva para contributors recurrentes.

## Preguntas de verificación GH-200

**P1.** Una organización tiene configurada la política "Allow GitHub-created actions and actions by Marketplace verified creators". Un desarrollador intenta usar `docker/login-action@v3` en su workflow. ¿Se ejecutará?

<details>
<summary>Respuesta</summary>
Depende de si Docker está verificado en el Marketplace de GitHub. Si el publisher `docker` tiene la insignia de verificación, la action se permite. Si no, queda bloqueada. En la práctica, `docker/login-action` es de un publisher verificado, así que se ejecutaría.
</details>

**P2.** Un administrador configura "Allow select actions" con la lista `actions/checkout@*, actions/setup-node@*`. Un workflow intenta usar `actions/upload-artifact@v4`. ¿Qué ocurre?

<details>
<summary>Respuesta</summary>
La ejecución falla. `actions/upload-artifact` no está en la lista. Para permitirla habría que añadir `actions/upload-artifact@*` o ampliar el comodín a `actions/*` para cubrir todo el namespace.
</details>

**P3.** ¿Cuál es la diferencia entre "Require approval for first-time contributors" y "Require approval for all outside collaborators"?

<details>
<summary>Respuesta</summary>
La primera solo aplica aprobación manual en el primer PR de un contributor externo; sus PRs posteriores corren automáticamente. La segunda exige aprobación manual en cada PR de cualquier cuenta externa, sin importar su historial previo.
</details>

---

← [4.2.2 Reusable Workflows: consumo](gha-d4-reusable-workflows-consumo.md) | [Índice](README.md) | [4.3.2 Permisos GITHUB_TOKEN y retención](gha-d4-politicas-token-retencion.md) →
