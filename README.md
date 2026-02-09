# 🚀 GitHub Actions & Git - Proyecto de Aprendizaje Completo AB

Este repositorio es una **guía completa y práctica** que cubre:
- **Git**: Funcionamiento interno, arquitectura y principios
- **GitHub Actions**: Desde fundamentos técnicos hasta ejemplos avanzados del mundo real

## 📚 Contenido del Repositorio

### 🎯 Ruta de Aprendizaje Recomendada

```

   ↓
2️⃣ GITHUB ACTIONS - ARQUITECTURA
   ↓
3️⃣ GITHUB ACTIONS - CONCEPTOS
   ↓

```

---

## PARTE I: FUNDAMENTOS DE GIT

### 📖 **[GIT_FUNCIONAMIENTO_INTERNO.md](./GIT_FUNCIONAMIENTO_INTERNO.md)** ⭐ **EMPEZAR AQUÍ**

**¿Por qué leer esto primero?**
GitHub Actions trabaja directamente con Git (commits, branches, tags, refs). Entender Git te permite usar GitHub Actions con confianza y crear workflows avanzados.

**Nivel**: Fundamentos técnicos explicados en profundidad
**Tiempo**: 2-3 horas de lectura
**Prerequisitos**: Ninguno (explica desde cero)

---

## PARTE II: GITHUB ACTIONS - DOCUMENTACIÓN TÉCNICA

### 1. **[GITHUB_ACTIONS_ARQUITECTURA_TECNICA.md](./GITHUB_ACTIONS_ARQUITECTURA_TECNICA.md)** ⭐ **LECTURA ESENCIAL**

bababab
cambio

**Nivel**: Arquitectura y funcionamiento interno
**Tiempo**: 2-3 horas de lectura
**Prerequisitos**: Haber leído GIT_FUNCIONAMIENTO_INTERNO.md

### 2. **[GITHUB_ACTIONS_CONTEXTOS.md](./GITHUB_ACTIONS_CONTEXTOS.md)**

**Contenido:**
remove

**Nivel**: Referencia técnica
**Tiempo**: 1 hora de consulta

### 3. **[GITHUB_ACTIONS_EVENTOS.md](./GITHUB_ACTIONS_EVENTOS.md)**

**Contenido:**
- Catálogo completo de eventos (triggers)
- Webhooks y cómo funcionan
- Eventos relacionados con Git (push, pull_request, create, delete)
- Ejemplos de cada tipo de evento

**Nivel**: Referencia técnica
**Tiempo**: 1 hora de consulta

### 4. **[GITHUB_ACTIONS_EXPRESIONES.md](./GITHUB_ACTIONS_EXPRESIONES.md)**

**Contenido:**
- Sintaxis de expresiones `${{ }}`
- Funciones disponibles
- Operadores y condicionales
- Acceso a contextos Git

**Nivel**: Referencia técnica
**Tiempo**: 30 minutos de consulta

### 5. **[GITHUB_ACTIONS_GUIA_COMPLETA.md](./GITHUB_ACTIONS_GUIA_COMPLETA.md)**

**Contenido:**
- Guía general de uso
- Mejores prácticas
- Tips y trucos
- Troubleshooting

**Nivel**: Guía práctica
**Tiempo**: 1 hora de lectura

---

## PARTE III: EJEMPLOS PRÁCTICOS EJECUTABLES

Ver **[EJEMPLOS_AVANZADOS_README.md](./EJEMPLOS_AVANZADOS_README.md)** para documentación completa.

### 🎮 Demo Interactiva
- **[00-demo-completa.yml](./.github/workflows/00-demo-completa.yml)** - 🌟 Demo interactiva de TODAS las capacidades

### 📦 Ejemplos por Categoría

1. **[01-compartir-datos.yml](./.github/workflows/01-compartir-datos.yml)**
   - Compartir datos entre steps y jobs
   - GITHUB_OUTPUT, GITHUB_ENV, GITHUB_PATH
   - Artifacts y job outputs
   - Matrices dinámicas

