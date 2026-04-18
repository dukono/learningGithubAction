# 1.14 Caché de dependencias

[← 1.13 Expresiones y funciones built-in](gha-d1-expresiones.md) | [1.15 Artefactos →](gha-d1-artefactos.md)

---

## Por qué importa el caché en CI

En un pipeline de integración continua, instalar dependencias es frecuentemente la operación más costosa en tiempo. Un proyecto Node.js mediano puede tardar entre 60 y 120 segundos solo en ejecutar `npm install`, y ese tiempo se multiplica por cada job, cada runner y cada push. El caché de dependencias de GitHub Actions resuelve este problema almacenando los archivos descargados entre ejecuciones del workflow, de modo que cuando la clave del caché coincide, el runner restaura los archivos en segundos en lugar de volver a descargarlos desde la red. La diferencia práctica es significativa: pipelines que tardaban 4 minutos se reducen a menos de 1 minuto cuando el caché tiene una tasa de aciertos alta.

---

## La acción `actions/cache`

La acción oficial `actions/cache` implementa tanto la restauración (al inicio del job) como el guardado (al final del job) en un único paso. Funciona en dos fases automáticas: cuando el step se ejecuta, intenta encontrar un caché existente que coincida con la clave; al terminar el job con éxito, guarda el estado actual de los paths indicados bajo la clave calculada.

Los tres parámetros esenciales son:

- **`key`**: cadena que identifica de forma única este caché. Si la clave coincide exactamente con un caché existente, se produce un cache hit completo. La clave suele incluir el sistema operativo, el nombre del gestor de paquetes y un hash del archivo de bloqueo de dependencias.
- **`restore-keys`**: lista de prefijos de fallback que GitHub evalúa en orden cuando la clave principal no coincide. Permite restaurar un caché parcialmente válido en lugar de empezar desde cero.
- **`path`**: uno o varios directorios o archivos que se guardan y restauran. Puede ser un valor único o una lista multilínea. Debe apuntar a los directorios de caché del gestor de paquetes, no al código fuente.

```yaml
- name: Configurar caché de npm
  uses: actions/cache@v4
  with:
    path: ~/.npm
    key: ${{ runner.os }}-npm-${{ hashFiles('**/package-lock.json') }}
    restore-keys: |
      ${{ runner.os }}-npm-
```

---

## El output `cache-hit`

Cuando `actions/cache` completa la fase de restauración, publica el output `cache-hit` con valor `true` o `false`. El valor es `true` únicamente cuando la clave principal (`key`) coincide exactamente con un caché almacenado. Cuando la restauración se produce a través de un `restore-key` de fallback, `cache-hit` es `false` aunque se hayan restaurado archivos.

Este output permite tomar decisiones en pasos posteriores. El caso de uso más común es omitir la instalación completa de dependencias cuando el caché es exacto, ahorrando tiempo adicional:

```yaml
- name: Configurar caché de npm
  id: cache-npm
  uses: actions/cache@v4
  with:
    path: ~/.npm
    key: ${{ runner.os }}-npm-${{ hashFiles('**/package-lock.json') }}
    restore-keys: |
      ${{ runner.os }}-npm-

- name: Instalar dependencias
  if: steps.cache-npm.outputs.cache-hit != 'true'
  run: npm ci
```

La condición `steps.cache-npm.outputs.cache-hit != 'true'` evalúa el output usando la sintaxis de expresiones de GitHub Actions. Cuando hay cache hit exacto, el step de instalación se omite completamente. Cuando el caché fue parcial (via restore-key) o inexistente, `npm ci` se ejecuta para garantizar un estado de dependencias limpio.

---

## Diagrama: algoritmo de búsqueda de caché

