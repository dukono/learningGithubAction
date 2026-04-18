# 4.5.1 Runner Groups: creación, niveles org/enterprise y políticas de acceso

← [4.4 IP Allow Lists](gha-d4-ip-allow-lists.md) | [Índice](README.md) | [4.5.2 Runner Groups: asignación](gha-d4-runner-groups-asignacion.md) →

---

Los runners en GitHub Actions pueden ser recursos compartidos o exclusivos. Sin control, cualquier repositorio de una organización podría usar cualquier runner, lo que crea problemas de seguridad, costes y equidad. Los **runner groups** resuelven este problema agrupando runners bajo una política de acceso: se define qué repositorios pueden ver y usar cada grupo de runners. Esto permite, por ejemplo, reservar runners con hardware especializado para proyectos de producción, o evitar que repositorios públicos accedan a runners que tienen acceso a redes internas.

## Qué es un runner group y para qué sirve

Un runner group es un contenedor lógico que agrupa uno o más runners y define una política de visibilidad. Los runners individuales se añaden al grupo y heredan su política. Los workflows en los repositorios autorizados pueden entonces usar esos runners con `runs-on`.

El propósito principal es el **control de acceso**: sin runner groups, cualquier repositorio de la organización puede usar cualquier runner self-hosted. Con runner groups se establece un perímetro claro sobre quién puede usar qué infraestructura.

Los runner groups operan en dos niveles:

- **Nivel organización**: controla qué repositorios dentro de la org pueden usar los runners del grupo.
- **Nivel enterprise**: controla qué organizaciones dentro del enterprise pueden usar los runners del grupo, y además define límites superiores para los runner groups de organización.

## El runner group predeterminado: Default

Cuando se registra un nuevo runner self-hosted en una organización, se asigna automáticamente al runner group llamado **Default**. Este grupo existe en todas las organizaciones y no puede eliminarse.

Por defecto, el grupo Default concede acceso a **todos los repositorios** de la organización. Esto significa que, en una instalación básica sin configuración adicional, todos los repositorios pueden usar todos los runners self-hosted. Si esta apertura no es aceptable, es necesario crear grupos adicionales con políticas más restrictivas y mover los runners allí.

> **Clave para el examen:** El grupo `Default` no puede eliminarse. Los runners recién registrados siempre van al grupo `Default` antes de ser movidos a otro grupo.

## Crear un runner group a nivel organización

La creación de runner groups a nivel de organización requiere permisos de administrador de la organización. El flujo de la interfaz es:

**Ruta de navegación:** `Settings de la organización > Actions > Runner groups > New runner group`

Al crear el grupo se configuran tres aspectos principales:

1. **Nombre del grupo**: identificador único dentro de la organización.
2. **Política de repositorios**: quién puede usar los runners de este grupo.
3. **Política de forks**: si los workflows de pull requests provenientes de forks pueden usar estos runners.

Las opciones de política de repositorios son:

| Política | Descripción |
|---|---|
| All repositories | Todos los repos de la org pueden usar este grupo (comportamiento igual al Default) |
| Selected repositories | Solo los repos explícitamente añadidos pueden usar este grupo |

La opción **Selected repositories** es la más segura para runners con acceso privilegiado o hardware dedicado.

> **Advertencia:** La política "All repositories" en un runner group con acceso a redes internas expone esa red a todos los repositorios de la organización, incluyendo repositorios públicos si la política de forks está habilitada.

## Política de forks y pull requests

Cuando un pull request llega desde un fork de un repositorio público, el código no es de confianza. Por eso GitHub, por defecto, **no ejecuta workflows con runners self-hosted en pull requests de forks**.

En la configuración del runner group existe una opción explícita: **"Allow public repositories"** o más específicamente **"Allow fork pull request workflows"**. Activar esta opción permite que PRs de forks usen los runners del grupo.

> **Clave para el examen:** Por razones de seguridad, los runners self-hosted NO ejecutan workflows de fork PRs por defecto. Debe habilitarse explícitamente en la configuración del runner group.

