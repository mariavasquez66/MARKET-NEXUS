# Pasada 1 — Panorama funcional

- Fuente: `SDD/Especificación Funcional del Negocio - NexusMarket.pdf`
- Páginas: 8
- Extracción: PDFBox 2.0.31, `PDFTextStripper` con `sortByPosition=true`.
- SHA-256 del texto extraído: `1C994B68FE6A237F7C67AA1AB98D5A47373E4E24D1690EDE082DB9C1C13109F5`
- Foco: contexto, objetivos, alcance, componentes y participantes.

## Hallazgos etiquetados

| ID | Hallazgo | Etiqueta RVI |
|---|---|---|
| P1-01 | NexusMarket es una plataforma centralizada que intermedia entre compradores y vendedores y administra registro, catálogo, logística, facturación y posventa. | [SPEC p.1 §1] |
| P1-02 | Los objetivos funcionales son OBJ-01 a OBJ-12: usuarios, vendedores, compradores, bodegas, catálogo, inventario, carrito, pedidos, facturación, logística, devoluciones/reembolsos y reportes. | [SPEC p.1-2 §2] |
| P1-03 | El alcance incluye 13 procesos: registro de compradores, administración de usuarios, registro administrativo de vendedores, bodegas, catálogo, inventario, carrito, pedidos, facturación, envíos, devoluciones, reembolsos y reportes administrativos. | [SPEC p.2-3 §3.1] |
| P1-04 | Quedan fuera del alcance las interfaces, apps móviles, portales, autenticación técnica, tecnologías, arquitectura y almacenamiento. | [SPEC p.3 §3.2] |
| P1-05 | Los componentes del negocio son usuarios, vendedores, compradores, bodegas, productos, inventario, pedidos, facturación y envíos. | [SPEC p.3-4 §4.1] |
| P1-06 | Los participantes son Comprador, Vendedor, Operador Logístico, Administrador y Supervisor. | [SPEC p.4 §5] |
| P1-07 | Cada participante desempeña un único rol y solo interactúa con la información de sus funciones. | [SPEC p.4 §5] |
| P1-08 | El flujo general tiene 8 pasos: incorporación, catálogo, inventario, publicación, compra, transacción, logística y cierre. | [SPEC p.4 §6.1] |
| P1-09 | El Administrador registra al vendedor y su primera bodega. | [SPEC p.4 §6.1.1] |
| P1-10 | El vendedor registra productos y define sus características. | [SPEC p.4 §6.1.2] |
| P1-11 | Las existencias iniciales se registran en las bodegas asociadas, sin indicar el rol ejecutor. | [SPEC p.4 §6.1.3] + [SUPUESTO: rol ejecutor no especificado] |
| P1-12 | El comprador selecciona productos en el carrito y confirma el pedido; luego se valida el pago. | [SPEC p.4 §6.1.5-6] |
| P1-13 | El pedido se marca finalizado tras la entrega confirmada. | [SPEC p.4 §6.1.8] |

## Observaciones de alcance

- El PDF no define autenticación técnica, tecnologías ni arquitectura; todo eso es `[TECH]` y se rige por el SDD/prompt.
- El PDF no menciona IVA, cancelación, envíos por bodega ni reglas de aprobación de devoluciones; no se tratarán como requisitos verificados.
