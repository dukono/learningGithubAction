# 2.2 Lectura e interpretación de logs

← [2.1 Interpretación de triggers y UI de ejecución](gha-d2-triggers-ui.md) | → [2.3.1 Diagnóstico de comportamiento](gha-d2-diagnostico-comportamiento.md)

---

## Introducción

Cuando un workflow falla, la primera pregunta es siempre la misma: ¿qué salió mal y en qué punto? GitHub Actions registra cada ejecución con un nivel de detalle elevado, pero ese volumen de información solo es útil si sabes dónde mirar. Este fichero enseña a navegar la interfaz de logs, a entender su estructura, a filtrar la información relevante y a interpretar los resúmenes que los propios workflows pueden generar. Dominar los logs es una habilidad central en el día a día con GitHub Actions y ocupa un peso notable en el examen GH-200.

> [EXAMEN] La certificación GH-200 incluye preguntas sobre la retención de logs, la descarga de archivos de log, la creación de log groups y el uso de GITHUB_STEP_SUMMARY. Es frecuente que estas preguntas se presenten como escenarios de diagnóstico.

---

## A2.1 Estructura de la vista de ejecución en la UI

La interfaz de GitHub organiza cada ejecución de workflow en tres niveles jerárquicos. El nivel superior es el **workflow run**, que representa la ejecución completa desencadenada por un evento. Dentro de un workflow run hay uno o varios **jobs**, cada uno representado con su nombre, estado (éxito, fallo, cancelado, omitido) y duración. Dentro de cada job hay una lista ordenada de **steps**, que son las unidades mínimas de ejecución.

Esta jerarquía importa porque el fallo se propaga hacia arriba: si un step falla, el job falla; si un job falla, el workflow run falla (salvo que se haya configurado `continue-on-error` o `if` para manejar el error). Cuando llegas a la página de un workflow run, el sistema te lleva directamente al job que falló y resalta visualmente el step problemático. Sin embargo, entender el contexto completo requiere revisar los steps anteriores al fallo.

```
Workflow Run
├── Job: build
│   ├── Step: Checkout
│   ├── Step: Set up Node.js
│   ├── Step: Install dependencies   ← fallo aquí
│   └── Step: Run tests              ← no ejecutado (skipped)
└── Job: deploy
    └── (no ejecutado, depende de build)
```

> [CONCEPTO] La vista de un job muestra los steps en orden de ejecución. Los steps no ejecutados por un fallo anterior aparecen en gris con el estado "skipped". Los steps que se ejecutaron antes del fallo aparecen en verde o rojo según su resultado individual.

---

## A2.2 Expandir y colapsar pasos en los logs de la UI

Cada step en la vista de job aparece inicialmente en un estado colapsado que muestra solo el nombre del step, su estado (icono de check verde, X roja, círculo amarillo) y la duración. Para ver el contenido completo del log de un step, hay que hacer clic sobre él para expandirlo.

Cuando un step falla, GitHub lo expande automáticamente y posiciona la vista en la primera línea de error detectada. Esto es conveniente para fallos directos, pero puede ocultar contexto relevante que aparece en steps anteriores. Un error de compilación, por ejemplo, puede aparecer como fallo en el step "Run tests" pero la causa real puede estar en el step "Install dependencies" si una dependencia no se instaló correctamente.

El comportamiento de expansión automática solo afecta al step que registra el código de salida distinto de cero. Los steps que producen advertencias (warnings) pero terminan con código 0 no se expanden automáticamente, aunque sus logs pueden contener información crítica para el diagnóstico.

> [ADVERTENCIA] No confundas el step que falla con el step que causa el fallo. La práctica correcta de diagnóstico es revisar todos los steps anteriores al fallo, no solo el step marcado en rojo.

---

## A2.3 Filtrado de logs por texto (búsqueda en la UI)

GitHub Actions ofrece una función de búsqueda de texto dentro de los logs de un job. Esta función se activa haciendo clic en el campo de búsqueda que aparece en la parte superior de la vista de logs del job. La búsqueda es sensible a mayúsculas y minúsculas y filtra las líneas de log visibles en tiempo real mientras escribes.

El filtrado muestra solo las líneas que contienen el texto buscado, colapsando automáticamente los steps que no tienen coincidencias y expandiendo los que sí las tienen. Esto es especialmente útil cuando el log de un job tiene cientos o miles de líneas y necesitas localizar una cadena específica como un mensaje de error, un nombre de archivo o una URL.

