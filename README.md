# Índice — GitHub Actions: fundamentos y preparación para la certificación GH-200

## Mapa de módulos

| Tema | Módulo | JSON |
|------|--------|------|
| 1 | D1 — Author and manage workflows (20–25%) | dominio-gha-d1.json |
| 2 | D2 — Consume and troubleshoot workflows (15–20%) | dominio-gha-d2.json |
| 3 | D3 — Author and maintain actions (15–20%) | dominio-gha-d3.json |
| 4 | D4 — Manage GitHub Actions for the enterprise (20–25%) | dominio-gha-d4.json |
| 5 | D5 — Secure and optimize automation (10–15%) | dominio-gha-d5.json |

---

## Verificación de cobertura

1. **¿Están representados todos los sub-dominios?**
   Sí. Los 5 dominios del temario GH-200 (enero 2026) están representados: D1 (20–25%), D2 (15–20%), D3 (15–20%), D4 (20–25%), D5 (10–15%). Cada nodo hoja proviene del análisis dominio-*.json correspondiente.

2. **¿Falta algún tema estándar según la fuente autoritativa?**
   No. Todos los tópicos del Study Guide GH-200 están cubiertos: triggers, jobs/steps, contextos/expresiones, caché/artefactos, environments (D1); troubleshooting/logs, matrix, reutilización (D2); tipos de action, action.yml, versionado, marketplace (D3); políticas org, runner groups, self-hosted runners, secrets/variables enterprise (D4); script injection, GITHUB_TOKEN, OIDC, SHA pinning, attestations, optimización (D5). Los temas "Fuera del alcance" (GitHub Advanced Security, Copilot, Administration como disciplina) no aparecen.

3. **¿El orden tiene sentido didáctico?**
   Sí. El orden sigue la estrategia de ordenación de meta.md: anatomía del workflow → triggers → jobs/steps → orquestación → contextos/expresiones → persistencia → security (D1); troubleshooting/consumo (D2); autoría de actions (D3); gestión enterprise (D4); seguridad avanzada y optimización (D5). Cada tema se comprende con los conocimientos aportados por los anteriores.

4. **¿Los nodos hoja son cohesivos?**
   Sí. Los splits detectados en el check (C5, C9 en D1; C3, C8 en D2; A1, A3, A5, A6, A9 en D4; Script Injection, GITHUB_TOKEN, OIDC, PIN, Attestations, Caché en D5) están reflejados como nodos intermedios agrupadores. Cada fichero cubre exactamente un tema.

5. **¿Hay nombres de fichero repetidos?**
   No. Revisión de los 73 ficheros hoja: todos son únicos. Posibles colisiones revisadas: `gha-d1-artefactos.md` ≠ `gha-d2-artefactos.md` (perspectivas distintas: subida vs. consumo). `gha-d1-environment-protections.md` ≠ `gha-environment-protections-security.md` (D1 configura, D5 analiza el riesgo). No se detectan duplicados.

Cobertura verificada: no se detectan lagunas.

## GitHub Actions — Guía de estudio GH-200

### 1. D1 — Author and manage workflows (20–25%)

- [1.1 Estructura raíz del workflow y YAML anchors](gha-d1-estructura-workflow.md)
- [1.2 Triggers de código y filtros (push, pull_request, pull_request_target)](gha-d1-triggers-codigo.md)
- [1.3 Triggers de automatización (schedule, workflow_dispatch, workflow_call, workflow_run)](gha-d1-triggers-automatizacion.md)
- [1.4 Triggers de eventos del repositorio](gha-d1-triggers-eventos.md)
- 1.5 Configuración de jobs
    - [1.5.1 Propiedades de identidad, entorno y salidas](gha-d1-jobs-identidad.md)
    - [1.5.2 Propiedades de control de flujo](gha-d1-jobs-control-flujo.md)
- [1.6 Configuración de steps](gha-d1-steps-configuracion.md)
- [1.7 Condicionales y funciones de estado](gha-d1-condicionales.md)
- [1.8 Dependencias entre jobs y propagación de outputs](gha-d1-dependencias-jobs.md)
- 1.9 Service containers y container jobs
    - [1.9.1 Service containers: sintaxis, imagen, ports, env y health-check](gha-d1-service-containers.md)
    - [1.9.2 Container jobs y networking Docker](gha-d1-container-jobs.md)
