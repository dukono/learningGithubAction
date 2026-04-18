# 1.15 Artefactos (upload, download, retención)

[← 1.14 Caché de dependencias](gha-d1-cache.md) | [1.16 File commands del workflow →](gha-d1-file-commands.md)

---

## ¿Qué son los artefactos?

Los artefactos son archivos o directorios generados durante la ejecución de un workflow que GitHub Actions persiste fuera del ciclo de vida efímero del runner. A diferencia de la caché, que está diseñada para acelerar ejecuciones futuras, los artefactos sirven para dos propósitos principales: compartir archivos entre jobs del mismo workflow y preservar resultados (binarios compilados, reportes, logs) para consulta posterior desde la UI o la API REST. Cada job corre en un runner limpio, por lo que sin artefactos los archivos generados en un job desaparecen antes de que el siguiente job empiece.

```
┌─────────────────────────────────────────────────────────────┐
│                        WORKFLOW RUN                         │
│                                                             │
│  ┌──────────────┐    sube artefacto    ┌────────────────┐  │
│  │  job: build  │ ──────────────────►  │  GitHub Store  │  │
│  │              │                      │  "my-app-dist" │  │
│  │ ./dist/*.js  │                      └───────┬────────┘  │
│  └──────────────┘                              │           │
│                                       descarga artefacto   │
│                                                │           │
│                                       ┌────────▼────────┐  │
│                                       │  job: deploy    │  │
│                                       │  needs: build   │  │
│                                       └─────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

---

## `actions/upload-artifact`

La action `actions/upload-artifact` sube uno o varios archivos al almacenamiento temporal de GitHub asociado a la ejecución del workflow. El parámetro `name` define el nombre del artefacto con el que quedará registrado; si dos pasos suben artefactos con el mismo nombre en la misma ejecución, los archivos se combinan (v4) o el segundo sobreescribe al primero (v3). El parámetro `path` acepta rutas individuales, directorios completos y patrones glob como `dist/**/*.js` o una lista multilínea de rutas; la ruta base del zip resultante excluye la parte común de los paths especificados. El parámetro `retention-days` permite sobrescribir el período de retención predeterminado del repositorio para ese artefacto concreto. El parámetro `if-no-files-found` controla qué ocurre cuando el path no coincide con ningún archivo.

```yaml
- name: Subir artefacto de build
  uses: actions/upload-artifact@v4
  with:
    name: my-app-dist
    path: |
      dist/
      package.json
    retention-days: 7
    if-no-files-found: error
```

---

## `actions/download-artifact`

La action `actions/download-artifact` descarga artefactos previamente subidos al almacenamiento de GitHub. El parámetro `name` indica qué artefacto recuperar; si se omite, se descargan todos los artefactos de la ejecución actual en subdirectorios con el nombre de cada uno. El parámetro `path` especifica el directorio local de destino en el runner; si no se indica, los archivos se depositan en el directorio de trabajo actual. El parámetro `run-id` permite descargar artefactos de una ejecución diferente a la actual, lo que es útil para flujos de promoción entre ambientes donde un job de deploy de producción recupera el binario exacto validado en staging; para usar `run-id` también se requiere el parámetro `github-token` con los permisos adecuados.

```yaml
- name: Descargar artefacto de build
  uses: actions/download-artifact@v4
  with:
    name: my-app-dist
    path: ./downloaded-dist
