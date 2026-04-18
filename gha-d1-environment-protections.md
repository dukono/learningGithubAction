# 1.19 Environment protections y deployment environments

[← 1.18 Badges de estado](gha-d1-badges.md) | [→ 1.20 Testing / Verificación de D1](gha-d1-testing.md)

---

## El problema: desplegar sin control en entornos críticos

Imagina un equipo que tiene un workflow que despliega a producción cada vez que se hace push a `main`. Un desarrollador hace merge de una PR un viernes por la tarde, el workflow arranca automáticamente y despliega código que nadie ha revisado manualmente. Si algo falla, el incidente ocurre fuera de horario, sin la persona adecuada disponible para revertir. Este escenario es real y evitable. GitHub Actions ofrece los **deployment environments** como mecanismo para introducir puntos de control entre el código y la ejecución de jobs de despliegue. Un environment puede requerir aprobación humana, esperar un tiempo mínimo, restringir qué ramas pueden desplegar y aislar sus propios secretos y variables.

---

## Crear y configurar un deployment environment en repo settings

Un deployment environment se crea desde la interfaz de GitHub: `Settings → Environments → New environment`. El nombre del environment es una cadena libre (convención habitual: `staging`, `production`, `preview`). Una vez creado, aparece en la lista y se puede configurar con protection rules. Los environments también se pueden crear implícitamente: si un job referencia `environment: production` y ese nombre no existe en la configuración del repo, GitHub lo crea vacío sin protecciones. Por eso es recomendable crearlos explícitamente antes de que los workflows los usen, así las protecciones están en su lugar desde el primer despliegue. Los environments son visibles en la pestaña principal del repositorio bajo "Deployments", donde queda un historial de cada ejecución asociada a cada environment.

---

## `required reviewers`: quién debe aprobar

La protection rule más importante es **required reviewers**. Permite designar hasta **6 usuarios o equipos** que deben aprobar el job antes de que este empiece a ejecutarse. Cuando un job llega a ese punto del workflow, GitHub envía notificaciones a los reviewers designados y el job queda en estado `waiting`. Cualquiera de los reviewers listados puede aprobar o rechazar. Si un reviewer rechaza, el job falla inmediatamente. Si nadie actúa y el job llega al timeout de espera (por defecto 30 días para environments), también falla. Un detalle importante: el actor que disparó el workflow (quien hizo el push o abrió la PR) **no puede aprobar su propio deployment** si está listado como reviewer; GitHub impide la auto-aprobación para preservar la separación de responsabilidades. Esta restricción aplica a usuarios individuales; los equipos no tienen esa limitación por defecto.

---

## `wait timer`: retraso mínimo antes de ejecutar

El **wait timer** es una protección complementaria a los reviewers. Permite configurar un número de minutos (entre 0 y 43200, es decir hasta 30 días) que el job debe esperar antes de ejecutarse, independientemente de si hay reviewers o no. Esto es útil en escenarios como: dar tiempo a que los sistemas de monitoreo detecten anomalías en staging antes de desplegar a producción, o cumplir con políticas internas que exigen un periodo de "soak time" entre builds. Si un environment tiene tanto wait timer como required reviewers, ambas condiciones deben cumplirse: primero pasa el tiempo configurado, luego (o simultáneamente) se espera la aprobación. En la práctica, el job queda en `waiting` durante el mayor de los dos tiempos.

---

## Deployment branches y tags rules

Las **deployment branches and tags rules** controlan qué ramas (o tags) del repositorio pueden desplegar a ese environment. Hay tres modos:

- **All branches**: cualquier rama puede desplegar. Útil para environments de desarrollo o preview.
- **Protected branches**: solo las ramas que tienen branch protection rules activas pueden desplegar. Garantiza que el código pasó por el proceso de revisión de PRs.
- **Selected branches and tags**: se especifican patrones explícitos, por ejemplo `main`, `release/*`, `v*.*.*`. Es el modo más restrictivo y recomendado para producción.

Si un job intenta desplegar a un environment desde una rama no permitida, el job falla con un error de policy antes de ejecutar ningún paso. Esto evita que ramas de feature o experimentos puedan impactar entornos críticos aunque el workflow esté configurado incorrectamente.

---

## Environment secrets: aislamiento y precedencia

Los **environment secrets** son secretos que solo están disponibles cuando un job referencia explícitamente ese environment. Son distintos de los repo secrets (disponibles en todos los jobs del repo) y de los org secrets (disponibles en repos de la organización según configuración). La precedencia es: **environment secret > repo secret > org secret**. Si existe un secreto llamado `API_KEY` en el environment `production` y otro `API_KEY` a nivel de repo, el job que corre en ese environment usará el del environment. Esto permite tener credenciales diferentes por entorno sin cambiar el nombre de los secretos en el YAML. Los environment secrets solo son accesibles cuando el job ha pasado las protection rules; un job que es rechazado por un reviewer nunca llega a ver esos secretos.

---

## Environment variables: distintas de repo variables

