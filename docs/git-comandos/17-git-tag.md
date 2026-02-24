# 17. git tag - Marcando Versiones

[🏠 Volver al Índice](../../GIT_COMANDOS_GUIA_PRACTICA.md)

---

## 17. git tag - Marcando Versiones
[⬆️ Top](#17-git-tag---marcando-versiones)

**¿Qué hace?**
Crea referencias inmutables que apuntan a commits concretos, usadas para marcar versiones de un proyecto (v1.0.0, v2.3.1...). A diferencia de las ramas, los tags no avanzan: siempre apuntan al mismo commit.

Son el mecanismo estándar para marcar "este commit es la versión 2.0.0 que salió a producción el día X".

**Funcionamiento interno:** [🔙](#17-git-tag---marcando-versiones)

```
Hay dos tipos de tags:

LIGHTWEIGHT TAG (tag ligero):
  → Solo un puntero directo al commit (como una rama que no se mueve)
  → Guardado en: .git/refs/tags/v1.0.0
  → Contiene: solo el hash del commit
  → No tiene autor, fecha propia, ni mensaje
  → Útil para marcas temporales personales, no para releases

ANNOTATED TAG (tag anotado) - RECOMENDADO para releases:
  → Es un objeto Git completo (como un commit, pero de tipo "tag")
  → Guardado en: .git/objects/ (objeto propio)
  → Contiene: mensaje, autor, fecha de creación del tag, firma GPG opcional
  → El objeto tag apunta al commit
  → Tiene su propio hash (diferente al hash del commit)
  → Visible en "git log --decorate" y en herramientas como GitHub

¿Por qué usar annotated para releases?
  - Queda registro de quién creó el tag y cuándo
  - Puede tener notas de release
  - Puede firmarse con GPG para verificar autenticidad
  - GitHub/GitLab crean releases automáticamente de annotated tags
```

**Todas las opciones importantes:** [🔙](#17-git-tag---marcando-versiones)

```bash
# Crear lightweight tag
git tag v1.0.0

# Crear annotated tag (RECOMENDADO)
git tag -a v1.0.0 -m "Release 1.0.0"

# ============================================
# LISTAR TAGS
# ============================================

# Listar todos los tags
git tag
# → Orden alfabético por defecto

# Listar con patrón
git tag -l "v1.*"
git tag -l "v*-beta*"
git tag --list "release-*"

# Listar tags que contienen un commit
git tag --contains abc123
git tag --contains HEAD

# Listar tags que NO contienen un commit
git tag --no-contains abc123

# Listar tags merged/no-merged
git tag --merged main
git tag --no-merged main

# Listar tags con anotaciones
git tag -n
git tag -n5  # Muestra hasta 5 líneas del mensaje

# Ordenar tags
git tag --sort=-creatordate      # Por fecha (más recientes primero)
git tag --sort=version:refname   # Por versión semántica
git tag --sort=refname           # Alfabético
git tag --sort=-taggerdate       # Por fecha del tagger


# ============================================
# VER DETALLES DE TAGS
# ============================================

# Ver información completa
git show v1.0.0
# → Muestra tag object + commit + diff

# Ver solo información del tag
git show v1.0.0 --no-patch

# Ver múltiples tags
git show v1.0.0 v2.0.0

# Ver commit al que apunta
git rev-list -n 1 v1.0.0

# Ver diferencia entre tags
git diff v1.0.0..v2.0.0
git log v1.0.0..v2.0.0 --oneline


# ============================================
# FORMATO PERSONALIZADO (--format)
# ============================================

> 📖 **NOTA:** Para una referencia completa de todos los placeholders disponibles,
> formatos avanzados, condicionales y ejemplos con otros comandos (log, branch,
> for-each-ref, show-ref, etc.), consulta la **[Sección 22: Referencias y Placeholders de Formato](#22-referencias-y-placeholders-de-formato)**.

# git tag también acepta placeholders como git branch
# Ver sección 22 para lista completa

# Lista simple con hash
git tag --format="%(refname:short) %(objectname:short)"
# Salida:
# v1.0.0 a1b2c3d
# v1.1.0 e4f5g6h
# v2.0.0 i7j8k9l

# Con fecha y autor
git tag --format="%(refname:short) | %(creatordate:short) | %(taggername)"
# Salida:
# v1.0.0 | 2024-01-15 | Juan Pérez
# v1.1.0 | 2024-02-20 | María García

# Con mensaje del tag
git tag --format="%(refname:short) - %(contents:subject)"
# Salida:
# v1.0.0 - Initial release
# v1.1.0 - Bug fixes and improvements

# Con información completa
git tag --format="Tag: %(refname:short)
Commit: %(objectname:short)
Fecha: %(creatordate:short)
Autor: %(taggername) <%(taggeremail)>
Mensaje: %(contents:subject)
---"

# Con colores
git tag --format="%(color:green)%(refname:short)%(color:reset) (%(creatordate:relative))"

# Ordenado por fecha con formato
git tag --sort=-creatordate --format="%(creatordate:short) %(refname:short) - %(contents:subject)"

# Export a CSV
git tag --format="%(refname:short),%(objectname:short),%(taggername),%(creatordate:short),%(contents:subject)" > tags.csv


# PLACEHOLDERS ESPECÍFICOS PARA TAGS:
%(refname)              # refs/tags/v1.0.0
%(refname:short)        # v1.0.0
%(objectname)           # Hash del tag object
%(objectname:short)     # Hash abreviado
%(objecttype)           # "tag" o "commit"
%(taggername)           # Nombre del tagger (solo annotated)
%(taggeremail)          # Email del tagger
%(taggerdate)           # Fecha del tag
%(taggerdate:short)     # 2024-02-13
%(taggerdate:relative)  # "2 days ago"
%(creatordate)          # Fecha de creación (funciona con lightweight)
%(contents)             # Mensaje completo del tag
%(contents:subject)     # Primera línea del mensaje
%(contents:body)        # Cuerpo del mensaje (sin subject)


# ============================================
# CREAR Y GESTIONAR TAGS
# ============================================

# Crear lightweight tag (simple puntero)
git tag v1.0.0
# → Solo referencia al commit, sin metadata

# Crear annotated tag (RECOMENDADO para releases)
git tag -a v1.0.0 -m "Release 1.0.0"
# → Objeto completo: mensaje, autor, fecha, firma opcional

# Tag con mensaje multilínea
git tag -a v1.0.0 -m "Release 1.0.0

Features:
- User authentication
- Payment integration
- Dashboard redesign"

# Tag en commit específico
git tag -a v1.0.0 abc123 -m "Release 1.0.0"

# Tag con editor
git tag -a v1.0.0
# → Abre editor para escribir mensaje extenso

# Tag con firma GPG
git tag -s v1.0.0 -m "Signed release 1.0.0"
# → Crea tag firmado, verificable

# Verificar firma de tag
git tag -v v1.0.0
git show --show-signature v1.0.0

# Tag forzado (reemplazar existente)
git tag -f v1.0.0
git tag -af v1.0.0 -m "Release 1.0.0 (updated)"


# ============================================
# ELIMINAR TAGS
# ============================================

# Eliminar tag local
git tag -d v1.0.0

# Eliminar múltiples tags locales
git tag -d v1.0.0 v1.1.0 v2.0.0

# Eliminar tag remoto
git push origin --delete v1.0.0
# o (sintaxis vieja):
git push origin :refs/tags/v1.0.0

# Eliminar todos los tags locales (cuidado)
git tag -l | xargs git tag -d


# ============================================
# PUSH DE TAGS
# ============================================

# Push de un tag específico
git push origin v1.0.0

# Push de todos los tags
git push --tags
# o:
git push origin --tags

# Push de tag y commit juntos
git push origin main --follow-tags
# → Pushea commit + tags anotados alcanzables

# Configurar push automático de tags
git config --global push.followTags true
# → Pushea tags automáticamente con commits


# ============================================
# CHECKOUT Y RAMAS DESDE TAGS
# ============================================

# Checkout de tag (detached HEAD)
git checkout v1.0.0
# → Estás en estado "detached HEAD"
# → Útil para revisar código de release

# Crear rama desde tag
git checkout -b hotfix-1.0.1 v1.0.0
# → Crea rama apuntando al commit del tag
# → Útil para hotfixes en versiones antiguas

# Ver en qué ramas está un tag
git branch --contains v1.0.0
git branch -a --contains v1.0.0  # Incluye remotas
```

**Semantic Versioning:** [🔙](#17-git-tag---marcando-versiones)

```bash
# Formato: v<MAJOR>.<MINOR>.<PATCH>
#
# MAJOR → cambio incompatible con versiones anteriores (breaking change)
# MINOR → nueva funcionalidad compatible con versiones anteriores
# PATCH → corrección de bugs compatible

# Ejemplos:
v1.0.0           # Primera versión estable
v1.0.0-alpha.1   # Versión alpha (muy inestable, solo para desarrollo)
v1.0.0-beta.2    # Versión beta (bastante estable, pruebas externas)
v1.0.0-rc.1      # Release candidate (casi lista, últimas pruebas)

# Cuándo incrementar cada número:
v1.2.3 → v2.0.0  # Eliminaste una API, cambiaste el formato de datos... (MAJOR)
v1.2.3 → v1.3.0  # Añadiste nueva funcionalidad sin romper lo anterior (MINOR)
v1.2.3 → v1.2.4  # Corregiste un bug sin añadir features (PATCH)
```

**Casos de uso reales:** [🔙](#17-git-tag---marcando-versiones)

```bash
# ─────────────────────────────────────────────────────────────────
# CASO 1: Proceso completo de release
# ─────────────────────────────────────────────────────────────────
# El equipo decide que la versión 2.1.0 está lista para salir.

# Paso 1: Asegurarse de estar en main con todo actualizado
git checkout main
git pull origin main

# Paso 2: Crear el tag annotado con notas de la release
git tag -a v2.1.0 -m "Release 2.1.0

Nuevas funcionalidades:
- Dashboard de análisis (issue #234)
- Exportación a PDF (issue #241)
- Soporte multi-idioma (issue #198)

Bugs corregidos:
- Fix error de login en IE11 (issue #267)
- Fix ordenación de tablas (issue #271)"

# Paso 3: Subir el commit Y el tag al remoto
git push origin main
git push origin v2.1.0
# → GitHub/GitLab crean automáticamente una "Release" con estas notas


# ─────────────────────────────────────────────────────────────────
# CASO 2: Hotfix en una versión antigua (v1.x mientras main ya está en v2)
# ─────────────────────────────────────────────────────────────────
# Hay un bug de seguridad en v1.5.0 y necesitas un patch.

# Paso 1: Crear rama de hotfix desde el tag de la versión afectada
git checkout -b hotfix/v1.5.1 v1.5.0

# Paso 2: Arreglar el bug
git add fix-seguridad.js
git commit -m "fix: parchear vulnerabilidad XSS en formulario login"

# Paso 3: Taggear el hotfix
git tag -a v1.5.1 -m "Security hotfix v1.5.1 - fix XSS vulnerability"
git push origin hotfix/v1.5.1
git push origin v1.5.1


# ─────────────────────────────────────────────────────────────────
# CASO 3: Ver qué cambió entre dos versiones
# ─────────────────────────────────────────────────────────────────
# Un cliente pregunta qué cambió entre v1.3.0 y v2.0.0

# Lista de commits:
git log v1.3.0..v2.0.0 --oneline

# Diferencia de código:
git diff v1.3.0 v2.0.0

# Solo qué archivos cambiaron:
git diff v1.3.0 v2.0.0 --name-status


# ─────────────────────────────────────────────────────────────────
# CASO 4: Revisar el código de una versión específica
# ─────────────────────────────────────────────────────────────────
# Quieres ver cómo estaba el código en la versión v1.2.0

git checkout v1.2.0
# → Detached HEAD: puedes ver el código pero no commitear
# Si quieres hacer cambios desde esa versión (ej: hotfix):
git checkout -b hotfix/desde-v1.2.0 v1.2.0


# ─────────────────────────────────────────────────────────────────
# CASO 5: Corregir o mover un tag erróneo
# ─────────────────────────────────────────────────────────────────
# Pusheaste v3.0.0 al commit equivocado.

# Si TODAVÍA NO has pusheado el tag:
git tag -d v3.0.0                    # Borra tag local
git tag -a v3.0.0 <hash-correcto> -m "Release 3.0.0"   # Re-crear en el commit correcto
git push origin v3.0.0               # Push del correcto

# Si YA pusheaste el tag (implica avisar al equipo):
git tag -d v3.0.0                    # Borra local
git push origin --delete v3.0.0      # Borra en remoto
git tag -a v3.0.0 <hash-correcto> -m "Release 3.0.0"
git push origin v3.0.0
# ⚠️ Si otros ya descargaron el tag antiguo, necesitarán borrarlo localmente
```

**Troubleshooting:** [🔙](#17-git-tag---marcando-versiones)

```bash
# ─────────────────────────────────────────────────────────────────
# Problema 1: El tag no aparece en GitHub después del push
# ─────────────────────────────────────────────────────────────────
# Causa: Hiciste push del commit pero olvidaste pushear el tag
git push origin v2.0.0               # Push del tag específico
# ó para subir todos los tags pendientes:
git push --tags


# ─────────────────────────────────────────────────────────────────
# Problema 2: "tag already exists" al intentar crear un tag
# ─────────────────────────────────────────────────────────────────
git tag -d v1.0.0                    # Borra el existente (si no está pusheado)
git tag -a v1.0.0 -m "Release 1.0.0" # Crea de nuevo
# Si ya está pusheado, necesitarás borrar en remoto también (ver Caso 5)


# ─────────────────────────────────────────────────────────────────
# Problema 3: Tags del remoto no aparecen localmente
# ─────────────────────────────────────────────────────────────────
git fetch --tags                     # Descarga todos los tags del remoto
git fetch origin v2.0.0              # Descarga un tag específico


# ─────────────────────────────────────────────────────────────────
# Problema 4: Ver en qué commit está un tag
# ─────────────────────────────────────────────────────────────────
git rev-list -n 1 v2.0.0             # Muestra el hash del commit
git show v2.0.0 --no-patch           # Muestra info del tag y del commit
```

**Mejores prácticas:** [🔙](#17-git-tag---marcando-versiones)

```bash
✓ Usa SIEMPRE annotated tags para releases públicas (-a)
✓ Sigue Semantic Versioning (vMAJOR.MINOR.PATCH)
✓ Escribe notas descriptivas en el tag (qué cambió, qué se arregló)
✓ Push tags explícitamente después de crearlos
✓ Crea el tag desde main DESPUÉS de hacer el merge de la release
✓ Firma tags de release con GPG (-s) si el proyecto lo requiere

✗ No muevas tags ya pusheados sin avisar al equipo
✗ No uses lightweight tags para releases públicas
✗ No olvides pushear: git push origin <tag>
✗ No uses nombres de tags inconsistentes (a veces v1.0, a veces 1.0.0, a veces release-1)
```

---

## Navegación

- [⬅️ Anterior: git stash](16-git-stash.md)
- [🏠 Volver al Índice](../../GIT_COMANDOS_GUIA_PRACTICA.md)
- [➡️ Siguiente: git revert](18-git-revert.md)

