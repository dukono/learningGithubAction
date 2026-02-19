# 3. git status - Inspeccionando el Estado

[🏠 Volver al Índice](../../GIT_COMANDOS_GUIA_PRACTICA.md)

---

## 3. git status - Inspeccionando el Estado
[⬆️ Top](#3-git-status---inspeccionando-el-estado)

**¿Qué hace?**
Muestra el estado actual del working directory y staging area. Es el comando de "diagnóstico" que usas constantemente para saber en qué punto estás: qué has modificado, qué está preparado para commitear, y cómo estás respecto al remoto.

**Funcionamiento interno:** [🔙](#3-git-status---inspeccionando-el-estado)

```
1. Compara working directory con HEAD
2. Compara staging con HEAD
3. Lee .git/index para archivos untracked
4. Compara con refs/remotes para ahead/behind
```

**Uso práctico:** [🔙](#3-git-status---inspeccionando-el-estado)

```bash
# Status normal (verbose)
git status

# Status corto (MUY ÚTIL)
git status -s
# Formato: XY archivo
# X = estado en staging (index)
# Y = estado en working directory

# Códigos más comunes:
# ?? = untracked (archivo nuevo no añadido)
# A  = added (archivo nuevo añadido al staging)
# M  = modified (archivo modificado y en staging)
#  M = modified (archivo modificado pero NO en staging)
# MM = modified en staging + modificado de nuevo en working
# D  = deleted (archivo eliminado y en staging)
#  D = deleted (archivo eliminado pero NO en staging)
# R  = renamed (archivo renombrado)
# C  = copied (archivo copiado)
# U  = updated but unmerged (conflicto sin resolver)

# Con info de branch
git status -sb

# Ver archivos ignorados
git status --ignored

# Formato porcelain (para scripts)
git status --porcelain
```

**Interpretación del output:** [🔙](#3-git-status---inspeccionando-el-estado)

```bash
# OUTPUT DE git status (verbose):
On branch main
Your branch is ahead of 'origin/main' by 2 commits
→ Tienes 2 commits no pusheados (ahead)
→ "behind" sería: commits remotos que no tienes localmente

Changes to be committed:
→ Staging area (listo para commit)

Changes not staged for commit:
→ Working directory modificado

Untracked files:
→ Archivos nuevos no en Git

# OUTPUT DE git status -s (corto):
 M archivo1.txt    # Modificado, NO en staging
M  archivo2.txt    # Modificado, en staging
MM archivo3.txt    # En staging + modificado de nuevo
A  archivo4.txt    # Nuevo, añadido al staging
?? archivo5.txt    # Nuevo, no añadido (untracked)
D  archivo6.txt    # Eliminado, en staging
 D archivo7.txt    # Eliminado, NO en staging
R  old.txt -> new.txt  # Renombrado
```

**Entendiendo ahead/behind:** [🔙](#3-git-status---inspeccionando-el-estado)

```bash
# Ahead (adelantado): Tienes commits locales no pusheados
Your branch is ahead of 'origin/main' by 2 commits
→ Solución: git push

# Behind (atrasado): El remoto tiene commits que tú no tienes
Your branch is behind 'origin/main' by 3 commits
→ Solución: git pull

# Diverged (divergido): Ambos tienen commits diferentes
Your branch and 'origin/main' have diverged,
and have 2 and 3 different commits each, respectively
→ Tienes 2 commits locales que el remoto no tiene
→ El remoto tiene 3 commits que tú no tienes
→ Necesitas reconciliar las diferencias
```

**¿Qué hacer cuando hay divergencia (diverged)?:** [🔙](#3-git-status---inspeccionando-el-estado) [🔙](#3-git-status---inspeccionando-el-estado)

```bash
# PASO 1: Investigar qué pasó
# Ver tus commits locales que no están en remoto
git log origin/main..HEAD --oneline

# Ver commits remotos que no tienes localmente
git log HEAD..origin/main --oneline

# Ver todas las diferencias
git log --oneline --graph --all

# PASO 2: Elegir estrategia de sincronización

# Opción A: MERGE (mantiene toda la historia)
git pull
# → Crea un merge commit
# → Historia completa pero más compleja
# → Recomendado para trabajo en equipo

# Opción B: REBASE (historia lineal)
git pull --rebase
# → Reaplica tus commits encima de los remotos
# → Historia más limpia
# → Recomendado para trabajo individual
# → NO usar si ya compartiste tus commits

# Opción C: FORZAR tus cambios (sobrescribir remoto)
git push --force-with-lease
# → Solo si estás SEGURO que tus cambios son correctos
# → Elimina los commits remotos
# → ⚠️ PELIGROSO en ramas compartidas

# Opción D: FORZAR cambios remotos (descartar locales)
git reset --hard origin/main
# → Descarta TUS commits locales
# → Sincroniza con remoto
# → ⚠️ Pierdes trabajo local

# PASO 3: Verificar después
git status
git log --oneline --graph --all
```

**Causas comunes de divergencia:** [🔙](#3-git-status---inspeccionando-el-estado)

```bash
# Causa 1: Usaste --amend después de push
git commit -m "A"
git push
git commit --amend -m "B"  # Cambia el commit
git push  # ❌ Error: diverged

# Causa 2: Múltiples personas trabajando en la misma rama
# Persona A: push commit 1
# Persona B: push commit 2 (sin pull primero)
# → Divergencia

# Causa 3: Push --force desde otro lugar
# Computadora A: git push --force
# Computadora B: ahora está divergida

# Causa 4: Rebase de rama ya compartida
git push
git rebase main  # Reescribe commits
git push  # ❌ Error: diverged
```

**Troubleshooting de divergencia:** [🔙](#3-git-status---inspeccionando-el-estado)

```bash
# Ver exactamente qué difiere
git diff origin/main

# Ver log comparativo
git log --left-right --oneline origin/main...HEAD
# < = commits en remoto
# > = commits locales

# Si no estás seguro qué hacer, haz backup
git branch backup-antes-de-sincronizar
# Luego puedes probar diferentes estrategias
```

**Mejores prácticas:** [🔙](#3-git-status---inspeccionando-el-estado)

```bash
✓ Ejecuta git status antes de commit (SIEMPRE)
✓ Usa -s para overview rápido
✓ Verifica tracking branch con -b

✗ No ignores el output
✗ No commitees sin revisar status primero
```

---

## Navegación

- [⬅️ Anterior: git commit](02-git-commit.md)
- [🏠 Volver al Índice](../../GIT_COMANDOS_GUIA_PRACTICA.md)
- [➡️ Siguiente: git diff](04-git-diff.md)