2. **[02-reusable-workflow.yml](./.github/workflows/02-reusable-workflow.yml)**
   - Workflow reusable con `workflow_call`
   - Inputs, outputs, y secrets
   - Validaciones y health checks

3. **[03-caller-workflow.yml](./.github/workflows/03-caller-workflow.yml)**
   - Llamar workflows reusables
   - Orquestación de múltiples deployments
   - Despliegues secuenciales con aprobaciones

4. **[04-cicd-completo.yml](./.github/workflows/04-cicd-completo.yml)**
   - Pipeline CI/CD completo profesional
   - Lint → Test → Build → Deploy
   - Multi-plataforma, multi-versión
   - Services (PostgreSQL, Redis)
   - Containers y environments

5. **[05-composite-actions.yml](./.github/workflows/05-composite-actions.yml)**
   - Crear composite actions personalizadas
   - Reutilización de lógica
   - Actions con inputs y outputs

6. **[06-cache-optimization.yml](./.github/workflows/06-cache-optimization.yml)**
   - Estrategias de cache
   - Multi-lenguaje (Python, Node, Go, Rust)
   - Cache incremental y fallback
   - Optimización de performance

7. **[07-secrets-security.yml](./.github/workflows/07-secrets-security.yml)**
   - Manejo seguro de secretos
   - Variables por entorno
   - Credenciales externas (AWS, Docker, SSH)
   - Security audit

8. **[08-dynamic-matrices.yml](./.github/workflows/08-dynamic-matrices.yml)**
   - Matrices estáticas y dinámicas
   - Include/Exclude
   - Matrices anidadas
   - Estrategias avanzadas

---

## 🎓 Cómo Usar Este Repositorio

### 🚀 Quick Start (Ruta Rápida)

```bash
# 1. Clonar el repositorio
git clone <tu-repo>
cd testsWithGitHubAction

# 2. EMPEZAR POR AQUÍ: Leer fundamentos de Git
cat GIT_FUNCIONAMIENTO_INTERNO.md
# Tiempo: 2-3 horas
# Por qué: Entender Git es fundamental para GitHub Actions

# 3. Leer arquitectura de GitHub Actions
cat GITHUB_ACTIONS_ARQUITECTURA_TECNICA.md
# Tiempo: 2-3 horas
# Por qué: Entender cómo funciona GitHub Actions internamente

# 4. Ver los workflows disponibles
ls -la .github/workflows/

# 5. Ejecutar la demo completa
gh workflow run "00 - DEMO COMPLETA" -f demo-mode=quick

# 6. Ver los resultados
gh run list
gh run view --log
```

### 📚 Ruta de Aprendizaje Completa

#### **FASE 1: FUNDAMENTOS (Semana 1)**

**Día 1-2: Git Interno**
1. 📖 Leer `GIT_FUNCIONAMIENTO_INTERNO.md` (completo)
   - Entender objetos, grafo, referencias
   - Comprender las tres áreas
   - Saber qué hacen los comandos internamente

**Día 3-4: GitHub Actions - Arquitectura**
2. 📖 Leer `GITHUB_ACTIONS_ARQUITECTURA_TECNICA.md` (completo)
   - Arquitectura de runners
   - Ciclo de vida de workflows
   - Integración con Git

**Día 5: Primeros Workflows**
3. 🎮 Ejecutar `00-demo-completa.yml` (modo quick)
4. 📦 Ejecutar `01-compartir-datos.yml`
5. 📖 Consultar `GITHUB_ACTIONS_CONTEXTOS.md` (según necesidad)

#### **FASE 2: INTERMEDIO (Semana 2)**

**Workflows Reusables:**
6. 🔄 Ejecutar `02-reusable-workflow.yml` y `03-caller-workflow.yml`
   - Entender workflow_call
   - Ver cómo reutilizar lógica

**Matrices y Optimización:**
7. 📊 Ejecutar `08-dynamic-matrices.yml`
   - Matrices estáticas y dinámicas
   - Paralelización
8. 💾 Ejecutar `06-cache-optimization.yml`
   - Estrategias de cache
   - Optimización de tiempo