```
Inicio del job
      │
      ▼
┌─────────────────────────────────────┐
│  Buscar caché con key exacta        │
│  runner.os-npm-<hash-exacto>        │
└─────────────────────────────────────┘
      │
   ¿Encontrado?
   /          \
 Sí            No
  │             │
  ▼             ▼
cache-hit=true  ┌─────────────────────────────────────┐
Restaurar       │  Evaluar restore-keys en orden       │
archivos        │  1. runner.os-npm-<hash-anterior>    │
                │  2. runner.os-npm-                   │
                │  3. runner.os-                       │
                └─────────────────────────────────────┘
                      │
                ¿Alguno coincide?
                /              \
              Sí                No
               │                 │
               ▼                 ▼
        cache-hit=false    cache-hit=false
        Restaurar caché    Sin restauración
        más reciente       (directorio vacío)
        con ese prefijo
               │                 │
               └────────┬────────┘
                        ▼
                  Ejecutar pasos del job
                        │
                        ▼
                ¿Job terminó con éxito?
                /                    \
              Sí                      No (con save-always: true → Sí)
               │                           │
               ▼                           ▼
        Guardar caché              No guardar caché
        bajo la key                (comportamiento por defecto)
```

---

## Scope del caché: qué ramas pueden acceder a qué cachés

GitHub Actions aplica un modelo de alcance (scope) para controlar qué caché puede acceder un workflow. Las reglas son:

1. Un workflow puede leer cachés creados en la **rama actual** en la que se ejecuta.
2. Si no hay caché en la rama actual, puede restaurar cachés de la **rama base** (la rama de la que se creó la pull request, o la rama en la que se hizo merge).
3. Si tampoco hay caché en la rama base, puede restaurar cachés de la **rama por defecto** del repositorio (normalmente `main` o `master`).

Este diseño significa que los cachés generados en `main` están disponibles como fallback para todas las ramas del repositorio, lo que maximiza los aciertos de caché en ramas nuevas que aún no han generado su propio caché. Sin embargo, los cachés creados en ramas de feature no están disponibles para otras ramas de feature ni para `main`, lo que evita contaminación cruzada entre contextos de trabajo. La consecuencia práctica: el primer run de una rama nueva usará el caché de `main`, que suele ser el más actualizado.

---

## Límite de tamaño: 10 GB por repositorio

Cada repositorio tiene un límite total de 10 GB de almacenamiento de caché. Este límite es compartido entre todos los cachés de todas las ramas. Cuando el repositorio supera los 10 GB, GitHub elimina automáticamente los cachés más antiguos (los que llevan más tiempo sin ser accedidos) hasta que el uso total queda por debajo del límite. Este proceso de evicción es transparente: no genera errores en los workflows, simplemente el próximo run que necesite ese caché no lo encontrará y tendrá que crearlo de nuevo.

Los cachés individuales que no han sido accedidos en los últimos 7 días también se eliminan automáticamente, independientemente del límite de tamaño. En repositorios con muchas ramas activas y dependencias pesadas, es importante diseñar las claves de caché para que sean lo suficientemente específicas (evitar guardar contenido redundante) pero no tan volátiles que generen un caché nuevo en cada commit.

---

## `save-always`: guardar incluso cuando el job falla

Por defecto, `actions/cache` solo guarda el caché si el job termina con éxito. Esto es un comportamiento seguro: si el job falla a mitad de la instalación, los archivos en el directorio de caché podrían estar en un estado inconsistente y guardarlos contaminaría el caché para runs futuros.

Sin embargo, hay casos donde sí interesa guardar el caché aunque el job falle. El parámetro `save-always: true` cambia este comportamiento:

```yaml
- name: Configurar caché de npm
  uses: actions/cache@v4
  with:
    path: ~/.npm
    key: ${{ runner.os }}-npm-${{ hashFiles('**/package-lock.json') }}
    restore-keys: |
      ${{ runner.os }}-npm-
    save-always: true
```

Un caso de uso típico de `save-always` es cuando el job ejecuta tests y se espera que algunos fallen. Sin `save-always`, los tests fallidos impedirían guardar las dependencias instaladas, forzando una reinstalación completa en el siguiente intento. Con `save-always`, las dependencias se cachean independientemente del resultado de los tests.

---

## Acciones separadas: `actions/cache/save` y `actions/cache/restore`

Además de la acción combinada, GitHub Actions ofrece dos acciones independientes que separan las fases de restauración y guardado. Esto da control granular sobre cuándo y cómo ocurre cada operación.