- [1.10 Matrix strategy](gha-d1-matrix-strategy.md)
- [1.11 Contextos de workflow y ambiente (github, runner, env, vars, job)](gha-d1-contextos-workflow.md)
- [1.12 Contextos de estado, datos y flujo (steps, needs, secrets, inputs, matrix)](gha-d1-contextos-estado.md)
- [1.13 Expresiones y funciones built-in](gha-d1-expresiones.md)
- [1.14 Caché de dependencias](gha-d1-cache.md)
- [1.15 Artefactos (upload, download, retención)](gha-d1-artefactos.md)
- [1.16 File commands del workflow (GITHUB_ENV, GITHUB_OUTPUT, GITHUB_STEP_SUMMARY, GITHUB_PATH)](gha-d1-file-commands.md)
- [1.17 Variables predefinidas del runner](gha-d1-variables-predefinidas.md)
- [1.18 Badges de estado del workflow](gha-d1-badges.md)
- [1.19 Environment protections y deployment environments](gha-d1-environment-protections.md)
- [1.20 Testing / Verificación de D1](gha-d1-testing.md)

### 2. D2 — Consume and troubleshoot workflows (15–20%)

- [2.1 Interpretación de triggers y UI de ejecución](gha-d2-triggers-ui.md)
- [2.2 Lectura e interpretación de logs](gha-d2-logs.md)
- 2.3 Diagnóstico y depuración de fallos
    - [2.3.1 Diagnóstico de comportamiento: exit codes, continue-on-error y timeout](gha-d2-diagnostico-comportamiento.md)
    - [2.3.2 Depuración avanzada: runner debug, step debug, matrix y concurrencia](gha-d2-debug-avanzado.md)
- [2.4 Re-ejecución de workflows y jobs](gha-d2-reejecutar.md)
- [2.5 Artefactos: consumo, gestión y API](gha-d2-artefactos.md)
- [2.6 Interpretación de matrix strategy en ejecuciones](gha-d2-matrix-interpretacion.md)
- [2.7 Starter workflows (workflow templates)](gha-d2-starter-workflows.md)
- 2.8 Reusable workflows
    - [2.8.1 Reusable workflows: sintaxis de llamada, inputs, secrets y outputs](gha-d2-reusable-workflows-consumo.md)
    - [2.8.2 Reusable workflows: restricciones, contextos y comparativa con otros patrones](gha-d2-reusable-workflows-avanzado.md)
- [2.9 Composite actions: consumo y diferencias con reusable workflows](gha-d2-composite-actions-consumo.md)
- [2.10 Deshabilitar y eliminar workflows](gha-d2-deshabilitar-workflows.md)
- [2.11 API REST de GitHub Actions para ejecuciones y logs](gha-d2-api-rest-ejecuciones.md)
- [2.12 Testing / Verificación de D2](gha-d2-testing.md)

### 3. D3 — Author and maintain actions (15–20%)

- [3.1 Tipos de GitHub Actions (JavaScript, Docker, Composite)](gha-action-tipos.md)
- [3.2 El fichero action.yml: estructura y metadatos](gha-action-yml.md)
- [3.3 Inputs y outputs de una action](gha-action-inputs-outputs.md)
- [3.4 Workflow commands dentro de actions](gha-action-workflow-commands.md)
- [3.5 Version pinning e immutable actions](gha-action-version-pinning.md)
- [3.6 Distribución de actions (pública, privada, interna)](gha-action-distribucion.md)
- [3.7 Publicación en el GitHub Marketplace y release strategies](gha-action-publicacion-marketplace.md)
- [3.8 Testing / Verificación de D3](gha-d3-testing.md)

### 4. D4 — Manage GitHub Actions for the enterprise (20–25%)

- [4.1 Workflow Templates (Starter Workflows) para la organización](gha-d4-workflow-templates.md)
- 4.2 Reusable Workflows
    - [4.2.1 Autoría: on workflow_call, inputs, outputs y secrets](gha-d4-reusable-workflows-autoria.md)
    - [4.2.2 Consumo: secrets inherit, anidamiento y visibilidad cross-org](gha-d4-reusable-workflows-consumo.md)
- 4.3 Políticas de organización
    - [4.3.1 Allow-list de actions y fork PR policies](gha-d4-politicas-allow-list.md)
    - [4.3.2 Permisos predeterminados de GITHUB_TOKEN y retención de datos](gha-d4-politicas-token-retencion.md)
