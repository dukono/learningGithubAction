[anterior: 1.19 Environment protections](gha-d1-environment-protections.md) | [siguiente: 2.1 Interpretación de triggers y UI](gha-d2-triggers-ui.md)

# X.20 Testing / Verificación de D1 — Author and manage workflows

Este fichero cubre la verificación del dominio D1 completo mediante preguntas de opción múltiple estilo GH-200 y ejercicios prácticos con YAML. Repasa todos los subtemas tratados en el módulo: estructura del workflow, triggers, jobs, steps, condicionales, dependencias, service containers, matrix, contextos, expresiones, caché, artefactos, file commands, variables predefinidas, badges y environment protections.

---

## Preguntas de opción múltiple

**Pregunta 1:** ¿Cuál es la clave obligatoria mínima que debe contener todo fichero de workflow de GitHub Actions?

A) `name`, `on`, `jobs`
B) `on` y `jobs`
C) `jobs` únicamente
D) `name` y `on`

> **Respuesta correcta: B)** — `on` define cuándo se dispara el workflow y `jobs` define qué se ejecuta. `name` es opcional.

---

**Pregunta 2:** Un workflow debe ejecutarse solo cuando se hace push a ramas que empiecen por `release/`. ¿Cuál es la sintaxis correcta?

A) `on: push: branches: "release/*"`
B) `on: { push: { branches: ["release/**"] } }`
C) `on: push: branches: - "release/**"`
D) `on: push: branch: release`

> **Respuesta correcta: C)** — El patrón `release/**` con glob doble captura cualquier rama con ese prefijo y la sintaxis YAML de lista es la correcta para `branches`.

---

**Pregunta 3:** ¿Qué evento se usa para ejecutar un workflow desde la UI de GitHub sin commits ni PRs?

A) `repository_dispatch`
B) `workflow_call`
C) `workflow_dispatch`
D) `schedule`

> **Respuesta correcta: C)** — `workflow_dispatch` permite lanzar el workflow manualmente desde la pestaña Actions de GitHub o mediante la API.

---

**Pregunta 4:** Un evento `schedule` usa sintaxis cron. ¿Cuál de las siguientes expresiones ejecuta el workflow a las 3:15 UTC todos los lunes?

A) `'15 3 * * 1'`
B) `'3 15 * * MON'`
C) `'* 3 15 * 1'`
D) `'15 3 1 * *'`

> **Respuesta correcta: A)** — El formato cron es `minuto hora día-mes mes día-semana`. Lunes = 1, hora 3, minuto 15.

---

**Pregunta 5:** ¿Qué permisos mínimos necesita el `GITHUB_TOKEN` para que un job pueda publicar comentarios en un pull request?

A) `contents: write`
B) `pull-requests: write`
C) `issues: write`
D) `actions: write`

> **Respuesta correcta: B)** — Los comentarios en PRs requieren el permiso `pull-requests: write`. `issues: write` aplica a issues, no a PRs.

---

**Pregunta 6:** ¿Cuál de las siguientes opciones detiene la ejecución del job actual pero permite que los jobs dependientes que usen `if: always()` continúen?

A) `continue-on-error: true` en el step fallido
B) `fail-fast: false` en la matrix
C) Un step con `if: failure()` que llame a `exit 0`
D) `continue-on-error: true` no afecta a jobs dependientes; `if: always()` en el job dependiente sí lo hace

> **Respuesta correcta: D)** — `continue-on-error: true` en un step marca el job como éxito aunque el step falle. Para que un job dependiente corra tras un fallo, se usa `if: always()` o `if: failure()` en el job dependiente.

---

**Pregunta 7:** En un step, ¿qué diferencia hay entre `run` y `uses`?

A) `run` ejecuta scripts de shell; `uses` invoca una action reutilizable
B) `run` invoca una action; `uses` ejecuta comandos de shell
C) Ambos son equivalentes pero `uses` solo funciona en runners self-hosted
D) `run` es para jobs y `uses` es para steps

> **Respuesta correcta: A)** — `run` ejecuta comandos en el shell del runner, mientras que `uses` referencia una action (del marketplace, local o de otro repo).

