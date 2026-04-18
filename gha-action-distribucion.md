# 3.6 Distribución de actions (pública, privada, interna)

← [3.5 Version pinning e immutable actions](gha-action-version-pinning.md) | [Índice](README.md) | [3.7 Publicación en el GitHub Marketplace y release strategies](gha-action-publicacion-marketplace.md) →

---

El modo de distribución de una action determina desde qué workflows puede ser invocada y cómo se escribe el campo `uses:`. Entender los tres modos es fundamental para diagnosticar errores de acceso y para elegir correctamente la sintaxis de referencia según el contexto de visibilidad del repositorio.

> [ADVERTENCIA] En el contexto de D3, "action privada" significa una action definida dentro del mismo repositorio que la usa (`.github/actions/`), no una action en un repositorio con visibilidad privada. Un repositorio privado compartido en una organización es la distribución "interna" (A5.3).

## Los tres modos en comparación

La diferencia entre los modos es fundamentalmente sobre qué repositorios pueden usar la action:

| Modo | Visibilidad del repo | Acceso desde | Sintaxis `uses:` |
|------|---------------------|--------------|-----------------|
| Local (privada) | Cualquiera | Solo el mismo repo | `./.github/actions/nombre` |
| Pública | Público | Cualquier workflow en GitHub | `owner/repo@ref` |
| Interna | Privado (org/enterprise) | Repos de la misma org | `owner/repo@ref` (con acceso configurado) |

## Action local (privada al repositorio)

Una action local se define en el directorio `.github/actions/` del mismo repositorio que la usa. Solo los workflows de ese repositorio pueden invocarla; no es accesible desde repositorios externos. La sintaxis de referencia usa una ruta relativa con `./` en lugar de `owner/repo@ref`.

Esta es la forma más común de encapsular lógica reutilizable dentro de un mismo repositorio: si varios workflows del repo necesitan los mismos pasos, se extrae a una action local en lugar de duplicar código.

```yaml
# Invocar una action local definida en .github/actions/setup-env/
- uses: ./.github/actions/setup-env
  with:
    node-version: "20"
```

> [EXAMEN] La ruta relativa `./` es obligatoria para actions locales. Sin `./`, GitHub interpreta el valor como `owner/repo` y busca un repositorio externo con ese nombre.

## Action pública

Una action pública está definida en un repositorio con visibilidad pública en GitHub. Cualquier workflow en cualquier repositorio puede invocarla usando la sintaxis `owner/repo@ref`. Es el modelo de distribución del GitHub Marketplace: las actions publicadas ahí son todas públicas.

```yaml
# Invocar una action pública del marketplace
- uses: actions/checkout@v4
- uses: actions/setup-node@v4
```

La referencia completa puede incluir un subdirectorio si la action no está en la raíz del repositorio:

```yaml
# Action pública en subdirectorio
- uses: owner/repo/path/to/action@ref
```

## Action interna (GitHub Enterprise / organización)

Una action interna está definida en un repositorio privado de una organización o empresa, pero se comparte con todos los repositorios de esa organización. En GitHub Enterprise, los repositorios con visibilidad `internal` son accesibles desde cualquier repo de la organización sin necesidad de que el repositorio sea público.

Para que los workflows puedan acceder a actions definidas en repositorios privados de la misma organización, el administrador debe habilitar la opción "Allow GitHub Actions to access repositories in this organization" en la configuración de la organización.

> [CONCEPTO] La distribución interna es el patrón empresarial para compartir actions propietarias sin exponerlas públicamente. El repositorio de la action es privado/internal, pero los workflows de la org pueden invocarlo igual que si fuera público.

## Configuración `internal` en GitHub Enterprise

En GitHub Enterprise Cloud o Server, los repositorios pueden tener visibilidad `internal`: son privados para el mundo exterior pero accesibles para todos los miembros de la organización/empresa. Una action en un repositorio `internal` puede ser usada por cualquier workflow de la organización sin configuración adicional en el nivel del repositorio, siempre que la política de la organización lo permita.

La configuración de qué actions están permitidas a nivel de organización (`Allow all actions`, `Allow local actions only`, `Allow select actions`) es una política de D4; este fichero documenta los modos de distribución, no las restricciones de política.

## Usar actions de repos privados del mismo owner

Si el repositorio de la action es privado (no `internal`) pero pertenece al mismo owner (usuario u organización) que el repositorio que la invoca, se puede usar con la sintaxis estándar `owner/repo/.github/actions/nombre@ref`, siempre que la acción tenga permisos de acceso entre repositorios configurados.

La sintaxis para una action en un subdirectorio de un repo privado del mismo owner es:

```yaml
- uses: mi-org/mi-repo-privado/.github/actions/mi-action@main
```

## Local action vs. action externa

La diferencia entre una action local (ruta `./`) y una action externa (`owner/repo@ref`) es el origen del código. Una action local se carga del checkout actual del repositorio en el runner: si el repositorio fue clonado en el paso `actions/checkout`, la action local existe en el workspace y puede ser invocada sin necesidad de descargarla por separado. Una action externa se descarga del repositorio remoto indicado en `uses:`, independientemente del contenido del workspace.

