> **Navegación:** [← 4.11 REST API Enterprise](gha-d4-api-rest-enterprise.md) | [Índice](README.md) | [4.13 Testing D4 →](gha-d4-testing.md)

# 4.12 Auditoría y Gobernanza de Actions en la organización

En una organización con múltiples equipos y repositorios, saber qué workflows se han ejecutado, qué secrets han sido accedidos, qué runners se han registrado y qué actions se han usado no es opcional: es un requisito de seguridad y cumplimiento. GitHub proporciona el **audit log de organización** como la fuente centralizada de verdad para todas estas preguntas. Combinado con políticas de gobernanza sobre actions permitidas y runners autorizados, el audit log permite a los administradores mantener visibilidad total del uso de GitHub Actions sin necesidad de revisar cada repositorio individualmente.

## Audit log de organización: acceso y navegación

El audit log registra eventos de seguridad y administración a nivel de organización. Para acceder a él se navega a **Organization Settings > Security > Audit log**. La interfaz web muestra los eventos de los últimos 90 días con búsqueda y filtrado en tiempo real.

> [CONCEPTO] El audit log de organización registra eventos de distintas categorías: `workflows.*`, `actions.*`, `secret.*`, `runners.*`, entre otras. Cada evento incluye el actor (usuario o app), la fecha/hora UTC, el repositorio afectado y metadatos del evento.

Para filtrar únicamente los eventos de GitHub Actions se usa el calificador `action:` en el campo de búsqueda de la UI o en la API:

```
action:workflows.*
action:actions.*
action:secret.*
action:runners.*
```

Puedes combinar calificadores: `action:workflows.* actor:username repo:org/repo` para acotar el resultado a un usuario y repositorio concretos.

## Tabla de eventos de audit log más comunes en Actions

Los siguientes eventos son los que con mayor frecuencia aparecen en escenarios de auditoría y en el examen GH-200.

| Evento | Descripción | Quién lo genera |
|--------|-------------|-----------------|
| `workflows.run` | Se inició una ejecución de workflow | Trigger (push, PR, manual…) |
| `workflows.completed` | Una ejecución de workflow terminó | Sistema |
| `workflows.disabled_repo` | Se deshabilitaron workflows en un repositorio | Admin |
| `actions.created` | Se añadió una action (referenciada por primera vez) | Workflow run |
| `actions.allowed` | Una action fue permitida en las políticas de la org | Admin |
| `secret.access` | Un secret de organización fue accedido por un workflow | Workflow run |
| `secret.create` | Se creó un secret de organización o de repositorio | Usuario |
| `secret.destroy` | Se eliminó un secret | Usuario |
| `runners.create` | Se registró un self-hosted runner en la organización | Usuario/script |
| `runners.delete` | Se eliminó un runner | Admin |
| `runner_groups.create` | Se creó un grupo de runners | Admin |

> [EXAMEN] El audit log **no registra el contenido de los secrets**, solo los eventos de acceso (cuándo, desde qué repositorio y qué workflow accedió). Esto es intencional por seguridad: si el audit log guardara el valor del secret, un actor con acceso al log podría exfiltrarlo.

## API para audit log

La interfaz web es útil para consultas puntuales, pero para automatizar auditorías, exportar datos a sistemas externos o procesar grandes volúmenes de eventos se usa la API REST.

El endpoint principal para el audit log de organización es:

```
GET /orgs/{org}/audit-log
```

Los parámetros más relevantes para filtrar eventos de Actions son:

```bash
curl -H "Authorization: Bearer $TOKEN" \
     -H "Accept: application/vnd.github+json" \
     "https://api.github.com/orgs/mi-org/audit-log?\
phrase=action:workflows.*&include=web&per_page=100"
```

| Parámetro | Valores posibles | Descripción |
|-----------|-----------------|-------------|
| `phrase` | `action:workflows.*` | Filtro de texto libre (misma sintaxis que la UI) |
| `include` | `web`, `git`, `all` | Tipo de eventos: web (UI/API), git (operaciones git), all |
| `per_page` | 1–100 | Resultados por página |
| `after` / `before` | cursor opaco | Paginación basada en cursor |
| `order` | `asc`, `desc` | Ordenación cronológica |

> [CONCEPTO] El parámetro `include=git` devuelve eventos de operaciones git (push, clone, fork). Para auditar Actions específicamente se usa `include=web` o `include=all`. El endpoint requiere un token con scope `read:audit_log` (PAT clásico) o permiso `organization: audit_log: read` (fine-grained PAT).

## Exportación CSV y JSON

