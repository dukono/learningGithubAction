# 5.7.2 Artifact Attestations — Generación y Verificación

← [5.7.1 SLSA Provenance — Fundamentos](gha-slsa-provenance-fundamentos.md) | [Índice](README.md) | [5.8.1 Cache Strategies](gha-cache-strategies.md) →

---

Entender SLSA es necesario; saber implementarlo es lo que el examen evalúa. Este documento cubre el uso práctico de `actions/attest-build-provenance` para generar attestations de provenance en pipelines de build, y de `gh attestation verify` para verificarlas en el momento de deployment o auditoría.

> [CONCEPTO] Las GitHub artifact attestations son la implementación nativa de SLSA provenance en GitHub Actions. Usan el token OIDC del runner para firmar el hash del artefacto mediante Sigstore, publican la firma en Rekor y la almacenan en el repositorio. Todo el proceso es transparente y verificable sin necesidad de gestionar claves.

## La action actions/attest-build-provenance

La action `actions/attest-build-provenance` genera una attestation de provenance SLSA para un artefacto producido por el workflow. Internamente obtiene el token OIDC del runner, solicita un certificado a Fulcio, firma el digest SHA-256 del artefacto y publica la firma en Rekor.

El parámetro principal para identificar el artefacto es `subject-path` (para binarios locales) o `subject-digest` combinado con `subject-name` (para artefactos remotos como imágenes de contenedor).

> [ADVERTENCIA] Los permisos `id-token: write` y `attestations: write` deben declararse en el job que ejecuta `attest-build-provenance`, no solo a nivel de workflow. Si se declaran solo a nivel de workflow y el job tiene su propio bloque `permissions:`, el job hereda solo lo que declare explícitamente. Omitir cualquiera de los dos permisos hace que la action falle con un error de autorización.

## Parámetros de la action

La action acepta dos formas de especificar el artefacto sujeto de la attestation. Estos parámetros son mutuamente excluyentes en su función pero pueden combinarse para imágenes de contenedor.

| Parámetro | Obligatorio | Descripción |
|---|---|---|
| `subject-path` | Condicional | Ruta al artefacto local (glob soportado). Calcula el digest automáticamente. |
| `subject-digest` | Condicional | Digest SHA-256 del artefacto (formato `sha256:<hex>`). Para artefactos remotos. |
| `subject-name` | Condicional | Nombre del artefacto cuando se usa `subject-digest`. Para imágenes: `ghcr.io/owner/repo`. |
| `push-to-registry` | No | `true` para adjuntar la attestation a una imagen en un registry OCI. Default: `false`. |
| `show-summary` | No | Muestra un resumen en el job summary. Default: `true`. |
| `github-token` | No | Token para publicar la attestation. Default: `${{ github.token }}`. |

## Permisos necesarios

Los dos permisos que habilitan la generación de attestations deben estar presentes en el job:

- `id-token: write`: permite que el runner solicite un token OIDC a GitHub. Este token es la identidad del runner y se usa para obtener el certificado de firma de Fulcio.
- `attestations: write`: permite que la action publique la attestation en la API de GitHub (endpoint `/repos/{owner}/{repo}/attestations`).

Sin `id-token: write`, el runner no puede autenticarse ante Fulcio y la firma no se puede generar. Sin `attestations: write`, la attestation no se puede almacenar en el repositorio de GitHub (aunque sí se publicaría en Rekor).

## Salida de la action

La action produce dos outputs que pueden referenciarse en steps posteriores:

| Output | Descripción |
|---|---|
| `bundle-path` | Ruta local al fichero JSON del bundle de attestation (formato DSSE) |
| `attestation-url` | URL de la attestation publicada en el repositorio de GitHub |

La `attestation-url` sigue el formato `https://github.com/{owner}/{repo}/attestations/{id}` y es accesible públicamente para repositorios públicos o por miembros autorizados para repositorios privados.

## Ejemplo central

El siguiente workflow completo cubre el ciclo build-attest-verify para un binario Go. El job `build` genera la attestation y el job `verify` la verifica antes de un deployment simulado.

