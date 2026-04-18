> anterior: [2.6 Interpretación de matrix strategy](gha-d2-matrix-interpretacion.md)
> siguiente: [2.8.1 Reusable workflows: sintaxis de llamada](gha-d2-reusable-workflows-consumo.md)

# 2.7 Starter workflows (workflow templates de organización)

Cuando una organización crece y tiene docenas de repositorios, mantener la consistencia en los pipelines de CI/CD se convierte en un reto real. Sin una solución centralizada, cada equipo inventa su propio workflow desde cero, duplicando esfuerzo y creando inconsistencias: unos usan Node 18, otros Node 20; unos despliegan con una acción, otros con otra. Los starter workflows existen para resolver exactamente ese problema: permiten que los administradores de la organización definan plantillas aprobadas que cualquier desarrollador puede instanciar con un clic desde la interfaz de GitHub.

Un starter workflow es un fichero YAML almacenado en un repositorio especial de la organización que actúa como punto de partida para nuevos workflows. A diferencia de copiar un fichero manualmente, los starter workflows se integran directamente en la UI de GitHub Actions, apareciendo como opciones disponibles cuando un desarrollador hace clic en "New workflow" dentro de cualquier repositorio de la organización.

## El repositorio especial `.github` de la organización

GitHub reconoce un repositorio con el nombre exacto `.github` como repositorio especial a nivel de organización. Este repositorio no es simplemente un repositorio convencional: GitHub lo trata de forma privilegiada y lo usa para almacenar configuraciones y recursos que se aplican a toda la organización.

Para que los starter workflows funcionen, deben residir en una carpeta específica dentro de ese repositorio especial. La carpeta se llama `workflow-templates` y debe estar en la raíz del repositorio `.github`. GitHub no busca starter workflows en ninguna otra ubicación; si la carpeta tiene otro nombre o está anidada en un subdirectorio, los templates no aparecerán en la UI.

La siguiente estructura muestra cómo organizar correctamente el repositorio `.github` de una organización con starter workflows:

```
organización/.github/
├── workflow-templates/
│   ├── ci-node.yml                  ← el workflow template
│   ├── ci-node.properties.json      ← metadata del template
│   ├── ci-python.yml
│   ├── ci-python.properties.json
│   └── deploy-production.yml
│       deploy-production.properties.json
├── ISSUE_TEMPLATE/
│   └── bug_report.md
└── PULL_REQUEST_TEMPLATE.md
```

Cada starter workflow requiere dos ficheros que deben tener exactamente el mismo nombre base: el fichero YAML del workflow y un fichero `.properties.json` con los metadatos. Ambos deben estar en `workflow-templates/`. Si el fichero `.properties.json` no existe, el template no aparecerá en la UI de la organización.

## El fichero `.properties.json`: metadatos del starter workflow

El fichero de propiedades es lo que convierte un YAML ordinario en un starter workflow reconocido por GitHub. Sin este fichero, el YAML en `workflow-templates/` es simplemente un fichero más; con él, GitHub lo registra como plantilla disponible para los repositorios de la organización.

Las propiedades que puede contener el fichero `.properties.json` son las siguientes:

| Propiedad | Tipo | Requerido | Descripción |
|---|---|---|---|
| `name` | string | Sí | Nombre visible del template en la UI de GitHub |
| `description` | string | Sí | Descripción breve que aparece bajo el nombre |
| `iconName` | string | No | Nombre del icono de Octicons a mostrar (sin prefijo `octicon-`) |
| `iconColor` | string | No | Color del icono en formato nombre CSS (`red`, `blue`, `green`, etc.) |
| `categories` | array de strings | No | Categorías para clasificar el template (ej. `["JavaScript", "Node"]`) |
| `filePatterns` | array de strings | No | Patrones de ficheros que sugieren automáticamente el template |

La propiedad `filePatterns` es especialmente útil: si se define con `["package.json$"]`, GitHub sugerirá automáticamente ese starter workflow cuando detecte que el repositorio contiene un fichero `package.json`. Esto mejora la experiencia de onboarding porque los developers ven sugerencias relevantes para su stack tecnológico.

