# 1.8 Dependencias entre jobs y propagación de outputs

[← 1.7 Condicionales y funciones de estado](gha-d1-condicionales.md) | [→ 1.9.1 Service containers](gha-d1-service-containers.md)

---

## El grafo de dependencias como estructura del pipeline

Un workflow de GitHub Actions no es una secuencia lineal de pasos: es un grafo dirigido acíclico (DAG) donde cada nodo es un job y cada arista representa una dependencia. Por defecto, todos los jobs de un workflow se ejecutan en paralelo a menos que se indique explícitamente lo contrario. La clave `needs` es el mecanismo que convierte ese conjunto de jobs independientes en un pipeline orquestado con orden, con propagación de datos y con manejo de fallos. Entender este grafo es entender cómo GitHub Actions decide qué ejecutar, cuándo ejecutarlo y qué hacer si algo falla en el camino.

---

## Diagrama ASCII del pipeline

El patrón más habitual en CI/CD combina fan-out (un job dispara varios en paralelo) con fan-in (varios jobs convergen en uno que los espera a todos):

```
          ┌─────────┐
          │  build  │  ← job raíz, sin needs
          └────┬────┘
               │  outputs: image_tag, artifact_path
       ┌───────┴───────┐
       ▼               ▼
  ┌─────────┐    ┌──────────┐
  │  test   │    │  lint    │   ← fan-out: corren en paralelo
  └────┬────┘    └────┬─────┘
       │              │
       └──────┬────────┘
              ▼
         ┌─────────┐
         │ deploy  │  ← fan-in: espera test Y lint
         └────┬────┘
              │
         ┌────┴─────┐
         │  notify  │  ← siempre corre (uses if: always())
         └──────────┘
```

---

## `needs` con un solo job

La forma más simple de dependencia declara un único job en `needs`. El job corriente no comenzará hasta que el job referenciado haya terminado con estado `success`. La sintaxis acepta tanto cadena simple como array de un elemento; en la práctica se usa la cadena directa cuando la dependencia es única.

```yaml
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - run: echo "compilando"

  test:
    runs-on: ubuntu-latest
    needs: build          # espera a build antes de arrancar
    steps:
      - run: echo "ejecutando tests"
```

El efecto inmediato es que GitHub Actions no encola `test` hasta que `build` reporte éxito. Si `build` falla, `test` se marca automáticamente como `skipped` sin ejecutar ninguno de sus steps.

---

## `needs` con array de jobs (fan-in)

Cuando un job debe esperar a varios jobs simultáneos, `needs` acepta un array. Todos los jobs del array deben completar con éxito para que el job dependiente arranque. Este patrón, llamado fan-in, es fundamental en pipelines donde se quiere paralelizar trabajo costoso (tests en distintas versiones, análisis estáticos, builds para distintas plataformas) y luego consolidar los resultados.

```yaml
jobs:
  test-unit:
    runs-on: ubuntu-latest
    steps:
      - run: echo "unit tests"

  test-integration:
    runs-on: ubuntu-latest
    steps:
      - run: echo "integration tests"

  deploy:
    runs-on: ubuntu-latest
    needs: [test-unit, test-integration]   # fan-in: espera ambos
    steps:
      - run: echo "desplegando solo si ambos tests pasaron"
```

GitHub Actions calcula el grafo completo al inicio del workflow y programa la ejecución maximizando el paralelismo: `test-unit` y `test-integration` arrancan al mismo tiempo, y `deploy` espera a que ambos terminen.

---

## Outputs del job: declarar y exponer

Un job puede exportar datos hacia jobs dependientes mediante dos mecanismos combinados. Primero, el step que genera el valor escribe en el archivo especial `$GITHUB_OUTPUT` usando la sintaxis `nombre=valor`. Segundo, el job declara en su clave `outputs` qué nombres expone y de qué step los toma.

```yaml
jobs:
  build:
    runs-on: ubuntu-latest
    outputs:
      image_tag: ${{ steps.tag.outputs.tag }}        # conecta step → job output
      artifact_path: ${{ steps.build.outputs.path }}
    steps:
      - name: Generate tag
        id: tag
        run: echo "tag=v1.0.${{ github.run_number }}" >> "$GITHUB_OUTPUT"

      - name: Build artifact
        id: build
        run: |
          mkdir -p dist
          echo "tag=dist/app.tar.gz" >> "$GITHUB_OUTPUT"
```

