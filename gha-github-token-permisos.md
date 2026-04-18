# 5.3.2 GITHUB_TOKEN — Configuración de Permisos Granulares

[← 5.3.1 GITHUB_TOKEN — Lifecycle](gha-github-token-lifecycle.md) | [Índice](README.md) | [5.4.1 OIDC — Fundamentos](gha-oidc-fundamentos-claims.md) →

---

## El modelo de permisos del GITHUB_TOKEN

GitHub Actions expone la clave `permissions:` para declarar qué puede hacer el `GITHUB_TOKEN` durante un run. Esta clave puede aparecer en dos lugares del fichero de workflow con distintos alcances: en la raíz del workflow (aplica a todos los jobs) y dentro de un job individual (sobreescribe la raíz solo para ese job). Declarar permisos explícitos es la práctica fundamental del principio de mínimo privilegio en GitHub Actions.

---

## Tabla de permisos disponibles

| Permiso | Scope de API cubierto | Valores |
|---|---|---|
| `actions` | Gestión de workflow runs, artefactos, runners | `read`, `write`, `none` |
| `attestations` | Generación de attestations de artefactos (sigstore) | `read`, `write`, `none` |
| `checks` | Crear y actualizar check runs y check suites | `read`, `write`, `none` |
| `contents` | Código, commits, ramas, releases, tags | `read`, `write`, `none` |
| `deployments` | Crear y actualizar deployment statuses | `read`, `write`, `none` |
| `discussions` | Leer y escribir GitHub Discussions | `read`, `write`, `none` |
| `id-token` | Solicitar el token OIDC del job al proveedor de GitHub | `write`, `none` |
| `issues` | Issues, labels, milestones | `read`, `write`, `none` |
| `metadata` | Metadatos básicos del repo (nombre, visibilidad) | `read` (implícito, no configurable) |
| `packages` | GitHub Packages (publicar, consumir) | `read`, `write`, `none` |
| `pages` | GitHub Pages (deployments) | `read`, `write`, `none` |
| `pull-requests` | PRs, reviews, review comments | `read`, `write`, `none` |
| `repository-projects` | Projects v1 (clásicos) | `read`, `write`, `none` |
| `security-events` | Code scanning, Dependabot alerts, SARIF | `read`, `write`, `none` |
| `statuses` | Commit statuses (la API legacy de checks) | `read`, `write`, `none` |

> **Nota:** `metadata: read` es el único permiso implícito e irrevocable. No puede establecerse en `none`. Aparece en todos los tokens independientemente de la configuración.

---

## Declaración top-level: aplica a todos los jobs

Declarar `permissions:` en la raíz del workflow establece el conjunto de permisos que todos los jobs heredarán por defecto. Si un job no declara su propio `permissions:`, usará exactamente este conjunto.

```yaml
name: CI con permisos mínimos
on: [push]

permissions:
  contents: read
  checks: write

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: npm test
```

El job `test` hereda `contents: read` y `checks: write`. El resto de permisos están implícitamente en `none`.

---

## Declaración por job: sobreescribe el top-level

Declarar `permissions:` dentro de un job define un conjunto completamente nuevo para ese job — **reemplaza** los permisos de la raíz, no los fusiona. Esto permite que un job tenga permisos elevados mientras el resto del workflow permanece restringido.

```yaml
permissions:
  contents: read

jobs:
  build:
    runs-on: ubuntu-latest
    # Hereda del top-level: contents: read
    steps:
      - uses: actions/checkout@v4

  deploy:
    runs-on: ubuntu-latest
    needs: build
    permissions:
      contents: read
      deployments: write
      id-token: write
    # Solo este job puede crear deployments y solicitar token OIDC
    steps:
      - name: Autenticar con OIDC
        uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: arn:aws:iam::123456789:role/deploy
          aws-region: eu-west-1
```

---

## permissions: {} — eliminar todos los permisos configurables

Establecer `permissions: {}` (mapa vacío) quita todos los permisos opcionales del token. El resultado es un token que solo tiene `metadata: read`, que es el mínimo absoluto e irrevocable. Se usa en jobs de análisis estático, lint o cualquier tarea que no necesite interactuar con la API de GitHub.

```yaml
jobs:
  lint:
    runs-on: ubuntu-latest
    permissions: {}
    steps:
      - uses: actions/checkout@v4
      - run: npm run lint
```

> **[EXAMEN]** Si el workflow declara `permissions: read-all` en la raíz y un job concreto declara `permissions: {}`, ese job únicamente tiene `metadata: read`. El `read-all` del top-level queda completamente anulado para ese job. La declaración por job siempre reemplaza, nunca suma.

---

## El permiso id-token: write para OIDC

El permiso `id-token: write` es el requisito previo para que GitHub Actions emita el token JWT que los proveedores cloud (AWS, GCP, Azure) verifican mediante OIDC. Sin este permiso, cualquier acción que solicite el token recibirá un error de autorización. Debe declararse solo en los jobs que interactúan con recursos cloud, no a nivel de workflow completo.

```yaml
jobs:
  deploy:
    permissions:
      id-token: write
      contents: read
    steps:
      - uses: actions/checkout@v4
      - uses: google-github-actions/auth@v2
        with:
          workload_identity_provider: "projects/123/.../providers/provider"
          service_account: deploy@proyecto.iam.gserviceaccount.com
```

---

## Herencia en reusable workflows: permissions: inherit

Cuando un workflow llama a un reusable workflow con `uses:`, el llamado recibe los permisos del job llamador. Para hacer explícita esa herencia se usa `permissions: inherit`.

```yaml
jobs:
  release:
    permissions:
      contents: write
      packages: write
    uses: ./.github/workflows/publish.yml
    # El reusable workflow hereda contents: write y packages: write

  # Alternativa explícita:
  release-explicit:
    uses: ./.github/workflows/publish.yml
    permissions: inherit  # pasa todos los permisos del caller
```

Si el reusable workflow declara `permissions:` en su raíz, esa declaración reemplaza lo que hereda del caller.

---

## Patrón: mínimo privilegio por job

La práctica recomendada para workflows de producción es no declarar `permissions:` en la raíz (o declarar `permissions: {}`) y definir permisos específicos en cada job según lo que necesita.

```yaml
name: Build, test y deploy
on:
  push:
    branches: [main]
permissions: {}  # Deniega todo por defecto en todos los jobs

jobs:
  test:
    runs-on: ubuntu-latest
    permissions:
      contents: read
    steps:
      - uses: actions/checkout@v4
      - run: npm ci && npm test

  publish-image:
    needs: test
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write
      id-token: write
    steps:
      - uses: actions/checkout@v4
      - name: Build y push imagen
        run: |
          echo "${{ secrets.GITHUB_TOKEN }}" | docker login ghcr.io -u ${{ github.actor }} --password-stdin
          docker build -t ghcr.io/${{ github.repository }}:${{ github.sha }} .
          docker push ghcr.io/${{ github.repository }}:${{ github.sha }}

  notify:
    needs: publish-image
    runs-on: ubuntu-latest
    if: always()
    permissions:
      issues: write
    steps:
      - run: gh issue comment 1 --body "Deploy ${{ job.status }}"
        env:
          GH_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```
