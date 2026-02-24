#!/bin/bash

# Script para regenerar los workflows vacíos

echo "Regenerando workflows..."

# 07 - Secrets and Security
cat > .github/workflows/07-secrets-security.yml << 'ENDOFFILE'
name: "07 - Manejo de Secretos y Seguridad"

# Demuestra manejo seguro de secretos, variables de entorno y seguridad
# IMPORTANTE: Nunca exponer secretos en logs

on:
  workflow_dispatch:
    inputs:
      environment:
        description: 'Entorno'
        type: choice
        options:
          - dev
          - staging
          - production
        default: dev

# Variables a nivel de workflow
env:
  APP_NAME: "SecureApp"
  PUBLIC_API_URL: "https://api.example.com"

jobs:
  # ============================================================================
  # JOB 1: JERARQUÍA DE VARIABLES Y SECRETOS
  # ============================================================================
  variable-hierarchy:
    name: "Jerarquía de Variables"
    runs-on: ubuntu-latest
    env:
      # Variables a nivel de job
      JOB_VAR: "job-level"
      OVERRIDE_TEST: "from-job"

    steps:
      - name: "Variables a nivel de workflow"
        run: |
          echo "📋 Variables de Workflow:"
          echo "  APP_NAME: $APP_NAME"
          echo "  PUBLIC_API_URL: $PUBLIC_API_URL"

      - name: "Variables a nivel de job"
        run: |
          echo "📋 Variables de Job:"
          echo "  JOB_VAR: $JOB_VAR"
          echo "  OVERRIDE_TEST: $OVERRIDE_TEST"

      - name: "Variables a nivel de step (override)"
        env:
          STEP_VAR: "step-level"
          OVERRIDE_TEST: "from-step"
        run: |
          echo "📋 Variables de Step:"
          echo "  STEP_VAR: $STEP_VAR"
          echo "  OVERRIDE_TEST: $OVERRIDE_TEST (sobrescribe job)"

      - name: "Variables desde secrets"
        env:
          GITHUB_TOKEN_PRESENT: ${{ secrets.GITHUB_TOKEN != '' }}
        run: |
          echo "📋 Secretos:"
          echo "  GITHUB_TOKEN presente: $GITHUB_TOKEN_PRESENT"

      - name: "Variables desde inputs"
        run: |
          echo "📋 Inputs del Workflow:"
          echo "  environment: ${{ inputs.environment }}"

      - name: "Variables desde contexts"
        run: |
          echo "📋 Contexts de GitHub:"
          echo "  Repositorio: ${{ github.repository }}"
          echo "  Actor: ${{ github.actor }}"
          echo "  SHA: ${{ github.sha }}"
          echo "  Ref: ${{ github.ref }}"
          echo "  Run ID: ${{ github.run_id }}"

  # ============================================================================
  # JOB 2: MANEJO SEGURO DE SECRETOS
  # ============================================================================
  secure-secrets:
    name: "Manejo Seguro de Secretos"
    runs-on: ubuntu-latest
    environment: ${{ inputs.environment }}

    steps:
      - name: "Checkout"
        uses: actions/checkout@v4

      - name: "Usar secretos correctamente"
        env:
          API_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        run: |
          echo "✅ Secretos cargados en variables de entorno"
          echo "✅ Secretos usados de forma segura"

      - name: "Crear archivo de configuración con secreto"
        env:
          API_KEY: ${{ secrets.GITHUB_TOKEN }}
        run: |
          CONFIG_FILE=$(mktemp)

          cat > "$CONFIG_FILE" << EOF
          api:
            key: $API_KEY
          EOF

          echo "✅ Archivo de configuración creado: $CONFIG_FILE"
          rm -f "$CONFIG_FILE"
          echo "🗑️ Archivo temporal eliminado"

      - name: "⚠️ Ejemplos de malas prácticas (comentados)"
        run: |
          cat << 'EOF'
          ❌ NO HACER ESTO:

          1. ❌ echo "Token: ${{ secrets.TOKEN }}"
          2. ❌ echo "${{ secrets.PASSWORD }}" > file.txt
          3. ❌ git commit -m "Add token ${{ secrets.TOKEN }}"

          ✅ EN SU LUGAR:

          1. ✅ env:
                TOKEN: ${{ secrets.TOKEN }}
          2. ✅ Usar archivos temporales con permisos restringidos
          3. ✅ Nunca commitear secretos
          EOF

      - name: "Validar secretos requeridos"
        run: |
          MISSING_SECRETS=()

          if [ -z "${{ secrets.GITHUB_TOKEN }}" ]; then
            MISSING_SECRETS+=("GITHUB_TOKEN")
          fi

          if [ ${#MISSING_SECRETS[@]} -gt 0 ]; then
            echo "❌ Faltan secretos: ${MISSING_SECRETS[@]}"
            exit 1
          fi

          echo "✅ Todos los secretos necesarios están configurados"

      - name: "Enmascarar valores personalizados"
        run: |
          GENERATED_TOKEN="super-secret-$(date +%s)"
          echo "::add-mask::$GENERATED_TOKEN"
          echo "Token generado (enmascarado)"
          echo "Token: $GENERATED_TOKEN"

  # ============================================================================
  # JOB 3: VARIABLES POR ENTORNO
  # ============================================================================
  environment-variables:
    name: "Variables por Entorno"
    runs-on: ubuntu-latest
    environment: ${{ inputs.environment }}

    steps:
      - name: "Configuración por entorno"
        run: |
          echo "🌍 Entorno: ${{ inputs.environment }}"

      - name: "Lógica condicional por entorno"
        run: |
          case "${{ inputs.environment }}" in
            dev)
              echo "🔧 Configuración de DESARROLLO"
              DEBUG_MODE=true
              LOG_LEVEL=debug
              REPLICAS=1
              ;;
            staging)
              echo "🧪 Configuración de STAGING"
              DEBUG_MODE=false
              LOG_LEVEL=info
              REPLICAS=2
              ;;
            production)
              echo "🚀 Configuración de PRODUCCIÓN"
              DEBUG_MODE=false
              LOG_LEVEL=warn
              REPLICAS=5
              ;;
          esac

          echo "DEBUG_MODE=$DEBUG_MODE" >> $GITHUB_ENV
          echo "LOG_LEVEL=$LOG_LEVEL" >> $GITHUB_ENV
          echo "REPLICAS=$REPLICAS" >> $GITHUB_ENV

      - name: "Usar configuración del entorno"
        run: |
          echo "⚙️ Configuración aplicada:"
          echo "  Debug: $DEBUG_MODE"
          echo "  Log Level: $LOG_LEVEL"
          echo "  Réplicas: $REPLICAS"

  # ============================================================================
  # JOB 4: REPORTE FINAL
  # ============================================================================
  report:
    name: "Reporte Final"
    runs-on: ubuntu-latest
    needs: [variable-hierarchy, secure-secrets, environment-variables]
    if: always()

    steps:
      - name: "Generar reporte"
        run: |
          cat >> $GITHUB_STEP_SUMMARY << 'EOF'
          # 📋 Reporte de Configuración y Seguridad

          ## 🔧 Variables de Entorno
          - **Workflow level:** Variables compartidas por todos los jobs
          - **Job level:** Variables específicas del job
          - **Step level:** Variables que sobrescriben las anteriores

          ## 🔐 Secretos
          - **Repository secrets:** Disponibles en todos los workflows
          - **Environment secrets:** Específicos del environment

          ## 🌍 Entorno Seleccionado
          `${{ inputs.environment }}`

          ## ✅ Estado de Jobs
          - **Variable Hierarchy:** ${{ needs.variable-hierarchy.result }}
          - **Secure Secrets:** ${{ needs.secure-secrets.result }}
          - **Environment Variables:** ${{ needs.environment-variables.result }}
          EOF
