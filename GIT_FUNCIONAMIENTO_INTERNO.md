# GIT - FUNCIONAMIENTO INTERNO Y ARQUITECTURA

> **Objetivo:** Entender cómo funciona Git internamente, sus principios de diseño, arquitectura y estructura de datos.

---

## 📚 Tabla de Contenidos

### PARTE I: FUNDAMENTOS
1. [¿Qué es Git? - Conceptos Base](#1-qué-es-git---conceptos-base)
2. [Filosofía y Principios de Diseño](#2-filosofía-y-principios-de-diseño)
3. [El Sistema de Objetos](#3-el-sistema-de-objetos)

### PARTE II: ARQUITECTURA
4. [Base de Datos de Contenido](#4-base-de-datos-de-contenido)
5. [El Grafo de Commits](#5-el-grafo-de-commits)
6. [Sistema de Referencias](#6-sistema-de-referencias)

### PARTE III: FUNCIONAMIENTO
7. [Las Tres Áreas de Git](#7-las-tres-áreas-de-git)
8. [Cómo Git Almacena el Historial](#8-cómo-git-almacena-el-historial)
9. [Operaciones Fundamentales](#9-operaciones-fundamentales)

### PARTE IV: INTEGRACIÓN
10. [Git y GitHub Actions](#10-git-y-github-actions)
11. [Conceptos Avanzados](#11-conceptos-avanzados)

---

# PARTE I: FUNDAMENTOS

## 1. ¿Qué es Git? - Conceptos Base

### 1.1 Definición

Git es un **sistema de control de versiones distribuido** (DVCS - Distributed Version Control System).

**¿Qué significa "distribuido"?**

```
Sistema Centralizado (SVN, CVS):
┌─────────────┐
│   SERVIDOR  │  ← Única fuente de verdad
│   CENTRAL   │
└──────┬──────┘
       │
   ┌───┴───┬───────┐
   │       │       │
┌──▼──┐ ┌──▼──┐ ┌──▼──┐
│ PC1 │ │ PC2 │ │ PC3 │  ← Solo tienen copias de trabajo
└─────┘ └─────┘ └─────┘

Problemas:
- Si servidor cae, nadie puede trabajar
- Toda operación requiere conexión
- Historia centralizada


Sistema Distribuido (Git):
┌─────────────┐
│   GitHub    │  ← Servidor opcional (conveniente)
└──────┬──────┘
       │
   ┌───┴───┬───────┐
   │       │       │
┌──▼──┐ ┌──▼──┐ ┌──▼──┐
│ PC1 │ │ PC2 │ │ PC3 │  ← Cada uno tiene REPOSITORIO COMPLETO
└─────┘ └─────┘ └─────┘

Ventajas:
✓ Cada copia es un backup completo
✓ Trabajas offline (commit, branch, merge localmente)
✓ Rápido (todo es local)
✓ No hay punto único de fallo
```

### 1.2 Git NO es...

Aclaremos malentendidos comunes:

```
┌──────────────────────────────────────────────────────┐
│ ❌ Git NO es un sistema de archivos con historial   │
├──────────────────────────────────────────────────────┤
│ Git NO guarda:                                       │
│ - archivo_v1.txt                                     │
│ - archivo_v2.txt                                     │
│ - archivo_v3.txt                                     │
│                                                      │
│ Eso sería ineficiente y confuso                      │
└──────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────┐
│ ✅ Git ES una base de datos de contenido            │
├──────────────────────────────────────────────────────┤
│ Git guarda:                                          │
│ - Objetos (contenido) identificados por hash        │
│ - Referencias (punteros) a esos objetos              │
│ - Un grafo que relaciona los objetos                 │
│                                                      │
│ Eficiente, rastreable, con integridad               │
└──────────────────────────────────────────────────────┘
```

### 1.3 ¿Qué problema resuelve Git?

**Problema:** Desarrollar software con múltiples personas.

```
Sin control de versiones:
┌────────────────────────────────────────┐
│ Juan modifica login.py                 │
│ María modifica login.py                │
│ Ambos quieren enviar sus cambios       │
│ ¿Quién gana? ¿Cómo se combinan?        │
│ ¿Quién hizo qué cambio y cuándo?       │
└────────────────────────────────────────┘

Con Git:
┌────────────────────────────────────────┐
│ ✓ Historial completo de cambios       │
│ ✓ Quién hizo cada cambio               │
│ ✓ Cuándo y por qué (mensaje)           │
│ ✓ Ramas para trabajar en paralelo      │
│ ✓ Merge automático o manual            │
│ ✓ Posibilidad de volver atrás          │
└────────────────────────────────────────┘
```

---

## 2. Filosofía y Principios de Diseño

### Introducción: ¿Por qué Git funciona como funciona?

Antes de entender los detalles técnicos de Git, es fundamental comprender **la filosofía de diseño** que hay detrás. Git no fue diseñado al azar: cada decisión arquitectónica responde a problemas específicos que existían en sistemas anteriores.

**El contexto histórico:**

En 2005, Linus Torvalds (creador de Linux) necesitaba un sistema de control de versiones que:
1. Fuera **rápido** (el kernel de Linux tiene millones de líneas)
2. Fuera **distribuido** (miles de desarrolladores en todo el mundo)
3. Protegiera contra **corrupción** (código crítico)
4. Permitiera **trabajo offline** (desarrolladores sin conexión constante)
5. Soportara **desarrollo no lineal** (miles de ramas simultáneas)

Los sistemas existentes (CVS, SVN) **no cumplían** estos requisitos. Eran:
- Centralizados (dependían de un servidor)
- Lentos (operaciones por red)
- Vulnerables (falta de integridad)
- Lineales (branching costoso)

**La decisión clave:**

Linus Torvalds tomó una decisión radical: **Git no sería un sistema de archivos con historial, sino una base de datos de contenido**.

```
Sistemas tradicionales:
"Dame el archivo X de la versión Y"
→ Busca por nombre y versión
→ Puede haber inconsistencias

Git:
"Dame el contenido con hash abc123"
→ Busca por contenido
→ Imposible que sea incorrecto (el hash lo garantiza)
```

Esta decisión fundamental define todo lo demás en Git.

**Los cuatro principios fundamentales:**

Git se construyó sobre cuatro principios técnicos que resuelven los problemas mencionados:

1. **Content-Addressable Storage** → Integridad y deduplicación
2. **Snapshots, no Diffs** → Velocidad y confiabilidad
3. **Inmutabilidad** → Seguridad y rastreabilidad
4. **Todo es Local** → Velocidad y trabajo offline

Veamos cada uno en detalle.

---

### 2.1 Principio 1: Content-Addressable Storage

**Concepto:** Los objetos se identifican por su **contenido**, no por su nombre.

```
Analogía: Biblioteca

Sistema tradicional (por nombre):
- Busco libro llamado "Historia"
- Puede haber muchos libros llamados "Historia"
- ¿Cuál es el correcto?

Sistema content-addressable (por contenido):
- Busco libro con ISBN 978-3-16-148410-0
- El ISBN se deriva del contenido
- Solo hay UN libro con ese ISBN
- Si el contenido cambia, el ISBN cambia
```

**En Git:**

```
Archivo: "Hello, World"
         ↓
    SHA-1 hash
         ↓
    af5626b4a114abcb...
         ↓
    .git/objects/af/5626b4a114abcb...

Ventajas:
1. Integridad: Si el contenido se corrompe, el hash no coincide
2. Deduplicación: Contenido idéntico = mismo hash = se guarda una vez
3. Identificación única: Mismo contenido en cualquier máquina = mismo hash
```

### 2.2 Principio 2: Snapshots, no Diffs

**Concepto:** Git NO guarda diferencias entre versiones, guarda **estados completos**.

```
Sistema basado en diffs (SVN):
Versión 1: [archivo completo]
Versión 2: [+ añadir línea 5, - eliminar línea 10]
Versión 3: [+ añadir línea 2]
Versión 4: [+ cambiar línea 15]

Para ver Versión 4: Aplicar todos los diffs desde Versión 1
→ Lento, propenso a errores


Git (snapshots):
Versión 1: [snapshot del proyecto completo]
Versión 2: [snapshot del proyecto completo]
Versión 3: [snapshot del proyecto completo]
Versión 4: [snapshot del proyecto completo]

Para ver Versión 4: Leer directamente Versión 4
→ Rápido, confiable

¿No es ineficiente?
NO, porque:
- Archivos sin cambios se reutilizan (mismo hash)
- Git comprime objetos
- Git empaqueta objetos similares
```

**Visualización:**

```
Commit A:
├── README.md (hash: abc123)
├── main.py   (hash: def456)
└── utils.py  (hash: ghi789)

Commit B (solo cambió main.py):
├── README.md (hash: abc123) ← Reusa el mismo objeto
├── main.py   (hash: xyz999) ← Nuevo objeto
└── utils.py  (hash: ghi789) ← Reusa el mismo objeto

Git NO copia archivos, solo referencia objetos
```

### 2.3 Principio 3: Inmutabilidad

**Concepto:** Una vez creado un objeto, **nunca cambia**.

```
Objeto creado:
Content: "Hello"
Hash:    5d41402a...

Este objeto NUNCA cambiará.
- No se puede modificar
- No se puede corromper sin que Git lo detecte
- Es permanente (hasta que se elimine explícitamente)

Si quieres "cambiar" algo:
- NO modificas el objeto existente
- CREAS un nuevo objeto con el nuevo contenido
- El nuevo objeto tiene un nuevo hash
```

**Implicaciones:**

```
1. SEGURIDAD:
   - Imposible alterar historia sin que se note
   - Los hashes garantizan integridad

2. CONFIANZA:
   - Un hash específico SIEMPRE apunta al mismo contenido
   - af5626b4... en mi máquina = af5626b4... en tu máquina

3. DESHACER ES SEGURO:
   - "Deshacer" solo mueve punteros
   - El contenido original sigue ahí
   - Puedes recuperar casi todo
```

### 2.4 Principio 4: Todo es Local

**Concepto:** La mayoría de operaciones NO necesitan red.

```
Operaciones locales (NO necesitan internet):
✓ git log       - Ver historial completo
✓ git diff      - Ver diferencias
✓ git branch    - Crear ramas
✓ git commit    - Guardar cambios
✓ git merge     - Fusionar ramas
✓ git checkout  - Cambiar de rama
✓ git blame     - Ver quién modificó qué

Operaciones remotas (SÍ necesitan internet):
→ git fetch     - Descargar cambios
→ git pull      - Descargar y fusionar
→ git push      - Subir cambios
→ git clone     - Clonar repositorio

Ventaja:
- Trabajas en avión, tren, sin wifi
- Operaciones instantáneas (no esperas red)
- Solo sincronizas cuando quieres/puedes
```

---

## 3. El Sistema de Objetos

### Introducción: El Corazón de Git

Si Git es una base de datos, los **objetos** son sus datos. Todo en Git —cada archivo, cada directorio, cada commit— se almacena como un objeto. Pero, ¿por qué Git usa objetos en lugar de simplemente copiar archivos?

**El problema que resuelve:**

Imagina un proyecto con 1000 archivos. Haces un commit. Luego modificas 1 archivo y haces otro commit. 

**Sistema tradicional (copiar archivos):**
- Commit 1: 1000 archivos copiados
- Commit 2: 1000 archivos copiados (999 idénticos + 1 modificado)
- Total: 2000 archivos en disco
- Desperdicio: 999 archivos duplicados

**Sistema de Git (objetos):**
- Commit 1: 1000 objetos creados
- Commit 2: 1 objeto nuevo (el modificado), 999 objetos reutilizados
- Total: 1001 objetos en disco
- Eficiencia: Solo se almacena lo nuevo

**¿Cómo lo logra?**

Git separa **QUÉ es el contenido** (objetos) de **CÓMO está organizado** (referencias). Los objetos son inmutables y se identifican por su contenido (hash). Si dos archivos tienen el mismo contenido, comparten el mismo objeto.

**La arquitectura de objetos:**

Git usa **exactamente 4 tipos de objetos**. No más, no menos. Cada tipo tiene un propósito específico:

```
BLOB   → "¿Qué contiene este archivo?"
TREE   → "¿Cómo se organizan los archivos?"
COMMIT → "¿Cuál es el estado del proyecto ahora?"
TAG    → "¿Cómo marco este momento como importante?"
```

Esta simplicidad es poderosa. Con solo 4 tipos de objetos, Git puede representar proyectos de cualquier tamaño y complejidad.

**La relación entre objetos:**

Los objetos forman una jerarquía:
1. **BLOBs** almacenan contenido puro
2. **TREEs** organizan blobs en estructura de directorios
3. **COMMITs** capturan el estado completo (apuntando a un tree)
4. **TAGs** etiquetan commits importantes

```
COMMIT apunta a → TREE apunta a → BLOB
                            ↓
                         Otro TREE apunta a → BLOB
```

Esta jerarquía permite que Git sea increíblemente eficiente: si un archivo no cambió entre commits, ambos commits pueden apuntar al mismo blob.

**La clave de la eficiencia:**

Cada objeto tiene un **hash único** derivado de su contenido. Este hash es:
- Su **identificador** (nombre del objeto)
- Su **garantía de integridad** (si el contenido cambia, el hash cambia)
- Su **mecanismo de deduplicación** (mismo contenido = mismo hash = un solo objeto)

Veamos cada tipo de objeto en detalle.

---

### 3.1 Los Cuatro Tipos de Objetos

Git almacena TODO como objetos en `.git/objects/`. Existen exactamente **4 tipos**:

```
┌──────────────────────────────────────────────┐
│  1. BLOB   → Contenido de archivos           │
│  2. TREE   → Estructura de directorios       │
│  3. COMMIT → Snapshot del proyecto           │
│  4. TAG    → Etiqueta anotada                │
└──────────────────────────────────────────────┘
```

### 3.2 BLOB - Contenido de Archivos

**Función:** Almacenar el contenido de un archivo.

```
Archivo: README.md
Contenido: "# Mi Proyecto\nDescripción del proyecto"

Git crea:
┌─────────────────────────────────────┐
│ BLOB Object                         │
├─────────────────────────────────────┤
│ type: blob                          │
│ size: 35 bytes                      │
│ content: "# Mi Proyecto\nDes..."    │
└─────────────────────────────────────┘
         ↓
    SHA-1: 8d0e4123...
         ↓
.git/objects/8d/0e4123...
```

**Características importantes:**

```
El BLOB NO sabe:
❌ Su nombre de archivo (README.md)
❌ En qué directorio está
❌ Sus permisos
❌ A qué commit pertenece

Solo sabe:
✓ Su contenido puro
✓ Su tamaño

¿Por qué?
- Permite deduplicación
- Si 10 archivos tienen el mismo contenido → 1 blob
- Más eficiente
```

**Ejemplo de deduplicación:**

```
Tienes:
- file1.txt: "Hello"
- file2.txt: "Hello"
- subdir/file3.txt: "Hello"

Git crea:
- 1 BLOB con contenido "Hello"
- Los 3 archivos apuntan al mismo blob

Ahorro de espacio inmediato
```

### 3.3 TREE - Estructura de Directorios

**Función:** Almacenar nombres de archivos, permisos y estructura de carpetas.

```
Directorio:
src/
├── main.py (contenido: "print('Hi')")
└── utils.py (contenido: "def helper()...")

Git crea:
1. BLOB para main.py  → hash: abc123
2. BLOB para utils.py → hash: def456
3. TREE para src/     → hash: ghi789

┌──────────────────────────────────────────┐
│ TREE Object (src/)                       │
├──────────────────────────────────────────┤
│ 100644 blob abc123  main.py              │
│ 100644 blob def456  utils.py             │
└──────────────────────────────────────────┘
   ↑      ↑    ↑       ↑
   │      │    │       └─ Nombre del archivo
   │      │    └─ Hash del contenido (apunta al blob)
   │      └─ Tipo de objeto (blob o tree)
   └─ Modo (permisos):
      100644 = archivo regular
      100755 = archivo ejecutable
      040000 = directorio
```

**Trees pueden contener otros trees:**

```
Proyecto:
├── README.md
└── src/
    ├── main.py
    └── lib/
        └── helper.py

Git crea:
┌──────────────────────────────────────┐
│ TREE (raíz)                          │
├──────────────────────────────────────┤
│ 100644 blob a1b2c3  README.md        │
│ 040000 tree d4e5f6  src              │ ←─┐
└──────────────────────────────────────┘   │
                                           │
┌──────────────────────────────────────┐   │
│ TREE (src/)                          │ ◄─┘
├──────────────────────────────────────┤
│ 100644 blob g7h8i9  main.py          │
│ 040000 tree j0k1l2  lib              │ ←─┐
└──────────────────────────────────────┘   │
                                           │
┌──────────────────────────────────────┐   │
│ TREE (lib/)                          │ ◄─┘
├──────────────────────────────────────┤
│ 100644 blob m3n4o5  helper.py        │
└──────────────────────────────────────┘

Estructura jerárquica de trees
```

### 3.4 COMMIT - Snapshot del Proyecto

**Función:** Representar el estado completo del proyecto en un momento dado.

```
┌─────────────────────────────────────────────┐
│ COMMIT Object                               │
├─────────────────────────────────────────────┤
│ tree      abc123...   ← Apunta al TREE raíz │
│ parent    def456...   ← Commit anterior     │
│ author    Juan <j@e.com> 1706918400 +0100  │
│ committer Juan <j@e.com> 1706918400 +0100  │
│                                             │
│ Add user authentication                     │
│                                             │
│ Implemented OAuth2 login and session mgmt   │
└─────────────────────────────────────────────┘
         ↓
    SHA-1: f1e2d3c4...
```

**Componentes del commit:**

```
1. tree → Hash del tree raíz
   - Apunta al estado completo del proyecto
   - NO almacena archivos, solo referencia el tree
   
2. parent → Hash del commit anterior
   - Forma la cadena de historia
   - Puede haber 0, 1, o múltiples padres:
     * 0 padres = commit inicial
     * 1 padre = commit normal
     * 2+ padres = merge commit
   
3. author → Quién escribió el código
   - Nombre, email, timestamp, zona horaria
   
4. committer → Quién hizo el commit
   - Normalmente igual al author
   - Puede diferir (ej: aplicar parche de otro)
   
5. message → Descripción del cambio
   - Primera línea: resumen (50 chars)
   - Líneas siguientes: detalles
```

**Ejemplo de cadena de commits:**

```
Commit C:
├── tree: tree_C
├── parent: B
└── message: "Add feature X"
         ↑
         │
Commit B:
├── tree: tree_B
├── parent: A
└── message: "Fix bug Y"
         ↑
         │
Commit A:
├── tree: tree_A
├── parent: (none)
└── message: "Initial commit"

Cada commit apunta al anterior
= Historia completa
```

### 3.5 TAG - Etiqueta Anotada

**Función:** Marcar un commit específico con nombre legible.

```
┌─────────────────────────────────────────────┐
│ TAG Object                                  │
├─────────────────────────────────────────────┤
│ object    f1e2d3c4...  ← Hash del commit    │
│ type      commit                            │
│ tag       v1.0.0                            │
│ tagger    Juan <j@e.com> 1706918400 +0100  │
│                                             │
│ Release version 1.0.0                       │
│                                             │
│ Major release with new auth system          │
└─────────────────────────────────────────────┘
         ↓
    SHA-1: a1b2c3d4...
```

**Dos tipos de tags:**

```
1. Lightweight tag:
   - Solo un puntero a un commit
   - Archivo en .git/refs/tags/v1.0.0
   - Contiene: hash del commit
   - Uso: marcadores temporales

2. Annotated tag:
   - Un objeto completo
   - Con metadata (autor, fecha, mensaje)
   - Firmable (GPG)
   - Uso: releases oficiales
```

---

## 4. Base de Datos de Contenido

### Introducción: Git como Sistema de Almacenamiento

Ahora que entendemos QUÉ son los objetos, necesitamos entender CÓMO Git los almacena y recupera. Git no es solo una colección de objetos: es una **base de datos especializada** optimizada para almacenar y recuperar contenido de forma eficiente.

**¿Por qué Git necesita ser una base de datos?**

Un repositorio Git puede contener:
- Millones de archivos
- Miles de commits
- Años de historia
- Gigabytes de datos

Si Git simplemente guardara todos estos objetos en un solo directorio, sería un caos. Las operaciones serían lentas, el sistema operativo tendría problemas con tantos archivos, y la búsqueda sería ineficiente.

**La solución: Una base de datos content-addressable**

Git implementa su propia base de datos con características únicas:

1. **Direccionamiento por contenido**: Los objetos se buscan por su hash, no por nombre
2. **Estructura eficiente**: Los objetos se distribuyen en subdirectorios para velocidad
3. **Compresión automática**: Cada objeto se comprime individualmente
4. **Empaquetado inteligente**: Objetos similares se comprimen juntos para máxima eficiencia
5. **Verificación de integridad**: Cada acceso verifica que el contenido no esté corrupto

**El directorio `.git/objects/`: El corazón del repositorio**

Todo el contenido de tu proyecto —cada versión de cada archivo que has committido— vive en `.git/objects/`. Este directorio ES el repositorio. Sin él, solo tienes una copia de trabajo.

```
.git/objects/ contiene:
- Todos los archivos que has committido (como blobs)
- Toda la estructura de directorios (como trees)
- Todos los commits (como commit objects)
- Todos los tags anotados (como tag objects)

TODO está aquí, organizado por hash
```

**La evolución del almacenamiento:**

Git tiene dos modos de almacenar objetos:

1. **Loose objects (objetos sueltos)**: 
   - Cada objeto es un archivo individual
   - Rápido para escribir (git add, git commit)
   - Menos eficiente en espacio
   - Usado para objetos nuevos

2. **Packed objects (objetos empaquetados)**:
   - Múltiples objetos en un solo archivo
   - Altamente comprimido
   - Más eficiente en espacio (hasta 90% de ahorro)
   - Git automáticamente empaqueta con el tiempo

```
Tu repositorio evoluciona:

Día 1 (pocos commits):
.git/objects/
├── ab/c123...  ← Objeto suelto
├── de/f456...  ← Objeto suelto
└── gh/i789...  ← Objeto suelto

Día 100 (muchos commits):
.git/objects/
├── pack/
│   ├── pack-xyz.pack  ← Miles de objetos empaquetados
│   └── pack-xyz.idx   ← Índice para búsqueda rápida
└── [algunos objetos sueltos recientes]

Git decide cuándo empaquetar (git gc)
```

**¿Por qué importa esto?**

Entender cómo Git almacena datos te ayuda a:
- Entender por qué Git es tan rápido (todo es local, hash-indexed)
- Entender por qué clones iniciales son grandes pero posteriores pequeños (packfiles)
- Entender por qué forzar push es peligroso (objetos compartidos)
- Diagnosticar problemas de repositorio (corrupción, tamaño)

Veamos los detalles de almacenamiento.

---

### 4.1 Cómo Git Almacena Objetos

**Directorio `.git/objects/`:**

```
.git/objects/
├── 00/
├── 01/
├── ...
├── af/
│   └── 5626b4a114abcb82d63db7c8082c3c4756e51b  ← Objeto
├── ...
└── ff/

¿Por qué directorios?
- Un repositorio puede tener millones de objetos
- Poner todos en un directorio sería lento
- Git usa primeros 2 caracteres del hash como subdirectorio
- Hash: af5626b4a114abcb82d63db7c8082c3c4756e51b
       ^^  ────────────────────────────────────
       │   └─ Nombre del archivo
       └─ Subdirectorio
```

### 4.2 Formato Interno de Objetos

**Estructura:**

```
Cada objeto es:
[header][contenido]

Header: "tipo tamaño\0"
- tipo: blob, tree, commit, tag
- tamaño: bytes del contenido
- \0: null byte

Ejemplo BLOB:
"blob 13\0Hello, World!"
 ↑    ↑  ↑ ↑
 │    │  │ └─ Contenido
 │    │  └─ Null byte
 │    └─ Tamaño
 └─ Tipo

Luego se comprime con zlib y se guarda
```

**Proceso de almacenamiento:**

```
1. Contenido: "Hello, World!"
2. Crear header: "blob 13\0"
3. Combinar: "blob 13\0Hello, World!"
4. Calcular SHA-1: af5626b4a114abcb...
5. Comprimir con zlib
6. Guardar en: .git/objects/af/5626b4a114abcb...
```

### 4.3 Empaquetado (Packfiles)

**Problema:** Con el tiempo, muchos objetos similares ocupan espacio.

**Solución:** Git empaqueta objetos similares.

```
Sin empaquetar:
file_v1.txt (1000 líneas) → blob 1 (50 KB)
file_v2.txt (1001 líneas) → blob 2 (50 KB)
file_v3.txt (1002 líneas) → blob 3 (50 KB)
Total: 150 KB

Empaquetado:
file_v1.txt → blob completo (50 KB)
file_v2.txt → delta desde v1 (100 bytes)
file_v3.txt → delta desde v2 (100 bytes)
Total: 50.2 KB

¡Ahorro del 66%!
```

**Estructura de packfiles:**

```
.git/objects/pack/
├── pack-abc123.idx   ← Índice (dónde está cada objeto)
└── pack-abc123.pack  ← Datos comprimidos

Git automáticamente:
- Empaqueta al hacer git gc (garbage collection)
- Empaqueta al hacer git push
- Desempaqueta al acceder objetos
```

---

## 5. El Grafo de Commits

### Introducción: La Historia como Grafo

Has visto que Git almacena objetos. Pero los objetos por sí solos son solo datos sin contexto. ¿Cómo sabe Git cuál es la historia del proyecto? ¿Cómo relaciona los commits entre sí? ¿Cómo puede manejar múltiples líneas de desarrollo paralelas?

La respuesta: **Git usa un grafo**.

**¿Por qué un grafo?**

La historia de un proyecto de software NO es lineal. Múltiples personas trabajan en paralelo, se crean branches, se fusionan cambios, se experimentan ideas que luego se descartan. Intentar representar esto como una lista simple sería limitante.

```
Historia lineal (limitante):
A → B → C → D → E
Solo una línea de desarrollo

Historia como grafo (poderoso):
       A ← B ← C ← F ← G    (main)
            ↖     ↗
              D ← E           (feature)
Múltiples líneas de desarrollo que convergen
```

**¿Qué es un Grafo Acíclico Dirigido (DAG)?**

Git usa un tipo específico de grafo con propiedades importantes:

1. **Dirigido**: Las conexiones tienen dirección (hijo → padre)
   - Cada commit "apunta" a su padre
   - Puedes seguir la historia hacia atrás
   - No puedes ir hacia adelante (no sabemos el futuro)

2. **Acíclico**: No hay ciclos
   - Un commit no puede ser su propio ancestro
   - No puedes volver al mismo commit siguiendo las flechas
   - La historia siempre fluye en una dirección: hacia el pasado

3. **Grafo**: Nodos (commits) conectados por aristas (relaciones padre-hijo)
   - Los nodos son commits
   - Las aristas representan "es hijo de"
   - Puede haber bifurcaciones y convergencias

**¿Por qué esta estructura es poderosa?**

El grafo permite:

1. **Desarrollo paralelo**: Múltiples branches pueden existir simultáneamente
   ```
   A ← B ← C        (rama 1)
       ↖
         D ← E      (rama 2)
         ↖
           F ← G    (rama 3)
   ```

2. **Fusión de trabajo**: Los branches pueden converger (merge)
   ```
   A ← B ← C ← M    M combina el trabajo de C y E
       ↖     ↗
         D ← E
   ```

3. **Historia completa**: Puedes rastrear cómo llegaste a cualquier punto
   ```
   Desde M: Seguir padres → C → B → A (una rama)
                         → E → D → B → A (otra rama)
   ```

4. **Trabajo distribuido**: Cada desarrollador tiene su propio subgrafo
   ```
   Desarrollador 1:     Desarrollador 2:
   A ← B ← C            A ← B ← D
   
   Después de sincronizar:
   A ← B ← C
       ↖
         D
   ```

**La implicación clave:**

Cuando haces `git log`, no estás viendo una "lista de commits". Estás navegando un **grafo**. Git:
1. Empieza en HEAD (dónde estás)
2. Sigue los punteros de padre en padre
3. Muestra todos los commits alcanzables
4. Se detiene cuando no hay más padres

Esto explica por qué:
- `git log main..feature` muestra commits en feature pero no en main
- Un commit puede aparecer en múltiples branches
- Puedes "perder" commits si ninguna referencia los apunta

**El poder del grafo:**

El grafo no es solo una estructura técnica: define **cómo piensas sobre tu código**:
- Los commits no son "versiones numeradas" (v1, v2, v3...)
- Son **nodos en un grafo** de decisiones y desarrollo
- Los branches no son "copias del código"
- Son **punteros que navegan el grafo**

Entender el grafo es entender Git.

---

### 5.1 Concepto de Grafo

**Git usa un Grafo Acíclico Dirigido (DAG - Directed Acyclic Graph):**

```
Grafo:
- Nodos: commits
- Aristas: relaciones padre-hijo
- Dirigido: las aristas tienen dirección (hijo → padre)
- Acíclico: no hay ciclos (no puedes volver al mismo commit)

Ejemplo simple:
A ← B ← C ← D
│   │   │   │
└───┴───┴───┴─ Cada nodo es un commit
    ←───←───← Las flechas apuntan al padre
```

### 5.2 Historia Lineal

```
Repositorio nuevo:

A                    (primer commit)
│
├── tree: tree_A
├── parent: (none)
└── msg: "Initial commit"

Hacer segundo commit:

A ← B
    │
    ├── tree: tree_B
    ├── parent: A     ← Apunta al anterior
    └── msg: "Add feature"

Hacer tercer commit:

A ← B ← C
        │
        ├── tree: tree_C
        ├── parent: B  ← Apunta al anterior
        └── msg: "Fix bug"

Historia lineal simple
```

### 5.3 Historia con Ramas

```
Crear rama desde B:

       main
         ↓
A ← B ← C
     ↖
       D ← E
           ↑
        feature

- C está en main
- D y E están en feature
- Ambas ramas comparten A y B
- Historia diverge desde B
```

**Internamente:**

```
Commits:
C ← parent: B
D ← parent: B  (mismo padre que C)
E ← parent: D

Referencias:
.git/refs/heads/main    → hash de C
.git/refs/heads/feature → hash de E

No se copian commits, solo hay punteros
```

### 5.4 Merge Commits

```
Fusionar feature en main:

ANTES:
       A ← B ← C      (main)
            ↖
              D ← E   (feature)

DESPUÉS:
       A ← B ← C ← M  (main)
            ↖     ↗
              D ← E   (feature)

M es un merge commit:
├── tree: tree_M
├── parent: C       ← Primer padre
├── parent: E       ← Segundo padre
└── msg: "Merge feature"

¡M tiene DOS padres!
- Primer padre: donde estabas (C)
- Segundo padre: lo que mergeaste (E)
```

**Importancia de los padres:**

```
Para reconstruir historia:
- Desde M, puedo ir a C (primer padre)
- Desde M, puedo ir a E (segundo padre)
- Git sigue ambos caminos para mostrar log
- git log muestra: M, C, E, D, B, A
```

### 5.5 Navegación del Grafo

**Sintaxis de referencias relativas:**

```
Commits:
A ← B ← C ← D ← E
                ↑
              HEAD

Navegación:
HEAD      → E  (donde estás)
HEAD^     → D  (un padre atrás)
HEAD^^    → C  (dos padres atrás)
HEAD~3    → B  (tres generaciones atrás)

Diferencia entre ^ y ~:
HEAD^  = primer padre (importante en merges)
HEAD~  = ancestro (siempre primer padre)
```

**Con merge commits:**

```
       A ← B ← C ← M
            ↖     ↗
              D ← E

M tiene dos padres: C y E

HEAD = M
HEAD^ = C    (primer padre)
HEAD^2 = E   (segundo padre)
HEAD~1 = C   (un ancestro atrás, siempre primer padre)
HEAD~2 = B   (dos ancestros atrás)
```

---

## 6. Sistema de Referencias

### Introducción: Navegar el Grafo

Ya sabes que Git almacena objetos en una base de datos y que los commits forman un grafo. Pero hay un problema: los hashes SHA-1 son imposibles de recordar.

```
¿Qué prefieres escribir?
git checkout a1b2c3d4e5f6789012345678901234567890abcd
                    VS
git checkout main
```

**El problema de los hashes:**

Los hashes son perfectos para Git (únicos, verificables, inmutables), pero terribles para humanos:
- Imposibles de memorizar
- Difíciles de comunicar ("checkout al commit a-uno-be-dos...")
- Propensos a errores al escribir
- No transmiten significado ("¿qué era abc123?")

**La solución: Referencias**

Git introduce una capa de **referencias** (refs): nombres legibles que apuntan a commits.

```
Sin referencias:
"Ve al commit a1b2c3d4e5f6789012345678901234567890abcd"
Difícil, propenso a errores

Con referencias:
"Ve a main"
Fácil, claro, sin errores
```

**Pero las referencias son más que conveniencia**

Las referencias no solo hacen Git más amigable: **definen la estructura de trabajo**:

1. **Branches (ramas)** = referencias móviles
   - Se mueven automáticamente al hacer commit
   - Representan líneas de desarrollo activas
   - Permiten trabajo en paralelo

2. **Tags (etiquetas)** = referencias fijas
   - No se mueven
   - Marcan puntos importantes (releases)
   - Documentan la historia

3. **HEAD** = referencia especial
   - Indica dónde estás ahora
   - Determina qué cambia al hacer commit
   - Es tu "posición actual" en el grafo

**El sistema de referencias es un índice del grafo**

Piensa en el grafo de commits como una ciudad enorme. Las referencias son:
- **Calles con nombre** (main, feature, develop)
- **Monumentos** (v1.0.0, v2.0.0)
- **Tu ubicación actual** (HEAD)

Sin referencias, tendrías que navegar usando coordenadas (hashes). Con referencias, usas nombres significativos.

**La estructura del directorio `.git/refs/`:**

```
.git/refs/
├── heads/         ← Branches locales (ramas de trabajo)
├── remotes/       ← Branches remotas (ramas de otros repos)
└── tags/          ← Tags (marcadores permanentes)

Cada archivo contiene un hash SHA-1 de 40 caracteres
Eso es todo: un puntero simple
```

**¿Por qué esta separación es poderosa?**

Separar referencias de objetos permite:

1. **Mover referencias sin tocar objetos**
   - Cambiar de branch = cambiar puntero (instantáneo)
   - Crear branch = crear archivo con hash (instantáneo)
   - Eliminar branch = eliminar archivo (instantáneo)
   - Los objetos nunca se tocan

2. **Múltiples referencias al mismo commit**
   ```
   Commit C puede estar en:
   - main
   - develop
   - v1.0.0
   - origin/main
   
   Un solo commit, múltiples nombres
   ```

3. **Trabajo distribuido sin conflictos**
   ```
   Tu main ≠ origin/main (pueden diferir)
   Tu feature-x ≠ la feature-x de otro
   
   Las referencias son locales
   ```

4. **Recuperación de "commits perdidos"**
   ```
   Git guarda un reflog (log de referencias)
   Si mueves una referencia, el commit anterior sigue existiendo
   Puedes recuperarlo del reflog
   ```

**La jerarquía de referencias:**

```
HEAD apunta a → rama apunta a → commit

Ejemplo:
HEAD → refs/heads/main → a1b2c3d4... → [commit object]

Cuando haces commit:
1. Se crea nuevo commit b2c3d4e5...
2. refs/heads/main se actualiza a b2c3d4e5...
3. HEAD sigue apuntando a refs/heads/main
4. Resultado: HEAD → main → b2c3d4e5...
```

**El poder de las referencias:**

Las referencias transforman Git de "una base de datos de objetos" a "un sistema de control de versiones usable". Sin referencias:
- ❌ No habría branches
- ❌ No habría forma fácil de navegar
- ❌ No habría colaboración práctica
- ❌ Todo serían hashes crípticos

Con referencias:
- ✅ Branches significativos (feature-login, bugfix-auth)
- ✅ Navegación intuitiva (git checkout main)
- ✅ Colaboración clara (pull de origin/main)
- ✅ Documentación histórica (tags v1.0.0)

Veamos cómo funciona cada tipo de referencia.

---

### 6.1 ¿Qué son las Referencias?

**Problema:** Los hashes SHA-1 son difíciles de recordar.

```
Sin referencias:
git checkout a1b2c3d4e5f6789012345678901234567890abcd
                ↑
          Difícil de usar

Con referencias:
git checkout main
             ↑
       Fácil de recordar
```

**Definición:** Una referencia es un **puntero** a un commit.

```
Archivo: .git/refs/heads/main
Contenido: a1b2c3d4e5f6789012345678901234567890abcd

"main" es una referencia que apunta al commit a1b2c3d4...
```

### 6.2 Tipos de Referencias

```
.git/refs/
├── heads/         ← Ramas locales
│   ├── main
│   └── feature
├── remotes/       ← Ramas remotas (copias)
│   └── origin/
│       ├── main
│       └── develop
└── tags/          ← Tags
    └── v1.0.0
```

**1. Ramas locales (`refs/heads/`):**

```
Archivo: .git/refs/heads/main
Contenido: f1e2d3c4e5f6...

¿Qué es una rama?
- Un puntero móvil a un commit
- Cuando haces commit, la rama se mueve
- Solo es un archivo con 40 bytes (el hash)
```

**2. Ramas remotas (`refs/remotes/origin/`):**

```
Archivo: .git/refs/remotes/origin/main
Contenido: a1b2c3d4e5f6...

¿Qué es origin/main?
- Copia local de la rama main en el servidor
- Se actualiza con git fetch
- Es read-only (no haces commit directamente)
- Muestra el estado del servidor la última vez que sincronizaste
```

**3. Tags (`refs/tags/`):**

```
Lightweight tag:
Archivo: .git/refs/tags/v1.0.0
Contenido: a1b2c3d4e5f6... (hash del commit)

Annotated tag:
Archivo: .git/refs/tags/v2.0.0
Contenido: x1y2z3w4... (hash del TAG object)

Tag object contiene:
- Hash del commit que etiqueta
- Nombre del tag
- Mensaje
- Autor, fecha
```

### 6.3 HEAD - La Referencia Especial

**HEAD indica dónde estás ahora.**

**Modo normal (attached):**

```
Archivo: .git/HEAD
Contenido: ref: refs/heads/main

HEAD apunta a una rama:
HEAD → main → commit C

Cuando haces commit:
- Se crea nuevo commit D
- main se mueve a D
- HEAD sigue apuntando a main
- Resultado: HEAD → main → commit D
```

**Modo detached:**

```
Archivo: .git/HEAD
Contenido: a1b2c3d4e5f6... (hash directo)

HEAD apunta directamente a un commit:
HEAD → commit B (sin rama)

Peligro:
- Commits nuevos no están en ninguna rama
- Al cambiar de rama, puedes "perder" el trabajo
```

**Visualización:**

```
Attached HEAD:
┌──────┐    ┌──────┐    ┌────────┐
│ HEAD │ → │ main │ → │ commit │
└──────┘    └──────┘    └────────┘
              ↑
           se mueve

Detached HEAD:
┌──────┐             ┌────────┐
│ HEAD │ ─────────→ │ commit │
└──────┘             └────────┘
   ↑
se mueve directamente
```

### 6.4 Reflog - Historial de Referencias

**Git mantiene un log de TODOS los cambios en referencias.**

```
Archivo: .git/logs/HEAD

Contenido:
0000000 a1b2c3d Juan <j@e.com> 1706900000 +0100  commit (initial): Initial
a1b2c3d e5f6a7b Juan <j@e.com> 1706901000 +0100  commit: Add feature
e5f6a7b f1e2d3c Juan <j@e.com> 1706902000 +0100  commit: Fix bug
f1e2d3c a1b2c3d Juan <j@e.com> 1706903000 +0100  checkout: moving to HEAD~2

Cada línea:
[hash anterior] [hash nuevo] [quien] [cuando] [acción]
```

**Uso del reflog:**

El reflog es como una **máquina del tiempo personal** de Git. Registra cada movimiento de HEAD, incluso los que parecen "destructivos". 

---

### **Cómo Leer el Output del Reflog: Anatomía Línea por Línea**

Veamos un reflog real y analicemos cada componente:

```bash
git reflog
```

**Output real:**
```
9674d15 (HEAD) HEAD@{0}: checkout: moving from bdcadc73ac16f26dbc766ace5c12a88748a5011b to HEAD@{12}
bdcadc7 (origin/feature, feature) HEAD@{1}: checkout: moving from 63c00f9bc6c78fea829816c93147e061c1c663a5 to HEAD@{1}
63c00f9 (origin/main, origin/HEAD, main) HEAD@{2}: checkout: moving from feature to HEAD@{3}
bdcadc7 (origin/feature, feature) HEAD@{3}: commit: new file
1118364 HEAD@{4}: commit: new file
63c00f9 (origin/main, origin/HEAD, main) HEAD@{5}: checkout: moving from main to feature
63c00f9 (origin/main, origin/HEAD, main) HEAD@{6}: commit: new
462a524 HEAD@{7}: commit: new
411d47c HEAD@{8}: commit: new
2c89456 HEAD@{9}: commit: new
9972a09 HEAD@{10}: commit: new
0e5124c HEAD@{11}: commit: new
7312698 HEAD@{12}: commit: new
9674d15 (HEAD) HEAD@{13}: clone: from https://github.com/dukono/testsWithGitHubAction.git
```

---

### **Desglose de una línea del reflog:**

Tomemos una línea como ejemplo:
```
bdcadc7 (origin/feature, feature) HEAD@{3}: commit: new file
```

**Componentes:**

```
bdcadc7  HEAD@{3}  commit:  new file
   │        │         │         │
   │        │         │         └─── Mensaje descriptivo de la acción
   │        │         └────────────── Tipo de acción (commit, checkout, reset, etc.)
   │        └──────────────────────── Posición en el reflog (más bajo = más reciente)
   └───────────────────────────────── SHA corto del commit (donde estaba HEAD)

(origin/feature, feature)  ← Referencias que apuntan a este commit
```

---

### **Tipos de acciones en el reflog:**

El reflog registra diferentes tipos de operaciones:

```bash
# 1. CHECKOUT (cambiar de rama/commit)
HEAD@{2}: checkout: moving from feature to HEAD@{3}
             │       │             │         │
             │       │             └─ Desde ─┘
             │       └─ "moving from X to Y"
             └─ Tipo de acción

# 2. COMMIT (crear un commit)
HEAD@{3}: commit: new file
          │       │
          │       └─ Mensaje del commit
          └─ Acción

# 3. CLONE (clonar repositorio)
HEAD@{13}: clone: from https://github.com/dukono/testsWithGitHubAction.git
           │      │
           │      └─ URL del repo clonado
           └─ Primera entrada (inicio del repo)

# 4. RESET (mover HEAD hacia atrás)
HEAD@{0}: reset: moving to HEAD~2
          │      │
          │      └─ A dónde se movió
          └─ Acción peligrosa (puede "perder" commits)

# 5. REBASE
HEAD@{5}: rebase (start): checkout main
HEAD@{4}: rebase (pick): Apply commit xyz
HEAD@{3}: rebase (finish): returning to refs/heads/feature
          │       │         │
          │       │         └─ Fase del rebase
          │       └─ Tipo de operación dentro del rebase
          └─ Operación compleja con múltiples pasos

# 6. MERGE
HEAD@{2}: merge feature: Merge made by 'recursive' strategy
          │     │        │
          │     │        └─ Estrategia de merge usada
          │     └─ Rama mergeada
          └─ Acción

# 7. PULL
HEAD@{1}: pull: Fast-forward
          │     │
          │     └─ Tipo de merge (fast-forward o recursive)
          └─ Acción (fetch + merge)

# 8. CHERRY-PICK
HEAD@{0}: cherry-pick: abc123f
          │            │
          │            └─ Commit cherry-pickeado
          └─ Acción
```

---

### **Interpretando las referencias (origin/feature, feature, HEAD)**

```
9674d15 (HEAD) HEAD@{0}: ...
        └─────┘
         HEAD apunta aquí AHORA

bdcadc7 (origin/feature, feature) HEAD@{1}: ...
        └────────────────────────┘
         Dos referencias apuntan aquí:
         - feature (rama local)
         - origin/feature (rama remota)

63c00f9 (origin/main, origin/HEAD, main) HEAD@{2}: ...
        └────────────────────────────────┘
         Tres referencias:
         - main (rama local)
         - origin/main (rama remota)
         - origin/HEAD (rama default del remoto)
```

**¿Qué significa esto?**
- Si ves `(HEAD)` → Ahí estás TÚ ahora
- Si ves `(origin/X, X)` → Rama local y remota sincronizadas
- Si solo ves `(X)` → Solo rama local (no pusheada o sin remoto)

---

### **Navegando el Reflog: De más reciente a más antiguo**

```
HEAD@{0}  ← Ahora (lo más reciente)
HEAD@{1}  ← Hace 1 operación
HEAD@{2}  ← Hace 2 operaciones
HEAD@{3}  ← Hace 3 operaciones
...
HEAD@{13} ← Hace 13 operaciones (el inicio en este caso: clone)
```

**Regla:** Números más bajos = más reciente
**Regla:** HEAD@{0} siempre es DONDE ESTÁS AHORA

---

### **Ejemplo Real Interpretado: Tu Reflog Completo**

Vamos línea por línea con tu ejemplo:

```bash
# LÍNEA 1 (presente - HEAD@{0})
9674d15 (HEAD) HEAD@{0}: checkout: moving from bdcadc73... to HEAD@{12}
                         └─ Hiciste checkout a HEAD@{12}
                         └─ Ahora HEAD está en 9674d15
                         └─ Este commit es el mismo que HEAD@{12}

# LÍNEA 2 (1 operación atrás - HEAD@{1})
bdcadc7 (origin/feature, feature) HEAD@{1}: checkout: moving from 63c00f9bc... to HEAD@{1}
                                             └─ Hiciste checkout a HEAD@{1} (recursivo)
                                             └─ HEAD estaba en bdcadc7

# LÍNEA 3 (2 operaciones atrás - HEAD@{2})
63c00f9 (origin/main, origin/HEAD, main) HEAD@{2}: checkout: moving from feature to HEAD@{3}
                                                    └─ Cambiaste de rama "feature" a HEAD@{3}
                                                    └─ HEAD estaba en 63c00f9 (main)

# LÍNEA 4 (3 operaciones atrás - HEAD@{3})
bdcadc7 (origin/feature, feature) HEAD@{3}: commit: new file
                                             └─ Hiciste un commit
                                             └─ Mensaje: "new file"
                                             └─ Estabas en rama "feature"

# LÍNEA 5 (4 operaciones atrás - HEAD@{4})
1118364 HEAD@{4}: commit: new file
                  └─ Otro commit
                  └─ No tiene referencias (commit intermedio)

# LÍNEA 6 (5 operaciones atrás - HEAD@{5})
63c00f9 (origin/main, origin/HEAD, main) HEAD@{5}: checkout: moving from main to feature
                                                    └─ Cambiaste de main a feature
                                                    └─ Estabas en main (63c00f9)

# LÍNEAS 7-12 (6-11 operaciones atrás)
63c00f9, 462a524, 411d47c, 2c89456, 9972a09, 0e5124c HEAD@{6-11}: commit: new
└─ Serie de commits en main
└─ Todos con mensaje "new"

# LÍNEA 13 (12 operaciones atrás - HEAD@{12})
7312698 HEAD@{12}: commit: new
        └─ Commit en main

# LÍNEA 14 (13 operaciones atrás - inicio - HEAD@{13})
9674d15 (HEAD) HEAD@{13}: clone: from https://github.com/dukono/testsWithGitHubAction.git
               └─ Primera acción: clonar el repo
               └─ Inicio de tu reflog
```

---

### **Reconstruyendo la Historia Desde el Reflog**

Basado en tu reflog, esto es lo que hiciste:

```
1. Clonaste el repo (HEAD@{13})
   └─ HEAD en 9674d15

2. Hiciste 6 commits en main (HEAD@{12} a HEAD@{6})
   └─ Todos con mensaje "new"
   └─ Último commit: 63c00f9

3. Cambiaste a rama "feature" (HEAD@{5})
   └─ git checkout feature

4. Hiciste 2 commits en "feature" (HEAD@{4} y HEAD@{3})
   └─ 1118364 y bdcadc7

5. Hiciste checkouts recursivos navegando el reflog:
   └─ checkout from feature to HEAD@{3} (HEAD@{2})
   └─ checkout to HEAD@{1} (HEAD@{1})
   └─ checkout to HEAD@{12} (HEAD@{0})  ← Volviste al commit 7312698
```

**Resultado final:**
- HEAD está en 9674d15 (el commit inicial después del clone)
- Rama "feature" sigue en bdcadc7
- Rama "main" sigue en 63c00f9

---

### **Caso Práctico: Recuperar Trabajo "Perdido"**

Imagina que después de hacer `checkout to HEAD@{12}`, te das cuenta que querías estar en "feature":

```bash
# Ver dónde está feature
git reflog | grep "feature"
bdcadc7 (origin/feature, feature) HEAD@{1}: ...
bdcadc7 (origin/feature, feature) HEAD@{3}: commit: new file

# feature está en bdcadc7, que es HEAD@{3}

# Recuperar:
git checkout feature
# O directamente:
git checkout HEAD@{3}

# O crear rama de respaldo:
git branch feature-backup HEAD@{3}
```

---

### **Comandos Útiles para Explorar el Reflog**

```bash
# Ver reflog completo
git reflog

# Ver reflog con fechas (más legible)
git reflog --date=relative
# Output: HEAD@{5 minutes ago}, HEAD@{2 hours ago}, etc.

# Ver reflog con timestamps exactos
git reflog --date=iso
# Output: HEAD@{2026-02-03 10:30:45 +0100}

# Ver reflog de una rama específica
git reflog show feature
git reflog show main

# Ver qué había en un punto específico
git show HEAD@{5}
git log HEAD@{5} --oneline -5

# Comparar dos puntos del reflog
git diff HEAD@{5}..HEAD@{2}

# Ver archivos en un punto del reflog
git ls-tree HEAD@{3}
```

---

### **Resumen: Qué Significa Cada Parte**

```
SHA  (refs)  HEAD@{n}:  tipo:  descripción
 │      │        │        │         │
 │      │        │        │         └─ Qué pasó
 │      │        │        └─────────── Tipo de acción
 │      │        └──────────────────── Posición (0=ahora, más=pasado)
 │      └───────────────────────────── Referencias que apuntan aquí
 └──────────────────────────────────── Dónde estaba HEAD
```

El reflog es tu **historia personal** en Git. Cada línea es un paso que diste. Con esta información puedes volver a cualquier punto del pasado, recuperar trabajo "perdido", y entender exactamente qué operaciones hiciste.

---

## 7. Las Tres Áreas de Git

### Introducción: El Flujo de Trabajo de Git

Hasta ahora hemos visto la estructura interna de Git: objetos, grafos, referencias. Pero cuando trabajas día a día con Git, interactúas con algo diferente: **las tres áreas de trabajo**.

**¿Por qué tres áreas?**

La mayoría de sistemas de control de versiones tienen dos estados:
1. Archivos modificados
2. Archivos commiteados

Git tiene **tres**:
1. Working Directory (directorio de trabajo)
2. Staging Area (área de preparación)
3. Repository (repositorio)

¿Por qué esta complejidad? Porque Git te da **control granular** sobre qué commiteas.

**El problema que resuelve:**

Imagina esta situación común:

```
Estás trabajando en una feature, modificaste 5 archivos:
- login.py      (nueva funcionalidad)
- auth.py       (nueva funcionalidad)  
- config.py     (nueva funcionalidad)
- debug.py      (código de debug temporal)
- test_data.py  (datos de prueba temporal)

¿Quieres commitear los 5 archivos juntos?
NO - Solo quieres commitear la nueva funcionalidad (3 archivos)
```

**Sistema de 2 áreas (otros VCS):**
```
Modificados: los 5 archivos
Commit: los 5 archivos (no hay opción)
→ Commit sucio con código temporal
```

**Sistema de 3 áreas (Git):**
```
Working:  5 archivos modificados
Staging:  solo los 3 archivos de feature (tú eliges)
Commit:   solo los 3 archivos staged
→ Commit limpio, profesional
```

**La arquitectura de tres capas:**

```
┌─────────────────────────────────────────────┐
│ 1. WORKING DIRECTORY                        │
│    "Tu espacio de trabajo"                  │
│    - Modificas archivos                     │
│    - Experimentas                           │
│    - Es tu disco duro                       │
└──────────────┬──────────────────────────────┘
               │ git add (preparar)
               ▼
┌─────────────────────────────────────────────┐
│ 2. STAGING AREA (INDEX)                     │
│    "La sala de espera"                      │
│    - Archivos preparados                    │
│    - Listo para commit                      │
│    - Puedes ajustar antes de commitear      │
└──────────────┬──────────────────────────────┘
               │ git commit (guardar)
               ▼
┌─────────────────────────────────────────────┐
│ 3. REPOSITORY                               │
│    "La historia permanente"                 │
│    - Commits guardados                      │
│    - Inmutable                              │
│    - Tu base de datos de versiones          │
└─────────────────────────────────────────────┘
```

**¿Por qué el staging area es revolucionario?**

El staging area (también llamado "index") es la innovación clave de Git que otros sistemas no tienen. Te permite:

1. **Commits atómicos**: Cada commit representa un cambio lógico único
   ```
   En lugar de:
   "Fixed login, added tests, updated docs, removed debug code"
   
   Puedes hacer:
   Commit 1: "Fixed login bug"
   Commit 2: "Added tests for login"
   Commit 3: "Updated documentation"
   (Sin incluir el debug code)
   ```

2. **Staging parcial**: Commitear parte de un archivo
   ```
   Modificaste 100 líneas en un archivo:
   - 50 líneas de Feature A
   - 50 líneas de Feature B
   
   Con staging parcial:
   Commit 1: Solo las 50 líneas de Feature A
   Commit 2: Solo las 50 líneas de Feature B
   ```

3. **Revisión antes de commitear**:
   ```
   Working → modificas código
   Staging → revisas qué vas a commitear
            → ajustas si algo no debe ir
   Commit  → guardas con confianza
   ```

4. **Workflow flexible**:
   ```
   Puedes:
   - Añadir archivos de uno en uno
   - Quitar archivos del staging
   - Modificar archivos después de añadirlos
   - Resetear todo el staging
   - Ver diferencias en cada paso
   ```

**El flujo completo:**

```
1. Modificas archivos
   Working: modificado
   Staging: vacío
   Repo:    anterior

2. git add file.txt
   Working: modificado
   Staging: preparado ← Git crea blob aquí
   Repo:    anterior

3. Modificas file.txt de nuevo (!)
   Working: nueva modificación
   Staging: versión anterior (la que hiciste add)
   Repo:    anterior

4. git add file.txt (otra vez)
   Working: nueva modificación
   Staging: nueva modificación
   Repo:    anterior

5. git commit
   Working: nueva modificación
   Staging: nueva modificación
   Repo:    commiteado ← Git crea commit aquí
```

**¿Dónde están físicamente estas áreas?**

```
Working Directory:
→ Tu sistema de archivos normal
→ Los archivos que ves en tu explorador

Staging Area:
→ .git/index (archivo binario)
→ Lista de archivos y sus hashes

Repository:
→ .git/objects/ (objetos)
→ .git/refs/ (referencias)
→ La base de datos de Git
```

**La potencia del modelo:**

Este modelo de tres áreas permite que Git sea:
- **Flexible**: Control total sobre qué commiteas
- **Seguro**: Puedes experimentar en working sin afectar repo
- **Profesional**: Commits limpios y organizados
- **Potente**: Staging parcial, múltiples estados

Sin el staging area, Git sería solo otro sistema de control de versiones. Con él, es una herramienta profesional de gestión de cambios.

Veamos cada área en detalle.

---

### 7.1 Arquitectura de Tres Capas

```
┌─────────────────────────────────────────────┐
│ 1. WORKING DIRECTORY (Directorio de Trabajo)│
│    - Archivos que ves y editas              │
│    - Tu sistema de archivos normal          │
└──────────────────┬──────────────────────────┘
                   │ git add
                   ▼
┌─────────────────────────────────────────────┐
│ 2. STAGING AREA / INDEX (Área de Staging)   │
│    - Archivos preparados para commit        │
│    - Archivo: .git/index                    │
└──────────────────┬──────────────────────────┘
                   │ git commit
                   ▼
┌─────────────────────────────────────────────┐
│ 3. REPOSITORY (Repositorio)                 │
│    - Commits guardados en .git/objects      │
│    - Historia permanente                    │
└─────────────────────────────────────────────┘
```

### 7.2 Working Directory

**Es tu sistema de archivos normal.**

```
Estado de archivos:
- Untracked: Git no los rastrea
- Tracked: Git los rastrea
  - Unmodified: Sin cambios desde último commit
  - Modified: Modificado pero no en staging
  - Staged: En staging, listo para commit
```

**Ejemplo:**

```
$ git status

On branch main
Changes not staged for commit:
  modified:   file1.txt     ← Modified

Untracked files:
  newfile.txt               ← Untracked

¿Qué significa?
- file1.txt: Git lo conoce, pero tiene cambios no staged
- newfile.txt: Git no lo rastrea aún
```

### 7.3 Staging Area (Index)

**Función:** Preparar el próximo commit.

**Estructura del index:**

```
Archivo: .git/index (binario)

Contiene lista de archivos con:
- Path: ruta/nombre del archivo
- Hash: SHA-1 del contenido
- Mode: permisos
- Size: tamaño
- Timestamps: modificación

Ejemplo:
100644 a1b2c3d4... file1.txt
100644 e5f6a7b8... file2.txt
100755 f1e2d3c4... script.sh
```

**¿Por qué existe el staging area?**

```
Ventaja: Control granular

Sin staging (otros sistemas):
- Modificas 5 archivos
- Commit incluye los 5
- No puedes separar cambios lógicos

Con staging (Git):
- Modificas 5 archivos
- git add file1.txt file2.txt
- git commit (solo file1 y file2)
- git add file3.txt
- git commit (solo file3)
- Commits atómicos y organizados
```

**Staging parcial:**

```
Modificaste un archivo con:
- Cambio A (líneas 10-20)
- Cambio B (líneas 50-60)

git add -p file.txt
- Te pregunta por cada "hunk"
- Puedes elegir qué cambios stagear
- Commit 1: Cambio A
- Commit 2: Cambio B

Commits ultra-granulares
```

### 7.4 Repository

**La base de datos de Git en `.git/`**

```
.git/
├── objects/       ← Todos los objetos (blobs, trees, commits, tags)
├── refs/          ← Referencias (ramas, tags)
├── HEAD           ← Donde estás ahora
├── index          ← Staging area
├── config         ← Configuración
└── logs/          ← Reflog

El repository contiene:
✓ Historia completa
✓ Todos los commits
✓ Todas las ramas
✓ Todos los tags
```

**Commits son inmutables:**

```
Una vez haces git commit:
- Se crean objetos (blob, tree, commit)
- Se guardan en objects/
- Se actualizan referencias
- NO se puede cambiar (inmutable)

Para "cambiar" un commit:
- NO modificas el existente
- CREAS uno nuevo
- MUEVES la referencia
```

---

## 8. Cómo Git Almacena el Historial

### Introducción: De Archivos a Historia

Ya conoces las piezas individuales de Git:
- Objetos (blobs, trees, commits, tags)
- El grafo de commits
- Las referencias
- Las tres áreas de trabajo

Pero, ¿cómo se unen todas estas piezas? ¿Qué sucede **realmente** cuando haces `git commit`? ¿Cómo se transforma tu código en historia versionada?

**El viaje de un archivo:**

```
1. Escribes código
   → Archivo en disco: "print('Hello')"

2. git add
   → Archivo se convierte en blob
   → Se guarda en .git/objects/
   → Se registra en .git/index

3. git commit
   → Blob se organiza en tree
   → Tree se asocia a commit
   → Commit se añade al grafo
   → Referencia se actualiza

4. Resultado
   → Tu código es ahora parte de la historia
   → Está versionado, rastreable, recuperable
```

**¿Por qué entender esto importa?**

Cuando entiendes cómo Git almacena la historia, entiendes:
- Por qué Git es tan rápido (objetos inmutables, reutilización)
- Por qué Git es tan eficiente (deduplicación, compresión)
- Por qué Git es tan confiable (checksums, inmutabilidad)
- Qué hace cada comando (manipula objetos, referencias)
- Cómo recuperar de errores (reflog, objetos huérfanos)

**La diferencia entre Git y otros sistemas:**

```
Sistema tradicional (SVN, CVS):
Servidor almacena:
- Versión 1 completa
- Diff 1→2
- Diff 2→3
- Diff 3→4

Para ver versión 4:
1. Descarga versión 1
2. Aplica diff 1→2
3. Aplica diff 2→3
4. Aplica diff 3→4
→ Lento, dependiente del servidor

Git:
Local almacena:
- Snapshot completo de cada versión
- Pero reusando objetos idénticos

Para ver versión 4:
1. Lee commit 4
2. Lee su tree
3. Lee los blobs
→ Instantáneo, local, independiente
```

**El proceso completo de commit:**

Git hace mucho más que "guardar cambios". Cuando haces commit:

1. **Creación de objetos**:
   - Lee archivos del staging
   - Calcula hash de cada archivo
   - Busca si el blob ya existe (deduplicación)
   - Crea blobs solo para contenido nuevo
   - Comprime y guarda blobs

2. **Construcción del árbol**:
   - Lee la estructura del staging
   - Crea trees para cada directorio
   - Organiza blobs en trees
   - Trees apuntan a blobs y otros trees
   - Calcula hash de cada tree

3. **Creación del commit**:
   - Crea commit object
   - Apunta al tree raíz
   - Apunta al commit padre (anterior)
   - Añade metadata (autor, fecha, mensaje)
   - Calcula hash del commit

4. **Actualización del grafo**:
   - El nuevo commit se añade al grafo
   - Su padre es el commit anterior
   - Se forma una cadena de historia

5. **Actualización de referencias**:
   - La rama actual (ej: main) se actualiza
   - Ahora apunta al nuevo commit
   - HEAD sigue apuntando a la rama

**La eficiencia de almacenamiento:**

Git es extremadamente eficiente porque:

1. **Reutilización de objetos**:
   ```
   Commit A tiene 100 archivos
   Commit B modifica 1 archivo
   
   Git NO copia los 100 archivos
   Git crea 1 blob nuevo + reutiliza 99 blobs
   ```

2. **Compresión inteligente**:
   ```
   Cada objeto se comprime con zlib
   Objetos similares se empaquetan juntos
   Delta compression: solo guarda diferencias
   ```

3. **Compartición entre branches**:
   ```
   main y feature comparten commits comunes
   No se duplican: mismos objetos, diferentes refs
   ```

**El costo real:**

```
Commit inicial (1000 archivos):
- 1000 blobs creados
- ~10 trees creados
- 1 commit creado
- Total: 1011 objetos

Commit 2 (modificaste 1 archivo):
- 1 blob nuevo
- ~2 trees nuevos (raíz + subdirectorio modificado)
- 1 commit nuevo
- Blobs sin cambiar: reutilizados (0 bytes)
- Total: 4 objetos nuevos

¡99% de reutilización!
```

**¿Por qué Git es más rápido que otros sistemas?**

1. **Todo es local**: No esperas la red
2. **Objetos inmutables**: No hay que recalcular nada
3. **Hash indexing**: Búsqueda instantánea por hash
4. **Deduplicación automática**: Menos datos que procesar
5. **Packfiles**: Acceso secuencial eficiente

Veamos el proceso interno paso a paso.

---

### 8.1 Proceso de Commit Completo

**Paso a paso interno:**

```
Estado inicial:
- Working: file.txt modificado
- Staging: vacío
- Repository: commit A

1. git add file.txt
   ┌─────────────────────────────────┐
   │ - Lee file.txt del disco        │
   │ - Calcula hash: abc123          │
   │ - Crea blob en objects/ab/c123  │
   │ - Actualiza index:              │
   │   file.txt → abc123             │
   └─────────────────────────────────┘

2. git commit -m "Update file"
   ┌─────────────────────────────────┐
   │ a) Lee el index                 │
   │    file.txt → abc123            │
   │                                 │
   │ b) Crea tree object:            │
   │    100644 blob abc123 file.txt  │
   │    Hash del tree: def456        │
   │                                 │
   │ c) Crea commit object:          │
   │    tree: def456                 │
   │    parent: A                    │
   │    message: "Update file"       │
   │    Hash del commit: ghi789      │
   │                                 │
   │ d) Actualiza rama:              │
   │    .git/refs/heads/main → ghi789│
   │                                 │
   │ e) Limpia index (opcional)      │
   └─────────────────────────────────┘

Resultado:
- Repository: commit A ← commit B (ghi789)
- main apunta a B
- HEAD apunta a main
```

### 8.2 Comparación: Git vs Otros Sistemas

**Sistema tradicional (SVN):**

```
Servidor:
- Versión 1
- Diff 1→2
- Diff 2→3
- Diff 3→4

Cliente:
- Solo working copy
- Necesita servidor para todo
- Lento (red)
- Dependiente
```

**Git:**

```
Local:
- Versión 1 (completa)
- Versión 2 (completa, reusa objetos)
- Versión 3 (completa, reusa objetos)
- Versión 4 (completa, reusa objetos)

Cliente:
- Repositorio completo local
- NO necesita servidor
- Rápido (disco local)
- Independiente
```

### 8.3 Eficiencia de Almacenamiento

**Reutilización de objetos:**

```
Commit A:
├── README.md → blob abc123 (50 KB)
├── main.py   → blob def456 (30 KB)
└── utils.py  → blob ghi789 (20 KB)
Total objetos: 100 KB

Commit B (solo cambió main.py):
├── README.md → blob abc123 (reutilizado)
├── main.py   → blob xyz999 (31 KB)
└── utils.py  → blob ghi789 (reutilizado)
Total NUEVO: 31 KB (solo main.py nuevo)

Commit C (solo cambió utils.py):
├── README.md → blob abc123 (reutilizado)
├── main.py   → blob xyz999 (reutilizado)
└── utils.py  → blob jkl012 (21 KB)
Total NUEVO: 21 KB (solo utils.py nuevo)

Almacenamiento total: 100 + 31 + 21 = 152 KB
Sin reutilización sería: 100 + 101 + 102 = 303 KB

¡Ahorro del 50%!
```

---

## 9. Operaciones Fundamentales
[↑ Top](#-tabla-de-contenidos)

### Introducción: Comandos como Manipulación de Objetos

Ya entiendes la arquitectura interna de Git: objetos, grafo, referencias, áreas de trabajo. Ahora viene la parte crucial: **cómo los comandos que usas día a día manipulan esta arquitectura**.

**El cambio de perspectiva:**

Antes de entender Git internamente, ves los comandos así:
```
git add      → "añadir archivos"
git commit   → "guardar cambios"
git branch   → "crear rama"
git checkout → "cambiar de rama"
```

Después de entender Git internamente, ves los comandos así:
```
git add      → "crear blobs y actualizar index"
git commit   → "crear tree, commit object, mover referencia"
git branch   → "crear archivo ref apuntando a commit"
git checkout → "actualizar HEAD, index y working directory"
```

Esta comprensión te da **poder real** sobre Git.

**Dos niveles de comandos:**

Git tiene dos conjuntos de comandos:

```
┌────────────────────────────────────────────┐
│ PORCELANA (Porcelain) - Nivel Usuario     │
├────────────────────────────────────────────┤
│ - Interfaz amigable                        │
│ - Lo que usas día a día                    │
│ - Comandos como: add, commit, push, pull   │
│                                            │
│ Ejemplos:                                  │
│ git commit -m "mensaje"                    │
│ git branch nueva-rama                      │
│ git merge feature                          │
└────────────────┬───────────────────────────┘
                 │ internamente usan
                 ▼
┌────────────────────────────────────────────┐
│ PLOMERÍA (Plumbing) - Nivel Interno       │
├────────────────────────────────────────────┤
│ - Operaciones de bajo nivel                │
│ - Manipulan objetos directamente           │
│ - Comandos como: hash-object, update-ref   │
│                                            │
│ Ejemplos:                                  │
│ git hash-object -w file                    │
│ git update-ref refs/heads/main abc123      │
│ git cat-file -p abc123                     │
└────────────────────────────────────────────┘
```

**¿Por qué esta distinción importa?**

Los comandos "porcelana" (add, commit, branch) son **composiciones** de comandos "plomería". Entender los comandos plomería te revela qué hace realmente cada operación.

**Ejemplo: `git commit` descompuesto**

```
Cuando ejecutas:
git commit -m "Add feature"

Git internamente ejecuta:

1. git write-tree
   → Lee .git/index
   → Crea tree objects
   → Retorna hash del tree raíz
   
2. git commit-tree <tree-hash> -p <parent-hash> -m "Add feature"
   → Crea commit object
   → Con el tree, padre, y mensaje
   → Retorna hash del commit
   
3. git update-ref refs/heads/main <commit-hash>
   → Actualiza la referencia main
   → Ahora apunta al nuevo commit
   
4. Resultado:
   → Objetos creados en .git/objects/
   → Referencia actualizada en .git/refs/heads/main
   → HEAD sigue apuntando a main
```

**El poder de entender esto:**

Cuando sabes que `git commit` realmente hace estas operaciones, entiendes:

1. **Por qué es atómico**: O todas las operaciones suceden, o ninguna
2. **Por qué es rápido**: Son solo operaciones de archivos (hash, escribir)
3. **Por qué es seguro**: Los objetos son inmutables, las refs son simples archivos
4. **Cómo recuperar**: Si algo falla, los objetos siguen ahí

**Comandos como manipulación del grafo:**

Cada comando de Git manipula el grafo de alguna forma:

```
git commit:
   Antes: A ← B ← C (main)
   Después: A ← B ← C ← D (main)
   → Añade nodo al grafo

git branch:
   Antes: A ← B ← C (main)
   Después: A ← B ← C (main, feature)
   → Añade referencia al mismo nodo

git merge:
   Antes: A ← B ← C (main)
              ↖
                D ← E (feature)
   Después: A ← B ← C ← M (main)
                ↖     ↗
                  D ← E (feature)
   → Añade nodo con dos padres

git checkout:
   Antes: HEAD → main → C
   Después: HEAD → feature → E
   → Mueve puntero HEAD

git reset:
   Antes: A ← B ← C ← D (main, HEAD)
   Después: A ← B ← C (main, HEAD)
   → Mueve referencia atrás (D sigue existiendo)
```

**La filosofía de operaciones:**

Git tiene una filosofía consistente:

1. **Nunca destruye objetos**: Los objetos son inmutables y permanentes
2. **Solo mueve referencias**: La mayoría de operaciones son mover punteros
3. **Todo es reversible**: Casi siempre puedes deshacer (reflog)
4. **Local primero**: Operaciones rápidas, sincronización después

**¿Por qué es importante saber esto?**

Porque cambia tu modelo mental:

```
Modelo mental incorrecto:
"git reset borra commits"
→ Tienes miedo de usarlo

Modelo mental correcto:
"git reset mueve referencia, commits siguen en reflog"
→ Usas reset con confianza, sabes que puedes recuperar
```

```
Modelo mental incorrecto:
"Crear branch copia archivos"
→ Evitas crear branches

Modelo mental correcto:
"Crear branch es crear archivo de 40 bytes"
→ Creas branches libremente
```

**El mapa de operaciones:**

```
Comandos que crean objetos:
├─ git add        → crea blobs
├─ git commit     → crea tree y commit
└─ git tag -a     → crea tag object

Comandos que mueven referencias:
├─ git commit     → mueve rama actual
├─ git branch -f  → mueve rama específica
├─ git reset      → mueve rama y HEAD
└─ git merge      → crea commit, mueve rama

Comandos que mueven HEAD:
├─ git checkout   → mueve HEAD a otra rama/commit
├─ git switch     → igual que checkout (más claro)
└─ git reset      → mueve HEAD junto con rama

Comandos que modifican working/staging:
├─ git add        → actualiza staging
├─ git reset      → actualiza staging desde commit
├─ git checkout   → actualiza working desde commit
└─ git restore    → restaura archivos específicos
```

**La clave del dominio:**

Cuando entiendes que:
- Los objetos son la **realidad** (datos inmutables)
- Las referencias son **ventanas** (punteros móviles)
- Los comandos son **manipuladores** (mueven punteros, crean objetos)

Entonces Git deja de ser mágico y se vuelve **predecible**. Sabes exactamente qué hace cada comando y por qué.

Veamos cómo funcionan los comandos más importantes.

---

### 9.1 Comandos Plumbing vs Porcelana

Git tiene dos niveles de comandos:

```
┌────────────────────────────────────────────┐
│ PORCELANA (Porcelain)                      │
│ Comandos para usuarios                     │
├────────────────────────────────────────────┤
│ git add, commit, branch, merge, etc.       │
│ Interfaz amigable                          │
│ Lo que usas día a día                      │
└────────────────────────────────────────────┘
         ↓ usan internamente
┌────────────────────────────────────────────┐
│ PLOMERÍA (Plumbing)                        │
│ Comandos de bajo nivel                     │
├────────────────────────────────────────────┤
│ hash-object, cat-file, update-ref, etc.    │
│ Operan directamente con objetos            │
│ Raramente usados directamente              │
└────────────────────────────────────────────┘
```

### 9.2 Comandos Plumbing (Internos)

**Estos comandos muestran CÓMO funciona Git:**

```
git hash-object
Función: Crear objetos
Uso: echo "contenido" | git hash-object -w --stdin
Internamente: Calcula SHA-1, comprime, guarda en objects/

git cat-file
Función: Leer objetos
Uso: git cat-file -p abc123
Internamente: Descomprime objeto, muestra contenido

git update-ref
Función: Actualizar referencias
Uso: git update-ref refs/heads/main abc123
Internamente: Escribe hash en archivo de referencia

git rev-parse
Función: Resolver referencias a hashes
Uso: git rev-parse HEAD
Internamente: Lee referencias, sigue punteros

git ls-tree
Función: Ver contenido de tree
Uso: git ls-tree HEAD
Internamente: Lee tree object, lista entradas
```

### 9.3 Cómo Funcionan Comandos Comunes

**git add:**

```
Internamente hace:
1. git hash-object -w file.txt
   → Crea blob, guarda en objects/

2. git update-index --add file.txt
   → Actualiza .git/index con hash del blob

Resultado:
- Blob creado en objects/
- Index actualizado
- NO se crea commit aún
```

**git commit:**

```
Internamente hace:
1. git write-tree
   → Lee index, crea tree object(s)
   
2. git commit-tree <tree-hash> -p <parent-hash> -m "mensaje"
   → Crea commit object
   
3. git update-ref refs/heads/<branch> <commit-hash>
   → Actualiza rama

Resultado:
- Tree object creado
- Commit object creado
- Rama actualizada
```

**git branch:**

```
Crear rama:
git branch nueva-rama

Internamente:
1. git rev-parse HEAD
   → Obtiene hash del commit actual
   
2. git update-ref refs/heads/nueva-rama <hash>
   → Crea archivo con hash

Resultado:
- Archivo .git/refs/heads/nueva-rama creado
- Contiene hash del commit
- ¡No se copia nada! Solo un puntero
```

**git checkout:**

```
Cambiar de rama:
git checkout main

Internamente:
1. git rev-parse refs/heads/main
   → Obtiene hash del commit de main
   
2. git read-tree <hash>
   → Lee tree del commit
   
3. Actualiza working directory con contenido del tree
   
4. git symbolic-ref HEAD refs/heads/main
   → Actualiza HEAD

Resultado:
- Archivos en working directory actualizados
- HEAD apunta a main
```

---

## 10. Git y GitHub Actions
[↑ Top](#-tabla-de-contenidos)

### Introducción: Git en Entornos de CI/CD

Ahora que entiendes cómo funciona Git internamente, es momento de ver cómo se aplica este conocimiento en **GitHub Actions** y otros sistemas de CI/CD.

**¿Por qué es importante entender Git para GitHub Actions?**

GitHub Actions ejecuta workflows automáticos basados en eventos de Git:
- Push a una rama
- Creación de pull request
- Creación de tag
- Actualización de referencia

Para usar GitHub Actions efectivamente, necesitas entender:
1. **Qué es un evento Git** (push, PR, tag)
2. **Qué información Git está disponible** (commit hash, refs, branches)
3. **Cómo acceder a la historia** (checkout, fetch-depth)
4. **Cómo manipular el repositorio** (crear commits, tags, branches desde CI)

**El contexto Git en GitHub Actions:**

Cuando un workflow se ejecuta, GitHub proporciona **contexto Git completo**:

```yaml
Variables disponibles:
${{ github.sha }}        → Hash del commit que disparó el workflow
${{ github.ref }}        → Referencia completa (refs/heads/main)
${{ github.ref_name }}   → Nombre corto (main)
${{ github.head_ref }}   → Branch del PR (si es PR)
${{ github.base_ref }}   → Branch base del PR (si es PR)
${{ github.event_name }} → Tipo de evento (push, pull_request, etc.)
```

Cada una de estas variables corresponde directamente a conceptos Git que has aprendido:
- `github.sha` es un **commit object hash**
- `github.ref` es una **referencia** (branch o tag)
- `github.head_ref` / `github.base_ref` son **branches** en el contexto de PR

**¿Qué hace actions/checkout?**

El action más usado en GitHub Actions es `actions/checkout`. Entender qué hace internamente te da poder:

```yaml
- uses: actions/checkout@v4

Internamente ejecuta:
1. git init
   → Crea .git/ vacío
   
2. git remote add origin <url>
   → Configura remoto
   
3. git fetch --depth=1 origin <ref>
   → Descarga solo el commit específico (shallow clone)
   → No descarga toda la historia
   
4. git checkout --detach <sha>
   → Detached HEAD en el commit específico
   → No está en ninguna rama
   
Resultado:
- Repositorio disponible
- HEAD apunta directamente a github.sha
- Historia mínima (solo 1 commit)
```

**¿Por qué detached HEAD?**

GitHub Actions usa detached HEAD por defecto porque:
1. No necesitas estar en una rama para ejecutar tests/builds
2. Es más explícito: estás en un commit específico, inmutable
3. Evita confusión sobre qué rama es "la actual"

**El problema del shallow clone:**

Por defecto, `actions/checkout` hace un **shallow clone** (profundidad 1):

```
Shallow clone (fetch-depth: 1):
Solo descarga:
  → El commit que disparó el workflow
  → Su tree
  → Sus blobs

NO descarga:
  ✗ Commits anteriores
  ✗ Otros branches
  ✗ Tags
  ✗ Historia completa

Ventajas:
  ✓ Rápido (menos datos)
  ✓ Eficiente (menos almacenamiento)
  
Limitaciones:
  ✗ git log no funciona bien
  ✗ No puedes comparar con commits anteriores
  ✗ No puedes ver tags
  ✗ git describe falla
```

**Cuándo necesitas historia completa:**

```yaml
- uses: actions/checkout@v4
  with:
    fetch-depth: 0  # Descarga TODO

Necesitas esto cuando:
✓ Quieres comparar con commits anteriores
✓ Necesitas contar commits desde un tag
✓ Usas git describe
✓ Analizas toda la historia
✓ Generas changelogs automáticos
```

**Eventos Git en GitHub Actions:**

Cada evento en GitHub Actions corresponde a una operación Git:

```yaml
on: push
→ Alguien hizo git push
→ github.sha = hash del commit pusheado
→ github.ref = rama a la que se pusheó

on: pull_request
→ Se creó/actualizó un PR
→ github.sha = hash del merge commit (simulado)
→ github.head_ref = branch del PR
→ github.base_ref = branch base (ej: main)

on: create
→ Se creó una rama o tag
→ github.ref = la nueva referencia

on: delete
→ Se eliminó una rama o tag
→ github.ref = la referencia eliminada
```

**Aplicaciones prácticas:**

Entender Git te permite hacer cosas poderosas en CI/CD:

1. **Versionado automático**:
   ```yaml
   - run: |
       # Contar commits desde último tag
       VERSION=$(git describe --tags --always)
       echo "Version: $VERSION"
   ```

2. **Análisis de cambios**:
   ```yaml
   - run: |
       # Ver qué archivos cambiaron
       git diff --name-only HEAD~1 HEAD
       # Ejecutar tests solo para archivos modificados
   ```

3. **Validación de commits**:
   ```yaml
   - run: |
       # Verificar mensajes de commit
       git log --format=%s HEAD~1..HEAD | grep -E '^(feat|fix|docs):'
   ```

4. **Generación de releases**:
   ```yaml
   - run: |
       # Generar changelog
       git log $(git describe --tags --abbrev=0)..HEAD --oneline
   ```

**La ventaja del conocimiento:**

Cuando entiendes Git internamente, GitHub Actions deja de ser una "caja negra" y se vuelve:
- **Predecible**: Sabes qué información está disponible y por qué
- **Depurable**: Puedes inspeccionar el estado de Git en el workflow
- **Poderoso**: Puedes manipular el repositorio de formas avanzadas
- **Eficiente**: Sabes cuándo necesitas historia completa vs shallow clone

Veamos los detalles de la integración.

---

### 10.1 Contexto Git en Actions

Cuando GitHub Actions ejecuta un workflow, tiene acceso completo al repositorio Git.

**Variables de contexto:**

```yaml
${{ github.sha }}
→ Hash SHA-1 del commit que disparó el workflow
→ Ejemplo: a1b2c3d4e5f6789012345678901234567890abcd

${{ github.ref }}
→ Referencia completa (branch o tag)
→ Ejemplos:
  - refs/heads/main (push a main)
  - refs/tags/v1.0.0 (push de tag)
  - refs/pull/123/merge (pull request)

${{ github.ref_name }}
→ Nombre corto de la referencia
→ Ejemplos: main, v1.0.0, 123/merge

${{ github.head_ref }}
→ Branch del pull request (solo en PR)
→ Ejemplo: feature-branch

${{ github.base_ref }}
→ Branch base del PR (solo en PR)
→ Ejemplo: main
```

**Relación con conceptos Git:**

```
github.sha:
- Es el hash de un COMMIT object
- Identifica exactamente qué código ejecutar
- Inmutable: siempre apunta al mismo contenido

github.ref:
- Es una REFERENCIA (branch o tag)
- refs/heads/main = rama main
- refs/tags/v1.0.0 = tag v1.0.0
- Apunta al commit (github.sha)

HEAD en Actions:
- actions/checkout configura HEAD
- Por defecto: detached HEAD en github.sha
- Opción: puede hacer checkout de branch
```

### 10.2 actions/checkout

**¿Qué hace actions/checkout?**

```yaml
- uses: actions/checkout@v4

Internamente ejecuta:
1. git clone --depth 1 <repo>
   → Clona solo el último commit (shallow)
   
2. git checkout <github.sha>
   → Detached HEAD en el commit específico
   
Resultado:
- Repositorio disponible en $GITHUB_WORKSPACE
- HEAD en el commit que disparó el workflow
- Historia mínima (solo 1 commit por defecto)
```

**Opciones comunes:**

```yaml
- uses: actions/checkout@v4
  with:
    fetch-depth: 0
    # Descarga TODA la historia
    # Permite git log, git diff con cualquier commit
    # Necesario para comparar con commits antiguos

- uses: actions/checkout@v4
  with:
    ref: main
    # Hace checkout de rama específica
    # En lugar de github.sha
    # Útil para workflows manuales

- uses: actions/checkout@v4
  with:
    fetch-depth: 10
    # Descarga últimos 10 commits
    # Intermedio entre shallow y completo
```

### 10.3 Uso de Git en Actions

**Ejemplo: Obtener información del commit**

```yaml
- name: Info del commit
  run: |
    # Hash corto (7 caracteres):
    SHORT_SHA=$(git rev-parse --short HEAD)
    echo "SHORT_SHA=$SHORT_SHA"
    
    # Mensaje del commit:
    COMMIT_MSG=$(git log -1 --pretty=%B)
    echo "Commit: $COMMIT_MSG"
    
    # Autor:
    AUTHOR=$(git log -1 --pretty=format:'%an')
    echo "Author: $AUTHOR"
    
    # Fecha:
    DATE=$(git log -1 --pretty=format:'%ci')
    echo "Date: $DATE"
```

**¿Por qué funciona?**

```
actions/checkout descargó el repositorio
→ .git/ está disponible
→ Todos los comandos git funcionan
→ Puedes inspeccionar objetos, referencias, historia
```

**Ejemplo: Comparar con rama base**

```yaml
- uses: actions/checkout@v4
  with:
    fetch-depth: 0  # Necesario para comparar

- name: Ver cambios desde main
  run: |
    # Archivos modificados:
    git diff --name-only origin/main..HEAD
    
    # Estadísticas:
    git diff --stat origin/main..HEAD
    
    # Commits nuevos:
    git log --oneline origin/main..HEAD
```

---

## 11. Conceptos Avanzados

### 11.1 Rebase vs Merge

**Diferencia conceptual:**

```
Merge:
- Preserva historia completa
- Crea merge commit
- No reescribe commits existentes
- Historia "verdadera"

Rebase:
- Reescribe historia
- Mueve commits a nueva base
- Cambia hashes (nuevos commits)
- Historia "limpia"
```

**Visualmente:**

```
ANTES:
       A ← B ← C    (main)
            ↖
              D ← E  (feature)

MERGE:
       A ← B ← C ← M    (main)
            ↖     ↗
              D ← E      (feature)
Preserve: D y E siguen ahí
Nuevo: M (merge commit)

REBASE:
       A ← B ← C ← D' ← E'  (feature)
Los commits D y E se "mueven"
D' y E' son NUEVOS commits (nuevos hashes)
D y E originales desaparecen (quedan en reflog)
```

**¿Cuándo usar cada uno?**

```
Usar MERGE cuando:
✓ Es rama pública (otros la tienen)
✓ Quieres preservar historia exacta
✓ Es main/master
✓ Ya hiciste push

Usar REBASE cuando:
✓ Es rama privada (solo tú)
✓ Quieres historia lineal
✓ Antes de hacer merge a main
✓ NO has hecho push
```

### 11.2 Fast-Forward

**Concepto:** Mover puntero sin crear merge commit.

```
Situación:
       A ← B ← C    (main)
                ↖
                  D ← E  (feature)

main NO avanzó desde que se creó feature
→ main está "atrás" de feature

git merge feature (desde main):
       A ← B ← C ← D ← E  (main, feature)

main simplemente "avanza rápido" a E
No se crea merge commit
Historia queda lineal

¿Por qué funciona?
- E contiene TODO lo que tiene C
- E es descendiente directo de C
- No hay divergencia, no hay conflicto posible
- Solo mueves el puntero
```

**Cuándo NO es posible:**

```
Situación:
       A ← B ← C ← F    (main)
            ↖
              D ← E      (feature)

main SÍ avanzó (commit F)
→ Historia divergió

git merge feature:
- NO puede hacer fast-forward
- Necesita merge commit o rebase
- main y feature tienen cambios independientes
```

### 11.3 Detached HEAD

**¿Qué es?**

```
HEAD normal (attached):
HEAD → refs/heads/main → commit C

HEAD detached:
HEAD → commit B (directo)

¿Cuándo ocurre?
- git checkout <commit-hash>
- git checkout <tag>
- git checkout HEAD~3
```

**Peligro:**

```
En detached HEAD:
HEAD → commit B

Haces commit:
HEAD → commit X
       X es un commit nuevo
       NO está en ninguna rama

git checkout main:
HEAD → refs/heads/main → commit C
       commit X queda "huérfano"
       
Después de ~30 días:
git gc elimina X (garbage collection)
Trabajo perdido
```

**Cómo evitar perder trabajo:**

```
Antes de cambiar de HEAD:
git branch temp-work  # Guarda en rama

O después (si recuerdas el hash):
git reflog  # Busca el commit
git branch recovered <hash>
```

---

## Resumen: Modelo Mental de Git

```
┌─────────────────────────────────────────────────────────┐
│ Git es una BASE DE DATOS de contenido                  │
├─────────────────────────────────────────────────────────┤
│                                                         │
│ OBJETOS (Contenido):                                    │
│ ├─ BLOB: contenido de archivos                         │
│ ├─ TREE: estructura de directorios                     │
│ ├─ COMMIT: snapshot del proyecto                       │
│ └─ TAG: etiqueta anotada                               │
│                                                         │
│ REFERENCIAS (Punteros):                                 │
│ ├─ Branches: punteros móviles a commits                │
│ ├─ Tags: punteros fijos a commits                      │
│ └─ HEAD: dónde estás ahora                             │
│                                                         │
│ GRAFO (Relaciones):                                     │
│ └─ Commits apuntan a padres → historia                 │
│                                                         │
│ ÁREAS (Flujo de trabajo):                               │
│ ├─ Working Directory: archivos que editas              │
│ ├─ Staging Area: preparación del commit                │
│ └─ Repository: commits permanentes                     │
│                                                         │
│ PRINCIPIOS:                                             │
│ ├─ Content-addressable: identificar por contenido      │
│ ├─ Snapshots: estados completos, no diffs              │
│ ├─ Inmutabilidad: objetos nunca cambian                │
│ └─ Local: la mayoría de ops no necesitan red           │
└─────────────────────────────────────────────────────────┘
```

---

## Conclusión

Git no es solo una herramienta para "guardar versiones". Es un sistema completo con:

1. **Arquitectura sólida:** Objetos, referencias, grafo
2. **Diseño inteligente:** Content-addressable, snapshots, inmutabilidad
3. **Eficiencia:** Deduplicación, compresión, packfiles
4. **Confiabilidad:** Checksums, reflog, recuperación

Entender estos fundamentos te permite:
- ✅ Usar Git con confianza
- ✅ Resolver problemas complejos
- ✅ Optimizar workflows
- ✅ Integrar con sistemas como GitHub Actions
- ✅ No tener miedo de "romper" algo (casi todo es recuperable)

**Git es simple en su núcleo:** objetos + referencias + grafo. Todo lo demás es construcción sobre estos fundamentos.