**`actions/cache/restore`** solo restaura el caché (sin guardarlo al final del job). Es útil en workflows de solo lectura donde no se quiere sobreescribir el caché existente:

```yaml
- name: Restaurar caché de npm (solo lectura)
  uses: actions/cache/restore@v4
  with:
    path: ~/.npm
    key: ${{ runner.os }}-npm-${{ hashFiles('**/package-lock.json') }}
    restore-keys: |
      ${{ runner.os }}-npm-
```

**`actions/cache/save`** solo guarda el caché. Se puede colocar en cualquier punto del workflow, incluso condicionalmente, o como step explícito al final de un job complejo:

```yaml
- name: Guardar caché de npm
  uses: actions/cache/save@v4
  if: always()
  with:
    path: ~/.npm
    key: ${{ runner.os }}-npm-${{ hashFiles('**/package-lock.json') }}
```

El caso de uso principal de estas acciones separadas es cuando se necesita lógica condicional avanzada: restaurar al inicio, ejecutar pasos que pueden fallar, y luego guardar solo si ciertas condiciones se cumplen. También son útiles en workflows que reutilizan el caché de solo lectura en múltiples jobs sin riesgo de sobreescritura accidental.

---

## Estrategia de `restore-keys`: fallback ordenado

Los `restore-keys` son prefijos evaluados de forma secuencial. GitHub toma el primer prefijo de la lista y busca el caché más reciente cuya clave comience por ese texto. Si no encuentra ninguno, pasa al siguiente prefijo. Este comportamiento permite definir niveles de granularidad decreciente.

```yaml
restore-keys: |
  ${{ runner.os }}-node-${{ env.NODE_VERSION }}-npm-
  ${{ runner.os }}-node-${{ env.NODE_VERSION }}-
  ${{ runner.os }}-node-
  ${{ runner.os }}-
```

En este ejemplo, la búsqueda de fallback va desde lo más específico (misma versión de Node y mismo gestor) hasta lo más genérico (solo el sistema operativo). Restaurar un caché parcialmente compatible es siempre mejor que no restaurar nada, porque aunque luego se ejecute `npm ci`, muchos paquetes ya estarán en la caché local de npm y solo se descargarán los que hayan cambiado. La regla práctica es: cuanto más larga sea la clave principal, más útil es tener restore-keys con prefijos de longitud decreciente.

---

## Ejemplo central: caché de npm con instalación condicional

El siguiente workflow demuestra todos los conceptos en un caso real: caché de dependencias npm con clave basada en el hash del lockfile, restore-keys de fallback, y uso de `cache-hit` para omitir la instalación cuando el caché es exacto.

```yaml
name: CI con caché de npm

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  build-and-test:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout del código
        uses: actions/checkout@v4

      - name: Configurar Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '20'

      # Paso de caché: restaura si existe, guarda al final si el job tiene éxito
      - name: Configurar caché de npm
        id: cache-npm
        uses: actions/cache@v4
        with:
          # Directorio de caché global de npm
          path: ~/.npm
          # Clave exacta: OS + hash del package-lock.json
          # Si package-lock.json cambia, la clave cambia y se crea un caché nuevo
          key: ${{ runner.os }}-npm-${{ hashFiles('**/package-lock.json') }}
          # Fallbacks: primero cualquier caché de npm en este OS,
          # luego cualquier caché de npm (cualquier OS)
          restore-keys: |
            ${{ runner.os }}-npm-
            npm-

      # Solo instala si NO hubo cache hit exacto
      # Si hubo restore-key match, npm ci actualiza solo lo necesario
      - name: Instalar dependencias
        if: steps.cache-npm.outputs.cache-hit != 'true'
        run: npm ci

      # Cuando cache-hit es false por restore-key parcial, también necesitamos instalar
      # Este step maneja el caso de restore-key match (caché parcial)
      - name: Sincronizar dependencias (caché parcial)
        if: steps.cache-npm.outputs.cache-hit != 'true'
        run: |
          echo "Cache hit: ${{ steps.cache-npm.outputs.cache-hit }}"
          echo "Instalando/actualizando dependencias..."
          npm ci

      - name: Compilar
        run: npm run build

      - name: Ejecutar tests
        run: npm test

      - name: Verificar estado del caché
        run: |
          echo "Cache hit exacto: ${{ steps.cache-npm.outputs.cache-hit }}"
          echo "Tamaño del directorio npm: $(du -sh ~/.npm 2>/dev/null || echo 'N/A')"
```

