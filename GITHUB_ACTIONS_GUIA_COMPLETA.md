# 📘 Guía Completa de GitHub Actions (Desde Cero)

## 📚 Tabla de Contenidos
1. [¿Qué es GitHub Actions?](#qué-es-github-actions)
2. [Conceptos Básicos](#conceptos-básicos)
3. [Estructura de un Workflow](#estructura-de-un-workflow)
4. [Triggers: Cuándo se ejecutan](#triggers-cuándo-se-ejecutan)
5. [Variables y Contextos](#variables-y-contextos)
6. [Trabajando con Python](#trabajando-con-python)
7. [Acciones (Actions) Predefinidas](#acciones-actions-predefinidas)
8. [Scripts y Comandos](#scripts-y-comandos)
9. [Secretos y Seguridad](#secretos-y-seguridad)
10. [Ejemplos Prácticos](#ejemplos-prácticos)
11. [Debugging y Troubleshooting](#debugging-y-troubleshooting)

---

## 🎯 ¿Qué es GitHub Actions?

**GitHub Actions** es un servicio de **automatización** que te permite ejecutar tareas automáticamente en respuesta a eventos en tu repositorio de GitHub.

### Analogía del Mundo Real
Imagina que tienes un **robot mayordomo** en tu casa:
- 🚪 Cuando alguien toca la puerta (evento) → El robot abre la puerta (acción)
- 📦 Cuando llega un paquete (evento) → El robot lo guarda en el almacén (acción)
- ⏰ Cada mañana a las 7am (evento) → El robot prepara café (acción)

**GitHub Actions es ese robot para tu código:**
- 📝 Cuando haces un commit → Ejecuta los tests
- 🔀 Cuando abres un Pull Request → Revisa la calidad del código
- 🏷️ Cuando creas un tag → Publica una nueva versión

### ¿Para qué sirve?
- ✅ Ejecutar tests automáticamente
- 🔍 Analizar calidad de código
- 📦 Compilar y desplegar aplicaciones
- 🔐 Verificar seguridad
- 📊 Generar reportes
- 🚀 Publicar releases
- 📧 Enviar notificaciones
- Y mucho más...

---

## 🧩 Conceptos Básicos

### 1. **Workflow (Flujo de Trabajo)**
Es un **archivo YAML** que define qué tareas ejecutar y cuándo.

**Ubicación:** `.github/workflows/mi-workflow.yml`

**Analogía:** Es como una **receta de cocina** que lista todos los pasos para hacer un plato.

### 2. **Event (Evento)**
Es el **disparador** que inicia el workflow.

**Ejemplos:**
- `push`: Cuando alguien hace push al repositorio
- `pull_request`: Cuando se crea/actualiza un PR
- `schedule`: A una hora específica (como un cron)
- `workflow_dispatch`: Manualmente

**Analogía:** Es el **timbre** que activa al robot mayordomo.

### 3. **Job (Trabajo)**
Es un **conjunto de pasos** que se ejecutan en el mismo entorno.

**Características:**
- Se ejecutan en paralelo por defecto
- Pueden tener dependencias entre ellos
- Cada job corre en su propia máquina virtual

**Analogía:** Son las **tareas grandes** como "limpiar la casa" o "hacer la compra".

### 4. **Step (Paso)**
Es una **tarea individual** dentro de un job.

**Tipos:**
- Ejecutar un comando shell
- Usar una acción predefinida
- Ejecutar un script

**Analogía:** Son los **pasos específicos** como "barrer el suelo" o "lavar los platos".

### 5. **Runner (Ejecutor)**
Es la **máquina** donde se ejecuta el workflow.

**Tipos:**
- **GitHub-hosted**: Máquinas de GitHub (gratis con límites)
  - Ubuntu
  - Windows
  - macOS
- **Self-hosted**: Tus propias máquinas

**Analogía:** Es el **robot físico** que hace las tareas.

### 6. **Action (Acción)**
Es un **bloque de código reutilizable** creado por la comunidad o por ti.

**Ejemplos:**
- `actions/checkout`: Descarga tu código
- `actions/setup-python`: Instala Python
- `actions/upload-artifact`: Guarda archivos

**Analogía:** Son **herramientas especializadas** como un "abridor de latas" o "aspiradora".

---

## 📄 Estructura de un Workflow

### Ejemplo Mínimo
```yaml
name: Mi Primer Workflow

on: push

jobs:
  saludar:
    runs-on: ubuntu-latest
    steps:
      - name: Decir Hola
        run: echo "¡Hola Mundo!"
```

### Desglose Línea por Línea

```yaml
name: Mi Primer Workflow
```
**Explicación:** Nombre descriptivo del workflow (aparece en GitHub UI)

---

```yaml
on: push
```
**Explicación:** Se ejecuta cuando alguien hace `push` a cualquier rama

---

```yaml
jobs:
```
**Explicación:** Define todos los trabajos del workflow

---

```yaml
  saludar:
```
**Explicación:** ID del job (puedes usar cualquier nombre sin espacios)

---

```yaml
    runs-on: ubuntu-latest
```
**Explicación:** Tipo de máquina donde ejecutar (Ubuntu más reciente)

**Opciones:**
- `ubuntu-latest` (Linux)
- `windows-latest` (Windows)
- `macos-latest` (macOS)

---

```yaml
    steps:
```
**Explicación:** Lista de pasos a ejecutar

---

```yaml
      - name: Decir Hola
```
**Explicación:** Nombre descriptivo del paso

---

```yaml
        run: echo "¡Hola Mundo!"
```
**Explicación:** Comando shell a ejecutar (en este caso, imprime "¡Hola Mundo!")

---

## 🎬 Triggers: Cuándo se ejecutan

### 1. **Push** (Cuando subes código)
```yaml
on: push
```

**Más específico:**
```yaml
on:
  push:
    branches:
      - main          # Solo en la rama main
      - develop       # Solo en develop
    paths:
      - 'src/**'      # Solo si cambian archivos en src/
      - '*.js'        # Solo archivos JavaScript
```

### 2. **Pull Request**
```yaml
on: pull_request
```

**Más específico:**
```yaml
on:
  pull_request:
    types:
      - opened        # Cuando se abre
      - synchronize   # Cuando se actualiza
      - reopened      # Cuando se reabre
    branches:
      - main          # Solo PRs hacia main
```

### 3. **Schedule** (Programado)
```yaml
on:
  schedule:
    - cron: '0 0 * * *'  # Todos los días a medianoche
```

**Formato Cron:**
```
┌───────────── minuto (0 - 59)
│ ┌───────────── hora (0 - 23)
│ │ ┌───────────── día del mes (1 - 31)
│ │ │ ┌───────────── mes (1 - 12)
│ │ │ │ ┌───────────── día de la semana (0 - 6) (0 = domingo)
│ │ │ │ │
* * * * *
```

**Ejemplos:**
```yaml
'0 0 * * *'      # Diario a medianoche
'0 */6 * * *'    # Cada 6 horas
'0 9 * * 1'      # Todos los lunes a las 9am
'0 0 1 * *'      # El día 1 de cada mes
```

### 4. **Manual** (workflow_dispatch)
```yaml
on:
  workflow_dispatch:
    inputs:
      nombre:
        description: 'Tu nombre'
        required: true
        default: 'Usuario'
```

Permite ejecutar el workflow manualmente desde GitHub UI con inputs personalizados.

### 5. **Múltiples Eventos**
```yaml
on:
  push:
    branches: [main]
  pull_request:
    branches: [main]
  schedule:
    - cron: '0 0 * * *'
```

---

## 🔤 Variables y Contextos

### ¿Qué son los Contextos?

Son **objetos** que contienen información sobre el workflow, el repositorio, los eventos, etc.

**Analogía:** Son como las **variables globales** que siempre están disponibles.

### Contextos Principales

#### 1. **`github`** - Información del evento y repositorio

**Variables generales más usadas:**

```yaml
# Información del repositorio
${{ github.repository }}              # usuario/repositorio
${{ github.repository_owner }}        # usuario o organización
${{ github.repository_name }}         # nombre del repositorio

# Información del evento
${{ github.event_name }}              # push, pull_request, schedule, etc.
${{ github.actor }}                   # Usuario que disparó el evento
${{ github.triggering_actor }}        # Usuario que realmente disparó (puede ser diferente)

# Información del commit
${{ github.sha }}                     # Hash del commit (40 caracteres)
${{ github.ref }}                     # refs/heads/main o refs/tags/v1.0.0
${{ github.ref_name }}                # main o v1.0.0 (sin refs/)
${{ github.ref_type }}                # branch o tag
${{ github.head_ref }}                # Rama del PR (solo en pull_request)
${{ github.base_ref }}                # Rama base del PR (solo en pull_request)

# URLs y paths
${{ github.workspace }}               # /home/runner/work/repo/repo
${{ github.server_url }}              # https://github.com
${{ github.api_url }}                 # https://api.github.com
${{ github.graphql_url }}             # https://api.github.com/graphql

# Información del workflow
${{ github.workflow }}                # Nombre del workflow
${{ github.run_id }}                  # ID único de la ejecución
${{ github.run_number }}              # Número secuencial de ejecución
${{ github.run_attempt }}             # Número de intento (re-run)
${{ github.job }}                     # ID del job actual
```

**Variables específicas para Pull Request:**

```yaml
# Información básica del PR
${{ github.event.pull_request.number }}           # Número del PR
${{ github.event.pull_request.title }}            # Título del PR
${{ github.event.pull_request.body }}             # Descripción del PR
${{ github.event.pull_request.draft }}            # true si es borrador, false si no
${{ github.event.pull_request.state }}            # open, closed
${{ github.event.pull_request.merged }}           # true si está merged
${{ github.event.pull_request.mergeable }}        # true si se puede mergear
${{ github.event.pull_request.locked }}           # true si está bloqueado

# Información del autor
${{ github.event.pull_request.user.login }}       # Usuario que creó el PR
${{ github.event.pull_request.user.id }}          # ID del usuario
${{ github.event.pull_request.user.type }}        # User, Bot, etc.

# Ramas
${{ github.event.pull_request.head.ref }}         # Rama origen
${{ github.event.pull_request.head.sha }}         # Commit de la rama origen
${{ github.event.pull_request.base.ref }}         # Rama destino
${{ github.event.pull_request.base.sha }}         # Commit de la rama destino

# Información de revisión
${{ github.event.pull_request.requested_reviewers }} # Revisores solicitados
${{ github.event.pull_request.requested_teams }}     # Equipos solicitados
${{ github.event.pull_request.assignees }}           # Asignados
${{ github.event.pull_request.labels }}              # Etiquetas (array)

# Estadísticas
${{ github.event.pull_request.changed_files }}    # Número de archivos cambiados
${{ github.event.pull_request.additions }}        # Líneas añadidas
${{ github.event.pull_request.deletions }}        # Líneas eliminadas
${{ github.event.pull_request.commits }}          # Número de commits

# URLs
${{ github.event.pull_request.html_url }}         # URL del PR
${{ github.event.pull_request.url }}              # URL de la API
${{ github.event.pull_request.diff_url }}         # URL del diff
${{ github.event.pull_request.patch_url }}        # URL del patch

# Fechas
${{ github.event.pull_request.created_at }}       # Fecha de creación
${{ github.event.pull_request.updated_at }}       # Última actualización
${{ github.event.pull_request.closed_at }}        # Fecha de cierre (si aplica)
${{ github.event.pull_request.merged_at }}        # Fecha de merge (si aplica)
```

**Variables específicas para Push:**

```yaml
${{ github.event.before }}                        # SHA antes del push
${{ github.event.after }}                         # SHA después del push
${{ github.event.commits }}                       # Array de commits
${{ github.event.head_commit.message }}           # Mensaje del último commit
${{ github.event.head_commit.author.name }}       # Nombre del autor
${{ github.event.head_commit.author.email }}      # Email del autor
${{ github.event.head_commit.timestamp }}         # Timestamp del commit
${{ github.event.pusher.name }}                   # Quien hizo push
${{ github.event.compare }}                       # URL para comparar cambios
```

**Ejemplo de uso básico:**
```yaml
- name: Mostrar información
  run: |
    echo "Evento: ${{ github.event_name }}"
    echo "Repositorio: ${{ github.repository }}"
    echo "Actor: ${{ github.actor }}"
    echo "Commit: ${{ github.sha }}"
    echo "Rama: ${{ github.ref_name }}"
```

**Ejemplo con Pull Request y Draft:**
```yaml
- name: Verificar si es PR draft
  if: github.event_name == 'pull_request'
  run: |
    echo "PR #${{ github.event.pull_request.number }}"
    echo "Título: ${{ github.event.pull_request.title }}"
    echo "Es draft: ${{ github.event.pull_request.draft }}"
    echo "Autor: ${{ github.event.pull_request.user.login }}"
    echo "De: ${{ github.event.pull_request.head.ref }}"
    echo "A: ${{ github.event.pull_request.base.ref }}"

- name: Solo ejecutar si NO es draft
  if: github.event_name == 'pull_request' && github.event.pull_request.draft == false
  run: echo "Este PR está listo para revisión"

- name: Solo ejecutar si ES draft
  if: github.event_name == 'pull_request' && github.event.pull_request.draft == true
  run: echo "Este PR es un borrador"
```

**Ejemplo con Labels:**
```yaml
- name: Verificar etiquetas
  if: github.event_name == 'pull_request'
  shell: python
  run: |
    import json
    import os
    
    # Obtener labels del PR
    labels_json = '''${{ toJson(github.event.pull_request.labels) }}'''
    labels = json.loads(labels_json)
    
    label_names = [label['name'] for label in labels]
    
    print(f"Etiquetas del PR: {', '.join(label_names)}")
    
    if 'bug' in label_names:
        print("🐛 Este PR corrige un bug")
    if 'enhancement' in label_names:
        print("✨ Este PR añade una mejora")

- name: Verificar si tiene etiqueta específica
  if: contains(github.event.pull_request.labels.*.name, 'urgent')
  run: echo "⚠️ Este PR es urgente"
```

#### 2. **`env`** - Variables de entorno

**Definir variables:**
```yaml
env:
  AMBIENTE: production
  VERSION: 1.0.0

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - name: Usar variable
        run: echo "Ambiente: $AMBIENTE"
```

**Niveles de variables:**
```yaml
# Nivel workflow (global)
env:
  GLOBAL: "disponible en todos"

jobs:
  mi-job:
    # Nivel job
    env:
      JOB: "disponible en este job"
    
    steps:
      # Nivel step
      - name: Mi paso
        env:
          STEP: "solo en este step"
        run: echo "$GLOBAL $JOB $STEP"
```

#### 3. **`secrets`** - Secretos encriptados

```yaml
- name: Usar secreto
  env:
    MI_TOKEN: ${{ secrets.GITHUB_TOKEN }}
  run: echo "Token configurado"
```

**Cómo crear secretos:**
1. Ve a tu repositorio en GitHub
2. Settings → Secrets and variables → Actions
3. New repository secret
4. Nombre: `MI_SECRETO`, Valor: `valor_secreto`

#### 4. **`vars`** - Variables de configuración

```yaml
- name: Usar variable de configuración
  run: echo "Entorno: ${{ vars.ENVIRONMENT }}"
```

Similar a secretos pero NO encriptados (para valores públicos).

#### 5. **`runner`** - Información del ejecutor

```yaml
${{ runner.os }}          # Sistema operativo (Linux, Windows, macOS)
${{ runner.arch }}        # Arquitectura (X64, ARM64)
${{ runner.temp }}        # Directorio temporal
```

#### 6. **`steps`** - Output de pasos anteriores

```yaml
steps:
  - name: Paso 1
    id: mi-paso
    run: echo "resultado=exitoso" >> $GITHUB_OUTPUT
  
  - name: Paso 2
    run: echo "El resultado fue: ${{ steps.mi-paso.outputs.resultado }}"
```

#### 7. **`job`** - Estado del job actual

```yaml
${{ job.status }}         # Estado (success, failure, cancelled)
```

#### 8. **`matrix`** - Valores de la matriz

```yaml
strategy:
  matrix:
    python-version: [3.8, 3.9, 3.10, 3.11]

steps:
  - name: Usar versión de matriz
    run: echo "Python ${{ matrix.python-version }}"
```

---

## 🐍 Trabajando con Python

### Setup Básico de Python

```yaml
name: Python Workflow

on: push

jobs:
  python-job:
    runs-on: ubuntu-latest
    
    steps:
      # 1. Descargar el código
      - name: Checkout code
        uses: actions/checkout@v4
      
      # 2. Instalar Python
      - name: Setup Python
        uses: actions/setup-python@v5
        with:
          python-version: '3.11'
      
      # 3. Verificar instalación
      - name: Verificar Python
        run: |
          python --version
          pip --version
```

### Instalar Dependencias

```yaml
- name: Instalar dependencias
  run: |
    pip install --upgrade pip
    pip install -r requirements.txt
```

### Ejecutar Tests

```yaml
- name: Ejecutar tests
  run: |
    pip install pytest
    pytest tests/
```

### Ejecutar Script Python

```yaml
- name: Ejecutar script
  run: python mi_script.py
```

### Script Python Inline

#### Opción 1: Shell Python
```yaml
- name: Script Python inline
  shell: python
  run: |
    import os
    print("¡Hola desde Python!")
    print(f"Rama: {os.getenv('GITHUB_REF_NAME')}")
```

#### Opción 2: Run con python -c
```yaml
- name: Cálculo en Python
  run: |
    python -c "
    import math
    resultado = math.sqrt(16)
    print(f'Raíz cuadrada: {resultado}')
    "
```

### Matriz de Versiones de Python

```yaml
jobs:
  test:
    runs-on: ubuntu-latest
    strategy:
      matrix:
        python-version: ['3.8', '3.9', '3.10', '3.11', '3.12']
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Setup Python ${{ matrix.python-version }}
        uses: actions/setup-python@v5
        with:
          python-version: ${{ matrix.python-version }}
      
      - name: Ejecutar tests
        run: pytest
```

Esto ejecuta los tests en **5 versiones diferentes de Python en paralelo**.

---

## 🔧 Python en GitHub Actions: Casos de Uso

### 1. Leer y Procesar YAML

```yaml
- name: Leer YAML
  shell: python
  run: |
    import yaml
    import os
    
    # Leer archivo YAML
    with open('application.yml', 'r') as f:
        data = yaml.safe_load(f)
    
    # Extraer valores
    project_name = data['metadata']['project_name']
    version = data['metadata']['version']
    
    # Imprimir
    print(f"Proyecto: {project_name}")
    print(f"Versión: {version}")
    
    # Guardar en variable de salida de GitHub
    github_output = os.getenv('GITHUB_OUTPUT')
    with open(github_output, 'a') as f:
        f.write(f'project-name={project_name}\n')
        f.write(f'version={version}\n')
```

**Usar el output:**
```yaml
- name: Usar valores extraídos
  run: |
    echo "Proyecto: ${{ steps.leer-yaml.outputs.project-name }}"
    echo "Versión: ${{ steps.leer-yaml.outputs.version }}"
```

### 2. Procesar JSON

```yaml
- name: Procesar JSON
  shell: python
  run: |
    import json
    
    # Parsear JSON desde variable de GitHub
    data = '''${{ toJson(github.event) }}'''
    event = json.loads(data)
    
    # Acceder a datos
    if 'pull_request' in event:
        pr_number = event['pull_request']['number']
        pr_title = event['pull_request']['title']
        print(f"PR #{pr_number}: {pr_title}")
```

### 3. Validar y Transformar Datos

```yaml
- name: Validar versión semántica
  shell: python
  run: |
    import re
    import sys
    
    version = "${{ github.ref_name }}"
    
    # Expresión regular para SemVer
    pattern = r'^v?(\d+)\.(\d+)\.(\d+)(-[a-zA-Z0-9.]+)?$'
    
    if re.match(pattern, version):
        print(f"✅ Versión válida: {version}")
    else:
        print(f"❌ Versión inválida: {version}")
        sys.exit(1)  # Falla el workflow
```

### 4. Generar Reportes

```yaml
- name: Generar reporte
  shell: python
  run: |
    import json
    from datetime import datetime
    
    # Datos del reporte
    report = {
        'fecha': datetime.now().isoformat(),
        'repositorio': '${{ github.repository }}',
        'commit': '${{ github.sha }}',
        'actor': '${{ github.actor }}',
        'tests_pasados': 42,
        'tests_fallados': 0
    }
    
    # Guardar como JSON
    with open('report.json', 'w') as f:
        json.dump(report, f, indent=2)
    
    print("✅ Reporte generado")
```

### 5. Interactuar con APIs

```yaml
- name: Llamar API
  shell: python
  run: |
    import requests
    import os
    
    # Token desde secreto
    token = os.getenv('GITHUB_TOKEN')
    
    # Headers
    headers = {
        'Authorization': f'Bearer {token}',
        'Accept': 'application/vnd.github.v3+json'
    }
    
    # Llamar GitHub API
    repo = '${{ github.repository }}'
    url = f'https://api.github.com/repos/{repo}/issues'
    
    response = requests.get(url, headers=headers)
    issues = response.json()
    
    print(f"Issues abiertas: {len(issues)}")
  env:
    GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

### 6. Procesar Archivos

```yaml
- name: Contar líneas de código
  shell: python
  run: |
    import os
    import pathlib
    
    total_lines = 0
    python_files = 0
    
    # Buscar archivos .py
    for py_file in pathlib.Path('.').rglob('*.py'):
        python_files += 1
        with open(py_file, 'r') as f:
            lines = len(f.readlines())
            total_lines += lines
            print(f"{py_file}: {lines} líneas")
    
    print(f"\n📊 Total: {python_files} archivos, {total_lines} líneas")
```

---

## 🎭 Acciones (Actions) Predefinidas

### Acciones Esenciales

#### 1. **Checkout** - Descargar código
```yaml
- name: Checkout
  uses: actions/checkout@v4
  with:
    fetch-depth: 0  # 0 = todo el historial, 1 = solo último commit
    ref: main       # rama específica
```

#### 2. **Setup Python** - Instalar Python
```yaml
- name: Setup Python
  uses: actions/setup-python@v5
  with:
    python-version: '3.11'
    cache: 'pip'  # Cachea dependencias de pip
```

#### 3. **Setup Node** - Instalar Node.js
```yaml
- name: Setup Node
  uses: actions/setup-node@v4
  with:
    node-version: '20'
    cache: 'npm'
```

#### 4. **Setup Java** - Instalar Java
```yaml
- name: Setup Java
  uses: actions/setup-java@v4
  with:
    distribution: 'temurin'
    java-version: '17'
    cache: 'maven'
```

#### 5. **Cache** - Cachear dependencias
```yaml
- name: Cache dependencies
  uses: actions/cache@v4
  with:
    path: ~/.cache/pip
    key: ${{ runner.os }}-pip-${{ hashFiles('requirements.txt') }}
    restore-keys: |
      ${{ runner.os }}-pip-
```

#### 6. **Upload Artifact** - Guardar archivos
```yaml
- name: Upload artifact
  uses: actions/upload-artifact@v4
  with:
    name: mi-reporte
    path: reports/
    retention-days: 30
```

#### 7. **Download Artifact** - Descargar archivos
```yaml
- name: Download artifact
  uses: actions/download-artifact@v4
  with:
    name: mi-reporte
    path: downloaded/
```

#### 8. **GitHub Script** - Ejecutar JavaScript con acceso a API
```yaml
- name: Comentar en PR
  uses: actions/github-script@v7
  with:
    script: |
      github.rest.issues.createComment({
        issue_number: context.issue.number,
        owner: context.repo.owner,
        repo: context.repo.repo,
        body: '✅ Build exitoso!'
      })
```

---

## 💻 Scripts y Comandos

### Comandos Multi-línea

```yaml
- name: Múltiples comandos
  run: |
    echo "Paso 1"
    mkdir -p output
    cd output
    echo "Hola" > archivo.txt
    cat archivo.txt
```

### Scripts Bash

```yaml
- name: Script Bash
  shell: bash
  run: |
    #!/bin/bash
    set -e  # Salir si hay error
    
    if [ -f "archivo.txt" ]; then
      echo "Archivo existe"
    else
      echo "Archivo no existe"
      exit 1
    fi
```

### Condicionales

```yaml
- name: Solo en main
  if: github.ref == 'refs/heads/main'
  run: echo "Estamos en main"

- name: Solo en PR
  if: github.event_name == 'pull_request'
  run: echo "Es un Pull Request"

- name: Solo si tiene éxito
  if: success()
  run: echo "El job fue exitoso"

- name: Siempre ejecutar
  if: always()
  run: echo "Esto siempre se ejecuta"

- name: Solo si falla
  if: failure()
  run: echo "Algo falló"
```

### Variables de Output

**Método 1: GITHUB_OUTPUT**
```yaml
- name: Generar output
  id: mi-paso
  run: |
    echo "fecha=$(date +'%Y-%m-%d')" >> $GITHUB_OUTPUT
    echo "hora=$(date +'%H:%M:%S')" >> $GITHUB_OUTPUT

- name: Usar output
  run: |
    echo "Fecha: ${{ steps.mi-paso.outputs.fecha }}"
    echo "Hora: ${{ steps.mi-paso.outputs.hora }}"
```

**Método 2: Python**
```yaml
- name: Generar output con Python
  id: calc
  shell: python
  run: |
    import os
    resultado = 2 + 2
    with open(os.getenv('GITHUB_OUTPUT'), 'a') as f:
        f.write(f'resultado={resultado}\n')

- name: Usar resultado
  run: echo "2 + 2 = ${{ steps.calc.outputs.resultado }}"
```

### Variables de Entorno

**Método 1: GITHUB_ENV**
```yaml
- name: Definir variable
  run: echo "MI_VARIABLE=valor123" >> $GITHUB_ENV

- name: Usar variable
  run: echo "Variable: $MI_VARIABLE"
```

**Método 2: Export**
```yaml
- name: Definir múltiples variables
  run: |
    export VAR1="valor1"
    export VAR2="valor2"
    echo "VAR1=$VAR1" >> $GITHUB_ENV
    echo "VAR2=$VAR2" >> $GITHUB_ENV
```

---

## 🔐 Secretos y Seguridad

### Usar Secretos

```yaml
jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - name: Deploy
        env:
          API_KEY: ${{ secrets.API_KEY }}
          DB_PASSWORD: ${{ secrets.DB_PASSWORD }}
        run: |
          # Los secretos NO se imprimen en logs
          echo "Desplegando con credenciales..."
          ./deploy.sh
```

### Secretos en Python

```yaml
- name: Script con secretos
  shell: python
  env:
    API_KEY: ${{ secrets.API_KEY }}
  run: |
    import os
    
    api_key = os.getenv('API_KEY')
    
    # NUNCA imprimas secretos
    # print(api_key)  # ❌ MAL
    
    if api_key:
        print("✅ API Key configurada")
    else:
        print("❌ API Key no encontrada")
```

### Token de GitHub Automático

GitHub proporciona un token automático `GITHUB_TOKEN` con permisos limitados:

```yaml
- name: Usar GITHUB_TOKEN
  env:
    GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
  run: |
    # Autenticar con GitHub CLI
    gh auth login --with-token <<< "$GITHUB_TOKEN"
    
    # Hacer operaciones
    gh pr list
```

**Permisos del GITHUB_TOKEN:**
```yaml
permissions:
  contents: read      # Leer código
  issues: write       # Escribir en issues
  pull-requests: write # Escribir en PRs
  checks: write       # Crear checks
```

---

## 📋 Ejemplos Prácticos Completos

### Ejemplo 1: Tests de Python con Coverage

```yaml
name: Python Tests

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest
    
    strategy:
      matrix:
        python-version: ['3.9', '3.10', '3.11']
    
    steps:
      - name: Checkout
        uses: actions/checkout@v4
      
      - name: Setup Python ${{ matrix.python-version }}
        uses: actions/setup-python@v5
        with:
          python-version: ${{ matrix.python-version }}
          cache: 'pip'
      
      - name: Instalar dependencias
        run: |
          pip install --upgrade pip
          pip install pytest pytest-cov
          if [ -f requirements.txt ]; then
            pip install -r requirements.txt
          fi
      
      - name: Ejecutar tests con coverage
        run: |
          pytest --cov=src --cov-report=xml --cov-report=html
      
      - name: Upload coverage
        uses: codecov/codecov-action@v3
        with:
          file: ./coverage.xml
          fail_ci_if_error: true
```

### Ejemplo 2: Linting y Formateo

```yaml
name: Code Quality

on: [push, pull_request]

jobs:
  lint:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Setup Python
        uses: actions/setup-python@v5
        with:
          python-version: '3.11'
      
      - name: Instalar herramientas
        run: |
          pip install black flake8 pylint mypy
      
      - name: Black (formateo)
        run: black --check .
      
      - name: Flake8 (linting)
        run: flake8 src/ tests/
      
      - name: Pylint
        run: pylint src/
      
      - name: MyPy (type checking)
        run: mypy src/
```

### Ejemplo 3: Build y Deploy

```yaml
name: Build and Deploy

on:
  push:
    tags:
      - 'v*'

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Setup Python
        uses: actions/setup-python@v5
        with:
          python-version: '3.11'
      
      - name: Build package
        run: |
          pip install build
          python -m build
      
      - name: Upload artifacts
        uses: actions/upload-artifact@v4
        with:
          name: dist
          path: dist/
  
  deploy:
    needs: build
    runs-on: ubuntu-latest
    
    steps:
      - name: Download artifacts
        uses: actions/download-artifact@v4
        with:
          name: dist
          path: dist/
      
      - name: Publish to PyPI
        env:
          TWINE_USERNAME: __token__
          TWINE_PASSWORD: ${{ secrets.PYPI_TOKEN }}
        run: |
          pip install twine
          twine upload dist/*
```

### Ejemplo 4: Análisis de Código Python

```yaml
name: Code Analysis

on: [push, pull_request]

jobs:
  analyze:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Setup Python
        uses: actions/setup-python@v5
        with:
          python-version: '3.11'
      
      - name: Analizar complejidad
        shell: python
        run: |
          import os
          import pathlib
          
          def count_lines(file_path):
              with open(file_path, 'r') as f:
                  lines = f.readlines()
                  code_lines = [l for l in lines if l.strip() and not l.strip().startswith('#')]
                  return len(code_lines)
          
          print("📊 Análisis de Código\n")
          print(f"{'Archivo':<40} {'Líneas':>10}")
          print("-" * 50)
          
          total = 0
          for py_file in pathlib.Path('src').rglob('*.py'):
              lines = count_lines(py_file)
              total += lines
              print(f"{str(py_file):<40} {lines:>10}")
          
          print("-" * 50)
          print(f"{'TOTAL':<40} {total:>10}")
          
          # Establecer límite
          if total > 10000:
              print("\n⚠️ Advertencia: Código muy grande")
      
      - name: Buscar TODOs
        shell: python
        run: |
          import pathlib
          
          todos = []
          for py_file in pathlib.Path('.').rglob('*.py'):
              with open(py_file, 'r') as f:
                  for i, line in enumerate(f, 1):
                      if 'TODO' in line or 'FIXME' in line:
                          todos.append(f"{py_file}:{i} - {line.strip()}")
          
          if todos:
              print("📝 TODOs encontrados:\n")
              for todo in todos:
                  print(f"  {todo}")
          else:
              print("✅ No hay TODOs pendientes")
```

### Ejemplo 5: Workflow con Matriz Compleja

```yaml
name: Multi-Platform Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ${{ matrix.os }}
    
    strategy:
      fail-fast: false
      matrix:
        os: [ubuntu-latest, windows-latest, macos-latest]
        python-version: ['3.9', '3.10', '3.11']
        include:
          - os: ubuntu-latest
            path: ~/.cache/pip
          - os: windows-latest
            path: ~\AppData\Local\pip\Cache
          - os: macos-latest
            path: ~/Library/Caches/pip
        exclude:
          - os: macos-latest
            python-version: '3.9'
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Setup Python ${{ matrix.python-version }}
        uses: actions/setup-python@v5
        with:
          python-version: ${{ matrix.python-version }}
      
      - name: Cache dependencies
        uses: actions/cache@v4
        with:
          path: ${{ matrix.path }}
          key: ${{ matrix.os }}-pip-${{ hashFiles('requirements.txt') }}
      
      - name: Install dependencies
        run: |
          python -m pip install --upgrade pip
          pip install pytest
          pip install -r requirements.txt
      
      - name: Run tests
        run: pytest
      
      - name: Report
        run: |
          echo "✅ Tests completados en ${{ matrix.os }} con Python ${{ matrix.python-version }}"
```

---

## 🐛 Debugging y Troubleshooting

### Habilitar Debug Logs

**Opción 1: En el repositorio**
Settings → Secrets → New secret
- Nombre: `ACTIONS_STEP_DEBUG`
- Valor: `true`

**Opción 2: En el workflow**
```yaml
- name: Debug
  run: echo "::debug::Mensaje de debug"
```

### Logs de Comandos

```yaml
- name: Debug con set -x
  run: |
    set -x  # Imprime cada comando antes de ejecutarlo
    cd src/
    ls -la
    pwd
```

### Imprimir Variables

```yaml
- name: Debug variables
  run: |
    echo "Event: ${{ github.event_name }}"
    echo "Ref: ${{ github.ref }}"
    echo "SHA: ${{ github.sha }}"
    echo "Actor: ${{ github.actor }}"
    echo "Runner OS: ${{ runner.os }}"
```

### Imprimir Todo el Contexto

```yaml
- name: Dump GitHub context
  env:
    GITHUB_CONTEXT: ${{ toJson(github) }}
  run: echo "$GITHUB_CONTEXT"

- name: Dump job context
  env:
    JOB_CONTEXT: ${{ toJson(job) }}
  run: echo "$JOB_CONTEXT"

- name: Dump steps context
  env:
    STEPS_CONTEXT: ${{ toJson(steps) }}
  run: echo "$STEPS_CONTEXT"
```

### Continue on Error

```yaml
- name: Paso que puede fallar
  continue-on-error: true
  run: |
    # Este paso no detendrá el workflow si falla
    comando_que_puede_fallar
```

### Timeout

```yaml
- name: Paso con timeout
  timeout-minutes: 10
  run: |
    # Se cancela después de 10 minutos
    comando_largo
```

### Reintentos

```yaml
- name: Paso con reintentos
  uses: nick-invision/retry@v2
  with:
    timeout_minutes: 10
    max_attempts: 3
    command: npm test
```

### Verificar Archivos

```yaml
- name: Debug filesystem
  run: |
    echo "📁 Directorio actual:"
    pwd
    
    echo -e "\n📂 Contenido:"
    ls -lah
    
    echo -e "\n🌳 Árbol de directorios:"
    tree -L 3 || find . -maxdepth 3
    
    echo -e "\n💾 Espacio en disco:"
    df -h
```

### Verificar Variables de Entorno

```yaml
- name: Debug environment
  run: |
    echo "🔧 Variables de entorno:"
    env | sort
    
    echo -e "\n🐍 Python:"
    which python
    python --version
    
    echo -e "\n📦 Pip:"
    which pip
    pip --version
    pip list
```

---

## 📚 Recursos y Mejores Prácticas

### Mejores Prácticas

1. **Nombres Descriptivos**
   ```yaml
   # ❌ Mal
   - name: Step 1
     run: pytest
   
   # ✅ Bien
   - name: Ejecutar tests unitarios con pytest
     run: pytest tests/unit/
   ```

2. **Usar Versiones Específicas**
   ```yaml
   # ❌ Mal
   uses: actions/checkout@main
   
   # ✅ Bien
   uses: actions/checkout@v4
   ```

3. **Cachear Dependencias**
   ```yaml
   - name: Cache pip
     uses: actions/cache@v4
     with:
       path: ~/.cache/pip
       key: ${{ runner.os }}-pip-${{ hashFiles('requirements.txt') }}
   ```

4. **Fail Fast Strategy**
   ```yaml
   strategy:
     fail-fast: true  # Detiene otros jobs si uno falla
     matrix:
       python-version: ['3.9', '3.10', '3.11']
   ```

5. **Usar Secretos para Datos Sensibles**
   ```yaml
   # ❌ Mal
   - name: Deploy
     run: deploy.sh --password=mipassword123
   
   # ✅ Bien
   - name: Deploy
     env:
       PASSWORD: ${{ secrets.DEPLOY_PASSWORD }}
     run: deploy.sh --password="$PASSWORD"
   ```

### Límites y Cuotas

**GitHub Free:**
- ⏱️ 2,000 minutos/mes para repos privados
- ✅ Ilimitado para repos públicos
- 💾 500 MB de storage de artifacts
- ⏰ 6 horas por job máximo
- 🔢 20 jobs concurrentes

**GitHub Pro:**
- ⏱️ 3,000 minutos/mes
- 💾 1 GB de storage

### Recursos Útiles

- 📘 [Documentación Oficial](https://docs.github.com/en/actions)
- 🎭 [Marketplace de Actions](https://github.com/marketplace?type=actions)
- 💡 [GitHub Actions Examples](https://github.com/actions/starter-workflows)
- 🐍 [Python en GitHub Actions](https://docs.github.com/en/actions/automating-builds-and-tests/building-and-testing-python)

---

## 🎓 Ejercicios Prácticos

### Ejercicio 1: Hello World
Crea un workflow que imprima "Hola Mundo" cuando hagas push.

<details>
<summary>Ver solución</summary>

```yaml
name: Hello World

on: push

jobs:
  greet:
    runs-on: ubuntu-latest
    steps:
      - name: Saludar
        run: echo "¡Hola Mundo!"
```
</details>

### Ejercicio 2: Python Script
Crea un workflow que ejecute un script Python que calcule el factorial de 5.

<details>
<summary>Ver solución</summary>

```yaml
name: Factorial

on: push

jobs:
  calculate:
    runs-on: ubuntu-latest
    steps:
      - name: Calcular factorial
        shell: python
        run: |
          import math
          resultado = math.factorial(5)
          print(f"5! = {resultado}")
```
</details>

### Ejercicio 3: Tests Programados
Crea un workflow que ejecute tests todos los días a las 9am.

<details>
<summary>Ver solución</summary>

```yaml
name: Daily Tests

on:
  schedule:
    - cron: '0 9 * * *'

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Setup Python
        uses: actions/setup-python@v5
        with:
          python-version: '3.11'
      - name: Run tests
        run: |
          pip install pytest
          pytest
```
</details>

---

## 🎉 Conclusión

¡Felicidades! Ahora entiendes:
- ✅ Qué es GitHub Actions y para qué sirve
- ✅ La estructura de un workflow
- ✅ Cómo usar triggers y eventos
- ✅ Variables, contextos y secretos
- ✅ Cómo trabajar con Python en workflows
- ✅ Acciones predefinidas útiles
- ✅ Debugging y troubleshooting
- ✅ Ejemplos prácticos reales

**Próximos pasos:**
1. 🧪 Experimenta con workflows simples en un repo de prueba
2. 📖 Lee la documentación oficial para casos avanzados
3. 🔍 Explora el Marketplace para encontrar actions útiles
4. 💪 Practica creando workflows para tus proyectos reales

---

*Última actualización: Enero 2026*