Para exportaciones ad-hoc o para archivar evidencia de cumplimiento, la UI de audit log ofrece botones de exportación directa. La exportación está disponible desde **Organization Settings > Security > Audit log > Export**, seleccionando el rango de fechas y el formato deseado.

- **CSV:** adecuado para análisis en hojas de cálculo o importación en herramientas SIEM que aceptan CSV.
- **JSON:** recomendado para procesamiento programático, ya que preserva todos los campos del evento, incluyendo metadatos que el CSV puede omitir.

> [ADVERTENCIA] La exportación manual desde la UI solo alcanza los 90 días de retención. Si necesitas retención a largo plazo para cumplimiento regulatorio, debes configurar **audit log streaming** antes de que los eventos expiren, ya que no es posible recuperar eventos pasados una vez caducados.

## Audit log streaming (solo GitHub Enterprise Cloud)

El streaming de audit log envía eventos en tiempo casi real a un sistema externo, eliminando la limitación de los 90 días de retención de la UI. Esta funcionalidad es **exclusiva de GitHub Enterprise Cloud** y no está disponible en GitHub Free, Team ni GitHub Enterprise Server en su configuración estándar.

Los destinos de streaming soportados son:

| Destino | Protocolo | Caso de uso típico |
|---------|-----------|-------------------|
| **Amazon S3** | HTTPS / S3 API | Almacenamiento a largo plazo, análisis con Athena |
| **Azure Event Hubs** | AMQP / HTTPS | Integración con SIEM de Azure (Microsoft Sentinel) |
| **Splunk** | HTTP Event Collector (HEC) | SIEM en Splunk Cloud o Splunk Enterprise |
| **Datadog** | HTTPS Logs API | Dashboards y alertas de seguridad en Datadog |
| **Google Cloud Storage** | HTTPS / GCS API | Almacenamiento en GCP, BigQuery |

La configuración se realiza en **Organization Settings > Security > Audit log > Log streaming**. Con streaming activo, la retención efectiva pasa de 90 días a hasta **7 años**, según la política de retención configurada en el destino externo.

> [EXAMEN] El audit log streaming es una característica exclusiva de **GitHub Enterprise Cloud**. Si un escenario de examen menciona retención superior a 90 días o integración en tiempo real con Splunk/Datadog/SIEM, la respuesta siempre requiere GitHub Enterprise Cloud con streaming habilitado.

## Gobernanza: visibilidad de uso de Actions en la organización

El audit log resuelve el "qué pasó", pero la gobernanza también requiere políticas preventivas que controlen "qué puede pasar". GitHub permite a los administradores de organización combinar tres palancas de gobernanza.

**Actions permitidas:** en **Organization Settings > Actions > General > Actions permissions** se puede restringir qué actions pueden usar los repositorios de la organización. Las opciones son: solo actions del mismo repositorio, solo actions del mismo owner, actions de GitHub verificadas, o una lista de patrones permitidos explícitos (`octocat/hello-world@v1`, `actions/*@*`).

**Runner groups:** los self-hosted runners se asignan a grupos. Un grupo puede limitarse a repositorios concretos, lo que impide que un runner con acceso a infraestructura sensible sea usado por cualquier repositorio de la organización.

**Visibilidad de secrets de organización:** al crear un secret de organización se define su visibilidad: todos los repositorios, solo repositorios privados, o una selección explícita de repositorios. El audit log registra cada acceso, lo que permite auditar si un secret de organización fue usado desde un repositorio que no debería tenerlo.

> [CONCEPTO] La gobernanza efectiva combina **políticas preventivas** (qué está permitido) con **auditoría reactiva** (qué ocurrió). Las políticas solas no bastan si nadie revisa el audit log; el audit log solo sirve si hay políticas que definan qué es una anomalía.

## Workflow de ejemplo: deploy que genera eventos de audit log

El siguiente workflow ilustra un despliegue a producción que, al ejecutarse, genera varios eventos en el audit log de la organización: `workflows.run`, `secret.access` (al acceder al secret de environment) y opcionalmente `runners.create` si el runner es efímero.

```yaml
# .github/workflows/deploy-produccion.yml
name: Deploy a Producción

on:
  push:
    branches: [main]

jobs:
  deploy:
    name: Deploy
    runs-on: ubuntu-latest
    environment: production           # protección de environment con reviewers
    permissions:
      contents: read
      id-token: write                 # necesario para OIDC si se usa

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Autenticar con proveedor cloud
        uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: ${{ secrets.AWS_ROLE_ARN }}   # genera secret.access
          aws-region: eu-west-1

      - name: Desplegar aplicación
        run: |
          echo "Desplegando versión ${{ github.sha }}"
          aws s3 sync ./dist s3://${{ secrets.S3_BUCKET }}  # genera secret.access

      - name: Notificar resultado
        if: always()
        run: echo "Deploy finalizado con estado ${{ job.status }}"
```