**Puntos clave de este ejemplo:**

- La función `hashFiles('**/package-lock.json')` genera una huella digital del archivo de bloqueo. Cualquier cambio en las dependencias cambia el hash y fuerza la creación de un caché nuevo.
- El `id: cache-npm` es obligatorio para poder referenciar el output `cache-hit` en pasos posteriores.
- Los `restore-keys` usan el patrón de prefijo decreciente: primero el OS específico, luego genérico.
- La condición `if` en el paso de instalación solo verifica `cache-hit != 'true'`, lo que cubre tanto el caso de caché inexistente como el de caché parcial por restore-key.

---

## Tabla de referencia: elementos clave de `actions/cache`

| Elemento | Tipo | Descripción | Ejemplo |
|---|---|---|---|
| `key` | Input (requerido) | Clave única que identifica el caché. Si coincide exactamente, se produce cache hit. | `${{ runner.os }}-npm-${{ hashFiles('**/package-lock.json') }}` |
| `path` | Input (requerido) | Directorio(s) o archivo(s) a guardar/restaurar. Soporta lista multilínea. | `~/.npm` |
| `restore-keys` | Input (opcional) | Prefijos de fallback evaluados en orden si `key` no coincide. El más reciente con ese prefijo se restaura. | `${{ runner.os }}-npm-` |
| `save-always` | Input (opcional) | Si es `true`, guarda el caché aunque el job falle. Por defecto: `false`. | `true` |
| `cache-hit` | Output | `true` solo si la clave principal coincidió exactamente. `false` si se usó restore-key o no hubo caché. | `steps.cache-id.outputs.cache-hit` |
| `enableCrossOsArchive` | Input (opcional) | Permite compartir cachés entre diferentes sistemas operativos. Por defecto: `false`. | `true` |

---

## Buenas y malas practicas

**Incluir el sistema operativo en la clave del caché**

Correcto:
```yaml
key: ${{ runner.os }}-npm-${{ hashFiles('**/package-lock.json') }}
```

Incorrecto:
```yaml
key: npm-cache-${{ hashFiles('**/package-lock.json') }}
```

Sin `runner.os` en la clave, un caché generado en `ubuntu-latest` podría restaurarse en `windows-latest`, donde las rutas y los binarios nativos son incompatibles. Incluir siempre el OS como prefijo evita este problema.

---

**Usar `hashFiles` con el archivo de bloqueo, no con `package.json`**

Correcto:
```yaml
key: ${{ runner.os }}-npm-${{ hashFiles('**/package-lock.json') }}
```

Incorrecto:
```yaml
key: ${{ runner.os }}-npm-${{ hashFiles('**/package.json') }}
```

`package.json` puede cambiar sin que cambien las dependencias instaladas (por ejemplo, al actualizar scripts o metadatos). `package-lock.json` refleja exactamente qué versiones están instaladas. Usar `package.json` para la clave genera invalidaciones de caché innecesarias.

---

**Definir restore-keys de granularidad decreciente**

Correcto:
```yaml
key: ${{ runner.os }}-node-20-npm-${{ hashFiles('**/package-lock.json') }}
restore-keys: |
  ${{ runner.os }}-node-20-npm-
  ${{ runner.os }}-node-
  ${{ runner.os }}-
```

Incorrecto:
```yaml
restore-keys: |
  cache-
```

Un prefijo demasiado genérico como `cache-` puede restaurar un caché de un ecosistema completamente diferente (por ejemplo, un caché de Python en un job de Node.js), causando comportamientos impredecibles.

---

**Cachear el directorio correcto del gestor de paquetes**

| Gestor | Path correcto |
|---|---|
| npm | `~/.npm` |
| yarn | `$(yarn cache dir)` o `~/.yarn/cache` |
| pip | `~/.cache/pip` |
| Maven | `~/.m2/repository` |
| Gradle | `~/.gradle/caches` |

