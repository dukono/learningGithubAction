# 4. git diff - Comparando Cambios

[🏠 Volver al Índice](../../GIT_COMANDOS_GUIA_PRACTICA.md)

---

## 4. git diff - Comparando Cambios
[⬆️ Top](#4-git-diff---comparando-cambios)

**¿Qué hace?**
Muestra las diferencias de contenido entre dos fuentes: puede comparar el working directory con el staging area, el staging area con el último commit, dos commits entre sí, o dos ramas. Es la herramienta para responder "¿qué cambié exactamente?".

**Funcionamiento interno:** [🔙](#4-git-diff---comparando-cambios)

```
git diff lee el contenido de dos fuentes y aplica un algoritmo de diff:

1. Lee el contenido de las dos fuentes a comparar (blobs, commits, working dir)
2. Ejecuta el algoritmo de diff elegido:
   - Myers (default): rápido y eficiente, bueno en general
   - patience: mejor para refactorizaciones (detecta bloques movidos)
   - histogram: evolución de patience, mejor para muchos cambios
3. Genera "hunks" (bloques de diferencias con contexto)
4. Formatea el output en formato unidiff:
   - Líneas con "-" = lo que había antes (eliminado)
   - Líneas con "+" = lo que hay ahora (añadido)
   - Líneas sin prefijo = contexto (sin cambios, solo para orientarte)

Las tres fuentes principales que puede comparar:
  Working Directory → lo que tienes modificado en disco, sin git add
  Staging Area      → lo que tienes preparado para commitear (después de git add)
  Commits           → cualquier commit del historial, rama, tag
```

**Todas las opciones importantes:** [🔙](#4-git-diff---comparando-cambios)

```bash
# ============================================
# LAS 3 COMPARACIONES MÁS FRECUENTES
# ============================================

# 1. Working directory vs Staging (lo NO stageado)
# Situación: Modificaste archivos pero no hiciste git add.
# Quieres ver exactamente qué cambiaste antes de añadirlo al staging.
git diff
# → Muestra cambios en disco que NO están en staging
# → Si ya hiciste git add, este diff estará vacío

# 2. Staging vs último commit (lo que VAS a commitear)
# Situación: Hiciste git add y quieres revisar qué está
# en el staging antes de hacer commit.
git diff --staged
# ó equivalente:
git diff --cached
# → Muestra lo que hay en staging comparado con HEAD
# → Esto es EXACTAMENTE lo que entrará en el próximo commit

# 3. Working directory vs último commit (TODOS los cambios)
# Situación: Quieres ver todo lo que has cambiado desde el último commit,
# ya sea que esté o no en staging.
git diff HEAD
# → Muestra diferencias entre todos tus cambios y HEAD


# ============================================
# COMPARAR COMMITS ENTRE SÍ
# ============================================

# Dos commits específicos:
git diff abc1234 def5678
# → Muestra qué cambió entre esos dos commits

# Commit anterior vs HEAD actual:
git diff HEAD~1
git diff HEAD~3 HEAD

# Commit anterior en un archivo concreto:
git diff HEAD~3 -- archivo.txt
# → Solo muestra cambios de ese archivo entre HEAD~3 y HEAD


# ============================================
# COMPARAR RAMAS
# ============================================

# Diferencia entre dos ramas (qué tiene una que no tiene la otra):
git diff main feature/login
# → Lo que hay en feature/login comparado con main
# → Como si preguntaras: "¿qué añadió feature/login respecto a main?"

# Comparar desde el punto de divergencia (.. vs ...):
git diff main..feature/login    # Equivale a: git diff main feature/login
git diff main...feature/login   # Diferencia SOLO de lo que feature/login añadió
                                 # desde que se separó de main (ignora lo que avanzó main)
# ¿Cuándo usar ...?
# Cuando main ha avanzado desde que creaste feature/login y quieres ver
# solo los cambios de la feature, no los de main.


# ============================================
# COMPARAR CON EL REMOTO
# ============================================
# Situación: Quieres ver qué diferencia hay entre tu código local
# y lo que hay en el remoto (después de hacer git fetch).
git diff HEAD origin/main
# → Lo que tu HEAD tiene diferente respecto a origin/main
git diff origin/main HEAD
# → Lo mismo pero desde la perspectiva opuesta


# ============================================
# LIMITAR A ARCHIVOS ESPECÍFICOS
# ============================================

# Solo un archivo:
git diff -- archivo.txt
git diff HEAD~3 -- src/auth.js

# Un directorio:
git diff -- src/

# Múltiples archivos:
git diff -- archivo1.txt archivo2.txt


# ============================================
# OPCIONES DE FORMATO DE SALIDA
# ============================================

# Solo los nombres de archivos que cambiaron:
git diff --name-only
git diff HEAD~1 --name-only

# Nombres + estado (M=modificado, A=añadido, D=eliminado):
git diff --name-status
git diff main feature/login --name-status

# Estadística resumida (cuántas líneas añadidas/eliminadas):
git diff --stat
git diff HEAD~5 HEAD --stat

# Estadística compacta (una línea por archivo):
git diff --shortstat


# ============================================
# OPCIONES DE VISUALIZACIÓN MEJORADA
# ============================================

# Diff por palabras (útil para documentación, Markdown):
git diff --word-diff
# → Muestra qué palabras cambiaron, no qué líneas

# Diff por palabras con colores:
git diff --word-diff=color

# Ignorar cambios de espacios en blanco:
git diff -w
git diff --ignore-all-space

# Ignorar cambios al final de línea (espacios, tabs):
git diff -b
git diff --ignore-space-change

# Detectar líneas que se movieron (util en refactorizaciones):
git diff --color-moved
git diff --color-moved=dimmed-zebra   # Con mejor visualización


# ============================================
# --diff-filter: FILTRAR POR ESTADO DEL ARCHIVO
# ============================================
# Situación: No quieres ver TODOS los archivos que cambiaron,
# sino solo los que tienen un estado concreto (solo añadidos,
# solo eliminados, solo los que tienen conflicto, etc.)
#
# Cada letra representa un estado:
#   A  → Added          (archivo nuevo que se añadió)
#   M  → Modified       (archivo existente que se modificó)
#   D  → Deleted        (archivo que se eliminó)
#   R  → Renamed        (archivo que se renombró)
#   C  → Copied         (archivo que se copió)
#   U  → Unmerged       (archivo con conflicto sin resolver)
#   T  → Type changed   (cambió de tipo: ej. archivo normal → symlink)
#   B  → Broken pair    (par rename/copy roto)
#   X  → Unknown        (estado desconocido)
#
# Minúscula = excluir ese tipo (invertir el filtro)

# Solo archivos añadidos:
git diff --name-only --diff-filter=A
git diff --name-only --diff-filter=A HEAD~1

# Solo archivos eliminados:
git diff --name-only --diff-filter=D

# Solo archivos modificados:
git diff --name-only --diff-filter=M

# Solo archivos renombrados:
git diff --name-only --diff-filter=R

# Solo archivos con conflicto (durante un merge):
git diff --name-only --diff-filter=U
# → El uso más frecuente: ver qué archivos siguen sin resolver

# Combinar varios estados (añadidos O modificados):
git diff --name-only --diff-filter=AM

# Excluir eliminados (todos menos los borrados):
git diff --name-only --diff-filter=d   # minúscula = excluir

# Con --name-status (ver la letra de estado junto al nombre):
git diff --name-status --diff-filter=AMR HEAD~3
# Salida ejemplo:
# A  src/nuevo-componente.js    ← añadido
# M  src/auth.js                ← modificado
# R  src/utils.js → src/helpers.js  ← renombrado


# ============================================
# CAMBIAR EL ALGORITMO DE DIFF
# ============================================

# patience: mejor para código muy refactorizado
git diff --diff-algorithm=patience

# histogram: aún mejor para muchos cambios
git diff --diff-algorithm=histogram
```

**Casos de uso reales:** [🔙](#4-git-diff---comparando-cambios)

```bash
# ─────────────────────────────────────────────────────────────────
# CASO 1: Rutina antes de commitear
# ─────────────────────────────────────────────────────────────────
# Antes de cada commit, revisa qué vas a incluir:
git diff           # Ver cambios NO stageados
git add .          # Stagear
git diff --staged  # Revisar exactamente qué va en el commit
git commit -m "feat: descripción de lo que realmente hice"


# ─────────────────────────────────────────────────────────────────
# CASO 2: Entender qué cambió en el remoto antes de hacer pull
# ─────────────────────────────────────────────────────────────────
git fetch origin
git diff HEAD origin/main --stat
# → Ver resumen de qué archivos cambiaron en el remoto
git diff HEAD origin/main
# → Ver el detalle del código


# ─────────────────────────────────────────────────────────────────
# CASO 3: Ver qué introdujo un commit específico
# ─────────────────────────────────────────────────────────────────
# Ver qué cambió en el commit abc1234:
git diff abc1234^ abc1234
# ó más fácil:
git show abc1234


# ─────────────────────────────────────────────────────────────────
# CASO 4: Comparar tu feature con main para una PR
# ─────────────────────────────────────────────────────────────────
# Estás en feature/checkout. Quieres ver qué cambias respecto a main:
git diff main...feature/checkout --stat
# → Solo los cambios de tu feature (sin lo que main avanzó)
git diff main...feature/checkout
# → Todo el código que cambiaste en la feature


# ─────────────────────────────────────────────────────────────────
# CASO 5: Encontrar cuándo se introdujo un cambio
# ─────────────────────────────────────────────────────────────────
# Ver la evolución de un archivo en los últimos 5 commits:
git diff HEAD~5 HEAD -- src/config.js
# → Todos los cambios en ese archivo desde 5 commits atrás


# ─────────────────────────────────────────────────────────────────
# CASO 6: Documentación - diff por palabras
# ─────────────────────────────────────────────────────────────────
# Modificaste un README. El diff normal muestra líneas enteras.
# Con --word-diff ves exactamente qué palabras cambiaron:
git diff --word-diff README.md
# Salida ejemplo:
# ## Instalación
# [-yarn install-]{+npm install+}   ← palabra "yarn" → "npm"


# ─────────────────────────────────────────────────────────────────
# CASO 7: Ver solo los archivos de un tipo de cambio concreto
# ─────────────────────────────────────────────────────────────────
# Hiciste una refactorización grande: renombraste archivos, añadiste
# otros y eliminaste algunos. Quieres ver solo los eliminados para
# asegurarte de que no borraste nada importante por error.
git diff HEAD~1 --name-only --diff-filter=D
# → Solo muestra los archivos que se eliminaron en el último commit

# O durante un merge con conflictos, saber cuáles faltan por resolver:
git diff --name-only --diff-filter=U
# → Lista SOLO los archivos que aún tienen conflictos sin resolver
```

**Entendiendo la salida de git diff:** [🔙](#4-git-diff---comparando-cambios)

```
@@ -10,7 +10,8 @@     ← encabezado del hunk
         contexto       ← líneas sin cambios (ayudan a orientarte)
-  línea eliminada      ← esta línea se quitó (fondo rojo)
+  línea añadida        ← esta línea se añadió (fondo verde)
+  otra línea añadida
         más contexto

El encabezado "@@ -10,7 +10,8 @@" significa:
  -10,7  → en el archivo ORIGINAL, empieza en línea 10 y muestra 7 líneas
  +10,8  → en el archivo NUEVO, empieza en línea 10 y muestra 8 líneas
           (8 en vez de 7 porque se añadió una línea)
```

**Mejores prácticas:** [🔙](#4-git-diff---comparando-cambios)

```bash
✓ Haz git diff antes de git add (revisa qué vas a stagear)
✓ Haz git diff --staged antes de git commit (revisa qué va en el commit)
✓ Usa --name-only ó --stat para una visión rápida sin entrar en detalles
✓ Usa --word-diff para comparar documentación o archivos de texto
✓ Usa ... (tres puntos) al comparar ramas para ver solo los cambios de la feature

✗ No confundas git diff (unstaged) con git diff --staged (en el staging)
✗ No uses git diff en vez de git show para ver qué introdujo un commit concreto
```

---

## Navegación

- [⬅️ Anterior: git status](03-git-status.md)
- [🏠 Volver al Índice](../../GIT_COMANDOS_GUIA_PRACTICA.md)
- [➡️ Siguiente: Referencias de Commits](04.1-referencias-commits.md)
