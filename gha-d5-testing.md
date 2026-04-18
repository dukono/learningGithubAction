# 5.9 Testing / Verificación de D5 — Secure and optimize automation

← [5.8.2 Retention Policies](gha-retention-costes-seguridad.md) | [Índice](README.md)

---

Este fichero consolida la comprensión de todos los sub-dominios de D5 mediante preguntas tipo examen GH-200 y un checklist de conocimiento. Cada pregunta cubre un concepto examinable y explica no solo la respuesta correcta sino por qué las demás opciones son incorrectas.

## Preguntas de verificación D5

### Environment Protections

**P1: ¿Qué ocurre cuando un job referencia un environment con un reviewer configurado?**
a) El job se ejecuta inmediatamente y notifica al reviewer tras completarse.
b) El job se pausa en el paso de deployment hasta que un reviewer aprueba o rechaza.
c) El job falla si el reviewer no responde en 30 minutos.
d) Los secrets del environment solo son accesibles después de la aprobación.

**Respuesta: b** — El job se detiene en el gate de deployment y espera aprobación manual. Los secrets del environment no están disponibles hasta que el reviewer aprueba. La opción (d) es parcialmente correcta pero no es la descripción completa: el job en sí se pausa, no solo el acceso a secrets.

---

**P2: Un environment tiene `wait-timer: 10`. ¿Qué significa esto?**
a) El job falla si tarda más de 10 minutos.
b) El job espera 10 minutos antes de iniciar la ejecución, independientemente de aprobaciones.
c) El reviewer tiene 10 minutos para responder antes de que el job se cancele.
d) Los secrets del environment expiran a los 10 minutos de ser usados.

**Respuesta: b** — El `wait-timer` introduce un retraso fijo (en minutos) antes de que el job comience, después de cualquier aprobación manual. Se usa para dar tiempo a sistemas externos a prepararse. No tiene relación con el tiempo de respuesta del reviewer.

---

### Script Injection

**P3: ¿Cuál de los siguientes steps es vulnerable a script injection?**
a) `run: echo "El título es $TITLE"` con `env: TITLE: ${{ github.event.pull_request.title }}`
b) `run: echo "El título es ${{ github.event.pull_request.title }}"`
c) `run: echo "El autor es $AUTHOR"` con `env: AUTHOR: ${{ github.actor }}`
d) `run: node -e "console.log(process.env.TITLE)"` con `env: TITLE: ${{ github.event.pull_request.title }}`

**Respuesta: b** — Interpolar directamente una expresión `${{ }}` en el bloque `run:` inserta el valor en el script de shell antes de su ejecución. Si el título del PR contiene `` `rm -rf /` `` o `$(malicious)`, se ejecutará. Las opciones (a), (c) y (d) pasan el valor a través de variables de entorno, que no se interpretan como código shell.

---

**P4: ¿Qué mitigación es más efectiva contra script injection en steps `run:`?**
a) Usar `set -e` al inicio del script.
b) Pasar los valores de contexto a través de variables de entorno (`env:`) en lugar de interpolarlos directamente.
c) Usar `github.event_name == 'push'` como condición para limitar qué eventos ejecutan el step.
d) Escapar manualmente los caracteres especiales con `sed` antes de usarlos.

**Respuesta: b** — Las variables de entorno no se interpretan como código shell; son datos puros que el proceso hijo recibe. La opción (d) puede funcionar pero es propensa a errores de implementación y no es la práctica recomendada. Las opciones (a) y (c) no mitigan la inyección.

---

### GITHUB_TOKEN

**P5: ¿Cuándo expira el GITHUB_TOKEN?**

a) A los 60 minutos de haber sido emitido.
b) Cuando el workflow run finaliza (completo, fallido o cancelado).
c) A medianoche UTC del día en que se emitió.
d) Cuando el actor que disparó el workflow cierra sesión.

**Respuesta: b** — El GITHUB_TOKEN expira al terminar el workflow run que lo generó, independientemente de la duración. No tiene un TTL en minutos ni está vinculado a la sesión del actor.

---

**P6: Un workflow de PR abierto desde un fork falla al intentar escribir en el repositorio base con el GITHUB_TOKEN. ¿Cuál es la causa?**

a) Los forks no pueden usar el GITHUB_TOKEN.
b) El GITHUB_TOKEN en PRs de forks es read-only por seguridad.
c) El token no tiene el scope `contents: write` por defecto.
d) El repositorio base tiene IP allow lists activados.

**Respuesta: b** — Para evitar que código de forks no confiables modifique el repositorio base, GitHub restringe el GITHUB_TOKEN a permisos read-only cuando el workflow es disparado por un PR de un fork.

---

### OIDC

**P7: ¿Qué permiso debe declararse en el workflow para poder usar OIDC con un cloud provider?**

a) `secrets: write`
b) `id-token: write`
c) `packages: read`
d) `deployments: write`

**Respuesta: b** — El permiso `id-token: write` autoriza al workflow a solicitar un token OIDC al servicio de tokens de GitHub. Sin él, la action de autenticación cloud recibe un 403.

---

**P8: ¿Qué claim del token OIDC identifica de forma única el workflow y branch que lo generó?**

a) `aud` (audience)
b) `iss` (issuer)
c) `sub` (subject)
d) `jti` (JWT ID)

**Respuesta: c** — El claim `sub` contiene información como `repo:owner/repo:ref:refs/heads/main` y es el que se usa en la trust policy del cloud provider para restringir qué workflows pueden asumir el rol o identidad.

---

### Pin de Actions a SHA

