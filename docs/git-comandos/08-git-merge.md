# 8. git merge - Integrando Cambios

[🏠 Volver al Índice](../../GIT_COMANDOS_GUIA_PRACTICA.md)

---

## 8. git merge - Integrando Cambios
[⬆️ Top](#tabla-de-contenidos)

**¿Qué hace?**
Integra cambios de una rama en otra, combinando el trabajo de diferentes líneas de desarrollo. Es uno de los comandos más críticos en Git para la colaboración en equipo.

**Funcionamiento interno:** [🔙](#8-git-merge---integrando-cambios)

```
Git merge puede operar de 3 formas diferentes:

1. FAST-FORWARD (merge "rápido"):
   main:    A---B
   feature:      C---D

   Resultado: main simplemente avanza al commit D
   main:    A---B---C---D

   → No crea merge commit
   → Solo mueve el puntero de la rama
   → Historia lineal limpia
   → Condición: main no ha avanzado desde que se creó feature

2. THREE-WAY MERGE (merge de 3 vías):
   main:    A---B---C
                \
   feature:      D---E

   Git usa 3 commits:
   - Ancestro común (B)
   - Último commit de main (C)
   - Último commit de feature (E)

   Resultado: Se crea nuevo merge commit (M)
   main:    A---B---C---M
                \     /
   feature:      D---E

   → Crea merge commit con 2 líneas de commits
   → Preserva historia completa
   → Historia no lineal (ramificada)

3. CONFLICTO:
   Cuando ambas ramas modifican las mismas líneas:
   → Git no puede decidir automáticamente
   → Marca conflictos en archivos
   → Requiere resolución manual
   → Crea merge commit tras resolver

Internamente:
1. git merge-base main feature  → Encuentra ancestro común
2. git diff-tree ancestro main  → Cambios en main
3. git diff-tree ancestro feature → Cambios en feature
4. Aplica ambos sets de cambios
5. Si no hay conflictos → merge automático
6. Si hay conflictos → pausa y marca conflictos
```

**Todas las opciones importantes:** [🔙](#8-git-merge---integrando-cambios)

```bash
# ============================================
# OPCIONES DE ESTRATEGIA DE MERGE
# ============================================

# 1. Merge básico (comportamiento por defecto)
# ─────────────────────────────────────────────
# Situación: Terminaste una feature y quieres integrarla a main.
# Git decide automáticamente cómo hacerlo:
#   - Si main no avanzó desde que creaste la rama → fast-forward (sin commit extra)
#   - Si main sí avanzó → crea un commit de merge
git merge feature-x


# 2. --no-ff → Forzar siempre un commit de merge
# ─────────────────────────────────────────────
# Situación: Quieres que en el historial quede visible que
# "aquí se integró la feature X", aunque no fuera necesario técnicamente.
#
# Sin --no-ff el historial queda así (no se distingue):
#   A---B---C---D  (main)
#
# Con --no-ff el historial queda así (se ve claramente la feature):
#   A---B---C---M  (main)
#            \ /
#             D  (feature-x)
#
# Beneficio: Si algún día la feature da problemas, puedes revertirla
# de un solo golpe con "git revert -m 1 M" sin afectar nada más.
git merge --no-ff feature-x


# 3. --ff-only → Solo merge si NO hace falta commit extra
# ─────────────────────────────────────────────
# Situación: Trabajas solo en una rama y quieres garantizar que
# el historial siempre sea una línea recta, sin ramificaciones.
# Si main avanzó mientras trabajabas, el comando falla con un error
# en lugar de crear un merge commit. Te obliga a hacer rebase primero.
#
# Útil en proyectos donde el equipo tiene la norma de "historia siempre lineal".
git merge --ff-only feature-x
# Si falla: primero haz  →  git rebase main  (desde tu rama)


# 4. --squash → Comprimir todos los commits de la rama en uno solo
# ─────────────────────────────────────────────
# Situación: Estuviste trabajando en una feature durante 3 días
# y tienes 20 commits tipo "wip", "fix typo", "arreglando de nuevo"...
# No quieres contaminar el historial de main con esos commits internos.
# Con --squash, todos esos cambios llegan a main como un ÚNICO commit limpio.
#
# Antes (rama feature-x tiene 20 commits):
#   wip → fix → fix2 → test → arreglo → ... (20 commits)
#
# Después en main:
#   "Add complete user login feature"  ← 1 solo commit limpio
#
# ⚠️ Importante: --squash NO hace el commit automáticamente.
#    Después debes hacer "git commit" manualmente con un buen mensaje.
# ⚠️ La rama feature-x NO queda marcada como mergeada (usa -d con cuidado).
git merge --squash feature-x
git commit -m "Add complete user login feature"


# 5. --edit / --no-edit → Controlar el mensaje del commit de merge
# ─────────────────────────────────────────────
# Situación: Al hacer merge, Git genera automáticamente un mensaje
# como "Merge branch 'feature-x'". A veces quieres personalizarlo
# para añadir más contexto (qué hace la feature, issue relacionado, etc.)
#
# --edit  → Abre el editor para que escribas tu propio mensaje
git merge --edit feature-x

# --no-edit → Usa el mensaje automático sin preguntar (útil en scripts/CI)
git merge --no-edit feature-x


# ============================================
# ESTRATEGIAS PARA RESOLVER CONFLICTOS AUTOMÁTICAMENTE
# ============================================
# Cuando dos ramas modifican la misma línea de un archivo,
# Git no sabe qué versión mantener → conflicto.
# Las siguientes opciones le dicen a Git cómo resolverlos sin preguntarte.
# ⚠️ Úsalas solo cuando estés seguro de qué versión es la correcta.


# 6. -X ours → En conflictos, ganar siempre nuestra versión
# ─────────────────────────────────────────────
# Situación: Estás integrando una rama externa o de un compañero,
# pero sabes que TU versión del código es la que debe prevalecer
# en todos los conflictos. En lugar de resolverlos uno a uno, le
# dices a Git: "si hay conflicto, quédate con lo que tengo yo".
#
# Ejemplo real: Merge de una rama de traducción que tocó archivos
# de configuración que tú también modificaste, y tu versión es la correcta.
git merge -X ours feature-x


# 7. -X theirs → En conflictos, ganar siempre la versión entrante
# ─────────────────────────────────────────────
# Situación: Quieres traer una rama de otro equipo y en caso de
# conflicto, aceptar siempre su versión. Por ejemplo, estás
# integrando una actualización de un proveedor externo y sus
# cambios son los que deben quedar.
git merge -X theirs feature-x


# 8. -X diff-algorithm → Cambiar el algoritmo de detección de diferencias
# ─────────────────────────────────────────────
# Situación: Haces un merge y Git te reporta muchos conflictos,
# pero cuando los abres, en realidad el código es casi idéntico.
# Esto pasa porque el algoritmo por defecto (myers) es rápido
# pero no muy "listo" para detectar qué cambió realmente en archivos
# con muchos bloques de código similares (llaves, corchetes repetidos...).
#
# Cambiar el algoritmo le permite a Git detectar mejor qué líneas
# realmente cambiaron, generando menos conflictos falsos.
#
# SINTAXIS: siempre se usa -X diff-algorithm=<nombre>

# myers (por defecto, el más rápido):
git merge feature-x
# → No hace falta especificarlo, es el comportamiento por defecto

# patience (más cuidadoso, menos conflictos falsos):
# → Útil en archivos grandes con muchos bloques de código parecidos
# → Ejemplo: archivos XML o JSON largos con muchas llaves/corchetes
git merge -X diff-algorithm=patience feature-x

# histogram (el más preciso, evolución de patience):
# → Generalmente el mejor para código fuente con muchas repeticiones
git merge -X diff-algorithm=histogram feature-x

# minimal (intenta producir el diff más pequeño posible):
git merge -X diff-algorithm=minimal feature-x

# Resumen de menor a mayor precisión (y menor a mayor coste):
#   myers < minimal < patience < histogram


# 9. -X ignore-space-change → Ignorar cambios de espacios en blanco
# ─────────────────────────────────────────────
# ⚠️ NOTA: Esta opción NO es un algoritmo de diff.
# Es una opción independiente que controla cómo se tratan
# los espacios en blanco al comparar líneas durante el merge.
#
# Situación: Tu compañero reformateó un archivo (cambió indentación,
# añadió espacios, etc.) pero no cambió la lógica. Al hacer merge,
# Git detecta conflictos en todas esas líneas aunque el código sea
# funcionalmente idéntico.
#
# Con esta opción, Git ignora esos cambios de espacios al comparar
# y solo marca conflicto cuando la lógica real es diferente.
git merge -X ignore-space-change feature-x

# Variantes disponibles (de menor a mayor agresividad):
# ignore-space-at-eol   → Solo ignora espacios al FINAL de cada línea
# ignore-space-change   → Ignora cambios en cantidad de espacios intermedios
# ignore-all-space      → Ignora TODOS los espacios en blanco (más agresivo)

git merge -X ignore-space-at-eol feature-x
git merge -X ignore-space-change feature-x
git merge -X ignore-all-space feature-x

# Se pueden combinar con un algoritmo específico:
git merge -X diff-algorithm=patience -X ignore-space-change feature-x


# 10. -X renormalize → Normalizar saltos de línea antes de comparar
# ─────────────────────────────────────────────
# ⚠️ NOTA: Esta opción tampoco es un algoritmo de diff.
# Es una opción de normalización que se aplica antes de la comparación.
#
# Situación: En equipos mixtos (Windows + Linux/Mac), los archivos
# a veces tienen diferentes tipos de salto de línea: Windows usa CRLF (\r\n)
# y Linux/Mac usa LF (\n). Al hacer merge entre ramas de distintos sistemas,
# Git puede ver conflictos en cada línea del archivo aunque nadie cambió nada.
#
# Con renormalize, Git normaliza los saltos de línea antes de comparar,
# evitando esos conflictos falsos causados solo por diferencias de plataforma.
git merge -X renormalize feature-x


# 11. Merge de múltiples ramas a la vez (Octopus merge)
# ─────────────────────────────────────────────
# Situación: Tienes 3 ramas independientes (feature-a, feature-b, feature-c)
# que no se tocan entre sí (modifican archivos distintos) y quieres
# integrarlas todas de golpe en develop. En vez de hacer 3 merges separados,
# puedes hacerlos todos en un solo comando.
#
# ⚠️ Limitación importante: si hay conflictos entre las ramas, este merge
# falla y tendrás que hacerlos por separado.
git merge feature-a feature-b feature-c


# 12. -s ours → Fingir que mergeaste sin aplicar nada
# ─────────────────────────────────────────────
# Situación: Tienes una rama antigua (old-experiment) que ya no quieres,
# pero Git sigue mostrándola como "no mergeada" en "git branch --no-merged".
# Con esta opción le dices a Git "sí, ya está mergeada" aunque en realidad
# NO se aplica ningún cambio de esa rama.
#
# ⚠️ NO confundir con "-X ours" (punto 6):
#   -s ours  → Ignora COMPLETAMENTE la otra rama. No aplica nada.
#   -X ours  → Sí intenta el merge, pero gana tu versión en conflictos.
git merge -s ours old-experiment


# 13. -s subtree → Integrar un proyecto externo dentro de un subdirectorio
# ─────────────────────────────────────────────
# Situación: Quieres incluir otro repositorio Git completo dentro
# de una carpeta de tu proyecto (por ejemplo, una librería que también
# desarrollas tú). Es parecido a los "git submodules" pero el código
# queda directamente dentro de tu repo, no como referencia externa.
#
# Ejemplo real: Tienes el repo "mi-app" y quieres meter el repo
# "mi-libreria" dentro de la carpeta "libs/". Con subtree, los commits
# de "mi-libreria" se integran en tu proyecto mapeados a esa carpeta.
#
# Antes de usarlo por primera vez, debes añadir el repo externo como remoto:
git remote add external-lib https://github.com/usuario/mi-libreria.git
git fetch external-lib
git merge -s subtree -X subtree=libs/ external-lib/main


# ============================================
# OPCIONES DE CONTROL Y VERIFICACIÓN
# ============================================

# 14. --no-commit --no-ff → Preparar el merge pero sin confirmar
# ─────────────────────────────────────────────
# Situación: Antes de hacer el merge definitivo, quieres ver exactamente
# qué cambios van a entrar, ejecutar los tests, o revisar si algo
# se va a romper. Con estas opciones, Git prepara todos los cambios
# en el staging area pero NO hace el commit todavía.
# Es como un "merge de prueba" que puedes inspeccionar antes de confirmar.
git merge --no-commit --no-ff feature-x
git diff --staged   # Ver qué va a entrar
npm test            # Comprobar que todo funciona
git commit          # Si todo está bien → confirmar
# o si algo falla:
git merge --abort   # Cancelar y volver al estado anterior


# 15. Ver qué va a entrar ANTES de hacer el merge
# ─────────────────────────────────────────────
# Situación: Quieres saber qué commits y qué cambios trae feature-x
# antes de integrarla, sin tocar nada.
git log HEAD..feature-x --oneline   # Lista de commits que van a entrar
git diff HEAD...feature-x           # Cambios concretos desde el punto de divergencia


# 16. --log → Incluir lista de commits en el mensaje de merge
# ─────────────────────────────────────────────
# Situación: Al hacer merge, quieres que el commit de merge incluya
# automáticamente un resumen de todos los commits que entran.
# Útil para tener un historial más descriptivo sin escribirlo a mano.
#
# El mensaje resultante sería algo así:
#   Merge branch 'feature-login'
#   * Add login form
#   * Add password validation
#   * Add session management
git merge --log feature-x


# 17. -S → Firmar el merge commit con tu clave GPG
# ─────────────────────────────────────────────
# Situación: En proyectos con requisitos de seguridad, se exige
# que cada commit esté firmado digitalmente para verificar que
# fue hecho por quien dice haberlo hecho (y no fue alterado).
# Requiere tener configurada una clave GPG.
git merge -S feature-x


# 18. -v / -q → Modo verboso o silencioso
# ─────────────────────────────────────────────
git merge -v feature-x   # Muestra detalles de lo que está haciendo (útil para aprender)
git merge -q feature-x   # Solo muestra errores (útil en scripts automáticos)


# ============================================
# MANEJO DE MERGE EN PROGRESO
# ============================================

# 19. --abort → Cancelar un merge que salió mal
# ─────────────────────────────────────────────
# Situación: Empezaste un merge, te aparecieron conflictos,
# y prefieres cancelarlo todo y volver al estado de antes.
# Git deshace todo lo que había empezado a mezclar.
git merge --abort


# 20. Continuar el merge después de resolver conflictos
# ─────────────────────────────────────────────
# Situación: Hiciste merge, aparecieron conflictos, los resolviste
# manualmente editando los archivos, y ahora quieres finalizar.
git add archivo-resuelto.txt   # Marcar como resuelto
git commit                     # Git detecta que había merge en curso
                                # y usa el mensaje de merge automáticamente


# 21. Ver el estado actual de un merge en progreso
# ─────────────────────────────────────────────
# Situación: No sabes si tienes un merge a medias o qué archivos
# todavía tienen conflictos sin resolver.
git status
# Muestra:
#   - "Unmerged paths" → archivos CON conflictos pendientes
#   - "Changes to be committed" → archivos ya resueltos

# También puedes verificar si hay merge en progreso:
ls .git/MERGE_HEAD   # Si este archivo existe → hay merge en curso
cat .git/MERGE_HEAD  # Muestra el SHA del commit que se está mergeando
```

**Resolución de conflictos - Guía completa:** [🔙](#8-git-merge---integrando-cambios)

```bash
# ============================================
# ¿QUÉ ES UN CONFLICTO Y CUÁNDO OCURRE?
# ============================================
# Un conflicto ocurre cuando dos personas (o dos ramas) modificaron
# la MISMA línea del MISMO archivo de forma diferente.
# Git no sabe cuál de las dos versiones es la correcta, así que
# se detiene y te pide que tú decidas.
#
# Ejemplo: Tú cambiaste la línea 42 de config.js a "timeout: 5000"
#          Tu compañero la cambió a "timeout: 3000"
#          Git no puede saber cuál es la correcta → conflicto.
#
# Mientras haya conflictos sin resolver, el merge está "pausado"
# y NO puedes hacer commits normales hasta resolverlos.

# ============================================
# PASO 1: VER QUÉ ARCHIVOS TIENEN CONFLICTOS
# ============================================

# Ver todos los archivos con conflicto
git status
# Los archivos conflictivos aparecen bajo "Unmerged paths:"
# con el estado "both modified" (ambos lo modificaron)

# Ver solo la lista de archivos en conflicto (más limpio)
git diff --name-only --diff-filter=U
# --diff-filter filtra el output de git diff según el ESTADO de cada archivo.
# El valor "U" significa "Unmerged" (sin resolver).
# → Muestra SOLO los archivos que tienen conflictos pendientes de resolver,
#   sin el ruido de los demás archivos ya resueltos.
#
# Otros valores útiles de --diff-filter durante un merge:
#   U  → Unmerged       (conflictos sin resolver) ← el más útil aquí
#   M  → Modified       (modificados en ambas ramas, sin conflicto)
#   A  → Added          (añadidos por la rama entrante)
#   D  → Deleted        (eliminados por la rama entrante)
#
# Se pueden combinar varios valores:
git diff --name-only --diff-filter=UM   # Conflictos + modificados
#
# Para una referencia completa de --diff-filter con todos sus valores,
# ver: [04-git-diff.md → sección --diff-filter](04-git-diff.md)

# Ver el detalle de qué líneas están en conflicto
git diff

# ============================================
# PASO 2: ENTENDER LAS MARCAS DE CONFLICTO
# ============================================
# Cuando abres un archivo en conflicto, verás algo así:

<<<<<<< HEAD
timeout: 5000   ← TU versión (la que tenías en tu rama actual)
=======
timeout: 3000   ← LA VERSIÓN ENTRANTE (la de la rama que mergeaste)
>>>>>>> feature-x

# Explicación de las marcas:
# <<<<<<< HEAD        → Aquí empieza TU versión
# =======             → Separador entre las dos versiones
# >>>>>>> feature-x   → Aquí termina la versión de la otra rama

# Tu tarea es: borrar las marcas (<<<, ===, >>>) y dejar el código
# como debe quedar. Puedes quedarte con una versión, con la otra,
# o combinar ambas.

# ============================================
# PASO 3: RESOLVER EL CONFLICTO
# ============================================

# OPCIÓN A: Resolución manual (la más habitual y recomendada)
# ─────────────────────────────────────────────
# 1. Abre el archivo en tu editor
# 2. Busca las marcas <<<, ===, >>>
# 3. Edita el código para que quede como debe quedar
# 4. Borra todas las marcas de conflicto
# 5. Guarda el archivo
git add archivo.txt    # Le dices a Git que ya resolviste este archivo
git commit             # Finalizas el merge


# OPCIÓN B: Quedarte con TU versión completa (sin editar)
# ─────────────────────────────────────────────
# Situación: Sabes con certeza que TU versión es la correcta
# y quieres descartar completamente los cambios de la otra rama.
git restore --ours archivo.txt
git add archivo.txt


# OPCIÓN C: Quedarte con la versión ENTRANTE completa
# ─────────────────────────────────────────────
# Situación: Sabes que los cambios de la otra rama son los correctos
# y quieres descartar tu versión del archivo.
git restore --theirs archivo.txt
git add archivo.txt


# OPCIÓN D: Ver ambas versiones antes de decidir
# ─────────────────────────────────────────────
# Si no tienes claro cuál versión elegir, primero observa
# qué cambió cada uno:
git diff --ours      # Muestra qué diferencia hay entre el ancestro y TU versión
git diff --theirs    # Muestra qué diferencia hay entre el ancestro y la versión entrante
git diff --base      # Muestra cómo era el archivo antes de que nadie lo tocara

# Ver el contenido exacto de cada versión:
git show :1:archivo.txt  # Cómo era ANTES del merge (ancestro común)
git show :2:archivo.txt  # Tu versión (HEAD)
git show :3:archivo.txt  # La versión entrante (feature-x)

# Guardarlos en archivos separados para compararlos con calma:
git show :2:archivo.txt > version-mia.txt
git show :3:archivo.txt > version-suya.txt


# OPCIÓN E: Usar herramienta visual (recomendado para conflictos complejos)
# ─────────────────────────────────────────────
# Situación: El conflicto es en un archivo grande o complejo y
# resolver línea a línea en el editor es confuso. Una herramienta
# visual muestra las dos versiones en paralelo y te permite elegir
# con más claridad.
git mergetool
# Abre la herramienta configurada (meld, vimdiff, kdiff3, vscode, etc.)
# Muestra 3 paneles: versión anterior (base), TU versión, versión entrante
# Puedes hacer clic para elegir qué líneas conservar

# Configurar VSCode como herramienta de merge:
git config --global merge.tool vscode
git config --global mergetool.vscode.cmd 'code --wait $MERGED'

# Configurar meld (más visual, recomendado para principiantes):
git config --global merge.tool meld
git config --global mergetool.prompt false

# ============================================
# CASOS ESPECIALES DE CONFLICTO
# ============================================

# CASO: Conflicto en archivo binario (imagen, PDF, etc.)
# ─────────────────────────────────────────────
# Los archivos binarios no se pueden resolver línea a línea.
# Git solo puede quedarse con UNA de las dos versiones completas.
git restore --ours archivo.png    # Conservar TU versión del binario
# o
git restore --theirs archivo.png  # Conservar la versión entrante
git add archivo.png


# CASO: Conflicto porque uno eliminó el archivo y el otro lo modificó
# ─────────────────────────────────────────────
# Git no sabe si mantener el archivo (con los cambios) o eliminarlo.
# Tú decides:
git rm archivo.txt    # Confirmas la eliminación
# o
git add archivo.txt   # Decides mantenerlo (con los cambios)


# CASO: Conflicto por archivo renombrado
# ─────────────────────────────────────────────
# Ocurre cuando uno renombró un archivo y el otro lo modificó.
# Git intenta detectarlo automáticamente.
# Si no lo detecta, debes resolver manualmente:
#   1. Añade el archivo con el nuevo nombre
#   2. Elimina el archivo con el nombre antiguo
#   3. git add + git rm + git commit


# ============================================
# PASO 4: CANCELAR SI TODO SALE MAL
# ============================================

# Si el merge se complica demasiado y quieres volver atrás:
git merge --abort
# → Cancela todo el merge
# → Restaura exactamente el estado que había antes de hacer "git merge"
# → Como si el merge nunca hubiera ocurrido

# Si resolviste mal un archivo y quieres volver a empezar con ese archivo:
git restore -m archivo.txt
# → Restaura las marcas de conflicto originales en ese archivo
# → Puedes volver a resolverlo desde cero


# ============================================
# PASO 5: VERIFICAR QUE TODO QUEDÓ BIEN
# ============================================

# Comprobar que no quedaron marcas de conflicto sin resolver
git diff --check
# → Si aparece algo, aún tienes marcas <<< o >>> en algún archivo

# Ver los archivos que ya están resueltos y listos para commit
git status
# Los archivos resueltos aparecen bajo "Changes to be committed"

# Ejecutar los tests antes de hacer el commit final
npm test  # (o el comando de tests de tu proyecto)
git commit

# Limpiar los archivos de backup que crea mergetool (terminan en .orig)
git clean -f
# o evitar que se creen en el futuro:
git config --global mergetool.keepBackup false
```

**Casos de uso del mundo real:**

```bash
# ============================================
# CASO 1: Feature simple lista para producción
# ============================================
git switch main
git pull origin main
git merge --no-ff feature-login
git push origin main
# → Usa --no-ff para mantener visible la feature en historia

# ============================================
# CASO 2: Sincronizar feature con main
# ============================================
# Estás en feature-x, main avanzó, quieres últimos cambios
git switch feature-x
git merge main
# → Trae cambios de main a tu feature
# → Resuelve conflictos ahora (no luego en main)
# → Testea todo funciona junto

# ============================================
# CASO 3: Multiple commits WIP, quieres 1 solo
# ============================================
git switch main
git merge --squash feature-x
# Archivo .git/SQUASH_MSG tiene todos los mensajes
git commit -m "Add user authentication system

- Login form
- Password validation
- Session management
- Remember me functionality"
# → Main tiene 1 commit limpio
# → Historia de desarrollo (commits WIP) se pierde

# ============================================
# CASO 4: Hotfix urgente en producción
# ============================================
git switch main
git switch -c hotfix-security
# ... fixes ...
git commit -m "Fix: Security vulnerability CVE-2024-1234"
git switch main
git merge --ff-only hotfix-security
# → --ff-only asegura merge limpio
# → Si falla, main se movió y hay que investigar
git push origin main
git branch -d hotfix-security

# ============================================
# CASO 5: Merge de múltiples features independientes
# ============================================
git switch develop
git merge feature-a feature-b feature-c
# → Octopus merge
# → Solo si no hay conflictos
# → Historia muestra merge simultáneo

# ============================================
# CASO 6: Merge con revisión antes de commitear
# ============================================
git merge --no-commit --no-ff feature-x
# → Prepara merge sin commitear
git diff --staged
# → Revisa todos los cambios
npm test
# → Verifica que funciona
git commit
# o si algo falla:
git merge --abort

# ============================================
# CASO 7: Rama obsoleta, solo quieres marcarla como mergeada
# ============================================
git merge -s ours old-experiment
# → No aplica ningún cambio de old-experiment
# → Pero Git la marca como mergeada
# → Útil para limpiar ramas sin afectar código

# ============================================
# CASO 8: Merge de release branch
# ============================================
# Merge a main (producción)
git switch main
git merge --no-ff --log release-1.5.0
# --log incluye lista de commits en mensaje

# Merge de vuelta a develop
git switch develop
git merge --no-ff release-1.5.0

# ============================================
# CASO 9: Resolver conflicto prefiriendo una versión
# ============================================
git merge feature-x
# ... conflicto ...
git restore --ours .      # Todas las versiones nuestras
# o
git restore --theirs .    # Todas las versiones de ellos
git add .
git commit

# Más selectivo (solo ciertos archivos):
git restore --ours src/
git restore --theirs config/
git add .
git commit

# ============================================
# CASO 10: Merge con conflictos, quieres ver qué cambió
# ============================================
git merge feature-x
# ... conflictos ...

# Ver historial de cambios en archivo conflictivo
git log --oneline --all -- archivo-conflicto.txt

# Ver qué cambió en cada rama
git log main..feature-x -- archivo-conflicto.txt
git show feature-x:archivo-conflicto.txt
git show main:archivo-conflicto.txt

# Resolver informadamente
# ... edita ...
git add archivo-conflicto.txt
git commit
```

**Troubleshooting y problemas comunes:**

```bash
# ============================================
# PROBLEMA 1: "Already up to date"
# ============================================
# Mensaje que ves:
#   Already up to date.
#
# Qué significa: Git revisó los commits de feature-x y vio que
# main ya los tiene todos. No hay nada nuevo que integrar.
#
# Causas comunes:
# - Ya hiciste el merge antes sin darte cuenta
# - Estás en la rama equivocada
# - La rama feature-x no tiene commits nuevos respecto a main
#
# Cómo diagnosticarlo:
git branch          # ¿Estás en main realmente?
git log main..feature-x --oneline  # ¿Hay commits en feature-x que no están en main?
# Si no aparece nada → feature-x no tiene nada nuevo


# ============================================
# PROBLEMA 2: "fatal: refusing to merge unrelated histories"
# ============================================
# Mensaje que ves:
#   fatal: refusing to merge unrelated histories
#
# Qué significa: Las dos ramas (o repositorios) nunca tuvieron
# un commit en común. Git no puede encontrar un punto de partida
# compartido para hacer el merge.
#
# Cuándo ocurre:
# - Intentas mergear un repo recién creado con "git init" con otro repo
# - Conectas un repo local con un remoto que tiene historia completamente diferente
# - Accidentalmente creaste dos repositorios separados del mismo proyecto
#
# Solución (con precaución):
git merge --allow-unrelated-histories other-branch
# ⚠️ Esto fuerza el merge aunque no haya historia común.
# Revisa bien el resultado porque puede mezclar archivos de dos proyectos distintos.


# ============================================
# PROBLEMA 3: "You have unmerged paths" (merge a medias)
# ============================================
# Mensaje que ves al hacer git status:
#   On branch main
#   You have unmerged paths.
#
# Qué significa: Iniciaste un merge, aparecieron conflictos,
# y los dejaste sin resolver. Git está "parado" esperando que
# termines la resolución.
#
# Cómo saberlo:
git status  # Muestra archivos bajo "Unmerged paths"
ls .git/MERGE_HEAD  # Si existe este archivo → hay merge en curso
#
# Solución 1: Terminar el merge resolviendo los conflictos
git status          # Ver qué archivos tienen conflicto
# Abre cada archivo y resuelve los marcadores <<<, ===, >>>
git add archivo.txt # Marca cada archivo como resuelto
git commit          # Finaliza el merge
#
# Solución 2: Cancelar todo y volver al estado anterior
git merge --abort


# ============================================
# PROBLEMA 4: Hiciste merge pero no querías commit de merge
# ============================================
# Situación: Hiciste "git merge feature-x" y Git creó un merge
# commit automáticamente, pero tú querías que el historial
# quedara lineal (sin ese commit extra).
#
# Solución: Deshacer el merge y repetirlo con la opción correcta
git reset --hard HEAD~1        # Deshace el último commit (el merge commit)
git merge --ff-only feature-x  # Solo permite merge si puede ser fast-forward
# Si falla → primero hay que hacer rebase en feature-x:
#   git switch feature-x
#   git rebase main
#   git switch main
#   git merge --ff-only feature-x


# ============================================
# PROBLEMA 5: Demasiados conflictos, imposible resolver
# ============================================
# Situación: El merge generó 50 conflictos en 20 archivos
# y no sabes por dónde empezar.
#
# Estrategia 1: Cancelar y resolver de uno en uno con rebase
# (rebase aplica los commits de uno en uno, los conflictos
#  son más pequeños y manejables)
git merge --abort
git switch feature-x
git rebase main     # Conflictos aparecen commit a commit, más fácil de resolver

# Estrategia 2: Aceptar una versión completa y revisar después
git merge -X theirs feature-x   # Acepta todos los cambios de feature-x
# Luego revisa manualmente los archivos críticos con:
git diff HEAD~1                  # Ver qué cambió

# Estrategia 3: Usar herramienta visual para ver mejor los conflictos
git mergetool  # Abre editor visual (meld, vscode, etc.)


# ============================================
# PROBLEMA 6: El merge borró un archivo que debería existir
# ============================================
# Situación: Después del merge, un archivo que necesitas
# ha desaparecido. Git lo eliminó automáticamente porque
# en una de las ramas se había borrado.
#
# Cómo recuperarlo del commit anterior al merge:
git show HEAD~1:ruta/archivo-perdido.txt > ruta/archivo-perdido.txt
git add ruta/archivo-perdido.txt
git commit --amend  # Modifica el merge commit para incluir el archivo recuperado


# ============================================
# PROBLEMA 7: El merge rompió funcionalidad (ya pusheaste)
# ============================================
# Situación: Hiciste merge, lo subiste al remoto, y ahora
# algo está roto. No puedes simplemente "deshacer" porque
# ya está en el servidor.
#
# Solución recomendada: revert (crea un commit que deshace el merge)
git revert -m 1 HEAD
# -m 1 → indica que quieres quedarte con la versión de main (rama principal)
# Esto es seguro porque NO borra historia, solo añade un commit nuevo
# que deshace los cambios del merge.
#
# Solución alternativa (solo si NO has pusheado o trabajas solo):
git reset --hard HEAD~1  # ⚠️ Borra el merge commit de la historia local


# ============================================
# PROBLEMA 8: "Your local changes would be overwritten by merge"
# ============================================
# Mensaje que ves:
#   error: Your local changes to 'archivo.txt' would be
#   overwritten by merge. Please commit or stash them.
#
# Qué significa: Tienes cambios en tu directorio de trabajo
# que NO están en un commit, y el merge necesita modificar
# esos mismos archivos. Git se niega a continuar para no
# borrar tu trabajo sin que lo hayas guardado.
#
# Solución 1: Guardar el trabajo en un commit temporal
git add .
git commit -m "WIP: guardando antes de merge"
git merge feature-x
# Después puedes hacer amend o squash si el commit WIP no te gusta

# Solución 2: Guardar temporalmente con stash (sin crear commit)
git stash           # Guarda tus cambios en un almacén temporal
git merge feature-x # Ahora el merge puede proceder
git stash pop       # Recupera tus cambios guardados

# Solución 3: Descartar tus cambios (¡CUIDADO: los pierdes para siempre!)
git restore .       # ⚠️ Descarta todos los cambios no commiteados
git merge feature-x
```

**Mejores prácticas y patrones:**

```bash
# ============================================
# ✅ BUENAS PRÁCTICAS
# ============================================

# 1. Siempre actualiza antes de merge
git switch main
git pull origin main
git merge feature-x

# 2. Usa --no-ff para features importantes
git merge --no-ff feature-login
# → Historia clara, fácil revertir feature completa

# 3. Resuelve conflictos en feature branch, no en main
git switch feature-x
git merge main
# ... resolver conflictos ...
git switch main
git merge feature-x  # Ahora sin conflictos

# 4. Testea tras resolver conflictos
git merge feature-x
# ... resolver ...
npm test
git commit

# 5. Usa mensajes de merge descriptivos
git merge --no-ff --edit feature-auth
# Edita para incluir:
# - Qué hace la feature
# - Issues relacionados (#123)
# - Reviewers

# 6. Squash para limpiar historia
git merge --squash feature-experiment
# → 47 commits de prueba → 1 commit limpio

# 7. Verifica antes de push
git log --oneline --graph -10
git diff origin/main
git push origin main

# 8. Usa merge commits para puntos importantes
git merge --no-ff release-2.0
# → Marca claramente releases en historia

# ============================================
# ✗ MALAS PRÁCTICAS
# ============================================

# 1. Mergear sin testear
git merge feature-x && git push  # ❌
# Puede romper main

# 2. Usar -X ours/theirs sin revisar
git merge -X theirs external-branch  # ❌
# Puede sobrescribir trabajo importante

# 3. Mergear directo a main sin revisión
# En proyectos serios, usa Pull Requests

# 4. Ignorar conflictos "pequeños"
# Todo conflicto requiere atención

# 5. No limpiar branches tras merge
git merge feature-x
git push
# Luego:
git branch -d feature-x  # ✅ Limpia local
git push origin --delete feature-x  # ✅ Limpia remoto

# 6. Merge de ramas públicas con rebase
# Causa problemas a colaboradores

# ============================================
# WORKFLOWS COMUNES
# ============================================

# GitHub Flow (simple)
1. Crea feature branch desde main
2. Desarrolla y commitea
3. Push y crea Pull Request
4. Revisión de código
5. Merge (con --no-ff) a main
6. Delete branch

# Git Flow (complejo)
- main: Producción
- develop: Integración
- feature/*: Nuevas features
- release/*: Preparar release
- hotfix/*: Fixes urgentes

# Feature → develop: --no-ff
# develop → main: --no-ff (con tag)
# hotfix → main y develop: --no-ff
```

**Comparación: merge vs rebase:**

```bash
# ============================================
# CUÁNDO USAR MERGE
# ============================================
✅ Integrar features completas a main
✅ Merges de release branches
✅ Colaboración en ramas públicas
✅ Preservar historia exacta de desarrollo
✅ Cuando múltiples devs trabajan en misma rama

Ventajas:
- No reescribe historia
- Seguro para ramas compartidas
- Preserva contexto (cuándo se mergeó)
- Fácil revertir (git revert -m 1)

Desventajas:
- Historia puede volverse compleja
- Grafo con muchas ramas
- "Merge commits" pueden saturar log

# ============================================
# CUÁNDO USAR REBASE
# ============================================
✅ Actualizar feature branch con main
✅ Limpiar commits locales antes de merge
✅ Mantener historia lineal
✅ Trabajo personal en rama local

Ventajas:
- Historia lineal y limpia
- Fácil de leer git log
- No crea merge commits extra

Desventajas:
- Reescribe historia (cambia SHAs)
- Peligroso en ramas públicas
- Puede causar problemas a colaboradores

# ============================================
# ESTRATEGIA HÍBRIDA (RECOMENDADA)
# ============================================

# 1. Durante desarrollo: rebase
git switch feature-x
git rebase main  # Mantiene feature actualizada y limpia

# 2. Para integrar: merge
git switch main
git merge --no-ff feature-x  # Integra feature completa

Resultado:
- Historia limpia en features (rebase)
- Historia clara en main (merge commits marcan features)
- Lo mejor de ambos mundos
```

**Configuración recomendada:**

```bash
# Configurar merge sin fast-forward por defecto
git config --global merge.ff false

# Siempre mostrar diffstat tras merge
git config --global merge.stat true

# Configurar herramienta de merge
git config --global merge.tool meld
git config --global mergetool.prompt false
git config --global mergetool.keepBackup false

# Estilo de conflictos (diff3 muestra ancestro común)
git config --global merge.conflictstyle diff3

# Ejemplo de conflicto con diff3:
<<<<<<< HEAD
código actual
||||||| merged common ancestors
código ancestro común
=======
código entrante
>>>>>>> feature-x

# Configurar para squash automático en certain branches
# (en .git/config o ~/.gitconfig)
[branch "develop"]
    mergeoptions = --no-ff

# Verificar configuración
git config --list | grep merge
```

---


---

## Navegación

- [⬅️ Anterior: git checkout / git switch](07-git-checkout-switch.md)
- [🏠 Volver al Índice](../../GIT_COMANDOS_GUIA_PRACTICA.md)
- [➡️ Siguiente: git rebase](09-git-rebase.md)

