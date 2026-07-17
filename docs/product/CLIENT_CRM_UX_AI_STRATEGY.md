# Propuesta de CRM de cliente, componentes de suscripción e IA

> Estado: propuesta para revisión. No aprueba cambios de datos, precios, facturación recurrente ni uso de IA hasta validar las decisiones del final.

## Estado de la primera entrega (2026-07-17)

- Implementado: **Centro de Cliente** con navegación principal agrupada, pantalla de trabajo, estado/siguiente acción y componente comercial **Servicio y SLA**.
- Implementado: activación manual por administrador, persistida por cliente y validada en Ktor/PostgreSQL; los clientes Priority/VIP existentes reciben Servicio y SLA durante la migración.
- Implementado: módulo visible en Inicio, Servicio y Cuenta; si no está contratado, el portal explica el valor y dirige a una solicitud, sin bloquear el soporte actual.
- Implementado: CRM operativo de administración para contactos y actividades de seguimiento, con una única migración final V6. Las rutas de portal `/client/*` entregan perfil, tickets, tareas, horas y un resumen ya aislado; los comentarios internos y el CRM interno no se exponen al cliente.
- Pendiente: miembros/roles por empresa, Roadmap y aprobaciones, catálogo de más módulos, cobro recurrente y piloto de IA con proveedor, consentimiento y retención aprobados.

## 1. Objetivo de producto

Convertir el actual portal de soporte en un **centro de continuidad del cliente**: un lugar al que una empresa quiera volver después de terminar una entrega para entender el estado del servicio, tomar una decisión, conservar conocimiento y pedir ayuda sin perder contexto.

La promesa no es “más pantallas de CRM”. Es esta:

> **En menos de un minuto, el cliente sabe si todo va bien, qué ha cambiado, qué debe hacer y cuál es el siguiente paso.**