**Referencias:**
9. 📖 Consultar `GITHUB_ACTIONS_EVENTOS.md`
10. 📖 Consultar `GITHUB_ACTIONS_EXPRESIONES.md`

#### **FASE 3: AVANZADO (Semana 3)**

**CI/CD Profesional:**
11. 🚀 Estudiar y ejecutar `04-cicd-completo.yml`
    - Pipeline completo
    - Multi-entorno
    - Services y containers

**Customización:**
12. 🔧 Ejecutar `05-composite-actions.yml`
    - Crear tus propias actions
    - Reutilización avanzada

**Seguridad:**
13. 🔐 Ejecutar `07-secrets-security.yml`
    - Manejo de secretos
    - Security best practices

**Práctica:**
14. 📖 Leer `EJEMPLOS_AVANZADOS_README.md` (completo)
15. 📖 Consultar `GITHUB_ACTIONS_GUIA_COMPLETA.md` (mejores prácticas)

---

## 🎯 Qué Aprenderás

### 📘 Parte I: Fundamentos Git

**Arquitectura Interna:**
- ✅ Sistema de objetos (blob, tree, commit, tag)
- ✅ Content-addressable storage
- ✅ Grafo de commits (DAG)
- ✅ Sistema de referencias (branches, tags, HEAD)
- ✅ Las tres áreas (working, staging, repository)

**Funcionamiento:**
- ✅ Cómo Git almacena datos
- ✅ Qué hacen los comandos internamente
- ✅ Por qué Git es tan eficiente
- ✅ Cómo recuperar de errores

**Integración CI/CD:**
- ✅ Git en GitHub Actions
- ✅ Variables de contexto Git (github.sha, github.ref)
- ✅ actions/checkout internamente
- ✅ Shallow clone vs historia completa

### 🔧 Parte II: GitHub Actions

**Arquitectura:**
- ✅ Runners (self-hosted y GitHub-hosted)
- ✅ Orchestration y execution
- ✅ Ciclo de vida de workflows
- ✅ Eventos y triggers
- ✅ Contextos y expresiones

**Capacidades Avanzadas:**
- ✅ Compartir datos entre steps y jobs
- ✅ Workflows reusables (`workflow_call`)
- ✅ Composite Actions personalizadas
- ✅ Matrices dinámicas y estáticas
- ✅ Cache y optimización
- ✅ Manejo seguro de secretos
- ✅ Environments y deployments
- ✅ Services y containers
- ✅ Artifacts y packages

**Mejores Prácticas:**
- ✅ Seguridad (secretos, permisos, audit)
- ✅ Performance (cache, paralelización)
- ✅ Reutilización (workflows reusables, composite actions)
- ✅ Debugging y troubleshooting
- ✅ CI/CD patterns profesionales

---

## 🎯 Rutas de Aprendizaje por Perfil

### 👨‍💻 Para Desarrolladores Backend/Frontend

**Objetivo**: Entender CI/CD para tus proyectos

```
1. GIT_FUNCIONAMIENTO_INTERNO.md (solo secciones 1-7)
2. GITHUB_ACTIONS_ARQUITECTURA_TECNICA.md (secciones 1-5)
3. 01-compartir-datos.yml (ejecutar)
4. 04-cicd-completo.yml (estudiar y adaptar)
5. 06-cache-optimization.yml (tu lenguaje)
```

**Tiempo**: 1 semana
**Resultado**: Puedes crear pipelines CI/CD para tus proyectos

### 🏗️ Para DevOps Engineers

**Objetivo**: Dominar GitHub Actions para infraestructura

```
1. GIT_FUNCIONAMIENTO_INTERNO.md (completo)
2. GITHUB_ACTIONS_ARQUITECTURA_TECNICA.md (completo)
3. Todos los workflows (ejecutar y estudiar)
4. GITHUB_ACTIONS_CONTEXTOS.md (completo)
5. EJEMPLOS_AVANZADOS_README.md (completo)
```

**Tiempo**: 3 semanas
**Resultado**: Experto en GitHub Actions y Git

### 📚 Para Aprendices / Estudiantes

**Objetivo**: Aprender desde cero

