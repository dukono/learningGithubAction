# Spring Cloud — Guía Completa Profesional

> Documentación exhaustiva desde cero hasta nivel experto.  
> Apta para pruebas técnicas empresariales y certificaciones.  
> Cubre Spring Boot 3 / Spring Cloud 2023.x

---

## ÍNDICE GENERAL

### PARTE 1 — FUNDAMENTOS CONCEPTUALES
→ [01-fundamentos.md](./01-fundamentos.md)

| # | Subtema |
|---|---------|
| 1.1 | [¿Qué es Spring Cloud?](./01-fundamentos.md#11-qué-es-spring-cloud) |
| 1.2 | [Historia y evolución](./01-fundamentos.md#12-historia-y-evolución) |
| 1.3 | [Spring Cloud vs Spring Boot vs Spring Framework](./01-fundamentos.md#13-spring-cloud-vs-spring-boot-vs-spring-framework) |
| 1.4 | [El problema que resuelve: monolito vs microservicios](./01-fundamentos.md#14-el-problema-que-resuelve-arquitectura-monolítica-vs-microservicios) |
| 1.5 | [Los 8 problemas clásicos de los microservicios](./01-fundamentos.md#15-los-8-problemas-clásicos-de-los-microservicios) |
| 1.6 | [Cómo Spring Cloud resuelve esos problemas](./01-fundamentos.md#16-cómo-spring-cloud-resuelve-esos-problemas) |
| 1.7 | [Versiones, BOM y compatibilidad con Spring Boot](./01-fundamentos.md#17-versiones-bom-y-compatibilidad-con-spring-boot) |

---

### PARTE 2 — ARQUITECTURA DE SPRING CLOUD
→ [02-arquitectura.md](./02-arquitectura.md)

| # | Subtema |
|---|---------|
| 2.1 | [Visión general de la arquitectura](./02-arquitectura.md#21-visión-general-de-la-arquitectura) |
| 2.2 | [Patrones de diseño que implementa Spring Cloud](./02-arquitectura.md#22-patrones-de-diseño-en-microservicios-que-implementa-spring-cloud) |
| 2.3 | [Mapa de componentes y sus relaciones](./02-arquitectura.md#23-mapa-de-componentes-y-sus-relaciones) |
| 2.4 | [Flujo de una petición en un ecosistema Spring Cloud](./02-arquitectura.md#24-flujo-de-una-petición-en-un-ecosistema-spring-cloud) |

---

### PARTE 3 — SPRING CLOUD CONFIG (Configuración centralizada)
→ [03-config.md](./03-config.md)

| # | Subtema |
|---|---------|
| 3.1 | [Qué es y para qué sirve](./03-config.md#31-qué-es-y-para-qué-sirve) |
| 3.2 | [Arquitectura: Config Server y Config Client](./03-config.md#32-arquitectura-config-server-y-config-client) |
| 3.3 | [Backends soportados: Git, filesystem, Vault, JDBC](./03-config.md#33-backends-soportados-git-filesystem-vault-jdbc) |
| 3.4 | [Configuración del Config Server](./03-config.md#34-configuración-del-config-server) |
| 3.5 | [Configuración del Config Client](./03-config.md#35-configuración-del-config-client) |
| 3.6 | [Perfiles y entornos (application-{profile}.yml)](./03-config.md#36-perfiles-y-entornos-application-profileyml) |
| 3.7 | [Refresco de configuración: @RefreshScope y Spring Cloud Bus](./03-config.md#37-refresco-de-configuración-refreshscope-y-spring-cloud-bus) |
| 3.8 | [Cifrado y descifrado de propiedades sensibles](./03-config.md#38-cifrado-y-descifrado-de-propiedades-sensibles) |
| 3.9 | [Alta disponibilidad del Config Server](./03-config.md#39-alta-disponibilidad-del-config-server) |

---

### PARTE 4 — SERVICE DISCOVERY (Descubrimiento de servicios)
→ [04-service-discovery.md](./04-service-discovery.md)

| # | Subtema |
|---|---------|
| 4.1 | [Qué es el Service Discovery y por qué es necesario](./04-service-discovery.md#41-qué-es-el-service-discovery-y-por-qué-es-necesario) |
| 4.2 | [Modelos: Client-Side vs Server-Side Discovery](./04-service-discovery.md#42-modelos-client-side-vs-server-side-discovery) |
| 4.3 | [Spring Cloud Netflix Eureka — Servidor](./04-service-discovery.md#43-spring-cloud-netflix-eureka--servidor) |
| 4.4 | [Spring Cloud Netflix Eureka — Cliente](./04-service-discovery.md#44-spring-cloud-netflix-eureka--cliente) |
| 4.5 | [Heartbeat, lease y evicción de instancias](./04-service-discovery.md#45-heartbeat-lease-y-evicción-de-instancias) |
| 4.6 | [Self-preservation mode](./04-service-discovery.md#46-self-preservation-mode) |
| 4.7 | [Eureka en alta disponibilidad (clúster)](./04-service-discovery.md#47-eureka-en-alta-disponibilidad-clúster) |
| 4.8 | [Alternativas a Eureka: Consul y Zookeeper](./04-service-discovery.md#48-alternativas-a-eureka-consul-y-zookeeper) |

---

### PARTE 5 — LOAD BALANCING (Balanceo de carga)
→ [05-load-balancing.md](./05-load-balancing.md)

| # | Subtema |
|---|---------|
| 5.1 | [Qué es el balanceo de carga en microservicios](./05-load-balancing.md#51-qué-es-el-balanceo-de-carga-en-microservicios) |
| 5.2 | [Spring Cloud LoadBalancer (sucesor de Ribbon)](./05-load-balancing.md#52-spring-cloud-loadbalancer-sucesor-de-ribbon) |
| 5.3 | [Algoritmos: Round Robin y Random](./05-load-balancing.md#53-algoritmos-round-robin-y-random) |
| 5.4 | [Configuración y personalización del balanceador](./05-load-balancing.md#54-configuración-y-personalización-del-balanceador) |
| 5.5 | [Integración con Eureka y OpenFeign](./05-load-balancing.md#55-integración-con-eureka-y-openfeign) |

---

### PARTE 6 — API GATEWAY
→ [06-api-gateway.md](./06-api-gateway.md)

| # | Subtema |
|---|---------|
| 6.1 | [Qué es un API Gateway y sus responsabilidades](./06-api-gateway.md#61-qué-es-un-api-gateway-y-sus-responsabilidades) |
| 6.2 | [Spring Cloud Gateway (arquitectura reactiva)](./06-api-gateway.md#62-spring-cloud-gateway-arquitectura-reactiva) |
| 6.3 | [Conceptos: Route, Predicate, Filter](./06-api-gateway.md#63-conceptos-route-predicate-filter) |
| 6.4 | [Configuración de rutas (YAML y Java DSL)](./06-api-gateway.md#64-configuración-de-rutas-yaml-y-java-dsl) |
| 6.5 | [Filtros predefinidos más importantes](./06-api-gateway.md#65-filtros-predefinidos-más-importantes) |
| 6.6 | [Filtros personalizados (GatewayFilter y GlobalFilter)](./06-api-gateway.md#66-filtros-personalizados-gatewayfilter-y-globalfilter) |
| 6.7 | [Rate Limiting con Redis](./06-api-gateway.md#67-rate-limiting-con-redis) |
| 6.8 | [Autenticación y autorización en el Gateway](./06-api-gateway.md#68-autenticación-y-autorización-en-el-gateway) |
| 6.9 | [Gateway vs Zuul (comparativa)](./06-api-gateway.md#69-gateway-vs-zuul-comparativa) |

---

### PARTE 7 — CIRCUIT BREAKER (Tolerancia a fallos)
→ [07-circuit-breaker.md](./07-circuit-breaker.md)

| # | Subtema |
|---|---------|
| 7.1 | [El problema: fallos en cascada](./07-circuit-breaker.md#71-el-problema-fallos-en-cascada) |
| 7.2 | [Patrón Circuit Breaker explicado](./07-circuit-breaker.md#72-patrón-circuit-breaker-explicado) |
| 7.3 | [Estados: CLOSED, OPEN, HALF-OPEN](./07-circuit-breaker.md#73-estados-closed-open-half-open) |
| 7.4 | [Resilience4j en Spring Cloud](./07-circuit-breaker.md#74-resilience4j-en-spring-cloud) |
| 7.5 | [Configuración de CircuitBreaker con Resilience4j](./07-circuit-breaker.md#75-configuración-de-circuitbreaker-con-resilience4j) |
| 7.6 | [Fallback: respuestas de emergencia](./07-circuit-breaker.md#76-fallback-respuestas-de-emergencia) |
| 7.7 | [Retry, TimeLimiter y Bulkhead](./07-circuit-breaker.md#77-retry-timelimiter-y-bulkhead) |
| 7.8 | [Integración con Spring Cloud Gateway](./07-circuit-breaker.md#78-integración-con-spring-cloud-gateway) |
| 7.9 | [Monitoreo con Actuator y métricas](./07-circuit-breaker.md#79-monitoreo-con-actuator-y-métricas) |

---

### PARTE 8 — COMUNICACIÓN ENTRE SERVICIOS
→ [08-comunicacion.md](./08-comunicacion.md)

| # | Subtema |
|---|---------|
| 8.1 | [Comunicación síncrona vs asíncrona](./08-comunicacion.md#81-comunicación-síncrona-vs-asíncrona) |
| 8.2 | [Spring Cloud OpenFeign — cliente HTTP declarativo](./08-comunicacion.md#82-spring-cloud-openfeign--cliente-http-declarativo) |
| 8.3 | [Configuración de Feign: timeouts, interceptors, logging](./08-comunicacion.md#83-configuración-de-feign-timeouts-interceptors-logging) |
| 8.4 | [Feign + LoadBalancer + Circuit Breaker (integración completa)](./08-comunicacion.md#84-feign--loadbalancer--circuit-breaker-integración-completa) |
| 8.5 | [Spring Cloud Stream — mensajería con Kafka y RabbitMQ](./08-comunicacion.md#85-spring-cloud-stream--mensajería-con-kafka-y-rabbitmq) |
| 8.6 | [Binders, Bindings y canales en Spring Cloud Stream](./08-comunicacion.md#86-binders-bindings-y-canales-en-spring-cloud-stream) |
| 8.7 | [Spring Cloud Bus — propagación de eventos](./08-comunicacion.md#87-spring-cloud-bus--propagación-de-eventos) |

---

### PARTE 9 — OBSERVABILIDAD (Trazabilidad distribuida)
→ [09-observabilidad.md](./09-observabilidad.md)

| # | Subtema |
|---|---------|
| 9.1 | [El problema: rastrear peticiones entre microservicios](./09-observabilidad.md#91-el-problema-rastrear-peticiones-entre-microservicios) |
| 9.2 | [Conceptos: Trace ID, Span ID, correlación](./09-observabilidad.md#92-conceptos-trace-id-span-id-correlación) |
| 9.3 | [Spring Cloud Sleuth (inyección automática de trazas)](./09-observabilidad.md#93-spring-cloud-sleuth-inyección-automática-de-trazas) |
| 9.4 | [Micrometer Tracing (sucesor de Sleuth en Spring Boot 3)](./09-observabilidad.md#94-micrometer-tracing-sucesor-de-sleuth-en-spring-boot-3) |
| 9.5 | [Zipkin: servidor de trazas distribuidas](./09-observabilidad.md#95-zipkin-servidor-de-trazas-distribuidas) |
| 9.6 | [Integración con Grafana y Prometheus](./09-observabilidad.md#96-integración-con-grafana-y-prometheus) |

---

### PARTE 10 — SEGURIDAD
→ [10-seguridad.md](./10-seguridad.md)

| # | Subtema |
|---|---------|
| 10.1 | [Seguridad en arquitecturas de microservicios](./10-seguridad.md#101-seguridad-en-arquitecturas-de-microservicios) |
| 10.2 | [OAuth2 y JWT en Spring Cloud](./10-seguridad.md#102-oauth2-y-jwt-en-spring-cloud) |
| 10.3 | [Spring Authorization Server](./10-seguridad.md#103-spring-authorization-server) |
| 10.4 | [Propagación de tokens entre servicios](./10-seguridad.md#104-propagación-de-tokens-entre-servicios) |
| 10.5 | [Seguridad en el Config Server](./10-seguridad.md#105-seguridad-en-el-config-server) |
| 10.6 | [mTLS y comunicación segura entre microservicios](./10-seguridad.md#106-mtls-y-comunicación-segura-entre-microservicios) |

---

### PARTE 11 — SPRING CLOUD KUBERNETES
→ [11-kubernetes.md](./11-kubernetes.md)

| # | Subtema |
|---|---------|
| 11.1 | [Spring Cloud en entornos Kubernetes](./11-kubernetes.md#111-spring-cloud-en-entornos-kubernetes) |
| 11.2 | [Service Discovery nativo en Kubernetes](./11-kubernetes.md#112-service-discovery-nativo-en-kubernetes) |
| 11.3 | [ConfigMaps y Secrets como fuente de configuración](./11-kubernetes.md#113-configmaps-y-secrets-como-fuente-de-configuración) |
| 11.4 | [Eureka vs Kubernetes Service Discovery](./11-kubernetes.md#114-eureka-vs-kubernetes-service-discovery) |

---

### PARTE 12 — PROYECTO PRÁCTICO COMPLETO
→ [12-proyecto-practico.md](./12-proyecto-practico.md)

| # | Subtema |
|---|---------|
| 12.1 | [Diseño del sistema de ejemplo](./12-proyecto-practico.md#121-diseño-del-sistema-de-ejemplo) |
| 12.2 | [Config Server con repositorio Git](./12-proyecto-practico.md#122-config-server-con-repositorio-git) |
| 12.3 | [Eureka Server](./12-proyecto-practico.md#123-eureka-server) |
| 12.4 | [Microservicio de productos](./12-proyecto-practico.md#124-microservicio-de-productos) |
| 12.5 | [Microservicio de pedidos (con Feign + Circuit Breaker)](./12-proyecto-practico.md#125-microservicio-de-pedidos-con-feign--circuit-breaker) |
| 12.6 | [API Gateway con rutas y filtros](./12-proyecto-practico.md#126-api-gateway-con-rutas-y-filtros) |
| 12.7 | [Trazabilidad con Zipkin](./12-proyecto-practico.md#127-trazabilidad-con-zipkin) |
| 12.8 | [Docker Compose del ecosistema completo](./12-proyecto-practico.md#128-docker-compose-del-ecosistema-completo) |

---

### PARTE 13 — REFERENCIA RÁPIDA Y CERTIFICACIÓN
→ [13-referencia-rapida.md](./13-referencia-rapida.md)

| # | Subtema |
|---|---------|
| 13.1 | [Tabla resumen de componentes](./13-referencia-rapida.md#131-tabla-resumen-de-componentes) |
| 13.2 | [Anotaciones clave de Spring Cloud](./13-referencia-rapida.md#132-anotaciones-clave-de-spring-cloud) |
| 13.3 | [Propiedades de configuración más importantes](./13-referencia-rapida.md#133-propiedades-de-configuración-más-importantes) |
| 13.4 | [Preguntas frecuentes en entrevistas técnicas](./13-referencia-rapida.md#134-preguntas-frecuentes-en-entrevistas-técnicas) |
| 13.5 | [Preguntas tipo certificación con respuestas razonadas](./13-referencia-rapida.md#135-preguntas-tipo-certificación-con-respuestas-razonadas) |
| 13.6 | [Errores comunes y cómo resolverlos](./13-referencia-rapida.md#136-errores-comunes-y-cómo-resolverlos) |
| 13.7 | [Checklist antes de ir a producción](./13-referencia-rapida.md#137-checklist-antes-de-ir-a-producción) |

---

> Versión objetivo: **Spring Cloud 2023.0.x (Leyton)** + **Spring Boot 3.2/3.3** + **Java 17/21**
