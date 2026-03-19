# ADR-001 Initial Architecture

## Estado

Aceptado.

## Contexto

El repositorio nace del wizard de Kotlin Multiplatform y ya incluye `composeApp`, `shared`, `server` e `iosApp`.

Tambien existian carpetas `androidApp` y `desktopApp`, pero no estan conectadas a Gradle.

## Decision

- Mantener `composeApp` como unica app cliente conectada al build.
- Mantener compatibilidad con el package vivo del wizard y crear el CRM nuevo bajo `com.requena.supportdesk`.
- Priorizar Desktop y Server en el MVP.
- Usar DI manual y repositorios fake para dejar la base lista sin sobreingenieria.

## Consecuencias

- El proyecto sigue alineado con el wizard y evita romper la configuracion actual.
- El dominio del CRM queda ya organizado con nombres profesionales y consistentes.
- La siguiente iteracion puede centrarse en conectar backend y persistencia sin rehacer la estructura.