De forma análoga a los secretos, los **environment variables** son variables que están disponibles únicamente en jobs que referencian ese environment. Tienen la misma precedencia: **environment variable > repo variable > org variable**. La diferencia con los secretos es que las variables no están enmascaradas en los logs; son adecuadas para valores de configuración no sensibles como URLs de endpoints, feature flags o nombres de recursos cloud. Un caso de uso típico: la variable `DEPLOY_URL` apunta a `https://staging.example.com` en el environment `staging` y a `https://example.com` en el environment `production`, usando el mismo step de despliegue sin hardcodear URLs.

---

## Referenciar un environment en un job

Un job referencia su environment con la clave `environment` al nivel del job (no dentro de `steps`). Hay dos formas:

```yaml
# Forma simple: solo nombre
jobs:
  deploy:
    environment: production

# Forma extendida: nombre + URL de despliegue
jobs:
  deploy:
    environment:
      name: production
      url: https://example.com
```

La `url` es opcional pero recomendada: aparece como enlace clickable en la vista de Deployments de GitHub y en las notificaciones de aprobación, facilitando que el reviewer pueda verificar el estado antes de aprobar. El valor de `url` puede ser una expresión que use outputs de steps anteriores, por ejemplo la URL generada dinámicamente por un servicio cloud.

---

## Diagrama de flujo: job con environment protection

```
Workflow disparado
       |
       v
  Job build (sin environment) ---> ejecuta normalmente
       |
       v
  Job deploy-production
  environment: production
       |
       v
  ¿Rama permitida por deployment rule?
       |
      NO --> job falla (policy violation)
       |
      SI
       v
  ¿Wait timer configurado? (ej: 5 min)
       |
      SI --> espera 5 minutos
       |
       v
  ¿Required reviewers configurados?
       |
      SI --> job en estado "waiting"
             GitHub notifica a reviewers
                    |
           +---------+---------+
           |                   |
        Aprueba            Rechaza
           |                   |
           v                   v
     Job ejecuta         Job falla
     con environment     (deployment
     secrets y vars       rejected)
```

---

## Tabla de elementos clave de environment protection

| Elemento | Descripción | Límites / Notas |
|---|---|---|
| **required reviewers** | Usuarios o equipos que deben aprobar el deployment | Máximo 6 por environment |
| **wait timer** | Minutos de espera obligatoria antes de ejecutar | 0–43200 min (hasta 30 días) |
| **deployment branches: all** | Cualquier rama puede desplegar | Sin restricción de origen |
| **deployment branches: protected** | Solo ramas con branch protection rules | Requiere tener branch rules activas |
| **deployment branches: selected** | Patrones explícitos de ramas/tags | Soporta wildcards: `release/*`, `v*` |
| **auto-aprobación** | El autor del push no puede aprobar su propio deployment | Solo para usuarios individuales |
| **environment secrets** | Secretos exclusivos del environment | Mayor precedencia que repo y org secrets |
| **environment variables** | Variables exclusivas del environment | Mayor precedencia que repo y org variables |
| **deployment URL** | Enlace opcional visible en la UI de aprovación | Puede usar expresiones de steps |

---

## Buenas y malas prácticas

**Bien: crear el environment antes del primer uso**
Crear el environment en Settings antes de que el workflow lo referencie asegura que las protection rules estén activas desde el primer despliegue. Si el environment se crea implícitamente por el workflow, el primer job corre sin protecciones.

**Mal: crear el environment implícitamente y añadir protecciones después**
Si el workflow corre antes de que el ambiente tenga protecciones, hay una ventana de tiempo durante la cual un despliegue no controlado puede llegar a producción.

---

**Bien: usar environment secrets para credenciales distintas por entorno**
Definir `DB_PASSWORD` como environment secret con valores distintos en `staging` y `production` garantiza que el código de staging nunca usa credenciales de producción, aunque el YAML sea idéntico.

**Mal: usar un solo repo secret con el valor de producción**
Si el mismo secreto se usa para todos los entornos, un error en staging puede impactar recursos de producción o exponer credenciales críticas en logs de entornos menos seguros.

---

**Bien: especificar `url` en el environment del job**
Incluir `url: https://example.com` hace que los reviewers vean un enlace directo al entorno desplegado en la notificación de aprobación, facilitando la verificación antes de aprobar.

**Mal: omitir la URL siempre**
Sin URL, el reviewer debe buscar manualmente dónde está el deployment para verificarlo, añadiendo fricción al proceso de aprobación y aumentando la probabilidad de aprobaciones sin revisión real.

---

**Bien: usar "selected branches" con el patrón `main` en production**
Restringir el environment de producción a deploys desde `main` evita que ramas de feature o hotfixes no revisados lleguen a producción accidentalmente.

**Mal: dejar "all branches" en el environment de producción**
Cualquier rama puede desplegar a producción si el job se dispara. Un desarrollador que hace push a una rama de experimento puede desencadenar un deployment de producción si el workflow está mal configurado.

---

## Ejemplo central: staging sin aprobación, production con aprobación

