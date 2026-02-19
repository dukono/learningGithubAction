# 9. git rebase - Reescribiendo Historia

[🏠 Volver al Índice](../../GIT_COMANDOS_GUIA_PRACTICA.md)

---

## 9. git rebase - Reescribiendo Historia
[⬆️ Top](#9-git-rebase---reescribiendo-historia)

**¿Qué hace?**
Reaplica commits de una rama encima de otra, reescribiendo la historia. Es como "mover" tus commits a otro punto de partida, haciendo que parezca que siempre trabajaste desde ahí.

**Funcionamiento interno:** [🔙](#9-git-rebase---reescribiendo-historia)

```
ANTES del rebase:
  main:    A---B---C
                \
  feature:       D---E---F

  Situación: quieres que feature parta de C (el último de main),
  pero cuando la creaste, main solo tenía B.

DESPUÉS del rebase (git rebase main estando en feature):
  main:    A---B---C
                    \
  feature:           D'--E'--F'

  Git hace internamente:
  1. Encuentra el ancestro común (B)
  2. Guarda los commits únicos de feature (D, E, F) como patches temporales
  3. Mueve el puntero de feature a C (último de main)
  4. Aplica cada patch uno a uno → crea D', E', F' (nuevos hashes)
  5. Los commits originales D, E, F ya no son accesibles (pero sí desde reflog)

  → Historia lineal: parece que siempre trabajaste desde C
  → Los hashes cambian (D ≠ D', E ≠ E', F ≠ F')
  → Si ya habías pusheado D, E, F, necesitarás --force-with-lease
```

**Todas las opciones importantes:** [🔙](#9-git-rebase---reescribiendo-historia)

```bash
# ============================================
# 1. Rebase básico sobre otra rama
# ============================================
# Situación: Llevas varios días en feature/login y main ha avanzado.
# Quieres incorporar los cambios de main en tu rama antes del merge.
#
# Estás en feature/login:
git rebase main
# → Tus commits se reaplican encima del último commit de main
# → Si hay conflictos, Git para y te pide resolverlos


# ============================================
# 2. Rebase interactivo (el más poderoso)
# ============================================
# Situación: Antes de hacer PR, tienes 8 commits tipo "wip", "fix typo",
# "arreglo temporal"... y quieres presentar un historial limpio.
git rebase -i HEAD~5
# → Abre editor con los últimos 5 commits
# → Puedes reorganizarlos, fusionarlos, cambiar mensajes, eliminar...

# Opciones disponibles en el editor interactivo:
# ─────────────────────────────────────────────
# pick   abc1234 feat: login form      → Usar el commit tal cual
# reword abc1234 feat: login form      → Usar pero cambiar el mensaje
# edit   abc1234 feat: login form      → Pausar para modificar el commit
# squash abc1234 feat: login form      → Fusionar con el anterior (combina mensajes)
# fixup  abc1234 feat: login form      → Fusionar con el anterior (descarta este mensaje)
# drop   abc1234 feat: login form      → Eliminar este commit completamente
# exec   npm test                      → Ejecutar comando después de este commit

# Ejemplo: tienes estos commits:
# pick a1b2c3 feat: add login form
# pick d4e5f6 wip
# pick g7h8i9 fix typo
# pick j0k1l2 more fixes
# pick m3n4o5 feat: login validation
#
# Resultado que quieres: 2 commits limpios
# pick a1b2c3 feat: add login form
# squash d4e5f6 wip
# squash g7h8i9 fix typo
# squash j0k1l2 more fixes
# pick m3n4o5 feat: login validation


# ============================================
# 3. --onto: mover rama a una base diferente
# ============================================
# Situación: Creaste feature-b A PARTIR DE feature-a por error,
# pero en realidad feature-b debía salir de main.
#
# Antes:
#   main:      A---B
#   feature-a:      C---D
#   feature-b:           E---F  ← creada desde feature-a
#
# Quieres:
#   main:      A---B
#   feature-a:      C---D
#   feature-b:  B---E'--F'  ← ahora parte de main
#
git rebase --onto main feature-a feature-b
# Sintaxis: git rebase --onto <nueva-base> <desde-donde> <hasta-donde>
# → feature-b ahora parte de main, sin incluir los commits de feature-a


# ============================================
# 4. --autosquash: squash automático por mensajes
# ============================================
# Situación: Estás en mitad de feature/payment y descubres un bug.
# Haces un commit de fix y lo marcas para que se fusione automáticamente:
git commit -m "fixup! feat: add payment form"
# ó
git commit -m "squash! feat: add payment form"

# Luego, al hacer rebase interactivo:
git rebase -i --autosquash main
# → Git automáticamente reorganiza y marca ese commit como fixup/squash
# → No necesitas editar manualmente el editor


# ============================================
# 5. --autostash: stash automático
# ============================================
# Situación: Tienes cambios en working directory sin commitear
# y quieres hacer rebase sin perderlos.
git rebase --autostash main
# → Hace stash automáticamente antes del rebase
# → Aplica el stash al terminar
# → Sin esto, Git te daría error si tienes cambios sin commitear


# ============================================
# 6. --exec: ejecutar comando tras cada commit
# ============================================
# Situación: Quieres asegurarte de que los tests pasan
# en CADA commit intermedio (útil antes de merge a main).
git rebase -i --exec "npm test" HEAD~5
# → Tras aplicar cada commit, ejecuta npm test
# → Si falla, el rebase se pausa para que puedas arreglar


# ============================================
# 7. Continuar, saltar o abortar tras conflicto
# ============================================
# Cuando hay conflicto durante el rebase, Git para y muestra:
# "CONFLICT (content): Merge conflict in archivo.js"

# Paso 1: Ver qué archivos tienen conflicto
git status

# Paso 2: Editar los archivos y resolver los conflictos
# (eliminar los marcadores <<<<<<, ======, >>>>>>>)

# Paso 3: Marcar como resuelto
git add archivo-resuelto.js

# Paso 4: Continuar
git rebase --continue

# Si el conflicto de este commit no tiene sentido y quieres saltarlo:
git rebase --skip

# Si quieres cancelar todo el rebase y volver al estado inicial:
git rebase --abort


# ============================================
# 8. --update-refs: actualizar ramas dependientes (Git 2.38+)
# ============================================
# Situación: Tienes varias ramas apiladas (feature-a → feature-b → feature-c)
# y rebases main. Quieres que todas las ramas intermedias se actualicen.
git rebase --update-refs main
# → Actualiza automáticamente todas las ramas que apuntan a commits
#   que fueron reescritos durante el rebase
```

**Casos de uso reales:** [🔙](#9-git-rebase---reescribiendo-historia)

```bash
# ─────────────────────────────────────────────────────────────────
# CASO 1: Preparar PR limpio antes de merge
# ─────────────────────────────────────────────────────────────────
# Situación: Llevas 2 semanas en feature/user-profile.
# Tienes 12 commits: muchos "wip", "fix", "tmp"...
# El equipo pide que el PR tenga máximo 3 commits bien descritos.
#
# Paso 1: Actualizar con los últimos cambios de main
git fetch origin
git rebase origin/main

# Paso 2: Limpiar historial (últimos 12 commits)
git rebase -i HEAD~12
# En el editor:
# - Deja el primero como "pick"
# - Los "wip/fix" los pones como "fixup"
# - Los hitos importantes como "pick" o "reword"

# Paso 3: Push (necesita force porque reescribiste historia)
git push --force-with-lease origin feature/user-profile


# ─────────────────────────────────────────────────────────────────
# CASO 2: Actualizar feature branch cada día
# ─────────────────────────────────────────────────────────────────
# Situación: Tu rama feature/checkout lleva 1 semana.
# Cada mañana, antes de empezar, sincronizas con main.
git fetch origin
git rebase origin/main
# Si hay conflictos: resolverlos y git rebase --continue
# → Tus commits siempre están encima de lo último de main
# → Cuando hagas el merge final, no habrá conflictos ni divergencias


# ─────────────────────────────────────────────────────────────────
# CASO 3: Dividir un commit enorme en dos
# ─────────────────────────────────────────────────────────────────
# Situación: Hiciste un commit con cambios del modelo Y la vista,
# y quieres separarlos en dos commits distintos.
git rebase -i HEAD~1
# Cambia "pick" por "edit" en ese commit

# Git para en ese commit. Ahora deshaces el commit (sin perder cambios):
git reset HEAD~1
# Ahora tienes todos los cambios en working directory

# Añades solo los archivos del modelo:
git add models/user.js
git commit -m "feat: add user model"

# Añades los de la vista:
git add views/user.html
git commit -m "feat: add user profile view"

# Continúas el rebase:
git rebase --continue


# ─────────────────────────────────────────────────────────────────
# CASO 4: Mover rama creada desde la rama equivocada
# ─────────────────────────────────────────────────────────────────
# Situación: Creaste feature/hotfix desde feature/big-refactor por error.
# Quieres que salga directamente de main.
git rebase --onto main feature/big-refactor feature/hotfix
# → feature/hotfix ahora parte de main
# → Los commits de big-refactor no están incluidos
```

**Rebase vs Merge:** [🔙](#9-git-rebase---reescribiendo-historia)

```bash
# ─────────────────────────────────────────────────────────────────
# MERGE: preserva la historia tal como ocurrió
# ─────────────────────────────────────────────────────────────────
git checkout main
git merge feature/login
#
# Resultado:
#   main:  A---B---C---M   ← M = merge commit
#                    \ /
#   feat:             D---E
#
# ✓ Muestra exactamente cuándo se integró la feature
# ✓ No reescribe commits existentes
# ✓ Seguro para ramas públicas compartidas
# ✗ Historial más complejo de leer (ramificaciones)


# ─────────────────────────────────────────────────────────────────
# REBASE: historia lineal más limpia
# ─────────────────────────────────────────────────────────────────
git checkout feature/login
git rebase main
git checkout main
git merge feature/login  # Fast-forward automático
#
# Resultado:
#   main:  A---B---C---D'--E'   ← historia lineal
#
# ✓ Historial fácil de leer y seguir con git log
# ✓ Fácil de buscar cuándo se introdujo un bug (git bisect)
# ✗ Reescribe commits (nuevos hashes)
# ✗ Peligroso si ya habías pusheado esos commits
```

**⚠️ Regla de oro del rebase:** [🔙](#9-git-rebase---reescribiendo-historia)

```
NUNCA hagas rebase de commits que ya están en un repositorio
compartido (público) y que otros han descargado.

¿Por qué? Porque rebase crea commits NUEVOS (nuevos hashes).
Si alguien tiene los commits originales y tú cambias la historia,
sus repositorios divergen y los merge posteriores son un caos.

✅ CORRECTO: rebase de commits solo en tu máquina local
✅ CORRECTO: rebase de tu feature branch antes del primer push
✅ CORRECTO: rebase de tu feature branch personal (nadie más trabaja en ella)

❌ INCORRECTO: rebase de main, develop o cualquier rama compartida
❌ INCORRECTO: rebase después de haber pusheado y otros descargaron
```

**Troubleshooting:** [🔙](#9-git-rebase---reescribiendo-historia)

```bash
# ─────────────────────────────────────────────────────────────────
# Problema 1: Conflicto durante rebase
# ─────────────────────────────────────────────────────────────────
# Git muestra:
# CONFLICT (content): Merge conflict in src/auth.js
# error: could not apply abc1234... feat: add token validation

# Solución:
git status                        # Ver qué archivos tienen conflicto
# Edita los archivos marcados con <<<<<<< HEAD ... >>>>>>> abc1234
git add src/auth.js               # Marcar como resuelto
git rebase --continue             # Continuar

# Si no sabes cómo resolver y quieres volver al estado inicial:
git rebase --abort                # Cancela todo, vuelves a donde estabas


# ─────────────────────────────────────────────────────────────────
# Problema 2: Commits duplicados tras rebase
# ─────────────────────────────────────────────────────────────────
# Situación: Hiciste push, luego rebase, luego push --force.
# Al hacer pull, los commits aparecen duplicados.
# Causa: otro compañero hizo pull antes de tu force push.
# Solución: hablar con el equipo y hacer:
git pull --rebase                 # Rebase local sobre la historia reescrita


# ─────────────────────────────────────────────────────────────────
# Problema 3: Me arrepiento del rebase, quiero deshacer
# ─────────────────────────────────────────────────────────────────
git reflog                        # Ver el estado antes del rebase
# Busca la línea: "HEAD@{5}: rebase: start"
# Justo ANTES de eso está tu estado original
git reset --hard HEAD@{5}         # Vuelve al estado anterior al rebase


# ─────────────────────────────────────────────────────────────────
# Problema 4: Push rechazado después de rebase
# ─────────────────────────────────────────────────────────────────
# Error: "Updates were rejected because the tip of your current branch is behind"
# Causa: Reescribiste historia, el remoto tiene commits que tú ya no tienes.
git push --force-with-lease       # Fuerza el push, pero solo si nadie más actualizó
# NUNCA usar --force a secas en ramas que otros usan
```

**Configuración recomendada:** [🔙](#9-git-rebase---reescribiendo-historia)

```bash
# Usar rebase por defecto en git pull (en vez de merge)
git config --global pull.rebase true

# Activar autosquash siempre en rebase interactivo
git config --global rebase.autoSquash true

# Activar autostash automático durante rebase
git config --global rebase.autoStash true

# Ver configuración actual
git config --list | grep rebase
```

**Mejores prácticas:** [🔙](#9-git-rebase---reescribiendo-historia)

```bash
✓ Usa rebase para limpiar tu historia LOCAL antes de hacer PR
✓ Haz rebase de tu feature sobre main antes del merge para evitar conflictos tardíos
✓ Usa --force-with-lease nunca --force
✓ Usa rebase -i para preparar commits limpios y descriptivos
✓ Ante duda, git rebase --abort cancela sin consecuencias

✗ NUNCA hagas rebase de ramas que otros estén usando (main, develop, shared)
✗ NUNCA hagas rebase después de que otros descargaron tus commits
✗ No uses rebase si no entiendes qué commits vas a reescribir
```

---

## Navegación

- [⬅️ Anterior: git merge](08-git-merge.md)
- [🏠 Volver al Índice](../../GIT_COMANDOS_GUIA_PRACTICA.md)
- [➡️ Siguiente: git clone](10-git-clone.md)
