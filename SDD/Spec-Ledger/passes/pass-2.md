# Pasada 2 — Dominios, atributos y estados

- Fuente: `SDD/Especificación Funcional del Negocio - NexusMarket.pdf`
- Extracción repetida desde el PDF: PDFBox 2.0.31, `sortByPosition=true`.
- SHA-256 del texto extraído: `1C994B68FE6A237F7C67AA1AB98D5A47373E4E24D1690EDE082DB9C1C13109F5`
- Foco: Dominios 1–7 del PDF, atributos, catálogos y restricciones explícitas.

## Hallazgos etiquetados

| ID | Hallazgo | Etiqueta RVI |
|---|---|---|
| P2-01 | Usuario: identificador obligatorio y único. | [SPEC p.5 Dominio 1] |
| P2-02 | Usuario: nombre completo obligatorio y no vacío. | [SPEC p.5 Dominio 1] |
| P2-03 | Usuario: correo electrónico obligatorio, único y medio principal de acceso/comunicación. | [SPEC p.5 Dominio 1] |
| P2-04 | Usuario: rol obligatorio y único por usuario. | [SPEC p.5 Dominio 1] |
| P2-05 | Usuario: estado obligatorio con catálogo definido; el PDF solo ejemplifica Activo y Bloqueado con “etc.”. | [SPEC p.5 Dominio 1] + [SUPUESTO: catálogo completo] |
| P2-06 | Comprador: dirección principal obligatoria, direcciones adicionales opcionales y estado comercial obligatorio. | [SPEC p.6 Dominio 2] |
| P2-07 | El comprador nunca administra información de otros compradores ni inventarios. | [SPEC p.6 Dominio 2] |
| P2-08 | Los vendedores no pueden autorregistrarse; los incorpora el Administrador. | [SPEC p.6 Dominio 3] |
| P2-09 | Bodegas: espacios físicos de almacenamiento, clasificados en bodegas del Marketplace y bodegas de Vendedores. | [SPEC p.6 Dominio 4] |
| P2-10 | Producto: tipo Físico o Digital. Los físicos requieren inventario y despacho; los digitales se entregan inmediatamente tras el pago. | [SPEC p.6-7 Dominio 5] |
| P2-11 | Producto: variantes como lista (color, talla, modelo, etc.). | [SPEC p.7 Dominio 5] |
| P2-12 | Producto: estado Publicado, Suspendido o Descontinuado. | [SPEC p.7 Dominio 5] |
| P2-13 | Inventario distribuido, obligatoriamente vinculado a un producto y a una bodega. | [SPEC p.7 Dominio 6] |
| P2-14 | Movimientos de inventario: Ingreso, Reserva, Salida por venta, Ajuste y Devolución. | [SPEC p.7 Dominio 6] |
| P2-15 | No se permiten existencias negativas bajo ninguna circunstancia. | [SPEC p.7 Dominio 6] |
| P2-16 | No se puede reservar inventario inexistente o marcado como “Dañado”. | [SPEC p.8 §11] |
| P2-17 | Pedido: compromiso comercial formal y ciclo de vida central del sistema. | [SPEC p.7 Dominio 7] |
| P2-18 | Estados del pedido: Carrito → Pendiente de Pago → Pagado → Despachado → Entregado/Finalizado. | [SPEC p.7 Dominio 7] |
| P2-19 | Un pedido finalizado no puede modificarse bajo ninguna circunstancia. | [SPEC p.8 §11] |
| P2-20 | El PDF no define atributos fiscales del vendedor, propietario/responsable de bodega, valores de estado comercial ni un estado separado de cancelación. | [SUPUESTO] |

## Observaciones

- “Entregado / Finalizado” aparece como un único paso del ciclo; no se afirma que sean dos estados obligatorios distintos.
- El PDF no describe el modelo de datos ni la arquitectura; esas decisiones son `[TECH]`.