Esto resuelve una necesidad real de postventa B2B. En la investigación de McKinsey, la mala experiencia digital, la falta de continuidad entre canales y la dificultad para llegar a la persona adecuada figuran entre los motivos para cambiar de proveedor. Sus compradores B2B usan de media diez puntos de contacto, por lo que el autoservicio debe conservar contexto y ofrecer salida humana cuando la petición lo requiere. [McKinsey, B2B Pulse 2024](https://www.mckinsey.com/capabilities/growth-marketing-and-sales/our-insights/five-fundamental-truths-how-b2b-winners-keep-growing)

## 2. Punto de partida comprobado en el proyecto

El producto ya tiene una base útil sobre la que construir:

| Ya existe | Qué aporta | Hueco para el CRM de continuidad |
|---|---|---|
| Inicio de cliente | Resumen, tickets, horas, tareas y acciones rápidas. | Prioriza métricas, pero no una única “siguiente acción” ni salud del servicio. |
| Tickets, tareas, tablero y actividad | Seguimiento operativo y trazabilidad de soporte. | Falta agruparlos en una narrativa de avance, decisiones e hitos. |
| Mi Servicio | Horas, tickets, categorías y tendencia. | No incluye bolsa/alcance, SLA, renovación ni recomendaciones accionables. |
| Mi Cuenta | Perfil, plan `STANDARD/PRIORITY/VIP` y datos de acceso. | El plan es estático: no hay catálogo, permisos por módulo, usuarios de empresa ni autoservicio comercial. |
| Autenticación y aislamiento actual | El token incluye `clientId`; tickets, tareas y horas se limitan a ese cliente en servidor. | Es una buena base de seguridad para módulos e IA, que debe mantenerse siempre en servidor. |

El portal actual expone ocho destinos de primer nivel (`Inicio`, `Nuevo ticket`, `Tickets`, `Tareas`, `Tablero`, `Mi Servicio`, `Actividad`, `Mi Cuenta`). En escritorio funciona como barra lateral; en compacto se convierte en una fila desplazable. Para una experiencia móvil más clara conviene reducir el nivel superior a cinco destinos y relegar las vistas secundarias a agrupaciones o a “Más”. Esta decisión aplica la regla de navegación de la habilidad local de UX: la navegación inferior móvil no debe tener más de cinco destinos y debe separar navegación primaria de secundaria.

## 3. Necesidades de cliente y trabajos que debe resolver

La investigación apunta a un portal que combina autonomía en gestiones simples con acceso humano contextual en las complejas. Las organizaciones de servicio de alto rendimiento ofrecen con más frecuencia centro de ayuda, portal y autoservicio guiado. [Salesforce, *State of Service*](https://www.salesforce.com/service/state-of-service-report/?bc=OTH)

### Trabajos del cliente (JTBD)

1. **Al acabar una entrega**, quiero ver el estado y el siguiente paso sin perseguir a nadie, para sentir que el servicio permanece controlado.
2. **Al aparecer una necesidad**, quiero resolverla o solicitarla sin redactar un correo largo ni repetir el contexto.
3. **Al informar o decidir internamente**, quiero una vista fiable de avance, horas/alcance, riesgos y próximos hitos.
4. **Al cambiar una persona de mi equipo**, quiero conservar el historial, los documentos y los permisos adecuados.
5. **Al necesitar más servicio**, quiero entender qué capacidad adicional me resuelve el problema y activarla con el mínimo roce.

### Prioridad de experiencia

| Prioridad | Dolor que elimina | Respuesta de producto |
|---|---|---|
| Estado claro | “No sé qué se ha hecho, qué falta o qué bloquea.” | Salud del servicio, último avance, próximo hito y una alerta solo cuando requiere acción. |
| Solicitud rápida y trazable | Canales dispersos y contexto repetido. | Solicitudes guiadas con adjuntos, responsable, SLA, estado e historial. |
| Control sin microgestión | Incertidumbre sobre horas, alcance, soporte y renovaciones. | Consumo, servicio contratado, decisiones pendientes y revisión mensual. |
| Colaboración de empresa | Un solo contacto concentra todo. | Miembros, roles, menciones, aprobaciones y notificaciones. |
| Memoria de la relación | Documentación, entregables y decisiones se pierden tras el proyecto. | Biblioteca privada de activos, decisiones y procedimientos. |
| Proactividad | El proveedor solo aparece al haber un problema o una factura. | Resumen de valor, riesgos verificables y próximos pasos recomendados. |

## 4. Posicionamiento y arquitectura de información

Nombre conceptual: **Centro de Cliente**. Es más amplio y útil que “portal de tickets”, pero puede conservar el nombre comercial actual en la primera versión.

### Navegación propuesta

| Nivel | Destino | Contenido | Estado |
|---|---|---|---|
| Primario | Inicio | Salud, siguiente acción, avances, actividad y accesos rápidos. | Sustituye el inicio actual. |
| Primario | Trabajo | Solicitudes, tareas, tablero e hitos. | Agrupa cuatro vistas existentes. |
| Primario | Solicitar | Crear ticket o petición de cambio, con IA solo como borrador. | Evoluciona “Nuevo ticket”. |
| Primario | Servicio | SLA, bolsa/consumo, informes, documentos y renovaciones. | Evoluciona “Mi Servicio”. |
| Primario | Cuenta | Empresa, miembros, permisos, notificaciones y plan. | Evoluciona “Mi Cuenta”. |
| Secundario | Más | Actividad completa, ajustes, ayuda y cerrar sesión. | Sheet/menú, no un sexto tab. |

En móvil se usarán los cinco destinos primarios con icono y texto; en escritorio, barra lateral con los mismos grupos y sus subrutas. Al volver atrás se conserva filtro, scroll y estado. Las rutas deben ser enlazables desde una notificación (“ver aprobación”, “ver ticket”) y el foco debe ir al contenido principal al cambiar de vista.

### Wireframe del inicio

```text
┌ Empresa / estado de conexión ───────────────────────────── IA con fuentes ┐
│ Buenos días, Acme                     Servicio estable · actualizado hoy │
├───────────────────────────┬───────────────────────────────────────────────┤
│ Estado del servicio       │ Tu siguiente acción                           │
│ ● Todo en curso           │ Aprobar propuesta de mantenimiento            │
│ 3 tareas · 1 ticket       │ [Ver aprobación]                              │
├───────────────────────────┴───────────────────────────────────────────────┤
│ Qué ha cambiado: avance de ticket, entrega publicada, respuesta del equipo │
├───────────────────────────────────┬───────────────────────────────────────┤
│ Trabajo en marcha                 │ Servicio contratado                     │
│ hitos · solicitudes · bloqueos    │ horas/alcance · SLA · próxima revisión │
├───────────────────────────────────┴───────────────────────────────────────┤
│ Acciones: solicitar ayuda · ver trabajo · consultar documentación          │
└───────────────────────────────────────────────────────────────────────────┘
```

Orden intencional: **salud → cambio → acción pendiente → trabajo → servicio → acción rápida → expansión contextual**. Los gráficos no serán decorativos: cada uno debe responder a una decisión y ofrecer tabla/resumen accesible alternativo.

## 5. Sistema UX/UI para una interfaz limpia y profesional

Se aplicaron las habilidades de `.claude` `mobile-app-ui-design` y `ui-ux-pro-max`; esta última generó un sistema de diseño para “CRM Freelance”. La síntesis recomendada es **SaaS B2B sobrio, plano y ligero**, con alta legibilidad, no una interfaz promocional ni una pared de métricas.

### Reglas visuales

- Usar los `SupportDeskThemeTokens` actuales como fuente de verdad; no introducir colores por pantalla.
- Paleta guía del sistema generado: azul corporativo `#2563EB`, acento verde `#059669`, fondo `#F8FAFC`, texto `#0F172A`, peligro `#DC2626`. Se mapeará a tokens semánticos y se verificará contraste en claro y oscuro.
- Jerarquía tipográfica limpia, con una sola familia de interfaz. La sugerencia de referencia es Plus Jakarta Sans; solo se adoptará si encaja con las fuentes multiplataforma existentes. Si no, se mantienen las fuentes del tema y sus escalas.
- Retícula de 4/8 dp, superficies planas, bordes discretos y sombra mínima. Nada de gradientes, 3D, efectos de cristal ni animaciones de presentación.
- Iconografía Lucide consistente, nunca emojis como iconos estructurales.
- Microinteracciones de 150–300 ms, estados pulsados estables y respeto por reducción de movimiento.

### Componentes reutilizables a crear

| Componente Compose | Propósito | Estados obligatorios |
|---|---|---|
| `ServiceHealthCard` | Salud y actualización del servicio con razón comprensible. | Estable, atención requerida, sin datos, error. |
| `NextActionCard` | Una decisión pendiente con responsable, vencimiento y CTA. | Pendiente, vencida, completada, no disponible por plan. |
| `WorkUpdateRow` | Cambio verificable con fecha, actor y enlace a detalle. | Leído/no leído, carga, vacío. |
| `ServiceAllowanceCard` | Consumo de horas/alcance/SLA sin prometer cifras si faltan datos. | Normal, cercano a límite, límite alcanzado, no contratado. |
| `FeatureGateCard` | Explica una capacidad no contratada y permite pedir información. | Bloqueada, recomendada por contexto, solicitud enviada. |
| `ApprovalCard` | Aprobación explícita para cambios, presupuesto o resumen. | Pendiente, aprobada, rechazada, caducada. |
| `DocumentItem` | Documento con tipo, versión, fecha, acceso y origen. | Disponible, procesamiento, sin permiso, error. |
| `AiAnswerCard` | Respuesta de IA con fuentes internas, fecha y salida a humano. | Cargando, con fuentes, abstención, fallo, feedback. |

### Calidad y accesibilidad no negociables

- Controles de al menos 48×48 dp en Android y 44×44 pt en iOS; texto e iconos con contraste suficiente (4,5:1 para texto pequeño).
- Color nunca como único indicador: los estados incluyen etiqueta e icono.
- Formularios con etiqueta visible, validación al perder foco, mensaje de error específico y foco al primer error tras enviar.
- Carga mediante skeleton; vacío con explicación y siguiente paso; error con reintento claro. Un gráfico vacío no debe dibujar ejes falsos.
- Pruebas en móvil pequeño, móvil grande, tableta, horizontal, modo oscuro, texto aumentado y reducción de movimiento.
- La navegación se emitirá desde ViewModels como evento de una sola vez, no como `StateFlow` reproducible; los componentes llevarán `contentDescription` y semántica Compose.

## 6. Componentes comerciales y modelo de expansión

Un “componente” es un módulo de producto con permiso (`entitlement`), límite de uso y valor medible. No es simplemente una pantalla bloqueada. El núcleo debe ser útil por sí mismo; los módulos se presentan solo al detectarse una necesidad concreta.

| Módulo | Incluido / add-on | Valor para el cliente | Señal contextual para ofrecerlo | Métrica |
|---|---|---|---|---|
| Centro de Cliente | Núcleo | Estado, trabajo, tickets, actividad, cuenta y soporte contextual. | Es la experiencia base. | Activación y usuarios activos por cuenta. |
| Servicio y SLA | Add-on | Bolsa de horas, cumplimiento, revisión mensual y alertas de servicio. | Consulta frecuente de horas/SLA o necesidad de informes. | Cumplimiento SLA y renovaciones. |
| Roadmap y aprobaciones | Add-on | Hitos, propuestas de mejora, cambios y aprobación trazable. | Se crea un cambio o participan varios decisores. | Tiempo hasta aprobación y ampliaciones. |
| Centro de conocimiento y activos | Add-on | Documentación, entregables, guías y enlaces seguros. | Se comparten documentos repetidamente o cambia el contacto. | Búsquedas resueltas y consultas autoservicio. |
| Colaboración de equipo | Add-on | Usuarios, roles, menciones y notificaciones por preferencia. | Se invita a más personas o se pierde información entre contactos. | Miembros activos y adopción de cuenta. |
| Informe ejecutivo | Add-on | Resumen mensual de valor, riesgos, servicio y decisiones. | El cliente necesita reportar internamente. | Apertura de informe, CSAT y retención. |
| Integraciones y automatizaciones | Add-on | Avisos en herramientas de trabajo y flujos verificables. | Repetición de avisos/copia manual. | Flujos activos y tiempo ahorrado. |
| Seguridad y gobierno | Add-on avanzado | Auditoría, accesos temporales y, después, SSO. | Requisito corporativo. | Requisito de venta cubierto y expansión. |

La recomendación de compra debe describir el problema y su resultado, no usar “Mejorar plan” como CTA genérico. Ejemplos: “Tu equipo necesita dos aprobadores: activa Roadmap y aprobaciones” o “Has consultado el consumo tres veces este mes: solicita el resumen SLA”. Nunca bloquear una acción ya prometida por el plan actual.

### Modelo de datos a planificar

Antes de cobro recurrente, separar **catálogo y permisos** de **facturación**:

```text
product_components ──< subscription_plans ──< plan_component_rules
                                      │
clients ──< client_component_entitlements ──< component_usage_events
        └──< client_memberships / client_roles
```

Tablas candidatas: `product_components`, `subscription_plans`, `plan_component_rules`, `client_component_entitlements`, `component_usage_events`, `client_memberships`, `client_roles`, `approval_requests` y `service_allowances`. Todas las filas multiempresa incorporan `client_id`; las comprobaciones de acceso se ejecutan en Ktor, nunca únicamente en Compose.

La generación local de PDF de facturas ya existente no debe convertirse en un sistema de suscripción por accidente: si se decide cobrar módulos de forma recurrente habrá que definir un dominio de billing servidor, proveedor de pago, impuestos, cancelaciones y auditoría en una iniciativa separada.

## 7. IA que aporta valor sin perder control

La oportunidad no es un chat genérico. Es disminuir incertidumbre y trabajo repetitivo usando los datos autorizados del cliente. La evidencia de Intercom sitúa clasificación, enrutado, traducción y consultas repetitivas entre los flujos de IA de soporte más maduros. [Intercom, 2026](https://www.intercom.com/blog/new-research-customer-service-team-evolution/)

### Priorización

| Fase | Capacidad | Qué hace | Restricción de seguridad |
|---|---|---|---|
| P0 | **¿Cómo va mi servicio?** | Resume tickets, tareas, bloqueos, próximas acciones y fecha de datos. | Solo lectura; cita cada ticket/tarea/documento y reconoce cuando no hay evidencia. |
| P0 | **Creador de solicitud guiado** | Convierte texto libre en borrador de asunto, impacto, pasos y adjuntos necesarios. | El cliente revisa y pulsa enviar; el servidor valida reglas y límites. |
| P1 | **Copiloto interno** | Resume conversaciones, detecta falta de información y sugiere respuestas o duplicados. | Nunca envía, prioriza o cambia estados sin confirmación humana. |
| P1 | **Resumen ejecutivo semanal** | Prepara avances, consumo, riesgos y decisiones. | Borrador que el profesional aprueba antes de compartir. |
| P2 | **Conocimiento conversacional** | Responde sobre manuales, entregas y procedimientos. | Solo documentos aprobados y del `clientId` autorizado. |
| P2 | **Alertas proactivas** | Señala aprobaciones/hitos/tickets esperando al cliente. | Basadas en reglas y datos comprobables, no predicciones opacas. |

No se iniciará con agentes que manden correos, modifiquen tareas, cobren, emitan facturas, prometan plazos/precios o tomen decisiones sobre personas. El coste de error es desproporcionado frente al valor inicial.

### Arquitectura de IA propuesta

```text
Compose Multiplatform
  → shared: AiAssistantRepository + ViewModel + estados tipados
  → Ktor: identidad, autorización, selección de fuentes, límites y validación
  → PostgreSQL: CRM, auditoría y conocimiento aislado por client_id
  → proveedor de IA: solo desde Ktor con una clave de servidor
```

Reglas de implementación:

1. Ktor obtiene siempre `clientId` y rol desde el token; la app no elige el ámbito de la consulta.
2. Cada consulta, documento y fragmento recuperado lleva `client_id`, rol y permiso de módulo. No hay memoria ni recuperación cruzada entre empresas.
3. Las primeras herramientas permitidas al modelo son: consultar resumen, buscar fuentes autorizadas y crear **borradores** de ticket. Sin SQL libre, URL arbitraria, correo, facturación ni cambios de estado.
4. Archivos, tickets y documentos son contenido no confiable: una instrucción dentro de ellos no es una orden. OWASP recuerda que RAG no elimina la inyección de instrucciones y recomienda mínimo privilegio y aprobación humana. [OWASP LLM01: Prompt Injection](https://genai.owasp.org/llmrisk/llm01-prompt-injection/)
5. `AiAnswerCard` mostrará “Generado por IA”, fuentes internas, fecha de actualización, “ver detalle”, feedback y “hablar con una persona”.
6. Minimizar/redactar datos personales antes de enviarlos a un proveedor; fijar finalidad, retención, borrado, consentimiento y `ai_generation_audit`. La protección de datos aplica durante desarrollo y despliegue de IA. [EDPB: AI and technology](https://www.edpb.europa.eu/topics/ai-and-technology/artificial-intelligence_en)
7. Para clientes de la UE, avisar de la interacción con IA y validar los requisitos aplicables antes de publicar. La Comisión Europea sitúa la aplicación de obligaciones de transparencia para sistemas interactivos el 2 de agosto de 2026. [Comisión Europea](https://digital-strategy.ec.europa.eu/en/news/commission-publishes-code-practice-marking-and-labelling-ai-generated-content)

Módulos de datos candidatos: `knowledge_documents`, `knowledge_chunks`, `ai_conversations`, `ai_feedback`, `ai_generation_audit` y un `AiRequestContext` que transporte identidad, `clientId`, rol, permisos contratados y fuentes permitidas. El marco operativo será continuo: gobernar, mapear, medir y gestionar riesgo, siguiendo el enfoque de [NIST AI RMF](https://www.nist.gov/itl/ai-risk-management-framework).

## 8. Roadmap y loop de producto

Cada fase se cierra con evidencia de uso, no solo con pantallas terminadas.

| Fase | Resultado | Validación de salida |
|---|---|---|
| 0. Descubrimiento (1–2 semanas) | Entrevistas con 5 clientes y 3 profesionales; mapa de cuentas, servicio y datos disponibles. | Se confirman los JTBD, top 3 dolores, vocabulario y métricas base. |
| 1. Núcleo de continuidad | Inicio rediseñado, navegación agrupada, estado, siguiente acción y trabajo existente integrado. | El usuario identifica estado y siguiente paso en prueba moderada sin ayuda. |
| 2. Entitlements y primer add-on | Catálogo/permisos, miembros básicos y un módulo piloto: Servicio y SLA **o** Roadmap y aprobaciones. | Los permisos se prueban en servidor; el módulo tiene adopción y una métrica de valor. |
| 3. IA P0 | Resumen con fuentes y borrador guiado de ticket. | Casos anonimizados, pruebas de aislamiento/prompt injection, buena tasa de fuentes y abstención correcta. |
| 4. Expansión | Conocimiento, informes, automatizaciones y módulos según uso. | Cada expansión mejora una métrica de adopción, ahorro, retención o MRR. |

### Loop operativo

1. **Medir cada semana:** primera visita útil, solicitudes creadas/resueltas, acciones pendientes completadas, documentos consultados y búsquedas sin resultado.
2. **Revisar cada mes por cuenta:** usuarios activos, cumplimiento de SLA, tiempo a primera respuesta, CSAT, uso de dos o más áreas y oportunidades de módulo detectadas.
3. **Entrevistar cada trimestre:** clientes activos, inactivos y perdidos; contrastar si el portal evita correos de estado y acelera decisiones.
4. **Ajustar una hipótesis a la vez:** problema → cambio → métrica esperada → resultado. No lanzar módulos por intuición ni por una solicitud aislada.
5. **Para IA:** revisar trazas, coste por cliente activo, feedback, cobertura de fuentes, fallos de aislamiento e incidencias antes de ampliar permisos o autonomía.

Métricas de negocio: activación, usuarios activos por cuenta, frecuencia mensual, tasa de autoservicio, cumplimiento SLA, tiempo de resolución, CSAT, conversión contextual de módulo, expansión MRR, renovación y churn. Un caso de Forrester sobre una experiencia digital postventa de Conga asocia la adopción a mayor retención; debe tratarse como referencia direccional, no como previsión para OryKai. [Forrester case study](https://www.forrester.com/report/conga-boosts-retention-and-growth-with-a-reinvented-digital-experience/RES185393)

## 9. Orden recomendado de construcción en este repositorio

1. Definir en `shared` modelos, eventos, estado y casos de uso de servicio/acciones/entitlements; conservar ViewModels inmutables y efectos de una sola vez.
2. Añadir migraciones y repositorios PostgreSQL/Ktor para permisos por cliente y miembros. Probar de forma explícita accesos cruzados y roles.
3. Crear los componentes Compose reutilizables en el design system y reconstruir `ClientPortalScreen` desde la nueva arquitectura de información, sin duplicar lógica de tickets/tareas existente.
4. Instrumentar eventos de producto sin capturar texto sensible innecesario.
5. Integrar el primer módulo comercial y, solo después de validar acceso y valor, crear el piloto IA P0 detrás de un permiso de componente.
6. Ejecutar pruebas de interfaz y accesibilidad en las plataformas objetivo, además de pruebas de repositorio/rutas y seguridad de autorización.

## 10. Decisiones que necesito antes de implementar

1. ¿El primer cliente objetivo es una pyme con una persona de contacto o equipos con varios decisores? Esto decide si miembros/roles es núcleo o primer add-on.
2. ¿Qué se venderá primero: Servicio y SLA o Roadmap y aprobaciones? Recomiendo elegir uno para validar valor y precio.
3. ¿Las suscripciones se gestionarán inicialmente de forma manual por administración, o se requiere cobro online? Recomiendo permisos manuales primero; billing es otra iniciativa.
4. ¿Qué documentos puede ver la IA y bajo qué consentimiento/retención? Recomiendo empezar solo con tickets, tareas y documentos marcados explícitamente como aprobados.
5. ¿Se mantiene el nombre “Portal cliente” o se adopta “Centro de Cliente” como nombre de producto?

## Fuentes de investigación

- [McKinsey — *Five fundamental truths: how B2B winners keep growing*](https://www.mckinsey.com/capabilities/growth-marketing-and-sales/our-insights/five-fundamental-truths-how-b2b-winners-keep-growing)
- [Salesforce — *State of Service*](https://www.salesforce.com/service/state-of-service-report/?bc=OTH)
- [Intercom — customer service team evolution](https://www.intercom.com/blog/new-research-customer-service-team-evolution/)
- [OWASP — LLM01 Prompt Injection](https://genai.owasp.org/llmrisk/llm01-prompt-injection/)
- [EDPB — Artificial intelligence](https://www.edpb.europa.eu/topics/ai-and-technology/artificial-intelligence_en)
- [European Commission — AI content marking and labelling](https://digital-strategy.ec.europa.eu/en/news/commission-publishes-code-practice-marking-and-labelling-ai-generated-content)
- [NIST — AI Risk Management Framework](https://www.nist.gov/itl/ai-risk-management-framework)
- [Forrester — Conga digital post-sale experience case study](https://www.forrester.com/report/conga-boosts-retention-and-growth-with-a-reinvented-digital-experience/RES185393)
