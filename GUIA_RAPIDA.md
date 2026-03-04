# 🎯 Guía Rápida de Referencia - GitHub Actions

Esta es una guía de referencia rápida con los patrones más comunes. Para ejemplos completos, ver los workflows en `.github/workflows/`.

## 📋 Índice Rápido

- [Compartir Datos](#compartir-datos)
- [Workflows Reusables](#workflows-reusables)
- [Matrices](#matrices)
- [Cache](#cache)
- [Secretos](#secretos)
- [Condicionales](#condicionales)
- [Artifacts](#artifacts)

---

## 🔄 Compartir Datos

### Entre Steps (mismo job)

```yaml
steps:
  # Producir
  - name: Generar valor
    id: producer
    run: echo "my-value=hello" >> $GITHUB_OUTPUT
  
  # Consumir
  - name: Usar valor
    run: echo "${{ steps.producer.outputs.my-value }}"
```

### Entre Jobs

```yaml
jobs:
  job1:
    outputs:
      data: ${{ steps.step1.outputs.value }}
    steps:
      - id: step1
        run: echo "value=shared-data" >> $GITHUB_OUTPUT
  
  job2:
    needs: job1
    steps:
      - run: echo "${{ needs.job1.outputs.data }}"
```

### Variables de Entorno Globales

```yaml
steps:
  - name: Crear variable global
    run: echo "MY_VAR=value" >> $GITHUB_ENV
  
  - name: Usar variable
    run: echo "$MY_VAR"
```

### Agregar al PATH

El PATH es donde el sistema busca comandos. `$GITHUB_PATH` agrega directorios permanentemente para los siguientes steps.

**Ejemplo Simple:**
```yaml
steps:
  # 1. Crear un script personalizado
  - name: Crear comando personalizado
    run: |
      mkdir -p $HOME/bin
      echo '#!/bin/bash' > $HOME/bin/saludar
      echo 'echo "¡Hola desde mi comando!"' >> $HOME/bin/saludar
      chmod +x $HOME/bin/saludar
  
  # 2. Agregar el directorio al PATH
  - name: Agregar al PATH
    run: echo "$HOME/bin" >> $GITHUB_PATH
  
  # 3. Ahora puedes usar el comando directamente
  - name: Usar comando
    run: saludar  # Funciona! El sistema encuentra el comando en PATH
```

**Ejemplo Real - Instalar herramienta:**
```yaml
steps:
  # Descargar una herramienta a un directorio
  - name: Instalar herramienta
    run: |
      mkdir -p /tmp/tools
      wget https://example.com/tool -O /tmp/tools/tool
      chmod +x /tmp/tools/tool
  
  # Agregar al PATH
  - run: echo "/tmp/tools" >> $GITHUB_PATH
  
  # Usar la herramienta sin especificar la ruta completa
  - run: tool --version        # ✅ Funciona
    # En lugar de: /tmp/tools/tool --version
```

**Sin GITHUB_PATH (error):**
```yaml
steps:
  - run: |
      mkdir -p $HOME/bin
      echo 'echo "Hola"' > $HOME/bin/mi-script
      chmod +x $HOME/bin/mi-script
  
  - run: mi-script  # ❌ ERROR: comando no encontrado
    # El sistema no sabe dónde buscar "mi-script"
```

**Con GITHUB_PATH (funciona):**
```yaml
steps:
  - run: |
      mkdir -p $HOME/bin
      echo 'echo "Hola"' > $HOME/bin/mi-script
      chmod +x $HOME/bin/mi-script
      echo "$HOME/bin" >> $GITHUB_PATH
  
  - run: mi-script  # ✅ Funciona! El sistema encuentra el comando
```

---

## ♻️ Workflows Reusables

### Definir Workflow Reusable

```yaml
# .github/workflows/reusable.yml
name: Reusable Workflow

on:
  workflow_call:
    inputs:
      environment:
        required: true
        type: string
      version:
        required: true
        type: string
    secrets:
      token:
        required: true
    outputs:
      result:
        value: ${{ jobs.deploy.outputs.result }}

jobs:
  deploy:
    runs-on: ubuntu-latest
    outputs:
      result: ${{ steps.deploy.outputs.result }}
    steps:
      - run: echo "Deploying ${{ inputs.version }} to ${{ inputs.environment }}"
      - id: deploy
        run: echo "result=success" >> $GITHUB_OUTPUT
```

### Llamar Workflow Reusable

```yaml
# .github/workflows/caller.yml
jobs:
  call-workflow:
    uses: ./.github/workflows/reusable.yml
    with:
      environment: production
      version: 1.0.0
    secrets:
      token: ${{ secrets.DEPLOY_TOKEN }}
  
  use-output:
    needs: call-workflow
    runs-on: ubuntu-latest
    steps:
      - run: echo "${{ needs.call-workflow.outputs.result }}"
```

---

## 📊 Matrices

### Matriz Estática Simple

```yaml
strategy:
  matrix:
    os: [ubuntu-latest, windows-latest, macos-latest]
    python: ['3.10', '3.11', '3.12']
# Genera 9 jobs (3 × 3)
```

### Matriz con Include/Exclude

```yaml
strategy:
  matrix:
    os: [ubuntu-latest, windows-latest]
    version: ['3.10', '3.11']
    include:
      # Agregar combinación específica
      - os: ubuntu-latest
        version: '3.12'
        experimental: true
    exclude:
      # Remover combinación
      - os: windows-latest
        version: '3.11'
```

### Matriz Dinámica

```yaml
jobs:
  setup:
    outputs:
      matrix: ${{ steps.generate.outputs.matrix }}
    steps:
      - id: generate
        run: |
          MATRIX='["env1","env2","env3"]'
          echo "matrix=$MATRIX" >> $GITHUB_OUTPUT
  
  use-matrix:
    needs: setup
    strategy:
      matrix:
        env: ${{ fromJSON(needs.setup.outputs.matrix) }}
    steps:
      - run: echo "Processing ${{ matrix.env }}"
```

### Matriz Anidada

```yaml
strategy:
  matrix:
    config:
      - name: small
        cpu: 2
        memory: 4GB
      - name: large
        cpu: 8
        memory: 16GB
steps:
  - run: |
      echo "Config: ${{ matrix.config.name }}"
      echo "CPU: ${{ matrix.config.cpu }}"
```

---

## 💾 Cache

### Cache Automático (Python)

```yaml
- uses: actions/setup-python@v5
  with:
    python-version: '3.11'
    cache: 'pip'  # Automático
```

### Cache Automático (Node.js)

```yaml
- uses: actions/setup-node@v4
  with:
    node-version: '20'
    cache: 'npm'  # Automático
```

### Cache Manual

```yaml
- uses: actions/cache@v4
  with:
    path: |
      ~/.cache/pip
      ~/.local
    key: ${{ runner.os }}-pip-${{ hashFiles('**/requirements.txt') }}
    restore-keys: |
      ${{ runner.os }}-pip-
      ${{ runner.os }}-
```

### Cache con Verificación

```yaml
- uses: actions/cache@v4
  id: cache
  with:
    path: dist/
    key: build-${{ hashFiles('src/**') }}

- name: Build solo si no hay cache
  if: steps.cache.outputs.cache-hit != 'true'
  run: npm run build

- name: Usar cache
  if: steps.cache.outputs.cache-hit == 'true'
  run: echo "Using cached build"
```

---

## 🔐 Secretos

### Uso Básico (CORRECTO)

```yaml
- name: Deploy
  env:
    API_TOKEN: ${{ secrets.API_TOKEN }}
    DB_PASSWORD: ${{ secrets.DB_PASSWORD }}
  run: |
    # Los secretos están disponibles en env vars
    # Automáticamente enmascarados en logs
    deploy.sh
```

### Secretos por Environment

```yaml
jobs:
  deploy:
    environment: production
    steps:
      - env:
          # Secretos específicos del environment "production"
          PROD_TOKEN: ${{ secrets.PROD_DEPLOY_TOKEN }}
        run: deploy.sh
```

### Enmascarar Valores Adicionales

```yaml
- run: |
    TOKEN="generated-$(date +%s)-secret"
    echo "::add-mask::$TOKEN"
    echo "Token: $TOKEN"  # Se mostrará como ***
```

### ❌ NO HACER

```yaml
# ❌ NUNCA hacer esto:
- run: echo "Token: ${{ secrets.TOKEN }}"
- run: echo "${{ secrets.PASSWORD }}" > file.txt
- run: curl https://api.com?token=${{ secrets.TOKEN }}
```

---

## ❓ Condicionales

### Condicionales Simples

```yaml
- name: Solo en main
  if: github.ref == 'refs/heads/main'
  run: echo "On main branch"

- name: Solo en PR
  if: github.event_name == 'pull_request'
  run: echo "This is a PR"

- name: Solo si job anterior tuvo éxito
  if: success()
  run: echo "Previous steps succeeded"

- name: Siempre ejecutar (incluso si falló)
  if: always()
  run: echo "Cleanup"

- name: Solo si falló
  if: failure()
  run: echo "Something failed"
```

### Condicionales Complejos

```yaml
- name: Deploy a producción
  if: |
    github.ref == 'refs/heads/main' &&
    github.event_name == 'push' &&
    !contains(github.event.head_commit.message, '[skip ci]')
  run: deploy.sh

- name: Condicional con outputs
  if: needs.build.outputs.should-deploy == 'true'
  run: deploy.sh
```

### Job Condicional

```yaml
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - run: npm test
  
  deploy:
    needs: test
    if: |
      github.ref == 'refs/heads/main' &&
      needs.test.result == 'success'
    runs-on: ubuntu-latest
    steps:
      - run: deploy.sh
```

---

## 📦 Artifacts

### Subir Artifacts

```yaml
- name: Build
  run: npm run build

- name: Upload artifacts
  uses: actions/upload-artifact@v4
  with:
    name: build-output
    path: |
      dist/
      build/
    retention-days: 7
```

### Descargar Artifacts

```yaml
- name: Download artifacts
  uses: actions/download-artifact@v4
  with:
    name: build-output
    path: ./downloaded

- name: Use artifacts
  run: ls -la downloaded/
```

### Artifacts entre Jobs

```yaml
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - run: echo "data" > output.txt
      - uses: actions/upload-artifact@v4
        with:
          name: my-artifact
          path: output.txt
  
  use:
    needs: build
    runs-on: ubuntu-latest
    steps:
      - uses: actions/download-artifact@v4
        with:
          name: my-artifact
      - run: cat output.txt
```

---

## 🎯 Patterns Comunes

### Pipeline CI/CD Básico

```yaml
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: npm ci
      - run: npm test
  
  build:
    needs: test
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: npm ci
      - run: npm run build
      - uses: actions/upload-artifact@v4
        with:
          name: dist
          path: dist/
  
  deploy:
    needs: build
    if: github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    environment: production
    steps:
      - uses: actions/download-artifact@v4
        with:
          name: dist
      - run: deploy.sh
```

### Multi-Environment Deploy

```yaml
jobs:
  deploy-dev:
    if: github.ref == 'refs/heads/develop'
    uses: ./.github/workflows/deploy.yml
    with:
      environment: dev
  
  deploy-staging:
    if: github.ref == 'refs/heads/main'
    uses: ./.github/workflows/deploy.yml
    with:
      environment: staging
  
  deploy-prod:
    needs: deploy-staging
    if: github.ref == 'refs/heads/main'
    uses: ./.github/workflows/deploy.yml
    with:
      environment: production
    # Requiere aprobación manual en settings
```

### Monorepo Pattern

```yaml
jobs:
  detect-changes:
    outputs:
      frontend: ${{ steps.changes.outputs.frontend }}
      backend: ${{ steps.changes.outputs.backend }}
    steps:
      - uses: actions/checkout@v4
      - id: changes
        run: |
          if git diff --name-only HEAD^ | grep "^frontend/"; then
            echo "frontend=true" >> $GITHUB_OUTPUT
          fi
          if git diff --name-only HEAD^ | grep "^backend/"; then
            echo "backend=true" >> $GITHUB_OUTPUT
          fi
  
  build-frontend:
    needs: detect-changes
    if: needs.detect-changes.outputs.frontend == 'true'
    runs-on: ubuntu-latest
    steps:
      - run: cd frontend && npm run build
  
  build-backend:
    needs: detect-changes
    if: needs.detect-changes.outputs.backend == 'true'
    runs-on: ubuntu-latest
    steps:
      - run: cd backend && go build
```

---

## 🔧 Funciones Útiles

### Funciones de Contexto

```yaml
# Convertir JSON
${{ fromJSON('[1,2,3]') }}

# Convertir a JSON
${{ toJSON(github.event) }}

# Comprobar contiene
${{ contains(github.ref, 'main') }}

# Starts with
${{ startsWith(github.ref, 'refs/heads/feature/') }}

# Ends with
${{ endsWith(github.ref, '/main') }}

# Format string
${{ format('Hello {0} {1}', 'GitHub', 'Actions') }}
```

### Funciones de Estado

```yaml
# Estado exitoso
${{ success() }}

# Siempre ejecutar
${{ always() }}

# Si falló
${{ failure() }}

# Si cancelado
${{ cancelled() }}
```

---

## 📊 Step Summary

```yaml
- name: Generar resumen
  run: |
    cat >> $GITHUB_STEP_SUMMARY << 'EOF'
    # Mi Reporte
    
    ## Resultados
    - Test: ✅ Passed
    - Build: ✅ Success
    
    ## Métricas
    | Metric | Value |
    |--------|-------|
    | Tests | 150 |
    | Coverage | 87% |
    EOF
```

---

## 🎯 Tips Rápidos

### Debugging

```yaml
# Ver todos los contexts
- run: echo '${{ toJSON(github) }}'
- run: echo '${{ toJSON(env) }}'
- run: echo '${{ toJSON(job) }}'

# Enable debug logging
# Settings > Secrets > Add ACTIONS_STEP_DEBUG=true
```

### Performance

```yaml
# Usar cache siempre que sea posible
# Ejecutar jobs en paralelo (no usar needs si no es necesario)
# Usar matrices para paralelizar
# Limitar concurrencia si es necesario:
concurrency:
  group: ${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: true
```

### Seguridad

```yaml
# Limitar permisos
permissions:
  contents: read
  packages: write

# Usar environments para producción
environment:
  name: production
  # Configurar required reviewers en settings

# Nunca exponer secretos en logs
# Usar ::add-mask:: para valores sensibles
```

---

## 📚 Referencias Rápidas

- **Documentación técnica:** `GITHUB_ACTIONS_ARQUITECTURA_TECNICA.md`
- **Ejemplos completos:** `EJEMPLOS_AVANZADOS_README.md`
- **Workflows de ejemplo:** `.github/workflows/`
- **Docs oficiales:** https://docs.github.com/en/actions

---

**🎉 Happy Coding con GitHub Actions!**