La búsqueda tiene una limitación importante: opera sobre el contenido visible en la UI, que puede estar truncado para logs muy largos. Si el log supera cierto tamaño, la UI muestra un aviso indicando que el contenido está truncado y recomienda descargar el archivo completo para ver todos los datos. En ese caso, la búsqueda en la UI solo cubre la porción visible.

> [CONCEPTO] La búsqueda en la UI es suficiente para la mayoría de los casos. Para logs muy voluminosos (builds con salida extensa, suites de tests con miles de casos), la descarga del archivo .zip y la búsqueda local es más fiable.

---

## A2.4 Timestamps y duración de cada paso en los logs

Cada línea de log en GitHub Actions puede mostrar el timestamp exacto en que fue emitida. Los timestamps se activan haciendo clic en el icono de reloj que aparece en la esquina superior derecha de la vista de logs de un job. Una vez activados, cada línea muestra la fecha y hora UTC en formato ISO 8601.

Los timestamps son útiles en dos escenarios principales. El primero es identificar pasos lentos: si un step tarda mucho más de lo esperado, los timestamps permiten ver exactamente cuánto tiempo transcurrió entre el inicio y el fin de una operación. El segundo es correlacionar eventos externos: si un servicio externo tuvo un incidente a las 14:32 UTC y el workflow falló a las 14:33 UTC, los timestamps confirman la relación causal.

La duración de cada step aparece directamente en la cabecera del step colapsado, sin necesidad de activar los timestamps. Esta duración se muestra en segundos o en minutos y segundos dependiendo de la longitud. La duración total del job aparece en la vista del workflow run junto al nombre del job.

> [EXAMEN] Los timestamps en los logs de GitHub Actions están siempre en UTC. Si el examen presenta un escenario con zonas horarias, recuerda que la UI no convierte los timestamps a la zona horaria local del usuario.

---

## A2.5 Log groups: creación con `::group::` / `::endgroup::` y visualización en la UI

Los log groups son una funcionalidad que permite agrupar visualmente múltiples líneas de log bajo una cabecera colapsable en la UI. Se crean mediante comandos de workflow especiales escritos en la salida estándar del step: `::group::<nombre>` para abrir el grupo y `::endgroup::` para cerrarlo. Todo lo que se imprima entre esas dos instrucciones aparece agrupado bajo el nombre especificado.

La utilidad principal de los log groups es organizar la salida de steps con mucha información. Un step que instala dependencias, ejecuta linters, genera reportes y limpia archivos temporales puede producir centenares de líneas de output. Sin grupos, esa salida es difícil de navegar. Con grupos, cada fase aparece como un elemento colapsable con un nombre descriptivo, y el lector puede expandir solo la parte que le interesa.

En la UI, los log groups aparecen con un triángulo de expansión junto al nombre del grupo. Por defecto aparecen colapsados si el step terminó con éxito. Si el step falló, GitHub expande automáticamente el grupo donde ocurrió el error, igual que hace con los steps.

> [CONCEPTO] Los log groups son puramente visuales: no afectan a la ejecución, no agrupan errores de forma especial y no cambian los códigos de salida. Son una herramienta de presentación para hacer los logs más legibles.

---

## A2.6 Descarga de logs completos (archivo .zip) desde la UI

GitHub Actions permite descargar todos los logs de un workflow run como un archivo .zip desde la página del workflow run. El botón de descarga se encuentra en el menú de opciones (tres puntos) de la vista del workflow run, bajo la opción "Download log archive". El archivo descargado contiene un fichero de texto plano por cada job del workflow, con todos los steps y su output completo.

La descarga de logs es necesaria en varios casos. El primero es cuando el log es demasiado grande para mostrarse completo en la UI: GitHub trunca la visualización en pantalla pero el archivo descargado contiene el log íntegro. El segundo es cuando necesitas compartir los logs con alguien que no tiene acceso al repositorio. El tercero es cuando quieres hacer búsquedas complejas con herramientas locales como `grep`, `awk` o editores de texto con soporte de regex avanzado.

El archivo .zip también contiene metadatos adicionales como el identificador del runner, la versión de las acciones utilizadas y los tiempos exactos de inicio y fin de cada step. Esta información no siempre es visible directamente en la UI pero está disponible en los archivos descargados.

> [ADVERTENCIA] La descarga de logs solo está disponible mientras los logs no hayan expirado. Una vez superado el periodo de retención, el botón de descarga desaparece y los logs no son recuperables.

---

## A2.7 Retención de logs: política por defecto y configuración personalizada

