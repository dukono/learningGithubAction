# 13. git pull - Descargando e Integrando Cambios Remotos

[🏠 Volver al Índice](../../GIT_COMANDOS_GUIA_PRACTICA.md)

---

## 13. git pull - Descargando e Integrando Cambios Remotos
[⬆️ Top](#tabla-de-contenidos)

**¿Qué hace?**
`git pull` es el comando fundamental para sincronizar tu trabajo local con el repositorio remoto. Descarga los cambios que otros desarrolladores han subido e integra esos cambios en tu rama actual. Es esencial para mantener tu trabajo actualizado en entornos colaborativos.

**Funcionamiento interno:**

```
git pull es en realidad DOS comandos ejecutados secuencialmente:

git pull = git fetch + git merge (por defecto)
         o git fetch + git rebase (con --rebase)

Paso a paso:

1. FETCH (descarga):
   - Conecta al repositorio remoto
   - Descarga objetos nuevos (commits, trees, blobs)
   - Actualiza refs remotas (refs/remotes/origin/*)
   - NO toca tu working directory ni rama actual

2. MERGE o REBASE (integración):
   - Integra los cambios descargados en tu rama
   - Dos estrategias disponibles:
     a) MERGE: Crea merge commit si hay divergencia
     b) REBASE: Reaplica tus commits encima de remotos

Ejemplo visual:

Estado inicial (local):
  A---B---C---D (main, local)

Estado remoto (origin/main):
  A---B---E---F (origin/main)

Después de git fetch:
  Local:  A---B---C---D (main)
  Remoto: A---B---E---F (origin/main)
  → origin/main actualizado, pero main local NO cambia

Después de git pull (merge):
  A---B---E---F
       \       \
        C---D---M (main)
  → Merge commit M combina ambas historias

Después de git pull --rebase:
  A---B---E---F---C'---D' (main)
  → Commits C y D replicados después de F
  → Historia lineal, sin merge commit
```

**Uso práctico - Comandos básicos:**

```bash
# 1. Pull básico (fetch + merge)
git pull
# → Descarga de origin/<rama-actual>
# → Merge con rama actual
# → Puede crear merge commit

# 2. Pull de rama específica
git pull origin main
# → Descarga de origin/main
# → Integra en rama actual
# → Útil si no hay tracking configurado

# 3. Pull con rebase (historia lineal)
git pull --rebase
# → Descarga cambios remotos
# → Reaplica tus commits encima
# → NO crea merge commit
# → Historia más limpia

# 4. Pull solo si es fast-forward
git pull --ff-only
# → Solo actualiza si NO hay divergencia
# → Falla si necesitarías merge o rebase
# → Más seguro, evita merges inesperados
```

**Estrategias de integración detalladas:**

```bash
# OPCIÓN A: MERGE (--no-rebase, default en muchos casos)
git pull --no-rebase
# o: git config pull.rebase false

Ventajas:
✓ Preserva historia exacta (cuándo se integraron cambios)
✓ No reescribe commits (hashes estables)
✓ Más seguro para ramas públicas/compartidas
✓ Conflictos se resuelven una sola vez

Desventajas:
✗ Crea merge commits (historia no lineal)
✗ Log más difícil de leer con muchos merges
✗ "Ruido" visual en git log --graph

Cuándo usar:
→ Ramas compartidas (main, develop)
→ Cuando quieres preservar contexto de integración
→ Equipos que prefieren historia completa

# OPCIÓN B: REBASE (--rebase)
git pull --rebase
# o: git config pull.rebase true

Ventajas:
✓ Historia lineal y limpia
✓ Log más fácil de leer
✓ No crea merge commits innecesarios
✓ Bisect más efectivo

Desventajas:
✗ Reescribe commits locales (cambia hashes)
✗ Conflictos pueden aparecer múltiples veces
✗ Peligroso si ya pusheaste (necesitas force push)
✗ Pierde contexto de cuándo se integraron cambios

Cuándo usar:
→ Feature branches personales
→ Antes de crear Pull Request
→ Cuando prefieres historia lineal
→ Trabajo local no compartido aún

# OPCIÓN C: FAST-FORWARD ONLY (--ff-only)
git pull --ff-only
# o: git config pull.ff only

Comportamiento:
→ Solo actualiza si tu rama NO ha avanzado
→ Falla si hay divergencia (commits locales)
→ Más conservador, evita sorpresas

Ventajas:
✓ Nunca crea merge commits inesperados
✓ Nunca reescribe historia
✓ Fuerza a decidir explícitamente (merge o rebase)
✓ Más seguro para principiantes

Cuándo usar:
→ Cuando quieres control total
→ Para evitar merges automáticos
→ En scripts automatizados
```

**Manejo de conflictos durante pull:**

```bash
# Escenario: git pull genera conflictos

# Si usaste git pull (merge):
git pull
# → Auto-merging archivo.txt
# → CONFLICT (content): Merge conflict in archivo.txt

Resolver:
1. Abre archivos con conflicto
2. Busca marcadores:
   <<<<<<< HEAD
   Tu código local
   =======
   Código del remoto
   >>>>>>> origin/main

3. Edita y deja versión correcta
4. Marca como resuelto:
   git add archivo.txt
5. Completa el merge:
   git commit  # (mensaje ya preparado)

# O aborta el merge:
git merge --abort
# → Vuelve al estado antes del pull

# Si usaste git pull --rebase:
git pull --rebase
# → Applying: Tu commit local
# → CONFLICT: archivo.txt

Resolver:
1. Resuelve conflicto (igual que arriba)
2. Marca como resuelto:
   git add archivo.txt
3. Continúa el rebase:
   git rebase --continue

# O aborta el rebase:
git rebase --abort
# → Vuelve al estado antes del pull

# O salta el commit conflictivo:
git rebase --skip
# → Omite tu commit (úsalo con cuidado)
```

**Opciones avanzadas:**

```bash
# 1. Pull con autostash (stash automático)
git pull --autostash
# → Guarda cambios no commiteados automáticamente
# → Hace pull
# → Restaura cambios guardados
# → Útil cuando tienes trabajo en progreso

# 2. Pull con estrategia de merge
git pull -X ours
# → En conflictos, prefiere versión LOCAL
# → Útil en merges complicados

git pull -X theirs
# → En conflictos, prefiere versión REMOTA
# → Usa con cuidado

# 3. Pull sin commit (solo merge)
git pull --no-commit
# → Hace merge pero NO commitea
# → Te da oportunidad de revisar
# → Útil para inspeccionar antes de finalizar

# 4. Pull verbose
git pull --verbose
# → Muestra información detallada
# → Útil para debugging

# 5. Pull desde múltiples remotos
git pull upstream main
# → Pull desde otro remoto (no origin)
# → Útil en forks

# 6. Pull con profundidad limitada
git pull --depth=10
# → Solo últimos 10 commits
# → Útil en repos gigantes
```

**Verificación antes y después de pull:**

```bash
# ANTES de pull - ver qué traerás:

# 1. Ver commits que te faltan
git fetch
git log HEAD..origin/main --oneline
# → Commits que traerá el pull

# 2. Ver cambios en archivos
git fetch
git diff HEAD...origin/main --name-status
# → Archivos que cambiaron en remoto

# 3. Ver si hay divergencia
git fetch
git status
# → Dice "have diverged" si hay commits locales y remotos

# DESPUÉS de pull - verificar qué cambió:

# 1. Ver commits traídos (usando reflog)
git log HEAD@{1}..HEAD --oneline
# → HEAD@{1} = posición antes del pull
# → HEAD = posición actual
# → Muestra SOLO commits nuevos traídos

# Alternativa con ORIG_HEAD:
git log ORIG_HEAD..HEAD --oneline
# → ORIG_HEAD también apunta al estado pre-pull

# 2. Ver cambios en archivos traídos
git diff --name-status HEAD@{1} HEAD
# → Lista archivos modificados, añadidos, eliminados
# → M = modified, A = added, D = deleted

git diff --stat HEAD@{1} HEAD
# → Resumen con estadísticas por archivo

# 3. Ver diff completo de los cambios
git diff HEAD@{1} HEAD
# → Muestra todas las diferencias línea por línea
# → Útil para revisar qué código cambió exactamente

# 4. Ver detalle de cada commit traído
git show <hash-commit>
# → Muestra mensaje, autor, fecha, y diff del commit
# → Repite para cada commit del log anterior

# Ejemplo completo de revisión:
git show HEAD~2  # Ver penúltimo commit
git show HEAD~1  # Ver último commit
git show HEAD    # Ver commit actual

# 5. Ver qué archivos específicos cambiaron
git diff --name-only HEAD@{1} HEAD
# → Solo nombres de archivos, sin estadísticas

git diff HEAD@{1} HEAD -- archivo.txt
# → Diff de archivo específico

# 6. Ver si quedaron conflictos sin resolver
git status
# → Debe estar limpio
# → Si dice "Unmerged paths", hay conflictos pendientes

# 7. Ver diferencias con remoto (debe estar sincronizado)
git diff origin/main
# → Debería estar vacío si pull fue exitoso
# → Si hay diferencias, tienes commits locales sin pushear

# 8. Ver cuántos commits se trajeron
git rev-list --count HEAD@{1}..HEAD
# → Número de commits traídos

# 9. Ver resumen visual con grafo
git log HEAD@{1}..HEAD --oneline --graph --stat
# → Combinación visual con archivos y estadísticas
```

**Ejemplo práctico completo - Después de pull:** [🔙](#5-git-log---explorando-la-historia)

```bash
# Acabas de hacer: git pull
# Quieres saber QUÉ cambió

# Paso 1: Ver cuántos commits se trajeron
$ git log HEAD@{1}..HEAD --oneline
a1b2c3d (HEAD -> main, origin/main) Fix: Corregir bug en login
d4e5f6g Feature: Añadir validación de email
h7i8j9k Docs: Actualizar README

# → Se trajeron 3 commits

# Paso 2: Ver qué archivos cambiaron
$ git diff --name-status HEAD@{1} HEAD
M       src/auth/login.js
A       src/validators/email.js
M       README.md
D       src/old-validator.js

# → 2 modificados, 1 añadido, 1 eliminado

# Paso 3: Ver estadísticas
$ git diff --stat HEAD@{1} HEAD
 README.md                | 15 ++++++++++++++-
 src/auth/login.js        | 8 +++-----
 src/old-validator.js     | 45 -------------------------------------------
 src/validators/email.js  | 30 ++++++++++++++++++++++++++++
 4 files changed, 47 insertions(+), 51 deletions(-)

# Paso 4: Ver detalle de commit específico
$ git show a1b2c3d
commit a1b2c3d...
Author: John Doe <john@example.com>
Date:   Mon Feb 10 10:30:00 2026

    Fix: Corregir bug en login

    - Validación de contraseña mejorada
    - Manejo de errores actualizado

diff --git a/src/auth/login.js b/src/auth/login.js
...
(muestra el diff completo)

# Paso 5: Ver diff de archivo específico
$ git diff HEAD@{1} HEAD -- src/auth/login.js
(muestra solo cambios en ese archivo)

# Paso 6: Verificar sincronización con remoto
$ git diff origin/main
# (vacío = perfectamente sincronizado)
# Si hay diferencias, tienes commits locales sin pushear

# Paso 7: Ver cuántos commits se trajeron
$ git rev-list --count HEAD@{1}..HEAD
# → Número de commits traídos

# Paso 8: Ver resumen visual con grafo
git log HEAD@{1}..HEAD --oneline --graph --stat
# → Combinación visual con archivos y estadísticas
```

**Comandos rápidos de verificación post-pull:** [🔙](#5-git-log---explorando-la-historia)

```bash
# Ver últimos 5 commits (incluyendo los traídos)
git log -5 --oneline

# Ver archivos modificados en últimos 3 commits
git log -3 --name-status --oneline

# Ver todo lo traído con contexto visual
git log HEAD@{1}..HEAD --oneline --graph --decorate --stat

# Comparar tu código actual vs hace 2 pulls
git diff HEAD@{2} HEAD
```

**⚠️ Notas importantes sobre HEAD@{n}:** [🔙](#5-git-log---explorando-la-historia)

```bash
# HEAD@{n} es del REFLOG (historial de operaciones)
# Solo se mantiene por tiempo limitado (default 90 días)

# Ver historial completo de HEAD:
git reflog
# Muestra todas las operaciones que movieron HEAD

# Si hiciste múltiples operaciones después del pull:
HEAD@{0}  → Estado actual
HEAD@{1}  → Operación anterior (puede NO ser el pull)
HEAD@{2}  → Dos operaciones atrás
HEAD@{n}  → n-ésima operación atrás

# Para asegurar que comparas con el pull correcto:
git reflog
# Busca la línea del pull
# Usa ese número específico

# Alternativa más segura si no estás seguro:
# Anota el hash ANTES de hacer pull:
git rev-parse HEAD  # Copia este hash
git pull
git log <hash-copiado>..HEAD --oneline
# → Garantiza comparación correcta
```

**Configuración recomendada:**

```bash
# Configurar estrategia por defecto (rebase)
git config --global pull.rebase true
# → Todos los pulls usarán rebase

# O configurar para fast-forward only
git config --global pull.ff only
# → Fuerza a especificar --rebase o --no-rebase

# Habilitar autostash con rebase
git config --global rebase.autoStash true
# → Stash automático en rebases

# Ver configuración actual
git config --get pull.rebase
git config --get pull.ff

# Configurar por repositorio (sin --global)
cd /ruta/proyecto
git config pull.rebase true
# → Solo afecta ese repositorio
```

**Situaciones comunes y soluciones:**

```bash
# PROBLEMA 1: "divergent branches" al hacer pull
git pull
# → hint: You have divergent branches...

Solución 1: Configurar estrategia
git config pull.rebase false  # merge
git pull

Solución 2: Especificar en comando
git pull --rebase

Solución 3: Fast-forward solo
git pull --ff-only

# PROBLEMA 2: Tracking branch no configurado
git pull
# → fatal: No remote repository specified

Solución:
git pull origin main  # Especifica remoto y rama
# O configura tracking:
git branch --set-upstream-to=origin/main main

# PROBLEMA 3: Cambios locales sin commitear
git pull
# → error: Your local changes would be overwritten

Solución 1: Commitea
git add .
git commit -m "WIP"
git pull

Solución 2: Stash
git stash
git pull
git stash pop

Solución 3: Autostash
git pull --autostash

# PROBLEMA 4: Necesitas forzar (después de rebase local)
git push
# → rejected (non-fast-forward)

Solución:
git push --force-with-lease
# → Solo fuerza si nadie más actualizó
```

**Workflows recomendados:**

```bash
# WORKFLOW 1: Feature branch (rebase)
# Situación: Trabajas en feature, main avanzó

# En feature branch:
git checkout feature-x
git pull origin main --rebase
# → Reaplica tus commits de feature-x encima de main actualizado
# → Historia lineal
# → Preparado para PR limpio

# WORKFLOW 2: Main branch (merge)
# Situación: Actualizas main local

git checkout main
git pull
# → Simple merge si es necesario
# → Preserva historia

# WORKFLOW 3: Sync fork con upstream
# Situación: Tu fork desactualizado

git fetch upstream
git checkout main
git pull upstream main
git push origin main
# → Actualiza tu fork desde original

# WORKFLOW 4: Colaboración continua
# Situación: Varios devs en misma rama

git pull --rebase --autostash
# → Stash auto, rebase, restaura
# → Mantiene historia limpia
# → Conveniente para trabajo continuo
```

**Comparación visual: merge vs rebase en pull:**

```bash
# ESCENARIO INICIAL:
Local:  A---B---C---D (main)
Remoto: A---B---E---F (origin/main)

# PULL CON MERGE (git pull --no-rebase):
A---B---E---F
     \       \
      C---D---M (main)

Características:
- Merge commit M con 2 líneas de commits
- Historia completa preservada
- Graph no lineal
- Hashes de C y D sin cambiar

# PULL CON REBASE (git pull --rebase):
A---B---E---F---C'---D' (main)

Características:
- Sin merge commit
- Historia lineal
- C y D reescritos (C' y D' con nuevos hashes)
- Más limpio visualmente

# PULL CON FF-ONLY (cuando no hay commits locales):
Local antes:  A---B (main)
Remoto:       A---B---E---F (origin/main)

Después:      A---B---E---F (main)

- Sin merge commit
- Sin rebase
- Solo movió puntero
- Ideal cuando solo necesitas actualizar
```

**Mejores prácticas:**

```bash
✓ Pull frecuentemente (al menos diariamente)
✓ Commitea o stash antes de pull
✓ Usa --rebase en feature branches personales
✓ Usa merge en ramas compartidas (main, develop)
✓ Configura pull.rebase según tu workflow
✓ Revisa cambios con git fetch primero
✓ Resuelve conflictos inmediatamente
✓ Usa --autostash para conveniencia
✓ Verifica con git status después de pull
✓ Comunica force pushes al equipo

✗ NO hagas pull sin revisar en ramas importantes
✗ NO ignores conflictos y continúes trabajando
✗ NO uses pull --rebase en commits ya pusheados
✗ NO hagas pull con cambios críticos sin commitear
✗ NO mezcles estrategias (elige merge O rebase)
✗ NO uses -X ours/-X theirs sin entender
✗ Evita pull en detached HEAD
✗ NO hagas pull sin tracking branch claro
```

**Debugging y troubleshooting:**

```bash
# Ver qué hará pull sin ejecutarlo
git fetch
git log HEAD..@{u} --oneline  # @{u} = upstream branch
git diff HEAD...@{u} --stat

# Ver configuración de tracking
git branch -vv
# → Muestra upstream de cada rama

# Ver qué remote y branch usa pull
git remote -v
git rev-parse --abbrev-ref --symbolic-full-name @{u}

# Simular pull con dry-run (no existe, pero puedes):
git fetch --dry-run  # Solo para fetch
# Luego inspecciona con git log

# Ver reflog después de pull problemático
git reflog
# → Encuentra estado anterior
git reset --hard HEAD@{1}  # Vuelve atrás

# Ver qué estrategia está configurada
git config --get-all pull.rebase
git config --get-all pull.ff
```

---


---

## Navegación

- [⬅️ Anterior: git fetch](12-git-fetch.md)
- [🏠 Volver al Índice](../../GIT_COMANDOS_GUIA_PRACTICA.md)
- [➡️ Siguiente: git push](14-git-push.md)

