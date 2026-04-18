# 1.18 Badges de estado del workflow

[<- 1.17 Variables predefinidas del runner](gha-d1-variables-predefinidas.md) | [1.19 Environment protections ->](gha-d1-environment-protections.md)

---

Los badges de estado son el indicador más visible de la salud de un proyecto. Cualquier visitante del repositorio puede ver de un vistazo si el CI está pasando o fallando sin necesidad de acceder a la pestaña Actions. Un badge rojo en el README es una señal de alarma inmediata; uno verde transmite confianza a colaboradores y usuarios. Por esta razón, incrustar el badge del workflow principal en el README es una práctica estándar en proyectos open source y en equipos que quieren hacer visible la calidad del código.

---

## URL del badge

GitHub genera automáticamente una imagen SVG para cada workflow. La URL sigue este patrón fijo:

```
https://github.com/{owner}/{repo}/actions/workflows/{workflow-file}/badge.svg
```

- `{owner}` — nombre de usuario u organización
- `{repo}` — nombre del repositorio
- `{workflow-file}` — nombre del archivo YAML dentro de `.github/workflows/`, incluyendo la extensión (por ejemplo `ci.yml`)

Esta URL devuelve siempre una imagen SVG dinámica que refleja el estado más reciente del workflow en la rama por defecto. No requiere autenticación para repositorios públicos.

---

## Parámetro `branch`

Por defecto la imagen muestra el estado de la rama por defecto del repositorio. Para mostrar el estado de una rama concreta se añade el parámetro `branch` como query string:

```
https://github.com/{owner}/{repo}/actions/workflows/{workflow-file}/badge.svg?branch=develop
```

Esto es especialmente útil cuando se trabaja con varias ramas de larga duración (`main`, `develop`, `release/*`) y se quiere exponer el estado de cada una de forma independiente en el README o en una wiki interna. El valor del parámetro debe coincidir exactamente con el nombre de la rama.

---

## Parámetro `event`

El parámetro `event` filtra el badge para que refleje únicamente las ejecuciones disparadas por un tipo de evento concreto:

```
https://github.com/{owner}/{repo}/actions/workflows/{workflow-file}/badge.svg?event=push
https://github.com/{owner}/{repo}/actions/workflows/{workflow-file}/badge.svg?event=pull_request
https://github.com/{owner}/{repo}/actions/workflows/{workflow-file}/badge.svg?event=schedule
```

Esto permite distinguir, por ejemplo, si el CI falla en pushes directos o solo en pull requests, o si el workflow programado nocturno está en error mientras el workflow de PR está verde. Los valores válidos son los mismos nombres de evento que se usan en el bloque `on:` del workflow.

---

## Variantes de URL y sus efectos

| URL | Muestra |
|-----|---------|
| `.../badge.svg` | Estado en la rama por defecto, cualquier evento |
| `.../badge.svg?branch=develop` | Estado en la rama `develop`, cualquier evento |
| `.../badge.svg?branch=main&event=push` | Estado en `main` solo para ejecuciones de `push` |
| `.../badge.svg?event=schedule` | Estado del último run programado (rama por defecto) |
| `.../badge.svg?branch=release/v2&event=push` | Estado en `release/v2` para pushes |

---

## Parámetros de la URL: referencia rápida

| Parámetro | Obligatorio | Valor ejemplo | Efecto |
|-----------|-------------|---------------|--------|
| `branch` | No | `main`, `develop` | Filtra por rama |
| `event` | No | `push`, `pull_request`, `schedule`, `workflow_dispatch` | Filtra por evento disparador |

Ambos parámetros son combinables. Si se omiten, GitHub muestra el estado del último run en cualquier rama y evento.

---

## Embeber el badge en el README

La sintaxis Markdown para incrustar un badge con enlace es:

```markdown
[![texto alternativo](URL-del-badge)](URL-de-destino)
```

El texto alternativo se muestra si la imagen no carga y es importante para accesibilidad. La URL de destino suele apuntar a la lista de ejecuciones del workflow para que el lector pueda ver el historial con un clic.

### Ejemplo central: sección de badges en README.md

```markdown
# Mi Proyecto

[![CI - main](https://github.com/mi-org/mi-repo/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/mi-org/mi-repo/actions/workflows/ci.yml)
[![CI - develop](https://github.com/mi-org/mi-repo/actions/workflows/ci.yml/badge.svg?branch=develop)](https://github.com/mi-org/mi-repo/actions/workflows/ci.yml)
[![Release](https://github.com/mi-org/mi-repo/actions/workflows/release.yml/badge.svg?branch=release/v2&event=push)](https://github.com/mi-org/mi-repo/actions/workflows/release.yml)

Descripción del proyecto...
```

