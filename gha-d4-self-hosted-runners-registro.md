# 4.7.1 Self-Hosted Runners: registro, configuración inicial y labels

← [4.6 Preinstalled Software](gha-d4-preinstalled-software.md) | [Índice](README.md) | [4.7.2 Self-Hosted Runners: seguridad](gha-d4-self-hosted-runners-seguridad.md) →

---

Un self-hosted runner es una máquina (física, virtual o contenedor) que tú administras y que GitHub Actions usa para ejecutar jobs. A diferencia de los runners alojados por GitHub, el self-hosted runner corre en tu propia infraestructura, dándote control total sobre el hardware, el sistema operativo y el software instalado.

> [CONCEPTO] Un self-hosted runner se comunica con GitHub a través de conexiones HTTPS salientes; no necesita abrir puertos de entrada en el firewall.

## Requisitos del sistema operativo

Antes de registrar un runner, la máquina debe cumplir los requisitos mínimos de la aplicación runner de GitHub Actions. Los sistemas soportados son Linux (x64, ARM64, ARM), Windows (x64) y macOS (x64, ARM64). La aplicación runner está escrita en .NET y requiere las dependencias que el instalador detecta automáticamente.

| Sistema | Arquitecturas soportadas | Notas |
|---------|--------------------------|-------|
| Linux   | x64, ARM64, ARM          | Ubuntu, Debian, RHEL, Fedora, SUSE |
| Windows | x64                      | Windows Server 2016+ |
| macOS   | x64, ARM64               | macOS 11+ |

> [ADVERTENCIA] El runner no soporta sistemas de 32 bits. Verifica la arquitectura con `uname -m` (Linux) o `echo %PROCESSOR_ARCHITECTURE%` (Windows) antes de empezar.

## Proceso de registro desde la UI

El registro de un runner siempre comienza en la interfaz de GitHub, donde se obtiene el token de registro. La ruta varía según el nivel de registro deseado.

**Nivel repositorio:** Settings > Actions > Runners > New self-hosted runner

**Nivel organización:** Organization Settings > Actions > Runners > New runner

**Nivel enterprise:** Enterprise Settings > Actions > Runners > New runner

Una vez en la pantalla de registro, GitHub muestra los comandos exactos incluyendo el token de registro temporal. Este token solo es válido durante **1 hora**; si expira, debes obtener uno nuevo desde la UI.

> [EXAMEN] El token de registro se obtiene desde Settings > Actions > Runners y tiene validez de 1 hora. Es distinto del token de autenticación ACTIONS_RUNNER_TOKEN que GitHub genera para cada job.

## Script de configuración config.sh

Tras descargar y descomprimir el paquete del runner, el script `./config.sh` enlaza la máquina con el repositorio, organización o enterprise. Los parámetros mínimos obligatorios son `--url` y `--token`.

Los parámetros más relevantes del script de configuración son los siguientes.

| Parámetro | Tipo | Obligatorio | Default | Descripción |
|-----------|------|-------------|---------|-------------|
| `--url` | string | Sí | — | URL del repo, org o enterprise |
| `--token` | string | Sí | — | Token de registro (1 hora) |
| `--name` | string | No | hostname | Nombre del runner en GitHub |
| `--labels` | string | No | — | Labels personalizadas separadas por comas |
| `--runnergroup` | string | No | Default | Runner group de destino |
| `--work` | string | No | `_work` | Directorio de trabajo para los jobs |
| `--ephemeral` | flag | No | false | El runner se elimina tras completar un job |

## Ejemplo central

El siguiente workflow completo muestra cómo registrar un runner Linux en una organización, asignarle labels personalizadas e instalarlo como servicio del sistema para que arranque automáticamente.

```yaml
# Comandos de registro y configuración del self-hosted runner
# (ejecutar en la máquina que actuará como runner)

# 1. Descargar el paquete del runner (versión actual: 2.317.0)
mkdir actions-runner && cd actions-runner
curl -o actions-runner-linux-x64-2.317.0.tar.gz -L \
  https://github.com/actions/runner/releases/download/v2.317.0/actions-runner-linux-x64-2.317.0.tar.gz
tar xzf ./actions-runner-linux-x64-2.317.0.tar.gz

# 2. Configurar el runner contra la organización con labels personalizadas
./config.sh \
  --url https://github.com/mi-organizacion \
  --token AABBCC1234567890DDEEFF \
  --name runner-produccion-01 \
  --labels self-hosted,linux,x64,produccion,docker \
  --runnergroup produccion \
  --work _work \
  --unattended

# 3. Instalar y arrancar como servicio del sistema (Linux/macOS)
sudo ./svc.sh install
sudo ./svc.sh start

# 4. Verificar estado del servicio
sudo ./svc.sh status
```

```yaml
# Workflow que usa el runner con label personalizada
name: Deploy produccion

on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: [self-hosted, linux, produccion]
    steps:
      - uses: actions/checkout@v4

      - name: Verificar entorno del runner
        run: |
          echo "Runner: $RUNNER_NAME"
          echo "OS: $RUNNER_OS"
          echo "Arch: $RUNNER_ARCH"
          uname -a

      - name: Ejecutar deploy
        run: ./scripts/deploy.sh
```

## Labels predeterminadas y personalizadas

Cuando se registra un runner, GitHub asigna automáticamente tres labels predeterminadas basadas en las características detectadas de la máquina. Estas labels no se pueden eliminar y permiten identificar el runner sin configuración adicional.