```
1. GIT_FUNCIONAMIENTO_INTERNO.md (leer despacio, secciones 1-5 primero)
2. Practicar comandos Git localmente
3. GITHUB_ACTIONS_ARQUITECTURA_TECNICA.md (secciones 1-3)
4. 00-demo-completa.yml (modo quick)
5. 01-compartir-datos.yml
6. Ir avanzando según el orden de FASE 1, 2, 3
```

**Tiempo**: 1 mes
**Resultado**: Fundamentos sólidos de Git y GitHub Actions

### 🚀 Para Arquitectos de Software

**Objetivo**: Diseñar estrategias de CI/CD

```
1. GIT_FUNCIONAMIENTO_INTERNO.md (enfoque en secciones 5, 8, 10)
2. GITHUB_ACTIONS_ARQUITECTURA_TECNICA.md (completo, enfoque arquitectura)
3. 02-reusable-workflow.yml + 03-caller-workflow.yml (patterns)
4. 04-cicd-completo.yml (pipeline patterns)
5. GITHUB_ACTIONS_GUIA_COMPLETA.md (mejores prácticas)
```

**Tiempo**: 1 semana
**Resultado**: Diseñar arquitecturas de CI/CD escalables

---

## 🎮 Cómo Ejecutar los Workflows

### Opción 1: GitHub UI (Recomendado para principiantes)

```
1. Ve a la pestaña "Actions" en GitHub
2. Selecciona un workflow del menú izquierdo
3. Click en "Run workflow" (botón verde)
4. Llena los inputs si el workflow los pide
5. Click "Run workflow"
6. Ve los logs en tiempo real
```

### Opción 2: GitHub CLI (Recomendado para avanzados)

```bash
# Listar workflows disponibles
gh workflow list

# Ejecutar workflow
gh workflow run "00 - DEMO COMPLETA" -f demo-mode=quick

# Ver runs en ejecución
gh run list

# Ver logs de un run
gh run view --log

# Ver logs de un run específico
gh run view 123456789 --log

# Esperar a que termine y ver resultado
gh run watch
```

### Opción 3: API (Para integración)

```bash
# Disparar workflow vía API
curl -X POST \
  -H "Accept: application/vnd.github+json" \
  -H "Authorization: Bearer $GITHUB_TOKEN" \
  https://api.github.com/repos/OWNER/REPO/actions/workflows/WORKFLOW_ID/dispatches \
  -d '{"ref":"main","inputs":{"demo-mode":"quick"}}'
```

---

## 📊 Estadísticas del Proyecto

### 📚 Documentación
- **6 documentos técnicos** completos y profundos
  - `GIT_FUNCIONAMIENTO_INTERNO.md` (2,896 líneas - ~73,835 caracteres)
  - `GITHUB_ACTIONS_ARQUITECTURA_TECNICA.md`
  - `GITHUB_ACTIONS_CONTEXTOS.md`
  - `GITHUB_ACTIONS_EVENTOS.md`
  - `GITHUB_ACTIONS_EXPRESIONES.md`
  - `GITHUB_ACTIONS_GUIA_COMPLETA.md`

### 🎯 Ejemplos Prácticos
- **9 workflows ejecutables** con ejemplos reales
- **100% funcional** y probado
- **Más de 2,500 líneas** de código YAML documentado

### 📖 Cobertura
- ✅ **Git completo**: Desde objetos hasta integración CI/CD
- ✅ **GitHub Actions completo**: Desde arquitectura hasta ejemplos avanzados
- ✅ **Todos los conceptos**: Sin vacíos de conocimiento

### 🎓 Valor Educativo
- **~6-8 horas** de lectura de fundamentos
- **~2-4 horas** de práctica con workflows
- **De principiante a avanzado** en 3 semanas

---

## 🗺️ Mapa del Repositorio