```yaml
# Ejemplo de workflow que usa un runner de un grupo específico
# El runner group controla si este repo puede ejecutarlo
name: Build con runner dedicado

on: [push]

jobs:
  build:
    runs-on: [self-hosted, linux, production]
    # 'production' es una label del runner en el grupo 'Production Runners'
    steps:
      - uses: actions/checkout@v4
      - name: Build
        run: make build
```

## Crear un runner group a nivel enterprise

En cuentas enterprise, existe un nivel adicional por encima de las organizaciones. Los runner groups de enterprise permiten compartir runners entre varias organizaciones bajo el mismo enterprise, o reservar runners de alto valor para organizaciones específicas.

**Ruta de navegación:** `Enterprise Settings > Policies > Actions > Runner groups > New runner group`

La lógica es similar al nivel organización, pero la política de acceso ahora se define en términos de organizaciones:

| Política enterprise | Descripción |
|---|---|
| All organizations | Todas las orgs del enterprise pueden usar este grupo |
| Selected organizations | Solo las orgs explícitamente añadidas tienen acceso |

Un runner group de enterprise puede además **definir restricciones** sobre cómo las organizaciones pueden configurar sus propios runner groups. Si el enterprise establece que ciertos runners solo están disponibles para organizaciones seleccionadas, esas organizaciones no pueden ampliar el acceso por su cuenta.

> **Clave para el examen:** Los runner groups de enterprise establecen el límite máximo de acceso. Una organización no puede dar más acceso del que el enterprise permite.

## Larger runners y runner groups

Los **larger runners** son runners GitHub-hosted con más CPU, memoria o características adicionales (como IPs estáticas). A diferencia de los runners estándar de GitHub, los larger runners se pueden asignar a runner groups específicos dentro de la organización.

Esto permite combinar las ventajas de los runners gestionados por GitHub (sin mantenimiento) con el control de acceso que ofrecen los runner groups. Por ejemplo, se puede crear un grupo `gpu-runners` con runners GPU-enabled y restringirlo solo a los repositorios de machine learning de la organización.

La asignación de larger runners a grupos se configura en la misma interfaz de runner groups, y los workflows los seleccionan mediante labels igual que con runners self-hosted.

## Ejemplo central

Un equipo de DevOps quiere reservar runners con acceso a la red de producción solo para los repositorios de deployment, mientras que el resto de repositorios sigue usando runners estándar. La solución es crear un runner group restringido.

```yaml
# .github/workflows/deploy-production.yml
# Solo funciona si este repositorio está en el runner group 'production-deployers'
name: Deploy a Producción

on:
  push:
    branches: [main]

jobs:
  deploy:
    # Este runner está en el grupo 'production-deployers'
    # Solo repos autorizados en ese grupo pueden ejecutar este job
    runs-on: [self-hosted, linux, deploy]
    environment: production
    steps:
      - uses: actions/checkout@v4

      - name: Verificar acceso a red interna
        run: curl -f http://internal-registry.company.local/health

      - name: Deploy
        run: |
          ./scripts/deploy.sh --env production
        env:
          DEPLOY_TOKEN: ${{ secrets.PRODUCTION_DEPLOY_TOKEN }}

  # Job de test usa runners estándar (GitHub-hosted)
  # No necesita acceso a red interna
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: npm test
```

La configuración en la UI de la organización sería:
- Runner group: `production-deployers`
- Política de repositorios: Selected repositories → `backend-api`, `infrastructure`
- Política de forks: desactivada (nunca ejecutar en código de forks)

## Tabla de elementos clave

Los runner groups tienen varios parámetros configurables. Esta tabla resume los más relevantes para el examen GH-200.