| Label predeterminada | Valores posibles | Descripción |
|----------------------|-----------------|-------------|
| `self-hosted` | siempre presente | Distingue de runners alojados por GitHub |
| Sistema operativo | `linux`, `windows`, `macos` | SO detectado automáticamente |
| Arquitectura | `x64`, `arm64`, `arm` | Arquitectura de la CPU detectada |

Las labels personalizadas se añaden con `--labels` en `config.sh` o desde la UI en Settings > Actions > Runners. Permiten segmentar runners por entorno (produccion, staging), capacidad (docker, gpu) o equipo (backend, frontend).

> [CONCEPTO] Los workflows seleccionan runners mediante `runs-on` con una lista de labels. GitHub elige un runner que tenga **todas** las labels especificadas; si ningún runner coincide, el job queda en cola hasta que uno esté disponible.

## Ejecutar runner como servicio del sistema

En Linux y macOS, el script `./svc.sh` instala y gestiona el runner como un servicio del sistema operativo (systemd en Linux, launchd en macOS). En Windows, se instala como un Windows Service.

```bash
# Linux y macOS
sudo ./svc.sh install    # instala el servicio
sudo ./svc.sh start      # arranca el runner
sudo ./svc.sh stop       # detiene el runner
sudo ./svc.sh status     # muestra estado actual
sudo ./svc.sh uninstall  # elimina el servicio del sistema
```

```powershell
# Windows (PowerShell como administrador)
.\svc.sh install
.\svc.sh start
.\svc.sh status
```

> [ADVERTENCIA] Si el runner se inicia como servicio con `sudo ./svc.sh install`, corre con el usuario que ejecutó el comando (o el especificado). No uses `root` para el servicio en producción; crea un usuario dedicado sin privilegios elevados para el runner.

## Registro a nivel repo, organización y enterprise

El nivel de registro determina qué repositorios pueden usar el runner. Un runner registrado a nivel de repositorio solo puede ejecutar jobs de ese repositorio específico. Registrar a nivel de organización lo hace disponible para todos los repositorios de la organización, y a nivel enterprise para todas las organizaciones del enterprise.

| Nivel | Ruta en UI | Disponibilidad |
|-------|-----------|----------------|
| Repositorio | Repo > Settings > Actions > Runners | Solo ese repositorio |
| Organización | Org > Settings > Actions > Runners | Todos los repos de la org |
| Enterprise | Enterprise > Settings > Actions > Runners | Todas las orgs del enterprise |

> [EXAMEN] Para registrar un runner a nivel de organización se necesita el rol de **Organization Owner**. Para nivel enterprise, se necesita el rol de **Enterprise Owner**.

## Buenas y malas prácticas

**Hacer:**
- Asignar labels descriptivas y específicas (`produccion`, `gpu`, `docker`) — razón: permite seleccionar runners precisos y evita que jobs de staging corran en runners de producción.
- Instalar el runner como servicio del sistema con `./svc.sh` — razón: garantiza que el runner arranque automáticamente tras reinicios de la máquina.
- Usar un usuario dedicado sin privilegios para el servicio del runner — razón: limita el impacto si un job malicioso intenta escalar privilegios.
- Registrar runners a nivel de organización en lugar de por repositorio — razón: reduce la carga operativa de gestionar runners duplicados en múltiples repos.

**Evitar:**
- Ejecutar el runner como `root` o `Administrator` — razón: un job comprometido tendría control total sobre la máquina anfitriona.
- Reusar el token de registro expirado — razón: `config.sh` fallará con error de autenticación; los tokens de registro son de un solo uso y duran 1 hora.
- Omitir labels personalizadas en el registro — razón: sin labels específicas, cualquier job con `runs-on: self-hosted` puede ejecutarse en runners no diseñados para esa carga.

## Verificación y práctica

**Pregunta 1:** ¿Cuánto tiempo es válido un token de registro de self-hosted runner obtenido desde la UI?

Respuesta: **1 hora**. Tras ese tiempo caduca y debes generar uno nuevo desde Settings > Actions > Runners.

**Pregunta 2:** Un workflow especifica `runs-on: [self-hosted, linux, gpu]`. ¿Qué runner seleccionará GitHub Actions?

Respuesta: GitHub selecciona un runner disponible (estado idle) que tenga **las tres labels simultáneamente**: `self-hosted`, `linux` y `gpu`. Si ningún runner coincide, el job espera en cola.

**Pregunta 3:** ¿Cuál es la diferencia entre registrar un runner a nivel de repositorio versus a nivel de organización?

Respuesta: Un runner de repositorio solo puede ejecutar jobs de ese repositorio. Un runner de organización está disponible para todos los repositorios de la organización, y puede restringirse mediante runner groups.

**Ejercicio práctico:** Registra un runner a nivel de organización con las labels `self-hosted`, `linux`, `x64` y `staging`, instálalo como servicio y crea un workflow que ejecute `echo "Corriendo en staging"` únicamente en ese runner.

```yaml
# Solución: workflow que usa el runner de staging
name: Verificar runner staging

on: [push]

jobs:
  verificar:
    runs-on: [self-hosted, linux, staging]
    steps:
      - name: Confirmar entorno
        run: echo "Corriendo en staging"

      - name: Mostrar labels del runner
        run: |
          echo "Runner: $RUNNER_NAME"
          echo "Labels incluyen: staging"
```

---
← [4.6 Preinstalled Software](gha-d4-preinstalled-software.md) | [Índice](README.md) | [4.7.2 Self-Hosted Runners: seguridad](gha-d4-self-hosted-runners-seguridad.md) →