---

**Pregunta 8:** ¿Cuál es el comportamiento de `if: github.event_name == 'push' && github.ref == 'refs/heads/main'` en un step?

A) El step solo corre si el evento es push Y la rama es main
B) El step corre si el evento es push O la rama es main
C) La condición siempre falla porque `github.ref` no contiene el prefijo `refs/heads/`
D) El step corre en todos los eventos pero solo en main

> **Respuesta correcta: A)** — El operador `&&` requiere que ambas condiciones sean verdaderas. `github.ref` sí incluye el prefijo `refs/heads/` en eventos push.

---

**Pregunta 9:** ¿Qué clave se usa para definir que el job B debe esperar a que el job A termine correctamente antes de ejecutarse?

A) `after: [A]`
B) `needs: [A]`
C) `depends-on: A`
D) `requires: A`

> **Respuesta correcta: B)** — La clave `needs` establece dependencias entre jobs. El job B no se ejecutará hasta que todos los jobs listados en `needs` completen con éxito.

---

**Pregunta 10:** Un service container de PostgreSQL se define en un job. ¿Cómo se accede a él desde los steps del mismo job cuando el runner es ubuntu-latest?

A) Mediante la IP `127.0.0.1` y el puerto mapeado en `ports`
B) Mediante el nombre del servicio como hostname y el puerto del contenedor
C) Solo mediante variables de entorno inyectadas automáticamente
D) Mediante `localhost` y el puerto nativo si no se define `ports`

> **Respuesta correcta: B)** — En runners hosted, los service containers se comunican por red Docker usando el nombre del servicio como hostname y el puerto interno del contenedor.

---

**Pregunta 11:** En una matrix con `os: [ubuntu-latest, windows-latest]` y `node: [16, 18]`, ¿cuántos jobs se crean?

A) 2
B) 4
C) 6
D) Depende de `fail-fast`

> **Respuesta correcta: B)** — La matrix genera el producto cartesiano de sus dimensiones: 2 sistemas operativos × 2 versiones de Node = 4 jobs.

---

**Pregunta 12:** ¿Cuál de los siguientes contextos está disponible en `defaults` pero NO en `on`?

A) `github`
B) `env`
C) `secrets`
D) Ninguno de los anteriores; los contextos no están disponibles en `defaults`

> **Respuesta correcta: D)** — En `defaults` los contextos de expresión no están disponibles. Los contextos como `github`, `env` y `secrets` tienen disponibilidad limitada según la clave del workflow donde se usen.

---

**Pregunta 13:** ¿Qué comando de file command se usa para añadir una variable de entorno que persista entre steps del mismo job?

A) `echo "VAR=value" >> $GITHUB_OUTPUT`
B) `echo "VAR=value" >> $GITHUB_ENV`
C) `echo "::set-env name=VAR::value"`
D) `export VAR=value`

> **Respuesta correcta: B)** — Escribir en `$GITHUB_ENV` hace que la variable esté disponible en todos los steps posteriores del mismo job. El comando `::set-env` está obsoleto y desactivado por seguridad.

---

**Pregunta 14:** ¿Cuál es la función de `actions/cache` y qué clave determina si se produce un cache hit?

A) Sube artefactos al almacenamiento de Actions; la clave es el SHA del commit
B) Cachea dependencias entre ejecuciones; la clave es un string que define el identificador único del caché
C) Comparte artefactos entre jobs; la clave es el nombre del artefacto
D) Es equivalente a `actions/upload-artifact` con compresión

> **Respuesta correcta: B)** — `actions/cache` persiste directorios entre ejecuciones de workflow. La clave (`key`) determina si hay hit exacto; si no coincide, se busca en `restore-keys`.

---

**Pregunta 15:** ¿Qué diferencia hay entre `actions/upload-artifact` y `actions/cache`?

A) No hay diferencia funcional; ambos persisten ficheros entre ejecuciones
B) `upload-artifact` comparte ficheros entre jobs de la misma ejecución y los guarda como descargables; `cache` persiste ficheros entre ejecuciones distintas para acelerar builds
C) `cache` solo funciona en runners self-hosted
D) `upload-artifact` requiere permiso `actions: write`; `cache` no requiere permisos

