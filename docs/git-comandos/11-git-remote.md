# 11. git remote - Gestionando Repositorios Remotos

[🏠 Volver al Índice](../../GIT_COMANDOS_GUIA_PRACTICA.md)

---

## 11. git remote - Gestionando Repositorios Remotos
[⬆️ Top](#11-git-remote---gestionando-repositorios-remotos)

**¿Qué hace?**
Gestiona las conexiones a repositorios externos (en GitHub, GitLab, Bitbucket, servidores propios, etc.). Un "remote" es simplemente un nombre que guarda una URL, para no tener que escribirla en cada push/pull.

**Funcionamiento interno:** [🔙](#11-git-remote---gestionando-repositorios-remotos)

```
Los remotos se guardan en el archivo .git/config:

[remote "origin"]
    url = https://github.com/usuario/repo.git
    fetch = +refs/heads/*:refs/remotes/origin/*

La línea "fetch" es una "refspec" que significa:
  "Trae TODAS las ramas del remoto (refs/heads/*)"
  "y guárdalas localmente como refs/remotes/origin/*"

Por ejemplo, la rama "main" del remoto:
  - En el remoto:             refs/heads/main
  - Copia local del remoto:   refs/remotes/origin/main  (= origin/main)
  - Tu rama local:            refs/heads/main            (= main)

El prefijo "+" en la refspec significa "actualiza aunque no sea fast-forward"

Puedes tener múltiples remotos:
  origin    → tu fork o tu repo principal
  upstream  → el repo original (en proyectos open source)
  backup    → un servidor de respaldo
  staging   → servidor de staging para deploy

Cada rama local puede estar asociada a una rama de tracking remota:
  git branch -vv
  → main    abc1234 [origin/main] último mensaje de commit
```

**Todas las opciones importantes:** [🔙](#11-git-remote---gestionando-repositorios-remotos)

```bash
# ============================================
# 1. Listar remotos
# ============================================
# Ver solo los nombres:
git remote

# Ver nombres + URLs (fetch y push pueden ser distintas):
git remote -v
# origin  https://github.com/usuario/repo.git (fetch)
# origin  https://github.com/usuario/repo.git (push)

# Ver información detallada de un remoto:
git remote show origin
# → Muestra: URL, ramas remotas, qué rama local hace tracking de cuál
# → Muestra si tu rama local está "ahead" o "behind" del remoto


# ============================================
# 2. Añadir remotos
# ============================================
# Situación: Acabas de hacer fork de un proyecto open source.
# Tienes "origin" (tu fork), pero necesitas "upstream" (el original).
git remote add upstream https://github.com/autor-original/proyecto.git

# Para deploy directo a un servidor:
git remote add production git@tu-servidor.com:/var/repos/proyecto.git
git remote add staging git@staging.tu-empresa.com:/var/repos/proyecto.git


# ============================================
# 3. Cambiar la URL de un remoto
# ============================================
# Situación: El repo se migró de HTTP a SSH, o cambió de plataforma.
git remote set-url origin git@github.com:usuario/repo.git

# Cambiar a la nueva URL después de migrar de GitHub a GitLab:
git remote set-url origin https://gitlab.com/usuario/repo.git

# Separar URLs de fetch y push (push a espejo, fetch del original):
git remote set-url --push origin https://gitlab.com/usuario/repo.git
# → Hace fetch de GitHub pero push a GitLab


# ============================================
# 4. Push a múltiples remotos simultáneamente
# ============================================
# Situación: Quieres mantener mirrors en GitHub y GitLab a la vez.
git remote set-url --add --push origin https://github.com/usuario/repo.git
git remote set-url --add --push origin https://gitlab.com/usuario/repo.git
# → Al hacer "git push", sube a AMBAS URLs


# ============================================
# 5. Renombrar y eliminar remotos
# ============================================
# Renombrar:
git remote rename origin github
git remote rename upstream original

# Eliminar:
git remote remove upstream
git remote remove backup


# ============================================
# 6. Limpiar referencias obsoletas
# ============================================
# Situación: Un compañero borró la rama "feature/login" en el remoto.
# Pero en tu local sigue apareciendo "origin/feature/login".
# Para limpiar esas referencias obsoletas:
git remote prune origin

# Ver qué se borraría SIN borrarlo todavía:
git remote prune origin --dry-run

# Alternativa (hace fetch + prune en un paso):
git fetch --prune

# ============================================
# 7. Actualizar todos los remotos
# ============================================
git remote update
# → Hace fetch de TODOS los remotos configurados (origin, upstream, etc.)
# → Equivalente a "git fetch --all" pero más explícito
```

**Casos de uso reales:** [🔙](#11-git-remote---gestionando-repositorios-remotos)

```bash
# ─────────────────────────────────────────────────────────────────
# CASO 1: Fork workflow completo (contribuir a open source)
# ─────────────────────────────────────────────────────────────────
# Objetivo: contribuir a "react" manteniendo tu fork sincronizado

# Paso 1: (En GitHub) Fork del repo original
# Paso 2: Clonar tu fork
git clone https://github.com/TU-USUARIO/react.git
cd react

# Paso 3: Añadir el original como upstream
git remote add upstream https://github.com/facebook/react.git

# Paso 4: Verificar
git remote -v
# origin   https://github.com/TU-USUARIO/react.git (fetch)
# origin   https://github.com/TU-USUARIO/react.git (push)
# upstream https://github.com/facebook/react.git (fetch)
# upstream https://github.com/facebook/react.git (push)

# Paso 5: Sincronizar tu fork con los cambios del original:
git fetch upstream
git checkout main
git merge upstream/main   # Actualiza tu main con el upstream
git push origin main       # Sube la actualización a tu fork


# ─────────────────────────────────────────────────────────────────
# CASO 2: Migrar repo de GitHub a GitLab
# ─────────────────────────────────────────────────────────────────
# Paso 1: Crear el repo vacío en GitLab
# Paso 2: Cambiar la URL del remoto
git remote set-url origin https://gitlab.com/empresa/proyecto.git

# Paso 3: Subir todo (ramas y tags)
git push origin --all         # Todas las ramas
git push origin --tags        # Todos los tags

# Paso 4: Verificar que todo llegó
git remote show origin


# ─────────────────────────────────────────────────────────────────
# CASO 3: Deploy con git push a servidor
# ─────────────────────────────────────────────────────────────────
# Configurar el remoto del servidor de producción:
git remote add production ssh://deploy@mi-servidor.com:/var/www/proyecto

# Desplegar:
git push production main
# → El servidor tiene un hook "post-receive" que ejecuta el deploy


# ─────────────────────────────────────────────────────────────────
# CASO 4: Push simultáneo a GitHub y GitLab como backup
# ─────────────────────────────────────────────────────────────────
git remote set-url --add --push origin https://github.com/usuario/repo.git
git remote set-url --add --push origin https://gitlab.com/usuario/repo.git

# Ahora un solo push actualiza ambas plataformas:
git push
# → Sube a GitHub Y GitLab automáticamente
```

**Troubleshooting:** [🔙](#11-git-remote---gestionando-repositorios-remotos)

```bash
# ─────────────────────────────────────────────────────────────────
# Problema 1: "remote origin already exists"
# ─────────────────────────────────────────────────────────────────
# Error al hacer "git remote add origin ..." cuando ya existe
# Solución: cambiar la URL en vez de añadir:
git remote set-url origin https://github.com/usuario/nuevo-repo.git
# ó eliminar y volver a añadir:
git remote remove origin
git remote add origin https://github.com/usuario/nuevo-repo.git


# ─────────────────────────────────────────────────────────────────
# Problema 2: Ramas remotas obsoletas que siguen apareciendo
# ─────────────────────────────────────────────────────────────────
# Un compañero borró ramas en el remoto pero en tu local siguen apareciendo
git remote prune origin
# ó, la próxima vez que hagas fetch:
git fetch --prune


# ─────────────────────────────────────────────────────────────────
# Problema 3: Ver qué cambió en el remoto sin integrar los cambios
# ─────────────────────────────────────────────────────────────────
git fetch origin
git log HEAD..origin/main --oneline   # Commits que tienen en remoto y tú no
git diff HEAD origin/main             # Diferencia de código
```

**Mejores prácticas:** [🔙](#11-git-remote---gestionando-repositorios-remotos)

```bash
✓ Usa nombres descriptivos: "origin" para tu repo, "upstream" para el original del fork
✓ Configura "upstream" en todos los forks para facilitar sincronización
✓ Usa --prune regularmente para no acumular refs obsoletas
✓ Para repos importantes, configura push a múltiples remotos como backup

✗ No pongas contraseñas o tokens en la URL del remoto que vayas a compartir
✗ No uses nombres confusos (ej: "origin2", "origin_old")
✗ No borres "origin" si es el único remoto sin tener otro configurado
```

---

## Navegación

- [⬅️ Anterior: git clone](10-git-clone.md)
- [🏠 Volver al Índice](../../GIT_COMANDOS_GUIA_PRACTICA.md)
- [➡️ Siguiente: git fetch](12-git-fetch.md)
