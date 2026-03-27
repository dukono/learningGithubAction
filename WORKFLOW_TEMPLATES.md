# 🏢 GitHub Actions: Workflow Templates y Administración Avanzada

## Índice

1. [Workflow Templates (Starter Workflows)](#1-workflow-templates-starter-workflows)
2. [Custom Deployment Protection Rules](#2-custom-deployment-protection-rules)
3. [Prevent Self-Review en Environments](#3-prevent-self-review-en-environments)
4. [Allow Administrators to Bypass](#4-allow-administrators-to-bypass)
5. [Deployment Branch & Tag Policies](#5-deployment-branch--tag-policies)
6. [JIT Runners (Just-in-Time)](#6-jit-runners-just-in-time)
7. [run-name: Nombrar Ejecuciones](#7-run-name-nombrar-ejecuciones)
8. [merge_group Trigger](#8-merge_group-trigger)
9. [Preguntas de Examen](#9-preguntas-de-examen)

---

## 1. Workflow Templates (Starter Workflows)

Los **Workflow Templates** (también llamados *Starter Workflows*) permiten a las organizaciones crear plantillas de workflows que los miembros pueden reutilizar al crear nuevos workflows en sus repositorios.

### Cómo crear un Workflow Template

1. En la organización, crear (o usar) un repositorio especial llamado **`.github`**
2. Dentro de ese repo, crear el directorio `workflow-templates/`
3. Añadir dos archivos por cada template:
   - `mi-template.yml` — el archivo de workflow
   - `mi-template.properties.json` — metadata del template

**Estructura del repositorio `.github` de la organización:**
```
.github/
└── workflow-templates/
    ├── ci-java.yml
    ├── ci-java.properties.json
    ├── deploy-k8s.yml
    └── deploy-k8s.properties.json
```

### Archivo de workflow template

```yaml
# .github/workflow-templates/ci-java.yml
name: CI Java

on:
  push:
    branches: [ $default-branch ]  # Variable especial — se reemplaza al instanciar
  pull_request:
    branches: [ $default-branch ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
      - name: Build con Maven
        run: mvn --batch-mode --update-snapshots package
```

> **Variable especial `$default-branch`**: Se reemplaza automáticamente por la rama por defecto del repositorio donde se instancia el template (ej: `main`).

### Archivo de properties

```json
// .github/workflow-templates/ci-java.properties.json
{
    "name": "CI Java con Maven",
    "description": "Pipeline de integración continua para proyectos Java con Maven",
    "iconName": "java",
    "categories": [
        "Java",
        "Maven"
    ],
    "filePatterns": [
        "pom.xml$",
        "\\.java$"
    ]
}
```

| Campo | Descripción |
|---|---|
| `name` | Nombre mostrado en la UI |
| `description` | Descripción del template |
| `iconName` | Icono (usa nombres de Octicons) |
| `categories` | Categorías para filtrar en la UI |
| `filePatterns` | Regex de archivos — si el repo contiene estos archivos, el template se sugiere automáticamente |

### Cómo usar un Workflow Template

En cualquier repositorio de la organización:
1. Ir a la pestaña **Actions**
2. Si no hay workflows, se muestra la lista de templates disponibles
3. Seleccionar el template y hacer clic en **"Configure"**
4. Se abre el editor con el workflow pre-rellenado para personalizar

Los templates también aparecen junto a los templates de GitHub en **"New workflow"**.

---

## 2. Custom Deployment Protection Rules

Las **Custom Deployment Protection Rules** permiten usar una **GitHub App** externa para aprobar o rechazar despliegues a un environment, en lugar de (o además de) los reviewers manuales.

### Casos de uso

- Verificar que un ticket de cambio está aprobado en Jira/ServiceNow
- Comprobar que las métricas de la aplicación están dentro de umbrales antes de desplegar
- Integrar con sistemas de gestión de cambios empresariales
- Verificar conformidad con políticas de seguridad

### Cómo funciona

1. Crear una **GitHub App** que implemente el webhook `deployment_protection_rule`
2. Instalar la app en la organización
3. En el environment, añadir la app como **deployment protection rule**

Cuando un workflow llega al job con ese environment:
1. GitHub llama al webhook de la app
2. La app evalúa su lógica (consulta Jira, verifica métricas, etc.)
3. La app responde con `approved` o `rejected` vía la API de GitHub
4. El workflow continúa o falla según la respuesta

**Configuración en el environment (UI de GitHub):**
```
Settings → Environments → [nombre] → Deployment protection rules
→ + Add rule → seleccionar la GitHub App instalada
```

### Diferencia con Required Reviewers

| | Required Reviewers | Custom Protection Rules |
|---|---|---|
| **Quién aprueba** | Personas/equipos de GitHub | Sistema externo (app) |
| **Automatización** | Manual | Totalmente automatizable |
| **Integración externa** | No | Sí (Jira, ServiceNow, etc.) |
| **Lógica personalizada** | No | Sí |

---

## 3. Prevent Self-Review en Environments

Por defecto, quien crea el PR puede ser también el reviewer del deployment. La opción **"Prevent self-review"** lo evita.

**Configuración:**
```
Settings → Environments → [nombre] → Required reviewers
→ ☑ Prevent self-review
```

Con esta opción activada:
- El usuario que disparó el workflow **no puede ser** el revisor de ese deployment
- Si los únicos reviewers configurados son el propio usuario, el deployment queda bloqueado
- Util en organizaciones con controles de separación de funciones (SOD)

---

## 4. Allow Administrators to Bypass

Por defecto, los administradores del repositorio también deben esperar la aprobación de los reviewers de un environment. La opción **"Allow administrators to bypass configured protection rules"** les permite saltarse esa espera.

**Configuración:**
```
Settings → Environments → [nombre]
→ ☑ Allow administrators to bypass configured protection rules
```

> ⚠️ **Consideración de seguridad**: Activar este bypass puede comprometer los controles de acceso. Se recomienda desactivarlo en environments críticos de producción.

---

## 5. Deployment Branch & Tag Policies

Controlan qué ramas y tags pueden desplegarse a un environment.

### Opciones disponibles

| Opción | Descripción |
|---|---|
| **No restriction** | Cualquier rama/tag puede desplegar |
| **Protected branches only** | Solo ramas con reglas de protección |
| **Selected branches and tags** | Solo las ramas/tags que especifiques |

### Configuración con patrones

```
Settings → Environments → [nombre] → Deployment branches and tags
→ Selected branches and tags → Add deployment branch or tag rule
```

**Patrones soportados:**
```
main              # Solo la rama main
release/*         # Cualquier rama que empiece por release/
v[0-9]+.[0-9]+.*  # Tags con formato semver (ej: v1.2.3)
```

**En el workflow**, si el branch no cumple la política, el deployment queda en estado `pending` y luego es rechazado automáticamente.

---

## 6. JIT Runners (Just-in-Time)

Los **JIT runners** (Just-in-Time) son una alternativa más segura a los runners efímeros registrados con `--ephemeral`.

### Diferencia con runners normales y --ephemeral

| | Runner normal | `--ephemeral` | JIT Runner |
|---|---|---|---|
| **Token de registro** | Largo plazo | Largo plazo | **Token de un solo uso** |
| **Reutilizable** | Sí | No | No |
| **Token robado** | Puede registrar muchos runners | Puede registrar 1 runner | **No puede registrar nada** |
| **Configuración** | `config.sh --token` | `config.sh --ephemeral --token` | API Just-in-Time |

### Cómo crear un JIT runner

1. Obtener un JIT token vía API antes de iniciar el runner:
```bash
# Para un repositorio
curl -X POST \
  -H "Authorization: Bearer $GITHUB_TOKEN" \
  -H "Accept: application/vnd.github+json" \
  https://api.github.com/repos/{owner}/{repo}/actions/runners/generate-jitconfig \
  -d '{
    "name": "my-jit-runner",
    "runner_group_id": 1,
    "labels": ["self-hosted", "linux"],
    "work_folder": "_work"
  }'
```

2. La respuesta incluye `encoded_jit_config` — un token de un solo uso
3. Iniciar el runner con ese token:
```bash
./run.sh --jitconfig $ENCODED_JIT_CONFIG
```

### Flujo típico con Kubernetes / contenedores

```
Job encolado → Controller crea pod → Pod obtiene JIT token →
Pod inicia runner con token → Runner ejecuta job → Pod termina
```

El token JIT solo sirve para registrar **ese runner específico**, una sola vez. Si se filtra, no puede usarse para registrar runners adicionales.

---

## 7. run-name: Nombrar Ejecuciones

El campo `run-name` permite personalizar el nombre que aparece en la lista de runs del workflow (pestaña Actions).

```yaml
name: CI Pipeline
run-name: "Deploy ${{ github.ref_name }} por ${{ github.actor }}"

on:
  push:
    branches: [main, 'release/*']
```

**Más ejemplos:**
```yaml
# Incluir el mensaje del commit
run-name: "[${{ github.event_name }}] ${{ github.event.head_commit.message }}"

# Para workflow_dispatch con inputs
on:
  workflow_dispatch:
    inputs:
      environment:
        type: choice
        options: [staging, production]

run-name: "Deploy to ${{ inputs.environment }} by ${{ github.actor }}"
```

**Comportamiento:**
- Si no se define, el nombre por defecto es el nombre del workflow + el evento
- Soporta **expresiones** (`${{ }}`) pero **no** funciones como `format()` o `join()`
- Máximo **512 caracteres**
- Los emojis son soportados

---

## 8. merge_group Trigger

El evento `merge_group` se dispara cuando un Pull Request entra en una **Merge Queue** (cola de merge).

```yaml
on:
  pull_request:
    branches: [main]
  merge_group:
    types: [checks_requested]
```

### ¿Qué es una Merge Queue?

La Merge Queue agrupa varios PRs listos para merge y los integra de forma ordenada, verificando que cada combinación pasa los checks antes de hacer el merge a main.

**Flujo:**
1. PR aprobado → desarrollador lo añade a la queue
2. GitHub crea una rama temporal con el PR + el estado actual de main
3. Se ejecutan los workflows con trigger `merge_group`
4. Si pasan, GitHub hace el merge; si fallan, el PR se saca de la queue

### Por qué añadir `merge_group` junto a `pull_request`

```yaml
# Sin merge_group: los checks en la queue no se ejecutan
on:
  pull_request: {}  # Solo se ejecuta en el PR abierto

# Con merge_group: se ejecuta tanto al abrir el PR como en la queue
on:
  pull_request: {}
  merge_group:
    types: [checks_requested]
```

Si los required checks solo tienen `pull_request` pero no `merge_group`, la queue esperará indefinidamente porque los checks nunca se ejecutarán en ese contexto.

### Contexto disponible en merge_group

```yaml
- name: Info del merge group
  run: |
    echo "Base SHA: ${{ github.event.merge_group.base_sha }}"
    echo "Head SHA: ${{ github.event.merge_group.head_sha }}"
    echo "Base ref: ${{ github.event.merge_group.base_ref }}"
    echo "Head ref: ${{ github.event.merge_group.head_ref }}"
```

---

## 9. Preguntas de Examen

**P: ¿Dónde se almacenan los Workflow Templates de una organización?**
> En el repositorio especial `.github` de la organización, dentro del directorio `workflow-templates/`.

**P: ¿Qué hace `$default-branch` en un Workflow Template?**
> Es una variable especial que se reemplaza automáticamente por la rama por defecto del repositorio donde se instancia el template.

**P: ¿Cuál es la diferencia entre un JIT runner y un runner con `--ephemeral`?**
> Ambos son de un solo uso, pero el JIT runner usa un token de registro de un solo uso — si se filtra, no puede usarse para registrar runners adicionales. El runner con `--ephemeral` usa un token reutilizable para el registro.

**P: ¿Para qué sirve "Prevent self-review" en un environment?**
> Evita que el usuario que disparó el workflow sea también quien lo aprueba en el environment. Implementa separación de funciones (SOD).

**P: ¿Qué ocurre si un workflow tiene `on: pull_request` pero no `on: merge_group` y ese workflow es un required check?**
> La Merge Queue quedará bloqueada esperando que el check se ejecute, pero nunca se ejecutará porque el trigger `merge_group` no está configurado.

**P: ¿Qué campo controla el nombre que aparece en la lista de runs del workflow?**
> `run-name` a nivel del workflow. Soporta expresiones `${{ }}`.

**P: ¿Qué son las Custom Deployment Protection Rules?**
> Son reglas que delegan la aprobación de un deployment a una GitHub App externa, permitiendo integrar con sistemas como Jira, ServiceNow o validaciones automáticas de métricas.
