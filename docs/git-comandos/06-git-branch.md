# 6. git branch - Gestionando Líneas de Desarrollo

[🏠 Volver al Índice](../../GIT_COMANDOS_GUIA_PRACTICA.md)

---

## 6. git branch - Gestionando Líneas de Desarrollo
[⬆️ Top](#tabla-de-contenidos)

**¿Qué hace?**
Crea, lista, renombra, elimina y gestiona ramas (branches). Las ramas en Git son extremadamente ligeras: solo punteros a commits, no copias de archivos.

**Funcionamiento interno:** [🔙](#6-git-branch---gestionando-líneas-de-desarrollo)

```
Crear rama:
1. Obtiene hash del commit actual (HEAD)
2. Crea archivo .git/refs/heads/nombre-rama con el hash
3. Tamaño: Solo 41 bytes (hash SHA-1 + newline)
4. Tiempo: Instantáneo (milisegundos)

Eliminar rama:
1. Verifica si está mergeada (con -d)
2. Elimina archivo .git/refs/heads/nombre-rama
3. No toca commits (quedan en reflog si es necesario recuperar)

Cambiar entre ramas:
1. Lee hash del commit de la rama destino
2. Actualiza working directory con ese tree object
3. Actualiza .git/HEAD para apuntar a la nueva rama
4. Actualiza .git/index (staging area)
```

**Uso práctico - Creación de ramas:** [🔙](#6-git-branch---gestionando-líneas-de-desarrollo)

```bash
# ============================================
# CREAR RAMAS CON GIT BRANCH
# ============================================

# 1. Crear rama sin cambiar a ella
git branch feature-x
# → Crea rama apuntando a HEAD actual
# → Te quedas en la rama actual

# 2. Crear rama desde commit específico
git branch feature-x abc123
git branch hotfix v1.2.3
# → Crea rama apuntando al commit especificado

# 3. Crear rama desde otra rama (no desde HEAD)
git branch feature-y feature-x
# → Crea feature-y apuntando donde está feature-x

# 4. Crear rama desde remota
git branch feature-x origin/feature-x
# → Crea rama local basada en remota
# → Solo crea, NO cambia a ella
# → NO configura tracking automáticamente

# 5. Copiar rama (crear con mismo contenido)
git branch nueva-copia rama-existente
# → nueva-copia apunta al mismo commit que rama-existente

# Nota: Para crear Y cambiar de rama, ver:
# - Sección "git switch" para método moderno
# - Sección "git checkout" para método clásico
```

**Uso práctico - Listar y ver ramas:** [🔙](#6-git-branch---gestionando-líneas-de-desarrollo)

```bash
# ============================================
# LISTAR RAMAS
# ============================================

# 1. Listar ramas locales
git branch
# → Muestra rama actual con *
# → Solo ramas locales

# 2. Listar todas las ramas (local + remoto)
git branch -a
# o: git branch --all
# → Locales + remotes/origin/*
# → Muy útil para ver qué hay en remoto

# 3. Listar solo ramas remotas
git branch -r
# o: git branch --remotes
# → Solo origin/main, origin/develop, etc.

# 4. Listar con último commit
git branch -v
# o: git branch --verbose
# Formato: nombre hash mensaje
# ejemplo:
#   main     abc123 Last commit message
# * feature  def456 Work in progress

# 5. Listar con información de tracking
git branch -vv
# Formato: nombre hash [upstream: ahead N, behind M] mensaje
# ejemplo:
#   main     abc123 [origin/main] Last commit
# * feature  def456 [origin/feature: ahead 2] WIP

# 6. Listar con más detalles (commit y autor)
git branch -v --abbrev-commit
git branch -vv --format="%(refname:short) %(objectname:short) %(upstream:track) %(committerdate:relative)"

# 7. Listar ramas mergeadas a rama actual
git branch --merged
# → Muestra ramas ya integradas en HEAD
# → Candidatas para eliminación
# → Solo muestra si merge fue completo

git branch --merged main
# → Ramas mergeadas a main (no necesariamente a HEAD)

# 8. Listar ramas NO mergeadas
git branch --no-merged
# → Ramas con commits únicos aún
# → Trabajo pendiente de integrar

git branch --no-merged main
# → Ramas no mergeadas a main

# 9. Listar ramas con patrón
git branch --list "feature/*"
git branch --list "*fix*"
# → Filtrado por patrón wildcard

# 10. Listar ramas que contienen commit
git branch --contains abc123
git branch --contains v1.0.0
# → Ramas que incluyen ese commit en su historia

# 11. Listar ramas que NO contienen commit
git branch --no-contains abc123
# → Ramas que no tienen ese commit

# 12. Ordenar por diferentes criterios
git branch --sort=-committerdate
# → Más recientemente modificadas primero
git branch --sort=authordate
git branch --sort=objectsize
```

**Uso práctico - Eliminar ramas:** [🔙](#6-git-branch---gestionando-líneas-de-desarrollo)

```bash
# ============================================
# ELIMINAR RAMAS
# ============================================

# 1. Eliminar rama local (safe)
git branch -d feature-x
# → Solo elimina si está mergeada
# → Previene pérdida de trabajo
# → Error si tiene commits únicos

# 2. Eliminar rama local (force)
git branch -D feature-x
# → Elimina aunque no esté mergeada
# → ⚠️ Puede perder trabajo
# → Útil para abandonar experimentos

# 3. Eliminar rama remota
git push origin --delete feature-x
# o: git push origin :feature-x (sintaxis vieja)
# → Elimina rama en GitHub/GitLab/etc
# → Referencias locales quedan (limpia con fetch --prune)

# 4. Eliminar múltiples ramas locales
git branch -d rama1 rama2 rama3
# → Elimina varias a la vez

# 5. Eliminar todas las ramas mergeadas
git branch --merged main | grep -v "^\*" | grep -v "main" | xargs git branch -d
# → Limpieza masiva de ramas ya integradas
# → Excluye main y rama actual (*)

# 6. Eliminar ramas locales cuyo remoto ya no existe
git fetch --prune
git branch -vv | grep ': gone]' | awk '{print $1}' | xargs git branch -D
# → Limpia "ramas fantasma"
# → Útil tras eliminación de ramas remotas

# 7. Forzar eliminación sin verificar merge
git branch -D feature-x feature-y hotfix-z
# → Borra múltiples sin verificación
```

**Uso práctico - Renombrar ramas:** [🔙](#6-git-branch---gestionando-líneas-de-desarrollo)

```bash
# ============================================
# RENOMBRAR RAMAS
# ============================================

# 1. Renombrar rama actual
git branch -m nuevo-nombre
# → Estás en la rama, la renombras

# 2. Renombrar otra rama (no actual)
git branch -m viejo-nombre nuevo-nombre
# → Renombras desde fuera de ella

# 3. Renombrar y actualizar remoto
git branch -m old-name new-name  # Renombrar local
git push origin :old-name        # Eliminar remoto viejo
git push origin new-name         # Subir nuevo nombre
git push origin -u new-name      # Configurar tracking

# 4. Forzar renombrado (sobrescribe si existe)
git branch -M nuevo-nombre
# → Como -m pero fuerza sobrescritura

# 5. Renombrar main a master (o viceversa)
git branch -m master main
git push -u origin main
git symbolic-ref refs/remotes/origin/HEAD refs/remotes/origin/main
```

**Uso práctico - Gestión avanzada:** [🔙](#6-git-branch---gestionando-líneas-de-desarrollo)

```bash
# ============================================
# OPERACIONES AVANZADAS
# ============================================

# 1. Mover rama a otro commit (sin checkout)
git branch -f feature-x abc123
# → Mueve puntero de feature-x al commit abc123
# → No necesitas estar en feature-x
# → ⚠️ Reescribe historia si mueves atrás

# 2. Copiar rama
git branch nueva-copia rama-original
# → Crea nueva-copia apuntando donde rama-original

# 3. Configurar upstream de rama existente
git branch -u origin/feature-x
# o: git branch --set-upstream-to=origin/feature-x
# → Configura tracking para push/pull
# → Útil si creaste rama local sin -track

# 4. Ver upstream configurado
git branch -vv
# → Muestra [origin/rama] si tiene upstream

# 5. Quitar upstream
git branch --unset-upstream
# → Elimina configuración de tracking
# → Deberás especificar remoto en push/pull

# 6. Editar descripción de rama
git branch --edit-description
# → Abre editor para añadir descripción
# → Útil para documentar propósito de rama

git branch --edit-description feature-x
# → Edita descripción de rama específica

# 7. Ver descripción de rama
git config branch.feature-x.description

# 8. Crear rama desde stash
git stash branch nueva-rama stash@{0}
# → Crea rama desde punto donde hiciste stash
# → Aplica cambios del stash
# → Elimina stash

# 9. Listar ramas con formato personalizado
git branch --format="%(refname:short) - %(authorname) - %(committerdate:short)"
# → Output personalizado
# → Ver más abajo sección completa de FORMAT

# 10. Ver ramas ordenadas por actividad
git for-each-ref --sort=-committerdate refs/heads/ --format='%(refname:short) %(committerdate:relative)'
# → Muy útil para ver qué ramas están activas
```

**Formato personalizado con --format (Completo):**

> 📖 **NOTA:** Para una referencia completa de todos los placeholders disponibles,
> formatos avanzados, condicionales y ejemplos con otros comandos (log, for-each-ref,
> show-ref, etc.), consulta la **[Sección 22: Referencias y Placeholders de Formato](#22-referencias-y-placeholders-de-formato)**.

```bash
# ============================================
# GIT BRANCH --FORMAT (PLACEHOLDERS COMPLETOS)
# ============================================

# git branch también acepta placeholders como git for-each-ref
# Ver sección 22 para lista completa de placeholders disponibles

# PLACEHOLDERS PRINCIPALES:

# Referencia
%(refname)              # refs/heads/main
%(refname:short)        # main
%(refname:lstrip=2)     # main (elimina "refs/heads/")

# Objeto
%(objectname)           # Hash completo SHA-1
%(objectname:short)     # Hash abreviado (7 chars)
%(objectname:short=10)  # Hash abreviado (10 chars)

# Commit info
%(tree)                 # Hash del árbol
%(parent)               # Hash(es) del padre
%(subject)              # Primera línea mensaje
%(body)                 # Cuerpo del mensaje
%(contents)             # Mensaje completo

# Autor
%(authorname)           # Nombre del autor
%(authoremail)          # Email del autor
%(authordate)           # Fecha del autor
%(authordate:relative)  # "2 days ago"
%(authordate:short)     # "2024-02-13"
%(authordate:iso)       # ISO 8601

# Committer
%(committername)        # Nombre del committer
%(committeremail)       # Email del committer
%(committerdate)        # Fecha del committer
%(committerdate:relative)
%(committerdate:short)
%(committerdate:iso)

# Tracking (upstream)
%(upstream)             # refs/remotes/origin/main
%(upstream:short)       # origin/main
%(upstream:track)       # [ahead 2, behind 1]
%(upstream:trackshort)  # <> (diverged), > (ahead), < (behind), = (up to date)
%(upstream:remotename)  # origin
%(upstream:remoteref)   # refs/heads/main

# Estado
%(HEAD)                 # '*' si es rama actual, ' ' si no
%(color:...)            # Aplicar color
%(if)%(then)%(else)%(end) # Condicionales


# ============================================
# EJEMPLOS PRÁCTICOS DE FORMATO
# ============================================

# 1. Lista simple con hash
git branch --format="%(refname:short) %(objectname:short)"
# Salida:
# main a1b2c3d
# develop e4f5g6h
# feature/login i7j8k9l

# 2. Con información de tracking
git branch --format="%(refname:short) → %(upstream:short) %(upstream:track)"
# Salida:
# main → origin/main [up to date]
# develop → origin/develop [ahead 2, behind 1]
# feature/login →

# 3. Con tracking abreviado (símbolos)
git branch --format="%(HEAD) %(refname:short) %(upstream:trackshort)"
# Salida:
# * main =     (actual, up to date)
#   develop <> (diverged)
#   feature >  (ahead)

# 4. Con último commit y autor
git branch --format="%(refname:short) | %(authorname) | %(committerdate:relative) | %(subject)"
# Salida:
# main | Juan Pérez | 2 days ago | Fix login bug
# develop | María García | 1 week ago | Add feature X

# 5. Con colores (visual)
git branch --format="%(if)%(HEAD)%(then)%(color:green)* %(else)  %(end)%(color:yellow)%(refname:short)%(color:reset) %(upstream:trackshort)"
# Salida coloreada:
# * main >    (verde si actual)
#   develop <  (amarillo)

# 6. Tabla alineada
git branch --format="%(align:20,left)%(refname:short)%(end) %(align:15,left)%(upstream:short)%(end) %(align:20,right)%(committerdate:short)%(end)"
# Salida:
# main                 origin/main     2024-02-13
# develop              origin/develop  2024-02-12
# feature/login                        2024-02-10

# 7. Solo ramas sin upstream
git branch --format="%(if)%(upstream)%(then)%(else)%(refname:short)%(end)" | grep -v '^$'
# Salida:
# feature/login
# hotfix/temp

# 8. Solo ramas con upstream (con estado)
git branch --format="%(if)%(upstream)%(then)%(refname:short) → %(upstream:short) %(upstream:track)%(end)" | grep -v '^$'
# Salida:
# main → origin/main [up to date]
# develop → origin/develop [ahead 2]

# 9. Formato tipo GitHub
git branch --format="%(color:bold yellow)%(refname:short)%(color:reset) %(color:dim)%(objectname:short)%(color:reset) %(subject)" --sort=-committerdate
# Salida:
# feature/new-ui a1b2c3d Add new dashboard
# develop e4f5g6h Merge feature X
# main i7j8k9l Hotfix security

# 10. Información completa para revisión
git branch --format="Rama: %(refname:short)
  Hash: %(objectname:short)
  Upstream: %(upstream:short)
  Estado: %(upstream:track)
  Último commit: %(subject)
  Autor: %(authorname)
  Fecha: %(committerdate:short)
  ---"

# 11. Solo ramas mergeadas con marca visual
git branch --merged main --format="✓ %(refname:short) (merged)"

# 12. Solo ramas NO mergeadas con marca visual
git branch --no-merged main --format="✗ %(refname:short) (%(committerdate:relative))"
# Salida:
# ✗ feature/new-ui (2 days ago)
# ✗ hotfix/urgent (5 hours ago)

# 13. Export a CSV para análisis
git branch --format="%(refname:short),%(objectname:short),%(authorname),%(authoremail),%(committerdate:short),%(subject)" > branches.csv

# 14. Buscar ramas de un autor específico
git branch --format="%(if:equals=Juan Pérez)%(authorname)%(then)%(refname:short) - %(subject)%(end)" | grep -v '^$'

# 15. Ramas con commits recientes (últimos 7 días)
git branch --format="%(if:newer=7.days.ago)%(committerdate)%(then)%(refname:short) - %(committerdate:relative)%(end)" | grep -v '^$'


# ============================================
# ORDENAMIENTO CON --sort
# ============================================

# Por fecha de commit (más recientes primero)
git branch --sort=-committerdate --format="%(committerdate:short) %(refname:short)"

# Por fecha de commit (más antiguos primero)
git branch --sort=committerdate --format="%(committerdate:short) %(refname:short)"

# Por nombre alfabético
git branch --sort=refname

# Por nombre alfabético inverso
git branch --sort=-refname

# Por fecha de autor
git branch --sort=-authordate --format="%(authordate:short) %(refname:short) %(authorname)"

# Múltiples criterios (fecha, luego nombre)
git branch --sort=-committerdate --sort=refname


# ============================================
# FILTROS COMBINADOS
# ============================================

# Ramas remotas sin merge con formato
git branch -r --no-merged main --format="%(refname:short) %(committerdate:relative)"

# Ramas locales que contienen un commit
git branch --contains abc123 --format="%(refname:short) ✓"

# Ramas locales que NO contienen un commit
git branch --no-contains abc123 --format="%(refname:short) ✗"

# Ramas con patrón y formato
git branch --list "feature/*" --format="%(refname:short) - %(subject)"


# ============================================
# CONDICIONALES AVANZADOS
# ============================================

# Mostrar solo si está ahead
git branch --format="%(if:notequals=)%(upstream:track)%(then)%(refname:short) %(upstream:track)%(end)" | grep -v '^$'

# Colorear según estado de tracking
git branch --format="%(if)%(upstream:track)%(then)%(color:red)%(else)%(color:green)%(end)%(refname:short)%(color:reset) %(upstream:track)"

# Marcar ramas sin upstream
git branch --format="%(refname:short)%(if)%(upstream)%(then) [tracked]%(else) [NO UPSTREAM]%(end)"
# Salida:
# main [tracked]
# feature/new [NO UPSTREAM]


# ============================================
# CASOS DE USO PRÁCTICOS
# ============================================

# 1. Encontrar ramas abandonadas (>3 meses sin commits)
git branch --sort=-committerdate --format="%(committerdate:short) %(refname:short)" | tail -10

# 2. Ver quién trabaja en qué
git branch --format="%(authorname): %(refname:short)" --sort=authorname

# 3. Estado de tracking de todas las ramas (dashboard)
git branch --format="%(align:25,left)%(refname:short)%(end)%(if)%(upstream)%(then)→ %(upstream:short) %(upstream:trackshort)%(else)(sin tracking)%(end)"
# Salida:
# main                     → origin/main =
# develop                  → origin/develop >
# feature/login            (sin tracking)

# 4. Ramas con commits pero sin push
git branch --format="%(if)%(upstream:track)%(then)%(if:equals=[ahead ?)%(upstream:track)%(then)%(refname:short) tiene commits locales%(end)%(end)" | grep -v '^$'

# 5. Generar comando para eliminar ramas mergeadas
git branch --merged main --format="git branch -d %(refname:short)" | grep -v "main"
# Salida (ejecutable):
# git branch -d feature-old
# git branch -d bugfix-123


# ============================================
# NOTA IMPORTANTE
# ============================================
# git branch --no-merged NO MUESTRA LA RAMA ACTUAL aunque no esté merged
# Esto es comportamiento estándar de Git

# Para verificar si tu rama actual está merged:
git branch --contains HEAD main
# o
git merge-base --is-ancestor HEAD main && echo "Está merged" || echo "NO está merged"
```

**Estrategias de branching completas:** [🔙](#6-git-branch---gestionando-líneas-de-desarrollo)

```bash
# ============================================
# FEATURE BRANCH WORKFLOW
# ============================================
# Estrategia: Una rama por feature, merge a main

main (estable, deployable)
 ├─ feature/user-authentication
 ├─ feature/payment-integration  
 ├─ feature/dashboard-redesign
 ├─ bugfix/login-timeout
 └─ hotfix/security-patch

Workflow:
git switch main
git pull origin main
git switch -c feature/nueva-feature
# ... desarrollo ...
git push -u origin feature/nueva-feature
# PR en GitHub/GitLab
# Tras aprobación:
git switch main
git merge --no-ff feature/nueva-feature
git push origin main
git branch -d feature/nueva-feature
git push origin --delete feature/nueva-feature

# ============================================
# GIT FLOW
# ============================================
# Estrategia: Ramas de largo plazo + features temporales

main (producción, solo releases)
 └─ hotfix/critical-bug → merge a main y develop
 
develop (integración, siguiente release)
 ├─ feature/feature-a → merge a develop
 ├─ feature/feature-b → merge a develop
 └─ release/v2.0.0 → merge a main y develop

Workflow nuevas features:
git switch develop
git switch -c feature/nueva-feature
# ... desarrollo ...
git switch develop
git merge --no-ff feature/nueva-feature
git branch -d feature/nueva-feature

Workflow releases:
git switch -c release/v1.5.0 develop
# ... bug fixes, versioning ...
git switch main
git merge --no-ff release/v1.5.0
git tag -a v1.5.0
git switch develop
git merge --no-ff release/v1.5.0
git branch -d release/v1.5.0

Workflow hotfixes:
git switch -c hotfix/critical main
# ... fix urgente ...
git switch main
git merge --no-ff hotfix/critical
git tag -a v1.5.1
git switch develop
git merge --no-ff hotfix/critical
git branch -d hotfix/critical

# ============================================
# GITHUB FLOW (SIMPLE)
# ============================================
# Estrategia: Solo main + ramas temporales, deploy continuo

main (siempre deployable)
 ├─ add-oauth-support
 ├─ fix-memory-leak
 └─ update-dependencies

Workflow:
git switch main
git pull origin main
git switch -c descriptive-branch-name
# ... commits ...
git push -u origin descriptive-branch-name
# Abrir Pull Request
# CI/CD ejecuta tests
# Code review
# Merge a main
# Auto-deploy a producción
# Eliminar rama

# ============================================
# TRUNK-BASED DEVELOPMENT
# ============================================
# Estrategia: Ramas de vida muy corta (<1 día), main siempre estable

main (trunk, siempre estable)
 ├─ short-lived-branch-1 (< 1 día)
 └─ short-lived-branch-2 (< 1 día)

Principios:
- Ramas viven máximo 1 día
- Commits pequeños y frecuentes
- Feature flags para features incompletas
- CI/CD muy robusto

Workflow:
git switch main
git pull origin main
git switch -c quick-fix
# ... cambio pequeño ...
git push -u origin quick-fix
# PR rápido, merge mismo día
git switch main
git pull origin main
git branch -d quick-fix
```

**Convenciones de nombres de ramas:** [🔙](#6-git-branch---gestionando-líneas-de-desarrollo)

```bash
# ============================================
# NOMENCLATURA RECOMENDADA
# ============================================

# Por tipo:
feature/user-authentication
feature/payment-gateway
bugfix/login-error
hotfix/security-vulnerability
release/v1.5.0
docs/update-readme
test/add-integration-tests
refactor/optimize-queries
chore/update-dependencies

# Por ticket/issue:
feature/JIRA-123-add-oauth
bugfix/GH-456-fix-memory-leak
hotfix/PROD-789-critical-fix

# Por desarrollador (en equipos pequeños):
john/new-dashboard
maria/fix-api

# Convenciones:
✓ Usa minúsculas
✓ Usa guiones (no underscores)
✓ Sé descriptivo pero conciso
✓ Incluye tipo de cambio
✓ Incluye referencia a ticket si existe

✗ No uses espacios
✗ No uses caracteres especiales (/, - solo)
✗ No uses nombres ambiguos ("fix", "test", "branch")
✗ No uses fechas como única identificación
```

**Troubleshooting y problemas comunes:** [🔙](#6-git-branch---gestionando-líneas-de-desarrollo)

```bash
# ============================================
# PROBLEMAS COMUNES
# ============================================

# Problema 1: No puedo cambiar de rama (cambios sin commitear)
git switch otra-rama
# error: Your local changes would be overwritten

Solución A (commitear):
git add .
git commit -m "WIP: trabajo en progreso"
git switch otra-rama

Solución B (stash):
git stash
git switch otra-rama
# ... trabajo ...
git switch rama-original
git stash pop

Solución C (forzar, ⚠️ pierdes cambios):
git switch -f otra-rama

# Problema 2: Borré rama por error
git reflog
# Encuentra el commit donde estaba la rama
git branch rama-recuperada abc123
# o:
git switch -c rama-recuperada abc123

# Problema 3: Rama no se elimina (no mergeada)
git branch -d feature-x
# error: branch not fully merged

Verificar:
git branch --no-merged
git log main..feature-x --oneline

Si realmente quieres borrar:
git branch -D feature-x

# Problema 4: Rama local dice "gone" en tracking
git branch -vv
# feature-x abc123 [origin/feature-x: gone] WIP

Causa: Rama remota fue eliminada
Solución:
git branch -D feature-x  # Si no necesitas cambios
# o
git branch --unset-upstream  # Quita tracking, mantén rama local

# Problema 5: Demasiadas ramas, repo desorganizado
# Listar ramas inactivas (más de 6 meses):
git for-each-ref --sort=-committerdate refs/heads/ \
  --format='%(refname:short) %(committerdate:relative)' | \
  tail -20

# Eliminar ramas mergeadas:
git branch --merged main | grep -v "main" | xargs git branch -d

# Problema 6: Rama con nombre incorrecto ya pusheada
git branch -m old-name new-name
git push origin :old-name new-name
git push origin -u new-name

# Problema 7: Quiero ver rama antigua sin afectar HEAD
git show rama-antigua:archivo.txt
git log rama-antigua
git diff main..rama-antigua
# Sin hacer checkout

# Problema 8: No sé en qué rama estoy
git branch
# o
git rev-parse --abbrev-ref HEAD
# o
git status | head -1
```

**Mejores prácticas:** [🔙](#6-git-branch---gestionando-líneas-de-desarrollo)

```bash
✓ Usa nombres descriptivos (feature/user-auth)
✓ Usa prefijos (feature/, bugfix/, hotfix/)
✓ Crea ramas frecuentemente (son gratis)
✓ Elimina ramas después de merge

✗ Evita nombres genéricos (test, temp)
✗ No trabajes directamente en main
✗ No uses git branch -D sin estar seguro
```

---


---

## Navegación

- [⬅️ Anterior: git log](05-git-log.md)
- [🏠 Volver al Índice](../../GIT_COMANDOS_GUIA_PRACTICA.md)
- [➡️ Siguiente: git checkout / git switch](07-git-checkout-switch.md)

