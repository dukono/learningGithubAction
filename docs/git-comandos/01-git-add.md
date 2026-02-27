# 1. git add - Preparando Cambios

[🏠 Volver al Índice](../../GIT_COMANDOS_GUIA_PRACTICA.md)

---

## 1. git add - Preparando Cambios
[⬆️ Top](#1-git-add---preparando-cambios)

**¿Qué hace?**
Prepara cambios del working directory para el próximo commit, moviéndolos al staging area (index). Es el paso intermedio entre modificar archivos y crear un commit: te permite elegir exactamente qué cambios quieres incluir.

**Funcionamiento interno:** [🔙](#1-git-add---preparando-cambios)

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

**Uso práctico y opciones:** [🔙](#1-git-add---preparando-cambios)

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

**Caso de uso real: Commits atómicos con -p:** [🔙](#1-git-add---preparando-cambios)

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

**Opciones avanzadas de add -p:** [🔙](#1-git-add---preparando-cambios)

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

**Patrones de uso comunes:** [🔙](#1-git-add---preparando-cambios)

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

**Ver qué está stageado:** [🔙](#1-git-add---preparando-cambios)

```bash
# Ver estado general (qué está staged y qué no):
git status

# Ver diferencias stageadas (lo que VA a entrar en el commit):
git diff --staged
# ó equivalente:
git diff --cached
# → Muestra QUÉ cambios están en staging

# Ver diferencias NO stageadas (lo que tienes modificado sin añadir):
git diff
# → Muestra cambios en working directory que NO están en staging
```

**Casos de uso reales:** [🔙](#1-git-add---preparando-cambios)

```bash
# ─────────────────────────────────────────────────────────────────
# CASO 1: Añadir todo antes de un commit (lo más común)
# ─────────────────────────────────────────────────────────────────
# Modificaste varios archivos de una misma feature y quieres commitearlos todos.
git status                    # Ver qué cambió
git diff                      # Revisar los cambios
git add .                     # Añadir todo
git diff --staged             # Confirmar qué va en el commit
git commit -m "feat: add product search"


# ─────────────────────────────────────────────────────────────────
# CASO 2: Separar en 2 commits cambios que están en el mismo archivo
# ─────────────────────────────────────────────────────────────────
# Modificaste un archivo con dos cambios independientes.
# Quieres que cada uno quede en un commit separado.
git add -p src/products.js    # Modo interactivo por hunks
# Git muestra el primer cambio:
# → y (yes): stagear este hunk y pasar al siguiente
# → n (no): no stagear, pasar al siguiente
# → s (split): intentar dividir el hunk en partes más pequeñas
# Aceptas solo los hunks del primer cambio
git commit -m "feat: add search by category"

git add src/products.js       # El resto del archivo ya modificado
git commit -m "fix: correct pagination calculation"


# ─────────────────────────────────────────────────────────────────
# CASO 3: Añadir solo archivos ya conocidos (ignorar los nuevos)
# ─────────────────────────────────────────────────────────────────
# Trabajas en código existente, tienes archivos nuevos temporales
# que no quieres incluir todavía.
git add -u
# → Solo actualiza archivos que ya Git rastreaba
# → NO añade los nuevos archivos (untracked)


# ─────────────────────────────────────────────────────────────────
# CASO 4: Ver qué añadiría git add . SIN hacerlo
# ─────────────────────────────────────────────────────────────────
git add -n .
# → Muestra los archivos que se añadirían, sin modificar el staging
# → Útil para verificar que .gitignore está funcionando bien


# ─────────────────────────────────────────────────────────────────
# CASO 5: Deshacer un git add (sacar del staging)
# ─────────────────────────────────────────────────────────────────
# Hiciste "git add ." y añadiste un archivo que no querías.
git restore --staged archivo-no-deseado.txt
# ó forma más antigua:
git reset HEAD archivo-no-deseado.txt
# → El archivo vuelve al working directory, sin staging
# → El contenido del archivo NO cambia
```

**Mejores prácticas:** [🔙](#1-git-add---preparando-cambios)

```bash
✓ Usa git add -p para commits granulares y atómicos
✓ Revisa con git diff --staged antes de commit (evita sorpresas)
✓ Usa git add -n para verificar qué incluirías antes de stagear
✓ Usa .gitignore para que ciertos archivos nunca se puedan añadir
✓ Considera git add -u cuando solo actualizas archivos ya existentes

✗ No hagas git add . ciegamente sin revisar git status primero
✗ No stagees archivos generados (dist/, node_modules/, *.pyc, *.class)
✗ No stagees archivos de configuración local (.env, .idea/, .vscode/)
✗ No uses git add -f (forzar ignorados) sin una razón muy clara
```


---

## Navegación

- [🏠 Volver al Índice](../../GIT_COMANDOS_GUIA_PRACTICA.md)
- [➡️ Siguiente: git commit](02-git-commit.md)