```
testsWithGitHubAction/
│
├── 📘 FUNDAMENTOS GIT
│   └── GIT_FUNCIONAMIENTO_INTERNO.md          ⭐ EMPEZAR AQUÍ
│
├── 📗 GITHUB ACTIONS - TEORÍA
│   ├── GITHUB_ACTIONS_ARQUITECTURA_TECNICA.md ⭐ LEER SEGUNDO
│   ├── GITHUB_ACTIONS_CONTEXTOS.md            📖 Referencia
│   ├── GITHUB_ACTIONS_EVENTOS.md              📖 Referencia
│   ├── GITHUB_ACTIONS_EXPRESIONES.md          📖 Referencia
│   └── GITHUB_ACTIONS_GUIA_COMPLETA.md        📖 Guía práctica
│
├── 🎯 EJEMPLOS PRÁCTICOS
│   ├── EJEMPLOS_AVANZADOS_README.md           📚 Índice de ejemplos
│   ├── RESUMEN_EJEMPLOS.md                    📝 Resumen
│   └── .github/workflows/
│       ├── 00-demo-completa.yml               🎮 Demo interactiva
│       ├── 01-compartir-datos.yml             📦 Compartir datos
│       ├── 02-reusable-workflow.yml           🔄 Workflow reusable
│       ├── 03-caller-workflow.yml             📞 Llamar workflows
│       ├── 04-cicd-completo.yml               🚀 Pipeline completo
│       ├── 05-composite-actions.yml           🔧 Actions custom
│       ├── 06-cache-optimization.yml          💾 Optimización
│       ├── 07-secrets-security.yml            🔐 Seguridad
│       └── 08-dynamic-matrices.yml            📊 Matrices
│
├── 🛠️ HERRAMIENTAS
│   ├── regenerate-workflows.sh                🔄 Regenerar workflows
│   └── test.py                                🧪 Tests
│
└── 📄 DOCUMENTOS BASE
    ├── README.md                              👈 Estás aquí
    ├── GUIA_RAPIDA.md                         ⚡ Quick reference
    └── README_GITHUB_ACTIONS.md               📋 Otro índice
```

---

## 💡 Tips para Aprender Efectivamente

### 📚 Para la Lectura

1. **No te saltes Git**: Aunque quieras ir directo a GitHub Actions, entender Git es fundamental
2. **Lee las introducciones completas**: Cada sección tiene una introducción extensa que explica el "por qué"
3. **Toma notas**: Los conceptos se relacionan entre sí
4. **Haz pausas**: Son documentos densos, tómate tu tiempo

### 🎮 Para la Práctica

1. **Ejecuta cada workflow**: No solo leas, ejecuta
2. **Modifica los ejemplos**: Cambia valores, añade steps
3. **Lee los logs**: GitHub Actions muestra todo lo que hace
4. **Experimenta**: Romper cosas es parte del aprendizaje

### 🔄 Para Reforzar

1. **Vuelve a la teoría**: Después de practicar, relee conceptos
2. **Crea tus propios workflows**: Aplica lo aprendido a tus proyectos
3. **Documenta tu aprendizaje**: Escribe lo que entendiste
4. **Enseña a otros**: La mejor forma de consolidar conocimiento

---

## 🎯 Objetivos de Aprendizaje

### Al terminar este repositorio, serás capaz de:

#### 🔧 Git Interno
- [ ] Explicar cómo Git almacena objetos (blob, tree, commit)
- [ ] Entender el grafo de commits y cómo navegarlo
- [ ] Saber qué hace cada comando Git internamente
- [ ] Comprender las tres áreas de Git
- [ ] Usar Git con confianza en CI/CD

#### 🚀 GitHub Actions
- [ ] Diseñar workflows desde cero
- [ ] Entender la arquitectura de runners
- [ ] Usar contextos y expresiones avanzadas
- [ ] Crear workflows reusables
- [ ] Optimizar pipelines con cache
- [ ] Manejar secretos de forma segura
- [ ] Implementar matrices dinámicas
- [ ] Crear composite actions personalizadas
- [ ] Debuggear workflows fallidos
- [ ] Diseñar estrategias de CI/CD completas

#### 💼 Profesional
- [ ] Implementar CI/CD en proyectos reales
- [ ] Optimizar tiempos de build
- [ ] Diseñar arquitecturas de deployment
- [ ] Seguir mejores prácticas de seguridad
- [ ] Documentar workflows para tu equipo