```yaml
name: Deploy Application

on:
  push:
    branches:
      - main

jobs:
  build:
    runs-on: ubuntu-latest
    outputs:
      image-tag: ${{ steps.meta.outputs.tags }}
    steps:
      - uses: actions/checkout@v4
      - name: Build Docker image
        id: meta
        run: |
          TAG="sha-${GITHUB_SHA::7}"
          echo "tags=$TAG" >> $GITHUB_OUTPUT
          echo "Building image with tag $TAG"

  deploy-staging:
    needs: build
    runs-on: ubuntu-latest
    environment:
      name: staging
      url: https://staging.example.com
    steps:
      - name: Deploy to staging
        env:
          DEPLOY_TOKEN: ${{ secrets.DEPLOY_TOKEN }}
          API_ENDPOINT: ${{ vars.API_ENDPOINT }}
        run: |
          echo "Deploying ${{ needs.build.outputs.image-tag }} to staging"
          echo "API endpoint: $API_ENDPOINT"
          # ./deploy.sh --env staging --tag ${{ needs.build.outputs.image-tag }}

  deploy-production:
    needs: deploy-staging
    runs-on: ubuntu-latest
    environment:
      name: production
      url: https://example.com
    steps:
      - name: Deploy to production
        env:
          DEPLOY_TOKEN: ${{ secrets.DEPLOY_TOKEN }}
          API_ENDPOINT: ${{ vars.API_ENDPOINT }}
        run: |
          echo "Deploying ${{ needs.build.outputs.image-tag }} to production"
          echo "API endpoint: $API_ENDPOINT"
          # ./deploy.sh --env production --tag ${{ needs.build.outputs.image-tag }}
```

En este ejemplo, `DEPLOY_TOKEN` y `API_ENDPOINT` tienen valores distintos en cada environment. El job `deploy-production` no arrancará hasta que un reviewer designado en la configuración del environment `production` apruebe el deployment. El reviewer verá el enlace `https://example.com` en la notificación.

Notar que `needs.build.outputs.image-tag` pasa el tag construido en el primer job a los jobs de despliegue. El job `deploy-staging` tampoco requiere aprobación porque el environment `staging` no tiene required reviewers configurados; arranca automáticamente cuando `build` termina con éxito. Solo `deploy-production` tiene la protection rule activa.

---

## Verificacion

**Pregunta 1 (GH-200)**
Un job referencia `environment: production`. El environment tiene `required reviewers: [alice, bob]` y `wait timer: 10`. El desarrollador `alice` hace push a `main` y el workflow se dispara. ¿Quién puede aprobar el deployment?

a) Alice o Bob
b) Solo Bob, porque Alice es la autora del push
c) Cualquier miembro del repositorio
d) Solo Alice porque es la autora

Respuesta correcta: **b**. GitHub impide que el actor que disparó el workflow apruebe su propio deployment cuando está listado como reviewer. Solo Bob puede aprobar.

---

**Pregunta 2 (GH-200)**
Un repo tiene un secret `DB_PASSWORD` a nivel de repo con el valor `repo-pass` y un secret `DB_PASSWORD` en el environment `production` con el valor `prod-pass`. Un job que corre con `environment: production` accede a `${{ secrets.DB_PASSWORD }}`. ¿Qué valor obtiene?

a) `repo-pass`, porque el repo secret tiene precedencia
b) `prod-pass`, porque el environment secret tiene mayor precedencia
c) Ambos valores concatenados
d) Error: no se puede tener el mismo nombre en dos niveles

Respuesta correcta: **b**. Los environment secrets tienen mayor precedencia que los repo secrets. El job recibe `prod-pass`.

---

**Pregunta 3 (GH-200)**
¿Cuál es el número máximo de usuarios o equipos que se pueden configurar como required reviewers en un deployment environment?

a) 2
b) 4
c) 6
d) 10

Respuesta correcta: **c**. El límite es 6 reviewers (usuarios o equipos combinados) por environment.

---

**Ejercicio practico**

Configurar en un repositorio de prueba:

1. Crear dos environments: `staging` (sin protecciones) y `production` (con required reviewer: tu usuario, wait timer: 1 minuto, deployment branches: solo `main`).

2. Crear el siguiente workflow en `.github/workflows/deploy.yml`:

```yaml
name: Practice Deploy

on:
  workflow_dispatch:

jobs:
  deploy-staging:
    runs-on: ubuntu-latest
    environment:
      name: staging
      url: https://staging.example.com
    steps:
      - run: echo "Deployed to staging"

  deploy-production:
    needs: deploy-staging
    runs-on: ubuntu-latest
    environment:
      name: production
      url: https://example.com
    steps:
      - run: echo "Deployed to production"
```

3. Disparar el workflow manualmente desde la rama `main` con "Run workflow".

4. Observar que `deploy-staging` arranca inmediatamente, mientras que `deploy-production` queda en estado `waiting`. Verificar que aparece la notificación de aprobación.

5. Esperar el minuto de wait timer y luego aprobar el deployment. Observar que el job pasa a ejecutarse.

6. Crear una rama `feature/test` y dispara el workflow desde esa rama. Verificar que `deploy-production` falla con un error de policy (branch not allowed) sin llegar a mostrar la solicitud de aprobacion.

---

[← 1.18 Badges de estado](gha-d1-badges.md) | [→ 1.20 Testing / Verificación de D1](gha-d1-testing.md)
