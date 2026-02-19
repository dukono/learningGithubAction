# 2. git commit - Guardando la Historia

[🏠 Volver al Índice](../../GIT_COMANDOS_GUIA_PRACTICA.md)

---

## 2. git commit - Guardando la Historia
[⬆️ Top](#2-git-commit---guardando-la-historia)

**¿Qué hace?**
Crea un snapshot inmutable del proyecto con los cambios del staging area. Cada commit es un punto en la historia del proyecto al que siempre puedes volver.

**Funcionamiento interno:** [🔙](#2-git-commit---guardando-la-historia)

```
1. Crea tree object del staging
2. Crea commit object con tree + parent + metadata
3. Actualiza referencia de rama
4. Actualiza reflog
```

**Uso práctico:** [🔙](#2-git-commit---guardando-la-historia)

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

**Casos de uso del --amend:** [🔙](#2-git-commit---guardando-la-historia)

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

**Opciones de formato de mensaje:** [🔙](#2-git-commit---guardando-la-historia)

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

**Commits interactivos:** [🔙](#2-git-commit---guardando-la-historia)

```bash
# Commit interactivo (elige qué añadir)
git commit -p
# → Similar a git add -p + commit
# → Selecciona hunks a commitear
```

**Mensajes de commit efectivos (Conventional Commits):** [🔙](#2-git-commit---guardando-la-historia)

```bash
# ─────────────────────────────────────────────────────────────────
# ¿Por qué importa un buen mensaje de commit?
# ─────────────────────────────────────────────────────────────────
# En 6 meses, cuando algo falle en producción y hagas "git log",
# el mensaje "fix" no te dice nada.
# El mensaje "fix: corregir null pointer en checkout cuando el carrito está vacío"
# te dice exactamente qué se hizo y por qué.
#
# Conventional Commits es un estándar muy extendido:
# → Permite generar CHANGELOGs automáticamente
# → Facilita decidir el número de versión (semver)
# → Hace el historial legible por herramientas y personas

# TIPOS PRINCIPALES:
feat:     # Nueva funcionalidad para el usuario
fix:      # Corrección de un bug
docs:     # Cambios en documentación
style:    # Formato, punto y coma... (sin cambios de lógica)
refactor: # Cambio de código que no añade feature ni corrige bug
test:     # Añadir o corregir tests
chore:    # Tareas de mantenimiento (build, dependencias, CI...)
perf:     # Mejora de rendimiento
ci:       # Cambios en configuración de CI

# FORMATO BÁSICO:
feat: Add user authentication
fix: Fix login validation bug
docs: Update README with new API endpoints

# CON SCOPE (qué módulo/área se ve afectado):
feat(auth): Add JWT token refresh
fix(api): Handle timeout errors in payment service
docs(readme): Add installation instructions for Windows

# FORMATO COMPLETO (título + cuerpo + footer):
feat(api): Add user registration endpoint

Implements POST /api/v1/register accepting email and password.
Validates email format and password strength.
Stores hashed password using bcrypt (cost factor 12).

Closes #123
Co-authored-by: Ana García <ana@empresa.com>

# BREAKING CHANGE (cambio incompatible):
feat(api)!: Change authentication to use OAuth2

BREAKING CHANGE: The /api/auth endpoint now requires OAuth2 tokens.
Basic auth credentials are no longer accepted.
Migration guide: docs/migration-to-oauth2.md
```

**Casos de uso reales:** [🔙](#2-git-commit---guardando-la-historia)

```bash
# ─────────────────────────────────────────────────────────────────
# CASO 1: Flujo de commit diario típico
# ─────────────────────────────────────────────────────────────────
# Pasaste la mañana implementando el formulario de registro.
git status                          # Ver qué modificaste
git diff                            # Revisar los cambios en detalle
git add src/components/RegisterForm.js
git add src/api/register.js
git diff --staged                   # Confirmar qué entra en el commit
git commit -m "feat(auth): add user registration form with email validation"


# ─────────────────────────────────────────────────────────────────
# CASO 2: Corregir el último commit antes de hacer push
# ─────────────────────────────────────────────────────────────────
# Acabas de hacer commit y te das cuenta de que olvidaste un archivo.
git add archivo-olvidado.js
git commit --amend --no-edit        # Añade el archivo sin cambiar mensaje

# O el mensaje tenía una errata:
git commit --amend -m "feat(auth): add user registration form with email validation"

# ⚠️ Solo usar --amend ANTES de hacer push


# ─────────────────────────────────────────────────────────────────
# CASO 3: Commit al final del día con trabajo inacabado
# ─────────────────────────────────────────────────────────────────
# No acabaste la feature pero quieres guardar el progreso.
# Opción A: Commit WIP (lo limpiarás mañana con rebase -i)
git add .
git commit -m "wip: login form half done - missing validation"

# Opción B: Usar stash (no crea commit, más limpio)
git stash push -m "WIP: login form - missing validation"


# ─────────────────────────────────────────────────────────────────
# CASO 4: Commit con múltiples cambios lógicamente separados
# ─────────────────────────────────────────────────────────────────
# Modificaste 5 archivos pero los cambios son de dos features distintas.
# Haces 2 commits separados usando git add -p:

# Commit 1: Solo los cambios de la feature A
git add -p                         # Selecciona los hunks de feature A
git commit -m "feat: add product search by category"

# Commit 2: Solo los cambios de la feature B
git add .                          # Añade el resto
git commit -m "fix: correct pagination calculation on product list"


# ─────────────────────────────────────────────────────────────────
# CASO 5: Commit para pair programming (co-autoría)
# ─────────────────────────────────────────────────────────────────
git commit -m "feat: implement checkout flow

Co-authored-by: María López <maria@empresa.com>"
# → GitHub muestra ambos nombres como autores del commit
```

**Troubleshooting común:** [🔙](#2-git-commit---guardando-la-historia)

```bash
# ─────────────────────────────────────────────────────────────────
# Problema 1: "Nothing to commit, working tree clean"
# ─────────────────────────────────────────────────────────────────
# Causa: No has hecho git add de ningún cambio.
git status       # Ver qué archivos tienes modificados
git add .        # Añadir al staging
git commit -m "Mensaje"


# ─────────────────────────────────────────────────────────────────
# Problema 2: Olvidaste añadir un archivo al commit
# ─────────────────────────────────────────────────────────────────
git add archivo-olvidado.txt
git commit --amend --no-edit    # Añade al último commit sin cambiar mensaje


# ─────────────────────────────────────────────────────────────────
# Problema 3: Mensaje de commit equivocado
# ─────────────────────────────────────────────────────────────────
git commit --amend -m "Mensaje correcto"
# ⚠️ Solo si NO has hecho push todavía


# ─────────────────────────────────────────────────────────────────
# Problema 4: Hice commit en la rama equivocada
# ─────────────────────────────────────────────────────────────────
# El commit está en main pero debería estar en feature/mi-tarea

# Paso 1: Ir a la rama correcta y traer el commit
git checkout feature/mi-tarea
git cherry-pick <hash-del-commit>   # Copia el commit a esta rama

# Paso 2: Eliminar el commit de main
git checkout main
git reset --hard HEAD~1             # Elimina el último commit de main
# ⚠️ Solo si main no estaba pusheado con ese commit


# ─────────────────────────────────────────────────────────────────
# Problema 5: "Please tell me who you are"
# ─────────────────────────────────────────────────────────────────
git config --global user.name "Tu Nombre"
git config --global user.email "tu@email.com"


# ─────────────────────────────────────────────────────────────────
# Problema 6: El editor se abre y no sabes cerrarlo (vim)
# ─────────────────────────────────────────────────────────────────
# En vim: presiona Esc, luego escribe :wq y Enter (guarda y sale)
# En vim: presiona Esc, luego escribe :q! y Enter (sale sin guardar = cancela commit)

# Para cambiar el editor a algo más fácil:
git config --global core.editor "nano"      # nano es más sencillo
git config --global core.editor "code --wait"  # VS Code
```

**Mejores prácticas:** [🔙](#2-git-commit---guardando-la-historia)

```bash
✓ Commits pequeños y atómicos (un commit = un cambio lógico)
✓ Mensajes que explican el POR QUÉ, no solo el QUÉ
✓ Usa Conventional Commits (feat, fix, docs, refactor...)
✓ Usa --amend solo en commits NO pusheados
✓ Revisa con git diff --staged antes de commitear

✗ Evita commits gigantes con 20 archivos mezclados
✗ Evita mensajes vagos ("fix", "update", "cambios", "wip2")
✗ No uses --amend en commits que ya están en ramas compartidas
✗ No commitees archivos de configuración local, .env, node_modules
```

---

## Navegación

- [⬅️ Anterior: git add](01-git-add.md)
- [🏠 Volver al Índice](../../GIT_COMANDOS_GUIA_PRACTICA.md)
- [➡️ Siguiente: git status](03-git-status.md)

