# Pasada 5 — verificación final contra el PDF

**Fecha:** 2026-11-05  
**Fuente:** `SDD/Especificación Funcional del Negocio - NexusMarket.pdf`  
**Método:** PDFBox 2.0.31, `PDFTextStripper` con `sortByPosition=true` y extracción p��gina a página.

## Evidencia de extracción

| Elemento | Resultado |
|---|---|
| Páginas | 8 |
| Líneas | 268 |
| Palabras | 1106 |
| Caracteres | 8396 |
| SHA-256 del texto | `1C994B68FE6A237F7C67AA1AB98D5A47373E4E24D1690EDE082DB9C1C13109F5` |
| Igualdad con pasadas 1–4 | Sí; mismo hash |

## Verificación de estructura

| Página | Contenido verificado |
|---|---|
| 1 | Introducción, objetivos OBJ-01…OBJ-09 |
| 2 | OBJ-10…OBJ-12, §3.1 procesos incluidos |
| 3 | §3.2 fuera de alcance, §4.1 componentes |
| 4 | §5 participantes, §6.1 flujo de 8 pasos |
| 5 | Dominio 1 Usuarios, Dominio 2 Compradores |
| 6 | Dominio 3 Vendedores, Dominio 4 Bodegas, Dominio 5 Catálogo |
| 7 | Dominio 6 Inventario, Dominio 7 Pedidos, §10 restricciones |
| 8 | §11 validaciones críticas, §12 matriz de responsabilidades |

## Matriz de responsabilidades (p.8 §12)

| Proceso | Roles |
|---|---|
| Registro de Vendedores | Admin |
| Registro de Productos | Vendedor |
| Administración de Inventario | Vendedor + Op. Logístico |
| Gestión de Pedidos | Comprador + Vendedor + Op. Logístico |
| Gestión de Reembolsos | Comprador + Admin |

Las cuatro columnas (`Comprador`, `Vendedor`, `Op. Logístico`, `Admin`) están presentes. No hay filas de devoluciones, reportes ni Supervisor. D-09 queda confirmada y no contradicha.

## Resultado de la pasada

- No aparecieron requisitos funcionales nuevos ni áreas que justifiquen 28 servicios/90 endpoints.
- Los 84 `REQ-*` siguen siendo el universo atómico verificable del PDF.
- Los supuestos abiertos (`SUP-01`…`SUP-15`) siguen sin respaldo explícito.
- Los 9 defectos técnicos de F3 quedaron corregidos sin alterar requisitos del PDF.
- No se detectaron nuevas contradicciones entre PDF, ledger y código.
- F4/F5 se mantienen N/A por falta de respaldo funcional; no se añadieron servicios ni endpoints artificiales.
