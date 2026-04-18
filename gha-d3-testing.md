# 3.8 Testing / Verificación de D3 — Author and maintain actions

← [3.7 Publicación en el GitHub Marketplace y release strategies](gha-action-publicacion-marketplace.md) | [Índice](README.md) | [4.1 Workflow Templates (Starter Workflows) para la organización](gha-d4-workflow-templates.md) →

---

Este fichero es el banco de preguntas y ejercicios de repaso del módulo D3 (Author and maintain actions, 15–20% del examen GH-200). Cubre los siete subtemas del módulo: tipos de action, `action.yml`, inputs/outputs, workflow commands, version pinning, distribución y publicación en el Marketplace.

## Preguntas de opción múltiple

### Tipos de action (3.1)

**Pregunta 1.** Un workflow debe ejecutarse en `ubuntu-latest`, `windows-latest` y `macos-latest`. Necesita una action que configure un entorno de Python con dependencias específicas. ¿Qué tipo de action es compatible con los tres SO?

- A) Docker container action
- B) JavaScript action
- **C) Composite action** ✅
- D) Cualquiera de los tres tipos

*A es incorrecta*: Docker actions solo funcionan en Linux. *B es parcialmente correcta pero menos directa*: una composite action es más natural para encadenar comandos de shell de instalación. *D es incorrecta*: Docker no es compatible con Windows/macOS.

---

**Pregunta 2.** ¿Cuál de las siguientes capacidades tiene una JavaScript action pero NO una composite action?

- A) Soportar múltiples sistemas operativos
- **B) Declarar hooks `pre:` y `post:` en `action.yml`** ✅
- C) Usar `inputs:` y `outputs:`
- D) Referenciar otras actions con `uses:`

*A es incorrecta*: ambas son multi-OS. *C es incorrecta*: ambas soportan inputs/outputs. *D es incorrecta*: composite actions pueden usar `uses:` en sus steps.

---

### El fichero action.yml (3.2)

**Pregunta 3.** ¿Cuál es el valor correcto de `runs.using` para una composite action?

- A) `'node20'`
- B) `'docker'`
- **C) `'composite'`** ✅
- D) `'shell'`

*A es incorrecta*: `node20` es para JavaScript actions. *B es incorrecta*: `docker` es para Docker container actions. *D es incorrecta*: `'shell'` no es un valor válido.

---

**Pregunta 4.** En `action.yml`, un input tiene `required: true` y no se proporciona `default`. ¿Qué ocurre si el workflow que invoca la action no pasa ese input?

- A) GitHub usa el valor vacío `""` como default
- **B) El job falla con error indicando que el input requerido no fue provisto** ✅
- C) La action continúa con `null` como valor del input
- D) GitHub Actions ignora el campo `required` en tiempo de ejecución

*A, C y D son incorrectas*: cuando `required: true` y no hay `default`, la ausencia del input detiene la ejecución del job con error explícito.

---

### Workflow commands (3.4)

**Pregunta 5.** Un desarrollador usa `::set-output name=version::1.2.3` en un step. ¿Cuál es el problema?

- A) La sintaxis es incorrecta — debe ser `::set-output::version=1.2.3`
- **B) `::set-output::` está deprecado — debe usarse `echo "version=1.2.3" >> $GITHUB_OUTPUT`** ✅
- C) `set-output` solo funciona en JavaScript actions, no en steps `run:`
- D) No hay ningún problema; es la sintaxis correcta actual

*A es incorrecta*: la sintaxis `name=value` era la correcta para `::set-output::`. *C es incorrecta*: `::set-output::` funcionaba en cualquier step. *D es incorrecta*: fue deprecado debido a vulnerabilidades de inyección.

---

**Pregunta 6.** ¿Qué workflow command debe usar un step para que su valor sea enmascarado en todos los logs del job?

- A) `::error::valor-secreto`
- **B) `::add-mask::valor-secreto`** ✅
- C) `echo "valor-secreto" >> $GITHUB_ENV`
- D) `::hide::valor-secreto`