---

## 🤝 Contribuir

Este es un proyecto de aprendizaje en constante evolución.

### 💡 Ideas para Contribuir
- Reportar errores en documentación
- Sugerir mejoras en ejemplos
- Añadir nuevos workflows de ejemplo
- Traducir documentación
- Mejorar explicaciones

### 📝 Cómo Contribuir
```bash
# 1. Fork el repositorio
# 2. Crea una rama para tu feature
git checkout -b feature/mejora-documentacion

# 3. Haz tus cambios
# 4. Commit con mensaje descriptivo
git commit -m "docs: mejorar explicación de staging area"

# 5. Push a tu fork
git push origin feature/mejora-documentacion

# 6. Crea un Pull Request
```

---

## 📚 Referencias y Recursos Externos

### Git
- [Pro Git Book (Español)](https://git-scm.com/book/es/v2)
- [Git Documentation](https://git-scm.com/doc)
- [Git Internals (Video)](https://www.youtube.com/watch?v=P6jD966jzlk)

### GitHub Actions
- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Workflow Syntax Reference](https://docs.github.com/en/actions/using-workflows/workflow-syntax-for-github-actions)
- [GitHub Actions Marketplace](https://github.com/marketplace?type=actions)
- [Awesome Actions](https://github.com/sdras/awesome-actions)

### CI/CD
- [CI/CD Best Practices](https://www.atlassian.com/continuous-delivery/principles/continuous-integration-vs-delivery-vs-deployment)
- [The Twelve-Factor App](https://12factor.net/)

---

## ⭐ Puntos Destacados

### 🌟 Para Empezar (Si tienes poco tiempo)
```
1. GIT_FUNCIONAMIENTO_INTERNO.md (secciones 1-3, 5)
   Tiempo: 1 hora
   
2. GITHUB_ACTIONS_ARQUITECTURA_TECNICA.md (secciones 1-3)
   Tiempo: 1 hora
   
3. 00-demo-completa.yml (modo quick)
   Tiempo: 15 minutos
   
Total: ~2.5 horas para bases sólidas
```

### 🔥 Para Ir Directo a Práctica (Si ya conoces teoría)
```
1. 01-compartir-datos.yml       (fundamentos)
2. 04-cicd-completo.yml         (pipeline completo)
3. 02+03-reusable-workflow.yml  (reutilización)
4. 08-dynamic-matrices.yml      (paralelización)

Total: ~2 horas de práctica intensiva
```

### 💎 Los Mejores Recursos del Repo
1. `GIT_FUNCIONAMIENTO_INTERNO.md` - 📘 Libro completo de Git
2. `GITHUB_ACTIONS_ARQUITECTURA_TECNICA.md` - 📗 Libro completo de Actions
3. `04-cicd-completo.yml` - 🚀 Pipeline profesional real
4. `EJEMPLOS_AVANZADOS_README.md` - 📚 Guía práctica detallada

---

## 🎉 ¡Comienza Tu Viaje!

```bash
# Paso 1: Clona el repo
git clone <tu-repo>
cd testsWithGitHubAction

# Paso 2: Abre el primer documento
cat GIT_FUNCIONAMIENTO_INTERNO.md

# Paso 3: ¡Empieza a aprender!
```

---

## 📞 Soporte y Preguntas

- 💬 **Issues**: Para reportar problemas o hacer preguntas
- 📧 **Discussions**: Para discusiones generales sobre aprendizaje
- ⭐ **Star el repo**: Si te resulta útil

---

**🎓 De principiante a experto en Git y GitHub Actions**

*Este repositorio te llevará desde entender cómo Git almacena un blob hasta diseñar arquitecturas de CI/CD empresariales con GitHub Actions.*

**📍 Recuerda**: Empieza con `GIT_FUNCIONAMIENTO_INTERNO.md`. Todo lo demás se construye sobre esa base.

---

**Última actualización**: Febrero 2026
**Mantenedor**: [Tu nombre]
**Licencia**: MIT
