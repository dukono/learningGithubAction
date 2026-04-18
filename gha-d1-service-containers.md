# X.9a Service Containers

[← 1.8 Dependencias entre jobs](gha-d1-dependencias-jobs.md) | [→ 1.9.2 Container jobs y networking Docker](gha-d1-container-jobs.md)

---

## Por qué existen los service containers

Cuando un workflow necesita una base de datos, una cache Redis o cualquier servicio externo, la alternativa clásica es instalar y arrancar ese servicio directamente en el runner. Eso requiere scripts de instalación, gestión del proceso en segundo plano, y limpieza posterior. GitHub Actions resuelve este problema con los **service containers**: contenedores Docker que se levantan automáticamente antes de que empiecen los steps del job, se conectan a la misma red que el runner y se destruyen al finalizar el job. No hace falta instalar nada en el runner ni escribir scripts de arranque. El servicio está disponible en cuanto los steps comienzan a ejecutarse.

---

## Sintaxis `services:` en un job

La clave `services:` se declara al nivel del job, al mismo nivel que `steps:` o `runs-on:`. Cada servicio recibe un nombre arbitrario que actúa como identificador interno. Dentro de cada servicio se especifican propiedades como la imagen Docker, los puertos, las variables de entorno y las opciones del contenedor.

```yaml
jobs:
  test:
    runs-on: ubuntu-latest
    services:
      nombre-servicio:
        image: nombre-imagen:tag
        ports:
          - host_port:container_port
        env:
          VAR: valor
        options: --health-cmd "..." --health-interval 10s
    steps:
      - name: Usar el servicio
        run: ...
```

El nombre del servicio (`nombre-servicio` en el ejemplo) tiene doble función: es el identificador en la definición YAML y, cuando el job corre dentro de un container job, se convierte en el hostname de red del servicio. Este segundo uso se detalla en el documento siguiente.

---

## Elemento `image:`

La propiedad `image:` especifica la imagen Docker que se usará para el servicio. Acepta cualquier imagen válida de Docker Hub o de un registro privado, con o sin tag. Si se omite el tag, Docker usará `latest` por defecto, lo que puede generar comportamientos no reproducibles entre ejecuciones. La práctica recomendada es siempre fijar un tag concreto.

```yaml
services:
  db:
    image: postgres:16.2-alpine
```

Para imágenes de registros privados se puede combinar con la propiedad `credentials:`:

```yaml
services:
  app-db:
    image: ghcr.io/mi-org/mi-imagen:1.0.0
    credentials:
      username: ${{ github.actor }}
      password: ${{ secrets.GHCR_TOKEN }}
```

---

## Elemento `ports:`

La propiedad `ports:` define el mapeo entre puertos del host runner y puertos del contenedor del servicio, con la sintaxis `host_port:container_port`. Este mapeo permite que los steps que corren en el runner (fuera del contenedor) accedan al servicio a través de `localhost:host_port`.

```yaml
services:
  cache:
    image: redis:7.2-alpine
    ports:
      - 6379:6379
```

Si el host port se omite (`- 6379`), Docker asigna un puerto efímero aleatorio. En ese caso se puede consultar el puerto asignado mediante el contexto `job.services.<nombre>.ports`:

```yaml
- run: echo "Puerto Redis: ${{ job.services.cache.ports['6379'] }}"
```

Fijar el host port explícitamente es más simple cuando solo hay una instancia del servicio por job.

---

## Elemento `env:`

La propiedad `env:` del servicio inyecta variables de entorno dentro del contenedor del servicio en el momento de su arranque. Son distintas de las variables de entorno del job o de los steps: solo existen dentro del contenedor del servicio y no son visibles en los steps del runner.

Estas variables son imprescindibles para configurar servicios como bases de datos, que requieren credenciales o nombres de base de datos en el arranque:

```yaml
services:
  db:
    image: postgres:16.2-alpine
    env:
      POSTGRES_USER: testuser
      POSTGRES_PASSWORD: testpass
      POSTGRES_DB: testdb
```

Los valores pueden referenciar secrets del repositorio para evitar exponer credenciales en texto plano:

```yaml
    env:
      POSTGRES_PASSWORD: ${{ secrets.DB_PASSWORD }}
```

---

## Elemento `options:` y health checks

La propiedad `options:` pasa argumentos directamente al comando `docker create` que GitHub Actions ejecuta para crear el contenedor. El uso más importante es configurar el health check del servicio, que permite a GitHub Actions esperar a que el servicio esté realmente operativo antes de comenzar los steps del job.

Sin health check, los steps podrían iniciarse cuando el contenedor ya existe pero el servicio interno (por ejemplo, PostgreSQL) aún está inicializando y no acepta conexiones.

Las opciones de health check más relevantes son:

| Opción | Descripción |
|---|---|
| `--health-cmd` | Comando que se ejecuta dentro del contenedor para verificar disponibilidad |
| `--health-interval` | Frecuencia con la que se ejecuta el comando de verificación |
| `--health-timeout` | Tiempo máximo para que el comando de verificación responda |
| `--health-retries` | Número de fallos consecutivos antes de declarar el contenedor no saludable |
| `--health-start-period` | Tiempo de gracia inicial antes de comenzar a contar fallos |

```yaml
services:
  db:
    image: postgres:16.2-alpine
    options: >-
      --health-cmd pg_isready
      --health-interval 10s
      --health-timeout 5s
      --health-retries 5
```

GitHub Actions espera a que el health check reporte `healthy` antes de ejecutar el primer step. Si el contenedor no alcanza el estado `healthy` en el tiempo límite, el job falla con un error de timeout.

---

## Acceso desde steps en runner host: `localhost:puerto`

Cuando el job corre directamente en el runner (sin container job), los steps se ejecutan en el sistema operativo del host. En este caso, los service containers son accesibles a través de `localhost` más el host port mapeado en `ports:`.

```yaml
jobs:
  test:
    runs-on: ubuntu-latest
    services:
      db:
        image: postgres:16.2-alpine
        ports:
          - 5432:5432
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
      - uses: actions/checkout@v4
      - name: Conectar a PostgreSQL
        run: |
          psql postgresql://testuser:testpass@localhost:5432/testdb -c "SELECT 1;"
```

El host port `5432` del mapeo `5432:5432` es el que se usa en la cadena de conexión. GitHub Actions configura automáticamente la red Docker para que `localhost:5432` en el runner apunte al puerto `5432` del contenedor del servicio.

---

## Acceso desde container jobs: hostname = nombre del servicio

Cuando el job usa un container job (propiedad `container:` en el job), tanto el contenedor del job como los service containers están en la misma red Docker. En este contexto, los service containers no se acceden por `localhost` sino por el **nombre del servicio** tal como está declarado en el YAML.

Si el servicio se llama `db`, la cadena de conexión usará `db` como hostname, y el puerto será el puerto del contenedor (no el host port). Este mecanismo de resolución por nombre es el tema central del documento siguiente y tiene sentido precisamente porque los service containers se declaran con un nombre identificador en la clave `services:`.

```yaml
# Cuando el job usa container:
steps:
  - name: Conectar al servicio usando su nombre
    run: psql postgresql://testuser:testpass@db:5432/testdb -c "SELECT 1;"
    # "db" es el nombre declarado en services:
```

---

## Diagrama de networking

```
Runner host (ubuntu-latest)
┌─────────────────────────────────────────────────────────┐
│                                                         │
│  Steps del job (proceso en el runner)                   │
│  Acceso: localhost:5432                                 │
│                          │                              │
│         ┌────────────────▼──────────┐                  │
│         │  Service container: db    │                  │
│         │  image: postgres:16.2     │                  │
│         │  puerto interno: 5432     │                  │
│         │  host port: 5432          │                  │
│         └───────────────────────────┘                  │
│                                                         │
└─────────────────────────────────────────────────────────┘

Container job (con container: en el job)
┌─────────────────────────────────────────────────────────┐
│  Red Docker interna                                     │
│                                                         │
│  ┌──────────────────────┐   ┌──────────────────────┐   │
│  │  Container del job   │   │ Service container    │   │
│  │  (runner container)  │──▶│ hostname: db         │   │
│  │                      │   │ puerto: 5432         │   │
│  │  Acceso: db:5432     │   │                      │   │
│  └──────────────────────┘   └──────────────────────┘   │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

---

## Ejemplo central completo

El siguiente workflow ejecuta tests de integración de una aplicación Node.js contra una base de datos PostgreSQL real y una cache Redis, ambas como service containers.

```yaml
name: Integration Tests

on:
  push:
    branches: [main]
  pull_request:

jobs:
  integration-test:
    runs-on: ubuntu-latest

    services:
      db:
        image: postgres:16.2-alpine
        ports:
          - 5432:5432
        env:
          POSTGRES_USER: testuser
          POSTGRES_PASSWORD: ${{ secrets.TEST_DB_PASSWORD }}
          POSTGRES_DB: testdb
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

      cache:
        image: redis:7.2-alpine
        ports:
          - 6379:6379
        options: >-
          --health-cmd "redis-cli ping"
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'npm'

      - name: Instalar dependencias
        run: npm ci

      - name: Ejecutar migraciones
        run: npm run db:migrate
        env:
          DATABASE_URL: postgresql://testuser:${{ secrets.TEST_DB_PASSWORD }}@localhost:5432/testdb

      - name: Ejecutar tests de integración
        run: npm test
        env:
          DATABASE_URL: postgresql://testuser:${{ secrets.TEST_DB_PASSWORD }}@localhost:5432/testdb
          REDIS_URL: redis://localhost:6379
