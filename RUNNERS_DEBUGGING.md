# 🖥️ GitHub Actions: Runners y Debugging

## 📚 Índice
1. [GitHub-Hosted Runners](#1-github-hosted-runners)
2. [Self-Hosted Runners](#2-self-hosted-runners)
3. [Runners en Organizaciones: Grupos y Labels](#3-runners-en-organizaciones-grupos-y-labels)
4. [GitHub CLI (gh) en Workflows](#4-github-cli-gh-en-workflows)
5. [Debugging: Encontrar y Resolver Errores](#5-debugging-encontrar-y-resolver-errores)
6. [act: Ejecutar Workflows Localmente](#6-act-ejecutar-workflows-localmente)
7. [Larger Runners y GPU Runners](#7-larger-runners-y-gpu-runners)
8. [IP Allowlists para Self-Hosted Runners](#8-ip-allowlists-para-self-hosted-runners)
9. [JIT Runners (Just-in-Time)](#9-jit-runners-just-in-time)
10. [Gestión de Ejecuciones de Workflows](#10-gestión-de-ejecuciones-de-workflows)
11. [Preguntas de Examen](#11-preguntas-de-examen)

---

## 1. GitHub-Hosted Runners

Un **runner** es la máquina (física o virtual) donde se ejecutan los jobs de un workflow. Cuando un job empieza, GitHub crea una máquina virtual nueva, descarga el código del repositorio (si hay `actions/checkout`), ejecuta todos los steps, y destruye la máquina al finalizar. Cada job tiene su propia máquina limpia — no comparte estado con otros jobs.

**GitHub-hosted runners** son máquinas gestionadas completamente por GitHub: GitHub las crea, las mantiene actualizadas con herramientas preinstaladas, y las destruye tras cada uso. No requieren ninguna configuración por parte del usuario. Se seleccionan con `runs-on:` usando un label predefinido.

### Tipos de runners disponibles

| Label | Sistema Operativo | CPU | RAM | Disco |
|---|---|---|---|---|
| `ubuntu-latest` | Ubuntu 24.04 | 4 | 16 GB | 14 GB |
| `ubuntu-24.04` | Ubuntu 24.04 | 4 | 16 GB | 14 GB |
| `ubuntu-22.04` | Ubuntu 22.04 | 4 | 16 GB | 14 GB |
| `ubuntu-20.04` | Ubuntu 20.04 | 4 | 16 GB | 14 GB |
| `windows-latest` | Windows Server 2022 | 4 | 16 GB | 14 GB |
| `windows-2022` | Windows Server 2022 | 4 | 16 GB | 14 GB |
| `windows-2019` | Windows Server 2019 | 4 | 16 GB | 14 GB |
| `macos-latest` | macOS 14 | 3 | 14 GB | 14 GB |
| `macos-14` | macOS 14 (M1) | 3 | 7 GB | 14 GB |
| `macos-13` | macOS 13 | 4 | 14 GB | 14 GB |

> ⚠️ **`ubuntu-latest`, `windows-latest`, `macos-latest`** apuntan a versiones específicas que pueden cambiar con el tiempo. Para reproducibilidad, usa la versión explícita (`ubuntu-22.04`).

### Herramientas preinstaladas

Los runners de GitHub tienen muchas herramientas preinstaladas:

```
Ubuntu: git, docker, node, npm, python, pip, java, mvn, gradle, go, 
        ruby, php, dotnet, aws-cli, az-cli, gcloud, kubectl, helm...

Windows: git, node, npm, python, java, go, ruby, nuget, choco...

macOS: git, docker (sin daemon), node, npm, python, pip, java, 
       brew, xcode, swift...
```

Ver lista completa: https://github.com/actions/runner-images

### Multiplicadores de tiempo de cómputo (repos privados)

| OS | Multiplicador |
|---|---|
| Linux | 1x |
| Windows | 2x |
| macOS | 10x |

```
Ejemplo: 10 minutos de macOS = 100 minutos consumidos del plan
```

### Runners más grandes (Large Runners)

Para repos de organizaciones (planes Team/Enterprise):

```yaml
runs-on: ubuntu-latest-4-cores    # 4 CPU, 16 GB RAM
runs-on: ubuntu-latest-8-cores    # 8 CPU, 32 GB RAM
runs-on: ubuntu-latest-16-cores   # 16 CPU, 64 GB RAM
```

---

## 2. Self-Hosted Runners

Los **self-hosted runners** son máquinas que tú controlas y que ejecutan los jobs en lugar de (o además de) los runners de GitHub.

### ¿Cuándo usar self-hosted?

```
✅ Necesitas hardware específico (GPU, mucha RAM, disco rápido)
✅ Necesitas acceso a recursos internos (base de datos privada, VPN)
✅ Tienes muchos minutos de ejecución y quieres reducir costos
✅ Necesitas persistencia entre runs (caché en disco local)
✅ Cumplimiento normativo (los datos no pueden salir de tu infraestructura)

❌ No uses para repos públicos (código de extraños ejecutará en tu máquina)
```

### Instalación de un self-hosted runner

1. **En GitHub**: Settings → Actions → Runners → New self-hosted runner
2. **Seleccionar OS** y ejecutar los comandos que da GitHub

```bash
# GitHub te da comandos como estos:
mkdir actions-runner && cd actions-runner

# Descargar
curl -o actions-runner-linux-x64-2.X.X.tar.gz -L \
  https://github.com/actions/runner/releases/download/v2.X.X/actions-runner-linux-x64-2.X.X.tar.gz

tar xzf ./actions-runner-linux-x64-2.X.X.tar.gz

# Configurar (GitHub te da el token)
./config.sh --url https://github.com/mi-org/mi-repo \
            --token AAAAAXXXXXXXXXX \
            --name "mi-runner" \
            --labels "self-hosted,Linux,mi-runner"

# Ejecutar
./run.sh

# O instalar como servicio
sudo ./svc.sh install
sudo ./svc.sh start
```

### Usar self-hosted runner en workflow

```yaml
jobs:
  build-with-gpu:
    runs-on: self-hosted                    # ← Cualquier self-hosted
    # O con labels específicos:
    runs-on: [self-hosted, Linux, gpu]      # ← Self-hosted con GPU en Linux
    steps:
      - uses: actions/checkout@v4
      - run: nvidia-smi                     # Herramienta disponible en tu máquina
```

### Seguridad con self-hosted runners

⚠️ **NUNCA usar self-hosted runners en repos públicos**:

```yaml
jobs:
  safe-job:
    # Restricción de seguridad: solo ejecutar en tu repo (no en forks)
    if: github.repository == 'mi-org/mi-repo'
    runs-on: [self-hosted, Linux]
    steps:
      - uses: actions/checkout@v4
      - run: ./build.sh
```

Código malicioso de un fork podría ejecutarse en tu máquina.

---

## 3. Runners en Organizaciones: Grupos y Labels

### Runner Groups

Los grupos permiten controlar qué repos pueden usar qué runners:

```
Organización
├── Runner Group: "Production" 
│   ├── Runner: prod-server-1
│   ├── Runner: prod-server-2
│   └── Acceso: solo repo "mi-app-prod"
│
└── Runner Group: "Development"
    ├── Runner: dev-server-1
    └── Acceso: todos los repos de la org
```

**Configuración:** Organization Settings → Actions → Runner Groups

### Seleccionar runner por grupo y labels

```yaml
jobs:
  deploy-prod:
    runs-on:
      group: production                  # ← Grupo específico
      labels: [linux, high-memory]       # ← Labels adicionales dentro del grupo
    steps:
      - run: ./deploy.sh

  # O simplemente con labels
  test-with-gpu:
    runs-on: [self-hosted, gpu, linux]   # ← Array de labels
    steps:
      - run: python train.py
```

---

## 4. GitHub CLI (gh) en Workflows

El CLI `gh` está preinstalado en todos los runners de GitHub y es muy útil para operaciones con la API de GitHub.

### Autenticación automática

```yaml
steps:
  - name: Usar gh CLI
    env:
      GH_TOKEN: ${{ secrets.GITHUB_TOKEN }}   # ← Necesario
    run: |
      gh pr list
      gh issue list
      gh release list
```

### Operaciones comunes con gh

```yaml
env:
  GH_TOKEN: ${{ secrets.GITHUB_TOKEN }}

# Comentar en un PR
- run: |
    gh pr comment ${{ github.event.pull_request.number }} \
      --body "✅ Todos los tests pasaron"

# Crear un issue
- run: |
    gh issue create \
      --title "Bug detectado en build" \
      --body "El build falló: ver ${{ github.server_url }}/${{ github.repository }}/actions/runs/${{ github.run_id }}" \
      --label "bug"

# Crear una release
- run: |
    gh release create v1.0.0 \
      --title "Release v1.0.0" \
      --notes "Primera release" \
      dist/app.zip

# Aprobar un PR
- run: gh pr merge ${{ github.event.pull_request.number }} --auto --squash

# Obtener información de la API
- run: |
    OPEN_PRS=$(gh pr list --state open --json number | jq length)
    echo "PRs abiertos: $OPEN_PRS"

# Añadir label a un PR
- run: |
    gh pr edit ${{ github.event.pull_request.number }} --add-label "reviewed"
```

### gh con token personalizado (más permisos)

```yaml
env:
  GH_TOKEN: ${{ secrets.MY_PAT }}   # Personal Access Token con más permisos
run: |
  # GITHUB_TOKEN solo puede operar en el repo actual
  # Un PAT puede operar en otros repos de la org
  gh issue create --repo mi-org/otro-repo --title "Notificación"
```

---

## 5. Debugging: Encontrar y Resolver Errores

Cuando un workflow falla, los logs se consultan en la pestaña **Actions** del repositorio → seleccionar la ejecución → expandir el job y el step que falló. Los logs tienen dos niveles de detalle:

- **Logs normales**: lo que imprimen los steps (`run:`) y los mensajes de GitHub Actions. Suficiente para la mayoría de errores.
- **Debug logging**: logs internos del runner y de cada action (normalmente ocultos). Se activa cuando el error no es evidente en los logs normales — por ejemplo, cuando una action falla sin mensaje claro o cuando hay problemas de conectividad o permisos en el runner.

Los tipos de error más frecuentes son:
- **Error en el comando** (`exit code != 0`): ver el output del step fallido.
- **Expresión evaluada incorrectamente**: activar debug y volcar contextos con `toJSON(github)`.
- **Permiso denegado** (API, secrets): revisar `permissions:` del job y el scope del `GITHUB_TOKEN`.
- **Herramienta no encontrada**: la herramienta no está preinstalada en el runner; hay que instalarla antes.

### Habilitar debug logging

**Opción 1: Secrets del repositorio**

Añadir secrets:
- `ACTIONS_RUNNER_DEBUG` = `true` → Logs del runner
- `ACTIONS_STEP_DEBUG` = `true` → Logs de steps (más detallado)

**Opción 2: Al re-ejecutar manualmente**

En la UI de GitHub, al hacer "Re-run jobs" → "Enable debug logging".

```yaml
# Verificar en el workflow si está en modo debug
- name: Debug info
  if: runner.debug == '1'     # ← Solo en modo debug
  run: |
    echo "Estamos en modo debug"
    env | sort                # Imprimir todas las variables de entorno
```

### Imprimir contextos completos

```yaml
- name: Dump contexts
  run: |
    echo "=== github context ==="
    cat << 'EOF'
    ${{ toJSON(github) }}
    EOF
    
    echo "=== job context ==="
    cat << 'EOF'
    ${{ toJSON(job) }}
    EOF
    
    echo "=== steps context ==="
    cat << 'EOF'
    ${{ toJSON(steps) }}
    EOF
    
    echo "=== runner context ==="
    cat << 'EOF'
    ${{ toJSON(runner) }}
    EOF

# Versión corta con env:
- name: Dump github context
  env:
    GITHUB_CONTEXT: ${{ toJSON(github) }}
  run: echo "$GITHUB_CONTEXT" | jq '.'
```

### Debugging con tmate (sesión interactiva SSH)

`tmate` permite abrir una sesión SSH interactiva en el runner para inspeccionar el estado:

```yaml
- name: Setup tmate session
  uses: mxschmitt/action-tmate@v3
  if: failure()              # ← Solo si algo falló
  timeout-minutes: 30        # ← Máximo 30 min de sesión
```

Cuando el step se ejecuta, imprime en los logs un comando SSH para conectarte:
```
ssh xxxxxxxxxxxx@nyc1.tmate.io
```

> ⚠️ Para repos públicos, cualquiera puede ver el link en los logs. Usar `limit-access-to-actor: true` para restringir al actor del workflow.

```yaml
- uses: mxschmitt/action-tmate@v3
  if: failure()
  with:
    limit-access-to-actor: true   # ← Solo el usuario que disparó el workflow
```

### Errores comunes y soluciones

**Error: "Context access might be invalid"**
```yaml
# ❌ Causa: el campo puede no existir para todos los eventos
run: echo "${{ github.event.pull_request.title }}"

# ✅ Solución: usar || para default
run: echo "${{ github.event.pull_request.title || 'N/A' }}"
```

**Error: "fatal: ambiguous argument ''"**
```bash
# Causa: variable de bash vacía usada como argumento git
git log $BEFORE..$AFTER  # Si BEFORE o AFTER están vacíos → error

# ✅ Solución: verificar antes de usar
if [ -n "$BEFORE" ] && [ -n "$AFTER" ]; then
  git log $BEFORE..$AFTER
fi
```

**Error: "Resource not accessible by integration"**
```yaml
# Causa: GITHUB_TOKEN no tiene los permisos necesarios
# ✅ Solución: añadir permissions explícitos
permissions:
  pull-requests: write
  issues: write
```

**Error: "Process completed with exit code 1" en multiline**
```yaml
# ❌ Causa: set -e hace que bash falle al primer error
run: |
  comando-que-puede-fallar
  echo "sigo aquí"

# ✅ Solución: usar || true o if
run: |
  comando-que-puede-fallar || true
  echo "sigo aquí"
```

**Error: Las comillas en YAML**
```yaml
# ⚠️ Cuidado con las comillas en if:
if: "!cancelled()"          # ✅ Necesita comillas si empieza con !
if: runner.os == 'Linux'    # ✅ Comillas simples dentro del valor YAML
if: github.ref == "refs/heads/main"  # ✅ También válido
```

### Workflow de diagnóstico

```yaml
name: Diagnose

on:
  workflow_dispatch:

jobs:
  diagnose:
    runs-on: ubuntu-latest
    steps:
      - name: Sistema operativo
        run: |
          uname -a
          cat /etc/os-release
      
      - name: Herramientas disponibles
        run: |
          echo "Git: $(git --version)"
          echo "Node: $(node --version)"
          echo "npm: $(npm --version)"
          echo "Python: $(python3 --version)"
          echo "Docker: $(docker --version)"
          echo "gh: $(gh --version)"
      
      - name: Variables de entorno GitHub
        run: env | grep GITHUB | sort
      
      - name: Espacio en disco
        run: df -h
      
      - name: Memoria
        run: free -h
      
      - name: CPU
        run: nproc
```

---

## 6. act: Ejecutar Workflows Localmente

`act` permite ejecutar workflows de GitHub Actions en tu máquina local usando Docker.

### Instalación

```bash
# macOS
brew install act

# Linux
curl https://raw.githubusercontent.com/nektos/act/master/install.sh | sudo bash

# Windows (con Chocolatey)
choco install act-cli
```

### Uso básico

```bash
# Simular un push
act push

# Simular un pull_request
act pull_request

# Ejecutar workflow específico
act -W .github/workflows/ci.yml

# Ejecutar job específico
act -j build

# Listar workflows disponibles
act -l

# Ver qué haría sin ejecutar
act -n   # Dry run

# Especificar plataforma
act --platform ubuntu-latest=catthehacker/ubuntu:act-latest
```

### Archivo de configuración .actrc

```bash
# .actrc en la raíz del proyecto
-P ubuntu-latest=catthehacker/ubuntu:act-latest
-P ubuntu-22.04=catthehacker/ubuntu:act-22.04
--secret-file .env.secrets
```

### Variables y secrets para act

```bash
# .env.secrets (no commitear)
MY_API_KEY=test_value
GITHUB_TOKEN=ghp_xxxxx
```

### Limitaciones de act

- No soporta todos los runners (macOS, Windows)
- Algunas actions de GitHub no funcionan localmente
- No tiene acceso al contexto real de GitHub (no hay PR real, etc.)
- Es útil para testing básico, no para reproducir exactamente el entorno de CI

---

## 7. Runners Efímeros (Ephemeral / Just-in-Time)

### ¿Qué son?

Un runner efímero se registra, ejecuta **un único job** y se da de baja automáticamente. Es el máximo nivel de aislamiento y seguridad en self-hosted runners.

```bash
# Registrar un runner en modo efímero
./config.sh --url https://github.com/mi-org/mi-repo \
            --token AAAA... \
            --ephemeral        # ← Se da de baja tras el primer job
```

```
Runner normal (persistente):
  Job 1 → ejecuta → vuelve a "idle" → Job 2 → ejecuta → ...
  ⚠️ El estado del sistema persiste entre jobs (archivos, procesos, credenciales residuales)

Runner efímero:
  Job 1 → ejecuta → se da de baja → se crea un runner nuevo → Job 2 → ...
  ✅ Estado limpio garantizado en cada job, igual que GitHub-hosted
```

### Just-in-Time (JIT) runners

Los JIT runners son efímeros registrados programáticamente vía API (sin pasar el token de registro al sistema):

```bash
# GitHub API: crear JIT runner token
curl -X POST \
  -H "Authorization: Bearer $GITHUB_TOKEN" \
  https://api.github.com/repos/OWNER/REPO/actions/runners/generate-jit-config \
  -d '{"name":"jit-runner","runner_group_id":1,"labels":["self-hosted","linux"],"work_folder":"_work"}'
```

El token devuelto se pasa al runner para que se registre y ejecute un único job sin ninguna configuración persistente.

### Actions Runner Controller (ARC)

ARC escala runners self-hosted automáticamente en **Kubernetes**:

```
Sin ARC:                         Con ARC:
Tienes N runners fijos           Tienes 0 runners en reposo
Si hay más jobs que runners      Cuando llega un job, ARC
→ se forman colas                crea un pod nuevo automáticamente
                                 Al terminar, el pod se destruye
```

```yaml
# En Kubernetes (HelmChart simplificado)
# arc-systems namespace: el controlador ARC
# arc-runners namespace: los pods runner
# 
# Se configura el escalado:
minRunners: 0      # Sin jobs → 0 pods
maxRunners: 10     # Máximo 10 runners simultáneos
```

---

## 8. Facturación y Límites de Uso

### Minutos incluidos por plan (repos privados)

| Plan | Minutos/mes | Storage |
|------|------------|---------|
| Free | 2.000 | 500 MB |
| Pro | 3.000 | 1 GB |
| Team | 3.000 | 2 GB |
| Enterprise | 50.000 | 50 GB |

> Repos **públicos**: siempre gratis e ilimitados.

### Multiplicadores de OS

| OS | Multiplicador | Ejemplo: 10 min de job |
|---|---|---|
| Linux | 1x | = 10 min consumidos |
| Windows | 2x | = 20 min consumidos |
| macOS | 10x | = 100 min consumidos |

### Dónde ver el consumo

`Settings → Billing and plans → Usage this month → Actions`

Muestra minutos consumidos por OS, storage usado por artifacts y packages.

### Límites técnicos

| Recurso | Límite |
|---------|--------|
| Duración máxima de un job | 6 horas |
| Duración máxima de un workflow | 35 días |
| Jobs concurrentes (Free) | 20 |
| Jobs concurrentes (Pro/Team) | 40 |
| Jobs concurrentes (Enterprise) | 500 |
| Jobs en cola por repo | 500 |
| Tamaño máximo de un artifact | 10 GB |
| Cache total por repo | 10 GB |
| Retención logs/artifacts (default) | 90 días |
| Retención logs/artifacts (mínimo) | 1 día |
| Retención logs/artifacts (máximo) | 400 días |
| Secrets por repo | 100 |
| Secrets por entorno (environment) | 100 |
| Secrets por organización | 300 |
| Tamaño máximo de un secret | 48 KB |
| Variables (vars) por repo | 500 |
| Outputs por step | 1 MB |
| Variables de entorno (env) totales | 256 KB |

### Retención de logs y artifacts: configuración administrativa

Se puede cambiar la retención por defecto a nivel de repositorio u organización:

```
Repo: Settings → Actions → General → Artifact and log retention
Org:  Organization Settings → Actions → General → Artifact and log retention
Rango: 1 – 400 días
```

En el workflow se puede sobreescribir por artifact:
```yaml
- uses: actions/upload-artifact@v4
  with:
    name: mi-artifact
    path: dist/
    retention-days: 7   # Sobreescribe el default del repo/org
```

---

## 7. Larger Runners y GPU Runners

Los **larger runners** son máquinas con más recursos que los runners estándar (2 CPU / 7 GB RAM). Están disponibles en planes Team y Enterprise.

### Especificaciones de larger runners (Linux/Windows)

| Label | CPU | RAM | Disco | Notas |
|---|---|---|---|---|
| `ubuntu-latest` / `ubuntu-22.04` | 2 | 7 GB | 14 GB | Estándar (incluido en todos los planes) |
| `ubuntu-latest-4-cores` | 4 | 16 GB | 14 GB | Requiere plan Team/Enterprise |
| `ubuntu-latest-8-cores` | 8 | 32 GB | 300 GB | Requiere plan Team/Enterprise |
| `ubuntu-latest-16-cores` | 16 | 64 GB | 600 GB | Requiere plan Team/Enterprise |
| `ubuntu-latest-32-cores` | 32 | 128 GB | 1200 GB | Requiere plan Team/Enterprise |
| `ubuntu-latest-64-cores` | 64 | 256 GB | 2040 GB | Requiere plan Team/Enterprise |
| `windows-latest-8-cores` | 8 | 32 GB | 300 GB | Requiere plan Team/Enterprise |

### GPU Runners

Disponibles solo en **GitHub Enterprise Cloud**:

| Label | GPU | CPU | RAM |
|---|---|---|---|
| `gpu-nvidia-t4-linux-medium` | 1x NVIDIA T4 (16 GB VRAM) | 4 | 28 GB |

**Casos de uso:** inferencia de modelos ML, procesamiento de imágenes/vídeo, compilación de shaders.

### Cómo usar larger runners

```yaml
jobs:
  build-heavy:
    runs-on: ubuntu-latest-16-cores    # Larger runner
    steps:
      - uses: actions/checkout@v4
      - run: make -j16                  # Compilación paralela con 16 cores

  ml-inference:
    runs-on: gpu-nvidia-t4-linux-medium  # GPU runner
    steps:
      - uses: actions/checkout@v4
      - run: python inference.py
```

### Facturación de larger runners

Los larger runners tienen un coste por minuto proporcional al número de cores (Linux):

| Runner | Multiplicador de coste |
|---|---|
| 2-core (estándar) | 1x |
| 4-core | 2x |
| 8-core | 4x |
| 16-core | 8x |
| 32-core | 16x |
| 64-core | 32x |

> Los repos **públicos** usan runners estándar de forma gratuita. Los larger runners en repos públicos requieren configuración especial y plan Enterprise.

---

## 8. IP Allowlists para Self-Hosted Runners

Las **IP allowlists** a nivel de organización o empresa permiten restringir qué IPs pueden comunicarse con la API de GitHub para registrar runners o recibir jobs.

### ¿Para qué sirve?

```
SIN allowlist:
  Cualquier IP puede registrar un runner y recibir jobs de la org

CON allowlist:
  Solo las IPs/rangos definidos pueden comunicarse con GitHub Actions
  → Self-hosted runners en datacenter propio: añadir IPs del datacenter
  → Previene que runners no autorizados reciban jobs sensibles
```

### Configuración

```
Organization Settings → Security → IP allow list
→ Add IP address or range
   - Formato: IP única (192.168.1.1) o CIDR (10.0.0.0/8)
   - Descripción: nombre descriptivo

Enterprise Settings → Security → IP allow list
→ Misma interfaz, aplica a toda la enterprise
```

### Comportamiento con GitHub-hosted runners

Si activas IP allowlist en una org con repos que usan **GitHub-hosted runners**, debes añadir los rangos de IPs de GitHub:

```bash
# Obtener los rangos actuales de GitHub Actions:
curl https://api.github.com/meta | jq '.actions'
# Retorna rangos CIDR que cambian periódicamente
```

> ⚠️ Los rangos de IPs de GitHub-hosted runners **cambian periódicamente**. Si activas IP allowlist, deberías automatizar la actualización de los rangos con un workflow que llame a la API de GitHub meta, o usar self-hosted runners exclusivamente.

### Casos de uso habituales

1. **Entornos regulados (finanzas, salud):** solo los runners en la red corporativa pueden ejecutar jobs
2. **Compliance:** auditoría requiere que el código solo se procese en infraestructura controlada
3. **Seguridad perimetral:** combinado con VPN para que los runners accedan a recursos internos

---

## 9. JIT Runners (Just-in-Time)

### ¿Qué son los JIT runners?

Los **JIT runners** (Just-in-Time) son runners que se registran usando un token de uso único generado por API, en lugar del flujo tradicional de `config.sh + run.sh`. Esto los hace más seguros porque el token nunca se almacena en el sistema ni se reutiliza.

```
Flujo tradicional:
  1. Generar token de registro (válido 1 hora)
  2. ./config.sh --token AAA...  → guarda configuración en disco
  3. ./run.sh                    → runner registrado, escucha jobs
  Token queda en archivos de configuración locales

Flujo JIT:
  1. API genera token efímero (corta duración, un solo uso)
  2. Token se pasa directamente al binario del runner
  3. Runner se registra, ejecuta un job y se da de baja
  Token nunca se persiste en disco
```

### Diferencia entre `--ephemeral` y JIT

Aunque ambos ejecutan un único job y luego se eliminan, hay una diferencia importante en cómo se registran:

| Característica | `--ephemeral` | JIT |
|---|---|---|
| Registro | `config.sh` con token estándar (1h de validez) | API genera token efímero de corta duración |
| Token en disco | Sí (archivos de configuración) | No |
| Caso de uso | Runners que se registran con antelación | Autoscaling: el orchestrator genera el token justo antes de provisionar la VM |
| Seguridad del token | Token estándar puede ser copiado | Token solo válido para un único registro |

### API para generar un JIT runner token

```bash
# Endpoint: POST /repos/{owner}/{repo}/actions/runners/generate-jit-config
curl -X POST \
  -H "Authorization: Bearer $GITHUB_TOKEN" \
  -H "Accept: application/vnd.github+json" \
  https://api.github.com/repos/OWNER/REPO/actions/runners/generate-jit-config \
  -d '{
    "name": "jit-runner-001",
    "runner_group_id": 1,
    "labels": ["self-hosted", "linux", "jit"],
    "work_folder": "_work"
  }'
```

**Respuesta de la API:**

```json
{
  "runner": {
    "id": 42,
    "name": "jit-runner-001",
    "os": "linux",
    "status": "offline"
  },
  "encoded_jit_config": "eyJhbGciOi..."   // ← Runner config encoded en base64
}
```

El campo `encoded_jit_config` se pasa directamente al runner:

```bash
# El orchestrator extrae el token y provisiona la VM
JIT_CONFIG=$(echo $API_RESPONSE | jq -r '.encoded_jit_config')

# En la VM nueva, iniciar el runner con el token JIT
./run.sh --jitconfig "$JIT_CONFIG"
# El runner se registra, ejecuta el job, y se elimina solo
```

### Ciclo de vida JIT en autoscaling

```
Orchestrator (Kubernetes, Lambda, etc.)
    │
    ├─ Detecta job en cola
    │
    ├─ Llama a: POST /repos/.../runners/generate-jit-config
    │   └─ Obtiene encoded_jit_config (token de corta duración)
    │
    ├─ Provisiona VM/pod nuevo con el token
    │
    └─ VM: ./run.sh --jitconfig "TOKEN"
          ├─ Se registra en GitHub (token se consume, ya no válido)
          ├─ Ejecuta el job
          └─ Se destruye
```

### Ventajas de seguridad de JIT

- **Token de un solo uso:** aunque el token sea interceptado antes de usarse, expira rápidamente y solo puede usarse para registrar un runner (no para hacer operaciones arbitrarias)
- **Sin estado persistente:** el runner no guarda credenciales en disco
- **Ideal para entornos de alto riesgo:** el orchestrator tiene el control total del ciclo de vida
- **Trazabilidad:** cada runner JIT tiene un nombre y ID únicos que permiten auditar qué jobs ejecutó

### Comparativa: persistente vs efímero vs JIT

| | Persistente | Efímero (`--ephemeral`) | JIT |
|---|---|---|---|
| Duración | Indefinida (hasta baja manual) | Un job, luego se da de baja | Un job, token de un solo uso |
| Registro | `config.sh` + `run.sh` | `config.sh --ephemeral` + `run.sh` | API + `run.sh --jitconfig` |
| Token en disco | Sí | Sí (hasta que termina) | No |
| Estado entre jobs | Puede acumular estado | Estado limpio | Estado limpio |
| Uso recomendado | Dev, CI estable | Runners autogestionados | Autoscaling moderno (ARC, Kubernetes) |
| Nivel de seguridad | Bajo | Medio | Alto |

---

## 10. Gestión de Ejecuciones de Workflows

### Re-ejecutar workflows

Cuando un job falla (o simplemente quieres volver a ejecutarlo), GitHub permite re-ejecutar sin hacer un nuevo push. Los re-runs **siempre usan el mismo SHA y ref** que el run original — no ejecutan código más reciente.

> Los re-runs están disponibles hasta **30 días** después del run inicial.

**Desde la UI:**

```
Actions → [workflow run] → "Re-run all jobs"       ← Todos los jobs
                         → "Re-run failed jobs"    ← Solo los fallidos
```

**Con GitHub CLI:**

```bash
# Re-run todos los jobs de un run
gh run rerun RUN_ID

# Re-run solo los jobs que fallaron
gh run rerun RUN_ID --failed

# Re-run un job específico (obtener JOB_ID con gh run view RUN_ID)
gh run rerun --job JOB_ID

# Re-run con debug logging activado
gh run rerun RUN_ID --debug
```

> En la UI, al hacer "Re-run jobs" hay un checkbox "Enable debug logging" — equivalente al flag `--debug` del CLI.

### Cancelar workflows

```bash
# Desde la UI: botón "Cancel workflow" en la ejecución en curso

# Con CLI:
gh run cancel RUN_ID

# Ver runs en curso para obtener el RUN_ID:
gh run list --status in_progress
```

### Deshabilitar y habilitar workflows

Deshabilitar un workflow impide que se dispare por eventos (push, PR, schedule, etc.), pero el archivo `.yml` permanece en el repositorio. El workflow queda en estado `disabled_manually`.

**Desde la UI:**

```
Actions → [seleccionar workflow en la barra lateral] → "..." (menú) → "Disable workflow"
Actions → [seleccionar workflow] → "..." → "Enable workflow"
```

**Con CLI:**

```bash
# Deshabilitar (se puede usar el nombre del archivo o el nombre del workflow)
gh workflow disable ci.yml
gh workflow disable "CI Pipeline"

# Habilitar
gh workflow enable ci.yml
gh workflow enable "CI Pipeline"

# Ver estado de todos los workflows
gh workflow list
```

**Auto-deshabilitación por inactividad:**

Los workflows con trigger `schedule` se deshabilitan automáticamente si no hay actividad (commits, PRs) en el repositorio durante **60 días**. GitHub envía un email de aviso antes. Para reactivarlos, basta con hacer un commit o habilitarlos manualmente.

### Ver y descargar logs

```bash
# Ver logs de un run completo en la terminal
gh run view RUN_ID --log

# Ver solo los logs de los jobs/steps fallidos
gh run view RUN_ID --log-failed

# Ver información del run (estado, duración, jobs)
gh run view RUN_ID

# Listar runs recientes
gh run list
gh run list --workflow ci.yml
gh run list --branch main --status failure
```

**Desde la UI:** dentro del log de cualquier job hay un botón de descarga (icono de engranaje o rueda) para descargar los logs completos del job como archivo `.zip`.

### Status badges

Los badges de estado muestran el resultado del último run de un workflow en el README u otras páginas.

**URL del badge:**

```
https://github.com/{owner}/{repo}/actions/workflows/{workflow-file}.yml/badge.svg
```

**Parámetro `?branch=` para rama específica:**

```
https://github.com/mi-org/mi-repo/actions/workflows/ci.yml/badge.svg?branch=main
```

**Markdown para el README:**

```markdown
![CI](https://github.com/mi-org/mi-repo/actions/workflows/ci.yml/badge.svg)

![CI en main](https://github.com/mi-org/mi-repo/actions/workflows/ci.yml/badge.svg?branch=main)
```

**Desde la UI (generador automático):**

```
Actions → [seleccionar workflow] → "..." → "Create status badge"
```

GitHub genera el snippet de Markdown listo para copiar.

> Los badges muestran el estado del workflow completo (no de un job individual). Si hay varios workflows en el repo, cada uno tiene su propio badge.

---

## 11. Preguntas de Examen

**P: ¿Cuánto consume en minutos un job de 10 minutos en macOS (repo privado)?**
→ 100 minutos (multiplicador x10).

**P: ¿Por qué no se deben usar self-hosted runners en repos públicos?**
→ Código malicioso de PRs de extraños podría ejecutarse en tu máquina con acceso a tu red interna.

**P: ¿Qué variable de entorno necesita `gh` CLI en un workflow?**
→ `GH_TOKEN` (asignarle `${{ secrets.GITHUB_TOKEN }}`).

**P: ¿Cómo habilitar debug logging en una ejecución?**
→ Añadir secrets `ACTIONS_RUNNER_DEBUG=true` y `ACTIONS_STEP_DEBUG=true`, o activar "Enable debug logging" al re-ejecutar manualmente.

**P: ¿Qué hace `mxschmitt/action-tmate`?**
→ Abre una sesión SSH interactiva en el runner para debugging en tiempo real.

**P: ¿Para qué sirve `act`?**
→ Para ejecutar workflows de GitHub Actions localmente en Docker, sin necesidad de hacer push a GitHub.

**P: ¿Qué son los Runner Groups?**
→ Grupos de self-hosted runners en una organización que permiten controlar qué repositorios tienen acceso a qué runners.

**P: ¿Qué diferencia hay entre `ubuntu-latest` y `ubuntu-22.04` en `runs-on`?**
→ `ubuntu-latest` apunta a la versión más reciente de Ubuntu disponible y puede cambiar. `ubuntu-22.04` siempre apunta a Ubuntu 22.04 — más reproducible y predecible.

**P: ¿Qué plan de GitHub se necesita para usar larger runners (`ubuntu-latest-8-cores`)?**
→ Plan Team o Enterprise. Los runners estándar de 2-core están disponibles en todos los planes.

**P: ¿Cuánto cuesta en minutos un job de 10 minutos en un runner de 16 cores Linux (repo privado)?**
→ 80 minutos (multiplicador 8x por los 16 cores).

**P: ¿Cómo se referencia un larger runner en `runs-on`?**
→ Con su label predefinido: `runs-on: ubuntu-latest-8-cores`. Para larger runners self-hosted con labels personalizados: `runs-on: [self-hosted, linux, large-memory]`.

**P: ¿Para qué se usa la IP allowlist en GitHub Actions y qué problema genera con GitHub-hosted runners?**
→ Para restringir qué IPs pueden comunicarse con la org/enterprise en GitHub. El problema con GitHub-hosted runners es que sus IPs cambian periódicamente y hay que mantener la allowlist actualizada (consultando `https://api.github.com/meta`).

**P: ¿En qué plan están disponibles los GPU runners de GitHub?**
→ Solo en GitHub Enterprise Cloud (label: `gpu-nvidia-t4-linux-medium`).

**P: ¿Qué diferencia hay entre un runner con `--ephemeral` y un JIT runner?**
→ Ambos ejecutan un solo job. La diferencia es el registro: `--ephemeral` usa `config.sh` con un token estándar que queda en archivos de configuración en disco. JIT usa una API que genera un token de corta duración que se pasa directamente al runner — el token nunca se persiste en disco y solo es válido para un único registro.

**P: ¿Qué endpoint de la API genera un token JIT?**
→ `POST /repos/{owner}/{repo}/actions/runners/generate-jit-config`. Devuelve un `encoded_jit_config` en base64 que se pasa a `./run.sh --jitconfig`.

**P: ¿Por qué los JIT runners son más seguros que los efímeros?**
→ El token de registro nunca se almacena en disco, es de corta duración y solo válido para un único registro. Aunque fuera interceptado, expira antes de que pueda reutilizarse.

**P: ¿En qué escenario típico se usan los JIT runners?**
→ Autoscaling con Kubernetes u otros orchestrators: el orchestrator detecta un job en cola, llama a la API para obtener el token JIT, provisiona una VM/pod nueva con ese token, el runner ejecuta el job y se destruye.

**P: ¿Los re-runs ejecutan el código más reciente del branch?**
→ No. Los re-runs siempre usan el mismo SHA y ref que el run original, sin importar si hay commits nuevos en el branch.

**P: ¿Hasta cuándo se puede re-ejecutar un workflow run?**
→ Hasta 30 días después del run inicial.

**P: ¿Cómo re-ejecutar solo los jobs fallidos de un run con CLI?**
→ `gh run rerun RUN_ID --failed`.

**P: ¿Qué ocurre con el archivo `.yml` de un workflow cuando se deshabilita?**
→ El archivo permanece en el repositorio. Solo se impide que el workflow se dispare por eventos. El estado queda como `disabled_manually`.

**P: ¿Cuándo se auto-deshabilita un workflow con trigger `schedule`?**
→ Cuando no hay actividad en el repositorio durante 60 días.

**P: ¿Cómo activar debug logging al re-ejecutar un run con CLI?**
→ `gh run rerun RUN_ID --debug`.

**P: ¿Cuál es la URL del status badge de un workflow?**
→ `https://github.com/{owner}/{repo}/actions/workflows/{workflow}.yml/badge.svg`. Para una rama específica: añadir `?branch=main`.

