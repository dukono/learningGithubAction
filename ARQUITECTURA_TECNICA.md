# GITHUB ACTIONS: ARQUITECTURA Y FUNCIONAMIENTO TÉCNICO

## 📚 TABLA DE CONTENIDOS

1. [¿Qué es GitHub Actions? - Fundamentos](#1-qué-es-github-actions---fundamentos)
2. [Arquitectura del Sistema](#2-arquitectura-del-sistema)
3. [Ciclo de Vida Completo](#3-ciclo-de-vida-completo)
4. [Sistema de Eventos](#4-sistema-de-eventos)
5. [Runners: La Infraestructura de Ejecución](#5-runners-la-infraestructura-de-ejecución)
6. [Dónde Continuar →](#6-dónde-continuar)

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

**RUNNER = Entorno de ejecución de un job**
- En **GitHub-hosted**: una VM efímera que se crea y destruye por job
- En **self-hosted**: tu propia máquina con el agente instalado
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
Comprueba si hay un slot de concurrencia libre en tu plan
    ↓
SI NO HAY SLOT DISPONIBLE (límite de concurrencia alcanzado):
  └─ El job queda en cola (queued) hasta que otro job termine y libere su slot

SI HAY SLOT DISPONIBLE:
  ┌─ Crea una VM nueva en Azure (siempre es nueva, nunca reutilizada)
  ┌─ Instala el OS base
  ┌─ Instala todo el software pre-configurado
  └─ Inicia el runner application (agente)
    (Esto toma 10-30 segundos)

⚠️ En GitHub-hosted NO existe el concepto de "runner disponible/en idle":
   cada job SIEMPRE obtiene una VM nueva. Lo que se controla es la concurrencia,
   no la disponibilidad de VMs. (Free: 20 simultáneos, Pro: 40, Enterprise: 180)


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
│ - Esperan hasta que termine un job y  │
│   se libere su slot de concurrencia    │
│ ⚠️ NO es que "no haya VMs": GitHub    │
│   podría crear más, pero tu plan       │
│   limita cuántas pueden correr a la    │
│   vez.                                 │
└────────────────────────────────────────┘
```

**Reutilización de runners: solo aplica a self-hosted**

En **GitHub-hosted** cada job **siempre** obtiene una VM nueva (no hay reutilización).
En **self-hosted** el agente puede reutilizar la misma máquina entre jobs:

```
GitHub-hosted (siempre VM nueva):
  Job 1 → Crea VM 1 → Ejecuta → Destruye VM 1
  Job 2 → Crea VM 2 → Ejecuta → Destruye VM 2
  Job 3 → Crea VM 3 → Ejecuta → Destruye VM 3
  (La cola se controla por slots de concurrencia del plan)

Self-hosted (reutilización del agente en la misma máquina):
  Job 1 → Agente en tu-maquina → Ejecuta → Agente vuelve a "idle"
  Job 2 → Agente en tu-maquina → Ejecuta → Agente vuelve a "idle"
  Job 3 → Agente en tu-maquina → Ejecuta → Agente vuelve a "idle"
  (La cola se controla por cuántos agentes self-hosted tienes registrados)

Nota: En self-hosted debes limpiar el entorno manualmente entre jobs
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

Un **runner** es el **entorno donde se ejecutan los jobs**. Está compuesto por dos partes:

- **Una máquina** (física o virtual): con CPU, RAM, disco y sistema operativo
- **El agente de GitHub Actions**: software que se comunica con GitHub, descarga el job, lo ejecuta y reporta resultados

> ⚠️ **Distinción importante:**
> - En **GitHub-hosted**: la máquina es una VM efímera que se aprovisiona y destruye por cada job. Lo que realmente está limitado son las **ejecuciones simultáneas** (concurrencia), no las VMs en sí. GitHub crea una VM nueva por cada slot disponible en tu plan.
> - En **self-hosted**: la máquina es tuya y persiste. Solo el **agente** vuelve al estado "idle" tras cada job.

```
RUNNER (GitHub-hosted) = VM efímera (creada por job) + Agente (destruido con la VM)
RUNNER (self-hosted)   = Tu máquina (persistente) + Agente (vuelve a "idle")

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
11. En GitHub-hosted: La VM se destruye completamente (no hay "idle")
    En self-hosted: La máquina persiste y el agente vuelve al estado "idle" (esperando nuevo job)

⚠️ NOTA: En GitHub-hosted NO hay reutilización del runner entre jobs.
   Lo que controla la cola de espera es el límite de concurrencia de tu plan
   (Free: 20 jobs simultáneos, Pro: 40, Enterprise: 180).
   Cuando un job termina, su VM se destruye y se libera un slot de concurrencia,
   permitiendo que el siguiente job en cola aprovisione una VM nueva.
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

## 6. DÓNDE CONTINUAR

Este documento cubre **la arquitectura y el motor de GitHub Actions**: qué ocurre internamente desde que haces un push hasta que el runner ejecuta tu código.

Los temas técnicos siguientes tienen su propio documento especializado:

| Tema | Documento |
|------|-----------|
| Contextos (`github`, `env`, `steps`, `needs`...) y variables | [`CONTEXTOS.md`](CONTEXTOS.md) |
| Eventos y triggers (`push`, `pull_request`, `schedule`...) | [`EVENTOS.md`](EVENTOS.md) |
| Expresiones `${{ }}`, operadores y funciones | [`EXPRESIONES.md`](EXPRESIONES.md) |
| Outputs, concurrency, matrix, containers, services | [`JOBS_AVANZADOS.md`](JOBS_AVANZADOS.md) |
| Workflows reutilizables (`workflow_call`) | [`WORKFLOWS_REUTILIZABLES.md`](WORKFLOWS_REUTILIZABLES.md) |
| Composite, JavaScript y Docker actions | [`ACTIONS_PERSONALIZADAS.md`](ACTIONS_PERSONALIZADAS.md) |
| GITHUB_TOKEN, secrets, OIDC, seguridad | [`SEGURIDAD_AVANZADA.md`](SEGURIDAD_AVANZADA.md) |
| Cache, artifacts, environments, Pages, Packages | [`CACHE_ARTIFACTS_DEPLOYMENT.md`](CACHE_ARTIFACTS_DEPLOYMENT.md) |
| Runners, debugging, act | [`RUNNERS_DEBUGGING.md`](RUNNERS_DEBUGGING.md) |

> 💡 **Punto de entrada recomendado**: Si buscas la ruta de aprendizaje completa, comienza por [`README.md`](README.md).


