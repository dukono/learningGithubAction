# 14. git push - Subiendo Cambios

[🏠 Volver al Índice](../../GIT_COMANDOS_GUIA_PRACTICA.md)

---

## 14. git push - Subiendo Cambios
[⬆️ Top](#14-git-push---subiendo-cambios)

**¿Qué hace?**
Envía los commits de tu rama local al repositorio remoto. Es la forma de compartir tu trabajo con el equipo o de guardar una copia en la nube.

**Funcionamiento interno:** [🔙](#14-git-push---subiendo-cambios)

```
ANTES del push:
  Tu local:   main → A---B---C  (tienes 2 commits nuevos: B y C)
  Remoto:     main → A          (solo tiene A)

DURANTE el push, Git:
  1. Conecta con el remoto
  2. Compara tu rama con la del remoto
  3. Verifica que el push es "fast-forward"
     (el remoto tiene A, tú tienes A→B→C, es una extensión directa)
  4. Empaqueta los objetos que el remoto no tiene (B y C)
  5. Envía los objetos comprimidos
  6. Actualiza la ref en el remoto: main → C

DESPUÉS del push:
  Tu local:   main → A---B---C
  Remoto:     main → A---B---C  (sincronizado)

¿Cuándo Git RECHAZA el push?
  Si el remoto tiene commits que tú no tienes localmente,
  el push NO es fast-forward y Git lo rechaza:

  Tu local:   main → A---B---C  (tu trabajo)
  Remoto:     main → A---D      (tu compañero pusheó D)

  Error: "rejected - non-fast-forward"
  Solución: Primero "git pull" para integrar D, luego push otra vez
```

**Todas las opciones importantes:** [🔙](#14-git-push---subiendo-cambios)

```bash
# ============================================
# 1. Push básico
# ============================================
# Situación: Ya tienes tracking configurado (lo más habitual).
# Simplemente subes los cambios de tu rama actual.
git push


# ============================================
# 2. Push especificando remoto y rama
# ============================================
# Situación: Quieres ser explícito, o estás pusheando a una rama
# diferente a la actual.
git push origin main
git push origin develop


# ============================================
# 3. -u / --set-upstream: primer push + configurar tracking
# ============================================
# Situación: Creaste una rama local nueva y quieres subirla
# al remoto por primera vez. Sin -u, Git no sabe a qué rama
# remota está conectada tu rama local.
git push -u origin feature/nuevo-checkout
# → Sube la rama al remoto
# → Configura que "feature/nuevo-checkout" hace tracking de "origin/feature/nuevo-checkout"
# → A partir de ahora, un simple "git push" funciona desde esa rama

# Equivalente manual sin -u:
git push origin feature/nuevo-checkout
git branch --set-upstream-to=origin/feature/nuevo-checkout


# ============================================
# 4. --force-with-lease: push forzado seguro
# ============================================
# Situación: Hiciste rebase en local y necesitas sobrescribir
# el historial en el remoto. Pero quieres asegurarte de que
# nadie más pusheó algo que perderías.
git push --force-with-lease
# → Solo fuerza el push si el remoto está exactamente donde tú lo dejaste
# → Si alguien más pusheó mientras tanto, FALLA con error (protegiéndote)
# → SIEMPRE usa esto en lugar de --force cuando necesites forzar

# --force a secas (PELIGROSO, nunca usar en ramas compartidas):
git push --force
# → Sobrescribe sin importar lo que haya en el remoto
# → Puede borrar commits de compañeros


# ============================================
# 5. --delete: eliminar rama remota
# ============================================
# Situación: La feature/login fue mergeada y ya no se necesita
# en el remoto.
git push origin --delete feature/login
# ó sintaxis alternativa:
git push origin :feature/login


# ============================================
# 6. Push de tags
# ============================================
# Los tags locales NO se suben automáticamente con git push.
# Hay que subirlos explícitamente:

# Subir un tag específico:
git push origin v2.0.0

# Subir TODOS los tags que no están en el remoto:
git push --tags

# Subir tags junto con commits (solo tags que apuntan a commits pusheados):
git push --follow-tags


# ============================================
# 7. --dry-run: simular el push sin hacerlo
# ============================================
# Situación: Quieres verificar qué se subiría sin subir nada todavía.
git push --dry-run
git push --dry-run origin feature/nueva-funcionalidad
# → Muestra qué objetos se enviarían, pero NO hace nada


# ============================================
# 8. --all: subir todas las ramas locales
# ============================================
# Situación: Tienes varias ramas locales y quieres subirlas todas.
git push --all origin
# → Sube TODAS las ramas locales (que no estén ya en el remoto)


# ============================================
# 9. push a rama remota con nombre diferente
# ============================================
# Situación: Quieres subir tu rama local "hotfix" a una rama
# remota que se llame "hotfix/v2.1.1"
git push origin hotfix:hotfix/v2.1.1
# Sintaxis: git push <remoto> <rama-local>:<rama-remota>


# ============================================
# 10. -o / --push-option: enviar opciones al servidor
# ============================================
# Situación: Quieres que el push no dispare el pipeline de CI.
# (Requiere soporte del servidor, ej: GitLab)
git push -o ci.skip
git push -o skip-ci
```

**Casos de uso reales:** [🔙](#14-git-push---subiendo-cambios)

```bash
# ─────────────────────────────────────────────────────────────────
# CASO 1: Flujo diario típico
# ─────────────────────────────────────────────────────────────────
# Hiciste cambios en tu feature branch. Al final del día, los subes:
git add .
git commit -m "feat: add user profile form"
git push
# Si es la primera vez:
git push -u origin feature/user-profile


# ─────────────────────────────────────────────────────────────────
# CASO 2: Después de hacer rebase (historial reescrito)
# ─────────────────────────────────────────────────────────────────
# Tenías pusheado feature/payment. Luego hiciste rebase -i.
# Los commits tienen nuevos hashes. Necesitas forzar:
git rebase -i HEAD~3       # Limpiaste 3 commits
git push --force-with-lease
# → Si alguien pusheó algo entre medias, fallará y te avisará
# → Si todo está bien, sobrescribe el historial limpio


# ─────────────────────────────────────────────────────────────────
# CASO 3: Release con tag
# ─────────────────────────────────────────────────────────────────
# Preparas la versión 3.0.0:
git checkout main
git merge feature/big-feature
git tag -a v3.0.0 -m "Release versión 3.0.0"

# Subir el commit Y el tag:
git push origin main
git push origin v3.0.0
# ó en un solo comando (sube tags que apuntan a commits pusheados):
git push --follow-tags origin main


# ─────────────────────────────────────────────────────────────────
# CASO 4: Push rechazado - alguien pusheó antes que tú
# ─────────────────────────────────────────────────────────────────
# Error: "rejected - non-fast-forward"
# Solución 1: Merge (historia con merge commit)
git pull origin main        # Descarga y mergea
git push origin main        # Ahora sí funciona

# Solución 2: Rebase (historia lineal más limpia)
git pull --rebase origin main  # Descarga y reaplica tus commits encima
git push origin main


# ─────────────────────────────────────────────────────────────────
# CASO 5: Limpiar ramas remotas obsoletas
# ─────────────────────────────────────────────────────────────────
# Tras merge de la PR, borras la rama remota:
git push origin --delete feature/completada
# También la local:
git branch -d feature/completada
```

**Configuración de push.default:** [🔙](#14-git-push---subiendo-cambios)

```bash
# "push.default" controla qué pasa cuando haces "git push" sin argumentos:
#
# simple (recomendado, es el default desde Git 2.0):
# → Solo sube la rama actual a su tracking branch configurada
# → Falla si el nombre local y remoto no coinciden
git config --global push.default simple

# current:
# → Sube la rama actual a una rama con el mismo nombre en el remoto
# → Útil cuando no tienes tracking configurado
git config --global push.default current

# upstream (ó tracking):
# → Sube a la rama de upstream configurada (aunque tenga nombre diferente)
git config --global push.default upstream

# matching (comportamiento antiguo, NO recomendado):
# → Sube TODAS las ramas que tienen nombre coincidente en el remoto
git config --global push.default matching
```

**⚠️ Force push - cuándo y cómo:** [🔙](#14-git-push---subiendo-cambios)

```bash
# ─────────────────────────────────────────────────────────────────
# Cuándo SÍ está bien hacer force push:
# ─────────────────────────────────────────────────────────────────
# ✓ Tu feature branch personal (nadie más trabaja en ella)
# ✓ Después de hacer rebase local de tu propia rama
# ✓ Para corregir commits antes de que la PR sea mergeada
# ✓ Para corregir un mensaje de commit que ya pusheaste

# ─────────────────────────────────────────────────────────────────
# Cuándo NO hacer force push NUNCA:
# ─────────────────────────────────────────────────────────────────
# ✗ main, develop, master, release/* → ramas compartidas
# ✗ Cualquier rama donde otros compañeros estén trabajando
# ✗ Ramas que tienen PRs abiertas con muchos comentarios

# ─────────────────────────────────────────────────────────────────
# SIEMPRE usa --force-with-lease, nunca --force a secas:
# ─────────────────────────────────────────────────────────────────
git push --force-with-lease    # ✓ Seguro
git push --force               # ✗ Peligroso
```

**Troubleshooting:** [🔙](#14-git-push---subiendo-cambios)

```bash
# ─────────────────────────────────────────────────────────────────
# Error: "The current branch X has no upstream branch"
# ─────────────────────────────────────────────────────────────────
# Causa: La rama local no tiene tracking configurado (es nueva)
git push -u origin nombre-de-tu-rama
# ó para que Git lo haga automáticamente siempre:
git config --global push.autoSetupRemote true


# ─────────────────────────────────────────────────────────────────
# Error: "rejected - non-fast-forward"
# ─────────────────────────────────────────────────────────────────
# Causa: El remoto tiene commits que tú no tienes (alguien pusheó antes)
git pull --rebase    # Trae los cambios y reaplica los tuyos encima
git push            # Ahora sí funciona


# ─────────────────────────────────────────────────────────────────
# Error: "pre-push hook failed"
# ─────────────────────────────────────────────────────────────────
# Causa: El repo tiene un hook que hace verificaciones antes del push
# (tests, linting, etc.) y algo falló
# Ver el error específico del hook y corregirlo
# ó si sabes lo que haces y quieres saltar el hook:
git push --no-verify    # ⚠️ Úsalo con cuidado


# ─────────────────────────────────────────────────────────────────
# Error: "Permission denied" o autenticación fallida
# ─────────────────────────────────────────────────────────────────
# Causa: Credenciales caducadas o no configuradas
# Para HTTPS: configura un token
git config credential.helper store
# Para SSH: verifica tu clave
ssh -T git@github.com
```

**Mejores prácticas:** [🔙](#14-git-push---subiendo-cambios)

```bash
✓ Haz push frecuentemente: es tu backup en la nube
✓ Usa -u la primera vez que subes una rama nueva
✓ Usa --force-with-lease en vez de --force cuando necesites forzar
✓ Verifica con --dry-run antes de operaciones de push importantes
✓ Sube tags de release explícitamente con git push --follow-tags

✗ NUNCA uses --force en ramas compartidas (main, develop)
✗ No pushees secretos, contraseñas, tokens ni claves privadas
✗ No pushees archivos binarios grandes sin Git LFS configurado
✗ No ignores errores de push - tienen un motivo
```

---

## Navegación

- [⬅️ Anterior: git pull](13-git-pull.md)
- [🏠 Volver al Índice](../../GIT_COMANDOS_GUIA_PRACTICA.md)
- [➡️ Siguiente: git reset](15-git-reset.md)