Cada vez que este workflow se ejecuta, el audit log de la organización registrará:

1. `workflows.run` — inicio de la ejecución desde el push a `main`
2. `secret.access` — acceso a `AWS_ROLE_ARN` del environment `production`
3. `secret.access` — acceso a `S3_BUCKET` del environment `production`
4. `workflows.completed` — finalización con el estado resultante

> [ADVERTENCIA] Usar `environment: production` sin configurar **required reviewers** en el environment permite que cualquier push a `main` acceda a los secrets de producción sin aprobación humana. Para workflows de deploy real, configura al menos un reviewer en **Repository Settings > Environments > production**.

## Buenas y malas prácticas

**Buenas prácticas:**

- Revisar el audit log periódicamente con el filtro `action:secret.*` para detectar accesos inesperados a secrets de organización.
- Configurar audit log streaming a un SIEM (Splunk, Datadog, Microsoft Sentinel) si la organización está en GitHub Enterprise Cloud y tiene requisitos de retención superiores a 90 días.
- Restringir las actions permitidas a una lista explícita en lugar de "todas las actions públicas", para reducir la superficie de ataque de supply chain.
- Usar runner groups con acceso limitado por repositorio para runners con acceso a infraestructura sensible.
- Auditar regularmente los secrets de organización y su visibilidad con `action:secret.create` y `action:secret.destroy`.

**Malas prácticas:**

- Confiar únicamente en la UI de audit log sin streaming: los eventos desaparecen a los 90 días sin posibilidad de recuperación.
- Asumir que el audit log registra el contenido de los secrets: solo registra el evento de acceso, nunca el valor.
- Ignorar los eventos `runners.create` desde IPs o usuarios desconocidos: un runner no autorizado puede interceptar jobs y exfiltrar secrets.

## Preguntas de examen

**Pregunta 1.** Un administrador quiere que el audit log de su organización en GitHub Enterprise Cloud se archive durante 3 años en Amazon S3. ¿Qué debe configurar?

A) Descargar el CSV del audit log mensualmente y subirlo a S3 manualmente  
B) Activar audit log streaming con destino Amazon S3 en Organization Settings  
C) Usar la API `GET /orgs/{org}/audit-log` con un cron job diario  
D) Ampliar la retención del audit log a 3 años en Organization Settings > Security

> **Respuesta correcta: B)** — El streaming de audit log es la única forma de conseguir retención a largo plazo de forma continua y sin intervención manual. A) es manual y no es continuo. C) solo recupera los últimos 90 días disponibles. D) no existe esa opción: la retención en UI es fija en 90 días.

---

**Pregunta 2.** ¿Qué información incluye un evento `secret.access` en el audit log de organización?

A) El nombre del secret, el valor enmascarado y el workflow que accedió  
B) El nombre del secret, el repositorio, el workflow run ID y el actor  
C) Solo el repositorio y la fecha/hora del acceso  
D) El nombre del secret y su valor encriptado con la clave pública de la organización

> **Respuesta correcta: B)** — El audit log registra metadatos del evento: qué secret (por nombre), desde qué repositorio, qué run y quién lo disparó. Nunca registra el valor del secret ni siquiera enmascarado (A y D son incorrectas). C) es incompleto.

---

**Pregunta 3.** Un equipo de seguridad necesita saber si algún workflow de la organización usó una action de un repositorio de terceros no aprobado en los últimos 30 días. ¿Cuál es el método más directo?

A) Revisar manualmente todos los archivos `.github/workflows/*.yml` de la organización  
B) Consultar el audit log con el filtro `action:actions.*` y buscar el nombre de la action  
C) Activar la política de actions permitidas y esperar a que falle el próximo workflow  
D) Crear un workflow de auditoría que enumere todos los `uses:` de los repositorios

> **Respuesta correcta: B)** — El audit log con `action:actions.*` muestra qué actions fueron invocadas y desde qué repositorios. A) es manual e impracticable a escala. C) es preventivo pero no revela qué ocurrió en el pasado. D) analiza la configuración actual, no el histórico de ejecuciones.

---

> **Navegación:** [← 4.11 REST API Enterprise](gha-d4-api-rest-enterprise.md) | [Índice](README.md) | [4.13 Testing D4 →](gha-d4-testing.md)