- [4.4 IP Allow Lists y GitHub-Hosted Runners](gha-d4-ip-allow-lists.md)
- 4.5 Runner Groups
    - [4.5.1 Creación, niveles org/enterprise y políticas de acceso](gha-d4-runner-groups-creacion.md)
    - [4.5.2 Asignación de repositorios, gestión de runners y ciclo de vida](gha-d4-runner-groups-asignacion.md)
- [4.6 Preinstalled Software en GitHub-Hosted Runners](gha-d4-preinstalled-software.md)
- 4.7 Self-Hosted Runners
    - [4.7.1 Registro, configuración inicial y labels](gha-d4-self-hosted-runners-registro.md)
    - [4.7.2 Ciclo de vida, jerarquía de visibilidad y seguridad](gha-d4-self-hosted-runners-seguridad.md)
- [4.8 Actions Runner Controller (ARC) y Scale Sets](gha-d4-arc-scale-sets.md)
- 4.9 Gestión de Secrets
    - [4.9.1 Secrets: niveles, precedencia, visibilidad y gestión via UI](gha-d4-secrets-ui.md)
    - [4.9.2 Secrets via REST API: endpoints, autenticación y cifrado](gha-d4-secrets-api.md)
- [4.10 Variables de Configuración (Configuration Variables)](gha-d4-variables-configuracion.md)
- [4.11 REST API para Gestión de GitHub Actions (Enterprise)](gha-d4-api-rest-enterprise.md)
- [4.12 Auditoría y Gobernanza de Actions en la organización](gha-d4-auditoria-gobernanza.md)
- [4.13 Testing / Verificación de D4](gha-d4-testing.md)

### 5. D5 — Secure and optimize automation (10–15%)

- [5.1 Environment Protections y Approval Gates](gha-environment-protections-security.md)
- 5.2 Script Injection
    - [5.2.1 Script Injection — Vectores y Anatomía del Ataque](gha-script-injection-vectores.md)
    - [5.2.2 Script Injection — Mitigación y Defensa en Profundidad](gha-script-injection-mitigacion.md)
- 5.3 GITHUB_TOKEN
    - [5.3.1 GITHUB_TOKEN — Naturaleza, Lifecycle y Scope](gha-github-token-lifecycle.md)
    - [5.3.2 GITHUB_TOKEN — Configuración de Permisos Granulares](gha-github-token-permisos.md)
- 5.4 OIDC para Federación Cloud
    - [5.4.1 OIDC — Fundamentos, Claims y Flujo](gha-oidc-fundamentos-claims.md)
    - [5.4.2 OIDC — Configuración por Cloud Provider y Troubleshooting](gha-oidc-cloud-providers.md)
- 5.5 Pin de Actions a SHA
    - [5.5.1 Pin de Actions a SHA — Conceptos y Riesgos](gha-pin-actions-sha-conceptos.md)
    - [5.5.2 Pin de Actions a SHA — Dependabot y Políticas de Organización](gha-pin-actions-sha-dependabot.md)
- [5.6 Enforcement de Policies de Seguridad](gha-enforcement-policies-seguridad.md)
- 5.7 Artifact Attestations y SLSA Provenance
    - [5.7.1 SLSA Provenance — Fundamentos y Niveles](gha-slsa-provenance-fundamentos.md)
    - [5.7.2 Artifact Attestations — Generación y Verificación](gha-artifact-attestations-uso.md)
- 5.8 Caché y Retención para Optimización de Costes
    - [5.8.1 Cache Strategies e Invalidación](gha-cache-strategies.md)
    - [5.8.2 Retention Policies, Costes y Seguridad de Caché](gha-retention-costes-seguridad.md)
- [5.9 Testing / Verificación de D5](gha-d5-testing.md)

## Tabla resumen

| Nro | Tema | Ficheros hoja | Líneas estimadas/fichero |
|-----|------|:-------------:|:------------------------:|
| 1 | D1 — Author and manage workflows (20–25%) | 22 | ~300–500 |
| 2 | D2 — Consume and troubleshoot workflows (15–20%) | 14 | ~300–450 |
| 3 | D3 — Author and maintain actions (15–20%) | 8 | ~300–450 |
| 4 | D4 — Manage GitHub Actions for the enterprise (20–25%) | 18 | ~300–480 |
| 5 | D5 — Secure and optimize automation (10–15%) | 15 | ~300–480 |
| **Total** | | **77** | |
