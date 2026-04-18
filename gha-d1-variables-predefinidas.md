# 1.17 Variables predefinidas del runner

[← 1.16 File commands del workflow](gha-d1-file-commands.md) | [1.18 Badges de estado →](gha-d1-badges.md)

---

## Variables de entorno predefinidas vs contextos

GitHub Actions expone metadatos del workflow a través de dos mecanismos paralelos: los **contextos** (sintaxis `${{ github.sha }}`, evaluados por el motor de Actions antes de ejecutar el step) y las **variables de entorno predefinidas** (sintaxis shell `$GITHUB_SHA`, disponibles en tiempo de ejecución del proceso). Ambos mecanismos exponen la misma información, pero se usan en momentos distintos.

Los contextos son la opción correcta cuando se necesita el valor dentro de expresiones de workflow: condiciones `if:`, matrices, configuración de jobs. Las variables de entorno predefinidas son la opción correcta dentro de scripts shell, donde la interpolación de contextos no está disponible o resulta incómoda. Mezclar ambos en el mismo step es perfectamente válido: el contexto define el entorno, el script lo consume.

---

## Las 10 variables esenciales

### GITHUB_SHA

Contiene el SHA completo del commit que disparó el workflow. En un evento `push` es el SHA del commit recién empujado. En un `pull_request` es el SHA del commit de merge generado por GitHub (no el HEAD de la rama). Se usa principalmente para etiquetar artefactos, imágenes Docker o despliegues con el identificador exacto del código que los produjo.

```bash
docker build -t myapp:$GITHUB_SHA .
```

```mermaid
flowchart TD
    PE["evento: push\na refs/heads/main"]
    PRE["evento: pull_request\nfeature/login → main"]

    PE --> PSHA["GITHUB_SHA =\nSHA del commit empujado"]

    PRE --> MERGE["GitHub genera commit de merge\n(rama base + rama del PR)"]
    MERGE --> MSHA["GITHUB_SHA =\nSHA del merge sintético"]
    MERGE --> WARN["para el HEAD real del PR:\ngithub.event.pull_request.head.sha"]

    classDef root    fill:#1f2328,color:#fff,stroke:#444,font-weight:bold
    classDef primary fill:#0969da,color:#fff,stroke:#0550ae
    classDef secondary fill:#2da44e,color:#fff,stroke:#1a7f37
    classDef warning fill:#9a6700,color:#fff,stroke:#7d4e00

    class PE,PRE root
    class PSHA,MSHA secondary
    class MERGE primary
    class WARN warning
```

### GITHUB_REF

Referencia Git completa que disparó el workflow, en formato largo: `refs/heads/main` para ramas y `refs/tags/v1.0.0` para tags. Cuando se necesita distinguir entre un push a rama y un push de tag, `GITHUB_REF` es la variable que permite hacer esa distinción con un simple `grep` o comparación de prefijo en shell.

```bash
if [[ "$GITHUB_REF" == refs/tags/* ]]; then
  echo "Es un release"
fi
```

### GITHUB_REF_NAME

Nombre corto de la rama o tag: `main`, `feature/login`, `v1.0.0`. Es la versión simplificada de `GITHUB_REF` sin el prefijo `refs/heads/` o `refs/tags/`. Útil para construir nombres de entorno, rutas de despliegue o mensajes de log sin necesidad de manipulación de cadenas.

```bash
echo "Desplegando rama: $GITHUB_REF_NAME"
```

### GITHUB_WORKSPACE

Ruta absoluta al directorio de trabajo donde la action `actions/checkout` clona el repositorio. En runners hosted de GitHub suele ser `/home/runner/work/repo-name/repo-name`. Los scripts que necesitan construir rutas a archivos del repositorio deben usar esta variable como base en lugar de asumir una ruta fija.

```bash
ls "$GITHUB_WORKSPACE/src"
```

### GITHUB_ACTOR

