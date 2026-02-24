# 23. Variables de Shell en Git - Nombres Dinámicos

[🏠 Volver al Índice](../../GIT_COMANDOS_GUIA_PRACTICA.md)

---

## ¿Qué son?

Las variables de shell (como `$(date)`, `$(whoami)`, etc.) permiten crear **nombres dinámicos** para ramas, tags, mensajes de commit y otros elementos de Git.

## ¿Por qué usarlas?

- Crear backups únicos con timestamps automáticos
- Identificar quién creó una rama en repos compartidos
- Generar nombres de ramas consistentes para sprints/releases
- Evitar conflictos de nombres con fechas/horas únicas
- Automatizar flujos de trabajo con scripts

## Variables de Fecha - $(date)

### Formatos más comunes

```bash
# Formato          Resultado         Uso recomendado
# %Y%m%d           20260216          Backups diarios, simple
# %Y-%m-%d         2026-02-16        Formato legible ISO 8601
# %Y%m%d-%H%M%S    20260216-153045   Timestamp completo (único)
# %Y.%m.%d         2026.02.16        Versiones/releases
```

### Ejemplos prácticos

```bash
# Backup con fecha
git branch backup/main-$(date +%Y%m%d)
# → backup/main-20260216

# Backup con hora (más precisión)
git branch backup-before-rebase-$(date +%Y%m%d-%H%M%S)
# → backup-before-rebase-20260216-153045

# Release con fecha
git branch release/$(date +%Y.%m.%d)
# → release/2026.02.16

# Tag con timestamp
git tag "checkpoint-$(date +%Y%m%d-%H%M)"
# → checkpoint-20260216-1530
```

## Variables de Usuario

```bash
# Usuario actual
git branch dev/$(whoami)/nueva-feature
# → dev/vitalyn/nueva-feature

# Usuario + fecha
git branch experiment/$(whoami)-$(date +%Y%m%d)
# → experiment/vitalyn-20260216

# Hostname (identificar máquina)
git branch test/$(hostname)/$(date +%Y%m%d)
# → test/laptop-dev/20260216
```

## Variables de Git

```bash
# Hash del commit actual
git branch backup/$(git rev-parse --short HEAD)-$(date +%Y%m%d)
# → backup/abc123f-20260216

# Tag más cercano
git branch snapshot/$(git describe --tags 2>/dev/null || echo "no-tag")-$(date +%Y%m%d)
# → snapshot/v1.2.3-20260216

# Rama actual
git branch backup/$(git rev-parse --abbrev-ref HEAD)-$(date +%Y%m%d)
# → backup/main-20260216
```

## Casos de Uso Comunes

### Backups antes de operaciones peligrosas

```bash
# Antes de rebase
git branch backup-before-rebase-$(date +%Y%m%d-%H%M%S)
git rebase -i HEAD~5

# Antes de reset hard
git branch backup-before-reset-$(date +%Y%m%d-%H%M)
git reset --hard HEAD~3

# Antes de merge complicado
git branch backup-before-merge-$(date +%Y%m%d)
git merge --no-ff feature/complex
```

### Ramas temporales y experimentales

```bash
# Test temporal único
git branch test/$(whoami)-$(date +%s)
# → test/vitalyn-1708098000

# Experimento con contexto
git branch experiment/$(date +%b%d)/$(whoami)/feature-x
# → experiment/Feb16/vitalyn/feature-x

# POC temporal
git branch poc/$(date +%Y%W)-$(whoami)
# → poc/202607-vitalyn
```

## Mejores Prácticas

### ✓ Recomendado

```bash
✓ Usa formatos consistentes en tu equipo
✓ Añade prefijos claros (backup/, temp/, experiment/)
✓ Usa timestamps completos para operaciones críticas
✓ Documenta en commit el propósito del backup
```

### ✗ Evitar

```bash
✗ No uses solo fecha como identificador (sé descriptivo)
✗ No acumules backups sin límite (limpia regularmente)
✗ No uses variables de shell en ramas permanentes (feature, bugfix)
✗ No uses espacios o caracteres especiales en nombres
```

## Limpieza de Backups

```bash
# Listar backups con fecha
git branch --list "backup/*"

# Eliminar backups de un mes específico
git branch -D $(git branch --list "backup/*-202601*")

# Eliminar backups antiguos (más de 7 días)
for branch in $(git branch --list "backup/*" --format="%(refname:short)"); do
  date_part=$(echo $branch | grep -oP '\d{8}' | head -1)
  if [[ -n "$date_part" ]]; then
    branch_date=$(date -d "$date_part" +%s 2>/dev/null || echo 0)
    week_ago=$(date -d "7 days ago" +%s)
    if [[ $branch_date -lt $week_ago ]]; then
      git branch -D "$branch"
    fi
  fi
done
```

## Referencias

Para más información sobre formatos de fecha y ejemplos avanzados, consulta:
- `man date` en tu terminal
- [Sección 22: Referencias y Placeholders](22-referencias-placeholders.md)

---

## Navegación

- [⬅️ Anterior: Referencias y Placeholders](22-referencias-placeholders.md)
- [🏠 Volver al Índice](../../GIT_COMANDOS_GUIA_PRACTICA.md)