> [ADVERTENCIA] Una action local (`./.github/actions/...`) requiere que el repositorio haya sido clonado primero con `actions/checkout`. Si se invoca antes del checkout, el runner no encontrará el directorio de la action y el job fallará.

## Ejemplo central

El siguiente workflow demuestra los tres modos de distribución en un mismo job. Requiere que el repositorio contenga la action local en `.github/actions/saludar/`.

```yaml
# .github/workflows/demo-distribucion.yml
name: Demo modos de distribución

on: [push]

jobs:
  distribucion:
    runs-on: ubuntu-latest

    steps:
      # Action local: definida en este mismo repo
      # Requiere checkout previo para que ./.github/actions/saludar/ exista
      - name: Checkout del repositorio
        uses: actions/checkout@v4

      - name: Usar action local
        uses: ./.github/actions/saludar
        with:
          nombre: "equipo"

      # Action pública: repo público en GitHub
      - name: Setup Node (action pública del marketplace)
        uses: actions/setup-node@v4
        with:
          node-version: "20"

      # Action de repositorio privado del mismo owner (requiere acceso configurado)
      # - name: Usar action interna
      #   uses: mi-org/actions-internas/.github/actions/deploy@v1
```

```yaml
# .github/actions/saludar/action.yml
name: Saludar
description: Action local de ejemplo

inputs:
  nombre:
    description: Nombre a saludar
    required: true

runs:
  using: composite
  steps:
    - name: Mostrar saludo
      shell: bash
      run: echo "Hola, ${{ inputs.nombre }}"
```

## Tabla de elementos clave

| Concepto | Descripción |
|----------|-------------|
| Action local | En `.github/actions/` del mismo repo; referenciada con `./` |
| Action pública | En repo público; accesible desde cualquier workflow |
| Action interna | En repo privado/internal de la org; accesible desde la org |
| `uses: ./path` | Referencia a action local (relativa al workspace) |
| `uses: owner/repo@ref` | Referencia a action externa (pública o privada con acceso) |
| `uses: owner/repo/subdir@ref` | Action en subdirectorio del repo externo |

## Buenas y malas prácticas

**Hacer:**
- **Usar actions locales para encapsular lógica que solo necesita este repositorio** — razón: no expone la lógica externamente, la mantiene versionada con el repositorio y no requiere gestión de releases separada.
- **Poner el checkout antes de invocar cualquier action local** — razón: sin `actions/checkout`, el directorio `.github/actions/` no existe en el workspace del runner y la invocación falla.
- **Usar distribución interna en entornos enterprise para actions propietarias** — razón: permite compartir actions entre todos los repositorios de la organización sin exponerlas públicamente, protegiendo la lógica interna.

**Evitar:**
- **Referenciar una action local sin el prefijo `./`** — razón: GitHub interpreta `mi-action` como `owner/mi-action` y busca un repositorio externo inexistente, causando error de `not found`.
- **Mezclar la visibilidad del repositorio con el modo de distribución de la action** — razón: el modo de distribución depende de dónde está definida la action y cómo se referencia, no solo de la visibilidad del repo.
- **Publicar en el Marketplace una action que usa lógica propietaria interna** — razón: el Marketplace requiere que el repositorio sea público; cualquier secreto o lógica sensible en el código quedaría expuesto.

## Verificación y práctica

### Preguntas de examen

**Pregunta 1.** Un workflow intenta invocar `uses: ./.github/actions/build` pero el job falla con "No such file or directory". ¿Cuál es la causa más probable?

- A) La action no está publicada en el Marketplace
- B) La acción está en un repo privado
- **C) El repositorio no fue clonado antes de invocar la action local** ✅
- D) Las acciones locales requieren `runs.using: composite`

*A es incorrecta*: las actions locales no necesitan el Marketplace. *B es incorrecta*: la action local está en el mismo repo, no en uno externo. *D es incorrecta*: las actions locales pueden ser de cualquier tipo.

---

**Pregunta 2.** ¿Cuál de estas sintaxis es correcta para invocar una action definida en `.github/actions/deploy/` del mismo repositorio?

- A) `uses: actions/deploy@v1`
- **B) `uses: ./.github/actions/deploy`** ✅
- C) `uses: ./actions/deploy`
- D) `uses: local/deploy`

*A es incorrecta*: esa sintaxis busca `owner/repo` externo llamado `actions/deploy`. *C es incorrecta*: la ruta debe incluir `.github/`. *D es incorrecta*: no existe tal convención en GitHub Actions.

---

**Ejercicio práctico.** Un repositorio tiene una composite action en `.github/actions/lint-check/action.yml`. Escribe el fragmento de workflow que: (1) clona el repositorio, (2) invoca la action local pasando el input `fail-on-error: true`, y (3) tras la action, ejecuta `echo "Lint completado"`.

```yaml
steps:
  - name: Checkout
    uses: actions/checkout@v4

  - name: Ejecutar lint-check
    uses: ./.github/actions/lint-check
    with:
      fail-on-error: "true"

  - name: Confirmar resultado
    run: echo "Lint completado"
```

---

← [3.5 Version pinning e immutable actions](gha-action-version-pinning.md) | [Índice](README.md) | [3.7 Publicación en el GitHub Marketplace y release strategies](gha-action-publicacion-marketplace.md) →