Login del usuario de GitHub que inició la ejecución: el autor del push, el creador del PR o quien disparó el workflow manualmente. Se usa en mensajes de auditoría, notificaciones o para aplicar lógica condicional basada en quién ejecuta el workflow.

```bash
echo "Workflow iniciado por: $GITHUB_ACTOR"
```

### GITHUB_REPOSITORY

Nombre completo del repositorio en formato `owner/repo`, por ejemplo `octocat/hello-world`. Necesario cuando se construyen URLs a la API de GitHub, se referencian artefactos de otro repositorio o se quiere que un script sea portable entre distintos repos sin hardcodear el nombre.

```bash
curl -H "Authorization: token $GITHUB_TOKEN" \
  "https://api.github.com/repos/$GITHUB_REPOSITORY/releases"
```

### GITHUB_RUN_ID

Identificador numérico único asignado a cada ejecución del workflow en toda la plataforma GitHub. No se repite entre ejecuciones ni entre repositorios. Se usa para construir URLs directas a la ejecución, para nombrar artefactos de forma única o para correlacionar logs de sistemas externos con ejecuciones de Actions.

```bash
echo "Ver ejecución: https://github.com/$GITHUB_REPOSITORY/actions/runs/$GITHUB_RUN_ID"
```

### GITHUB_RUN_NUMBER

Número secuencial que incrementa con cada nueva ejecución de un workflow específico dentro del repositorio. Empieza en 1 y sube de forma monotónica. A diferencia de `GITHUB_RUN_ID`, este número es legible y predecible, por lo que se usa habitualmente como número de versión de build o para nombrar artefactos con un identificador amigable.

```bash
echo "Build número: $GITHUB_RUN_NUMBER"
```

### GITHUB_TOKEN

Token de autenticación generado automáticamente por GitHub para cada ejecución del workflow. Tiene el mismo valor que `${{ secrets.GITHUB_TOKEN }}` y permite interactuar con la API de GitHub sin necesidad de crear tokens personales. Sus permisos son granulares y configurables por scope; los detalles de permisos se tratan en [D5 — Permisos del GITHUB_TOKEN](gha-github-token-permisos.md).

```bash
gh release create "v$GITHUB_RUN_NUMBER" --generate-notes
# gh CLI usa GITHUB_TOKEN del entorno automáticamente
```

### RUNNER_OS

Sistema operativo del runner donde se ejecuta el job: `Linux`, `Windows` o `macOS`. Se usa en scripts multiplataforma para seleccionar el comando correcto, la extensión de ejecutable adecuada o la ruta del sistema de archivos correspondiente. Evita hardcodear el OS en el script cuando el mismo workflow puede ejecutarse en distintos runners.

```bash
if [[ "$RUNNER_OS" == "Windows" ]]; then
  ./build.bat
else
  ./build.sh
fi
```

---

## Tabla de referencia rápida

| Variable | Ejemplo de valor real | Equivalente en contexto |
|---|---|---|
| `GITHUB_SHA` | `a1b2c3d4e5f6...` (40 chars) | `${{ github.sha }}` |
| `GITHUB_REF` | `refs/heads/main` | `${{ github.ref }}` |
| `GITHUB_REF_NAME` | `main` | `${{ github.ref_name }}` |
| `GITHUB_WORKSPACE` | `/home/runner/work/repo/repo` | `${{ github.workspace }}` |
| `GITHUB_ACTOR` | `octocat` | `${{ github.actor }}` |
| `GITHUB_REPOSITORY` | `octocat/hello-world` | `${{ github.repository }}` |
| `GITHUB_RUN_ID` | `1658821493` | `${{ github.run_id }}` |
| `GITHUB_RUN_NUMBER` | `42` | `${{ github.run_number }}` |
| `GITHUB_TOKEN` | `ghs_xxxxxxxxxxxx` | `${{ secrets.GITHUB_TOKEN }}` |
| `RUNNER_OS` | `Linux` | `${{ runner.os }}` |