El mecanismo es deliberadamente explícito: no hay magia. El step produce el valor, el job lo re-expone con un nombre visible para el resto del workflow. Si el `id` del step o el nombre en `outputs` no coinciden exactamente, el valor llegará vacío a los jobs dependientes.

---

## Acceso a outputs: `needs.<job-id>.outputs.<name>`

Los jobs que declaran `needs` sobre otro job pueden leer sus outputs mediante la expresión `${{ needs.<job-id>.outputs.<name> }}`. Este acceso es seguro en tiempo de evaluación: GitHub Actions garantiza que los outputs ya están resueltos antes de que el job dependiente arranque.

```yaml
jobs:
  deploy:
    runs-on: ubuntu-latest
    needs: build
    steps:
      - name: Deploy image
        run: |
          echo "Desplegando imagen: ${{ needs.build.outputs.image_tag }}"
          echo "Artifact en: ${{ needs.build.outputs.artifact_path }}"
```

El acceso a outputs de un job que no está en `needs` no está disponible. La dependencia explícita en `needs` es el requisito previo para acceder a los datos; sin esa declaración, GitHub Actions no garantiza el orden ni la disponibilidad del valor.

---

## Comportamiento por defecto: job omitido si la dependencia falla

Cuando un job de la cadena `needs` termina con estado `failure` o `cancelled`, todos los jobs que dependen de él (directa o transitivamente) son marcados como `skipped` de forma automática. Este comportamiento protege el pipeline de continuar en un estado inconsistente: no tiene sentido desplegar si la compilación ha fallado. El job omitido aparece en la interfaz de GitHub con el estado gris de "skipped" y no consume minutos de runner.

```yaml
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - run: exit 1   # falla intencionalmente

  test:
    needs: build
    runs-on: ubuntu-latest
    steps:
      - run: echo "esto NUNCA se ejecuta si build falla"
  # test queda en estado: skipped
```

---

## Override con `if: always()` o `if: failure()`

El comportamiento por defecto de omisión se puede sobreescribir con una condición `if` en el job. La función de estado `always()` garantiza que el job se ejecute independientemente del resultado de sus dependencias. La función `failure()` limita la ejecución al caso en que alguna dependencia haya fallado, útil para notificaciones de error o limpieza de recursos.

```yaml
jobs:
  notify:
    runs-on: ubuntu-latest
    needs: [test, deploy]
    if: always()   # corre siempre, incluso si test o deploy fallaron
    steps:
      - run: echo "notificando resultado del pipeline"

  cleanup:
    runs-on: ubuntu-latest
    needs: deploy
    if: failure()  # solo corre si deploy (o sus dependencias) fallaron
    steps:
      - run: echo "limpiando recursos por fallo en deploy"
```

Sin `if: always()`, un job de notificación situado al final del pipeline nunca se ejecutaría cuando ocurre un fallo, que es precisamente cuando más se necesita. Es uno de los patrones más importantes para pipelines robustos.

---

## Propagación de resultado via `needs.<job-id>.result`

Además de los outputs con datos, cada job expone su resultado final a través de `needs.<job-id>.result`. Los valores posibles son `success`, `failure`, `cancelled` y `skipped`. Este campo permite tomar decisiones condicionales basadas en lo que ocurrió en las dependencias, siendo especialmente útil en jobs que usan `if: always()` y necesitan actuar de forma diferente según el resultado.

```yaml
jobs:
  notify:
    runs-on: ubuntu-latest
    needs: [build, test, deploy]
    if: always()
    steps:
      - name: Notificar éxito
        if: ${{ needs.deploy.result == 'success' }}
        run: echo "Pipeline completado con éxito"

      - name: Notificar fallo
        if: ${{ needs.build.result == 'failure' || needs.test.result == 'failure' }}
        run: echo "Pipeline falló en build o test"

      - name: Mostrar todos los resultados
        run: |
          echo "build: ${{ needs.build.result }}"
          echo "test:  ${{ needs.test.result }}"
          echo "deploy: ${{ needs.deploy.result }}"
```