```yaml
# .github/workflows/build-attest-verify.yml
name: Build, Attest and Verify

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  build:
    name: Build and Attest
    runs-on: ubuntu-latest
    permissions:
      contents: read
      id-token: write      # Token OIDC para firma Sigstore
      attestations: write  # Publicar attestation en GitHub

    outputs:
      artifact-digest: ${{ steps.attest.outputs.bundle-path }}
      attestation-url: ${{ steps.attest.outputs.attestation-url }}

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up Go
        uses: actions/setup-go@v5
        with:
          go-version: "1.22"

      - name: Build binary
        run: |
          mkdir -p dist
          go build -trimpath -o dist/myapp ./cmd/myapp

      - name: Generate SLSA provenance attestation
        id: attest
        uses: actions/attest-build-provenance@v1
        with:
          subject-path: dist/myapp

      - name: Upload artifact
        uses: actions/upload-artifact@v4
        with:
          name: myapp-${{ github.sha }}
          path: dist/myapp

      - name: Show attestation URL
        run: echo "Attestation published at ${{ steps.attest.outputs.attestation-url }}"

  verify:
    name: Verify Before Deploy
    runs-on: ubuntu-latest
    needs: build
    if: github.ref == 'refs/heads/main'
    permissions:
      contents: read

    steps:
      - name: Download artifact
        uses: actions/download-artifact@v4
        with:
          name: myapp-${{ github.sha }}
          path: dist/

      - name: Verify attestation
        env:
          GH_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        run: |
          gh attestation verify dist/myapp \
            --repo ${{ github.repository }} \
            --format json

      - name: Deploy (simulado)
        run: echo "Artefacto verificado. Procediendo con el deployment."
```

> [EXAMEN] El comando `gh attestation verify` requiere el flag `--repo` para especificar el repositorio contra el que se verifica la attestation. Sin este flag, el comando no sabe qué repositorio de GitHub aloja la attestation firmada. El flag acepta el formato `owner/repo`.

## Verificación con gh attestation verify

El comando `gh attestation verify` consulta Rekor y la API de GitHub para confirmar que la attestation del artefacto es válida, fue emitida por el repositorio especificado y que el artefacto no ha sido modificado tras la firma.

Sintaxis para binarios locales:

```bash
gh attestation verify <ruta-al-artefacto> --repo owner/repo
```

Sintaxis para imágenes de contenedor en un OCI registry:

```bash
gh attestation verify oci://ghcr.io/owner/repo:tag --repo owner/repo
```

La diferencia entre los dos casos es el prefijo `oci://`: indica que el artefacto a verificar es una imagen en un registry OCI, no un fichero local. El digest de la imagen se resuelve automáticamente desde el registry.

Opciones relevantes del comando:

| Flag | Descripción |
|---|---|
| `--repo owner/repo` | Repositorio que generó la attestation (obligatorio) |
| `--format json` | Salida en JSON para procesamiento programático |
| `--deny-self-hosted-runners` | Falla si la attestation fue generada en un self-hosted runner |
| `--bundle <fichero>` | Verifica contra un bundle local en lugar de consultar Rekor |

> [ADVERTENCIA] El flag `--deny-self-hosted-runners` es relevante para compliance estricto: un self-hosted runner puede estar bajo el control total del developer, lo que significa que podría generar attestations en un entorno no auditado. Si el modelo de amenaza incluye runners comprometidos, este flag añade una capa de protección.

## Diferencia entre binarios y contenedores

El proceso de attestation difiere ligeramente según el tipo de artefacto:

**Para binarios:** se usa `subject-path` con la ruta local al fichero. La action calcula el SHA-256 del fichero automáticamente.

**Para imágenes de contenedor:** se usa `subject-digest` con el digest de la imagen (obtenido del output `digest` de `docker/build-push-action`) y `subject-name` con el nombre completo de la imagen (incluyendo registry). Si se quiere adjuntar la attestation a la imagen en el registry, se añade `push-to-registry: true`.

```yaml
# Fragmento para imágenes de contenedor
- name: Build and push
  id: push
  uses: docker/build-push-action@v5
  with:
    context: .
    push: true
    tags: ghcr.io/${{ github.repository }}:latest

- name: Attest container image
  uses: actions/attest-build-provenance@v1
  with:
    subject-name: ghcr.io/${{ github.repository }}
    subject-digest: ${{ steps.push.outputs.digest }}
    push-to-registry: true
```

## Diferencia frente a firma convencional (GPG)

La tabla siguiente contrasta el modelo de Sigstore (usado por GitHub artifact attestations) con el de GPG, que es el mecanismo de firma tradicional.

| Aspecto | Sigstore / GitHub Attestations | GPG tradicional |
|---|---|---|
| Gestión de claves | Sin claves de larga duración; certificados OIDC efímeros | Claves privadas gestionadas por el developer |
| Distribución de confianza | Transparency log público (Rekor) | Distribución manual de claves públicas (keyservers) |
| Identidad del firmante | Identidad OIDC del runner (vinculada al workflow) | Identidad GPG del developer (personal) |
| Auditoría | Automática e independiente vía Rekor | Manual; requiere verificar la clave pública |
| Revocación | No es necesaria (certificados efímeros) | Requiere publicar certificado de revocación |
| SLSA compliance | Nativo (L2 con GitHub-hosted runners) | No aplica al framework SLSA |