Un error común es cachear `node_modules` directamente en lugar de `~/.npm`. Cachear `node_modules` puede incluir binarios compilados específicos del sistema y archivos de estado que no son portables entre runs, mientras que la caché global de npm contiene paquetes en formato tarball que son seguros de reutilizar.

---

## Verificacion

**Pregunta 1 (GH-200)**

Un workflow configura `actions/cache` con una clave basada en el hash del `package-lock.json`. En el siguiente run, el `package-lock.json` no ha cambiado. El paso de instalación tiene la condición `if: steps.cache.outputs.cache-hit != 'true'`. ¿Qué ocurre?

A) El paso de instalación se ejecuta porque `cache-hit` es siempre `false` en el primer uso de restore-keys  
B) El paso de instalación se omite porque `cache-hit` es `true` al coincidir la clave exacta  
C) El workflow falla porque no puede evaluar el output `cache-hit` antes de que el job termine  
D) El paso de instalación se ejecuta porque `cache-hit` solo es `true` en la primera ejecución del workflow  

**Respuesta correcta: B.** Cuando el `package-lock.json` no cambia, el hash es idéntico, la clave coincide exactamente, `cache-hit` es `true` y la condición `!= 'true'` es `false`, por lo que el paso se omite.

---

**Pregunta 2 (GH-200)**

Un repositorio tiene cachés en las ramas `main`, `feature/auth` y `feature/payments`. Un nuevo workflow se ejecuta en la rama `feature/search`. ¿Qué cachés puede restaurar?

A) Solo los cachés de `feature/search` (ninguno existe aún)  
B) Los cachés de `feature/search` y de `feature/auth` por ser ambas ramas de feature  
C) Los cachés de `feature/search` y de `main` como rama por defecto  
D) Los cachés de cualquier rama del repositorio sin restricciones  

**Respuesta correcta: C.** El scope de caché permite acceder primero a cachés de la rama actual, luego de la rama base (si es una PR) y finalmente de la rama por defecto del repositorio. Las ramas de feature no comparten cachés entre sí.

---

**Pregunta 3 (GH-200)**

Un job configura `actions/cache` sin `save-always: true`. El job restaura el caché, instala dependencias, y falla en el paso de tests. ¿Qué ocurre con el caché al final del job?

A) El caché se guarda porque la instalación de dependencias fue exitosa  
B) El caché no se guarda porque el job terminó con estado de fallo  
C) El caché se guarda parcialmente con solo los archivos del paso exitoso  
D) El caché existente se elimina para evitar que el próximo run use un caché corrupto  

**Respuesta correcta: B.** Sin `save-always: true`, `actions/cache` solo guarda el caché cuando el job termina con éxito. Un fallo en cualquier paso posterior a la restauración impide el guardado.

---

**Ejercicio practico**

Dado este workflow con un problema de configuracion de caché, identifica y corrige los errores:

```yaml
# WORKFLOW CON ERRORES - Identifica los problemas
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Caché
        uses: actions/cache@v4
        with:
          path: node_modules
          key: npm-${{ hashFiles('package.json') }}

      - name: Instalar
        if: steps.cache.outputs.cache-hit != 'true'
        run: npm install

      - name: Tests
        run: npm test
```

**Errores y correcciones:**

1. El `id` falta en el paso de caché, por lo que `steps.cache.outputs.cache-hit` no puede resolverse. Agregar `id: cache-npm` al paso.
2. El `path` apunta a `node_modules` en lugar de `~/.npm`. Cambiar a `~/.npm`.
3. La `key` usa `hashFiles('package.json')` en lugar de `hashFiles('**/package-lock.json')`. Cambiar al lockfile.
4. La clave no incluye `runner.os`, lo que puede causar conflictos entre sistemas operativos.
5. `npm install` debería ser `npm ci` para instalaciones reproducibles en CI.

---

[← 1.13 Expresiones y funciones built-in](gha-d1-expresiones.md) | [1.15 Artefactos →](gha-d1-artefactos.md)