**P9: ¿Por qué pinear una action a un tag de versión como `@v4` no garantiza inmutabilidad?**

a) Los tags de versión solo son válidos durante 30 días.
b) Un tag git puede moverse para apuntar a un commit diferente (y potencialmente malicioso).
c) GitHub elimina los tags antiguos cuando se publica una nueva versión.
d) Los tags no funcionan en runners self-hosted.

**Respuesta: b** — Un tag git es una referencia mutable. El propietario del repositorio de la action puede reapuntarlo a un commit diferente sin aviso. Un SHA de commit completo (40 caracteres) es inmutable porque identifica un contenido específico en el grafo de Git.

---

**P10: ¿Cómo puede un equipo mantener sus SHA pins actualizados sin trabajo manual?**

a) Usando `actions/cache` para reutilizar la versión pineada entre runs.
b) Configurando Dependabot con `package-ecosystem: github-actions` en `.github/dependabot.yml`.
c) Habilitando "Auto-merge" en el repositorio.
d) Usando `@latest` como referencia de versión.

**Respuesta: b** — Dependabot detecta nuevas versiones de las actions usadas en los workflows y abre PRs automáticos que actualizan el SHA al del nuevo tag, manteniendo el comentario con el nombre de versión legible.

---

### Enforcement de Policies

**P11: ¿Qué configuración bloquea el merge de un PR hasta que un workflow de CI específico pase?**

a) Environment protection rules con required reviewers.
b) Branch protection rules con required status checks.
c) IP allow lists de la organización.
d) Runner groups con política de acceso restringida.

**Respuesta: b** — Los required status checks en branch protection rules bloquean el botón de merge hasta que el check con el nombre exacto del job pase con éxito. Los environment protections bloquean ejecución de jobs, no el merge de PRs.

---

### SLSA Provenance y Artifact Attestations

**P12: ¿Qué garantía proporciona SLSA Build Level 2 (L2) sobre un artefacto?**

a) El artefacto no contiene vulnerabilidades conocidas.
b) El artefacto fue construido por una plataforma de build alojada (hosted), no en una máquina local.
c) El artefacto está firmado con una clave GPG del autor.
d) El artefacto fue revisado manualmente por un segundo desarrollador.

**Respuesta: b** — SLSA L2 garantiza que el build ocurrió en una plataforma hosted (como GitHub Actions con runners hosted), lo que proporciona trazabilidad del builder. No dice nada sobre vulnerabilidades, firmas de autor o revisión manual.

---

**P13: ¿Qué permisos son necesarios para usar `actions/attest-build-provenance`?**

a) `contents: write` y `packages: write`
b) `id-token: write` y `attestations: write`
c) `security-events: write` y `checks: write`
d) Solo `contents: read` (lectura por defecto)

**Respuesta: b** — `id-token: write` es necesario para OIDC (la attestation se firma usando el token OIDC) y `attestations: write` es necesario para publicar la attestation en el repositorio.

---

### Caché y Retención

**P14: ¿Cuánto tiempo persiste por defecto una entrada de caché en GitHub Actions si no se accede a ella?**

a) 24 horas
b) 7 días
c) 30 días
d) 90 días

**Respuesta: b** — Las entradas de caché se eliminan automáticamente si no se accede a ellas durante 7 días. Además, el total de caché por repositorio no puede superar 10 GB; cuando se alcanza el límite, se eliminan las entradas más antiguas.

---

**P15: ¿Desde qué ramas puede un job acceder a una caché creada en la rama `feature/login`?**

a) Desde cualquier rama del repositorio.
b) Solo desde la rama `feature/login`.
c) Desde `feature/login`, la rama base y la rama predeterminada.
d) Desde `feature/login` y todas las ramas que tengan el mismo prefijo `feature/`.

**Respuesta: c** — El scope de caché sigue la jerarquía de ramas: rama actual → rama base del PR → rama predeterminada. Una caché creada en `feature/login` es accesible desde esa rama y sus ancestros en el árbol de ramas, pero no desde otras ramas feature ni desde ramas hijo.

---

## Checklist de conocimiento D5

Antes de presentar el examen GH-200, verifica que puedes:

- [ ] Configurar `required reviewers`, `wait-timer` y `deployment branches` en un environment
- [ ] Distinguir entre environment protection rules y branch protection rules
- [ ] Identificar un step vulnerable a script injection y corregirlo con `env:`
- [ ] Explicar por qué `pull_request_target` + checkout del PR es peligroso
- [ ] Describir el ciclo de vida del GITHUB_TOKEN y cuándo expira
- [ ] Declarar `permissions:` a nivel de workflow y por job, incluyendo `permissions: {}`
- [ ] Explicar qué claim del token OIDC se usa en la trust policy del cloud provider
- [ ] Configurar el permiso `id-token: write` para OIDC
- [ ] Explicar por qué un SHA de commit completo garantiza inmutabilidad y un tag no
- [ ] Configurar Dependabot para mantener SHA pins actualizados
- [ ] Distinguir entre required status checks (merge) y environment protections (ejecución)
- [ ] Explicar qué garantiza SLSA Build Level 2 (y qué NO garantiza)
- [ ] Usar `actions/attest-build-provenance` con los permisos correctos
- [ ] Distinguir entre caché (7 días, acelera builds) y artefactos (90 días, persiste outputs)
- [ ] Describir el vector de cache poisoning desde forks y cómo el scope de caché lo mitiga

---

← [5.8.2 Retention Policies](gha-retention-costes-seguridad.md) | [Índice](README.md)
