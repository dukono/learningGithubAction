# 12. git fetch - Descargando Cambios

[🏠 Volver al Índice](../../GIT_COMANDOS_GUIA_PRACTICA.md)

---

## 12. git fetch - Descargando Cambios
[⬆️ Top](#12-git-fetch---descargando-cambios)

**¿Qué hace?**
Descarga objetos y referencias del repositorio remoto, actualizando las "remote tracking branches" (origin/main, origin/develop...) pero SIN tocar tu working directory ni tus ramas locales. Es como "mirar qué hay de nuevo" sin comprometerte a integrarlo todavía.

**Funcionamiento interno:** [🔙](#12-git-fetch---descargando-cambios)

```
ANTES del fetch:
  Tu local:                   Remoto (GitHub):
  main         → commit A     main → commits A, B, C (ellos avanzaron)
  origin/main  → commit A     

DESPUÉS del fetch:
  Tu local:                   Remoto (GitHub):
  main         → commit A     main → commits A, B, C (sin cambios)
  origin/main  → commit C  ← ¡actualizado!

  Tu rama "main" sigue en A.
  Solo "origin/main" (la copia local del estado remoto) avanzó a C.

Pasos internos:
  1. Conecta con el remoto
  2. Descarga objetos (commits, trees, blobs) que no tienes en local
  3. Actualiza refs/remotes/origin/* con los nuevos estados
  4. NO toca refs/heads/* (tus ramas locales)
  5. NO toca el working directory

Diferencia clave:
  git fetch  → descarga pero NO integra (tú decides cuándo y cómo)
  git pull   → descarga Y automáticamente hace merge/rebase
```

**Todas las opciones importantes:** [🔙](#12-git-fetch---descargando-cambios)

```bash
# ============================================
# 1. Fetch básico (lo más común)
# ============================================
# Situación: Empiezas el día de trabajo y quieres saber qué pasó
# mientras no estabas. Descargas todo sin modificar nada local.
git fetch
# ó explícitamente indicando el remoto:
git fetch origin


# ============================================
# 2. Fetch de una rama específica
# ============================================
# Situación: Solo te interesa saber qué pasó en "develop",
# no quieres descargar todas las demás ramas.
git fetch origin develop
# → Solo actualiza origin/develop


# ============================================
# 3. --all: fetch de todos los remotos
# ============================================
# Situación: Tienes configurados varios remotos (origin, upstream, backup)
# y quieres actualizar todos de un golpe.
git fetch --all
# → Equivale a hacer "git fetch" para cada remoto configurado


# ============================================
# 4. --prune: limpiar refs obsoletas
# ============================================
# Situación: Un compañero borró "feature/old-login" en el remoto.
# En tu local sigue apareciendo "origin/feature/old-login".
git fetch --prune
# → Descarga nuevos cambios Y elimina refs que ya no existen en el remoto
# → Equivalente a: git fetch && git remote prune origin

# Solo limpiar sin descargar nada nuevo:
git remote prune origin


# ============================================
# 5. --tags / --no-tags: control de tags
# ============================================
# Por defecto, fetch descarga tags que apuntan a commits que ya tienes.
# Para descargar TODOS los tags del remoto:
git fetch --tags

# Para NO descargar ningún tag:
git fetch --no-tags


# ============================================
# 6. --dry-run: ver qué pasaría sin hacer nada
# ============================================
# Situación: Quieres saber qué se descargaría antes de decidir.
git fetch --dry-run
# → Muestra qué refs se actualizarían, pero NO descarga nada realmente


# ============================================
# 7. Fetch actualizando directamente una rama local
# ============================================
# Situación: Quieres actualizar tu rama local "main" con el remoto
# sin tener que hacer checkout + pull.
git fetch origin main:main
# → Trae origin/main Y actualiza directamente tu rama local main
# ⚠️ Solo funciona si NO estás en la rama main (si estás, Git lo rechaza)


# ============================================
# 8. --depth: fetch superficial (para clones con --depth)
# ============================================
# Situación: Clonaste con --depth 1 y ahora necesitas más historia.
git fetch --depth 50 origin main
# → Amplía la historia disponible a los últimos 50 commits

# Para obtener el historial completo desde un clone superficial:
git fetch --unshallow


# ============================================
# 9. Fetch de Pull Requests de GitHub
# ============================================
# Situación: Quieres revisar localmente una PR antes de aprobarla.
git fetch origin pull/123/head:pr-123
# → Crea rama local "pr-123" con el contenido de la PR #123
git checkout pr-123
# → Ahora puedes revisar, ejecutar tests, etc.
```

**Cómo inspeccionar qué llegó tras el fetch:** [🔙](#12-git-fetch---descargando-cambios)

```bash
# ─────────────────────────────────────────────────────────────────
# Ver commits que llegaron del remoto (que tú no tienes en local)
# ─────────────────────────────────────────────────────────────────
git log HEAD..origin/main --oneline
# → Lista commits que tiene origin/main y tu main NO tiene todavía
# → Si no muestra nada, estás al día

# Con más detalle:
git log HEAD..origin/main --oneline --stat
# → Muestra también qué archivos cambiaron en cada commit


# ─────────────────────────────────────────────────────────────────
# Ver la diferencia de código (qué cambió exactamente)
# ─────────────────────────────────────────────────────────────────
git diff HEAD origin/main
# → Muestra qué código cambió entre tu main y el remoto

git diff HEAD origin/main --name-only
# → Solo los nombres de los archivos que cambiaron


# ─────────────────────────────────────────────────────────────────
# Vista general de la situación
# ─────────────────────────────────────────────────────────────────
git status
# → Si dice "Your branch is behind 'origin/main' by 3 commits"
# → Significa que hay 3 commits en remoto que tú no tienes integrados

git log --oneline --graph --all --decorate -15
# → Vista visual del grafo con todas las ramas
```

**Casos de uso reales:** [🔙](#12-git-fetch---descargando-cambios)

```bash
# ─────────────────────────────────────────────────────────────────
# CASO 1: Rutina diaria de sincronización
# ─────────────────────────────────────────────────────────────────
# Cada mañana antes de empezar a trabajar:
git fetch origin
git status
# → Ves si hay cambios que necesitas integrar
git log HEAD..origin/main --oneline
# → Ves exactamente qué commits llegaron
git merge origin/main
# → Integras los cambios cuando estés listo


# ─────────────────────────────────────────────────────────────────
# CASO 2: Revisar los cambios de un compañero antes de integrar
# ─────────────────────────────────────────────────────────────────
# Tu compañero pusheó su feature. Quieres revisarla antes de mergear.
git fetch origin
# → Se actualiza origin/feature/login
git log origin/main..origin/feature/login --oneline
# → Ves los commits de su feature
git diff origin/main origin/feature/login
# → Ves exactamente qué código cambió
# Cuando todo ok:
git merge origin/feature/login


# ─────────────────────────────────────────────────────────────────
# CASO 3: Revisar una PR localmente (GitHub)
# ─────────────────────────────────────────────────────────────────
# PR #42 está esperando revisión. Quieres ejecutarla en tu máquina.
git fetch origin pull/42/head:pr-42
git checkout pr-42
npm test                # Ejecutas los tests
# Si todo OK, apruebas la PR en GitHub


# ─────────────────────────────────────────────────────────────────
# CASO 4: Limpiar referencias obsoletas regularmente
# ─────────────────────────────────────────────────────────────────
# El equipo hace merge y borra ramas frecuentemente.
# Tu "git branch -r" muestra muchas ramas que ya no existen.
git fetch --prune
# → Sincroniza Y limpia ramas remotas obsoletas en un paso
```

**Fetch vs Pull:** [🔙](#12-git-fetch---descargando-cambios)

```bash
# ─────────────────────────────────────────────────────────────────
# FETCH: control total, tú decides cuándo y cómo integrar
# ─────────────────────────────────────────────────────────────────
git fetch origin main
# Estado: origin/main actualizado, main local SIN cambios
git log HEAD..origin/main --oneline  # Revisas qué llegó
git diff origin/main                  # Revisas el código
git merge origin/main                 # Integras cuando estés listo
# ✓ Nunca te sorprende
# ✓ Puedes revisar antes de integrar
# ✓ No rompe tu trabajo actual


# ─────────────────────────────────────────────────────────────────
# PULL: más rápido, menos control
# ─────────────────────────────────────────────────────────────────
git pull origin main
# Estado: origin/main actualizado Y main local ya tiene los cambios
# ✓ Rápido, un solo comando
# ✗ Integra inmediatamente sin que puedas revisar
# ✗ Puede crear merge commit inesperado
# ✗ Si hay conflictos, te los encuentra en el momento

# ¿Cuándo usar cada uno?
# FETCH: cuando quieres revisar antes de integrar (recomendado)
# PULL:  en ramas simples donde sabes que no hay conflictos
```

**Mejores prácticas:** [🔙](#12-git-fetch---descargando-cambios)

```bash
✓ Usa fetch al inicio de la jornada para ver qué llegó
✓ Usa --prune regularmente para limpiar refs obsoletas
✓ Revisa con git log HEAD..origin/main antes de hacer merge
✓ En repos con muchos remotos, usa --all para actualizar todos

✗ No confundas "fetch" con "pull": fetch NO modifica tu código
✗ No hagas merge ciegamente después del fetch, revisa primero
✗ No asumas que porque no tienes conflictos locales no los habrá al mergear
```

---

## Navegación

- [⬅️ Anterior: git remote](11-git-remote.md)
- [🏠 Volver al Índice](../../GIT_COMANDOS_GUIA_PRACTICA.md)
- [➡️ Siguiente: git pull](13-git-pull.md)