Los logs de GitHub Actions no se conservan indefinidamente. La política por defecto establece una retención de **90 días** para los logs de workflow en repositorios con plan gratuito y en repositorios de organizaciones. Pasados esos 90 días, los logs se eliminan automáticamente y no pueden recuperarse.

Este periodo de retención puede modificarse a nivel de repositorio o de organización desde los ajustes correspondientes. En un repositorio, la configuración se encuentra en Settings → Actions → General → Artifact and log retention. El valor mínimo es 1 día y el máximo es 400 días para planes de pago. En repositorios públicos gratuitos el máximo también es 90 días.

Es importante distinguir entre la retención de logs y la retención de artefactos. Aunque comparten la misma sección de configuración, los artefactos tienen su propia política de retención independiente. Un workflow puede configurar una retención específica para sus artefactos usando el parámetro `retention-days` en la acción `actions/upload-artifact`, pero los logs del workflow en sí siguen la política del repositorio.

> [EXAMEN] La retención por defecto de los logs es 90 días. Este dato aparece frecuentemente en preguntas de certificación. Recuerda también que el máximo configurable es 400 días y que no existe forma de recuperar logs eliminados por expiración.

---

## A2.8 Logs eliminados o expirados: qué ocurre y qué no se puede recuperar

Cuando los logs de un workflow run expiran, la UI de GitHub muestra la página del workflow run con el estado final (éxito o fallo) y la lista de jobs y steps, pero sin ningún contenido de log. En lugar del log aparece un mensaje indicando que los logs han expirado. El estado del workflow run se conserva: GitHub sigue mostrando si terminó en verde o en rojo, cuándo se ejecutó y qué commit disparó la ejecución.

Lo que se pierde irreversiblemente al expirar los logs es todo el contenido de stdout y stderr de cada step. No hay mecanismo de recuperación: GitHub no almacena copias de seguridad de los logs y no ofrece ninguna API para recuperarlos una vez eliminados. Esto tiene implicaciones prácticas para equipos que necesiten mantener registros de auditoría o evidencias de ejecución de pipelines durante periodos largos.

Para escenarios que requieren conservar la salida de los workflows más allá del periodo de retención, las alternativas son: aumentar el periodo de retención hasta 400 días si el plan lo permite, exportar los logs relevantes como artefactos durante la propia ejecución del workflow, o enviar la salida a un sistema de logging externo (como CloudWatch, Datadog o un bucket de almacenamiento).

> [ADVERTENCIA] El estado de la ejecución (éxito/fallo) se conserva después de que los logs expiren, pero sin los logs no hay forma de saber qué causó un fallo. Planifica la estrategia de retención antes de que surja la necesidad de consultar logs antiguos.

---

## A2.9 GITHUB_STEP_SUMMARY: cómo leer los resúmenes generados en la pestaña Summary

`GITHUB_STEP_SUMMARY` es una variable de entorno que contiene la ruta a un fichero especial donde los steps pueden escribir contenido Markdown. Todo lo que se escriba en ese fichero aparece en la pestaña "Summary" del workflow run, que es una vista de resumen de alto nivel separada de los logs detallados de cada job.

La pestaña Summary es el primer lugar que ve un usuario cuando navega a un workflow run. A diferencia de los logs de jobs, que son técnicos y detallados, el Summary está pensado para presentar un resumen legible: resultados de tests, métricas de cobertura, listas de artefactos generados, tablas de comparación de rendimiento o cualquier información que ayude a evaluar rápidamente el resultado de la ejecución.

Para leer un resumen generado por un workflow, basta con navegar a la pestaña "Summary" del workflow run. Si varios steps escribieron en `GITHUB_STEP_SUMMARY`, sus contenidos se concatenan en orden de ejecución. El Summary soporta Markdown completo: encabezados, tablas, listas, código, negrita, cursiva y emojis. El contenido del Summary también expira con los logs del workflow run tras el periodo de retención configurado.

> [CONCEPTO] GITHUB_STEP_SUMMARY no es un log en el sentido técnico: es una vista de presentación que el workflow construye activamente. Un workflow que no escribe nada en GITHUB_STEP_SUMMARY tendrá una pestaña Summary vacía o con solo la información básica de ejecución.

---

## A2.10 Diferencia entre stdout del step y el log estructurado de la UI

Cuando un step ejecuta un comando, todo lo que ese comando escribe en stdout y stderr aparece en el log del step en la UI. Esto se llama la salida no estructurada del step: texto plano que el runner captura y almacena tal cual. La UI presenta esa salida línea a línea con su timestamp.