Este ejemplo muestra tres badges independientes:
1. Estado del CI en la rama `main` (rama de producción)
2. Estado del CI en la rama `develop` (integración continua)
3. Estado del workflow de release en la rama `release/v2` filtrando solo pushes

---

## Cómo obtener la URL desde la UI de GitHub Actions

GitHub ofrece un asistente integrado para generar la URL del badge sin escribirla manualmente:

1. Ir a la pestaña **Actions** del repositorio
2. Seleccionar el workflow en el panel izquierdo
3. Hacer clic en los tres puntos (`...`) en la esquina superior derecha de la lista de ejecuciones
4. Seleccionar **Create status badge**
5. En el diálogo emergente, elegir la rama y el evento deseados
6. Copiar el fragmento Markdown generado y pegarlo en el README

Este método evita errores tipográficos en el nombre del archivo YAML o en el nombre de la rama.

---

## Buenas y malas practicas

| Buena practica | Mala practica |
|----------------|---------------|
| Apuntar el badge siempre a la rama por defecto (`main`) como indicador principal de salud | Apuntar a una rama de feature efimera que puede desaparecer dejando el badge roto |
| Incluir el texto alternativo descriptivo: `CI - main` en lugar de dejarlo vacio | Dejar el texto alternativo en blanco (`[![](url)](link)`), lo que perjudica la accesibilidad |
| Enlazar el badge a la pagina del workflow (`/actions/workflows/ci.yml`) para acceso rapido al historial | Enlazar a la raiz del repo o no enlazar, perdiendo la utilidad de navegacion directa |
| Usar `?event=push` cuando el workflow tiene multiples disparadores y se quiere mostrar solo el estado del CI directo | Mostrar el badge de un workflow de `schedule` que falla habitualmente por razones externas, generando alarma falsa |
| Actualizar los badges cuando se renombra el archivo YAML del workflow | Dejar URLs rotas en el README que devuelven imagen de error por archivo inexistente |

---

## Preguntas GH-200

**P1.** Un workflow llamado `build-and-test.yml` ejecuta el CI en varias ramas. ¿Cuál es la URL correcta del badge para mostrar el estado en la rama `release/2.0` solo para eventos `push`?

<details>
<summary>Respuesta</summary>

```
https://github.com/{owner}/{repo}/actions/workflows/build-and-test.yml/badge.svg?branch=release/2.0&event=push
```

El nombre del archivo debe incluir la extension `.yml` y los parametros se concatenan con `&`.
</details>

---

**P2.** Al acceder a la URL del badge de un workflow, la imagen muestra "no status". ¿Cuales son las dos causas mas probables?

<details>
<summary>Respuesta</summary>

1. El workflow nunca ha sido ejecutado en esa rama o con ese evento (no hay historial).
2. El nombre del archivo en la URL no coincide exactamente con el nombre del archivo en `.github/workflows/` (sensible a mayusculas y minusculas).
</details>

---

**P3.** ¿Que ventaja tiene usar el asistente **Create status badge** de la UI de GitHub frente a construir la URL manualmente?

<details>
<summary>Respuesta</summary>

El asistente genera el fragmento Markdown completo con la URL correctamente codificada, incluyendo el nombre exacto del archivo YAML y permitiendo seleccionar rama y evento desde desplegables. Elimina errores de tipografia y codificacion de caracteres especiales (por ejemplo, `/` en nombres de ramas).
</details>

---

## Ejercicio practico

Dado el siguiente fragmento de README con un badge roto:

```markdown
[![Build](https://github.com/acme/api/actions/workflows/CI.yml/badge.svg?branch=Main)](https://github.com/acme/api/actions)
```

El repositorio tiene el archivo `.github/workflows/ci.yml` y la rama principal se llama `main`. Identifica los dos errores y escribe la URL corregida.

<details>
<summary>Solucion</summary>

**Error 1:** El nombre del archivo en la URL es `CI.yml` pero el archivo real es `ci.yml`. Las URLs de GitHub Actions son sensibles a mayusculas.

**Error 2:** El parametro `branch=Main` usa `M` mayuscula, pero la rama se llama `main`. Los nombres de rama tambien son sensibles a mayusculas en este contexto.

**URL corregida:**
```markdown
[![Build](https://github.com/acme/api/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/acme/api/actions/workflows/ci.yml)
```
</details>

---

[<- 1.17 Variables predefinidas del runner](gha-d1-variables-predefinidas.md) | [1.19 Environment protections ->](gha-d1-environment-protections.md)
