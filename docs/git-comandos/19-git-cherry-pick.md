# 19. git cherry-pick - Aplicando Commits Selectivos

[🏠 Volver al Índice](../../GIT_COMANDOS_GUIA_PRACTICA.md)

---

## 19. git cherry-pick - Aplicando Commits Selectivos
[⬆️ Top](#19-git-cherry-pick---aplicando-commits-selectivos)

**¿Qué hace?**
Toma un commit específico de cualquier rama y lo aplica sobre la rama actual, creando un nuevo commit con los mismos cambios pero un hash diferente. Es como "copiar" un commit de un lugar a otro, sin traer toda la rama.

Piénsalo así: en vez de hacer merge de toda una rama, solo "eliges" los commits concretos que quieres traer, como elegir cerezas de un árbol (cherry-pick = recoger cerezas).

**Funcionamiento interno:** [🔙](#19-git-cherry-pick---aplicando-commits-selectivos)

```
Situación:
  main:     A---B---C  (HEAD)
  develop:  A---D---E---F

  El commit E contiene un fix importante que necesitas AHORA en main,
  pero D y F tienen código inacabado que no quieres en main todavía.

git cherry-pick E (estando en main):
  1. Lee el commit E
  2. Calcula el diff que introduce E
     (diferencia entre D y E)
  3. Aplica ese diff a la rama actual (main)
  4. Crea un nuevo commit E' en main con el mismo contenido
     pero un HASH DIFERENTE (porque el contexto/padre es distinto)

Resultado:
  main:     A---B---C---E'  (E' tiene mismo contenido que E, hash distinto)
  develop:  A---D---E---F   (sin cambios)

  → E y E' tienen el mismo código de cambio, pero son commits distintos
  → Si más tarde haces merge de develop a main, Git puede detectar la duplicación
    y omitir E (porque E' ya tiene esos cambios)
```

**Todas las opciones importantes:** [🔙](#19-git-cherry-pick---aplicando-commits-selectivos)

```bash
# ============================================
# 1. Cherry-pick básico
# ============================================
# Situación: Un fix urgente está en develop pero lo necesitas en main.
git cherry-pick abc1234
# → Aplica los cambios del commit abc1234 a la rama actual
# → Crea un nuevo commit automáticamente


# ============================================
# 2. --no-commit: aplicar sin commitear automáticamente
# ============================================
# Situación: Quieres combinar varios cherry-picks en un solo commit,
# o revisar los cambios antes de commitear.
git cherry-pick --no-commit abc1234
git cherry-pick --no-commit def5678
git cherry-pick --no-commit ghi9012
# → Los 3 commits se aplican al staging area sin crear commits
git commit -m "fix: aplicar fixes de develop a main"
# → Un solo commit con todos los cambios


# ============================================
# 3. Múltiples commits en un solo comando
# ============================================
# Varios commits específicos:
git cherry-pick abc1234 def5678 ghi9012
# → Crea 3 commits en el orden indicado

# Rango de commits (EXCLUSIVO el primero):
git cherry-pick abc1234..ghi9012
# → Aplica todos los commits desde abc1234 (exclusive) hasta ghi9012

# Rango INCLUSIVO (incluye el primer commit):
git cherry-pick abc1234^..ghi9012
# → Aplica desde abc1234 inclusive hasta ghi9012


# ============================================
# 4. -x: añadir referencia al commit original
# ============================================
# Situación: Quieres que quede registro de dónde vino el commit.
# Muy útil cuando haces backports a múltiples versiones.
git cherry-pick -x abc1234
# → El mensaje del nuevo commit incluye:
# "feat: fix de login
# (cherry picked from commit abc1234)"


# ============================================
# 5. --edit: cambiar el mensaje del commit
# ============================================
# Situación: Quieres adaptar el mensaje al contexto de la rama destino.
git cherry-pick --edit abc1234
# → Abre el editor para modificar el mensaje antes de commitear


# ============================================
# 6. --signoff: añadir firma
# ============================================
# Situación: El proyecto requiere que firmes los commits que aplicas.
git cherry-pick --signoff abc1234
# → Añade "Signed-off-by: Tu Nombre <tu@email.com>" al mensaje


# ============================================
# 7. -m: cherry-pick de un merge commit
# ============================================
# Los merge commits tienen 2 padres. Debes indicar cuál es la "línea principal".
git cherry-pick -m 1 abc1234
# -m 1 → usar padre 1 como base
# -m 2 → usar padre 2 como base


# ============================================
# 8. Continuar, saltar o abortar tras conflicto
# ============================================
# Si hay conflictos:
git status                    # Ver archivos en conflicto
# Editar y resolver (<<<<<<, ======, >>>>>>>)
git add archivos-resueltos.js
git cherry-pick --continue    # Crear el commit con los conflictos resueltos

# Saltar este commit y continuar con el siguiente (en rango):
git cherry-pick --skip

# Cancelar completamente:
git cherry-pick --abort
```

**Casos de uso reales:** [🔙](#19-git-cherry-pick---aplicando-commits-selectivos)

```bash
# ─────────────────────────────────────────────────────────────────
# CASO 1: Hotfix urgente - llevar un fix de develop a producción
# ─────────────────────────────────────────────────────────────────
# Contexto: main es producción, develop es donde trabajas.
# Un bug crítico se encontró y fue arreglado en develop (commit: abc1234).
# No puedes mergear develop completo porque hay código inacabado.

# Paso 1: Ver qué commits tiene develop que no tiene main
git log main..develop --oneline
# abc1234 fix: corregir null pointer en checkout   ← ESTE quieres
# def5678 feat: nueva pantalla de configuración    ← este no (inacabado)
# ghi9012 wip: refactor del módulo de pagos        ← este no (wip)

# Paso 2: Ir a main
git checkout main

# Paso 3: Cherry-pick solo el fix
git cherry-pick abc1234 -x
# → El fix está ahora en main, nada más

# Paso 4: Subir a producción
git push origin main


# ─────────────────────────────────────────────────────────────────
# CASO 2: Backport - llevar un fix a versiones anteriores mantenidas
# ─────────────────────────────────────────────────────────────────
# Mantienes v1.x y v2.x en paralelo. Un security fix se hizo en main (v3).
# Necesitas aplicarlo también en v1 y v2.

# El fix está en commit: sec1234
git checkout release/v2.x
git cherry-pick -x sec1234
git push origin release/v2.x

git checkout release/v1.x
git cherry-pick -x sec1234
git push origin release/v1.x

# -x deja rastro: "(cherry picked from commit sec1234)"
# → Facilita rastrear que el fix fue aplicado en todas las versiones


# ─────────────────────────────────────────────────────────────────
# CASO 3: Recuperar commits hechos en la rama equivocada
# ─────────────────────────────────────────────────────────────────
# Hiciste 3 commits en main por error. Deberían estar en feature/nueva-feature.

git log --oneline -5
# abc1234 feat: nueva feature parte 3   ← estos 3 están en el lugar
# def5678 feat: nueva feature parte 2   ← equivocado (main)
# ghi9012 feat: nueva feature parte 1   ←
# jkl3456 commit anterior correcto

# Paso 1: Crear/ir a la rama correcta y traer los commits
git checkout feature/nueva-feature
git cherry-pick ghi9012 def5678 abc1234  # En orden cronológico

# Paso 2: Eliminar los commits de main
git checkout main
git reset --hard jkl3456   # Vuelve al estado antes de los commits erróneos


# ─────────────────────────────────────────────────────────────────
# CASO 4: Recuperar un commit perdido por reset accidental
# ─────────────────────────────────────────────────────────────────
# Hiciste git reset --hard y perdiste un commit.
# Pero git reflog lo recuerda.

git reflog
# HEAD@{3}: commit: feat: nueva funcionalidad importante  ← el que perdiste
# abc1234 es el hash

git cherry-pick abc1234
# → El commit "perdido" vuelve a tu rama actual


# ─────────────────────────────────────────────────────────────────
# CASO 5: Combinar varios fixes de develop en un solo commit en main
# ─────────────────────────────────────────────────────────────────
# develop tiene 3 pequeños fixes que quieres como un único commit en main

git checkout main
git cherry-pick --no-commit fix1hash
git cherry-pick --no-commit fix2hash
git cherry-pick --no-commit fix3hash
git commit -m "fix: agrupar correcciones de seguridad de la semana"
```

**Cherry-pick vs otras alternativas:** [🔙](#19-git-cherry-pick---aplicando-commits-selectivos)

```bash
# ─────────────────────────────────────────────────────────────────
# CHERRY-PICK: commits específicos
# ─────────────────────────────────────────────────────────────────
# ✓ Solo quieres commits concretos, no toda la rama
# ✓ Hotfixes urgentes a producción
# ✓ Backports a versiones anteriores
# ✗ Crea commits duplicados (con hashes diferentes)
# ✗ Si abusas, el historial se vuelve confuso


# ─────────────────────────────────────────────────────────────────
# MERGE: toda la rama
# ─────────────────────────────────────────────────────────────────
# ✓ Cuando quieres TODA la rama integrada
# ✓ No crea duplicados
# ✗ Trae TODO, incluso lo que no quieres


# ─────────────────────────────────────────────────────────────────
# REBASE --onto: mover commits de rama a otra base
# ─────────────────────────────────────────────────────────────────
# ✓ Cuando quieres mover un conjunto de commits a otra base
# ✓ Más potente que cherry-pick para mover ramas enteras
# ✗ Más complejo de entender
```

**Troubleshooting:** [🔙](#19-git-cherry-pick---aplicando-commits-selectivos)

```bash
# ─────────────────────────────────────────────────────────────────
# Problema 1: Conflictos durante cherry-pick
# ─────────────────────────────────────────────────────────────────
# Ocurre cuando el código de la rama destino es diferente al contexto
# en que el commit original fue creado.
git status
# Ver los archivos marcados como "both modified"
# Editar y resolver (<<<<<<, ======, >>>>>>>)
git add archivos-resueltos.js
git cherry-pick --continue


# ─────────────────────────────────────────────────────────────────
# Problema 2: Commits duplicados al hacer merge posterior
# ─────────────────────────────────────────────────────────────────
# Hiciste cherry-pick de E de develop a main.
# Luego haces merge de develop a main.
# El commit E puede aparecer "duplicado" (E y E').
# Git es inteligente y normalmente lo detecta, pero puede causar
# conflictos o commits repetidos en el historial.
# Solución preventiva: usa -x en cherry-pick para trazar el origen.
# Si ya ocurrió: revisar con git log --cherry-pick main...develop


# ─────────────────────────────────────────────────────────────────
# Problema 3: Cherry-pick de un merge commit
# ─────────────────────────────────────────────────────────────────
# Error: "is a merge but no -m option was given"
git cherry-pick -m 1 abc1234  # Usa el padre 1 como base
```

**Mejores prácticas:** [🔙](#19-git-cherry-pick---aplicando-commits-selectivos)

```bash
✓ Usa cherry-pick para hotfixes y backports: es su caso de uso ideal
✓ Usa -x para dejar rastro del origen del commit
✓ Usa --no-commit para combinar varios cherry-picks en uno
✓ Para recuperar commits de reflog, cherry-pick es perfecto

✗ No abuses: si necesitas muchos cherry-picks, quizás deberías hacer merge
✗ No uses como sustituto de merge cuando quieres toda la rama
✗ Evita cherry-pick de merges (muy complejo, usa -m con cuidado)
```

---

## Navegación

- [⬅️ Anterior: git revert](18-git-revert.md)
- [🏠 Volver al Índice](../../GIT_COMANDOS_GUIA_PRACTICA.md)
- [➡️ Siguiente: git clean](20-git-clean.md)