El log estructurado es diferente: es la capa de organización que GitHub Actions añade por encima del stdout. Incluye la cabecera del step con nombre, estado y duración, los log groups creados con `::group::`, los mensajes de error o warning creados con `::error::` y `::warning::`, y los timestamps calculados por el runner. Esta estructura no proviene del comando ejecutado sino del propio sistema de GitHub Actions.

La distinción es importante para el diagnóstico porque ayuda a identificar el origen de cada elemento del log. Una línea con formato `::error file=src/app.js,line=42::mensaje` es una instrucción de workflow que crea una anotación de error; no es la salida del compilador sino una instrucción explícita que alguien añadió al script. En cambio, la salida sin prefijos especiales es el stdout real del proceso ejecutado.

> [PREREQUISITO] Para entender completamente la diferencia entre stdout y log estructurado es necesario conocer los workflow commands, que se introducen en detalle en el fichero de comandos de workflow. Los comandos como `::group::`, `::error::` y `::warning::` son instrucciones que el runner interpreta desde el stdout del step.

---

## Ejemplo central: log groups y GITHUB_STEP_SUMMARY

El siguiente workflow completo y funcional demuestra la creación de log groups para organizar la salida de un step y el uso de GITHUB_STEP_SUMMARY para generar un resumen visible en la pestaña Summary del workflow run.

```yaml
name: Demo logs y summary

on:
  push:
    branches: [main]
  workflow_dispatch:

jobs:
  build-and-test:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout del repositorio
        uses: actions/checkout@v4

      - name: Configurar Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'npm'

      - name: Instalar dependencias con grupos de log
        run: |
          echo "::group::Instalación de dependencias de producción"
          npm ci --omit=dev
          echo "::endgroup::"

          echo "::group::Instalación de dependencias de desarrollo"
          npm ci
          echo "::endgroup::"

          echo "::group::Verificación de versiones instaladas"
          npm list --depth=0
          echo "::endgroup::"

      - name: Ejecutar linter
        run: |
          echo "::group::Resultado del linter"
          npm run lint 2>&1 || true
          echo "::endgroup::"

      - name: Ejecutar tests y generar reporte
        id: tests
        run: |
          echo "::group::Ejecución de tests unitarios"
          # Simula la ejecución de tests con contadores
          TESTS_TOTAL=42
          TESTS_PASSED=40
          TESTS_FAILED=2
          TESTS_SKIPPED=0

          echo "Tests ejecutados: $TESTS_TOTAL"
          echo "Tests pasados:    $TESTS_PASSED"
          echo "Tests fallidos:   $TESTS_FAILED"
          echo "Tests omitidos:   $TESTS_SKIPPED"
          echo "::endgroup::"

          # Exportar resultados para el summary
          echo "tests_total=$TESTS_TOTAL" >> "$GITHUB_OUTPUT"
          echo "tests_passed=$TESTS_PASSED" >> "$GITHUB_OUTPUT"
          echo "tests_failed=$TESTS_FAILED" >> "$GITHUB_OUTPUT"

      - name: Generar resumen en la pestaña Summary
        if: always()
        run: |
          TOTAL="${{ steps.tests.outputs.tests_total }}"
          PASSED="${{ steps.tests.outputs.tests_passed }}"
          FAILED="${{ steps.tests.outputs.tests_failed }}"

          # Determinar icono de estado
          if [ "$FAILED" -eq 0 ]; then
            STATUS_ICON="✅"
            STATUS_TEXT="Todos los tests pasaron"
          else
            STATUS_ICON="❌"
            STATUS_TEXT="$FAILED tests fallaron"
          fi

          # Escribir el resumen en Markdown
          {
            echo "## $STATUS_ICON Resultado del build"
            echo ""
            echo "**Commit:** \`${{ github.sha }}\`"
            echo "**Rama:** \`${{ github.ref_name }}\`"
            echo "**Ejecutado por:** ${{ github.actor }}"
            echo ""
            echo "### Resultados de tests"
            echo ""
            echo "| Métrica | Valor |"
            echo "|---------|-------|"
            echo "| Total   | $TOTAL |"
            echo "| Pasados | $PASSED |"
            echo "| Fallidos | $FAILED |"
            echo ""
            echo "**Estado:** $STATUS_TEXT"
          } >> "$GITHUB_STEP_SUMMARY"
```

