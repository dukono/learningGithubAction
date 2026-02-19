# 15. git reset - Moviendo Referencias

[🏠 Volver al Índice](../../GIT_COMANDOS_GUIA_PRACTICA.md)

---

## 15. git reset - Moviendo Referencias
[⬆️ Top](#15-git-reset---moviendo-referencias)

**¿Qué hace?**
Mueve el puntero HEAD y la rama actual a otro commit, con la opción de qué hacer con los cambios que "quedan atrás": conservarlos en staging, en working directory, o descartarlos.

Es la herramienta para "deshacer" commits o preparaciones (git add) de forma local (antes de hacer push).

**Funcionamiento interno:** [🔙](#15-git-reset---moviendo-referencias)

```
Situación: tienes estos commits y quieres deshacer el último:
  A → B → C  (HEAD, main)

git reset HEAD~1 → mueve main de C a B

Los tres modos controlan qué pasa con los cambios de C:

--soft:  A → B  (HEAD)    Los cambios de C: en STAGING
--mixed: A → B  (HEAD)    Los cambios de C: en WORKING DIRECTORY
--hard:  A → B  (HEAD)    Los cambios de C: ELIMINADOS (pérdida de datos)

Internamente:
1. Actualiza refs/heads/<rama> para que apunte al commit destino
2. Según el modo:
   --soft:  No toca index ni working
   --mixed: Actualiza .git/index para que coincida con el nuevo HEAD
   --hard:  Actualiza index Y working directory para coincidir con el nuevo HEAD
```

**Todas las opciones importantes:** [🔙](#15-git-reset---moviendo-referencias)

```bash
# ============================================
# 1. --soft: deshacer commit, conservar cambios en STAGING
# ============================================
# Situación: Hiciste un commit pero quieres reescribir el mensaje
# o añadir más archivos al mismo commit. Los cambios vuelven
# al staging listos para ser comiteados de nuevo.
git reset --soft HEAD~1
# → Commit deshecho
# → Cambios vuelven a staging (como si hubieras hecho git add)
# → Útil para: corregir mensaje, añadir archivos olvidados, combinar commits


# ============================================
# 2. --mixed (DEFAULT): deshacer commit, cambios en WORKING DIRECTORY
# ============================================
# Situación: Hiciste un commit pero quieres revisar y reorganizar
# qué exactamente commitear. Los cambios vuelven sin estar en staging,
# para que tú decidas qué añadir y qué no.
git reset HEAD~1
# ó explícitamente:
git reset --mixed HEAD~1
# → Commit deshecho
# → Cambios vuelven a working directory (NO en staging)
# → Útil para: reorganizar un commit grande en varios más pequeños


# ============================================
# 3. --hard: deshacer commit Y ELIMINAR todos los cambios
# ============================================
# Situación: Quieres descartar COMPLETAMENTE el último commit y sus cambios.
# O quieres sincronizar con el remoto descartando todo lo local.
git reset --hard HEAD~1
# → Commit deshecho
# → TODOS los cambios de ese commit se PIERDEN
# → Working directory y staging quedan limpios
# ⚠️ PELIGROSO: no recuperable sin reflog


# ============================================
# 4. Quitar archivo del staging (unstage)
# ============================================
# Situación: Hiciste "git add ." y accidentalmente incluiste un archivo
# que no debería ir en el commit. Quieres quitarlo del staging
# sin perder tus cambios en el archivo.
git reset HEAD archivo.txt
# ó en Git moderno (equivalente):
git restore --staged archivo.txt
# → El archivo vuelve al working directory (sin staging)
# → El archivo NO se modifica, solo sale del staging


# ============================================
# 5. Reset a commit específico (no solo el anterior)
# ============================================
# Situación: Quieres deshacer los últimos 3 commits.
git reset --soft HEAD~3      # Los 3 commits → staging
git reset HEAD~3             # Los 3 commits → working directory
git reset --hard HEAD~3      # Los 3 commits → ELIMINADOS

# Reset a un commit por su hash:
git reset --soft abc1234
git reset --hard abc1234


# ============================================
# 6. Sincronizar con el remoto (descartar todo lo local)
# ============================================
# Situación: Tu rama local y la remota divergieron.
# Quieres que tu rama quede EXACTAMENTE como la remota,
# descartando todos tus commits locales.
git fetch origin
git reset --hard origin/main
# → Tu rama local queda idéntica al remoto
# ⚠️ Perderás todos tus commits locales no pusheados


# ============================================
# 7. Unstage de directorio completo
# ============================================
git reset HEAD directorio/
# → Quita todo el directorio del staging
```

# 6. Reset a remoto
git reset --hard origin/main
# → Sincroniza con remoto, descartando cambios locales

# 7. Reset de un directorio específico
git reset HEAD directorio/
```

**FLUJO DE ESTADOS con reset:** [🔙](#15-git-reset---moviendo-referencias)

```bash
# ESTADOS EN GIT:
# Working Directory → Staging (Index) → Commit → Remote
#
# COMANDOS PARA AVANZAR:
# Working → Staging:   git add <archivo>
# Staging → Commit:    git commit
# Commit → Remote:     git push
#
# COMANDOS PARA RETROCEDER (reset):
# Staging → Working:   git reset HEAD <archivo>
# Commit → Staging:    git reset --soft HEAD~1
# Commit → Working:    git reset --mixed HEAD~1 (default)
# Commit → (borrado):  git reset --hard HEAD~1 (PELIGRO)
```

**Casos de uso prácticos:** [🔙](#15-git-reset---moviendo-referencias)

```bash
# Caso 1: Quitar un archivo del último commit
git reset --soft HEAD~1     # Deshace commit → archivos a staging
git reset HEAD archivo.txt  # Quita archivo del staging
git commit -m "Mensaje"     # Recommitea sin ese archivo

# Caso 2: Rehacer último commit con más cambios
git reset --soft HEAD~1     # Deshace commit → archivos a staging
git add mas-cambios.txt     # Añade más archivos
git commit -m "Mensaje completo"

# Caso 3: Deshacer commit y revisar cambios
git reset HEAD~1            # Cambios a working directory
git diff                    # Revisa qué cambiaste
git add -p                  # Añade selectivamente
git commit -m "Mejor mensaje"

# Caso 4: Unstage archivo antes de commit
git add .                   # Añadiste todo
git reset HEAD config.txt   # Quitas un archivo del staging
git commit -m "Mensaje"     # Commiteas sin config.txt

# Caso 5: Limpiar todo y empezar de nuevo
git reset --hard HEAD       # Descarta TODOS los cambios
git clean -fd               # Elimina archivos untracked

# Caso 6: Deshacer múltiples commits
git reset --soft HEAD~3     # Deshace 3 commits → staging
git commit -m "Squashed commit"  # Un solo commit
```

**Comparación de modos:** [🔙](#15-git-reset---moviendo-referencias)

```bash
git reset --soft HEAD~1
→ Commit deshecho
→ Cambios en staging ✓
→ Working intacto ✓

git reset HEAD~1  (mixed, default)
→ Commit deshecho
→ Cambios en working ✓
→ Staging limpio

git reset --hard HEAD~1
→ Commit deshecho
→ Staging limpio
→ Working limpio
→ ¡CAMBIOS PERDIDOS!
```

**Reset vs Revert:** [🔙](#15-git-reset---moviendo-referencias)

```bash
RESET (reescribe historia):
→ Mueve rama atrás
→ Commits "desaparecen"
→ Solo para commits locales

REVERT (preserva historia):
→ Crea nuevo commit que deshace
→ Historia intacta
→ Seguro para commits públicos
```

**Recuperación:** [🔙](#15-git-reset---moviendo-referencias)

```bash
# Si hiciste reset por error:
git reflog
git reset --hard HEAD@{1}
```

**Troubleshooting común:** [🔙](#15-git-reset---moviendo-referencias)

```bash
# Problema 1: Hice reset --hard por error
# Solución: Usar reflog para recuperar
git reflog                  # Encuentra el commit perdido
git reset --hard HEAD@{2}   # Vuelve a ese estado

# Problema 2: No sé qué modo de reset usar
# Solución:
# --soft:  Solo quieres rehacer el commit, mantener cambios en staging
# --mixed: Quieres revisar/reorganizar antes de commitear de nuevo
# --hard:  Quieres BORRAR todo (úsalo con cuidado)

# Problema 3: Reset no funciona como esperaba
# Solución: Verifica el estado antes y después
git log --oneline           # Ve dónde estás
git reset --soft HEAD~1
git status                  # Verifica que cambios están en staging

# Problema 4: Quiero deshacer reset
# Solución: Usar reflog
git reflog
git reset --hard HEAD@{1}   # Vuelve al estado anterior

# Problema 5: Reset en rama compartida
# Solución: NO hagas reset en ramas públicas
# Usa git revert en su lugar (ver sección de revert)
```

**Mejores prácticas:** [🔙](#15-git-reset---moviendo-referencias)

```bash
✓ Usa --soft para reorganizar commits
✓ Usa --mixed para unstage
✓ Usa --hard solo si estás seguro
✓ Recuerda: reflog es tu red de seguridad

✗ No uses reset --hard en commits públicos
✗ No uses reset en main/develop compartidos
✗ Evita reset --hard sin verificar cambios
```

---


---

## Navegación

- [⬅️ Anterior: git push](14-git-push.md)
- [🏠 Volver al Índice](../../GIT_COMANDOS_GUIA_PRACTICA.md)
- [➡️ Siguiente: git stash](16-git-stash.md)

