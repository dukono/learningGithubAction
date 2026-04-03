# Spring Cloud — Documentación Completa Profesional

> Documentación exhaustiva desde cero hasta nivel experto.
> Apta para pruebas técnicas empresariales y certificaciones.
> Cubre **Spring Boot 3.x / Spring Cloud 2023.x** + **Java 17/21**

---

## ÍNDICE GENERAL

---

### BLOQUE 0 — CONTEXTO Y FUNDAMENTOS PREVIOS
→ [00-contexto.md](./00-contexto.md) *(pendiente)*

**0.1 El problema de los sistemas distribuidos**
- 0.1.1 Monolito vs Microservicios: evolución histórica
- 0.1.2 Problemas que emergen al distribuir sistemas
- 0.1.3 Las 8 falacias de la computación distribuida
- 0.1.4 CAP Theorem: Consistencia, Disponibilidad, Tolerancia a particiones

**0.2 Qué es Spring Cloud**
- 0.2.1 Definición y propósito
- 0.2.2 Relación con Spring Boot y Spring Framework
- 0.2.3 Relación con Netflix OSS y otros proyectos
- 0.2.4 Spring Cloud vs soluciones nativas de Kubernetes

**0.3 Ecosistema y versiones**
- 0.3.1 Release Train: nomenclatura y compatibilidad
- 0.3.2 Módulos principales y opcionales
- 0.3.3 Estado actual: deprecaciones y reemplazos (Ribbon → LoadBalancer, Zuul → Gateway, etc.)

---

### BLOQUE 1 — FUNDAMENTOS Y ARQUITECTURA
→ [01-fundamentos.md](./01-fundamentos.md) | [02-arquitectura.md](./02-arquitectura.md)

Cubre: qué es Spring Cloud, historia, relación Boot/Framework, monolito vs microservicios, mapa de componentes, patrones, flujo de una petición.

---

### BLOQUE 2 — CONFIGURACIÓN CENTRALIZADA
→ [03-config.md](./03-config.md)

**1.1 El problema de la configuración en microservicios**
- 1.1.1 Por qué no sirve application.properties en un sistema distribuido
- 1.1.2 Requisitos de una solución de configuración centralizada

**1.2 Spring Cloud Config Server**
- 1.2.1 Arquitectura y funcionamiento interno
- 1.2.2 Backends soportados: Git, filesystem, Vault, JDBC
- 1.2.3 Configuración del servidor (`@EnableConfigServer`)
- 1.2.4 Estructura del repositorio de configuración
- 1.2.5 Perfiles y entornos: `application-{profile}.yml`
- 1.2.6 Seguridad del Config Server (HTTP Basic, SSL)
- 1.2.7 Cifrado y descifrado de propiedades sensibles

**1.3 Spring Cloud Config Client**
- 1.3.1 Dependencias y configuración del cliente
- 1.3.2 Bootstrap context vs Application context
- 1.3.3 `bootstrap.yml` vs `application.yml`
- 1.3.4 Orden de resolución de propiedades
- 1.3.5 Actualización dinámica con `@RefreshScope`
- 1.3.6 Spring Cloud Bus: propagación de cambios en masa

---

### BLOQUE 3 — DESCUBRIMIENTO DE SERVICIOS (SERVICE DISCOVERY)
→ [04-service-discovery.md](./04-service-discovery.md)

**2.1 El problema del descubrimiento**
- 2.1.1 Por qué las IPs dinámicas rompen los sistemas distribuidos
- 2.1.2 Client-Side Discovery vs Server-Side Discovery

**2.2 Eureka (Netflix)**
- 2.2.1 Arquitectura del Eureka Server
- 2.2.2 Registro y heartbeat de servicios
- 2.2.3 Configuración del Eureka Server (`@EnableEurekaServer`)
- 2.2.4 Configuración del Eureka Client (`@EnableDiscoveryClient`)
- 2.2.5 Self-preservation mode y zonas
- 2.2.6 Alta disponibilidad: clustering de Eureka
- 2.2.7 Dashboard web de Eureka

**2.3 Consul / Zookeeper / Kubernetes**
- Diferencias con Eureka, health checks nativos, cuándo usar cada uno

---

### BLOQUE 4 — BALANCEO DE CARGA (LOAD BALANCING)
→ [05-load-balancing.md](./05-load-balancing.md)

