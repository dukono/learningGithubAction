# 3.7 Publicación en el GitHub Marketplace y release strategies

← [3.6 Distribución de actions](gha-action-distribucion.md) | [Índice](README.md) | [3.8 Testing / Verificación de D3](gha-d3-testing.md) →

---

Publicar una action en el Marketplace y gestionar su ciclo de vida con una estrategia de versionado adecuada son dos operaciones que se realizan juntas: cada nueva versión implica crear un release en GitHub, y ese release es el mecanismo tanto para actualizar la action en el Marketplace como para mover los tags de versión. Este fichero asume conocimiento de version pinning ([3.5](gha-action-version-pinning.md)).

## Ciclo de vida de publicación

El siguiente diagrama muestra el flujo desde el repositorio hasta el Marketplace y las actualizaciones:

```
repo público + action.yml con branding
        │
        ▼
  Crear GitHub Release (tag v1.0.0)
        │
        ├──► Opción "Publish to Marketplace" → action visible en marketplace.github.com
        │
        ▼
  Nueva versión (v1.1.0)
        │
        ├──► Nuevo release → Marketplace se actualiza automáticamente
        └──► git tag -fa v1 → floating major tag actualizado
```

## Requisitos para publicar en el Marketplace

Para que GitHub permita publicar una action en el Marketplace deben cumplirse tres requisitos simultáneamente: el repositorio debe ser público, el fichero `action.yml` debe estar en la raíz del repositorio, y `action.yml` debe incluir el bloque `branding:` con `icon` y `color` válidos. Sin cualquiera de estos tres, GitHub no muestra la opción de publicación al crear el release.

> [EXAMEN] Los tres requisitos obligatorios para el Marketplace: (1) repo público, (2) `action.yml` en raíz, (3) `branding` con icon y color. La ausencia de cualquiera bloquea la publicación.

## Proceso de publicación

El proceso de publicación ocurre al crear un GitHub Release. Al crear el release desde la UI de GitHub (o via API), aparece la casilla "Publish this action to the GitHub Marketplace". Al marcarla y publicar el release, la action queda disponible en el Marketplace con los metadatos de `action.yml` y el contenido del `README.md` como documentación.

## Categorías del Marketplace

Al publicar, GitHub solicita asignar una o más categorías a la action (por ejemplo: "Continuous integration", "Deployment", "Testing"). La categoría correcta mejora la visibilidad en las búsquedas del Marketplace. No afecta al funcionamiento de la action.

## Badge de verificación

GitHub otorga un badge de verificación a las actions publicadas por organizaciones que han verificado su identidad en GitHub. El badge indica que la organización es quien dice ser, no que la action sea segura o libre de vulnerabilidades. Los consumidores deben seguir auditando el código independientemente del badge.

## Actualizar una action publicada

Cada nuevo GitHub Release actualiza automáticamente la página de la action en el Marketplace. No es necesario volver a marcar la casilla de publicación: una vez que la action está publicada, todos los releases futuros del mismo repositorio la actualizan.

## Retirar una action del Marketplace

Para retirar una action del Marketplace se accede a la configuración del repositorio > "GitHub Marketplace" y se selecciona "Remove from Marketplace". Esto no elimina el repositorio ni los tags; solo deja de aparecer en el Marketplace. Los workflows que ya la usan siguen funcionando.

## README.md como documentación del Marketplace

El contenido del `README.md` del repositorio es lo que GitHub muestra en la página de la action en el Marketplace. Un README bien estructurado con ejemplos de uso, tabla de inputs/outputs y requisitos es parte de la distribución de la action, no solo documentación interna.

## Semantic Versioning aplicado a actions

El versionado semántico (SemVer) define tres niveles de cambio: MAJOR (cambios que rompen compatibilidad), MINOR (nuevas funcionalidades compatibles hacia atrás) y PATCH (correcciones de bugs). Aplicado a actions: un cambio en los inputs requeridos es MAJOR; añadir un input opcional es MINOR; corregir un bug sin cambiar la interfaz es PATCH.

> [CONCEPTO] SemVer en actions no es solo convención: los consumidores que usan `@v1` (floating major tag) esperan que nunca reciban un cambio que rompa su workflow. Un cambio MAJOR debe publicarse como `v2`, nunca como un nuevo patch de `v1`.