```

---

## Compartir artefactos entre jobs del mismo workflow

Para que un job acceda a los archivos producidos por otro job dentro del mismo workflow, se combina `upload-artifact` en el job productor con `download-artifact` en el job consumidor, y se declara la dependencia mediante `needs`. Sin `needs`, los dos jobs corren en paralelo y el job consumidor intentaría descargar un artefacto que todavía no existe, lo que resulta en error. La dependencia `needs` garantiza el orden correcto de ejecución: primero termina el job de build con su upload y luego arranca el job de deploy que hace el download. Este patrón es la forma idiomática de pasar binarios compilados, reportes de cobertura o cualquier archivo entre stages de un pipeline.

---

## Retención de artefactos

GitHub almacena los artefactos durante un período de retención configurable a tres niveles: organización, repositorio y artefacto individual. El valor predeterminado es **90 días** para repositorios con plan gratuito y de pago, aunque los administradores de organización pueden cambiarlo. El mínimo absoluto es **1 día** y el máximo es **400 días**; valores fuera de ese rango son rechazados por la API con error de validación. El parámetro `retention-days` en `upload-artifact` solo puede reducir el período respecto al configurado en el repositorio, no aumentarlo más allá del límite institucional. Una vez expirado el período, los artefactos se eliminan automáticamente y no son recuperables. Para artefactos de releases o builds de producción que deben conservarse indefinidamente conviene adjuntarlos como assets a un GitHub Release en lugar de depender de la retención de workflow.

---

## `if-no-files-found`

El parámetro `if-no-files-found` de `upload-artifact` determina el comportamiento cuando el `path` especificado no resuelve a ningún archivo. Con el valor `warn` (predeterminado) el step termina con éxito pero emite una advertencia visible en los logs; es útil cuando el artefacto es opcional, por ejemplo reportes de cobertura que solo se generan si los tests pasan. Con el valor `error` el step falla, lo que provoca el fallo del job completo; es la opción correcta cuando el artefacto es indispensable para jobs posteriores o para la auditoría del build. Con el valor `ignore` el step termina silenciosamente sin advertencia ni error; se reserva para artefactos completamente opcionales en pipelines donde cualquier ruido en los logs es problemático. La elección incorrecta del valor puede ocultar fallos de build o generar falsos negativos difíciles de diagnosticar.

---

## Ejemplo completo: build y deploy con artefactos

```yaml
name: Build y Deploy

on:
  push:
    branches: [main]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Instalar dependencias
        run: npm ci

      - name: Compilar aplicación
        run: npm run build

      - name: Subir artefacto de distribución
        uses: actions/upload-artifact@v4
        with:
          name: app-dist-${{ github.sha }}
          path: dist/
          retention-days: 30
          if-no-files-found: error

  deploy:
    runs-on: ubuntu-latest
    needs: build                        # garantiza orden y disponibilidad del artefacto
    environment: production
    steps:
      - name: Descargar artefacto de distribución
        uses: actions/download-artifact@v4
        with:
          name: app-dist-${{ github.sha }}
          path: ./dist

      - name: Desplegar en servidor
        run: |
          echo "Desplegando archivos desde ./dist"
          ls -la ./dist
          # rsync, scp, az cli, etc.