*A es incorrecta*: `::error::` crea una anotación de error, no enmascara. *C es incorrecta*: exportar una variable de entorno no la enmascara. *D es incorrecta*: `::hide::` no existe.

---

### Version pinning (3.5)

**Pregunta 7.** ¿Qué método de referencia garantiza que el workflow ejecute exactamente el mismo código de la action en cada ejecución, incluso si el mantenedor reescribe sus tags?

- A) `uses: actions/checkout@v4`
- B) `uses: actions/checkout@v4.1.7`
- **C) `uses: actions/checkout@a81bbbf8298c0fa03ea29cdc473d45769f953675`** ✅
- D) `uses: actions/checkout@main`

*A y D son incorrectas*: ambas son referencias móviles. *B es incorrecta*: aunque es más específico, un tag puede ser borrado y recreado. Solo el SHA inmutable garantiza reproducibilidad total.

---

### Distribución (3.6)

**Pregunta 8.** Un workflow tiene el step `uses: ./.github/actions/build` pero falla con "path not found". ¿Cuál es la causa más probable?

- A) La action debe estar en `.github/workflows/actions/`
- **B) El repositorio no fue clonado antes de invocar la action local** ✅
- C) Las actions locales deben referenciarse como `uses: local/build`
- D) El archivo de la action debe llamarse `main.yml`, no `action.yml`

*A es incorrecta*: `.github/actions/` es la ruta convencional correcta. *C es incorrecta*: la sintaxis `./` es la correcta para actions locales. *D es incorrecta*: el nombre del archivo es `action.yml`.

---

### Marketplace y versionado (3.7)

**Pregunta 9.** Para publicar una action en el GitHub Marketplace, ¿qué requisito es obligatorio además de tener `action.yml` en la raíz del repositorio?

- A) El repositorio debe tener al menos 10 estrellas
- **B) El repositorio debe ser público y `action.yml` debe incluir campos de `branding:` (icon y color)** ✅
- C) La action debe estar escrita en JavaScript
- D) El repositorio debe estar verificado como organización en GitHub

*A y D son incorrectas*: no son requisitos del Marketplace. *C es incorrecta*: el Marketplace acepta los tres tipos de action.

---

**Pregunta 10.** Un mantenedor acaba de publicar la versión `v2.0.0` de su action y quiere que todos los usuarios de `@v2` reciban la actualización automáticamente. ¿Qué debe hacer?

- A) Crear un nuevo release `v2` en la UI de GitHub Releases
- **B) Ejecutar `git tag -fa v2 && git push origin v2 --force` para mover el floating tag** ✅
- C) Editar el tag `v2` en la UI de GitHub para apuntar al nuevo commit
- D) No es necesario hacer nada adicional; GitHub mueve los tags automáticamente

*A es incorrecta*: crear un nuevo release `v2` crea un tag adicional pero no mueve el tag existente. *C es incorrecta*: la UI de GitHub no permite editar tags directamente. *D es incorrecta*: GitHub nunca mueve tags automáticamente.

---

## Ejercicios prácticos

### Ejercicio 1: Composite action completa con branding

Escribe el fichero `action.yml` para una composite action llamada `run-tests` que: (1) acepta un input `working-directory` (default: `.`), (2) produce un output `test-result` con el resultado del test, (3) ejecuta `npm test` en el directorio indicado, (4) incluye branding para el Marketplace.

```yaml
# action.yml
name: Run Tests
description: Ejecuta los tests de Node.js en el directorio especificado
author: "Mi Organización"

branding:
  icon: check-circle
  color: green

inputs:
  working-directory:
    description: Directorio donde ejecutar los tests
    required: false
    default: "."

outputs:
  test-result:
    description: Resultado de la ejecución de tests (passed o failed)
    value: ${{ steps.run.outputs.result }}

runs:
  using: composite
  steps:
    - name: Ejecutar tests
      id: run
      shell: bash
      working-directory: ${{ inputs.working-directory }}
      run: |
        if npm test; then
          echo "result=passed" >> $GITHUB_OUTPUT
        else
          echo "result=failed" >> $GITHUB_OUTPUT
          exit 1
        fi
```