## Buenas y malas prácticas

**Hacer:**
- Declarar `id-token: write` y `attestations: write` en el job específico que genera la attestation — razón: los permisos a nivel de job tienen precedencia sobre los del workflow cuando ambos están declarados; omitirlos en el job hace que fallen aunque estén en el workflow.
- Usar `--deny-self-hosted-runners` en `gh attestation verify` cuando el compliance requiere SLSA L2 estricto — razón: los self-hosted runners no garantizan el aislamiento de la plataforma gestionada.
- Verificar la attestation en el job de deployment, no solo después del build — razón: el valor está en la verificación en el punto de consumo; verificar solo en build no protege contra sustitución del artefacto entre etapas.
- Usar `push-to-registry: true` para imágenes de contenedor — razón: adjunta la attestation al manifest de la imagen en el registry, permitiendo verificación directa con `oci://` sin necesidad de un bundle externo.

**Evitar:**
- Omitir `--repo` en `gh attestation verify` — razón: el comando necesita saber contra qué repositorio verificar; sin este flag falla con un error de argumento.
- Usar `subject-path` para imágenes de contenedor — razón: las imágenes se identifican por digest desde el registry, no por un fichero local; usar `subject-path` en ese contexto no cubre la imagen real desplegada.
- Asumir que la attestation verifica el contenido del código fuente — razón: la attestation certifica el proceso de build (qué workflow, qué repositorio, qué runner), no que el código fuente sea seguro.
- Publicar el bundle de attestation como artefacto de workflow en lugar de usar `push-to-registry` para imágenes — razón: los artefactos de workflow expiran; el bundle debe estar en Rekor y opcionalmente en el registry para verificación duradera.

## Verificación y práctica

**Pregunta 1:** ¿Cuál es la diferencia entre `subject-path` y `subject-digest` en `actions/attest-build-provenance`?

Respuesta: `subject-path` recibe la ruta a un fichero local y la action calcula el SHA-256 automáticamente. `subject-digest` recibe el digest ya calculado (formato `sha256:<hex>`) y se usa junto con `subject-name` para artefactos remotos como imágenes de contenedor donde no hay un fichero local disponible en el runner. Los dos parámetros son mutuamente excluyentes como mecanismo de identificación del sujeto.

**Pregunta 2:** Un pipeline genera una imagen Docker y la publica en GHCR. ¿Qué combinación de parámetros debe usar `actions/attest-build-provenance` para adjuntar la attestation a la imagen en el registry?

Respuesta: Debe usar `subject-name: ghcr.io/owner/repo` (nombre completo de la imagen), `subject-digest: ${{ steps.push.outputs.digest }}` (digest obtenido del output de `docker/build-push-action`) y `push-to-registry: true` para que la attestation se adjunte al manifest de la imagen en GHCR.

**Pregunta 3:** ¿Cómo se verifica la attestation de una imagen de contenedor con `gh attestation verify`?

Respuesta: Usando el prefijo `oci://` antes del nombre de la imagen: `gh attestation verify oci://ghcr.io/owner/repo:tag --repo owner/repo`. El prefijo `oci://` indica que el digest debe resolverse desde el registry OCI, no desde un fichero local.

**Ejercicio:** Escribe un job de verificación que descargue un binario publicado como artefacto de workflow y verifique su attestation antes de desplegarlo. El repositorio es `myorg/myapp` y el artefacto se llama `myapp-binary`.

```yaml
  deploy:
    name: Verify and Deploy
    runs-on: ubuntu-latest
    needs: build
    environment: production
    permissions:
      contents: read

    steps:
      - name: Download artifact
        uses: actions/download-artifact@v4
        with:
          name: myapp-binary
          path: dist/

      - name: Verify SLSA attestation
        env:
          GH_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        run: |
          gh attestation verify dist/myapp \
            --repo myorg/myapp \
            --deny-self-hosted-runners \
            --format json | jq '.[] | {verified, predicate_type}'

      - name: Deploy to production
        run: |
          echo "Attestation verificada. Desplegando dist/myapp en producción."
          # Aquí iría el comando real de deployment
```

El flag `--deny-self-hosted-runners` garantiza que la attestation fue generada en un GitHub-hosted runner (SLSA L2). El `--format json` con `jq` permite inspeccionar el resultado de verificación de forma programática y fallar si los campos no son los esperados.

---

← [5.7.1 SLSA Provenance — Fundamentos](gha-slsa-provenance-fundamentos.md) | [Índice](README.md) | [5.8.1 Cache Strategies](gha-cache-strategies.md) →
