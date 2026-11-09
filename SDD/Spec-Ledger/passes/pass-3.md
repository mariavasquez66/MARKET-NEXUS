# Pasada 3 — Restricciones y validaciones críticas

- Fuente: `SDD/Especificación Funcional del Negocio - NexusMarket.pdf`
- Extracción repetida desde el PDF: PDFBox 2.0.31, `sortByPosition=true`.
- SHA-256 del texto extraído: `1C994B68FE6A237F7C67AA1AB98D5A47373E4E24D1690EDE082DB9C1C13109F5`
- Foco: RG-01…RG-03, validaciones críticas, reglas de inventario y pedidos.

## Hallazgos etiquetados

| ID | Hallazgo | Etiqueta RVI |
|---|---|---|
| P3-01 | Toda operación debe ejecutarse por un usuario autenticado. | [SPEC p.7 §10 RG-01] |
| P3-02 | Cada usuario tendrá un único rol dentro del sistema. | [SPEC p.7 §10 RG-02] |
| P3-03 | Ningún participante podrá administrar información fuera de su rol. | [SPEC p.8 §10 RG-03] |
| P3-04 | No se puede reservar inventario inexistente. | [SPEC p.8 §11 Inventario] |
| P3-05 | No se puede reservar inventario marcado como “Dañado”. | [SPEC p.8 §11 Inventario] |
| P3-06 | Un pedido finalizado no puede modificarse bajo ninguna circunstancia. | [SPEC p.8 §11 Pedidos] |
| P3-07 | Documento de identidad y correo electrónico deben ser únicos en la plataforma. | [SPEC p.8 §11 Usuarios] |
| P3-08 | No se permiten existencias negativas bajo ninguna circunstancia. | [SPEC p.7 Dominio 6] |
| P3-09 | El PDF no exige autenticación técnica concreta (JWT, sesiones, BCrypt); solo exige usuario autenticado. | [SPEC p.7 RG-01] + [TECH] |
| P3-10 | El PDF no menciona cancelación de pedidos, IVA, reembolsos parciales, ni reglas de aprobación de devoluciones. | [SUPUESTO] |

## Verificación de contradicciones

- `OrderStatus.CANCELLED`, `UserStatus.INACTIVE`, estados comerciales concretos y la separación `DELIVERED`/`FINALIZED` no están definidos explícitamente en el PDF; se registran como decisiones derivadas o supuestos, no como requisitos verificados.
- La regla de no modificar pedidos finalizados sí está respaldada y es obligatoria.
- La regla de inventario dañado sí está respaldada y es obligatoria.