**3.1 El problema del balanceo en el cliente**
- 3.1.1 Hardware Load Balancer vs Software Load Balancer
- 3.1.2 Client-side load balancing: ventajas y desventajas

**3.2 Spring Cloud LoadBalancer (actual)**
- 3.2.1 Arquitectura y extensibilidad
- 3.2.2 Algoritmos: Round Robin, Random
- 3.2.3 Integración con RestTemplate y WebClient
- 3.2.4 Caché de instancias y TTL
- 3.2.5 Zonas y afinidad de zona

**3.3 Ribbon (deprecado — importante para exámenes)**

---

### BLOQUE 5 — COMUNICACIÓN ENTRE SERVICIOS
→ [08-comunicacion.md](./08-comunicacion.md)

**4.1 RestTemplate con balanceo** — `@LoadBalanced`

**4.2 OpenFeign**
- 4.2.1 Qué es Feign y el patrón declarativo HTTP
- 4.2.2 Configuración de Spring Cloud OpenFeign
- 4.2.3 `@FeignClient`: definición y parámetros
- 4.2.4 Interceptores, encoders y decoders
- 4.2.5 Manejo de errores con `ErrorDecoder`
- 4.2.6 Timeout y reintentos en Feign
- 4.2.7 Feign + Circuit Breaker

**4.3 WebClient reactivo** — modelo reactivo, `@LoadBalanced WebClient`

**4.4 Spring Cloud Stream** — mensajería con Kafka/RabbitMQ, binders, functional model

**4.5 Spring Cloud Bus** — propagación de eventos, refresh masivo

---

### BLOQUE 6 — RESILIENCIA Y TOLERANCIA A FALLOS
→ [07-circuit-breaker.md](./07-circuit-breaker.md)

**5.1 El problema de las fallas en cascada**
- 5.1.1 Cascading failures y "death star" architecture
- 5.1.2 Bulkhead, Timeout, Retry, Circuit Breaker, Fallback patterns

**5.2 Resilience4j (actual)**
- Circuit Breaker, Rate Limiter, Bulkhead (semaphore/thread pool), Retry, Time Limiter
- Integración con Spring Cloud Circuit Breaker abstraction
- Métricas y actuator endpoints

**5.3 Hystrix (Netflix — deprecado, importante para exámenes)**
- `@HystrixCommand`, Hystrix Dashboard, Turbine
- Por qué fue deprecado

---

### BLOQUE 7 — API GATEWAY
→ [06-api-gateway.md](./06-api-gateway.md)

**6.1 El patrón API Gateway**
- Por qué los clientes no deben conocer los servicios internos
- Responsabilidades: routing, autenticación, rate limiting, logging
- BFF (Backend For Frontend) pattern

**6.2 Spring Cloud Gateway (actual)**
- 6.2.1 Arquitectura reactiva basada en Netty
- 6.2.2 Predicates: Path, Host, Method, Header, etc.
- 6.2.3 Filters: Pre-filters y Post-filters
- 6.2.4 Configuración en YAML vs Java DSL
- 6.2.5 Built-in filters: AddHeader, RewritePath, StripPrefix, etc.
- 6.2.6 Custom filters: `GatewayFilter` y `GlobalFilter`
- 6.2.7 Integración con Service Discovery (`lb://`)
- 6.2.8 Rate Limiting con Redis
- 6.2.9 Circuit Breaker en Gateway
- 6.2.10 Seguridad: OAuth2, JWT en el Gateway
- 6.2.11 CORS en Spring Cloud Gateway

**6.3 Zuul (Netflix — deprecado, importante para exámenes)**

---

### BLOQUE 8 — TRAZABILIDAD Y OBSERVABILIDAD
→ [09-observabilidad.md](./09-observabilidad.md)

**8.1 Los tres pilares: Logs, Metrics, Traces**

**8.2 Spring Cloud Sleuth (deprecado → Micrometer Tracing)**
- Conceptos: Trace ID, Span ID, baggage
- Propagación automática de contexto (HTTP, Messaging)
- Integración con Zipkin

**8.3 Micrometer Tracing (actual — Spring Boot 3+)**
- Migración de Sleuth a Micrometer
- Integración con Zipkin y Jaeger
- B3 propagation vs W3C TraceContext

**8.4 Métricas con Micrometer**
- Counters, Gauges, Timers, Distribution Summaries
- Integración con Prometheus y Grafana

---

