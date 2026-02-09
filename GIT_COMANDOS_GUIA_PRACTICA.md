# GIT - GUÍA PRÁCTICA DE COMANDOS

> **Objetivo:** Guía completa y práctica de los comandos Git más importantes, con ejemplos del mundo real, opciones avanzadas, casos de uso y mejores prácticas.

---

<a id="tabla-de-contenidos"></a>
## 📚 Tabla de Contenidos

### COMANDOS BÁSICOS ESENCIALES
1. [git add - Preparando Cambios](#1-git-add---preparando-cambios)
2. [git commit - Guardando la Historia](#2-git-commit---guardando-la-historia)
3. [git status - Inspeccionando el Estado](#3-git-status---inspeccionando-el-estado)
4. [git diff - Comparando Cambios](#4-git-diff---comparando-cambios)
5. [git log - Explorando la Historia](#5-git-log---explorando-la-historia)

### GESTIÓN DE RAMAS
6. [git branch - Gestionando Líneas de Desarrollo](#6-git-branch---gestionando-líneas-de-desarrollo)
7. [git checkout / git switch - Navegando el Código](#7-git-checkout--git-switch---navegando-el-código)
8. [git merge - Integrando Cambios](#8-git-merge---integrando-cambios)
9. [git rebase - Reescribiendo Historia](#9-git-rebase---reescribiendo-historia)

### TRABAJO CON REMOTOS
10. [git clone - Copiando Repositorios](#10-git-clone---copiando-repositorios)
11. [git remote - Gestionando Repositorios Remotos](#11-git-remote---gestionando-repositorios-remotos)
12. [git fetch - Descargando Cambios](#12-git-fetch---descargando-cambios)
13. [git pull - Descargando e Integrando Cambios Remotos](#13-git-pull---descargando-e-integrando-cambios-remotos)
14. [git push - Subiendo Cambios](#14-git-push---subiendo-cambios)

### CONTROL DE ESTADO Y VERSIONES
15. [git reset - Moviendo Referencias](#15-git-reset---moviendo-referencias)
16. [git stash - Guardado Temporal](#16-git-stash---guardado-temporal)
17. [git tag - Marcando Versiones](#17-git-tag---marcando-versiones)
18. [git revert - Deshaciendo Commits Públicos](#18-git-revert---deshaciendo-commits-públicos)
19. [git cherry-pick - Aplicando Commits Selectivos](#19-git-cherry-pick---aplicando-commits-selectivos)

### LIMPIEZA Y MANTENIMIENTO
20. [git clean - Limpiando Archivos No Rastreados](#20-git-clean---limpiando-archivos-no-rastreados)
21. [git rm y git mv - Eliminando y Moviendo Archivos](#21-git-rm-y-git-mv---eliminando-y-moviendo-archivos)

---

## INTRODUCCIÓN

Esta guía cubre los **21 comandos Git más importantes** que todo desarrollador debe conocer, desde principiante hasta experto. Cada comando incluye:

✅ **Funcionamiento interno** - Qué hace Git bajo el capó
✅ **15-20+ opciones y flags** - Uso básico a avanzado  
✅ **10+ casos de uso reales** - Ejemplos del mundo profesional
✅ **Troubleshooting completo** - Problemas y soluciones
✅ **Mejores prácticas** - Qué hacer y qué evitar

**Relación con otros documentos:**
- Para teoría y funcionamiento interno: Ver `GIT_FUNCIONAMIENTO_INTERNO.md`
- Para GitHub Actions: Ver `GITHUB_ACTIONS_*.md`

---

## 1. git add - Preparando Cambios
[⬆️ Top](#tabla-de-contenidos)

**¿Qué hace?**
Prepara cambios del working directory para el próximo commit, moviéndolos al staging area (index).

**Funcionamiento interno:**

```
Internamente hace:
1. git hash-object -w file.txt
   → Calcula SHA-1 del contenido
   → Comprime con zlib
   → Guarda blob en .git/objects/

2. git update-index --add file.txt
   → Actualiza .git/index con:
     - Ruta del archivo
     - Hash del blob
     - Permisos (100644, 100755, etc.)
     - Timestamp

Resultado:
- Blob creado en objects/
- Index (.git/index) actualizado
- Working directory NO cambia
- Repository NO cambia (aún no hay commit)
```

**Uso práctico y opciones:**

```bash
# 1. Añadir archivo específico
git add archivo.txt
# → Stagea solo archivo.txt

# 2. Añadir todos los archivos modificados y nuevos
git add .
# → Stagea todo desde directorio actual
# → Incluye subdirectorios
# → Respeta .gitignore

# 3. Añadir todos los archivos del repositorio
git add -A
# o: git add --all
# → Stagea TODO: nuevos, modificados, eliminados
# → Desde cualquier directorio

# 4. Añadir solo archivos rastreados (ignora nuevos)
git add -u
# o: git add --update
# → Solo archivos ya en Git
# → NO añade archivos nuevos
# → Útil para "actualizar solo lo existente"

# 5. Añadir interactivamente (PODER REAL)
git add -i
# → Modo interactivo con menú
# → Puedes elegir qué hacer con cada archivo

# 6. Añadir por parches (SUPER ÚTIL)
git add -p archivo.txt
# o: git add --patch
# → Te muestra cada "hunk" de cambios
# → Preguntas: Stage this hunk? [y,n,q,a,d,s,e,?]
# → Puedes stagear solo PARTE de un archivo
```

**Caso de uso real: Commits atómicos con -p**

```bash
Escenario: Modificaste un archivo con 2 features diferentes

# archivo.py tiene:
# - Cambio A: Nueva función calculate()
# - Cambio B: Fix bug en validate()

# Quieres 2 commits separados:

# Paso 1: Stagea solo cambios de calculate()
git add -p archivo.py
# → Ves el hunk con calculate()
# → Presionas 'y' (yes)
# → Ves el hunk con validate()
# → Presionas 'n' (no)

git commit -m "feat: Add calculate function"

# Paso 2: Stagea el resto
git add archivo.py
git commit -m "fix: Fix validation bug"

Resultado: 2 commits atómicos, historia más clara
```

**Opciones avanzadas de add -p:**

```
Durante git add -p, opciones disponibles:

y - Stage this hunk (sí, añadir este cambio)
n - Do not stage (no, saltar)
q - Quit (salir, no procesar más)
a - Stage this and all remaining hunks (todos los siguientes)
d - Do not stage this or any remaining (ninguno de los siguientes)
s - Split into smaller hunks (dividir en partes más pequeñas)
e - Manually edit hunk (editar manualmente)
? - Help (ayuda)

Opción 's' (split) es PODEROSA:
→ Si un hunk tiene múltiples cambios cercanos
→ Puedes intentar dividirlo en hunks más pequeños
→ Para control más granular

Opción 'e' (edit) es para EXPERTOS:
→ Abre editor con el diff
→ Puedes editar líneas manualmente
→ Útil cuando 's' no divide suficiente
```

**Patrones de uso comunes:**

```bash
# Patrón 1: Añadir por tipo de archivo
git add *.py          # Solo archivos Python
git add src/          # Todo en directorio src/
git add "*.txt"       # Todos los .txt (comillas para expansión)

# Patrón 2: Añadir excepto algunos
git add .
git reset HEAD archivo-no-deseado.txt
# → Añade todo, luego quita uno

# Patrón 3: Añadir forzando (ignorar .gitignore)
git add -f archivo-ignorado.log
# → Fuerza añadir aunque esté en .gitignore
# → Úsalo con CUIDADO

# Patrón 4: Dry run (ver qué se añadiría)
git add -n .
# o: git add --dry-run .
# → Muestra qué se añadiría sin hacerlo

# Patrón 5: Añadir con verbose
git add -v archivo.txt
# → Muestra qué archivos se añaden
```

**Ver qué está stageado:**

```bash
# Ver estado
git status
# → Muestra archivos stageados y no stageados

# Ver diferencias stageadas
git diff --staged
# o: git diff --cached
# → Muestra QUÉ cambios están en staging

# Ver diferencias NO stageadas
git diff
# → Muestra cambios en working directory
# → Que NO están en staging
```

**Mejores prácticas:**

```bash
✓ Usa git add -p para commits granulares
✓ Revisa con git diff --staged antes de commit
✓ No uses git add . ciegamente, revisa qué añades
✓ Usa .gitignore para archivos que nunca deben añadirse
✓ Considera git add -u cuando solo actualizas existentes

✗ Evita git add * (puede añadir archivos no deseados)
✗ No uses git add -f a menos que sea absolutamente necesario
✗ No stagees archivos generados (builds, logs, node_modules)
```
# → Ves hunk con feature B: presionas 'n'
git commit -m "feat: Add feature A"

git add archivo.py
git commit -m "feat: Add feature B"
```

**Mejores prácticas:**

```bash
✓ Usa git add -p para commits granulares
✓ Revisa con git diff --staged antes de commit
✓ Usa .gitignore para archivos que nunca deben añadirse

✗ Evita git add * (puede añadir no deseados)
✗ No stagees archivos generados (builds, node_modules)
```

---

## 2. git commit - Guardando la Historia
[⬆️ Top](#tabla-de-contenidos)

**¿Qué hace?**
Crea un snapshot inmutable del proyecto con los cambios del staging area.

**Funcionamiento interno:**
```
1. Crea tree object del staging
2. Crea commit object con tree + parent + metadata
3. Actualiza referencia de rama
4. Actualiza reflog
```

**Uso práctico:**

```bash
# 1. Commit básico
git commit -m "Mensaje descriptivo"

# 2. Mensaje multilínea (título + descripción)
git commit -m "Título corto" -m "Descripción detallada más larga"

# 3. Abrir editor para mensaje largo
git commit
# → Se abre tu editor configurado
# → Primera línea = título
# → Línea vacía
# → Resto = descripción

# 4. Add + commit automático (SOLO archivos tracked)
git commit -am "Mensaje"
# o: git commit --all -m "Mensaje"
# → Añade y commitea archivos modificados
# → NO añade archivos nuevos (untracked)
# → Útil para cambios rápidos

# 5. Modificar último commit (IMPORTANTE)
git commit --amend -m "Nuevo mensaje"
# → Reemplaza el último commit
# → Útil para corregir errores

# 6. Amend sin cambiar mensaje
git commit --amend --no-edit
# → Añade cambios al último commit
# → Mantiene el mensaje original

# 7. Amend solo el mensaje
git commit --amend
# → Abre editor para cambiar mensaje
# → No añade cambios nuevos

# 8. Commit vacío (útil para CI/CD)
git commit --allow-empty -m "Trigger CI"
# → Crea commit sin cambios
# → Útil para forzar rebuild

# 9. Commit con fecha específica
git commit -m "Mensaje" --date="2024-01-15 10:30:00"
# → Sobrescribe fecha del commit

# 10. Commit como otro autor
git commit -m "Mensaje" --author="Nombre <email@ejemplo.com>"
# → Útil para pair programming
# → O commits de otros

# 11. Commit sin hooks
git commit -m "Mensaje" --no-verify
# o: git commit -m "Mensaje" -n
# → Omite pre-commit y commit-msg hooks
# → Úsalo con CUIDADO

# 12. Commit con template
git commit -t plantilla.txt
# → Usa archivo como plantilla de mensaje

# 13. Commit verboso (muestra diff)
git commit -v
# → Muestra diff en el editor
# → Ayuda a escribir mejor mensaje

# 14. Commit solo de archivos específicos
git commit archivo1.txt archivo2.txt -m "Mensaje"
# → Commitea solo esos archivos (deben estar staged)

# 15. Commit con firma GPG
git commit -S -m "Signed commit"
# → Firma el commit con tu clave GPG
# → Verifica identidad del autor

# 16. Reutilizar mensaje de otro commit
git commit -C <commit-hash>
# → Copia mensaje de otro commit
# O editar el mensaje:
git commit -c <commit-hash>
```

**Casos de uso del --amend:**

```bash
# Caso 1: Olvidaste un archivo
git add archivo-olvidado.txt
git commit --amend --no-edit
# → Añade el archivo al último commit

# Caso 2: Error de escritura en mensaje
git commit --amend -m "Mensaje corregido"
# → Corrige el mensaje del último commit

# Caso 3: Añadir más cambios al último commit
git add mas-cambios.txt
git commit --amend
# → Añade cambios y edita mensaje si quieres

# ⚠️ IMPORTANTE: Solo usa --amend en commits NO pusheados
# Si ya hiciste push, necesitarás force push (peligroso en ramas compartidas)
```

**Opciones de formato de mensaje:**

```bash
# Mensaje desde archivo
git commit -F mensaje.txt

# Mensaje desde stdin
echo "Mi mensaje" | git commit -F -

# Limpiar espacios del mensaje
git commit --cleanup=strip -m "  Mensaje con espacios  "
# → Elimina espacios extra

# Mantener mensaje tal cual
git commit --cleanup=verbatim -m "Mensaje exacto"
```

**Commits interactivos:**

```bash
# Commit interactivo (elige qué añadir)
git commit -p
# → Similar a git add -p + commit
# → Selecciona hunks a commitear
```

**Mensajes de commit efectivos (Conventional Commits):**

```bash
feat: Add user authentication
fix: Fix login validation bug
docs: Update README
style: Format code
refactor: Simplify auth logic
test: Add integration tests
chore: Update dependencies

# Con scope:
feat(auth): Add login endpoint
fix(api): Handle timeout errors

# Formato completo:
feat(api): Add user registration

- Implement POST /api/register
- Add email validation
- Add password hashing

Closes #123
```

**Troubleshooting común:**

```bash
# Problema 1: "Nothing to commit"
# Solución: Añade archivos al staging primero
git add .
git commit -m "Mensaje"

# Problema 2: Olvidaste añadir un archivo al commit
# Solución: Usar --amend
git add archivo-olvidado.txt
git commit --amend --no-edit

# Problema 3: Mensaje de commit equivocado
# Solución: Usar --amend
git commit --amend -m "Mensaje correcto"

# Problema 4: Necesitas modificar el último commit
# Solución: Ver ejemplos de --amend arriba
git commit --amend

# Problema 5: Commit en rama equivocada
# Solución: Usar cherry-pick (ver sección de cherry-pick)
# O usar reset para deshacer (ver sección de reset)

# Problema 6: "Please tell me who you are"
# Solución: Configurar identidad
git config --global user.name "Tu Nombre"
git config --global user.email "tu@email.com"

# Problema 7: Editor no se abre o no sabes usar vi
# Solución: Cambiar editor
git config --global core.editor "nano"
# O usar -m directamente:
git commit -m "Mensaje"

# Problema 8: Quieres deshacer un commit
# Solución: Ver sección "git reset" o "git revert" según el caso
```

**Mejores prácticas:**

```bash
✓ Commits pequeños y atómicos
✓ Mensajes descriptivos (explica POR QUÉ)
✓ Usa convenciones (Conventional Commits)
✓ Usa --amend solo en commits NO pusheados

✗ Evita commits gigantes
✗ Evita mensajes genéricos ("fix", "update")
✗ No uses --amend en commits públicos
```

---

## 3. git status - Inspeccionando el Estado
[⬆️ Top](#tabla-de-contenidos)

**¿Qué hace?**
Muestra el estado actual del working directory y staging area.

**Funcionamiento interno:**
```
1. Compara working directory con HEAD
2. Compara staging con HEAD
3. Lee .git/index para archivos untracked
4. Compara con refs/remotes para ahead/behind
```

**Uso práctico:**

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

**Interpretación del output:**

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

**Entendiendo ahead/behind:**

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

**¿Qué hacer cuando hay divergencia (diverged)?**

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

**Causas comunes de divergencia:**

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

**Troubleshooting de divergencia:**

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

**Mejores prácticas:**

```bash
✓ Ejecuta git status antes de commit (SIEMPRE)
✓ Usa -s para overview rápido
✓ Verifica tracking branch con -b

✗ No ignores el output
✗ No commitees sin revisar status primero
```

---

## 4. git diff - Comparando Cambios
[⬆️ Top](#tabla-de-contenidos)

**¿Qué hace?**
Muestra diferencias entre working directory, staging, commits y ramas.

**Funcionamiento interno:**
```
1. Lee contenido de dos fuentes
2. Ejecuta algoritmo de diff (Myers, patience, histogram)
3. Genera "hunks" (bloques de diferencias)
4. Formatea output
```

**Uso práctico:**

```bash
# Diff de working (NO stageado)
git diff

# Diff de staging (lo que vas a commitear)
git diff --staged
# o: git diff --cached

# Diff completo (working vs último commit)
git diff HEAD

# Diff entre commits
git diff abc123 def456
git diff HEAD~5 HEAD

# Diff entre ramas
git diff main feature-x
git diff main...feature-x  # Desde punto de divergencia

# Diff de archivo específico
git diff archivo.txt
git diff HEAD~3 -- archivo.txt

# Diff con stats (resumen)
git diff --stat

# Diff solo nombres de archivos
git diff --name-only
git diff --name-status

# Diff por palabras (útil para textos)
git diff --word-diff

# Ignorar espacios
git diff -w

# Detectar líneas movidas
git diff --color-moved
```

**Mejores prácticas:**

```bash
✓ Usa git diff antes de add
✓ Usa git diff --staged antes de commit
✓ Usa --word-diff para documentación
✓ Usa ... (tres puntos) para comparar ramas

✗ No ignores el diff antes de commitear
✗ No confundas git diff con git diff --staged
```

---

## 5. git log - Explorando la Historia
[⬆️ Top](#tabla-de-contenidos)

**¿Qué hace?**
Muestra la historia de commits del repositorio.

**Funcionamiento interno:**
```
1. Obtiene commit actual (HEAD)
2. Sigue cadena de parents recursivamente
3. Formatea output según opciones
```

**Uso práctico:**

```bash
# Log básico
git log

# Log compacto (UNA LÍNEA)
git log --oneline

# Log con grafo visual
git log --oneline --graph --all

# Log de archivo específico
git log -- archivo.txt
git log --follow -- archivo.txt  # Sigue renames

# Log con diff
git log -p
git log -p -2  # Últimos 2 commits

# Log con stats
git log --stat

# Log con formato personalizado
git log --pretty=format:"%h - %an, %ar : %s"

# Búsqueda en log
git log --grep="fix"  # En mensaje
git log -S"función"  # En código (pickaxe)

# Log de rango
git log main..feature-x  # Commits en feature no en main
git log main...feature-x  # Commits que difieren

# Por autor
git log --author="John"

# Por fecha
git log --since="2 weeks ago"
git log --after="2024-01-01" --before="2024-12-31"

# Solo merges / sin merges
git log --merges
git log --no-merges

# Ver commit específico
git show abc123
git show HEAD~3
```

**Alias útiles (añadir a ~/.gitconfig):**

```bash
[alias]
    lg = log --graph --abbrev-commit --decorate --format=format:'%C(bold blue)%h%C(reset) - %C(bold green)(%ar)%C(reset) %C(white)%s%C(reset) %C(dim white)- %an%C(reset)%C(bold yellow)%d%C(reset)' --all
    ls = log --oneline --decorate
    ll = log --stat --abbrev-commit
    last = log -1 HEAD --stat
    unpushed = log origin/main..HEAD --oneline
```

**Mejores prácticas:**

```bash
✓ Usa --oneline para overview rápido
✓ Usa --graph para entender merges
✓ Usa --all para ver TODO
✓ Crea alias para comandos frecuentes

✗ No corras git log sin límites en repos gigantes
✗ No olvides --follow para archivos renombrados
```

---

## 6. git branch - Gestionando Líneas de Desarrollo
[⬆️ Top](#tabla-de-contenidos)

**¿Qué hace?**
Crea, lista, renombra y elimina ramas (branches).

**Funcionamiento interno:**
```
Crear rama:
1. Obtiene hash del commit actual
2. Crea archivo .git/refs/heads/rama con el hash
→ Solo 41 bytes, instantáneo
```

**Uso práctico:**

```bash
# Crear rama
git branch feature-x

# Crear y cambiar
git checkout -b feature-x
# o: git switch -c feature-x

# Listar ramas
git branch
git branch -a  # Incluye remotas
git branch -v  # Con último commit

# Ver ramas mergeadas/no mergeadas
git branch --merged
git branch --no-merged

# Eliminar rama
git branch -d feature-x  # Safe delete
git branch -D feature-x  # Force delete

# Renombrar rama
git branch -m nuevo-nombre
git branch -m viejo nuevo

# Mover rama a otro commit
git branch -f feature-x abc123
```

**Estrategias de branching:**

```bash
# Feature Branch Workflow
main
 └─ feature/user-auth
 └─ feature/payment
 └─ bugfix/login-error

# Git Flow
main (producción)
 ├─ develop (integración)
 │   ├─ feature/x
 │   └─ feature/y
 └─ hotfix/critical

# GitHub Flow (simple)
main
 ├─ feature-x
 └─ bugfix-y
```

**Mejores prácticas:**

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

## 7. git checkout / git switch - Navegando el Código
[⬆️ Top](#tabla-de-contenidos)

**¿Qué hace?**
Cambia de rama o restaura archivos del working directory.

**Funcionamiento interno:**
```
1. Obtiene hash del commit de la rama
2. Lee el tree object
3. Actualiza working directory
4. Actualiza HEAD
5. Actualiza .git/index
```

**git checkout vs git switch vs git restore:**

```bash
# CHECKOUT (multiuso, confuso)
git checkout main          # Cambiar rama
git checkout abc123        # Detached HEAD
git checkout -- file.txt   # Descartar cambios

# SWITCH (solo ramas, Git 2.23+)
git switch main
git switch -c nueva
git switch -

# RESTORE (solo archivos, Git 2.23+)
git restore file.txt
git restore --staged file.txt
git restore --source=abc123 file.txt

RECOMENDACIÓN: Usa switch para ramas, restore para archivos
```

**Uso práctico - git switch:**

```bash
# Cambiar a rama
git switch main

# Crear y cambiar
git switch -c feature-x

# Volver a rama anterior
git switch -

# Forzar cambio
git switch -f otra-rama
```

**Detached HEAD:**

```bash
# Entras con:
git checkout abc123
git checkout v1.0.0

# ¿Por qué?
✓ Inspeccionar commits viejos
✓ Probar código de commit específico

# Salir:
git checkout main
# O crear rama:
git switch -c nueva-rama
```

**Mejores prácticas:**

```bash
✓ Usa git switch para cambiar ramas
✓ Usa git restore para archivos
✓ Commitea o stash antes de cambiar ramas
✓ Entiende detached HEAD antes de usarlo

✗ Evita checkout sin especificar qué
✗ No trabajes en detached HEAD sin crear rama
```

---

## 8. git merge - Integrando Cambios
[⬆️ Top](#tabla-de-contenidos)

**¿Qué hace?**
Integra cambios de una rama en otra, combinando el trabajo de diferentes líneas de desarrollo. Es uno de los comandos más críticos en Git para la colaboración en equipo.

**Funcionamiento interno:**

```
Git merge puede operar de 3 formas diferentes:

1. FAST-FORWARD (merge "rápido"):
   main:    A---B
   feature:      C---D
   
   Resultado: main simplemente avanza al commit D
   main:    A---B---C---D
   
   → No crea merge commit
   → Solo mueve el puntero de la rama
   → Historia lineal limpia
   → Condición: main no ha avanzado desde que se creó feature

2. THREE-WAY MERGE (merge de 3 vías):
   main:    A---B---C
                \
   feature:      D---E
   
   Git usa 3 commits:
   - Ancestro común (B)
   - Último commit de main (C)
   - Último commit de feature (E)
   
   Resultado: Se crea nuevo merge commit (M)
   main:    A---B---C---M
                \     /
   feature:      D---E
   
   → Crea merge commit con 2 padres
   → Preserva historia completa
   → Historia no lineal (ramificada)

3. CONFLICTO:
   Cuando ambas ramas modifican las mismas líneas:
   → Git no puede decidir automáticamente
   → Marca conflictos en archivos
   → Requiere resolución manual
   → Crea merge commit tras resolver

Internamente:
1. git merge-base main feature  → Encuentra ancestro común
2. git diff-tree ancestro main  → Cambios en main
3. git diff-tree ancestro feature → Cambios en feature
4. Aplica ambos sets de cambios
5. Si no hay conflictos → merge automático
6. Si hay conflictos → pausa y marca conflictos
```

**Todas las opciones importantes:**

```bash
# ============================================
# OPCIONES DE ESTRATEGIA DE MERGE
# ============================================

# 1. Merge básico (comportamiento por defecto)
git merge feature-x
# → Fast-forward si es posible
# → Three-way merge si no

# 2. Forzar merge commit (sin fast-forward)
git merge --no-ff feature-x
# → SIEMPRE crea merge commit
# → Preserva historia de rama
# → Útil para features importantes
# → Mantiene visible qué commits pertenecían a la feature

# 3. Solo fast-forward (falla si no es posible)
git merge --ff-only feature-x
# → Solo avanza puntero
# → Falla si requiere merge commit
# → Útil para mantener historia lineal estricta
# → Común en workflows con rebase

# 4. Squash merge (aplasta todos los commits en uno)
git merge --squash feature-x
# → Aplica TODOS los cambios de feature-x
# → NO crea merge commit automáticamente
# → Debes hacer commit manual después
# → Resultado: 1 solo commit en main
# → Pierde historia individual de commits de feature
git commit -m "Add complete feature X"

# 5. Merge con edición de mensaje
git merge --edit feature-x
# → Abre editor para personalizar mensaje de merge commit
# → Por defecto usa mensaje automático

git merge --no-edit feature-x
# → Usa mensaje automático sin preguntar

# ============================================
# ESTRATEGIAS DE MERGE
# ============================================

# 6. Estrategia "ours" (en conflictos, prefiere nuestra versión)
git merge -X ours feature-x
# → En conflictos automáticos, usa versión de rama actual
# → CUIDADO: Puede silenciar cambios importantes
# → Útil cuando estás seguro de que tu versión es correcta

# 7. Estrategia "theirs" (en conflictos, prefiere su versión)
git merge -X theirs feature-x
# → En conflictos automáticos, usa versión de rama entrante
# → CUIDADO: Puede sobrescribir tu trabajo
# → Útil cuando aceptas completamente los cambios externos

# 8. Estrategia "recursive" (por defecto, más opciones)
git merge -s recursive -X patience feature-x
# patience: Algoritmo más cuidadoso (más lento, menos conflictos)

git merge -s recursive -X diff-algorithm=histogram feature-x
# Algoritmos: myers (default), minimal, patience, histogram

git merge -s recursive -X ignore-space-change feature-x
# Ignora cambios solo de espacios en blanco

git merge -s recursive -X ignore-all-space feature-x
# Ignora todos los espacios al comparar

git merge -s recursive -X ignore-space-at-eol feature-x
# Ignora espacios al final de línea

git merge -s recursive -X renormalize feature-x
# Re-normaliza archivos (útil con cambios de line-endings)

# 9. Estrategia "octopus" (merge múltiples ramas)
git merge branch1 branch2 branch3
# → Merge de 3+ ramas simultáneamente
# → Falla si hay conflictos (no soporta resolución manual)
# → Útil para integrar múltiples features simples

# 10. Estrategia "ours" (NO confundir con -X ours)
git merge -s ours old-feature
# → IGNORA completamente cambios de old-feature
# → Solo registra merge en historia
# → Útil para marcar rama como mergeada sin aplicar cambios
# → Diferente de -X ours (que sí intenta merge)

# 11. Estrategia "subtree"
git merge -s subtree -X subtree=libs/ external-lib
# → Merge de repositorio externo como subdirectorio
# → Útil para dependencias embebidas

# ============================================
# OPCIONES DE CONTROL Y VERIFICACIÓN
# ============================================

# 12. Verificar merge sin hacerlo
git merge --no-commit --no-ff feature-x
# → Prepara merge pero NO commitea
# → Permite revisar antes de finalizar
# → Útil para verificar resultado
# Luego:
git commit  # Para finalizar
# o
git merge --abort  # Para cancelar

# 13. Ver qué se va a mergear
git log HEAD..feature-x
# → Commits que entrarán en merge
git diff HEAD...feature-x
# → Cambios desde punto de divergencia

# 14. Merge con log de commits incluidos
git merge --log feature-x
# → Incluye resumen de commits en mensaje
git merge --no-log feature-x
# → No incluye resumen

# 15. Merge con firma GPG
git merge -S feature-x
# → Firma merge commit con GPG
# → Para verificación de autoría

# 16. Merge verboso
git merge -v feature-x
# → Muestra información detallada del proceso

git merge -q feature-x
# → Modo silencioso (solo errores)

# ============================================
# MANEJO DE MERGE EN PROGRESO
# ============================================

# 17. Abortar merge
git merge --abort
# → Cancela merge en curso
# → Restaura estado pre-merge
# → Solo funciona si merge no está completo

# 18. Continuar merge tras resolver conflictos
git add archivo-resuelto.txt
git commit
# → Git detecta merge en progreso
# → Usa mensaje de merge automático

# 19. Estado de merge en progreso
git status
# → Muestra archivos en conflicto
# → Indica que merge está pendiente

ls -la .git/
# → .git/MERGE_HEAD existe durante merge
# → Contiene SHA del commit siendo mergeado
```

**Resolución de conflictos - Guía completa:**

```bash
# ============================================
# IDENTIFICAR CONFLICTOS
# ============================================

# Ver archivos en conflicto
git status
# Muestra:
# - Unmerged paths (archivos con conflictos)
# - Changes to be committed (archivos auto-mergeados)

# Listar solo archivos con conflictos
git diff --name-only --diff-filter=U

# Ver conflictos con contexto
git diff

# Ver estadísticas de conflictos
git diff --stat

# ============================================
# ANATOMÍA DE UN CONFLICTO
# ============================================

# Git marca conflictos en el archivo:
<<<<<<< HEAD (rama actual)
código de la rama actual (main)
este código estaba aquí antes
=======
código de la rama entrante (feature-x)
este código viene del merge
>>>>>>> feature-x (rama que se está mergeando)

# Significado de marcadores:
# <<<<<<< HEAD        → Inicio de tu versión
# =======             → Separador
# >>>>>>> feature-x   → Fin de su versión

# ============================================
# ESTRATEGIAS DE RESOLUCIÓN
# ============================================

# 1. Resolución manual (más común)
# - Abre archivo en editor
# - Elimina marcadores <<<, ===, >>>
# - Edita código para combinar o elegir
# - Guarda archivo
git add archivo.txt
git commit

# 2. Aceptar versión completa (sin editar)
git checkout --ours archivo.txt    # Usar nuestra versión
git add archivo.txt

git checkout --theirs archivo.txt  # Usar su versión
git add archivo.txt

# 3. Ver diferencias durante conflicto
git diff --ours      # Diferencias con nuestra versión
git diff --theirs    # Diferencias con su versión
git diff --base      # Diferencias con ancestro común

# 4. Herramienta visual de merge
git mergetool
# → Abre herramienta configurada (vimdiff, meld, kdiff3, etc.)
# → Muestra 3 paneles: base, ours, theirs
# → Facilita resolución visual

# Configurar herramienta:
git config --global merge.tool meld
git config --global mergetool.prompt false

# 5. Ver contenido de versiones específicas
git show :1:archivo.txt  # Versión ancestro común (base)
git show :2:archivo.txt  # Versión nuestra (ours/HEAD)
git show :3:archivo.txt  # Versión suya (theirs)

# Guardar para comparar:
git show :2:archivo.txt > archivo-ours.txt
git show :3:archivo.txt > archivo-theirs.txt
# Comparar con herramienta externa

# ============================================
# CASOS ESPECIALES
# ============================================

# Conflictos en archivos binarios
git checkout --ours archivo.bin
# o
git checkout --theirs archivo.bin
# (No se pueden resolver manualmente línea a línea)

# Conflictos por archivo eliminado en una rama
# Git pregunta si mantener o eliminar:
git rm archivo.txt      # Confirma eliminación
# o
git add archivo.txt     # Mantiene archivo

# Conflictos por archivo renombrado
# Git puede detectar rename automáticamente
# Si no, resolver manualmente y hacer add

# ============================================
# ABORTAR Y REINTENTAR
# ============================================

# Abortar merge completo
git merge --abort
# → Vuelve a estado pre-merge
# → Útil si te equivocaste en resolución

# Reiniciar resolución de un archivo
git checkout -m archivo.txt
# → Restaura marcadores de conflicto
# → Permite resolver de nuevo

# Ver merge que causó conflicto
cat .git/MERGE_HEAD
# → SHA del commit siendo mergeado

git log -1 MERGE_HEAD
# → Detalles del commit en conflicto

# ============================================
# POST-RESOLUCIÓN
# ============================================

# Verificar que no quedan conflictos
git diff --check
# → Detecta marcadores de conflicto olvidados

# Verificar que todo compila/funciona
npm test  # o tu sistema de tests
git commit

# Limpiar archivos .orig (backup de mergetool)
git clean -f
# o configurar para no crearlos:
git config --global mergetool.keepBackup false
```

**Casos de uso del mundo real:**

```bash
# ============================================
# CASO 1: Feature simple lista para producción
# ============================================
git checkout main
git pull origin main
git merge --no-ff feature-login
git push origin main
# → Usa --no-ff para mantener visible la feature en historia

# ============================================
# CASO 2: Sincronizar feature con main
# ============================================
# Estás en feature-x, main avanzó, quieres últimos cambios
git checkout feature-x
git merge main
# → Trae cambios de main a tu feature
# → Resuelve conflictos ahora (no luego en main)
# → Testea todo funciona junto

# ============================================
# CASO 3: Multiple commits WIP, quieres 1 solo
# ============================================
git checkout main
git merge --squash feature-x
# Archivo .git/SQUASH_MSG tiene todos los mensajes
git commit -m "Add user authentication system

- Login form
- Password validation
- Session management
- Remember me functionality"
# → Main tiene 1 commit limpio
# → Historia de desarrollo (commits WIP) se pierde

# ============================================
# CASO 4: Hotfix urgente en producción
# ============================================
git checkout main
git checkout -b hotfix-security
# ... fixes ...
git commit -m "Fix: Security vulnerability CVE-2024-1234"
git checkout main
git merge --ff-only hotfix-security
# → --ff-only asegura merge limpio
# → Si falla, main se movió y hay que investigar
git push origin main
git branch -d hotfix-security

# ============================================
# CASO 5: Merge de múltiples features independientes
# ============================================
git checkout develop
git merge feature-a feature-b feature-c
# → Octopus merge
# → Solo si no hay conflictos
# → Historia muestra merge simultáneo

# ============================================
# CASO 6: Merge con revisión antes de commitear
# ============================================
git merge --no-commit --no-ff feature-x
# → Prepara merge sin commitear
git diff --staged
# → Revisa todos los cambios
npm test
# → Verifica que funciona
git commit
# o si algo falla:
git merge --abort

# ============================================
# CASO 7: Rama obsoleta, solo quieres marcarla como mergeada
# ============================================
git merge -s ours old-experiment
# → No aplica ningún cambio de old-experiment
# → Pero Git la marca como mergeada
# → Útil para limpiar ramas sin afectar código

# ============================================
# CASO 8: Merge de release branch
# ============================================
# Merge a main (producción)
git checkout main
git merge --no-ff --log release-1.5.0
# --log incluye lista de commits en mensaje

# Merge de vuelta a develop
git checkout develop
git merge --no-ff release-1.5.0

# ============================================
# CASO 9: Resolver conflicto prefiriendo una versión
# ============================================
git merge feature-x
# ... conflicto ...
git checkout --ours .      # Todas las versiones nuestras
# o
git checkout --theirs .    # Todas las versiones de ellos
git add .
git commit

# Más selectivo (solo ciertos archivos):
git checkout --ours src/
git checkout --theirs config/
git add .
git commit

# ============================================
# CASO 10: Merge con conflictos, quieres ver qué cambió
# ============================================
git merge feature-x
# ... conflictos ...

# Ver historial de cambios en archivo conflictivo
git log --oneline --all -- archivo-conflicto.txt

# Ver qué cambió en cada rama
git log main..feature-x -- archivo-conflicto.txt
git show feature-x:archivo-conflicto.txt
git show main:archivo-conflicto.txt

# Resolver informadamente
# ... edita ...
git add archivo-conflicto.txt
git commit
```

**Troubleshooting y problemas comunes:**

```bash
# ============================================
# PROBLEMA 1: "Already up to date"
# ============================================
git merge feature-x
# Already up to date.

Causa: feature-x no tiene commits nuevos vs main
Solución: 
- Verificar que estás en rama correcta
- Verificar que feature-x tiene commits:
  git log main..feature-x

# ============================================
# PROBLEMA 2: "fatal: refusing to merge unrelated histories"
# ============================================
Causa: Ramas sin ancestro común (repos separados)
Solución:
git merge --allow-unrelated-histories other-branch
# ⚠️ CUIDADO: Puede crear merge complejo

# ============================================
# PROBLEMA 3: Merge incompleto, .git/MERGE_HEAD existe
# ============================================
git status
# On branch main
# You have unmerged paths.

Causa: Merge con conflictos sin resolver
Solución:
1. Resolver conflictos:
   git status  # Ver qué falta
   # ... resolver ...
   git add .
   git commit
2. O abortar:
   git merge --abort

# ============================================
# PROBLEMA 4: Merge commit no deseado
# ============================================
# Ya hiciste merge y no querías merge commit
git reset --hard HEAD~1  # Deshace último commit
git merge --ff-only feature-x  # Intenta fast-forward

# ============================================
# PROBLEMA 5: Conflictos masivos, difícil resolver
# ============================================
Solución 1: Abortar y usar rebase
git merge --abort
git rebase main  # Resuelve conflicto por conflicto

Solución 2: Estrategia más agresiva
git merge -X theirs feature-x
# LUEGO revisa cambios críticos manualmente

Solución 3: Resolver en herramienta visual
git mergetool

# ============================================
# PROBLEMA 6: Merge eliminó archivo que debería existir
# ============================================
# Git puede auto-mergear eliminación incorrectamente
git show HEAD:archivo-perdido.txt > archivo-perdido.txt
git add archivo-perdido.txt
git commit --amend  # Corrige merge commit

# ============================================
# PROBLEMA 7: Merge rompió funcionalidad
# ============================================
# Opción 1: Revert del merge
git revert -m 1 HEAD
# -m 1 indica mantener lado 1 (main) del merge

# Opción 2: Reset (si no pusheaste)
git reset --hard HEAD~1

# ============================================
# PROBLEMA 8: No puedes hacer merge (archivos sucios)
# ============================================
error: Your local changes would be overwritten by merge.

Solución 1: Commitear cambios
git add .
git commit -m "WIP"
git merge feature-x

Solución 2: Stash
git stash
git merge feature-x
git stash pop

Solución 3: Descartar cambios
git reset --hard  # ⚠️ PIERDE CAMBIOS
git merge feature-x
```

**Mejores prácticas y patrones:**

```bash
# ============================================
# ✅ BUENAS PRÁCTICAS
# ============================================

# 1. Siempre actualiza antes de merge
git checkout main
git pull origin main
git merge feature-x

# 2. Usa --no-ff para features importantes
git merge --no-ff feature-login
# → Historia clara, fácil revertir feature completa

# 3. Resuelve conflictos en feature branch, no en main
git checkout feature-x
git merge main
# ... resolver conflictos ...
git checkout main
git merge feature-x  # Ahora sin conflictos

# 4. Testea tras resolver conflictos
git merge feature-x
# ... resolver ...
npm test
git commit

# 5. Usa mensajes de merge descriptivos
git merge --no-ff --edit feature-auth
# Edita para incluir:
# - Qué hace la feature
# - Issues relacionados (#123)
# - Reviewers

# 6. Squash para limpiar historia
git merge --squash feature-experiment
# → 47 commits de prueba → 1 commit limpio

# 7. Verifica antes de push
git log --oneline --graph -10
git diff origin/main
git push origin main

# 8. Usa merge commits para puntos importantes
git merge --no-ff release-2.0
# → Marca claramente releases en historia

# ============================================
# ✗ MALAS PRÁCTICAS
# ============================================

# 1. Mergear sin testear
git merge feature-x && git push  # ❌
# Puede romper main

# 2. Usar -X ours/theirs sin revisar
git merge -X theirs external-branch  # ❌
# Puede sobrescribir trabajo importante

# 3. Mergear directo a main sin revisión
# En proyectos serios, usa Pull Requests

# 4. Ignorar conflictos "pequeños"
# Todo conflicto requiere atención

# 5. No limpiar branches tras merge
git merge feature-x
git push
# Luego:
git branch -d feature-x  # ✅ Limpia local
git push origin --delete feature-x  # ✅ Limpia remoto

# 6. Merge de ramas públicas con rebase
# Causa problemas a colaboradores

# ============================================
# WORKFLOWS COMUNES
# ============================================

# GitHub Flow (simple)
1. Crea feature branch desde main
2. Desarrolla y commitea
3. Push y crea Pull Request
4. Revisión de código
5. Merge (con --no-ff) a main
6. Delete branch

# Git Flow (complejo)
- main: Producción
- develop: Integración
- feature/*: Nuevas features
- release/*: Preparar release
- hotfix/*: Fixes urgentes

# Feature → develop: --no-ff
# develop → main: --no-ff (con tag)
# hotfix → main y develop: --no-ff
```

**Comparación: merge vs rebase:**

```bash
# ============================================
# CUÁNDO USAR MERGE
# ============================================
✅ Integrar features completas a main
✅ Merges de release branches
✅ Colaboración en ramas públicas
✅ Preservar historia exacta de desarrollo
✅ Cuando múltiples devs trabajan en misma rama

Ventajas:
- No reescribe historia
- Seguro para ramas compartidas
- Preserva contexto (cuándo se mergeó)
- Fácil revertir (git revert -m 1)

Desventajas:
- Historia puede volverse compleja
- Grafo con muchas ramas
- "Merge commits" pueden saturar log

# ============================================
# CUÁNDO USAR REBASE
# ============================================
✅ Actualizar feature branch con main
✅ Limpiar commits locales antes de merge
✅ Mantener historia lineal
✅ Trabajo personal en rama local

Ventajas:
- Historia lineal y limpia
- Fácil de leer git log
- No crea merge commits extra

Desventajas:
- Reescribe historia (cambia SHAs)
- Peligroso en ramas públicas
- Puede causar problemas a colaboradores

# ============================================
# ESTRATEGIA HÍBRIDA (RECOMENDADA)
# ============================================

# 1. Durante desarrollo: rebase
git checkout feature-x
git rebase main  # Mantiene feature actualizada y limpia

# 2. Para integrar: merge
git checkout main
git merge --no-ff feature-x  # Integra feature completa

Resultado:
- Historia limpia en features (rebase)
- Historia clara en main (merge commits marcan features)
- Lo mejor de ambos mundos
```

**Configuración recomendada:**

```bash
# Configurar merge sin fast-forward por defecto
git config --global merge.ff false

# Siempre mostrar diffstat tras merge
git config --global merge.stat true

# Configurar herramienta de merge
git config --global merge.tool meld
git config --global mergetool.prompt false
git config --global mergetool.keepBackup false

# Estilo de conflictos (diff3 muestra ancestro común)
git config --global merge.conflictstyle diff3

# Ejemplo de conflicto con diff3:
<<<<<<< HEAD
código actual
||||||| merged common ancestors
código ancestro común
=======
código entrante
>>>>>>> feature-x

# Configurar para squash automático en certain branches
# (en .git/config o ~/.gitconfig)
[branch "develop"]
    mergeoptions = --no-ff

# Verificar configuración
git config --list | grep merge
```

---

## 9. git rebase - Reescribiendo Historia
[⬆️ Top](#tabla-de-contenidos)

**¿Qué hace?**
Reaplica commits de una rama encima de otra, reescribiendo historia.

**Funcionamiento interno:**
```
1. Identifica commits únicos
2. Guarda como patches temporales
3. Resetea rama a base
4. Aplica patches uno por uno (nuevos commits)
```

**Uso práctico:**

```bash
# Rebase básico
git checkout feature-x
git rebase main

# Rebase interactivo (SUPER PODEROSO)
git rebase -i main
# Opciones:
# pick   - Usar commit
# reword - Cambiar mensaje
# edit   - Pausar para modificar
# squash - Combinar con anterior (mantiene mensaje)
# fixup  - Combinar con anterior (descarta mensaje)
# drop   - Eliminar commit

# Squash últimos N commits
git rebase -i HEAD~3

# Continuar tras conflicto
git add archivo-resuelto
git rebase --continue

# Saltar commit
git rebase --skip

# Abortar
git rebase --abort
```

**Rebase vs Merge:**

```bash
MERGE:
✓ Historia completa
✓ No reescribe commits
✓ Seguro para ramas públicas
✗ Grafo complejo

REBASE:
✓ Historia lineal
✓ Fácil de entender
✗ Reescribe commits
✗ Peligroso para ramas públicas

# ¿Cuándo usar cada uno?
REBASE: Rama local/feature antes de merge
MERGE: Integrar features a main
```

**⚠️ Regla de oro:**

```bash
NUNCA rebasees commits ya pusheados a repositorio público

Correcto:
git rebase main          # OK, commits solo locales
git push origin feature-x

Incorrecto:
git push origin feature-x
git rebase main          # ¡ROMPE REPO DE OTROS!
git push --force
```

**Mejores prácticas:**

```bash
✓ Usa rebase para limpiar historia local
✓ Rebase feature sobre main antes de merge
✓ Usa --force-with-lease en vez de --force
✓ Nunca rebasees ramas públicas compartidas

✗ No rebasees main o develop
✗ No rebasees commits públicos
✗ No uses --force sin --force-with-lease
```

---

## 10. git clone - Copiando Repositorios

**¿Qué hace?**
Crea una copia local completa de un repositorio remoto.

**Funcionamiento interno:**
```
1. Crea directorio
2. git init
3. git remote add origin <url>
4. git fetch origin
5. git checkout <default-branch>
```

**Uso práctico:**

```bash
# Clone básico
git clone https://github.com/user/repo.git

# Clone con nombre personalizado
git clone https://github.com/user/repo.git mi-proyecto

# Clone shallow (solo último commit, rápido)
git clone --depth 1 https://github.com/user/repo.git

# Clone de rama específica
git clone -b develop https://github.com/user/repo.git

# Clone con submódulos
git clone --recursive https://github.com/user/repo.git

# Clone parcial (sin blobs)
git clone --filter=blob:none https://github.com/user/repo.git
```

**Protocolos:**

```bash
# HTTPS (recomendado, universal)
git clone https://github.com/user/repo.git

# SSH (más rápido, requiere key)
git clone git@github.com:user/repo.git

# Local
git clone /ruta/al/repo.git
```

**Mejores prácticas:**

```bash
✓ Usa HTTPS para proyectos públicos
✓ Usa SSH para proyectos privados
✓ Usa --depth 1 en CI/CD
✓ Usa --recursive para repos con submódulos

✗ No clones con --depth si necesitas historia
✗ No desactives SSL verification sin razón
```

---

## 11. git remote - Gestionando Repositorios Remotos
[⬆️ Top](#tabla-de-contenidos)

**¿Qué hace?**
Gestiona referencias a repositorios remotos.

**Funcionamiento interno:**
```
Remotos se guardan en .git/config:
[remote "origin"]
    url = https://github.com/user/repo.git
    fetch = +refs/heads/*:refs/remotes/origin/*
```

**Uso práctico:**

```bash
# Listar remotos
git remote
git remote -v

# Añadir remoto
git remote add upstream https://github.com/original/repo.git

# Ver detalles
git remote show origin

# Cambiar URL
git remote set-url origin https://nuevo-url.git
git remote set-url origin git@github.com:user/repo.git

# Renombrar
git remote rename origin nuevo-nombre

# Eliminar
git remote remove upstream

# Limpiar refs obsoletas
git remote prune origin
git remote prune origin --dry-run
```

**Fork workflow:**

```bash
git clone https://github.com/tu-fork/proyecto.git
cd proyecto
git remote add upstream https://github.com/original/proyecto.git
git remote -v
# origin    tu-fork (fetch/push)
# upstream  original (fetch/push)

# Workflow:
git fetch upstream
git merge upstream/main
git push origin main
```

**Mejores prácticas:**

```bash
✓ Usa nombres descriptivos (origin, upstream, backup)
✓ Usa SSH para repos privados
✓ Configura upstream para forks
✓ Limpia con prune regularmente

✗ No pongas credenciales en URL
✗ No uses nombres confusos
✗ No borres origin sin reemplazarlo
```

---

## 12. git fetch - Descargando Cambios
[⬆️ Top](#tabla-de-contenidos)

**¿Qué hace?**
Descarga objetos y refs del remoto SIN modificar working directory.

**Funcionamiento interno:**
```
1. Conecta con remoto
2. Compara refs locales vs remotas
3. Descarga objetos faltantes
4. Actualiza refs/remotes/origin/*
5. NO modifica ramas locales
6. NO modifica working directory
```

**Uso práctico:**

```bash
# Fetch básico
git fetch
git fetch origin

# Fetch de rama específica
git fetch origin main

# Fetch de todos los remotos
git fetch --all

# Fetch con prune (limpia refs obsoletas)
git fetch --prune

# Fetch de PR (GitHub)
git fetch origin pull/123/head:pr-123

# Ver resultado
git log HEAD..origin/main --oneline
git diff origin/main
```

**Fetch vs Pull:**

```bash
# FETCH: Solo descarga
git fetch origin main
→ origin/main actualizado
→ main local SIN cambios
→ Puedes revisar antes de integrar

# PULL: Fetch + Merge
git pull origin main
→ Descarga Y mergea automáticamente
→ Más rápido pero menos control
```

**Mejores prácticas:**

```bash
✓ Usa fetch antes de pull (revisa cambios)
✓ Usa --prune regularmente
✓ Fetch frecuentemente
✓ Revisa con git log tras fetch

✗ No confundas fetch con pull
✗ No asumas que fetch cambia working
✗ No olvides mergear después de fetch
```

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

# DESPUÉS de pull - verificar:

# 1. Ver qué se integró
git log -5 --oneline
# → Últimos 5 commits

# 2. Ver si quedaron conflictos sin resolver
git status
# → Debe estar limpio

# 3. Ver diferencias con remoto
git diff origin/main
# → Debería estar vacío
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
- Merge commit M con 2 padres
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

## 14. git push - Subiendo Cambios
[⬆️ Top](#tabla-de-contenidos)

**¿Qué hace?**
Envía commits locales al repositorio remoto.

**Funcionamiento interno:**
```
1. Conecta con remoto
2. Compara refs
3. Verifica que sea fast-forward
4. Empaqueta objetos faltantes
5. Envía objetos
6. Actualiza refs remotas
```

**Uso práctico:**

```bash
# Push básico
git push
git push origin main

# Push con tracking
git push -u origin feature-x

# Push forzado (¡CUIDADO!)
git push --force  # PELIGROSO
git push --force-with-lease  # PREFERIBLE

# Push de tags
git push origin v1.0.0
git push --tags

# Eliminar rama remota
git push origin --delete feature-x

# Push dry-run
git push --dry-run
```

**⚠️ Force push:**

```bash
NUNCA fuerces push en ramas compartidas (main, develop)

Cuándo SÍ:
✓ Feature branch personal
✓ Después de rebase local
✓ Corregir commits antes de merge

Cuándo NO:
✗ main/develop/master
✗ Ramas de otros
✗ Ramas públicas

SIEMPRE usa --force-with-lease (no --force):
git push --force-with-lease
→ Solo fuerza si nadie más actualizó
```

**Mejores prácticas:**

```bash
✓ Commitea cambios atómicos, push frecuentemente
✓ Usa --force-with-lease en vez de --force
✓ Verifica con --dry-run antes de push importante
✓ Pull antes de push (evita rechazos)

✗ NO uses --force en ramas compartidas
✗ NO pushees credenciales, secrets, keys
✗ NO pushees archivos gigantes
✗ NO ignores errores de push
```

---

## 15. git reset - Moviendo Referencias

**¿Qué hace?**
Mueve HEAD y rama actual, opcionalmente modificando staging y working.

**Funcionamiento interno:**
```
Tres modos:
--soft:  Solo mueve HEAD/rama
--mixed: Mueve HEAD/rama + resetea staging
--hard:  Mueve HEAD/rama + resetea staging + working
```

**Uso práctico:**

```bash
# 1. Reset suave (mantiene cambios en staging)
git reset --soft HEAD~1
# → Deshace commit
# → Cambios vuelven a staging
# → Útil para rehacer commit

# 2. Reset mixto (default, cambios en working)
git reset HEAD~1
# o: git reset --mixed HEAD~1
# → Deshace commit
# → Cambios vuelven a working directory
# → Útil para reorganizar qué commitear

# 3. Reset duro (¡PIERDES CAMBIOS!)
git reset --hard HEAD~1
# → Deshace commit
# → BORRA todos los cambios
# → ⚠️ PELIGROSO: no recuperable sin reflog

# 4. Unstage archivo (quitar del staging)
git reset HEAD archivo.txt
# → Mueve archivo de staging a working
# → NO modifica el último commit

# 5. Reset a commit específico
git reset --soft abc123
git reset --mixed abc123
git reset --hard abc123

# 6. Reset a remoto
git reset --hard origin/main
# → Sincroniza con remoto, descartando cambios locales

# 7. Reset de un directorio específico
git reset HEAD directorio/
```

**FLUJO DE ESTADOS con reset:**

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

**Casos de uso prácticos:**

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

**Comparación de modos:**

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

**Reset vs Revert:**

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

**Recuperación:**

```bash
# Si hiciste reset por error:
git reflog
git reset --hard HEAD@{1}
```

**Troubleshooting común:**

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

**Mejores prácticas:**

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

## 16. git stash - Guardado Temporal
[⬆️ Top](#tabla-de-contenidos)

**¿Qué hace?**
Guarda trabajo en progreso temporalmente sin commitear.

**Funcionamiento interno:**
```
1. Crea commits especiales en refs/stash
2. Guarda working + staging
3. Limpia working directory
4. Como una pila (stack): LIFO
```

**Uso práctico:**

```bash
# Stash básico
git stash
git stash push -m "WIP: feature half done"

# Stash incluyendo untracked
git stash -u

# Stash interactivo
git stash -p

# Ver lista
git stash list

# Ver contenido
git stash show
git stash show -p

# Aplicar stash (mantiene en lista)
git stash apply
git stash apply stash@{2}

# Pop stash (aplica y elimina)
git stash pop

# Crear rama desde stash
git stash branch nueva-rama

# Eliminar stash
git stash drop
git stash drop stash@{1}

# Limpiar todos
git stash clear
```

**Casos de uso:**

```bash
# Cambio urgente en otra rama
git stash
git checkout main
git checkout -b hotfix
# ... arreglas ...
git checkout feature-x
git stash pop

# Pull con cambios locales
git stash
git pull
git stash pop
# O:
git pull --autostash
```

**Mejores prácticas:**

```bash
✓ Usa mensajes descriptivos con -m
✓ Limpia stashes viejos regularmente
✓ Usa stash -u si añadiste archivos nuevos
✓ Prefiere stash pop sobre apply

✗ No uses stash como sistema de backup
✗ No acumules decenas de stashes
✗ No stashees y olvides
```

---

## 17. git tag - Marcando Versiones
[⬆️ Top](#tabla-de-contenidos)

**¿Qué hace?**
Crea referencias inmutables a commits (usualmente para versiones).

**Funcionamiento interno:**
```
Lightweight tag: Solo referencia
Annotated tag: Objeto completo con mensaje, autor, fecha
```

**Uso práctico:**

```bash
# Crear lightweight tag
git tag v1.0.0

# Crear annotated tag (RECOMENDADO)
git tag -a v1.0.0 -m "Release 1.0.0"

# Listar tags
git tag
git tag -l "v1.*"

# Ver detalles
git show v1.0.0

# Tag con firma GPG
git tag -s v1.0.0 -m "Signed release"

# Eliminar tag
git tag -d v1.0.0

# Push de tags
git push origin v1.0.0
git push --tags

# Eliminar tag remoto
git push origin --delete v1.0.0

# Checkout tag
git checkout v1.0.0

# Crear rama desde tag
git checkout -b hotfix-1.0.1 v1.0.0
```

**Semantic Versioning:**

```bash
v<MAJOR>.<MINOR>.<PATCH>

Ejemplos:
v1.0.0           # Release estable
v1.0.0-alpha.1   # Pre-release
v1.0.0-beta.2    # Beta
v1.0.0-rc.1      # Release candidate

Incremento:
v1.2.3 → v2.0.0  # Breaking change (MAJOR)
v1.2.3 → v1.3.0  # New feature (MINOR)
v1.2.3 → v1.2.4  # Bug fix (PATCH)
```

**Mejores prácticas:**

```bash
✓ Usa annotated tags para releases (-a)
✓ Sigue semantic versioning
✓ Firma tags importantes con GPG (-s)
✓ Push tags explícitamente
✓ Tag desde main después de merge

✗ No muevas tags ya pusheados
✗ No uses lightweight tags para releases
✗ No olvides pushear tags
```

---

## 18. git revert - Deshaciendo Commits Públicos

**¿Qué hace?**
Crea NUEVO commit que deshace cambios de commit anterior.

**Funcionamiento interno:**
```
1. Lee commit a revertir
2. Calcula inverso de cambios
3. Aplica cambios inversos
4. Crea nuevo commit
5. Historia se mantiene intacta
```

**Uso práctico:**

```bash
# Revert de commit
git revert abc123
git revert abc123 --no-edit

# Revert de HEAD
git revert HEAD
git revert HEAD~3

# Revert múltiples
git revert HEAD~3..HEAD
git revert abc123 def456 ghi789

# Revert sin commit automático
git revert --no-commit abc123

# Revert de merge commit
git revert -m 1 abc123
# -m 1 = mantiene padre 1 (main line)

# Abortar/continuar
git revert --abort
git revert --continue
```

**Revert vs Reset:**

```bash
RESET (reescribe historia):
→ Mueve rama atrás
→ Commits desaparecen
→ Solo commits locales

REVERT (preserva historia):
→ Nuevo commit que deshace
→ Historia intacta
→ Seguro para commits públicos

¿Cuándo usar cada uno?
RESET: Commits locales no pusheados
REVERT: Commits ya pusheados/públicos
```

**Mejores prácticas:**

```bash
✓ Usa revert para commits públicos
✓ Usa --no-commit para múltiples como uno
✓ Incluye razón del revert en mensaje
✓ Usa -m 1 para revert de merges

✗ No uses revert para commits locales (usa reset)
✗ No omitas -m en revert de merge
```

---

## 19. git cherry-pick - Aplicando Commits Selectivos

**¿Qué hace?**
Aplica cambios de commit específico a rama actual.

**Funcionamiento interno:**
```
1. Lee commit a cherry-pick
2. Calcula diff
3. Aplica diff a rama actual
4. Crea NUEVO commit (hash diferente)
```

**Uso práctico:**

```bash
# Cherry-pick básico
git cherry-pick abc123

# Sin commit automático
git cherry-pick --no-commit abc123

# Múltiples commits
git cherry-pick abc123 def456 ghi789
git cherry-pick abc123..ghi789

# Con nota de origen
git cherry-pick -x abc123
# Añade: (cherry picked from commit abc123)

# Abortar/continuar
git cherry-pick --abort
git cherry-pick --continue
```

**Casos de uso:**

```bash
# Hotfix de producción
git checkout production
git cherry-pick abc123  # Fix de develop
git push origin production

# Backport a versión anterior
git checkout release-2.0
git cherry-pick def456  # Feature de main
git push origin release-2.0

# Mover commits entre ramas
git checkout rama-correcta
git cherry-pick abc123
git checkout rama-incorrecta
git reset --hard HEAD~1
```

**Cherry-pick vs Merge:**

```bash
MERGE:
→ Trae toda la rama
→ Merge commit
→ Historia completa

CHERRY-PICK:
→ Solo commits específicos
→ Sin merge commit
→ Commits duplicados

¿Cuándo usar?
MERGE: Feature completa
CHERRY-PICK: Hotfixes, backports
```

**Mejores prácticas:**

```bash
✓ Usa cherry-pick para fixes urgentes
✓ Usa -x para rastrear origen
✓ Usa --no-commit para combinar múltiples

✗ No uses como reemplazo de merge
✗ No cherry-picks en exceso
✗ Evita cherry-pick de merges sin -m
```

---

## 20. git clean - Limpiando Archivos No Rastreados
[⬆️ Top](#tabla-de-contenidos)

**¿Qué hace?**
Elimina archivos untracked del working directory.

**⚠️ PELIGRO: Eliminación NO es reversible**

**Funcionamiento interno:**
```
1. Escanea working directory
2. Identifica archivos untracked
3. Los elimina del filesystem
```

**Uso práctico:**

```bash
# ⚠️ SIEMPRE DRY-RUN PRIMERO
git clean -n
git clean --dry-run

# Eliminar archivos
git clean -f

# Eliminar archivos + directorios
git clean -fd

# Eliminar TODO (incluye .gitignore)
git clean -fxd

# Interactivo (RECOMENDADO)
git clean -i

# Con exclusiones
git clean -fxd -e "*.log"
git clean -fd -e node_modules
```

**Clean vs Reset:**

```bash
CLEAN: Elimina archivos untracked
→ NO en Git
→ NO recuperables

RESET: Descarta cambios tracked
→ En Git
→ Recuperables con reflog

COMBINACIÓN (reset completo):
git reset --hard HEAD  # Tracked
git clean -fxd         # Untracked
```

**Mejores prácticas:**

```bash
✓ SIEMPRE usa -n (dry-run) primero
✓ Usa -i (interactive) para selectivo
✓ Usa .gitignore para archivos ignorables
✓ Verifica con git status antes

✗ NUNCA uses git clean sin revisar
✗ No uses -x sin entender consecuencias
✗ No asumas que puedes recuperar
```

---

## 21. git rm y git mv - Eliminando y Moviendo Archivos
[⬆️ Top](#tabla-de-contenidos)

**¿Qué hace?**
Elimina o mueve archivos en Git y working directory.

**Funcionamiento interno:**
```
git rm:
1. Elimina archivo del filesystem
2. Actualiza .git/index
3. Cambio stageado (necesitas commitear)

git mv:
1. git rm old
2. git add new
3. Git detecta rename automáticamente
```

**Uso práctico - git rm:**

```bash
# Eliminar archivo (disk + Git)
git rm archivo.txt

# Eliminar solo de Git (mantener en disk)
git rm --cached archivo.txt

# Eliminar forzado
git rm -f archivo.txt

# Eliminar directorio
git rm -r directorio/

# Con wildcards
git rm '*.txt'
```

**Uso práctico - git mv:**

```bash
# Mover/renombrar archivo
git mv viejo.txt nuevo.txt

# Mover a directorio
git mv archivo.txt src/

# Renombrar directorio
git mv old-dir/ new-dir/
```

**rm/mv vs git rm/mv:**

```bash
# RM (comando shell):
rm archivo.txt
git add archivo.txt
→ 2 pasos

# GIT RM:
git rm archivo.txt
→ 1 paso, automáticamente stageado

# MV (comando shell):
mv old.txt new.txt
git rm old.txt && git add new.txt
→ Git detecta rename igual

# GIT MV:
git mv old.txt new.txt
→ Más claro, rename explícito
```

**Casos de uso:**

```bash
# Eliminar archivo sensible
git rm --cached .env
echo ".env" >> .gitignore
git add .gitignore
git commit -m "chore: Remove .env from tracking"

# Reorganizar proyecto
git mv lib/*.js src/lib/
git commit -m "refactor: Reorganize structure"

# Case-sensitive rename (macOS/Windows)
git mv readme.md temp
git mv temp README.md
git commit -m "docs: Fix README capitalization"
```

**Mejores prácticas:**

```bash
✓ Usa git rm en vez de rm (más claro)
✓ Usa git mv en vez de mv (detecta rename)
✓ Usa --cached para unstage sin eliminar
✓ Commitea después de rm/mv

✗ No uses rm -rf .git (NUNCA)
✗ No uses git rm -f sin revisar
✗ No olvides commitear después
```

---

## WORKFLOWS COMUNES

### Workflow 1: Feature Branch

```bash
# Crear feature
git checkout -b feature/user-auth
# ... desarrollo ...
git add .
git commit -m "feat: Add user authentication"
git push -u origin feature/user-auth

# PR en GitHub
# Tras aprobación:
git checkout main
git pull origin main
git merge --no-ff feature/user-auth
git push origin main
git branch -d feature/user-auth
git push origin --delete feature/user-auth
```

### Workflow 2: Fork Contribution

```bash
# Setup
git clone https://github.com/tu-fork/proyecto.git
cd proyecto
git remote add upstream https://github.com/original/proyecto.git

# Sincronizar
git fetch upstream
git checkout main
git merge upstream/main
git push origin main

# Contribuir
git checkout -b fix/bug-123
# ... fixes ...
git commit -am "fix: Resolve issue #123"
git push -u origin fix/bug-123
# PR a upstream
```

### Workflow 3: Hotfix

```bash
# Hotfix urgente
git checkout main
git checkout -b hotfix/critical-bug
# ... fix ...
git commit -am "fix: Critical security issue"
git push -u origin hotfix/critical-bug

# Fast merge
git checkout main
git merge hotfix/critical-bug
git push origin main
git branch -d hotfix/critical-bug

# Tag
git tag -a v1.0.1 -m "Hotfix: Security patch"
git push origin v1.0.1
```

---

## TROUBLESHOOTING RÁPIDO

### Deshacer cambios

```bash
# Archivo modificado, no stageado
git restore archivo.txt

# Archivo stageado
git restore --staged archivo.txt

# Último commit (local)
git reset --soft HEAD~1

# Último commit (público)
git revert HEAD

# Múltiples commits (local)
git reset --hard HEAD~3

# Working directory completo
git reset --hard HEAD
git clean -fxd
```

### Recuperar trabajo perdido

```bash
# Ver reflog
git reflog

# Recuperar commit
git checkout abc123
git branch rescue-branch

# Recuperar después de reset
git reset --hard HEAD@{2}
```

### Conflictos de merge

```bash
# Durante merge
git status  # Ver conflictos
# Editar archivos
git add archivo-resuelto
git commit

# O abortar
git merge --abort

# Elegir versión completa
git checkout --ours archivo.txt
git checkout --theirs archivo.txt
```

### Problemas con push

```bash
# Push rechazado
git pull --rebase
git push

# Necesitas force push (feature branch)
git push --force-with-lease

# Remoto cambió
git fetch origin
git reset --hard origin/main
```

---

## CONFIGURACIÓN RECOMENDADA

```bash
# Identidad
git config --global user.name "Tu Nombre"
git config --global user.email "tu@email.com"

# Editor
git config --global core.editor "code --wait"

# Diff/merge tool
git config --global diff.tool vscode
git config --global merge.tool vscode

# Pull con rebase
git config --global pull.rebase true

# Push solo rama actual
git config --global push.default current

# Auto-setup tracking
git config --global push.autoSetupRemote true

# Fetch con prune
git config --global fetch.prune true

# Colores
git config --global color.ui auto

# Alias útiles
git config --global alias.st status
git config --global alias.co checkout
git config --global alias.br branch
git config --global alias.cm commit
git config --global alias.lg "log --graph --oneline --all"
git config --global alias.last "log -1 HEAD --stat"
git config --global alias.unstage "reset HEAD --"
```

---

## RECURSOS ADICIONALES

**Documentación relacionada:**
- `GIT_FUNCIONAMIENTO_INTERNO.md` - Teoría y arquitectura de Git
- `GITHUB_ACTIONS_*.md` - Integración con CI/CD

**Aprend más:**
- [Git Documentation](https://git-scm.com/doc)
- [Pro Git Book](https://git-scm.com/book/en/v2)
- [Git Cheat Sheet](https://training.github.com/downloads/github-git-cheat-sheet/)

---

**Última actualización:** Febrero 2026  
**Versión:** 1.0.0

Este documento cubre los 21 comandos Git más importantes con ejemplos prácticos del mundo real. Para entender el funcionamiento interno de Git, consulta `GIT_FUNCIONAMIENTO_INTERNO.md`.