Este workflow demuestra varios conceptos en un flujo real. El step "Instalar dependencias con grupos de log" crea tres grupos colapsables que separan visualmente las fases de instalación. El step "Ejecutar tests y generar reporte" exporta resultados mediante `GITHUB_OUTPUT` para que el step siguiente pueda usarlos. El step final usa `if: always()` para garantizar que el resumen se genera incluso si steps anteriores fallaron, y construye una tabla Markdown que se verá en la pestaña Summary.

> [EXAMEN] La condición `if: always()` en el step de summary es una práctica importante: sin ella, si los tests fallan, el step de summary no se ejecutaría y el resumen no se generaría precisamente cuando más se necesita.

---

## Tabla de referencia: elementos de los logs y la UI

La siguiente tabla resume los elementos clave de la interfaz de logs, su ubicación en la UI y su propósito principal.

| Elemento | Ubicación en la UI | Propósito |
|---|---|---|
| Workflow run | Página principal de la ejecución | Vista global del estado de todos los jobs |
| Job | Lista dentro del workflow run | Agrupación de steps con estado y duración |
| Step | Lista dentro del job (expandible) | Unidad mínima de ejecución con su output |
| Timestamps | Icono de reloj en la vista de logs | Ver hora exacta UTC de cada línea de log |
| Búsqueda de texto | Campo en la parte superior del job | Filtrar líneas por texto dentro de los logs |
| Log group | Sección colapsable dentro de un step | Organizar output en bloques con nombre |
| Download log archive | Menú de opciones del workflow run | Descargar todos los logs como .zip |
| Pestaña Summary | Pestaña superior del workflow run | Ver el resumen Markdown generado por el workflow |
| Retención | Settings → Actions → General | Configurar cuánto tiempo se conservan los logs |
| `GITHUB_STEP_SUMMARY` | Variable de entorno del runner | Ruta al fichero donde escribir el resumen |

---

## Diagrama: flujo de navegación para diagnosticar un fallo

El siguiente diagrama muestra el flujo recomendado para diagnosticar un fallo usando la UI de GitHub Actions.

```
Workflow Run (fallo detectado)
        │
        ▼
┌───────────────────────┐
│  Ver lista de jobs    │
│  ¿Cuál falló?         │
└──────────┬────────────┘
           │
           ▼
┌───────────────────────┐
│  Abrir job fallido    │
│  Step rojo = fallo    │
└──────────┬────────────┘
           │
           ▼
┌───────────────────────┐
│  Revisar steps        │
│  ANTERIORES al fallo  │
│  (pueden tener causa) │
└──────────┬────────────┘
           │
           ├──── Log largo? ──► Activar timestamps
           │
           ├──── Mucho texto? ─► Usar búsqueda de texto
           │
           ├──── Log truncado? ─► Descargar .zip
           │
           └──── ¿Resumen? ────► Pestaña Summary
```

---

## Buenas y malas prácticas

Las siguientes prácticas distinguen un uso eficiente de los logs de GitHub Actions de uno que dificulta el diagnóstico.

**Usar log groups para steps con salida extensa — no dejar todo en un bloque plano**

Un step que instala dependencias, compila, linta y ejecuta tests puede producir miles de líneas de output. Sin log groups, esa salida es una pared de texto ilegible. Con log groups, cada fase es un bloque con nombre que se puede expandir o colapsar independientemente. La buena práctica es crear un grupo por cada fase lógica del step; la mala práctica es emitir todo el output sin estructura.

**Generar el summary con `if: always()` — no depender del éxito del workflow**

El summary es más valioso cuando el workflow falla, porque es el primer lugar donde el equipo busca información. Si el step que genera el summary solo se ejecuta cuando todo va bien (`if: success()` o sin condición), el summary estará vacío precisamente en los fallos. La buena práctica es generar siempre el summary usando `if: always()`; la mala práctica es omitir la condición o usar `if: success()`.

**Configurar la retención de logs según las necesidades del equipo — no dejar el valor por defecto sin analizarlo**

90 días es el valor por defecto, pero puede ser insuficiente para equipos que necesitan auditar ejecuciones de meses anteriores o que tienen ciclos de sprint largos. La buena práctica es revisar explícitamente la política de retención y ajustarla al máximo necesario; la mala práctica es descubrir que los logs de un incidente importante han expirado cuando ya se necesitan.

**Buscar el origen del fallo en steps anteriores al error — no asumir que el step rojo es la causa raíz**

