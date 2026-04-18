# [4.9.1] Secrets: Niveles, Precedencia, Visibilidad y Gestión via UI

← [4.8 ARC y Scale Sets](gha-d4-arc-scale-sets.md) | [Índice](README.md) | [4.9.2 Secrets via REST API](gha-d4-secrets-api.md) →

---

Los secrets en GitHub Actions permiten almacenar credenciales, tokens y cualquier dato sensible de forma cifrada. GitHub los enmascara automáticamente en los logs de ejecución. Existen cuatro niveles de alcance: repositorio, entorno (environment), organización y empresa (enterprise), cada uno con su propia jerarquía de precedencia.

## Niveles de Secrets

GitHub Actions organiza los secrets en una jerarquía de cuatro niveles. Cada nivel tiene un alcance diferente y determina qué workflows y jobs pueden acceder al secret.

| Nivel | Alcance | Configuración |
|-------|---------|---------------|
| Repositorio | Solo el repo donde se define | Settings > Secrets and variables > Actions |
| Environment | Solo jobs que usan ese environment | Settings > Environments > [env] > Secrets |
| Organización | Repos autorizados de la org | Org Settings > Secrets and variables > Actions |
| Enterprise | Toda la empresa (solo admins) | Enterprise Settings > Secrets |

> [CONCEPTO] Los secrets de tipo **environment** solo están disponibles cuando el job declara `environment: nombre-env`. Un job sin `environment` no puede acceder a esos secrets aunque esté en el mismo repositorio.

## Precedencia de Secrets

Cuando un mismo nombre de secret existe en múltiples niveles, GitHub aplica una regla de precedencia estricta. El nivel más específico siempre gana sobre el más general.

La precedencia de mayor a menor es:

1. **Environment** (más específico — sobreescribe todo)
2. **Repository** (sobreescribe org)
3. **Organization** (nivel más general)

> [EXAMEN] Si `DB_PASSWORD` existe en el environment `production` y también en el repositorio, el job que use `environment: production` verá el valor del environment. Un job sin environment ve el valor del repositorio.

## Crear Secret en Repositorio

Para crear un secret a nivel de repositorio se accede a la configuración del repo. El proceso es sencillo y no requiere permisos especiales más allá de ser administrador del repositorio.

Pasos en la UI:

1. Ir a **Settings** del repositorio
2. Seleccionar **Secrets and variables** en el menú lateral
3. Hacer clic en **Actions**
4. Pulsar **New repository secret**
5. Introducir el nombre (sin espacios, sin prefijo `GITHUB_`) y el valor
6. Confirmar con **Add secret**

> [ADVERTENCIA] Una vez guardado, el valor del secret **nunca es visible** en la UI. Solo se puede sobreescribir o eliminar. Si pierdes el valor, debes recrear el secret.

## Crear Secret en Environment

Los secrets de environment añaden una capa adicional de seguridad al vincular el secret a un entorno específico. Esto permite usar el mismo nombre de secret con valores diferentes en producción y staging.

Pasos en la UI:

1. Ir a **Settings** del repositorio
2. Seleccionar **Environments**
3. Hacer clic en el environment deseado (o crear uno nuevo)
4. En la sección **Environment secrets**, pulsar **Add secret**
5. Introducir nombre y valor

## Crear Secret en Organización

Los secrets de organización permiten compartir credenciales entre múltiples repositorios sin duplicarlos. Al crearlos, se define una política de visibilidad que controla qué repos pueden usar el secret.

Políticas de visibilidad disponibles:

| Política | Descripción |
|----------|-------------|
| All repositories | Todos los repos de la org |
| Private repositories | Solo repos privados e internos |
| Selected repositories | Lista explícita de repos autorizados |

Pasos: **Org Settings** > **Secrets and variables** > **Actions** > **New organization secret**

## Ejemplo Central

El siguiente workflow demuestra el uso de secrets en tres niveles distintos: un secret de organización para credenciales compartidas, un secret de repositorio para configuración específica, y un secret de environment para credenciales de despliegue.

```yaml
name: Deploy con Secrets por Niveles

on:
  push:
    branches: [main]

jobs:
  build:
    runs-on: ubuntu-latest
    # Este job accede a secrets de repo y org (no de environment)
    steps:
      - uses: actions/checkout@v4

      - name: Autenticar con registro de contenedores
        # REGISTRY_TOKEN es un secret de organización
        run: |
          echo "${{ secrets.REGISTRY_TOKEN }}" | docker login ghcr.io -u ${{ github.actor }} --password-stdin

      - name: Build imagen
        # BUILD_ARGS es un secret de repositorio
        run: |
          docker build \
            --build-arg API_KEY="${{ secrets.BUILD_ARGS }}" \
            -t ghcr.io/${{ github.repository }}:${{ github.sha }} .

      - name: Push imagen
        run: docker push ghcr.io/${{ github.repository }}:${{ github.sha }}

  deploy-staging:
    needs: build
    runs-on: ubuntu-latest
    environment: staging
    # Este job accede a secrets del environment 'staging'
    # Si DB_PASSWORD existe en 'staging', ese valor tiene precedencia
    # sobre cualquier DB_PASSWORD de repo u org
    steps:
      - name: Deploy a staging
        run: |
          echo "Desplegando en staging..."
          # DEPLOY_KEY viene del environment 'staging'
          curl -X POST https://api.staging.example.com/deploy \
            -H "Authorization: Bearer ${{ secrets.DEPLOY_KEY }}" \
            -d '{"image": "ghcr.io/${{ github.repository }}:${{ github.sha }}"}'

  deploy-production:
    needs: deploy-staging
    runs-on: ubuntu-latest
    environment: production
    # DEPLOY_KEY aquí tiene el valor del environment 'production'
    # diferente al de 'staging', aunque comparten el mismo nombre
    steps:
      - name: Deploy a producción
        run: |
          echo "Desplegando en producción..."
          curl -X POST https://api.production.example.com/deploy \
            -H "Authorization: Bearer ${{ secrets.DEPLOY_KEY }}" \
            -d '{"image": "ghcr.io/${{ github.repository }}:${{ github.sha }}"}'
```