> **Respuesta correcta: B)** — Los artefactos permiten compartir outputs entre jobs y descargarlos; el caché acelera ejecuciones futuras reutilizando dependencias entre runs.

---

**Pregunta 16:** ¿Qué URL tiene el badge de estado de un workflow llamado `CI` en el repositorio `org/repo`?

A) `https://github.com/org/repo/actions/badge.svg`
B) `https://github.com/org/repo/workflows/CI/badge.svg`
C) `https://github.com/org/repo/actions/workflows/CI.yml/badge.svg`
D) `https://img.shields.io/github/actions/org/repo/CI`

> **Respuesta correcta: C)** — El formato correcto es `https://github.com/{owner}/{repo}/actions/workflows/{workflow-file}.yml/badge.svg`. También se puede filtrar por rama con `?branch=main`.

---

**Pregunta 17:** Un environment con `required_reviewers` configurado hace que el job que lo usa:

A) Falle automáticamente si no hay reviewers asignados
B) Se pause esperando aprobación manual antes de ejecutar los steps del job
C) Envíe una notificación pero continúe sin esperar
D) Solo aplique las variables del environment, no las protecciones

> **Respuesta correcta: B)** — Cuando un job referencia un environment con revisores requeridos, GitHub pausa la ejecución en ese punto hasta que un revisor apruebe o rechace el deployment.

---

**Pregunta 18:** La variable predefinida `GITHUB_SHA` en un evento `pull_request` contiene:

A) El SHA del último commit de la rama base
B) El SHA del merge commit de prueba generado por GitHub
C) El SHA del último commit del PR
D) El SHA del commit que disparó el workflow, que puede ser un merge commit temporal

> **Respuesta correcta: D)** — En eventos `pull_request`, `GITHUB_SHA` es el SHA del merge commit temporal que GitHub crea para probar la fusión entre la rama del PR y la base.

---

## Ejercicios prácticos

### Ejercicio 1: Workflow con matrix, caché y artefactos

Se pide crear un workflow que ejecute tests de Node.js en tres versiones (16, 18, 20) sobre Ubuntu. El workflow debe cachear `node_modules`, correr los tests y subir el reporte de cobertura como artefacto solo desde la versión 20.

```yaml
name: Node.js Test Matrix

on:
  push:
    branches: ["main"]
  pull_request:

jobs:
  test:
    runs-on: ubuntu-latest
    strategy:
      matrix:
        node-version: [16, 18, 20]
      fail-fast: false

    steps:
      - name: Checkout código
        uses: actions/checkout@v4

      - name: Configurar Node.js ${{ matrix.node-version }}
        uses: actions/setup-node@v4
        with:
          node-version: ${{ matrix.node-version }}

      - name: Restaurar caché de dependencias
        uses: actions/cache@v4
        with:
          path: node_modules
          key: ${{ runner.os }}-node-${{ matrix.node-version }}-${{ hashFiles('package-lock.json') }}
          restore-keys: |
            ${{ runner.os }}-node-${{ matrix.node-version }}-

      - name: Instalar dependencias
        run: npm ci

      - name: Ejecutar tests con cobertura
        run: npm test -- --coverage

      - name: Subir reporte de cobertura
        if: matrix.node-version == 20
        uses: actions/upload-artifact@v4
        with:
          name: coverage-report
          path: coverage/
          retention-days: 7
```

La clave del caché incluye el hash de `package-lock.json` para invalidarlo cuando cambien las dependencias. La condición `if: matrix.node-version == 20` asegura que solo una instancia de la matrix sube el artefacto, evitando conflictos de nombres.

---

### Ejercicio 2: Workflow con environment protection y outputs entre jobs

Se pide crear un workflow que despliegue a producción con aprobación manual requerida, exponga el timestamp de despliegue como output del job y use secrets del environment.