GitHub marca en rojo el step cuyo proceso terminó con código de salida distinto de cero, pero ese proceso puede haber fallado porque un step anterior dejó el entorno en un estado incorrecto. La buena práctica es revisar los steps anteriores al fallo, especialmente los que configuran el entorno (checkout, setup de lenguaje, instalación de dependencias); la mala práctica es centrarse únicamente en el step marcado en rojo y perder el contexto.

**Descargar el archivo .zip cuando el log está truncado — no asumir que la UI muestra todo**

Para builds con salida voluminosa, la UI trunca la visualización y puede ocultar líneas importantes. La buena práctica es descargar el archivo completo cuando la UI indica truncamiento; la mala práctica es confiar en la búsqueda de la UI sobre un log truncado y concluir que cierto mensaje no existe en el log.

---

## Verificación y práctica

### Preguntas estilo certificación

**Pregunta 1**

Un workflow tiene un job con 8 steps. El step 5 falla. ¿Qué ocurre con los steps 6, 7 y 8?

A) Se ejecutan normalmente porque están en el mismo job  
B) Se marcan como "skipped" y no se ejecutan, a menos que tengan `if: always()` o condiciones equivalentes  
C) Se ejecutan pero sus resultados se ignoran  
D) El runner los ejecuta en paralelo para intentar recuperar el workflow  

> Respuesta correcta: B. Por defecto, cuando un step falla, los steps siguientes del mismo job no se ejecutan y quedan en estado "skipped". La excepción son los steps con `if: always()`, `if: failure()` u otras condiciones que evalúen explícitamente el estado del job.

**Pregunta 2**

¿Cuánto tiempo se conservan por defecto los logs de un workflow run en un repositorio con plan gratuito?

A) 30 días  
B) 60 días  
C) 90 días  
D) 400 días  

> Respuesta correcta: C. La retención por defecto de los logs de GitHub Actions es 90 días. El valor de 400 días es el máximo configurable en planes de pago, no el valor por defecto.

**Pregunta 3**

Un desarrollador quiere que el step "Publicar resumen de tests" se ejecute siempre, incluso si los tests fallaron. ¿Qué configuración es correcta?

```yaml
# Opción A
- name: Publicar resumen de tests
  run: ./scripts/generate-summary.sh

# Opción B
- name: Publicar resumen de tests
  if: always()
  run: ./scripts/generate-summary.sh

# Opción C
- name: Publicar resumen de tests
  if: success()
  run: ./scripts/generate-summary.sh

# Opción D
- name: Publicar resumen de tests
  continue-on-error: true
  run: ./scripts/generate-summary.sh
```

> Respuesta correcta: B. La opción A se ejecuta solo si todos los steps anteriores tuvieron éxito (comportamiento por defecto). La opción C es equivalente a A. La opción D ejecuta el step solo si los anteriores tuvieron éxito, y además ignora los errores del propio step. Solo la opción B garantiza la ejecución independientemente del resultado de los steps anteriores.

---

### Ejercicio práctico

Crea un workflow en tu repositorio que haga lo siguiente:

1. Se dispare con `workflow_dispatch` para poder ejecutarlo manualmente.
2. Tenga un job llamado `demo-logs` con los siguientes steps:

   **Step 1 — "Preparar entorno"**: usa log groups para separar al menos dos fases (por ejemplo, verificar versiones de herramientas disponibles y listar el directorio de trabajo).

   **Step 2 — "Simular proceso con resultados"**: crea variables con contadores ficticios (elementos procesados, errores encontrados, advertencias) y expórtalas con `GITHUB_OUTPUT`.

   **Step 3 — "Publicar summary"**: con `if: always()`, lee las variables del step anterior y escribe un resumen Markdown en `GITHUB_STEP_SUMMARY` que incluya al menos una tabla con los resultados.

3. Ejecuta el workflow y verifica:
   - Que los log groups aparecen como secciones colapsables en la UI.
   - Que la pestaña Summary muestra el contenido Markdown generado.
   - Que al activar los timestamps en la UI, cada línea muestra la hora en UTC.

> [CONCEPTO] Este ejercicio cubre los tres conceptos más frecuentemente evaluados en el GH-200 relacionados con los logs: log groups, GITHUB_STEP_SUMMARY y la navegación por la jerarquía de la UI. Completarlo de forma práctica consolida mejor los conceptos que solo leerlos.

---

← [2.1 Interpretación de triggers y UI de ejecución](gha-d2-triggers-ui.md) | → [2.3.1 Diagnóstico de comportamiento](gha-d2-diagnostico-comportamiento.md)
