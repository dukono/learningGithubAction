# 0. git config - Configuración de Git

[🏠 Volver al Índice Principal](../../GIT_COMANDOS_GUIA_PRACTICA.md)

---

## Tabla de Contenidos

- [¿Qué es git config?](#qué-es-git-config)
- [Los 3 niveles de configuración](#los-3-niveles-de-configuración)
- [Archivos de configuración](#archivos-de-configuración)
- [Cómo funciona la precedencia](#cómo-funciona-la-precedencia)
- [Uso básico del comando](#uso-básico-del-comando)
- [Configuración esencial (primer uso)](#configuración-esencial-primer-uso)
- [Ver la configuración actual](#ver-la-configuración-actual)
- [Claves de configuración más importantes](#claves-de-configuración-más-importantes)
  - [Identidad](#identidad)
  - [Editor](#editor)
  - [Colores](#colores)
  - [Alias](#alias)
  - [Comportamiento de ramas y push](#comportamiento-de-ramas-y-push)
  - [Pull y merge](#pull-y-merge)
  - [Credenciales](#credenciales)
  - [Diferencias y herramientas externas](#diferencias-y-herramientas-externas)
  - [Rendimiento y red](#rendimiento-y-red)
  - [Saltos de línea (cross-platform)](#saltos-de-línea-cross-platform)
- [Configuración por repositorio concreto](#configuración-por-repositorio-concreto)
- [Configuración condicional (includeIf)](#configuración-condicional-includeif)
- [Editar el fichero de configuración directamente](#editar-el-fichero-de-configuración-directamente)
- [Eliminar claves de configuración](#eliminar-claves-de-configuración)
- [Casos de uso reales](#casos-de-uso-reales)
- [Troubleshooting](#troubleshooting)

---

## ¿Qué es git config?

`git config` es el comando que controla **todo el comportamiento de Git**: tu identidad, el editor que usa, cómo gestiona credenciales, cómo hace merges, colores, aliases, y mucho más.

Sin una configuración mínima (nombre y email), Git ni siquiera te deja hacer commits.

---

## Los 3 niveles de configuración

Git tiene **tres niveles** de configuración, cada uno con su alcance y su fichero:

| Nivel | Flag | Alcance | Quién lo usa |
|-------|------|---------|--------------|
| **system** | `--system` | Todo el sistema, todos los usuarios | Administrador del sistema |
| **global** | `--global` | Solo tu usuario, todos sus repos | Tu configuración personal |
| **local** | `--local` | Solo el repositorio actual | Configuración por proyecto |

```
Sistema operativo
  └── system  (/etc/gitconfig)
        └── global  (~/.gitconfig  o  ~/.config/git/config)
              └── local  (.git/config  en el repo actual)
```

---

## Archivos de configuración

```bash
# SYSTEM - aplica a todos los usuarios del sistema
/etc/gitconfig

# GLOBAL - aplica solo a tu usuario (el más común)
~/.gitconfig
# o en sistemas nuevos:
~/.config/git/config

# LOCAL - aplica solo al repositorio actual
.git/config          # dentro de la carpeta del repo

# WORKTREE - nivel extra para git worktree (raro)
.git/config.worktree
```

Para **ver** dónde está cada fichero:
```bash
git config --list --show-origin       # muestra cada clave y desde qué fichero viene
git config --list --show-scope        # muestra el nivel (system/global/local)
```

---

## Cómo funciona la precedencia

**local > global > system**

Si una clave está definida en varios niveles, **el nivel más cercano al repo gana**:

```
system:  user.email = admin@empresa.com
global:  user.email = yo@personal.com
local:   user.email = yo@trabajo-clienteX.com   ← ESTE gana en este repo
```

Esto es muy útil para tener **un email diferente por repositorio** (trabajo vs personal).

---

## Uso básico del comando

```bash
# Leer una clave
git config <clave>
git config user.name

# Escribir una clave (nivel local por defecto)
git config <clave> <valor>
git config user.name "Mi Nombre"

# Escribir a nivel global
git config --global <clave> <valor>
git config --global user.email "yo@email.com"

# Escribir a nivel system (necesita permisos de admin)
git config --system <clave> <valor>

# Leer de un nivel concreto
git config --global user.name
git config --local user.email
```

---

## Configuración esencial (primer uso)

Lo mínimo para poder hacer commits:

```bash
# Identidad - OBLIGATORIO
git config --global user.name "Tu Nombre Completo"
git config --global user.email "tu@email.com"

# Editor por defecto para mensajes de commit, rebase interactivo, etc.
git config --global core.editor "nano"           # nano (sencillo)
git config --global core.editor "vim"            # vim
git config --global core.editor "code --wait"    # VS Code
git config --global core.editor "idea --wait"    # IntelliJ IDEA

# Nombre de la rama por defecto al hacer git init
git config --global init.defaultBranch main
```

---

## Ver la configuración actual

```bash
# Ver toda la configuración efectiva (fusión de los 3 niveles)
git config --list

# Ver con el origen (qué fichero define cada clave)
git config --list --show-origin

# Ver con el nivel (system/global/local)
git config --list --show-scope

# Ver solo la configuración global
git config --global --list

# Ver solo la configuración local (del repo actual)
git config --local --list

# Ver una clave concreta
git config user.name
git config user.email
git config core.editor

# Ver una clave de un nivel concreto
git config --global user.email
git config --local remote.origin.url
```

---

## Claves de configuración más importantes

### Identidad

```bash
git config --global user.name "Nombre Apellido"
git config --global user.email "nombre@empresa.com"

# Para un repo concreto con identidad diferente (trabajo vs personal)
git config --local user.name "Nombre Trabajo"
git config --local user.email "nombre@cliente.com"
```

---

### Editor

El editor se usa en: mensajes de commit largos, rebase interactivo (`-i`), resolución de conflictos manual, `git notes`, etc.

```bash
git config --global core.editor "nano"
git config --global core.editor "vim"
git config --global core.editor "emacs"
git config --global core.editor "code --wait"      # VS Code (--wait es OBLIGATORIO)
git config --global core.editor "idea --wait"      # IntelliJ
git config --global core.editor "subl -n -w"       # Sublime Text
```

---

### Colores

```bash
# Activar colores automáticamente (recomendado)
git config --global color.ui auto

# Desactivar colores (útil para scripts)
git config --global color.ui false

# Forzar siempre colores (incluso si la salida no es un terminal)
git config --global color.ui always

# Colores específicos por comando
git config --global color.status.changed "yellow bold"
git config --global color.status.untracked "red"
git config --global color.status.added "green bold"
git config --global color.diff.new "green bold"
git config --global color.diff.old "red bold"
git config --global color.branch.current "green bold"
git config --global color.branch.remote "red"
```

---

### Alias

Los alias te permiten crear atajos para comandos largos:

```bash
# Crear alias
git config --global alias.st "status"
git config --global alias.co "checkout"
git config --global alias.br "branch"
git config --global alias.lg "log --oneline --graph --all --decorate"
git config --global alias.last "log -1 HEAD --stat"
git config --global alias.unstage "restore --staged"
git config --global alias.undo "reset HEAD~1 --mixed"

# Uso:
git st          # equivale a: git status
git lg          # equivale a: git log --oneline --graph --all --decorate
git last        # ver el último commit con estadísticas

# Ver todos los alias configurados
git config --global --list | grep alias
```

---

### Comportamiento de ramas y push

```bash
# Qué hace push por defecto cuando no especificas rama
git config --global push.default simple       # solo pushea la rama actual a su upstream (recomendado, default desde Git 2.0)
git config --global push.default current      # pushea al mismo nombre en remoto
git config --global push.default matching     # pushea todas las ramas que coincidan (antiguo comportamiento)
git config --global push.default nothing      # obliga a especificar siempre destino

# Subir tags automáticamente al hacer push
git config --global push.followTags true

# Rama por defecto al crear repos nuevos
git config --global init.defaultBranch main

# Comportamiento del tracking automático
git config --global branch.autoSetupMerge always
git config --global branch.autoSetupRebase always
```

---

### Pull y merge

```bash
# Cómo reconciliar ramas divergentes al hacer pull
git config --global pull.rebase false    # merge (comportamiento tradicional)
git config --global pull.rebase true     # rebase (historial más limpio)
git config --global pull.ff only         # solo fast-forward (falla si hay divergencia)

# Comportamiento del merge
git config --global merge.ff false       # nunca hacer fast-forward (siempre crea merge commit)
git config --global merge.stat true      # mostrar estadísticas tras el merge
git config --global merge.conflictstyle diff3   # mostrar versión base en conflictos (muy útil)

# Herramienta para resolver conflictos
git config --global merge.tool meld
git config --global merge.tool vimdiff
git config --global merge.tool code      # VS Code

# Rebase
git config --global rebase.autoStash true    # guarda stash automáticamente antes del rebase
git config --global rebase.autoSquash true   # auto-squash commits marcados con fixup!/squash!
```

---

### Credenciales

Cómo y dónde guarda Git las credenciales (usuario/contraseña o tokens):

```bash
# Ver el helper actual
git config --global credential.helper

# Opciones disponibles:

# 1. CACHE - guarda en memoria por un tiempo (default: 15 min)
git config --global credential.helper cache
git config --global credential.helper "cache --timeout=3600"   # 1 hora

# 2. STORE - guarda en fichero de texto plano (~/.git-credentials) ⚠️ no seguro
git config --global credential.helper store

# 3. MANAGER - gestor del sistema operativo (keychain, secretservice, etc.) - RECOMENDADO
git config --global credential.helper manager          # Windows (Git Credential Manager)
git config --global credential.helper osxkeychain      # macOS
git config --global credential.helper libsecret        # Linux con GNOME Keyring

# Dónde está el fichero de store (si usas store):
cat ~/.git-credentials
# formato: https://usuario:token@github.com

# Para un repo concreto (credenciales distintas por repo):
git config --local credential.helper store
git config --local credential.https://github.com.username miusuario

# Borrar credenciales guardadas:
git credential reject   # borra credenciales de la URL especificada
# o editar manualmente:
nano ~/.git-credentials
```

> ⚠️ **Nota sobre GitHub**: GitHub ya no acepta contraseñas. Usa un **Personal Access Token (PAT)** como contraseña, o usa SSH.

---

### Diferencias y herramientas externas

```bash
# Herramienta externa para diff
git config --global diff.tool meld
git config --global diff.tool vimdiff
git config --global diff.tool code       # VS Code

# Activar el uso de la herramienta automáticamente
git config --global diff.external ""

# Mostrar estadísticas de diff por defecto
git config --global diff.stat true

# Algoritmo de diff (patience es más legible en muchos casos)
git config --global diff.algorithm patience

# Herramienta para merge
git config --global mergetool.prompt false         # no preguntar antes de abrir
git config --global mergetool.keepBackup false     # no dejar ficheros .orig tras resolver
```

---

### Rendimiento y red

```bash
# Buffer para operaciones HTTP (útil para repos grandes o conexiones lentas)
git config --global http.postBuffer 524288000     # 500 MB

# Verificación SSL (deshabilitar solo en entornos controlados, no recomendado en producción)
git config --global http.sslVerify false

# Proxy HTTP
git config --global http.proxy http://proxy.empresa.com:8080
git config --global https.proxy http://proxy.empresa.com:8080

# Compresión
git config --global core.compression 9            # máxima compresión (más CPU, menos ancho de banda)

# Fetch en paralelo
git config --global fetch.parallel 4

# Limpiar refs remotas obsoletas automáticamente en cada fetch
git config --global fetch.prune true
```

---

### Saltos de línea (cross-platform)

Muy importante cuando colaboras entre Windows y Linux/macOS:

```bash
# En Linux/macOS: NO convertir saltos de línea
git config --global core.autocrlf input

# En Windows: convertir CRLF→LF al subir, LF→CRLF al bajar
git config --global core.autocrlf true

# Desactivar completamente la conversión
git config --global core.autocrlf false

# Modo estricto (falla si hay saltos de línea inconsistentes)
git config --global core.safecrlf true
```

---

## Configuración por repositorio concreto

Para proyectos donde necesitas identidad, email o comportamiento diferente:

```bash
# Entrar al directorio del repo
cd /ruta/al/repo

# Configurar solo para ESE repo (sin --global)
git config user.name "Nombre Cliente"
git config user.email "yo@cliente.com"
git config pull.rebase true
git config core.editor "nano"

# Verificar que se guardó en .git/config
cat .git/config
```

Ejemplo de `.git/config` local tras esas configuraciones:
```ini
[core]
    repositoryformatversion = 0
    filemode = true
    bare = false
    logallrefupdates = true
    editor = nano
[remote "origin"]
    url = https://github.com/usuario/repo.git
    fetch = +refs/heads/*:refs/remotes/origin/*
[branch "main"]
    remote = origin
    merge = refs/heads/main
[user]
    name = Nombre Cliente
    email = yo@cliente.com
[pull]
    rebase = true
```

---

## Configuración condicional (includeIf)

Permite **cambiar automáticamente** la configuración según el directorio donde estés. Ideal para tener configuración personal en `~/personal/` y de trabajo en `~/trabajo/`:

```bash
# En ~/.gitconfig añadir:
[includeIf "gitdir:~/trabajo/"]
    path = ~/.gitconfig-trabajo

[includeIf "gitdir:~/personal/"]
    path = ~/.gitconfig-personal
```

```bash
# ~/.gitconfig-trabajo
[user]
    name = Tu Nombre Trabajo
    email = tu@empresa.com
[pull]
    rebase = true
```

```bash
# ~/.gitconfig-personal
[user]
    name = Tu Nombre Personal
    email = tu@gmail.com
[pull]
    rebase = false
```

Desde Git 2.36 también existe `hasconfig:remote.*.url:`:
```bash
[includeIf "hasconfig:remote.*.url:https://github.com/**"]
    path = ~/.gitconfig-github
```

---

## Editar el fichero de configuración directamente

```bash
# Abrir el fichero global en el editor configurado
git config --global --edit

# Abrir el fichero local en el editor configurado
git config --local --edit

# O editarlo directamente
nano ~/.gitconfig
cat ~/.gitconfig
```

Ejemplo de `~/.gitconfig` completo:
```ini
[user]
    name = Tu Nombre
    email = tu@email.com

[core]
    editor = nano
    autocrlf = input

[init]
    defaultBranch = main

[pull]
    rebase = false

[push]
    default = simple
    followTags = true

[color]
    ui = auto

[alias]
    st = status
    co = checkout
    br = branch
    lg = log --oneline --graph --all --decorate
    last = log -1 HEAD --stat
    unstage = restore --staged

[credential]
    helper = store

[fetch]
    prune = true

[merge]
    conflictstyle = diff3

[diff]
    algorithm = patience
```

---

## Eliminar claves de configuración

```bash
# Eliminar una clave del nivel global
git config --global --unset user.email

# Eliminar una clave del nivel local
git config --local --unset pull.rebase

# Eliminar una sección entera
git config --global --remove-section alias
git config --local --remove-section user
```

---

## Casos de uso reales

### Caso 1: Primer uso de Git en una máquina nueva
```bash
git config --global user.name "Ana García"
git config --global user.email "ana@empresa.com"
git config --global core.editor "nano"
git config --global init.defaultBranch main
git config --global pull.rebase false
git config --global color.ui auto
git config --global fetch.prune true
git config --global push.default simple
git config --global credential.helper store
```

### Caso 2: Repo de trabajo con email diferente al personal
```bash
cd ~/proyectos/cliente-x
git config user.email "ana@cliente-x.com"
git config user.name "Ana García (ClienteX)"
# Verificar
git config user.email   # → ana@cliente-x.com
git config --global user.email  # → ana@personal.com (no cambia)
```

### Caso 3: Ver de dónde viene cada configuración (diagnóstico)
```bash
git config --list --show-origin --show-scope
# Salida ejemplo:
# system  file:/etc/gitconfig         core.symlinks=false
# global  file:/home/ana/.gitconfig   user.name=Ana García
# global  file:/home/ana/.gitconfig   user.email=ana@personal.com
# local   file:.git/config            user.email=ana@cliente-x.com  ← sobreescribe global
```

### Caso 4: Guardar credenciales para un repo específico (HTTPS)
```bash
cd /ruta/al/repo
git config --local credential.helper store
# La próxima vez que hagas push/pull te pedirá usuario y contraseña (o token)
# y las guardará en ~/.git-credentials

# Ver lo que se guardó:
cat ~/.git-credentials
# https://usuario:ghp_tokenXXXXXX@github.com
```

### Caso 5: Cambiar el upstream sin conocer el nombre completo
```bash
# Asociar la rama actual a su upstream del mismo nombre:
git branch --set-upstream-to=origin/$(git branch --show-current)

# O al hacer push por primera vez:
git push -u origin HEAD    # HEAD toma el nombre de la rama actual automáticamente
```

### Caso 6: Configurar pull.rebase para no ver el aviso al hacer pull
```bash
# Si git pull te dice "hint: You have divergent branches..."
# elige uno de estos según tu preferencia:
git config --global pull.rebase false   # merge (más sencillo de entender)
git config --global pull.rebase true    # rebase (historial más lineal)
git config --global pull.ff only        # solo fast-forward (más estricto)
```

---

## Troubleshooting

### "Please tell me who you are" al hacer commit
```bash
git config --global user.email "tu@email.com"
git config --global user.name "Tu Nombre"
```

### El editor no abre o abre uno que no quieres
```bash
git config --global core.editor "nano"
# Si usas VS Code:
git config --global core.editor "code --wait"
# Verifica que 'code' está en el PATH
which code
```

### Las credenciales se piden en cada push/pull
```bash
# Usar cache por 1 hora:
git config --global credential.helper "cache --timeout=3600"
# O guardar permanentemente (menos seguro):
git config --global credential.helper store
```

### Ver exactamente qué configuración está activa en el repo actual
```bash
git config --list --show-origin --show-scope | grep -E "user|email|pull|push"
```

### Una clave no parece tener efecto
```bash
# Puede estar sobreescrita por un nivel más local
git config --list --show-scope | grep <clave>
# El nivel "local" siempre gana sobre "global" y "system"
```

### Deshacer una configuración global
```bash
git config --global --unset <clave>
# Ejemplo:
git config --global --unset pull.rebase
```

---

[🔝 Top](#0-git-config---configuración-de-git) | [🏠 Volver al Índice Principal](../../GIT_COMANDOS_GUIA_PRACTICA.md)

