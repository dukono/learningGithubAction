# X.9b Container jobs y networking Docker con service containers

Prerequisito: [1.9.1 Service containers](gha-d1-service-containers.md)

Navegacion: [← 1.9.1 Service containers](gha-d1-service-containers.md) | [1.10 Matrix strategy →](gha-d1-matrix-strategy.md)

---

## Introduccion: container job vs runner host

Cuando defines un job en GitHub Actions, por defecto los steps se ejecutan directamente en el runner host (la VM de Ubuntu, Windows o macOS). Sin embargo, puedes indicarle a GitHub Actions que ejecute todos los steps del job dentro de un contenedor Docker con la propiedad `jobs.<id>.container`. Esta eleccion afecta no solo donde corre tu codigo, sino tambien como tus steps se comunican con los service containers que definas en el mismo job. La razon principal para preferir un container job es garantizar un entorno reproducible: si tu aplicacion depende de una version exacta de Node, Python, Ruby o cualquier otra herramienta, encapsulas esa dependencia en la imagen Docker en lugar de confiar en lo que el runner tenga instalado.

---

## Propiedad `jobs.<id>.container`

La propiedad `jobs.<id>.container` acepta una imagen Docker (como string corto) o un objeto con configuracion detallada. Las sub-propiedades disponibles son:

| Sub-propiedad | Tipo | Descripcion |
|---|---|---|
| `image` | string | Imagen Docker a usar, por ejemplo `node:20-alpine` |
| `env` | map | Variables de entorno inyectadas en el contenedor |
| `ports` | lista | Puertos a exponer del contenedor hacia el runner host |
| `volumes` | lista | Montajes de volumen, formato `origen:destino` |
| `options` | string | Flags adicionales para `docker run`, por ejemplo `--cpus 1` |
| `credentials` | map | `username` y `password` para registros privados |

Cuando se especifica solo la imagen como string (`container: node:20`), GitHub Actions usa valores por defecto para el resto. Cuando se necesita configuracion adicional, se usa la forma de objeto con `image` como clave obligatoria.

---

## Diferencia entre runner host y container job

En un job sin `container`, cada step se ejecuta como proceso hijo en la VM del runner. El runner tiene acceso directo al sistema de archivos del host y hereda las herramientas preinstaladas (git, docker, node, etc.). En un container job, GitHub Actions descarga la imagen especificada, crea un contenedor, monta el workspace del repositorio dentro de el, e instala el runner agent dentro de ese contenedor. Todos los steps del job — incluyendo `run`, `uses` para actions JavaScript y actions compuestas — se ejecutan dentro de ese contenedor. Las actions Docker (`uses: ./` con `Dockerfile`) se ejecutan en sus propios contenedores pero comparten la red del job. El resultado practico es que el entorno de ejecucion es exactamente el que define la imagen, sin dependencias del software del runner host.

---

## Acceso a service containers desde un container job

Este es el punto mas importante y frecuente en el examen GH-200. Cuando tu job usa `container`, los service containers se conectan a una red Docker compartida creada automaticamente por GitHub Actions. En esta red, cada service container es accesible por el nombre que le diste en el bloque `services`. Por ejemplo, si defines `services.postgres`, el hostname reachable desde dentro del container job es `postgres`, no `localhost`. Esto es porque son contenedores separados en la misma red bridge de Docker, y Docker asigna el nombre del servicio como entrada DNS en esa red. Intentar conectarse a `localhost` desde el container job fallara porque `localhost` dentro del contenedor del job es el propio contenedor del job, no el service container.

---

## Diferencia de networking: runner host vs container job

La distincion de networking es critica y genera errores frecuentes cuando se migra de un modo al otro:

| Escenario | Como acceder al service container |
|---|---|
| Job en runner host (sin `container`) | `localhost:<puerto>` o `127.0.0.1:<puerto>` |
| Container job (con `container`) | `<nombre-del-servicio>:<puerto>` |

En el runner host, los service containers exponen sus puertos en la interfaz loopback del host, de modo que `localhost:5432` llega a PostgreSQL. En el container job, los contenedores estan en una red Docker aislada; el service container no esta en el localhost del contenedor job, sino en la misma red virtual donde es accesible por nombre. Si defines `services.db`, usas `db:5432`. Si defines `services.redis-cache`, usas `redis-cache:6379`. Ademas, en container job no es necesario mapear `ports` en el service container para que el job pueda acceder a el, aunque si puede ser necesario para acceso externo o depuracion.

---

## Casos de uso de container jobs