---

### Ejercicio 2: Workflow con inputs/outputs y mitigación de script injection

Escribe un workflow que invoque la action `run-tests` del ejercicio anterior, lea su output, y demuestre la diferencia entre el uso inseguro y seguro de un valor de contexto externo en un step `run:`.

```yaml
# .github/workflows/ci.yml
name: CI

on:
  pull_request:
    types: [opened, synchronize]

jobs:
  test:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@a81bbbf8298c0fa03ea29cdc473d45769f953675  # v4.1.7

      - name: Ejecutar tests
        id: tests
        uses: ./.github/actions/run-tests
        with:
          working-directory: "./src"

      - name: Mostrar resultado
        run: echo "Resultado de tests: ${{ steps.tests.outputs.test-result }}"

      # INSEGURO — NO hacer esto: el título del PR puede contener comandos shell
      # - name: Log inseguro
      #   run: echo "PR: ${{ github.event.pull_request.title }}"

      # SEGURO — usar variable de entorno intermedia para sanitizar
      - name: Log seguro del título del PR
        env:
          PR_TITLE: ${{ github.event.pull_request.title }}
        run: echo "PR: $PR_TITLE"
```

---

### Ejercicio 3: Ciclo completo de release con floating major tag

Describe los comandos necesarios para publicar la versión `v1.2.0` de una action, manteniendo el tag flotante `v1` actualizado para que los usuarios de `@v1` reciban la actualización automáticamente.

```bash
# 1. Asegurarse de estar en el commit correcto (el que será v1.2.0)
git checkout main
git pull origin main

# 2. Crear el tag semántico específico (immutable)
git tag v1.2.0
git push origin v1.2.0

# 3. Mover el floating major tag v1 al mismo commit
git tag -fa v1 -m "v1.2.0"
git push origin v1 --force

# 4. En GitHub: ir a Releases > Draft a new release
# Seleccionar el tag v1.2.0, añadir descripción de cambios
# Si la action ya está en el Marketplace, la nueva release la actualiza automáticamente
```

El resultado es que los workflows con `uses: org/action@v1` obtienen automáticamente `v1.2.0`, mientras que los workflows con `uses: org/action@v1.2.0` o `uses: org/action@<SHA>` siguen en la versión que tenían.

---

## Checklist de repaso — D3

Antes del examen, verifica que puedes responder estas preguntas sin consultar el material:

- [ ] ¿Cuáles son los tres tipos de action y sus diferencias de SO soportados?
- [ ] ¿Qué campos son obligatorios en `action.yml`?
- [ ] ¿Cuáles son los valores válidos de `runs.using` para cada tipo?
- [ ] ¿Qué reemplaza a `::set-output::` y por qué fue deprecado?
- [ ] ¿Cómo se exporta una variable de entorno al job desde un step?
- [ ] ¿Qué diferencia hay entre `v4` y `v4.1.7` en términos de seguridad?
- [ ] ¿Por qué es el SHA completo más seguro que cualquier tag?
- [ ] ¿Qué sintaxis se usa para invocar una action local? ¿Qué step debe precederla?
- [ ] ¿Cuáles son los requisitos para publicar en el Marketplace?
- [ ] ¿Qué comando git mueve un floating tag al commit actual?
- [ ] ¿Cómo se mitiga el script injection al usar valores de `github.event.*` en `run:`?
- [ ] ¿Qué restricciones tienen las composite actions respecto a los jobs normales?

---

← [3.7 Publicación en el GitHub Marketplace y release strategies](gha-action-publicacion-marketplace.md) | [Índice](README.md) | [4.1 Workflow Templates (Starter Workflows) para la organización](gha-d4-workflow-templates.md) →
