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
   - 9.1 [Comandos Plumbing vs Porcelana](#91-comandos-plumbing-vs-porcelana)
   - 9.2 [Comandos Plumbing (Internos)](#92-comandos-plumbing-internos)
   - 9.3 [Cómo Funcionan Comandos Comunes](#93-cómo-funcionan-comandos-comunes) → **Ver `GIT_COMANDOS_GUIA_PRACTICA.md`**

### PARTE IV: INTEGRACIÓN
10. [Git y GitHub Actions](#10-git-y-github-actions)
11. [Conceptos Avanzados](#11-conceptos-avanzados)

---

# PARTE I: FUNDAMENTOS

## 1. ¿Qué es Git? - Conceptos Base
[↑ Top](#-tabla-de-contenidos)

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
[↑ Top](#-tabla-de-contenidos)

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
[↑ Top](#-tabla-de-contenidos)

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
[↑ Top](#-tabla-de-contenidos)

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
[↑ Top](#-tabla-de-contenidos)

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
[↑ Top](#-tabla-de-contenidos)

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
[↑ Top](#-tabla-de-contenidos)

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
[↑ Top](#-tabla-de-contenidos)

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

> **📘 Para uso práctico completo de comandos:** Ver `GIT_COMANDOS_GUIA_PRACTICA.md`  
> Este documento contiene la guía completa de 21 comandos Git con ejemplos del mundo real, opciones avanzadas, casos de uso y mejores prácticas.

Esta sección se enfoca en entender **cómo los comandos Porcelain descomponen en comandos Plumbing** internamente, revelando qué hace Git bajo el capó.

---

**La relación Plumbing → Porcelain:**

Los comandos "porcelana" (interfaz amigable) internamente ejecutan múltiples comandos "plomería" (operaciones de bajo nivel):

**Ejemplo 1: `git add file.txt`**

```bash
# Usuario ejecuta:
git add file.txt

# Git internamente ejecuta:
git hash-object -w file.txt   # Crea blob, retorna hash
git update-index --add file.txt # Actualiza index con hash
```

**Ejemplo 2: `git commit -m "mensaje"`**

```bash
# Usuario ejecuta:
git commit -m "Add feature"

# Git internamente ejecuta:
git write-tree                 # Crea tree object, retorna hash
git commit-tree <tree> -p <parent> -m "Add feature"  # Crea commit object
git update-ref refs/heads/main <commit-hash>  # Actualiza rama
```

**Ejemplo 3: `git branch nueva-rama`**

```bash
# Usuario ejecuta:
git branch feature-x

# Git internamente ejecuta:
git rev-parse HEAD              # Obtiene hash del commit actual
git update-ref refs/heads/feature-x <hash>  # Crea archivo con hash
# → Solo 41 bytes, instantáneo
```

**Ejemplo 4: `git checkout main`**

```bash
# Usuario ejecuta:
git checkout main

# Git internamente ejecuta:
git rev-parse refs/heads/main   # Obtiene hash de main
git read-tree <hash>            # Lee tree del commit
# Actualiza working directory
git symbolic-ref HEAD refs/heads/main  # Actualiza HEAD
# Actualiza .git/index
```

---

**Resumen de comandos Porcelain y su descomposición:**

| Comando Porcelain | Comandos Plumbing Internos | Efecto |
|-------------------|---------------------------|---------|
| `git add` | `hash-object` + `update-index` | Crea blob, actualiza index |
| `git commit` | `write-tree` + `commit-tree` + `update-ref` | Crea tree, commit, mueve rama |
| `git branch` | `rev-parse` + `update-ref` | Crea referencia (41 bytes) |
| `git checkout` | `rev-parse` + `read-tree` + `symbolic-ref` | Mueve HEAD, actualiza working |
| `git merge` | `merge-base` + `read-tree` + `write-tree` + `commit-tree` | Three-way merge o ff |
| `git reset` | `update-ref` + (opcional) `read-tree` | Mueve rama, actualiza staging/working |
| `git tag` | `update-ref refs/tags/*` | Crea referencia inmutable |

---

**Por qué esta comprensión importa:**

Entender la descomposición interna te permite:

1. **Diagnosticar problemas**: Sabes qué falló exactamente
2. **Usar Git con confianza**: Entiendes las consecuencias
3. **Optimizar workflows**: Sabes qué es costoso y qué es barato
4. **Recuperar de errores**: Conoces los mecanismos subyacentes
5. **Crear automatizaciones**: Puedes usar comandos plumbing directamente

**Ejemplos de poder con comandos Plumbing:**

```bash
# Crear commit manualmente (sin staging):
tree=$(git write-tree)
parent=$(git rev-parse HEAD)
commit=$(git commit-tree $tree -p $parent -m "Direct commit")
git update-ref refs/heads/main $commit

# Ver objeto exactamente como Git lo ve:
git cat-file -p HEAD

# Listar todos los objetos del repo:
git rev-list --objects --all

# Ver tamaño de objetos:
git count-objects -v

# Verificar integridad completa:
git fsck --full
```

---

**Para uso práctico diario:**

Consulta `GIT_COMANDOS_GUIA_PRACTICA.md` donde encontrarás:

- ✅ **21 comandos Git** cubiertos completamente
- ✅ **15-20+ opciones** por comando con ejemplos
- ✅ **10+ casos de uso reales** por comando
- ✅ **Troubleshooting completo** con soluciones
- ✅ **Mejores prácticas** (qué hacer y qué evitar)
- ✅ **Workflows comunes** (Feature Branch, Fork, Hotfix)
- ✅ **Configuración recomendada** para productividad

La separación de documentos mantiene este archivo enfocado en **teoría y arquitectura interna**, mientras que el documento de comandos se enfoca en **práctica y uso real**.

---

## 10. Git y GitHub Actions
[↑ Top](#-tabla-de-contenidos)

### Introducción: Git en Entornos de CI/CD

Ahora que entiendes cómo funciona Git internamente, es momento de ver cómo se aplica este conocimiento en **GitHub Actions** y otros sistemas de CI/CD.

**¿Por qué es importante entender Git para GitHub Actions?**
   → Comprime con zlib
   → Guarda blob en .git/objects/

2. git update-index --add file.txt
   → Actualiza .git/index con:
     - Ruta del archivo
     - Hash del blob
     - Permisos (100644, 100755, etc.)
     - Timestamp

Resultado:
- Blob creado en objects/
- Index (.git/index) actualizado
- Working directory NO cambia
- Repository NO cambia (aún no hay commit)
```

**Uso práctico y opciones:**

```bash
# 1. Añadir archivo específico
git add archivo.txt
# → Stagea solo archivo.txt

# 2. Añadir todos los archivos modificados y nuevos
git add .
# → Stagea todo desde directorio actual
# → Incluye subdirectorios
# → Respeta .gitignore

# 3. Añadir todos los archivos del repositorio
git add -A
# o: git add --all
# → Stagea TODO: nuevos, modificados, eliminados
# → Desde cualquier directorio

# 4. Añadir solo archivos rastreados (ignora nuevos)
git add -u
# o: git add --update
# → Solo archivos ya en Git
# → NO añade archivos nuevos
# → Útil para "actualizar solo lo existente"

# 5. Añadir interactivamente (PODER REAL)
git add -i
# → Modo interactivo con menú
# → Puedes elegir qué hacer con cada archivo

# 6. Añadir por parches (SUPER ÚTIL)
git add -p archivo.txt
# o: git add --patch
# → Te muestra cada "hunk" de cambios
# → Preguntas: Stage this hunk? [y,n,q,a,d,s,e,?]
# → Puedes stagear solo PARTE de un archivo
```

**Caso de uso real: Commits atómicos con -p**

```bash
Escenario: Modificaste un archivo con 2 features diferentes

# archivo.py tiene:
# - Cambio A: Nueva función calculate()
# - Cambio B: Fix bug en validate()

# Quieres 2 commits separados:

# Paso 1: Stagea solo cambios de calculate()
git add -p archivo.py
# → Ves el hunk con calculate()
# → Presionas 'y' (yes)
# → Ves el hunk con validate()
# → Presionas 'n' (no)

git commit -m "feat: Add calculate function"

# Paso 2: Stagea el resto
git add archivo.py
git commit -m "fix: Fix validation bug"

Resultado: 2 commits atómicos, historia más clara
```

**Opciones avanzadas de add -p:**

```
Durante git add -p, opciones disponibles:

y - Stage this hunk (sí, añadir este cambio)
n - Do not stage (no, saltar)
q - Quit (salir, no procesar más)
a - Stage this and all remaining hunks (todos los siguientes)
d - Do not stage this or any remaining (ninguno de los siguientes)
s - Split into smaller hunks (dividir en partes más pequeñas)
e - Manually edit hunk (editar manualmente)
? - Help (ayuda)

Opción 's' (split) es PODEROSA:
→ Si un hunk tiene múltiples cambios cercanos
→ Puedes intentar dividirlo en hunks más pequeños
→ Para control más granular

Opción 'e' (edit) es para EXPERTOS:
→ Abre editor con el diff
→ Puedes editar líneas manualmente
→ Útil cuando 's' no divide suficiente
```

**Patrones de uso comunes:**

```bash
# Patrón 1: Añadir por tipo de archivo
git add *.py          # Solo archivos Python
git add src/          # Todo en directorio src/
git add "*.txt"       # Todos los .txt (comillas para expansión)

# Patrón 2: Añadir excepto algunos
git add .
git reset HEAD archivo-no-deseado.txt
# → Añade todo, luego quita uno

# Patrón 3: Añadir forzando (ignorar .gitignore)
git add -f archivo-ignorado.log
# → Fuerza añadir aunque esté en .gitignore
# → Úsalo con CUIDADO

# Patrón 4: Dry run (ver qué se añadiría)
git add -n .
# o: git add --dry-run .
# → Muestra qué se añadiría sin hacerlo

# Patrón 5: Añadir con verbose
git add -v archivo.txt
# → Muestra qué archivos se añaden
```

**Ver qué está stageado:**

```bash
# Ver estado
git status
# → Muestra archivos stageados y no stageados

# Ver diferencias stageadas
git diff --staged
# o: git diff --cached
# → Muestra QUÉ cambios están en staging

# Ver diferencias NO stageadas
git diff
# → Muestra cambios en working directory
# → Que NO están en staging
```

**Mejores prácticas:**

```bash
✓ Usa git add -p para commits granulares
✓ Revisa con git diff --staged antes de commit
✓ No uses git add . ciegamente, revisa qué añades
✓ Usa .gitignore para archivos que nunca deben añadirse
✓ Considera git add -u cuando solo actualizas existentes

✗ Evita git add * (puede añadir archivos no deseados)
✗ No uses git add -f a menos que sea absolutamente necesario
✗ No stagees archivos generados (builds, logs, node_modules)
```

---

#### 9.3.2 git commit - Guardando la Historia

**Funcionamiento interno:**

```
Internamente hace:
1. git write-tree
   → Lee .git/index
   → Crea tree object para cada directorio
   → Organiza blobs en árboles
   → Calcula hash del tree raíz
   
2. git commit-tree <tree-hash> -p <parent-hash> -m "mensaje"
   → Crea commit object con:
     - tree: <tree-hash>
     - parent: <parent-hash>
     - author: nombre <email> timestamp
     - committer: nombre <email> timestamp
     - mensaje
   → Calcula hash del commit
   
3. git update-ref refs/heads/<branch> <commit-hash>
   → Actualiza archivo de referencia
   → La rama ahora apunta al nuevo commit
   
4. Actualiza reflog
   → .git/logs/refs/heads/<branch>
   → Registra el movimiento de la referencia

Resultado:
- Tree object(s) creado(s) en objects/
- Commit object creado en objects/
- Referencia de rama actualizada
- Reflog actualizado
- Index se limpia (opcional, según configuración)
```

**Uso práctico y opciones:**

```bash
# 1. Commit básico
git commit -m "Mensaje descriptivo"
# → Crea commit con mensaje simple
# → Requiere que staging NO esté vacío

# 2. Commit con mensaje multilínea
git commit -m "Título del commit" -m "Descripción detallada del cambio"
# → Primer -m: título (línea 1)
# → Segundo -m: descripción (línea 3+)
# → Línea 2 queda vacía automáticamente

# 3. Commit abriendo editor
git commit
# → Abre editor configurado (vim, nano, VSCode)
# → Puedes escribir mensaje complejo
# → Líneas que empiezan con # son comentarios

# 4. Commit con add automático (CUIDADO)
git commit -a -m "Mensaje"
# o: git commit -am "Mensaje"
# → Stagea automáticamente archivos MODIFICADOS
# → NO añade archivos nuevos
# → Útil para cambios rápidos, pero menos control

# 5. Commit vacío (sin cambios)
git commit --allow-empty -m "Trigger CI"
# → Crea commit sin cambios
# → Útil para re-ejecutar CI/CD
# → Útil para marcar puntos en historia

# 6. Amend: Modificar último commit
git commit --amend -m "Nuevo mensaje"
# → "Modifica" el último commit (realmente crea uno nuevo)
# → Útil para corregir mensajes
# → Útil para añadir archivos olvidados

# 7. Amend sin cambiar mensaje
git commit --amend --no-edit
# → Añade staging al último commit
# → Mantiene el mensaje anterior

# 8. Commit con fecha específica
git commit --date="2024-01-15 10:30:00" -m "Mensaje"
# → Útil para backporting
# → Útil para testing

# 9. Commit como otro usuario
git commit --author="Nombre <email@example.com>" -m "Mensaje"
# → Author diferente a committer
# → Útil para aplicar patches de otros
```

**Mensajes de commit efectivos:**

```bash
# FORMATO CONVENCIONAL (Conventional Commits):

feat: Add user authentication
^     ^
│     └─ Descripción clara, imperativo
└─ Tipo de cambio

Tipos comunes:
feat:     Nueva funcionalidad
fix:      Corrección de bug
docs:     Cambios en documentación
style:    Formato, sin cambios de código
refactor: Refactorización, sin cambios de comportamiento
test:     Añadir/modificar tests
chore:    Tareas de mantenimiento

# FORMATO CON SCOPE:
feat(auth): Add login endpoint
     ^
     └─ Componente afectado

# FORMATO COMPLETO:
feat(api): Add user registration endpoint

- Implement POST /api/register
- Add email validation
- Add password hashing with bcrypt
- Add integration tests

Closes #123

^ Título (50 chars max)
^ Línea vacía
^ Descripción detallada
^ Referencias a issues
```

**Casos de uso avanzados:**

```bash
# Caso 1: Olvidaste añadir un archivo
git add archivo-olvidado.txt
git commit --amend --no-edit
# → Añade el archivo al último commit sin cambiar mensaje

# Caso 2: Mensaje de commit incorrecto
git commit --amend -m "Mensaje corregido"
# → Corrige el mensaje del último commit

# Caso 3: Añadir co-autores (para pair programming)
git commit -m "feat: Add feature" -m "Co-authored-by: Nombre <email>"
# → GitHub reconoce ambos autores
# → Aparecen ambos en la UI

# Caso 4: Commit con template
git config commit.template ~/.gitmessage
# Archivo .gitmessage:
# tipo(scope): 
# 
# - 
# 
# Refs: #
git commit
# → Se abre editor con tu template

# Caso 5: Sign commits (GPG)
git commit -S -m "Mensaje"
# → Firma el commit con tu clave GPG
# → GitHub muestra "Verified"

# Caso 6: Commitear solo parte de archivos stageados
# (Técnicamente no es opción de commit, sino workflow)
git add -p archivo.txt    # Stagea solo parte
git commit -m "Cambio 1"  # Commit de lo stageado
git add archivo.txt       # Stagea el resto
git commit -m "Cambio 2"  # Otro commit
```

**Ver commits:**

```bash
# Ver historia
git log
# → Lista de commits, más reciente primero

# Log compacto
git log --oneline
# → Un commit por línea: hash corto + mensaje

# Log con grafo
git log --oneline --graph --all
# → Muestra ramas visualmente
# → Muy útil para ver merges

# Log de un archivo específico
git log -- archivo.txt
# → Solo commits que modificaron ese archivo

# Log con diff
git log -p
# → Muestra cambios completos en cada commit

# Log con stats
git log --stat
# → Muestra qué archivos cambiaron y cuánto

# Ver commit específico
git show abc123
# → Muestra commit completo: mensaje + diff

# Ver solo mensaje
git log --format="%H %s" -n 5
# → 5 commits más recientes, hash + mensaje
```

**Mejores prácticas:**

```bash
✓ Commits pequeños y atómicos (un cambio lógico)
✓ Mensajes descriptivos (explica POR QUÉ, no QUÉ)
✓ Usa convenciones (Conventional Commits)
✓ Revisa con git diff --staged antes de commit
✓ Usa --amend solo en commits NO pusheados
✓ Commitea frecuentemente (puedes squash después)

✗ Evita commits gigantes con múltiples cambios
✗ Evita mensajes genéricos ("fix", "update", "changes")
✗ No uses --amend en commits ya pusheados (reescribe historia)
✗ No comitees archivos generados o sensibles
✗ No uses commit -a sin revisar qué se commitea
```

---

#### 9.3.3 git branch - Gestionando Líneas de Desarrollo

**Funcionamiento interno:**

```
Crear rama:
git branch nueva-rama

Internamente:
1. git rev-parse HEAD
   → Resuelve HEAD al hash del commit actual
   → Por ejemplo: abc123def456...
   
2. Crea archivo .git/refs/heads/nueva-rama
   → Contiene: abc123def456...
   → Es solo un archivo de texto con 40 caracteres
   
3. (Opcional) Actualiza packed-refs si está configurado

Resultado:
- Archivo de referencia creado (41 bytes)
- Apunta al mismo commit que HEAD
- NO se copia ningún archivo
- NO cambia el working directory
- HEAD sigue donde estaba

IMPORTANTE: Crear rama es INSTANTÁNEO y GRATIS
→ Solo crea un archivo de 41 bytes
→ No importa tamaño del proyecto
```

**Uso práctico y opciones:**

```bash
# 1. Crear rama
git branch feature-x
# → Crea rama desde commit actual
# → NO cambia a ella (HEAD sigue igual)

# 2. Crear y cambiar en un comando
git checkout -b feature-x
# o en Git 2.23+:
git switch -c feature-x
# → Crea rama Y cambia a ella

# 3. Crear rama desde commit específico
git branch feature-x abc123
# → Crea rama desde commit abc123
# → No desde HEAD

# 4. Crear rama desde otra rama
git branch feature-x main
# → Crea feature-x desde tip de main
# → Útil si estás en otra rama

# 5. Listar ramas locales
git branch
# → Marca con * la rama actual
# → Solo ramas locales

# 6. Listar todas las ramas (incluye remotas)
git branch -a
# → Muestra local y remote tracking branches

# 7. Listar ramas con último commit
git branch -v
# → Muestra rama + hash + mensaje

# 8. Listar ramas mergeadas
git branch --merged
# → Ramas ya mergeadas en rama actual
# → Seguras para eliminar

# 9. Listar ramas NO mergeadas
git branch --no-merged
# → Ramas con cambios únicos
# → Cuidado al eliminar

# 10. Eliminar rama
git branch -d feature-x
# → Elimina solo si está mergeada
# → "Safe delete"

# 11. Forzar eliminación de rama
git branch -D feature-x
# → Elimina incluso si no está mergeada
# → ¡Puedes perder trabajo!

# 12. Renombrar rama actual
git branch -m nuevo-nombre
# → Mueve/renombra rama actual

# 13. Renombrar otra rama
git branch -m viejo-nombre nuevo-nombre
# → Renombra rama específica

# 14. Mover rama a otro commit (¡PODER!)
git branch -f feature-x abc123
# → Mueve feature-x al commit abc123
# → Útil para "rewind" de rama
# → Cuidado: reescribe historia
```

**Estrategias de branching:**

```bash
# ESTRATEGIA 1: Feature Branch Workflow
main
 └─ feature/user-auth      (desarrollo de feature)
 └─ feature/payment        (otra feature en paralelo)
 └─ bugfix/login-error     (fix de bug)

Workflow:
git checkout -b feature/user-auth
# ... trabajas ...
git push origin feature/user-auth
# ... abres PR ...
# ... mergeas a main ...
git branch -d feature/user-auth

# ESTRATEGIA 2: Git Flow
main                    (producción)
 ├─ develop            (integración)
 │   ├─ feature/x     (features)
 │   └─ feature/y
 └─ hotfix/critical   (fixes urgentes)

# ESTRATEGIA 3: GitHub Flow (simple)
main
 ├─ feature-x         (todo sale de main)
 └─ bugfix-y          (todo vuelve a main)

# ESTRATEGIA 4: Trunk-Based Development
main                  (todos commitean aquí)
 └─ release/v1.0     (solo para releases)
```

**Casos de uso reales:**

```bash
# Caso 1: Feature en progreso, necesitas hotfix
# Estás en: feature-x (trabajo a medias)
git stash                    # Guarda trabajo actual
git checkout main
git checkout -b hotfix-login
# ... arreglas ...
git checkout main
git merge hotfix-login
git push
git checkout feature-x
git stash pop               # Recupera trabajo

# Caso 2: Múltiples features en paralelo
git checkout -b feature-auth
# ... trabajo en auth ...
git checkout main
git checkout -b feature-payment
# ... trabajo en payment ...
git checkout feature-auth   # Vuelves a auth
# Cada rama independiente

# Caso 3: Limpieza de ramas viejas
git branch --merged | grep -v "main" | xargs git branch -d
# → Elimina todas las ramas ya mergeadas
# → Excepto main
# → Limpieza automática

# Caso 4: Ver qué ramas tienen trabajo único
git branch --no-merged
# → Identifica ramas con commits no mergeados
# → Revisa antes de eliminar

# Caso 5: Sincronizar rama con main
git checkout feature-x
git merge main              # Opción 1: merge
# o:
git rebase main            # Opción 2: rebase (historia lineal)

# Caso 6: Rescatar rama eliminada accidentalmente
git branch feature-x abc123
# → Si recuerdas el commit hash
# o:
git reflog | grep feature-x
# → Busca en reflog el hash
git branch feature-x <hash>
```

**Ver información de ramas:**

```bash
# Ver rama actual
git branch --show-current
# → Solo nombre de rama actual

# Ver todas con detalles
git branch -vv
# → Muestra remote tracking info

# Ver ramas ordenadas por fecha
git branch --sort=-committerdate
# → Más recientes primero

# Ver qué commits están en rama A pero no en B
git log main..feature-x
# → Commits únicos de feature-x

# Ver diferencias entre ramas
git diff main..feature-x
# → Qué cambió entre las ramas
```

**Mejores prácticas:**

```bash
✓ Usa nombres descriptivos (feature/user-auth, no rama1)
✓ Usa prefijos (feature/, bugfix/, hotfix/)
✓ Crea ramas frecuentemente (son gratis)
✓ Elimina ramas después de merge
✓ Sincroniza con main/develop regularmente
✓ Usa git branch --merged para identificar ramas obsoletas

✗ Evita nombres genéricos (test, temp, new)
✗ No trabajes directamente en main (usa branches)
✗ No dejes ramas sin mergear por meses
✗ No uses git branch -D a menos que estés seguro
✗ No nombres ramas con espacios o caracteres especiales
```

---

#### 9.3.4 git checkout / git switch - Navegando el Código

**Funcionamiento interno:**

```
Cambiar de rama:
git checkout main

Internamente:
1. git rev-parse refs/heads/main
   → Obtiene hash del commit de main
   → Por ejemplo: abc123...
   
2. git read-tree abc123
   → Lee el tree object del commit
   
3. Actualiza working directory
   → Para cada archivo en tree:
     - Si difiere de working: actualiza
     - Si no existe: crea
   → Para cada archivo en working no en tree:
     - Si está rastreado: elimina
     - Si no rastreado: mantiene
   
4. git symbolic-ref HEAD refs/heads/main
   → Actualiza HEAD para apuntar a main
   → Archivo .git/HEAD ahora contiene: ref: refs/heads/main
   
5. Actualiza .git/index
   → Sincroniza staging con nuevo tree

Resultado:
- Working directory actualizado
- Staging actualizado
- HEAD apunta a nueva rama
- Commit al que apunta HEAD cambió
```

**git checkout vs git switch (Git 2.23+):**

```bash
# CHECKOUT: Comando "multiuso" (confuso)
git checkout main          # Cambiar rama
git checkout abc123        # Ir a commit (detached HEAD)
git checkout -- file.txt   # Descartar cambios de archivo
git checkout -b nueva      # Crear y cambiar a rama

# SWITCH: Solo para cambiar ramas (claro)
git switch main            # Cambiar rama
git switch -c nueva        # Crear y cambiar a rama
git switch -               # Volver a rama anterior

# RESTORE: Solo para restaurar archivos (Git 2.23+)
git restore file.txt       # Descartar cambios
git restore --staged file.txt  # Unstage
git restore --source=abc123 file.txt  # Desde commit

RECOMENDACIÓN:
→ Usa switch para ramas
→ Usa restore para archivos
→ checkout sigue funcionando (compatibilidad)
```

**Uso práctico - git checkout:**

```bash
# 1. Cambiar a rama existente
git checkout main
# → HEAD → main
# → Working directory actualizado

# 2. Crear y cambiar a nueva rama
git checkout -b feature-x
# → Crea rama
# → Cambia a ella
# → Equivale a: git branch feature-x && git checkout feature-x

# 3. Ir a commit específico (detached HEAD)
git checkout abc123
# → HEAD apunta directamente al commit
# → NO estás en ninguna rama
# → Útil para inspeccionar historia

# 4. Ir a tag
git checkout v1.0.0
# → Similar a checkout de commit
# → Detached HEAD en el commit del tag

# 5. Volver a rama anterior
git checkout -
# → Atajo para volver a donde estabas
# → Equivale a: cd - en bash

# 6. Descartar cambios de archivo
git checkout -- archivo.txt
# → Restaura archivo desde staging
# → Pierdes cambios en working directory
# → ¡CUIDADO! No es reversible

# 7. Restaurar archivo desde commit específico
git checkout abc123 -- archivo.txt
# → Trae archivo de ese commit
# → Lo pone en staging
# → Útil para "rescatar" versión antigua

# 8. Checkout remoto
git checkout origin/main
# → Detached HEAD en commit de origin/main
# → Para inspeccionar rama remota

# 9. Checkout con path (archivos específicos)
git checkout main -- src/
# → Trae directorio src/ desde main
# → Sin cambiar de rama
```

**Uso práctico - git switch (recomendado):**

```bash
# 1. Cambiar a rama
git switch main
# → Más claro que checkout

# 2. Crear y cambiar
git switch -c feature-x
# → Equivale a: checkout -b

# 3. Volver a rama anterior
git switch -
# → Igual que checkout -

# 4. Forzar cambio (descartar cambios locales)
git switch -f otra-rama
# o: git switch --force otra-rama
# → Cambia aunque tengas cambios no commiteados
# → ¡CUIDADO! Pierdes cambios

# 5. Crear rama desde commit
git switch -c nueva-rama abc123
# → Crea rama desde commit específico

# 6. Detach HEAD a commit
git switch --detach abc123
# → Explícitamente ir a detached HEAD
```

**Uso práctico - git restore:**

```bash
# 1. Descartar cambios en archivo
git restore archivo.txt
# → Restaura desde staging
# → Equivale a: checkout -- archivo.txt

# 2. Unstage archivo
git restore --staged archivo.txt
# → Quita de staging
# → Archivo modificado sigue en working

# 3. Descartar y unstage
git restore --staged --worktree archivo.txt
# → Quita de staging Y descarta cambios

# 4. Restaurar desde commit específico
git restore --source=abc123 archivo.txt
# → Trae archivo de ese commit
# → Va a working directory

# 5. Restaurar y stagear desde commit
git restore --source=abc123 --staged archivo.txt
# → Trae archivo y lo stagea

# 6. Restaurar todo
git restore .
# → Descarta cambios de TODOS los archivos
```

**Detached HEAD explicado:**

```bash
¿Qué es detached HEAD?

HEAD normalmente apunta a rama:
HEAD → main → abc123

Detached HEAD apunta directamente a commit:
HEAD → abc123

# Entras a detached HEAD con:
git checkout abc123
git checkout v1.0.0
git checkout HEAD~3

# WARNING visible:
You are in 'detached HEAD' state...

# ¿Por qué usar detached HEAD?
✓ Inspeccionar commits viejos
✓ Probar código de commit específico
✓ Ver estado en punto específico de historia

# ¿Peligro?
- Si comiteas en detached HEAD, el commit queda "huérfano"
- No hay rama que lo referencie
- Se puede perder en garbage collection

# Cómo salir de detached HEAD:
# Opción 1: Volver a rama
git checkout main

# Opción 2: Crear rama aquí
git switch -c nueva-rama

# Opción 3: Si comiteaste y quieres guardar
git branch temp-rama       # Crea rama en HEAD actual
git checkout main
git merge temp-rama        # Integra el trabajo
```

**Casos de uso reales:**

```bash
# Caso 1: Comparar versiones
git checkout v1.0.0
# ... revisa código ...
git checkout v2.0.0
# ... compara ...
git checkout main          # Vuelves

# Caso 2: Bisect (encontrar bug)
git checkout abc123        # Commit viejo
# ... pruebas: ¿bug presente? ...
git checkout def456        # Commit más nuevo
# ... pruebas: ¿bug presente? ...
# (git bisect automatiza esto)

# Caso 3: Rescatar archivo borrado
git log -- archivo-borrado.txt    # Encuentra último commit con archivo
git checkout abc123 -- archivo-borrado.txt
# → Archivo restaurado y stageado

# Caso 4: Probar PR sin mergear
git fetch origin pull/123/head:pr-123
git checkout pr-123
# ... pruebas ...
git checkout main

# Caso 5: Trabajo simultáneo en múltiples ramas
git switch feature-a
# ... código ...
git stash                  # Guarda cambios
git switch feature-b
# ... código diferente ...
git switch feature-a
git stash pop              # Recupera cambios

# Caso 6: Ver código en producción
git checkout production
# ... revisas ...
git switch -               # Vuelves a donde estabas
```

**Problemas comunes y soluciones:**

```bash
# Problema 1: No puedes cambiar (cambios no commiteados)
$ git checkout main
error: Your local changes would be overwritten

# Soluciones:
git stash                  # Guarda temporal
git checkout main
git stash pop              # Recupera

# o:
git checkout -f main       # Fuerza (¡pierdes cambios!)

# o:
git commit -m "WIP"        # Commitea temporal
git checkout main

# Problema 2: Detached HEAD accidental
$ git checkout abc123
# ... hiciste commits ...
$ git checkout main
WARNING: Commit huérfano

# Solución:
git reflog                 # Encuentra hash del commit
git branch rescue-branch <hash>
git merge rescue-branch

# Problema 3: Archivo en conflicto
$ git checkout main
error: path 'file.txt' is unmerged

# Solución:
git reset --merge          # Aborta merge en progreso
# o:
git checkout --ours file.txt    # Elige versión local
git checkout --theirs file.txt  # Elige versión remota
```

**Mejores prácticas:**

```bash
✓ Usa git switch para cambiar ramas (más claro)
✓ Usa git restore para archivos (más claro)
✓ Commitea o stash antes de cambiar ramas
✓ Usa checkout -b para crear ramas (o switch -c)
✓ Entiende detached HEAD antes de usarlo
✓ Usa git switch - para alternar rápidamente

✗ Evita checkout sin especificar qué (ambiguo)
✗ No uses checkout -f a menos que estés seguro
✗ No trabajes en detached HEAD sin crear rama
✗ No uses checkout para deshacer (confuso, usa restore/reset)
✗ Evita checkout de ramas remotas directamente
```

---

#### 9.3.5 git merge - Integrando Cambios

**Funcionamiento interno:**

```
Merge de rama:
git merge feature-x

Git analiza tres puntos:
1. Commit base común (merge base)
2. Tip de rama actual (ours)
3. Tip de rama a mergear (theirs)

Casos:

CASO 1: Fast-forward (historia lineal)
main:    A ← B
feature:      ↖ C ← D

Merge:
→ Solo mueve puntero de main a D
→ No se crea commit de merge
→ Historia queda: A ← B ← C ← D (main)

CASO 2: Three-way merge (historias divergentes)
main:    A ← B ← C
feature:     ↖ D ← E

Merge:
1. Encuentra base común (B)
2. Compara cambios:
   - B → C (cambios en main)
   - B → E (cambios en feature)
3. Combina ambos cambios
4. Crea commit de merge M con DOS padres:
   A ← B ← C ←←
              M (main)
         D ← E ↗

CASO 3: Conflicto
→ Si mismas líneas cambiaron en ambas ramas
→ Git no puede decidir automáticamente
→ Marca conflictos en archivos
→ Usuario debe resolver manualmente

Internamente:
1. git merge-base main feature-x
   → Encuentra commit base común
   
2. git read-tree -m <base> <ours> <theirs>
   → Lee tres trees, identifica diferencias
   
3. Si no hay conflictos:
   git write-tree → crea tree del resultado
   git commit-tree → crea commit de merge
   git update-ref → actualiza rama
   
4. Si hay conflictos:
   → Marca archivos en .git/index
   → Usuario resuelve
   → Usuario commitea manualmente
```

**Uso práctico y opciones:**

```bash
# 1. Merge básico
git checkout main
git merge feature-x
# → Integra feature-x en main
# → Fast-forward si es posible

# 2. Merge sin fast-forward (siempre crea commit de merge)
git merge --no-ff feature-x
# → Fuerza creación de merge commit
# → Útil para mantener historia de features
# → GitHub hace esto por defecto en PRs

# 3. Merge con mensaje personalizado
git merge feature-x -m "Merge feature X: add authentication"
# → Mensaje personalizado del merge commit

# 4. Merge solo si es fast-forward
git merge --ff-only feature-x
# → Aborta si requiere merge commit
# → Útil para mantener historia lineal
# → Falla si hay divergencia

# 5. Merge squash (combina todos los commits en uno)
git merge --squash feature-x
# → Aplica todos los cambios
# → NO crea commit automáticamente
# → Los cambios quedan stageados
git commit -m "Add feature X"
# → Historia limpia: un commit por feature

# 6. Abortar merge en progreso
git merge --abort
# → Cancela merge
# → Restaura estado previo
# → Útil si conflictos son complejos

# 7. Continuar después de resolver conflictos
# ... resuelves conflictos en archivos ...
git add archivo-resuelto.txt
git merge --continue
# → Completa el merge

# 8. Ver estado de merge
git status
# → Muestra archivos en conflicto
# → Muestra qué hacer

# 9. Merge con estrategia específica
git merge -X ours feature-x
# → En conflictos, prefiere versión "ours" (main)
git merge -X theirs feature-x
# → En conflictos, prefiere versión "theirs" (feature-x)

# 10. Merge sin commit automático
git merge --no-commit feature-x
# → Aplica merge
# → NO commitea
# → Puedes revisar antes de commitear
```

**Resolución de conflictos:**

```bash
# Cuando hay conflicto, archivo se ve así:
<<<<<<< HEAD (main)
código de main
=======
código de feature-x
>>>>>>> feature-x

# Proceso de resolución:
# 1. Ver archivos en conflicto
git status
# → Lista "both modified"

# 2. Abrir archivo, elegir qué conservar
# Opción A: Conservar versión main
# Opción B: Conservar versión feature
# Opción C: Combinar manualmente

# 3. Eliminar marcadores de conflicto
# Dejar solo código final

# 4. Añadir archivo resuelto
git add archivo-resuelto.txt

# 5. Continuar merge
git commit
# → Abre editor con mensaje de merge predeterminado

# Herramientas para resolución:
git mergetool
# → Abre herramienta gráfica (vimdiff, meld, etc.)

# Ver diferencias durante conflicto:
git diff               # Muestra conflictos
git diff --ours        # Solo cambios en rama actual
git diff --theirs      # Solo cambios en rama mergeada
git diff --base        # Cambios desde base común

# Elegir versión completa de archivo:
git checkout --ours archivo.txt    # Usa versión de main
git checkout --theirs archivo.txt  # Usa versión de feature
git add archivo.txt                # Marca como resuelto
```

**Estrategias de merge:**

```bash
# Estrategia 1: Merge commits (por defecto)
git merge feature-x
# → Historia mantiene todas las ramas
# → Grafo complejo pero completo
Ventajas: Historia completa, reversible
Desventajas: Grafo puede ser confuso

# Estrategia 2: Squash merge (historia limpia)
git merge --squash feature-x
git commit -m "feat: Add feature X"
# → Un commit por feature
# → Historia lineal
Ventajas: Historia simple y clara
Desventajas: Pierdes commits individuales de feature

# Estrategia 3: Fast-forward only
git merge --ff-only feature-x
# → Solo si historia es lineal
# → Falla si hay divergencia
Ventajas: Historia perfectamente lineal
Desventajas: Requiere rebase frecuente

# Estrategia 4: Rebase antes de merge
git checkout feature-x
git rebase main
git checkout main
git merge feature-x  # Ahora es fast-forward
Ventajas: Historia lineal
Desventajas: Reescribe commits de feature
```

**Casos de uso reales:**

```bash
# Caso 1: Merge de PR en GitHub (simular localmente)
git checkout main
git merge --no-ff feature-x -m "Merge PR #123: Add auth"
# → Crea merge commit aunque sea fast-forward
# → Marca claramente el merge en historia

# Caso 2: Integración continua de develop
git checkout main
git merge develop
# → Integra cambios acumulados

# Caso 3: Merge con conflictos extensos
git merge feature-x
# ... muchos conflictos ...
git merge --abort      # Aborta
# Alternativa: pide al autor de feature-x que rebase
git checkout feature-x
git rebase main        # Resuelve conflictos aquí
git checkout main
git merge feature-x    # Ahora sin conflictos

# Caso 4: Merge de múltiples ramas
git merge feature-a feature-b feature-c
# → Octopus merge (múltiples padres)
# → Solo funciona sin conflictos

# Caso 5: Cherry-pick específico vs merge completo
# Si solo quieres un commit:
git cherry-pick abc123
# Si quieres toda la rama:
git merge feature-x
```

**Mejores prácticas:**

```bash
✓ Usa --no-ff para features importantes (marca en historia)
✓ Resuelve conflictos en rama feature, no en main
✓ Prueba el código después de merge antes de push
✓ Usa --squash para features con commits WIP
✓ Documenta resolución de conflictos complejos
✓ Considera rebase antes de merge para historia limpia

✗ No hagas merge de main a feature frecuentemente (usa rebase)
✗ No fuerces -X ours/theirs sin revisar cambios
✗ No ignores conflictos (revisar siempre)
✗ No hagas merge directo a main sin revisar (usa PRs)
✗ Evita merges gigantes (integra frecuentemente)
```

---

#### 9.3.6 git rebase - Reescribiendo Historia

**Funcionamiento interno:**

```
Rebase de rama:
git rebase main

Estado inicial:
main:    A ← B ← C
feature:     ↖ D ← E (HEAD)

Git hace:
1. Identifica commits únicos de feature (D, E)
2. Guarda como patches temporales
3. Resetea feature a main (C)
4. Aplica patches uno por uno:
   - Aplica cambios de D → crea D'
   - Aplica cambios de E → crea E'

Resultado:
main:    A ← B ← C
feature:             ↖ D' ← E' (HEAD)

IMPORTANTE:
→ D' y E' son NUEVOS commits (diferente hash)
→ D y E originales quedan huérfanos (se pueden GC)
→ Historia parece lineal

Internamente:
1. git merge-base feature main
   → Encuentra punto de divergencia (B)
   
2. git log B..feature
   → Lista commits a replicar (D, E)
   
3. Para cada commit:
   git format-patch        → Crea patch
   git reset --hard main   → Mueve feature a main
   git apply <patch>       → Aplica cambios
   git commit              → Crea nuevo commit
   
4. git update-ref refs/heads/feature <nuevo-hash>
   → Feature apunta al último commit nuevo
```

**Uso práctico y opciones:**

```bash
# 1. Rebase básico
git checkout feature-x
git rebase main
# → Mueve commits de feature-x encima de main
# → Historia lineal

# 2. Rebase interactivo (SUPER PODEROSO)
git rebase -i main
# → Abre editor con lista de commits
# → Puedes reordenar, editar, squash, drop

# Opciones en rebase interactivo:
pick abc123 feat: Add login
pick def456 fix: Typo
pick ghi789 refactor: Clean code

Comandos disponibles:
pick   → Usar commit tal cual
reword → Cambiar mensaje del commit
edit   → Pausar para modificar commit
squash → Combinar con commit anterior (mantiene mensaje)
fixup  → Combinar con commit anterior (descarta mensaje)
drop   → Eliminar commit
exec   → Ejecutar comando shell

# 3. Squash de múltiples commits
git rebase -i HEAD~3
# Cambia:
pick abc123 WIP: feature
pick def456 WIP: more work
pick ghi789 WIP: final
# A:
pick abc123 WIP: feature
squash def456 WIP: more work
squash ghi789 WIP: final
# Resultado: 1 commit con cambios de los 3

# 4. Rebase de último N commits
git rebase -i HEAD~5
# → Rebase interactivo de últimos 5 commits

# 5. Rebase hacia atrás (más raro)
git rebase --onto main feature-a feature-b
# → Mueve commits de feature-b (que vienen de feature-a) a main

# 6. Continuar rebase después de resolver conflicto
# ... resuelves conflicto ...
git add archivo-resuelto.txt
git rebase --continue

# 7. Saltar commit con conflicto
git rebase --skip
# → Omite el commit actual
# → Útil si el cambio ya no es relevante

# 8. Abortar rebase
git rebase --abort
# → Cancela rebase
# → Restaura estado original

# 9. Rebase automático resolviendo conflictos
git rebase -X ours main
# → Prefiere versión actual en conflictos
git rebase -X theirs main
# → Prefiere versión de main en conflictos

# 10. Rebase preservando merges
git rebase --rebase-merges main
# → Mantiene merge commits en historia
# → Útil para ramas complejas
```

**Rebase interactivo - Casos de uso:**

```bash
# Caso 1: Limpiar commits WIP
git rebase -i HEAD~5
# Antes:
# - WIP: start feature
# - WIP: continue
# - WIP: almost done
# - WIP: done
# - fix: typo
# Después:
# - feat: Add complete feature

# Caso 2: Reordenar commits
git rebase -i HEAD~3
# Reordena:
pick ghi789 docs: Update README
pick abc123 feat: Add feature
pick def456 test: Add tests
# A:
pick abc123 feat: Add feature
pick def456 test: Add tests
pick ghi789 docs: Update README
# → Orden lógico mejor

# Caso 3: Editar commit antiguo
git rebase -i HEAD~10
# Marca commit para editar:
edit abc123 feat: Add feature
# Git pausa en ese commit:
# ... haces cambios ...
git add .
git commit --amend
git rebase --continue

# Caso 4: Dividir commit grande
git rebase -i HEAD~3
# Marca para editar:
edit abc123 feat: Multiple changes
# Git pausa:
git reset HEAD~        # Unstage todo
git add file1.txt
git commit -m "feat: Add part 1"
git add file2.txt
git commit -m "feat: Add part 2"
git rebase --continue

# Caso 5: Eliminar commit sensible
git rebase -i HEAD~10
# Cambia pick a drop:
drop abc123 chore: Add API keys (¡ups!)
# O simplemente borra la línea
```

**Rebase vs Merge:**

```bash
# MERGE: Preserva historia completa
git merge feature-x
Resultado:
main:    A ← B ← C ←←
              ↖    M (main)
         D ← E ← ↗
Ventajas:
✓ Historia completa
✓ No reescribe commits
✓ Seguro para ramas públicas
Desventajas:
✗ Grafo complejo
✗ Historia difícil de seguir

# REBASE: Historia lineal
git rebase main
Resultado:
main:    A ← B ← C ← D' ← E' (feature)
Ventajas:
✓ Historia lineal
✓ Fácil de entender
✓ Bisect más efectivo
Desventajas:
✗ Reescribe commits
✗ Peligroso para ramas públicas
✗ Conflictos más frecuentes

# ¿Cuándo usar cada uno?
REBASE:
→ Rama local/feature antes de merge
→ Actualizar feature con main
→ Limpiar commits antes de PR

MERGE:
→ Integrar features a main
→ Ramas públicas colaborativas
→ Cuando historia es importante
```

**Regla de oro del rebase:**

```bash
⚠️ NUNCA rebasees commits ya pusheados a repositorio público

Incorrecto:
git push origin feature-x
# ... otros trabajan en feature-x ...
git rebase main           # ¡REESCRIBE COMMITS PÚBLICOS!
git push --force          # ¡ROMPES REPO DE OTROS!

Correcto:
# Antes de push:
git rebase main           # OK, commits solo locales
git push origin feature-x

# Después de push:
git merge main            # Usa merge, no rebase
git push origin feature-x
```

**Casos de uso reales:**

```bash
# Caso 1: Feature con commits WIP
git checkout feature-x
# ... 20 commits tipo "WIP", "temp", "fix typo" ...
git rebase -i main
# Squash todo en commits lógicos
git push --force-with-lease origin feature-x

# Caso 2: Sincronizar feature con main
git checkout feature-x
git fetch origin
git rebase origin/main
# → Feature ahora está encima de main actualizado

# Caso 3: Limpiar historia antes de PR
git rebase -i HEAD~10
# → Squash, reorder, reword
# → Historia profesional

# Caso 4: Actualizar rama abandonada
git checkout old-feature
git rebase main
# ... resuelves conflictos ...
git rebase --continue
# → Rama ahora compatible con main actual

# Caso 5: Backport de commits
git rebase --onto release-1.0 main feature-x
# → Mueve commits de feature-x a release-1.0
```

**Problemas comunes y soluciones:**

```bash
# Problema 1: Conflictos en cada commit
git rebase main
# ... conflicto en commit 1 ...
# ... conflicto en commit 2 ...
# ... conflicto en commit 3 ...

# Solución: Aborta y usa merge
git rebase --abort
git merge main

# Problema 2: Perdiste commits después de rebase
git reflog
# Encuentra hash de antes del rebase
git reset --hard abc123

# Problema 3: Force push rechazado
git push --force origin feature-x
# error: Updates were rejected

# Solución: Usa --force-with-lease (más seguro)
git push --force-with-lease origin feature-x
# → Solo fuerza si nadie más actualizó la rama
```

**Mejores prácticas:**

```bash
✓ Usa rebase para limpiar historia local
✓ Rebase feature sobre main antes de merge
✓ Usa rebase interactivo para commits profesionales
✓ Usa --force-with-lease en vez de --force
✓ Nunca rebasees ramas públicas compartidas
✓ Commitea o stash antes de rebase

✗ No rebasees main o develop
✗ No rebasees commits ya pusheados (sin coordinación)
✗ No uses --force sin --force-with-lease
✗ No rebasees si no entiendes las consecuencias
✗ Evita rebase en ramas con múltiples colaboradores
```

---

#### 9.3.7 git reset - Moviendo Referencias

**Funcionamiento interno:**

```
git reset mueve referencias y opcionalmente modifica staging/working

Tres "objetivos" que puede resetear:
1. HEAD (y rama actual)
2. Staging area (.git/index)
3. Working directory

Tres modos principales:

git reset --soft HEAD~1
→ Solo mueve HEAD/rama
→ Staging se mantiene
→ Working se mantiene
Uso: "Deshacer commit, mantener cambios stageados"

git reset --mixed HEAD~1 (POR DEFECTO)
→ Mueve HEAD/rama
→ Resetea staging desde nuevo HEAD
→ Working se mantiene
Uso: "Deshacer commit y staging, mantener archivos"

git reset --hard HEAD~1
→ Mueve HEAD/rama
→ Resetea staging
→ Resetea working directory
Uso: "Deshacer todo, volver a commit antiguo"

Internamente:
1. git rev-parse <target>
   → Resuelve target a hash de commit
   
2. git update-ref refs/heads/<branch> <hash>
   → Mueve referencia de rama
   
3. (si --mixed o --hard) git read-tree <hash>
   → Actualiza staging desde commit
   
4. (si --hard) Actualiza working directory
   → Sobrescribe archivos

IMPORTANTE: NO borra commits, solo mueve punteros
→ Commits "perdidos" siguen en reflog
→ Recuperables con git reflog
```

**Uso práctico y opciones:**

```bash
# 1. Reset suave (deshacer commit, mantener cambios)
git reset --soft HEAD~1
# → Commit deshecho
# → Cambios en staging
# → Útil para re-commitear con mejor mensaje

# 2. Reset mixto (por defecto)
git reset HEAD~1
# o explícito:
git reset --mixed HEAD~1
# → Commit deshecho
# → Cambios en working directory (sin stagear)
# → Útil para reorganizar commits

# 3. Reset duro (CUIDADO: pierdes cambios)
git reset --hard HEAD~1
# → Commit deshecho
# → Cambios descartados
# → Working directory limpio
# → Útil para descartar trabajo

# 4. Reset a commit específico
git reset --hard abc123
# → Mueve rama a commit abc123
# → Descarta todo posterior

# 5. Unstage archivo (uso común)
git reset HEAD archivo.txt
# o en Git 2.23+:
git restore --staged archivo.txt
# → Quita archivo de staging
# → Archivo sigue modificado en working

# 6. Unstage todo
git reset
# → Quita todo de staging
# → Archivos siguen modificados

# 7. Reset a origen remoto
git reset --hard origin/main
# → Sincroniza con remoto
# → Descarta cambios locales
# → Útil para "empezar de cero"

# 8. Reset manteniendo archivos untracked
git reset --hard HEAD
# → Descarta cambios en archivos rastreados
# → Mantiene archivos nuevos sin añadir

# 9. Reset de path específico (avanzado)
git reset HEAD~2 -- archivo.txt
# → Trae versión de archivo de 2 commits atrás
# → Solo afecta ese archivo
```

**Comparación de modos:**

```bash
Estado inicial:
Repository: A ← B ← C (main, HEAD)
Staging:    [cambios preparados]
Working:    [archivo.txt modificado]

# SOFT:
git reset --soft HEAD~1
Repository: A ← B (main, HEAD), C (huérfano)
Staging:    [cambios preparados] ← MANTIENE
Working:    [archivo.txt modificado] ← MANTIENE

# MIXED (default):
git reset HEAD~1
Repository: A ← B (main, HEAD), C (huérfano)
Staging:    [vacío] ← RESETEA
Working:    [archivo.txt modificado] ← MANTIENE

# HARD:
git reset --hard HEAD~1
Repository: A ← B (main, HEAD), C (huérfano)
Staging:    [vacío] ← RESETEA
Working:    [archivo.txt limpio] ← RESETEA
```

**Casos de uso reales:**

```bash
# Caso 1: Corrección de mensaje de commit
git reset --soft HEAD~1
git commit -m "Mensaje corregido"
# Alternativa más simple: git commit --amend

# Caso 2: Combinar últimos N commits
git reset --soft HEAD~3
# → 3 commits deshech os
# → Cambios en staging
git commit -m "Combined feature"

# Caso 3: Mover cambios a nueva rama
# Estás en main con cambios sin commitear
git branch feature-x       # Crea rama aquí
git reset --hard origin/main  # Limpia main
git checkout feature-x     # Cambia a feature
# → Cambios preservados en feature-x

# Caso 4: Descartar trabajo en progreso
git reset --hard HEAD
# → Descarta todos los cambios
git clean -fd
# → Elimina archivos untracked

# Caso 5: Sincronizar con remoto (abandonar local)
git fetch origin
git reset --hard origin/main
# → main local = main remoto
# → Cambios locales perdidos

# Caso 6: Unstage selectivo
git add .
git status  # Ups, añadiste archivo secreto
git reset HEAD secreto.txt
# → Solo secreto.txt fuera de staging

# Caso 7: "Rewind" de múltiples commits
git reset --hard HEAD~10
# → Vuelves 10 commits atrás
# → Útil para experimentar
# → Recuperable con reflog
```

**Reset vs Revert vs Checkout:**

```bash
# RESET: Mueve rama atrás (reescribe historia)
git reset --hard HEAD~1
Antes: A ← B ← C (main)
Después: A ← B (main), C (huérfano)
→ "Borra" commits moviendo puntero
→ Peligroso para commits públicos

# REVERT: Crea nuevo commit que deshace cambios
git revert HEAD
Antes: A ← B ← C (main)
Después: A ← B ← C ← C' (main)
→ C' deshace cambios de C
→ Historia se mantiene
→ Seguro para commits públicos

# CHECKOUT: Mueve HEAD, no rama
git checkout abc123
Antes: HEAD → main → C
Después: HEAD → abc123 (detached)
→ main sigue en C
→ Solo navegas, no reescribes
```

**Recuperación después de reset:**

```bash
# Si hiciste reset por error:
git reset --hard HEAD~5
# ¡Oh no! Quería esos commits

# Solución con reflog:
git reflog
# abc123 HEAD@{0}: reset: moving to HEAD~5
# def456 HEAD@{1}: commit: Important work
# ...

git reset --hard def456
# o:
git reset --hard HEAD@{1}
# → Commits recuperados

# Ver reflog completo:
git reflog show main
# → Historia de movimientos de main
```

**Mejores prácticas:**

```bash
✓ Usa --soft para reorganizar commits
✓ Usa --mixed (default) para unstage
✓ Usa --hard solo si estás seguro
✓ Usa reset para cambios locales no pusheados
✓ Verifica con git status antes de reset --hard
✓ Recuerda: reflog es tu red de seguridad

✗ No uses reset --hard en commits públicos
✗ No uses reset en main/develop compartidos
✗ Evita reset --hard sin verificar cambios
✗ No confundas reset con revert
✗ No uses reset si hay uncommitted work importante
```

---

#### 9.3.8 git stash - Guardado Temporal

**Funcionamiento interno:**

```
git stash guarda trabajo en progreso temporalmente

Internamente crea commits especiales:
1. Stash commit principal (working directory)
2. Index stash (staging area)
3. Untracked stash (opcional)

Estos commits NO están en ninguna rama:
→ Se guardan en refs/stash
→ Como una pila (stack): LIFO
→ Cada stash tiene un identificador: stash@{0}, stash@{1}, etc.

git stash
Internamente:
1. git commit -m "WIP on <branch>"
   → Commitea working directory y staging
   
2. Guarda en refs/stash
   → Commit especial referenciado por stash
   
3. git reset --hard HEAD
   → Limpia working directory
   
Resultado:
→ Working y staging limpios
→ Cambios guardados en stash
→ Puedes cambiar de rama
```

**Uso práctico y opciones:**

```bash
# 1. Stash básico
git stash
# o explícito:
git stash push
# → Guarda working y staging
# → Working directory queda limpio
# → Con mensaje automático

# 2. Stash con mensaje
git stash push -m "WIP: feature half done"
# → Mensaje personalizado
# → Útil para identificar después

# 3. Stash incluyendo untracked
git stash -u
# o:
git stash --include-untracked
# → Guarda también archivos nuevos
# → Útil cuando añadiste archivos

# 4. Stash incluyendo ignored
git stash -a
# o:
git stash --all
# → Guarda TODO, incluso .gitignore
# → Raramente necesario

# 5. Stash solo de archivos específicos
git stash push -m "Partial work" archivo.txt src/
# → Solo stashea archivos indicados
# → Resto se mantiene

# 6. Stash interactivo (elegir qué guardar)
git stash -p
# → Similar a git add -p
# → Eliges qué hunks stashear

# 7. Ver lista de stashes
git stash list
# stash@{0}: WIP on main: abc123 Last commit message
# stash@{1}: On feature-x: def456 Other work
# → Más reciente = stash@{0}

# 8. Ver contenido de stash
git stash show
# → Muestra resumen de cambios de stash@{0}

git stash show -p
# → Muestra diff completo

git stash show stash@{1}
# → Muestra stash específico

# 9. Aplicar stash (mantiene en lista)
git stash apply
# → Aplica stash@{0}
# → Stash sigue en lista
# → Útil si quieres aplicar en múltiples ramas

git stash apply stash@{2}
# → Aplica stash específico

# 10. Pop stash (aplica y elimina)
git stash pop
# → Aplica stash@{0}
# → Elimina de lista
# → Uso más común

# 11. Crear rama desde stash
git stash branch nueva-rama
# → Crea rama desde commit donde se stasheó
# → Aplica stash
# → Elimina stash
# → Útil si stash tiene conflictos en rama actual

# 12. Eliminar stash
git stash drop
# → Elimina stash@{0}

git stash drop stash@{1}
# → Elimina stash específico

# 13. Limpiar todos los stashes
git stash clear
# → ELIMINA TODOS los stashes
# → ¡CUIDADO! No reversible fácilmente
```

**Casos de uso reales:**

```bash
# Caso 1: Cambio urgente en otra rama
# Estás en feature-x con trabajo a medias
git stash
git checkout main
git checkout -b hotfix
# ... arreglas bug ...
git checkout feature-x
git stash pop
# → Trabajo recuperado

# Caso 2: Pull con cambios locales
git pull
# error: Your local changes would be overwritten

git stash
git pull
git stash pop
# → Ahora tienes tus cambios + cambios remotos

# Caso 3: Experimentar sin perder trabajo
git stash
# ... experimentas con código ...
git reset --hard HEAD  # Descarta experimento
git stash pop          # Recupera trabajo original

# Caso 4: Stash para aplicar en múltiples ramas
# Tienes fix que aplica a varias ramas
git stash
git checkout release-1.0
git stash apply        # Aplica (no elimina)
git commit -am "Fix for 1.0"
git checkout release-2.0
git stash apply        # Aplica otra vez
git commit -am "Fix for 2.0"
git stash drop         # Ahora sí elimina

# Caso 5: Limpiar working directory temporalmente
git stash -u
# → Working completamente limpio
# ... revisas algo ...
git stash pop
# → Vuelve a estado anterior

# Caso 6: "Mover" cambios a otra rama
# Hiciste cambios en main por error
git stash
git checkout -b feature-x
git stash pop
# → Cambios ahora en feature-x

# Caso 7: Stash de archivos no relacionados
# Modificaste file1.txt (feature) y file2.txt (bug)
git stash push -m "Bug fix" file2.txt
git commit -am "feat: Add feature (file1)"
git stash pop
git commit -am "fix: Bug (file2)"
```

**Stash con conflictos:**

```bash
# Si al aplicar stash hay conflictos:
git stash pop
# Auto-merging file.txt
# CONFLICT (content): Merge conflict in file.txt

# Resuelves como merge normal:
# ... editas archivo ...
git add file.txt
# Git NO hace commit automáticamente
git stash drop  # Opcional: elimina el stash manualmente

# Alternativa si conflictos son complejos:
git stash branch temp-branch
# → Crea rama desde donde se stasheó
# → Aplica stash sin conflictos
# → Puedes mergear después
```

**Ver contenido detallado:**

```bash
# Ver stash como commit
git show stash@{0}
# → Muestra diff completo

# Ver archivos en stash
git stash show stash@{0} --name-only
# → Lista de archivos modificados

# Ver stats
git stash show stash@{0} --stat
# → Estadísticas de cambios

# Ver solo staging de stash (avanzado)
git show stash@{0}^2
# → Cada stash tiene 2-3 commits internos
# → ^1 = commit base
# → ^2 = staging
# → ^3 = working (si existe)
```

**Stash avanzado:**

```bash
# Stash sin resetear staging
git stash --keep-index
# → Stashea working directory
# → Mantiene staging intacto
# → Útil para probar commit antes de hacerlo

# Crear stash sin modificar working
git stash create
# → Retorna hash del stash
# → NO limpia working directory
# → Útil para scripts

# Guardar stash con hash específico
hash=$(git stash create)
git stash store -m "My stash" $hash
# → Control manual de stashes
```

**Mejores prácticas:**

```bash
✓ Usa mensajes descriptivos con -m
✓ Limpia stashes viejos regularmente
✓ Usa stash para cambios temporales, no largo plazo
✓ Prefiere stash pop sobre apply (limpia automáticamente)
✓ Usa git stash -u si añadiste archivos nuevos
✓ Verifica con git stash show antes de aplicar

✗ No uses stash como sistema de backup
✗ No acumules decenas de stashes
✗ No stashees y olvides (revisa git stash list)
✗ No uses stash para changes que deberían ser commits
✗ Evita stash clear sin revisar lista
```

---

#### 9.3.9 git log - Explorando la Historia

**Funcionamiento interno:**

```
git log recorre el grafo de commits

Internamente:
1. git rev-parse HEAD
   → Obtiene commit actual
   
2. Lee commit object
   → Extrae parent hash(es)
   
3. Sigue cadena de parents recursivamente:
   HEAD → C → B → A
   
4. Para cada commit:
   → Lee commit object
   → Extrae metadata
   → Formatea output según opciones
   
5. (con --all) Recorre todas las referencias
   → refs/heads/*
   → refs/remotes/*
   → refs/tags/*
```

**Uso práctico y opciones:**

```bash
# 1. Log básico
git log
# → Lista commits, más reciente primero
# → Muestra: hash, author, date, message

# 2. Log compacto (UN LINER)
git log --oneline
# → Una línea por commit
# → Hash corto + mensaje
# → Perfecto para overview rápido

# 3. Log con grafo
git log --graph
# → Muestra ramas visualmente
# → Útil para ver merges

git log --oneline --graph --all
# → Grafo completo de todo el repo
# → Comando MUY ÚTIL

# 4. Log con decoraciones
git log --oneline --decorate
# → Muestra referencias (ramas, tags)
# → Por defecto en Git moderno

# 5. Log de archivo específico
git log -- archivo.txt
# → Solo commits que modificaron ese archivo

git log --follow -- archivo.txt
# → Sigue renombres del archivo

# 6. Log con diff
git log -p
# → Muestra cambios completos en cada commit
# → Útil para revisar qué se cambió

git log -p -2
# → Solo últimos 2 commits con diff

# 7. Log con stats
git log --stat
# → Muestra archivos modificados y cantidad
# → Más ligero que -p

# 8. Log con formato personalizado
git log --pretty=format:"%h - %an, %ar : %s"
# abc123 - John Doe, 2 days ago : Add feature
# → %h = hash corto
# → %an = author name
# → %ar = author date, relative
# → %s = subject (mensaje)

# Formatos útiles:
git log --pretty=format:"%C(yellow)%h%C(reset) %s %C(green)(%cr)%C(reset) %C(blue)<%an>%C(reset)"
# → Coloreado y formateado

# 9. Log con búsqueda
git log --grep="fix"
# → Commits con "fix" en mensaje

git log -S"función_específica"
# → Commits que añadieron/eliminaron esa string en código (pickaxe)

git log -G"regex_pattern"
# → Commits que modificaron líneas matching regex

# 10. Log de rango
git log main..feature-x
# → Commits en feature-x que NO están en main

git log feature-x..main
# → Commits en main que NO están en feature-x

git log main...feature-x
# → Commits que difieren entre ambas ramas

# 11. Log por autor
git log --author="John"
# → Commits de autor con "John" en nombre

# 12. Log por fecha
git log --since="2 weeks ago"
git log --after="2024-01-01"
git log --before="2024-12-31"
git log --since="1 month ago" --until="1 week ago"

# 13. Log limitado
git log -n 5
# → Solo 5 commits más recientes

git log --max-count=10
# → Mismo efecto

# 14. Log de merges
git log --merges
# → Solo merge commits

git log --no-merges
# → Excluye merge commits
# → Útil para ver trabajo "real"

# 15. Log de rama específica
git log main
# → Historia de main

git log origin/main
# → Historia de main remoto

# 16. Log con referencias
git log --all
# → Todos los commits de todas las ramas

git log --branches
# → Solo ramas locales

git log --remotes
# → Solo ramas remotas

git log --tags
# → Solo tags

# 17. Log invertido (más viejo primero)
git log --reverse
# → Útil para ver "evolución"

# 18. Log gráfico avanzado
git log --graph --abbrev-commit --decorate --format=format:'%C(bold blue)%h%C(reset) - %C(bold green)(%ar)%C(reset) %C(white)%s%C(reset) %C(dim white)- %an%C(reset)%C(bold yellow)%d%C(reset)' --all
# → Formato profesional y coloreado

# 19. Ver commit específico
git show abc123
# → Commit completo: mensaje + diff

git show HEAD
# → Commit actual

git show HEAD~3
# → 3 commits atrás

# 20. Log con paths específicos
git log -- src/ test/
# → Solo commits que modificaron esos directorios
```

**Casos de uso reales:**

```bash
# Caso 1: Ver qué cambió en rama feature
git log main..feature-x --oneline
# → Lista commits únicos de feature-x
# → Útil antes de merge/PR

# Caso 2: Buscar cuándo se introdujo bug
git log -S"bug_function" -p
# → Commits que mencionan la función
# → Con diff completo

# Caso 3: Ver trabajo de persona específica
git log --author="John" --since="1 month ago" --oneline
# → Commits de John en último mes

# Caso 4: Revisar cambios antes de push
git log origin/main..HEAD --oneline
# → Commits locales no pusheados

# Caso 5: Generar changelog
git log --oneline --no-merges v1.0.0..v2.0.0
# → Cambios entre versiones
# → Sin merge commits

# Caso 6: Encontrar commit que modificó archivo
git log -p --all -- path/to/deleted/file
# → Encuentra cuándo se modificó/eliminó
# → Con contenido completo

# Caso 7: Ver merges recientes
git log --merges --oneline -10
# → Últimos 10 merges
# → Útil para revisar integraciones

# Caso 8: Buscar por palabra en mensaje
git log --grep="TICKET-123" --oneline
# → Commits relacionados con ticket

# Caso 9: Ver commits que modificaron rango de líneas
git log -L 15,30:archivo.txt
# → Commits que modificaron líneas 15-30 de archivo
# → SÚPER ÚTIL para auditar cambios específicos

# Caso 10: Timeline completo del proyecto
git log --all --graph --oneline --decorate --simplify-by-decoration
# → Solo muestra commits con refs (branches, tags)
# → Vista de hitos
```

**Alias útiles (añadir a ~/.gitconfig):**

```bash
[alias]
    # Log gráfico bonito
    lg = log --graph --abbrev-commit --decorate --format=format:'%C(bold blue)%h%C(reset) - %C(bold green)(%ar)%C(reset) %C(white)%s%C(reset) %C(dim white)- %an%C(reset)%C(bold yellow)%d%C(reset)' --all
    
    # Log compacto
    ls = log --oneline --decorate
    
    # Log con stats
    ll = log --stat --abbrev-commit
    
    # Ver últimos N commits
    last = log -1 HEAD --stat
    
    # Ver commits no pusheados
    unpushed = log origin/main..HEAD --oneline
    
    # Ver qué viene en pull
    incoming = log ..@{upstream} --oneline
    
    # Log con búsqueda
    find = log --all --oneline --graph --decorate -S

# Uso:
git lg            # Log bonito
git last          # Último commit
git unpushed      # Qué voy a pushear
```

**Formato personalizado completo:**

```bash
# Crear archivo ~/.gitconfig con:
[format]
    pretty = format:%C(yellow)%h%Creset -%C(red)%d%Creset %s %C(green)(%ar) %C(bold blue)<%an>%Creset

# Opciones de formato:
%H  - Commit hash completo
%h  - Commit hash corto
%T  - Tree hash
%t  - Tree hash corto
%P  - Parent hashes
%p  - Parent hashes cortos
%an - Author name
%ae - Author email
%ad - Author date
%ar - Author date, relative
%cn - Committer name
%ce - Committer email
%cd - Committer date
%cr - Committer date, relative
%s  - Subject (mensaje)
%b  - Body (descripción)
%d  - Ref names (branches, tags)

# Colores:
%C(red)    - Rojo
%C(green)  - Verde
%C(blue)   - Azul
%C(yellow) - Amarillo
%C(bold)   - Negrita
%C(reset)  - Reset color
```

**Git log avanzado:**

```bash
# Ver commits que tocan función específica
git log -L :nombre_funcion:archivo.py
# → Historia completa de esa función
# → Sigue renombres

# Ver commits que no están en ninguna rama
git log --all --not --remotes
# → Commits locales no pusheados en ninguna rama

# Ver estadísticas generales
git shortlog
# → Agrupa commits por autor

git shortlog -sn
# → Cuenta de commits por autor, ordenado

# Ver velocidad de commits
git log --since="6 months ago" --format='%ad' --date=short | uniq -c
# → Commits por día

# Ver archivos más modificados
git log --pretty=format: --name-only | sort | uniq -c | sort -rg | head -10
# → Top 10 archivos más cambiados

# Buscar commit que introdujo string
git log -S"search_term" --source --all
# → En qué rama se introdujo
```

**Mejores prácticas:**

```bash
✓ Usa --oneline para overview rápido
✓ Usa --graph para entender merges
✓ Usa --all para ver TODO el repositorio
✓ Combina opciones: git log --oneline --graph --all
✓ Crea alias para comandos frecuentes
✓ Usa --since/--until para limitar búsquedas
✓ Usa -- path para búsquedas específicas

✗ No corras git log sin límites en repos gigantes
✗ No uses -p en all (demasiado output)
✗ No olvides --follow para archivos renombrados
✗ Evita formatos complejos sin alias
```

---

#### 9.3.10 git status - Inspeccionando el Estado

**Funcionamiento interno:**

```
git status compara tres áreas:

Internamente ejecuta:
1. git diff --name-status HEAD
   → Compara working directory con HEAD
   → Identifica archivos modificados
   
2. git diff --cached --name-status
   → Compara staging con HEAD
   → Identifica archivos stageados
   
3. Lee .git/index
   → Identifica archivos untracked (no en index)
   → Identifica archivos deleted
   
4. Compara con refs/remotes/origin/<branch>
   → Muestra si estás ahead/behind del remoto
   
5. Verifica estado de merge/rebase/cherry-pick
   → Lee .git/MERGE_HEAD, .git/REBASE_HEAD, etc.
   → Muestra si hay operación en progreso

Resultado:
→ Informe completo del estado actual
→ Qué cambió, qué está stageado, qué falta
→ Relación con remoto
```

**Uso práctico y opciones:**

```bash
# 1. Status normal
git status
# → Muestra todo: staged, modified, untracked
# → Verbose, con instrucciones de ayuda

# 2. Status corto (MUY ÚTIL)
git status -s
# o: git status --short
# → Formato compacto: ?? archivo-nuevo.txt
#                      M  archivo-modificado.txt
#                      A  archivo-añadido.txt

# Códigos en formato corto:
?? - Untracked (no rastreado)
A  - Added (añadido al staging)
M  - Modified (modificado)
D  - Deleted (eliminado)
R  - Renamed (renombrado)
C  - Copied (copiado)
U  - Updated but unmerged (conflicto)

# Primera columna = staging area
# Segunda columna = working directory

# Ejemplos:
 M archivo.txt    → Modificado en working, no stageado
M  archivo.txt    → Modificado y stageado
MM archivo.txt    → Stageado, luego modificado otra vez
?? nuevo.txt      → Untracked

# 3. Status con branch info
git status -b
# o: git status --branch
# → Muestra info de tracking branch
# → Ahead/behind con remoto

# 4. Ver archivos ignorados
git status --ignored
# → Muestra también archivos en .gitignore

# 5. Status sin hints (limpio)
git status --short --branch
# → Compacto pero con info de branch

# 6. Ver solo archivos untracked
git status --short | grep '^??'

# 7. Ver solo archivos modificados
git status --short | grep '^ M'

# 8. Status en formato porcelain (para scripts)
git status --porcelain
# → Formato estable, no cambia entre versiones
# → Ideal para parsear en scripts

git status --porcelain=v2
# → Formato mejorado con más info

# 9. Status de submodules
git status --submodule
# → Incluye estado de submódulos
```

**Interpretación del output:**

```bash
# OUTPUT NORMAL:
On branch main
Your branch is ahead of 'origin/main' by 2 commits.
  (use "git push" to publish your local commits)

Changes to be committed:
  (use "git restore --staged <file>..." to unstage)
        modified:   file1.txt
        new file:   file2.txt

Changes not staged for commit:
  (use "git add <file>..." to update what will be committed)
  (use "git restore <file>..." to discard changes)
        modified:   file3.txt

Untracked files:
  (use "git add <file>..." to include in what will be committed)
        file4.txt

# INTERPRETACIÓN:
1. "On branch main" → HEAD → refs/heads/main
2. "ahead by 2 commits" → Tienes 2 commits no pusheados
3. "Changes to be committed" → Staging area (index)
4. "Changes not staged" → Working directory modificado
5. "Untracked files" → Archivos no en Git aún
```

**Casos de uso reales:**

```bash
# Caso 1: Verificar antes de commit
git status
# → Revisa qué vas a commitear
git add .
git status
# → Confirma que todo está stageado
git commit -m "mensaje"

# Caso 2: Ver qué cambió después de trabajar
git status -s
# → Vista rápida de cambios

# Caso 3: Identificar archivos olvidados
git status --ignored
# → ¿Hay archivos que deberían estar en .gitignore?

# Caso 4: Ver relación con remoto
git status -sb
# → ¿Necesito pull? ¿Push?

# Caso 5: En scripts (verificar si hay cambios)
if [[ -n $(git status -s) ]]; then
  echo "Hay cambios sin commitear"
else
  echo "Working directory limpio"
fi

# Caso 6: Durante merge con conflictos
git merge feature
# ... conflictos ...
git status
# → Muestra archivos en conflicto claramente
# → Con instrucciones de cómo resolver
```

**Status durante operaciones especiales:**

```bash
# Durante MERGE:
git status
# On branch main
# You have unmerged paths.
#   (fix conflicts and run "git commit")
# 
# Unmerged paths:
#   (use "git add <file>..." to mark resolution)
#         both modified:   file.txt

# Durante REBASE:
git status
# interactive rebase in progress; onto abc123
# Last command done (1 command done):
#    pick def456 Commit message
# Next commands to do (2 remaining commands):
#    pick ghi789 Another commit
# You are currently rebasing branch 'feature' on 'abc123'.

# Durante CHERRY-PICK:
git status
# On branch main
# You are currently cherry-picking commit abc123.
#   (fix conflicts and run "git cherry-pick --continue")
```

**Configuración útil:**

```bash
# Siempre mostrar formato corto
git config --global status.short true

# Color personalizado
git config --global color.status.changed "yellow"
git config --global color.status.untracked "red"

# Mostrar info de submodules por defecto
git config --global status.submoduleSummary true

# Ver archivos untracked en directorios untracked
git config --global status.showUntrackedFiles all
```

**Mejores prácticas:**

```bash
✓ Ejecuta git status antes de commit (siempre)
✓ Usa git status -s para overview rápido
✓ Revisa con git status después de pull/merge
✓ Usa en scripts con --porcelain (estable)
✓ Verifica branch tracking con -b
✓ Consulta antes de cambiar de rama

✗ No ignores el output de status
✗ No commitees sin revisar git status primero
✗ No dependas solo de IDE (confirma con git status)
✗ Evita asumir estado sin verificar
```

---

#### 9.3.11 git diff - Comparando Cambios

**Funcionamiento interno:**

```
git diff compara contenido entre:
- Working directory
- Staging area (index)
- Commits
- Ramas

Internamente:
1. Lee contenido de las dos fuentes a comparar
2. Ejecuta algoritmo de diff (Myers, patience, histogram)
3. Genera "hunks" (bloques de diferencias)
4. Formatea output en formato unificado

Formato de output:
diff --git a/file.txt b/file.txt
index abc123..def456 100644
--- a/file.txt
+++ b/file.txt
@@ -10,7 +10,8 @@
 línea sin cambios
-línea eliminada
+línea añadida
+otra línea añadida
 línea sin cambios

Leyenda:
- (rojo) = eliminado
+ (verde) = añadido
  (blanco) = sin cambios
@@ -10,7 +10,8 @@ = ubicación (línea 10, 7 líneas antes; línea 10, 8 líneas después)
```

**Uso práctico y opciones:**

```bash
# 1. Diff de working directory (NO stageado)
git diff
# → Muestra cambios en working vs staging
# → Lo que perderías con git restore

# 2. Diff de staging (lo que vas a commitear)
git diff --staged
# o: git diff --cached
# → Muestra cambios stageados vs último commit
# → Lo que irá en el próximo commit

# 3. Diff completo (staged + unstaged)
git diff HEAD
# → Working directory vs último commit
# → Todo lo que cambió desde último commit

# 4. Diff entre commits
git diff abc123 def456
# → Diferencias entre dos commits

git diff HEAD~5 HEAD
# → Cambios en últimos 5 commits

git diff HEAD~5
# → Desde hace 5 commits hasta ahora

# 5. Diff entre ramas
git diff main feature-x
# → Diferencias entre ramas

git diff main...feature-x
# → Cambios en feature-x desde punto de divergencia
# → Útil para ver qué aporta la rama

# 6. Diff de archivo específico
git diff archivo.txt
git diff HEAD~3 -- archivo.txt
git diff main feature -- archivo.txt

# 7. Diff con stats (resumen)
git diff --stat
# → Lista archivos modificados con +/-
# → Sin mostrar líneas completas

# 8. Diff compacto (solo nombres)
git diff --name-only
# → Solo lista archivos modificados

git diff --name-status
# → Lista con estado (M, A, D, R)

# 9. Diff con contexto ampliado
git diff -U10
# → 10 líneas de contexto (default es 3)
# → Útil para ver más alrededor del cambio

# 10. Diff por palabras (no líneas)
git diff --word-diff
# → Muestra diferencias por palabra
# → Útil para textos, documentación

git diff --word-diff=color
# → Colores en palabras cambiadas

# 11. Diff ignorando espacios
git diff -w
# o: git diff --ignore-all-space
# → Ignora cambios en espacios/tabs
# → Útil tras reformateo

git diff --ignore-space-change
# → Ignora cambios en cantidad de espacios

# 12. Diff con algoritmo específico
git diff --patience
# → Algoritmo patience (mejor para código)

git diff --histogram
# → Algoritmo histogram (más rápido)

# 13. Diff coloreado por palabras
git diff --color-words
# → Colorea palabras específicas cambiadas
# → Muy legible

# 14. Diff de binarios
git diff --binary
# → Incluye archivos binarios

git diff --no-binary
# → Excluye binarios (más rápido)

# 15. Diff con líneas movidas
git diff --color-moved
# → Detecta líneas movidas (no eliminadas/añadidas)
# → Útil en refactors

# 16. Ver diff de merge
git diff main...feature-x
# → Cambios que merge traería

# 17. Diff de commit específico
git show abc123
# → Diff del commit (equivale a git diff abc123^ abc123)

# 18. Diff con filtro de archivos
git diff -- '*.py'
# → Solo archivos Python

git diff -- src/
# → Solo directorio src/

# 19. Diff inverso (útil para revert)
git diff -R
# → Invierte + y -
```

**Casos de uso reales:**

```bash
# Caso 1: Revisar antes de commit
git diff
# → ¿Qué cambié?
git add .
git diff --staged
# → ¿Qué voy a commitear?
git commit -m "mensaje"

# Caso 2: Comparar con producción
git diff production main
# → Qué irá en próximo deploy

# Caso 3: Ver qué trae un PR
git fetch origin pull/123/head:pr-123
git diff main...pr-123
# → Cambios del PR

# Caso 4: Identificar conflictos antes de merge
git diff main...feature-x
# → Si hay muchos cambios en mismos archivos, habrá conflictos

# Caso 5: Code review desde terminal
git diff --stat main...feature-x
# → Resumen de cambios
git diff main...feature-x -- src/
# → Detalle de directorio específico

# Caso 6: Buscar cuándo cambió algo
git log -p -S"función_importante"
# → Commits con diff donde aparece la función

# Caso 7: Generar patch
git diff > cambios.patch
# → Exporta diff a archivo
# Aplicar después:
git apply cambios.patch

# Caso 8: Comparar configuraciones
git diff production:config.yml staging:config.yml
# → Compara archivos entre ramas

# Caso 9: Ver solo funciones cambiadas
git diff -U0 --function-context
# → Muestra función completa donde hubo cambio

# Caso 10: Diff ignorando archivos específicos
git diff -- . ':(exclude)package-lock.json'
# → Todo excepto package-lock.json
```

**Formato de diff explicado:**

```bash
diff --git a/archivo.txt b/archivo.txt
# → Comparando versiones a y b de archivo.txt

index abc123..def456 100644
# → Hashes de blobs, modo de archivo

--- a/archivo.txt
+++ b/archivo.txt
# → - es versión antigua, + es nueva

@@ -10,7 +10,8 @@ función_contexto
# → Hunk header
# → -10,7 = línea 10, 7 líneas en versión vieja
# → +10,8 = línea 10, 8 líneas en versión nueva
# → función_contexto = contexto (cabecera función)

 línea sin cambios
-línea eliminada
+línea añadida
+otra línea nueva
 línea sin cambios
# → Espacio = sin cambios
# → - (rojo) = eliminado
# → + (verde) = añadido
```

**Diff con herramientas gráficas:**

```bash
# Configurar difftool
git config --global diff.tool meld
# o: vimdiff, kdiff3, vscode, etc.

# Usar difftool
git difftool
# → Abre herramienta gráfica

git difftool main feature-x
# → Compara ramas gráficamente

# Difftool sin preguntar
git difftool --no-prompt
```

**Alias útiles:**

```bash
# En ~/.gitconfig:
[alias]
    d = diff
    ds = diff --staged
    dw = diff --word-diff=color
    dst = diff --stat
    dno = diff --name-only
    
# Uso:
git d        # diff rápido
git ds       # diff staged
git dw       # diff por palabras
```

**Mejores prácticas:**

```bash
✓ Usa git diff antes de add (ver qué añades)
✓ Usa git diff --staged antes de commit (confirmar)
✓ Usa git diff --stat para overview rápido
✓ Usa --patience para diffs más legibles
✓ Usa --word-diff para textos/documentación
✓ Ignora espacios con -w tras reformateo
✓ Usa ... (tres puntos) para comparar ramas

✗ No ignores el diff antes de commitear
✗ No uses diff sin contexto suficiente
✗ No confundas git diff con git diff --staged
✗ Evita diff de archivos binarios grandes
✗ No asumas que diff vacío = sin cambios (puede estar todo stageado)
```

---

#### 9.3.12 git clone - Copiando Repositorios

**Funcionamiento interno:**

```
git clone crea copia completa de repositorio remoto

Internamente ejecuta:
1. Crea directorio destino
2. git init → inicializa .git/
3. git remote add origin <url> → configura remoto
4. git fetch origin → descarga objetos y refs
5. git checkout <default-branch> → crea working directory
6. Configura tracking branches automáticamente

Resultado:
→ Repositorio completo local
→ Toda la historia descargada
→ Working directory del branch default
→ Remoto "origin" configurado
```

**Uso práctico y opciones:**

```bash
# 1. Clone básico
git clone https://github.com/user/repo.git
# → Crea directorio "repo/"
# → Descarga todo
# → Checkout de rama default (main/master)

# 2. Clone con nombre personalizado
git clone https://github.com/user/repo.git mi-proyecto
# → Crea directorio "mi-proyecto/"

# 3. Clone shallow (solo commits recientes)
git clone --depth 1 https://github.com/user/repo.git
# → Solo último commit de cada rama
# → Mucho más rápido
# → Útil para CI/CD
# → Historia limitada

git clone --depth 10 <url>
# → Últimos 10 commits

# 4. Clone de rama específica
git clone -b develop https://github.com/user/repo.git
# → Solo descarga rama develop
# → Checkout automático de develop

git clone --single-branch -b feature-x <url>
# → SOLO rama feature-x, no otras

# 5. Clone bare (sin working directory)
git clone --bare https://github.com/user/repo.git repo.git
# → Solo .git/ (repositorio puro)
# → Sin working directory
# → Útil para mirrors, servidores

# 6. Clone mirror (réplica exacta)
git clone --mirror https://github.com/user/repo.git repo.git
# → Réplica completa incluyendo refs
# → Para backups

# 7. Clone con submodules
git clone --recursive https://github.com/user/repo.git
# → Clona repo + sus submódulos
# → Inicializa submódulos automáticamente

git clone --recurse-submodules <url>
# → Igual que --recursive

# 8. Clone sin checkout
git clone --no-checkout https://github.com/user/repo.git
# o: git clone -n <url>
# → Descarga repo pero no crea working directory
# → Útil para inspeccionar sin archivos

# 9. Clone parcial (partial clone)
git clone --filter=blob:none https://github.com/user/repo.git
# → Descarga commits y trees, NO blobs
# → Blobs se descargan on-demand
# → Mucho más rápido en repos grandes

git clone --filter=tree:0 <url>
# → Solo commits, ni trees ni blobs
# → Mínimo posible

# 10. Clone con configuración específica
git clone -c http.sslVerify=false https://... 
# → Clona con config temporal
# → Útil para certificados self-signed

# 11. Clone de directorio local
git clone /ruta/al/repo/.git mi-copia
# → Clona repo local
# → Más rápido (hardlinks si es posible)

git clone --local /ruta/al/repo mi-copia
# → Fuerza usar filesystem, no protocolo Git

# 12. Clone con progreso verbose
git clone --verbose https://github.com/user/repo.git
# o: git clone -v <url>
# → Muestra progreso detallado

# 13. Clone sin tags
git clone --no-tags https://github.com/user/repo.git
# → Omite tags
# → Más rápido si hay muchos tags

# 14. Clone con template
git clone --template=/ruta/template https://...
# → Usa template custom para .git/
```

**Casos de uso reales:**

```bash
# Caso 1: Desarrollo normal
git clone https://github.com/company/project.git
cd project
git checkout -b mi-feature
# → Clona, entra, crea rama de trabajo

# Caso 2: CI/CD (optimizado)
git clone --depth 1 --single-branch --branch main https://...
# → Solo último commit de main
# → Máxima velocidad

# Caso 3: Contribuir a open source
git clone https://github.com/original/repo.git
cd repo
git remote add upstream https://github.com/original/repo.git
git remote set-url origin https://github.com/tu-fork/repo.git
# → Clona, configura origin y upstream

# Caso 4: Backup de repositorio
git clone --mirror https://github.com/user/repo.git backup.git
# → Backup completo

# Caso 5: Repo con submódulos
git clone --recursive https://github.com/user/repo.git
# → Todo en un comando

# O si olvidaste --recursive:
git clone https://github.com/user/repo.git
git submodule update --init --recursive

# Caso 6: Repos gigantes (Linux kernel, Chromium)
git clone --depth 1 --filter=blob:none https://...
# → Solo estructura, blobs on-demand
# → De GBs a MBs

# Caso 7: Inspección rápida
git clone --depth 1 --no-checkout https://... temp-repo
cd temp-repo
git log --oneline
# → Ver historia sin descargar archivos

# Caso 8: Múltiples versiones locales
git clone https://... proyecto-stable
git clone -b develop https://... proyecto-dev
# → Dos versiones en paralelo
```

**Protocolos de clone:**

```bash
# HTTPS (recomendado)
git clone https://github.com/user/repo.git
# → Funciona en cualquier red
# → Requiere autenticación (token/password)
# → Más lento que SSH

# SSH
git clone git@github.com:user/repo.git
# → Más rápido
# → Requiere SSH key configurada
# → Firewall-friendly

# Git protocol (raro hoy)
git clone git://github.com/user/repo.git
# → Sin autenticación
# → Obsoleto, inseguro

# File protocol (local)
git clone file:///ruta/completa/repo.git
git clone /ruta/completa/repo.git
# → Copia local
```

**Troubleshooting:**

```bash
# Problema 1: Clone muy lento
# Solución: Usa shallow clone
git clone --depth 1 <url>

# Problema 2: Repo muy grande
# Solución: Partial clone
git clone --filter=blob:none <url>

# Problema 3: Certificado SSL inválido
git -c http.sslVerify=false clone <url>
# O permanente:
git config --global http.sslVerify false

# Problema 4: Timeout en red lenta
git -c http.postBuffer=524288000 clone <url>
# → Buffer más grande

# Problema 5: Submódulos no clonados
git submodule update --init --recursive

# Problema 6: Clone interrumpido
# Simplemente re-ejecuta git clone, sobrescribe
# O:
cd repo-parcial
git fetch origin
git checkout main
```

**Después del clone:**

```bash
# Ver configuración
git remote -v
# origin https://github.com/user/repo.git (fetch)
# origin https://github.com/user/repo.git (push)

# Ver ramas
git branch -a
# * main
#   remotes/origin/HEAD -> origin/main
#   remotes/origin/main
#   remotes/origin/develop

# Ver historia
git log --oneline -10

# Ver tags
git tag

# Actualizar
git pull origin main
```

**Mejores prácticas:**

```bash
✓ Usa HTTPS para proyectos públicos (universal)
✓ Usa SSH para proyectos privados (más rápido)
✓ Usa --depth 1 en CI/CD (velocidad)
✓ Usa --recursive para repos con submódulos
✓ Usa --filter=blob:none para repos gigantes
✓ Verifica con git remote -v tras clonar

✗ No clones con --depth si necesitas historia completa
✗ No uses git:// protocol (inseguro)
✗ No desactives SSL verification sin razón
✗ No clones repo dentro de otro repo
✗ Evita clone de repos con credenciales en URL
```

---

#### 9.3.13 git remote - Gestionando Repositorios Remotos

**Funcionamiento interno:**

```
git remote gestiona referencias a repositorios remotos

Remotos se guardan en:
→ .git/config

[remote "origin"]
    url = https://github.com/user/repo.git
    fetch = +refs/heads/*:refs/remotes/origin/*

Cada remoto tiene:
→ Nombre (origin, upstream, etc.)
→ URL (donde está el repo)
→ Fetch refspec (qué ramas descargar)
→ Push refspec (opcional, qué ramas subir)

git remote lee/escribe esta configuración
```

**Uso práctico y opciones:**

```bash
# 1. Listar remotos
git remote
# origin

git remote -v
# origin  https://github.com/user/repo.git (fetch)
# origin  https://github.com/user/repo.git (push)

# 2. Añadir remoto
git remote add upstream https://github.com/original/repo.git
# → Añade nuevo remoto llamado "upstream"

# 3. Ver detalles de remoto
git remote show origin
# * remote origin
#   Fetch URL: https://github.com/user/repo.git
#   Push  URL: https://github.com/user/repo.git
#   HEAD branch: main
#   Remote branches:
#     main    tracked
#     develop tracked
#   Local branch configured for 'git pull':
#     main merges with remote main
#   Local ref configured for 'git push':
#     main pushes to main (up to date)

# 4. Cambiar URL de remoto
git remote set-url origin https://nuevo-url.git
# → Cambia URL de origin

git remote set-url origin git@github.com:user/repo.git
# → Cambiar de HTTPS a SSH

# 5. Renombrar remoto
git remote rename origin nuevo-nombre
# → Renombra remoto

# 6. Eliminar remoto
git remote remove upstream
# o: git remote rm upstream
# → Elimina remoto

# 7. Cambiar URL de push (diferente de fetch)
git remote set-url --push origin https://otro-url.git
# → Push va a URL diferente
# → Útil para forks

# 8. Añadir múltiples push URLs
git remote set-url --add --push origin https://github.com/user/repo.git
git remote set-url --add --push origin https://gitlab.com/user/repo.git
# → Push sincroniza a ambos
# → Mirror automático

# 9. Ver URLs específicas
git remote get-url origin
# → Solo URL de fetch

git remote get-url --push origin
# → URL de push

git remote get-url --all origin
# → Todas las URLs

# 10. Actualizar referencias remotas (prune)
git remote prune origin
# → Elimina referencias a ramas remotas ya borradas

git remote prune origin --dry-run
# → Ver qué se eliminaría sin hacerlo

# 11. Update (fetch all remotes)
git remote update
# → Fetch de TODOS los remotos

git remote update --prune
# → Fetch + limpieza
```

**Casos de uso reales:**

```bash
# Caso 1: Fork workflow (contribuir a open source)
git clone https://github.com/tu-fork/proyecto.git
cd proyecto
git remote add upstream https://github.com/original/proyecto.git
git remote -v
# origin    https://github.com/tu-fork/proyecto.git (fetch)
# origin    https://github.com/tu-fork/proyecto.git (push)
# upstream  https://github.com/original/proyecto.git (fetch)
# upstream  https://github.com/original/proyecto.git (push)

# Workflow:
git fetch upstream
git merge upstream/main
git push origin main

# Caso 2: Cambiar de HTTPS a SSH
git remote -v
# origin  https://github.com/user/repo.git
git remote set-url origin git@github.com:user/repo.git
git remote -v
# origin  git@github.com:user/repo.git

# Caso 3: Mirror a múltiples servicios
git remote set-url --add --push origin https://github.com/user/repo.git
git remote set-url --add --push origin https://gitlab.com/user/repo.git
git remote set-url --add --push origin https://bitbucket.org/user/repo.git
git push origin main
# → Push sincronizado a 3 servicios

# Caso 4: Múltiples colaboradores
git remote add alice https://github.com/alice/proyecto.git
git remote add bob https://github.com/bob/proyecto.git
git fetch alice
git fetch bob
git log --oneline alice/feature-x
# → Revisar trabajo de otros

# Caso 5: Limpiar ramas remotas eliminadas
git branch -r
# origin/main
# origin/feature-old (ya borrada en GitHub)
git remote prune origin
git branch -r
# origin/main
# → feature-old eliminada localmente

# Caso 6: Repositorio movido
# Repo cambió de GitHub a GitLab
git remote set-url origin https://gitlab.com/user/repo.git
# → Actualiza URL

# Caso 7: Trabajar con staging y production
git remote add staging https://git.staging.com/repo.git
git remote add production https://git.production.com/repo.git
git push staging develop
git push production main
# → Deploy a diferentes ambientes

# Caso 8: Backup automático
git remote add backup https://backup-server.com/repo.git
# En .git/hooks/post-commit:
#!/bin/bash
git push backup main --quiet &
# → Push automático a backup
```

**Configuración avanzada:**

```bash
# Ver configuración en .git/config
cat .git/config

[remote "origin"]
    url = https://github.com/user/repo.git
    fetch = +refs/heads/*:refs/remotes/origin/*
    
# Editar manualmente (alternativa a git remote)
git config remote.origin.url https://nuevo-url.git

# Fetch refspec personalizado
git config remote.origin.fetch "+refs/heads/main:refs/remotes/origin/main"
# → Solo fetch de rama main

# Push refspec personalizado
git config remote.origin.push "refs/heads/main:refs/heads/production"
# → git push origin: pushea main a production

# URL con credenciales (NO RECOMENDADO)
git remote set-url origin https://user:token@github.com/user/repo.git
# Mejor: usa credential helper
```

**Remote tracking branches:**

```bash
# Ver branches y sus tracking
git branch -vv
# * main    abc123 [origin/main] Commit message
#   feature def456 [origin/feature: ahead 2] Another commit

# Configurar tracking
git branch --set-upstream-to=origin/main main
# o al crear rama:
git checkout -b feature origin/feature

# Ver tracking status
git status -sb
# ## main...origin/main [ahead 2, behind 1]
```

**Troubleshooting:**

```bash
# Problema 1: Remote existe pero no se puede fetch
git remote show origin
# → Muestra error específico

# Problema 2: URL incorrecta
git remote -v
# → Verifica URL
git remote set-url origin <url-correcta>

# Problema 3: Múltiples origins por error
git remote remove origin
git remote add origin <url-correcta>

# Problema 4: Referencias obsoletas
git remote prune origin
# → Limpia

# Problema 5: Push rechazado (URL read-only)
git remote set-url --push origin <url-write>
```

**Mejores prácticas:**

```bash
✓ Usa nombres descriptivos (origin, upstream, backup)
✓ Usa SSH para repos privados (más seguro)
✓ Configura upstream para forks
✓ Limpia con prune regularmente
✓ Verifica con git remote -v tras cambios
✓ Usa set-url en vez de remove/add

✗ No pongas credenciales en URL
✗ No uses nombres confusos (origin1, origin2)
✗ No borres origin sin reemplazarlo
✗ Evita múltiples push URLs sin razón clara
✗ No uses URL de otros sin permiso
```

---

#### 9.3.14 git fetch - Descargando Cambios

**Funcionamiento interno:**

```
git fetch descarga objetos y refs del remoto SIN modificar working directory

Internamente:
1. Conecta con remoto (origin)
2. Compara refs locales vs remotas
3. Identifica objetos faltantes
4. Descarga objetos (commits, trees, blobs)
5. Actualiza refs/remotes/origin/*
6. NO modifica ramas locales
7. NO modifica working directory
8. NO hace merge

Resultado:
→ objects/ tiene nuevos objetos
→ refs/remotes/origin/* actualizadas
→ Ramas locales SIN cambios
→ Working directory intacto
```

**Uso práctico y opciones:**

```bash
# 1. Fetch básico (remoto default)
git fetch
# → Descarga de origin
# → Todas las ramas

git fetch origin
# → Explícito: desde origin

# 2. Fetch de rama específica
git fetch origin main
# → Solo rama main

git fetch origin feature-x:refs/remotes/origin/feature-x
# → Refspec explícito

# 3. Fetch de todos los remotos
git fetch --all
# → Fetch de origin, upstream, etc.

# 4. Fetch con prune (limpiar refs obsoletas)
git fetch --prune
# o: git fetch -p
# → Elimina refs de ramas ya borradas en remoto
# → Mantiene local sincronizado

# 5. Fetch de tags
git fetch --tags
# → Descarga todos los tags

git fetch --no-tags
# → Omite tags

# 6. Fetch shallow (solo commits recientes)
git fetch --depth 1
# → Solo último commit

git fetch --depth 10
# → Últimos 10 commits

# 7. Fetch deepen (agregar más historia a shallow)
git fetch --deepen=10
# → 10 commits más atrás

git fetch --unshallow
# → Convierte shallow a completo

# 8. Fetch con verbose
git fetch --verbose
# o: git fetch -v
# → Muestra progreso detallado

# 9. Fetch dry-run (ver qué se descargaría)
git fetch --dry-run
# → Simula fetch sin descargar

# 10. Fetch de PR (GitHub)
git fetch origin pull/123/head:pr-123
# → Descarga PR #123 a rama local pr-123

# 11. Fetch forzado
git fetch --force
# → Fuerza actualización de refs

# 12. Fetch con refspec personalizado
git fetch origin '+refs/heads/*:refs/remotes/origin/*'
# → Refspec explícito

# 13. Fetch parcial (partial clone)
git fetch --filter=blob:none
# → Solo commits y trees, no blobs

# 14. Fetch con progreso
git fetch --progress
# → Muestra barra de progreso
```

**Casos de uso reales:**

```bash
# Caso 1: Actualizar información sin mergear
git fetch origin
git log HEAD..origin/main --oneline
# → Ver qué hay nuevo en remoto
git merge origin/main
# → Mergear cuando estés listo

# Caso 2: Revisar cambios antes de pull
git fetch
git diff main origin/main
# → Ver diferencias
git log --oneline --graph --all
# → Ver historial gráfico
# Si te gusta:
git merge origin/main

# Caso 3: Fork workflow
git fetch upstream
git merge upstream/main
git push origin main
# → Sincronizar con upstream

# Caso 4: Ver trabajo de colaborador
git fetch origin
git checkout origin/feature-alice
# → Detached HEAD, inspección

git log origin/feature-alice --oneline
# → Ver qué hizo Alice

# Caso 5: Limpiar ramas eliminadas
git fetch --prune
git branch -r
# → Ramas remotas actualizadas

# Caso 6: Probar PR localmente
git fetch origin pull/123/head:pr-123
git checkout pr-123
# ... pruebas ...
git checkout main
git branch -D pr-123

# Caso 7: Actualizar todas las ramas
git fetch --all
git branch -a
# → Ver todas las ramas actualizadas

# Caso 8: Sincronización periódica (script)
#!/bin/bash
git fetch --all --prune --tags
echo "Repositorio sincronizado"
# → Cron job para mantener actualizado

# Caso 9: Comparar versiones
git fetch origin
git diff main origin/main -- package.json
# → ¿Cambió dependencias?

# Caso 10: Ver qué viene en pull
git fetch
git log ..@{upstream} --oneline
# → Commits que traería git pull
```

**Fetch vs Pull:**

```bash
# FETCH: Solo descarga
git fetch origin main
# → Descarga origin/main
# → main local SIN cambios
# → Puedes revisar antes de integrar

# PULL: Fetch + Merge
git pull origin main
# Equivale a:
git fetch origin main
git merge origin/main
# → Descarga Y mergea automáticamente
# → Más rápido pero menos control

# ¿Cuándo usar cada uno?
FETCH:
→ Quieres revisar cambios primero
→ Workflow más controlado
→ Integración manual
→ Inspección de ramas remotas

PULL:
→ Confías en los cambios
→ Workflow rápido
→ Integración automática
→ Sincronización simple
```

**Ver resultado de fetch:**

```bash
# Después de fetch, ver qué llegó:

# 1. Ver ramas remotas
git branch -r

# 2. Ver commits nuevos
git log HEAD..origin/main --oneline
# → Qué hay en origin/main que no tengo

# 3. Ver diferencias
git diff origin/main

# 4. Ver stats
git diff --stat origin/main

# 5. Comparar múltiples ramas
git log --oneline --graph --all

# 6. Ver tags nuevos
git tag -l

# 7. Ver reflog de remote
git log -g origin/main
# → Historia de movimientos de origin/main
```

**Configuración útil:**

```bash
# Auto-prune al fetch
git config --global fetch.prune true
# → git fetch siempre hace prune

# Mostrar progreso
git config --global fetch.showProgress true

# Parallel fetch (múltiples remotos)
git config --global fetch.parallel 4
# → Fetch en paralelo

# Refspec permanente
git config remote.origin.fetch "+refs/heads/*:refs/remotes/origin/*"

# Fetch tags automáticamente
git config remote.origin.tagOpt --tags
```

**Troubleshooting:**

```bash
# Problema 1: Fetch muy lento
# Solución: Fetch shallow
git fetch --depth 1

# Problema 2: Fetch falla por refs
git remote prune origin
git fetch

# Problema 3: Fetch no trae rama nueva
git fetch origin
git branch -r | grep nueva-rama
# Si no aparece:
git fetch origin nueva-rama

# Problema 4: Fetch sin cambios visibles
git fetch --verbose
# → Ver qué se descargó

# Problema 5: Refs desactualizadas
git fetch --force --prune

# Problema 6: Timeout en red lenta
git config --global http.postBuffer 524288000
git fetch
```

**Mejores prácticas:**

```bash
✓ Usa fetch antes de pull (revisa cambios)
✓ Usa --prune regularmente (limpia refs)
✓ Fetch frecuentemente (mantén sincronizado)
✓ Revisa con git log tras fetch
✓ Usa --dry-run para verificar sin descargar
✓ Configura fetch.prune = true globalmente

✗ No confundas fetch con pull
✗ No asumas que fetch cambia working directory
✗ No olvides mergear después de fetch
✗ Evita fetch --force sin razón
✗ No ignores output de fetch (puede haber errores)
```

---

#### 9.3.15 git pull - Descargando e Integrando

**Funcionamiento interno:**

```
git pull = git fetch + git merge (o rebase)

Internamente:
1. git fetch origin <branch>
   → Descarga objetos y actualiza origin/<branch>
   
2. git merge origin/<branch>
   → Mergea cambios remotos en rama local
   
   O si configured --rebase:
   git rebase origin/<branch>
   → Reaplica commits locales encima de remotos

Resultado:
→ Rama local actualizada con cambios remotos
→ Working directory actualizado
→ Posibles conflictos si hay divergencia
```

**Uso práctico y opciones:**

```bash
# 1. Pull básico
git pull
# → Fetch + merge de tracking branch
# → Si estás en main, pull de origin/main

git pull origin main
# → Explícito: desde origin, rama main

# 2. Pull con rebase (historia lineal)
git pull --rebase
# → Fetch + rebase en vez de merge
# → Evita merge commits
# → Historia más limpia

git pull --rebase origin main
# → Explícito con rebase

# 3. Pull con fast-forward only
git pull --ff-only
# → Solo si es fast-forward
# → Aborta si requiere merge
# → Útil para mantener historia lineal

# 4. Pull sin commit (revisar antes)
git pull --no-commit
# → Hace merge pero NO commitea
# → Puedes revisar cambios
# → git commit cuando estés listo

# 5. Pull de todos los remotos
git pull --all
# → Pull de todos los remotos configurados

# 6. Pull con estrategia de merge
git pull -X ours
# → En conflictos, prefiere versión local

git pull -X theirs
# → En conflictos, prefiere versión remota

# 7. Pull con squash
git pull --squash origin feature-x
# → Trae cambios pero NO crea merge commit
# → Cambios quedan stageados
# → Commiteas manualmente

# 8. Pull con depth (shallow)
git pull --depth 1
# → Solo último commit

# 9. Pull con prune
git pull --prune
# → Fetch con prune + merge

# 10. Pull con tags
git pull --tags
# → Incluye todos los tags

# 11. Pull sin fast-forward
git pull --no-ff
# → Siempre crea merge commit
# → Aunque sea fast-forward posible

# 12. Pull con verbose
git pull --verbose
# o: git pull -v
# → Muestra progreso detallado

# 13. Pull forzando remote
git pull --force
# → Sobrescribe local con remoto (CUIDADO)

# 14. Pull con autostash
git pull --autostash
# → Stash automático si hay cambios locales
# → Pull
# → Pop automático del stash
# → Muy conveniente

# 15. Pull de remoto y rama específicos
git pull upstream develop
# → Pull de upstream/develop
```

**Casos de uso reales:**

```bash
# Caso 1: Sincronización simple
git pull
# → Actualiza main con origin/main

# Caso 2: Pull con trabajo local
# Tienes commits locales no pusheados
git pull --rebase
# → Reaplica tus commits encima de remotos
# → Historia lineal

# Caso 3: Pull con cambios sin commitear
git stash
git pull
git stash pop
# O más simple:
git pull --autostash

# Caso 4: Pull seguro (solo fast-forward)
git pull --ff-only
# Si falla: hay divergencia, decide si merge o rebase

# Caso 5: Fork workflow
git pull upstream main
git push origin main
# → Sincroniza fork con upstream

# Caso 6: Pull para revisión
git pull --no-commit
git diff HEAD
# → Revisar cambios antes de commitear
git commit  # o git merge --abort

# Caso 7: Pull resolviendo conflictos automáticamente
git pull -X theirs
# → Acepta versión remota en conflictos
# → Útil si sabes que remoto es correcto

# Caso 8: Pull de feature branch
git checkout feature-x
git pull origin feature-x
# → Actualiza feature branch

# Caso 9: Pull desde múltiples remotos
git pull origin main
git pull upstream main
# → Integra de ambos

# Caso 10: Pull periódico (script)
#!/bin/bash
cd /ruta/al/repo
git pull --autostash --rebase --prune
# → Pull limpio y automático
```

**Pull con conflictos:**

```bash
# Si pull resulta en conflictos:
git pull
# Auto-merging file.txt
# CONFLICT (content): Merge conflict in file.txt
# Automatic merge failed; fix conflicts and then commit.

# Resolver:
# 1. Ver archivos en conflicto
git status

# 2. Editar y resolver
vim file.txt  # Resolver marcadores <<<<< >>>>> =====

# 3. Añadir resuelto
git add file.txt

# 4. Completar merge
git commit

# O abortar:
git merge --abort
```

**Pull vs Fetch + Merge:**

```bash
# PULL (rápido pero menos control):
git pull
# ✓ Un comando
# ✓ Rápido
# ✗ Menos control
# ✗ Merge automático

# FETCH + MERGE (más control):
git fetch
git log HEAD..origin/main --oneline  # Revisar
git diff origin/main  # Ver diferencias
git merge origin/main  # Mergear si OK
# ✓ Revisas antes de integrar
# ✓ Control total
# ✗ Más pasos
# ✗ Más lento

# Recomendación:
# → Usa pull para syncs rutinarios
# → Usa fetch + merge para cambios importantes
```

**Configuración útil:**

```bash
# Pull con rebase por defecto
git config --global pull.rebase true
# → git pull siempre hace rebase

# Solo fast-forward
git config --global pull.ff only
# → git pull solo si es ff posible

# Autostash por defecto
git config --global rebase.autoStash true
# → Con pull --rebase, auto-stash

# Estrategia de merge default
git config --global pull.strategy recursive
git config --global pull.strategyOption ours

# Ver configuración
git config --list | grep pull
```

**Troubleshooting:**

```bash
# Problema 1: Pull rechazado (cambios locales)
git pull
# error: Your local changes would be overwritten

# Solución:
git stash
git pull
git stash pop
# O:
git pull --autostash

# Problema 2: Conflictos en pull
git pull
# CONFLICT...

# Soluciones:
# A) Resolver manualmente
git add archivo-resuelto
git commit

# B) Abortar
git merge --abort

# C) Aceptar versión remota
git checkout --theirs archivo
git add archivo
git commit

# Problema 3: Pull de rama incorrecta
git pull origin wrong-branch
# ¡Ups!

# Solución: Undo
git reset --hard HEAD@{1}

# Problema 4: Pull muy lento
# Solución: Pull shallow
git pull --depth 1

# Problema 5: Pull sin tracking branch
git pull
# error: no tracking information

# Solución:
git branch --set-upstream-to=origin/main main
# O explícito:
git pull origin main

# Problema 6: Divergencia (commits en ambos lados)
git pull
# fatal: Not possible to fast-forward

# Soluciones:
# A) Merge (crea merge commit)
git pull --no-ff

# B) Rebase (historia lineal)
git pull --rebase

# C) Reset a remoto (pierdes locales)
git reset --hard origin/main
```

**Pull vs Rebase:**

```bash
# Escenario: Tienes commits locales, alguien pusheó a remoto

Estado:
origin/main: A ← B ← C ← D
main (local): A ← B ← C ← E ← F

# Opción 1: Pull (merge)
git pull
Resultado: A ← B ← C ← D ←←
                    ↖     M
                 E ← F ← ↗
→ Merge commit M
→ Historia con bifurcación

# Opción 2: Pull --rebase
git pull --rebase
Resultado: A ← B ← C ← D ← E' ← F'
→ E y F reescritos como E' y F'
→ Historia lineal
→ Sin merge commit

# ¿Cuál usar?
PULL (merge):
✓ Historia completa
✓ No reescribe commits
✓ Seguro para commits públicos

PULL --rebase:
✓ Historia limpia
✓ Sin merge commits
✓ Mejor para commits locales
```

**Mejores prácticas:**

```bash
✓ Usa pull --rebase para historia limpia
✓ Commitea o stash antes de pull
✓ Usa --autostash para conveniencia
✓ Verifica tracking branch con git status
✓ Resuelve conflictos cuidadosamente
✓ Usa fetch + merge para cambios importantes
✓ Configura pull.rebase = true globalmente

✗ No uses pull con cambios importantes sin revisar
✗ No uses pull --force sin entender consecuencias
✗ No ignores conflictos (resuélvelos siempre)
✗ No uses -X ours/theirs sin verificar
✗ Evita pull sin tracking branch configurado
✗ No hagas pull de rama incorrecta
```

---

#### 9.3.16 git push - Subiendo Cambios

**Funcionamiento interno:**

```
git push envía commits locales al remoto

Internamente:
1. Conecta con remoto
2. Compara refs locales vs remotas
3. Verifica que push sea fast-forward (o forzado)
4. Empaqueta objetos faltantes (commits, trees, blobs)
5. Envía objetos al remoto
6. Actualiza refs remotas
7. Remoto ejecuta hooks (si están configurados)

Resultado:
→ Remoto tiene tus commits
→ origin/<branch> actualizado localmente
→ Otros pueden pull tus cambios

Validaciones:
→ No se permite push no fast-forward (sin --force)
→ Requiere autenticación
→ Puede ser rechazado por hooks remotos
```

**Uso práctico y opciones:**

```bash
# 1. Push básico
git push
# → Push de rama actual a tracking remote

git push origin main
# → Explícito: push main a origin/main

# 2. Push creando rama remota
git push -u origin feature-x
# o: git push --set-upstream origin feature-x
# → Push Y configura tracking
# → Futuros git push sin argumentos funcionan

# 3. Push forzado (CUIDADO)
git push --force
# → Sobrescribe remoto con local
# → Puede perder commits remotos
# → ¡PELIGROSO!

git push --force-with-lease
# → Forzado "seguro"
# → Solo fuerza si nadie más actualizó
# → PREFERIBLE a --force

# 4. Push de todas las ramas
git push --all
# → Push de todas las ramas locales

# 5. Push de tags
git push --tags
# → Push de todos los tags

git push origin v1.0.0
# → Push de tag específico

git push origin --tags
# → Ambos: ramas + tags

# 6. Eliminar rama remota
git push origin --delete feature-x
# o: git push origin :feature-x
# → Elimina rama remota

# 7. Eliminar tag remoto
git push origin --delete tag v1.0.0
# → Elimina tag remoto

# 8. Push sin ejecutar hooks
git push --no-verify
# → Salta pre-push hooks locales

# 9. Push dry-run
git push --dry-run
# → Simula push sin hacerlo
# → Ver qué se enviaría

# 10. Push con progreso
git push --verbose
# → Muestra progreso detallado

# 11. Push a URL directa
git push https://github.com/user/repo.git main
# → Push sin remote configurado

# 12. Push con refspec
git push origin main:production
# → Push rama local main a remota production

git push origin HEAD:refs/heads/main
# → Refspec explícito

# 13. Push de commits específicos
# (No directamente, necesitas rama temporal)
git branch temp-push abc123
git push origin temp-push
git branch -D temp-push

# 14. Push sin fast-forward warning
git push --force-if-includes
# → Fuerza solo si incluye fetch previo
# → Git 2.30+

# 15. Push atómico
git push --atomic
# → Todo se push a o todo falla
# → No push parcial

# 16. Push con firma
git push --signed
# → Firma el push con GPG
# → Verificable en remoto
```

**Casos de uso reales:**

```bash
# Caso 1: Primera vez pushing rama
git checkout -b feature-auth
# ... commits ...
git push -u origin feature-auth
# → Crea rama remota + tracking

# Caso 2: Push rutinario
git add .
git commit -m "feat: Add feature"
git push
# → Flujo normal

# Caso 3: Push después de rebase (historia reescrita)
git pull --rebase
# ... resuelves conflictos ...
git push
# → Push de historia limpia

# Si rebasaste commits ya pusheados:
git push --force-with-lease
# → Forzado seguro

# Caso 4: Push a múltiples remotos
git push origin main
git push backup main
# O configurado:
git remote set-url --add --push origin https://backup.git
git push  # → Push a ambos

# Caso 5: Deploy a producción
git tag -a v2.1.0 -m "Release 2.1.0"
git push origin v2.1.0
# → Tag pushed, trigger deploy CI/CD

# Caso 6: Eliminar rama tras merge
git branch -d feature-x  # Local
git push origin --delete feature-x  # Remoto

# Caso 7: Corregir push incorrecto
# Hiciste push del commit equivocado
git reset --hard HEAD~1
git push --force-with-lease
# → Deshace el push

# Caso 8: Push selectivo
git push origin feature-a feature-b feature-c
# → Push de múltiples ramas

# Caso 9: Push sin verificación (urgente)
# Pre-push hook slow o failing
git push --no-verify
# → Salta hooks (úsalo con cuidado)

# Caso 10: Verificar antes de push
git push --dry-run
# → Ver qué se enviaría
git log origin/main..HEAD --oneline
# → Ver commits a pushear
git push  # Si OK
```

**Push rechazado (common issues):**

```bash
# Problema 1: Push rechazado (no fast-forward)
git push
# error: failed to push some refs
# hint: Updates were rejected because the remote contains work

# Causa: Alguien pusheó antes que tú
# Solución:
git pull --rebase  # O: git pull
git push

# Problema 2: Push forzado necesario (tras rebase)
git push
# error: failed to push

# Si rebasaste:
git push --force-with-lease
# → Seguro si nadie más trabaja en rama

# Problema 3: Push muy grande rechazado
git push
# error: RPC failed; HTTP 413

# Solución: Aumentar buffer
git config --global http.postBuffer 524288000
git push

# O push incremental:
git config --global http.maxRequestBuffer 100M
git config --global http.postBuffer 524288000

# Problema 4: Autenticación falla
git push
# Username: ...
# Password: ... (WRONG)

# Solución: Configurar credentials
git config --global credential.helper store
# O usar SSH keys

# Problema 5: Protected branch
git push
# error: You're not allowed to push to main

# Solución: Usa PR/MR workflow
git push -u origin feature-x
# → Luego crea Pull Request en UI

# Problema 6: Hook rechaza push
git push
# remote: error: commit message doesn't follow convention

# Solución: Corrige commits
git rebase -i HEAD~3  # Reword commits
git push --force-with-lease
```

**Push force: Cuándo y cómo:**

```bash
# ⚠️ NUNCA fuerces push en ramas compartidas (main, develop)

# Cuándo SÍ usar force push:
✓ En tu feature branch personal
✓ Después de rebase local
✓ Corregir commits antes de merge
✓ Limpieza de historia personal

# Cuándo NO:
✗ En main/develop/master
✗ En ramas de otros
✗ Si no estás 100% seguro
✗ En ramas públicas compartidas

# SIEMPRE usa --force-with-lease (no --force):
git push --force-with-lease
# → Solo fuerza si tu versión local incluye últimos cambios remotos
# → Protege contra sobrescribir trabajo de otros

# Ejemplo seguro:
git fetch origin
git rebase -i origin/main
# ... limpias commits ...
git push --force-with-lease origin feature-x
```

**Configuración útil:**

```bash
# Push solo rama actual (no todas)
git config --global push.default current
# → git push sin args pushea rama actual

# Opciones de push.default:
# nothing  - Error sin especificar rama
# current  - Push rama actual (recomendado)
# upstream - Push a tracking branch
# simple   - Push a upstream si mismo nombre

# Siempre requerir --force-with-lease
git config --global push.default simple
git config --global push.followTags true
# → Pushea tags anotados automáticamente

# Auto setup tracking
git config --global push.autoSetupRemote true
# → git push crea branch remoto automáticamente (Git 2.37+)

# Verificar push
git config --global push.default current
git config --global push.followTags true
```

**Push con GitHub/GitLab:**

```bash
# GitHub: Push triggering Actions
git push origin main
# → Triggers workflows on: push

# GitLab: Push con opciones CI
git push -o ci.skip  # Skip CI
git push -o ci.variable="KEY=value"  # Set variable

# GitHub: Push con PRs
git push -u origin feature-x
# → Luego en GitHub: Create Pull Request

# Protected branches
# Configurado en GitHub/GitLab UI
# Requiere:
# - Reviews
# - Status checks
# - Admin override
```

**Ver resultado de push:**

```bash
# Después de push:
# 1. Ver que se pusheó
git log --oneline origin/main

# 2. Verificar sincronización
git status
# Your branch is up to date with 'origin/main'

# 3. Ver diferencias (debería estar vacío)
git diff origin/main

# 4. Ver log gráfico
git log --oneline --graph --all
```

**Mejores prácticas:**

```bash
✓ Commitea cambios atómicos, push frecuentemente
✓ Usa --force-with-lease en vez de --force
✓ Verifica con --dry-run antes de push importante
✓ Usa -u en primer push de rama nueva
✓ Pull antes de push (evita rechazos)
✓ Usa SSH para autenticación automática
✓ Pushea tags explícitamente cuando releases
✓ Verifica con git log origin/<branch>..HEAD

✗ NO uses --force en ramas compartidas (main)
✗ NO pushees con cambios sin testar
✗ NO pushees credenciales, secrets, keys
✗ NO pushees archivos gigantes (usa Git LFS)
✗ NO ignores errores de push
✗ Evita push --all sin revisar todas las ramas
✗ NO pushees trabajo a medias a main
```

---

#### 9.3.17 git tag - Marcando Versiones

**Funcionamiento interno:**

```
git tag crea referencias inmutables a commits

Dos tipos de tags:

1. Lightweight tag:
   → Solo referencia en refs/tags/
   → Archivo con hash de commit
   → Como rama que no se mueve

2. Annotated tag:
   → Objeto completo en objects/
   → Con mensaje, autor, fecha
   → Firmable con GPG
   → RECOMENDADO para releases

Internamente:
Lightweight:
1. git update-ref refs/tags/v1.0.0 abc123
   → Crea archivo refs/tags/v1.0.0 con hash

Annotated:
1. Crea tag object en objects/
2. git update-ref refs/tags/v1.0.0 <tag-object-hash>
3. Tag object apunta al commit
```

**Uso práctico y opciones:**

```bash
# 1. Crear lightweight tag
git tag v1.0.0
# → Tag en HEAD actual
# → Solo referencia

git tag v0.9.0 abc123
# → Tag en commit específico

# 2. Crear annotated tag (RECOMENDADO)
git tag -a v1.0.0 -m "Release 1.0.0"
# → Con mensaje
# → Crea objeto tag

git tag -a v1.0.0 -m "Release 1.0.0" abc123
# → En commit específico

# 3. Listar tags
git tag
# v0.1.0
# v0.2.0
# v1.0.0

git tag -l
# → Igual que git tag

git tag -l "v1.*"
# → Lista tags matching pattern
# v1.0.0
# v1.0.1
# v1.1.0

# 4. Ver detalles de tag
git show v1.0.0
# → Muestra tag object + commit

git tag -v v1.0.0
# → Verifica firma GPG (si existe)

# 5. Listar con formato
git tag -n
# → Tags con primera línea de mensaje

git tag -n5
# → Con 5 líneas de mensaje

git tag --format='%(refname:short) %(taggerdate:short) %(subject)'
# → Formato personalizado

# 6. Crear tag con firma GPG
git tag -s v1.0.0 -m "Signed release"
# → Tag firmado con GPG
# → Verificable

# 7. Eliminar tag local
git tag -d v1.0.0
# → Elimina tag local

git tag --delete v0.9.0
# → Sintaxis alternativa

# 8. Push de tags
git push origin v1.0.0
# → Push de tag específico

git push origin --tags
# → Push de TODOS los tags

git push --tags
# → Atajo (si hay remote default)

# 9. Eliminar tag remoto
git push origin --delete v1.0.0
# o: git push origin :refs/tags/v1.0.0
# → Elimina tag en remoto

# 10. Fetch tags
git fetch --tags
# → Descarga todos los tags

git fetch origin tag v1.0.0
# → Fetch de tag específico

# 11. Checkout tag
git checkout v1.0.0
# → Detached HEAD en tag
# → Para inspección

# 12. Crear rama desde tag
git checkout -b hotfix-1.0.1 v1.0.0
# → Crea rama desde tag
# → Para hotfixes

# 13. Reemplazar tag (mover)
git tag -f v1.0.0 abc123
# → Mueve tag (NO recomendado si ya pusheado)

git push origin :refs/tags/v1.0.0  # Elimina remoto
git push origin v1.0.0             # Push nuevo

# 14. Listar tags ordenados
git tag --sort=-version:refname
# → Orden por versión, descendente

git tag --sort=creatordate
# → Por fecha de creación

# 15. Tags con  metadatos
git for-each-ref --format='%(refname:short) %(taggerdate) %(subject)' refs/tags
# → Info completa de cada tag
```

**Casos de uso reales:**

```bash
# Caso 1: Release workflow
git checkout main
git pull origin main
git tag -a v2.1.0 -m "Release 2.1.0

- Feature A added
- Bug B fixed
- Performance improvements"
git push origin v2.1.0
# → Tag pushed, triggers deploy/release

# Caso 2: Semantic versioning
git tag -a v1.0.0 -m "Major release: Breaking changes"
git tag -a v1.1.0 -m "Minor: New features"
git tag -a v1.1.1 -m "Patch: Bug fixes"

# Caso 3: Pre-releases
git tag -a v2.0.0-alpha.1 -m "Alpha release"
git tag -a v2.0.0-beta.1 -m "Beta release"
git tag -a v2.0.0-rc.1 -m "Release candidate"
git tag -a v2.0.0 -m "Stable release"

# Caso 4: Hotfix desde tag
git checkout -b hotfix-1.0.1 v1.0.0
# ... fixes ...
git commit -am "fix: Critical bug"
git tag -a v1.0.1 -m "Hotfix: Critical bug"
git push origin v1.0.1

# Caso 5: Ver código de versión específica
git checkout v1.5.0
# → Detached HEAD, inspección
cat VERSION
git checkout main

# Caso 6: Comparar versiones
git diff v1.0.0 v2.0.0
git log v1.0.0..v2.0.0 --oneline
# → Changelog entre versiones

# Caso 7: Signed releases (verificable)
git tag -s v3.0.0 -m "Secure release"
git push origin v3.0.0
# Otros verifican:
git tag -v v3.0.0

# Caso 8: Automatic tagging (CI/CD)
# En GitHub Actions:
# - name: Create tag
#   run: |
#     git tag -a v${{ github.run_number }} -m "Auto release"
#     git push origin v${{ github.run_number }}

# Caso 9: Cleanup old tags
git tag -l | grep 'alpha\|beta' | xargs git tag -d
# → Elimina tags de pre-releases

# Caso 10: Generar changelog
git log v1.0.0..v2.0.0 --oneline --no-merges > CHANGELOG.md
```

**Convenciones de versionado:**

```bash
# Semantic Versioning (semver.org):
v<MAJOR>.<MINOR>.<PATCH>[-<PRERELEASE>][+<BUILDMETADATA>]

Ejemplos:
v1.0.0           # Release estable
v1.0.0-alpha.1   # Pre-release
v1.0.0-beta.2    # Beta
v1.0.0-rc.1      # Release candidate
v1.0.0+20240204  # Con build metadata

# Calendar versioning (calver.org):
vYYYY.MM.DD
v2024.02.04

# Incremento:
MAJOR: Cambios incompatibles (breaking changes)
MINOR: Nueva funcionalidad compatible
PATCH: Bug fixes compatibles

# Ejemplos:
v1.2.3 → v2.0.0  # Breaking change
v1.2.3 → v1.3.0  # New feature
v1.2.3 → v1.2.4  # Bug fix
```

**Annotated vs Lightweight:**

```bash
# ANNOTATED (recomendado para releases):
git tag -a v1.0.0 -m "Release 1.0.0"

Ventajas:
✓ Tiene mensaje completo
✓ Incluye autor y fecha
✓ Puede firmarse con GPG
✓ Es un objeto completo
✓ git describe lo usa
✓ GitHub crea Release page

Ver info:
git show v1.0.0
# tag v1.0.0
# Tagger: User <email>
# Date: ...
# 
# Release 1.0.0
# 
# commit abc123...

# LIGHTWEIGHT (referencia simple):
git tag v1.0.0

Ventajas:
✓ Más simple
✓ Más rápido
✓ Útil para marcadores temporales

Ver info:
git show v1.0.0
# commit abc123...  (solo commit, no tag object)

# ¿Cuál usar?
→ Annotated para releases públicos
→ Lightweight para marcadores personales
```

**Tags en Git describe:**

```bash
# git describe usa tags para nombrar commits
git describe
# v1.2.3-14-g abc123
# ^      ^  ^  ^
# |      |  |  └─ hash abreviado
# |      |  └──── 'g' de git
# |      └─────── commits desde tag
# └────────────── tag más cercano

git describe --tags
# → Incluye lightweight tags

git describe --abbrev=0
# v1.2.3
# → Solo tag, sin extras

git describe --all
# → Incluye branches si no hay tags
```

**Mejores prácticas:**

```bash
✓ Usa annotated tags para releases (-a)
✓ Sigue semantic versioning
✓ Incluye changelog en mensaje de tag
✓ Firma tags importantes con GPG (-s)
✓ Push tags explícitamente (no auto)
✓ Tag desde main/master después de merge
✓ Usa CI/CD para automatizar tagging
✓ Documenta proceso de release en CONTRIBUTING.md

✗ No muevas tags ya pusheados
✗ No uses lightweight tags para releases
✗ No tags commits de trabajo en progreso
✗ No olvides pushear tags después de crearlos
✗ Evita tags sin seguir convención
✗ No uses tags como ramas (son inmutables)
```

---

#### 9.3.18 git revert - Deshaciendo Commits Públicos

**Funcionamiento interno:**

```
git revert crea NUEVO commit que deshace cambios de commit anterior

A diferencia de reset (mueve referencias):
- Revert NO modifica historia
- Revert crea commit nuevo
- Seguro para commits públicos

Internamente:
1. Lee commit a revertir
2. Calcula inverso de cambios:
   - Líneas añadidas → se eliminan
   - Líneas eliminadas → se añaden
3. Aplica cambios inversos
4. Crea nuevo commit
5. Historia se mantiene intacta

Antes: A ← B ← C ← D (HEAD)
git revert C
Después: A ← B ← C ← D ← C' (HEAD)
         └─ C' deshace cambios de C
```

**Uso práctico y opciones:**

```bash
# 1. Revert de commit específico
git revert abc123
# → Crea commit que deshace abc123
# → Abre editor para mensaje

git revert abc123 --no-edit
# → Usa mensaje default

# 2. Revert de HEAD
git revert HEAD
# → Deshace último commit

git revert HEAD~3
# → Deshace commit de hace 3

# 3. Revert múltiples commits
git revert HEAD~3..HEAD
# → Revierte últimos 3 commits
# → Crea 3 commits de revert

git revert abc123 def456 ghi789
# → Revierte específicos

# 4. Revert sin commit automático
git revert --no-commit abc123
# o: git revert -n abc123
# → Aplica cambios a staging
# → NO commitea automáticamente
# → Puedes modificar antes de commitear

# 5. Revert con estrategia de merge
git revert -X theirs abc123
# → En conflictos, usa versión "theirs"

# 6. Revert de merge commit
git revert -m 1 abc123
# → Revierte merge commit
# → -m 1: mantiene padre 1 (main line)
# → -m 2: mantiene padre 2 (merged branch)

# 7. Abortar revert en progreso
git revert --abort
# → Cancela revert con conflictos

# 8. Continuar revert tras resolver conflictos
# ... resuelves conflictos ...
git add archivo-resuelto
git revert --continue

# 9. Revert con mensaje personalizado
git revert abc123 -m "Revert: reason explanation"

# 10. Revert sin ejecutar hooks
git revert --no-verify abc123
# → Salta pre-commit hooks
```

**Casos de uso reales:**

```bash
# Caso 1: Revertir commit problemático en producción
git log --oneline
# abc123 feat: New feature (BROKEN)
# def456 fix: Bug fix
# ghi789 feat: Another feature

git revert abc123
git push origin main
# → Producción restaurada, historia intacta

# Caso 2: Revertir múltiples commits relacionados
git revert HEAD~2..HEAD --no-commit
# → Revierte últimos 2 en un solo commit
git commit -m "Revert: feature X rollback"

# Caso 3: Revert de merge (feature integrada)
git log --oneline --merges
# abc123 Merge branch 'feature-x'

git revert -m 1 abc123
# → Deshace todo el merge
# → Como si nunca se hubiera mergeado

# Caso 4: Revert temporal para testing
git revert abc123  # Remueve feature
# ... testing sin feature ...
git revert HEAD    # Revert del revert = restaura feature

# Caso 5: Revert parcial con edición
git revert --no-commit abc123
# → Cambios en staging
git restore --staged algunos-archivos
# → Quitas algunos archivos del revert
git commit -m "Partial revert of abc123"

# Caso 6: Revert en hotfix branch
git checkout -b hotfix-123 v1.0.0
git revert def456  # Commit problemático
git tag -a v1.0.1 -m "Hotfix"
git push origin v1.0.1

# Caso 7: Revert con conflictos
git revert abc123
# CONFLICT...
# ... editas archivo ...
git add archivo
git revert --continue
# → Completa revert con resolución

# Caso 8: Revert cadena de commits
git log --oneline
# abc123 commit 3
# def456 commit 2  
# ghi789 commit 1

git revert --no-commit ghi789 def456 abc123
# → Revierte los 3 en orden
git commit -m "Revert: rollback feature X completely"
```

**Revert vs Reset:**

```bash
# Escenario: Commit problemático en historia

RESET (reescribe historia):
git reset --hard HEAD~1
# Antes: A ← B ← C ← D (HEAD)
# Después: A ← B ← C (HEAD)
# → D desaparece de historia
# → Solo local, no uses en commits públicos

REVERT (preserva historia):
git revert D
# Antes: A ← B ← C ← D (HEAD)
# Después: A ← B ← C ← D ← D' (HEAD)
# → D se mantiene, D' lo deshace
# → Seguro para commits públicos

# ¿Cuándo usar cada uno?
RESET:
→ Commits solo locales
→ Historia no compartida
→ Quieres "borrar" commits

REVERT:
→ Commits ya pusheados
→ Historia compartida/pública
→ Quieres deshacer mantener registro
```

**Revert de merge commits:**

```bash
# Merge commit tiene DOS padres:
#        A ← B ← C (main)
#             ↖   ↗ M
#           D ← E (feature)

# Revertir merge:
git revert -m 1 M
# -m 1: mantiene línea principal (main: A-B-C)
# -m 2: mantiene línea mergeada (feature: D-E)

# Resultado con -m 1:
#        A ← B ← C ← M ← M'
#             ↖   ↗
#           D ← E
# M' deshace cambios de D-E

# ⚠️ PROBLEMA al re-mergear después:
git revert M'  # Intento de restaurar feature
git merge feature  # Git piensa feature ya está mergeada
# Solución:
git revert M'  # Revierte el revert
# O rebase feature sobre main
```

**Revert con conflictos:**

```bash
# Si revert causa conflictos:
git revert abc123
# CONFLICT (content): Merge conflict in file.txt
# Automatic revert failed

# Proceso de resolución:
# 1. Ver archivos en conflicto
git status
# You are currently reverting commit abc123

# 2. Resolver manualmente
vim file.txt  # Editar

# 3. Añadir resueltos
git add file.txt

# 4. Continuar
git revert --continue

# O abortar:
git revert --abort
```

**Revert avanzado:**

```bash
# Revert pero mantener algunos cambios
git revert --no-commit abc123
git restore --staged archivo-a-mantener
git restore archivo-a-mantener
git commit -m "Partial revert: kept file X"

# Revert rango con formato
git log --oneline
# abc Commit 3
# def Commit 2
# ghi Commit 1
git revert --no-commit ghi^..abc
# → Revierte de ghi (exclusive) a abc (inclusive)

# Revert con git bisect
git bisect start
git bisect bad HEAD
git bisect good v1.0.0
# ... git bisect identifica abc123 como bad ...
git bisect reset
git revert abc123
```

**Mejores prácticas:**

```bash
✓ Usa revert para commits públicos/pusheados
✓ Usa --no-commit para revertir múltiples como uno
✓ Incluye razón del revert en mensaje
✓ Usa -m 1 para revert de merges (usualmente)
✓ Resuelve conflictos cuidadosamente
✓ Considera revert temporal para debugging
✓ Documenta reverts en changelog

✗ No uses revert para commits locales (usa reset)
✗ No revertir sin entender impacto
✗ No omitas -m en revert de merge (ambiguo)
✗ Evita revert de revert (confuso, usa rebase o revert del revert)
✗ No uses revert como reemplazo de branches
```

---

#### 9.3.19 git cherry-pick - Aplicando Commits Selectivos

**Funcionamiento interno:**

```
git cherry-pick aplica cambios de commit específico a rama actual

Internamente:
1. Lee commit a cherry-pick
2. Calcula diff del commit
3. Aplica diff a rama actual
4. Crea NUEVO commit con mismo cambio
5. Nuevo commit tiene hash diferente (diferente padre)

Antes:
main:    A ← B ← C (HEAD)
feature: A ← D ← E ← F

git cherry-pick E

Después:
main:    A ← B ← C ← E' (HEAD)
feature: A ← D ← E ← F
         └─ E' es copia de E, hash diferente
```

**Uso práctico y opciones:**

```bash
# 1. Cherry-pick básico
git cherry-pick abc123
# → Aplica commit abc123 a rama actual
# → Crea nuevo commit

# 2. Cherry-pick sin commit automático
git cherry-pick --no-commit abc123
# o: git cherry-pick -n abc123
# → Aplica cambios a staging
# → NO commitea
# → Puedes modificar antes de commitear

# 3. Cherry-pick múltiples commits
git cherry-pick abc123 def456 ghi789
# → Aplica tres commits en orden

git cherry-pick abc123..ghi789
# → Rango de commits (abc123 exclusive)

git cherry-pick abc123^..ghi789
# → Rango (abc123 inclusive)

# 4. Cherry-pick con mensaje editado
git cherry-pick -e abc123
# o: git cherry-pick --edit abc123
# → Abre editor para modificar mensaje

# 5. Cherry-pick manteniendo autor original
git cherry-pick --author="Original Author <email>" abc123
# → Cambia autor

# Por defecto cherry-pick mantiene autor original

# 6. Cherry-pick con estrategia de merge
git cherry-pick -X theirs abc123
# → En conflictos, prefiere versión de abc123

git cherry-pick -X ours abc123
# → Prefiere versión de rama actual

# 7. Abortar cherry-pick
git cherry-pick --abort
# → Cancela cherry-pick en progreso

# 8. Continuar después de resolver conflictos
# ... resuelves conflictos ...
git add archivo-resuelto
git cherry-pick --continue

# 9. Skip commit problemático
git cherry-pick --skip
# → Salta commit actual en cherry-pick de rango

# 10. Cherry-pick añadiendo nota
git cherry-pick -x abc123
# → Añade línea al mensaje:
#   (cherry picked from commit abc123...)
# → Útil para rastrear origen

# 11. Cherry-pick de merge commit
git cherry-pick -m 1 merge-commit
# → -m 1: aplica cambios respecto a primer padre

# 12. Cherry-pick sin cambiar autor/fecha
git cherry-pick --ff abc123
# → Fast-forward si es posible
# → Mantiene metadata original

# 13. Cherry-pick con estrategia específica
git cherry-pick --strategy=recursive -X patience abc123
# → Usa algoritmo patience
```

**Casos de uso reales:**

```bash
# Caso 1: Hotfix de producción
# Bug fix en develop, necesitas en production
git checkout production
git cherry-pick abc123  # Commit del fix de develop
git push origin production

# Caso 2: Backport de feature a release anterior
# Feature en main, necesitas en release-2.0
git checkout release-2.0
git log main --oneline | grep "feature-x"
# def456 feat: Feature X
git cherry-pick def456
git push origin release-2.0

# Caso 3: Mover commits entre ramas
# Commiteaste en rama incorrecta
git log --oneline
# abc123 commit A (en rama incorrecta)
git checkout rama-correcta
git cherry-pick abc123
git checkout rama-incorrecta
git reset --hard HEAD~1  # Elimina de rama incorrecta

# Caso 4: Aplicar solo parte de rama feature
# feature tiene 10 commits, solo quieres 2
git log feature --oneline
# ... muchos commits ...
# abc123 commit importante 1
# def456 commit importante 2
git cherry-pick abc123 def456

# Caso 5: Cherry-pick múltiples con edición
git cherry-pick -n abc123 def456 ghi789
# → Todos en staging, sin commitear
git commit -m "Combined cherry-picks: reason"

# Caso 6: Sincronizar fix entre versiones
# Fix en v2.0, aplicar a v1.5 y v1.8
git checkout v1.5-branch
git cherry-pick -x abc123  # Fix
git checkout v1.8-branch
git cherry-pick -x abc123  # Mismo fix
# -x anota origen del cherry-pick

# Caso 7: Rescatar commit de rama eliminada
git reflog
# abc123 HEAD@{5}: commit: Important work
git cherry-pick abc123
# → Rescatado

# Caso 8: Test parcial de feature
git checkout -b test-feature
git cherry-pick feature-branch~5  # Solo último commit de feature
# ... testing ...
git checkout main
git branch -D test-feature

# Caso 9: Cherry-pick con conflictos
git cherry-pick abc123
# CONFLICT...
vim file.txt  # Resolver
git add file.txt
git cherry-pick --continue

# Caso 10: Cherry-pick inverso (aplicar revert)
git cherry-pick abc123  # Commit original
# Más tarde:
git revert abc123  # Crea revert commit
# En otra rama:
git cherry-pick def456  # def456 es el revert commit
```

**Cherry-pick vs Merge:**

```bash
# Escenario: Quieres traer cambios de feature a main

MERGE (trae todo):
git checkout main
git merge feature
# → Todos los commits de feature
# → Merge commit si no es ff
# → Historia muestra integración completa

CHERRY-PICK (selectivo):
git checkout main
git cherry-pick abc123 def456
# → Solo commits específicos
# → Sin merge commit
# → Historia lineal
# → Commits duplicados (diferentes hashes)

# ¿Cuándo usar cada uno?
MERGE:
→ Quieres feature completa
→ Mantienes historia de rama
→ Integración oficial

CHERRY-PICK:
→ Solo algunos commits
→ Hotfixes
→ Backports
→ Corrección de commits en rama incorrecta
```

**Cherry-pick con conflictos:**

```bash
# Cuando hay conflictos:
git cherry-pick abc123
# error: could not apply abc123...
# hint: after resolving the conflicts, mark the corrected paths
# hint: with 'git add <paths>' or 'git rm <paths>'

# Proceso:
# 1. Ver estado
git status
# You are currently cherry-picking commit abc123

# 2. Ver conflictos
git diff

# 3. Resolver
vim file.txt

# 4. Añadir
git add file.txt

# 5. Continuar
git cherry-pick --continue

# O abortar:
git cherry-pick --abort
```

**Cherry-pick avanzado:**

```bash
# Cherry-pick rango excluyendo algunos
git cherry-pick abc123..ghi789
# Durante el proceso:
# Si un commit causa problemas:
git cherry-pick --skip  # Salta ese commit

# Cherry-pick manteniendo commits vacíos
git cherry-pick --allow-empty abc123
# → Permite cherry-pick de commits sin cambios

# Cherry-pick con mainline específico (merges)
git cherry-pick -m 2 merge-commit
# → Aplica cambios respecto a segundo padre

# Cherry-pick con keep-redundant-commits
git cherry-pick --keep-redundant-commits abc123..def456
# → No omite commits que generan cambios vacíos
```

**Problemas comunes:**

```bash
# Problema 1: Cherry-pick resulta en commit vacío
git cherry-pick abc123
# The previous cherry-pick is now empty

# Causa: Cambios ya están en rama
# Solución:
git cherry-pick --skip
# O:
git cherry-pick --allow-empty abc123

# Problema 2: Conflictos complejos
# Solución: Abortar y usar merge
git cherry-pick --abort
git merge feature  # Mergea todo

# Problema 3: Cherry-pick de demasiados commits
# Mejor: merge o rebase
# Cherry-pick para pocos commits específicos

# Problema 4: Perder rastro de origen
# Solución: Usa -x
git cherry-pick -x abc123
# Mensaje incluye: (cherry picked from commit abc123)
```

**Mejores prácticas:**

```bash
✓ Usa cherry-pick para fixes urgentes específicos
✓ Usa -x para rastrear origen del cherry-pick
✓ Usa --no-commit para combinar múltiples cherry-picks
✓ Resuelve conflictos cuidadosamente
✓ Documenta por qué usaste cherry-pick
✓ Considera merge si necesitas muchos commits
✓ Usa cherry-pick para backports a versiones antiguas

✗ No uses cherry-pick como reemplazo de merge regular
✗ No cherry-picks en exceso (historia confusa)
✗ Evita cherry-pick de merges sin -m
✗ No ignores conflictos (resuélvelos correctamente)
✗ Evita cherry-pick si merge es más apropiado
✗ No cherry-picks sin comunicar al equipo
```

---

#### 9.3.20 git clean - Limpiando Archivos No Rastreados

**Funcionamiento interno:**

```
git clean elimina archivos untracked del working directory

Archivos "untracked" son:
→ No están en Git (no han sido added)
→ No están en .gitignore
→ Aparecen como "??" en git status -s

PELIGRO: Eliminación NO es reversible
→ Archivos eliminados no están en Git
→ No recuperables con Git
→ Usa con CUIDADO

Internamente:
1. Escanea working directory
2. Compara con .git/index (archivos rastreados)
3. Compara con .gitignore (archivos ignorados)
4. Identifica archivos untracked
5. Los elimina del filesystem
```

**Uso práctico y opciones:**

```bash
# ⚠️ SIEMPRE usa -n (dry-run) PRIMERO

# 1. Ver qué se eliminaría (DRY RUN - SEGURO)
git clean -n
# o: git clean --dry-run
# → Muestra qué se eliminaría
# → NO elimina nada
# → ¡SIEMPRE ejecuta esto primero!

# 2. Eliminar archivos untracked
git clean -f
# o: git clean --force
# → Elimina archivos untracked
# → Requiere -f por seguridad

# 3. Eliminar directorios untracked también
git clean -fd
# → Elimina archivos Y directorios

# 4. Eliminar archivos ignorados también
git clean -fx
# → Elimina untracked E ignorados (.gitignore)

git clean -fxd
# → Archivos + directorios + ignorados
# → Limpieza COMPLETA

# 5. Interactivo (RECOMENDADO)
git clean -i
# o: git clean --interactive
# → Menú interactivo
# → Eliges qué eliminar
# → Más seguro

# 6. Ver solo archivos ignored
git clean -fxd --dry-run
# → Qué archivos .gitignore se eliminarían

# 7. Eliminar con exclusiones
git clean -fxd -e "*.log"
# → Elimina todo excepto *.log

git clean -fd -e node_modules
# → Excepto node_modules/

# 8. Solo directorios
git clean -fd -n
# → Ver directorios que se eliminarían

# 9. Quiet mode
git clean -fq
# → Sin output

# 10. Eliminar en subdirectorio específico
git clean -fd src/
# → Solo en src/
```

**Casos de uso reales:**

```bash
# Caso 1: Limpiar build artifacts
git clean -fxd
# → Elimina builds, node_modules, __pycache__, etc.
# → Útil antes de rebuild limpio

# Caso 2: Resetear working directory completamente
git reset --hard HEAD  # Descarta cambios en tracked files
git clean -fxd         # Elimina untracked
# → Working directory como en HEAD

# Caso 3: Limpiar después de merge/rebase fallido
git merge --abort
git clean -fd
# → Elimina archivos temporales del merge

# Caso 4: Eliminar archivos de compilación
# Antes de git clean:
# .gitignore contiene: *.o, *.exe, build/
git clean -fxd
# → Elimina *.o, *.exe, build/ aunque están ignored

# Caso 5: Preparar para deploy limpio
npm run build     # Build inicial
git clean -fxd    # Limpia
npm run build     # Build limpio
# → Sin archivos viejos

# Caso 6: Limpiar selectivamente con interactive
git clean -i
# Would remove the following items:
#   temp.txt
#   debug.log
#   test/
# *** Commands ***
#     1: clean    2: filter by pattern    3: select by numbers
#     4: ask each 5: quit                 6: help
# What now> 3
# Select items to delete:
#   1: temp.txt   2: debug.log   3: test/
# Input> 1 2
# → Solo elimina temp.txt y debug.log

# Caso 7: Limpiar antes de cambiar de rama
git status
# Untracked files: varios...
git clean -fd
git checkout otra-rama
# → Sin arrastrar archivos untracked

# Caso 8: Script de limpieza automática
#!/bin/bash
echo "Limpiando archivos untracked..."
git clean -fxd --dry-run
read -p "Proceder? (y/n) " -n 1 -r
if [[ $REPLY =~ ^[Yy]$ ]]; then
    git clean -fxd
    echo "Limpieza completa"
fi

# Caso 9: Limpiar pero preservar configuración local
git clean -fxd -e "config.local.json" -e ".env.local"
# → Limpia todo excepto config local

# Caso 10: Ver cuánto espacio liberarías
git clean -fxd --dry-run | wc -l
# → Cuenta archivos a eliminar
```

**Menú interactivo explicado:**

```bash
git clean -i

Would remove the following items:
  temp.txt
  cache/
  *.log

*** Commands ***
    1: clean                  # Elimina todo listado
    2: filter by pattern      # Filtra con wildcards
    3: select by numbers      # Elige por números
    4: ask each              # Pregunta por cada archivo
    5: quit                  # Salir sin hacer nada
    6: help                  # Ayuda

What now> 2
  temp.txt
  cache/
  debug.log
  error.log
Input> *.log
Would remove:
  debug.log
  error.log

What now> 1
Removing debug.log
Removing error.log
```

**Clean vs Reset vs Restore:**

```bash
# CLEAN: Elimina archivos untracked
git clean -fd
→ Elimina archivos NO en Git
→ NO afecta archivos rastreados

# RESET: Mueve referencias, opcionalmente afecta tracked
git reset --hard HEAD
→ Descarta cambios en archivos rastreados
→ NO elimina untracked

# RESTORE: Restaura archivos tracked
git restore archivo.txt
→ Descarta cambios en archivo específico
→ NO elimina untracked

# COMBINACIÓN (reset completo):
git reset --hard HEAD  # Tracked files
git clean -fxd         # Untracked files
→ Working directory = HEAD exactamente
```

**Archivos que clean elimina:**

```bash
# Ejemplo de working directory:

archivo-tracked.txt      # Rastreado por Git
archivo-modificado.txt   # Tracked pero modificado
archivo-nuevo.txt        # ← git clean -f elimina
directorio-nuevo/        # ← git clean -fd elimina
node_modules/            # En .gitignore ← git clean -fx elimina
.env                     # En .gitignore ← git clean -fx elimina

git clean -f
→ Elimina: archivo-nuevo.txt

git clean -fd
→ Elimina: archivo-nuevo.txt, directorio-nuevo/

git clean -fxd
→ Elimina: archivo-nuevo.txt, directorio-nuevo/, node_modules/, .env
```

**Configuración:**

```bash
# Requerir doble confirmación
git config --global clean.requireForce true  # Default

# Permitir sin -f (NO RECOMENDADO)
git config --global clean.requireForce false
# Ahora git clean funciona sin -f

# Ver configuración
git config --list | grep clean
```

**Protección de .gitignore:**

```bash
# Si quieres mantener archivos ignored:
git clean -fd        # NO los elimina
git clean -fxd       # SÍ los elimina

# Ejemplo:
# .gitignore contiene: node_modules/

git clean -fd
# → node_modules/ se mantiene (está ignored)

git clean -fxd
# → node_modules/ se ELIMINA

# Para realmente limpiar EVERYTHING:
git clean -fxd
```

**Troubleshooting:**

```bash
# Problema 1: git clean sin efecto
git clean -f
# Stopping at filesystem boundary...

# Causa: Directorio es submódulo
# Solución:
git submodule deinit <path>
git clean -fd

# Problema 2: Archivos protegidos no se eliminan
# Algunos archivos con permisos especiales

# Solución:
chmod -R u+w directory/
git clean -fd

# Problema 3: Eliminaste por error
# Archivos ya NO están en Git
# Recuperación depende de filesystem/backups

# Prevención: SIEMPRE usa -n primero
git clean -n
# Revisa output
git clean -f  # Si estás seguro
```

**Mejores prácticas:**

```bash
✓ SIEMPRE usa -n (dry-run) antes de clean real
✓ Usa -i (interactive) para eliminar selectivamente
✓ Usa .gitignore para archivos que deben ignorarse
✓ Usa -e para preservar archivos específicos
✓ Commitea o stash antes de clean
✓ Verifica con git status antes de clean
✓ Usa -fd para directorios también

✗ NUNCA uses git clean sin revisar primero
✗ No uses -x sin entender que elimina ignored files
✗ No asumas que puedes recuperar archivos
✗ No uses clean en repo de otros sin permiso
✗ Evita clean en directorio equivocado
✗ No ignores warnings de clean
✗ No uses -f por defecto (configura requireForce true)
```

---

#### 9.3.21 git rm y git mv - Eliminando y Moviendo Archivos

**Funcionamiento interno:**

```
git rm elimina archivo del working directory Y staging

Internamente:
1. Elimina archivo del filesystem
2. Actualiza .git/index (quita entrada)
3. Cambio queda stageado
4. Necesitas commitear

git mv mueve/renombra archivo

Internamente (git mv old new):
1. git rm old
2. git add new
3. Git detecta rename automáticamente
4. Commit muestra como rename, no delete+add
```

**git rm - Uso práctico:**

```bash
# 1. Eliminar archivo (working + staging)
git rm archivo.txt
# → Elimina de disk y staging
# → Listo para commitear

# 2. Eliminar solo de Git (keep on disk)
git rm --cached archivo.txt
# → Elimina de Git (unstaged)
# → Archivo permanece en disk
# → Útil para archivos que deberían estar en .gitignore

# 3. Eliminar forzado (con cambios uncommited)
git rm -f archivo.txt
# o: git rm --force archivo.txt
# → Fuerza eliminación aunque tenga cambios

# 4. Eliminar directorio completo
git rm -r directorio/
# → Recursivo, elimina todo el directorio

git rm -rf directorio/
# → Recursivo y forzado

# 5. Eliminar con wildcards
git rm '*.txt'
# → Todos los .txt en repo
# → Comillas para que Git expanda, no shell

git rm 'logs/*.log'
# → Todos los .log en logs/

# 6. Dry run (ver qué se eliminaría)
git rm --dry-run -r src/
# → Muestra qué se eliminaría sin hacerlo

# 7. Eliminar archivos ya eliminados del filesystem
# Eliminaste archivo con rm (no git rm)
git add -u
# o:
git rm $(git ls-files --deleted)
# → Stagea las eliminaciones

# 8. Eliminar archivos ignored
git rm --cached -r node_modules/
# → Elimina de Git
# → Añade a .gitignore después

# 9. Eliminar con verbose
git rm -v archivo1.txt archivo2.txt
# rm 'archivo1.txt'
# rm 'archivo2.txt'
```

**git mv - Uso práctico:**

```bash
# 1. Mover/renombrar archivo
git mv viejo-nombre.txt nuevo-nombre.txt
# → Rename detectado automáticamente

# 2. Mover a directorio
git mv archivo.txt src/
# → Mueve a src/archivo.txt

# 3. Mover múltiples archivos
git mv archivo1.txt archivo2.txt directorio/
# → Ambos a directorio/

# 4. Renombrar directorio
git mv old-dir/ new-dir/
# → Renombra directorio completo

# 5. Dry run
git mv --dry-run old.txt new.txt
# → Ver qué haría sin hacerlo

# 6. Forzar movimiento (sobrescribe destino)
git mv -f old.txt existing.txt
# → Sobrescribe existing.txt

# 7. Verbose
git mv -v old.txt new.txt
# Renaming old.txt to new.txt

# 8. Movimiento manual (no recomendado pero equivalente)
mv old.txt new.txt
git rm old.txt
git add new.txt
# Git detecta rename automáticamente si >50% similar
```

**Casos de uso reales:**

```bash
# Caso 1: Renombrar archivo
git mv README.txt README.md
git commit -m "docs: Convert README to Markdown"

# Caso 2: Reorganizar estructura de proyecto
git mv lib/*.js src/lib/
git mv tests/*.js src/tests/
git commit -m "refactor: Reorganize project structure"

# Caso 3: Eliminar archivo sensible del repo
git rm --cached .env
echo ".env" >> .gitignore
git add .gitignore
git commit -m "chore: Remove .env from tracking"

# Caso 4: Limpiar archivos de build del historial
git rm --cached -r build/
echo "build/" >> .gitignore
git add .gitignore
git commit -m "chore: Ignore build directory"

# Caso 5: Eliminar archivos obsoletos
git rm old-config.yml legacy-code.js
git commit -m "chore: Remove obsolete files"

# Caso 6: Migrar archivos entre módulos
git mv src/module-a/utils.js src/module-b/utils.js
git commit -m "refactor: Move utils to module-b"

# Caso 7: Eliminar archivos ya borrados localmente
# Usaste rm en vez de git rm
git status
# deleted: file1.txt
# deleted: file2.txt
git add -u  # Stagea todas las deletes
git commit -m "Remove files"

# Caso 8: Case-sensitive rename (macOS/Windows)
# macOS/Windows: filesystems case-insensitive
git mv readme.md README.md  # NO funciona (mismo nombre)

# Solución:
git mv readme.md temp-readme.md
git mv temp-readme.md README.md
git commit -m "docs: Fix README capitalization"

# Caso 9: Eliminar node_modules ya committed
git rm -r --cached node_modules/
echo "node_modules/" >> .gitignore
git add .gitignore
git commit -m "chore: Untrack node_modules"

# Caso 10: Refactor: renombrar múltiples archivos
for file in *.js; do
  git mv "$file" "${file%.js}.ts"
done
git commit -m "refactor: Convert JS to TS"
```

**Detección de renames por Git:**

```bash
# Git detecta renames automáticamente si:
# → Contenido >50% similar (configurable)
# → Incluso si usaste mv + git add

# Ejemplo:
mv old.txt new.txt
git rm old.txt
git add new.txt

git status
# renamed: old.txt -> new.txt  ← Git detectó rename

# Configurar threshold de detección:
git config --global diff.renameLimit 1000
git config --global diff.renames true
git config --global diff.rename 50  # % de similitud

# Ver renames en log:
git log --follow archivo-renombrado.txt
# → Sigue historia a través de renames

# Ver renames en diff:
git diff --find-renames
# o:
git diff -M

# Ajustar threshold:
git diff -M50%  # Detect renames >50% similar
git diff -M10%  # Más sensible
```

**rm vs git rm:**

```bash
# RM (comando shell):
rm archivo.txt
# → Elimina de disk
# → Git lo ve como "deleted"
# → Necesitas: git add archivo.txt o git add -u

git status
# Changes not staged:
#   deleted: archivo.txt

# GIT RM:
git rm archivo.txt
# → Elimina de disk
# → Automáticamente staged
# → Listo para commit

git status
# Changes to be committed:
#   deleted: archivo.txt

# ¿Cuál usar?
→ git rm: workflow Git (recomendado)
→ rm: si necesitas eliminar sin stagear aún
```

**mv vs git mv:**

```bash
# MV (comando shell):
mv old.txt new.txt
# → Git ve: deleted old.txt, untracked new.txt
# → Necesitas: git rm old.txt && git add new.txt

git status
# Changes not staged:
#   deleted: old.txt
# Untracked files:
#   new.txt

# GIT MV:
git mv old.txt new.txt
# → Rename detectado
# → Automáticamente staged

git status
# Changes to be committed:
#   renamed: old.txt -> new.txt

# ¿Cuál usar?
→ git mv: más claro (recomendado)
→ mv: si ya lo hiciste, Git detecta rename igual
```

**Operaciones complejas:**

```bash
# Eliminar todos los archivos de cierto tipo
git rm '**/*.tmp'
# → Todos los .tmp en todo el repo

# Eliminar excepto algunos
git ls-files '*.log' | grep -v 'important.log' | xargs git rm

# Renombrar con patrón
for file in *.jsx; do
  git mv "$file" "${file%.jsx}.tsx"
done

# Mover archivos de raíz a subdirectorio
mkdir src
git mv *.js src/
git commit -m "refactor: Move JS files to src/"

# Eliminar archivos modificados (forzar)
git rm -f archivo-con-cambios.txt

# Dejar de rastrear pero mantener en working
git rm --cached config.local.yml
```

**Problemas comunes:**

```bash
# Problema 1: git rm de archivo modificado
git rm archivo.txt
# error: 'archivo.txt' has local modifications

# Soluciones:
# A) Commitear cambios primero
git add archivo.txt
git commit -m "Last changes before removal"
git rm archivo.txt

# B) Forzar
git rm -f archivo.txt

# Problema 2: git rm de archivo no existe
git rm archivo-no-existe.txt
# fatal: pathspec 'archivo-no-existe.txt' did not match any files

# Solución: Ya está eliminado
git add -u

# Problema 3: Rename no detectado
mv file.txt file2.txt
git status
# deleted: file.txt
# untracked: file2.txt

# Solución: Add ambos
git add file.txt file2.txt
# Git detecta rename automáticamente

# Problema 4: Case-sensitive rename no funciona
git mv readme.md README.md
# fatal: destination exists

# Solución:
git mv readme.md temp
git mv temp README.md
```

**Mejores prácticas:**

```bash
✓ Usa git rm en vez de rm (más claro)
✓ Usa git mv en vez de mv (detecta rename)
✓ Usa --cached para unstage sin eliminar de disk
✓ Commitea después de rm/mv (cambios en staging)
✓ Usa -r para directorios
✓ Usa wildcards con comillas ('*.txt')
✓ Usa git log --follow para historia de renames
✓ Usa --dry-run para verificar antes de ejecutar

✗ No uses rm -rf .git (NUNCA)
✗ No uses git rm -f sin revisar cambios
✗ No olvides commitear después de rm/mv
✗ No uses rm cuando quieres unstage (usa reset)
✗ Evita eliminar .gitignore accidentally
✗ No renames en commits gigantes (difícil de revisar)
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
[↑ Top](#-tabla-de-contenidos)

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