## Tabla de Elementos Clave

La siguiente tabla resume las propiedades y restricciones más importantes para trabajar correctamente con secrets en GitHub Actions.

| Parámetro | Tipo | Obligatorio | Default | Descripción |
|-----------|------|-------------|---------|-------------|
| Nombre | String | Sí | — | Alfanumérico y guiones bajos; sin espacios; sin prefijo `GITHUB_` |
| Valor | String | Sí | — | Máximo 64 KB por secret |
| Nivel | Enum | Sí | — | repo / environment / org / enterprise |
| Política org | Enum | Solo en org | — | All / Private / Selected repositories |
| Referencia | Expresión | Sí | — | `${{ secrets.NOMBRE }}` |

> [CONCEPTO] Los nombres de secrets son **case-insensitive** al resolverse, pero GitHub los almacena en mayúsculas por convención. Usar `secrets.mi_token` y `secrets.MI_TOKEN` accede al mismo secret.

## Secret Masking en Logs

GitHub Actions enmascara automáticamente los valores de los secrets en los logs de ejecución. Cualquier aparición del valor en la salida estándar se reemplaza por `***`. Este comportamiento es automático y no requiere configuración adicional.

Sin embargo, existen situaciones donde el masking puede fallar. Si el valor del secret se transforma (por ejemplo, codificado en base64 o dividido en múltiples variables), GitHub no reconoce el valor transformado como secret y no lo enmascara.

> [ADVERTENCIA] Si haces `echo "${{ secrets.TOKEN }}" | base64`, el valor codificado en base64 **no** será enmascarado en los logs. Nunca imprimas ni transformes secrets en pasos de debug.

## Buenas y Malas Prácticas

**Hacer:**
- Usar secrets de environment para credenciales de producción — razón: obliga a declarar `environment:` en el job, lo que puede requerir aprobación manual antes de ejecutarse.
- Usar secrets de organización para tokens compartidos entre repos — razón: evita duplicar el mismo token en decenas de repositorios y facilita la rotación centralizada.
- Rotar secrets periódicamente — razón: limita el impacto en caso de que un secret sea comprometido sin que se detecte.

**Evitar:**
- Imprimir secrets en pasos de debug con `echo` — razón: aunque GitHub intenta enmascarar, las transformaciones del valor pueden exponer datos sensibles en los logs.
- Usar el mismo secret para múltiples entornos con permisos distintos — razón: si el secret se filtra, compromete todos los entornos a la vez.
- Nombrar secrets con el prefijo `GITHUB_` — razón: este prefijo está reservado para variables de entorno del sistema de GitHub y puede causar comportamientos inesperados.

## Verificación y Práctica

**Pregunta 1:** Un workflow tiene un secret `API_KEY` definido a nivel de repositorio y otro `API_KEY` definido en el environment `production`. El job declara `environment: production`. ¿Qué valor se usa?

**Respuesta:** Se usa el valor del environment `production`. La precedencia es environment > repository > organization. El secret más específico siempre tiene prioridad.

---

**Pregunta 2:** ¿Qué ocurre si un job intenta usar `${{ secrets.ENV_SECRET }}` donde `ENV_SECRET` es un secret del environment `staging`, pero el job no declara `environment: staging`?

**Respuesta:** El secret se resuelve como una cadena vacía. Los secrets de environment solo son inyectados en jobs que declaran explícitamente ese environment con la clave `environment:`.

---

**Pregunta 3:** ¿Cuál es el tamaño máximo de un secret en GitHub Actions y qué restricciones tiene el nombre?

**Respuesta:** El valor máximo es 64 KB. El nombre no puede contener espacios, no puede comenzar con el prefijo `GITHUB_`, y debe usar solo caracteres alfanuméricos y guiones bajos.

---

**Ejercicio:** Configura un workflow que use un secret de repositorio `DOCKER_PASSWORD` para hacer login en Docker Hub en el job de build, y un secret de environment `KUBE_TOKEN` en el environment `production` para el job de deploy. El deploy solo debe ejecutarse si el build tiene éxito.

```yaml
name: Build y Deploy con Secrets

on:
  push:
    branches: [main]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Login Docker Hub
        run: |
          echo "${{ secrets.DOCKER_PASSWORD }}" | \
            docker login -u ${{ secrets.DOCKER_USERNAME }} --password-stdin

      - name: Build y push
        run: |
          docker build -t myuser/myapp:${{ github.sha }} .
          docker push myuser/myapp:${{ github.sha }}

  deploy:
    needs: build
    runs-on: ubuntu-latest
    environment: production
    steps:
      - name: Deploy a producción
        run: |
          kubectl --token="${{ secrets.KUBE_TOKEN }}" \
            set image deployment/myapp \
            myapp=myuser/myapp:${{ github.sha }}
```

---

← [4.8 ARC y Scale Sets](gha-d4-arc-scale-sets.md) | [Índice](README.md) | [4.9.2 Secrets via REST API](gha-d4-secrets-api.md) →