## Ejemplo completo: starter workflow para Node.js

El siguiente ejemplo muestra un starter workflow de CI para proyectos Node.js. Hay que prestar especial atención al placeholder `$default-branch`, que se explicará en la sección siguiente.

El fichero `workflow-templates/ci-node.yml`:

```yaml
name: Node.js CI

on:
  push:
    branches: [ "$default-branch" ]
  pull_request:
    branches: [ "$default-branch" ]

jobs:
  build:
    runs-on: ubuntu-latest

    strategy:
      matrix:
        node-version: [18.x, 20.x, 22.x]

    steps:
      - name: Checkout repository
        uses: actions/checkout@v4

      - name: Use Node.js ${{ matrix.node-version }}
        uses: actions/setup-node@v4
        with:
          node-version: ${{ matrix.node-version }}
          cache: 'npm'

      - name: Install dependencies
        run: npm ci

      - name: Run tests
        run: npm test

      - name: Build project
        run: npm run build --if-present
```

El fichero `workflow-templates/ci-node.properties.json`:

```json
{
  "name": "Node.js CI",
  "description": "Build and test a Node.js project with multiple versions using npm.",
  "iconName": "node",
  "iconColor": "green",
  "categories": [
    "JavaScript",
    "Node"
  ],
  "filePatterns": [
    "package.json$",
    "^Makefile$"
  ]
}
```

Ambos ficheros deben llamarse exactamente igual salvo por la extensión: `ci-node.yml` y `ci-node.properties.json`. Si los nombres no coinciden, el template no funcionará correctamente.

## El placeholder `$default-branch`

El placeholder `$default-branch` es una funcionalidad exclusiva de los starter workflows que no existe en workflows normales. Cuando un desarrollador instancia un starter workflow desde la UI de GitHub, GitHub reemplaza automáticamente todas las ocurrencias de `$default-branch` por el nombre real de la rama por defecto del repositorio destino.

Esto es importante porque no todos los repositorios usan la misma rama por defecto. Algunos usan `main`, otros usan `master`, y algunos proyectos legacy pueden usar `develop` o incluso nombres personalizados. Si el starter workflow codificara `main` directamente, fallaría silenciosamente en repositorios cuya rama principal se llame de otra forma.

El placeholder solo funciona en el contexto de starter workflows. Si se copia ese YAML directamente a un repositorio sin pasar por la UI de GitHub, el string `$default-branch` no se reemplazará y el workflow fallará porque `$default-branch` no es una rama válida. Este es un error común que aparece en la certificación.

> **Clave de examen:** `$default-branch` solo se sustituye cuando el workflow se instancia desde la UI de GitHub Actions. En un workflow copiado manualmente, debe reemplazarse a mano por el nombre real de la rama.

## Cómo acceder a los starter workflows desde la UI

El proceso para un desarrollador es deliberadamente simple. Dentro de cualquier repositorio de la organización, el desarrollador navega a la pestaña "Actions". Si el repositorio no tiene workflows, GitHub muestra directamente la página de selección de templates. Si ya tiene workflows, debe hacer clic en el botón "New workflow".

En la página de selección, GitHub muestra dos secciones diferenciadas: los workflows sugeridos por GitHub (para proyectos open source como Node.js CI, Python CI, etc.) y, más arriba o en una sección separada titulada con el nombre de la organización, los starter workflows definidos por la propia organización.

El desarrollador selecciona el template deseado y hace clic en "Configure". GitHub abre el editor de workflows con el YAML pre-rellenado, habiendo ya sustituido `$default-branch` por el nombre correcto. El desarrollador puede ajustar el workflow si lo necesita antes de hacer commit, pero la mayor parte del trabajo ya está hecho.

> **Requisito de visibilidad:** Para que los starter workflows de una organización aparezcan en la UI, el repositorio `.github` de la organización debe ser **público**. Si es privado, los templates solo serán visibles para los miembros de la organización con acceso al repositorio `.github`.

## Diferencia entre starter workflow y workflow template de repositorio

Es importante no confundir dos conceptos que suenan similares pero son mecanismos distintos con propósitos diferentes.

