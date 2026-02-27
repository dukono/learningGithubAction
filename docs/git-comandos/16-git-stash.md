# 16. git stash - Guardado Temporal

[🏠 Volver al Índice](../../GIT_COMANDOS_GUIA_PRACTICA.md)

---

## 16. git stash - Guardado Temporal
[⬆️ Top](#16-git-stash---guardado-temporal)

**¿Qué hace?**
Guarda temporalmente los cambios del working directory y el staging area en una "pila" (stack), dejando el directorio limpio como si no hubieras tocado nada. Cuando quieras retomar el trabajo, recuperas el stash y continúas donde lo dejaste.

Es el equivalente a "guardar partida" en un videojuego para ir a hacer otra cosa.

**Funcionamiento interno:** [🔙](#16-git-stash---guardado-temporal)

```
El stash es una PILA (stack) con estructura LIFO (último en entrar, primero en salir).
Cada stash es en realidad un commit especial guardado en refs/stash.

Antes del stash:
  Working directory: archivos modificados
  Staging area:      archivos preparados para commit
  HEAD:              último commit

Al hacer "git stash":
  1. Crea un commit de tree con el estado del staging area
  2. Crea un commit con el estado del working directory
  3. Crea un "stash commit" que enlaza ambos
  4. Guarda la referencia en refs/stash
  5. Resetea working directory y staging al estado de HEAD

Estructura de la pila:
  stash@{0}  → el stash más reciente (el último que añadiste)
  stash@{1}  → el anterior
  stash@{2}  → el anterior al anterior
  ...

  git stash push  → añade al TOP de la pila (stash@{0})
  git stash pop   → saca del TOP de la pila (y lo elimina)
  git stash apply → recupera del TOP pero NO lo elimina

Qué guarda por defecto:
  ✓ Cambios en archivos rastreados (tracked)
  ✓ Cambios en staging area
  ✗ Archivos nuevos NO rastreados (untracked)  → necesitas -u
  ✗ Archivos ignorados por .gitignore           → necesitas -a
```

**Todas las opciones importantes:** [🔙](#16-git-stash---guardado-temporal)

```bash
# ============================================
# 1. Stash básico
# ============================================
# Situación: Necesitas cambiar de rama urgentemente pero tienes
# cambios a medias que no quieres commitear todavía.
git stash
# ó con un mensaje descriptivo (MUY recomendado):
git stash push -m "WIP: formulario de registro - falta validación email"
# → Working directory queda limpio
# → Puedes cambiar de rama, hacer pull, etc.


# ============================================
# 2. -u / --include-untracked: incluir archivos nuevos
# ============================================
# Situación: Creaste archivos nuevos que todavía no hiciste "git add".
# El stash básico NO los incluye. Necesitas -u.
git stash -u
git stash push -u -m "WIP: nuevos archivos incluidos"
# → Guarda también los archivos que nunca se hicieron "git add"


# ============================================
# 3. -a / --all: incluir todo, incluso ignorados
# ============================================
# Situación: Quieres guardar ABSOLUTAMENTE todo, incluido lo que
# está en .gitignore (node_modules, .env local, etc.)
git stash -a
# ⚠️ Úsalo con cuidado: puede guardar mucho si tienes node_modules


# ============================================
# 4. -p / --patch: stash interactivo (por partes)
# ============================================
# Situación: Tienes cambios en varios archivos pero solo quieres
# hacer stash de algunos de ellos, no todos.
git stash push -p
# → Abre modo interactivo hunk por hunk
# → Para cada cambio decides: stash (y) o no (n)


# ============================================
# 5. Ver la lista de stashes
# ============================================
git stash list
# Muestra algo como:
# stash@{0}: On feature/login: WIP: formulario - falta validación
# stash@{1}: On main: WIP: prueba de concepto cache
# stash@{2}: WIP on develop: abc1234 fix: corregir null pointer


# ============================================
# 6. Ver el contenido de un stash
# ============================================
# Ver resumen de qué archivos cambiaron:
git stash show
git stash show stash@{2}    # Para un stash específico

# Ver el diff completo (qué cambió en el código):
git stash show -p
git stash show -p stash@{1}


# ============================================
# 7. Recuperar stash
# ============================================
# apply: aplica el stash pero LO MANTIENE en la lista
# → Útil si quieres aplicar el mismo stash en varias ramas
git stash apply              # Aplica el más reciente (stash@{0})
git stash apply stash@{2}    # Aplica un stash específico

# pop: aplica el stash y LO ELIMINA de la lista (lo más común)
git stash pop                # Aplica y elimina stash@{0}
git stash pop stash@{1}      # Aplica y elimina uno específico

# --index: restaurar también el estado del staging area
# (sin --index, todo aparece como unstaged aunque antes estuviera staged)
git stash pop --index


# ============================================
# 8. Crear rama desde un stash
# ============================================
# Situación: Guardaste trabajo en stash, luego el código base cambió tanto
# que al hacer pop hay muchos conflictos. Mejor crear una rama nueva
# con el estado en que estaba cuando hiciste el stash.
git stash branch feature/trabajo-guardado
git stash branch feature/trabajo-guardado stash@{2}   # De un stash específico
# → Crea rama nueva
# → Hace checkout de esa rama
# → Aplica el stash
# → Elimina el stash (como pop)


# ============================================
# 9. --keep-index: hacer stash solo de lo que NO está en staging
# ============================================
# Situación: Tienes algunos cambios en staging (añadidos con git add)
# y otros sin añadir. Quieres probar el código con SOLO los cambios
# del staging, sin el resto.
git stash push --keep-index
# → Hace stash de los cambios que NO están en staging
# → Los cambios del staging se MANTIENEN
# → Así puedes testear "solo lo que ibas a commitear"


# ============================================
# 10. Eliminar stashes
# ============================================
# Eliminar el stash más reciente:
git stash drop
git stash drop stash@{1}    # Eliminar uno específico

# Eliminar TODOS los stashes (limpieza total):
git stash clear
```

**Casos de uso reales:** [🔙](#16-git-stash---guardado-temporal)

```bash
# ─────────────────────────────────────────────────────────────────
# CASO 1: Interrupción urgente - bug en producción
# ─────────────────────────────────────────────────────────────────
# Estás a mitad de implementar feature/newsletter.
# De repente: "¡El carrito de compras está caído en producción!"
# Necesitas cambiarte a main y arreglar el bug ahora.

# Paso 1: Guarda tu trabajo actual
git stash push -m "WIP: feature newsletter - falta template email"

# Paso 2: Ve a main y crea la rama del hotfix
git checkout main
git checkout -b hotfix/carrito-caido

# Paso 3: Arregla el bug, commitea y sube
git add .
git commit -m "fix: corregir null pointer en checkout service"
git push -u origin hotfix/carrito-caido

# Paso 4: Vuelve a tu feature
git checkout feature/newsletter
git stash pop
# → Continúas exactamente donde lo dejaste


# ─────────────────────────────────────────────────────────────────
# CASO 2: Stash para hacer pull sin conflictos
# ─────────────────────────────────────────────────────────────────
# Tienes cambios locales pero necesitas hacer pull de urgencia.
# Git rechazaría el pull porque tienes cambios sin commitear.

git stash                   # Guarda cambios locales
git pull origin main        # Actualiza sin problemas
git stash pop               # Recupera tus cambios
# Si hay conflictos al hacer pop: resolverlos normalmente


# ─────────────────────────────────────────────────────────────────
# CASO 3: "¡Me equivoqué de rama!"
# ─────────────────────────────────────────────────────────────────
# Llevas 2 horas trabajando y te das cuenta de que estás en "main"
# en lugar de en tu feature branch.

git stash push -m "WIP: cambios que hice en la rama equivocada"
git checkout feature/mi-tarea
git stash pop
# → Todos tus cambios ahora están en la rama correcta


# ─────────────────────────────────────────────────────────────────
# CASO 4: Probar si el código funciona sin tus cambios actuales
# ─────────────────────────────────────────────────────────────────
# Sospechas que tus cambios están causando un bug.
# Quieres probar el código limpio (sin tus cambios) para confirmar.

git stash              # Oculta tus cambios
npm test               # Ejecuta los tests con el código limpio
git stash pop          # Recupera tus cambios


# ─────────────────────────────────────────────────────────────────
# CASO 5: Conflicto al recuperar stash
# ─────────────────────────────────────────────────────────────────
# Guardaste un stash hace 3 días. El código base cambió mucho.
# Al hacer pop hay conflictos.

git stash pop
# → Si hay conflictos, el stash NO se elimina automáticamente
# → Resuelves los conflictos normalmente (editando los archivos)
git add archivos-resueltos.js
git stash drop          # Ahora sí eliminas el stash manualmente
# ó si es muy complicado, crea una rama nueva:
git stash branch rama-del-stash-conflictivo
```

**Troubleshooting:** [🔙](#16-git-stash---guardado-temporal)

```bash
# ─────────────────────────────────────────────────────────────────
# Problema 1: El stash no guardó mis archivos nuevos
# ─────────────────────────────────────────────────────────────────
# Causa: Los archivos nuevos (untracked) no se guardan por defecto
git stash push -u     # ó --include-untracked
# ó primero añádelos al staging y luego stash normal:
git add archivo-nuevo.js
git stash


# ─────────────────────────────────────────────────────────────────
# Problema 2: Perdí el stash después de hacer clear o drop
# ─────────────────────────────────────────────────────────────────
# Los stashes son commits internos. Se pueden recuperar con reflog.
git fsck --unreachable | grep commit
# → Muestra commits "huérfanos" que ya no son accesibles
# Para ver su contenido:
git show <hash-del-commit-huerfano>
# Para restaurar:
git stash apply <hash-del-commit-huerfano>


# ─────────────────────────────────────────────────────────────────
# Problema 3: git stash pop crea conflictos
# ─────────────────────────────────────────────────────────────────
# Normal cuando el código base cambió. El stash sigue ahí.
git status             # Ver qué archivos tienen conflicto
# Editar archivos con conflicto (<<<<<<, ======, >>>>>>>)
git add archivos-resueltos.js
git stash drop         # Eliminar el stash manualmente


# ─────────────────────────────────────────────────────────────────
# Problema 4: No recuerdo qué tenía en los stashes
# ─────────────────────────────────────────────────────────────────
git stash list                    # Ver todos los stashes
git stash show stash@{0}          # Ver qué archivos cambiaron
git stash show -p stash@{0}       # Ver el código exacto
```

**Mejores prácticas:** [🔙](#16-git-stash---guardado-temporal)

```bash
✓ SIEMPRE usa mensajes descriptivos: git stash push -m "descripción clara"
✓ Usa -u si tienes archivos nuevos que quieres incluir
✓ Usa stash branch si hay muchos conflictos al recuperar
✓ Limpia stashes viejos regularmente con git stash drop

✗ No uses stash como sistema de backup a largo plazo (usa branches)
✗ No acumules 20 stashes: te perderás en ellos
✗ No hagas git stash clear sin revisar qué hay dentro
✗ No confundas stash con commit: el stash no está en el historial
```

---

## Navegación

- [⬅️ Anterior: git reset](15-git-reset.md)
- [🏠 Volver al Índice](../../GIT_COMANDOS_GUIA_PRACTICA.md)
- [➡️ Siguiente: git tag](17-git-tag.md)