Los container jobs son ideales en tres escenarios concretos. Primero, cuando necesitas una version exacta de un runtime que puede diferir de la del runner: por ejemplo, probar con Python 3.8 en un runner Ubuntu 22 que tiene Python 3.10. Segundo, cuando tu pipeline depende de herramientas especializadas no disponibles en runners estandar, como compiladores de lenguajes menos comunes, SDKs propietarios o versiones especificas de CLIs. Tercero, para lograr consistencia entre CI y entorno de desarrollo local: si el equipo usa la misma imagen Docker para desarrollo y para CI, se eliminan discrepancias del tipo "funciona en mi maquina". Un caso tipico es una imagen personalizada con dependencias de sistema preinstaladas (bibliotecas nativas, certificados, configuraciones) que seria costoso instalar en cada ejecucion con `apt-get`.

---

## Ejemplo completo: container job con PostgreSQL

El siguiente workflow ejecuta tests de integracion en Node 20 contra PostgreSQL, con todos los steps dentro del container job:

```yaml
name: Integration tests con container job

on: [push]

jobs:
  integration-test:
    runs-on: ubuntu-latest

    # Todos los steps corren dentro de este contenedor
    container:
      image: node:20-alpine
      env:
        NODE_ENV: test
      options: --cpus 1

    services:
      postgres:
        image: postgres:15
        env:
          POSTGRES_USER: testuser
          POSTGRES_PASSWORD: testpass
          POSTGRES_DB: testdb
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Install dependencies
        run: npm ci

      - name: Run migrations
        env:
          # hostname = nombre del servicio, NO localhost
          DATABASE_URL: postgresql://testuser:testpass@postgres:5432/testdb
        run: npm run db:migrate

      - name: Run integration tests
        env:
          DATABASE_URL: postgresql://testuser:testpass@postgres:5432/testdb
        run: npm test
```

Puntos clave del ejemplo:
- `container: image: node:20-alpine` — steps corren en Node Alpine, no en Ubuntu del runner
- El service `postgres` es accesible como hostname `postgres` (NO `localhost`)
- `options: --health-cmd pg_isready` espera a que Postgres este listo antes de los steps
- No se definen `ports` en el service porque no se necesitan desde fuera de la red Docker

---

## Buenas y malas practicas

**Usar el hostname del servicio como nombre declarado en `services`**
- Correcto: si el servicio se llama `postgres`, conectar con `postgres:5432`
- Incorrecto: conectar con `localhost:5432` desde un container job — fallara silenciosamente o con error de conexion rechazada

**Especificar health checks en services con container job**
- Correcto: usar `options: --health-cmd ... --health-interval ...` para que GitHub Actions espere a que el servicio este listo
- Incorrecto: asumir que el service container esta listo al inicio del primer step; puede haber race conditions si el servicio tarda en arrancar

**Usar imagenes ligeras y especificas para el container job**
- Correcto: `node:20-alpine`, `python:3.11-slim` — rapidas de descargar, predecibles
- Incorrecto: usar `ubuntu:latest` como container job y luego instalar todo via `apt-get`; convierte el contenedor en un runner host alternativo sin ventaja real

---

## Verificacion GH-200

**Pregunta 1.** Tu job tiene `container: image: python:3.11` y un service llamado `mysql`. Cual es la cadena de conexion correcta para acceder a MySQL desde los steps?

- A) `mysql://user:pass@localhost:3306/db`
- B) `mysql://user:pass@mysql:3306/db`
- C) `mysql://user:pass@127.0.0.1:3306/db`
- D) `mysql://user:pass@host.docker.internal:3306/db`

Respuesta correcta: **B** — en container job, el service es accesible por su nombre declarado en `services`.

**Pregunta 2.** Cual de las siguientes afirmaciones sobre container jobs es verdadera?

- A) Los steps `run` se ejecutan en el runner host aunque haya `container` definido
- B) La propiedad `container` solo acepta un string con el nombre de la imagen
- C) En un container job, los service containers comparten una red Docker con el contenedor del job
- D) Los puertos del service container deben mapearse en `ports` para ser accesibles desde el container job

Respuesta correcta: **C** — comparten la red Docker del job y el acceso es por nombre de servicio.

**Ejercicio practico.** Tienes un workflow con runner host que usa `DATABASE_URL: postgresql://user:pass@localhost:5432/db` y un service `postgres`. Debes migrarlo a container job usando `image: python:3.11`. Describe los dos cambios minimos necesarios en el YAML.

Solucion: (1) Agregar el bloque `container: image: python:3.11` al job. (2) Cambiar `localhost` por `postgres` (nombre del servicio) en `DATABASE_URL`. No es necesario ningun otro cambio estructural.

---

Navegacion: [← 1.9.1 Service containers](gha-d1-service-containers.md) | [1.10 Matrix strategy →](gha-d1-matrix-strategy.md)