```yaml
name: Deploy a Producción

on:
  workflow_dispatch:
    inputs:
      version:
        description: "Versión a desplegar (ej: v1.2.3)"
        required: true
        type: string

jobs:
  build:
    runs-on: ubuntu-latest
    outputs:
      image-tag: ${{ steps.tag.outputs.tag }}
    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Generar tag de imagen
        id: tag
        run: echo "tag=${{ github.event.inputs.version }}-${{ github.run_number }}" >> $GITHUB_OUTPUT

      - name: Build imagen
        run: echo "Construyendo imagen con tag ${{ steps.tag.outputs.tag }}"

  deploy-production:
    needs: build
    runs-on: ubuntu-latest
    environment: production
    outputs:
      deployed-at: ${{ steps.timestamp.outputs.ts }}

    steps:
      - name: Registrar timestamp
        id: timestamp
        run: echo "ts=$(date -u +%Y-%m-%dT%H:%M:%SZ)" >> $GITHUB_OUTPUT

      - name: Desplegar a producción
        env:
          DEPLOY_TOKEN: ${{ secrets.DEPLOY_TOKEN }}
          API_URL: ${{ vars.PRODUCTION_API_URL }}
        run: |
          echo "Desplegando ${{ needs.build.outputs.image-tag }} en $API_URL"

  notify:
    needs: deploy-production
    runs-on: ubuntu-latest
    if: always()
    steps:
      - name: Notificar resultado
        run: |
          echo "Estado: ${{ needs.deploy-production.result }}"
          echo "Desplegado a las: ${{ needs.deploy-production.outputs.deployed-at }}"
```

El job `deploy-production` referencia el environment `production`, que en GitHub debe tener configurados `required_reviewers`. La ejecución se pausará hasta recibir aprobación manual. El job `notify` usa `if: always()` para ejecutarse independientemente del resultado del despliegue.

---

### Ejercicio 3: Workflow con service container de PostgreSQL y condicionales

Se pide un workflow que levante PostgreSQL como service container, ejecute tests de integración, y solo corra los tests de rendimiento si el evento es push a main. El resumen de la ejecución debe escribirse siempre.

```yaml
name: Integration Tests

on:
  push:
    branches: ["main", "develop"]
  pull_request:

jobs:
  integration-tests:
    runs-on: ubuntu-latest

    services:
      postgres:
        image: postgres:15
        env:
          POSTGRES_USER: testuser
          POSTGRES_PASSWORD: testpass
          POSTGRES_DB: testdb
        ports:
          - 5432:5432
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

    env:
      DATABASE_URL: postgresql://testuser:testpass@localhost:5432/testdb

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Configurar Python
        uses: actions/setup-python@v5
        with:
          python-version: "3.11"

      - name: Instalar dependencias
        run: pip install -r requirements.txt

      - name: Ejecutar migraciones
        run: python manage.py migrate

      - name: Tests de integración
        run: pytest tests/integration/ -v

      - name: Tests de rendimiento
        if: github.event_name == 'push' && github.ref == 'refs/heads/main'
        run: pytest tests/performance/ -v --benchmark-only

      - name: Exportar resumen
        if: always()
        run: |
          echo "## Resultado de tests" >> $GITHUB_STEP_SUMMARY
          echo "- Evento: ${{ github.event_name }}" >> $GITHUB_STEP_SUMMARY
          echo "- Rama: ${{ github.ref_name }}" >> $GITHUB_STEP_SUMMARY
          echo "- Commit: ${{ github.sha }}" >> $GITHUB_STEP_SUMMARY
```

El service container `postgres` se accede mediante `localhost` porque se define el mapeo de puertos. La opción `--health-cmd pg_isready` garantiza que GitHub Actions espere a que PostgreSQL esté listo antes de ejecutar los steps. El step de rendimiento usa una expresión compuesta para ejecutarse solo en push a main, y el step de resumen escribe markdown en el panel de la ejecución usando `$GITHUB_STEP_SUMMARY`.

---

[anterior: 1.19 Environment protections](gha-d1-environment-protections.md) | [siguiente: 2.1 Interpretación de triggers y UI](gha-d2-triggers-ui.md)
