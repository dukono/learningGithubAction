# 🔄 GitHub Actions: Workflows Reutilizables

## 📚 Índice
1. [¿Qué es un Workflow Reutilizable?](#1-qué-es-un-workflow-reutilizable)
2. [Sintaxis del Workflow Llamado (Callee)](#2-sintaxis-del-workflow-llamado-callee)
3. [Sintaxis del Workflow Llamador (Caller)](#3-sintaxis-del-workflow-llamador-caller)
4. [Inputs: Tipos y Opciones](#4-inputs-tipos-y-opciones)
5. [Outputs del Workflow](#5-outputs-del-workflow)
6. [Secrets: inherit vs explícito](#6-secrets-inherit-vs-explícito)
7. [Limitaciones Importantes](#7-limitaciones-importantes)
8. [Patrones Avanzados](#8-patrones-avanzados)
9. [Ejemplo Completo Real](#9-ejemplo-completo-real)
10. [Preguntas de Examen](#10-preguntas-de-examen)

---

## 1. ¿Qué es un Workflow Reutilizable?

Un **workflow reutilizable** (reusable workflow) es un archivo `.yml` en `.github/workflows/` que puede ser **llamado por otros workflows**, como si fuera una función.

**¿Por qué usarlos?**

```
SIN reutilizables:                    CON reutilizables:
───────────────────────               ────────────────────────────

ci.yml (100 líneas)                   reusable-test.yml (50 líneas)
  → lint                                  → instalar deps
  → test                                  → ejecutar tests
  → build                                 → subir coverage

cd.yml (100 líneas)                   ci.yml (20 líneas)
  → lint (MISMO código)                   → uses: ./...reusable-test.yml
  → test (MISMO código)
  → deploy                            cd.yml (20 líneas)
                                          → uses: ./...reusable-test.yml
release.yml (100 líneas)                  → deploy
  → lint (MISMO código)
  → test (MISMO código)               release.yml (20 líneas)
  → release                               → uses: ./...reusable-test.yml
                                          → release
TOTAL: 300 líneas duplicadas          TOTAL: 90 líneas, sin duplicación
```

**Diferencia clave con Composite Actions:**

| | Reusable Workflow | Composite Action |
|---|---|---|
| Nivel | Workflow completo | Step individual |
| Puede tener jobs | ✅ Sí | ❌ No (solo steps) |
| Trigger | `workflow_call` | Se usa con `uses:` en un step |
| Archivos | `.github/workflows/*.yml` | `action.yml` en cualquier repo |
| Runners propios | ✅ Sí (define su propio `runs-on`) | ❌ Hereda el del job padre |

---

## 2. Sintaxis del Workflow Llamado (Callee)

El workflow reutilizable se identifica por tener `workflow_call` en el `on:`.

```yaml
# .github/workflows/reusable-ci.yml
name: CI Reutilizable

on:
  workflow_call:                    # ← Esto lo hace reutilizable
    inputs:                         # ← Parámetros que recibe
      node-version:
        description: 'Versión de Node.js'
        required: false
        default: '20'
        type: string
      run-tests:
        description: 'Si ejecutar los tests'
        required: false
        default: true
        type: boolean
      environment:
        description: 'Ambiente destino'
        required: true
        type: string
    
    secrets:                        # ← Secretos que necesita
      npm-token:
        description: 'Token de NPM'
        required: false
      deploy-key:
        description: 'Clave de deploy'
        required: true
    
    outputs:                        # ← Lo que devuelve al caller
      version:
        description: 'Versión del build'
        value: ${{ jobs.build.outputs.version }}
      artifact-name:
        description: 'Nombre del artifact generado'
        value: ${{ jobs.build.outputs.artifact-name }}

jobs:
  build:
    runs-on: ubuntu-latest
    outputs:                        # ← Outputs del job
      version: ${{ steps.version.outputs.version }}
      artifact-name: ${{ steps.build.outputs.name }}
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Setup Node
        uses: actions/setup-node@v4
        with:
          node-version: ${{ inputs.node-version }}  # ← Usa el input
      
      - name: Install
        run: npm ci
        env:
          NPM_TOKEN: ${{ secrets.npm-token }}       # ← Usa el secret
      
      - name: Test
        if: ${{ inputs.run-tests }}                 # ← Condicional con input
        run: npm test
      
      - name: Get version
        id: version
        run: echo "version=$(node -p "require('./package.json').version")" >> $GITHUB_OUTPUT
      
      - name: Build
        id: build
        run: |
          npm run build
          echo "name=app-${{ steps.version.outputs.version }}.zip" >> $GITHUB_OUTPUT
```

---

## 3. Sintaxis del Workflow Llamador (Caller)

El caller llama al workflow reutilizable usando `uses:` a nivel de **job** (no de step).

```yaml
# .github/workflows/ci.yml
name: CI

on:
  push:
    branches: [main]
  pull_request:

jobs:
  # ⭐ Llamada a workflow reutilizable
  test-and-build:
    uses: ./.github/workflows/reusable-ci.yml    # ← Ruta relativa (mismo repo)
    # O para otro repo:
    # uses: mi-org/mi-repo/.github/workflows/reusable-ci.yml@main
    with:
      node-version: '20'
      run-tests: true
      environment: 'staging'
    secrets:
      npm-token: ${{ secrets.NPM_TOKEN }}
      deploy-key: ${{ secrets.DEPLOY_KEY }}
  
  # Job que depende del reutilizable
  deploy:
    needs: test-and-build
    runs-on: ubuntu-latest
    steps:
      - name: Deploy
        run: |
          echo "Versión: ${{ needs.test-and-build.outputs.version }}"
          echo "Artifact: ${{ needs.test-and-build.outputs.artifact-name }}"
```

**⚠️ Restricción importante:** Un job que usa `uses:` (workflow reutilizable) NO puede tener `steps:` ni `runs-on:`. Son mutuamente excluyentes:

```yaml
jobs:
  mi-job:
    uses: ./.github/workflows/reusable.yml    # ✅ Solo esto
    # runs-on: ubuntu-latest    # ❌ No puede coexistir
    # steps:                    # ❌ No puede coexistir
    #   - run: echo "hola"
```

---

## 4. Inputs: Tipos y Opciones

Los inputs de `workflow_call` tienen los mismos tipos que `workflow_dispatch`:

| Tipo | Descripción | Ejemplo |
|---|---|---|
| `string` | Texto | `'main'`, `'v1.0.0'` |
| `number` | Número | `3`, `1.5` |
| `boolean` | Verdadero/falso | `true`, `false` |
| `choice` | ⚠️ Solo en `workflow_dispatch`, NO en `workflow_call` | — |
| `environment` | ⚠️ Solo en `workflow_dispatch`, NO en `workflow_call` | — |

```yaml
on:
  workflow_call:
    inputs:
      # String con default
      app-name:
        type: string
        required: false
        default: 'my-app'
      
      # Boolean
      enable-cache:
        type: boolean
        required: false
        default: true
      
      # Number
      timeout-minutes:
        type: number
        required: false
        default: 30
      
      # String requerido
      deploy-env:
        type: string
        required: true    # Sin default → obligatorio
```

**Acceso en el callee:**
```yaml
jobs:
  mi-job:
    timeout-minutes: ${{ inputs.timeout-minutes }}   # ← En campos YAML
    steps:
      - name: Mi step
        run: echo "App: ${{ inputs.app-name }}"      # ← En expresiones
      
      - name: Cache condicional
        if: ${{ inputs.enable-cache }}               # ← En condiciones
        uses: actions/cache@v4
```

---

## 5. Outputs del Workflow

Los outputs fluyen así:

```
GITHUB_OUTPUT (step)  →  outputs del job  →  outputs del workflow  →  needs del caller
```

**Paso a paso:**

```yaml
# === EN EL CALLEE ===

on:
  workflow_call:
    outputs:
      # 3. Exponer al caller lo que produce el workflow
      final-version:
        description: 'Versión final'
        value: ${{ jobs.build.outputs.computed-version }}  # ← referencia al job output
      
      deploy-url:
        description: 'URL del deploy'
        value: ${{ jobs.deploy.outputs.url }}

jobs:
  build:
    runs-on: ubuntu-latest
    outputs:
      # 2. Exponer desde el job lo que calculó un step
      computed-version: ${{ steps.calc.outputs.version }}
    
    steps:
      - name: Calcular versión
        id: calc
        run: |
          # 1. Guardar en GITHUB_OUTPUT
          echo "version=1.2.3-$(git rev-parse --short HEAD)" >> $GITHUB_OUTPUT
  
  deploy:
    needs: build
    runs-on: ubuntu-latest
    outputs:
      url: ${{ steps.deploy-step.outputs.deployment-url }}
    steps:
      - id: deploy-step
        run: echo "deployment-url=https://staging.example.com" >> $GITHUB_OUTPUT


# === EN EL CALLER ===

jobs:
  run-reusable:
    uses: ./.github/workflows/reusable-ci.yml
    with:
      environment: staging
    secrets: inherit
  
  use-outputs:
    needs: run-reusable
    runs-on: ubuntu-latest
    steps:
      - run: |
          # 4. Acceder en el caller
          echo "Versión: ${{ needs.run-reusable.outputs.final-version }}"
          echo "URL: ${{ needs.run-reusable.outputs.deploy-url }}"
```

---

## 6. Secrets: inherit vs explícito

Hay dos formas de pasar secrets a un workflow reutilizable:

### Forma 1: `secrets: inherit` (heredar todos)

```yaml
# Caller
jobs:
  mi-job:
    uses: ./.github/workflows/reusable.yml
    with:
      environment: prod
    secrets: inherit    # ← Pasa TODOS los secrets del caller automáticamente
```

- ✅ Más sencillo
- ✅ El callee puede usar cualquier secret disponible en el caller
- ⚠️ Menos control — el callee puede acceder a secrets que no necesita
- ⚠️ Solo funciona cuando caller y callee están en el mismo repo u organización

### Forma 2: Secrets explícitos

```yaml
# Callee — declara qué secrets necesita
on:
  workflow_call:
    secrets:
      api-key:
        required: true
      optional-token:
        required: false

# Caller — pasa exactamente esos secrets
jobs:
  mi-job:
    uses: ./.github/workflows/reusable.yml
    secrets:
      api-key: ${{ secrets.MY_API_KEY }}
      optional-token: ${{ secrets.OPTIONAL }}
```

- ✅ Control explícito — solo lo que se necesita
- ✅ Funciona entre repos y organizaciones
- ⚠️ Hay que actualizar caller si el callee necesita un secret nuevo

**Regla recomendada:**
- Mismo repo/organización: `secrets: inherit`
- Repos externos / organización diferente: secrets explícitos

---

## 7. Limitaciones Importantes

Los workflows reutilizables funcionan como funciones llamadas remotamente: el caller **cede el control** al callee, que corre sus propios jobs en sus propios runners, y devuelve el control con sus outputs cuando termina. Esta arquitectura impone límites técnicos: el callee es un proceso separado que no tiene acceso al entorno del caller (variables de entorno, contexto de ejecución, etc.) salvo lo que se le pase explícitamente como `inputs` o `secrets`.

Las limitaciones siguientes son frecuentes en exámenes y reflejan estos límites arquitecturales:

### 7.1 Anidamiento máximo: 4 niveles

```
Caller A
  └── Callee B (nivel 1)
        └── Callee C (nivel 2)
              └── Callee D (nivel 3)
                    └── Callee E (nivel 4) ← MÁXIMO
                          └── ❌ Error: no se puede anidar más
```

### 7.2 No se puede usar en matrix directamente

```yaml
# ❌ ESTO NO FUNCIONA
jobs:
  test:
    strategy:
      matrix:
        version: ['18', '20', '22']
    uses: ./.github/workflows/reusable.yml  # ❌ Error
    with:
      node-version: ${{ matrix.version }}
```

```yaml
# ✅ SOLUCIÓN: El reutilizable define su propio matrix internamente
# O: El caller hace múltiples jobs explícitos
jobs:
  test-18:
    uses: ./.github/workflows/reusable.yml
    with:
      node-version: '18'
  test-20:
    uses: ./.github/workflows/reusable.yml
    with:
      node-version: '20'
```

### 7.3 No puede llamar a otros reutilizables (en el contexto de un job que ya fue llamado)

Un workflow reutilizable puede llamar a otro reutilizable, pero solo desde el nivel de workflow (no hay manera de encadenar si no tienes el workflow completo). La restricción real es el límite de 4 niveles.

### 7.4 El campo `uses:` y `runs-on:` son excluyentes

```yaml
jobs:
  mi-job:
    uses: ./.github/workflows/reusable.yml
    runs-on: ubuntu-latest  # ❌ Error de sintaxis
```

### 7.5 No puede acceder a variables de entorno del caller

```yaml
# Caller
env:
  MY_VAR: "valor"

jobs:
  run:
    uses: ./.github/workflows/reusable.yml
    # El callee NO puede acceder a MY_VAR
    # Debes pasarla como input:
    with:
      my-var: ${{ env.MY_VAR }}  # ✅ Así sí
```

### 7.6 Contextos disponibles en el callee

| Contexto | Disponible en callee |
|---|---|
| `github` | ✅ Sí (el del caller) |
| `inputs` | ✅ Sí (los declarados en `workflow_call`) |
| `secrets` | ✅ Sí (los heredados o declarados) |
| `vars` | ✅ Sí (del repo del callee) |
| `env` del caller | ❌ No (hay que pasar como input) |

---

## 8. Patrones Avanzados

### Patrón 1: Workflow de CI/CD completo

```yaml
# .github/workflows/ci-cd.yml (caller principal)
name: CI/CD

on:
  push:
    branches: [main]
  pull_request:

jobs:
  ci:
    uses: ./.github/workflows/reusable-ci.yml
    with:
      node-version: '20'
    secrets: inherit
  
  staging-deploy:
    needs: ci
    if: github.ref == 'refs/heads/main'
    uses: ./.github/workflows/reusable-deploy.yml
    with:
      environment: staging
      version: ${{ needs.ci.outputs.version }}
    secrets: inherit
  
  prod-deploy:
    needs: staging-deploy
    if: github.ref == 'refs/heads/main'
    uses: ./.github/workflows/reusable-deploy.yml
    with:
      environment: production
      version: ${{ needs.ci.outputs.version }}
    secrets: inherit
```

### Patrón 2: Biblioteca de workflows en organización

```yaml
# En el repo mi-org/workflows-library
# Archivo: .github/workflows/nodejs-ci.yml
on:
  workflow_call:
    inputs:
      node-version:
        type: string
        default: '20'

# En cualquier repo de la organización
jobs:
  ci:
    uses: mi-org/workflows-library/.github/workflows/nodejs-ci.yml@main
    with:
      node-version: '20'
    secrets: inherit
```

### Patrón 3: Condicional según evento del caller

El callee puede inspeccionar el `github.event_name` del caller:

```yaml
# En el callee
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - name: Deploy solo en push a main
        if: github.event_name == 'push' && github.ref == 'refs/heads/main'
        run: echo "Desplegando..."
      
      - name: Solo validar en PR
        if: github.event_name == 'pull_request'
        run: echo "Solo validando..."
```

---

## 9. Ejemplo Completo Real

Un sistema de CI/CD completo con workflows reutilizables:

```yaml
# ── .github/workflows/reusable-test.yml ──────────────────────────
name: Tests Reutilizables

on:
  workflow_call:
    inputs:
      node-version:
        type: string
        required: false
        default: '20'
      coverage-threshold:
        type: number
        required: false
        default: 80
    secrets:
      npm-token:
        required: false
    outputs:
      coverage:
        description: 'Porcentaje de coverage'
        value: ${{ jobs.test.outputs.coverage }}
      test-passed:
        description: 'Si los tests pasaron'
        value: ${{ jobs.test.outputs.passed }}

jobs:
  test:
    runs-on: ubuntu-latest
    outputs:
      coverage: ${{ steps.run-tests.outputs.coverage }}
      passed: ${{ steps.run-tests.outputs.passed }}
    
    steps:
      - uses: actions/checkout@v4
      
      - uses: actions/setup-node@v4
        with:
          node-version: ${{ inputs.node-version }}
          cache: 'npm'
      
      - name: Install dependencies
        run: npm ci
        env:
          NPM_TOKEN: ${{ secrets.npm-token }}
      
      - name: Run tests with coverage
        id: run-tests
        run: |
          npm test -- --coverage --coverageReporters=text-summary 2>&1 | tee coverage.txt
          COVERAGE=$(grep "Statements" coverage.txt | grep -oP '\d+(?=%)' | head -1)
          echo "coverage=${COVERAGE:-0}" >> $GITHUB_OUTPUT
          if [ "${COVERAGE:-0}" -ge ${{ inputs.coverage-threshold }} ]; then
            echo "passed=true" >> $GITHUB_OUTPUT
          else
            echo "passed=false" >> $GITHUB_OUTPUT
            echo "❌ Coverage $COVERAGE% < threshold ${{ inputs.coverage-threshold }}%"
            exit 1
          fi
```

```yaml
# ── .github/workflows/ci.yml (caller) ────────────────────────────
name: CI

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  test-node18:
    uses: ./.github/workflows/reusable-test.yml
    with:
      node-version: '18'
      coverage-threshold: 75
    secrets: inherit
  
  test-node20:
    uses: ./.github/workflows/reusable-test.yml
    with:
      node-version: '20'
      coverage-threshold: 80
    secrets: inherit
  
  report:
    needs: [test-node18, test-node20]
    runs-on: ubuntu-latest
    if: always()
    steps:
      - name: Reporte de cobertura
        run: |
          echo "## Resultados de Tests" >> $GITHUB_STEP_SUMMARY
          echo "| Node | Coverage | Estado |" >> $GITHUB_STEP_SUMMARY
          echo "|------|----------|--------|" >> $GITHUB_STEP_SUMMARY
          echo "| 18 | ${{ needs.test-node18.outputs.coverage }}% | ${{ needs.test-node18.result }} |" >> $GITHUB_STEP_SUMMARY
          echo "| 20 | ${{ needs.test-node20.outputs.coverage }}% | ${{ needs.test-node20.result }} |" >> $GITHUB_STEP_SUMMARY
```

---

## 10. Preguntas de Examen

**P: ¿Cuál es el trigger que hace reutilizable un workflow?**
→ `workflow_call` en el campo `on:`

**P: ¿Puede un job con `uses:` tener también `steps:` y `runs-on:`?**
→ No. Son mutuamente excluyentes.

**P: ¿Cuántos niveles máximos de workflows reutilizables se pueden anidar?**
→ 4 niveles.

**P: ¿Cuál es la diferencia entre `secrets: inherit` y secrets explícitos?**
→ `inherit` pasa todos los secrets automáticamente (solo mismo repo/org). Explícitos dan control granular y funcionan entre repositorios.

**P: ¿Puede un workflow reutilizable usarse en una matrix?**
→ No directamente. El job con `uses:` no puede tener `strategy.matrix`. Hay que crear múltiples jobs o poner la matrix dentro del callee.

**P: ¿Cómo accede el caller a los outputs del workflow reutilizable?**
→ Con `${{ needs.<job-id>.outputs.<output-name> }}`, igual que con cualquier otro job.

**P: ¿Puede el callee acceder a variables de entorno (`env:`) definidas en el caller?**
→ No. Debe recibirlas como `inputs`.

---

*Referencia: [GitHub Docs - Reusing Workflows](https://docs.github.com/en/actions/using-workflows/reusing-workflows)*