Un **starter workflow** (o workflow template de organización) vive en el repositorio `.github` de la organización bajo `workflow-templates/`. Su propósito es estandarizar la forma en que los nuevos proyectos empiezan. Cuando se instancia, se copia el YAML al repositorio destino y a partir de ese momento ese repositorio tiene su propia copia independiente. Los cambios posteriores al template original no afectan a los workflows ya instanciados.

Un **reusable workflow** es un mecanismo completamente diferente: no se copia, sino que se llama desde otros workflows con la sintaxis `uses`. El workflow reutilizable se ejecuta en el contexto del workflow que lo llama y los cambios en el reusable workflow afectan a todos los callers en tiempo de ejecución. Los reusable workflows son para compartir lógica de forma dinámica, mientras que los starter workflows son para compartir puntos de partida.

La siguiente tabla resume las diferencias clave:

| Característica | Starter workflow | Reusable workflow |
|---|---|---|
| Ubicación | `org/.github/workflow-templates/` | Cualquier repo, carpeta `.github/workflows/` |
| Mecanismo | Se copia al repositorio destino | Se llama con `uses: org/repo/.github/workflows/file.yml` |
| Actualización | No afecta a copias ya creadas | Afecta a todos los callers |
| Propósito | Punto de partida estandarizado | Lógica compartida reutilizable |
| Acceso | UI de GitHub "New workflow" | Referencia directa en el YAML |
| Archivo metadata | Requiere `.properties.json` | No requiere metadata extra |
| `$default-branch` | Soportado | No aplica |

## Casos de uso de starter workflows en la organización

Los starter workflows son especialmente valiosos en organizaciones con muchos repositorios o con equipos que crean repositorios frecuentemente. Proporcionan una forma de garantizar que todos los proyectos nuevos siguen los estándares de la organización desde el primer día.

Un caso de uso común es la estandarización de herramientas de análisis de código. Si la organización ha adoptado SonarQube o CodeQL para análisis de seguridad, puede crear un starter workflow que configure esa herramienta correctamente. En lugar de que cada equipo busque documentación y configure la integración manualmente, simplemente seleccionan el template aprobado.

Otro caso de uso es la estandarización de procesos de release y versionado. Si la organización usa Semantic Release o una convención específica de tags, un starter workflow puede incluir los pasos necesarios ya configurados. Esto evita que equipos implementen sus propias variantes incompatibles con las herramientas de monitorización corporativas.

Las organizaciones con requisitos de compliance también se benefician de los starter workflows. Si todos los deployments deben pasar por un gate de aprobación, o si debe generarse un SBOM (Software Bill of Materials), esos pasos pueden estar incluidos en el template aprobado, asegurando que ningún equipo los omita por desconocimiento.

## Buenas y malas prácticas

Las siguientes prácticas aplican tanto al diseño de starter workflows como a su uso.

**Usar `$default-branch` siempre en starter workflows** — nunca hardcodear `main` o `master`. Si el repositorio destino usa una rama principal diferente, el workflow fallará o no se activará. El placeholder existe precisamente para este propósito y su uso es obligatorio en un starter workflow bien diseñado.

**No hacer:** hardcodear la rama en el trigger.
```yaml
# MAL: fallará en repos con rama principal distinta de main
on:
  push:
    branches: [ "main" ]
```

**Hacer:** usar el placeholder para que GitHub lo resuelva al instanciar.
```yaml
# BIEN: GitHub sustituirá $default-branch por la rama real
on:
  push:
    branches: [ "$default-branch" ]
```

---

**Incluir siempre el fichero `.properties.json`** — sin el fichero de metadatos, el YAML en `workflow-templates/` no aparece en la UI. No sirve de nada tener el workflow definido si los desarrolladores no pueden encontrarlo. El fichero `name` y `description` son obligatorios; `categories` y `filePatterns` mejoran enormemente la experiencia de descubrimiento.

**No hacer:** dejar el `workflow-templates/` solo con ficheros `.yml` sin su correspondiente `.properties.json`.

**Hacer:** mantener siempre el par `nombre.yml` + `nombre.properties.json` en la misma carpeta.