```

En este ejemplo, los dos servicios tienen health check configurado. GitHub Actions no ejecuta el primer step hasta que ambos reportan estado `healthy`. Las variables de entorno `DATABASE_URL` y `REDIS_URL` se pasan a los steps que las necesitan, usando `localhost` y los host ports mapeados.

---

## Tabla de propiedades del service container

| Propiedad | Obligatoria | Descripción |
|---|---|---|
| `image:` | Si | Imagen Docker con tag recomendado |
| `ports:` | No | Mapeo `host:container`; necesario para acceso desde runner host |
| `env:` | No | Variables de entorno inyectadas en el contenedor del servicio |
| `options:` | No | Argumentos `docker create`; se usa principalmente para health checks |
| `credentials:` | No | Usuario y password para registros privados |
| `volumes:` | No | Montaje de volúmenes en el contenedor del servicio |

---

## Buenas y malas practicas

**Siempre fijar el tag de la imagen.**
Usar `postgres:latest` puede producir fallos inesperados si la imagen upstream cambia entre ejecuciones. Usar `postgres:16.2-alpine` garantiza reproducibilidad.

**Configurar health check en servicios que tienen tiempo de inicializacion.**
Bases de datos como PostgreSQL o MySQL tardan unos segundos en estar listas para aceptar conexiones aunque el contenedor ya haya arrancado. Sin health check, el primer step puede intentar conectarse antes de que el servicio esté operativo y fallar con un error de conexion rechazada.

**No hardcodear credenciales en el YAML del servicio.**
Aunque los valores de `env:` en `services:` no aparecen en los logs, siguen estando en el fichero YAML que vive en el repositorio. Usar `${{ secrets.NOMBRE }}` para cualquier dato sensible como passwords.

**No exponer puertos innecesarios en el host.**
Si varios jobs del mismo runner corren en paralelo y todos mapean el mismo host port, habrá conflictos. Usar el mapeo sin host port explícito (`- 5432`) y leer el puerto asignado desde `job.services.<nombre>.ports` es más seguro en entornos con paralelismo alto.

**Separar variables de entorno del servicio y del step.**
Las variables en `services.<nombre>.env:` solo existen dentro del contenedor del servicio. Los steps necesitan sus propias variables de entorno (en `env:` del step o del job) para saber cómo conectarse al servicio. Son contextos distintos y no se comparten automáticamente.

**No asumir que el orden de los servicios es el orden de arranque.**
GitHub Actions puede arrancar los servicios en paralelo. Si un servicio depende de otro (por ejemplo, una app que necesita la base de datos), configurar health checks en ambos y dejar que GitHub Actions gestione la espera.

---

## Verificacion

**Pregunta 1.** Un job tiene un service container con `ports: ["5432:5432"]` y `runs-on: ubuntu-latest` sin propiedad `container:`. Desde los steps, la cadena de conexion correcta es:

- A) `db:5432`
- B) `localhost:5432`
- C) `0.0.0.0:5432`
- D) El nombre del job como hostname

**Respuesta:** B. Sin container job, los steps corren en el runner host y acceden a los service containers por `localhost` mas el host port mapeado.

---

**Pregunta 2.** Un service container de PostgreSQL arranca pero el primer step falla con "connection refused". La causa mas probable es:

- A) El `image:` no tiene tag especificado
- B) No se configuró `options:` con health check y el step se ejecutó antes de que PostgreSQL terminara de inicializar
- C) El puerto `5432` no está en la lista de puertos permitidos por GitHub Actions
- D) Las variables `env:` del servicio no se propagaron a los steps

**Respuesta:** B. Sin health check, GitHub Actions no espera a que el servicio este operativo. PostgreSQL puede tardar varios segundos en estar listo aunque el contenedor ya haya arrancado.

---

**Pregunta 3.** Que propiedad del service container se usa para pasar argumentos directamente al comando `docker create`?

- A) `env:`
- B) `ports:`
- C) `options:`
- D) `config:`

**Respuesta:** C. La propiedad `options:` pasa argumentos literales al comando `docker create`, lo que permite configurar health checks y otras opciones de contenedor.

---

**Ejercicio practico.** Escribe la seccion `services:` para un job que necesite:
- MySQL 8.0 con base de datos `appdb`, usuario `appuser` y password desde secret `MYSQL_PASSWORD`
- Health check que use `mysqladmin ping` cada 10 segundos con 5 reintentos
- Puerto 3306 del host mapeado al puerto 3306 del contenedor

Solución de referencia:

```yaml
services:
  mysql:
    image: mysql:8.0
    ports:
      - 3306:3306
    env:
      MYSQL_ROOT_PASSWORD: ${{ secrets.MYSQL_PASSWORD }}
      MYSQL_DATABASE: appdb
      MYSQL_USER: appuser
      MYSQL_PASSWORD: ${{ secrets.MYSQL_PASSWORD }}
    options: >-
      --health-cmd "mysqladmin ping -h localhost"
      --health-interval 10s
      --health-timeout 5s
      --health-retries 5
```

---

[← 1.8 Dependencias entre jobs](gha-d1-dependencias-jobs.md) | [→ 1.9.2 Container jobs y networking Docker](gha-d1-container-jobs.md)
