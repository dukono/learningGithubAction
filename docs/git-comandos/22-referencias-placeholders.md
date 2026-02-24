# 22. Referencias y Placeholders de Formato

[🏠 Volver al Índice](../../GIT_COMANDOS_GUIA_PRACTICA.md)

---

## 22. Referencias y Placeholders de Formato
[⬆️ Top](#tabla-de-contenidos)

**¿Qué son?**
Son variables internas que Git expone para personalizar la salida de comandos como `git log`, `git for-each-ref`, `git show-ref`, etc. Permiten crear formatos personalizados para scripts, informes y automatización.

**¿Dónde se usan?**
- En comandos con la opción `--format="..."`
- En plantillas de hooks
- En scripts para procesar información de Git
- Para exportar datos estructurados

> 📖 **REFERENCIAS CRUZADAS:** Esta sección proporciona la referencia completa de placeholders.
> Para ejemplos específicos de cada comando, consulta:
> - **[Sección 5: git log](#5-git-log---explorando-la-historia)** - Formatos personalizados con `--pretty`
> - **[Sección 6: git branch](#6-git-branch---gestionando-líneas-de-desarrollo)** - Formato con `--format`
> - **[Sección 17: git tag](#17-git-tag---marcando-versiones)** - Listar tags con formato personalizado

---

### Comandos que usan placeholders

#### 1. git for-each-ref

**Descripción:** Itera sobre todas las referencias (ramas, tags, etc.) y muestra información personalizada.

**Sintaxis:**
```bash
git for-each-ref [<opciones>] [<patrón>]
```

**Placeholders principales:**

```bash
# Información de la referencia
%(refname)           # Nombre completo: refs/heads/main
%(refname:short)     # Nombre corto: main
%(refname:lstrip=N)  # Elimina N componentes del inicio
%(refname:rstrip=N)  # Elimina N componentes del final

# Información del objeto
%(objecttype)        # Tipo: commit, tag, tree, blob
%(objectsize)        # Tamaño del objeto en bytes
%(objectname)        # Hash SHA-1 completo
%(objectname:short)  # Hash SHA-1 abreviado (7 caracteres)
%(objectname:short=N) # Hash abreviado con N caracteres

# Información del commit/tag
%(tree)              # Hash del árbol
%(parent)            # Hash(es) del/los padre(s)
%(author)            # Autor completo: Nombre <email>
%(authorname)        # Solo el nombre del autor
%(authoremail)       # Solo el email del autor
%(authordate)        # Fecha del autor (formato por defecto)
%(committer)         # Committer completo: Nombre <email>
%(committername)     # Solo el nombre del committer
%(committeremail)    # Solo el email del committer
%(committerdate)     # Fecha del committer
%(subject)           # Primera línea del mensaje de commit
%(body)              # Cuerpo del mensaje (sin el subject)
%(contents)          # Mensaje completo (subject + body)

# Información de tracking
%(upstream)          # Rama remota asociada (upstream)
%(upstream:short)    # Nombre corto de la rama remota
%(upstream:track)    # Estado de tracking: [ahead N, behind M]
%(upstream:trackshort) # Estado abreviado: >, <, <>, =

# Información adicional
%(HEAD)              # '*' si es la rama actual, ' ' si no
%(color:...)         # Aplicar color
%(align:...)         # Alinear texto
%(if:...)%(then)%(else)%(end) # Condicionales
```

**Ejemplos prácticos:**

```bash
# 1. Listar todas las ramas con sus hashes
git for-each-ref --format="%(refname:short) %(objectname:short)" refs/heads/

# Salida:
# main a1b2c3d
# develop e4f5g6h
# feature/login i7j8k9l

# 2. Ramas con información de tracking
git for-each-ref --format="%(refname:short) %(upstream:short) %(upstream:track)" refs/heads/

# Salida:
# main origin/main [ahead 2, behind 1]
# develop origin/develop [ahead 5]
# feature/login  

# 3. Listar tags con fechas y autores
git for-each-ref --format="%(refname:short) %(authordate:short) %(authorname)" refs/tags/

# Salida:
# v1.0.0 2024-01-15 Juan Pérez
# v1.1.0 2024-02-20 María García

# 4. Información completa formateada
git for-each-ref --format="Rama: %(refname:short)
  Último commit: %(objectname:short)
  Autor: %(authorname)
  Fecha: %(authordate:relative)
  Mensaje: %(subject)
  Tracking: %(upstream:track)
" refs/heads/

# 5. Con colores
git for-each-ref --format="%(color:green)%(refname:short)%(color:reset) - %(subject)" refs/heads/

# 6. Ordenar por fecha de commit
git for-each-ref --sort=-committerdate --format="%(committerdate:short) %(refname:short)" refs/heads/

# Salida:
# 2024-03-01 feature/new-ui
# 2024-02-28 develop
# 2024-02-15 main

# 7. Filtrar ramas remotas
git for-each-ref --format="%(refname:short)" refs/remotes/origin/

# 8. Ramas con ahead/behind visual
git for-each-ref --format="%(refname:short) %(upstream:trackshort)" refs/heads/

# Salida:
# main <>    (divergente: tengo commits y hay remotos)
# develop >  (ahead: tengo commits para subir)
# feature <  (behind: hay commits remotos para traer)
```

**Opciones de formato de fecha:**

```bash
%(authordate:relative)    # "2 days ago"
%(authordate:short)       # "2024-02-13"
%(authordate:local)       # En zona horaria local
%(authordate:iso)         # ISO 8601: "2024-02-13 14:30:45 +0100"
%(authordate:iso-strict)  # ISO 8601 estricto
%(authordate:rfc)         # RFC 2822
%(authordate:raw)         # Unix timestamp + zona
%(authordate:unix)        # Solo Unix timestamp
%(authordate:format:...)  # Formato personalizado (strftime)
```

**Opciones de git for-each-ref:**

```bash
--count=<n>              # Limitar a n referencias
--sort=<key>             # Ordenar por campo (- para descending)
--format=<format>        # Formato de salida personalizado
--shell                  # Formato para shell scripts
--perl                   # Formato para Perl
--python                 # Formato para Python
--tcl                    # Formato para Tcl
--points-at=<object>     # Solo refs que apuntan a objeto
--merged[=<commit>]      # Solo refs fusionadas en commit
--no-merged[=<commit>]   # Solo refs NO fusionadas en commit
--contains[=<commit>]    # Solo refs que contienen commit
--no-contains[=<commit>] # Solo refs que NO contienen commit
```

---

#### 2. git log con --format

**Placeholders para commits:**

```bash
# Hash del commit
%H    # Hash completo (40 caracteres)
%h    # Hash abreviado
%T    # Hash del tree
%t    # Hash del tree abreviado
%P    # Hashes de los padres (completos)
%p    # Hashes de los padres (abreviados)

# Información del autor
%an   # Nombre del autor
%ae   # Email del autor
%aE   # Email del autor (respetando .mailmap)
%ad   # Fecha del autor (formato según --date)
%aD   # Fecha del autor (RFC2822)
%ar   # Fecha del autor (relativa)
%at   # Fecha del autor (timestamp UNIX)
%ai   # Fecha del autor (ISO 8601)
%aI   # Fecha del autor (ISO 8601 estricto)

# Información del committer
%cn   # Nombre del committer
%ce   # Email del committer
%cE   # Email del committer (respetando .mailmap)
%cd   # Fecha del committer
%cD   # Fecha del committer (RFC2822)
%cr   # Fecha del committer (relativa)
%ct   # Fecha del committer (timestamp UNIX)
%ci   # Fecha del committer (ISO 8601)
%cI   # Fecha del committer (ISO 8601 estricto)

# Referencias (ramas/tags)
%d    # Nombres de ref (como --decorate)
%D    # Nombres de ref sin los paréntesis
%S    # Ref name (dada en la línea de comando)

# Mensaje del commit
%s    # Subject (primera línea)
%f    # Subject sanitizado (para nombre de archivo)
%b    # Body (resto del mensaje)
%B    # Body raw (sin procesar)
%N    # Notas del commit
%GG   # Mensaje raw de verificación GPG
%G?   # Estado de firma GPG
%GS   # Nombre del firmante GPG
%GK   # Key usada para firmar

# Colores
%Cred       # Cambiar a rojo
%Cgreen     # Cambiar a verde
%Cblue      # Cambiar a azul
%Creset     # Reset color
%C(...)     # Color específico (por nombre o código)

# Otros
%n    # Nueva línea
%x00  # Byte nulo
%%    # Literal '%'
```

**Ejemplos prácticos con git log:**

```bash
# 1. Log personalizado básico
git log --format="%h - %an, %ar : %s"

# Salida:
# a1b2c3d - Juan Pérez, 2 days ago : Add login feature
# e4f5g6h - María García, 1 week ago : Fix bug in payment

# 2. Con colores
git log --format="%C(yellow)%h%C(reset) - %C(cyan)%an%C(reset), %ar : %s"

# 3. Formato completo estilo GitHub
git log --format="%C(auto)%h%d %s %C(black)%C(bold)%cr by %an"

# 4. Para exportar a CSV
git log --format="%h,%an,%ae,%ad,%s" --date=short > commits.csv

# 5. Con estadísticas
git log --format="%h %an %s" --stat

# 6. Commits con firma GPG
git log --format="%h %s %G? %GS"
# %G? muestra: G (buena), B (mala), U (sin verificar), N (sin firma)

# 7. Log detallado para análisis
git log --format="Commit: %H
Autor: %an <%ae>
Fecha: %ad
Committer: %cn <%ce>
Fecha Commit: %cd

%s

%b
---"

# 8. Log tipo GitHub/GitLab
git log --graph --format="%C(yellow)%h%C(reset) %C(bold blue)%an%C(reset) %C(dim white)%ar%C(reset) %s %C(auto)%d"

# 9. Solo hash y subject
git log --format="%h %s" -10

# 10. Log con información de merge
git log --format="%h %s (padres: %p)" --merges
```

**Opciones de --date para git log:**

```bash
git log --format="%h %ad %s" --date=relative   # "2 hours ago"
git log --format="%h %ad %s" --date=local      # Zona horaria local
git log --format="%h %ad %s" --date=iso        # ISO 8601
git log --format="%h %ad %s" --date=iso-strict # ISO 8601 estricto
git log --format="%h %ad %s" --date=rfc        # RFC 2822
git log --format="%h %ad %s" --date=short      # YYYY-MM-DD
git log --format="%h %ad %s" --date=raw        # Timestamp + zona
git log --format="%h %ad %s" --date=unix       # Timestamp UNIX
git log --format="%h %ad %s" --date=format:"%Y-%m-%d %H:%M"  # Personalizado
git log --format="%h %ad %s" --date=human      # Formato legible
git log --format="%h %ad %s" --date=default    # Formato por defecto
```

---

#### 3. git show-ref

**Descripción:** Muestra referencias disponibles en el repositorio local.

**Sintaxis:**
```bash
git show-ref [<opciones>] [<patrón>]
```

**Salida por defecto:**
```bash
git show-ref

# Formato: <hash> <refname>
a1b2c3d4... refs/heads/main
e5f6g7h8... refs/heads/develop
i9j0k1l2... refs/remotes/origin/main
m3n4o5p6... refs/tags/v1.0.0
```

**Opciones:**

```bash
--head              # Incluir HEAD
--heads             # Solo ramas locales (refs/heads/)
--tags              # Solo tags (refs/tags/)
-d, --dereference   # Mostrar objeto al que apunta un tag anotado
--hash[=<n>]        # Solo mostrar hash (opcionalmente primeros n chars)
--abbrev[=<n>]      # Abreviar hash a n caracteres
--quiet             # No mostrar nada, solo retornar código de salida
--verify            # Verificar que existe exactamente una referencia
--exclude-existing  # Filtrar refs que ya existen
```

**Ejemplos:**

```bash
# 1. Ver todas las referencias
git show-ref

# 2. Solo ramas locales
git show-ref --heads

# 3. Solo tags
git show-ref --tags

# 4. Buscar una rama específica
git show-ref main
# a1b2c3d4... refs/heads/main
# i9j0k1l2... refs/remotes/origin/main

# 5. Solo el hash
git show-ref --hash refs/heads/main
# a1b2c3d4e5f6...

# 6. Hash abreviado
git show-ref --hash --abbrev refs/heads/main
# a1b2c3d

# 7. Verificar que existe una referencia
git show-ref --verify refs/heads/main
# Retorna 0 si existe, 1 si no

# 8. Con HEAD
git show-ref --head

# 9. Tags con dereferencia (objeto apuntado)
git show-ref --tags --dereference
# m3n4o5p6... refs/tags/v1.0.0
# a1b2c3d4... refs/tags/v1.0.0^{}  (commit al que apunta)
```

---

#### 4. Otros comandos que usan placeholders

##### git branch --format

```bash
# Formato personalizado para ramas
git branch --format="%(refname:short) → %(upstream:short) %(upstream:track)"

# Con colores
git branch --format="%(color:green)%(refname:short)%(color:reset) %(upstream:trackshort)"
```

##### git tag --format

```bash
# Listar tags con información
git tag --format="%(refname:short): %(subject) - %(authorname)"

# Tags ordenados por fecha
git tag --sort=-creatordate --format="%(creatordate:short) %(refname:short)"
```

---

### Formato avanzado: condicionales y alineación

#### Condicionales

```bash
# Sintaxis: %(if:condition)%(then)TEXTO%(else)OTRO%(end)

# Ejemplo: mostrar upstream solo si existe
git for-each-ref --format="%(refname:short) %(if)%(upstream)%(then)→ %(upstream:short)%(end)" refs/heads/

# Ejemplo: color según si está merged
git for-each-ref --format="%(if:equals=refs/heads/main)%(refname)%(then)%(color:green)%(end)%(refname:short)%(color:reset)"
```

#### Alineación

```bash
# %(align:<width>,<position>)TEXTO%(end)
# position: left, right, middle

# Ejemplo: tabla alineada
git for-each-ref --format="%(align:20,left)%(refname:short)%(end) %(align:10,right)%(objectname:short)%(end) %(subject)" refs/heads/

# Salida:
# main                  a1b2c3d    Initial commit
# develop               e4f5g6h    Add feature
# feature/login         i7j8k9l    Login page
```

---

### Casos de uso prácticos

#### 1. Listar ramas desactualizadas

```bash
git for-each-ref --sort=-committerdate --format="%(committerdate:short) %(refname:short)" refs/heads/ | head -10
```

#### 2. Encontrar ramas sin upstream

```bash
git for-each-ref --format="%(if)%(upstream)%(then)%(else)%(refname:short)%(end)" refs/heads/ | grep -v '^$'
```

#### 3. Exportar historial para análisis

```bash
git log --format="%H,%an,%ae,%ad,%s" --date=iso-strict --all > commits.csv
```

#### 4. Verificar firmas GPG

```bash
git log --format="%h %G? %GS: %s" --show-signature
```

#### 5. Listar ramas merged y no merged

```bash
# Merged en main
git for-each-ref --merged=main --format="✓ %(refname:short)" refs/heads/

# No merged en main
git for-each-ref --no-merged=main --format="✗ %(refname:short)" refs/heads/
```

#### 6. Ver estado de tracking de todas las ramas

```bash
git for-each-ref --format="%(refname:short)%(if)%(upstream)%(then) → %(upstream:short) %(upstream:track)%(else) (sin upstream)%(end)" refs/heads/

# Salida:
# main → origin/main [ahead 2, behind 1]
# develop → origin/develop [up to date]
# feature/login (sin upstream)
```

#### 7. Buscar quién hizo el último commit en cada rama

```bash
git for-each-ref --format="%(refname:short): %(authorname) - %(authordate:relative)" refs/heads/
```

#### 8. Script para limpiar ramas merged

```bash
#!/bin/bash
# Listar ramas merged (excepto main/develop) y eliminarlas
git for-each-ref --format="%(refname:short)" --merged=main refs/heads/ | \
  grep -v -E '^(main|develop)$' | \
  xargs -r git branch -d
```

#### 9. Log estilo commit convencional

```bash
git log --format="%C(yellow)%h%C(reset) %C(blue)%ad%C(reset) %C(green)%an%C(reset)%n  %s%n" --date=short
```

#### 10. Comparar fechas de autor vs committer

```bash
git log --format="Commit: %h%nAutor fecha: %ai%nCommitter fecha: %ci%nDiferencia: %ar vs %cr%n---"
```

---

### Resumen de placeholders más usados

| Placeholder | Descripción | Ejemplo de salida |
|-------------|-------------|-------------------|
| `%(refname)` | Nombre completo de ref | `refs/heads/main` |
| `%(refname:short)` | Nombre corto de ref | `main` |
| `%(objectname)` | Hash SHA-1 completo | `a1b2c3d4e5f6...` |
| `%(objectname:short)` | Hash abreviado | `a1b2c3d` |
| `%(upstream)` | Rama upstream | `refs/remotes/origin/main` |
| `%(upstream:short)` | Upstream corto | `origin/main` |
| `%(upstream:track)` | Estado tracking | `[ahead 2, behind 1]` |
| `%(upstream:trackshort)` | Estado abreviado | `<>` |
| `%(authorname)` | Nombre del autor | `Juan Pérez` |
| `%(authoremail)` | Email del autor | `juan@example.com` |
| `%(authordate)` | Fecha del autor | `Tue Feb 13 14:30:00 2024` |
| `%(subject)` | Primera línea mensaje | `Add login feature` |
| `%(contents)` | Mensaje completo | Todo el mensaje |
| `%h` (git log) | Hash abreviado | `a1b2c3d` |
| `%s` (git log) | Subject | `Fix bug` |
| `%an` (git log) | Autor nombre | `Juan Pérez` |
| `%ad` (git log) | Autor fecha | Según `--date` |
| `%d` (git log) | Refs decoradas | `(HEAD -> main, origin/main)` |

---

### Mejores prácticas

✅ **Usar en scripts:** Ideal para automatización y CI/CD
✅ **Exportar datos:** CSV, JSON-like para análisis
✅ **Personalizar salidas:** Adaptar a tus necesidades
✅ **Combinación con otros comandos:** Potente con grep, awk, etc.

❌ **No abusar de colores en scripts:** Solo para terminal
❌ **No confiar en orden sin --sort:** Especifica orden explícitamente
❌ **Cuidado con caracteres especiales:** Sanitizar para shell

---

### Troubleshooting

**Problema:** El formato no muestra lo esperado
```bash
# Verificar que el campo existe
git for-each-ref --format="%(refname) %(upstream)" refs/heads/
# Si upstream está vacío, esa rama no tiene tracking
```

**Problema:** Fechas en formato incorrecto
```bash
# Especificar formato de fecha
git for-each-ref --format="%(authordate:short)" refs/heads/
```

**Problema:** Condicionales no funcionan
```bash
# Asegurar sintaxis correcta
git for-each-ref --format="%(if)%(upstream)%(then)Tiene upstream%(else)Sin upstream%(end)" refs/heads/
```

---

### Recursos adicionales

- `git help for-each-ref` - Documentación completa de placeholders
- `git help log` - Sección PRETTY FORMATS
- [Git Documentation - for-each-ref](https://git-scm.com/docs/git-for-each-ref)
- [Git Documentation - log formats](https://git-scm.com/docs/git-log#_pretty_formats)

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

---

## Navegación

- [⬅️ Anterior: git rm y git mv](21-git-rm-mv.md)
- [🏠 Volver al Índice](../../GIT_COMANDOS_GUIA_PRACTICA.md)
- [➡️ Siguiente: Variables de Shell](23-variables-shell.md)

