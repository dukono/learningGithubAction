# 21. git rm y git mv - Eliminando y Moviendo Archivos

[🏠 Volver al Índice](../../GIT_COMANDOS_GUIA_PRACTICA.md)

---

## 21. git rm y git mv - Eliminando y Moviendo Archivos
[⬆️ Top](#21-git-rm-y-git-mv---eliminando-y-moviendo-archivos)

**¿Qué hacen?**
- `git rm`: Elimina archivos del working directory Y del tracking de Git (staging area).
- `git mv`: Mueve o renombra archivos, notificando a Git del cambio.

La diferencia con el `rm` y `mv` del sistema operativo es que Git se entera del cambio: con `git rm/mv` el cambio queda preparado para el próximo commit, sin pasos extra.

**Funcionamiento interno:** [🔙](#21-git-rm-y-git-mv---eliminando-y-moviendo-archivos)

```
git rm archivo.txt:
  1. Elimina el archivo del filesystem (working directory)
  2. Actualiza .git/index (staging area) marcando el archivo como eliminado
  3. El cambio queda en staging, listo para commitear
  → Al commitear, el archivo desaparece del árbol del repositorio

git rm --cached archivo.txt:
  1. Elimina SOLO la referencia en .git/index
  2. El archivo PERMANECE en disco
  3. Pasa a ser "untracked" (Git ya no lo sigue)
  → Útil para "deshacer" un git add, o para dejar de trackear algo

git mv viejo.txt nuevo.txt:
  Internamente hace exactamente esto:
  1. git rm viejo.txt       → elimina el viejo nombre del index
  2. git add nuevo.txt      → añade el nuevo nombre al index
  3. Git detecta automáticamente que es un rename (si el contenido es similar)
  → Al commitear, el historial muestra el rename correctamente
```

**Todas las opciones de git rm:** [🔙](#21-git-rm-y-git-mv---eliminando-y-moviendo-archivos)

```bash
# ============================================
# 1. Eliminar archivo (del disco Y de Git)
# ============================================
# Situación: Borraste lógicamente un archivo y quieres que Git también
# sepa que fue eliminado para incluirlo en el próximo commit.
git rm archivo.txt
# → Archivo desaparece del disco
# → Staged como eliminado
# → Necesitas hacer git commit después


# ============================================
# 2. --cached: eliminar solo de Git (mantener en disco)
# ============================================
# Situación: Commitaste accidentalmente un archivo que no debería
# estar en el repo (.env, node_modules, archivo con contraseñas...).
# Quieres que Git deje de rastrearlo, pero el archivo sigue siendo
# útil en tu máquina.
git rm --cached .env
# → Git deja de rastrear .env
# → El archivo .env sigue en tu disco
# → Necesitas añadir .env al .gitignore después

# Para un directorio entero:
git rm --cached -r node_modules/
git rm --cached -r .idea/


# ============================================
# 3. -r: eliminar directorio recursivamente
# ============================================
# Situación: Quieres eliminar una carpeta entera del repo.
git rm -r carpeta/
# → Elimina carpeta/ y todo su contenido del disco y de Git
git rm -r --cached .idea/
# → Elimina .idea/ de Git pero lo conserva en disco


# ============================================
# 4. -f / --force: forzar eliminación
# ============================================
# Situación: El archivo tiene cambios sin commitear y Git se niega
# a eliminarlo como medida de seguridad.
git rm -f archivo-modificado.txt
# ⚠️ Usa con cuidado: perderás los cambios no commiteados


# ============================================
# 5. -n / --dry-run: ver qué se eliminaría sin hacerlo
# ============================================
# Situación: Quieres verificar qué archivos afectarías antes de eliminar.
git rm -n "*.log"
git rm --dry-run -r logs/
# → Muestra los archivos que SE ELIMINARÍAN sin eliminar nada


# ============================================
# 6. Con wildcards y patrones
# ============================================
# Eliminar todos los archivos .log:
git rm "*.log"        # Comillas importantes para evitar expansión del shell
git rm log/**/*.log   # Archivos .log en subdirectorios de log/
```

**Todas las opciones de git mv:** [🔙](#21-git-rm-y-git-mv---eliminando-y-moviendo-archivos)

```bash
# ============================================
# 1. Renombrar un archivo
# ============================================
# Situación: Quieres renombrar un archivo y que Git lo detecte
# como rename en el historial (no como borrar + crear).
git mv viejo-nombre.js nuevo-nombre.js
# → viejo-nombre.js desaparece
# → nuevo-nombre.js aparece con el mismo contenido
# → Staged como rename, el historial muestra continuidad


# ============================================
# 2. Mover a un directorio diferente
# ============================================
git mv archivo.js src/components/
# → Mueve el archivo a src/components/archivo.js

# Mover múltiples archivos a un directorio:
git mv utils.js helpers.js src/lib/
# → Ambos archivos van a src/lib/


# ============================================
# 3. Renombrar un directorio
# ============================================
git mv old-folder/ new-folder/
# → Renombra el directorio y todo su contenido


# ============================================
# 4. -f: forzar (sobrescribir si el destino ya existe)
# ============================================
git mv -f archivo.js destino-existente.js
# ⚠️ Si destino-existente.js ya existía, lo sobreescribe


# ============================================
# 5. Case-sensitive rename (problema en macOS/Windows)
# ============================================
# Situación: Quieres renombrar "readme.md" a "README.md" pero
# en macOS/Windows el filesystem no distingue mayúsculas y
# git mv readme.md README.md falla silenciosamente.
#
# Solución: usar un nombre temporal intermedio
git mv readme.md temp-readme.md
git mv temp-readme.md README.md
git commit -m "docs: Fix README capitalization"
```

**Casos de uso reales:** [🔙](#21-git-rm-y-git-mv---eliminando-y-moviendo-archivos)

```bash
# ─────────────────────────────────────────────────────────────────
# CASO 1: Eliminar un archivo sensible que se commitó por error
# ─────────────────────────────────────────────────────────────────
# Commitaste accidentalmente el archivo .env con contraseñas.
# Necesitas eliminarlo del repo Y añadirlo al .gitignore.

# Paso 1: Eliminar de Git pero NO del disco (lo necesitas localmente)
git rm --cached .env

# Paso 2: Asegurarte de que nunca se vuelva a añadir
echo ".env" >> .gitignore
git add .gitignore

# Paso 3: Commitear la eliminación
git commit -m "chore: remove .env from tracking, add to gitignore"

# Paso 4 (CRÍTICO si ya lo pusheaste): El archivo sigue en el historial.
# Si tenía contraseñas reales, debes cambiarlas AHORA porque siguen
# accesibles en el historial. Para limpiar el historial completo
# necesitarías git filter-branch o BFG Repo Cleaner.


# ─────────────────────────────────────────────────────────────────
# CASO 2: Reorganizar estructura de directorios del proyecto
# ─────────────────────────────────────────────────────────────────
# El proyecto creció y necesitas reorganizar las carpetas.
# Antes:
#   helpers.js, utils.js, config.js (todos en raíz)
# Después:
#   src/utils/helpers.js, src/utils/utils.js, src/config/config.js

git mv helpers.js src/utils/helpers.js
git mv utils.js src/utils/utils.js
git mv config.js src/config/config.js
git commit -m "refactor: reorganize project structure into src/"
# → El historial de cada archivo se preserva (git log --follow)


# ─────────────────────────────────────────────────────────────────
# CASO 3: Dejar de trackear node_modules o carpeta de IDE
# ─────────────────────────────────────────────────────────────────
# El proyecto no tenía .gitignore y se commiteó node_modules o .idea/

# Ver el tamaño del problema:
git ls-files node_modules/ | wc -l   # Cuántos archivos hay trackeados

# Eliminar de Git (sin borrar del disco):
git rm -r --cached node_modules/
git rm -r --cached .idea/

# Actualizar .gitignore:
echo "node_modules/" >> .gitignore
echo ".idea/" >> .gitignore
git add .gitignore

git commit -m "chore: stop tracking node_modules and .idea"
# → El repo deja de incluir esas carpetas
# → Las carpetas siguen en tu disco para que funcione el proyecto


# ─────────────────────────────────────────────────────────────────
# CASO 4: Eliminar archivos de build generados automáticamente
# ─────────────────────────────────────────────────────────────────
# Tienes dist/ o target/ commiteado y quieres eliminarlo del repo.

git rm -r --cached dist/
echo "dist/" >> .gitignore
git add .gitignore
git commit -m "chore: remove dist/ from tracking"


# ─────────────────────────────────────────────────────────────────
# CASO 5: Preservar el historial de un archivo renombrado
# ─────────────────────────────────────────────────────────────────
# Si haces mv + rm + add (manualmente), git log no muestra el historial previo.
# Con git mv, el historial se preserva:

git mv auth.js authentication.js
git commit -m "refactor: rename auth.js to authentication.js"

# Ver el historial completo (incluyendo antes del rename):
git log --follow authentication.js
# → Muestra commits de antes y después del rename
```

**rm del shell vs git rm:** [🔙](#21-git-rm-y-git-mv---eliminando-y-moviendo-archivos)

```bash
# ─────────────────────────────────────────────────────────────────
# CON rm del shell (2 pasos):
# ─────────────────────────────────────────────────────────────────
rm archivo.txt              # Elimina del disco
git add archivo.txt         # ó git add -u   → notifica a Git de la eliminación
# → Dos pasos, pero el resultado es el mismo

# ─────────────────────────────────────────────────────────────────
# CON git rm (1 paso más claro):
# ─────────────────────────────────────────────────────────────────
git rm archivo.txt          # Elimina del disco Y notifica a Git
# → Un solo paso, más explícito

# ─────────────────────────────────────────────────────────────────
# CON mv del shell (3 pasos):
# ─────────────────────────────────────────────────────────────────
mv viejo.js nuevo.js        # Mueve en disco
git rm viejo.js             # Notifica eliminación del viejo
git add nuevo.js            # Notifica creación del nuevo
# → Git detecta que es un rename si el contenido es similar (>50% igual)

# ─────────────────────────────────────────────────────────────────
# CON git mv (1 paso):
# ─────────────────────────────────────────────────────────────────
git mv viejo.js nuevo.js    # Mueve en disco + notifica rename a Git
# → Un solo paso
# → Git siempre detecta correctamente el rename
```

**Troubleshooting:** [🔙](#21-git-rm-y-git-mv---eliminando-y-moviendo-archivos)

```bash
# ─────────────────────────────────────────────────────────────────
# Problema 1: "error: the following file has local modifications"
# ─────────────────────────────────────────────────────────────────
# Git te protege de borrar cambios no commiteados
# Solución A: Commitea primero, luego rm
# Solución B: Usa -f para forzar (perderás los cambios):
git rm -f archivo.txt


# ─────────────────────────────────────────────────────────────────
# Problema 2: Hice git rm por error, quiero recuperar el archivo
# ─────────────────────────────────────────────────────────────────
# Si todavía no has commiteado:
git restore archivo.txt     # Recupera el archivo del último commit
# ó:
git checkout HEAD archivo.txt

# Si ya commiteaste el rm:
git revert HEAD             # Crea un commit que deshace el rm
# ó para recuperar solo ese archivo de un commit anterior:
git checkout HEAD~1 -- archivo.txt


# ─────────────────────────────────────────────────────────────────
# Problema 3: git mv falla en macOS/Windows (case-sensitive)
# ─────────────────────────────────────────────────────────────────
# "rename readme.md → README.md" falla porque el sistema no distingue
# Solución: nombre temporal intermedio
git mv readme.md tmp_readme.md && git mv tmp_readme.md README.md


# ─────────────────────────────────────────────────────────────────
# Problema 4: El historial no sigue el archivo renombrado
# ─────────────────────────────────────────────────────────────────
git log --follow --oneline -- nuevo-nombre.js
# --follow: sigue el historial antes y después del rename
```

**Mejores prácticas:** [🔙](#21-git-rm-y-git-mv---eliminando-y-moviendo-archivos)

```bash
✓ Usa git rm --cached para dejar de trackear sin borrar del disco
✓ Usa git mv en vez de mv del shell para preservar el historial de renames
✓ Haz git add .gitignore después de git rm --cached para que no vuelva
✓ Usa --dry-run (-n) para verificar qué archivos afectarías antes de borrar

✗ NUNCA hagas rm -rf .git (destruye todo el repositorio)
✗ No uses git rm -f sin revisar qué tienes pendiente de commitear
✗ No olvides commitear después de git rm / git mv
✗ Si eliminas un archivo con secretos que ya está en el historial, recuerda
  que el historial pasado sigue siendo accesible - cambia las contraseñas
```

---

## Navegación

- [⬅️ Anterior: git clean](20-git-clean.md)
- [🏠 Volver al Índice](../../GIT_COMANDOS_GUIA_PRACTICA.md)
- [➡️ Siguiente: Referencias y Placeholders](22-referencias-placeholders.md)