## Estrategia de floating major tag

La estrategia de floating major tag consiste en mantener un tag `v1` que siempre apunta al último commit de la serie major 1. Cuando se publica `v1.2.3`, el mantenedor mueve el tag `v1` para que apunte al mismo commit. Los consumidores que usan `@v1` reciben automáticamente todos los patches y minors sin cambiar su workflow.

```bash
# Publicar v1.2.3 y actualizar el floating tag v1
git tag v1.2.3
git push origin v1.2.3
git tag -fa v1 -m "v1.2.3"
git push origin v1 --force
```

## Estrategia immutable

La estrategia immutable crea un tag único por cada release (`v1.0.0`, `v1.0.1`, `v1.1.0`) y nunca mueve tags anteriores. Los consumidores deben actualizar explícitamente su workflow para adoptar la nueva versión. Esta estrategia prioriza la reproducibilidad sobre la comodidad.

## Cuándo usar cada estrategia

La elección entre floating y immutable depende del contrato con los consumidores: si la action es de uso amplio y se quiere que los usuarios reciban bugfixes automáticamente, floating major tag es la opción estándar (es la estrategia que usan `actions/checkout`, `actions/setup-node`, etc.). Si la action es crítica para infraestructura y se prefiere que ningún cambio ocurra sin aprobación explícita, immutable es la opción correcta.

## GitHub Releases vs. git tags

Un git tag es un puntero a un commit en el historial de Git. Un GitHub Release es una entidad de la plataforma GitHub que envuelve un tag y añade metadatos: título, descripción de cambios (changelog), assets adjuntos. Para publicar en el Marketplace se requiere un GitHub Release, no solo un tag. Al crear el release, GitHub crea el tag automáticamente si no existe.

## Comunicar breaking changes

Cuando una nueva versión introduce cambios incompatibles (MAJOR), el mantenedor debe crear el nuevo major tag (`v2`) y mantener `v1` funcional durante un período de transición. El CHANGELOG y las release notes deben documentar claramente qué cambió y cómo migrar.

## Deprecación de versiones antiguas

Para deprecar una versión, el mantenedor puede: añadir un `::warning::` en el código de la action que avise a los consumidores al ejecutarse, actualizar el README con un aviso de deprecación, y publicar un issue o discussion en el repositorio anunciando la fecha de fin de soporte.

## Ejemplo central

El siguiente `action.yml` muestra el branding completo requerido para el Marketplace, seguido de los comandos git del ciclo completo de release:

```yaml
# action.yml — listo para publicar en el Marketplace
name: Deploy to Cloud
description: Despliega la aplicación al proveedor cloud configurado
author: "Mi Organización"

branding:
  icon: upload-cloud
  color: blue

inputs:
  environment:
    description: Entorno destino (staging, production)
    required: true
  version:
    description: Versión a desplegar
    required: false
    default: "latest"

outputs:
  deployment-url:
    description: URL del despliegue realizado
    value: ${{ steps.deploy.outputs.url }}

runs:
  using: composite
  steps:
    - name: Ejecutar despliegue
      id: deploy
      shell: bash
      run: |
        echo "Desplegando ${{ inputs.version }} en ${{ inputs.environment }}"
        echo "url=https://${{ inputs.environment }}.ejemplo.com" >> $GITHUB_OUTPUT
```

```bash
# Ciclo completo de release v1.0.0 (primera publicación)
git tag v1.0.0
git push origin v1.0.0
git tag -fa v1 -m "v1.0.0 — primera release"
git push origin v1 --force
# En GitHub UI: Releases > Draft new release > tag v1.0.0 > ☑ Publish to Marketplace

# Publicar patch v1.0.1 (bugfix)
git tag v1.0.1
git push origin v1.0.1
git tag -fa v1 -m "v1.0.1"
git push origin v1 --force
# En GitHub UI: nuevo release con tag v1.0.1 → Marketplace se actualiza solo

# Publicar v2.0.0 (breaking change)
git tag v2.0.0
git push origin v2.0.0
git tag -fa v2 -m "v2.0.0 — breaking: input 'environment' ahora es requerido"
git push origin v2 --force
# v1 sigue existiendo para consumidores que no han migrado
```