```mermaid
graph LR
    YAML["Expresion YAML\n${{ github.ref }}"]
    SHELL["Variable de entorno\n$GITHUB_REF"]

    YAML -->|"evaluada por Actions\nantes de ejecutar el runner"| IF["Condiciones if:\nprops de job/step"]
    SHELL -->|"evaluada por el shell\ndurante ejecucion del step"| RUN["Comandos run:\nscripts bash/pwsh"]

    classDef root fill:#1f2328,color:#fff,stroke:#444,font-weight:bold
    classDef primary fill:#0969da,color:#fff,stroke:#0550ae
    classDef secondary fill:#2da44e,color:#fff,stroke:#1a7f37

    class YAML,SHELL root
    class IF primary
    class RUN secondary
```

---

## Ejemplo central: workflow usando 6 variables predefinidas

```yaml
name: Build y publish

on:
  push:
    branches: [main]
    tags: ["v*"]

jobs:
  build:
    runs-on: ubuntu-latest
    permissions:
      contents: write
      packages: write

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Información del entorno
        run: |
          echo "Repositorio : $GITHUB_REPOSITORY"
          echo "Rama/Tag    : $GITHUB_REF_NAME"
          echo "Commit SHA  : $GITHUB_SHA"
          echo "Iniciado por: $GITHUB_ACTOR"
          echo "Run número  : $GITHUB_RUN_NUMBER"
          echo "OS del runner: $RUNNER_OS"

      - name: Construir imagen Docker
        run: |
          docker build \
            --label "git-sha=$GITHUB_SHA" \
            --label "build-number=$GITHUB_RUN_NUMBER" \
            -t ghcr.io/$GITHUB_REPOSITORY:$GITHUB_SHA \
            -t ghcr.io/$GITHUB_REPOSITORY:$GITHUB_REF_NAME \
            "$GITHUB_WORKSPACE"

      - name: Login en GHCR
        run: |
          echo "$GITHUB_TOKEN" | docker login ghcr.io \
            -u "$GITHUB_ACTOR" --password-stdin

      - name: Push de imagen
        run: |
          docker push ghcr.io/$GITHUB_REPOSITORY:$GITHUB_SHA
          docker push ghcr.io/$GITHUB_REPOSITORY:$GITHUB_REF_NAME

      - name: Crear release si es tag
        if: startsWith(github.ref, 'refs/tags/')
        env:
          GH_TOKEN: ${{ github.token }}
        run: |
          gh release create "$GITHUB_REF_NAME" \
            --title "Release $GITHUB_REF_NAME (build $GITHUB_RUN_NUMBER)" \
            --generate-notes
```

El step "Login en GHCR" combina `GITHUB_TOKEN` (password) con `GITHUB_ACTOR` (username), un patrón idiomático para autenticarse en el registro de contenedores de GitHub. El step de Docker build usa `GITHUB_WORKSPACE` como contexto de build en lugar de `.` para ser explícito sobre la ruta absoluta.

---

## Buenas y malas practicas

**Usar `$GITHUB_WORKSPACE` como base de rutas absolutas — no `./` ni rutas hardcodeadas**

Correcto:
```bash
cp "$GITHUB_WORKSPACE/config/app.yaml" /etc/app/
```
Incorrecto:
```bash
cp ./config/app.yaml /etc/app/          # falla si el cwd no es el workspace
cp /home/runner/work/myrepo/myrepo/config/app.yaml /etc/app/  # hardcodeado
```

**Etiquetar artefactos con `GITHUB_SHA` — no con `GITHUB_RUN_NUMBER` si se necesita trazabilidad exacta**

Correcto:
```bash
docker tag myapp:latest myapp:$GITHUB_SHA   # vinculo exacto al commit
```
Incorrecto:
```bash
docker tag myapp:latest myapp:build-$GITHUB_RUN_NUMBER  # no mapea al commit
```

