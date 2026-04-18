# 4.4 IP Allow Lists y GitHub-Hosted Runners

← [4.3.2 Permisos GITHUB_TOKEN y retención](gha-d4-politicas-token-retencion.md) | [Índice](README.md) | [4.5.1 Runner Groups: creación](gha-d4-runner-groups-creacion.md) →

---

En GitHub Enterprise Cloud las organizaciones pueden restringir el acceso a sus recursos definiendo una lista de IPs autorizadas (IP allow list). Sin embargo, los GitHub-hosted runners obtienen sus direcciones IP de un pool dinámico gestionado por GitHub, lo que hace que esas IPs no sean predecibles ni fijas. El resultado es que un job en un GitHub-hosted runner puede fallar al intentar conectarse a recursos internos o servicios de terceros que filtran por IP. Este fichero explica las tres soluciones disponibles y cuándo aplicar cada una.

## El problema: IPs dinámicas de GitHub-hosted runners

Los GitHub-hosted runners se crean en el momento en que un job comienza a ejecutarse. Cada runner obtiene una IP del bloque de direcciones de GitHub, que puede cambiar entre ejecuciones. No existe forma de predecir qué IP usará el próximo job.

Cuando la organización tiene activada la IP allow list, solo los clientes con IPs en esa lista pueden acceder a los recursos de la organización (repositorios, paquetes, APIs internas). Un GitHub-hosted runner con IP dinámica no estará en esa lista, por lo que cualquier operación de red que requiera autenticación ante el recurso protegido fallará con un error de conexión rechazada o timeout.

> [CONCEPTO] El endpoint `GET https://api.github.com/meta` devuelve en el campo `"actions"` la lista de rangos CIDR que GitHub usa actualmente para sus runners. Esta lista puede tener decenas de bloques y cambia con frecuencia, lo que la hace inmanejable para mantenerla en una allow list corporativa.

## Tres soluciones disponibles

### Solución 1: deshabilitar la IP allow list para GitHub Actions

GitHub permite excluir a GitHub Actions del filtrado por IP. Con esta opción, la IP allow list sigue aplicándose a todos los demás clientes (usuarios, integraciones, tokens de acceso), pero los workflows de GitHub Actions pueden acceder a los recursos sin restricción de IP.

Ruta de configuración en la UI:

```
Settings (organización) > Security > IP allow list
> "Enable IP allow list for GitHub Actions runners" → desactivar
```

Esta opción afecta a toda la organización y aplica tanto a GitHub-hosted runners como a self-hosted runners alojados fuera de la red corporativa.

### Solución 2: larger runners con IP estática

GitHub ofrece larger runners (runners de mayor tamaño con recursos dedicados) que pueden tener una IP estática asignada a la organización. Esta IP es predecible y puede añadirse permanentemente a la IP allow list.

El larger runner con IP estática se configura al crear el runner en la UI de la organización y requiere plan GitHub Team o GitHub Enterprise Cloud.

```yaml
jobs:
  deploy:
    # Usar el larger runner con IP estática registrada en la allow list
    runs-on: ubuntu-latest-4-cores
    steps:
      - uses: actions/checkout@v4
      - name: Publicar artefacto en registro interno
        run: |
          curl -X POST https://registro.interno.empresa.com/api/upload \
            -H "Authorization: Bearer ${{ secrets.REGISTRY_TOKEN }}" \
            -F "file=@dist/app.tar.gz"
```

### Solución 3: self-hosted runners dentro de la red corporativa

Los self-hosted runners se ejecutan en infraestructura controlada por la organización. Si se despliegan dentro de la red corporativa, sus IPs pertenecen al rango corporativo, que normalmente ya está en la IP allow list.

```yaml
jobs:
  build-interno:
    runs-on: self-hosted
    steps:
      - uses: actions/checkout@v4
      - name: Acceder a repositorio de artefactos interno
        run: |
          mvn deploy -DaltDeploymentRepository=interno::default::https://nexus.corp.empresa.com/repository/releases
```

## Consultar rangos de IP de GitHub

El endpoint público `GET https://api.github.com/meta` devuelve los rangos de IP actuales de todos los servicios de GitHub. El campo `"actions"` contiene específicamente los bloques CIDR usados por GitHub-hosted runners.

```bash
# Obtener rangos de IP de GitHub Actions runners
curl -s https://api.github.com/meta | jq '.actions'
# Ejemplo de respuesta (los rangos cambian con frecuencia):
# [
#   "192.30.252.0/22",
#   "185.199.108.0/22",
#   "140.82.112.0/20",
#   ...
# ]
```