## Tabla de elementos clave

| Elemento | Obligatorio para Marketplace | Descripción |
|----------|:---------------------------:|-------------|
| Repo público | Sí | El repositorio debe ser público |
| `action.yml` en raíz | Sí | El fichero debe estar en la raíz, no en subdirectorio |
| `branding.icon` | Sí | Nombre de icono Feather Icons válido |
| `branding.color` | Sí | Uno de: white, yellow, blue, green, orange, red, purple, gray-dark |
| GitHub Release | Sí | El tag debe ser un Release, no solo un tag de git |
| `name` y `description` | Sí | Aparecen en la página del Marketplace |
| `README.md` | No (recomendado) | Contenido visible en la página del Marketplace |

## Buenas y malas prácticas

**Hacer:**
- **Mantener el floating major tag (`v1`) actualizado en cada patch** — razón: los consumidores que usan `@v1` esperan recibir bugfixes automáticamente; si el tag no se mueve, siguen con la versión antigua.
- **Documentar breaking changes con major version bump** — razón: publicar un cambio incompatible como minor o patch rompe los workflows de todos los consumidores que usan el floating tag sin previo aviso.
- **Escribir el README antes de publicar en el Marketplace** — razón: es la documentación visible en la página de la action; sin ella, los consumidores no saben cómo usarla.

**Evitar:**
- **Mover el floating tag `v1` cuando hay un cambio MAJOR** — razón: los consumidores de `@v1` verían su workflow roto sin haber cambiado nada en su código.
- **Publicar sin branding** — razón: GitHub bloquea la publicación si faltan `icon` o `color` en el bloque `branding`.
- **Eliminar tags de versiones anteriores** — razón: los workflows que pinen a esos tags fallarán con "reference not found".

## Comparación: floating major tag vs. immutable

| Criterio | Floating (`v1`) | Immutable (`v1.0.0`) |
|----------|:--------------:|:-------------------:|
| El consumidor recibe updates automáticos | Sí | No |
| Reproducibilidad garantizada | No | Sí |
| Riesgo de rotura silenciosa | Medio | Ninguno |
| Adopción de bugfixes | Automática | Manual |
| Estrategia usada por actions/ oficiales | Sí | No |
| Recomendado para entornos críticos | No | Sí (junto con SHA) |

## Verificación y práctica

### Preguntas de examen

**Pregunta 1.** ¿Cuál de estos requisitos NO es obligatorio para publicar una action en el GitHub Marketplace?

- A) El repositorio debe ser público
- B) `action.yml` debe estar en la raíz del repositorio
- **C) La action debe tener al menos un input declarado** ✅
- D) `action.yml` debe incluir el bloque `branding` con icon y color

*C es la respuesta correcta*: los inputs son opcionales. *A, B y D son obligatorios* para la publicación en el Marketplace.

---

**Pregunta 2.** Un mantenedor publica `v1.2.0` y quiere que los usuarios de `@v1` la reciban automáticamente. ¿Qué comando mueve el floating tag?

- A) `git tag v1 v1.2.0 && git push origin v1`
- **B) `git tag -fa v1 -m "v1.2.0" && git push origin v1 --force`** ✅
- C) `git push origin v1.2.0:v1`
- D) GitHub mueve el tag automáticamente al crear el release

*A es incorrecta*: sintaxis inválida para mover un tag existente. *C es incorrecta*: esa sintaxis no mueve tags. *D es incorrecta*: GitHub nunca mueve tags automáticamente.

---

**Ejercicio práctico.** Escribe el bloque `branding` de `action.yml` para una action de testing que use el icono `check-circle` en color verde, y los comandos git para publicar `v2.0.0` como primera release de la major version 2:

```yaml
# En action.yml
branding:
  icon: check-circle
  color: green
```

```bash
# Publicar v2.0.0 con floating major tag v2
git tag v2.0.0
git push origin v2.0.0
git tag -fa v2 -m "v2.0.0"
git push origin v2 --force
# En GitHub: crear Release con tag v2.0.0 → marcar "Publish to Marketplace"
```

---

← [3.6 Distribución de actions](gha-action-distribucion.md) | [Índice](README.md) | [3.8 Testing / Verificación de D3](gha-d3-testing.md) →