**Leer `GITHUB_TOKEN` del entorno — no hardcodear tokens personales en scripts**

Correcto:
```bash
curl -H "Authorization: token $GITHUB_TOKEN" https://api.github.com/...
```
Incorrecto:
```bash
curl -H "Authorization: token ghp_miTokenPersonalHardcodeado" https://...
```

**Preferir `GITHUB_REF_NAME` sobre manipular `GITHUB_REF` con `sed` o `cut`**

Correcto:
```bash
BRANCH="$GITHUB_REF_NAME"               # ya viene limpio
```
Incorrecto:
```bash
BRANCH=$(echo "$GITHUB_REF" | sed 's|refs/heads/||')   # frágil con tags
```

**No asumir que `GITHUB_REF_NAME` es siempre una rama — puede ser un tag**

Correcto:
```bash
if [[ "$GITHUB_REF" == refs/tags/* ]]; then
  echo "Tag: $GITHUB_REF_NAME"
elif [[ "$GITHUB_REF" == refs/heads/* ]]; then
  echo "Rama: $GITHUB_REF_NAME"
fi
```
Incorrecto:
```bash
git checkout -b "$GITHUB_REF_NAME"   # falla si es un tag, no una rama
```

---

## Verificacion GH-200

**Pregunta 1.** Un step de shell necesita construir la URL de la ejecucion actual para incluirla en un mensaje de Slack. ¿Que combinacion de variables usa?

- A) `$GITHUB_RUN_NUMBER` y `$GITHUB_REPOSITORY`
- B) `$GITHUB_RUN_ID` y `$GITHUB_REPOSITORY`
- C) `$GITHUB_SHA` y `$GITHUB_ACTOR`
- D) `$GITHUB_REF` y `$GITHUB_RUN_NUMBER`

Respuesta correcta: **B** — La URL de una ejecucion es `https://github.com/$GITHUB_REPOSITORY/actions/runs/$GITHUB_RUN_ID`. `GITHUB_RUN_NUMBER` es secuencial por workflow, no forma parte de la URL.

---

**Pregunta 2.** En un workflow con trigger `pull_request`, ¿que valor contiene `GITHUB_SHA`?

- A) El SHA del ultimo commit de la rama base
- B) El SHA del ultimo commit de la rama del PR
- C) El SHA del commit de merge sintetico generado por GitHub
- D) El SHA del primer commit del PR

Respuesta correcta: **C** — En eventos `pull_request`, GitHub genera un commit de merge temporal entre la rama base y la rama del PR; `GITHUB_SHA` apunta a ese commit sintetico.

---

**Pregunta 3.** Un script multiplataforma necesita usar `.\build.bat` en Windows y `./build.sh` en Linux/macOS. ¿Que variable permite esa decision?

- A) `RUNNER_ARCH`
- B) `RUNNER_OS`
- C) `GITHUB_ENV`
- D) `GITHUB_REF_NAME`

Respuesta correcta: **B** — `RUNNER_OS` devuelve `Linux`, `Windows` o `macOS`, permitiendo condiciones de shell sobre el sistema operativo del runner.

---

**Ejercicio practico**

Escribe un step que imprima en una sola linea el siguiente formato, usando exclusivamente variables de entorno predefinidas (sin contextos):

```
[octocat/hello-world] Run #42 | SHA: a1b2c3d | Branch: main | OS: Linux
```

Solucion de referencia:

```bash
- name: Resumen del run
  run: |
    SHORT_SHA="${GITHUB_SHA:0:7}"
    echo "[$GITHUB_REPOSITORY] Run #$GITHUB_RUN_NUMBER | SHA: $SHORT_SHA | Branch: $GITHUB_REF_NAME | OS: $RUNNER_OS"
```

---

[← 1.16 File commands del workflow](gha-d1-file-commands.md) | [1.18 Badges de estado →](gha-d1-badges.md)