> [EXAMEN] Conocer este endpoint es importante para el examen. GitHub lo documenta como la fuente oficial para obtener los rangos de IP de Actions. Sin embargo, la frecuencia de cambio hace inviable mantener estos rangos en una allow list corporativa manualmente.

## Tabla comparativa: las 3 soluciones

| Solución | Cómo funciona | Ventajas | Inconvenientes | Cuándo usar |
|----------|--------------|----------|----------------|-------------|
| Deshabilitar allow list para Actions | Exenta a GitHub Actions del filtrado por IP a nivel de organización | Cero configuración adicional; todos los repos se benefician | Reduce la protección: cualquier workflow puede acceder a recursos de la org sin filtro IP | Org pequeña o cuando la seguridad perimetral es secundaria |
| Larger runners con IP estática | El runner tiene una IP fija asignada a la org; se añade a la allow list | IP predecible y permanente; mantiene la allow list activa para el resto | Coste adicional (larger runners son más caros); requiere plan Team o Enterprise | Necesidad de IP fija con allow list activa |
| Self-hosted runners en red corporativa | El runner vive en la red interna; su IP ya está en el rango corporativo autorizado | Control total de la infraestructura; alineado con políticas de red existentes | Overhead operativo: aprovisionamiento, mantenimiento, actualizaciones de software | Entornos con requisitos de seguridad estrictos o acceso a recursos sin exposición pública |

> [EXAMEN] Las soluciones más frecuentes en preguntas de examen son: (1) deshabilitar el allow list para Actions cuando la pregunta pide la solución más sencilla sin cambiar de runner, y (2) larger runners con IP estática cuando la pregunta pide mantener la allow list activa con GitHub-hosted runners.

## Buenas y malas prácticas

**Hacer:**
- Evaluar el nivel de exposición antes de deshabilitar el allow list para Actions — razón: esta opción es reversible pero afecta a toda la organización de inmediato.
- Documentar la IP estática del larger runner en el runbook de operaciones — razón: si la IP cambia (por ejemplo, al recrear el runner), habrá que actualizar la allow list manualmente.
- Usar self-hosted runners solo cuando los recursos internos no tienen exposición pública posible — razón: el overhead operativo solo se justifica si no existe una alternativa más sencilla.

**Evitar:**
- Intentar añadir manualmente los rangos CIDR de `api.github.com/meta` a la allow list — razón: GitHub actualiza estos rangos sin previo aviso y el mantenimiento manual es inviable en producción.
- Asumir que deshabilitar el allow list para Actions es permanentemente seguro — razón: cualquier workflow con acceso a `write` en la organización puede usar esa exención para acceder a recursos internos.

## Verificación y práctica

**Pregunta 1:** Una organización tiene la IP allow list activada. Un workflow falla al intentar hacer push a un registro de paquetes privado de la organización. ¿Cuál es la causa más probable y cuál es la solución más inmediata?

**Respuesta:** La causa es que el GitHub-hosted runner usa una IP dinámica no incluida en la allow list, por lo que la conexión al registro de paquetes es rechazada. La solución más inmediata es deshabilitar la IP allow list para GitHub Actions desde Settings > Security > IP allow list. Si se necesita mantener la allow list activa, la alternativa es migrar a larger runners con IP estática o usar self-hosted runners en la red corporativa.

---

**Pregunta 2:** ¿Qué devuelve el campo `actions` del endpoint `GET https://api.github.com/meta`?

**Respuesta:** Devuelve la lista de rangos CIDR (notación de red con máscara) que GitHub usa actualmente para los GitHub-hosted runners. Esta lista es pública y no requiere autenticación, pero cambia con frecuencia, por lo que no es práctico usarla como fuente de verdad para una IP allow list corporativa mantenida manualmente.

---

**Pregunta 3:** Un equipo quiere usar GitHub-hosted runners pero necesita mantener la IP allow list activa para cumplir con políticas de auditoría. ¿Qué solución es la más adecuada?

**Respuesta:** Usar larger runners con IP estática asignada a la organización. Esta solución permite mantener la IP allow list completamente activa (incluyendo para GitHub Actions), añadiendo solo la IP estática del larger runner a la lista. Los runners estándar no tienen IP fija, por lo que no son válidos para este caso.

---

← [4.3.2 Permisos GITHUB_TOKEN y retención](gha-d4-politicas-token-retencion.md) | [Índice](README.md) | [4.5.1 Runner Groups: creación](gha-d4-runner-groups-creacion.md) →