```

---

## Referencia de parámetros

| Action | Parámetro | Tipo | Requerido | Descripción |
|---|---|---|---|---|
| upload-artifact | `name` | string | si | Nombre del artefacto en el store de GitHub |
| upload-artifact | `path` | string/list | si | Archivos o directorios a subir; admite glob |
| upload-artifact | `retention-days` | number | no | Días de retención (1–400); anula el default del repo |
| upload-artifact | `if-no-files-found` | enum | no | `warn` (default), `error`, `ignore` |
| upload-artifact | `compression-level` | number | no | Nivel de compresión 0–9 (default 6) |
| download-artifact | `name` | string | no | Nombre del artefacto; si se omite, descarga todos |
| download-artifact | `path` | string | no | Directorio destino en el runner |
| download-artifact | `run-id` | string | no | ID de otra ejecución para descargar sus artefactos |
| download-artifact | `github-token` | string | no | Token necesario cuando se usa `run-id` externo |

---

## Buenas y malas prácticas

**Nombrar artefactos con el SHA del commit**
- Correcto: `name: app-dist-${{ github.sha }}` garantiza unicidad y trazabilidad; cada ejecución produce un artefacto identificable.
- Incorrecto: `name: dist` en un workflow con matrix produce colisiones de nombre entre las distintas combinaciones de la matriz.

**Usar `if-no-files-found: error` en artefactos críticos**
- Correcto: Si el artefacto es prerequisito para el job de deploy, `error` detiene el pipeline antes de que el deploy intente trabajar con archivos inexistentes.
- Incorrecto: Usar `warn` o `ignore` en artefactos críticos enmascara fallos de compilación; el job de deploy arranca y falla con mensajes de error confusos.

**Ajustar `retention-days` según el tipo de artefacto**
- Correcto: Logs de debug y reportes de PR con `retention-days: 3`; binarios de release con `retention-days: 90`; esto reduce el consumo de almacenamiento facturado.
- Incorrecto: Usar el default de 90 días para todos los artefactos, incluyendo artefactos de builds de feature branches que se merging en horas.

**Usar `needs` explícito para garantizar el orden**
- Correcto: Declarar `needs: build` en el job consumidor hace explícita la dependencia y evita condiciones de carrera.
- Incorrecto: Asumir que el orden de declaración de jobs en el YAML implica orden de ejecución; GitHub Actions ejecuta jobs en paralelo por defecto.

---

## Artefactos en workflows con matrix

Cuando un workflow usa una estrategia `matrix`, cada combinación de la matriz corre en un job independiente y genera su propio conjunto de archivos. Si todos los jobs de la matriz intentan subir un artefacto con el mismo `name`, en `actions/upload-artifact@v4` los archivos se combinan en un único artefacto (comportamiento merge), mientras que en v3 el segundo upload sobreescribe al primero. La solución recomendada es incluir la variable de la matriz en el nombre del artefacto para garantizar unicidad: `name: test-results-${{ matrix.os }}-${{ matrix.node-version }}`. Cuando el job de consolidación necesita descargar todos los artefactos de la matriz, puede omitir el parámetro `name` en `download-artifact` para obtenerlos todos en subdirectorios separados.

```yaml
jobs:
  test:
    strategy:
      matrix:
        os: [ubuntu-latest, windows-latest]
        node: [18, 20]
    runs-on: ${{ matrix.os }}
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: ${{ matrix.node }}
      - run: npm ci && npm test
      - uses: actions/upload-artifact@v4
        if: always()                            # sube el reporte aunque los tests fallen
        with:
          name: test-results-${{ matrix.os }}-node${{ matrix.node }}
          path: test-results/
          retention-days: 7
          if-no-files-found: warn

  report:
    needs: test
    runs-on: ubuntu-latest
    steps:
      - uses: actions/download-artifact@v4      # descarga todos los artefactos
        with:
          path: all-results/                    # cada artefacto en su subdirectorio
      - name: Listar todos los reportes
        run: find all-results/ -name "*.xml" | sort
```

---

## Consideraciones de almacenamiento y coste

GitHub factura el almacenamiento de artefactos que supere la cuota gratuita incluida en el plan. La cuota gratuita es de 500 MB para el plan Free y de 2 GB para el plan Team; el plan Enterprise tiene cuota configurable. El almacenamiento se mide como el promedio diario de todos los artefactos activos del repositorio. Para mantener el consumo bajo control conviene: fijar `retention-days` bajos en artefactos de ramas de features (1-3 días), evitar subir `node_modules` o dependencias que pertenecen a la caché, usar `compression-level: 9` para artefactos de texto o JSON, y limpiar artefactos obsoletos con la API REST o `gh run delete`. Los artefactos de releases de larga duración conviene almacenarlos como assets de GitHub Release o en un registro de artefactos externo (Artifactory, S3, Azure Blob) para no depender de la retención de workflow.

---

## Artefactos vs caché: cuándo usar cada uno

Aunque tanto artefactos como caché persisten archivos entre ejecuciones, tienen propósitos distintos. La caché (`actions/cache`) está optimizada para acelerar builds futuros almacenando dependencias descargadas; se comparte entre múltiples ejecuciones del mismo workflow y su gestión es automática (evicción por LRU y límite de 10 GB). Los artefactos (`upload-artifact` / `download-artifact`) están diseñados para compartir archivos entre jobs de la misma ejecución o para preservar resultados auditables; no se reutilizan automáticamente en ejecuciones futuras y están sujetos al período de retención. Una regla práctica: si el archivo es un producto del build (binario, reporte, cobertura), usa artefactos; si el archivo es un insumo del build (dependencias, compilaciones intermedias reutilizables), usa caché.

| Criterio | Artefactos | Caché |
|---|---|---|
| Propósito principal | Compartir entre jobs / preservar resultados | Acelerar builds futuros |
| Alcance temporal | Una ejecución (+ retención) | Múltiples ejecuciones |
| Tamaño máximo | Sin límite explícito por artefacto | 10 GB por repositorio |
| Retención | 1–400 días (configurable) | 7 días sin uso |
| Casos de uso | Binarios, reportes, logs | node_modules, .m2, pip cache |

---

## Temas relacionados

- **Artifact attestations y SLSA provenance**: la firma criptográfica de artefactos para cadena de suministro segura se trata en D5 → `gha-artifact-attestations-uso.md`.
- **Consumo via UI y API REST**: cómo listar, descargar y eliminar artefactos desde la interfaz web y mediante `gh api` o la REST API se documenta en D2 → `gha-d2-artefactos.md`.

---

## Patrones avanzados

### Descargar artefactos de otra ejecución

En pipelines de promoción entre ambientes es habitual que el job de deploy a producción recupere el binario exacto que fue validado en staging, sin recompilar. Para esto se usa `run-id` en `download-artifact`:

```yaml
jobs:
  deploy-prod:
    runs-on: ubuntu-latest
    environment: production
    steps:
      - name: Descargar binario validado en staging
        uses: actions/download-artifact@v4
        with:
          name: app-dist-${{ github.sha }}
          path: ./dist
          run-id: ${{ inputs.staging_run_id }}   # run-id pasado como input
          github-token: ${{ secrets.GITHUB_TOKEN }}

      - name: Desplegar en producción
        run: ./scripts/deploy.sh ./dist
