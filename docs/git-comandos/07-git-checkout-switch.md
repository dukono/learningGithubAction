# 7. git checkout / git switch - Navegando el Código

[🏠 Volver al Índice](../../GIT_COMANDOS_GUIA_PRACTICA.md)

---

## 7. git checkout / git switch - Navegando el Código
[⬆️ Top](#tabla-de-contenidos)

**¿Qué hace?**
Cambia de rama, navega por commits históricos, o restaura archivos del working directory. Es uno de los comandos más versátiles (y confusos) de Git, por eso se dividió en `git switch` (ramas) y `git restore` (archivos) en versiones modernas.

> **📝 NOTA IMPORTANTE:** Esta sección cubre **tres comandos diferentes**:
> - **git switch** (moderno) - Para cambiar de rama
> - **git restore** (moderno) - Para restaurar archivos
> - **git checkout** (legacy) - Hace ambas cosas (confuso)
>
> **Recomendación:** Usa `git switch` para ramas y `git restore` para archivos.

**Funcionamiento interno:** [🔙](#7-git-checkout--git-switch---navegando-el-código)

```
Al cambiar de rama:
1. Verifica que no haya conflictos con working directory
2. Lee hash del commit de la rama destino desde .git/refs/heads/rama
3. Lee tree object del commit destino
4. Compara tree actual con tree destino
5. Actualiza archivos en working directory (solo los diferentes)
6. Actualiza .git/index (staging area)
7. Actualiza .git/HEAD → ref: refs/heads/rama-destino
8. Si hay conflictos, aborta y muestra errores

Al checkout de archivo:
1. Lee archivo desde tree object del commit especificado
2. Sobrescribe archivo en working directory
3. Actualiza staging area con esa versión
4. NO cambia HEAD

Al checkout de commit (detached HEAD):
1. Similar a cambio de rama
2. Pero HEAD apunta directamente a commit (no a rama)
3. .git/HEAD contiene hash en vez de ref
4. Commits nuevos quedan "huérfanos" al cambiar
```

---

### 7.1. git switch - Cambiar de Rama (Moderno, Recomendado)

**Uso práctico - Cambiar entre ramas:** [🔙](#7-git-checkout--git-switch---navegando-el-código)

```bash
# 1. Cambiar a rama existente
git switch main
git switch feature-x

# 2. Crear rama nueva y cambiar a ella
git switch -c nueva-rama
# → Equivalente a: git branch nueva-rama && git switch nueva-rama

# 3. Crear rama desde commit específico
git switch -c hotfix abc123
git switch -c bugfix HEAD~3

# 4. Volver a rama anterior
git switch -
# → Alterna entre dos ramas rápidamente
# → Como "cd -" en bash

# 5. Crear y cambiar con tracking automático
git switch -c feature-x --track origin/feature-x
# → Configura upstream automáticamente

# 6. Forzar cambio (descarta cambios locales)
git switch -f otra-rama
# → ⚠️ Pierdes cambios no commiteados

# 7. Cambiar con merge de cambios locales
git switch -m otra-rama
# → Intenta mergear cambios locales a nueva rama

# 8. Cambiar a rama remota (crea local tracking)
git switch feature-x
# → Si no existe local pero sí origin/feature-x
# → Crea local automáticamente con tracking
```

**Uso práctico - Detached HEAD con switch:** [🔙](#7-git-checkout--git-switch---navegando-el-código)

```bash
# Entrar en Detached HEAD
git switch --detach abc123
git switch --detach HEAD~3
git switch --detach v1.0.0

# → HEAD apunta directamente a commit (no a rama)
# → Útil para inspección, no para desarrollo
```

---

### 7.2. git restore - Restaurar Archivos (Moderno, Recomendado)

**Uso práctico - Descartar cambios:** [🔙](#7-git-checkout--git-switch---navegando-el-código)

```bash
# 1. Descartar cambios en working directory
git restore file.txt
# → Restaura desde staging (o HEAD si no está staged)

# 2. Descartar todos los cambios
git restore .
# → Restaura todos los archivos modificados

# 3. Unstage archivo (quitar de staging)
git restore --staged file.txt
# → Mueve de staging a working directory
# → Equivalente a: git reset HEAD file.txt

# 4. Unstage y descartar cambios
git restore --staged --worktree file.txt
# → Quita de staging Y descarta cambios

# 5. Restaurar desde commit específico
git restore --source=abc123 file.txt
git restore --source=HEAD~3 file.txt
git restore --source=main file.txt

# 6. Restaurar archivo borrado
git restore deleted-file.txt
# → Solo si estaba tracked antes

# 7. Restaurar con patrón
git restore '*.js'
git restore 'src/**/*.txt'

# 8. Restaurar directorio completo
git restore src/
```

**Uso práctico - Casos especiales:** [🔙](#7-git-checkout--git-switch---navegando-el-código)

```bash
# Restaurar archivo de otra rama sin cambiar de rama
git restore --source=feature-x -- config.json

# Restaurar múltiples archivos de commit antiguo
git restore --source=HEAD~5 -- file1.txt file2.txt

# Ver qué se restauraría sin hacerlo (no existe, usa diff)
git diff file.txt  # Ver cambios antes de restaurar
git restore file.txt
```

---

### 7.3. git checkout - Comando Legacy (Multiuso)

**Comparación de sintaxis:**

```bash
# ============================================
# CHECKOUT (Multiuso, confuso - comando legacy)
# ============================================
git checkout main               # Cambiar de rama
git checkout -b nueva           # Crear y cambiar
git checkout abc123             # Ir a commit (detached HEAD)
git checkout -- file.txt        # Descartar cambios de archivo
git checkout abc123 file.txt    # Restaurar archivo desde commit
git checkout tags/v1.0.0        # Checkout de tag

# PROBLEMA: ¿checkout cambia rama o restaura archivo?
# → Sintaxis ambigua, fácil confundirse
# → Por eso se crearon switch y restore

# ============================================
# EQUIVALENCIAS: checkout → switch/restore
# ============================================

# Cambiar de rama:
git checkout main          →  git switch main
git checkout -b nueva      →  git switch -c nueva
git checkout -            →  git switch -

# Descartar cambios:
git checkout -- file.txt   →  git restore file.txt
git checkout -- .          →  git restore .

# Restaurar desde commit:
git checkout abc123 file.txt  →  git restore --source=abc123 file.txt

# Ir a commit:
git checkout abc123        →  git switch --detach abc123

# Restaurar desde otra rama:
git checkout main file.txt →  git restore --source=main file.txt
```

**Uso de checkout (si usas Git < 2.23):**

```bash
# Cambiar de rama
git checkout main
git checkout feature-x

# Crear y cambiar
git checkout -b nueva-rama
git checkout -b hotfix abc123

# Descartar cambios (IMPORTANTE: usa --)
git checkout -- file.txt
git checkout -- .

# Restaurar desde commit
git checkout abc123 -- file.txt
git checkout HEAD~3 -- file.txt

# Restaurar desde otra rama (sin cambiar)
git checkout feature-x -- src/lib.js

# Detached HEAD
git checkout abc123
git checkout v1.0.0
git checkout HEAD~5

# PROBLEMA con checkout:
git checkout rama           # ¿Cambia rama?
git checkout -- rama        # ¿O restaura archivo llamado "rama"?
# → Ambigüedad confusa, por eso switch/restore son mejores
```

---

### Detached HEAD - Explicación Completa

**¿Qué es Detached HEAD?**

```bash
# Estado normal (HEAD apunta a rama):
.git/HEAD contiene: ref: refs/heads/main
→ HEAD → main → commit abc123

# Detached HEAD (HEAD apunta a commit directamente):
.git/HEAD contiene: abc123
→ HEAD → commit abc123 (sin rama)

# Problema: Commits en detached HEAD quedan "huérfanos"
# Si cambias a otra rama, pierdes referencia a esos commits
```

**Entrar en Detached HEAD:**

```bash
# Con switch (moderno):
git switch --detach abc123
git switch --detach HEAD~3
git switch --detach v1.0.0

# Con checkout (legacy):
git checkout abc123
git checkout HEAD~5
git checkout v1.0.0
git checkout tags/v1.0.0
```

**¿Por qué usar Detached HEAD?**

```bash
✓ Inspeccionar código antiguo sin crear rama
✓ Probar build de versión específica
✓ Reproducir bug histórico
✓ Auditar cambios
✓ Ejecutar tests en commit específico

✗ NO para desarrollo (commits se pierden fácilmente)
✗ NO para trabajo que quieres guardar
```

**Salir de Detached HEAD:**

```bash
# Opción 1: Volver a rama (descarta trabajo en detached)
git switch main
# → Commits hechos en detached quedan sin referencia

# Opción 2: Crear rama con el trabajo (RECOMENDADO)
git switch -c nueva-rama
# → Convierte trabajo en rama permanente

# Opción 3: Crear rama apuntando a donde estás
git branch rescue-branch
git switch main
# → rescue-branch guarda tu trabajo
```

**Recuperar trabajo perdido en Detached HEAD:**

```bash
# Si saliste de detached sin crear rama:
git reflog
# Busca el commit donde estabas
git switch -c recuperar abc123
# o
git checkout -b recuperar abc123
```

**Ver si estás en Detached HEAD:**

```bash
git branch
# * (HEAD detached at abc123)  ← En detached
# * main                        ← En rama normal

git status
# HEAD detached at abc123       ← En detached
# On branch main                ← En rama normal
```

---

### Casos de Uso Avanzados

**Caso 1: Olvidé cambiar de rama antes de trabajar** [🔙](#7-git-checkout--git-switch---navegando-el-código)

```bash
# Estás en main, hiciste cambios, querías estar en feature
git stash
git switch feature-x
git stash pop

# o (con merge automático):
git switch -m feature-x
```

**Caso 2: Quiero archivo de otra rama sin cambiar**

```bash
# Con restore (moderno):
git restore --source=feature-x -- src/lib.js

# Con checkout (legacy):
git checkout feature-x -- src/lib.js
```

**Caso 3: Explorar bug histórico**

```bash
git log --oneline | grep "bug aparece"
# Encuentra commit: abc123

git switch --detach abc123
npm test  # Reproduce el bug
git switch main  # Vuelve a main
```

**Caso 4: Recuperar archivo borrado hace commits**

```bash
git log --oneline --all --full-history -- deleted-file.txt
# Encuentra último commit: def456

git restore --source=def456 -- deleted-file.txt
```

**Caso 5: Crear hotfix desde tag de producción**

```bash
git switch -c hotfix/critical v1.2.3
# ... fix ...
git commit -am "fix: Critical issue"
```

---

### Troubleshooting

**Problema 1: No puedo cambiar (cambios sin commitear)**

```bash
Solución A: Commitear
git add .
git commit -m "WIP"
git switch otra-rama

Solución B: Stash
git stash
git switch otra-rama
git stash pop

Solución C: Switch con merge
git switch -m otra-rama

Solución D: Forzar (⚠️ pierdes cambios)
git switch -f otra-rama
```

**Problema 2: Hice commits en Detached HEAD**

```bash
git reflog
# Encuentra el commit: abc123
git switch -c rescue-branch abc123
```

**Problema 3: Archivo y rama con mismo nombre**

```bash
# Moderno (sin ambigüedad):
git switch test          # Definitivamente rama
git restore test         # Definitivamente archivo

# Legacy (ambiguo):
git checkout test        # ¿Rama o archivo?
git checkout -- test     # Fuerza archivo
```

**Problema 4: Cambié de rama y perdí trabajo**

```bash
git reflog
git switch -c recuperar HEAD@{1}
# o
git checkout -b recuperar HEAD@{1}
```

---

### Mejores Prácticas

[🔙](#7-git-checkout--git-switch---navegando-el-código)

```bash
✓ Usa git switch para cambiar ramas (claro y específico)
✓ Usa git restore para archivos (sin ambigüedad)
✓ Commitea o stash antes de cambiar ramas
✓ Entiende detached HEAD antes de usarlo
✓ Crea rama desde detached si hiciste commits
✓ Usa git switch - para alternar entre dos ramas

✗ Evita git checkout (confuso y ambiguo)
✗ No trabajes en detached HEAD sin crear rama después
✗ No uses git checkout sin "--" para archivos
✗ No confundas switch (ramas) con restore (archivos)
✗ No asumas que checkout siempre cambia ramas
```

---


---

## Navegación

- [⬅️ Anterior: git branch](06-git-branch.md)
- [🏠 Volver al Índice](../../GIT_COMANDOS_GUIA_PRACTICA.md)
- [➡️ Siguiente: git merge](08-git-merge.md)