---

**Documentar en la `description` del `.properties.json` qué hace el template** — la descripción aparece directamente en la UI de selección. Una descripción vaga como "CI workflow" no ayuda al desarrollador a decidir si ese template es el adecuado para su proyecto. Una descripción como "Build, test and publish a Node.js package to npm registry with semantic versioning" es mucho más útil.

**No hacer:**
```json
{
  "name": "CI",
  "description": "Continuous integration"
}
```

**Hacer:**
```json
{
  "name": "Node.js CI + Publish to npm",
  "description": "Run tests against Node 18/20/22, check coverage and publish to npm when a release tag is pushed.",
  "categories": ["JavaScript", "Node", "npm"]
}
```

---

**Mantener el repositorio `.github` público** — si el repositorio `.github` de la organización es privado, los starter workflows solo serán visibles para quienes tienen acceso a ese repositorio. Para que todos los miembros de la organización puedan ver y usar los templates al crear nuevos workflows, el repositorio debe ser público o al menos accesible para todos los miembros.

---

**Tratar los starter workflows como código, con revisión y versionado** — como cualquier otro código de infraestructura, los starter workflows deben estar sujetos a pull requests, revisión por pares y posiblemente tests. Un error en un starter workflow ampliamente adoptado puede propagarse a muchos repositorios si no se detecta antes de mergear.

## Verificación y práctica

Las siguientes preguntas replican el estilo de la certificación GH-200. Intenta responderlas antes de ver la respuesta.

**Pregunta 1.** Un desarrollador en tu organización no ve los starter workflows de la organización cuando hace clic en "New workflow" en un repositorio. El repositorio `.github` existe y contiene la carpeta `workflow-templates/` con ficheros `.yml`. ¿Cuál es la causa más probable?

<details>
<summary>Respuesta</summary>

Faltan los ficheros `.properties.json` correspondientes a cada `.yml`. Sin el fichero de metadatos, GitHub no registra el template en la UI. También podría ser que el repositorio `.github` sea privado y el developer no tenga acceso a él.

</details>

---

**Pregunta 2.** Un starter workflow contiene `branches: [ "$default-branch" ]`. Un desarrollador copia el fichero YAML directamente al directorio `.github/workflows/` de su repositorio sin usar la UI de GitHub. ¿Qué ocurre cuando hace push a la rama `main`?

<details>
<summary>Respuesta</summary>

El workflow no se activa. `$default-branch` es un placeholder que solo sustituye GitHub en el momento de instanciar el starter workflow desde la UI. Al copiarlo manualmente, el string literal `$default-branch` permanece en el YAML y no coincide con ninguna rama real. El developer debe reemplazarlo manualmente por `main` (o el nombre de su rama principal).

</details>

---

**Pregunta 3.** ¿Cuál es la diferencia fundamental entre un starter workflow y un reusable workflow?

<details>
<summary>Respuesta</summary>

Un starter workflow se copia al repositorio destino cuando se instancia; a partir de ese momento la copia es independiente del template original. Un reusable workflow no se copia: se llama en tiempo de ejecución con `uses:` y los cambios en el workflow reutilizable afectan a todos los repositorios que lo llaman. Los starter workflows son puntos de partida; los reusable workflows son lógica compartida dinámica.

</details>

---

**Ejercicio práctico.** Crea en tu organización (o simula la estructura) los ficheros necesarios para un starter workflow de Python CI que:

1. Se active en push y pull_request a la rama por defecto
2. Use Python 3.10, 3.11 y 3.12 en matrix
3. Instale dependencias con `pip install -r requirements.txt`
4. Ejecute tests con `pytest`
5. Tenga un `.properties.json` con nombre descriptivo, categorías `["Python"]` y `filePatterns` que detecte `requirements.txt`

Comprueba que los ficheros tienen el nombre base correcto y que el placeholder `$default-branch` está presente.

> anterior: [2.6 Interpretación de matrix strategy](gha-d2-matrix-interpretacion.md)
> siguiente: [2.8.1 Reusable workflows: sintaxis de llamada](gha-d2-reusable-workflows-consumo.md)