ENDOFFILE

echo "✅ Archivo 07-secrets-security.yml creado"

# 08 - Dynamic Matrices
cat > .github/workflows/08-dynamic-matrices.yml << 'ENDOFFILE'
name: "08 - Matrices Dinámicas y Estrategias Avanzadas"

# Demuestra matrices dinámicas, condicionales complejas y estrategias avanzadas

on:
  workflow_dispatch:
    inputs:
      test-scope:
        description: 'Alcance de tests'
        type: choice
        options:
          - minimal
          - standard
          - full
        default: standard

jobs:
  # ============================================================================
  # JOB 1: GENERAR MATRIZ DINÁMICAMENTE
  # ============================================================================
  generate-matrix:
    name: "Generar Matriz Dinámica"
    runs-on: ubuntu-latest
    outputs:
      matrix: ${{ steps.generate.outputs.matrix }}
      test-matrix: ${{ steps.generate.outputs.test-matrix }}

    steps:
      - name: "Checkout"
        uses: actions/checkout@v4

      - name: "Generar matriz según input"
        id: generate
        run: |
          case "${{ inputs.test-scope }}" in
            minimal)
              MATRIX='{"os":["ubuntu-latest"],"python":["3.11"]}'
              TEST_MATRIX='["unit"]'
              ;;
            standard)
              MATRIX='{"os":["ubuntu-latest","windows-latest"],"python":["3.10","3.11"]}'
              TEST_MATRIX='["unit","integration"]'
              ;;
            full)
              MATRIX='{"os":["ubuntu-latest","windows-latest","macos-latest"],"python":["3.9","3.10","3.11","3.12"]}'
              TEST_MATRIX='["unit","integration","e2e"]'
              ;;
          esac

          echo "matrix=$MATRIX" >> $GITHUB_OUTPUT
          echo "test-matrix=$TEST_MATRIX" >> $GITHUB_OUTPUT

          echo "📊 Matriz generada para scope: ${{ inputs.test-scope }}"

  # ============================================================================
  # JOB 2: MATRIZ ESTÁTICA COMPLEJA
  # ============================================================================
  static-matrix:
    name: "${{ matrix.os }} - ${{ matrix.arch }}"
    runs-on: ${{ matrix.os }}
    strategy:
      fail-fast: false
      matrix:
        os: [ubuntu-latest, windows-latest, macos-latest]
        arch: [x64]
        include:
          - os: ubuntu-latest
            arch: x64
            extra-flags: "--extra"
        exclude:
          - os: macos-latest
            arch: arm64

    steps:
      - name: "Información de la matriz"
        run: |
          echo "🖥️ Sistema Operativo: ${{ matrix.os }}"
          echo "🏗️ Arquitectura: ${{ matrix.arch }}"

      - name: "Simular build"
        run: |
          echo "Compilando para ${{ matrix.arch }}..."
          sleep 2
          echo "✅ Build completado"

  # ============================================================================
  # JOB 3: USAR MATRIZ DINÁMICA
  # ============================================================================
  dynamic-matrix-tests:
    name: "Test: ${{ matrix.type }}"
    runs-on: ubuntu-latest
    needs: generate-matrix
    strategy:
      fail-fast: false
      matrix:
        type: ${{ fromJSON(needs.generate-matrix.outputs.test-matrix) }}

    steps:
      - name: "Ejecutar tests ${{ matrix.type }}"
        run: |
          echo "🧪 Ejecutando tests de tipo: ${{ matrix.type }}"

          case "${{ matrix.type }}" in
            unit)
              DURATION=3
              ;;
            integration)
              DURATION=5
              ;;
            e2e)
              DURATION=10
              ;;
          esac

          sleep $DURATION
          echo "✅ Tests ${{ matrix.type }} completados"

  # ============================================================================
  # JOB 4: REPORTE CONSOLIDADO
  # ============================================================================
  matrix-report:
    name: "Reporte de Matrices"
    runs-on: ubuntu-latest
    needs: [generate-matrix, static-matrix, dynamic-matrix-tests]
    if: always()

    steps:
      - name: "Generar reporte completo"
        run: |
          cat >> $GITHUB_STEP_SUMMARY << 'EOF'
          # 📊 Reporte de Matrices

          ## Resultados de Jobs
          - **Generate Matrix:** ${{ needs.generate-matrix.result }}
          - **Static Matrix:** ${{ needs.static-matrix.result }}
          - **Dynamic Matrix Tests:** ${{ needs.dynamic-matrix-tests.result }}

          ## 💡 Técnicas Demostradas

          ### 1. Matriz Estática
          ```yaml
          strategy:
            matrix:
              os: [ubuntu, windows, macos]
              version: [3.10, 3.11]
          ```

          ### 2. Matriz Dinámica
          ```yaml
          strategy:
            matrix:
              config: ${{ fromJSON(needs.job.outputs.matrix) }}
          ```

          ### 3. Include/Exclude
          ```yaml
          strategy:
            matrix:
              include:
                - os: ubuntu
                  extra: "special"
              exclude:
                - os: windows
                  version: "3.12"
          ```

          ## 📈 Métricas
          - **Scope:** `${{ inputs.test-scope }}`
          - **Test types:** `${{ needs.generate-matrix.outputs.test-matrix }}`
          EOF
ENDOFFILE

echo "✅ Archivo 08-dynamic-matrices.yml creado"

echo ""
echo "🎉 Todos los workflows han sido regenerados!"
echo ""
echo "Archivos creados:"
echo "  - .github/workflows/07-secrets-security.yml"
echo "  - .github/workflows/08-dynamic-matrices.yml"

