# 20. git clean - Limpiando Archivos No Rastreados

[🏠 Volver al Índice](../../GIT_COMANDOS_GUIA_PRACTICA.md)

---

## 20. git clean - Limpiando Archivos No Rastreados
[⬆️ Top](#20-git-clean---limpiando-archivos-no-rastreados)

**¿Qué hace?**
Elimina archivos no rastreados (untracked) del working directory. Son los archivos que Git "no conoce": los que nunca se han hecho `git add`, los que están en `.gitignore`, o los archivos de build generados automáticamente.

**⚠️ ADVERTENCIA: Lo que elimina `git clean` NO se puede recuperar con `git reflog` ni con ningún comando de Git. Una vez borrado, está borrado. Siempre usa `--dry-run` primero.**

**Funcionamiento interno:** [🔙](#20-git-clean---limpiando-archivos-no-rastreados)

```
Git clasifica los archivos en:

  TRACKED (rastreados):
    → Archivos que Git conoce (están en el último commit ó en staging)
    → Cambios en estos archivos: git reset ó git restore los descarta
    → git clean NO los toca

  UNTRACKED (no rastreados):
    → Archivos nuevos que nunca se han hecho git add
    → git clean -f los elimina

  IGNORED (ignorados):
    → Archivos que coinciden con patrones en .gitignore
    → git clean normal NO los toca
    → git clean -x ó -X los elimina (ver opciones)

git clean escanea el working directory, identifica los archivos
que caen en la categoría correspondiente, y los elimina del filesystem.
Los objetos Git (en .git/) no se tocan.
```

**Todas las opciones importantes:** [🔙](#20-git-clean---limpiando-archivos-no-rastreados)

```bash
# ============================================
# ⚠️ SIEMPRE empieza con dry-run para ver qué se eliminaría
# ============================================
# Situación: ANTES de eliminar nada, quieres saber exactamente
# qué archivos se verían afectados.
git clean -n
# ó equivalente:
git clean --dry-run
# → Muestra la lista de archivos que SE ELIMINARÍAN pero NO hace nada


# ============================================
# 1. -f / --force: eliminar archivos untracked
# ============================================
# Situación: Tienes archivos sueltos creados a mano o por scripts
# que no son parte del repo y quieres limpiarlos.
# Git requiere -f explícitamente como medida de seguridad.
git clean -f
# → Elimina archivos untracked (pero NO directorios ni ignorados)


# ============================================
# 2. -fd: eliminar archivos Y directorios untracked
# ============================================
# Situación: Creaste carpetas temporales o de prueba que no son
# parte del repo.
git clean -fd
# → Elimina archivos untracked Y directorios untracked
# → Ej: borra una carpeta "temp/" que creaste y nunca hiciste add


# ============================================
# 3. -fx / -fxd: eliminar incluyendo archivos ignorados
# ============================================
# Situación: Quieres un working directory COMPLETAMENTE limpio,
# incluyendo node_modules, archivos de build, .env locales, etc.
# Útil antes de un build "desde cero" (clean build).
git clean -fx    # Elimina untracked + ignorados (sin directorios)
git clean -fxd   # Elimina untracked + ignorados + directorios
# → PELIGRO: elimina node_modules, dist/, .env, etc.
# → Después necesitarás "npm install" u equivalente


# ============================================
# 4. -fX (mayúscula): eliminar SOLO los archivos ignorados
# ============================================
# Situación: Tienes archivos nuevos untracked que SÍ quieres conservar
# (trabajo en progreso que aún no commiteaste), pero quieres limpiar
# solo los artefactos de build que están en .gitignore.
git clean -fX    # Solo elimina archivos ignorados por .gitignore
git clean -fXd   # Solo elimina ignorados (archivos + directorios)
# → ¡Diferencia clave entre -x (minúscula) y -X (mayúscula)!
# → -x (minúscula): elimina untracked + ignorados (más agresivo)
# → -X (mayúscula): elimina SOLO ignorados (conserva los untracked)


# ============================================
# 5. -i / --interactive: modo interactivo
# ============================================
# Situación: Quieres decidir archivo por archivo qué eliminar
# sin hacer dry-run y luego confirmar ciegamente.
git clean -i
# → Muestra un menú interactivo:
#   What now> 1: clean, 2: filter by pattern, 3: select by numbers,
#              4: ask each, 5: quit, 6: help
# → Opción 4 "ask each": pregunta uno por uno si eliminar


# ============================================
# 6. -e / --exclude: excluir patrones específicos
# ============================================
# Situación: Quieres hacer clean general pero EXCEPTO ciertos archivos.
git clean -fd -e "*.log"           # Limpia todo MENOS archivos .log
git clean -fd -e node_modules      # Limpia todo MENOS node_modules
git clean -fxd -e .env.local       # Limpia TODO incluyendo ignorados, MENOS .env.local


# ============================================
# 7. -q / --quiet: modo silencioso
# ============================================
# Para usar en scripts donde no quieres output:
git clean -fdq
# → Elimina sin mostrar los nombres de archivos eliminados
```

**La diferencia crítica entre -x, -X y el comportamiento por defecto:** [🔙](#20-git-clean---limpiando-archivos-no-rastreados)

```bash
# Ejemplo de working directory:
#   nuevo-componente.js  → untracked (nuevo archivo, nunca git add)
#   dist/bundle.js       → ignorado por .gitignore
#   node_modules/        → ignorado por .gitignore

git clean -fn          # Sin flags extra:
# → would remove nuevo-componente.js
# No toca: dist/bundle.js, node_modules/ (están ignorados)

git clean -fnx         # Con -x (minúscula):
# → would remove nuevo-componente.js
# → would remove dist/bundle.js
# → would remove node_modules/  (¡ojo! MUY lento, borra todo)

git clean -fnX         # Con -X (MAYÚSCULA):
# → would remove dist/bundle.js
# NO toca nuevo-componente.js (porque -X conserva untracked)
```

**Casos de uso reales:** [🔙](#20-git-clean---limpiando-archivos-no-rastreados)

```bash
# ─────────────────────────────────────────────────────────────────
# CASO 1: Limpiar artefactos de build antes de un build limpio
# ─────────────────────────────────────────────────────────────────
# Situación: Los tests fallan extrañamente. Sospechas que hay archivos
# de build corruptos o desactualizados en dist/ o target/.
# Quieres hacer un build completamente desde cero.

# Paso 1: Ver qué se eliminaría
git clean -nX         # Solo ignorados (conserva tu código untracked)

# Paso 2: Si todo parece correcto, ejecutar:
git clean -fXd        # Elimina solo los archivos ignorados (dist/, *.class, etc.)

# Paso 3: Reconstruir
npm install && npm run build   # ó mvn clean install, gradle build, etc.


# ─────────────────────────────────────────────────────────────────
# CASO 2: Preparar directorio exactamente como en el repo (reset completo)
# ─────────────────────────────────────────────────────────────────
# Situación: Quieres que tu working directory esté EXACTAMENTE igual
# que el último commit, sin ningún cambio ni archivo extra.
# Útil para reproducir el comportamiento "limpio" del CI.

git reset --hard HEAD  # Descarta cambios en archivos tracked
git clean -fxd         # Elimina untracked + ignorados + directorios
# → Resultado: working directory idéntico al último commit
# ⚠️ Perderás archivos como .env.local y node_modules


# ─────────────────────────────────────────────────────────────────
# CASO 3: Limpiar solo los archivos ignorados, conservar tu trabajo
# ─────────────────────────────────────────────────────────────────
# Situación: Tienes archivos nuevos sin commitear (código en progreso)
# y quieres limpiar solo los archivos de build sin tocar tu código.

git clean -n           # Primero ves qué hay untracked
git clean -nX          # Ves qué ignorados se limpiarían
git clean -fXd         # Solo limpias ignorados (dist/, *.pyc, etc.)
# → Tu código untracked queda intacto


# ─────────────────────────────────────────────────────────────────
# CASO 4: En pipeline de CI - limpiar entre builds sin hacer clone
# ─────────────────────────────────────────────────────────────────
# Para reutilizar el workspace entre builds sin hacer clone completo:
git checkout main
git pull
git clean -fxd         # Limpia todo lo que no es del repo
npm install            # Reinstala dependencias limpias
npm test
```

**git clean vs git restore / git reset:** [🔙](#20-git-clean---limpiando-archivos-no-rastreados)

```bash
# ─────────────────────────────────────────────────────────────────
# git restore / git reset → para archivos TRACKED
# ─────────────────────────────────────────────────────────────────
git restore .          # Descarta cambios en archivos rastreados
git reset --hard HEAD  # Descarta cambios en tracked + resetea staging

# ─────────────────────────────────────────────────────────────────
# git clean → para archivos UNTRACKED e IGNORED
# ─────────────────────────────────────────────────────────────────
git clean -fd          # Elimina archivos/dirs untracked
git clean -fxd         # Elimina untracked + ignorados

# ─────────────────────────────────────────────────────────────────
# RESET COMPLETO DEL WORKING DIRECTORY:
# ─────────────────────────────────────────────────────────────────
git reset --hard HEAD  # Paso 1: archivos tracked
git clean -fxd         # Paso 2: archivos untracked e ignorados
# Combinados: working directory idéntico al commit HEAD
```

**Alternativa más segura - git stash:** [🔙](#20-git-clean---limpiando-archivos-no-rastreados)

```bash
# Cuando NO estás seguro y quieres poder recuperar:
git stash -u           # Guarda los untracked también
# Trabaja, prueba, etc.
git stash pop          # Recupera los archivos si los necesitas

# vs:
git clean -fd          # ⚠️ IRREVERSIBLE
```

**Troubleshooting:** [🔙](#20-git-clean---limpiando-archivos-no-rastreados)

```bash
# ─────────────────────────────────────────────────────────────────
# Problema 1: clean no elimina lo que esperaba
# ─────────────────────────────────────────────────────────────────
# Causa A: Los archivos están en .gitignore → usa -x para incluirlos
# Causa B: Los archivos están en staging → clean no toca staged files
#          usa: git restore --staged . para sacarlos del staging primero
# Causa C: Son directorios → añade -d para incluirlos


# ─────────────────────────────────────────────────────────────────
# Problema 2: Borré archivos por error con git clean
# ─────────────────────────────────────────────────────────────────
# Si los archivos eran TRACKED (ya commiteados):
git restore .           # Restaura archivos tracked
# Si los archivos eran UNTRACKED (nunca en Git):
# ❌ No se pueden recuperar con Git
# → Buscar en la papelera del sistema operativo
# → Buscar en backups del IDE (IntelliJ, VS Code tienen historial local)


# ─────────────────────────────────────────────────────────────────
# Problema 3: "error: clean.requireForce = true"
# ─────────────────────────────────────────────────────────────────
# Git requiere -f por seguridad. Siempre debes añadir -f:
git clean -f   # o -fd, -fxd, etc.
```

**Mejores prácticas:** [🔙](#20-git-clean---limpiando-archivos-no-rastreados)

```bash
✓ SIEMPRE haz dry-run primero: git clean -n
✓ Usa -i (interactivo) cuando no estás seguro de qué eliminar
✓ Prefiere -X (solo ignorados) sobre -x (agresivo) cuando tengas trabajo sin commitear
✓ Si no estás seguro, usa git stash -u en su lugar (recuperable)

✗ NUNCA hagas git clean sin hacer dry-run antes
✗ No uses -x sin entender que borrará node_modules y dist/
✗ No confundas -x (elimina todo untracked + ignorados) con -X (solo ignorados)
✗ No asumas que puedes recuperar lo borrado: en Git no hay forma
```

---

## Navegación

- [⬅️ Anterior: git cherry-pick](19-git-cherry-pick.md)
- [🏠 Volver al Índice](../../GIT_COMANDOS_GUIA_PRACTICA.md)
- [➡️ Siguiente: git rm y git mv](21-git-rm-mv.md)
