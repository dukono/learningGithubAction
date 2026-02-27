# 5. git log - Explorando la Historia

[🏠 Volver al Índice](../../GIT_COMANDOS_GUIA_PRACTICA.md)

---

## 5. git log - Explorando la Historia
[⬆️ Top](#tabla-de-contenidos)

**¿Qué hace?**
Muestra la historia de commits del repositorio con múltiples opciones de filtrado, formato y análisis. Es una herramienta fundamental para entender la evolución del código, buscar bugs, auditar cambios y analizar contribuciones.

**Funcionamiento interno:** [🔙](#5-git-log---explorando-la-historia)

```
1. Lee HEAD (o referencia especificada)
2. Obtiene commit object del hash
3. Lee metadata: author, date, message, tree, parents
4. Sigue recursivamente la cadena de commits anteriores
5. Aplica filtros especificados (autor, fecha, mensaje, archivos)
6. Formatea output según opciones (oneline, graph, stat, patch)
7. Pagina resultado (usa less por defecto)

Optimizaciones:
- Usa commit-graph para acelerar traversal
- Cache de objetos en memoria
- Traversal paralelo en repos grandes
```

**Uso práctico - Formatos básicos:** [🔙](#5-git-log---explorando-la-historia)

```bash
# ============================================
# FORMATOS DE VISUALIZACIÓN
# ============================================

# 1. Log básico (verbose, por defecto)
git log
# Muestra:
# - Hash completo
# - Autor y email
# - Fecha
# - Mensaje completo

# 2. Log compacto (UNA LÍNEA por commit)
git log --oneline
# Formato: hash-corto mensaje
# Ejemplo: abc123 Add user authentication

# 3. Log con decoraciones (refs)
git log --oneline --decorate
# Muestra: HEAD, ramas, tags
# Ejemplo: abc123 (HEAD -> main, origin/main) Add feature

# 4. Log con grafo visual (SUPER ÚTIL)
git log --oneline --graph
# Muestra estructura de ramas y merges
# Ejemplo:
# * abc123 Merge branch 'feature'
# |\
# | * def456 Add feature
# |/
# * 789abc Fix bug

# 5. Log con grafo de todas las ramas
git log --oneline --graph --all
# → Muestra TODO el repositorio
# → Incluye ramas locales y remotas
# → Muy útil para overview completo

# 6. Log con estadísticas de cambios
git log --stat
# Muestra archivos modificados y líneas +/-
# archivo.txt | 10 +++++-----

# 7. Log con diff completo (patch)
git log -p
# o: git log --patch
# → Muestra diff de cada commit
# → Útil para code review histórico

# 8. Log con diff de últimos N commits
git log -p -2
# → Solo últimos 2 commits con diff

# 9. Log con resumen corto
git log --oneline --stat
# → Combina hash + mensaje + stats
# → Balance perfecto de info

# 10. Log con formato personalizado
git log --pretty=format:"%h - %an, %ar : %s"
# Formato: hash - autor, fecha relativa : mensaje
# Ejemplo: abc123 - John, 2 days ago : Fix bug

# 11. Formatos predefinidos
git log --pretty=oneline
git log --pretty=short
git log --pretty=medium  # Default
git log --pretty=full
git log --pretty=fuller
git log --pretty=reference
```

**Uso práctico - Filtros por rango de commits:** [🔙](#5-git-log---explorando-la-historia)

```bash
# ============================================
# RANGOS Y EXCLUSIONES
# ============================================

# 1. Commits en rama A pero NO en rama B
git log main..feature-x
# → Commits únicos de feature-x
# → Útil para ver qué traerá el merge

git log origin/main..HEAD
# → Commits locales no pusheados
# → Equivalente a: git log @{u}..HEAD

# 2. Commits que difieren entre ramas (symmetric difference)
git log main...feature-x
# → Commits en A o B pero no en ambas
# → Muestra divergencia

# 3. Excluir commits (operador NOT)
git log main --not feature-x
# → Commits en main que NO están en feature-x
# → Equivalente a: git log feature-x..main

git log --all --not origin/main
# → Todo excepto lo que está en origin/main
# → Útil para ver trabajo local en todas las ramas

git log HEAD --not origin/main --not origin/develop
# → Commits locales no pusheados a ninguna de esas ramas

# 4. Commits que tocan archivo específico
git log -- archivo.txt
# → Historia de archivo específico
# → Sigue renames y movimientos

git log --all -- archivo.txt
# → Busca archivo en TODAS las ramas

# 5. Commits entre dos fechas
git log --since="2024-01-01" --until="2024-12-31"
# o: --after / --before

git log --since="2 weeks ago"
git log --since="yesterday"
git log --after="2024-01-01 10:30"

# 6. Últimos N commits
git log -n 5
# o: git log -5
# → Solo 5 commits más recientes

# 7. Commits desde tag específico
git log v1.0.0..HEAD
# → Commits desde release v1.0.0 hasta ahora

# 8. Primeros N commits (más antiguos)
git log --reverse | head -20
# → Invierte orden, muestra más antiguos

# 9. Commits de merge específicamente
git log --merges
# → Solo merge commits

git log --no-merges
# → Excluye merge commits (útil para features)

# 10. Commits que NO están en remoto
git log origin/main..HEAD --oneline
# → Ver qué falta pushear
```

**Uso práctico - Búsquedas y filtros:** [🔙](#5-git-log---explorando-la-historia)

```bash
# ============================================
# BÚSQUEDA EN COMMITS
# ============================================

# 1. Buscar en mensaje de commit
git log --grep="fix"
# → Commits con "fix" en el mensaje
# → Case-sensitive por defecto

git log --grep="bug" --grep="fix" --all-match
# → Commits con AMBAS palabras

git log --grep="feature" --grep="refactor" 
# → Commits con CUALQUIERA de las palabras (OR)

git log -i --grep="FIX"
# → Case-insensitive

# 2. Buscar por autor
git log --author="John"
git log --author="john@example.com"
git log --author="John\|Maria"  # Regex: John O Maria

# 3. Buscar por committer (diferente de author)
git log --committer="Jenkins"
# → Útil para commits automáticos

# 4. Buscar cambios en código (pickaxe)
git log -S"función_importante"
# → Commits que AÑADIERON o ELIMINARON ese string
# → Super útil para encontrar cuándo desapareció algo

git log -S"password" --all
# → Busca en todas las ramas

# 5. Buscar cambios en código (con diff)
git log -G"regex.*pattern"
# → Commits donde el diff matchea el regex
# → Más flexible que -S

# 6. Buscar por función específica (para lenguajes soportados)
git log -L :nombre_funcion:archivo.py
# → Historia de esa función específica
# → Sigue renames y movimientos

git log -L 10,20:archivo.txt
# → Historia de líneas 10-20 de archivo

# 7. Commits que afectan ruta específica
git log -- src/
git log -- "*.js"
git log -- src/**/*.py

# 8. Commits que tocan múltiples archivos
git log -- archivo1.txt archivo2.txt

# 9. Buscar commits que modificaron archivo específico
git log --diff-filter=M -- archivo.txt
# M = modificado
# A = añadido
# D = eliminado
# R = renombrado
# C = copiado

git log --diff-filter=D --summary
# → Archivos eliminados

# 10. Seguir renames de archivo
git log --follow -- archivo.txt
# → Sigue historia aunque cambie de nombre
# → IMPORTANTE para archivos renombrados
```

**Uso práctico - Formatos personalizados avanzados:** [🔙](#5-git-log---explorando-la-historia)

```bash
# ============================================
# PRETTY FORMATS (PERSONALIZACIÓN)
# ============================================

# Placeholders comunes:
# %H  - Hash completo
# %h  - Hash corto
# %T  - Tree hash
# %P  - Parent hashes
# %an - Author name
# %ae - Author email
# %ad - Author date
# %ar - Author date, relative (2 days ago)
# %cn - Committer name
# %cd - Commit date
# %cr - Commit date, relative
# %s  - Subject (mensaje)
# %b  - Body (mensaje completo)
# %d  - Ref names (HEAD, branches, tags)

# Colores:
# %C(red), %C(green), %C(blue), %C(yellow)
# %C(bold), %C(dim), %C(reset)

# 1. Formato compacto con autor y fecha
git log --pretty=format:"%h %an %ar: %s"
# abc123 John 2 days ago: Fix bug

# 2. Formato con colores
git log --pretty=format:"%C(yellow)%h%C(reset) %C(blue)%an%C(reset) %s"

# 3. Formato para CSV/export
git log --pretty=format:"%h,%an,%ae,%ad,%s" --date=short > commits.csv

# 4. Formato con árbol decorado
git log --graph --pretty=format:"%C(bold blue)%h%C(reset) - %C(bold green)(%ar)%C(reset) %s %C(bold yellow)%d%C(reset)"

# 5. Mostrar parent commits
git log --pretty=format:"%h %P %s"
# → Útil para entender merges

# 6. Formato JSON-like (para scripts)
git log --pretty=format:'{"commit":"%H","author":"%an","date":"%ad","message":"%s"}' --date=iso

# 7. Solo hash (para scripting)
git log --pretty=format:"%H"

# 8. Formato detallado con body
git log --pretty=format:"%h - %an (%ar)%n%n  %s%n%n%b%n" -3
```

**Uso práctico - Filtros de archivos y paths:**

```bash
# ============================================
# FILTROS POR ARCHIVOS Y RUTAS
# ============================================

# 1. Historia de archivo específico
git log -- ruta/archivo.txt

# 2. Historia de directorio
git log -- src/utils/

# 3. Archivos con patrón
git log -- "*.js"
git log -- "src/**/*.py"

# 4. Múltiples archivos
git log -- archivo1.txt archivo2.txt

# 5. Excluir paths
git log -- . ":(exclude)tests/"
git log -- . ":(exclude)*.md"

# 6. Solo archivos modificados (no añadidos/eliminados)
git log --diff-filter=M

# 7. Solo archivos añadidos
git log --diff-filter=A --summary

# 8. Solo archivos eliminados
git log --diff-filter=D --name-only

# 9. Solo archivos renombrados
git log --diff-filter=R --summary

# 10. Cambios en archivo específico con diff
git log -p -- archivo.txt

# 11. Mostrar nombres de archivos afectados
git log --name-only
git log --name-status  # Con tipo de cambio (M/A/D/R)

# 12. Mostrar estadísticas por archivo
git log --stat -- src/

# 13. Seguir archivo renombrado
git log --follow -- nuevo-nombre.txt
# → Sigue historia aunque se haya renombrado
```

**Uso práctico - Análisis y estadísticas:**

```bash
# ============================================
# ANÁLISIS DE REPOSITORIO
# ============================================

# 1. Contar commits por autor
git log --pretty=format:"%an" | sort | uniq -c | sort -rn
# Ejemplo output:
#   150 John Doe
#    95 Jane Smith
#    42 Bob Johnson

# 2. Contar commits por mes
git log --pretty=format:"%ad" --date=short | cut -c1-7 | sort | uniq -c

# 3. Actividad por día de la semana
git log --pretty=format:"%ad" --date=format:"%A" | sort | uniq -c | sort -rn

# 4. Ver quién modificó cada línea de archivo
git blame archivo.txt
git log -p -M --follow --stat -- archivo.txt

# 5. Commits en última semana
git log --since="1 week ago" --oneline | wc -l

# 6. Tamaño de commits (líneas cambiadas)
git log --shortstat --oneline

# 7. Archivos más modificados
git log --pretty=format: --name-only | sort | uniq -c | sort -rn | head -20

# 8. Autores más activos en archivo
git log --follow --pretty=format:"%an" -- archivo.txt | sort | uniq -c | sort -rn

# 9. Frecuencia de commits por hora
git log --pretty=format:"%ad" --date=format:"%H" | sort | uniq -c

# 10. Velocidad de desarrollo (commits/día)
git log --since="1 month ago" --pretty=format:"%ad" --date=short | sort | uniq -c
```

**Uso práctico - Debugging y bisect:**

```bash
# ============================================
# ENCONTRAR BUGS Y CAMBIOS
# ============================================

# 1. ¿Cuándo se introdujo este string?
git log -S"bug_causante" --source --all
# → Encuentra commit que añadió/eliminó ese código

# 2. ¿Cuándo se borró esta función?
git log -G"function delete_user" --all

# 3. ¿Quién cambió estas líneas?
git log -L 150,160:archivo.py
# → Historia de líneas 150-160

# 4. ¿En qué commit desapareció este archivo?
git log --all --full-history -- archivo-borrado.txt

# 5. Ver cambios entre dos versiones
git log v1.0..v2.0 --oneline

# 6. Commits que tocaron archivo Y contienen palabra
git log --grep="refactor" -- archivo.txt

# 7. Primer commit que introdujo archivo
git log --diff-filter=A --follow -- archivo.txt

# 8. Último commit que tocó archivo
git log -1 -- archivo.txt

# 9. Commits ordenados por fecha de commit (no autor)
git log --date-order

# 10. Ver commit y sus cambios
git show abc123
git show abc123:archivo.txt  # Ver versión de archivo en ese commit
```

**Uso práctico - Visualización avanzada:**

```bash
# ============================================
# GRAFOS Y VISUALIZACIÓN
# ============================================

# 1. Grafo completo decorado
git log --oneline --graph --all --decorate

# 2. Grafo solo de rama actual
git log --oneline --graph

# 3. Grafo con estadísticas
git log --graph --stat --oneline

# 4. Grafo compacto con fechas
git log --graph --date=relative --pretty=format:"%h %ad %s"

# 5. Ver merge commits con ambas líneas
git log --oneline --graph --first-parent
# → Sigue solo primera línea (más limpio en repos complejos)

# 6. Simplificar grafo (solo merges importantes)
git log --oneline --graph --simplify-by-decoration

# 7. Topological order (respeta estructura)
git log --topo-order --graph

# 8. Reverse chronological (más recientes primero) - default
git log --date-order

# 9. Author order (por fecha de author, no commit)
git log --author-date-order
```

**Opciones avanzadas y combinaciones:**

```bash
# ============================================
# COMBINACIONES PODEROSAS
# ============================================

# 1. Commits no pusheados con diff
git log origin/main..HEAD -p

# 2. Actividad de autor en fecha específica
git log --author="John" --since="2024-01-01" --until="2024-01-31" --oneline

# 3. Commits que afectan múltiples áreas
git log -- src/auth/ src/api/ --oneline

# 4. Merges problemáticos (con conflictos resueltos)
git log --merges -p --cc
# --cc muestra combined diff

# 5. Commits sin merge con stats de archivos JavaScript
git log --no-merges --stat -- "*.js"

# 6. Buscar en todas las ramas palabra en mensaje
git log --all --grep="JIRA-123"

# 7. Ver qué ramas contienen commit
git branch --contains abc123

# 8. Listar tags con sus commits
git log --oneline --decorate --simplify-by-decoration

# 9. Commits que modificaron permisos
git log -p | grep "old mode\|new mode"

# 10. Formato para code review
git log --oneline --no-merges --reverse v1.0.0..HEAD
```

**Alias recomendados para .gitconfig:**

```bash
[alias]
    # Log visual completo
    lg = log --graph --abbrev-commit --decorate --format=format:'%C(bold blue)%h%C(reset) - %C(bold green)(%ar)%C(reset) %C(white)%s%C(reset) %C(dim white)- %an%C(reset)%C(bold yellow)%d%C(reset)' --all
    
    # Log compacto
    ls = log --oneline --decorate
    
    # Log con stats
    ll = log --stat --abbrev-commit
    
    # Último commit
    last = log -1 HEAD --stat
    
    # Commits no pusheados
    unpushed = log @{u}..HEAD --oneline
    
    # Commits no traidos del remoto
    unpulled = log HEAD..@{u} --oneline
    
    # Historial de archivo
    filelog = log --follow -p --
    
    # Contribuciones por autor
    contributors = shortlog --summary --numbered --email
    
    # Grafo simple
    tree = log --oneline --graph --decorate --all
    
    # Ver qué cambió hoy
    today = log --since="midnight" --oneline --author="Tu Nombre"
    
    # Buscar en commits
    search = log --all --grep
```

**Troubleshooting y problemas comunes:**

```bash
# ============================================
# PROBLEMAS Y SOLUCIONES
# ============================================

# Problema 1: Log muy largo, no puedo salir
# → Presiona 'q' para salir del pager (less)

# Problema 2: No veo colores
git config --global color.ui auto

# Problema 3: Log de archivo no muestra nada
git log --all --full-history -- archivo.txt
# → Busca en todas las ramas e historia completa

# Problema 4: Quiero log sin paginación
git --no-pager log
# o:
git log | cat

# Problema 5: Log muy lento en repo grande
git log --oneline -100  # Limita resultados
git log --since="1 month ago"  # Limita rango

# Problema 6: No encuentro commit con mensaje específico
git log --all --grep="texto" -i
# → Busca case-insensitive en todas las ramas

# Problema 7: Quiero exportar log a archivo
git log --pretty=format:"%h %an %ad %s" --date=short > log.txt

# Problema 8: No sé qué commits faltan traer
git fetch
git log HEAD..origin/main --oneline

# Problema 9: Grafo muy complejo, no entiendo
git log --oneline --graph --first-parent
# → Solo primera línea (más simple)

# Problema 10: Busco commit pero no recuerdo rama
git log --all -S"texto_unico" --source
# → Muestra en qué rama está cada commit
```

**Casos de uso del mundo real:**

```bash
# ============================================
# ESCENARIOS REALES
# ============================================

# 1. Code review de PR
git log main..feature-branch --oneline --no-merges

# 2. ¿Qué cambió en último release?
git log v1.9.0..v2.0.0 --oneline

# 3. Auditoría de seguridad
git log -S"password" --all -p

# 4. ¿Quién rompió el build?
git log --since="yesterday" --until="now" --oneline

# 5. Generar CHANGELOG
git log v1.0.0..HEAD --pretty=format:"- %s (%h)" --no-merges

# 6. Encontrar cuándo se introdujo bug
git log -S"bug_code" -p

# 7. Ver trabajo de la semana pasada
git log --author="$(git config user.name)" --since="1 week ago" --oneline

# 8. Comparar actividad entre ramas
git log develop --not main --oneline

# 9. Listar todos los merges de feature branches
git log --merges --grep="Merge branch 'feature" --oneline

# 10. Verificar que commit está en producción
git log origin/production --oneline | grep abc123
```

**Mejores prácticas:** [🔙](#5-git-log---explorando-la-historia)

```bash
✓ Usa --oneline para overview rápido
✓ Usa --graph para entender merges
✓ Usa --all para ver TODO el repositorio
✓ Usa --follow para archivos renombrados
✓ Usa -S o -G para buscar código
✓ Usa --not para exclusiones complejas
✓ Crea alias para comandos frecuentes
✓ Limita resultados con -n en repos grandes
✓ Usa --stat para resumen de cambios
✓ Combina --since y --until para rangos específicos

✗ No corras git log sin límites en repos gigantes
✗ No olvides --follow para archivos renombrados
✗ No uses --all si solo necesitas rama actual
✗ No ignores --no-merges para análisis de features
✗ No uses formato complejo sin guardarlo en alias
```

---


---

## Navegación

- [⬅️ Anterior: Referencias de Commits](04.1-referencias-commits.md)
- [🏠 Volver al Índice](../../GIT_COMANDOS_GUIA_PRACTICA.md)
- [➡️ Siguiente: git branch](06-git-branch.md)