---

## Ejemplo central: pipeline build → test → deploy + notify

Este ejemplo integra todos los conceptos: dependencias simples y en array, outputs con propagación, comportamiento por defecto y override con `always()`.

```yaml
name: Pipeline CI/CD completo

on:
  push:
    branches: [main]

jobs:
  build:
    runs-on: ubuntu-latest
    outputs:
      image_tag: ${{ steps.tag.outputs.tag }}
      artifact_name: ${{ steps.artifact.outputs.name }}
    steps:
      - uses: actions/checkout@v4

      - name: Generar tag de imagen
        id: tag
        run: echo "tag=app-${{ github.sha }}" >> "$GITHUB_OUTPUT"

      - name: Compilar y empaquetar
        id: artifact
        run: |
          mkdir -p dist
          echo "build content" > dist/app.jar
          echo "name=dist/app.jar" >> "$GITHUB_OUTPUT"

      - name: Subir artifact
        uses: actions/upload-artifact@v4
        with:
          name: app-artifact
          path: dist/

  test:
    runs-on: ubuntu-latest
    needs: build                              # espera solo a build
    steps:
      - uses: actions/checkout@v4

      - name: Descargar artifact
        uses: actions/download-artifact@v4
        with:
          name: app-artifact
          path: dist/

      - name: Ejecutar tests
        run: |
          echo "Testeando imagen: ${{ needs.build.outputs.image_tag }}"
          echo "Tests pasados"

  deploy:
    runs-on: ubuntu-latest
    needs: [build, test]                      # fan-in: espera build Y test
    environment: production
    steps:
      - name: Desplegar
        run: |
          echo "Desplegando ${{ needs.build.outputs.image_tag }}"
          echo "Artifact: ${{ needs.build.outputs.artifact_name }}"

  notify:
    runs-on: ubuntu-latest
    needs: [build, test, deploy]              # depende de toda la cadena
    if: always()                              # corre aunque alguno haya fallado
    steps:
      - name: Resultado del pipeline
        run: |
          echo "=== Resultados ==="
          echo "build:  ${{ needs.build.result }}"
          echo "test:   ${{ needs.test.result }}"
          echo "deploy: ${{ needs.deploy.result }}"

      - name: Notificar fallo
        if: ${{ contains(needs.*.result, 'failure') }}
        run: echo "ALERTA: algún job ha fallado"
```

---

## Tabla de elementos clave

| Elemento | Sintaxis | Descripción |
|---|---|---|
| Dependencia simple | `needs: build` | Espera un único job |
| Dependencia múltiple | `needs: [build, test]` | Fan-in: espera todos |
| Escribir output de step | `echo "k=v" >> "$GITHUB_OUTPUT"` | Expone valor desde step |
| Declarar output de job | `outputs: nombre: ${{ steps.id.outputs.k }}` | Conecta step con job output |
| Leer output de job | `${{ needs.job-id.outputs.nombre }}` | Accede al valor en job dependiente |
| Resultado del job | `${{ needs.job-id.result }}` | `success`, `failure`, `cancelled`, `skipped` |
| Ejecutar siempre | `if: always()` | Override: ignora resultado de dependencias |
| Ejecutar solo en fallo | `if: failure()` | Solo si alguna dependencia falló |
| Verificar cualquier fallo | `contains(needs.*.result, 'failure')` | Comprueba todos los resultados a la vez |

---

## Buenas y malas practicas

**Dependencias y orden**

Mala practica: omitir `needs` confiando en que los jobs se ejecuten en el orden escrito. Los jobs sin `needs` corren en paralelo sin garantía de orden. El resultado puede ser no determinista y difícil de reproducir.

Buena practica: declarar siempre las dependencias reales con `needs`. Si `deploy` necesita que `test` pase, lo declara explícitamente. El orden queda documentado en el grafo y GitHub Actions lo respeta.

---

**Propagacion de datos**

Mala practica: usar variables de entorno globales o artifacts como único mecanismo para pasar datos pequeños (como un tag de imagen o un número de versión) entre jobs. Requiere subir y descargar artifacts innecesariamente.