```

El token necesita el permiso `actions: read` sobre el repositorio. Si los workflows pertenecen a repositorios distintos se requiere un PAT con acceso al repositorio origen.

### Subir artefactos solo ante fallos

Un patrón habitual en CI es subir logs de diagnóstico únicamente cuando el job falla, para no consumir almacenamiento en cada ejecución exitosa:

```yaml
- name: Compilar
  id: build
  run: npm run build

- name: Subir logs de error
  if: failure()
  uses: actions/upload-artifact@v4
  with:
    name: build-logs-${{ github.run_id }}
    path: |
      *.log
      tmp/build-error.txt
    retention-days: 3
    if-no-files-found: ignore    # los logs pueden no existir si el fallo fue anterior
```

La combinación `if: failure()` + `if-no-files-found: ignore` es robusta: el step solo corre cuando hay fallo y no protesta si los archivos de log no llegaron a crearse.

### Artefactos con nombres dinámicos para pull requests

En workflows de PR es útil incluir el número de PR en el nombre del artefacto para facilitar la identificación en la UI:

```yaml
- uses: actions/upload-artifact@v4
  with:
    name: pr-${{ github.event.pull_request.number }}-coverage
    path: coverage/
    retention-days: 5
```

---

## Verificación GH-200

**Pregunta 1.** Un workflow tiene dos jobs: `test` genera un reporte HTML y `notify` debe adjuntarlo a un comentario de PR. El `path` de upload apunta a `coverage/report.html` pero los tests fallan antes de generar el archivo. ¿Qué valor de `if-no-files-found` es apropiado si quieres que el job `test` marque el workflow como fallido?

> `error`. Con este valor el step de upload falla inmediatamente si no encuentra archivos, lo que propaga el fallo al job y al workflow completo, haciendo visible el problema.

**Pregunta 2.** Tienes un artefacto generado en la ejecución `run-id: 9876543210` de otro workflow. ¿Qué parámetros adicionales necesitas en `actions/download-artifact@v4` para descargarlo?

> Los parámetros `run-id: 9876543210` y `github-token: ${{ secrets.GITHUB_TOKEN }}` (con el permiso `actions: read`).

**Pregunta 3.** ¿Cuál es el período de retención máximo absoluto que se puede especificar en `retention-days`?

> 400 días.

**Ejercicio práctico.** Escribe un workflow con tres jobs: `lint` (sube un reporte `lint-report.txt`), `test` (sube `coverage/`), y `summary` (descarga ambos artefactos y los lista). Asegúrate de que `summary` espere a que `lint` y `test` terminen, y configura los artefactos de lint con retención de 3 días y los de coverage con retención de 14 días.

---

[← 1.14 Caché de dependencias](gha-d1-cache.md) | [1.16 File commands del workflow →](gha-d1-file-commands.md)
