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
13. [git pull - Descargando e Integrando](#13-git-pull---descargando-e-integrando)
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
# Commit básico
git commit -m "Mensaje descriptivo"

# Mensaje multilínea
git commit -m "Título" -m "Descripción detallada"

# Abrir editor
git commit

# Add + commit automático (solo tracked files)
git commit -am "Mensaje"

# Modificar último commit
git commit --amend -m "Nuevo mensaje"

# Amend sin cambiar mensaje
git commit --amend --no-edit

# Commit vacío (útil para triggering CI)
git commit --allow-empty -m "Trigger CI"
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
# ?? = untracked
# M  = staged
#  M = modified (not staged)
# MM = staged + modified again

# Con info de branch
git status -sb

# Ver archivos ignorados
git status --ignored

# Formato porcelain (para scripts)
git status --porcelain
```

**Interpretación del output:**

```bash
On branch main
Your branch is ahead of 'origin/main' by 2 commits
→ Tienes 2 commits no pusheados

Changes to be committed:
→ Staging area (listo para commit)

Changes not staged for commit:
→ Working directory modificado

Untracked files:
→ Archivos nuevos no en Git
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
Integra cambios de una rama en otra.

**Funcionamiento interno:**
```
Casos:
1. Fast-forward: Solo mueve puntero (historia lineal)
2. Three-way merge: Crea merge commit (historias divergentes)
3. Conflicto: Requiere resolución manual
```

**Uso práctico:**

```bash
# Merge básico
git checkout main
git merge feature-x

# Merge sin fast-forward (siempre crea merge commit)
git merge --no-ff feature-x

# Merge solo si es fast-forward
git merge --ff-only feature-x

# Merge squash (un commit por feature)
git merge --squash feature-x
git commit -m "Add feature X"

# Abortar merge
git merge --abort

# Merge con estrategia
git merge -X ours feature-x    # Prefiere versión local
git merge -X theirs feature-x  # Prefiere versión remota
```

**Resolución de conflictos:**

```bash
# Archivo con conflicto:
<<<<<<< HEAD (main)
código de main
=======
código de feature-x
>>>>>>> feature-x

# Proceso:
1. git status  # Ver archivos en conflicto
2. Editar archivo, elegir qué conservar
3. git add archivo-resuelto.txt
4. git commit

# Herramientas:
git mergetool
git diff --ours
git diff --theirs
git checkout --ours archivo.txt
git checkout --theirs archivo.txt
```

**Mejores prácticas:**

```bash
✓ Usa --no-ff para features importantes
✓ Resuelve conflictos en rama feature, no en main
✓ Prueba después de merge antes de push
✓ Usa --squash para features con commits WIP

✗ No fuerces -X ours/theirs sin revisar
✗ No ignores conflictos
✗ No hagas merge directo a main sin revisar
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

## 13. git pull - Descargando e Integrando

**¿Qué hace?**
Descarga cambios del remoto Y los integra en rama local.

**Funcionamiento interno:**
```
git pull = git fetch + git merge (o rebase)
```

**Uso práctico:**

```bash
# Pull básico
git pull
git pull origin main

# Pull con rebase (historia lineal)
git pull --rebase

# Pull solo si es fast-forward
git pull --ff-only

# Pull con autostash (stash automático)
git pull --autostash

# Pull con estrategia
git pull -X ours     # Prefiere local en conflictos
git pull -X theirs   # Prefiere remoto en conflictos
```

**Configuración útil:**

```bash
# Pull con rebase por defecto
git config --global pull.rebase true

# Solo fast-forward
git config --global pull.ff only

# Autostash por defecto
git config --global rebase.autoStash true
```

**Mejores prácticas:**

```bash
✓ Usa pull --rebase para historia limpia
✓ Commitea o stash antes de pull
✓ Usa --autostash para conveniencia
✓ Configura pull.rebase = true globalmente

✗ No uses pull sin revisar cambios importantes
✗ No ignores conflictos
✗ Evita pull sin tracking branch configurado
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
# Reset suave (mantiene cambios en staging)
git reset --soft HEAD~1

# Reset mixto (default, cambios en working)
git reset HEAD~1

# Reset duro (¡PIERDES CAMBIOS!)
git reset --hard HEAD~1

# Unstage archivo
git reset HEAD archivo.txt

# Reset a remoto
git reset --hard origin/main
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