### BLOQUE 9 — SEGURIDAD
→ [10-seguridad.md](./10-seguridad.md)

**9.1 Seguridad en arquitecturas de microservicios**
- Autenticación vs Autorización
- Token-based security: JWT, OAuth2
- El patrón de seguridad en el Gateway

**9.2 Spring Cloud Security**
- Propagación de tokens entre servicios
- Resource Server configuration
- Scopes y roles en microservicios

**9.3 OAuth2 con Spring Cloud Gateway**
- Token Relay filter
- Integración con Keycloak, Auth0, Okta

---

### BLOQUE 10 — DESPLIEGUE Y ECOSISTEMA CLOUD
→ [11-kubernetes.md](./11-kubernetes.md)

**10.1 Spring Cloud Kubernetes**
- ConfigMap y Secret como fuente de configuración
- DiscoveryClient nativo de Kubernetes
- Health checks y readiness/liveness probes
- Eureka vs Kubernetes Service Discovery

**10.2 Spring Cloud AWS** *(brevemente cubierto)*
- S3, SQS, SNS, Parameter Store

**10.3 Spring Cloud Vault** *(brevemente cubierto)*
- HashiCorp Vault como backend de secretos

---

### BLOQUE 11 — PROYECTO PRÁCTICO COMPLETO
→ [12-proyecto-practico.md](./12-proyecto-practico.md)

**11.1 Diseño del sistema de ejemplo** — e-commerce con microservicios

**11.2 Implementación paso a paso**
- Config Server con repositorio Git
- Eureka Server
- Microservicio de productos
- Microservicio de pedidos (Feign + Circuit Breaker)
- API Gateway con rutas y filtros
- Trazabilidad con Zipkin

**11.3 Docker Compose del ecosistema completo**

---

### BLOQUE 12 — REFERENCIA RÁPIDA Y CERTIFICACIÓN
→ [13-referencia-rapida.md](./13-referencia-rapida.md) *(pendiente)*

**12.1 Cheatsheet de anotaciones clave**

**12.2 Cheatsheet de propiedades de configuración**

**12.3 Tabla de deprecaciones y reemplazos**

**12.4 Preguntas frecuentes en entrevistas técnicas**

**12.5 Preguntas tipo certificación con respuestas razonadas**

**12.6 Errores comunes y cómo resolverlos**

**12.7 Checklist antes de ir a producción**

---

## Estado de la documentación

| Archivo | Bloque | Estado |
|---------|--------|--------|
| [00-contexto.md](./00-contexto.md) | Bloque 0 | Pendiente |
| [01-fundamentos.md](./01-fundamentos.md) | Bloque 1 | Completo |
| [02-arquitectura.md](./02-arquitectura.md) | Bloque 1 | Completo |
| [03-config.md](./03-config.md) | Bloque 2 | Completo |
| [04-service-discovery.md](./04-service-discovery.md) | Bloque 3 | Completo |
| [05-load-balancing.md](./05-load-balancing.md) | Bloque 4 | Completo |
| [06-api-gateway.md](./06-api-gateway.md) | Bloque 7 | Completo |
| [07-circuit-breaker.md](./07-circuit-breaker.md) | Bloque 6 | Completo |
| [08-comunicacion.md](./08-comunicacion.md) | Bloque 5 | Completo |
| [09-observabilidad.md](./09-observabilidad.md) | Bloque 8 | Completo |
| [10-seguridad.md](./10-seguridad.md) | Bloque 9 | Completo |
| [11-kubernetes.md](./11-kubernetes.md) | Bloque 10 | Completo |
| [12-proyecto-practico.md](./12-proyecto-practico.md) | Bloque 11 | Completo |
| [13-referencia-rapida.md](./13-referencia-rapida.md) | Bloque 12 | Pendiente |

---

## Convenciones

| Símbolo | Significado |
|---------|-------------|
| `[CONCEPTO]` | Definición teórica importante |
| `[CÓDIGO]` | Fragmento de código práctico |
| `[ADVERTENCIA]` | Error común o trampa frecuente |
| `[EXAMEN]` | Punto frecuente en pruebas técnicas |
| `[LEGACY]` | Componente deprecado — conocer para exámenes, no usar en proyectos nuevos |

---

> Versión objetivo: **Spring Cloud 2023.0.x (Leyton)** + **Spring Boot 3.2/3.3** + **Java 17/21**
