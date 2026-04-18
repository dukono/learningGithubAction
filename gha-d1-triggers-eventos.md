# 1.4 Triggers de eventos del repositorio

[← 1.3 Triggers de automatización](gha-d1-triggers-automatizacion.md) | [1.5.1 Propiedades de identidad del job →](gha-d1-jobs-identidad.md)

---

## Introduccion

Los workflows no solo reaccionan a cambios de codigo. GitHub Actions permite disparar automatizaciones en respuesta a la actividad del repositorio: cuando alguien abre un issue, publica una release, crea una rama, o incluso cuando un sistema externo envia una senal personalizada via API. Este grupo de eventos extiende la automatizacion mas alla del ciclo de vida del codigo y hacia el ciclo de vida del proyecto.

---

## Tabla de eventos y types disponibles

| Evento             | Types disponibles                                                                 | Uso tipico                              |
|--------------------|-----------------------------------------------------------------------------------|-----------------------------------------|
| `repository_dispatch` | Definidos por el usuario                                                       | Integraciones externas, webhooks propios |
| `release`          | `published`, `created`, `released`, `prereleased`, `edited`, `deleted`, `unpublished` | Deploy, notificaciones de version       |
| `issues`           | `opened`, `edited`, `deleted`, `labeled`, `unlabeled`, `closed`, `reopened`, `assigned`, `unassigned`, `milestoned`, `demilestoned`, `transferred`, `pinned`, `unpinned`, `locked`, `unlocked` | Triaje, automatizacion de soporte       |
| `issue_comment`    | `created`, `edited`, `deleted`                                                    | Bots de comandos, notificaciones        |
| `create`           | _(sin types, aplica a branches y tags)_                                           | Inicializar entornos por rama           |
| `delete`           | _(sin types, aplica a branches y tags)_                                           | Limpieza de entornos, notificaciones    |

---

## `repository_dispatch`: eventos personalizados via API

El evento `repository_dispatch` permite que sistemas externos activen un workflow enviando una peticion HTTP autenticada al repositorio. Se define con `types` propios, lo que evita colisiones entre distintas integraciones.

Para dispararlo desde la API REST de GitHub se utiliza el endpoint `POST /repos/{owner}/{repo}/dispatches` con un cuerpo JSON que incluye `event_type` y opcionalmente `client_payload`:

```bash
curl -X POST \
  -H "Authorization: Bearer $TOKEN" \
  -H "Accept: application/vnd.github+json" \
  https://api.github.com/repos/mi-org/mi-repo/dispatches \
  -d '{"event_type":"deploy-produccion","client_payload":{"version":"2.1.0","entorno":"prod"}}'
```

El `client_payload` es un objeto JSON libre (hasta 10 niveles de profundidad) que queda disponible en el contexto del workflow bajo `github.event.client_payload`.

```yaml
# .github/workflows/deploy-externo.yml
name: Deploy desde sistema externo

on:
  repository_dispatch:
    types: [deploy-produccion, deploy-staging]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - name: Mostrar version a desplegar
        run: |
          echo "Version: ${{ github.event.client_payload.version }}"
          echo "Entorno: ${{ github.event.client_payload.entorno }}"

      - name: Checkout en la version indicada
        uses: actions/checkout@v4

      - name: Deploy condicional por entorno
        if: github.event.client_payload.entorno == 'prod'
        run: ./scripts/deploy-prod.sh "${{ github.event.client_payload.version }}"
```

El token usado en la llamada API debe tener el scope `repo` para repositorios privados o `public_repo` para publicos.

---

## `release`: reaccionar al ciclo de vida de versiones

El evento `release` se activa cuando se realizan acciones sobre releases del repositorio. El type mas usado en produccion es `published`, que se dispara cuando una release (incluidas pre-releases marcadas como publicadas) queda visible publicamente.

La distincion entre types es importante: `created` se activa al guardar un borrador, `released` solo cuando se desmarca la opcion "pre-release", y `prereleased` cuando se marca como pre-release. Usar el type incorrecto puede provocar deploys no deseados a produccion.

```yaml
on:
  release:
    types: [published]

jobs:
  publicar-paquete:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Obtener tag de la release
        run: echo "TAG=${{ github.event.release.tag_name }}" >> $GITHUB_ENV
      - name: Publicar en registry
        run: npm publish --tag ${{ github.event.release.tag_name }}
```

Campos utiles del payload: `github.event.release.tag_name`, `github.event.release.name`, `github.event.release.body`, `github.event.release.prerelease`, `github.event.release.html_url`.

---

## `issues`: automatizacion sobre issues

El evento `issues` cubre todo el ciclo de vida de un issue. El type `opened` es el mas frecuente para automatizaciones de triaje; `labeled` permite reaccionar cuando se asigna una etiqueta especifica.

```yaml
on:
  issues:
    types: [opened, labeled]

jobs:
  triaje:
    runs-on: ubuntu-latest
    if: github.event.action == 'labeled' && github.event.label.name == 'bug'
    steps:
      - name: Asignar issue a equipo de bugs
        uses: actions/github-script@v7
        with:
          script: |
            github.rest.issues.addAssignees({
              owner: context.repo.owner,
              repo: context.repo.repo,
              issue_number: context.issue.number,
              assignees: ['equipo-bugs']
            })
```

Campos utiles: `github.event.issue.number`, `github.event.issue.title`, `github.event.issue.user.login`, `github.event.issue.labels`, `github.event.label.name` (solo en tipo `labeled`/`unlabeled`).