| Parámetro | Nivel | Opciones | Default | Descripción |
|---|---|---|---|---|
| Nombre del grupo | Org / Enterprise | Texto libre | — | Identificador único del grupo en su nivel |
| Política de repos | Org | All / Selected | All | Qué repositorios pueden usar el grupo |
| Política de orgs | Enterprise | All / Selected | All | Qué organizaciones pueden usar el grupo |
| Fork PR workflows | Org | Habilitado / Deshabilitado | Deshabilitado | Permite PRs de forks usar estos runners |
| Allow public repos | Org | Habilitado / Deshabilitado | Deshabilitado | Permite repos públicos usar estos runners |

## Buenas y malas prácticas

**Hacer:**
- Crear runner groups separados por entorno (staging, production) — razón: evita que workflows de desarrollo accedan accidentalmente a infraestructura de producción.
- Mantener el grupo Default solo con runners de uso general sin acceso privilegiado — razón: cualquier repositorio nuevo de la organización tendrá acceso automático al grupo Default.
- Documentar la política de cada runner group en su descripción — razón: facilita auditorías de seguridad y onboarding de nuevos equipos.

**Evitar:**
- Habilitar "Allow fork pull request workflows" en runners con acceso a secretos o redes internas — razón: código de forks no auditado puede exfiltrar secretos o atacar sistemas internos.
- Poner todos los runners en el grupo Default con política "All repositories" — razón: elimina el control de acceso granular que justifica usar runners self-hosted.
- Crear runner groups a nivel enterprise cuando la necesidad es solo a nivel organización — razón: complica la administración sin beneficio real.

## Verificación y práctica

Las siguientes preguntas están modeladas sobre el formato del examen GH-200 para evaluar la comprensión de runner groups.

**Pregunta 1:** Un runner self-hosted recién registrado en una organización que tiene tres runner groups configurados: Default, staging-runners y production-runners. ¿En qué grupo se encuentra el runner inmediatamente después del registro?

> **Respuesta: Default.** Los runners recién registrados siempre se asignan al grupo Default, independientemente de cuántos grupos existan. Deben moverse manualmente al grupo deseado desde la interfaz de Settings.

**Pregunta 2:** Un repositorio público de la organización intenta ejecutar un workflow con `runs-on: [self-hosted, linux]` desde un pull request de un fork. El runner group que contiene ese runner tiene la política "All repositories". ¿El workflow se ejecutará?

> **Respuesta: No.** Aunque el grupo permite todos los repositorios, la opción "Allow fork pull request workflows" está desactivada por defecto. Los workflows de fork PRs no se ejecutan en runners self-hosted a menos que esta opción esté habilitada explícitamente en la configuración del runner group.

**Pregunta 3:** ¿Cuál es la diferencia entre crear un runner group a nivel organización vs. a nivel enterprise?

> **Respuesta:** Un runner group a nivel **organización** controla qué repositorios dentro de esa organización pueden usar los runners. Un runner group a nivel **enterprise** controla qué organizaciones dentro del enterprise pueden usar los runners, y establece el límite máximo de acceso que las organizaciones no pueden superar. Los runner groups de enterprise permiten compartir runners entre múltiples organizaciones.

**Ejercicio práctico:** Configura un runner group llamado `ml-gpu` que solo permita los repositorios `data-science` y `model-training` de la organización, y que bloquee el acceso desde forks. Luego escribe el fragmento de workflow para usar esos runners.

```yaml
# Solución: fragmento de workflow para usar runners del grupo ml-gpu
# Configuración del grupo en la UI:
# - Nombre: ml-gpu
# - Política: Selected repositories → data-science, model-training
# - Fork PR workflows: deshabilitado

name: Train Model

on:
  push:
    branches: [main]

jobs:
  train:
    # El runner en el grupo ml-gpu tiene label 'gpu'
    # Solo funciona si este repo está autorizado en el grupo ml-gpu
    runs-on: [self-hosted, linux, gpu]
    steps:
      - uses: actions/checkout@v4
      - name: Train
        run: python train.py --epochs 100
```

---

← [4.4 IP Allow Lists](gha-d4-ip-allow-lists.md) | [Índice](README.md) | [4.5.2 Runner Groups: asignación](gha-d4-runner-groups-asignacion.md) →
