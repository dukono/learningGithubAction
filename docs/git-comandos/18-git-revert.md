# 18. git revert - Deshaciendo Commits Públicos

[🏠 Volver al Índice](../../GIT_COMANDOS_GUIA_PRACTICA.md)

---

## 18. git revert - Deshaciendo Commits Públicos
[⬆️ Top](#18-git-revert---deshaciendo-commits-públicos)

**¿Qué hace?**
Crea un NUEVO commit que aplica exactamente el inverso de un commit anterior. El commit original permanece en la historia. Es la forma segura de deshacer cambios que ya han sido compartidos con otros (pusheados al remoto).

La diferencia con `git reset` es clave: reset borra commits del historial, revert los neutraliza creando nuevos commits. Si ya compartiste un commit con tu equipo y quieres deshacerlo, **debes usar revert**, no reset.

**Funcionamiento interno:** [🔙](#18-git-revert---deshaciendo-commits-públicos)

```
Situación: tienes estos commits:
  A → B → C → D (HEAD)
  
  Commit C añadió una feature que resultó tener bugs.
  Quieres deshacer C sin tocar D.

git revert C:
  1. Lee el commit C
  2. Calcula el INVERSO de los cambios de C
     (lo que C añadió, revert lo elimina; lo que C eliminó, revert lo añade)
  3. Aplica esos cambios inversos al working directory
  4. Crea un nuevo commit C' con esos cambios inversos
  
Resultado:
  A → B → C → D → C'  (HEAD)
  
  → C sigue ahí, la historia está intacta
  → C' cancela los efectos de C
  → D no se toca (si D no dependía de C)
  → Seguro para ramas públicas: los demás pueden hacer pull normalmente

Diferencia visual:
  RESET (peligroso para commits públicos):
    A → B → [C eliminado] → D  ← la historia cambia
  
  REVERT (seguro):
    A → B → C → D → C'         ← la historia se preserva
```

**Todas las opciones importantes:** [🔙](#18-git-revert---deshaciendo-commits-públicos)

```bash
# ============================================
# 1. Revert básico
# ============================================
# Situación: El último commit introdujo un bug. Necesitas deshacerlo.
git revert HEAD
# → Abre el editor para confirmar el mensaje del commit de revert
# → Por defecto: "Revert "mensaje original del commit""

# Sin abrir el editor (acepta el mensaje por defecto):
git revert HEAD --no-edit

# Revert de un commit específico por su hash:
git revert abc1234
git revert abc1234 --no-edit


# ============================================
# 2. Revert de commits pasados (no el último)
# ============================================
# Situación: Un commit de hace 3 días introdujo un problema,
# pero los commits posteriores (D) son válidos y quieres mantenerlos.
git revert HEAD~3
# → Revierte el commit que está 3 posiciones atrás de HEAD
# → Los commits intermedios (HEAD~2, HEAD~1, HEAD) se mantienen intactos

# Nota: si el commit que revertimos tiene dependencias en los posteriores,
# pueden aparecer conflictos. Deberás resolverlos.


# ============================================
# 3. Revert de múltiples commits
# ============================================
# Situación: Los últimos 3 commits formaban una feature entera
# y necesitas deshacer todo ese bloque.

# Opción A: Revert de un rango (crea un commit de revert por cada commit):
git revert HEAD~3..HEAD
# → Crea 3 commits de revert, uno para cada commit del rango
# Nota: El rango HEAD~3..HEAD es EXCLUSIVO en el extremo izquierdo.
# Para incluir HEAD~3: usar HEAD~3^..HEAD ó HEAD~4..HEAD

# Opción B: --no-commit → aplica todos los reverts como uno solo
# Situación: Quieres que todos los reverts queden en un ÚNICO commit,
# no uno por cada commit revertido.
git revert --no-commit HEAD~3..HEAD
# → Aplica los cambios inversos al staging area pero NO commitea
git commit -m "revert: quitar feature X por problemas de rendimiento"
# → Un solo commit limpio que deshace todos los commits del rango


# ============================================
# 4. -m: Revert de un merge commit (el más complejo)
# ============================================
# Situación: Mergeaste feature/pago en main y causó problemas.
# El merge commit tiene DOS padres:
#   Padre 1 (main antes del merge)
#   Padre 2 (feature/pago)
#
# Git necesita saber QUÉ PADRE conservar como "la línea principal":

git revert -m 1 abc1234
# -m 1 → mantener el padre 1 (main antes del merge)
#       → efectivamente "deshace" el merge, volviendo main a cómo estaba
#
# ¿Por qué no -m 2? Porque el padre 2 es la feature que queremos deshacer.
# -m 2 mantendría la feature y desharía lo que había en main → no tiene sentido.

# ⚠️ TRAMPA CONOCIDA: Revert del revert
# Si más tarde quieres volver a aplicar esa feature mergeada,
# NO puedes hacer simplemente otro merge de feature/pago.
# Git creerá que ya aplicó esos commits (porque están en el historial).
# Debes hacer: git revert <hash-del-commit-de-revert>
# (un revert del revert restaura la feature)


# ============================================
# 5. Abortar o continuar revert con conflictos
# ============================================
# Si al hacer revert hay conflictos:
git status                  # Ver qué archivos tienen conflicto
# Editar y resolver los conflictos (<<<<<<, ======, >>>>>>>)
git add archivo-resuelto.js
git revert --continue       # Finalizar el revert

# Si cambias de opinión y quieres cancelar:
git revert --abort          # Vuelve al estado antes del revert
```

**Casos de uso reales:** [🔙](#18-git-revert---deshaciendo-commits-públicos)

```bash
# ─────────────────────────────────────────────────────────────────
# CASO 1: Deshacer una feature que llegó a producción con bugs
# ─────────────────────────────────────────────────────────────────
# La feature de "nuevo sistema de pagos" está en main (y en producción).
# Los usuarios reportan errores. Necesitas revertirla urgentemente.

git log --oneline -10
# abc1234 feat: nuevo sistema de pagos (commit que quieres revertir)
# def5678 fix: corrección de UI en header (este DEBE mantenerse)
# ...

git revert abc1234 --no-edit
git push origin main
# → Producción vuelve a funcionar sin el sistema de pagos
# → El historial queda: ... → abc1234 → def5678 → Revert "feat: nuevo sistema..."
# → Los demás hacen pull normalmente, sin conflictos


# ─────────────────────────────────────────────────────────────────
# CASO 2: Revertir un merge completo de producción
# ─────────────────────────────────────────────────────────────────
# Mergeaste feature/nueva-api en main. El merge commit es M.
# La API nueva rompe integraciones externas.

git log --oneline --graph -5
# * abc1234 (HEAD -> main) Merge branch 'feature/nueva-api'  ← M
# |\
# | * def5678 feat: nuevo endpoint /api/v2
# | * ghi9012 feat: refactor api service
# * jkl3456 feat: mejorar validación de login

git revert -m 1 abc1234 --no-edit
git push origin main
# → main vuelve a cómo estaba antes del merge de feature/nueva-api


# ─────────────────────────────────────────────────────────────────
# CASO 3: Rollback de sprint completo (múltiples commits)
# ─────────────────────────────────────────────────────────────────
# El sprint 23 introdujo demasiados problemas. Quieres revertir
# los 5 commits de ese sprint en un solo commit limpio.

git log --oneline -7
# abc1234 feat: sprint-23 feature C
# def5678 feat: sprint-23 feature B
# ghi9012 feat: sprint-23 feature A
# jkl3456 feat: sprint-22 último commit (este NO quieres revertir)

# Aplica todos los reverts al staging sin commitear:
git revert --no-commit ghi9012^..abc1234
# ó equivalente:
git revert --no-commit ghi9012 def5678 abc1234

# Crea un único commit descriptivo:
git commit -m "revert: rollback de sprint-23 por inestabilidad en producción"


# ─────────────────────────────────────────────────────────────────
# CASO 4: "Revert del revert" - re-aplicar feature revertida
# ─────────────────────────────────────────────────────────────────
# Hace 2 días revertiste la feature/pagos con commit xyz9876.
# Ahora arreglaron los bugs y quieres volver a activarla.

git log --oneline -5
# xyz9876 Revert "feat: nuevo sistema de pagos"  ← necesitas revertir ESTE

git revert xyz9876 --no-edit
# → El revert del revert restaura la feature original
```

**Revert vs Reset:** [🔙](#18-git-revert---deshaciendo-commits-públicos)

```bash
# ─────────────────────────────────────────────────────────────────
# RESET: reescribe la historia (PELIGROSO para commits públicos)
# ─────────────────────────────────────────────────────────────────
git reset --hard HEAD~1
# → El commit desaparece del historial
# → Si ya lo habías pusheado, necesitarás --force
# → Los demás tendrán conflictos si habían descargado ese commit
# ✓ Úsalo SOLO para commits locales (no pusheados)


# ─────────────────────────────────────────────────────────────────
# REVERT: añade a la historia (SEGURO para commits públicos)
# ─────────────────────────────────────────────────────────────────
git revert HEAD~1
# → El commit sigue en el historial
# → Se crea un nuevo commit que deshace sus efectos
# → Los demás pueden hacer pull normalmente
# ✓ Úsalo para commits que ya están en ramas compartidas

# ¿Cuándo usar cada uno?
# RESET:  Commits locales todavía no pusheados
# REVERT: Commits ya pusheados / en ramas compartidas
```

**Troubleshooting:** [🔙](#18-git-revert---deshaciendo-commits-públicos)

```bash
# ─────────────────────────────────────────────────────────────────
# Problema 1: Conflictos durante el revert
# ─────────────────────────────────────────────────────────────────
# Ocurre cuando commits posteriores dependen del commit que estás revirtiendo
git status                        # Ver archivos en conflicto
# Edita y resuelve los conflictos
git add archivos-resueltos.js
git revert --continue


# ─────────────────────────────────────────────────────────────────
# Problema 2: Revert de merge sin -m falla
# ─────────────────────────────────────────────────────────────────
# Error: "error: commit abc1234 is a merge but no -m option was given"
# Solución: añadir -m 1 (casi siempre padre 1 = línea principal)
git revert -m 1 abc1234


# ─────────────────────────────────────────────────────────────────
# Problema 3: Después de revert de merge, no puedo volver a mergear la feature
# ─────────────────────────────────────────────────────────────────
# Causa: Git ya "conoce" los commits de esa feature (están en el historial)
# y no los aplicará de nuevo con un merge normal.
# Solución: revertir el commit de revert
git log --oneline | grep "Revert"
# Encuentra el hash del commit de revert (ej: xyz9876)
git revert xyz9876   # Revert del revert = restaurar la feature
```

**Mejores prácticas:** [🔙](#18-git-revert---deshaciendo-commits-públicos)

```bash
✓ Usa revert para commits que ya están en ramas compartidas o producción
✓ Usa --no-commit para agrupar múltiples reverts en un solo commit limpio
✓ Usa -m 1 siempre que revertas un merge commit
✓ Incluye el motivo del revert en el mensaje de commit
✓ Para re-aplicar una feature revertida: revertir el commit de revert

✗ No uses reset para commits ya pusheados (usa revert)
✗ No omitas -m al revertir merge commits (dará error)
✗ No hagas revert de revert con merge sin entender las consecuencias
```

---

## Navegación

- [⬅️ Anterior: git tag](17-git-tag.md)
- [🏠 Volver al Índice](../../GIT_COMANDOS_GUIA_PRACTICA.md)
- [➡️ Siguiente: git cherry-pick](19-git-cherry-pick.md)
