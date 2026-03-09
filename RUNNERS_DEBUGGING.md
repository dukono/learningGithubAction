# 🖥️ GitHub Actions: Runners y Debugging

## 📚 Índice
1. [GitHub-Hosted Runners](#1-github-hosted-runners)
2. [Self-Hosted Runners](#2-self-hosted-runners)
3. [Runners en Organizaciones: Grupos y Labels](#3-runners-en-organizaciones-grupos-y-labels)
4. [GitHub CLI (gh) en Workflows](#4-github-cli-gh-en-workflows)
5. [Debugging: Encontrar y Resolver Errores](#5-debugging-encontrar-y-resolver-errores)
6. [act: Ejecutar Workflows Localmente](#6-act-ejecutar-workflows-localmente)
7. [Preguntas de Examen](#7-preguntas-de-examen)

---

## 1. GitHub-Hosted Runners

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

## 9. Preguntas de Examen

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