---

## `issue_comment`: reaccionar a comentarios

El evento `issue_comment` se activa para comentarios tanto en issues como en pull requests. Distingue tres types: `created`, `edited` y `deleted`.

Un patron comun es implementar bots de comandos que leen el cuerpo del comentario y ejecutan acciones:

```yaml
on:
  issue_comment:
    types: [created]

jobs:
  comando-bot:
    runs-on: ubuntu-latest
    if: github.event.comment.body == '/deploy-preview'
    steps:
      - name: Crear deploy de preview
        run: echo "Desplegando preview para issue #${{ github.event.issue.number }}"
```

Campos utiles: `github.event.comment.body`, `github.event.comment.user.login`, `github.event.issue.number`, `github.event.issue.pull_request` (presente si el comentario es en un PR).

---

## `create` y `delete`: branches y tags

Los eventos `create` y `delete` no admiten filtros por `types`; se activan para cualquier rama o tag creado o eliminado. Para distinguir entre ramas y tags se usa `github.event.ref_type` (`branch` o `tag`) y `github.event.ref` para el nombre.

```yaml
on:
  create:
  delete:

jobs:
  gestionar-entorno:
    runs-on: ubuntu-latest
    steps:
      - name: Crear entorno al crear rama de feature
        if: github.event_name == 'create' && github.event.ref_type == 'branch' && startsWith(github.event.ref, 'feature/')
        run: ./scripts/crear-entorno.sh "${{ github.event.ref }}"

      - name: Eliminar entorno al borrar rama
        if: github.event_name == 'delete' && github.event.ref_type == 'branch'
        run: ./scripts/eliminar-entorno.sh "${{ github.event.ref }}"
```

Nota: el evento `create` no se activa en el primer push al repositorio. Para detectar la creacion del repositorio se usa el evento `push` con condicion sobre `github.ref`.

---

## Acceder al payload via `github.event`

Todos los eventos exponen su payload completo bajo el contexto `github.event`. La estructura exacta varia segun el evento, pero el patron de acceso es uniforme.

Para inspeccionar el payload completo durante el desarrollo se puede volcar el contexto completo:

```yaml
- name: Debug del payload
  run: echo '${{ toJSON(github.event) }}'
```

Otros campos del contexto relacionados con el evento:

- `github.event_name`: nombre del evento que disparo el workflow (`issues`, `release`, etc.)
- `github.event.action`: type especifico del evento (`opened`, `published`, etc.)
- `github.ref`: ref que disparo el workflow (en `create`/`delete` es la ref creada/eliminada)
- `github.sha`: SHA del commit asociado al evento

---

## Buenas y malas practicas

**Filtrar siempre por types especificos**
Correcto: `types: [opened, labeled]` — el workflow solo se ejecuta para los casos necesarios.
Incorrecto: omitir `types` en eventos como `issues` o `release`, lo que provoca ejecuciones innecesarias ante cualquier actividad.

**Proteger `repository_dispatch` con validacion del payload**
Correcto: verificar campos esperados del `client_payload` antes de usarlos en comandos de shell y usar `fromJSON` para tipado seguro.
Incorrecto: interpolar directamente `${{ github.event.client_payload.campo }}` en `run:` sin validacion, exponiendo el workflow a inyeccion de comandos si el payload viene de fuentes no confiables.

**Usar condiciones `if` para afinar la logica dentro del job**
Correcto: combinar el tipo de evento con condiciones adicionales como `github.event.label.name == 'urgent'` para evitar jobs innecesarios.
Incorrecto: ejecutar todos los pasos del job e incluir la logica condicional solo dentro de scripts shell, lo que consume minutos de ejecucion sin razon.

**Documentar los `event_type` de `repository_dispatch` en el repositorio**
Correcto: mantener un registro de los tipos definidos, sus payloads esperados y quienes los invocan.
Incorrecto: usar tipos genéricos como `trigger` o `run` sin documentacion, lo que dificulta el mantenimiento cuando hay multiples sistemas integrados.

---

## Verificacion GH-200

**Pregunta 1.** Un sistema de CD externo necesita iniciar un workflow pasando la version a desplegar y el entorno destino. Cual es el mecanismo correcto en GitHub Actions?

a) Hacer push de un tag especial al repositorio
b) Usar `workflow_dispatch` con inputs desde la API
c) Usar `repository_dispatch` con `client_payload` via `POST /repos/.../dispatches`
d) Crear un issue con el titulo `deploy:version:entorno`

_Respuesta: c_

**Pregunta 2.** Se necesita ejecutar un workflow solo cuando una release es publicada publicamente, no cuando se guarda como borrador. Cual type es el correcto?

a) `created`
b) `released`
c) `published`
d) `prereleased`

_Respuesta: c — `published` es el unico type que garantiza que la release es visible publicamente, incluyendo pre-releases publicadas._

**Ejercicio practico.** Escribe un workflow que:
1. Se active cuando se crea un comentario en un issue con el texto exacto `/cerrar-duplicado`.
2. Cierre el issue usando `actions/github-script`.
3. Agregue la etiqueta `duplicate` al issue cerrado.

Verifica que el workflow no se active para comentarios editados o eliminados, y que solo responda al comentario exacto.

---

[← 1.3 Triggers de automatización](gha-d1-triggers-automatizacion.md) | [1.5.1 Propiedades de identidad del job →](gha-d1-jobs-identidad.md)
