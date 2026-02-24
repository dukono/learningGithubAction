# 10. git clone - Copiando Repositorios

[🏠 Volver al Índice](../../GIT_COMANDOS_GUIA_PRACTICA.md)

---

## 10. git clone - Copiando Repositorios
[⬆️ Top](#10-git-clone---copiando-repositorios)

**¿Qué hace?**
Crea una copia local completa de un repositorio remoto. Es el punto de partida cuando vas a trabajar en un proyecto que ya existe en algún servidor (GitHub, GitLab, Bitbucket, etc.).

**Funcionamiento interno:** [🔙](#10-git-clone---copiando-repositorios)

```
git clone hace internamente estos pasos:
1. Crea un directorio nuevo con el nombre del repositorio
2. git init                          → inicializa repo vacío
3. git remote add origin <url>       → registra la URL como "origin"
4. git fetch origin                  → descarga todos los objetos y refs
5. git checkout <rama-por-defecto>   → hace checkout de main/master

Tras el clone, la estructura de referencias queda así:
  refs/heads/main           → rama local "main" (la que usas)
  refs/remotes/origin/main  → copia del estado remoto de main
  refs/remotes/origin/HEAD  → apunta a la rama por defecto del remoto

Las ramas del remoto (origin/develop, origin/feature-x...) están
disponibles como "remote tracking branches" pero NO como ramas locales.
Para trabajar en una rama remota, debes crearla localmente:
  git checkout develop  → crea la local develop tracking origin/develop
```

**Todas las opciones importantes:** [🔙](#10-git-clone---copiando-repositorios)

```bash
# ============================================
# 1. Clone básico (la forma más común)
# ============================================
# Situación: Vas a incorporarte a un proyecto.
# El tech lead te da la URL del repo. Solo necesitas esto:
git clone https://github.com/empresa/proyecto.git
# → Crea directorio "proyecto/" con todo el código
# → Rama por defecto ya activa y lista para usar


# ============================================
# 2. Clone con nombre personalizado
# ============================================
# Situación: El repo se llama "backend-service-api-v2" pero tú
# quieres que la carpeta local se llame algo más corto.
git clone https://github.com/empresa/backend-service-api-v2.git mi-backend
# → Crea directorio "mi-backend/" en vez de "backend-service-api-v2/"


# ============================================
# 3. --depth: clone superficial (solo últimos N commits)
# ============================================
# Situación: En un pipeline de CI/CD, clonar el historial completo
# de un repo con 10 años de historia tarda demasiado.
# Solo necesitas el código actual, no el historial.
git clone --depth 1 https://github.com/empresa/proyecto.git
# → Solo descarga el último commit (sin historial)
# → Mucho más rápido y ligero
# → DESVENTAJA: no puedes hacer git log del historial completo
# → DESVENTAJA: no puedes hacer git blame de cambios antiguos

# Clonar con los últimos 10 commits (algo de historial):
git clone --depth 10 https://github.com/empresa/proyecto.git


# ============================================
# 4. -b: clonar una rama específica
# ============================================
# Situación: El repo tiene main (producción) y develop (desarrollo).
# Quieres empezar trabajando directamente en develop.
git clone -b develop https://github.com/empresa/proyecto.git
# → Hace checkout de develop en vez de main

# Clonar un tag específico (útil para trabajar con una versión concreta):
git clone --branch v2.3.0 --depth 1 https://github.com/empresa/proyecto.git


# ============================================
# 5. --single-branch: solo descargar UNA rama
# ============================================
# Situación: Un monorepo enorme. Solo necesitas la rama develop.
# No quieres que Git descargue objetos de feature/*, release/* etc.
git clone --single-branch -b develop https://github.com/empresa/proyecto.git
# → Solo descarga objetos de la rama develop
# → Ahorra mucho espacio en repos con muchas ramas

# Combinado con --depth para el máximo ahorro:
git clone --depth 1 --single-branch -b main https://github.com/empresa/proyecto.git


# ============================================
# 6. --recursive / --recurse-submodules
# ============================================
# Situación: El repo tiene submódulos (otros repos Git anidados).
# Si no usas --recursive, los directorios de submódulos aparecen vacíos.
git clone --recursive https://github.com/empresa/proyecto.git
# ó en Git moderno:
git clone --recurse-submodules https://github.com/empresa/proyecto.git


# ============================================
# 7. --mirror vs --bare: para copias de servidor
# ============================================
# --bare: Clone sin working directory (solo los objetos Git)
# Uso: hospedar el repo en tu propio servidor
git clone --bare https://github.com/empresa/proyecto.git proyecto.git

# --mirror: Como --bare pero sincroniza TODAS las refs (incluyendo remotas, tags)
# Uso: hacer mirror/backup exacto de un repo
git clone --mirror https://github.com/empresa/proyecto.git proyecto-backup.git
# Para actualizar el mirror después:
# cd proyecto-backup.git && git remote update


# ============================================
# 8. --filter: clone parcial (sin descargar todos los blobs)
# ============================================
# Situación: Monorepo enorme con archivos grandes (imágenes, binarios).
# Solo necesitas el código fuente.
git clone --filter=blob:none https://github.com/empresa/proyecto.git
# → Descarga solo el árbol de directorios, SIN el contenido de los archivos
# → Los archivos se descargan bajo demanda cuando los necesitas
# → Perfecto para repos con histórico de archivos grandes

# Para repos con archivos muy grandes en tree también:
git clone --filter=tree:0 https://github.com/empresa/proyecto.git


# ============================================
# 9. --local: clonar desde sistema de archivos local
# ============================================
# Situación: Tienes un repo en /home/usuario/proyecto y quieres
# crear una copia local sin ocupar espacio extra.
git clone --local /home/usuario/proyecto /home/usuario/proyecto-copia
# → Usa hardlinks en vez de copiar archivos (ahorra espacio en disco)
# → Solo funciona si origen y destino están en el mismo filesystem


# ============================================
# Protocolos disponibles
# ============================================
# HTTPS (el más universal):
git clone https://github.com/user/repo.git
# → Funciona en cualquier red, detrás de proxies y firewalls
# → Requiere autenticación con usuario/contraseña o token

# SSH (más rápido, no requiere contraseña si tienes key configurada):
git clone git@github.com:user/repo.git
# → Requiere configurar una clave SSH pública en GitHub/GitLab

# Local:
git clone /ruta/absoluta/al/repo
git clone ../ruta/relativa/al/repo
```

**Casos de uso reales:** [🔙](#10-git-clone---copiando-repositorios)

```bash
# ─────────────────────────────────────────────────────────────────
# CASO 1: Incorporarte a un proyecto del trabajo
# ─────────────────────────────────────────────────────────────────
# El tech lead te pasa la URL. Clone normal y a trabajar:
git clone git@github.com:empresa/mi-proyecto.git
cd mi-proyecto
git checkout -b feature/mi-tarea
# Listo, ya puedes empezar a trabajar


# ─────────────────────────────────────────────────────────────────
# CASO 2: CI/CD pipeline (rapidez es lo más importante)
# ─────────────────────────────────────────────────────────────────
# En el YAML de GitHub Actions / Jenkins / GitLab CI:
git clone --depth 1 https://github.com/empresa/proyecto.git
cd proyecto
npm install && npm test
# → El historial completo no sirve para compilar y testear
# → --depth 1 puede reducir el tiempo de 60s a 5s en repos grandes


# ─────────────────────────────────────────────────────────────────
# CASO 3: Trabajar en un proyecto open source (fork workflow)
# ─────────────────────────────────────────────────────────────────
# Paso 1: Hacer fork en GitHub (botón Fork en la web)
# Paso 2: Clonar TU fork (no el original)
git clone https://github.com/TU-USUARIO/proyecto-original.git
cd proyecto-original

# Paso 3: Añadir el repo original como "upstream" para sincronizar
git remote add upstream https://github.com/autor-original/proyecto-original.git

# Paso 4: Verificar remotos
git remote -v
# origin    https://github.com/TU-USUARIO/proyecto-original.git (tu fork)
# upstream  https://github.com/autor-original/proyecto-original.git (original)


# ─────────────────────────────────────────────────────────────────
# CASO 4: Backup/mirror de un repo importante
# ─────────────────────────────────────────────────────────────────
# Crear el mirror:
git clone --mirror https://github.com/empresa/proyecto-critico.git backup/proyecto.git

# Actualizar el mirror periódicamente (en un cron job):
cd backup/proyecto.git
git remote update
# → El mirror tiene TODO: ramas, tags, refs, notas


# ─────────────────────────────────────────────────────────────────
# CASO 5: Monorepo enorme - solo necesitas parte del código
# ─────────────────────────────────────────────────────────────────
# Un monorepo con 50 servicios, solo trabajas en "payments":
git clone --filter=blob:none --no-checkout https://github.com/empresa/monorepo.git
cd monorepo
git sparse-checkout init --cone
git sparse-checkout set services/payments shared/utils
git checkout main
# → Solo tienes en disco services/payments/ y shared/utils/
# → El resto se descarga bajo demanda
```

**Troubleshooting:** [🔙](#10-git-clone---copiando-repositorios)

```bash
# ─────────────────────────────────────────────────────────────────
# Problema 1: Error de autenticación
# ─────────────────────────────────────────────────────────────────
# Error: "Authentication failed" o "Permission denied (publickey)"
#
# Para HTTPS: usa un Personal Access Token (PAT) en vez de contraseña:
# → Ve a GitHub > Settings > Developer settings > Personal access tokens
# → Usa el token como contraseña
git clone https://tu-usuario:ghp_TuTokenAqui@github.com/empresa/repo.git

# Para SSH: verifica que tu clave está configurada:
ssh -T git@github.com    # Debe decir "Hi username!"
# Si falla, necesitas añadir tu clave SSH a GitHub


# ─────────────────────────────────────────────────────────────────
# Problema 2: Timeout o muy lento en repos grandes
# ─────────────────────────────────────────────────────────────────
# Solución: clone superficial primero, luego profundizar si necesitas
git clone --depth 1 https://github.com/empresa/repo-enorme.git
cd repo-enorme

# Si después necesitas más historia:
git fetch --unshallow          # Descarga el historial completo
# ó para los últimos 100 commits:
git fetch --depth 100


# ─────────────────────────────────────────────────────────────────
# Problema 3: Submódulos vacíos tras el clone
# ─────────────────────────────────────────────────────────────────
# Si clonaste sin --recursive y los submódulos están vacíos:
git submodule update --init --recursive
# → Descarga e inicializa todos los submódulos


# ─────────────────────────────────────────────────────────────────
# Problema 4: Clonar detrás de un proxy corporativo
# ─────────────────────────────────────────────────────────────────
git config --global http.proxy http://proxy.empresa.com:8080
git clone https://github.com/empresa/repo.git
# Después, para desactivar el proxy:
git config --global --unset http.proxy
```

**Mejores prácticas:** [🔙](#10-git-clone---copiando-repositorios)

```bash
✓ Usa SSH para repos privados del trabajo (más rápido y sin pedir contraseña)
✓ Usa HTTPS para proyectos públicos o cuando SSH no está disponible
✓ Usa --depth 1 en pipelines CI/CD (acelera enormemente)
✓ Usa --recursive si el proyecto tiene submódulos (evita directorios vacíos)
✓ Para forks, añade siempre el repo original como "upstream"

✗ No pongas credenciales directamente en la URL en scripts que se comparten
✗ No clones con --depth si necesitas hacer git blame o git log del historial completo
✗ No deshabilites SSL verification (git config http.sslVerify false) sin un motivo muy sólido
```

---

## Navegación

- [⬅️ Anterior: git rebase](09-git-rebase.md)
- [🏠 Volver al Índice](../../GIT_COMANDOS_GUIA_PRACTICA.md)
- [➡️ Siguiente: git remote](11-git-remote.md)