Buena practica: usar `$GITHUB_OUTPUT` y `job.outputs` para datos pequeños (strings, IDs, flags). Reservar los artifacts para ficheros binarios o resultados voluminosos que realmente necesitan persistencia entre jobs.

---

**Jobs de notificacion**

Mala practica: poner el job de notificación o reporte al final de la cadena sin `if: always()`. Cuando el pipeline falla, el job de notificación queda en `skipped` y el equipo no recibe alertas cuando más las necesita.

Buena practica: añadir `if: always()` a cualquier job de notificación, limpieza o reporte final. Combinarlo con `needs.*.result` o condiciones específicas para diferenciar el mensaje según el resultado.

---

**Acceso a outputs de jobs no declarados en needs**

Mala practica: intentar acceder a `needs.otro-job.outputs.valor` cuando `otro-job` no está en el `needs` del job corriente. El valor llega vacío sin error explícito, introduciendo bugs silenciosos.

Buena practica: si un job necesita un output, debe declarar la dependencia en `needs`. La regla es simple: no se puede leer lo que no se ha declarado como dependencia.

---

## Preguntas tipo GH-200

**Pregunta 1.** Un workflow tiene los jobs `build`, `test` y `deploy`. El job `deploy` declara `needs: [build, test]`. Si `test` falla, ¿cuál es el estado final de `deploy`?

a) `failure`, porque una dependencia falló  
b) `skipped`, porque el comportamiento por defecto omite jobs con dependencias fallidas  
c) `cancelled`, porque GitHub Actions cancela el workflow  
d) `success`, si `build` tuvo éxito  

Respuesta correcta: **b**. Por defecto, un job cuyos `needs` incluyen un job fallido queda en estado `skipped`.

---

**Pregunta 2.** Un step escribe `echo "version=1.2.3" >> "$GITHUB_OUTPUT"` y tiene `id: versioning`. El job lo expone con `outputs: app_version: ${{ steps.versioning.outputs.version }}`. ¿Cómo accede un job dependiente llamado `package` a este valor?

a) `${{ jobs.build.outputs.version }}`  
b) `${{ needs.build.outputs.app_version }}`  
c) `${{ steps.versioning.outputs.version }}`  
d) `${{ env.version }}`  

Respuesta correcta: **b**. El acceso usa `needs.<job-id>.outputs.<nombre-declarado-en-outputs-del-job>`.

---

**Pregunta 3.** Un job `cleanup` debe ejecutarse solo cuando `deploy` falla. ¿Qué condición es correcta?

a) `if: needs.deploy.result == 'failure'`  
b) `if: failure()` con `needs: deploy`  
c) `if: needs.deploy.status == 'error'`  
d) `if: cancelled()`  

Respuesta correcta: **b**. La función `failure()` evalúa si algún job en `needs` ha fallado. La opción a) es sintácticamente válida pero solo si el job usa `if: always()` además; sin `always()`, un job con dependencia fallida no evalúa la condición `if` porque ya está en estado `skipped`.

---

## Ejercicio practico

Diseña un workflow para un proyecto Node.js con los siguientes requisitos:

1. Job `install`: instala dependencias y guarda el hash del `package-lock.json` como output.
2. Jobs `test-unit` y `test-e2e`: corren en paralelo tras `install`, usando el hash para decidir si regenerar la cache.
3. Job `build`: corre tras `install` en paralelo con los tests, genera un artifact y expone el nombre del artifact como output.
4. Job `publish`: corre solo si `test-unit`, `test-e2e` y `build` han tenido éxito. Usa los outputs de `build` para publicar.
5. Job `report`: corre siempre al final, imprime el resultado de todos los jobs anteriores.

Pistas clave: `needs` en array para fan-in en `publish`, `if: always()` en `report`, `$GITHUB_OUTPUT` en `install` y `build`, acceso via `needs.<id>.outputs.<name>` en `publish` y `report`.

---

[← 1.7 Condicionales y funciones de estado](gha-d1-condicionales.md) | [→ 1.9.1 Service containers](gha-d1-service-containers.md)
